package com.hag.mypersonalcrossword.core

// ============================================================
// ECONOMY — every number that moves a player's score lives here.
// See docs/ECONOMY.md for the audit behind these values. Changing any
// of them changes progression for existing players; update the audit first.
// ============================================================

object Economy {
    /** Lifetime points charged the moment a hint letter is revealed. */
    const val HINT_COST = 1

    /** Flat reward for completing the Daily Puzzle (once per UTC day). */
    const val DAILY_POINTS = 20

    // Vindictive round scoring (in-match points, banked at the end, floored at 0).
    const val VIND_OWN_CORRECT      = 1
    const val VIND_OWN_WRONG        = -1
    const val VIND_ASSIGNED_CORRECT = 1
    const val VIND_ASSIGNED_WRONG   = -2
    const val VIND_PASS             = -1

    fun pointsForDifficulty(diff: Difficulty): Int = when (diff) {
        Difficulty.EASY   -> 1
        Difficulty.MEDIUM -> 2
        Difficulty.HARD   -> 4
        Difficulty.EXPERT -> 8
        Difficulty.GENIUS -> 20
    }

    /** Base completion reward for a solo / team / daily puzzle. */
    fun completionPoints(diff: Difficulty, isDaily: Boolean): Int =
        if (isDaily) DAILY_POINTS else pointsForDifficulty(diff)

    /**
     * Net value of a finished puzzle as shown on the results card and stored in
     * the best-record stat: reward minus hints, never negative.
     */
    fun netPoints(diff: Difficulty, isDaily: Boolean, hintsUsed: Int): Int =
        (completionPoints(diff, isDaily) - hintsUsed * HINT_COST).coerceAtLeast(0)

    /**
     * Amount added to lifetime score when a puzzle is completed. Hints were
     * already charged at the moment of use, so they are not deducted again —
     * the lifetime total for the puzzle therefore equals reward − hints, which
     * is exactly what the results card advertises.
     */
    fun lifetimeAwardOnWin(diff: Difficulty, isDaily: Boolean): Int =
        completionPoints(diff, isDaily)

    /** Vindictive: a player's in-match score banked into lifetime score. */
    fun vindictiveBank(matchScore: Int): Int = matchScore.coerceAtLeast(0)
}

fun pointsForDifficulty(diff: Difficulty): Int = Economy.pointsForDifficulty(diff)

fun checkStreakMilestone(streak: Int): String? = when {
    streak == 3              -> "On Fire! 🔥"
    streak == 5              -> "Unstoppable! ⚡"
    streak == 10             -> "Legendary! 🏆"
    streak == 15             -> "Crossword God! 👑"
    streak > 15 && streak % 5 == 0 -> "Still Going! 🌟 ($streak in a row)"
    else                     -> null
}
