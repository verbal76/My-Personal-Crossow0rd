package com.hag.mypersonalcrossword.core

// ============================================================
// PUZZLE SESSION — everything needed to restore a puzzle in progress,
// plus the save-slot identity that keeps modes from overwriting each other.
// ============================================================

/**
 * Identifies one resumable save. SINGLE slots keep the legacy id format
 * (`CATEGORY__DIFFICULTY`) so saves written by earlier versions still resume.
 * Every other mode is namespaced (`TEAM::CATEGORY__DIFFICULTY`) so a Team or
 * Vindictive game can never overwrite — or be resumed as — a solo puzzle.
 * DAILY slots use the UTC date key as their category.
 *
 * Combined puzzles: [category] is the slot KEY, not the display label. Build
 * the slot with [forPuzzle] (or [combinedKey]) so it is the sorted, de-duplicated
 * source categories joined by `+` (`CITIES+FOOD+PETS+SPORTS__HARD`). The label
 * the UI shows ("4 Categories", "FOOD+CITIES") lives in [PuzzleSession.category]
 * and is free to collide; the key isn't. Category names are A–Z only, so `+`
 * can't occur in a single-category slot.
 */
data class SaveSlot(val mode: GameMode, val category: String, val difficulty: Difficulty) {
    val id: String
        get() = if (mode == GameMode.SINGLE) "${category}__${difficulty.name}"
                else "${mode.name}::${category}__${difficulty.name}"

    /** True for a slot keyed by a combined category set (new or legacy `A+B` label). */
    val isCombined: Boolean get() = '+' in category

    /**
     * Source categories of a combined slot, sorted (empty otherwise) — enough to
     * regenerate the puzzle when its save can't be loaded. Legacy "N Categories"
     * slots don't record their categories and return empty.
     */
    val combinedCategories: List<String>
        get() = if (isCombined) category.split('+').filter { it.isNotBlank() }.distinct().sorted() else emptyList()

    companion object {
        /** Canonical slot key for a combined puzzle: sorted, distinct, `+`-joined. */
        fun combinedKey(categories: Collection<String>): String =
            categories.map { it.trim() }.filter { it.isNotEmpty() }.distinct().sorted().joinToString("+")

        /**
         * The slot a puzzle saves into. Prefer this over the constructor at every
         * call site: Daily ⇒ date slot; combined ⇒ [combinedKey]; else [label].
         */
        fun forPuzzle(mode: GameMode, label: String, difficulty: Difficulty,
                      combined: Collection<String> = emptyList(), dailyKey: String? = null): SaveSlot = when {
            dailyKey != null      -> daily(dailyKey)
            combined.isNotEmpty() -> SaveSlot(mode, combinedKey(combined), difficulty)
            else                  -> SaveSlot(mode, label, difficulty)
        }

        fun parse(id: String): SaveSlot? {
            val (modePart, rest) = if ("::" in id) {
                val m = runCatching { GameMode.valueOf(id.substringBefore("::")) }.getOrNull() ?: return null
                m to id.substringAfter("::")
            } else GameMode.SINGLE to id
            val cat  = rest.substringBeforeLast("__", missingDelimiterValue = "")
            val diff = runCatching { Difficulty.valueOf(rest.substringAfterLast("__")) }.getOrNull() ?: return null
            if (cat.isEmpty()) return null
            return SaveSlot(modePart, cat, diff)
        }

        fun daily(dateKey: String) = SaveSlot(GameMode.DAILY, dateKey, Difficulty.EXPERT)
    }
}

data class PuzzleSession(
    val mode:           GameMode,
    val category:       String,                 // display label (combined label for combined puzzles)
    val difficulty:     Difficulty,
    val words:          List<PlacedWord>,
    val inputs:         Map<Cell, Char>     = emptyMap(),
    val revealed:       Set<Cell>           = emptySet(),   // cells filled by hints
    val combined:       List<String>        = emptyList(),  // source categories of a combined puzzle
    val dailyKey:       String?             = null,
    val elapsedSeconds: Long                = 0L,
    val hintsUsed:      Int                 = 0,
    val streak:         Int                 = 0,
    val player2:        String              = "",
    val team:           TeamState           = TeamState(),
    val vind:           VindState           = VindState(),
    val vindTimerSecs:  Int                 = 30,
    // Seconds left on the answer clock when saved mid-clue; null = a fresh clock.
    // Without it, quitting and resuming handed the answerer a full timer again.
    val vindSecondsLeft: Int?               = null,
    val bgArgb:         Int                 = 0xFFC0C0C0.toInt(),
    val bgImage:        String              = ""
) {
    val isDaily: Boolean get() = dailyKey != null

    /** Combined puzzles key their slot by the sorted category set, not the label. */
    val slot: SaveSlot
        get() = SaveSlot.forPuzzle(mode, category, difficulty, combined, dailyKey)
}

// ── CODEC ────────────────────────────────────────────────────────────────────
// Line-oriented, versioned text. Player names and clues can't contain the
// delimiters (sanitizeName / parseWordCsv enforce that), so no escaping needed.
//
//   v2
//   meta:key=value§key=value…
//   words:word§clue§x§y§h§n§cat|…
//   inputs:x|y|C;…
//   revealed:x|y;…

object SessionCodec {
    private const val VERSION = "v2"

    /** Vindictive answer-timer lengths the UI offers; anything else decodes as 30. */
    val VIND_TIMER_CHOICES = setOf(15, 30, 60)

    fun encodeWords(words: List<PlacedWord>): String = words.joinToString("|") {
        "${it.word}§${it.clue}§${it.startX}§${it.startY}§${it.isHorizontal}§${it.number}§${it.category}"
    }

    /** Parses words; malformed segments are skipped instead of discarding the whole list. */
    fun decodeWords(raw: String): List<PlacedWord> = decodeWordsCounting(raw).first

    /** Decoded words plus the number of segments that were malformed and skipped. */
    private fun decodeWordsCounting(raw: String): Pair<List<PlacedWord>, Int> {
        if (raw.isBlank()) return emptyList<PlacedWord>() to 0
        val segments = raw.split("|")
        val words = segments.mapNotNull { seg ->
            val p = seg.split("§")
            if (p.size < 6) return@mapNotNull null
            runCatching {
                val w = p[0]
                require(w.isNotEmpty() && w.all { it in 'A'..'Z' })
                PlacedWord(w, p[1], p[2].toInt(), p[3].toInt(), p[4].toBooleanStrict(), p[5].toInt(),
                    p.getOrElse(6) { "" })
            }.getOrNull()
        }
        return words to (segments.size - words.size)
    }

    fun encodeInputs(inputs: Map<Cell, Char>): String =
        inputs.entries.joinToString(";") { (pos, ch) -> "${pos.first}|${pos.second}|$ch" }

    fun decodeInputs(raw: String): Map<Cell, Char> {
        if (raw.isBlank()) return emptyMap()
        val out = LinkedHashMap<Cell, Char>()
        raw.split(";").forEach { seg ->
            val p = seg.split("|")
            if (p.size != 3 || p[2].length != 1 || p[2][0] !in 'A'..'Z') return@forEach
            val x = p[0].toIntOrNull() ?: return@forEach
            val y = p[1].toIntOrNull() ?: return@forEach
            out[Pair(x, y)] = p[2][0]
        }
        return out
    }

    private fun encodeCells(cells: Set<Cell>): String = cells.joinToString(";") { "${it.first}|${it.second}" }

    private fun decodeCells(raw: String): Set<Cell> =
        if (raw.isBlank()) emptySet() else raw.split(";").mapNotNull { seg ->
            val p = seg.split("|")
            val x = p.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val y = p.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            Pair(x, y)
        }.toSet()

    fun encode(s: PuzzleSession): String {
        val meta = linkedMapOf(
            "mode" to s.mode.name,
            "category" to s.category,
            "difficulty" to s.difficulty.name,
            "combined" to s.combined.joinToString(","),
            "daily" to (s.dailyKey ?: ""),
            "elapsed" to s.elapsedSeconds.toString(),
            "hints" to s.hintsUsed.toString(),
            "streak" to s.streak.toString(),
            "p2" to s.player2,
            "t1" to s.team.p1Correct.toString(), "t2" to s.team.p2Correct.toString(),
            "tturn" to s.team.turn.toString(),
            "th1" to s.team.p1Hints.toString(), "th2" to s.team.p2Hints.toString(),
            "vphase" to s.vind.phase.name, "vcur" to s.vind.currentPlayer.toString(),
            "v1" to s.vind.p1Score.toString(), "v2" to s.vind.p2Score.toString(),
            "vidx" to s.vind.assignedIndex.toString(), "vtimer" to s.vindTimerSecs.toString(),
            "vleft" to (s.vindSecondsLeft?.toString() ?: ""),
            "bg" to s.bgArgb.toString(), "bgimg" to s.bgImage
        )
        return buildString {
            append(VERSION).append('\n')
            append("meta:").append(meta.entries.joinToString("§") { "${it.key}=${it.value}" }).append('\n')
            append("words:").append(encodeWords(s.words)).append('\n')
            append("inputs:").append(encodeInputs(s.inputs)).append('\n')
            append("revealed:").append(encodeCells(s.revealed))
        }
    }

    /** Returns null if the text is not a v2 session or holds no usable words. */
    fun decode(raw: String): PuzzleSession? {
        val lines = raw.split('\n')
        if (lines.firstOrNull() != VERSION) return null
        fun section(name: String) = lines.firstOrNull { it.startsWith("$name:") }?.substringAfter(':') ?: ""
        val meta = section("meta").split("§").mapNotNull { kv ->
            val i = kv.indexOf('='); if (i <= 0) null else kv.substring(0, i) to kv.substring(i + 1)
        }.toMap()
        val (words, skipped) = decodeWordsCounting(section("words"))
        if (words.isEmpty()) return null
        fun int(k: String, d: Int) = meta[k]?.toIntOrNull() ?: d
        fun count(k: String) = int(k, 0).coerceAtLeast(0)   // counts can't be negative
        val mode  = runCatching { GameMode.valueOf(meta["mode"] ?: "") }.getOrDefault(GameMode.SINGLE)
        val diff  = runCatching { Difficulty.valueOf(meta["difficulty"] ?: "") }.getOrDefault(Difficulty.MEDIUM)
        val phase = runCatching { VindicativePhase.valueOf(meta["vphase"] ?: "") }.getOrDefault(VindicativePhase.PICK_OWN)
        // A skipped word segment shifts every later index, so a stored assigned-clue
        // index could point at the wrong word: drop it rather than guess.
        val vidx  = int("vidx", -1).takeIf { it in words.indices && skipped == 0 } ?: -1
        return PuzzleSession(
            mode           = mode,
            category       = meta["category"].orEmpty(),
            difficulty     = diff,
            words          = words,
            inputs         = decodeInputs(section("inputs")),
            revealed       = decodeCells(section("revealed")),
            combined       = meta["combined"].orEmpty().split(",").filter { it.isNotBlank() },
            dailyKey       = meta["daily"]?.takeIf { it.isNotBlank() },
            elapsedSeconds = (meta["elapsed"]?.toLongOrNull() ?: 0L).coerceAtLeast(0L),
            hintsUsed      = count("hints"),
            streak         = count("streak"),
            player2        = meta["p2"].orEmpty(),
            team           = TeamState(count("t1"), count("t2"), int("tturn", 0).coerceIn(0, 1),
                                       count("th1"), count("th2")),
            vind           = VindState(
                                 // An assigned-clue phase without a valid clue can't continue — fall back
                                 // to letting the current player assign again.
                                 phase         = if (phase == VindicativePhase.OPPONENT_WAIT && vidx < 0)
                                                     VindicativePhase.ASSIGN_CLUE else phase,
                                 currentPlayer = int("vcur", 0).coerceIn(0, 1),
                                 p1Score       = int("v1", 0),
                                 p2Score       = int("v2", 0),
                                 assignedIndex = vidx),
            vindTimerSecs  = int("vtimer", 30).takeIf { it in VIND_TIMER_CHOICES } ?: 30,
            vindSecondsLeft = meta["vleft"]?.toIntOrNull()?.coerceIn(0, int("vtimer", 30).takeIf { it in VIND_TIMER_CHOICES } ?: 30),
            bgArgb         = int("bg", 0xFFC0C0C0.toInt()),
            bgImage        = meta["bgimg"].orEmpty()
        )
    }
}
