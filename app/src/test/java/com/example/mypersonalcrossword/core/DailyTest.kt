package com.hag.mypersonalcrossword.core

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
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

    @Test fun epochDayRoundTrips() {
        for (day in listOf(-1L, 0L, 11_574L, 19_782L, 19_783L, 20_000L, 73_000L)) {
            val key = DailyPuzzle.dateKey(day * 86_400_000L)
            assertEquals(key, day, DailyPuzzle.epochDayOf(key))
        }
        assertEquals(null, DailyPuzzle.epochDayOf("2026-13-01"))
        assertEquals(null, DailyPuzzle.epochDayOf("nonsense"))
    }

    /**
     * Regression: the key was built with String.format, which uses the default
     * locale's digits — an Arabic or Bengali device got "٢٠٢٦-…", a different
     * board, a different save slot and a second Daily payout.
     */
    @Test fun dateKeyIsAsciiInEveryLocale() {
        val ms = 1_790_000_000_000L
        val saved = Locale.getDefault()
        val expected = try { Locale.setDefault(Locale.US); DailyPuzzle.dateKey(ms) } finally { Locale.setDefault(saved) }
        assertEquals("2026-09-21", expected)
        for (tag in listOf("ar", "ar-EG", "fa-IR", "bn-BD", "mr-IN", "my-MM", "ne-NP", "th-TH-u-nu-thai", "tr-TR")) {
            try {
                Locale.setDefault(Locale.forLanguageTag(tag))
                assertEquals(tag, expected, DailyPuzzle.dateKey(ms))
                assertEquals(tag, "2026-09-21__EXPERT", SaveSlot.daily(DailyPuzzle.dateKey(ms)).id.substringAfter("::"))
            } finally {
                Locale.setDefault(saved)
            }
        }
    }

    @Test fun normalizeKeyMigratesLegacyDigitKeys() {
        assertEquals("2026-09-21", DailyPuzzle.normalizeKey("2026-09-21"))
        assertEquals("2026-09-21", DailyPuzzle.normalizeKey("\u0662\u0660\u0662\u0666-\u0660\u0669-\u0662\u0661"))   // Arabic-Indic
        assertEquals("2026-09-21", DailyPuzzle.normalizeKey("\u06F2\u06F0\u06F2\u06F6-\u06F0\u06F9-\u06F2\u06F1"))   // Persian
        assertEquals("2026-09-21", DailyPuzzle.normalizeKey("\u09E8\u09E6\u09E8\u09EC-\u09E6\u09EF-\u09E8\u09E7"))   // Bengali
        assertEquals("2026-09-21", DailyPuzzle.normalizeKey("\u0E52\u0E50\u0E52\u0E56-\u0E50\u0E59-\u0E52\u0E51"))   // Thai
        assertEquals("2026-09-21", DailyPuzzle.normalizeKey(" 2026-09-21 "))
        assertNull(DailyPuzzle.normalizeKey("2026-02-31"))
        assertNull(DailyPuzzle.normalizeKey("garbage"))
        assertNull(DailyPuzzle.normalizeKey(""))
        // A legacy key generates the same board as its canonical form.
        assertEquals(DailyPuzzle.generate(entries, "2026-09-21"),
            DailyPuzzle.generate(entries, "\u0662\u0660\u0662\u0666-\u0660\u0669-\u0662\u0661"))
    }

    @Test fun epochDayOfIsStrict() {
        assertEquals(20_515L, DailyPuzzle.epochDayOf("2026-03-03"))
        for (bad in listOf("2026-02-31", "2026-02-29", "2026-04-31", "2026-3-3", "+2026-03-03", " 2026-03-03",
                           "2026-03-03 ", "02026-03-03", "2026-00-10", "2026-13-01", "2026-01-00", "-001-01-01",
                           "\u0662\u0660\u0662\u0666-\u0660\u0663-\u0660\u0663")) {
            assertNull(bad, DailyPuzzle.epochDayOf(bad))
        }
        assertEquals(DailyPuzzle.epochDayOf("2024-02-28")!! + 1, DailyPuzzle.epochDayOf("2024-02-29"))
    }

    @Test fun streakIgnoresAliasesButCountsLegacyDigitKeys() {
        // "2026-02-30" and "2026-3-3" used to alias real days and extend the streak.
        assertEquals(1, DailyPuzzle.streak(setOf("2026-03-02", "2026-02-30", "2026-3-3"), "2026-03-02"))
        val arabicYesterday = "\u0662\u0660\u0662\u0666-\u0660\u0663-\u0660\u0662"
        assertEquals(2, DailyPuzzle.streak(setOf(arabicYesterday, "2026-03-03"), "2026-03-03"))
    }

    @Test fun dailyStreakCountsConsecutiveDays() {
        val done = setOf("2026-09-24", "2026-09-25", "2026-09-26", "2026-09-20")
        assertEquals("today not played yet: streak runs through yesterday", 3, DailyPuzzle.streak(done, "2026-09-27"))
        assertEquals(4, DailyPuzzle.streak(done + "2026-09-27", "2026-09-27"))
        assertEquals("gap breaks the streak", 0, DailyPuzzle.streak(done, "2026-09-29"))
        assertEquals("across a month boundary", 3,
            DailyPuzzle.streak(setOf("2026-02-27", "2026-02-28", "2026-03-01"), "2026-03-01"))
    }

    /** The Daily must feel new each day: no answer may dominate a month of boards. */
    @Test fun dailyAnswersVaryFromDayToDay() {
        val days = (1..30).map { "2026-10-%02d".format(Locale.ROOT, it) }
        val counts = days.flatMap { d -> DailyPuzzle.generate(entries, d).map { it.word } }
            .groupingBy { it }.eachCount()
        val (worst, n) = counts.maxByOrNull { it.value }!!.toPair()
        // Before v3 four answers were in all 30 boards.
        assertTrue("$worst is in $n of 30 Dailies", n <= 12)
        assertTrue("only ${counts.size} distinct answers in 30 Dailies", counts.size >= 300)
    }
}
