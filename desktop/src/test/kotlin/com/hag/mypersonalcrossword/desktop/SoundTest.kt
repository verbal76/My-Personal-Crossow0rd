package com.hag.mypersonalcrossword.desktop

import com.hag.mypersonalcrossword.core.SoundSynth
import org.junit.Assert.*
import org.junit.Test

class SoundTest {
    @Test fun everyEffectConvertsToValidSixteenBitMonoPcm() {
        for (e in SoundSynth.Effect.entries) {
            val f = SoundSynth.render(e); val pcm = Pcm.fromFloats(f)
            assertEquals("$e", f.size * 2, pcm.size); assertTrue("$e has audio", pcm.any { it.toInt() != 0 })
        }
    }
    @Test fun conversionClampsAndEncodesLittleEndian() {
        val pcm = Pcm.fromFloats(floatArrayOf(1f, -1f, 0f, 2f, -2f))
        fun s(i: Int) = ((pcm[2 * i + 1].toInt() shl 8) or (pcm[2 * i].toInt() and 0xFF)).toShort().toInt()
        assertEquals(32767, s(0)); assertEquals(-32767, s(1)); assertEquals(0, s(2)); assertEquals(32767, s(3)); assertEquals(-32767, s(4))
    }
    @Test fun streakStepsRaiseThePitchByPlayingFaster() {
        assertTrue(Pcm.format(SoundSynth.streakRate(3)).sampleRate > Pcm.format(SoundSynth.streakRate(0)).sampleRate)
        assertEquals(SoundSynth.SAMPLE_RATE.toFloat(), Pcm.format(1f).sampleRate, 0.01f)
    }
    @Test fun playingWithoutAnyAudioDeviceNeverThrows() {
        val s = DesktopSound()
        for (e in SoundSynth.Effect.entries) s.play(e, 2)       // a sandbox/CI box has no mixer: it must just go quiet
        s.play(SoundSynth.Effect.CORRECT, 99)
    }
}
