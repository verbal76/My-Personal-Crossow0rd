package com.hag.mypersonalcrossword.desktop

import org.junit.Assert.*
import org.junit.Test

class LayoutTest {
    private val sizes = listOf(1280 to 800, 1920 to 1080, 2560 to 1440, 1024 to 640, 800 to 500, 3840 to 2160)

    @Test fun steamDeckReferenceLayoutIsReadable() {
        val l = PuzzleLayout.compute(1280, 800, 15, 15, keyboardOpen = false)
        assertEquals(1f, l.ui, 0.001f)
        assertTrue("cells at least 40px on the Deck", l.cell >= 40)
        assertTrue("clue panel at least 300px", l.clues.w >= 300)
    }

    @Test fun regionsStayInsideTheWindowAndNeverOverlap() {
        for ((w, h) in sizes) for (kb in listOf(false, true)) for ((c, r) in listOf(8 to 8, 15 to 15, 30 to 30)) {
            val l = PuzzleLayout.compute(w, h, c, r, kb)
            for (rect in listOfNotNull(l.header, l.board, l.clues, l.keyboard)) {
                assertTrue("$w x $h $rect", rect.x >= 0 && rect.y >= 0 && rect.right <= w && rect.bottom <= h)
            }
            assertTrue("board and clues don't overlap at $w x $h", l.board.right <= l.clues.x)
            l.keyboard?.let { assertTrue(l.board.bottom <= it.y) }
            assertTrue(l.cell >= 14)
        }
    }

    @Test fun bigBoardsScrollInsteadOfShrinkingBelowLegibility() {
        val l = PuzzleLayout.compute(1280, 800, 40, 40, false)
        assertTrue(l.cell >= 40); assertTrue(l.colsVisible < 40 && l.rowsVisible < 40)
    }

    @Test fun smallBoardsGetBigCellsButNotAbsurdOnes() {
        val l = PuzzleLayout.compute(1280, 800, 6, 6, false)
        assertTrue(l.cell <= 76); assertEquals(6, l.colsVisible)
    }

    @Test fun scalingFollowsWindowWithinBounds() {
        assertEquals(1.5f, PuzzleLayout.scale(1920, 1200), 0.001f)
        assertEquals(0.5f, PuzzleLayout.scale(100, 100), 0.001f)
        assertTrue(PuzzleLayout.scale(7680, 4320) <= 4f)
    }

    @Test fun cameraKeepsTheCursorVisible() {
        var cam = 0
        for (cursor in 0 until 40) {
            cam = PuzzleLayout.camera(cursor, 40, 10, cam)
            assertTrue("cursor $cursor cam $cam", cursor in cam until cam + 10)
            assertTrue(cam in 0..30)
        }
        for (cursor in 39 downTo 0) { cam = PuzzleLayout.camera(cursor, 40, 10, cam); assertTrue(cursor in cam until cam + 10) }
        assertEquals(0, PuzzleLayout.camera(5, 8, 10, 3))
    }

    @Test fun onScreenKeyboardNavigationWrapsAndHitTests() {
        val k = OnScreenKeyboard(); val area = Rect(10, 500, 1000, 200)
        val start = k.key()
        repeat(OnScreenKeyboard.ROWS[k.row].size) { k.move(1, 0) }; assertEquals("a full lap wraps to the start", start, k.key())
        repeat(OnScreenKeyboard.ROWS.size) { k.move(0, 1) }
        for (r in OnScreenKeyboard.ROWS.indices) for (c in OnScreenKeyboard.ROWS[r].indices) {
            val kr = k.keyRect(area, r, c)
            assertEquals(r to c, k.hit(area, kr.x + kr.w / 2, kr.y + kr.h / 2))
            assertTrue(kr.x >= area.x && kr.right <= area.right && kr.bottom <= area.bottom)
        }
        assertNull(k.hit(area, 0, 0))
    }

    @Test fun everyLetterIsReachableByDpadOnly() {
        val reachable = HashSet<String>(); val k = OnScreenKeyboard()
        val seen = HashSet<Pair<Int, Int>>(); val q = ArrayDeque(listOf(k.row to k.col))
        // BFS over cursor positions using the real move()
        fun at(r: Int, c: Int): OnScreenKeyboard = OnScreenKeyboard().also { it.set(r, c) }
        while (q.isNotEmpty()) {
            val (r, c) = q.removeFirst(); if (!seen.add(r to c)) continue
            reachable += OnScreenKeyboard.ROWS[r][c]
            for ((dx, dy) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) { val n = at(r, c); n.move(dx, dy); q += n.row to n.col }
        }
        for (ch in 'A'..'Z') assertTrue("$ch", ch.toString() in reachable)
        assertTrue("⌫" in reachable && "CLOSE" in reachable)
    }
}
