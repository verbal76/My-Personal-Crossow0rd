package com.hag.mypersonalcrossword.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyTest {

    private val entries = TestAssets.wordData.entries

    @Test fun dateKeyIsUtcCalendarDay() {
        assertEquals("1970-01-01", DailyPuzzle.dateKey(0L))
        assertEquals("2001-09-09", DailyPuzzle.dateKey(1_000_000_000_000L))   // 2001-09-09T01:46:40Z
        assertEquals("2024-02-29", DailyPuzzle.dateKey(1_709_164_800_000L))   // leap day, 00:00Z
        assertEquals("2024-02-29", DailyPuzzle.dateKey(1_709_251_199_999L))   // 23:59:59.999Z
        assertEquals("2024-03-01", DailyPuzzle.dateKey(1_709_251_200_000L))
        assertEquals("1969-12-31", DailyPuzzle.dateKey(-1L))
    }

    @Test fun samDateSameWordListGivesIdenticalPuzzle() {
        val a = DailyPuzzle.generate(entries, "2026-09-27")
        val b = DailyPuzzle.generate(entries, "2026-09-27")
        assertEquals(a, b)
        assertEquals(DailyPuzzle.WORD_COUNT, a.size)
        assertValidBoard(a)
    }

    @Test fun inputOrderDoesNotMatter() {
        val a = DailyPuzzle.generate(entries, "2026-09-27")
        val b = DailyPuzzle.generate(entries.reversed(), "2026-09-27")
        val c = DailyPuzzle.generate(entries.shuffled(kotlin.random.Random(99)), "2026-09-27")
        assertEquals(a, b)
        assertEquals(a, c)
    }

    @Test fun differentDaysGiveDifferentPuzzles() {
        val a = DailyPuzzle.generate(entries, "2026-09-27").map { it.word }.toSet()
        val b = DailyPuzzle.generate(entries, "2026-09-28").map { it.word }.toSet()
        assertNotEquals(a, b)
    }

    @Test fun wordListChangeChangesFingerprint() {
        val fp1 = wordDataFingerprint(entries)
        val fp2 = wordDataFingerprint(entries.drop(1))
        assertNotEquals(fp1, fp2)
        assertEquals(fp1, wordDataFingerprint(entries.reversed()))
    }

    @Test fun millisUntilNextMidnight() {
        assertEquals(86_400_000L, DailyPuzzle.millisUntilNext(0L))
        assertEquals(1L, DailyPuzzle.millisUntilNext(86_399_999L))
        assertTrue(DailyPuzzle.millisUntilNext(System.currentTimeMillis()) in 1..86_400_000L)
    }

    @Test fun dailySlotIsDistinctPerDateAndNotASingleSlot() {
        val s = SaveSlot.daily("2026-09-27")
        assertEquals(GameMode.DAILY, s.mode)
        assertEquals(s, SaveSlot.parse(s.id))
        assertNotEquals(SaveSlot.daily("2026-09-28").id, s.id)
    }
}
