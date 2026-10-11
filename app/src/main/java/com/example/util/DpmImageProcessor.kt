package com.example.util

import android.graphics.Bitmap
import android.graphics.Color

object DpmImageProcessor {

    /**
     * Inverts a grayscale / Y-plane luminance byte array so that white-on-black marks
     * (e.g. laser-etched metal, PCB solder masks, dark mode screens) become standard black-on-white.
     */
    fun invertLuminance(bytes: ByteArray): ByteArray {
        val inverted = ByteArray(bytes.size)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            inverted[i] = (255 - v).toByte()
        }
        return inverted
    }

    /**
     * Stretches the dynamic range of a luminance array to suppress glare and deepen groove shadows
     * typical of Direct Part Marking (DPM) laser etches and dot-peened surfaces.
     */
    fun stretchContrast(bytes: ByteArray): ByteArray {
        if (bytes.isEmpty()) return bytes

        var min = 255
        var max = 0
        // Sample every 4th byte for rapid min/max discovery without CPU stalling
        val step = 4
        for (i in 0 until bytes.size step step) {
            val v = bytes[i].toInt() and 0xFF
            if (v < min) min = v
            if (v > max) max = v
        }

        val range = max - min
        if (range <= 15) return bytes // Too low contrast or blank frame; keep original

        val stretched = ByteArray(bytes.size)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            val normalized = ((v - min) * 255) / range
            stretched[i] = normalized.coerceIn(0, 255).toByte()
        }
        return stretched
    }

    /**
     * Inverts an RGB Bitmap for processing gallery images or captured camera frames.
     */
    fun invertBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val invertedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val color = pixels[i]
            val a = Color.alpha(color)
            val r = 255 - Color.red(color)
            val g = 255 - Color.green(color)
            val b = 255 - Color.blue(color)
            pixels[i] = Color.argb(a, r, g, b)
        }

        invertedBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return invertedBitmap
    }

    /**
     * Applies contrast enhancement to a Bitmap for DPM codes.
     */
    fun enhanceDpmBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var minLum = 255
        var maxLum = 0
        for (i in 0 until pixels.size step 4) {
            val color = pixels[i]
            val lum = (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000
            if (lum < minLum) minLum = lum
            if (lum > maxLum) maxLum = lum
        }

        val range = maxLum - minLum
        if (range <= 20) return bitmap

        for (i in pixels.indices) {
            val color = pixels[i]
            val a = Color.alpha(color)
            val r = (((Color.red(color) - minLum) * 255) / range).coerceIn(0, 255)
            val g = (((Color.green(color) - minLum) * 255) / range).coerceIn(0, 255)
            val b = (((Color.blue(color) - minLum) * 255) / range).coerceIn(0, 255)
            pixels[i] = Color.argb(a, r, g, b)
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }
}
