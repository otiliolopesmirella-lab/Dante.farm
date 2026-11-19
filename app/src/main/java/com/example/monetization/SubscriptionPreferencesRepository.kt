package com.example.monetization

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.subscriptionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "dante_subscription_prefs"
)

data class PersistedLicenseData(
    val activeLicenseKey: String? = null,
    val maxSeenClockMs: Long = 0L,
    val salesSupportUrl: String = "",
    val lastTrialDayKey: String? = null,
    val lastTrialStartMs: Long = 0L,
    val lastTrialExpiresAtMs: Long = 0L,
)

class SubscriptionPreferencesRepository(private val context: Context) {

    private object Keys {
        val ACTIVE_LICENSE_KEY = stringPreferencesKey("active_license_key")
        val MAX_SEEN_CLOCK_MS = longPreferencesKey("max_seen_clock_ms")
        val SALES_SUPPORT_URL = stringPreferencesKey("sales_support_url")
        val LAST_TRIAL_DAY_KEY = stringPreferencesKey("last_trial_day_key")
        val LAST_TRIAL_START_MS = longPreferencesKey("last_trial_start_ms")
        val LAST_TRIAL_EXPIRES_MS = longPreferencesKey("last_trial_expires_ms")
    }

    val licenseDataFlow: Flow<PersistedLicenseData> = context.subscriptionDataStore.data.map { prefs ->
        PersistedLicenseData(
            activeLicenseKey = prefs[Keys.ACTIVE_LICENSE_KEY],
            maxSeenClockMs = prefs[Keys.MAX_SEEN_CLOCK_MS] ?: 0L,
            salesSupportUrl = prefs[Keys.SALES_SUPPORT_URL].orEmpty(),
            lastTrialDayKey = prefs[Keys.LAST_TRIAL_DAY_KEY],
            lastTrialStartMs = prefs[Keys.LAST_TRIAL_START_MS] ?: 0L,
            lastTrialExpiresAtMs = prefs[Keys.LAST_TRIAL_EXPIRES_MS] ?: 0L,
        )
    }

    suspend fun saveLicenseKey(key: String, nowMs: Long = System.currentTimeMillis()) {
        context.subscriptionDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_LICENSE_KEY] = key.trim()
            val prevMax = prefs[Keys.MAX_SEEN_CLOCK_MS] ?: 0L
            if (nowMs > prevMax) {
                prefs[Keys.MAX_SEEN_CLOCK_MS] = nowMs
            }
        }
    }

    suspend fun clearLicenseKey() {
        context.subscriptionDataStore.edit { prefs ->
            prefs.remove(Keys.ACTIVE_LICENSE_KEY)
        }
    }

    suspend fun activateDailyTrial(nowMs: Long = System.currentTimeMillis()): Boolean {
        var activated = false
        context.subscriptionDataStore.edit { prefs ->
            val prevMax = prefs[Keys.MAX_SEEN_CLOCK_MS] ?: 0L
            val eval = DailyTrialEvaluator.evaluate(
                lastTrialDayKey = prefs[Keys.LAST_TRIAL_DAY_KEY],
                lastTrialStartMs = prefs[Keys.LAST_TRIAL_START_MS] ?: 0L,
                lastTrialExpiresAtMs = prefs[Keys.LAST_TRIAL_EXPIRES_MS] ?: 0L,
                nowMs = nowMs,
                maxSeenClockMs = prevMax,
            )
            if (eval.canStartToday) {
                prefs[Keys.LAST_TRIAL_DAY_KEY] = DailyTrialEvaluator.dayKeyFor(nowMs)
                prefs[Keys.LAST_TRIAL_START_MS] = nowMs
                prefs[Keys.LAST_TRIAL_EXPIRES_MS] = nowMs + DailyTrialEvaluator.DAILY_TRIAL_DURATION_MS
                if (nowMs > prevMax) {
                    prefs[Keys.MAX_SEEN_CLOCK_MS] = nowMs
                }
                activated = true
            }
        }
        return activated
    }

    suspend fun recordObservedClock(nowMs: Long = System.currentTimeMillis()) {
        context.subscriptionDataStore.edit { prefs ->
            val prevMax = prefs[Keys.MAX_SEEN_CLOCK_MS] ?: 0L
            if (nowMs > prevMax) {
                prefs[Keys.MAX_SEEN_CLOCK_MS] = nowMs
            }
        }
    }

    suspend fun getMaxSeenClockMs(): Long {
        return licenseDataFlow.first().maxSeenClockMs
    }
}
