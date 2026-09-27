package com.hag.mypersonalcrossword.core

import kotlin.random.Random

// ============================================================
// GENERATOR — BFS EXPANSION + CONSTRAINT INDEX
// Architecture from: VGV "How we developed a scalable, incredibly fast crossword generator"
//
//   1. Constraint index (len → pos → char → words): O(1) word lookup vs O(pool × wordLen)
//   2. Grid maps (char + direction): O(wordLen) validation vs O(placed × wordLen)
//   3. BFS expansion from seed: systematic, no wasted random restarts
//   4. Candidate queue with visited set, plus one re-sweep pass for cells that
//      only became usable after later words were placed.
//
// All randomness flows through the `rng` parameter, so a seeded Random gives a
// fully reproducible board (the Daily Puzzle depends on this). Iteration over
// the internal maps is insertion-ordered, never hash-ordered.
// ============================================================

typealias WordIndex = HashMap<Int, HashMap<Int, HashMap<Char, MutableList<RawEntry>>>>

/** Number of full board attempts; the best-scoring board wins. */
const val GENERATOR_ATTEMPTS = 50

// ── CONSTRAINT INDEX ─────────────────────────────────────────────────────────
// Maps length → positionInWord → character → matching entries.
// Example: wordIndex[5][2]['R'] = all 5-letter words with 'R' at position 2.
fun buildWordIndex(items: List<RawEntry>): WordIndex {
    val index = WordIndex()
    for (entry in items) {
        val len = entry.answer.length
        for (pos in entry.answer.indices) {
            index
                .getOrPut(len) { HashMap() }
                .getOrPut(pos) { HashMap() }
                .getOrPut(entry.answer[pos]) { mutableListOf() }
                .add(entry)
        }
    }
    return index
}

// ── GRID MAPS ────────────────────────────────────────────────────────────────
// gridChar: cell → character already placed there
// gridDir:  cell → direction of the word that placed it
//           true  = horizontal word only
//           false = vertical word only
//           null  = intersection (one H word + one V word share this cell)
fun addToGrid(
    word:     PlacedWord,
    gridChar: HashMap<Cell, Char>,
    gridDir:  HashMap<Cell, Boolean?>
) {
    val h = word.isHorizontal
    for (i in word.word.indices) {
        val pos = word.cellAt(i)
        gridChar[pos] = word.word[i]
        gridDir[pos]  = when {
            !gridDir.containsKey(pos) -> h      // first word to claim this cell
            gridDir[pos] != h         -> null    // opposite direction = intersection
            else                      -> h       // same dir (shouldn't occur in valid board)
        }
    }
}

// ── FAST VALIDATION ───────────────────────────────────────────────────────────
// Three rules enforced:
//   1. End-caps before/after the word must be empty.
//   2. Occupied cells in the path must be valid perpendicular intersections
//      (wrong char = conflict; same-direction or already-intersection = co-linear).
//   3. Empty cells must have no perpendicular neighbors (prevents parallel word adjacency).
fun isValidFast(
    word:     String,  sx: Int, sy: Int, h: Boolean,
    gridChar: HashMap<Cell, Char>,
    gridDir:  HashMap<Cell, Boolean?>
): Boolean {
    val dx = if (h) 1 else 0
    val dy = if (h) 0 else 1

    if (gridChar.containsKey(Pair(sx - dx, sy - dy))) return false
    if (gridChar.containsKey(Pair(sx + word.length * dx, sy + word.length * dy))) return false

    var intersections = 0
    for (i in word.indices) {
        val pos          = Pair(sx + i * dx, sy + i * dy)
        val existingChar = gridChar[pos]

        if (existingChar != null) {
            if (existingChar != word[i]) return false
            val dir = gridDir[pos]
            if (dir == null || dir == h) return false
            intersections++
        } else {
            if (gridChar.containsKey(Pair(sx + i * dx - dy, sy + i * dy - dx))) return false
            if (gridChar.containsKey(Pair(sx + i * dx + dy, sy + i * dy + dx))) return false
        }
    }
    return intersections > 0   // word must connect to the existing board
}

// ── CANDIDATE QUEUE ───────────────────────────────────────────────────────────
fun enqueueFromWord(
    word:    PlacedWord,
    queue:   ArrayDeque<Triple<Int,Int,Boolean>>,
    visited: HashSet<Triple<Int,Int,Boolean>>
) {
    val hNew = !word.isHorizontal
    for (i in word.word.indices) {
        val (cx, cy) = word.cellAt(i)
        val cand = Triple(cx, cy, hNew)
        if (visited.add(cand)) queue.add(cand)
    }
}

// ── WORD FINDER ───────────────────────────────────────────────────────────────
// For a BFS candidate cell (cx, cy) and direction hNew:
//   1. Try each possible offset of (cx,cy) within the new word.
//   2. Walk forward from the word start, collecting crossing constraints and every
//      length at which a word could legally end.
//   3. Query wordIndex[len][posInWord][crossChar] for matching entries.
//   4. Filter by the constraints that fall INSIDE the candidate's length
//      (a crossing further along the line is irrelevant to a shorter word),
//      validate with isValidFast, then score.
fun findBestPlacement(
    cx:        Int, cy:    Int, hNew:     Boolean, crossChar: Char,
    inPool:    Set<String>,
    wordIndex: WordIndex,
    gridChar:  HashMap<Cell, Char>,
    gridDir:   HashMap<Cell, Boolean?>,
    usedWords: Set<String>,
    category:  (RawEntry) -> String = { it.category }
): PlacedWord? {
    val dx = if (hNew) 1 else 0
    val dy = if (hNew) 0 else 1

    var bestWord:  PlacedWord? = null
    var bestScore              = Int.MIN_VALUE

    for (posInWord in 0..MAX_ANSWER_LENGTH - 1) {
        val sx = cx - posInWord * dx
        val sy = cy - posInWord * dy

        // Before-cap must be empty — can't start here if previous cell is occupied
        if (gridChar.containsKey(Pair(sx - dx, sy - dy))) continue

        val knownChars = sortedMapOf(posInWord to crossChar)
        val endable    = BooleanArray(MAX_ANSWER_LENGTH + 1)   // endable[len] — a word of this length may end here
        var maxLen = 0

        for (ext in 0 until MAX_ANSWER_LENGTH) {
            val wx    = sx + ext * dx
            val wy    = sy + ext * dy
            val wChar = gridChar[Pair(wx, wy)]
            val wDir  = gridDir[Pair(wx, wy)]

            if (wChar != null) {
                if (wDir == hNew) break         // co-linear cell — can't extend
                knownChars[ext] = wChar          // perpendicular intersection — collect constraint
            } else {
                // Empty cell — stop if a parallel word runs alongside
                if (gridChar.containsKey(Pair(wx - dy, wy - dx))) break
                if (gridChar.containsKey(Pair(wx + dy, wy + dx))) break
            }
            if (!gridChar.containsKey(Pair(wx + dx, wy + dy))) {
                endable[ext + 1] = true
                maxLen = ext + 1
            }
        }

        if (maxLen < MIN_ANSWER_LENGTH || posInWord >= maxLen) continue

        for (len in maxLen downTo maxOf(posInWord + 1, MIN_ANSWER_LENGTH)) {
            if (!endable[len]) continue
            val candidates = wordIndex[len]?.get(posInWord)?.get(crossChar) ?: continue
            // Only constraints inside this length matter for this candidate.
            val inside = knownChars.headMap(len)
            for (entry in candidates) {
                if (!inPool.contains(entry.answer)) continue
                if (!inside.all { (p, c) -> entry.answer[p] == c }) continue
                if (!isValidFast(entry.answer, sx, sy, hNew, gridChar, gridDir)) continue

                val isInner     = posInWord > 0 && posInWord < len - 1
                val bridgeBonus = when {
                    inside.size >= 3 -> 900_000
                    inside.size == 2 -> 400_000
                    else             -> 0
                }
                val score = (if (isInner) 30_000 else -15_000) +
                        bridgeBonus + len * 500 -
                        (if (usedWords.contains(entry.answer)) 30_000 else 0)

                if (score > bestScore) {
                    bestScore = score
                    bestWord  = PlacedWord(entry.answer, entry.clue, sx, sy, hNew, category = category(entry))
                }
            }
        }
    }

    return if (bestScore > -20_000) bestWord else null
}

// ── MAIN GENERATOR ────────────────────────────────────────────────────────────
/**
 * Builds a crossword of up to [target] words from [items].
 * Words in [usedWords] are deprioritised (tried last, penalised in scoring) but
 * still usable so a well-explored category never produces an empty board.
 * Returns a normalised (top-left at 0,0), numbered word list; empty if [items]
 * can't produce a board of at least two words.
 */
fun generateCrossword(
    items:     List<RawEntry>,
    target:    Int,
    usedWords: Set<String>,
    rng:       Random = Random.Default,
    attempts:  Int    = GENERATOR_ATTEMPTS
): List<PlacedWord> {
    // Shuffle before de-duplicating so an answer that appears with several clues
    // (e.g. ROGER in CARTOONS) doesn't always surface the same clue.
    val uniqueItems   = items.shuffled(rng).distinctBy { it.answer }
    val unusedItems   = uniqueItems.filter { !usedWords.contains(it.answer) }
    val usedItems     = uniqueItems.filter {  usedWords.contains(it.answer) }

    val wordIndex     = buildWordIndex(uniqueItems)
    val entryByAnswer = uniqueItems.associateBy { it.answer }

    var bestResult = emptyList<PlacedWord>()
    var maxScore   = Long.MIN_VALUE

    for (iteration in 0 until attempts) {
        val inPool = LinkedHashSet<String>().apply {
            unusedItems.shuffled(rng).forEach { add(it.answer) }
            usedItems.shuffled(rng).forEach   { add(it.answer) }
        }

        val gridChar = HashMap<Cell, Char>(512)
        val gridDir  = HashMap<Cell, Boolean?>(512)
        val placed   = mutableListOf<PlacedWord>()

        // Seed: place one long word to anchor the BFS expansion
        val anchorH  = (iteration % 2 == 0)
        val topWords = inPool.take(20).mapNotNull { entryByAnswer[it] }
            .sortedByDescending { it.answer.length }.take(5)
        if (topWords.isEmpty()) break
        val anchor   = topWords[rng.nextInt(topWords.size)]
        val seed     = PlacedWord(anchor.answer, anchor.clue, 0, 0, anchorH, category = anchor.category)
        placed.add(seed)
        inPool.remove(anchor.answer)
        addToGrid(seed, gridChar, gridDir)

        // BFS: expand from every letter of every placed word. A second sweep
        // re-tries every cell once, because many cells only become usable after
        // later words were placed around them.
        for (pass in 0 until 2) {
            if (placed.size >= target) break
            val visited = HashSet<Triple<Int,Int,Boolean>>(512)
            val queue   = ArrayDeque<Triple<Int,Int,Boolean>>()
            placed.forEach { enqueueFromWord(it, queue, visited) }

            while (queue.isNotEmpty() && placed.size < target) {
                val (cx, cy, hNew) = queue.removeFirst()
                if (gridDir[Pair(cx, cy)] == null) continue   // already an intersection
                val crossChar = gridChar[Pair(cx, cy)] ?: continue

                val newWord = findBestPlacement(
                    cx, cy, hNew, crossChar,
                    inPool, wordIndex, gridChar, gridDir, usedWords
                ) ?: continue

                placed.add(newWord)
                inPool.remove(newWord.word)
                addToGrid(newWord, gridChar, gridDir)
                enqueueFromWord(newWord, queue, visited)
            }
        }

        if (placed.size > 1) {   // BFS + isValidFast guarantees connectivity
            val finalArea          = calculateArea(placed)
            val finalIntersections = gridDir.values.count { it == null }
            val targetPenalty      = if (placed.size < target) (target - placed.size) * 1_000_000L else 0L

            val boardScore = (placed.size * 500_000L) +
                    (finalIntersections * 150_000L) -
                    (finalArea / 3L) -
                    targetPenalty

            if (boardScore > maxScore) {
                maxScore   = boardScore
                bestResult = placed.toList()
            }

            // Good enough — stop spending attempts.
            if (bestResult.size >= target && finalIntersections >= target - 2) break
        }
    }

    if (bestResult.isEmpty()) return emptyList()
    return numberBoard(bestResult)
}

/** Translate to (0,0) and assign clue numbers in reading order. */
fun numberBoard(words: List<PlacedWord>): List<PlacedWord> {
    val minX       = words.minOf { it.startX }
    val minY       = words.minOf { it.startY }
    val normalized = words.map { it.copy(startX = it.startX - minX, startY = it.startY - minY) }
    val sorted     = normalized.sortedWith(compareBy({ it.startY }, { it.startX }, { !it.isHorizontal }))
    val numMap     = mutableMapOf<Cell, Int>(); var n = 1
    return sorted.map { w -> w.copy(number = numMap.getOrPut(Pair(w.startX, w.startY)) { n++ }) }
}
