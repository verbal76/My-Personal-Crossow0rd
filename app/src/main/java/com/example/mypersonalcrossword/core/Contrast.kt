package com.hag.mypersonalcrossword.core

import kotlin.math.pow

// ── COLOUR CONTRAST (WCAG 2.x) ────────────────────────────────────────────────
// Players pick their own button colour, and every button, header and tile puts
// white text on it. These keep that text readable whatever they pick.

/** WCAG relative luminance of an ARGB colour, 0 (black) to 1 (white). */
fun relativeLuminance(argb: Int): Double {
    fun channel(c: Int): Double {
        val s = c / 255.0
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel((argb shr 16) and 0xFF) +
           0.7152 * channel((argb shr 8) and 0xFF) +
           0.0722 * channel(argb and 0xFF)
}

/** WCAG contrast ratio between two colours, 1 to 21. */
fun contrastRatio(a: Int, b: Int): Double {
    val la = relativeLuminance(a); val lb = relativeLuminance(b)
    return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
}

/** WCAG AA for normal-size text. */
const val MIN_TEXT_CONTRAST = 4.5

/**
 * The player's colour, deepened just enough that white text on it meets
 * [MIN_TEXT_CONTRAST]. Hue is kept (all channels scale together); colours that
 * already pass are returned unchanged. Alpha is forced opaque.
 */
fun readableUnderWhiteText(argb: Int): Int {
    var r = (argb shr 16) and 0xFF
    var g = (argb shr 8) and 0xFF
    var b = argb and 0xFF
    fun pack() = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    var c = pack()
    var guard = 0
    while (contrastRatio(c, WHITE) < MIN_TEXT_CONTRAST && guard++ < 100) {
        r = (r * 0.92).toInt(); g = (g * 0.92).toInt(); b = (b * 0.92).toInt()
        c = pack()
    }
    return c
}

/** [preferred] when it reads well on [background], otherwise [fallback]. */
fun readableTextColor(preferred: Int, background: Int, fallback: Int): Int =
    if (contrastRatio(preferred, background) >= MIN_TEXT_CONTRAST) preferred else fallback

private const val WHITE = 0xFFFFFFFF.toInt()
