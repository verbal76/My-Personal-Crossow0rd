package com.hag.mypersonalcrossword.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordDataTest {

    private fun parse(vararg rows: String) = parseWordCsv(sequenceOf("Answer,Clue,Category", *rows))

    @Test fun validRowsParseAndNormalise() {
        val d = parse("cat,Purring pet,pets", " COW , Farm animal , NATURE ")
        assertEquals(listOf(RawEntry("CAT", "Purring pet", "PETS"), RawEntry("COW", "Farm animal", "NATURE")), d.entries)
        assertTrue(d.rejected.isEmpty())
    }

    /** Regression: "A,ROD Yankees slugger,ATHLETES"-style rows used to create 1-letter answers. */
    @Test fun extraCommaIsRejectedNotSplitIntoBogusFields() {
        val d = parse("A,ROD Yankees slugger,ATHLETES", "LA,PAZ,Seat of government,CITIES")
        assertTrue(d.entries.isEmpty())
        assertEquals(2, d.rejected.size)
    }

    @Test fun unsupportedAnswersAreRejected() {
        val d = parse("UP,Pixar film,ENTERTAINMENT", "ICE CREAM,Frozen treat,FOOD", "A-ROD,Slugger,ATHLETES",
                      "X".repeat(MAX_ANSWER_LENGTH + 1) + ",Too long,FOOD")
        assertTrue(d.entries.isEmpty())
        assertEquals(4, d.rejected.size)
    }

    @Test fun delimiterInClueIsRejected() {
        val d = parse("CAT,Pet | feline,PETS", "DOG,Pet § canine,PETS", "RAT,Pet; rodent,PETS")
        assertTrue(d.entries.isEmpty())
    }

    @Test fun exactDuplicatesCollapseButAlternateCluesSurvive() {
        val d = parse("ROGER,Klotz the bully,CARTOONS", "ROGER,Klotz the bully,CARTOONS", "ROGER,American Dad alien,CARTOONS")
        assertEquals(2, d.entries.size)
    }

    @Test fun leakDetection() {
        assertTrue(clueLeaksAnswer(RawEntry("CARS", "Pixar movie with race cars", "X")))
        assertTrue(!clueLeaksAnswer(RawEntry("CAR", "Cartoon about racing", "X")))   // substring, not word
    }

    // ── The shipped word list ───────────────────────────────────────────────

    @Test fun shippedWordListHasNoRejectedRows() {
        val bad = TestAssets.wordData.rejected
        assertTrue("rejected rows:\n" + bad.joinToString("\n"), bad.isEmpty())
    }

    @Test fun shippedCluesNeverContainTheirAnswer() {
        val leaks = TestAssets.wordData.entries.filter(::clueLeaksAnswer)
        assertTrue("clues leaking answers:\n" + leaks.joinToString("\n"), leaks.isEmpty())
    }

    @Test fun shippedCluesHaveNoAuthoringLeftovers() {
        val junk = Regex("\\bclue (uses|says)\\b|\\bclue last name\\b|style not needed", RegexOption.IGNORE_CASE)
        val bad = TestAssets.wordData.entries.filter { junk.containsMatchIn(it.clue) }
        assertTrue("garbled clues:\n" + bad.joinToString("\n"), bad.isEmpty())
    }

    @Test fun everyCategoryIsLargeEnoughForGenius() {
        val counts = TestAssets.wordData.entries.groupBy { it.category }.mapValues { it.value.size }
        assertEquals(16, counts.size)
        counts.forEach { (cat, n) -> assertTrue("$cat has only $n entries", n >= Difficulty.GENIUS.wordCount * 3) }
    }
}
