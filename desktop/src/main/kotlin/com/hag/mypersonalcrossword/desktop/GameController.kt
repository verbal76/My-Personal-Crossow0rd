package com.hag.mypersonalcrossword.desktop

import com.hag.mypersonalcrossword.core.*
import kotlin.random.Random

/** Shown on the results card. Same arithmetic as Android: net = reward - hints, floored at 0. */
data class PuzzleResult(val reward: Int, val hints: Int, val net: Int, val seconds: Long, val daily: Boolean, val paid: Boolean)

/**
 * All puzzle state and rules for the desktop build. Pure Kotlin (no AWT), so it is unit-tested
 * without a display. It only calls the shared core (InputEngine, Economy, pickHintCell,
 * DailyPuzzle, generateCrossword, SessionCodec); there are no desktop-specific game rules.
 * Scope today: SINGLE and DAILY. Team / Vindictive / online stay Android-only.
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
    /** Fractions of a second carried between [advance] calls. */
    private var carryMs = 0L

    val categories: List<String> by lazy { listOf("MIXED") + entries.map { it.category }.distinct().sorted() }
    val active: Boolean get() = session != null && result == null
    val inputs: Map<Cell, Char> get() = session?.inputs ?: emptyMap()
    val revealed: Set<Cell> get() = session?.revealed ?: emptySet()
    val locked: Set<Cell> get() = board?.lockedCells(inputs, revealed) ?: emptySet()
    val activeWord: PlacedWord? get() = board?.activeWord(selection)
    val dailyKeyToday: String get() = DailyPuzzle.dateKey(nowMillis())
    val dailyDoneToday: Boolean get() = dailyKeyToday in store.dailyDone

    // ── Starting / resuming ──────────────────────────────────────────────────

    fun startPuzzle(category: String, difficulty: Difficulty): Boolean {
        val pool = if (category == "MIXED") entries else entries.filter { it.category == category }
        val words = generateCrossword(pool, difficulty.wordCount, emptySet(), rng())
        if (words.size < 2) { message = "Couldn't build a puzzle for $category"; return false }
        load(PuzzleSession(GameMode.SINGLE, category, difficulty, words))
        return true
    }

    fun startDaily(): Boolean {
        val key = dailyKeyToday
        // Resume today's Daily if one is in progress; otherwise generate (deterministic per UTC day).
        store.loadSession(SaveSlot.daily(key).id)?.let { load(it); return true }
        val words = DailyPuzzle.generate(entries, key)
        if (words.size < 2) { message = "Couldn't build today's Daily"; return false }
        load(PuzzleSession(GameMode.DAILY, DailyPuzzle.CATEGORY, Difficulty.EXPERT, words, dailyKey = key))
        return true
    }

    fun resume(slotId: String): Boolean {
        val s = store.loadSession(slotId) ?: return false
        load(s); return true
    }

    private fun load(s: PuzzleSession) {
        session = s
        board = Board(s.words)
        result = null
        carryMs = 0
        message = ""
        selection = InputEngine.initialSelection(board!!, s.inputs, locked)
        // A save that is somehow already complete is judged now rather than left unfinishable.
        if (board!!.words.all { isWordSolved(it, s.inputs) }) complete()
    }

    // ── Selection and navigation ─────────────────────────────────────────────

    fun tap(cell: Cell) {
        val b = board ?: return
        if (!active) return
        selection = InputEngine.tap(b, selection, cell, locked)
    }

    fun flip() {
        val sel = selection ?: return
        tap(sel.cell)
    }

    /** Arrow navigation: next board square in [dx],[dy], skipping blank squares. */
    fun move(dx: Int, dy: Int) {
        val b = board ?: return
        if (!active) return
        val sel = selection ?: return
        var x = sel.cell.first; var y = sel.cell.second
        repeat(40) {
            x += dx; y += dy
            val c = Pair(x, y)
            if (b.contains(c)) {
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
        if (!active) return
        selection = InputEngine.nextUnsolved(b, selection, inputs, locked, forward)
    }

    fun selectWord(w: PlacedWord) {
        val b = board ?: return
        if (!active) return
        selection = InputEngine.selectWord(w, inputs, locked)
    }

    // ── Typing ───────────────────────────────────────────────────────────────

    fun type(ch: Char) {
        val b = board ?: return; val s = session ?: return
        if (!active) return
        val r = InputEngine.type(b, selection, s.inputs, locked, ch)
        session = s.copy(inputs = r.inputs)
        selection = r.selection
        judge(r.filled)
    }

    fun backspace() {
        val b = board ?: return; val s = session ?: return
        if (!active) return
        val r = InputEngine.backspace(b, selection, s.inputs, locked)
        session = s.copy(inputs = r.inputs)
        selection = r.selection
    }

    private fun judge(filled: List<PlacedWord>) {
        val b = board ?: return
        for (w in filled) {
            message = if (isWordSolved(w, inputs)) "Correct: ${w.word}" else "Not quite — check ${w.number}${if (w.isHorizontal) "A" else "D"}"
        }
        if (b.words.all { isWordSolved(it, inputs) }) complete()
        else if (filled.any { isWordSolved(it, inputs) }) {
            // A solved word locks; move on to the next unsolved clue so play flows.
            selection = InputEngine.nextUnsolved(b, selection, inputs, locked, true) ?: selection
        }
    }

    // ── Hints ────────────────────────────────────────────────────────────────

    /** Reveals one letter of the active word. Charged once, at use, to the lifetime score (Economy.HINT_COST). */
    fun hint(): Boolean {
        val b = board ?: return false; val s = session ?: return false
        if (!active) return false
        val w = b.activeWord(selection) ?: return false
        val (cell, ch) = pickHintCell(w, s.inputs, selection?.cell) ?: return false
        val newInputs = s.inputs + (cell to ch)
        session = s.copy(inputs = newInputs, revealed = s.revealed + cell, hintsUsed = s.hintsUsed + 1)
        store.score = store.score - Economy.HINT_COST
        message = "Hint used (-${Economy.HINT_COST})"
        val filled = b.wordsAt(cell).filter { isWordFilled(it, newInputs) }
        judge(filled)
        return true
    }

    // ── Time, saving, completion ─────────────────────────────────────────────

    /** Advances the puzzle clock; call from the UI timer with elapsed milliseconds. */
    fun advance(ms: Long) {
        val s = session ?: return
        if (!active) return
        carryMs += ms
        val whole = carryMs / 1000
        if (whole > 0) { carryMs -= whole * 1000; session = s.copy(elapsedSeconds = s.elapsedSeconds + whole) }
    }

    /** Saves the puzzle in progress. A finished or absent puzzle writes nothing. */
    fun save() {
        val s = session ?: return
        if (active) store.saveSession(s)
    }

    /** Leaves the puzzle after saving; a solved grid is never kept loaded (it would re-pay). */
    fun unload() {
        save()
        session = null; board = null; selection = null; result = null; message = ""
    }

    private fun complete() {
        val s = session ?: return
        val fp = puzzleFingerprint(s.words)
        val daily = s.isDaily
        val dailyKey = s.dailyKey
        val reward = Economy.completionPoints(s.difficulty, daily)
        var paid = false
        val owed = !ledgerContains(store.ledger, fp) && !(daily && dailyKey != null && dailyKey in store.dailyDone)
        if (owed) {
            store.score = store.score + Economy.lifetimeAwardOnWin(s.difficulty, daily)
            store.ledger = appendToLedger(store.ledger, fp)
            if (daily && dailyKey != null) store.dailyDone = store.dailyDone + dailyKey
            paid = true
        }
        store.clearSession(s.slot.id)
        result = PuzzleResult(reward, s.hintsUsed, Economy.netPoints(s.difficulty, daily, s.hintsUsed), s.elapsedSeconds, daily, paid)
        message = "Puzzle complete!"
    }
}
