package com.hag.mypersonalcrossword.core

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundSynthTest {

    /** Regression: stacked voices summed to 1.35–1.7 and were hard-clipped. */
    @Test fun noEffectClipsOrIsSilent() {
        for (e in SoundSynth.Effect.entries) {
            val buf = SoundSynth.render(e)
            val peak = buf.maxOf { abs(it) }
            assertTrue("$e peak $peak", peak <= SoundSynth.TARGET_PEAK + 1e-4f)
            assertTrue("$e is silent", peak > 0.2f)
            assertTrue("$e has NaN", buf.none { it.isNaN() })
        }
    }

    /** Regression: the old 41 ms click was cut off before output latency elapsed. */
    @Test fun clickIsLongEnoughToBeHeard() {
        val ms = SoundSynth.render(SoundSynth.Effect.CLICK).size * 1000 / SoundSynth.SAMPLE_RATE
        assertTrue("click is $ms ms", ms >= 60)
    }

    @Test fun effectsStartAndEndQuietly() {
        for (e in SoundSynth.Effect.entries) {
            val buf = SoundSynth.render(e)
            assertTrue("$e starts with a pop", abs(buf.first()) < 0.05f)
            assertTrue("$e ends with a pop", abs(buf.last()) < 0.05f)
        }
    }

    @Test fun wavHeaderIsValid() {
        val wav = SoundSynth.toWav(floatArrayOf(0f, 1f, -1f))
        assertEquals("RIFF", String(wav, 0, 4))
        assertEquals("WAVE", String(wav, 8, 4))
        assertEquals("data", String(wav, 36, 4))
        assertEquals(44 + 6, wav.size)
        // Full-scale positive sample → 0x7FFF little-endian.
        assertEquals(0xFF.toByte(), wav[46]); assertEquals(0x7F.toByte(), wav[47])
    }

    @Test fun streakRateRisesAndCaps() {
        assertEquals(1f, SoundSynth.streakRate(0), 1e-4f)
        assertTrue(SoundSynth.streakRate(3) > SoundSynth.streakRate(2))
        assertEquals(2f, SoundSynth.streakRate(40), 1e-4f)
    }
}
