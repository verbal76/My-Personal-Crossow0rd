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
}
