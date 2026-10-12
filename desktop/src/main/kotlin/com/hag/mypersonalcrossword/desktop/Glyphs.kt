package com.hag.mypersonalcrossword.desktop

import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage

/**
 * Controller / keyboard prompt glyphs from the studio glyph library (see resources/glyphs/NOTICE.txt).
 * Xbox-style ABXY is used for every pad (it is what Steam Deck and Steam Input present).
 */
object Glyphs {
    private val cache = HashMap<String, BufferedImage?>()

    fun image(name: String): BufferedImage? = synchronized(cache) {
        cache.getOrPut(name) { if (name == "xbox_menu") menuGlyph() else Assets.image("glyphs/$name.png")?.let(::trim) }
    }

    /** Crops transparent margins so wide keys (Tab, Space, Enter) draw at a readable size. */
    private fun trim(img: BufferedImage): BufferedImage {
        var x0 = img.width; var y0 = img.height; var x1 = -1; var y1 = -1
        for (y in 0 until img.height) for (x in 0 until img.width) if ((img.getRGB(x, y) ushr 24) > 16) {
            if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y
        }
        return if (x1 < x0) img else img.getSubimage(x0, y0, x1 - x0 + 1, y1 - y0 + 1)
    }

    /** The library has no Start/Menu glyph, so this draws a plain, clearly generic one. */
    private fun menuGlyph(): BufferedImage {
        val img = BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.color = Color(38, 38, 40); g.fillOval(2, 2, 124, 124)
        g.color = Color.WHITE; g.stroke = BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        for (y in listOf(40, 64, 88)) g.drawLine(38, y, 90, y)
        g.dispose(); return img
    }
}

/** One legend entry: icons followed by a label, e.g. [xbox_a] "Select". */
data class Prompt(val glyphs: List<String>, val label: String)

object PromptBar {
    private val DPAD = listOf("xbox_dpad", "xbox_lstick")
    private val ARROWS = listOf("key_up", "key_down", "key_left", "key_right")

    /** What to show along the bottom for the current screen and last-used input device. */
    fun forState(screen: Screen, input: String, keyboardOpen: Boolean): List<Prompt> {
        val pad = input == "pad"
        return when (screen) {
            Screen.MENU -> if (pad) listOf(Prompt(DPAD, "Move / change"), Prompt(listOf("xbox_a"), "Select"))
                else listOf(Prompt(ARROWS, "Move / change"), Prompt(listOf("key_enter"), "Select"), Prompt(listOf("key_f11"), "Fullscreen"))
            Screen.PUZZLE -> when {
                pad && keyboardOpen -> listOf(Prompt(DPAD, "Pick key"), Prompt(listOf("xbox_a"), "Type"), Prompt(listOf("xbox_b"), "Close"),
                    Prompt(listOf("xbox_x"), "Flip"), Prompt(listOf("xbox_y"), "Hint"), Prompt(listOf("xbox_lb", "xbox_rb"), "Clue"))
                pad -> listOf(Prompt(DPAD, "Move"), Prompt(listOf("xbox_a"), "Keyboard"), Prompt(listOf("xbox_x"), "Flip"),
                    Prompt(listOf("xbox_y"), "Hint"), Prompt(listOf("xbox_lb", "xbox_rb"), "Clue"), Prompt(listOf("xbox_menu"), "Menu"))
                else -> listOf(Prompt(ARROWS, "Move"), Prompt(listOf("key_tab"), "Next clue"), Prompt(listOf("key_space"), "Flip"),
                    Prompt(listOf("key_f1"), "Hint"), Prompt(listOf("key_esc"), "Menu"))
            }
            Screen.PAUSE -> if (pad) listOf(Prompt(DPAD, "Move"), Prompt(listOf("xbox_a"), "Select"), Prompt(listOf("xbox_b"), "Resume"))
                else listOf(Prompt(ARROWS, "Move"), Prompt(listOf("key_enter"), "Select"), Prompt(listOf("key_esc"), "Resume"))
            Screen.RESULTS -> listOf(Prompt(listOf(if (pad) "xbox_a" else "key_enter"), "Continue"))
            else -> emptyList()
        }
    }

    fun allGlyphNames(): Set<String> =
        Screen.values().flatMap { s -> listOf("pad", "keyboard").flatMap { i -> listOf(false, true).flatMap { k -> forState(s, i, k) } } }
            .flatMap { it.glyphs }.toSet()
}
