package com.hag.mypersonalcrossword.core

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratorTest {

    private val data = TestAssets.wordData

    @Test fun everyCategoryAndDifficultyProducesAValidBoard() {
        for (cat in data.categories) {
            val pool = data.entries.filter { it.category == cat }
            for (diff in Difficulty.entries) {
                val board = generateCrossword(pool, diff.wordCount, emptySet(), Random(cat.hashCode() + diff.ordinal))
                assertTrue("$cat/$diff produced ${board.size} words", board.size >= 2)
                assertTrue("$cat/$diff overshot target", board.size <= diff.wordCount)
                assertValidBoard(board)
            }
        }
    }

    @Test fun smallAndMediumBoardsReachTheirTarget() {
        // Every category has ~200 answers; Easy/Medium/Hard must hit their promised word count.
        for (cat in data.categories) {
            val pool = data.entries.filter { it.category == cat }
            for (diff in listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD)) {
                val board = generateCrossword(pool, diff.wordCount, emptySet(), Random(7))
                assertEquals("$cat/$diff", diff.wordCount, board.size)
            }
        }
    }

    @Test fun seededGenerationIsDeterministic() {
        val pool = data.entries.filter { it.category == "FOOD" }
        val a = generateCrossword(pool, 15, emptySet(), Random(42))
        val b = generateCrossword(pool, 15, emptySet(), Random(42))
        assertEquals(a, b)
    }

    @Test fun placedWordsCarryTheirSourceCategory() {
        val pool = data.entries.filter { it.category == "FOOD" || it.category == "CITIES" }
        val board = generateCrossword(pool, 15, emptySet(), Random(3))
        val validPairs = pool.map { it.answer to it.category }.toSet()
        board.forEach { assertTrue("${it.word} -> ${it.category}", (it.word to it.category) in validPairs) }
    }

    @Test fun usedWordsAreDeprioritisedNotBanned() {
        val pool = data.entries.filter { it.category == "PETS" }
        val used = pool.map { it.answer }.toSet()                 // everything "used"
        val board = generateCrossword(pool, 8, used, Random(1))
        assertEquals("fully explored category must still generate", 8, board.size)
    }

    /**
     * Regression: a used word at a word edge scored -15 000 - 30 000, under the
     * -20 000 cutoff — effectively banned. It must lose to unused words only.
     */
    @Test fun usedWordAtAWordEdgeIsStillPlaceable() {
        val gridChar = HashMap<Cell, Char>()
        val gridDir  = HashMap<Cell, Boolean?>()
        addToGrid(PlacedWord("CAT", "", 0, 0, true), gridChar, gridDir)
        val index = buildWordIndex(listOf(RawEntry("TOE", "Foot digit", "X")))
        val placed = findBestPlacement(2, 0, false, 'T', setOf("TOE"), index, gridChar, gridDir, usedWords = setOf("TOE"))
        assertEquals(PlacedWord("TOE", "Foot digit", 2, 0, false, category = "X"), placed)

        // Fully used tiny pool: still a board, not "empty".
        val tiny = listOf(RawEntry("CAT", "Pet", "X"), RawEntry("TOE", "Foot digit", "X"), RawEntry("COW", "Farm animal", "X"))
        val board = generateCrossword(tiny, 3, tiny.map { it.answer }.toSet(), Random(1))
        assertEquals(3, board.size)
        assertValidBoard(board)
    }

    @Test fun unusedWordBeatsUsedWordForTheSameSpot() {
        val gridChar = HashMap<Cell, Char>()
        val gridDir  = HashMap<Cell, Boolean?>()
        addToGrid(PlacedWord("CAT", "", 0, 0, true), gridChar, gridDir)
        val pool  = listOf(RawEntry("TOE", "Foot digit", "X"), RawEntry("TIE", "Neckwear", "X"))
        val index = buildWordIndex(pool)
        for (used in listOf("TOE", "TIE")) {
            val placed = findBestPlacement(2, 0, false, 'T', setOf("TOE", "TIE"), index, gridChar, gridDir, setOf(used))
            assertTrue("$used is used", placed != null && placed.word != used)
        }
    }

    @Test fun partiallyUsedPoolPrefersUnusedAnswers() {
        // Half of each category already seen. Repeats should be rare overall (about
        // 6% today; 10% before board scoring counted them) and never dominate a board.
        var usedTotal = 0; var wordsTotal = 0
        for (cat in listOf("PETS", "FOOD", "CITIES")) {
            val pool = data.entries.filter { it.category == cat }
            val answers = pool.map { it.answer }.distinct().sorted()
            val used = answers.filterIndexed { i, _ -> i % 2 == 0 }.toSet()   // half the category seen before
            for (seed in 1..10) {
                val board = generateCrossword(pool, Difficulty.HARD.wordCount, used, Random(seed))
                assertEquals(Difficulty.HARD.wordCount, board.size)
                val usedCount = board.count { it.word in used }
                assertTrue("$cat/$seed placed $usedCount used of ${board.size}", usedCount * 3 <= board.size)
                usedTotal += usedCount; wordsTotal += board.size
                assertValidBoard(board)
            }
        }
        assertTrue("$usedTotal of $wordsTotal placed answers were repeats", usedTotal * 10 <= wordsTotal)
    }

    /**
     * The early stop used to compare the BEST board's size with THIS attempt's
     * intersections. Stopping early must only ever return a board that meets the
     * bar; otherwise the result is the same as spending every attempt. (Because a
     * connected board of n words always has ≥ n − 1 crossings, the old mix-up was
     * never observable on real data — this pins the invariant, see below.)
     */
    @Test fun earlyStopOnlyReturnsBoardsThatMeetTheBar() {
        fun intersections(b: List<PlacedWord>) = b.flatMap { it.cells() }.groupingBy { it }.eachCount().count { it.value > 1 }
        var stoppedEarly = 0
        for (cat in data.categories) {
            val pool = data.entries.filter { it.category == cat }
            for (diff in listOf(Difficulty.EXPERT, Difficulty.GENIUS)) for (seed in 1..2) {
                val target = diff.wordCount
                val early  = generateCrossword(pool, target, emptySet(), Random(seed))
                val full   = generateCrossword(pool, target, emptySet(), Random(seed), stopEarly = false)
                if (early == full) continue
                stoppedEarly++
                assertEquals("$cat/$diff/$seed stopped early with a short board", target, early.size)
                assertTrue("$cat/$diff/$seed stopped early with ${intersections(early)} intersections",
                    intersections(early) >= target - 2)
            }
        }
        assertTrue("early stop never triggered; test is vacuous", stoppedEarly > 0)
    }

    @Test fun emptyOrTinyPoolReturnsEmptyInsteadOfCrashing() {
        assertTrue(generateCrossword(emptyList(), 8, emptySet(), Random(1)).isEmpty())
        val one = listOf(RawEntry("CAT", "Pet", "PETS"))
        assertTrue(generateCrossword(one, 8, emptySet(), Random(1)).isEmpty())
    }

    /**
     * Regression: a crossing further down the line than the candidate word reaches
     * used to reject every shorter word (the constraint filter demanded all collected
     * constraints fit inside the word). COW must be placeable down from the C of CAT
     * even though BOX sits four rows below.
     */
    @Test fun shorterWordIsNotRejectedByCrossingBeyondItsEnd() {
        val gridChar = HashMap<Cell, Char>()
        val gridDir  = HashMap<Cell, Boolean?>()
        addToGrid(PlacedWord("CAT", "", 0, 0, true), gridChar, gridDir)
        addToGrid(PlacedWord("BOX", "", 0, 4, true), gridChar, gridDir)
        val pool  = listOf(RawEntry("COW", "Farm animal", "NATURE"))
        val index = buildWordIndex(pool)
        val placed = findBestPlacement(0, 0, false, 'C', setOf("COW"), index, gridChar, gridDir, emptySet())
        assertNotNull(placed)
        assertEquals(PlacedWord("COW", "Farm animal", 0, 0, false, category = "NATURE"), placed)
    }

    @Test fun everyBoardHasAtLeastOneCrossingPerAddedWord() {
        fun intersections(b: List<PlacedWord>) = b.flatMap { it.cells() }.groupingBy { it }.eachCount().count { it.value > 1 }
        for (cat in data.categories) for (diff in Difficulty.entries) {
            val board = generateCrossword(data.entries.filter { it.category == cat }, diff.wordCount, emptySet(), Random(11))
            assertTrue("$cat/$diff", intersections(board) >= board.size - 1)
        }
    }

    @Test fun boardValidatorRejectsAWordHiddenInsideALongerOne() {
        val nested = listOf(PlacedWord("CATS", "", 0, 0, true, 1), PlacedWord("CAT", "", 0, 0, true, 1))
        val error = runCatching { assertValidBoard(nested) }.exceptionOrNull()
        assertTrue("CAT inside CATS must be rejected", error is AssertionError && "maximal" in error.message.orEmpty())
        val shifted = listOf(PlacedWord("SCAT", "", 0, 0, true, 1), PlacedWord("CAT", "", 1, 0, true, 2))
        assertTrue(runCatching { assertValidBoard(shifted) }.exceptionOrNull() is AssertionError)
    }

    @Test fun areaIsInclusiveOnBothAxes() {
        assertEquals(3L, calculateArea(listOf(PlacedWord("CAT", "", 0, 0, true))))
        assertEquals(3L, calculateArea(listOf(PlacedWord("CAT", "", 0, 0, false))))
        assertEquals(9L, calculateArea(listOf(PlacedWord("CAT", "", 0, 0, true), PlacedWord("COW", "", 0, 0, false))))
    }

    @Test fun gridCellsKeepLowestNumberAtSharedStart() {
        val words = numberBoard(listOf(PlacedWord("CAT", "", 0, 0, true), PlacedWord("COW", "", 0, 0, false)))
        val cells = buildGridCells(words).associateBy { Pair(it.x, it.y) }
        assertEquals(1, cells.getValue(Pair(0, 0)).number)
        assertEquals(null, cells.getValue(Pair(1, 0)).number)
    }

    /**
     * Boards must branch, not be combs: one long word crossed by about half of all
     * the others (the old first-in-first-out growth). Measured as the most crossings
     * on any one word divided by the board's word count, averaged per difficulty.
     */
    @Test fun boardsBranchInsteadOfFormingACombAroundOneSpine() {
        val entries = TestAssets.wordData.entries
        val cats = entries.map { it.category }.distinct().sorted()
        fun spine(board: List<PlacedWord>): Double {
            val count = HashMap<Pair<Int, Int>, Int>()
            board.forEach { w -> w.cells().forEach { count[it] = (count[it] ?: 0) + 1 } }
            return board.maxOf { w -> w.cells().count { (count[it] ?: 0) > 1 } }.toDouble() / board.size
        }
        for (diff in listOf(Difficulty.HARD, Difficulty.EXPERT)) {
            val boards = cats.map { c -> generateCrossword(entries.filter { it.category == c }, diff.wordCount, emptySet(), SplitMix64(c.length * 7919L)) }
                .filter { it.size == diff.wordCount }
            val avg = boards.map(::spine).average()
            assertTrue("$diff average spine %.2f (comb boards were ~0.49)".format(avg), avg <= 0.42)
        }
        val dailies = (1..20).map { DailyPuzzle.generate(entries, "2026-11-%02d".format(it)) }
        val avg = dailies.map(::spine).average()
        assertTrue("Daily average spine %.2f (comb boards were 0.49)".format(avg), avg <= 0.40)
    }
}
