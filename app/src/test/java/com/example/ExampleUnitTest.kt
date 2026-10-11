package com.example

import com.example.data.ScanResult
import com.example.util.QrGenerator
import com.example.util.ScanUtils
import com.google.mlkit.vision.barcode.common.Barcode
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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

    @Test
    fun testScanUtils_parseWifiString() {
        val wifi = ScanUtils.parseWifiString("WIFI:S:OfficeNetwork;T:WPA;P:SuperSecret123;;")
        assertEquals("OfficeNetwork", wifi.ssid)
        assertEquals("SuperSecret123", wifi.password)
        assertEquals("WPA", wifi.type)
    }

    @Test
    fun testScanUtils_exportJsonAndCsv() {
        val scans = listOf(
            ScanResult(id = 1, content = "https://example.com", format = Barcode.FORMAT_QR_CODE, type = Barcode.TYPE_URL, isFavorite = true),
            ScanResult(id = 2, content = "123456789", format = Barcode.FORMAT_CODE_128, type = Barcode.TYPE_TEXT, isFavorite = false, note = "Warehouse box")
        )

        val csv = ScanUtils.exportHistoryAsCsv(scans)
        assertTrue(csv.contains("https://example.com"))
        assertTrue(csv.contains("Warehouse box"))

        val json = ScanUtils.exportHistoryAsJson(scans)
        assertTrue(json.contains("\"id\": 1"))
        assertTrue(json.contains("\"isFavorite\": true"))
        assertTrue(json.contains("\"note\": \"Warehouse box\""))
    }

    @Test
    fun testQrGenerator_createsBitmap() {
        val bitmap = QrGenerator.generateQrCode("Hello SwiftScan", 256)
        assertNotNull(bitmap)
        assertEquals(256, bitmap?.width)
        assertEquals(256, bitmap?.height)
    }
}
