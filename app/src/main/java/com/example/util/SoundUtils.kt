package com.example.util

import android.media.AudioManager
import android.media.ToneGenerator

object SoundUtils {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
        } catch (_: Exception) {
            toneGenerator = null
        }
    }

    fun playBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
        } catch (_: Exception) {
            // Safe fallback if audio device unavailable
        }
    }
}
