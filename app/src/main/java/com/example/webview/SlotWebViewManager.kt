package com.example.webview

import android.annotation.SuppressLint
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.MutableContextWrapper
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.data.SlotCredentialsStore
import com.example.model.AccountSlotConfig
import com.example.model.SLOT_COUNT
import com.example.model.SlotCredentials
import com.example.model.SlotRuntimeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.abs

class FarmWebView(context: Context) : WebView(context) {
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        return super.onTouchEvent(event)
    }
}

class SlotWebViewManager(private val appContext: Context) {

    private val webViews = arrayOfNulls<FarmWebView>(SLOT_COUNT)
    private val defaultUserAgents = arrayOfNulls<String>(SLOT_COUNT)
    private val lastAppliedConfigs = arrayOfNulls<AccountSlotConfig>(SLOT_COUNT)
    private val slotCredentials = arrayOfNulls<SlotCredentials>(SLOT_COUNT)
    private val lastKnownUrls = arrayOfNulls<String>(SLOT_COUNT)
    private val lastAutoSubmitTimeMs = LongArray(SLOT_COUNT)
    private val consecutiveAutoLoginAttempts = IntArray(SLOT_COUNT)
    private val rendererRecoveryCount = IntArray(SLOT_COUNT)
    private val verifiedProfileIsolated = BooleanArray(SLOT_COUNT)
    private val runtimeForcePaused = BooleanArray(SLOT_COUNT)
    private val lastReportedProgress = IntArray(SLOT_COUNT) { -1 }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val ioExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "dante-webview-io").apply { isDaemon = true }
    }

    @Volatile
    private var cacheDirsPrepared = false

    val isMultiProfileSupported: Boolean =
        WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)

    val isMuteAudioSupported: Boolean =
        WebViewFeature.isFeatureSupported(WebViewFeature.MUTE_AUDIO)

    private val _runtimeStates = MutableStateFlow(
        List(SLOT_COUNT) { idx ->
            SlotRuntimeState(
                index = idx,
                isIsolatedProfileActive = isMultiProfileSupported,
            )
        }
    )
    val runtimeStates: StateFlow<List<SlotRuntimeState>> = _runtimeStates.asStateFlow()

    @Volatile
    var keepAliveInBackground: Boolean = true

    init {
        ensureWebViewCacheDirectories()
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateWebView(
        slotIndex: Int,
        initialConfig: AccountSlotConfig,
        hostContext: Context = appContext,
    ): FarmWebView {
        val existing = webViews[slotIndex]
        if (existing != null) {
            (existing.context as? MutableContextWrapper)?.baseContext = hostContext
            (existing.parent as? ViewGroup)?.removeView(existing)
            return existing
        }

        ensureWebViewCacheDirectories()

        val webView = FarmWebView(MutableContextWrapper(hostContext)).apply {
            setBackgroundColor(Color.parseColor("#070203"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            isFocusable = true
            isFocusableInTouchMode = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Keep renderer at full priority even when app is in background (e.g. YouTube)
                setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
            }
        }

        // Assign isolated WebView profile BEFORE configuring settings or loading any URL
        var slotIsolated = false
        if (isMultiProfileSupported) {
            runCatching {
                val profileName = profileNameForSlot(slotIndex)
                val profile = ProfileStore.getInstance().getOrCreateProfile(profileName)
                WebViewCompat.setProfile(webView, profile.name)
                val activeProfileName = runCatching { WebViewCompat.getProfile(webView).name }.getOrNull()
                slotIsolated = activeProfileName == profile.name
                profile.cookieManager.setAcceptCookie(true)
                profile.cookieManager.setAcceptThirdPartyCookies(webView, true)
            }
        } else {
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        }
        verifiedProfileIsolated[slotIndex] = slotIsolated
        updateSlotState(slotIndex) { it.copy(isIsolatedProfileActive = slotIsolated) }

        val settings = webView.settings
        defaultUserAgents[slotIndex] = settings.userAgentString
        @Suppress("DEPRECATION")
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = false
            setGeolocationEnabled(false)
            saveFormData = false
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportMultipleWindows(false)
            allowFileAccess = false
            allowContentAccess = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
            ): Boolean {
                val scheme = request?.url?.scheme?.lowercase() ?: return false
                // Keep http/https inside the isolated WebView; ignore external schemes safely
                return scheme != "http" && scheme != "https"
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                if (!url.isNullOrBlank()) {
                    lastKnownUrls[slotIndex] = url
                }
                updateSlotState(slotIndex) {
                    it.copy(
                        currentUrl = url ?: it.currentUrl,
                        isLoading = true,
                        hasError = false,
                        errorDescription = null,
                        canGoBack = view?.canGoBack() ?: false,
                    )
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                if (!url.isNullOrBlank()) {
                    lastKnownUrls[slotIndex] = url
                }
                rendererRecoveryCount[slotIndex] = 0
                flushSlotCookies(slotIndex)

                val cfg = lastAppliedConfigs[slotIndex] ?: initialConfig
                applyLiveZoom(view, cfg.zoomPercent)
                if (keepAliveInBackground) {
                    injectBackgroundVisibilityShim(view)
                }
                if (cfg.isMuted && !isMuteAudioSupported) {
                    injectFallbackAudioMute(view, true)
                }
                updateSlotState(slotIndex) {
                    it.copy(
                        currentUrl = url ?: it.currentUrl,
                        pageTitle = view?.title.orEmpty(),
                        isLoading = false,
                        progress = 100,
                        canGoBack = view?.canGoBack() ?: false,
                        lastReloadTimestampMs = System.currentTimeMillis(),
                    )
                }
                if (!cfg.isPaused) {
                    maybeAutoFillLogin(slotIndex, view, delayMs = 700L)
                    maybeAutoFillLogin(slotIndex, view, delayMs = 2400L)
                }
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
                if (!url.isNullOrBlank()) {
                    lastKnownUrls[slotIndex] = url
                }
                updateSlotState(slotIndex) {
                    it.copy(
                        currentUrl = url ?: it.currentUrl,
                        canGoBack = view?.canGoBack() ?: it.canGoBack,
                    )
                }
                val cfg = lastAppliedConfigs[slotIndex]
                if (cfg?.isPaused != true) {
                    maybeAutoFillLogin(slotIndex, view, delayMs = 900L)
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?,
            ) {
                if (request?.isForMainFrame == true) {
                    updateSlotState(slotIndex) {
                        it.copy(
                            isLoading = false,
                            hasError = true,
                            errorDescription = error?.description?.toString()
                                ?: "Falha ao carregar página",
                        )
                    }
                }
            }

            override fun onRenderProcessGone(
                view: WebView?,
                detail: RenderProcessGoneDetail?,
            ): Boolean {
                webViews[slotIndex]?.let { wv ->
                    (wv.parent as? ViewGroup)?.removeView(wv)
                    wv.destroy()
                }
                webViews[slotIndex] = null
                lastAppliedConfigs[slotIndex] = null

                val shouldAutoRecover =
                    detail?.didCrash() != true && rendererRecoveryCount[slotIndex] < MAX_RENDERER_RECOVERIES

                if (shouldAutoRecover) {
                    rendererRecoveryCount[slotIndex]++
                    updateSlotState(slotIndex) {
                        it.copy(
                            isLoading = true,
                            hasError = false,
                            errorDescription = null,
                            recoveryNonce = it.recoveryNonce + 1,
                        )
                    }
                } else {
                    updateSlotState(slotIndex) {
                        it.copy(
                            isLoading = false,
                            hasError = true,
                            errorDescription = "Processo da aba reiniciado. Toque em tentar novamente.",
                        )
                    }
                }
                return true
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                val prev = lastReportedProgress[slotIndex]
                val shouldEmit = newProgress == 100 ||
                    prev < 0 ||
                    (prev == 100 && newProgress < 100) ||
                    abs(newProgress - prev) >= 15
                if (!shouldEmit) return
                lastReportedProgress[slotIndex] = newProgress
                updateSlotState(slotIndex) {
                    val nextLoading = newProgress < 100
                    if (it.progress == newProgress && it.isLoading == nextLoading) it
                    else it.copy(progress = newProgress, isLoading = nextLoading)
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                val cleanTitle = title.orEmpty()
                updateSlotState(slotIndex) {
                    if (it.pageTitle == cleanTitle) it else it.copy(pageTitle = cleanTitle)
                }
            }
        }

        webViews[slotIndex] = webView
        applyConfig(
            slotIndex = slotIndex,
            config = initialConfig,
            forceLoadIfEmpty = true,
            overrideLoadUrl = lastKnownUrls[slotIndex],
        )
        return webView
    }

    fun applyConfig(
        slotIndex: Int,
        config: AccountSlotConfig,
        forceLoadIfEmpty: Boolean = false,
        overrideLoadUrl: String? = null,
    ) {
        val webView = webViews[slotIndex] ?: return
        val prev = lastAppliedConfigs[slotIndex]
        lastAppliedConfigs[slotIndex] = config

        // 1. Desktop vs Mobile UA
        if (prev == null || prev.desktopMode != config.desktopMode) {
            val targetUa = if (config.desktopMode) {
                DESKTOP_USER_AGENT
            } else {
                defaultUserAgents[slotIndex]
            }
            if (targetUa != null && webView.settings.userAgentString != targetUa) {
                webView.settings.userAgentString = targetUa
                if (prev != null && !config.isPaused) {
                    webView.reload()
                }
            }
        }

        // 2. Zoom level (both initialScale & instant CSS zoom without reloading)
        if (prev == null || prev.zoomPercent != config.zoomPercent) {
            webView.setInitialScale(config.zoomPercent)
            applyLiveZoom(webView, config.zoomPercent)
        }

        // 3. Per-slot audio mute
        if (prev == null || prev.isMuted != config.isMuted) {
            if (isMuteAudioSupported) {
                runCatching { WebViewCompat.setAudioMuted(webView, config.isMuted) }
            } else {
                injectFallbackAudioMute(webView, config.isMuted)
            }
        }

        // 4. Pause / Resume (uses per-instance onPause/onResume — NEVER global pauseTimers)
        if (prev == null || prev.isPaused != config.isPaused || runtimeForcePaused[slotIndex]) {
            if (config.isPaused) {
                webView.onPause()
            } else {
                runtimeForcePaused[slotIndex] = false
                webView.onResume()
                if (isMuteAudioSupported) {
                    runCatching { WebViewCompat.setAudioMuted(webView, config.isMuted) }
                } else {
                    injectFallbackAudioMute(webView, config.isMuted)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
                }
            }
        }

        // 5. URL navigation only when explicitly forced or user genuinely changed the slot URL
        val urlChanged = prev != null && normalizeComparableUrl(prev.url) != normalizeComparableUrl(config.url)
        if (forceLoadIfEmpty || urlChanged) {
            if (!config.isPaused) {
                val targetUrl = if (forceLoadIfEmpty && !overrideLoadUrl.isNullOrBlank()) {
                    overrideLoadUrl
                } else {
                    config.url
                }
                if (targetUrl.isBlank()) {
                    lastKnownUrls[slotIndex] = null
                    if (urlChanged) {
                        webView.loadUrl("about:blank")
                    }
                    updateSlotState(slotIndex) {
                        it.copy(
                            currentUrl = "",
                            isLoading = false,
                            hasError = false,
                            errorDescription = null,
                        )
                    }
                } else {
                    webView.loadUrl(targetUrl)
                }
            }
        }
    }

    fun reloadSlot(slotIndex: Int, targetUrl: String? = null) {
        rendererRecoveryCount[slotIndex] = 0
        consecutiveAutoLoginAttempts[slotIndex] = 0
        val webView = webViews[slotIndex]
        val cfgUrl = lastAppliedConfigs[slotIndex]?.url.orEmpty()
        if (targetUrl.isNullOrBlank() && cfgUrl.isBlank() && webView?.url.isNullOrBlank()) {
            return
        }
        if (webView == null) {
            updateSlotState(slotIndex) {
                it.copy(
                    isLoading = true,
                    hasError = false,
                    errorDescription = null,
                    recoveryNonce = it.recoveryNonce + 1,
                )
            }
            return
        }
        if (!targetUrl.isNullOrBlank()) {
            webView.loadUrl(targetUrl)
        } else {
            if (webView.url.isNullOrBlank() || webView.url == "about:blank") {
                if (cfgUrl.isBlank()) return
                webView.loadUrl(cfgUrl)
            } else {
                webView.reload()
            }
        }
    }

    fun reloadAll() {
        var staggerIndex = 0
        for (i in 0 until SLOT_COUNT) {
            val cfg = lastAppliedConfigs[i]
            if (cfg?.isPaused != true && !cfg?.url.isNullOrBlank()) {
                val delayMs = staggerIndex * 350L
                staggerIndex++
                if (delayMs == 0L) {
                    reloadSlot(i)
                } else {
                    mainHandler.postDelayed({ reloadSlot(i) }, delayMs)
                }
            }
        }
    }

    fun goBack(slotIndex: Int) {
        val webView = webViews[slotIndex] ?: return
        if (webView.canGoBack()) {
            webView.goBack()
        }
    }

    fun loadHomeUrl(slotIndex: Int, url: String) {
        if (url.isBlank()) return
        consecutiveAutoLoginAttempts[slotIndex] = 0
        webViews[slotIndex]?.loadUrl(url)
    }

    fun sendAntiAfkPulse() {
        val pulseJs = """
            (function() {
                try {
                    document.dispatchEvent(new Event('mousemove', { bubbles: true }));
                    window.dispatchEvent(new Event('focus'));
                } catch (e) {}
            })();
        """.trimIndent()
        var staggerIndex = 0
        for (i in 0 until SLOT_COUNT) {
            val cfg = lastAppliedConfigs[i]
            if (cfg?.isPaused != true && !cfg?.url.isNullOrBlank()) {
                val wv = webViews[i] ?: continue
                val delayMs = staggerIndex * 200L
                staggerIndex++
                val task = Runnable {
                    if (webViews[i] === wv && lastAppliedConfigs[i]?.isPaused != true) {
                        if (keepAliveInBackground) {
                            wv.onResume()
                        }
                        wv.evaluateJavascript(pulseJs, null)
                    }
                }
                if (delayMs == 0L) wv.post(task) else wv.postDelayed(task, delayMs)
            }
        }
    }

    fun resumeActiveWebViews() {
        for (i in 0 until SLOT_COUNT) {
            val cfg = lastAppliedConfigs[i]
            if (cfg?.isPaused != true && !runtimeForcePaused[i] && !cfg?.url.isNullOrBlank()) {
                val wv = webViews[i] ?: continue
                wv.onResume()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    wv.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
                }
            }
        }
    }

    fun pauseLockedSlot(slotIndex: Int) {
        if (slotIndex !in 1 until SLOT_COUNT) return
        if (runtimeForcePaused[slotIndex]) return
        runtimeForcePaused[slotIndex] = true
        val wv = webViews[slotIndex] ?: return
        wv.onPause()
        if (isMuteAudioSupported) {
            runCatching { WebViewCompat.setAudioMuted(wv, true) }
        } else {
            injectFallbackAudioMute(wv, true)
        }
    }

    fun checkAndAutoLoginAllSlots(proActive: Boolean) {
        val maxSlots = if (proActive) SLOT_COUNT else 1
        var staggerIndex = 0
        for (i in 0 until maxSlots) {
            val cfg = lastAppliedConfigs[i]
            if (cfg?.isPaused == true || cfg?.url.isNullOrBlank()) continue
            val cred = slotCredentials[i]
            if (cred == null || !cred.hasCredentials || !cred.autoFillLogin) continue
            val wv = webViews[i] ?: continue
            maybeAutoFillLogin(i, wv, delayMs = staggerIndex * 250L)
            staggerIndex++
        }
    }

    fun flushSlotCookies(slotIndex: Int) {
        ioExecutor.execute {
            if (isMultiProfileSupported) {
                runCatching {
                    ProfileStore.getInstance()
                        .getProfile(profileNameForSlot(slotIndex))
                        ?.cookieManager
                        ?.flush()
                }
            } else {
                runCatching { CookieManager.getInstance().flush() }
            }
        }
    }

    fun flushAllCookies() {
        val activeIndices = (0 until SLOT_COUNT).filter { webViews[it] != null }
        if (activeIndices.isEmpty()) return
        ioExecutor.execute {
            if (isMultiProfileSupported) {
                for (i in activeIndices) {
                    runCatching {
                        ProfileStore.getInstance()
                            .getProfile(profileNameForSlot(i))
                            ?.cookieManager
                            ?.flush()
                    }
                }
            } else {
                runCatching { CookieManager.getInstance().flush() }
            }
        }
    }

    @Suppress("DEPRECATION")
    fun trimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            // Clear RAM-only WebView resource cache (includeDiskFiles = false) to prevent OOM stalls
            // without touching saved cookies, localStorage, or login sessions.
            mainHandler.post {
                for (i in 0 until SLOT_COUNT) {
                    runCatching { webViews[i]?.clearCache(false) }
                }
            }
        }
    }

    fun updateCredentials(list: List<SlotCredentials>) {
        for (cred in list) {
            if (cred.slotIndex in 0 until SLOT_COUNT) {
                val prev = slotCredentials[cred.slotIndex]
                slotCredentials[cred.slotIndex] = cred
                if (prev != cred) {
                    consecutiveAutoLoginAttempts[cred.slotIndex] = 0
                    lastAutoSubmitTimeMs[cred.slotIndex] = 0L
                }
            }
        }
    }

    fun fillLoginNow(slotIndex: Int, overrideCredentials: SlotCredentials? = null) {
        val cred = overrideCredentials ?: slotCredentials.getOrNull(slotIndex) ?: return
        if (!cred.hasCredentials) return
        val wv = webViews.getOrNull(slotIndex) ?: return
        consecutiveAutoLoginAttempts[slotIndex] = 0
        lastAutoSubmitTimeMs[slotIndex] = System.currentTimeMillis()
        val script = SlotCredentialsStore.buildLoginAutofillScript(
            username = cred.username,
            password = cred.password,
            autoSubmit = true,
            forceOverwrite = true,
        )
        wv.evaluateJavascript(script) { result ->
            if (result?.contains("NO_PASSWORD_FIELD") == true) {
                consecutiveAutoLoginAttempts[slotIndex] = 0
            }
        }
    }

    private fun maybeAutoFillLogin(slotIndex: Int, view: WebView?, delayMs: Long = 700L) {
        val cred = slotCredentials.getOrNull(slotIndex) ?: return
        if (!cred.hasCredentials || !cred.autoFillLogin) return
        if (consecutiveAutoLoginAttempts[slotIndex] >= MAX_CONSECUTIVE_AUTO_LOGIN_ATTEMPTS) return
        val wv = view ?: webViews.getOrNull(slotIndex) ?: return
        val now = System.currentTimeMillis()
        if (now - lastAutoSubmitTimeMs[slotIndex] < AUTO_LOGIN_COOLDOWN_MS) return

        val script = SlotCredentialsStore.buildLoginAutofillScript(
            username = cred.username,
            password = cred.password,
            autoSubmit = true,
            forceOverwrite = false,
        )

        val executeCheck = Runnable {
            val latestCred = slotCredentials.getOrNull(slotIndex) ?: return@Runnable
            if (!latestCred.hasCredentials || !latestCred.autoFillLogin) return@Runnable
            if (consecutiveAutoLoginAttempts[slotIndex] >= MAX_CONSECUTIVE_AUTO_LOGIN_ATTEMPTS) return@Runnable
            if (System.currentTimeMillis() - lastAutoSubmitTimeMs[slotIndex] < AUTO_LOGIN_COOLDOWN_MS) return@Runnable

            wv.evaluateJavascript(script) { result ->
                when {
                    result?.contains("NO_PASSWORD_FIELD") == true -> {
                        // Logged in (no login form visible): reset failure streak so future disconnects auto-relogin
                        consecutiveAutoLoginAttempts[slotIndex] = 0
                    }
                    result?.contains("FILLED") == true -> {
                        lastAutoSubmitTimeMs[slotIndex] = System.currentTimeMillis()
                        consecutiveAutoLoginAttempts[slotIndex]++
                    }
                }
            }
        }

        if (delayMs <= 0L) {
            wv.post(executeCheck)
        } else {
            wv.postDelayed(executeCheck, delayMs)
        }
    }

    private fun injectBackgroundVisibilityShim(view: WebView?) {
        view?.evaluateJavascript(
            """
            (function() {
                try {
                    if (window.__danteKeepAlivePatched) return;
                    window.__danteKeepAlivePatched = true;
                    Object.defineProperty(document, 'hidden', { get: function() { return false; }, configurable: true });
                    Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; }, configurable: true });
                    document.hasFocus = function() { return true; };
                    document.addEventListener('visibilitychange', function(e) {
                        e.stopImmediatePropagation();
                    }, true);
                    window.addEventListener('blur', function(e) {
                        e.stopImmediatePropagation();
                    }, true);
                    document.addEventListener('blur', function(e) {
                        if (e.target === document || e.target === window) {
                            e.stopImmediatePropagation();
                        }
                    }, true);
                } catch (e) {}
            })();
            """.trimIndent(),
            null
        )
    }

    fun clearSlotSession(slotIndex: Int, reloadUrl: String) {
        consecutiveAutoLoginAttempts[slotIndex] = 0
        lastAutoSubmitTimeMs[slotIndex] = 0L
        val webView = webViews[slotIndex]
        val clearLocalJs = """
            try {
                localStorage.clear();
                sessionStorage.clear();
                if (window.indexedDB && indexedDB.databases) {
                    indexedDB.databases().then(function(dbs) {
                        dbs.forEach(function(db) { if (db.name) indexedDB.deleteDatabase(db.name); });
                    });
                }
            } catch(e) {}
        """.trimIndent()
        webView?.evaluateJavascript(clearLocalJs, null)

        val finishReload = {
            webView?.post {
                webView.clearCache(false)
                webView.clearHistory()
                webView.loadUrl(reloadUrl)
            }
            Unit
        }

        if (isMultiProfileSupported) {
            val profile = runCatching {
                ProfileStore.getInstance().getProfile(profileNameForSlot(slotIndex))
            }.getOrNull()
            if (profile != null) {
                profile.cookieManager.removeAllCookies {
                    profile.cookieManager.flush()
                    finishReload()
                }
            } else {
                finishReload()
            }
        } else {
            val cm = CookieManager.getInstance()
            cm.removeAllCookies {
                cm.flush()
                WebStorage.getInstance().deleteAllData()
                finishReload()
            }
        }
    }

    fun destroyAll() {
        mainHandler.removeCallbacksAndMessages(null)
        flushAllCookies()
        for (i in 0 until SLOT_COUNT) {
            webViews[i]?.let { wv ->
                (wv.context as? MutableContextWrapper)?.baseContext = appContext
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.stopLoading()
                wv.destroy()
            }
            webViews[i] = null
        }
        ioExecutor.shutdown()
    }

    private fun applyLiveZoom(view: WebView?, zoomPercent: Int) {
        view?.evaluateJavascript(
            "try { document.documentElement.style.zoom = '${zoomPercent}%'; } catch(e) {}",
            null
        )
    }

    private fun injectFallbackAudioMute(view: WebView?, muted: Boolean) {
        val flag = if (muted) "true" else "false"
        view?.evaluateJavascript(
            """
            (function() {
                try {
                    document.querySelectorAll('audio, video').forEach(function(el) {
                        el.muted = $flag;
                    });
                } catch(e) {}
            })();
            """.trimIndent(),
            null
        )
    }

    private fun updateSlotState(
        slotIndex: Int,
        transform: (SlotRuntimeState) -> SlotRuntimeState,
    ) {
        _runtimeStates.update { list ->
            val current = list[slotIndex]
            val updated = transform(current)
            if (updated == current) return@update list
            list.mapIndexed { idx, state ->
                if (idx == slotIndex) updated else state
            }
        }
    }

    private fun ensureWebViewCacheDirectories() {
        if (cacheDirsPrepared) return
        cacheDirsPrepared = true
        runCatching {
            val cacheRoot = appContext.cacheDir
            if (!cacheRoot.exists()) {
                cacheRoot.mkdirs()
            }
            val webViewCache = File(cacheRoot, "WebView")
            val profileDirs = listOf("Default", "Profile 1", "Profile 2", "Profile 3", "Profile 4")
            for (dirName in profileDirs) {
                val codeCache = File(webViewCache, "$dirName/HTTP Cache/Code Cache")
                File(codeCache, "js").mkdirs()
                File(codeCache, "wasm").mkdirs()
                File(codeCache, "webui_js").mkdirs()
            }
        }
    }

    companion object {
        private const val AUTO_LOGIN_COOLDOWN_MS = 20_000L
        private const val MAX_CONSECUTIVE_AUTO_LOGIN_ATTEMPTS = 3
        private const val MAX_RENDERER_RECOVERIES = 5
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        fun profileNameForSlot(slotIndex: Int): String = "digiidle_slot_${slotIndex + 1}"

        fun normalizeComparableUrl(raw: String): String = raw.trim().trimEnd('/')
    }
}
