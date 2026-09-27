package com.hag.mypersonalcrossword.core

// ============================================================
// CROSSWORD INPUT — cell selection, direction, typing, backspace,
// navigation. Pure functions over an immutable Board so every
// interaction rule is unit-tested.
//
// Conventions (matching mainstream crossword apps):
//  • Tapping a cell selects it; tapping the selected cell again flips
//    Across/Down when the cell belongs to words in both directions.
//  • Typing writes the letter and advances to the next empty cell of the
//    current word (skipping letters already there); at the end of a word the
//    cursor stays so the finished word can be judged.
//  • Locked cells (letters of solved words and hint reveals) can't be
//    overwritten or erased; the cursor skips over them.
//  • Backspace erases the current cell, or — if it's already empty — moves back
//    one editable cell and erases that.
// ============================================================

enum class Direction { ACROSS, DOWN;
    val other: Direction get() = if (this == ACROSS) DOWN else ACROSS
}

val PlacedWord.direction: Direction get() = if (isHorizontal) Direction.ACROSS else Direction.DOWN

data class Selection(val cell: Cell, val direction: Direction)

class Board(val words: List<PlacedWord>) {
    val solution: Map<Cell, Char> = solutionOf(words)
    private val across = HashMap<Cell, PlacedWord>()
    private val down   = HashMap<Cell, PlacedWord>()

    init {
        for (w in words) for (c in w.cells()) (if (w.isHorizontal) across else down)[c] = w
    }

    /** Clue order: all Across by number, then all Down by number. */
    val clueOrder: List<PlacedWord> =
        words.filter { it.isHorizontal }.sortedBy { it.number } + words.filter { !it.isHorizontal }.sortedBy { it.number }

    fun contains(cell: Cell) = cell in solution

    fun wordAt(cell: Cell, direction: Direction): PlacedWord? =
        if (direction == Direction.ACROSS) across[cell] else down[cell]

    fun wordsAt(cell: Cell): List<PlacedWord> = listOfNotNull(across[cell], down[cell])

    /** The word a selection points into (falls back to the other direction). */
    fun activeWord(sel: Selection?): PlacedWord? =
        sel?.let { wordAt(it.cell, it.direction) ?: wordAt(it.cell, it.direction.other) }

    /** Cells that may not be edited: every cell of a solved word, plus hint reveals. */
    fun lockedCells(inputs: Map<Cell, Char>, revealed: Set<Cell>): Set<Cell> {
        val out = HashSet<Cell>(revealed)
        for (w in words) if (isWordSolved(w, inputs)) out.addAll(w.cells())
        return out
    }
}

data class InputResult(
    val inputs:    Map<Cell, Char>,
    val selection: Selection?,
    /** Words that became completely filled by this keystroke — to be judged. */
    val filled:    List<PlacedWord> = emptyList()
)

object InputEngine {

    /** Select [cell], or flip direction when it is already selected. */
    fun tap(board: Board, current: Selection?, cell: Cell): Selection? {
        if (!board.contains(cell)) return current
        val hasAcross = board.wordAt(cell, Direction.ACROSS) != null
        val hasDown   = board.wordAt(cell, Direction.DOWN) != null
        if (current?.cell == cell) {
            val flipped = current.direction.other
            return if (board.wordAt(cell, flipped) != null) current.copy(direction = flipped) else current
        }
        val preferred = current?.direction ?: Direction.ACROSS
        val dir = when {
            preferred == Direction.ACROSS && hasAcross -> Direction.ACROSS
            preferred == Direction.DOWN   && hasDown   -> Direction.DOWN
            hasAcross -> Direction.ACROSS
            else      -> Direction.DOWN
        }
        return Selection(cell, dir)
    }

    /** Select a whole word (from the clue list/bar): cursor on its first empty editable cell. */
    fun selectWord(word: PlacedWord, inputs: Map<Cell, Char>, locked: Set<Cell>): Selection {
        val cells = word.cells()
        val target = cells.firstOrNull { it !in locked && inputs[it] == null }
            ?: cells.firstOrNull { it !in locked }
            ?: cells.first()
        return Selection(target, word.direction)
    }

    /**
     * Enters [ch] at the cursor. If the cursor sits on a locked cell the letter goes
     * into the next editable cell of the word instead. Returns the words (active and
     * crossing) that this keystroke completed.
     */
    fun type(board: Board, sel: Selection?, inputs: Map<Cell, Char>, locked: Set<Cell>, ch: Char): InputResult {
        val word = board.activeWord(sel) ?: return InputResult(inputs, sel)
        val letter = ch.uppercaseChar()
        if (letter !in 'A'..'Z') return InputResult(inputs, sel)
        val cells = word.cells()
        val startIdx = cells.indexOf(sel!!.cell).coerceAtLeast(0)
        val targetIdx = (startIdx until cells.size).firstOrNull { cells[it] !in locked }
            ?: return InputResult(inputs, sel)                // nothing editable from here on
        val target = cells[targetIdx]
        val next = inputs + (target to letter)

        // Advance: the next EMPTY editable cell after the target; else wrap to the first
        // empty editable cell of the word; else (word full) the next editable cell after
        // the target (overwrite mode); else stay on the target at the end of the word.
        val after = (targetIdx + 1 until cells.size)
        val moveIdx = after.firstOrNull { cells[it] !in locked && next[cells[it]] == null }
            ?: cells.indices.firstOrNull { cells[it] !in locked && next[cells[it]] == null }
            ?: after.firstOrNull { cells[it] !in locked }
            ?: targetIdx
        val newSel = Selection(cells[moveIdx], word.direction)

        val filled = board.wordsAt(target).filter { w ->
            isWordFilled(w, next) && !(isWordFilled(w, inputs) && inputs[target] == letter)
        }
        // Active word first, crossing word second.
        return InputResult(next, newSel, filled.sortedBy { if (it == word) 0 else 1 })
    }

    /** Erases the current cell, or steps back one editable cell and erases that. */
    fun backspace(board: Board, sel: Selection?, inputs: Map<Cell, Char>, locked: Set<Cell>): InputResult {
        val word = board.activeWord(sel) ?: return InputResult(inputs, sel)
        val cells = word.cells()
        val idx = cells.indexOf(sel!!.cell).coerceAtLeast(0)
        val here = cells[idx]
        if (here !in locked && inputs[here] != null) {
            return InputResult(inputs - here, Selection(here, word.direction))
        }
        val prevIdx = (idx - 1 downTo 0).firstOrNull { cells[it] !in locked } ?: return InputResult(inputs, Selection(here, word.direction))
        val prev = cells[prevIdx]
        return InputResult(inputs - prev, Selection(prev, word.direction))
    }

    /**
     * Next (or previous) unsolved word in clue order, wrapping around; selection
     * lands on its first empty editable cell. Returns [sel] if every word is solved.
     */
    fun nextUnsolved(board: Board, sel: Selection?, inputs: Map<Cell, Char>, locked: Set<Cell>, forward: Boolean = true): Selection? {
        val order = board.clueOrder
        if (order.isEmpty()) return sel
        val current = board.activeWord(sel)
        val start = current?.let { order.indexOf(it) } ?: -1
        for (step in 1..order.size) {
            val i = Math.floorMod(start + if (forward) step else -step, order.size)
            val w = order[i]
            if (!isWordSolved(w, inputs)) return selectWord(w, inputs, locked)
        }
        return sel
    }

    /** First selection for a fresh puzzle: the first unsolved clue. */
    fun initialSelection(board: Board, inputs: Map<Cell, Char>, locked: Set<Cell>): Selection? =
        board.clueOrder.firstOrNull { !isWordSolved(it, inputs) }?.let { selectWord(it, inputs, locked) }

    /** Clears every editable letter of [word] (used after a failed one-shot attempt). */
    fun clearWord(word: PlacedWord, inputs: Map<Cell, Char>, locked: Set<Cell>): Map<Cell, Char> =
        inputs - word.cells().filter { it !in locked }.toSet()

    /** Clears every editable letter on the board (turn changes in Vindictive / Team). */
    fun clearUnlocked(inputs: Map<Cell, Char>, locked: Set<Cell>): Map<Cell, Char> =
        inputs.filterKeys { it in locked }
}
