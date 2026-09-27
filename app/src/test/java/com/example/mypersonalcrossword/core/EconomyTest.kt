package com.hag.mypersonalcrossword.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pins the game economy (see docs/ECONOMY.md). If one of these fails, a change
 * altered player progression — update the audit document before the test.
 */
class EconomyTest {

    @Test fun difficultyRewardsAreUnchanged() {
        assertEquals(listOf(1, 2, 4, 8, 20), Difficulty.entries.map { Economy.pointsForDifficulty(it) })
        assertEquals(listOf(8, 12, 15, 25, 50), Difficulty.entries.map { it.wordCount })
    }

    @Test fun dailyAndHintValuesAreUnchanged() {
        assertEquals(20, Economy.DAILY_POINTS)
        assertEquals(1, Economy.HINT_COST)
        assertEquals(20, DailyPuzzle.WORD_COUNT)
    }

    @Test fun vindictiveValuesMatchTheTutorial() {
        assertEquals(1, Economy.VIND_OWN_CORRECT)
        assertEquals(-1, Economy.VIND_OWN_WRONG)
        assertEquals(1, Economy.VIND_ASSIGNED_CORRECT)
        assertEquals(-2, Economy.VIND_ASSIGNED_WRONG)
        assertEquals(-1, Economy.VIND_PASS)
        assertEquals(0, Economy.vindictiveBank(-5))
        assertEquals(7, Economy.vindictiveBank(7))
    }

    /**
     * Regression: hints were charged twice — once when revealed and again when the
     * puzzle's reward was paid. The lifetime delta of a puzzle must equal the net
     * shown on the results card: reward − hints.
     */
    @Test fun hintsAreChargedExactlyOnce() {
        for (diff in Difficulty.entries) for (hints in 0..5) {
            val chargedAtUse = hints * Economy.HINT_COST
            val lifetimeDelta = Economy.lifetimeAwardOnWin(diff, isDaily = false) - chargedAtUse
            assertEquals(Economy.pointsForDifficulty(diff) - hints, lifetimeDelta)
        }
        assertEquals(20 - 3, Economy.lifetimeAwardOnWin(Difficulty.EXPERT, isDaily = true) - 3)
    }

    @Test fun netPointsNeverNegative() {
        assertEquals(0, Economy.netPoints(Difficulty.EASY, false, 4))
        assertEquals(17, Economy.netPoints(Difficulty.EASY, true, 3))
    }

    @Test fun streakMilestones() {
        assertEquals("On Fire! 🔥", checkStreakMilestone(3))
        assertNull(checkStreakMilestone(4))
        assertEquals("Still Going! 🌟 (20 in a row)", checkStreakMilestone(20))
    }
}
