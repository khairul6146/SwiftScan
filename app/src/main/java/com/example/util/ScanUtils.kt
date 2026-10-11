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
import android.widget.Toast
import com.google.mlkit.vision.barcode.common.Barcode

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
            else -> "Barcode"
        }
    }

    fun getTypeName(type: Int, content: String): String {
        return when (type) {
            Barcode.TYPE_URL -> "Website URL"
            Barcode.TYPE_WIFI -> "Wi-Fi Network"
            Barcode.TYPE_EMAIL -> "Email Address"
            Barcode.TYPE_PHONE -> "Phone Number"
            Barcode.TYPE_SMS -> "SMS Message"
            Barcode.TYPE_GEO -> "Geo Location"
            Barcode.TYPE_CONTACT_INFO -> "Contact Card"
            Barcode.TYPE_CALENDAR_EVENT -> "Calendar Event"
            Barcode.TYPE_PRODUCT -> "Product Code"
            Barcode.TYPE_ISBN -> "Book ISBN"
            else -> {
                if (content.startsWith("http://") || content.startsWith("https://")) {
                    "Website URL"
                } else {
                    "Plain Text"
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

    fun shareText(context: Context, text: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Scanned QR Code")
        context.startActivity(shareIntent)
    }

    fun openAction(context: Context, content: String, type: Int) {
        try {
            when {
                type == Barcode.TYPE_URL || content.startsWith("http://") || content.startsWith("https://") -> {
                    val url = if (!content.startsWith("http://") && !content.startsWith("https://")) {
                        "https://$content"
                    } else {
                        content
                    }
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                }
                type == Barcode.TYPE_PHONE || content.startsWith("tel:") -> {
                    val uri = if (content.startsWith("tel:")) Uri.parse(content) else Uri.parse("tel:$content")
                    val intent = Intent(Intent.ACTION_DIAL, uri)
                    context.startActivity(intent)
                }
                type == Barcode.TYPE_EMAIL || content.startsWith("mailto:") -> {
                    val uri = if (content.startsWith("mailto:")) Uri.parse(content) else Uri.parse("mailto:$content")
                    val intent = Intent(Intent.ACTION_SENDTO, uri)
                    context.startActivity(intent)
                }
                type == Barcode.TYPE_SMS || content.startsWith("smsto:") -> {
                    val uri = if (content.startsWith("smsto:")) Uri.parse(content) else Uri.parse("smsto:$content")
                    val intent = Intent(Intent.ACTION_SENDTO, uri)
                    context.startActivity(intent)
                }
                type == Barcode.TYPE_GEO || content.startsWith("geo:") -> {
                    val uri = if (content.startsWith("geo:")) Uri.parse(content) else Uri.parse("geo:$content")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    context.startActivity(intent)
                }
                else -> {
                    copyToClipboard(context, content)
                }
            }
        } catch (e: Exception) {
            copyToClipboard(context, content)
            Toast.makeText(context, "Could not open directly; copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerHapticFeedback(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(60)
                }
            }
        } catch (_: Exception) {
            // Ignore if vibration is not permitted or unavailable
        }
    }

    fun exportHistoryAsCsv(scans: List<com.example.data.ScanResult>): String {
        val sb = StringBuilder()
        sb.append("ID,Type,Format,Timestamp,Content\n")
        scans.forEach { scan ->
            val escapedContent = "\"" + scan.content.replace("\"", "\"\"") + "\""
            sb.append("${scan.id},${getTypeName(scan.type, scan.content)},${getFormatName(scan.format)},${scan.timestamp},$escapedContent\n")
        }
        return sb.toString()
    }
}
