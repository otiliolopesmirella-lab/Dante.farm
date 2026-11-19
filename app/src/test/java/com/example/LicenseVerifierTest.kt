package com.example

import android.util.Base64
import com.example.monetization.LicenseValidationResult
import com.example.monetization.LicenseVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LicenseVerifierTest {

    private fun generateTestKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec("secp256r1"))
        return generator.generateKeyPair()
    }

    private fun signLicense(
        keyPair: KeyPair,
        deviceId: String,
        expiresAtMs: Long,
        holderName: String = "Cliente Teste",
        planCode: String = "MENSAL",
    ): String {
        val json = """{"d":"$deviceId","p":"$planCode","exp":$expiresAtMs,"n":"$holderName"}"""
        val payloadB64 = Base64.encodeToString(
            json.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
        val signer = Signature.getInstance("SHA256withECDSA")
        signer.initSign(keyPair.private)
        signer.update(payloadB64.toByteArray(Charsets.UTF_8))
        val sigB64 = Base64.encodeToString(
            signer.sign(),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
        return "DF1.$payloadB64.$sigB64"
    }

    @Test
    fun `valid device bound license succeeds`() {
        val kp = generateTestKeyPair()
        val pubB64 = Base64.encodeToString(kp.public.encoded, Base64.NO_WRAP)
        val now = 1_700_000_000_000L
        val exp = now + 30L * 24L * 3600L * 1000L
        val key = signLicense(kp, deviceId = "DF-AB12CD34", expiresAtMs = exp, holderName = "Dante Player")

        val result = LicenseVerifier.verify(
            rawKey = key,
            currentDeviceId = "DF-AB12CD34",
            nowMs = now,
            maxSeenClockMs = now,
            publicKeyDerBase64 = pubB64,
        )

        assertTrue(result is LicenseValidationResult.Valid)
        val valid = result as LicenseValidationResult.Valid
        assertEquals("Dante Player", valid.holderName)
        assertEquals(exp, valid.expiresAtMs)
        assertEquals("DF-AB12CD34", valid.boundDeviceId)
    }

    @Test
    fun `wildcard device license works on any device`() {
        val kp = generateTestKeyPair()
        val pubB64 = Base64.encodeToString(kp.public.encoded, Base64.NO_WRAP)
        val now = 1_700_000_000_000L
        val exp = now + 86_400_000L
        val key = signLicense(kp, deviceId = "*", expiresAtMs = exp)

        val result = LicenseVerifier.verify(
            rawKey = key,
            currentDeviceId = "DF-99887766",
            nowMs = now,
            publicKeyDerBase64 = pubB64,
        )

        assertTrue(result is LicenseValidationResult.Valid)
    }

    @Test
    fun `license bound to another device is rejected`() {
        val kp = generateTestKeyPair()
        val pubB64 = Base64.encodeToString(kp.public.encoded, Base64.NO_WRAP)
        val now = 1_700_000_000_000L
        val exp = now + 86_400_000L
        val key = signLicense(kp, deviceId = "DF-11112222", expiresAtMs = exp)

        val result = LicenseVerifier.verify(
            rawKey = key,
            currentDeviceId = "DF-33334444",
            nowMs = now,
            publicKeyDerBase64 = pubB64,
        )

        assertTrue(result is LicenseValidationResult.Invalid)
    }

    @Test
    fun `expired license is rejected`() {
        val kp = generateTestKeyPair()
        val pubB64 = Base64.encodeToString(kp.public.encoded, Base64.NO_WRAP)
        val now = 1_700_000_000_000L
        val exp = now - 1_000L
        val key = signLicense(kp, deviceId = "DF-AB12CD34", expiresAtMs = exp)

        val result = LicenseVerifier.verify(
            rawKey = key,
            currentDeviceId = "DF-AB12CD34",
            nowMs = now,
            publicKeyDerBase64 = pubB64,
        )

        assertTrue(result is LicenseValidationResult.Invalid)
    }

    @Test
    fun `clock rollback is detected via maxSeenClockMs`() {
        val kp = generateTestKeyPair()
        val pubB64 = Base64.encodeToString(kp.public.encoded, Base64.NO_WRAP)
        val exp = 1_700_500_000_000L
        val rolledBackNow = 1_700_000_000_000L
        val previouslyObservedClock = 1_700_600_000_000L
        val key = signLicense(kp, deviceId = "DF-AB12CD34", expiresAtMs = exp)

        val result = LicenseVerifier.verify(
            rawKey = key,
            currentDeviceId = "DF-AB12CD34",
            nowMs = rolledBackNow,
            maxSeenClockMs = previouslyObservedClock,
            publicKeyDerBase64 = pubB64,
        )

        assertTrue(result is LicenseValidationResult.Invalid)
    }

    @Test
    fun `tampered payload fails signature verification`() {
        val kp = generateTestKeyPair()
        val pubB64 = Base64.encodeToString(kp.public.encoded, Base64.NO_WRAP)
        val now = 1_700_000_000_000L
        val exp = now + 86_400_000L
        val key = signLicense(kp, deviceId = "DF-AB12CD34", expiresAtMs = exp)
        val parts = key.split(".")
        val tamperedJson = """{"d":"*","p":"MENSAL","exp":${exp + 999999999L},"n":"Hacker"}"""
        val tamperedPayload = Base64.encodeToString(
            tamperedJson.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
        val forged = "DF1.$tamperedPayload.${parts[2]}"

        val result = LicenseVerifier.verify(
            rawKey = forged,
            currentDeviceId = "DF-AB12CD34",
            nowMs = now,
            publicKeyDerBase64 = pubB64,
        )

        assertTrue(result is LicenseValidationResult.Invalid)
    }

    @Test
    fun `default subscription plans cover daily weekly and monthly pricing`() {
        val offers = com.example.monetization.SubscriptionStatus.defaultPlanOffers()
        assertEquals(3, offers.size)

        assertEquals(com.example.monetization.SubscriptionStatus.PRODUCT_DAILY, offers[0].productId)
        assertEquals("R$ 2,00", offers[0].fallbackPriceLabel)
        assertEquals(1, offers[0].durationDays)

        assertEquals(com.example.monetization.SubscriptionStatus.PRODUCT_WEEKLY, offers[1].productId)
        assertEquals("R$ 15,00", offers[1].fallbackPriceLabel)
        assertEquals(7, offers[1].durationDays)

        assertEquals(com.example.monetization.SubscriptionStatus.PRODUCT_MONTHLY, offers[2].productId)
        assertEquals("R$ 30,00", offers[2].fallbackPriceLabel)
        assertEquals(30, offers[2].durationDays)
    }

    @Test
    fun `remaining time label formats hours for daily plan and days for weekly or monthly`() {
        val now = 1_700_000_000_000L
        val dailyStatus = com.example.monetization.SubscriptionStatus(
            isProActive = true,
            expiresAtMs = now + 12L * 3_600_000L,
        )
        assertEquals("12h", dailyStatus.shortRemainingBadge(now))
        assertEquals("12h 0min restantes", dailyStatus.remainingTimeLabel(now))

        val weeklyStatus = com.example.monetization.SubscriptionStatus(
            isProActive = true,
            expiresAtMs = now + 7L * 86_400_000L,
        )
        assertEquals("7d", weeklyStatus.shortRemainingBadge(now))
        assertEquals("7 dias restantes", weeklyStatus.remainingTimeLabel(now))
    }

    @Test
    fun `background farm mode is enabled by default`() {
        val defaults = com.example.model.GlobalFarmSettings()
        assertTrue(defaults.backgroundFarmEnabled)
    }

    @Test
    fun `whatsApp pix url targets 5551997904558 and preselects plan price and device id`() {
        val offers = com.example.monetization.SubscriptionStatus.defaultPlanOffers()
        val dailyUrl = com.example.monetization.SubscriptionStatus.buildWhatsAppPixUrl(offers[0], "DF-12345678")
        val weeklyMsg = com.example.monetization.SubscriptionStatus.buildWhatsAppPixMessage(offers[1], "DF-12345678")
        val monthlyMsg = com.example.monetization.SubscriptionStatus.buildWhatsAppPixMessage(offers[2], "DF-12345678")

        assertTrue(dailyUrl.startsWith("https://wa.me/5551997904558?text="))
        assertTrue(dailyUrl.contains("2%2C00"))
        assertTrue(dailyUrl.contains("DF-12345678"))
        assertTrue(weeklyMsg.contains("R$ 15,00"))
        assertTrue(monthlyMsg.contains("R$ 30,00"))
    }

    @Test
    fun `daily 30 minute trial activates once per day, expires after 30 min, and resets next day`() {
        val start = 1_763_550_000_000L // fixed timestamp
        val dayKey = com.example.monetization.DailyTrialEvaluator.dayKeyFor(start)
        val exp = start + com.example.monetization.DailyTrialEvaluator.DAILY_TRIAL_DURATION_MS

        // Fresh device can start trial
        val initial = com.example.monetization.DailyTrialEvaluator.evaluate(
            lastTrialDayKey = null,
            lastTrialStartMs = 0L,
            lastTrialExpiresAtMs = 0L,
            nowMs = start,
            maxSeenClockMs = start,
        )
        assertTrue(!initial.isActive)
        assertTrue(initial.canStartToday)

        // 15 minutes later: still active, cannot start another
        val mid = start + 15L * 60_000L
        val activeEval = com.example.monetization.DailyTrialEvaluator.evaluate(
            lastTrialDayKey = dayKey,
            lastTrialStartMs = start,
            lastTrialExpiresAtMs = exp,
            nowMs = mid,
            maxSeenClockMs = mid,
        )
        assertTrue(activeEval.isActive)
        assertTrue(!activeEval.canStartToday)
        val trialStatus = com.example.monetization.SubscriptionStatus(
            isProActive = true,
            source = com.example.monetization.SubscriptionSource.TRIAL,
            expiresAtMs = activeEval.expiresAtMs,
        )
        assertEquals("15m", trialStatus.shortRemainingBadge(mid))
        assertEquals("15min restantes", trialStatus.remainingTimeLabel(mid))

        // 31 minutes later (same day): expired and blocked for the rest of today
        val afterExp = start + 31L * 60_000L
        val expiredEval = com.example.monetization.DailyTrialEvaluator.evaluate(
            lastTrialDayKey = dayKey,
            lastTrialStartMs = start,
            lastTrialExpiresAtMs = exp,
            nowMs = afterExp,
            maxSeenClockMs = afterExp,
        )
        assertTrue(!expiredEval.isActive)
        assertTrue(!expiredEval.canStartToday)

        // Clock rollback attempt during active window is blocked
        val rolledBack = com.example.monetization.DailyTrialEvaluator.evaluate(
            lastTrialDayKey = dayKey,
            lastTrialStartMs = start,
            lastTrialExpiresAtMs = exp,
            nowMs = start - 3_600_000L,
            maxSeenClockMs = mid,
        )
        assertTrue(rolledBack.clockRolledBack)
        assertTrue(!rolledBack.isActive)
        assertTrue(!rolledBack.canStartToday)

        // Next calendar day: can start 30-min trial again
        val nextDay = start + 24L * 3_600_000L
        val nextDayEval = com.example.monetization.DailyTrialEvaluator.evaluate(
            lastTrialDayKey = dayKey,
            lastTrialStartMs = start,
            lastTrialExpiresAtMs = exp,
            nowMs = nextDay,
            maxSeenClockMs = afterExp,
        )
        assertTrue(!nextDayEval.isActive)
        assertTrue(nextDayEval.canStartToday)
    }

    @Test
    fun `SubscriptionPreferencesRepository enforces one 30 min trial per day`() = kotlinx.coroutines.test.runTest {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val repo = com.example.monetization.SubscriptionPreferencesRepository(context)
        val t0 = 1_763_550_000_000L

        assertTrue(repo.activateDailyTrial(t0))
        // Cannot activate again same day
        assertTrue(!repo.activateDailyTrial(t0 + 40L * 60_000L))
        // Next calendar day unlocks again
        assertTrue(repo.activateDailyTrial(t0 + 25L * 3_600_000L))
    }
}
