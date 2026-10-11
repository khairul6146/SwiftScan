package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.QrGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    assertEquals("SwiftScan", appName)
  }

  @Test
  fun `generate qr code bitmap`() {
    val bitmap = QrGenerator.generateQrCode("https://example.com", 256)
    assertNotNull(bitmap)
    assertEquals(256, bitmap!!.width)
    assertEquals(256, bitmap.height)
  }
}
