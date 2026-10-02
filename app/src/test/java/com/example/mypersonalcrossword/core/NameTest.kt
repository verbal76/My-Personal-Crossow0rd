package com.hag.mypersonalcrossword.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NameTest {

    private fun hasLoneSurrogate(s: String) = s.indices.any { i ->
        (s[i].isHighSurrogate() && (i + 1 >= s.length || !s[i + 1].isLowSurrogate())) ||
        (s[i].isLowSurrogate() && (i == 0 || !s[i - 1].isHighSurrogate()))
    }

    /** Regression: take(24) cut an emoji in half, leaving an unencodable lone surrogate. */
    @Test fun lengthCapNeverSplitsAnEmoji() {
        val s = sanitizeName("a".repeat(23) + "😀😀")
        assertEquals("a".repeat(23) + "😀", s)
        assertFalse(hasLoneSurrogate(s))
        assertEquals(s, String(s.toByteArray(Charsets.UTF_8), Charsets.UTF_8))

        assertEquals("😀".repeat(MAX_NAME_CODE_POINTS), sanitizeName("😀".repeat(30)))
        assertEquals(24, MAX_NAME_CODE_POINTS)
    }

    @Test fun shortNamesAndForbiddenCharacters() {
        assertEquals("Kev", sanitizeName("Kev"))
        assertEquals("Kev_2", sanitizeName("Kev_2"))                 // '_' stays allowed
        assertEquals("Kevin", sanitizeName("K§e|v;i\nn\r\t"))
        assertEquals("a".repeat(24), sanitizeName("a".repeat(40)))
        assertEquals("Ann 😀", normalizeName("  Ann 😀  "))
    }
}
