package com.hag.mypersonalcrossword.core

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail

/** Locates the shipped word list whether tests run from the module dir, repo root, or a harness. */
object TestAssets {
    fun wordCsv(): File {
        val candidates = listOfNotNull(
            System.getProperty("crossword.assets"),
            "src/main/assets",
            "app/src/main/assets"
        ).map { File(it, "test.csv") }
        return candidates.firstOrNull { it.exists() }
            ?: error("test.csv not found; looked in ${candidates.map { it.absolutePath }}")
    }

    val wordData: WordData by lazy { wordCsv().bufferedReader().useLines { parseWordCsv(it) } }
}

/**
 * Asserts a board is a legal crossword:
 *  • every placed word's letters are consistent where words cross,
 *  • every placed word is a maximal run (no letter just before its start or just
 *    after its end in its own direction — catches CAT placed inside CATS),
 *  • every maximal horizontal/vertical run of 2+ letters is exactly one placed word
 *    (no accidental adjacent-letter "non-words"),
 *  • all words are connected,
 *  • no answer appears twice,
 *  • clue numbers follow reading order and shared starts share a number.
 */
fun assertValidBoard(words: List<PlacedWord>) {
    val grid = HashMap<Cell, Char>()
    for (w in words) for (i in w.word.indices) {
        val c = w.cellAt(i)
        val prev = grid.put(c, w.word[i])
        if (prev != null && prev != w.word[i]) fail("conflict at $c: $prev vs ${w.word[i]} (${w.word})")
    }
    assertEquals("duplicate answers", words.size, words.map { it.word }.toSet().size)

    for (w in words) {
        val dx = if (w.isHorizontal) 1 else 0
        val dy = if (w.isHorizontal) 0 else 1
        val before = Pair(w.startX - dx, w.startY - dy)
        val after  = Pair(w.startX + w.word.length * dx, w.startY + w.word.length * dy)
        assertTrue("${w.word} is not a maximal run: letter before its start at $before", before !in grid)
        assertTrue("${w.word} is not a maximal run: letter after its end at $after", after !in grid)
    }

    val placedRuns = words.map { Triple(it.startX, it.startY, it.isHorizontal) to it.word.length }.toSet()
    for (h in listOf(true, false)) {
        for ((cell, _) in grid) {
            val (x, y) = cell
            val prev = if (h) Pair(x - 1, y) else Pair(x, y - 1)
            if (grid.containsKey(prev)) continue            // not the start of a run
            var len = 0
            while (grid.containsKey(if (h) Pair(x + len, y) else Pair(x, y + len))) len++
            if (len >= 2) assertTrue(
                "unplaced run of $len letters at $cell ${if (h) "across" else "down"}",
                Triple(x, y, h) to len in placedRuns
            )
        }
    }

    // Connectivity over shared cells.
    if (words.size > 1) {
        val cellsOf = words.map { it.cells().toSet() }
        val seen = BooleanArray(words.size); seen[0] = true
        val stack = ArrayDeque(listOf(0))
        while (stack.isNotEmpty()) {
            val i = stack.removeLast()
            for (j in words.indices) if (!seen[j] && cellsOf[i].any { it in cellsOf[j] }) { seen[j] = true; stack.add(j) }
        }
        assertTrue("disconnected board", seen.all { it })
    }

    // Numbering.
    val byStart = words.groupBy { Pair(it.startX, it.startY) }
    byStart.values.forEach { ws -> assertEquals("shared start, different numbers", 1, ws.map { it.number }.toSet().size) }
    val ordered = byStart.keys.sortedWith(compareBy({ it.second }, { it.first }))
    ordered.forEachIndexed { i, start -> assertEquals("numbering order", i + 1, byStart.getValue(start).first().number) }
}
