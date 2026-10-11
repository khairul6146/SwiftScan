package com.example.util

import android.net.Uri
import java.util.Locale
import java.util.regex.Pattern

object SecurityUtils {

    enum class RiskLevel {
        SAFE,
        CAUTION,
        DANGEROUS
    }

    data class SecurityAnalysis(
        val originalUrl: String,
        val cleanedUrl: String,
        val rootDomain: String,
        val fullHost: String,
        val isHttps: Boolean,
        val riskLevel: RiskLevel,
        val riskReason: String,
        val isShortener: Boolean,
        val hasTrackers: Boolean
    )

    private val IP_PATTERN = Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")

    private val SHORTENER_DOMAINS = setOf(
        "bit.ly", "tinyurl.com", "t.co", "rb.gy", "is.gd",
        "ow.ly", "buff.ly", "cutt.ly", "goo.gl", "rebrand.ly",
        "bl.ink", "shorturl.at"
    )

    private val TRACKING_PARAMS = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "gclid", "fbclid", "igshid", "mc_eid", "msclkid", "yclid",
        "_hsenc", "_hsmi", "ref", "ref_src", "si", "s", "feature"
    )

    private val DANGEROUS_EXTENSIONS = setOf(
        ".apk", ".exe", ".bat", ".cmd", ".vbs", ".msi", ".scr", ".sh", ".bin"
    )

    /**
     * Inspects a URL for security threats, phishing indicators, and privacy trackers.
     */
    fun analyzeUrl(rawUrl: String): SecurityAnalysis {
        val trimmed = rawUrl.trim()
        val normalized = if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            "https://$trimmed"
        } else {
            trimmed
        }

        val uri = try {
            Uri.parse(normalized)
        } catch (_: Exception) {
            null
        }

        val scheme = uri?.scheme?.lowercase(Locale.ROOT) ?: ""
        val host = uri?.host?.lowercase(Locale.ROOT) ?: ""
        val isHttps = scheme == "https"
        val rootDomain = extractRootDomain(host)

        val isIp = IP_PATTERN.matcher(host).matches()
        val isShortener = SHORTENER_DOMAINS.contains(rootDomain) || SHORTENER_DOMAINS.contains(host)

        val path = uri?.path?.lowercase(Locale.ROOT) ?: ""
        val hasDangerousExt = DANGEROUS_EXTENSIONS.any { path.endsWith(it) }

        val cleanedUrl = stripTrackingParameters(normalized)
        val hasTrackers = cleanedUrl != normalized

        val (riskLevel, riskReason) = when {
            hasDangerousExt -> {
                RiskLevel.DANGEROUS to "Direct download link for an executable or installer ($path)"
            }
            scheme != "https" && scheme != "http" -> {
                RiskLevel.DANGEROUS to "Unusual URI scheme ($scheme)"
            }
            isIp -> {
                RiskLevel.CAUTION to "Raw IP address URL instead of a verified domain name"
            }
            isShortener -> {
                RiskLevel.CAUTION to "Shortened link (true destination is masked)"
            }
            !isHttps -> {
                RiskLevel.CAUTION to "Unencrypted HTTP connection (vulnerable to interception)"
            }
            else -> {
                RiskLevel.SAFE to "Standard encrypted HTTPS connection"
            }
        }

        return SecurityAnalysis(
            originalUrl = normalized,
            cleanedUrl = cleanedUrl,
            rootDomain = rootDomain,
            fullHost = host,
            isHttps = isHttps,
            riskLevel = riskLevel,
            riskReason = riskReason,
            isShortener = isShortener,
            hasTrackers = hasTrackers
        )
    }

    /**
     * Extracts the primary root domain (e.g. 'paypal.com' from 'auth.login.paypal.com').
     */
    fun extractRootDomain(host: String): String {
        if (host.isBlank()) return "Unknown"
        if (IP_PATTERN.matcher(host).matches()) return host

        val parts = host.split(".")
        if (parts.size <= 2) return host

        // Handle common country-code second level domains like '.co.uk', '.com.my', '.org.uk'
        val secondLevelDomains = setOf("co", "com", "org", "net", "edu", "gov")
        return if (parts.size >= 3 && secondLevelDomains.contains(parts[parts.size - 2])) {
            "${parts[parts.size - 3]}.${parts[parts.size - 2]}.${parts.last()}"
        } else {
            "${parts[parts.size - 2]}.${parts.last()}"
        }
    }

    /**
     * Strips analytics, advertising, and tracking query parameters while preserving functional parameters.
     */
    fun stripTrackingParameters(url: String): String {
        return try {
            val uri = Uri.parse(url)
            val queryNames = uri.queryParameterNames
            if (queryNames.isEmpty()) return url

            val remainingNames = queryNames.filter { !TRACKING_PARAMS.contains(it.lowercase(Locale.ROOT)) }
            if (remainingNames.size == queryNames.size) return url

            val builder = uri.buildUpon().clearQuery()
            for (param in remainingNames) {
                val values = uri.getQueryParameters(param)
                for (value in values) {
                    builder.appendQueryParameter(param, value)
                }
            }
            builder.build().toString()
        } catch (_: Exception) {
            url
        }
    }
}
