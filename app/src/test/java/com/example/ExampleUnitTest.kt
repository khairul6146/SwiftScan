package com.example

import com.example.util.QrGenerator
import com.example.util.ScanUtils
import com.google.mlkit.vision.barcode.common.Barcode
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testScanUtils_getFormatName() {
    assertEquals("QR Code", ScanUtils.getFormatName(Barcode.FORMAT_QR_CODE))
    assertEquals("EAN-13", ScanUtils.getFormatName(Barcode.FORMAT_EAN_13))
    assertEquals("Code 128", ScanUtils.getFormatName(Barcode.FORMAT_CODE_128))
  }

  @Test
  fun testScanUtils_getTypeName() {
    assertEquals("Website URL", ScanUtils.getTypeName(Barcode.TYPE_URL, "https://example.com"))
    assertEquals("Wi-Fi Network", ScanUtils.getTypeName(Barcode.TYPE_WIFI, "WIFI:S:Home;P:pass;;"))
    assertEquals("Phone Number", ScanUtils.getTypeName(Barcode.TYPE_PHONE, "+1234567890"))
    assertEquals("Plain Text", ScanUtils.getTypeName(Barcode.TYPE_TEXT, "Hello World"))
  }
}
