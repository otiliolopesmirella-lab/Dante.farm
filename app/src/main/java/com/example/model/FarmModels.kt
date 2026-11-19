package com.example.model

const val SLOT_COUNT = 4
const val DIGIIDLE_PRIMARY_URL = "https://digiidle.com"
const val DIGIIDLE_BR_URL = "https://digiidle.com.br"

enum class LayoutMode(val labelPt: String) {
    GRID_2X2("Grade 2×2"),
    FOCUS_1_PLUS_3("Foco 1+3"),
    STACK_4("Lista 4×"),
}

data class AccountSlotConfig(
    val index: Int,
    val label: String,
    val url: String = DIGIIDLE_PRIMARY_URL,
    val zoomPercent: Int = 65,
    val desktopMode: Boolean = false,
    val isMuted: Boolean = index > 0,
    val isPaused: Boolean = false,
)

data class SlotRuntimeState(
    val index: Int,
    val currentUrl: String = DIGIIDLE_PRIMARY_URL,
    val pageTitle: String = "",
    val progress: Int = 0,
    val isLoading: Boolean = true,
    val canGoBack: Boolean = false,
    val hasError: Boolean = false,
    val errorDescription: String? = null,
    val isIsolatedProfileActive: Boolean = false,
    val lastReloadTimestampMs: Long = 0L,
)

data class GlobalFarmSettings(
    val layoutMode: LayoutMode = LayoutMode.GRID_2X2,
    val focusedSlotIndex: Int? = null,
    val keepScreenOn: Boolean = true,
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
        url = DIGIIDLE_PRIMARY_URL,
        zoomPercent = 65,
        desktopMode = false,
        isMuted = idx > 0,
        isPaused = false,
    )
}
