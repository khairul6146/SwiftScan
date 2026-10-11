package com.example

import android.graphics.Bitmap
import android.graphics.Color
import com.example.util.DpmImageProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DpmImageProcessorTest {

    @Test
    fun testInvertLuminance_byteValues() {
        val original = byteArrayOf(0.toByte(), 100.toByte(), 200.toByte(), 255.toByte())
        val inverted = DpmImageProcessor.invertLuminance(original)

        assertEquals(255.toByte(), inverted[0])
        assertEquals((255 - 100).toByte(), inverted[1])
        assertEquals((255 - 200).toByte(), inverted[2])
        assertEquals(0.toByte(), inverted[3])
    }

    @Test
    fun testStretchContrast_dynamicRange() {
        val lowContrast = byteArrayOf(100.toByte(), 110.toByte(), 120.toByte(), 130.toByte(), 140.toByte(), 150.toByte())
        val stretched = DpmImageProcessor.stretchContrast(lowContrast)

        // Min (100) should be remapped to 0, Max (150) to 255
        val firstVal = stretched[0].toInt() and 0xFF
        val lastVal = stretched[stretched.size - 1].toInt() and 0xFF
        assertEquals(0, firstVal)
        assertEquals(255, lastVal)
    }

    @Test
    fun testInvertBitmap_colorsInverted() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        bitmap.setPixel(0, 0, Color.BLACK)
        bitmap.setPixel(1, 1, Color.WHITE)

        val inverted = DpmImageProcessor.invertBitmap(bitmap)
        assertNotNull(inverted)
        assertEquals(Color.WHITE, inverted.getPixel(0, 0))
        assertEquals(Color.BLACK, inverted.getPixel(1, 1))
    }
}
