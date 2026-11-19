package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FarmPreferencesRepository
import com.example.model.AccountSlotConfig
import com.example.model.GlobalFarmSettings
import com.example.model.LayoutMode
import com.example.model.SLOT_COUNT
import com.example.model.SlotRuntimeState
import com.example.model.defaultSlotConfigs
import com.example.webview.SlotWebViewManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FarmUiState(
    val settings: GlobalFarmSettings = GlobalFarmSettings(),
    val slots: List<AccountSlotConfig> = defaultSlotConfigs(),
    val runtimeStates: List<SlotRuntimeState> = List(SLOT_COUNT) { SlotRuntimeState(index = it) },
    val remainingAutoRefreshSeconds: Int? = null,
    val editingSlotIndex: Int? = null,
    val showGlobalSettingsSheet: Boolean = false,
)

class FarmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FarmPreferencesRepository(application)
    val webViewManager = SlotWebViewManager(application)

    private val focusedSlotIndex = MutableStateFlow<Int?>(null)
    private val editingSlotIndex = MutableStateFlow<Int?>(null)
    private val showGlobalSettingsSheet = MutableStateFlow(false)
    private val remainingAutoRefreshSeconds = MutableStateFlow<Int?>(null)

    private var currentAutoRefreshMinutes = 0
    private var secondsSinceLastRefresh = 0

    private data class LocalUiOverrides(
        val focusedSlot: Int?,
        val editingSlot: Int?,
        val showSettings: Boolean,
        val remainingSeconds: Int?,
    )

    private val localOverrides = combine(
        focusedSlotIndex,
        editingSlotIndex,
        showGlobalSettingsSheet,
        remainingAutoRefreshSeconds,
    ) { focused, editing, showSettings, remaining ->
        LocalUiOverrides(
            focusedSlot = focused,
            editingSlot = editing,
            showSettings = showSettings,
            remainingSeconds = remaining,
        )
    }

    val uiState: StateFlow<FarmUiState> = combine(
        repository.stateFlow,
        webViewManager.runtimeStates,
        localOverrides,
    ) { persisted, runtime, overrides ->
        val mergedSettings = persisted.settings.copy(
            focusedSlotIndex = overrides.focusedSlot,
            multiProfileSupported = webViewManager.isMultiProfileSupported,
            audioMuteSupported = webViewManager.isMuteAudioSupported,
        )

        currentAutoRefreshMinutes = mergedSettings.autoRefreshMinutes

        // Sync any config changes to live WebViews
        persisted.slots.forEach { slotConfig ->
            webViewManager.applyConfig(slotConfig.index, slotConfig)
        }

        FarmUiState(
            settings = mergedSettings,
            slots = persisted.slots,
            runtimeStates = runtime,
            remainingAutoRefreshSeconds = overrides.remainingSeconds,
            editingSlotIndex = overrides.editingSlot,
            showGlobalSettingsSheet = overrides.showSettings,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FarmUiState(
            settings = GlobalFarmSettings(
                multiProfileSupported = webViewManager.isMultiProfileSupported,
                audioMuteSupported = webViewManager.isMuteAudioSupported,
            )
        ),
    )

    init {
        startTimersLoop()
    }

    private fun startTimersLoop() {
        viewModelScope.launch {
            var tickCount = 0
            while (true) {
                delay(1_000L)
                tickCount++

                // Anti-AFK gentle heartbeat every 45s
                if (tickCount % 45 == 0 && uiState.value.settings.antiAfkHeartbeat) {
                    webViewManager.sendAntiAfkPulse()
                }

                // Auto-refresh countdown
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

    fun setLayoutMode(mode: LayoutMode) {
        focusedSlotIndex.value = null
        viewModelScope.launch { repository.updateLayoutMode(mode) }
    }

    fun toggleFocusSlot(slotIndex: Int) {
        focusedSlotIndex.value = if (focusedSlotIndex.value == slotIndex) null else slotIndex
    }

    fun exitFocusMode() {
        focusedSlotIndex.value = null
    }

    fun reloadSlot(slotIndex: Int) {
        webViewManager.reloadSlot(slotIndex)
    }

    fun reloadAllSlots() {
        secondsSinceLastRefresh = 0
        webViewManager.reloadAll()
    }

    fun goBackSlot(slotIndex: Int) {
        webViewManager.goBack(slotIndex)
    }

    fun goHomeSlot(slotIndex: Int) {
        val url = uiState.value.slots.getOrNull(slotIndex)?.url ?: return
        webViewManager.loadHomeUrl(slotIndex, url)
    }

    fun toggleMuteSlot(slotIndex: Int) {
        val current = uiState.value.slots.getOrNull(slotIndex) ?: return
        viewModelScope.launch {
            repository.updateSlot(current.copy(isMuted = !current.isMuted))
        }
    }

    fun togglePauseSlot(slotIndex: Int) {
        val current = uiState.value.slots.getOrNull(slotIndex) ?: return
        viewModelScope.launch {
            repository.updateSlot(current.copy(isPaused = !current.isPaused))
        }
    }

    fun toggleDesktopModeSlot(slotIndex: Int) {
        val current = uiState.value.slots.getOrNull(slotIndex) ?: return
        viewModelScope.launch {
            repository.updateSlot(current.copy(desktopMode = !current.desktopMode))
        }
    }

    fun updateSlotZoom(slotIndex: Int, zoomPercent: Int) {
        val current = uiState.value.slots.getOrNull(slotIndex) ?: return
        viewModelScope.launch {
            repository.updateSlot(current.copy(zoomPercent = zoomPercent.coerceIn(35, 130)))
        }
    }

    fun saveSlotConfig(slotIndex: Int, label: String, url: String, zoomPercent: Int, desktopMode: Boolean) {
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
            editingSlotIndex.value = null
        }
    }

    fun clearSlotSession(slotIndex: Int) {
        val url = uiState.value.slots.getOrNull(slotIndex)?.url ?: return
        webViewManager.clearSlotSession(slotIndex, url)
    }

    fun openSlotEditor(slotIndex: Int) {
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

    fun setAutoRefreshMinutes(minutes: Int) {
        secondsSinceLastRefresh = 0
        viewModelScope.launch { repository.updateAutoRefreshMinutes(minutes) }
    }

    fun setAntiAfkHeartbeat(enabled: Boolean) {
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
