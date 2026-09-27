package com.hag.mypersonalcrossword.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionCodecTest {

    private val words = listOf(
        PlacedWord("CAT", "Purring pet", 0, 0, true, 1, "PETS"),
        PlacedWord("COW", "Farm animal (no spaces)", 0, 0, false, 1, "NATURE")
    )

    @Test fun fullSessionRoundTrips() {
        val s = PuzzleSession(
            mode = GameMode.VINDICTIVE, category = "PETS+NATURE", difficulty = Difficulty.HARD,
            words = words, inputs = mapOf(Pair(0, 0) to 'C', Pair(1, 0) to 'X'),
            revealed = setOf(Pair(0, 0)), combined = listOf("PETS", "NATURE"),
            elapsedSeconds = 125, hintsUsed = 2, streak = 3, player2 = "Sam",
            team = TeamState(1, 2, 1, 3, 4),
            vind = VindState(VindicativePhase.OPPONENT_WAIT, 1, -2, 5, 1),
            vindTimerSecs = 15, bgArgb = 0xFF112233.toInt(), bgImage = "sunset.webp"
        )
        assertEquals(s, SessionCodec.decode(SessionCodec.encode(s)))
    }

    @Test fun dailySessionKeepsItsDateAndSlot() {
        val s = PuzzleSession(GameMode.DAILY, DailyPuzzle.CATEGORY, Difficulty.EXPERT, words, dailyKey = "2026-09-27")
        val back = SessionCodec.decode(SessionCodec.encode(s))!!
        assertEquals("2026-09-27", back.dailyKey)
        assertEquals(SaveSlot.daily("2026-09-27"), back.slot)
    }

    @Test fun malformedWordSegmentIsSkippedNotFatal() {
        val raw = "CAT§Purring pet§0§0§true§1|BROKEN§x|COW§Farm animal§0§0§false§1"
        assertEquals(listOf("CAT", "COW"), SessionCodec.decodeWords(raw).map { it.word })
    }

    @Test fun legacySixFieldWordsStillParse() {
        val w = SessionCodec.decodeWords("CAT§Purring pet§2§3§true§4").single()
        assertEquals(PlacedWord("CAT", "Purring pet", 2, 3, true, 4, ""), w)
    }

    @Test fun badInputsAreDropped() {
        val m = SessionCodec.decodeInputs("0|0|C;x|1|A;1|1|;2|2|ab;3|3|?;4|4|Z")
        assertEquals(mapOf(Pair(0, 0) to 'C', Pair(4, 4) to 'Z'), m)
    }

    @Test fun notASessionDecodesToNull() {
        assertNull(SessionCodec.decode(""))
        assertNull(SessionCodec.decode("v1\nwords:"))
        assertNull(SessionCodec.decode("v2\nmeta:mode=SINGLE\nwords:\ninputs:\nrevealed:"))
    }

    @Test fun opponentWaitWithoutClueFallsBackToAssign() {
        val s = PuzzleSession(GameMode.VINDICTIVE, "PETS", Difficulty.EASY, words,
            vind = VindState(VindicativePhase.OPPONENT_WAIT, 1, 0, 0, -1))
        assertEquals(VindicativePhase.ASSIGN_CLUE, SessionCodec.decode(SessionCodec.encode(s))!!.vind.phase)
    }

    // ── SaveSlot ────────────────────────────────────────────────────────────

    @Test fun singleSlotKeepsLegacyIdFormat() {
        val slot = SaveSlot(GameMode.SINGLE, "FOOD", Difficulty.HARD)
        assertEquals("FOOD__HARD", slot.id)
        assertEquals(slot, SaveSlot.parse("FOOD__HARD"))
    }

    @Test fun multiplayerSlotsAreNamespacedAwayFromSingle() {
        val team = SaveSlot(GameMode.TEAM, "FOOD", Difficulty.HARD)
        val vind = SaveSlot(GameMode.VINDICTIVE, "FOOD", Difficulty.HARD)
        val solo = SaveSlot(GameMode.SINGLE, "FOOD", Difficulty.HARD)
        assertEquals(3, setOf(team.id, vind.id, solo.id).size)
        assertEquals(team, SaveSlot.parse(team.id))
        assertEquals(vind, SaveSlot.parse(vind.id))
    }

    @Test fun combinedLabelsSurviveSlotRoundTrip() {
        val slot = SaveSlot(GameMode.TEAM, "FOOD+CITIES+PETS", Difficulty.GENIUS)
        assertEquals(slot, SaveSlot.parse(slot.id))
    }

    @Test fun garbageSlotIdsAreRejected() {
        assertNull(SaveSlot.parse("nonsense"))
        assertNull(SaveSlot.parse("FOOD__IMPOSSIBLE"))
        assertNull(SaveSlot.parse("BOGUS::FOOD__EASY"))
        assertNull(SaveSlot.parse("__EASY"))
    }
}
