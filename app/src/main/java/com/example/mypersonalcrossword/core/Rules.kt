package com.hag.mypersonalcrossword.core

// ============================================================
// GAME RULES — pure state transitions for Team and Vindictive play,
// hint selection, and online input merging. The UI layer calls these
// and then renders / syncs the result; it never re-implements scoring.
// ============================================================

// ── TEAM ─────────────────────────────────────────────────────────────────────

data class TeamState(
    val p1Correct: Int = 0,      // words solved by player index 0
    val p2Correct: Int = 0,      // words solved by player index 1
    val turn:      Int = 0,      // 0 or 1 — whose turn it is
    val p1Hints:   Int = 0,
    val p2Hints:   Int = 0
)

object TeamRules {
    /**
     * A word attempt finished (the word was fully entered). The turn always
     * alternates — correct or not — matching the Team tutorial ("players
     * alternate answering clues").
     */
    fun onAttempt(s: TeamState, correct: Boolean): TeamState {
        val credited = when {
            !correct      -> s
            s.turn == 0   -> s.copy(p1Correct = s.p1Correct + 1)
            else          -> s.copy(p2Correct = s.p2Correct + 1)
        }
        return credited.copy(turn = 1 - s.turn)
    }

    fun onHint(s: TeamState): TeamState =
        if (s.turn == 0) s.copy(p1Hints = s.p1Hints + 1) else s.copy(p2Hints = s.p2Hints + 1)

    /** Player index 0 = host / logged-in player, 1 = guest / Player 2. */
    fun mvp(s: TeamState): Int? = when {
        s.p1Correct > s.p2Correct -> 0
        s.p2Correct > s.p1Correct -> 1
        else                      -> null
    }
}

// ── VINDICTIVE ───────────────────────────────────────────────────────────────

data class VindState(
    val phase:         VindicativePhase = VindicativePhase.PICK_OWN,
    val currentPlayer: Int = 0,          // player index who acts in this phase
    val p1Score:       Int = 0,
    val p2Score:       Int = 0,
    val assignedIndex: Int = -1          // index into the word list, -1 = none
)

enum class VindOutcome { OWN_CORRECT, OWN_WRONG, ASSIGNED_CORRECT, ASSIGNED_WRONG, PASSED }

object VindictiveRules {

    private fun VindState.addToCurrent(delta: Int) =
        if (currentPlayer == 0) copy(p1Score = p1Score + delta) else copy(p2Score = p2Score + delta)

    /** Opening move: the current player answered a clue they picked themselves. */
    fun onOwnAnswer(s: VindState, correct: Boolean): Pair<VindState, VindOutcome> {
        require(s.phase == VindicativePhase.PICK_OWN) { "own answer outside PICK_OWN" }
        val delta = if (correct) Economy.VIND_OWN_CORRECT else Economy.VIND_OWN_WRONG
        return s.addToCurrent(delta).copy(phase = VindicativePhase.ASSIGN_CLUE) to
            (if (correct) VindOutcome.OWN_CORRECT else VindOutcome.OWN_WRONG)
    }

    /** The current player hands clue [wordIndex] to the opponent, who now must answer it. */
    fun onAssign(s: VindState, wordIndex: Int): VindState {
        require(s.phase == VindicativePhase.ASSIGN_CLUE) { "assign outside ASSIGN_CLUE" }
        return s.copy(
            phase         = VindicativePhase.OPPONENT_WAIT,
            currentPlayer = 1 - s.currentPlayer,
            assignedIndex = wordIndex
        )
    }

    /** The opponent answered their assigned clue. They then pick a clue for the other player. */
    fun onAssignedAnswer(s: VindState, correct: Boolean): Pair<VindState, VindOutcome> {
        require(s.phase == VindicativePhase.OPPONENT_WAIT) { "answer outside OPPONENT_WAIT" }
        val delta = if (correct) Economy.VIND_ASSIGNED_CORRECT else Economy.VIND_ASSIGNED_WRONG
        return s.addToCurrent(delta).copy(phase = VindicativePhase.ASSIGN_CLUE, assignedIndex = -1) to
            (if (correct) VindOutcome.ASSIGNED_CORRECT else VindOutcome.ASSIGNED_WRONG)
    }

    /** The opponent passed on their assigned clue (offered when the timer runs out). */
    fun onPass(s: VindState): Pair<VindState, VindOutcome> {
        require(s.phase == VindicativePhase.OPPONENT_WAIT) { "pass outside OPPONENT_WAIT" }
        return s.addToCurrent(Economy.VIND_PASS).copy(phase = VindicativePhase.ASSIGN_CLUE, assignedIndex = -1) to
            VindOutcome.PASSED
    }

    /**
     * Which device runs the answer countdown. Offline there is one device; online
     * only the answerer's device owns the timer (and writes it for the other side).
     */
    fun ownsTimer(s: VindState, isOnline: Boolean, myIndex: Int): Boolean =
        s.phase == VindicativePhase.OPPONENT_WAIT && (!isOnline || s.currentPlayer == myIndex)

    /** Which device may act (type / pick / assign) in the current phase when online. */
    fun canAct(s: VindState, isOnline: Boolean, myIndex: Int): Boolean =
        !isOnline || s.currentPlayer == myIndex

    fun winner(s: VindState): Int? = when {
        s.p1Score > s.p2Score -> 0
        s.p2Score > s.p1Score -> 1
        else                  -> null
    }
}

// ── HINTS ────────────────────────────────────────────────────────────────────

/** Cells of [word] that do not yet hold the correct letter. */
fun unrevealedCells(word: PlacedWord, inputs: Map<Cell, Char>): List<Pair<Cell, Char>> =
    word.word.indices.mapNotNull { i ->
        val pos = word.cellAt(i)
        if (inputs[pos] == word.word[i]) null else pos to word.word[i]
    }

/**
 * Picks the letter a hint reveals. A square holding a *wrong* letter comes first
 * (the selected one if it is wrong): revealing an empty square of a word with a
 * typo would complete it wrongly and count as a failed attempt. Otherwise the
 * selected cell when it is part of the word and still empty, else the first open
 * cell in reading order. Deterministic so the player can predict it.
 */
fun pickHintCell(word: PlacedWord, inputs: Map<Cell, Char>, preferred: Cell? = null): Pair<Cell, Char>? {
    val open = unrevealedCells(word, inputs)
    if (open.isEmpty()) return null
    val wrong = open.filter { inputs[it.first] != null }
    if (wrong.isNotEmpty()) return wrong.firstOrNull { it.first == preferred } ?: wrong.first()
    return open.firstOrNull { it.first == preferred } ?: open.first()
}

// ── ONLINE MERGE ─────────────────────────────────────────────────────────────

/**
 * Merges letters received from the other device into the local grid.
 * The remote side only ever publishes letters of solved words and hints, so a
 * remote letter is accepted only if it is the *correct* letter for that cell —
 * it then overrides whatever (possibly wrong, in-progress) letter is local.
 * Remote data can never erase a local letter or plant a wrong one.
 */
fun mergeRemoteInputs(
    local:    Map<Cell, Char>,
    remote:   Map<Cell, Char>,
    solution: Map<Cell, Char>
): Map<Cell, Char> {
    var out: MutableMap<Cell, Char>? = null
    for ((cell, ch) in remote) {
        if (solution[cell] != ch) continue       // not a valid solved letter — ignore
        if (local[cell] == ch) continue
        if (out == null) out = local.toMutableMap()
        out[cell] = ch
    }
    return out ?: local
}

fun solutionOf(words: List<PlacedWord>): Map<Cell, Char> {
    val m = HashMap<Cell, Char>()
    words.forEach { w -> w.word.indices.forEach { i -> m[w.cellAt(i)] = w.word[i] } }
    return m
}

// ── TAUNTS ───────────────────────────────────────────────────────────────────

// Passive-aggressive wrong-answer taunts. Part of Vindictive's personality — keep.
private val TAUNTS = listOf<(String, String) -> String>(
    { _, p2 -> "$p2 knew you didn't know that. 😏" },
    { _, _ -> "That was what you thought? Really?" },
    { _, p2 -> "I told $p2 you'd never figure that out." },
    { _, _ -> "Get good, scrub." },
    { _, _ -> "Total pleb answer." },
    { _, p2 -> "Even $p2 cringed a little." },
    { _, _ -> "Was that a guess? Bold strategy." },
    { p1, _ -> "$p1, honey… no." },
    { p1, _ -> "The answer was right there, $p1." },
    { _, _ -> "That's… not it, chief." },
    { _, p2 -> "$p2 is going to love this." },
    { _, _ -> "Yikes. Just… yikes." },
    { _, _ -> "Did you just make that up?" },
    { p1, _ -> "$p1 with the classic wrong answer." },
    { _, _ -> "At least you tried. (You didn't try.)" },
    { _, p2 -> "Wrong! $p2 says thanks for the easy win." },
    { p1, _ -> "That one hurt to read, $p1." },
    { _, p2 -> "Somewhere, $p2 is smiling." },
    { _, _ -> "Not even close. Not even in the same ZIP code." },
    { _, _ -> "Confidently incorrect. Respect, kind of." }
)

val TAUNT_COUNT: Int get() = TAUNTS.size

/**
 * [playerName] is the player who got it wrong; [otherName] is their opponent.
 * Callers pass the names from the wrong-answerer's perspective so the jab lands
 * on the right person.
 */
fun vindictiveTaunt(index: Int, playerName: String, otherName: String): String =
    TAUNTS[Math.floorMod(index, TAUNTS.size)](playerName, otherName)
