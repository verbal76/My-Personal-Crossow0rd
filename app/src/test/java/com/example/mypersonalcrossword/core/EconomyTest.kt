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
     * puzzle's reward was paid. Plays a puzzle through the same calls the app makes
     * (one charge per hint as it is used, then the completion award) and checks the
     * lifetime change against hard-coded expectations: reward − hints, unfloored.
     */
    @Test fun hintsAreChargedExactlyOnce() {
        val rewards = mapOf(Difficulty.EASY to 1, Difficulty.MEDIUM to 2, Difficulty.HARD to 4,
                            Difficulty.EXPERT to 8, Difficulty.GENIUS to 20)
        for ((diff, reward) in rewards) for (isDaily in listOf(false, true)) for (hints in 0..25) {
            var lifetime = 100
            repeat(hints) { lifetime -= Economy.hintCharges(1) }            // charged at use
            lifetime += Economy.lifetimeAwardOnWin(diff, isDaily)           // paid at completion
            val expectedReward = if (isDaily) 20 else reward
            val label = "$diff daily=$isDaily hints=$hints"
            assertEquals(label, 100 + expectedReward - hints, lifetime)
            assertEquals(label, lifetime - 100, Economy.lifetimeDelta(expectedReward, hints))
            // The results card's net matches the lifetime change until it would go negative.
            assertEquals(label, maxOf(0, expectedReward - hints), Economy.netPoints(diff, isDaily, hints))
            if (hints <= expectedReward) assertEquals(label, Economy.netPoints(diff, isDaily, hints), lifetime - 100)
        }
    }

    @Test fun lifetimeDeltaIsUnflooredWhileNetIsFloored() {
        assertEquals(-3, Economy.lifetimeDelta(Economy.pointsForDifficulty(Difficulty.EASY), 4))
        assertEquals(0, Economy.netPoints(Difficulty.EASY, false, 4))
        assertEquals(0, Economy.hintCharges(-2))
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
