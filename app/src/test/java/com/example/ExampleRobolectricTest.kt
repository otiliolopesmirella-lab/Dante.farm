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
    assertEquals("Dante Farm DigiIdle", appName)
  }

  @Test
  fun `default slot configs contain 4 independent DigiIdle slots`() {
    val slots = com.example.model.defaultSlotConfigs()
    assertEquals(4, slots.size)
    assertEquals("https://digiidle.com", slots.first().url)
    assertEquals("https://digiidle.com", com.example.data.FarmPreferencesRepository.normalizeUrl("digiidle.com"))
  }
}
