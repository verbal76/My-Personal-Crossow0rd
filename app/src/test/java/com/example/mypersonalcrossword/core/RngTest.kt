package com.hag.mypersonalcrossword.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RngTest {

    @Test fun sameSeedSameSequenceDifferentSeedDifferent() {
        val a = SplitMix64(7); val b = SplitMix64(7); val c = SplitMix64(8)
        val sa = List(50) { a.nextInt(1000) }
        assertEquals(sa, List(50) { b.nextInt(1000) })
        assertNotEquals(sa, List(50) { c.nextInt(1000) })
    }

    @Test fun boundedValuesStayInRangeAndCoverIt() {
        val r = SplitMix64(1)
        val seen = IntArray(7)
        repeat(7_000) { val v = r.nextInt(7); assertTrue(v in 0..6); seen[v]++ }
        seen.forEach { assertTrue("roughly uniform: ${seen.toList()}", it in 800..1200) }
        repeat(1_000) { assertTrue(r.nextInt(-5, 5) in -5..4) }
        repeat(1_000) { assertTrue(r.nextInt(Int.MIN_VALUE, Int.MAX_VALUE) < Int.MAX_VALUE) }
        repeat(1_000) { val d = r.nextDouble(); assertTrue(d >= 0.0 && d < 1.0) }
        repeat(1_000) { assertTrue(r.nextBits(5) in 0..31) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonPositiveBoundIsRejected() { SplitMix64(1).nextInt(0) }

    @Test fun fisherYatesIsAPinnedPermutation() {
        val src = (0 until 10).toList()
        val out = src.fisherYates(SplitMix64(42))
        assertEquals(src, out.sorted())
        assertEquals(PINNED_SHUFFLE, out)
        assertEquals(listOf(1), listOf(1).fisherYates(SplitMix64(1)))
        assertEquals(emptyList<Int>(), emptyList<Int>().fisherYates(SplitMix64(1)))
    }

    @Test fun generatorIsReproducibleOnSplitMix() {
        val pool = TestAssets.wordData.entries.filter { it.category == "FOOD" }
        assertEquals(generateCrossword(pool, 15, emptySet(), SplitMix64(9)),
                     generateCrossword(pool, 15, emptySet(), SplitMix64(9)))
    }

    private companion object {
        val PINNED_SHUFFLE = listOf(3, 8, 4, 2, 9, 5, 7, 1, 0, 6)   // independently computed (Python reference)
    }
}
