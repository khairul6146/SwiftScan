package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.example.ui.theme.*
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ColorContrastTest {
    private fun contrast(fg: Color, bg: Color): Double =
        ColorUtils.calculateContrast(fg.toArgb(), bg.toArgb())

    @Test
    fun testTitaniumCobaltContrastMeetsWcag() {
        val onBase = contrast(TextHighEmphasis, VoidCanvas)
        assertTrue("High emphasis text on base canvas must be >= 7.0 (AAA): was $onBase", onBase >= 7.0)

        val onSlate = contrast(TextMediumEmphasis, SurfaceSlate)
        assertTrue("Medium emphasis text on slate must be >= 4.5 (AA): was $onSlate", onSlate >= 4.5)

        val onCobaltBtn = contrast(Color.White, PrimaryCobalt)
        assertTrue("White text on Cobalt CTA must be >= 4.5 (AA): was $onCobaltBtn", onCobaltBtn >= 4.5)
    }
}
