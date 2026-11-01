package com.example.util

import com.example.data.ScanResult
import com.google.mlkit.vision.barcode.common.Barcode
import java.util.Locale

enum class PayloadCategory {
    URL,
    WIFI,
    PRODUCT,
    CONTACT,
    TEXT
}

data class ParsedPayload(
    val category: PayloadCategory,
    val title: String,
    val subtitle: String,
    val badge: String,
    val rawContent: String,
    val cleanedContent: String,
    val primaryLabel: String,
    val secondaryLabel: String,
    val wifiSsid: String? = null,
    val wifiPassword: String? = null,
    val contactName: String? = null,
    val contactPhone: String? = null
)

object PayloadParser {

    fun parse(content: String, format: Int, type: Int = Barcode.TYPE_UNKNOWN): ParsedPayload {
        val trimmed = content.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        // 1. Wi-Fi QR Code
        if (type == Barcode.TYPE_WIFI || lower.startsWith("wifi:")) {
            val ssidRegex = Regex("""S:([^;]+)""", RegexOption.IGNORE_CASE)
            val passRegex = Regex("""P:([^;]+)""", RegexOption.IGNORE_CASE)
            val typeRegex = Regex("""T:([^;]+)""", RegexOption.IGNORE_CASE)

            val ssid = ssidRegex.find(trimmed)?.groupValues?.get(1) ?: "Wi-Fi Network"
            val pass = passRegex.find(trimmed)?.groupValues?.get(1)
            val secType = typeRegex.find(trimmed)?.groupValues?.get(1)?.uppercase(Locale.ROOT) ?: "WPA2"

            return ParsedPayload(
                category = PayloadCategory.WIFI,
                title = ssid,
                subtitle = if (pass != null) "$secType Secured • Key: $pass" else "$secType Secured (Open)",
                badge = "WI-FI",
                rawContent = trimmed,
                cleanedContent = pass ?: ssid,
                primaryLabel = "Connect Now",
                secondaryLabel = if (pass != null) "Copy Key" else "Copy SSID",
                wifiSsid = ssid,
                wifiPassword = pass
            )
        }

        // 2. Web Links / URLs
        val isUrl = type == Barcode.TYPE_URL || lower.startsWith("http://") || lower.startsWith("https://")
        if (isUrl) {
            val securityAnalysis = SecurityUtils.analyzeUrl(trimmed)
            val rootDomain = securityAnalysis.rootDomain
            val strippedCount = if (securityAnalysis.hasTrackers) "Trackers stripped" else "Direct link"
            val badgeText = when (securityAnalysis.riskLevel) {
                SecurityUtils.RiskLevel.DANGEROUS -> "THREAT"
                SecurityUtils.RiskLevel.CAUTION -> "CAUTION"
                SecurityUtils.RiskLevel.SAFE -> "SAFE"
            }

            return ParsedPayload(
                category = PayloadCategory.URL,
                title = rootDomain.ifBlank { trimmed },
                subtitle = "✓ $strippedCount",
                badge = badgeText,
                rawContent = trimmed,
                cleanedContent = securityAnalysis.cleanedUrl,
                primaryLabel = "Open Link",
                secondaryLabel = "Copy URL"
            )
        }

        // 3. Retail Products & Books (EAN, UPC, ISBN)
        val isProduct = type == Barcode.TYPE_PRODUCT || type == Barcode.TYPE_ISBN ||
                format == Barcode.FORMAT_EAN_13 || format == Barcode.FORMAT_EAN_8 ||
                format == Barcode.FORMAT_UPC_A || format == Barcode.FORMAT_UPC_E ||
                (trimmed.length == 13 && trimmed.all { it.isDigit() })

        if (isProduct) {
            val formatName = ScanUtils.getFormatName(format)
            return ParsedPayload(
                category = PayloadCategory.PRODUCT,
                title = trimmed,
                subtitle = "$formatName • Retail GTIN",
                badge = "PRODUCT",
                rawContent = trimmed,
                cleanedContent = trimmed,
                primaryLabel = "Search Web",
                secondaryLabel = "Copy Code"
            )
        }

        // 4. Contact Cards (vCard)
        if (type == Barcode.TYPE_CONTACT_INFO || lower.startsWith("begin:vcard")) {
            val fnRegex = Regex("""(?m)^FN:(.+)""", RegexOption.IGNORE_CASE)
            val nRegex = Regex("""(?m)^N:(.+)""", RegexOption.IGNORE_CASE)
            val telRegex = Regex("""(?m)^TEL[^:]*:(.+)""", RegexOption.IGNORE_CASE)
            
            val fnMatch = fnRegex.find(trimmed)?.groupValues?.get(1)?.trim()
            val name = if (!fnMatch.isNullOrBlank()) {
                fnMatch
            } else {
                val nParts = nRegex.find(trimmed)?.groupValues?.get(1)?.split(";")?.filter { it.isNotBlank() }
                if (!nParts.isNullOrEmpty()) nParts.reversed().joinToString(" ") else "Contact Card"
            }
            val phone = telRegex.find(trimmed)?.groupValues?.get(1)?.trim()

            return ParsedPayload(
                category = PayloadCategory.CONTACT,
                title = name,
                subtitle = phone ?: "vCard Address Book",
                badge = "CONTACT",
                rawContent = trimmed,
                cleanedContent = trimmed,
                primaryLabel = "Save Contact",
                secondaryLabel = if (phone != null) "Call Phone" else "Copy Info",
                contactName = name,
                contactPhone = phone
            )
        }

        // 5. Default Plain Text / Serials / OCR Text
        val isOcr = format == OcrUtils.FORMAT_OCR_TEXT
        val displayTitle = if (trimmed.length > 36) trimmed.take(33) + "..." else trimmed
        return ParsedPayload(
            category = PayloadCategory.TEXT,
            title = displayTitle,
            subtitle = if (isOcr) "Extracted OCR Serial" else "Plain Text (${trimmed.length} chars)",
            badge = if (isOcr) "SERIAL" else "TEXT",
            rawContent = trimmed,
            cleanedContent = trimmed,
            primaryLabel = "Copy Text",
            secondaryLabel = "Search Web"
        )
    }

    fun parse(scanResult: ScanResult): ParsedPayload {
        return parse(scanResult.content, scanResult.format, scanResult.type)
    }
}
