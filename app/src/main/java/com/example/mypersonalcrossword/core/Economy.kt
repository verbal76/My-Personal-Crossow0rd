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
     * the best-record stat: reward minus hints, floored at 0. It differs from the
     * real lifetime change ([lifetimeDelta]) only when hints cost more than the
     * reward (e.g. 4 hints on Easy: shows 0, lifetime moved by −3).
     */
    fun netPoints(diff: Difficulty, isDaily: Boolean, hintsUsed: Int): Int =
        (completionPoints(diff, isDaily) - hintsUsed * HINT_COST).coerceAtLeast(0)

    /**
     * Amount added to lifetime score when a puzzle is completed. Hints were
     * already charged at the moment of use, so they are not deducted again —
     * the lifetime total for the puzzle therefore moves by [lifetimeDelta]
     * (reward − hints, unfloored). That equals [netPoints] whenever the hints
     * cost no more than the reward; [netPoints] alone is floored at 0.
     */
    fun lifetimeAwardOnWin(diff: Difficulty, isDaily: Boolean): Int =
        completionPoints(diff, isDaily)

    /** Lifetime points charged for [hintsUsed] hints, at the moment each is used. */
    fun hintCharges(hintsUsed: Int): Int = hintsUsed.coerceAtLeast(0) * HINT_COST

    /**
     * Total lifetime change for one completed puzzle: the hint charges already
     * taken plus [lifetimeAwardOnWin]. Can be negative (unlike [netPoints]);
     * use it wherever the UI claims to show what the puzzle did to the total.
     */
    fun lifetimeDelta(reward: Int, hintsUsed: Int): Int = reward - hintCharges(hintsUsed)

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
