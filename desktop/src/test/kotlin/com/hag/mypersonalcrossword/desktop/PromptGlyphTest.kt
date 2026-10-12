package com.hag.mypersonalcrossword.desktop

import org.junit.Assert
import org.junit.Test

class PromptGlyphTest {
    @Test fun everyGlyphUsedByAPromptExistsAndIsReadable() {
        val names = PromptBar.allGlyphNames()
        Assert.assertTrue(names.size >= 15)
        for (n in names) Assert.assertNotNull("glyph $n", Glyphs.image(n))
    }
    @Test fun promptsFollowTheInputDevice() {
        val pad = PromptBar.forState(Screen.PUZZLE, "pad", false).flatMap { it.glyphs }
        val kb = PromptBar.forState(Screen.PUZZLE, "keyboard", false).flatMap { it.glyphs }
        Assert.assertTrue(pad.all { it.startsWith("xbox_") }); Assert.assertTrue(kb.all { it.startsWith("key_") })
        Assert.assertTrue(PromptBar.forState(Screen.STUDIO_SPLASH, "pad", false).isEmpty())
    }
    @Test fun mouseUsersGetKeyboardPrompts() {
        Assert.assertEquals(PromptBar.forState(Screen.MENU, "keyboard", false), PromptBar.forState(Screen.MENU, "mouse", false))
    }
}
