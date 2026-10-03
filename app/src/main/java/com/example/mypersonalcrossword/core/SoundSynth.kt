package com.hag.mypersonalcrossword.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

// ============================================================
// SOUND SYNTHESIS — every effect is rendered once, normalised, and
// encoded as a 16-bit mono WAV that SoundPool can load. Pure Kotlin so
// levels and lengths are unit-tested (no clipping, audible click).
// ============================================================

object SoundSynth {
    const val SAMPLE_RATE = 44_100
    /** Peak level after normalisation — headroom so stacked voices never clip. */
    const val TARGET_PEAK = 0.85f

    enum class Effect { CLICK, KEY, CORRECT, WRONG, CLAP, CELEBRATION, TICK }

    fun render(effect: Effect): FloatArray = normalise(when (effect) {
        Effect.CLICK       -> click()
        Effect.KEY         -> key()
        Effect.CORRECT     -> correct()
        Effect.WRONG       -> wrong()
        Effect.CLAP        -> clap()
        Effect.CELEBRATION -> celebration()
        Effect.TICK        -> tick()
    }, if (effect == Effect.CLICK || effect == Effect.KEY || effect == Effect.TICK) 0.45f else TARGET_PEAK)

    private fun samples(seconds: Double) = FloatArray((SAMPLE_RATE * seconds).toInt())

    /** Adds a sine voice with a short attack and an exponential release (no clicks at the edges). */
    private fun tone(buf: FloatArray, startSec: Double, durSec: Double, freq: Double, amp: Double, decay: Double = 6.0) {
        val start = (startSec * SAMPLE_RATE).toInt()
        val n     = (durSec * SAMPLE_RATE).toInt()
        val attack = (0.004 * SAMPLE_RATE).toInt().coerceAtLeast(1)
        val release = (0.012 * SAMPLE_RATE).toInt().coerceAtLeast(1)
        for (i in 0 until n) {
            val idx = start + i
            if (idx >= buf.size) break
            val t   = i.toDouble() / SAMPLE_RATE
            val env = minOf(1.0, i.toDouble() / attack, (n - i).toDouble() / release) * exp(-decay * t)
            buf[idx] += (amp * env * sin(2 * PI * freq * t)).toFloat()
        }
    }

    private fun click() = samples(0.07).also { tone(it, 0.0, 0.07, 1250.0, 1.0, decay = 55.0) }

    /** Softer, lower tick for letter entry. */
    private fun key() = samples(0.05).also { tone(it, 0.0, 0.05, 820.0, 1.0, decay = 70.0) }

    /** Countdown tick. */
    private fun tick() = samples(0.06).also { tone(it, 0.0, 0.06, 1600.0, 1.0, decay = 60.0) }

    /** C5 → E5 → G5 arpeggio with a soft octave shimmer. */
    private fun correct() = samples(0.55).also { b ->
        listOf(523.25 to 0.0, 659.25 to 0.09, 783.99 to 0.18).forEach { (f, s) ->
            tone(b, s, 0.34, f, 1.0, decay = 5.5)
            tone(b, s, 0.34, f * 2, 0.22, decay = 8.0)
        }
    }

    /**
     * Two falling "bonks" in the 180–240 Hz range, which phone speakers can
     * actually reproduce (the old 70/90 Hz layers were inaudible and only muddied it).
     */
    private fun wrong() = samples(0.42).also { b ->
        tone(b, 0.00, 0.17, 240.0, 1.0, decay = 9.0)
        tone(b, 0.00, 0.17, 480.0, 0.25, decay = 12.0)
        tone(b, 0.17, 0.24, 185.0, 1.0, decay = 7.0)
        tone(b, 0.17, 0.24, 370.0, 0.25, decay = 10.0)
    }

    /** Five short band-limited noise bursts — reads as applause, not static. */
    private fun clap() = samples(0.95).also { b ->
        val rng = Random(7)          // fixed seed: the same clap every time
        for (burst in 0 until 5) {
            val s = (burst * 0.17 * SAMPLE_RATE).toInt()
            val n = (0.08 * SAMPLE_RATE).toInt()
            var lp = 0f; var prev = 0f
            for (i in 0 until n) {
                val idx = s + i; if (idx >= b.size) break
                val white = rng.nextFloat() * 2f - 1f
                lp += 0.35f * (white - lp)            // low-pass
                val bp = lp - prev; prev = lp          // then high-pass → band-pass
                val env = exp(-i.toDouble() / (0.018 * SAMPLE_RATE)).toFloat() * minOf(1f, i / 60f)
                b[idx] += bp * 3f * env
            }
        }
    }

    /** C5-E5-G5-C6 fanfare with harmonics and a sparkling tail. */
    private fun celebration() = samples(1.45).also { b ->
        listOf(523.25 to 0.0, 659.25 to 0.16, 783.99 to 0.32, 1046.50 to 0.50).forEach { (f, s) ->
            tone(b, s, 0.55, f, 1.0, decay = 3.5)
            tone(b, s, 0.55, f * 2, 0.3, decay = 5.0)
        }
        listOf(1318.51 to 0.80, 1567.98 to 0.92, 2093.00 to 1.04).forEach { (f, s) ->
            tone(b, s, 0.40, f, 0.35, decay = 6.0)
        }
    }

    /** Scales the buffer so its absolute peak equals [peak] (silence stays silence). */
    fun normalise(buf: FloatArray, peak: Float = TARGET_PEAK): FloatArray {
        val max = buf.maxOfOrNull { abs(it) } ?: 0f
        if (max <= 0f) return buf
        val k = peak / max
        for (i in buf.indices) buf[i] *= k
        return buf
    }

    /** Encodes mono float samples in [-1, 1] as a 16-bit PCM RIFF/WAVE file. */
    fun toWav(samples: FloatArray, sampleRate: Int = SAMPLE_RATE): ByteArray {
        val dataLen = samples.size * 2
        val out = ByteArray(44 + dataLen)
        fun putStr(off: Int, s: String) = s.forEachIndexed { i, c -> out[off + i] = c.code.toByte() }
        fun putInt(off: Int, v: Int) { for (i in 0..3) out[off + i] = (v shr (8 * i)).toByte() }
        fun putShort(off: Int, v: Int) { out[off] = v.toByte(); out[off + 1] = (v shr 8).toByte() }
        putStr(0, "RIFF"); putInt(4, 36 + dataLen); putStr(8, "WAVE")
        putStr(12, "fmt "); putInt(16, 16); putShort(20, 1); putShort(22, 1)
        putInt(24, sampleRate); putInt(28, sampleRate * 2); putShort(32, 2); putShort(34, 16)
        putStr(36, "data"); putInt(40, dataLen)
        samples.forEachIndexed { i, f ->
            val v = (f.coerceIn(-1f, 1f) * 32767f).toInt()
            putShort(44 + i * 2, v)
        }
        return out
    }

    /**
     * Playback-rate multiplier for the correct chime: rises a semitone per streak
     * step (capped at an octave) so a run of right answers audibly escalates.
     */
    fun streakRate(streak: Int): Float =
        Math.pow(2.0, (streak.coerceIn(0, 12)) / 12.0).toFloat().coerceIn(0.5f, 2f)
}
