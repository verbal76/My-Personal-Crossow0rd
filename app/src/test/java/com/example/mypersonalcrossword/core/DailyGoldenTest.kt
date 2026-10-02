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

    private val pinnedVersion     = 3
    private val pinnedFingerprint = -5448452892837725536L

    private val golden = linkedMapOf(
        "2026-01-01" to "JOSEPHKINGOFDREAMS:13,0,D PANCAKES:8,6,A CHARLIEANDTHECHOCOLATEFACTORY:11,6,D " +
            "BOACONSTRICTOR:17,6,D COLETRAIN:5,8,A ELLIOTT:9,10,A GUATEMALACITY:19,11,D SEWINGNEEDLE:4,12,A " +
            "PERCYJACKSON:0,14,A COMBINEDCLIMBING:15,14,D DRAGONITE:11,15,A SESAMESTREET:0,16,A " +
            "NURSEHELPER:2,18,A GRILLEDCHEESE:3,20,A CHAUCER:11,22,A DOMSANTIAGO:1,23,A LOLLIPOP:11,24,A " +
            "PHILADELPHIA:0,25,A POMERANIAN:8,27,A AVENGERS:11,29,A",
        "2026-09-27" to "TIBETANMASTIFF:9,0,A TEENAGEMUTANTNINJATURTLES:13,0,D GAELICFOOTBALL:11,2,A " +
            "CAMPCOUNSELOR:12,4,A TRACKANDFIELD:3,6,A THERMOMETER:5,9,A MINECRAFTMOVIE:11,9,D " +
            "MENINBLACK:9,11,A TECHNICIAN:9,13,A DOGTRAINER:6,15,A GAMEDEVELOPER:17,16,D " +
            "SESAMESTREET:0,17,A PLANKTON:15,17,A PIRATESOFTHECARIBBEAN:15,17,D JUNICHIRO:3,19,A " +
            "ARTISTICSWIMMING:19,19,D VERLANDER:11,20,A SPINELLI:4,21,A ELLIE:11,22,A LORDTHERINGS:2,24,A",
        "2027-02-28" to "PIRATESOFTHECARIBBEAN:18,0,D SUPERINTENDENT:10,5,A NIGHTATTHEMUSEUM:16,5,D RAGDOLL:14,7,A " +
            "LOVECRAFT:8,9,A WAREHOUSEWORKER:0,11,A RALPHBREAKSTHEINTERNET:14,11,D BEAUTYANDBEAST:22,11,D " +
            "WRECKITRALPH:5,13,A PARKINGATTENDANT:20,14,D JELLYFISH:6,15,A BLUEBERRY:14,16,A " +
            "ROETHLISBERGER:1,17,A KIZARU:11,19,A SCIENTIST:7,21,A CHRONOSTASIS:13,23,A DIDDYKONG:13,25,A " +
            "BOACONSTRICTOR:3,27,A WORKSHEET:12,29,A HORSEBACKRIDING:10,31,A"
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
