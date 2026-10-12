package com.hag.mypersonalcrossword.desktop

import com.hag.mypersonalcrossword.core.*
import kotlin.random.Random

enum class ModalKind { NOTICE, TIMEOUT }

/** A blocking message: "Pass the phone", a Vindictive timeout, a welcome back. Input is held until it is answered. */
data class Modal(val kind: ModalKind, val title: String, val message: String, val buttons: List<String>)

/** Shown on the results card. Same arithmetic as Android: net = reward - hints, floored at 0. */
data class PuzzleResult(
    val mode: GameMode, val reward: Int, val hints: Int, val net: Int, val awarded: Int, val seconds: Long,
    val daily: Boolean, val paid: Boolean, val team: TeamState, val vind: VindState, val names: List<String>
)

/**
 * All puzzle state and rules for the desktop build. Pure Kotlin (no AWT), so it is unit-tested without a
 * display. Scoring and turn rules come from the shared core (InputEngine, Economy, TeamRules, VindictiveRules,
 * pickHintCell, DailyPuzzle, generateCrossword, SessionCodec, puzzleFingerprint, vindictiveTaunt); this class
 * only sequences them the way the Android screen does (docs/DESKTOP_PARITY.md lists what is and isn't ported).
 * Local play only: Team and Vindictive are "pass the Deck"; there is no online mode on desktop.
 */
class GameController(
    private val store: ProfileStore,
    val entries: List<RawEntry>,
    private val rng: () -> Random = { Random.Default },
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
) {
    var session: PuzzleSession? = null; private set
    var board: Board? = null; private set
    var selection: Selection? = null; private set
    var result: PuzzleResult? = null; private set
    var message: String = ""; private set
    var modal: Modal? = null; private set
    /** Milliseconds left on the Vindictive answer clock (only meaningful in OPPONENT_WAIT). */
    var vindMsLeft: Long = 0; private set
    /** Sound hook (effect, streak). The window wires it to real audio; tests record it. */
    var onSound: (SoundSynth.Effect, Int) -> Unit = { _, _ -> }

    private var carryMs = 0L
    private var lastTickSecond = -1L
    private val rand by lazy { rng() }

    val categories: List<String> by lazy { listOf("MIXED") + entries.map { it.category }.distinct().sorted() }
    val active: Boolean get() = session != null && result == null
    val inputs: Map<Cell, Char> get() = session?.inputs ?: emptyMap()
    val revealed: Set<Cell> get() = session?.revealed ?: emptySet()
    val locked: Set<Cell> get() = board?.lockedCells(inputs, revealed) ?: emptySet()
    val activeWord: PlacedWord? get() = board?.activeWord(selection)
    val dailyKeyToday: String get() = DailyPuzzle.dateKey(nowMillis())
    val dailyDoneToday: Boolean get() = dailyKeyToday in store.dailyDone

    val mode: GameMode get() = session?.mode ?: GameMode.SINGLE
    val team: TeamState get() = session?.team ?: TeamState()
    val vind: VindState get() = session?.vind ?: VindState()
    val streak: Int get() = session?.streak ?: 0
    val names: List<String> get() = listOf(P1, session?.player2?.ifBlank { null } ?: P2)
    private fun nameOf(i: Int) = names[i]
    private val twoPlayer: Boolean get() = mode == GameMode.TEAM || mode == GameMode.VINDICTIVE

    /** Whose turn it is in a two-player game (0 or 1). */
    val currentPlayer: Int get() = when (mode) { GameMode.TEAM -> team.turn; GameMode.VINDICTIVE -> vind.currentPlayer; else -> 0 }

    /** Vindictive: the clue the answering player is forced onto. */
    val forcedWord: PlacedWord? get() {
        val s = session ?: return null
        return if (s.mode == GameMode.VINDICTIVE && s.vind.phase == VindicativePhase.OPPONENT_WAIT) s.words.getOrNull(s.vind.assignedIndex) else null
    }
    val assigning: Boolean get() = mode == GameMode.VINDICTIVE && vind.phase == VindicativePhase.ASSIGN_CLUE && active
    val canGive: Boolean get() = assigning && modal == null && activeWord?.let { !isWordSolved(it, inputs) } == true
    val canPass: Boolean get() = active && forcedWord != null && vindMsLeft <= 0 && modal == null

    /** Why typing is held right now; null when the current player may type. */
    val blockedReason: String? get() = when {
        !active -> ""
        modal != null -> "Answer the message first"
        assigning -> "${nameOf(vind.currentPlayer)}: pick a clue for ${nameOf(1 - vind.currentPlayer)}, then Give"
        else -> null
    }

    /** One line under the header for the two-player modes. */
    val statusLine: String get() = when (mode) {
        GameMode.TEAM -> "${nameOf(team.turn)}'s turn   ·   ${nameOf(0)} ${team.p1Correct} : ${team.p2Correct} ${nameOf(1)}"
        GameMode.VINDICTIVE -> {
            val v = vind
            val who = nameOf(v.currentPlayer)
            val phase = when (v.phase) {
                VindicativePhase.PICK_OWN -> "$who: answer any clue"
                VindicativePhase.ASSIGN_CLUE -> "$who: pick a clue and Give it to ${nameOf(1 - v.currentPlayer)}"
                VindicativePhase.OPPONENT_WAIT -> "$who must answer" + if (vindMsLeft > 0) "  ${(vindMsLeft + 999) / 1000}s" else "  (time's up: pass is open)"
            }
            "$phase   ·   ${nameOf(0)} ${v.p1Score} : ${v.p2Score} ${nameOf(1)}"
        }
        else -> ""
    }

    // ── Starting / resuming ──────────────────────────────────────────────────

    fun startPuzzle(category: String, difficulty: Difficulty, mode: GameMode = GameMode.SINGLE, vindTimerSecs: Int = 30): Boolean {
        val pool = if (category == "MIXED") entries else entries.filter { it.category == category }
        val words = generateCrossword(pool, difficulty.wordCount, emptySet(), rng())
        if (words.size < 2) { message = "Couldn't build a puzzle for $category"; return false }
        val m = if (mode == GameMode.DAILY) GameMode.SINGLE else mode
        val timer = vindTimerSecs.takeIf { it in SessionCodec.VIND_TIMER_CHOICES } ?: 30
        load(PuzzleSession(m, category, difficulty, words, player2 = if (m == GameMode.SINGLE) "" else P2, vindTimerSecs = timer), fresh = true)
        return true
    }

    fun startDaily(): Boolean {
        val key = dailyKeyToday
        // Resume today's Daily if one is in progress; otherwise generate (deterministic per UTC day). Always solo.
        store.loadSession(SaveSlot.daily(key).id)?.let { load(it, fresh = false); return true }
        val words = DailyPuzzle.generate(entries, key)
        if (words.size < 2) { message = "Couldn't build today's Daily"; return false }
        load(PuzzleSession(GameMode.DAILY, DailyPuzzle.CATEGORY, Difficulty.EXPERT, words, dailyKey = key), fresh = true)
        return true
    }

    fun resume(slotId: String): Boolean {
        val s = store.loadSession(slotId) ?: return false
        load(s, fresh = false); return true
    }

    private fun load(s: PuzzleSession, fresh: Boolean) {
        session = s
        board = Board(s.words)
        result = null; modal = null
        carryMs = 0; lastTickSecond = -1
        message = ""
        vindMsLeft = if (s.mode == GameMode.VINDICTIVE && s.vind.phase == VindicativePhase.OPPONENT_WAIT)
            (s.vindSecondsLeft ?: s.vindTimerSecs) * 1000L else 0L
        selection = forcedWord?.let { InputEngine.selectWord(it, s.inputs, locked) }
            ?: InputEngine.initialSelection(board!!, s.inputs, locked)
        // A save that is somehow already complete is judged now rather than left unfinishable.
        if (board!!.words.all { isWordSolved(it, s.inputs) }) { complete(); return }
        if (!fresh && twoPlayer) {      // handing the Deck back after a quit: say whose turn it is and hold the clock
            modal = Modal(ModalKind.NOTICE, "Welcome back", "${nameOf(currentPlayer)}'s turn.\n${statusLine}", listOf("I'm Ready"))
        }
    }

    // ── Selection and navigation ─────────────────────────────────────────────

    fun tap(cell: Cell) {
        val b = board ?: return
        if (!active || modal != null) return
        val forced = forcedWord
        selection = if (forced != null) {
            if (cell in forced.cells()) Selection(cell, forced.direction) else InputEngine.selectWord(forced, inputs, locked)
        } else InputEngine.tap(b, selection, cell, locked)
    }

    fun flip() {
        val sel = selection ?: return
        if (forcedWord != null) return
        tap(sel.cell)
    }

    /** Arrow navigation: next board square in [dx],[dy], skipping blank squares. */
    fun move(dx: Int, dy: Int) {
        val b = board ?: return
        if (!active || modal != null) return
        val sel = selection ?: return
        var x = sel.cell.first; var y = sel.cell.second
        val forced = forcedWord
        repeat(40) {
            x += dx; y += dy
            val c = Pair(x, y)
            if (b.contains(c)) {
                if (forced != null) { if (c in forced.cells()) selection = Selection(c, forced.direction); return }
                // Moving along the word's own axis keeps its direction; across it, switch to the crossing word.
                val want = if (dx != 0) Direction.ACROSS else Direction.DOWN
                val dir = if (b.wordAt(c, want) != null) want else sel.direction.takeIf { b.wordAt(c, it) != null } ?: want.other
                selection = Selection(c, dir)
                return
            }
        }
    }

    fun nextClue(forward: Boolean = true) {
        val b = board ?: return
        if (!active || modal != null) return
        forcedWord?.let { selection = InputEngine.selectWord(it, inputs, locked); return }
        selection = InputEngine.nextUnsolved(b, selection, inputs, locked, forward)
    }

    fun selectWord(w: PlacedWord) {
        if (!active || modal != null) return
        val forced = forcedWord
        if (forced != null && w != forced) return
        selection = InputEngine.selectWord(w, inputs, locked)
    }

    // ── Typing ───────────────────────────────────────────────────────────────

    fun type(ch: Char) {
        val b = board ?: return; val s = session ?: return
        if (!active || blockedReason != null) return
        var sel = selection
        val forced = forcedWord
        if (forced != null && b.activeWord(sel) != forced) sel = InputEngine.selectWord(forced, s.inputs, locked)
        val activeW = b.activeWord(sel) ?: return
        val r = InputEngine.type(b, sel, s.inputs, locked, ch)
        if (r.inputs == s.inputs && r.selection == sel) return
        session = s.copy(inputs = r.inputs)
        selection = r.selection
        onSound(SoundSynth.Effect.KEY, 0)
        judge(r.filled, activeW)
    }

    fun backspace() {
        val b = board ?: return; val s = session ?: return
        if (!active || blockedReason != null) return
        val r = InputEngine.backspace(b, selection, s.inputs, locked)
        session = s.copy(inputs = r.inputs)
        selection = r.selection
    }

    /** Judges the words a keystroke (or hint) completed. Only the active word is an attempt; a crossing word finished correctly just counts as solved. */
    private fun judge(filled: List<PlacedWord>, activeW: PlacedWord) {
        val b = board ?: return
        if (filled.isEmpty()) return
        // A crossing word finished correctly by the same letter simply counts as solved (solo plays the chime).
        if (!twoPlayer) filled.filter { it != activeW && isWordSolved(it, inputs) }.forEach { _ -> correctFeedback() }
        if (activeW in filled) attempt(activeW, isWordSolved(activeW, inputs))
        if (active && b.words.all { isWordSolved(it, inputs) }) { complete(); return }
    }

    private fun correctFeedback() {
        val s = session ?: return
        val streak = s.streak + 1
        session = s.copy(streak = streak)
        onSound(SoundSynth.Effect.CORRECT, streak - 1)
        checkStreakMilestone(streak)?.let { message = it; onSound(SoundSynth.Effect.CLAP, 0) }
    }

    private fun wrongFeedback() {
        session = session?.copy(streak = 0)
        onSound(SoundSynth.Effect.WRONG, 0)
    }

    /** The active word was completely filled by the player whose turn it is. */
    private fun attempt(word: PlacedWord, correct: Boolean) {
        val b = board ?: return
        when (mode) {
            GameMode.SINGLE, GameMode.DAILY -> {
                if (correct) { message = "Correct: ${word.word}"; correctFeedback() }     // a streak milestone may replace the message
                else { message = "Not quite: check ${word.number}${if (word.isHorizontal) "A" else "D"}"; wrongFeedback() }
                if (correct) selection = InputEngine.nextUnsolved(b, selection, inputs, locked, true) ?: selection
            }
            GameMode.TEAM -> {
                val before = team
                val who = before.turn
                message = if (correct) "Correct: ${word.word}" else "Not quite"
                if (correct) correctFeedback() else {
                    wrongFeedback()
                    session = session?.copy(inputs = InputEngine.clearWord(word, inputs, locked))   // one-shot: a wrong attempt is cleared
                }
                val after = TeamRules.onAttempt(before, correct)
                session = session?.copy(team = after)
                if (!allSolved()) {
                    modal = Modal(ModalKind.NOTICE, "Pass the Phone!",
                        (if (correct) "Nice one, ${nameOf(who)}!" else "Not quite, ${nameOf(who)}.") +
                            "\nPass to ${nameOf(after.turn)}\n${nameOf(0)}: ${after.p1Correct} solved  |  ${nameOf(1)}: ${after.p2Correct} solved",
                        listOf("I'm Ready"))
                    selection = InputEngine.nextUnsolved(b, selection, inputs, locked, true) ?: selection
                }
            }
            GameMode.VINDICTIVE -> {
                val before = vind
                val actor = before.currentPlayer
                val (after, outcome) = when (before.phase) {
                    VindicativePhase.PICK_OWN -> VindictiveRules.onOwnAnswer(before, correct)
                    VindicativePhase.OPPONENT_WAIT -> VindictiveRules.onAssignedAnswer(before, correct)
                    VindicativePhase.ASSIGN_CLUE -> return
                }
                val taunt = if (correct) "" else vindictiveTaunt(rand.nextInt(TAUNT_COUNT), nameOf(actor), nameOf(1 - actor))
                message = if (correct) "Correct: ${word.word}" else taunt
                if (correct) correctFeedback() else wrongFeedback()
                // The turn changes hands: no half-typed letters carry over.
                session = session?.copy(inputs = InputEngine.clearUnlocked(inputs, locked), vind = after)
                vindMsLeft = 0
                if (!allSolved()) {
                    selection = InputEngine.nextUnsolved(b, selection, inputs, locked, true) ?: selection
                    if (before.phase == VindicativePhase.OPPONENT_WAIT) {     // same player keeps the Deck to pick a clue
                        val line = if (outcome == VindOutcome.ASSIGNED_CORRECT) "Correct! +${Economy.VIND_ASSIGNED_CORRECT} for ${nameOf(actor)}"
                                   else "Wrong! ${Economy.VIND_ASSIGNED_WRONG} for ${nameOf(actor)}"
                        modal = Modal(ModalKind.NOTICE, "${nameOf(actor)} picks next",
                            (if (taunt.isNotEmpty()) "$taunt\n" else "") + "$line\n${nameOf(actor)} now picks a clue for ${nameOf(1 - actor)}.\n" +
                                "${nameOf(0)}: ${after.p1Score} pts  |  ${nameOf(1)}: ${after.p2Score} pts", listOf("I'm Ready"))
                    }
                }
            }
        }
    }

    private fun allSolved() = board?.words?.all { isWordSolved(it, inputs) } == true

    /** Vindictive: the picking player hands the active clue to the opponent, who answers against the clock. */
    fun give(): Boolean {
        val s = session ?: return false
        val w = activeWord ?: return false
        if (!canGive) return false
        val idx = s.words.indexOf(w)
        if (idx < 0) return false
        val after = VindictiveRules.onAssign(s.vind, idx)
        session = s.copy(vind = after)
        vindMsLeft = s.vindTimerSecs * 1000L
        lastTickSecond = -1
        selection = InputEngine.selectWord(w, inputs, locked)
        onSound(SoundSynth.Effect.CLICK, 0)
        modal = Modal(ModalKind.NOTICE, "Pass the Phone!",
            "Pass to ${nameOf(after.currentPlayer)}!\nA clue is waiting. The ${s.vindTimerSecs}s timer starts when they press I'm Ready.", listOf("I'm Ready"))
        return true
    }

    /** Vindictive: the answerer passes on the assigned clue (-1). Available once the clock has run out. */
    fun pass(): Boolean {
        val s = session ?: return false
        if (s.mode != GameMode.VINDICTIVE || s.vind.phase != VindicativePhase.OPPONENT_WAIT || modal?.kind == ModalKind.NOTICE) return false
        val who = s.vind.currentPlayer
        val (after, _) = VindictiveRules.onPass(s.vind)
        session = s.copy(vind = after, inputs = InputEngine.clearUnlocked(s.inputs, locked))
        vindMsLeft = 0
        modal = Modal(ModalKind.NOTICE, "${nameOf(who)} picks next",
            "${nameOf(who)} passed (${Economy.VIND_PASS})\n${nameOf(who)} now picks a clue for ${nameOf(1 - who)}.\n" +
                "${nameOf(0)}: ${after.p1Score} pts  |  ${nameOf(1)}: ${after.p2Score} pts", listOf("I'm Ready"))
        selection = InputEngine.nextUnsolved(board!!, selection, inputs, locked, true) ?: selection
        return true
    }

    /** Answers the open [modal]: index of the pressed button. */
    fun resolveModal(index: Int) {
        val m = modal ?: return
        modal = null
        if (m.kind == ModalKind.TIMEOUT) { if (index == 1) pass() }     // 0 = "Answer it": the letters stay, the pass stays available
    }

    // ── Hints ────────────────────────────────────────────────────────────────

    /** Lifetime score of whoever pays for a hint: the player whose turn it is. Only Player 1 has a profile on this device. */
    private fun payerScore(): Int = if (currentPlayer == 0) store.score else 0

    /** Hints cost [Economy.HINT_COST], charged once when used, and need the points to pay for them (as on Android). */
    val canHint: Boolean get() {
        if (!active || blockedReason != null) return false
        val w = activeWord?.takeIf { !isWordSolved(it, inputs) } ?: return false
        if (payerScore() < Economy.HINT_COST) return false
        // Vindictive is head-to-head: a hint may help, but the last letter must be the player's own.
        if (mode == GameMode.VINDICTIVE && unrevealedCells(w, inputs).size <= 1) return false
        return true
    }

    fun hint(): Boolean {
        val b = board ?: return false; val s = session ?: return false
        if (!canHint) return false
        val w = activeWord ?: return false
        val (cell, ch) = pickHintCell(w, s.inputs, selection?.cell) ?: return false
        val before = s.inputs
        val newInputs = s.inputs + (cell to ch)
        var next = s.copy(inputs = newInputs, revealed = s.revealed + cell, hintsUsed = s.hintsUsed + 1)
        if (mode == GameMode.TEAM) next = next.copy(team = TeamRules.onHint(team))
        session = next
        if (currentPlayer == 0) store.score = (store.score - Economy.HINT_COST).coerceAtLeast(0)
        message = "Hint used (-${Economy.HINT_COST})"
        onSound(SoundSynth.Effect.CLICK, 0)
        // A reveal can complete words: judge them like a typed letter.
        val filled = b.wordsAt(cell).filter { isWordFilled(it, newInputs) && !(isWordFilled(it, before) && before[cell] == ch) }
        judge(filled.sortedBy { if (it == w) 0 else 1 }, w)
        return true
    }

    // ── Time, saving, completion ─────────────────────────────────────────────

    /** Advances the puzzle clock and the Vindictive answer clock; call from the UI timer. */
    fun advance(ms: Long) {
        val s = session ?: return
        if (!active) return
        carryMs += ms
        val whole = carryMs / 1000
        if (whole > 0) { carryMs -= whole * 1000; session = s.copy(elapsedSeconds = s.elapsedSeconds + whole) }
        // The answer clock runs only while the answerer is looking at the clue (no message open).
        if (forcedWord != null && modal == null && vindMsLeft > 0) {
            vindMsLeft = (vindMsLeft - ms).coerceAtLeast(0)
            val sec = (vindMsLeft + 999) / 1000
            if (sec != lastTickSecond) { lastTickSecond = sec; if (sec in 1..5) onSound(SoundSynth.Effect.TICK, 0) }
            if (vindMsLeft == 0L) {
                onSound(SoundSynth.Effect.WRONG, 0)
                modal = Modal(ModalKind.TIMEOUT, "Time's up!", "${nameOf(vind.currentPlayer)} ran out of time. Answer it anyway, or pass (${Economy.VIND_PASS}).",
                    listOf("Answer it", "Pass (${Economy.VIND_PASS})"))
            }
        }
    }

    /** Saves the puzzle in progress. A finished or absent puzzle writes nothing. */
    fun save() {
        val s = session ?: return
        if (!active) return
        val left = if (forcedWord != null) ((vindMsLeft + 999) / 1000).toInt() else null
        store.saveSession(s.copy(vindSecondsLeft = left))
    }

    /** Leaves the puzzle after saving; a solved grid is never kept loaded (it would re-pay). */
    fun unload() {
        save()
        session = null; board = null; selection = null; result = null; message = ""; modal = null
    }

    private fun complete() {
        val s = session ?: return
        modal = null
        val fp = puzzleFingerprint(s.words)
        val daily = s.isDaily
        val dailyKey = s.dailyKey
        val reward = Economy.completionPoints(s.difficulty, daily)
        val owed = !ledgerContains(store.ledger, fp) && !(daily && dailyKey != null && dailyKey in store.dailyDone)
        var awarded = 0
        if (owed) {
            // Same economy as Android: SINGLE/DAILY and TEAM pay the full reward; Vindictive banks the match score (floored at 0).
            awarded = if (s.mode == GameMode.VINDICTIVE) Economy.vindictiveBank(s.vind.p1Score) else Economy.lifetimeAwardOnWin(s.difficulty, daily)
            store.score = store.score + awarded
            store.ledger = appendToLedger(store.ledger, fp)
            if (daily && dailyKey != null) store.dailyDone = store.dailyDone + dailyKey
        }
        store.clearSession(s.slot.id)
        val hints = if (s.mode == GameMode.TEAM) s.team.p1Hints else s.hintsUsed
        result = PuzzleResult(s.mode, reward, s.hintsUsed, Economy.netPoints(s.difficulty, daily, hints), awarded, s.elapsedSeconds,
            daily, owed, s.team, s.vind, names)
        message = "Puzzle complete!"
        onSound(SoundSynth.Effect.CELEBRATION, 0)
    }

    companion object { const val P1 = "Player 1"; const val P2 = "Player 2" }
}
