package com.hag.mypersonalcrossword.core

// ============================================================
// WORD DATA — parsing and validation of assets/test.csv
// ============================================================
// Format: header row, then `ANSWER,Clue text,CATEGORY` — plain CSV, no quoting.
// The clue may not contain commas or control characters; the answer must be
// A–Z only. Rows that break any rule are rejected (and reported) instead of
// silently producing bogus grid cells, bogus categories, or save-file delimiter
// collisions. Answers and categories are checked BEFORE upper-casing: "ß"
// upper-cases to "SS", dotless "ı" to "I" and "ﬁ" to "FI", which would
// otherwise slip in as answers nobody can type from the clue.

const val MIN_ANSWER_LENGTH = 3
const val MAX_ANSWER_LENGTH = 30

/** Characters reserved by SaveManager / Firebase serialisation. */
val RESERVED_DELIMITERS = setOf('§', '|', ';')

private val ANSWER_RE   = Regex("[A-Za-z]+")
private val CATEGORY_RE = Regex("[A-Za-z]+")
private val CONTROL_RE  = Regex("\\p{Cc}")

data class RejectedRow(val lineNumber: Int, val line: String, val reason: String)

data class WordData(val entries: List<RawEntry>, val rejected: List<RejectedRow>) {
    val categories: List<String> by lazy { entries.map { it.category }.distinct().sorted() }
}

/** True for the `Answer,Clue,Category` header row (any case, optional BOM/spaces). */
fun isWordCsvHeader(line: String): Boolean =
    line.trimStart('\uFEFF').trimEnd('\r').split(",").map { it.trim().lowercase() } ==
        listOf("answer", "clue", "category")

/**
 * Parses CSV lines. Header rows are recognised by content (and skipped wherever
 * they appear), so a file without one loses no data. [lines] is consumed lazily so an asset stream can
 * be passed straight through.
 */
fun parseWordCsv(lines: Sequence<String>): WordData {
    val entries  = mutableListOf<RawEntry>()
    val rejected = mutableListOf<RejectedRow>()
    val seen     = HashSet<Triple<String, String, String>>()

    lines.forEachIndexed { idx, rawLine ->
        val lineNo = idx + 1
        val line   = (if (idx == 0) rawLine.trimStart('\uFEFF') else rawLine).trimEnd('\r')
        if (line.isBlank() || isWordCsvHeader(line)) return@forEachIndexed

        val parts = line.split(",")
        if (parts.size != 3) {
            rejected += RejectedRow(lineNo, line, "expected 3 comma-separated fields, found ${parts.size}")
            return@forEachIndexed
        }
        val rawAnswer   = parts[0].trim()
        val clue        = parts[1].trim()
        val rawCategory = parts[2].trim()
        val answer      = rawAnswer.uppercase()
        val category    = rawCategory.uppercase()

        val reason = when {
            !ANSWER_RE.matches(rawAnswer)       -> "answer must be letters A-Z only"
            answer.length < MIN_ANSWER_LENGTH   -> "answer shorter than $MIN_ANSWER_LENGTH letters"
            answer.length > MAX_ANSWER_LENGTH   -> "answer longer than $MAX_ANSWER_LENGTH letters"
            clue.isEmpty()                      -> "empty clue"
            clue.any { it in RESERVED_DELIMITERS } -> "clue contains a reserved delimiter"
            CONTROL_RE.containsMatchIn(clue)    -> "clue contains a control character"
            !CATEGORY_RE.matches(rawCategory)   -> "category must be letters A-Z only"
            else                                -> null
        }
        if (reason != null) {
            rejected += RejectedRow(lineNo, line, reason)
            return@forEachIndexed
        }
        // Exact duplicate rows add nothing; the same answer with a *different*
        // clue is kept (the generator picks one per puzzle).
        if (seen.add(Triple(answer, clue, category))) entries += RawEntry(answer, clue, category)
    }
    return WordData(entries, rejected)
}

/** True when the answer appears as a whole word inside its own clue. */
fun clueLeaksAnswer(entry: RawEntry): Boolean =
    Regex("\\b" + Regex.escape(entry.answer.lowercase()) + "\\b").containsMatchIn(entry.clue.lowercase())

/**
 * Stable fingerprint of the word list. Two installs with the same fingerprint
 * generate identical Daily Puzzles for the same date.
 */
fun wordDataFingerprint(entries: List<RawEntry>): Long {
    var h = FNV_OFFSET
    entries.sortedWith(compareBy({ it.category }, { it.answer }, { it.clue })).forEach { e ->
        h = fnv1a(h, e.category); h = fnv1a(h, "\u0001")
        h = fnv1a(h, e.answer);   h = fnv1a(h, "\u0001")
        h = fnv1a(h, e.clue);     h = fnv1a(h, "\u0002")
    }
    return h
}

internal const val FNV_OFFSET = -0x340d631b7bdddcdbL   // 0xcbf29ce484222325
private  const val FNV_PRIME  = 0x100000001b3L

internal fun fnv1a(seed: Long, s: String): Long {
    var h = seed
    for (ch in s) {
        h = h xor ch.code.toLong()
        h *= FNV_PRIME
    }
    return h
}
