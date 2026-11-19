package com.example.model

import androidx.compose.runtime.Immutable

const val SLOT_COUNT = 4
const val DEFAULT_GAME_URL = ""

enum class LayoutMode(val labelPt: String) {
    GRID_2X2("Grade 2×2"),
    FOCUS_1_PLUS_3("Foco 1+3"),
    STACK_4("Lista 4×"),
}

@Immutable
data class AccountSlotConfig(
    val index: Int,
    val label: String,
    val url: String = DEFAULT_GAME_URL,
    val zoomPercent: Int = 65,
    val desktopMode: Boolean = false,
    val isMuted: Boolean = index > 0,
    val isPaused: Boolean = false,
    val hasSavedCredentials: Boolean = false,
)

@Immutable
data class SlotCredentials(
    val slotIndex: Int,
    val username: String = "",
    val password: String = "",
    val autoFillLogin: Boolean = true,
) {
    val hasCredentials: Boolean
        get() = username.isNotBlank() && password.isNotBlank()
}

@Immutable
data class SlotRuntimeState(
    val index: Int,
    val currentUrl: String = DEFAULT_GAME_URL,
    val pageTitle: String = "",
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val hasError: Boolean = false,
    val errorDescription: String? = null,
    val isIsolatedProfileActive: Boolean = false,
    val lastReloadTimestampMs: Long = 0L,
    val recoveryNonce: Int = 0,
)

@Immutable
data class GlobalFarmSettings(
    val layoutMode: LayoutMode = LayoutMode.GRID_2X2,
    val focusedSlotIndex: Int? = null,
    val keepScreenOn: Boolean = true,
    val backgroundFarmEnabled: Boolean = true,
    val autoRefreshMinutes: Int = 0,
    val antiAfkHeartbeat: Boolean = true,
    val isHeaderCollapsed: Boolean = false,
    val multiProfileSupported: Boolean = true,
    val audioMuteSupported: Boolean = true,
)

fun defaultSlotConfigs(): List<AccountSlotConfig> = List(SLOT_COUNT) { idx ->
    AccountSlotConfig(
        index = idx,
        label = "Conta ${idx + 1}",
        url = DEFAULT_GAME_URL,
        zoomPercent = 65,
        desktopMode = false,
        isMuted = idx > 0,
        isPaused = false,
    )
}
