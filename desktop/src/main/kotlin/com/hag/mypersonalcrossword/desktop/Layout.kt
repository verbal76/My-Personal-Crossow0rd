package com.hag.mypersonalcrossword.desktop

import kotlin.math.max
import kotlin.math.min

data class Rect(val x: Int, val y: Int, val w: Int, val h: Int) {
    fun contains(px: Int, py: Int) = px >= x && py >= y && px < x + w && py < y + h
    val right: Int get() = x + w
    val bottom: Int get() = y + h
}

/**
 * Pure layout maths for the puzzle screen, so it can be tested at any window size
 * (1280x800 Steam Deck, 1920x1080, a small resized window) without a display.
 * Reference design is 1280x800 (16:10); everything scales by [ui] = min(w/1280, h/800).
 */
data class PuzzleLayout(
    val ui: Float, val header: Rect, val status: Rect, val message: Rect, val board: Rect, val clues: Rect, val keyboard: Rect?,
    val cell: Int, val colsVisible: Int, val rowsVisible: Int
) {
    /** Font size in px that never drops below [minPx] at the 1280x800 reference. */
    fun font(basePx: Float, minPx: Float = 0f) = max(basePx * ui, minPx).let { if (ui < 1f) max(it, minPx * ui.coerceAtLeast(0.75f)) else it }

    companion object {
        const val REF_W = 1280f; const val REF_H = 800f
        const val MIN_CELL_REF = 40; const val MAX_CELL_REF = 76

        fun scale(w: Int, h: Int): Float = min(w / REF_W, h / REF_H).coerceIn(0.5f, 4f)

        fun compute(w: Int, h: Int, cols: Int, rows: Int, keyboardOpen: Boolean, statusStrip: Boolean = false): PuzzleLayout {
            val ui = scale(w, h)
            val pad = (16 * ui).toInt()
            val headerH = (56 * ui).toInt()
            val header = Rect(0, 0, w, headerH)
            val promptH = (46 * ui).toInt()                      // glyph legend strip along the bottom
            val kbH = if (keyboardOpen) (210 * ui).toInt() else 0
            val kb = if (keyboardOpen) Rect(pad, h - promptH - kbH, w - 2 * pad, kbH) else null
            val statusH = if (statusStrip) (32 * ui).toInt() else 0     // turn / score line for Team and Vindictive
            val msgH = (28 * ui).toInt()                                // one line for the latest message (correct / not quite / taunt)
            val bodyTop = headerH + statusH + msgH + pad
            val bodyBottom = h - promptH - (if (keyboardOpen) kbH + pad else 0)
            val clueW = (w * 0.32f).toInt().coerceAtLeast((300 * ui).toInt())
            val board = Rect(pad, bodyTop, w - clueW - 3 * pad, max(bodyBottom - bodyTop, 1))
            val clues = Rect(board.right + pad, bodyTop, clueW, board.h)
            val fit = min(board.w / max(cols, 1), board.h / max(rows, 1))
            val cell = fit.coerceIn((MIN_CELL_REF * ui).toInt().coerceAtLeast(14), (MAX_CELL_REF * ui).toInt().coerceAtLeast(20))
            val cv = min(cols, max(board.w / cell, 1)); val rv = min(rows, max(board.h / cell, 1))
            return PuzzleLayout(ui, header, Rect(0, headerH, w, statusH), Rect(pad, headerH + statusH, w - 2 * pad, msgH), board, clues, kb, cell, cv, rv)
        }

        /** First visible column/row so the cursor stays on screen (and the board isn't over-scrolled). */
        fun camera(cursor: Int, total: Int, visible: Int, current: Int): Int {
            if (visible >= total) return 0
            var o = current.coerceIn(0, total - visible)
            if (cursor < o + 1) o = cursor - 1
            if (cursor > o + visible - 2) o = cursor - visible + 2
            return o.coerceIn(0, total - visible)
        }
    }
}

/** On-screen keyboard geometry + cursor, navigable by D-pad and tappable by touch. */
class OnScreenKeyboard {
    companion object {
        val ROWS: List<List<String>> = listOf(
            "QWERTYUIOP".map { it.toString() },
            "ASDFGHJKL".map { it.toString() },
            "ZXCVBNM".map { it.toString() } + listOf("⌫"),
            listOf("FLIP", "HINT", "NEXT", "GIVE", "PASS", "CLOSE")
        )
    }
    var row = 1; private set
    var col = 4; private set
    fun key(): String = ROWS[row][col]
    fun move(dx: Int, dy: Int) {
        if (dy != 0) {
            val old = ROWS[row].size
            row = Math.floorMod(row + dy, ROWS.size)
            col = (col * ROWS[row].size / old).coerceIn(0, ROWS[row].size - 1)
        } else col = Math.floorMod(col + dx, ROWS[row].size)
    }
    fun set(r: Int, c: Int) { row = r.coerceIn(0, ROWS.size - 1); col = c.coerceIn(0, ROWS[row].size - 1) }

    /** Rectangle of key (r,c) inside [area]. Each row's keys share the row width equally. */
    fun keyRect(area: Rect, r: Int, c: Int): Rect {
        val rowH = area.h / ROWS.size; val n = ROWS[r].size; val kw = area.w / 10
        val rowW = if (r < 3) kw * n else area.w
        val keyW = if (r < 3) kw else area.w / n
        val x0 = area.x + (area.w - rowW) / 2
        return Rect(x0 + c * keyW, area.y + r * rowH, keyW, rowH)
    }
    fun hit(area: Rect, px: Int, py: Int): Pair<Int, Int>? {
        for (r in ROWS.indices) for (c in ROWS[r].indices) if (keyRect(area, r, c).contains(px, py)) return r to c
        return null
    }
}
