package com.example

import com.example.util.SecurityUtils
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecurityUtilsTest {

    @Test
    fun testExtractRootDomain() {
        assertEquals("google.com", SecurityUtils.extractRootDomain("www.google.com"))
        assertEquals("google.com", SecurityUtils.extractRootDomain("google.com"))
        assertEquals("paypal.com", SecurityUtils.extractRootDomain("sub.domain.paypal.com"))
        assertEquals("bbc.co.uk", SecurityUtils.extractRootDomain("news.bbc.co.uk"))
        assertEquals("192.168.1.1", SecurityUtils.extractRootDomain("192.168.1.1"))
    }

    @Test
    fun testAnalyzeUrl_safeHttps() {
        val analysis = SecurityUtils.analyzeUrl("https://github.com/khairul6146/SwiftScan")
        assertEquals(SecurityUtils.RiskLevel.SAFE, analysis.riskLevel)
        assertTrue(analysis.isHttps)
        assertEquals("github.com", analysis.rootDomain)
        assertFalse(analysis.isShortener)
    }

    @Test
    fun testAnalyzeUrl_ipAddress() {
        val analysis = SecurityUtils.analyzeUrl("http://192.168.1.1/admin")
        assertEquals(SecurityUtils.RiskLevel.CAUTION, analysis.riskLevel)
        assertTrue(analysis.riskReason.contains("IP address"))
    }

    @Test
    fun testAnalyzeUrl_shortener() {
        val analysis = SecurityUtils.analyzeUrl("https://bit.ly/my-link")
        assertEquals(SecurityUtils.RiskLevel.CAUTION, analysis.riskLevel)
        assertTrue(analysis.isShortener)
    }

    @Test
    fun testAnalyzeUrl_dangerousExtension() {
        val analysis = SecurityUtils.analyzeUrl("https://malicious-site.com/downloads/free_game.apk")
        assertEquals(SecurityUtils.RiskLevel.DANGEROUS, analysis.riskLevel)
        assertTrue(analysis.riskReason.contains("executable"))
    }

    @Test
    fun testStripTrackingParameters() {
        val dirtyUrl = "https://example.com/product?id=123&utm_source=newsletter&utm_medium=email&fbclid=abcdef"
        val cleanUrl = SecurityUtils.stripTrackingParameters(dirtyUrl)
        assertTrue(cleanUrl.contains("id=123"))
        assertFalse(cleanUrl.contains("utm_source"))
        assertFalse(cleanUrl.contains("utm_medium"))
        assertFalse(cleanUrl.contains("fbclid"))
    }
}
