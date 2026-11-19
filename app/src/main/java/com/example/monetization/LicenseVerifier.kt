package com.example.monetization

import android.util.Base64
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

sealed interface LicenseValidationResult {
    data class Valid(
        val holderName: String,
        val expiresAtMs: Long,
        val boundDeviceId: String,
        val planCode: String,
    ) : LicenseValidationResult

    data class Invalid(val reason: String) : LicenseValidationResult
}

object LicenseVerifier {
    // ECDSA P-256 Public Key ONLY (safe to embed in APK; cannot generate licenses)
    private const val DEFAULT_PUBLIC_KEY_DER_B64 =
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE435IrFRb0oikfJ+KlTVJVPnjgLTkqp/gQRN85CUmUVEv1Ef8s/vDmuNKw7G1UA/hAQ/UezO2CNO738JZUQLuIQ=="

    fun verify(
        rawKey: String,
        currentDeviceId: String,
        nowMs: Long = System.currentTimeMillis(),
        maxSeenClockMs: Long = 0L,
        publicKeyDerBase64: String = DEFAULT_PUBLIC_KEY_DER_B64,
    ): LicenseValidationResult {
        val trimmed = rawKey.trim().replace("\\s+".toRegex(), "")
        val parts = trimmed.split(".")
        if (parts.size != 3 || parts[0] != "DF1") {
            return LicenseValidationResult.Invalid("Formato de chave inválido. Use uma chave DF1 válida.")
        }

        val payloadBytes = decodeBase64Url(parts[1])
            ?: return LicenseValidationResult.Invalid("Dados da chave corrompidos.")
        val signatureBytes = decodeBase64Url(parts[2])
            ?: return LicenseValidationResult.Invalid("Assinatura da chave corrompida.")

        val signatureValid = verifyEcdsaSignature(
            payload = parts[1].toByteArray(Charsets.UTF_8),
            rawOrDerSignature = signatureBytes,
            publicKey = loadPublicKey(publicKeyDerBase64),
        )

        if (!signatureValid) {
            return LicenseValidationResult.Invalid("Assinatura criptográfica inválida.")
        }

        val json = runCatching { JSONObject(String(payloadBytes, Charsets.UTF_8)) }.getOrNull()
            ?: return LicenseValidationResult.Invalid("Conteúdo da licença ilegível.")

        val targetDevice = json.optString("d", "").trim().uppercase()
        val planCode = json.optString("p", "PRO").trim().uppercase()
        val expiresAtMs = json.optLong("exp", 0L)
        val holderName = json.optString("n", "Assinante PRO").trim().ifBlank { "Assinante PRO" }

        if (targetDevice != "*" && targetDevice != currentDeviceId.uppercase()) {
            return LicenseValidationResult.Invalid(
                "Esta chave pertence ao dispositivo $targetDevice (seu ID: $currentDeviceId)."
            )
        }

        val effectiveClockMs = maxOf(nowMs, maxSeenClockMs)
        if (expiresAtMs <= effectiveClockMs) {
            return LicenseValidationResult.Invalid("Esta licença mensal expirou. Renove sua assinatura.")
        }

        return LicenseValidationResult.Valid(
            holderName = holderName,
            expiresAtMs = expiresAtMs,
            boundDeviceId = targetDevice,
            planCode = planCode,
        )
    }

    private fun verifyEcdsaSignature(
        payload: ByteArray,
        rawOrDerSignature: ByteArray,
        publicKey: PublicKey?,
    ): Boolean {
        if (publicKey == null) return false
        val derSig = if (rawOrDerSignature.size == 64 && rawOrDerSignature[0] != 0x30.toByte()) {
            p1363ToDer(rawOrDerSignature)
        } else {
            rawOrDerSignature
        }
        return runCatching {
            val sig = Signature.getInstance("SHA256withECDSA")
            sig.initVerify(publicKey)
            sig.update(payload)
            sig.verify(derSig)
        }.getOrDefault(false)
    }

    private fun loadPublicKey(derBase64: String): PublicKey? = runCatching {
        val bytes = Base64.decode(derBase64, Base64.DEFAULT)
        KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(bytes))
    }.getOrNull()

    private fun decodeBase64Url(input: String): ByteArray? = runCatching {
        Base64.decode(input, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }.getOrNull()

    // Converts 64-byte IEEE P1363 (r || s) from WebCrypto into ASN.1 DER for Java Signature
    private fun p1363ToDer(p1363: ByteArray): ByteArray {
        val r = toDerInteger(p1363.copyOfRange(0, 32))
        val s = toDerInteger(p1363.copyOfRange(32, 64))
        val out = ByteArrayOutputStream()
        out.write(0x30)
        val totalLen = r.size + s.size
        if (totalLen >= 128) {
            out.write(0x81)
        }
        out.write(totalLen)
        out.write(r)
        out.write(s)
        return out.toByteArray()
    }

    private fun toDerInteger(bytes: ByteArray): ByteArray {
        var offset = 0
        while (offset < bytes.size - 1 && bytes[offset] == 0.toByte()) {
            offset++
        }
        val trimmed = bytes.copyOfRange(offset, bytes.size)
        val needsZeroPad = (trimmed[0].toInt() and 0x80) != 0
        val out = ByteArrayOutputStream()
        out.write(0x02)
        out.write(trimmed.size + if (needsZeroPad) 1 else 0)
        if (needsZeroPad) out.write(0x00)
        out.write(trimmed)
        return out.toByteArray()
    }
}
