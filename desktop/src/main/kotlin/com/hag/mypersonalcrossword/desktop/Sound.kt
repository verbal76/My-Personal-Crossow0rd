package com.hag.mypersonalcrossword.desktop

import com.hag.mypersonalcrossword.core.SoundSynth
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import javax.sound.sampled.LineEvent
import kotlin.math.log10
import kotlin.math.roundToInt

/** 16-bit signed little-endian mono PCM from the shared synthesiser's float samples. Pure, so it is unit-tested. */
object Pcm {
    fun fromFloats(samples: FloatArray): ByteArray {
        val out = ByteArray(samples.size * 2)
        for ((i, f) in samples.withIndex()) {
            val v = (f.coerceIn(-1f, 1f) * 32767f).roundToInt()
            out[2 * i] = (v and 0xFF).toByte(); out[2 * i + 1] = ((v shr 8) and 0xFF).toByte()
        }
        return out
    }
    /** Playing the same samples at a higher sample rate raises the pitch: the correct-answer chime rises a semitone per streak step (SoundSynth.streakRate). */
    fun format(rate: Float) = AudioFormat(SoundSynth.SAMPLE_RATE * rate, 16, 1, true, false)
}

/**
 * Sound effects for the desktop build: the SAME synthesised effects Android plays (core/SoundSynth), through
 * javax.sound (JDK only). If the machine has no audio output (a headless box, a CI runner) it switches itself off
 * silently instead of failing. Music is not ported (docs/DESKTOP_PARITY.md).
 */
class DesktopSound {
    private val pcm = HashMap<SoundSynth.Effect, ByteArray>()
    @Volatile var available = true; private set

    private val volumes = mapOf(SoundSynth.Effect.CLICK to 0.7f, SoundSynth.Effect.KEY to 0.55f, SoundSynth.Effect.TICK to 0.6f, SoundSynth.Effect.WRONG to 0.9f)

    fun play(effect: SoundSynth.Effect, streak: Int = 0) {
        if (!available) return
        runCatching {
            val data = synchronized(pcm) { pcm.getOrPut(effect) { Pcm.fromFloats(SoundSynth.render(effect)) } }
            val rate = if (effect == SoundSynth.Effect.CORRECT) SoundSynth.streakRate(streak) else 1f
            val clip = AudioSystem.getClip()
            clip.addLineListener { e -> if (e.type == LineEvent.Type.STOP) clip.close() }
            clip.open(Pcm.format(rate), data, 0, data.size)
            setVolume(clip, volumes[effect] ?: 1f)
            clip.start()
        }.onFailure { available = false }      // no mixer / no output device: stay quiet from now on
    }

    private fun setVolume(clip: Clip, volume: Float) {
        runCatching {
            val c = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            c.value = (20f * log10(volume.coerceAtLeast(0.001f))).coerceIn(c.minimum, c.maximum)
        }
    }
}
