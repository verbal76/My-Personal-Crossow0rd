package com.hag.mypersonalcrossword.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
            vindTimerSecs = 15, vindSecondsLeft = 7, bgArgb = 0xFF112233.toInt(), bgImage = "sunset.webp"
        )
        assertEquals(s, SessionCodec.decode(SessionCodec.encode(s)))
    }

    @Test fun vindictiveClockRemainingIsKeptAndBounded() {
        val base = PuzzleSession(GameMode.VINDICTIVE, "PETS", Difficulty.EASY, words, vindTimerSecs = 30)
        assertEquals(null, SessionCodec.decode(SessionCodec.encode(base))!!.vindSecondsLeft)
        assertEquals(0, SessionCodec.decode(SessionCodec.encode(base.copy(vindSecondsLeft = 0)))!!.vindSecondsLeft)
        // A tampered value can't exceed the clock or go negative.
        val raw = SessionCodec.encode(base.copy(vindSecondsLeft = 12))
        assertEquals(30, SessionCodec.decode(raw.replace("vleft=12", "vleft=999"))!!.vindSecondsLeft)
        assertEquals(0, SessionCodec.decode(raw.replace("vleft=12", "vleft=-4"))!!.vindSecondsLeft)
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

    @Test fun negativeCountsAndBogusTimerAreClamped() {
        val raw = "v2\nmeta:hints=-5§elapsed=-9§streak=-2§vtimer=0§t1=-3§t2=-1§th1=-4§th2=-7§v1=-3§v2=-6\n" +
                  "words:CAT§c§0§0§true§1\ninputs:\nrevealed:"
        val s = SessionCodec.decode(raw)!!
        assertEquals(0, s.hintsUsed)
        assertEquals(0L, s.elapsedSeconds)
        assertEquals(0, s.streak)
        assertEquals(TeamState(0, 0, 0, 0, 0), s.team)
        assertEquals(30, s.vindTimerSecs)
        // Vindictive match scores really can go negative; they are left alone.
        assertEquals(-3, s.vind.p1Score); assertEquals(-6, s.vind.p2Score)
        for (t in listOf(15, 30, 60)) {
            assertEquals(t, SessionCodec.decode(raw.replace("vtimer=0", "vtimer=$t"))!!.vindTimerSecs)
        }
        assertEquals(30, SessionCodec.decode(raw.replace("vtimer=0", "vtimer=45"))!!.vindTimerSecs)
        assertEquals(30, SessionCodec.decode(raw.replace("vtimer=0", "vtimer=x"))!!.vindTimerSecs)
    }

    /** Regression: a skipped word shifted the assigned-clue index onto the wrong word. */
    @Test fun malformedWordDropsTheAssignedClueIndex() {
        val dog = PlacedWord("DOG", "Barker", 5, 5, true, 2, "PETS")
        // CAT (index 1) is assigned; once DOG is skipped, index 1 would be COW.
        val s = PuzzleSession(GameMode.VINDICTIVE, "PETS", Difficulty.EASY, listOf(dog) + words,
            vind = VindState(VindicativePhase.OPPONENT_WAIT, 1, 0, 0, 1))
        val intact = SessionCodec.decode(SessionCodec.encode(s))!!
        assertEquals("CAT", intact.words[intact.vind.assignedIndex].word)
        assertEquals(VindicativePhase.OPPONENT_WAIT, intact.vind.phase)

        val back = SessionCodec.decode(SessionCodec.encode(s).replace("DOG§Barker", "D0G§Barker"))!!
        assertEquals(listOf("CAT", "COW"), back.words.map { it.word })
        assertEquals(-1, back.vind.assignedIndex)
        assertEquals(VindicativePhase.ASSIGN_CLUE, back.vind.phase)
    }

    // ── SaveSlot ────────────────────────────────────────────────────────────

    /** Regression: every 4+ category combo shared the label "N Categories", and A+B ≠ B+A. */
    @Test fun combinedSlotsAreKeyedBySortedCategorySet() {
        fun slot(cats: List<String>, label: String = if (cats.size <= 3) cats.joinToString("+") else "${cats.size} Categories") =
            PuzzleSession(GameMode.SINGLE, label, Difficulty.HARD, words, combined = cats).slot
        val a = slot(listOf("FOOD", "PETS", "CITIES", "SPORTS"))
        val b = slot(listOf("FOOD", "PETS", "CITIES", "MUSIC"))
        val c = slot(listOf("SPORTS", "CITIES", "PETS", "FOOD"))
        assertEquals("CITIES+FOOD+PETS+SPORTS__HARD", a.id)
        assertTrue(a.id != b.id)
        assertEquals("order of selection doesn't matter", a, c)
        assertEquals(slot(listOf("FOOD", "PETS")), slot(listOf("PETS", "FOOD")))
        assertEquals(listOf("CITIES", "FOOD", "PETS", "SPORTS"), a.combinedCategories)
        assertTrue(a.isCombined)
        // Modes stay separate.
        val team = PuzzleSession(GameMode.TEAM, "4 Categories", Difficulty.HARD, words,
            combined = listOf("FOOD", "PETS", "CITIES", "SPORTS")).slot
        assertEquals("TEAM::CITIES+FOOD+PETS+SPORTS__HARD", team.id)
        for (s in listOf(a, b, c, team)) assertEquals(s, SaveSlot.parse(s.id))
    }

    @Test fun forPuzzleMatchesSessionSlot() {
        assertEquals(SaveSlot(GameMode.SINGLE, "FOOD", Difficulty.EASY), SaveSlot.forPuzzle(GameMode.SINGLE, "FOOD", Difficulty.EASY))
        assertEquals(SaveSlot.daily("2026-09-27"),
            SaveSlot.forPuzzle(GameMode.DAILY, DailyPuzzle.CATEGORY, Difficulty.EXPERT, dailyKey = "2026-09-27"))
        assertEquals("FOOD+PETS__EASY", SaveSlot.forPuzzle(GameMode.SINGLE, "PETS+FOOD", Difficulty.EASY, listOf("PETS", "FOOD", "PETS")).id)
        assertEquals(SaveSlot(GameMode.SINGLE, "FOOD", Difficulty.EASY).combinedCategories, emptyList<String>())
    }

    @Test fun everyLegacySlotIdStillParses() {
        val legacy = mapOf(
            "FOOD__HARD"                    to SaveSlot(GameMode.SINGLE, "FOOD", Difficulty.HARD),
            "FOOD+CITIES__EASY"             to SaveSlot(GameMode.SINGLE, "FOOD+CITIES", Difficulty.EASY),
            "4 Categories__GENIUS"          to SaveSlot(GameMode.SINGLE, "4 Categories", Difficulty.GENIUS),
            "Daily__EXPERT"                 to SaveSlot(GameMode.SINGLE, "Daily", Difficulty.EXPERT),
            "TEAM::FOOD__MEDIUM"            to SaveSlot(GameMode.TEAM, "FOOD", Difficulty.MEDIUM),
            "VINDICTIVE::5 Categories__HARD" to SaveSlot(GameMode.VINDICTIVE, "5 Categories", Difficulty.HARD),
            "DAILY::2026-09-27__EXPERT"     to SaveSlot.daily("2026-09-27")
        )
        for ((id, slot) in legacy) {
            assertEquals(id, slot, SaveSlot.parse(id))
            assertEquals(id, slot.id)
        }
        assertTrue(!SaveSlot.parse("4 Categories__GENIUS")!!.isCombined)
    }


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
