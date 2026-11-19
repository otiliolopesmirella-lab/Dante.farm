package com.example.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.CookieManager
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
import com.example.model.AccountSlotConfig
import com.example.model.SLOT_COUNT
import com.example.model.SlotRuntimeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateWebView(slotIndex: Int, initialConfig: AccountSlotConfig): FarmWebView {
        val existing = webViews[slotIndex]
        if (existing != null) {
            (existing.parent as? ViewGroup)?.removeView(existing)
            return existing
        }

        val webView = FarmWebView(appContext).apply {
            setBackgroundColor(Color.parseColor("#090D16"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            isFocusable = true
            isFocusableInTouchMode = true
        }

        // Assign isolated WebView profile BEFORE configuring settings or loading any URL
        if (isMultiProfileSupported) {
            runCatching {
                val profileName = profileNameForSlot(slotIndex)
                val profile = ProfileStore.getInstance().getOrCreateProfile(profileName)
                WebViewCompat.setProfile(webView, profile.name)
                profile.cookieManager.setAcceptCookie(true)
                profile.cookieManager.setAcceptThirdPartyCookies(webView, true)
            }
        } else {
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        }

        val settings = webView.settings
        defaultUserAgents[slotIndex] = settings.userAgentString
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
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
                val cfg = lastAppliedConfigs[slotIndex] ?: initialConfig
                applyLiveZoom(view, cfg.zoomPercent)
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
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                updateSlotState(slotIndex) {
                    it.copy(
                        progress = newProgress,
                        isLoading = newProgress < 100,
                    )
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                updateSlotState(slotIndex) {
                    it.copy(pageTitle = title.orEmpty())
                }
            }
        }

        webViews[slotIndex] = webView
        applyConfig(slotIndex, initialConfig, forceLoadIfEmpty = true)
        return webView
    }

    fun applyConfig(
        slotIndex: Int,
        config: AccountSlotConfig,
        forceLoadIfEmpty: Boolean = false,
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
        if (prev == null || prev.isPaused != config.isPaused) {
            if (config.isPaused) {
                webView.onPause()
            } else {
                webView.onResume()
            }
        }

        // 5. URL navigation when changed or initial load
        if (forceLoadIfEmpty || (prev != null && prev.url != config.url)) {
            if (!config.isPaused) {
                webView.loadUrl(config.url)
            }
        }
    }

    fun reloadSlot(slotIndex: Int, targetUrl: String? = null) {
        val webView = webViews[slotIndex] ?: return
        if (targetUrl != null) {
            webView.loadUrl(targetUrl)
        } else {
            if (webView.url.isNullOrBlank()) {
                val cfgUrl = lastAppliedConfigs[slotIndex]?.url ?: return
                webView.loadUrl(cfgUrl)
            } else {
                webView.reload()
            }
        }
    }

    fun reloadAll() {
        for (i in 0 until SLOT_COUNT) {
            val cfg = lastAppliedConfigs[i]
            if (cfg?.isPaused != true) {
                reloadSlot(i)
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
        for (i in 0 until SLOT_COUNT) {
            val cfg = lastAppliedConfigs[i]
            if (cfg?.isPaused != true) {
                webViews[i]?.evaluateJavascript(pulseJs, null)
            }
        }
    }

    fun clearSlotSession(slotIndex: Int, reloadUrl: String) {
        val webView = webViews[slotIndex]
        if (isMultiProfileSupported) {
            runCatching {
                val profile = ProfileStore.getInstance().getProfile(profileNameForSlot(slotIndex))
                profile?.cookieManager?.removeAllCookies {
                    profile.cookieManager.flush()
                }
                // Clear site storage inside the profile via JS + WebStorage if needed
            }
        } else {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
        }
        webView?.apply {
            evaluateJavascript(
                "try { localStorage.clear(); sessionStorage.clear(); } catch(e) {}",
                null
            )
            clearCache(true)
            clearHistory()
            loadUrl(reloadUrl)
        }
    }

    fun destroyAll() {
        for (i in 0 until SLOT_COUNT) {
            webViews[i]?.let { wv ->
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.stopLoading()
                wv.destroy()
            }
            webViews[i] = null
        }
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
            list.mapIndexed { idx, state ->
                if (idx == slotIndex) transform(state) else state
            }
        }
    }

    companion object {
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        fun profileNameForSlot(slotIndex: Int): String = "digiidle_slot_${slotIndex + 1}"
    }
}
