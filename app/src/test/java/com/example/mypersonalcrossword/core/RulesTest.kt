package com.hag.mypersonalcrossword.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesTest {

    // ── Vindictive ──────────────────────────────────────────────────────────

    @Test fun ownAnswerScoresAndMovesToAssign() {
        val (ok, o1) = VindictiveRules.onOwnAnswer(VindState(currentPlayer = 1), correct = true)
        assertEquals(VindOutcome.OWN_CORRECT, o1)
        assertEquals(VindState(VindicativePhase.ASSIGN_CLUE, 1, 0, 1), ok)
        val (bad, o2) = VindictiveRules.onOwnAnswer(VindState(currentPlayer = 0), correct = false)
        assertEquals(VindOutcome.OWN_WRONG, o2)
        assertEquals(VindState(VindicativePhase.ASSIGN_CLUE, 0, -1, 0), bad)
    }

    @Test fun assignHandsTurnToOpponent() {
        val s = VindictiveRules.onAssign(VindState(VindicativePhase.ASSIGN_CLUE, 0), 4)
        assertEquals(VindState(VindicativePhase.OPPONENT_WAIT, 1, 0, 0, 4), s)
    }

    @Test fun assignedAnswerScoresAnswererWhoThenAssigns() {
        val waiting = VindState(VindicativePhase.OPPONENT_WAIT, 1, 3, 3, 4)
        val (right, _) = VindictiveRules.onAssignedAnswer(waiting, true)
        assertEquals(VindState(VindicativePhase.ASSIGN_CLUE, 1, 3, 4, -1), right)
        val (wrong, out) = VindictiveRules.onAssignedAnswer(waiting, false)
        assertEquals(VindOutcome.ASSIGNED_WRONG, out)
        assertEquals(VindState(VindicativePhase.ASSIGN_CLUE, 1, 3, 1, -1), wrong)
    }

    /** Regression: Pass must cost exactly 1 (tutorial) — the old timeout path silently took 2. */
    @Test fun passCostsOnePoint() {
        val (s, out) = VindictiveRules.onPass(VindState(VindicativePhase.OPPONENT_WAIT, 0, 5, 5, 2))
        assertEquals(VindOutcome.PASSED, out)
        assertEquals(VindState(VindicativePhase.ASSIGN_CLUE, 0, 4, 5, -1), s)
    }

    @Test(expected = IllegalArgumentException::class)
    fun passOutsideOpponentWaitIsRejected() { VindictiveRules.onPass(VindState()) }

    /** Regression: online both devices ran the countdown and double-applied the penalty. */
    @Test fun onlyTheAnswerersDeviceOwnsTheTimer() {
        val s = VindState(VindicativePhase.OPPONENT_WAIT, currentPlayer = 1, assignedIndex = 0)
        assertTrue(VindictiveRules.ownsTimer(s, isOnline = true, myIndex = 1))
        assertFalse(VindictiveRules.ownsTimer(s, isOnline = true, myIndex = 0))
        assertTrue(VindictiveRules.ownsTimer(s, isOnline = false, myIndex = 0))
        assertFalse(VindictiveRules.ownsTimer(s.copy(phase = VindicativePhase.ASSIGN_CLUE), false, 0))
    }

    @Test fun winnerAndTies() {
        assertEquals(0, VindictiveRules.winner(VindState(p1Score = 3, p2Score = 1)))
        assertEquals(1, VindictiveRules.winner(VindState(p1Score = -1, p2Score = 0)))
        assertNull(VindictiveRules.winner(VindState(p1Score = 2, p2Score = 2)))
    }

    // ── Team ────────────────────────────────────────────────────────────────

    /** Regression: turn only passed on a correct answer, contradicting the tutorial. */
    @Test fun teamTurnAlternatesOnEveryAttempt() {
        var s = TeamState(turn = 1)
        s = TeamRules.onAttempt(s, correct = false)
        assertEquals(TeamState(0, 0, 0), s)
        s = TeamRules.onAttempt(s, correct = true)
        assertEquals(TeamState(1, 0, 1), s)
        s = TeamRules.onAttempt(s, correct = true)
        assertEquals(TeamState(1, 1, 0), s)
    }

    @Test fun teamHintsAreChargedToWhoeverHasTheTurn() {
        assertEquals(TeamState(turn = 1, p2Hints = 1), TeamRules.onHint(TeamState(turn = 1)))
        assertEquals(TeamState(turn = 0, p1Hints = 1), TeamRules.onHint(TeamState(turn = 0)))
    }

    @Test fun teamMvp() {
        assertEquals(1, TeamRules.mvp(TeamState(p1Correct = 2, p2Correct = 5)))
        assertNull(TeamRules.mvp(TeamState(p1Correct = 2, p2Correct = 2)))
    }

    // ── Hints ───────────────────────────────────────────────────────────────

    private val cat = PlacedWord("CAT", "", 0, 0, true)

    @Test fun hintPrefersSelectedCellThenReadingOrder() {
        assertEquals(Pair(Pair(1, 0), 'A'), pickHintCell(cat, emptyMap(), preferred = Pair(1, 0)))
        assertEquals(Pair(Pair(0, 0), 'C'), pickHintCell(cat, emptyMap(), preferred = Pair(9, 9)))
        // A wrong letter counts as unrevealed.
        assertEquals(Pair(Pair(0, 0), 'C'), pickHintCell(cat, mapOf(Pair(0, 0) to 'X')))
        assertEquals(Pair(Pair(1, 0), 'A'), pickHintCell(cat, mapOf(Pair(0, 0) to 'C')))
    }

    @Test fun noHintForASolvedWord() {
        assertNull(pickHintCell(cat, mapOf(Pair(0, 0) to 'C', Pair(1, 0) to 'A', Pair(2, 0) to 'T')))
    }

    // ── Online merge ────────────────────────────────────────────────────────

    private val solution = solutionOf(listOf(cat, PlacedWord("COW", "", 0, 0, false)))

    @Test fun remoteCorrectLetterOverridesLocalWrongLetter() {
        val merged = mergeRemoteInputs(mapOf(Pair(1, 0) to 'X'), mapOf(Pair(1, 0) to 'A'), solution)
        assertEquals(mapOf(Pair(1, 0) to 'A'), merged)
    }

    @Test fun remoteNeverErasesOrPlantsWrongLetters() {
        val local = mapOf(Pair(0, 1) to 'O', Pair(2, 0) to 'Q')
        val merged = mergeRemoteInputs(local, mapOf(Pair(0, 2) to 'Z', Pair(9, 9) to 'A'), solution)
        assertSame(local, merged)
    }

    @Test fun remoteLettersForNewCellsAreAdded() {
        val merged = mergeRemoteInputs(emptyMap(), mapOf(Pair(0, 0) to 'C', Pair(0, 2) to 'W'), solution)
        assertEquals(mapOf(Pair(0, 0) to 'C', Pair(0, 2) to 'W'), merged)
    }

    // ── Taunts ──────────────────────────────────────────────────────────────

    @Test fun tauntsNameTheRightPeopleAndNeverCrash() {
        // Index 7 is "$p1, honey… no." — p1 is the player who got it wrong.
        assertEquals("Sam, honey… no.", vindictiveTaunt(7, "Sam", "Kev"))
        assertEquals("Kev knew you didn't know that. 😏", vindictiveTaunt(0, "Sam", "Kev"))
        for (i in -3..(TAUNT_COUNT * 2)) assertTrue(vindictiveTaunt(i, "A", "B").isNotBlank())
    }
}
