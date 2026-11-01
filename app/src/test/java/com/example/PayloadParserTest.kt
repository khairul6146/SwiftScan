package com.example

import com.example.util.PayloadCategory
import com.example.util.PayloadParser
import com.google.mlkit.vision.barcode.common.Barcode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PayloadParserTest {

    @Test
    fun testParseWifiPayload() {
        val wifiString = "WIFI:S:MyOfficeNet;T:WPA;P:SuperSecretPass;;"
        val result = PayloadParser.parse(wifiString, Barcode.FORMAT_QR_CODE, Barcode.TYPE_WIFI)

        assertEquals(PayloadCategory.WIFI, result.category)
        assertEquals("MyOfficeNet", result.title)
        assertEquals("SuperSecretPass", result.wifiPassword)
        assertEquals("Connect Now", result.primaryLabel)
        assertEquals("Copy Key", result.secondaryLabel)
        assertEquals("WI-FI", result.badge)
    }

    @Test
    fun testParseUrlPayloadWithTrackers() {
        val url = "https://example.com/product?id=42&utm_source=newsletter&fbclid=xyz"
        val result = PayloadParser.parse(url, Barcode.FORMAT_QR_CODE, Barcode.TYPE_URL)

        assertEquals(PayloadCategory.URL, result.category)
        assertEquals("example.com", result.title)
        assertTrue(result.subtitle.contains("Trackers stripped"))
        assertEquals("https://example.com/product?id=42", result.cleanedContent)
        assertEquals("Open Link", result.primaryLabel)
        assertEquals("Copy URL", result.secondaryLabel)
    }

    @Test
    fun testParseEan13ProductPayload() {
        val ean = "9780132350884" // Clean Code ISBN
        val result = PayloadParser.parse(ean, Barcode.FORMAT_EAN_13, Barcode.TYPE_ISBN)

        assertEquals(PayloadCategory.PRODUCT, result.category)
        assertEquals("9780132350884", result.title)
        assertEquals("Search Web", result.primaryLabel)
        assertEquals("Copy Code", result.secondaryLabel)
        assertEquals("PRODUCT", result.badge)
    }

    @Test
    fun testParseVCardPayload() {
        val vcard = """
            BEGIN:VCARD
            VERSION:3.0
            N:Doe;John;;;
            FN:John Doe
            TEL;TYPE=CELL:+1234567890
            END:VCARD
        """.trimIndent()

        val result = PayloadParser.parse(vcard, Barcode.FORMAT_QR_CODE, Barcode.TYPE_CONTACT_INFO)

        assertEquals(PayloadCategory.CONTACT, result.category)
        assertEquals("John Doe", result.title)
        assertEquals("+1234567890", result.contactPhone)
        assertEquals("Save Contact", result.primaryLabel)
        assertEquals("Call Phone", result.secondaryLabel)
    }

    @Test
    fun testParsePlainTextPayload() {
        val text = "PART-SN-8921-X"
        val result = PayloadParser.parse(text, Barcode.FORMAT_CODE_128, Barcode.TYPE_TEXT)

        assertEquals(PayloadCategory.TEXT, result.category)
        assertEquals("PART-SN-8921-X", result.title)
        assertEquals("Copy Text", result.primaryLabel)
        assertEquals("Search Web", result.secondaryLabel)
    }
}
