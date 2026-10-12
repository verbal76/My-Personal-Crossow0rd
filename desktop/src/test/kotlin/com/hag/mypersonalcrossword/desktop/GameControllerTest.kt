package com.hag.mypersonalcrossword.desktop

import com.hag.mypersonalcrossword.core.*
import org.junit.Assert.*
import org.junit.Test

class GameControllerTest {
    @Test fun everyDifficultyGeneratesAPlayableBoard() {
        for (d in Difficulty.values()) {
            val g = T.controller(); assertTrue("$d", g.startPuzzle("MIXED", d))
            assertTrue(g.board!!.words.size >= 2); assertNotNull(g.selection)
        }
    }

    @Test fun solvingByTypingCompletesAndPaysOnce() {
        val store = ProfileStore(T.dir()); val g = T.controller(store)
        g.startPuzzle("MIXED", Difficulty.EASY); T.solve(g)
        val r = g.result!!
        assertEquals(Economy.pointsForDifficulty(Difficulty.EASY), store.score)
        assertTrue(r.paid); assertEquals(0, r.hints)
        assertFalse(g.active)
        assertEquals("a finished puzzle leaves no save", 0, store.savedSessions().size)
    }

    @Test fun samePuzzleNeverPaysTwice() {
        val store = ProfileStore(T.dir())
        val a = T.controller(store, seed = 3); a.startPuzzle("MIXED", Difficulty.EASY); T.solve(a)
        val once = store.score
        val b = T.controller(store, seed = 3); b.startPuzzle("MIXED", Difficulty.EASY); T.solve(b)
        assertEquals(once, store.score); assertFalse(b.result!!.paid)
    }

    @Test fun hintCostsOnePointAndRevealsALockedLetter() {
        val store = ProfileStore(T.dir()); store.score = 10
        val g = T.controller(store); g.startPuzzle("MIXED", Difficulty.HARD)
        assertTrue(g.hint())
        assertEquals(10 - Economy.HINT_COST, store.score)
        assertEquals(1, g.session!!.hintsUsed); assertEquals(1, g.revealed.size)
        assertTrue(g.revealed.first() in g.locked)
    }

    @Test fun finishingWithHintsNetsRewardMinusHints() {
        val store = ProfileStore(T.dir()); val g = T.controller(store)
        g.startPuzzle("MIXED", Difficulty.MEDIUM); g.hint(); g.hint(); T.solve(g)
        assertEquals(Economy.pointsForDifficulty(Difficulty.MEDIUM) - 2, store.score)
        assertEquals(Economy.netPoints(Difficulty.MEDIUM, false, 2), g.result!!.net)
    }

    @Test fun wrongLettersStayForCorrectionAndFixedWordSolves() {
        val g = T.controller(); g.startPuzzle("MIXED", Difficulty.EASY)
        val w = g.board!!.clueOrder.first(); g.selectWord(w)
        val wrong = if (w.word[0] == 'Z') 'Q' else 'Z'
        for (c in w.word.indices) g.type(if (c == 0) wrong else w.word[c])
        assertFalse(isWordSolved(w, g.inputs)); assertTrue(g.inputs.isNotEmpty())
        g.selectWord(w); g.move(0, 0)
        g.tap(w.cellAt(0)); g.type(w.word[0])
        assertTrue(isWordSolved(w, g.inputs))
    }

    @Test fun backspaceErasesAndSolvedLettersAreProtected() {
        val g = T.controller(); g.startPuzzle("MIXED", Difficulty.EASY)
        val w = g.board!!.clueOrder.first(); g.selectWord(w)
        g.type(w.word[0]); g.backspace()
        assertNull(g.inputs[w.cellAt(0)])
        for (ch in w.word) g.type(ch)
        assertTrue(isWordSolved(w, g.inputs))
        val before = g.inputs
        g.tap(w.cellAt(0)); g.backspace()
        assertEquals("a solved word's letters can't be erased", before.filterKeys { it in w.cells() }, g.inputs.filterKeys { it in w.cells() })
    }

    @Test fun arrowsMoveAcrossBoardSquaresOnly() {
        val g = T.controller(); g.startPuzzle("MIXED", Difficulty.MEDIUM)
        repeat(30) { g.move(1, 0); g.move(0, 1) }
        assertTrue(g.board!!.contains(g.selection!!.cell))
    }

    @Test fun tapFlipsDirectionOnCrossings() {
        val g = T.controller(); g.startPuzzle("MIXED", Difficulty.MEDIUM)
        val cross = g.board!!.solution.keys.first { g.board!!.wordsAt(it).size == 2 }
        g.tap(cross); val d = g.selection!!.direction
        g.flip(); assertEquals(d.other, g.selection!!.direction)
    }

    @Test fun nextClueCyclesThroughUnsolvedWords() {
        val g = T.controller(); g.startPuzzle("MIXED", Difficulty.EASY)
        val seen = HashSet<PlacedWord?>(); repeat(g.board!!.words.size) { seen += g.activeWord; g.nextClue() }
        assertEquals(g.board!!.words.size, seen.size)
    }

    @Test fun savedPuzzleResumesWithEverything() {
        val store = ProfileStore(T.dir()); val a = T.controller(store)
        a.startPuzzle("MIXED", Difficulty.MEDIUM); a.hint(); a.type('Q'); a.advance(65_000); a.unload()
        val b = T.controller(store); assertTrue(b.resume(store.savedSessions().first().slot.id))
        assertEquals(1, b.session!!.hintsUsed); assertEquals(65L, b.session!!.elapsedSeconds)
        assertTrue(b.inputs.size >= 2)
    }

    @Test fun clockCarriesFractionsOfSeconds() {
        val g = T.controller(); g.startPuzzle("MIXED", Difficulty.EASY)
        repeat(10) { g.advance(250) }
        assertEquals(2L, g.session!!.elapsedSeconds)
    }

    @Test fun dailyIsIdenticalOnEveryDeviceAndPaysTwentyOncePerDay() {
        val s1 = ProfileStore(T.dir()); val s2 = ProfileStore(T.dir())
        val a = T.controller(s1, seed = 1); val b = T.controller(s2, seed = 999)   // rng must not matter
        a.startDaily(); b.startDaily()
        assertEquals(a.board!!.words, b.board!!.words)
        T.solve(a)
        assertEquals(Economy.DAILY_POINTS, s1.score); assertTrue(a.dailyDoneToday)
        val again = T.controller(s1); again.startDaily(); T.solve(again)
        assertEquals("no second Daily payout", Economy.DAILY_POINTS, s1.score)
    }

    @Test fun dailyMatchesTheSharedCoreGenerator() {
        val g = T.controller(); g.startDaily()
        assertEquals(DailyPuzzle.generate(T.entries, g.dailyKeyToday), g.board!!.words)
    }

    @Test fun categoryPuzzlesOnlyUseThatCategory() {
        val cat = T.entries.map { it.category }.distinct().sorted().first()
        val g = T.controller(); assertTrue(g.startPuzzle(cat, Difficulty.EASY))
        assertTrue(g.board!!.words.all { it.category == cat })
    }
}
