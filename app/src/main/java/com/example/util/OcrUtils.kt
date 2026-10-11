package com.example.util

import java.util.regex.Pattern

object OcrUtils {

    const val FORMAT_OCR_TEXT = 999

    data class OcrResult(
        val fullText: String,
        val primarySerial: String?,
        val displaySummary: String
    )

    // Regex for explicitly labeled serial numbers, lot numbers, or VINs (requires separator : # or whitespace)
    private val LABELED_SERIAL_PATTERN = Pattern.compile(
        "(?i)\\b(?:SN|S/N|SERIAL|LOT|VIN|PN|P/N|PART|REF|BATCH|PO)[:#\\s]+([A-Z0-9-_]{4,28})\\b"
    )

    // Regex for standalone structured codes (e.g. 'ABC-1234-XYZ', '2026-X8491')
    private val STRUCTURED_CODE_PATTERN = Pattern.compile(
        "\\b[A-Z0-9]{2,8}[-_][A-Z0-9-_]{2,16}\\b"
    )

    // Regex for clean alphanumeric identifiers (e.g. IMEI, tracking numbers, MAC)
    private val ALPHANUMERIC_TOKEN_PATTERN = Pattern.compile(
        "\\b[A-Z0-9]{7,24}\\b"
    )

    /**
     * Parses raw OCR vision text to identify serial numbers, batch IDs, or part codes.
     */
    fun parseOcrText(rawText: String): OcrResult {
        val trimmed = rawText.trim()
        if (trimmed.isBlank()) {
            return OcrResult(fullText = "", primarySerial = null, displaySummary = "No Text Detected")
        }

        // 1. Check for labeled serial / lot / part
        val labeledMatcher = LABELED_SERIAL_PATTERN.matcher(trimmed)
        if (labeledMatcher.find()) {
            val candidate = labeledMatcher.group(1)?.trim()
            if (!candidate.isNullOrBlank()) {
                return OcrResult(
                    fullText = trimmed,
                    primarySerial = candidate,
                    displaySummary = candidate
                )
            }
        }

        // 2. Check for structured code with hyphens or underscores
        val structuredMatcher = STRUCTURED_CODE_PATTERN.matcher(trimmed)
        if (structuredMatcher.find()) {
            val candidate = structuredMatcher.group()
            return OcrResult(
                fullText = trimmed,
                primarySerial = candidate,
                displaySummary = candidate
            )
        }

        // 3. Check for standalone alphanumeric token (e.g. 8+ characters)
        val tokenMatcher = ALPHANUMERIC_TOKEN_PATTERN.matcher(trimmed)
        if (tokenMatcher.find()) {
            val candidate = tokenMatcher.group()
            return OcrResult(
                fullText = trimmed,
                primarySerial = candidate,
                displaySummary = candidate
            )
        }

        // 4. Default to the first non-empty line
        val firstLine = trimmed.lines().firstOrNull { it.isNotBlank() } ?: trimmed
        return OcrResult(
            fullText = trimmed,
            primarySerial = null,
            displaySummary = firstLine.take(40)
        )
    }
}
