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

    private val pinnedVersion     = 2
    private val pinnedFingerprint = -5448452892837725536L

    private val golden = linkedMapOf(
        "2026-01-01" to "GROUPPROJECT:10,0,A JOSEPHKINGOFDREAMS:18,0,D RALPHBREAKSTHEINTERNET:22,1,D " +
            "GOLDFISHCRACKERS:12,2,A CHARLIEANDTHECHOCOLATEFACTORY:20,6,D PARKINGATTENDANT:1,11,A " +
            "TEENAGEMUTANTNINJATURTLES:16,11,D ENTREPRENEUR:7,13,A HOTELTRANSYLVANIA:0,15,A GEMSTONE:16,16,A " +
            "MISSIONIMPOSSIBLE:0,17,A NATIONALTREASURE:3,19,A TEAMPROJECT:14,21,A DOMSANTIAGO:10,23,A " +
            "PIRATESOFTHECARIBBEAN:1,25,A BEETLEJUICE:10,27,A HOWTOTRAINYOURDRAGON:13,29,A " +
            "ARTISTICSWIMMING:15,31,A BRUSSELSSPROUTS:10,33,A NIGHTATTHEMUSEUM:4,35,A",
        "2026-09-27" to "PIRATESOFTHECARIBBEAN:25,0,D HOWTOTRAINYOURDRAGON:21,2,D RALPHBREAKSTHEINTERNET:23,6,D " +
            "PICKLEBALLDOUBLES:27,10,D PIKACHU:15,14,A CHARLIEANDTHECHOCOLATEFACTORY:19,14,D ADDISABABA:19,16,A " +
            "AIRTRAFFICCONTROLLER:0,17,A LEASH:19,18,A LEWANDOWSKI:9,19,A FANNING:18,21,A DELIVERYDRIVER:11,23,A " +
            "LORDTHERINGS:14,25,A CROSSCOUNTRY:14,27,A JOSEPHKINGOFDREAMS:18,29,A HOTELTRANSYLVANIA:18,31,A " +
            "TEENAGEMUTANTNINJATURTLES:15,33,A MINIATUREPINSCHER:11,35,A NATIONALTREASURE:18,37,A " +
            "ARTISTICSWIMMING:17,39,A",
        "2027-02-28" to "RALPHBREAKSTHEINTERNET:22,0,D PIRATESOFTHECARIBBEAN:20,2,D HOWTOTRAINYOURDRAGON:26,5,D " +
            "PICKLEBALLDOUBLES:24,8,D MUSCAT:13,10,A CHARLIEANDTHECHOCOLATEFACTORY:16,10,D " +
            "TEENAGEMUTANTNINJATURTLES:18,10,D MARRAKESH:12,12,A LEADCLIMBING:16,14,A LEWANDOWSKI:6,15,A " +
            "EVE:16,16,A HOTELTRANSYLVANIA:0,17,A VIARDOT:12,19,A RIHANNA:14,21,A SAILBOATRACING:6,23,A " +
            "MEASURINGSPOON:5,25,A NATIONALTREASURE:12,27,A MINIATUREPINSCHER:12,29,A RECEPTIONCLERK:13,31,A " +
            "WAREHOUSEWORKER:15,33,A"
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
