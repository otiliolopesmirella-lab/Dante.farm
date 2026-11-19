package com.example.ui

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FarmPreferencesRepository
import com.example.data.SlotCredentialsStore
import com.example.model.AccountSlotConfig
import com.example.model.GlobalFarmSettings
import com.example.model.LayoutMode
import com.example.model.SLOT_COUNT
import com.example.model.SlotCredentials
import com.example.model.SlotRuntimeState
import com.example.model.defaultSlotConfigs
import com.example.monetization.DailyTrialEvaluator
import com.example.monetization.DeviceIdProvider
import com.example.monetization.LicenseValidationResult
import com.example.monetization.LicenseVerifier
import com.example.monetization.SubscriptionPreferencesRepository
import com.example.monetization.SubscriptionSource
import com.example.monetization.SubscriptionStatus
import com.example.service.FarmKeepAliveService
import com.example.webview.SlotWebViewManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class FarmUiState(
    val settings: GlobalFarmSettings = GlobalFarmSettings(),
    val slots: List<AccountSlotConfig> = defaultSlotConfigs(),
    val slotCredentials: List<SlotCredentials> = List(SLOT_COUNT) { SlotCredentials(slotIndex = it) },
    val runtimeStates: List<SlotRuntimeState> = List(SLOT_COUNT) { SlotRuntimeState(index = it) },
    val remainingAutoRefreshSeconds: Int? = null,
    val editingSlotIndex: Int? = null,
    val showGlobalSettingsSheet: Boolean = false,
    val showPaywallSheet: Boolean = false,
    val subscription: SubscriptionStatus = SubscriptionStatus(),
    val licenseFeedback: String? = null,
)

class FarmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FarmPreferencesRepository(application)
    private val credentialsStore = SlotCredentialsStore(application)
    private val subscriptionRepository = SubscriptionPreferencesRepository(application)
    private val deviceId = DeviceIdProvider.getDeviceCode(application)

    val webViewManager = SlotWebViewManager(application)

    private val focusedSlotIndex = MutableStateFlow<Int?>(null)
    private val editingSlotIndex = MutableStateFlow<Int?>(null)
    private val showGlobalSettingsSheet = MutableStateFlow(false)
    private val showPaywallSheet = MutableStateFlow(false)
    private val licenseFeedback = MutableStateFlow<String?>(null)
    private val remainingAutoRefreshSeconds = MutableStateFlow<Int?>(null)
    private val trialClockTickMs = MutableStateFlow(System.currentTimeMillis())

    private var currentAutoRefreshMinutes = 0
    private var secondsSinceLastRefresh = 0
    private var currentProActive = false
    @Volatile
    private var isAppInForeground = true

    // Cache verified ECDSA license so periodic clock ticks never re-run asymmetric crypto on Main Thread
    private var cachedLicenseRawKey: String? = null
    private var cachedLicenseMaxSeen: Long = -1L
    private var cachedLicenseResult: LicenseValidationResult? = null

    private data class LocalUiOverrides(
        val focusedSlot: Int?,
        val editingSlot: Int?,
        val showSettings: Boolean,
        val showPaywall: Boolean,
        val feedback: String?,
        val remainingSeconds: Int?,
    )

    private val localOverrides = combine(
        combine(focusedSlotIndex, editingSlotIndex, showGlobalSettingsSheet) { f, e, s ->
            Triple(f, e, s)
        },
        combine(showPaywallSheet, licenseFeedback, remainingAutoRefreshSeconds) { p, fb, r ->
            Triple(p, fb, r)
        },
    ) { left, right ->
        LocalUiOverrides(
            focusedSlot = left.first,
            editingSlot = left.second,
            showSettings = left.third,
            showPaywall = right.first,
            feedback = right.second,
            remainingSeconds = right.third,
        )
    }

    private val subscriptionStatusFlow = combine(
        subscriptionRepository.licenseDataFlow,
        trialClockTickMs,
    ) { licenseData, tickNowMs ->
        val savedKey = licenseData.activeLicenseKey
        val validated = if (!savedKey.isNullOrBlank()) {
            val cached = cachedLicenseResult
            val canReuseCache = cachedLicenseRawKey == savedKey &&
                cachedLicenseMaxSeen == licenseData.maxSeenClockMs &&
                cached is LicenseValidationResult.Valid &&
                tickNowMs < cached.expiresAtMs &&
                tickNowMs + 120_000L >= licenseData.maxSeenClockMs
            if (canReuseCache) {
                cached
            } else {
                LicenseVerifier.verify(
                    rawKey = savedKey,
                    currentDeviceId = deviceId,
                    nowMs = tickNowMs,
                    maxSeenClockMs = licenseData.maxSeenClockMs,
                ).also { result ->
                    cachedLicenseRawKey = savedKey
                    cachedLicenseMaxSeen = licenseData.maxSeenClockMs
                    cachedLicenseResult = result
                }
            }
        } else {
            cachedLicenseRawKey = null
            cachedLicenseResult = null
            null
        }

        if (validated is LicenseValidationResult.Valid) {
            SubscriptionStatus(
                isProActive = true,
                source = SubscriptionSource.LICENSE_KEY,
                planCode = validated.planCode,
                expiresAtMs = validated.expiresAtMs,
                holderName = validated.holderName,
                deviceId = deviceId,
                canStartDailyTrial = false,
            )
        } else {
            val trialEval = DailyTrialEvaluator.evaluate(
                lastTrialDayKey = licenseData.lastTrialDayKey,
                lastTrialStartMs = licenseData.lastTrialStartMs,
                lastTrialExpiresAtMs = licenseData.lastTrialExpiresAtMs,
                nowMs = tickNowMs,
                maxSeenClockMs = licenseData.maxSeenClockMs,
            )
            if (trialEval.isActive) {
                SubscriptionStatus(
                    isProActive = true,
                    source = SubscriptionSource.TRIAL,
                    planCode = "TESTE 30M",
                    expiresAtMs = trialEval.expiresAtMs,
                    holderName = "Teste Grátis Diário",
                    deviceId = deviceId,
                    canStartDailyTrial = false,
                )
            } else {
                SubscriptionStatus(
                    isProActive = false,
                    source = SubscriptionSource.NONE,
                    deviceId = deviceId,
                    canStartDailyTrial = trialEval.canStartToday,
                )
            }
        }
    }.distinctUntilChanged()

    val uiState: StateFlow<FarmUiState> = combine(
        repository.stateFlow,
        webViewManager.runtimeStates,
        localOverrides,
        subscriptionStatusFlow,
        credentialsStore.credentialsFlow,
    ) { persisted, runtime, overrides, subStatus, creds ->
        val mergedSettings = persisted.settings.copy(
            focusedSlotIndex = overrides.focusedSlot,
            multiProfileSupported = webViewManager.isMultiProfileSupported,
            audioMuteSupported = webViewManager.isMuteAudioSupported,
        )

        val slotsWithCredFlag = persisted.slots.map { slotConfig ->
            val hasCreds = creds.getOrNull(slotConfig.index)?.hasCredentials == true
            if (slotConfig.hasSavedCredentials == hasCreds) slotConfig
            else slotConfig.copy(hasSavedCredentials = hasCreds)
        }

        FarmUiState(
            settings = mergedSettings,
            slots = slotsWithCredFlag,
            slotCredentials = creds,
            runtimeStates = runtime,
            remainingAutoRefreshSeconds = if (subStatus.isProActive) overrides.remainingSeconds else null,
            editingSlotIndex = overrides.editingSlot,
            showGlobalSettingsSheet = overrides.showSettings,
            showPaywallSheet = overrides.showPaywall,
            subscription = subStatus,
            licenseFeedback = overrides.feedback,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = FarmUiState(
            settings = GlobalFarmSettings(
                multiProfileSupported = webViewManager.isMultiProfileSupported,
                audioMuteSupported = webViewManager.isMuteAudioSupported,
            ),
            subscription = SubscriptionStatus(deviceId = deviceId),
        ),
    )

    init {
        viewModelScope.launch {
            subscriptionRepository.recordObservedClock()
        }
        observeWebViewConfigSync()
        startTimersLoop()
        observeBackgroundServiceTicks()
    }

    /**
     * Syncs persisted slot configs, credentials, and PRO lock state to WebViews ONLY when
     * those inputs genuinely change — never on WebView progress ticks or timer ticks.
     */
    private fun observeWebViewConfigSync() {
        viewModelScope.launch {
            combine(
                repository.stateFlow,
                subscriptionStatusFlow.map { it.isProActive }.distinctUntilChanged(),
                credentialsStore.credentialsFlow,
            ) { persisted, isProActive, creds ->
                Triple(persisted, isProActive, creds)
            }.collect { (persisted, isProActive, creds) ->
                currentAutoRefreshMinutes = persisted.settings.autoRefreshMinutes
                currentProActive = isProActive
                webViewManager.keepAliveInBackground = persisted.settings.backgroundFarmEnabled
                webViewManager.updateCredentials(creds)

                persisted.slots.forEach { slotConfig ->
                    val hasCreds = creds.getOrNull(slotConfig.index)?.hasCredentials == true
                    val effective = if (slotConfig.hasSavedCredentials == hasCreds) slotConfig
                    else slotConfig.copy(hasSavedCredentials = hasCreds)
                    if (effective.index == 0 || isProActive) {
                        webViewManager.applyConfig(effective.index, effective)
                    } else {
                        webViewManager.pauseLockedSlot(effective.index)
                    }
                }
            }
        }
    }

    private fun observeBackgroundServiceTicks() {
        viewModelScope.launch {
            FarmKeepAliveService.backgroundTickCount.collectLatest { tick ->
                // Only run background keep-alive work when app is actually in background
                // to avoid duplicating foreground timer work.
                if (tick > 0L && !isAppInForeground && uiState.value.settings.backgroundFarmEnabled) {
                    webViewManager.resumeActiveWebViews()
                    webViewManager.checkAndAutoLoginAllSlots(currentProActive)
                    if (tick % 4L == 0L) {
                        webViewManager.flushAllCookies()
                    }
                    if (currentProActive && uiState.value.settings.antiAfkHeartbeat) {
                        webViewManager.sendAntiAfkPulse()
                    }
                }
            }
        }
    }

    private fun startTimersLoop() {
        viewModelScope.launch {
            var tickCount = 0
            while (true) {
                delay(1_000L)
                tickCount++

                // Record monotonic clock every 5 minutes to deter clock rollback
                if (tickCount % 300 == 0) {
                    subscriptionRepository.recordObservedClock()
                }

                // Only tick trial clock frequently while a 30m trial is active (or once per minute for midnight reset)
                val sub = uiState.value.subscription
                val activeTrialExp = sub.takeIf { it.isTrialActive }?.expiresAtMs
                val nowMs = System.currentTimeMillis()
                if ((activeTrialExp != null && (nowMs >= activeTrialExp || tickCount % 15 == 0)) ||
                    (activeTrialExp == null && tickCount % 60 == 0)
                ) {
                    trialClockTickMs.value = nowMs
                }

                // Every 15s check if any unlocked slot with saved credentials logged out in an SPA
                if (tickCount % 15 == 0) {
                    webViewManager.checkAndAutoLoginAllSlots(currentProActive)
                }

                // Every 60s persist per-profile cookies asynchronously on background IO thread
                if (tickCount % 60 == 0) {
                    webViewManager.flushAllCookies()
                }

                if (!currentProActive) {
                    secondsSinceLastRefresh = 0
                    if (remainingAutoRefreshSeconds.value != null) {
                        remainingAutoRefreshSeconds.value = null
                    }
                    continue
                }

                // Anti-AFK gentle heartbeat every 45s (PRO feature)
                if (tickCount % 45 == 0 && uiState.value.settings.antiAfkHeartbeat) {
                    webViewManager.sendAntiAfkPulse()
                }

                // Auto-refresh countdown (PRO feature)
                val intervalMinutes = currentAutoRefreshMinutes
                if (intervalMinutes > 0) {
                    val totalSeconds = intervalMinutes * 60
                    secondsSinceLastRefresh++
                    val remaining = (totalSeconds - secondsSinceLastRefresh).coerceAtLeast(0)
                    remainingAutoRefreshSeconds.value = remaining
                    if (remaining == 0) {
                        secondsSinceLastRefresh = 0
                        webViewManager.reloadAll()
                    }
                } else {
                    secondsSinceLastRefresh = 0
                    if (remainingAutoRefreshSeconds.value != null) {
                        remainingAutoRefreshSeconds.value = null
                    }
                }
            }
        }
    }

    fun onAppForegrounded() {
        isAppInForeground = true
        trialClockTickMs.value = System.currentTimeMillis()
        syncKeepAliveService()
        webViewManager.resumeActiveWebViews()
        webViewManager.checkAndAutoLoginAllSlots(currentProActive)
    }

    fun onAppResumed() {
        isAppInForeground = true
        syncKeepAliveService()
    }

    fun onAppBackgrounded() {
        isAppInForeground = false
        webViewManager.flushAllCookies()
        syncKeepAliveService()
    }

    fun onTrimMemory(level: Int) {
        webViewManager.trimMemory(level)
    }

    fun syncKeepAliveService() {
        val state = uiState.value
        val app = getApplication<Application>()
        if (state.settings.backgroundFarmEnabled) {
            val activeSlots = if (state.subscription.isProActive) SLOT_COUNT else 1
            FarmKeepAliveService.startOrUpdate(app, activeSlots)
        } else {
            FarmKeepAliveService.stop(app)
        }
    }

    fun openPaywall() {
        licenseFeedback.value = null
        showPaywallSheet.value = true
    }

    fun closePaywall() {
        showPaywallSheet.value = false
    }

    fun activateLicenseKey(rawKey: String) {
        viewModelScope.launch {
            val maxSeen = subscriptionRepository.getMaxSeenClockMs()
            when (
                val result = LicenseVerifier.verify(
                    rawKey = rawKey,
                    currentDeviceId = deviceId,
                    maxSeenClockMs = maxSeen,
                )
            ) {
                is LicenseValidationResult.Valid -> {
                    subscriptionRepository.saveLicenseKey(rawKey)
                    licenseFeedback.value = "Licença PRO ativada para ${result.holderName}!"
                }
                is LicenseValidationResult.Invalid -> {
                    licenseFeedback.value = result.reason
                }
            }
        }
    }

    fun removeLicenseKey() {
        viewModelScope.launch {
            subscriptionRepository.clearLicenseKey()
            licenseFeedback.value = "Chave removida deste aparelho."
        }
    }

    fun startDailyTrial() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val started = subscriptionRepository.activateDailyTrial(now)
            trialClockTickMs.value = System.currentTimeMillis()
            if (started) {
                licenseFeedback.value = "Teste grátis de 30 minutos ativado! As 4 telas estão liberadas."
                showPaywallSheet.value = false
                syncKeepAliveService()
            } else {
                licenseFeedback.value = "O teste grátis de 30 min de hoje já foi utilizado. Volta amanhã!"
            }
        }
    }

    fun setLayoutMode(mode: LayoutMode) {
        focusedSlotIndex.value = null
        viewModelScope.launch { repository.updateLayoutMode(mode) }
    }

    fun toggleFocusSlot(slotIndex: Int) {
        if (slotIndex > 0 && !currentProActive) {
            openPaywall()
            return
        }
        focusedSlotIndex.value = if (focusedSlotIndex.value == slotIndex) null else slotIndex
    }

    fun exitFocusMode() {
        focusedSlotIndex.value = null
    }

    fun reloadSlot(slotIndex: Int) {
        if (slotIndex > 0 && !currentProActive) {
            openPaywall()
            return
        }
        webViewManager.reloadSlot(slotIndex)
    }

    fun reloadAllSlots() {
        secondsSinceLastRefresh = 0
        if (currentProActive) {
            webViewManager.reloadAll()
        } else {
            webViewManager.reloadSlot(0)
        }
    }

    fun goBackSlot(slotIndex: Int) {
        webViewManager.goBack(slotIndex)
    }

    fun goHomeSlot(slotIndex: Int) {
        val url = uiState.value.slots.getOrNull(slotIndex)?.url ?: return
        webViewManager.loadHomeUrl(slotIndex, url)
    }

    fun toggleMuteSlot(slotIndex: Int) {
        if (slotIndex > 0 && !currentProActive) {
            openPaywall()
            return
        }
        val current = uiState.value.slots.getOrNull(slotIndex) ?: return
        viewModelScope.launch {
            repository.updateSlot(current.copy(isMuted = !current.isMuted))
        }
    }

    fun togglePauseSlot(slotIndex: Int) {
        if (slotIndex > 0 && !currentProActive) {
            openPaywall()
            return
        }
        val current = uiState.value.slots.getOrNull(slotIndex) ?: return
        viewModelScope.launch {
            repository.updateSlot(current.copy(isPaused = !current.isPaused))
        }
    }

    fun toggleDesktopModeSlot(slotIndex: Int) {
        if (slotIndex > 0 && !currentProActive) {
            openPaywall()
            return
        }
        val current = uiState.value.slots.getOrNull(slotIndex) ?: return
        viewModelScope.launch {
            repository.updateSlot(current.copy(desktopMode = !current.desktopMode))
        }
    }

    fun saveSlotConfig(
        slotIndex: Int,
        label: String,
        url: String,
        zoomPercent: Int,
        desktopMode: Boolean,
        username: String,
        password: String,
        autoFillLogin: Boolean,
    ) {
        val current = uiState.value.slots.getOrNull(slotIndex) ?: return
        viewModelScope.launch {
            repository.updateSlot(
                current.copy(
                    label = label.ifBlank { "Conta ${slotIndex + 1}" },
                    url = FarmPreferencesRepository.normalizeUrl(url),
                    zoomPercent = zoomPercent.coerceIn(35, 130),
                    desktopMode = desktopMode,
                )
            )
            credentialsStore.saveCredentials(slotIndex, username, password, autoFillLogin)
            editingSlotIndex.value = null
        }
    }

    fun fillSlotLoginNow(
        slotIndex: Int,
        username: String? = null,
        password: String? = null,
        autoFillLogin: Boolean? = null,
    ) {
        if (slotIndex > 0 && !currentProActive) {
            openPaywall()
            return
        }
        viewModelScope.launch {
            if (username != null && password != null) {
                val auto = autoFillLogin ?: true
                credentialsStore.saveCredentials(slotIndex, username, password, auto)
                val updated = SlotCredentials(
                    slotIndex = slotIndex,
                    username = username.trim(),
                    password = password,
                    autoFillLogin = auto,
                )
                webViewManager.fillLoginNow(slotIndex, updated)
            } else {
                webViewManager.fillLoginNow(slotIndex)
            }
        }
    }

    fun clearSlotCredentials(slotIndex: Int) {
        viewModelScope.launch {
            credentialsStore.clearCredentials(slotIndex)
        }
    }

    fun clearSlotSession(slotIndex: Int) {
        val url = uiState.value.slots.getOrNull(slotIndex)?.url ?: return
        webViewManager.clearSlotSession(slotIndex, url)
    }

    fun openSlotEditor(slotIndex: Int) {
        if (slotIndex > 0 && !currentProActive) {
            openPaywall()
            return
        }
        editingSlotIndex.value = slotIndex
    }

    fun closeSlotEditor() {
        editingSlotIndex.value = null
    }

    fun openGlobalSettings() {
        showGlobalSettingsSheet.value = true
    }

    fun closeGlobalSettings() {
        showGlobalSettingsSheet.value = false
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { repository.updateKeepScreenOn(enabled) }
    }

    fun setBackgroundFarmEnabled(enabled: Boolean) {
        webViewManager.keepAliveInBackground = enabled
        viewModelScope.launch {
            repository.updateBackgroundFarmEnabled(enabled)
            val app = getApplication<Application>()
            if (enabled) {
                val activeSlots = if (currentProActive) SLOT_COUNT else 1
                FarmKeepAliveService.startOrUpdate(app, activeSlots)
            } else {
                FarmKeepAliveService.stop(app)
            }
        }
    }

    fun setAutoRefreshMinutes(minutes: Int) {
        if (!currentProActive && minutes > 0) {
            openPaywall()
            return
        }
        secondsSinceLastRefresh = 0
        viewModelScope.launch { repository.updateAutoRefreshMinutes(minutes) }
    }

    fun setAntiAfkHeartbeat(enabled: Boolean) {
        if (!currentProActive && enabled) {
            openPaywall()
            return
        }
        viewModelScope.launch { repository.updateAntiAfkHeartbeat(enabled) }
    }

    fun toggleHeaderCollapsed() {
        val next = !uiState.value.settings.isHeaderCollapsed
        viewModelScope.launch { repository.updateHeaderCollapsed(next) }
    }

    fun applyUrlToAllSlots(url: String) {
        viewModelScope.launch { repository.setAllSlotsUrl(url) }
    }

    fun applyZoomToAllSlots(zoomPercent: Int) {
        viewModelScope.launch { repository.setAllSlotsZoom(zoomPercent) }
    }

    override fun onCleared() {
        webViewManager.destroyAll()
        super.onCleared()
    }
}
