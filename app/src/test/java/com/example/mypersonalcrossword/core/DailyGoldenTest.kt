package com.hag.mypersonalcrossword.core

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the Daily Puzzle for fixed dates against the shipped word list. Every
 * device and every build must produce exactly these grids, or players on
 * different versions get different "same for everyone" puzzles.
 *
 * If this fails:
 *  • word list changed (test.csv edited)? The fingerprint check fails first —
 *    re-pin both the fingerprint and the grids below; that is expected.
 *  • generator / RNG / Daily code changed ON PURPOSE? Bump
 *    DailyPuzzle.ALGORITHM_VERSION, then re-pin the grids below.
 *  • neither? Something made generation non-deterministic (hash-ordered
 *    iteration, stdlib Random/shuffle, locale) — fix that instead.
 *
 * Format per word: ANSWER:x,y,A|D (A = across, D = down), in board order.
 */
class DailyGoldenTest {

    private val entries = TestAssets.wordData.entries

    private val pinnedVersion     = 4
    private val pinnedFingerprint = -5448452892837725536L

    private val golden = linkedMapOf(
        "2026-01-01" to "DASTMALCHIAN:14,0,D FLIGHTINSTRUCTOR:16,2,D MELBOURNE:19,4,D ORANGE:12,5,A SKATEBOARDING:0,7,A " +
            "GUATEMALACITY:12,7,D OBEDIENCE:18,7,A GERALT:10,10,D TRANSFORMERSONE:12,10,A " +
            "GRILLEDCHEESE:0,11,A GROCERYCLERK:15,12,A CARACAS:18,12,D KAWAKAMI:7,13,A ISLINGTON:14,13,D " +
            "SECURITYGUARD:14,14,A YEARBOOK:21,14,D RACQUETBALL:4,15,A WATERFOUNTAIN:20,16,A " +
            "BROCCOLI:5,17,A NORMAN:14,17,A",
        "2026-09-27" to "FASHIONDESIGNER:16,0,D AIRTRAFFICCONTROLLER:12,3,D TEAMPROJECT:14,4,D RICOTTA:8,6,A " +
            "HOTELTRANSYLVANIA:22,7,D BALM:10,10,D TIBETANMASTIFF:19,10,D SHIGARAKI:4,11,A " +
            "LITTLEMERMAID:18,11,A MACNCHEESE:10,13,A TREASUREPLANET:19,14,A WEIGHTLIFTING:1,15,A " +
            "CHARLIEANDTHECHOCOLATEFACTORY:17,15,D SHANGTSUNG:16,16,A FITZGERALD:6,17,A LOBSTER:14,17,D " +
            "READALOUD:17,18,A PICKLEBALL:8,19,A SEWINGNEEDLE:16,21,A BASKETBALLTHREE:0,22,A",
        "2027-02-28" to "ROETHLISBERGER:16,0,D WONDERWOMAN:14,3,D KITEBOARDING:9,10,A TRANSFORMERSONE:11,10,D " +
            "BAGGAGEHANDLER:10,12,A URYU:9,14,D BIN:13,14,D CHARLIEANDTHECHOCOLATEFACTORY:15,14,D " +
            "CHEWTOY:18,14,D GRIFFITHS:8,15,A HORSEBACKRIDING:18,15,A SAKURA:21,15,D SECURITYGUARD:0,17,A " +
            "IRONWORKER:14,17,A THUMBTACK:13,18,D JOSEPHKINGOFDREAMS:8,19,A OBEDIENCE:9,19,D MIAMI:13,21,A " +
            "BLACKBOARD:0,22,A DESPICABLEME:15,23,A"
    )

    private fun compact(words: List<PlacedWord>) =
        words.joinToString(" ") { "${it.word}:${it.startX},${it.startY},${if (it.isHorizontal) "A" else "D"}" }

    @Test fun wordListIsTheOneTheGridsWerePinnedAgainst() {
        assertEquals("test.csv changed: re-pin pinnedFingerprint and the golden grids in DailyGoldenTest",
            pinnedFingerprint, wordDataFingerprint(entries))
    }

    @Test fun dailyGridsMatchTheGoldenSnapshot() {
        if (wordDataFingerprint(entries) != pinnedFingerprint) return   // reported by the test above
        assertEquals("ALGORITHM_VERSION changed: re-pin pinnedVersion and the golden grids",
            pinnedVersion, DailyPuzzle.ALGORITHM_VERSION)
        for ((date, expected) in golden) {
            val board = DailyPuzzle.generate(entries, date)
            assertEquals(
                "Daily $date changed. If generation changed on purpose, bump DailyPuzzle.ALGORITHM_VERSION " +
                    "and re-pin this snapshot; otherwise generation has become non-deterministic.",
                expected, compact(board))
            assertValidBoard(board)
        }
    }

    /** SplitMix64 reference vector (Vigna, seed 1234567): pins the PRNG itself. */
    @Test fun dailyRngMatchesReferenceSplitMix64() {
        val r = SplitMix64(1_234_567L)
        assertEquals(6_457_827_717_110_365_317L, r.nextRaw())
        assertEquals(3_203_168_211_198_807_973L, r.nextRaw())
        assertEquals(-8_629_252_141_511_181_193L, r.nextRaw())   // 9817491932198370423 unsigned
    }
}
