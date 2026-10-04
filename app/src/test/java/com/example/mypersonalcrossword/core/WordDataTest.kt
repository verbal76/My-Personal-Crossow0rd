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

    /** Regression: answers were upper-cased before validation, so ß→SS, ı→I, ﬁ→FI got in. */
    @Test fun nonAsciiLettersAreRejectedBeforeUppercasing() {
        val d = parse("straße,German street,PLACES", "ıce,Frozen water,FOOD", "ﬁsh,Swims,FOOD",
                      "CAFÉ,Coffee shop,FOOD", "CAT,Pet,peтs", "DOG,Pet,ﬁsh")
        assertTrue(d.entries.toString(), d.entries.isEmpty())
        assertEquals(6, d.rejected.size)
    }

    @Test fun controlCharactersInCluesAreRejected() {
        val d = parse("CAT,Pet\u0000x,PETS", "DOG,Pet\u0007,PETS", "RAT,Rod\tent,PETS", "COW,Farm \u001B animal,NATURE", "EMU,Big bird,NATURE")
        assertEquals(listOf("EMU"), d.entries.map { it.answer })
        assertEquals(4, d.rejected.size)
        assertTrue(d.rejected.all { it.reason.contains("control") })
    }

    /** Regression: without a header row the first entry was silently dropped. */
    @Test fun headerIsDetectedByContent() {
        assertEquals(listOf("CAT", "DOG"), parseWordCsv(sequenceOf("CAT,Pet,PETS", "DOG,Pet,PETS")).entries.map { it.answer })
        assertEquals(listOf("CAT"), parseWordCsv(sequenceOf("\uFEFFanswer , CLUE,category\r", "CAT,Pet,PETS")).entries.map { it.answer })
        assertEquals(listOf("CAT"), parseWordCsv(sequenceOf("", "Answer,Clue,Category", "CAT,Pet,PETS")).entries.map { it.answer })
        // A repeated header (concatenated files) is skipped, not turned into an entry.
        val d = parseWordCsv(sequenceOf("CAT,Pet,PETS", "Answer,Clue,Category"))
        assertEquals(listOf("CAT"), d.entries.map { it.answer })
        assertTrue(d.rejected.isEmpty())
        // A BOM-prefixed first data row is still data.
        assertEquals(listOf("CAT"), parseWordCsv(sequenceOf("\uFEFFCAT,Pet,PETS")).entries.map { it.answer })
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
