package com.hag.mypersonalcrossword.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 *   C A T        1 = CAT (across), 1 = COW (down), 2 = TOE (down)
 *   O . O
 *   W . E
 */
class InputEngineTest {

    private val cat = PlacedWord("CAT", "Pet", 0, 0, true, 1)
    private val cow = PlacedWord("COW", "Farm animal", 0, 0, false, 1)
    private val toe = PlacedWord("TOE", "Foot digit", 2, 0, false, 2)
    private val board = Board(listOf(cat, cow, toe))
    private val none = emptySet<Cell>()

    private fun c(x: Int, y: Int) = Pair(x, y)

    // ── Selection ───────────────────────────────────────────────────────────

    @Test fun firstTapPrefersAcrossWhereBothExist() {
        assertEquals(Selection(c(0, 0), Direction.ACROSS), InputEngine.tap(board, null, c(0, 0)))
    }

    @Test fun repeatTapFlipsDirectionOnlyWhereBothExist() {
        var s = InputEngine.tap(board, null, c(0, 0))
        s = InputEngine.tap(board, s, c(0, 0)); assertEquals(Direction.DOWN, s!!.direction)
        s = InputEngine.tap(board, s, c(0, 0)); assertEquals(Direction.ACROSS, s!!.direction)
        // (1,0) belongs only to CAT: repeat taps keep ACROSS.
        s = InputEngine.tap(board, s, c(1, 0))
        s = InputEngine.tap(board, s, c(1, 0)); assertEquals(Selection(c(1, 0), Direction.ACROSS), s)
    }

    @Test fun tapKeepsDirectionWhenPossibleOtherwiseSwitches() {
        val down = Selection(c(0, 0), Direction.DOWN)
        assertEquals(Selection(c(2, 1), Direction.DOWN), InputEngine.tap(board, down, c(2, 1)))
        assertEquals(Selection(c(1, 0), Direction.ACROSS), InputEngine.tap(board, down, c(1, 0)))
    }

    @Test fun tapOffTheBoardChangesNothing() {
        val s = Selection(c(0, 0), Direction.ACROSS)
        assertEquals(s, InputEngine.tap(board, s, c(1, 1)))
    }

    @Test fun selectingAWordLandsOnItsFirstEmptyCell() {
        assertEquals(Selection(c(1, 0), Direction.ACROSS), InputEngine.selectWord(cat, mapOf(c(0, 0) to 'C'), none))
        assertEquals(Selection(c(0, 0), Direction.DOWN), InputEngine.selectWord(cow, emptyMap(), none))
    }

    // ── Typing ──────────────────────────────────────────────────────────────

    @Test fun typingWritesUppercaseAndAdvances() {
        val r = InputEngine.type(board, Selection(c(0, 0), Direction.ACROSS), emptyMap(), none, 'c')
        assertEquals(mapOf(c(0, 0) to 'C'), r.inputs)
        assertEquals(Selection(c(1, 0), Direction.ACROSS), r.selection)
        assertTrue(r.filled.isEmpty())
    }

    @Test fun typingSkipsCellsThatAlreadyHaveLetters() {
        val r = InputEngine.type(board, Selection(c(0, 0), Direction.ACROSS), mapOf(c(1, 0) to 'A'), none, 'C')
        assertEquals(Selection(c(2, 0), Direction.ACROSS), r.selection)
    }

    @Test fun typingWrapsBackToAnEarlierGap() {
        // C _ T typed at T position: the only gap left is (1,0).
        val r = InputEngine.type(board, Selection(c(2, 0), Direction.ACROSS), mapOf(c(0, 0) to 'C'), none, 'T')
        assertEquals(Selection(c(1, 0), Direction.ACROSS), r.selection)
    }

    @Test fun lockedLettersAreNeverOverwrittenAndAreSkipped() {
        val solvedCat = mapOf(c(0, 0) to 'C', c(1, 0) to 'A', c(2, 0) to 'T')
        val locked = board.lockedCells(solvedCat, emptySet())
        // Cursor on the locked C of COW: the letter goes into the next editable cell.
        val r = InputEngine.type(board, Selection(c(0, 0), Direction.DOWN), solvedCat, locked, 'O')
        assertEquals('C', r.inputs[c(0, 0)])
        assertEquals('O', r.inputs[c(0, 1)])
        assertEquals(Selection(c(0, 2), Direction.DOWN), r.selection)
    }

    @Test fun cursorStaysAtTheEndOfAFullWord() {
        val r = InputEngine.type(board, Selection(c(2, 0), Direction.ACROSS), mapOf(c(0, 0) to 'C', c(1, 0) to 'A'), none, 'T')
        assertEquals(Selection(c(2, 0), Direction.ACROSS), r.selection)
        assertEquals(listOf(cat), r.filled)
    }

    @Test fun oneLetterCanCompleteTheActiveAndACrossingWord() {
        // C A _ with T O E's O,E present: typing T fills CAT and TOE together.
        val before = mapOf(c(0, 0) to 'C', c(1, 0) to 'A', c(2, 1) to 'O', c(2, 2) to 'E')
        val r = InputEngine.type(board, Selection(c(2, 0), Direction.ACROSS), before, none, 'T')
        assertEquals(listOf(cat, toe), r.filled)
    }

    @Test fun retypingTheSameLetterInAFullWordDoesNotRejudgeIt() {
        val full = mapOf(c(0, 0) to 'C', c(1, 0) to 'A', c(2, 0) to 'X')
        assertTrue(InputEngine.type(board, Selection(c(2, 0), Direction.ACROSS), full, none, 'X').filled.isEmpty())
        assertEquals(listOf(cat), InputEngine.type(board, Selection(c(2, 0), Direction.ACROSS), full, none, 'T').filled)
    }

    @Test fun nonLettersAreIgnored() {
        val s = Selection(c(0, 0), Direction.ACROSS)
        assertEquals(InputResult(emptyMap(), s), InputEngine.type(board, s, emptyMap(), none, '3'))
    }

    // ── Backspace ───────────────────────────────────────────────────────────

    @Test fun backspaceClearsCurrentCellFirst() {
        val r = InputEngine.backspace(board, Selection(c(1, 0), Direction.ACROSS), mapOf(c(0, 0) to 'C', c(1, 0) to 'A'), none)
        assertEquals(mapOf(c(0, 0) to 'C'), r.inputs)
        assertEquals(Selection(c(1, 0), Direction.ACROSS), r.selection)
    }

    @Test fun backspaceOnEmptyCellStepsBackAndClears() {
        val r = InputEngine.backspace(board, Selection(c(2, 0), Direction.ACROSS), mapOf(c(0, 0) to 'C', c(1, 0) to 'A'), none)
        assertEquals(mapOf(c(0, 0) to 'C'), r.inputs)
        assertEquals(Selection(c(1, 0), Direction.ACROSS), r.selection)
    }

    @Test fun backspaceNeverErasesLockedLetters() {
        val inputs = mapOf(c(0, 0) to 'C', c(0, 1) to 'O')
        val locked = setOf(c(0, 0))
        val r1 = InputEngine.backspace(board, Selection(c(0, 2), Direction.DOWN), inputs, locked)
        assertEquals(mapOf(c(0, 0) to 'C'), r1.inputs)
        val r2 = InputEngine.backspace(board, r1.selection, r1.inputs, locked)
        assertEquals("C is locked", mapOf(c(0, 0) to 'C'), r2.inputs)
    }

    // ── Navigation ──────────────────────────────────────────────────────────

    @Test fun nextUnsolvedFollowsClueOrderAndWraps() {
        assertEquals(listOf(cat, cow, toe), board.clueOrder)
        val start = Selection(c(0, 0), Direction.ACROSS)
        assertEquals(Direction.DOWN, InputEngine.nextUnsolved(board, start, emptyMap(), none)!!.direction)
        val fromToe = Selection(c(2, 0), Direction.DOWN)
        assertEquals(Selection(c(0, 0), Direction.ACROSS), InputEngine.nextUnsolved(board, fromToe, emptyMap(), none))
        assertEquals(Selection(c(0, 0), Direction.DOWN),
            InputEngine.nextUnsolved(board, fromToe, emptyMap(), none, forward = false))
    }

    @Test fun nextUnsolvedSkipsSolvedWords() {
        val cowSolved = mapOf(c(0, 0) to 'C', c(0, 1) to 'O', c(0, 2) to 'W')
        val locked = board.lockedCells(cowSolved, emptySet())
        val s = InputEngine.nextUnsolved(board, Selection(c(1, 0), Direction.ACROSS), cowSolved, locked)
        assertEquals(Selection(c(2, 0), Direction.DOWN), s)
    }

    @Test fun initialSelectionIsFirstUnsolvedClue() {
        assertEquals(Selection(c(0, 0), Direction.ACROSS), InputEngine.initialSelection(board, emptyMap(), none))
    }

    // ── Clearing ────────────────────────────────────────────────────────────

    @Test fun clearingKeepsLockedLetters() {
        val inputs = mapOf(c(0, 0) to 'C', c(1, 0) to 'X', c(2, 1) to 'Q')
        val locked = setOf(c(0, 0))
        assertEquals(mapOf(c(0, 0) to 'C', c(2, 1) to 'Q'), InputEngine.clearWord(cat, inputs, locked))
        assertEquals(mapOf(c(0, 0) to 'C'), InputEngine.clearUnlocked(inputs, locked))
    }

    @Test fun lockedCellsIncludeSolvedWordsAndReveals() {
        val inputs = mapOf(c(0, 0) to 'C', c(1, 0) to 'A', c(2, 0) to 'T', c(2, 2) to 'E')
        assertEquals(setOf(c(0, 0), c(1, 0), c(2, 0), c(0, 2)), board.lockedCells(inputs, setOf(c(0, 2))))
    }
}
