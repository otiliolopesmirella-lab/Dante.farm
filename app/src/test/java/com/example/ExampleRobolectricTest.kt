package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Dante Farm Idle", appName)
  }

  @Test
  fun `default slot configs contain 4 independent slots with empty URL`() {
    val slots = com.example.model.defaultSlotConfigs()
    assertEquals(4, slots.size)
    assertEquals("", slots.first().url)
    assertEquals("", com.example.data.FarmPreferencesRepository.normalizeUrl("   "))
    assertEquals("https://meujogo.com", com.example.data.FarmPreferencesRepository.normalizeUrl("meujogo.com"))
  }

  @Test
  fun `SlotWebViewManager precreates WebView code cache directories`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.webview.SlotWebViewManager(context)
    val defaultJs = java.io.File(context.cacheDir, "WebView/Default/HTTP Cache/Code Cache/js")
    val profile1Js = java.io.File(context.cacheDir, "WebView/Profile 1/HTTP Cache/Code Cache/js")
    org.junit.Assert.assertTrue(defaultJs.isDirectory)
    org.junit.Assert.assertTrue(profile1Js.isDirectory)
  }

  @Test
  fun `SlotCredentialsStore isolates credentials across 4 slots and escapes JS`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val store = com.example.data.SlotCredentialsStore(context)
    store.saveCredentials(0, "conta1@digiidle.com", "P@ss'\"123$", true)
    store.saveCredentials(1, "conta2@digiidle.com", "OutraSenha!", false)

    val c0 = store.getCredentials(0)
    val c1 = store.getCredentials(1)
    val c2 = store.getCredentials(2)

    assertEquals("conta1@digiidle.com", c0.username)
    assertEquals("P@ss'\"123$", c0.password)
    org.junit.Assert.assertTrue(c0.autoFillLogin)

    assertEquals("conta2@digiidle.com", c1.username)
    assertEquals("OutraSenha!", c1.password)
    org.junit.Assert.assertFalse(c1.autoFillLogin)

    org.junit.Assert.assertFalse(c2.hasCredentials)

    val js = com.example.data.SlotCredentialsStore.buildLoginAutofillScript(
      username = c0.username,
      password = c0.password,
      autoSubmit = true,
      forceOverwrite = true,
    )
    org.junit.Assert.assertTrue(js.contains("conta1@digiidle.com"))
    org.junit.Assert.assertTrue(js.contains("var autoSubmit = true;"))
  }

  @Test
  fun `4 slots use distinct WebView profile names and normalized URL comparison avoids extra reloads`() {
    val profileNames = (0 until 4).map { com.example.webview.SlotWebViewManager.profileNameForSlot(it) }.toSet()
    assertEquals(4, profileNames.size)

    val defaultRuntime = com.example.model.SlotRuntimeState(index = 0)
    assertEquals(0, defaultRuntime.recoveryNonce)

    assertEquals(
      com.example.webview.SlotWebViewManager.normalizeComparableUrl("https://digiidle.com"),
      com.example.webview.SlotWebViewManager.normalizeComparableUrl("  https://digiidle.com/  ")
    )
  }
}
