package com.hag.mypersonalcrossword.core

import org.junit.Assert.assertEquals
import org.junit.Test

class SpokenTest {
    @Test fun decorationIsDroppedFromBothEnds() {
        assertEquals("Host Game", spokenLabel("🌐 Host Game"))
        assertEquals("Answer It", spokenLabel("Answer It  ▶"))
        assertEquals("Got it — Set Up Players", spokenLabel("Got it — Set Up Players ▶"))
        assertEquals("Yes, play today's puzzle!".trimEnd('!'), spokenLabel("📅 Yes, play today's puzzle!"))
    }

    @Test fun meaningfulPunctuationStays() {
        assertEquals("Pass  (−1 pt)", spokenLabel("Pass  (−1 pt)"))
        assertEquals("−1 hint", spokenLabel("−1 hint"))
        assertEquals("Log out", spokenLabel("Log out"))
    }

    @Test fun allSymbolLabelsAreLeftAlone() {
        assertEquals("▶", spokenLabel("▶"))
    }
}
