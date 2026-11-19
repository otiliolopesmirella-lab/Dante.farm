package com.example.monetization

import androidx.compose.runtime.Immutable
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SubscriptionSource {
    NONE,
    TRIAL,
    LICENSE_KEY,
}

data class DailyTrialEvaluation(
    val isActive: Boolean,
    val canStartToday: Boolean,
    val expiresAtMs: Long? = null,
    val clockRolledBack: Boolean = false,
)

object DailyTrialEvaluator {
    const val DAILY_TRIAL_DURATION_MS = 30L * 60_000L
    const val DAILY_TRIAL_MINUTES = 30
    private const val CLOCK_ROLLBACK_TOLERANCE_MS = 120_000L

    fun dayKeyFor(timeMs: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formatter.format(Date(timeMs))
    }

    fun evaluate(
        lastTrialDayKey: String?,
        lastTrialStartMs: Long,
        lastTrialExpiresAtMs: Long,
        nowMs: Long = System.currentTimeMillis(),
        maxSeenClockMs: Long = 0L,
    ): DailyTrialEvaluation {
        val clockRolledBack = maxSeenClockMs > 0L && nowMs + CLOCK_ROLLBACK_TOLERANCE_MS < maxSeenClockMs
        val effectiveNowMs = maxOf(nowMs, maxSeenClockMs)
        val todayKey = dayKeyFor(effectiveNowMs)

        val isWindowValid = lastTrialStartMs > 0L &&
            lastTrialExpiresAtMs > lastTrialStartMs &&
            (lastTrialExpiresAtMs - lastTrialStartMs) <= (DAILY_TRIAL_DURATION_MS + 1_000L)

        val isActive = !clockRolledBack &&
            isWindowValid &&
            effectiveNowMs < lastTrialExpiresAtMs

        val alreadyUsedToday = !lastTrialDayKey.isNullOrBlank() && lastTrialDayKey == todayKey
        val canStartToday = !isActive && !clockRolledBack && !alreadyUsedToday

        return DailyTrialEvaluation(
            isActive = isActive,
            canStartToday = canStartToday,
            expiresAtMs = if (isActive) lastTrialExpiresAtMs else null,
            clockRolledBack = clockRolledBack,
        )
    }
}

@Immutable
data class SubscriptionPlanOffer(
    val productId: String,
    val planCode: String,
    val title: String,
    val subtitle: String,
    val fallbackPriceLabel: String,
    val durationDays: Int,
    val badge: String? = null,
    val isHighlighted: Boolean = false,
) {
    val displayPrice: String
        get() = fallbackPriceLabel
}

@Immutable
data class SubscriptionStatus(
    val isProActive: Boolean = false,
    val source: SubscriptionSource = SubscriptionSource.NONE,
    val planCode: String? = null,
    val expiresAtMs: Long? = null,
    val holderName: String? = null,
    val deviceId: String = "",
    val canStartDailyTrial: Boolean = true,
    val offers: List<SubscriptionPlanOffer> = defaultPlanOffers(),
) {
    val isPaidPro: Boolean
        get() = isProActive && source == SubscriptionSource.LICENSE_KEY

    val isTrialActive: Boolean
        get() = isProActive && source == SubscriptionSource.TRIAL

    fun daysRemaining(nowMs: Long = System.currentTimeMillis()): Int? {
        val exp = expiresAtMs ?: return null
        val diff = exp - nowMs
        if (diff <= 0L) return 0
        return ((diff + MILLIS_PER_DAY - 1L) / MILLIS_PER_DAY).toInt()
    }

    fun shortRemainingBadge(nowMs: Long = System.currentTimeMillis()): String? {
        val exp = expiresAtMs ?: return null
        val diff = exp - nowMs
        if (diff <= 0L) return "0m"
        return when {
            diff < MILLIS_PER_HOUR -> {
                val mins = ((diff + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
                "${mins}m"
            }
            diff < MILLIS_PER_DAY -> {
                val hours = ((diff + MILLIS_PER_HOUR - 1L) / MILLIS_PER_HOUR).toInt().coerceAtLeast(1)
                "${hours}h"
            }
            else -> {
                val days = ((diff + MILLIS_PER_DAY - 1L) / MILLIS_PER_DAY).toInt()
                "${days}d"
            }
        }
    }

    fun remainingTimeLabel(nowMs: Long = System.currentTimeMillis()): String? {
        val exp = expiresAtMs ?: return null
        val diff = exp - nowMs
        if (diff <= 0L) return "Expirado"
        return if (diff < MILLIS_PER_DAY) {
            val totalMinutes = if (diff < MILLIS_PER_HOUR) {
                ((diff + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
            } else {
                (diff / 60_000L).toInt().coerceAtLeast(1)
            }
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            if (hours > 0) "${hours}h ${mins}min restantes" else "${mins}min restantes"
        } else {
            val days = ((diff + MILLIS_PER_DAY - 1L) / MILLIS_PER_DAY).toInt()
            if (days == 1) "1 dia restante" else "$days dias restantes"
        }
    }

    companion object {
        private const val MILLIS_PER_HOUR = 3_600_000L
        private const val MILLIS_PER_DAY = 86_400_000L

        const val WHATSAPP_PHONE_E164 = "5551997904558"
        const val WHATSAPP_PHONE_DISPLAY = "(51) 99790-4558"

        const val PRODUCT_DAILY = "dante_farm_diario"
        const val PRODUCT_WEEKLY = "dante_farm_semanal"
        const val PRODUCT_MONTHLY = "dante_farm_mensal"

        private val DEFAULT_PLAN_OFFERS: List<SubscriptionPlanOffer> = listOf(
            SubscriptionPlanOffer(
                productId = PRODUCT_DAILY,
                planCode = "DIARIO",
                title = "Plano Diário (24h)",
                subtitle = "1 dia • 4 Contas + 2º Plano + Auto-Login",
                fallbackPriceLabel = "R$ 2,00",
                durationDays = 1,
                badge = "24 HORAS",
            ),
            SubscriptionPlanOffer(
                productId = PRODUCT_WEEKLY,
                planCode = "SEMANAL",
                title = "Plano Semanal (7 dias)",
                subtitle = "7 dias • 4 Contas + 2º Plano + Auto-Login",
                fallbackPriceLabel = "R$ 15,00",
                durationDays = 7,
                badge = "7 DIAS",
            ),
            SubscriptionPlanOffer(
                productId = PRODUCT_MONTHLY,
                planCode = "MENSAL",
                title = "Plano Mensal (30 dias)",
                subtitle = "30 dias • Acesso total 4× + 2º Plano",
                fallbackPriceLabel = "R$ 30,00",
                durationDays = 30,
                badge = "MAIS POPULAR",
                isHighlighted = true,
            ),
        )

        fun defaultPlanOffers(): List<SubscriptionPlanOffer> = DEFAULT_PLAN_OFFERS

        fun buildWhatsAppPixMessage(offer: SubscriptionPlanOffer, deviceId: String): String {
            val idLine = if (deviceId.isNotBlank()) "\nMeu ID do Aparelho: $deviceId" else ""
            return "Olá! Quero assinar o Dante Farm Idle PRO - ${offer.title} no valor de ${offer.displayPrice} via PIX.$idLine\n\nPode me enviar a chave PIX para pagamento?"
        }

        fun buildWhatsAppPixUrl(offer: SubscriptionPlanOffer, deviceId: String): String {
            val msg = buildWhatsAppPixMessage(offer, deviceId)
            val encoded = URLEncoder.encode(msg, "UTF-8").replace("+", "%20")
            return "https://wa.me/$WHATSAPP_PHONE_E164?text=$encoded"
        }
    }
}
