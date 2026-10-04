package com.hag.mypersonalcrossword.core

// ============================================================
// CORE MODEL — pure Kotlin, no Android dependencies.
// Everything in this package is unit-tested on the JVM (app/src/test/.../core).
// ============================================================

enum class GameMode { SINGLE, TEAM, VINDICTIVE, DAILY }

/** Vindictive turn phases. (Historical spelling kept — it is persisted in saves and Firebase.) */
enum class VindicativePhase { PICK_OWN, ASSIGN_CLUE, OPPONENT_WAIT }

enum class OnlineRole { HOST, GUEST }

// Word count caps for each difficulty level
enum class Difficulty(val label: String, val wordCount: Int, val emoji: String) {
    EASY("Easy",     8,  "🟢"),
    MEDIUM("Medium", 12, "🟡"),
    HARD("Hard",     15, "🔴"),
    EXPERT("Expert", 25, "🟣"),
    GENIUS("Genius", 50, "⭐")
}

data class RawEntry(val answer: String, val clue: String, val category: String)

/**
 * A word placed on the board. [category] is the real source category of the entry
 * (empty for legacy saves written before it existed) — used to credit combined and
 * daily puzzles to the right per-category history.
 */
data class PlacedWord(
    val word: String,
    val clue: String,
    val startX: Int,
    val startY: Int,
    val isHorizontal: Boolean,
    val number: Int = 0,
    val category: String = ""
)

data class GridCell(val x: Int, val y: Int, val char: Char, val number: Int? = null)

typealias Cell = Pair<Int, Int>

// Serialised as: category§diff§mode§hints§timeSeconds§score§partner§partnerScore§won
data class StatRecord(
    val category:     String,
    val diffName:     String,
    val gameMode:     String,   // GameMode.name
    val hintsUsed:    Int,
    val timeSeconds:  Long,
    val score:        Int,
    val partner:      String  = "",
    val partnerScore: Int     = 0,
    val won:          Boolean = true
)

// Forbidden delimiter chars in player / partner names. SaveManager serialises
// stat and puzzle records with §, |, and ; — letting the user type any of those
// silently corrupts the next save. Strip them at input time.
// The length cap counts code points, so an emoji at the boundary is kept or
// dropped whole — never split into a lone surrogate (which isn't valid UTF-8).
private val FORBIDDEN_NAME_CHARS = setOf('§', '|', ';', '\n', '\r', '\t')
const val MAX_NAME_CODE_POINTS = 24
fun sanitizeName(s: String): String {
    val filtered = s.filter { it !in FORBIDDEN_NAME_CHARS }
    val count    = filtered.codePointCount(0, filtered.length)
    return if (count <= MAX_NAME_CODE_POINTS) filtered
           else filtered.substring(0, filtered.offsetByCodePoints(0, MAX_NAME_CODE_POINTS))
}

/** Trimmed, sanitised form used as the persistent profile key. */
fun normalizeName(s: String): String = sanitizeName(s).trim()

// ── GEOMETRY HELPERS ─────────────────────────────────────────────────────────

fun PlacedWord.cellAt(i: Int): Cell =
    if (isHorizontal) Pair(startX + i, startY) else Pair(startX, startY + i)

fun PlacedWord.cells(): List<Cell> = word.indices.map { cellAt(it) }

fun getCellsForWord(w: PlacedWord): Set<Cell> = w.cells().toSet()

// Returns true when every cell of this word is correctly filled by the player.
fun isWordSolved(word: PlacedWord, userInputs: Map<Cell, Char>): Boolean =
    word.word.indices.all { i -> userInputs[word.cellAt(i)] == word.word[i] }

/** True when every cell of the word holds *some* letter (right or wrong). */
fun isWordFilled(word: PlacedWord, userInputs: Map<Cell, Char>): Boolean =
    word.word.indices.all { i -> userInputs[word.cellAt(i)] != null }

// Bounding-box area of a board. Both ends inclusive in both directions.
fun calculateArea(words: List<PlacedWord>): Long {
    if (words.isEmpty()) return 0
    val minX = words.minOf { it.startX }
    val maxX = words.maxOf { if (it.isHorizontal) it.startX + it.word.length - 1 else it.startX }
    val minY = words.minOf { it.startY }
    val maxY = words.maxOf { if (it.isHorizontal) it.startY else it.startY + it.word.length - 1 }
    return (maxX - minX + 1).toLong() * (maxY - minY + 1).toLong()
}

fun buildGridCells(words: List<PlacedWord>): List<GridCell> {
    val map = LinkedHashMap<Cell, GridCell>()
    words.forEach { w ->
        for (i in w.word.indices) {
            val pos      = w.cellAt(i)
            val existing = map[pos]
            val newNumber = when {
                i != 0       -> existing?.number          // interior cell: never assign a start number
                existing?.number == null -> w.number      // first word to claim this cell as its start
                else -> minOf(existing.number, w.number)  // two words start here: keep the lower number
            }
            map[pos] = GridCell(pos.first, pos.second, w.word[i], newNumber)
        }
    }
    return map.values.toList()
}
