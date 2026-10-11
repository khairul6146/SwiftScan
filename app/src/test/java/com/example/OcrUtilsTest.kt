package com.example

import com.example.util.OcrUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class OcrUtilsTest {

    @Test
    fun testParseOcrText_labeledSerial() {
        val result = OcrUtils.parseOcrText("MANUFACTURER INC\nSN: 8492-9182-AB\nDATE: 2026-10")
        assertNotNull(result.primarySerial)
        assertEquals("8492-9182-AB", result.primarySerial)
        assertEquals("8492-9182-AB", result.displaySummary)
    }

    @Test
    fun testParseOcrText_lotNumber() {
        val result = OcrUtils.parseOcrText("PRODUCT PHARMA\nLOT 20261011\nEXP 2028-12")
        assertEquals("20261011", result.primarySerial)
    }

    @Test
    fun testParseOcrText_structuredCode() {
        val result = OcrUtils.parseOcrText("COMPONENT ITEM\nPART-84921-X\nWAREHOUSE 4")
        assertEquals("PART-84921-X", result.primarySerial)
    }

    @Test
    fun testParseOcrText_emptyOrBlank() {
        val result = OcrUtils.parseOcrText("   ")
        assertNull(result.primarySerial)
        assertEquals("No Text Detected", result.displaySummary)
    }
}
