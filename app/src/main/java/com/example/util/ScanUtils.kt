package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.widget.Toast
import com.example.data.ScanResult
import com.google.mlkit.vision.barcode.common.Barcode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ScanUtils {

    fun getFormatName(format: Int): String {
        return when (format) {
            Barcode.FORMAT_QR_CODE -> "QR Code"
            Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
            Barcode.FORMAT_AZTEC -> "Aztec"
            Barcode.FORMAT_PDF417 -> "PDF417"
            Barcode.FORMAT_CODE_128 -> "Code 128"
            Barcode.FORMAT_CODE_39 -> "Code 39"
            Barcode.FORMAT_CODE_93 -> "Code 93"
            Barcode.FORMAT_CODABAR -> "Codabar"
            Barcode.FORMAT_EAN_13 -> "EAN-13"
            Barcode.FORMAT_EAN_8 -> "EAN-8"
            Barcode.FORMAT_ITF -> "ITF"
            Barcode.FORMAT_UPC_A -> "UPC-A"
            Barcode.FORMAT_UPC_E -> "UPC-E"
            OcrUtils.FORMAT_OCR_TEXT -> "OCR Text / Serial"
            else -> "Barcode"
        }
    }

    fun is1dLinear(format: Int): Boolean {
        return when (format) {
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_CODABAR,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_ITF,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E -> true
            else -> false
        }
    }

    fun is2dMatrix(format: Int): Boolean {
        return when (format) {
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_DATA_MATRIX,
            Barcode.FORMAT_AZTEC -> true
            else -> false
        }
    }

    fun isStacked2d(format: Int): Boolean {
        return format == Barcode.FORMAT_PDF417
    }

    fun getTypeName(type: Int, content: String): String {
        return when (type) {
            Barcode.TYPE_URL -> "Website URL"
            Barcode.TYPE_WIFI -> "Wi-Fi Network"
            Barcode.TYPE_EMAIL -> "Email Address"
            Barcode.TYPE_PHONE -> "Phone Number"
            Barcode.TYPE_SMS -> "SMS Message"
            Barcode.TYPE_GEO -> "Geo Location"
            Barcode.TYPE_CONTACT_INFO -> "Contact Card (vCard)"
            Barcode.TYPE_CALENDAR_EVENT -> "Calendar Event"
            Barcode.TYPE_PRODUCT -> "Product Barcode"
            Barcode.TYPE_ISBN -> "Book ISBN"
            else -> {
                val lower = content.lowercase(Locale.ROOT)
                when {
                    lower.startsWith("http://") || lower.startsWith("https://") -> "Website URL"
                    lower.startsWith("wifi:") -> "Wi-Fi Network"
                    lower.startsWith("begin:vcard") -> "Contact Card (vCard)"
                    lower.startsWith("mailto:") -> "Email Address"
                    lower.startsWith("tel:") -> "Phone Number"
                    lower.startsWith("smsto:") -> "SMS Message"
                    lower.startsWith("geo:") -> "Geo Location"
                    else -> "Plain Text"
                }
            }
        }
    }

    fun copyToClipboard(context: Context, text: String, label: String = "Scanned Content") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun shareText(context: Context, text: String, title: String = "Share Scanned Code") {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, title)
        context.startActivity(shareIntent)
    }

    data class WifiConfig(
        val ssid: String,
        val password: String,
        val type: String
    )

    fun parseWifiString(wifiString: String): WifiConfig {
        var ssid = ""
        var password = ""
        var type = "WPA"

        // Format: WIFI:S:SSID;T:WPA;P:PASSWORD;;
        val clean = wifiString.removePrefix("WIFI:").removePrefix("wifi:")
        val tokens = clean.split(";")
        for (token in tokens) {
            if (token.startsWith("S:", ignoreCase = true)) {
                ssid = token.substring(2)
            } else if (token.startsWith("P:", ignoreCase = true)) {
                password = token.substring(2)
            } else if (token.startsWith("T:", ignoreCase = true)) {
                type = token.substring(2)
            }
        }
        return WifiConfig(ssid = ssid, password = password, type = type)
    }

    fun openWifiSettings(context: Context, wifiString: String) {
        val config = parseWifiString(wifiString)
        if (config.password.isNotBlank()) {
            copyToClipboard(context, config.password, "Wi-Fi Password: ${config.ssid}")
            Toast.makeText(context, "Password for '${config.ssid}' copied. Opening Wi-Fi settings...", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Opening Wi-Fi settings for '${config.ssid}'...", Toast.LENGTH_SHORT).show()
        }

        try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun openAction(context: Context, content: String, type: Int) {
        try {
            val lower = content.lowercase(Locale.ROOT)
            when {
                type == Barcode.TYPE_WIFI || lower.startsWith("wifi:") -> {
                    openWifiSettings(context, content)
                }
                type == Barcode.TYPE_URL || lower.startsWith("http://") || lower.startsWith("https://") -> {
                    val url = if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
                        "https://$content"
                    } else {
                        content
                    }
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                type == Barcode.TYPE_PHONE || lower.startsWith("tel:") -> {
                    val uri = if (lower.startsWith("tel:")) Uri.parse(content) else Uri.parse("tel:$content")
                    val intent = Intent(Intent.ACTION_DIAL, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                type == Barcode.TYPE_EMAIL || lower.startsWith("mailto:") -> {
                    val uri = if (lower.startsWith("mailto:")) Uri.parse(content) else Uri.parse("mailto:$content")
                    val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                type == Barcode.TYPE_SMS || lower.startsWith("smsto:") -> {
                    val uri = if (lower.startsWith("smsto:")) Uri.parse(content) else Uri.parse("smsto:$content")
                    val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                type == Barcode.TYPE_GEO || lower.startsWith("geo:") -> {
                    val uri = if (lower.startsWith("geo:")) Uri.parse(content) else Uri.parse("geo:$content")
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                else -> {
                    copyToClipboard(context, content)
                }
            }
        } catch (_: Exception) {
            copyToClipboard(context, content)
            Toast.makeText(context, "Copied content to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerHapticFeedback(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(45)
                }
            }
        } catch (_: Exception) {
            // Ignore if vibration is not permitted or unavailable
        }
    }

    fun exportHistoryAsCsv(scans: List<ScanResult>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("ID,Timestamp,Formatted_Date,Type,Format,Favorite,Note,Content\n")
        scans.forEach { scan ->
            val dateStr = dateFormat.format(Date(scan.timestamp))
            val escapedContent = "\"" + scan.content.replace("\"", "\"\"") + "\""
            val escapedNote = "\"" + (scan.note?.replace("\"", "\"\"") ?: "") + "\""
            sb.append("${scan.id},${scan.timestamp},$dateStr,${getTypeName(scan.type, scan.content)},${getFormatName(scan.format)},${scan.isFavorite},$escapedNote,$escapedContent\n")
        }
        return sb.toString()
    }

    fun exportHistoryAsJson(scans: List<ScanResult>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        val sb = StringBuilder()
        sb.append("[\n")
        scans.forEachIndexed { index, scan ->
            val escapedContent = scan.content.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
            val escapedNote = scan.note?.replace("\\", "\\\\")?.replace("\"", "\\\"")?.replace("\n", "\\n")?.replace("\r", "\\r")
            sb.append("  {\n")
            sb.append("    \"id\": ${scan.id},\n")
            sb.append("    \"timestamp\": ${scan.timestamp},\n")
            sb.append("    \"date\": \"${dateFormat.format(Date(scan.timestamp))}\",\n")
            sb.append("    \"type\": \"${getTypeName(scan.type, scan.content)}\",\n")
            sb.append("    \"format\": \"${getFormatName(scan.format)}\",\n")
            sb.append("    \"isFavorite\": ${scan.isFavorite},\n")
            if (escapedNote != null) {
                sb.append("    \"note\": \"$escapedNote\",\n")
            } else {
                sb.append("    \"note\": null,\n")
            }
            sb.append("    \"content\": \"$escapedContent\"\n")
            sb.append("  }${if (index < scans.size - 1) "," else ""}\n")
        }
        sb.append("]\n")
        return sb.toString()
    }
}
