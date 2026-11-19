package com.example.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.model.SLOT_COUNT
import com.example.model.SlotCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts and persists per-slot login credentials (username/password) locally on-device using
 * Android Keystore AES-256-GCM.
 */
class SlotCredentialsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _credentialsFlow = MutableStateFlow(loadAllCredentials())
    val credentialsFlow: StateFlow<List<SlotCredentials>> = _credentialsFlow.asStateFlow()

    suspend fun saveCredentials(
        slotIndex: Int,
        username: String,
        password: String,
        autoFillLogin: Boolean,
    ) = withContext(Dispatchers.IO) {
        if (slotIndex !in 0 until SLOT_COUNT) return@withContext
        val cleanUser = username.trim()
        if (cleanUser.isBlank() && password.isBlank()) {
            clearCredentials(slotIndex)
            return@withContext
        }
        val payload = JSONObject().apply {
            put("u", cleanUser)
            put("p", password)
            put("a", autoFillLogin)
        }.toString()

        val encrypted = encryptString(payload)
        prefs.edit().putString(keyForSlot(slotIndex), encrypted).apply()
        _credentialsFlow.value = loadAllCredentials()
    }

    suspend fun clearCredentials(slotIndex: Int) = withContext(Dispatchers.IO) {
        if (slotIndex !in 0 until SLOT_COUNT) return@withContext
        prefs.edit().remove(keyForSlot(slotIndex)).apply()
        _credentialsFlow.value = loadAllCredentials()
    }

    fun getCredentials(slotIndex: Int): SlotCredentials {
        return _credentialsFlow.value.getOrElse(slotIndex) { SlotCredentials(slotIndex = slotIndex) }
    }

    private fun loadAllCredentials(): List<SlotCredentials> = List(SLOT_COUNT) { idx ->
        val stored = prefs.getString(keyForSlot(idx), null)
        if (stored.isNullOrBlank()) {
            SlotCredentials(slotIndex = idx)
        } else {
            val decrypted = decryptString(stored)
            if (decrypted == null) {
                SlotCredentials(slotIndex = idx)
            } else {
                runCatching {
                    val json = JSONObject(decrypted)
                    SlotCredentials(
                        slotIndex = idx,
                        username = json.optString("u", ""),
                        password = json.optString("p", ""),
                        autoFillLogin = json.optBoolean("a", true),
                    )
                }.getOrElse { SlotCredentials(slotIndex = idx) }
            }
        }
    }

    private fun encryptString(plainText: String): String {
        val secretKey = getOrCreateKeystoreKey()
        if (secretKey != null) {
            return runCatching {
                val cipher = Cipher.getInstance(AES_GCM_TRANSFORM)
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                val iv = cipher.iv
                val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
                val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
                val dataB64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
                "KS1:$ivB64:$dataB64"
            }.getOrElse { fallbackObfuscate(plainText) }
        }
        return fallbackObfuscate(plainText)
    }

    private fun decryptString(encoded: String): String? {
        if (encoded.startsWith("KS1:")) {
            val parts = encoded.split(":", limit = 3)
            if (parts.size != 3) return null
            val secretKey = getOrCreateKeystoreKey() ?: return null
            return runCatching {
                val iv = Base64.decode(parts[1], Base64.NO_WRAP)
                val cipherBytes = Base64.decode(parts[2], Base64.NO_WRAP)
                val cipher = Cipher.getInstance(AES_GCM_TRANSFORM)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
                String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
            }.getOrNull()
        }
        if (encoded.startsWith("RAW1:")) {
            return runCatching {
                val raw = Base64.decode(encoded.removePrefix("RAW1:"), Base64.NO_WRAP)
                String(raw, Charsets.UTF_8)
            }.getOrNull()
        }
        return null
    }

    // Used only if AndroidKeyStore provider is unavailable (e.g. local Robolectric JVM tests)
    private fun fallbackObfuscate(plainText: String): String {
        val b64 = Base64.encodeToString(plainText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return "RAW1:$b64"
    }

    @Volatile
    private var cachedSecretKey: SecretKey? = null

    private fun getOrCreateKeystoreKey(): SecretKey? {
        cachedSecretKey?.let { return it }
        return runCatching {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existing = ks.getKey(KEY_ALIAS, null) as? SecretKey
            val resolved = existing ?: run {
                val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build()
                keyGen.init(spec)
                keyGen.generateKey()
            }
            cachedSecretKey = resolved
            resolved
        }.getOrNull()
    }

    companion object {
        private const val PREFS_NAME = "dante_slot_vault"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "dante_slot_credentials_aes_v1"
        private const val AES_GCM_TRANSFORM = "AES/GCM/NoPadding"

        private fun keyForSlot(index: Int) = "slot_cred_$index"

        fun buildLoginAutofillScript(
            username: String,
            password: String,
            autoSubmit: Boolean,
            forceOverwrite: Boolean,
        ): String {
            val quotedUser = JSONObject.quote(username)
            val quotedPass = JSONObject.quote(password)
            val submitFlag = if (autoSubmit) "true" else "false"
            val forceFlag = if (forceOverwrite) "true" else "false"
            return """
                (function() {
                    try {
                        var userVal = $quotedUser;
                        var passVal = $quotedPass;
                        var autoSubmit = $submitFlag;
                        var forceOverwrite = $forceFlag;
                        var setNativeVal = function(el, val) {
                            if (!el) return false;
                            var proto = window.HTMLInputElement && window.HTMLInputElement.prototype;
                            var desc = proto ? Object.getOwnPropertyDescriptor(proto, 'value') : null;
                            if (desc && desc.set) {
                                desc.set.call(el, val);
                            } else {
                                el.value = val;
                            }
                            el.dispatchEvent(new Event('input', { bubbles: true }));
                            el.dispatchEvent(new Event('change', { bubbles: true }));
                            el.dispatchEvent(new Event('blur', { bubbles: true }));
                            return true;
                        };
                        var isVisible = function(el) {
                            return el && !el.disabled && el.offsetParent !== null;
                        };
                        var allInputs = Array.prototype.slice.call(document.querySelectorAll('input'));
                        var passInput = allInputs.find(function(i) {
                            var t = (i.type || '').toLowerCase();
                            var ac = (i.autocomplete || '').toLowerCase();
                            return (t === 'password' || ac === 'current-password') && isVisible(i);
                        });
                        if (!passInput) return 'NO_PASSWORD_FIELD';
                        if (!forceOverwrite && passInput.value && passInput.value.length > 0) {
                            return 'ALREADY_FILLED';
                        }
                        var scope = passInput.form || passInput.closest('form') || document;
                        var textInputs = Array.prototype.slice.call(scope.querySelectorAll('input')).filter(function(i) {
                            var t = (i.type || 'text').toLowerCase();
                            return i !== passInput && isVisible(i) && (t === 'email' || t === 'text' || t === 'tel' || t === 'username' || t === '');
                        });
                        var userInput = textInputs.find(function(i) {
                            var meta = ((i.name || '') + ' ' + (i.id || '') + ' ' + (i.placeholder || '') + ' ' + (i.autocomplete || '')).toLowerCase();
                            return meta.indexOf('email') !== -1 || meta.indexOf('user') !== -1 || meta.indexOf('login') !== -1 || meta.indexOf('conta') !== -1 || meta.indexOf('nick') !== -1;
                        }) || textInputs[0];
                        if (userInput && (forceOverwrite || !userInput.value)) {
                            setNativeVal(userInput, userVal);
                        }
                        setNativeVal(passInput, passVal);
                        if (autoSubmit) {
                            setTimeout(function() {
                                var btns = Array.prototype.slice.call(scope.querySelectorAll('button, input[type="submit"]')).filter(function(b) {
                                    return !b.disabled && isVisible(b);
                                });
                                var submitBtn = btns.find(function(b) {
                                    var t = (b.type || '').toLowerCase();
                                    var txt = (b.innerText || b.value || '').toLowerCase();
                                    return t === 'submit' || txt.indexOf('entrar') !== -1 || txt.indexOf('login') !== -1 || txt.indexOf('log in') !== -1 || txt.indexOf('sign in') !== -1 || txt.indexOf('acessar') !== -1 || txt.indexOf('conectar') !== -1;
                                });
                                if (submitBtn) {
                                    submitBtn.click();
                                } else if (passInput.form && passInput.form.requestSubmit) {
                                    passInput.form.requestSubmit();
                                } else if (passInput.form) {
                                    passInput.form.submit();
                                } else {
                                    passInput.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', keyCode: 13, which: 13, bubbles: true }));
                                    passInput.dispatchEvent(new KeyboardEvent('keyup', { key: 'Enter', keyCode: 13, which: 13, bubbles: true }));
                                }
                            }, 200);
                        }
                        return 'FILLED';
                    } catch (e) {
                        return 'ERR';
                    }
                })();
            """.trimIndent()
        }
    }
}
