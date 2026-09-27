package com.hag.mypersonalcrossword.core

import kotlin.random.Random

// ============================================================
// DAILY PUZZLE — deterministic by construction
// ============================================================
// Same UTC date + same word list  ⇒  same puzzle, on every device.
//
//  • The date key is computed from epoch millis in UTC (no local time zone).
//  • The pool is put into a canonical order first, so asset read order is irrelevant.
//  • Every random choice inside the generator comes from one Random seeded by
//    (algorithm version, date key, word-list fingerprint).
//  • Per-player used-word history is NOT consulted, so two players with different
//    histories still get the same grid — and playing the Daily never writes to
//    that history, so it can't feed back into future Dailies either.

object DailyPuzzle {
    /** Category label used for Daily saves and stats. */
    const val CATEGORY = "Daily"

    /** Words requested from the generator for a Daily grid. */
    const val WORD_COUNT = 20

    /** Bump when the generation algorithm changes in a way that alters output. */
    const val ALGORITHM_VERSION = 1

    /** ISO `yyyy-MM-dd` for the UTC calendar day containing [epochMillis]. */
    fun dateKey(epochMillis: Long): String {
        val days = Math.floorDiv(epochMillis, 86_400_000L)
        val (y, m, d) = civilFromDays(days)
        return "%04d-%02d-%02d".format(y, m, d)
    }

    fun seed(dateKey: String, fingerprint: Long): Long {
        var h = fnv1a(FNV_OFFSET, "daily-v$ALGORITHM_VERSION:")
        h = fnv1a(h, dateKey)
        return h xor fingerprint
    }

    fun generate(entries: List<RawEntry>, dateKey: String): List<PlacedWord> {
        val canonical = entries.sortedWith(compareBy({ it.category }, { it.answer }, { it.clue }))
        val rng       = Random(seed(dateKey, wordDataFingerprint(canonical)))
        return generateCrossword(canonical, WORD_COUNT, usedWords = emptySet(), rng = rng)
    }

    /**
     * Consecutive days, ending today (or yesterday, if today isn't solved yet),
     * on which a Daily was solved. [completedKeys] are yyyy-MM-dd date keys.
     */
    fun streak(completedKeys: Set<String>, todayKey: String): Int {
        val today = epochDayOf(todayKey) ?: return 0
        val done  = completedKeys.mapNotNull { epochDayOf(it) }.toHashSet()
        var day   = if (today in done) today else today - 1
        var n     = 0
        while (day in done) { n++; day-- }
        return n
    }

    /** Days since 1970-01-01 for a yyyy-MM-dd key, or null if malformed. */
    fun epochDayOf(key: String): Long? {
        val parts = key.split('-')
        if (parts.size != 3) return null
        val y = parts[0].toLongOrNull() ?: return null
        val m = parts[1].toLongOrNull()?.takeIf { it in 1..12 } ?: return null
        val d = parts[2].toLongOrNull()?.takeIf { it in 1..31 } ?: return null
        // Inverse of civilFromDays (Hinnant's days_from_civil).
        val yy  = if (m <= 2) y - 1 else y
        val era = Math.floorDiv(yy, 400L)
        val yoe = yy - era * 400
        val doy = (153 * (if (m > 2) m - 3 else m + 9) + 2) / 5 + d - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146_097 + doe - 719_468
    }

    /** Milliseconds until the next UTC midnight — for "new puzzle in Xh" copy. */
    fun millisUntilNext(epochMillis: Long): Long =
        86_400_000L - Math.floorMod(epochMillis, 86_400_000L)

    // Howard Hinnant's days-from-civil inverse. Valid for the whole proleptic Gregorian range.
    private fun civilFromDays(z0: Long): Triple<Int, Int, Int> {
        val z   = z0 + 719_468
        val era = Math.floorDiv(z, 146_097L)
        val doe = z - era * 146_097
        val yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
        val y   = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp  = (5 * doy + 2) / 153
        val d   = doy - (153 * mp + 2) / 5 + 1
        val m   = if (mp < 10) mp + 3 else mp - 9
        return Triple((if (m <= 2) y + 1 else y).toInt(), m.toInt(), d.toInt())
    }
}
