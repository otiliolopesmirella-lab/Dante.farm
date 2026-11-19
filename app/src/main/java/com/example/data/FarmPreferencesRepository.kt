package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AccountSlotConfig
import com.example.model.DEFAULT_GAME_URL
import com.example.model.GlobalFarmSettings
import com.example.model.LayoutMode
import com.example.model.SLOT_COUNT
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.farmDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "digiquad_farm_prefs"
)

data class PersistedFarmState(
    val settings: GlobalFarmSettings,
    val slots: List<AccountSlotConfig>,
)

class FarmPreferencesRepository(private val context: Context) {

    private object Keys {
        val LAYOUT_MODE = stringPreferencesKey("layout_mode")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val BACKGROUND_FARM_ENABLED = booleanPreferencesKey("background_farm_enabled")
        val AUTO_REFRESH_MINUTES = intPreferencesKey("auto_refresh_minutes")
        val ANTI_AFK_HEARTBEAT = booleanPreferencesKey("anti_afk_heartbeat")
        val HEADER_COLLAPSED = booleanPreferencesKey("header_collapsed")

        fun slotLabel(i: Int) = stringPreferencesKey("slot_${i}_label")
        fun slotUrl(i: Int) = stringPreferencesKey("slot_${i}_url")
        fun slotZoom(i: Int) = intPreferencesKey("slot_${i}_zoom")
        fun slotDesktop(i: Int) = booleanPreferencesKey("slot_${i}_desktop")
        fun slotMuted(i: Int) = booleanPreferencesKey("slot_${i}_muted")
        fun slotPaused(i: Int) = booleanPreferencesKey("slot_${i}_paused")
    }

    val stateFlow: Flow<PersistedFarmState> = context.farmDataStore.data.map { prefs ->
        val layoutMode = prefs[Keys.LAYOUT_MODE]?.let { raw ->
            runCatching { LayoutMode.valueOf(raw) }.getOrNull()
        } ?: LayoutMode.GRID_2X2

        val settings = GlobalFarmSettings(
            layoutMode = layoutMode,
            keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: true,
            backgroundFarmEnabled = prefs[Keys.BACKGROUND_FARM_ENABLED] ?: true,
            autoRefreshMinutes = prefs[Keys.AUTO_REFRESH_MINUTES] ?: 0,
            antiAfkHeartbeat = prefs[Keys.ANTI_AFK_HEARTBEAT] ?: true,
            isHeaderCollapsed = prefs[Keys.HEADER_COLLAPSED] ?: false,
        )

        val slots = List(SLOT_COUNT) { idx ->
            AccountSlotConfig(
                index = idx,
                label = prefs[Keys.slotLabel(idx)] ?: "Conta ${idx + 1}",
                url = prefs[Keys.slotUrl(idx)] ?: DEFAULT_GAME_URL,
                zoomPercent = (prefs[Keys.slotZoom(idx)] ?: 65).coerceIn(35, 130),
                desktopMode = prefs[Keys.slotDesktop(idx)] ?: false,
                isMuted = prefs[Keys.slotMuted(idx)] ?: (idx > 0),
                isPaused = prefs[Keys.slotPaused(idx)] ?: false,
            )
        }

        PersistedFarmState(settings = settings, slots = slots)
    }

    suspend fun updateLayoutMode(mode: LayoutMode) {
        context.farmDataStore.edit { it[Keys.LAYOUT_MODE] = mode.name }
    }

    suspend fun updateKeepScreenOn(enabled: Boolean) {
        context.farmDataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
    }

    suspend fun updateBackgroundFarmEnabled(enabled: Boolean) {
        context.farmDataStore.edit { it[Keys.BACKGROUND_FARM_ENABLED] = enabled }
    }

    suspend fun updateAutoRefreshMinutes(minutes: Int) {
        context.farmDataStore.edit { it[Keys.AUTO_REFRESH_MINUTES] = minutes.coerceAtLeast(0) }
    }

    suspend fun updateAntiAfkHeartbeat(enabled: Boolean) {
        context.farmDataStore.edit { it[Keys.ANTI_AFK_HEARTBEAT] = enabled }
    }

    suspend fun updateHeaderCollapsed(collapsed: Boolean) {
        context.farmDataStore.edit { it[Keys.HEADER_COLLAPSED] = collapsed }
    }

    suspend fun updateSlot(config: AccountSlotConfig) {
        context.farmDataStore.edit { prefs ->
            val i = config.index
            prefs[Keys.slotLabel(i)] = config.label.ifBlank { "Conta ${i + 1}" }
            prefs[Keys.slotUrl(i)] = normalizeUrl(config.url)
            prefs[Keys.slotZoom(i)] = config.zoomPercent.coerceIn(35, 130)
            prefs[Keys.slotDesktop(i)] = config.desktopMode
            prefs[Keys.slotMuted(i)] = config.isMuted
            prefs[Keys.slotPaused(i)] = config.isPaused
        }
    }

    suspend fun setAllSlotsUrl(url: String) {
        val cleanUrl = normalizeUrl(url)
        context.farmDataStore.edit { prefs ->
            for (i in 0 until SLOT_COUNT) {
                prefs[Keys.slotUrl(i)] = cleanUrl
            }
        }
    }

    suspend fun setAllSlotsZoom(zoomPercent: Int) {
        val clamped = zoomPercent.coerceIn(35, 130)
        context.farmDataStore.edit { prefs ->
            for (i in 0 until SLOT_COUNT) {
                prefs[Keys.slotZoom(i)] = clamped
            }
        }
    }

    companion object {
        fun normalizeUrl(raw: String): String {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return DEFAULT_GAME_URL
            return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                trimmed
            } else {
                "https://$trimmed"
            }
        }
    }
}
