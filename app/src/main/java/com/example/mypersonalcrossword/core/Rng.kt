package com.hag.mypersonalcrossword.core

import kotlin.random.Random

// ============================================================
// DETERMINISTIC RANDOMNESS — owned by this codebase, not the stdlib.
// ============================================================
// kotlin.random.Random(seed) documents that its seeded algorithm may change
// between Kotlin versions, and stdlib shuffle() doesn't promise a fixed
// algorithm either. The Daily Puzzle must be identical on every device and
// every build, so it runs on SplitMix64 (Steele, Lea & Flood 2014) and an
// in-house Fisher–Yates. Every Random method the generator can reach is
// overridden here, so no stdlib default implementation sits on the path.

/**
 * SplitMix64 as a [Random], so any code taking `rng: Random` can use it.
 * Same seed ⇒ same sequence, forever. Changing anything in this class changes
 * every Daily: bump [DailyPuzzle.ALGORITHM_VERSION] if you ever must.
 */
class SplitMix64(seed: Long) : Random() {
    private var state = seed

    /** Next raw 64-bit output. */
    fun nextRaw(): Long {
        state += GOLDEN_GAMMA
        var z = state
        z = (z xor (z ushr 30)) * MIX_1
        z = (z xor (z ushr 27)) * MIX_2
        return z xor (z ushr 31)
    }

    override fun nextBits(bitCount: Int): Int =
        if (bitCount <= 0) 0 else (nextRaw() ushr (64 - bitCount.coerceAtMost(32))).toInt()

    override fun nextInt(): Int = (nextRaw() ushr 32).toInt()

    override fun nextInt(until: Int): Int = nextInt(0, until)

    override fun nextInt(from: Int, until: Int): Int =
        (from + nextBounded(until.toLong() - from.toLong())).toInt()

    override fun nextLong(): Long = nextRaw()

    override fun nextBoolean(): Boolean = nextRaw() < 0

    override fun nextDouble(): Double = (nextRaw() ushr 11) * DOUBLE_UNIT

    /** Unbiased value in [0, bound) by rejection sampling over 63 bits. */
    private fun nextBounded(bound: Long): Long {
        require(bound > 0) { "bound must be positive: $bound" }
        while (true) {
            val r = nextRaw() ushr 1
            val v = r % bound
            if (r - v + (bound - 1) >= 0) return v   // reject the biased tail (overflow ⇒ negative)
        }
    }

    private companion object {
        const val GOLDEN_GAMMA = -0x61c8864680b583ebL   // 0x9E3779B97F4A7C15
        const val MIX_1        = -0x40a7b892e31b1a47L   // 0xBF58476D1CE4E5B9
        const val MIX_2        = -0x6b2fb644ecceee15L   // 0x94D049BB133111EB
        const val DOUBLE_UNIT  = 1.0 / (1L shl 53)
    }
}

/**
 * Fisher–Yates (Durstenfeld) shuffle with a pinned algorithm: walks from the
 * end, swapping index i with rng.nextInt(i + 1). Used instead of stdlib
 * shuffled() wherever output must be reproducible.
 */
fun <T> List<T>.fisherYates(rng: Random): List<T> {
    val out = toMutableList()
    for (i in out.lastIndex downTo 1) {
        val j = rng.nextInt(i + 1)
        val t = out[i]; out[i] = out[j]; out[j] = t
    }
    return out
}
