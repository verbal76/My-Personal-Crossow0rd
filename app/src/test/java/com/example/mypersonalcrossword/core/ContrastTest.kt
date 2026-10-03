package com.hag.mypersonalcrossword.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** White text on the player's chosen button colour is always readable. */
class ContrastTest {

    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()

    @Test fun contrastMatchesWcagReferenceValues() {
        assertEquals(21.0, contrastRatio(white, black), 0.01)
        assertEquals(1.0, contrastRatio(white, white), 0.0001)
        // #767676 is the classic "just passes 4.5:1 on white" grey.
        assertEquals(4.54, contrastRatio(0xFF767676.toInt(), white), 0.01)
    }

    @Test fun anyPickedColourIsDeepenedUntilWhiteTextReads() {
        val picks = listOf(0xFFFFFFFF, 0xFFFFEB3B, 0xFF00FFFF, 0xFF8BC34A, 0xFFFFC0CB, 0xFF6650A4, 0xFF000000)
            .map { it.toInt() }
        for (p in picks) {
            val safe = readableUnderWhiteText(p)
            assertTrue("%08X -> %08X".format(p, safe), contrastRatio(safe, white) >= MIN_TEXT_CONTRAST)
        }
    }

    @Test fun coloursThatAlreadyPassAreUnchanged() {
        // The default purple and black need no change.
        assertEquals(0xFF6650A4.toInt(), readableUnderWhiteText(0xFF6650A4.toInt()))
        assertEquals(black, readableUnderWhiteText(black))
    }

    @Test fun deepeningKeepsTheHue() {
        val yellow = readableUnderWhiteText(0xFFFFEB3B.toInt())
        val r = (yellow shr 16) and 0xFF; val g = (yellow shr 8) and 0xFF; val b = yellow and 0xFF
        assertTrue("still yellow-ish: r=$r g=$g b=$b", r >= g && g > b)
    }

    @Test fun textFallsBackWhenTheAccentIsTooDim() {
        val darkSurface = 0xFF1C1B1F.toInt()
        val lightText   = 0xFFE6E1E5.toInt()
        assertEquals(lightText, readableTextColor(0xFF6650A4.toInt(), darkSurface, lightText))
        assertEquals(0xFFFFEB3B.toInt(), readableTextColor(0xFFFFEB3B.toInt(), darkSurface, lightText))
    }
}
