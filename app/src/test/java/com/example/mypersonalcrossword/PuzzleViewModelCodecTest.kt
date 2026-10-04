package com.hag.mypersonalcrossword

import com.hag.mypersonalcrossword.core.Direction
import com.hag.mypersonalcrossword.core.Selection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The cursor survives process death through SavedStateHandle. */
class PuzzleViewModelCodecTest {

    @Test fun selectionRoundTrips() {
        for (s in listOf(Selection(Pair(0, 0), Direction.ACROSS), Selection(Pair(12, -3), Direction.DOWN))) {
            assertEquals(s, PuzzleViewModel.decodeSelection(PuzzleViewModel.encodeSelection(s)))
        }
    }

    @Test fun missingOrCorruptSelectionIsNull() {
        assertNull(PuzzleViewModel.decodeSelection(PuzzleViewModel.encodeSelection(null)))
        assertNull(PuzzleViewModel.decodeSelection(null))
        assertNull(PuzzleViewModel.decodeSelection("1,2"))
        assertNull(PuzzleViewModel.decodeSelection("a,b,ACROSS"))
        assertNull(PuzzleViewModel.decodeSelection("1,2,SIDEWAYS"))
    }
}
