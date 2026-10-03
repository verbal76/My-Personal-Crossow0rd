package com.hag.mypersonalcrossword.core

// ============================================================
// DAILY PUZZLE — deterministic by construction
// ============================================================
// Same UTC date + same word list  ⇒  same puzzle, on every device.
//
//  • The date key is computed from epoch millis in UTC (no local time zone).
//  • The pool is put into a canonical order first, so asset read order is irrelevant.
//  • Every random choice inside the generator comes from one SplitMix64 (Rng.kt,
//    in-house, so a Kotlin upgrade can't change it) seeded by
//    (algorithm version, date key, word-list fingerprint).
//  • The date key is built from ASCII digits by hand. String.format would use
//    the default locale's digits (Arabic, Persian, Bengali…), giving a different
//    key — a different board, a different save slot, and a second payout.
//  • Per-player used-word history is NOT consulted, so two players with different
//    histories still get the same grid — and playing the Daily never writes to
//    that history, so it can't feed back into future Dailies either.

object DailyPuzzle {
    /** Category label used for Daily saves and stats. */
    const val CATEGORY = "Daily"

    /** Words requested from the generator for a Daily grid. */
    const val WORD_COUNT = 20

    /**
     * Bump when the generation algorithm changes in a way that alters output.
     * v2: SplitMix64 + in-house Fisher–Yates; used words placeable at word edges;
     *     early stop tracks the best board's intersections.
     * v3: each day draws from a seeded [DAILY_POOL_SIZE]-entry sample (the same
     *     longest answers were in every Daily).
     * v4: board shape — random, centre-biased growth, a real area penalty and a
     *     stricter early stop (boards were combs around one long spine).
     */
    const val ALGORITHM_VERSION = 4

    /** Entries each day's Daily is built from (a seeded sample of the whole list). */
    const val DAILY_POOL_SIZE = 400

    /** ISO `yyyy-MM-dd` for the UTC calendar day containing [epochMillis]. */
    fun dateKey(epochMillis: Long): String {
        val days = Math.floorDiv(epochMillis, 86_400_000L)
        val (y, m, d) = civilFromDays(days)
        return pad(y, 4) + "-" + pad(m, 2) + "-" + pad(d, 2)
    }

    /**
     * Maps a stored date key to the canonical ASCII key, or null if it isn't a real
     * date. Accepts keys written by older builds with locale digits (e.g. "٢٠٢٦-١٠-٠٢"
     * from an Arabic device), so saved completion sets and Daily slot ids can be
     * migrated: any Unicode decimal digit is read by value.
     */
    fun normalizeKey(raw: String): String? {
        val ascii = StringBuilder(raw.length)
        for (ch in raw.trim()) {
            val digit = Character.digit(ch, 10)
            ascii.append(if (digit >= 0) ('0' + digit) else ch)
        }
        return epochDayOf(ascii.toString())?.let { dateKey(it * 86_400_000L) }
    }

    fun seed(dateKey: String, fingerprint: Long): Long {
        var h = fnv1a(FNV_OFFSET, "daily-v$ALGORITHM_VERSION:")
        h = fnv1a(h, dateKey)
        return h xor fingerprint
    }

    fun generate(entries: List<RawEntry>, dateKey: String): List<PlacedWord> {
        val key       = normalizeKey(dateKey) ?: dateKey   // a legacy-digit key still gets its day's board
        val canonical = entries.sortedWith(compareBy({ it.category }, { it.answer }, { it.clue }))
        val rng       = SplitMix64(seed(key, wordDataFingerprint(canonical)))
        // Each day draws from its own sample of the list. Over the whole list the
        // board scoring always found room for the very longest answers, so the same
        // few (PIRATESOFTHECARIBBEAN, …) appeared in every single Daily.
        val pool = if (canonical.size <= DAILY_POOL_SIZE) canonical
                   else canonical.fisherYates(rng).take(DAILY_POOL_SIZE)
        return generateCrossword(pool, WORD_COUNT, usedWords = emptySet(), rng = rng)
    }

    /**
     * Consecutive days, ending today (or yesterday, if today isn't solved yet),
     * on which a Daily was solved. [completedKeys] are yyyy-MM-dd date keys.
     */
    fun streak(completedKeys: Set<String>, todayKey: String): Int {
        val today = epochDayOf(normalizeKey(todayKey) ?: return 0) ?: return 0
        val done  = completedKeys.mapNotNull { k -> normalizeKey(k)?.let { epochDayOf(it) } }.toHashSet()
        var day   = if (today in done) today else today - 1
        var n     = 0
        while (day in done) { n++; day-- }
        return n
    }

    /**
     * Days since 1970-01-01 for a canonical yyyy-MM-dd key (ASCII digits, zero-padded,
     * a real calendar date), or null otherwise. Strict, so "2026-02-31", "2026-3-3"
     * and "+2026-03-03" can't alias a real day. Use [normalizeKey] for stored keys.
     */
    fun epochDayOf(key: String): Long? {
        if (!CANONICAL_KEY.matches(key)) return null
        val y = key.substring(0, 4).toLong()
        val m = key.substring(5, 7).toLong().takeIf { it in 1..12 } ?: return null
        val d = key.substring(8, 10).toLong().takeIf { it in 1..31 } ?: return null
        // Inverse of civilFromDays (Hinnant's days_from_civil).
        val yy  = if (m <= 2) y - 1 else y
        val era = Math.floorDiv(yy, 400L)
        val yoe = yy - era * 400
        val doy = (153 * (if (m > 2) m - 3 else m + 9) + 2) / 5 + d - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        val day = era * 146_097 + doe - 719_468
        // Day 31 of a 30-day month (or Feb 30) would roll over; the round trip catches it.
        return day.takeIf { dateKey(it * 86_400_000L) == key }
    }

    private val CANONICAL_KEY = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")

    /** Zero-padded ASCII decimal, independent of the default locale. */
    private fun pad(n: Int, width: Int): String = n.toString().padStart(width, '0')

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
