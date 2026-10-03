package com.hag.mypersonalcrossword.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** A finished board pays out once, even if a stale copy of it is restored. */
class LedgerTest {

    private val cat = PlacedWord("CAT", "Pet", 0, 0, true, number = 1)
    private val cow = PlacedWord("COW", "Moo", 0, 0, false, number = 1)

    @Test fun fingerprintIgnoresOrderNumberingAndClues() {
        val renumbered = listOf(cow.copy(number = 7, clue = "x"), cat.copy(category = "PETS"))
        assertEquals(puzzleFingerprint(listOf(cat, cow)), puzzleFingerprint(renumbered))
    }

    @Test fun fingerprintChangesWithPlacement() {
        val base = puzzleFingerprint(listOf(cat, cow))
        assertNotEquals(base, puzzleFingerprint(listOf(cat.copy(startX = 1), cow)))
        assertNotEquals(base, puzzleFingerprint(listOf(cat.copy(isHorizontal = false), cow.copy(isHorizontal = true))))
        assertNotEquals(base, puzzleFingerprint(listOf(cat.copy(word = "CAB"), cow)))
    }

    @Test fun ledgerRemembersAndCaps() {
        var ledger = ""
        for (i in 1..5) ledger = appendToLedger(ledger, "fp$i", cap = 3)
        assertFalse(ledgerContains(ledger, "fp2"))
        assertTrue(ledgerContains(ledger, "fp5"))
        assertEquals("fp3,fp4,fp5", ledger)
        // Re-adding moves it to the newest end without duplicating.
        assertEquals("fp4,fp5,fp3", appendToLedger(ledger, "fp3", cap = 3))
        assertFalse(ledgerContains("", "fp1"))
        assertFalse(ledgerContains("fp12", "fp1"))
    }
}
