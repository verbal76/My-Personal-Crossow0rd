package com.hag.mypersonalcrossword.core

// ============================================================
// PLAYER KEYS — which profile owns a SharedPreferences key.
// ============================================================
// SaveManager embeds the profile name in its keys, and names may contain '_',
// so a plain startsWith("psession_${name}_") also matches every key of a profile
// whose name merely STARTS with that name plus '_': deleting "Kev" used to wipe
// "Kev_2"'s saves, stats and used-word history. This decides ownership against
// the full list of profile names instead.
//
// Key shapes written by SaveManager (MainActivity.kt):
//   exact     "<prefix><name>"          score_, completed_, cellcolor_, btncolor_,
//                                       recentcolors_, saves_, statkeys_, dailydone_
//   scoped    "<prefix><name>_<rest>"   used_, puzzle_ (legacy), psession_time_,
//                                       psession_, stat_, elapsed_ (legacy)
// `all_players` and `last_user` aren't per-player keys and never match.
//
// Rule: a scoped key belongs to the LONGEST known name N such that the key is
// "<prefix>N_<rest>" and <rest> has the shape SaveManager writes for that prefix
// (a category for used_, a slot id for psession_, `CAT_DIFF` for stat_ …).
// Categories never contain '_', so:
//   • "used_Kev_2_FOOD" is Kev_2's (longest name), not Kev's;
//   • "stat_Kev_FOOD_EASY" stays Kev's even if a profile "Kev_FOOD" exists,
//     because "EASY" alone isn't a valid stat remainder.
// Where one prefix is itself a prefix of another (psession_ / psession_time_),
// the longer prefix is tried first, then the shorter, so a profile literally
// named "time" still owns "psession_time_FOOD__EASY". If no candidate's remainder
// has a known shape under any prefix (corrupt or future key), the longest name
// under the longest prefix wins, so deletion still sweeps junk.

object PlayerKeys {
    /** Keys that are exactly "<prefix><name>". */
    val EXACT_PREFIXES: List<String> = listOf(
        "score_", "completed_", "cellcolor_", "btncolor_", "recentcolors_",
        "saves_", "statkeys_", "dailydone_"
    )

    /** Keys shaped "<prefix><name>_<rest>". */
    val SCOPED_PREFIXES: List<String> = listOf(
        "used_", "puzzle_", "psession_time_", "psession_", "stat_", "elapsed_"
    )

    /**
     * The profile in [allNames] that owns [key], or null if it is not a per-player
     * key of any of them.
     */
    fun ownerOf(
        key:            String,
        allNames:       Collection<String>,
        exactPrefixes:  List<String> = EXACT_PREFIXES,
        scopedPrefixes: List<String> = SCOPED_PREFIXES
    ): String? {
        val names = allNames.filter { it.isNotEmpty() }
        for (prefix in exactPrefixes) {
            if (key.startsWith(prefix) && key.substring(prefix.length) in names) return key.substring(prefix.length)
        }
        var fallback: String? = null
        for (prefix in scopedPrefixes.sortedByDescending { it.length }) {
            if (!key.startsWith(prefix)) continue
            val rest       = key.substring(prefix.length)
            val candidates = names.filter { rest.startsWith(it + "_") }.sortedByDescending { it.length }
            val shape      = REMAINDER_SHAPES[prefix]
            candidates.firstOrNull { shape == null || shape(rest.substring(it.length + 1)) }?.let { return it }
            if (fallback == null) fallback = candidates.firstOrNull()
        }
        return fallback
    }

    // ── Remainder shapes, as SaveManager writes them ───────────────────────────
    private val DIFFS        = Difficulty.entries.joinToString("|") { it.name }
    private val CATEGORY     = Regex("[^_]+")                                   // A–Z, or a legacy combined label
    private val STAT_SOLO    = Regex("[^_]+_($DIFFS)")                          // stat_<p>_<cat>_<DIFF>
    private val ELAPSED      = STAT_SOLO                                        // elapsed_<p>_<cat>_<DIFF>
    private val LEGACY_FIELD = Regex("[^_]+_($DIFFS)_(words|inputs|bg|bgimg|time)")
    private fun isSlotId(s: String) = SaveSlot.parse(s) != null && !s.substringBeforeLast("__").contains('_')

    private val REMAINDER_SHAPES: Map<String, (String) -> Boolean> = mapOf(
        "used_"          to { r -> CATEGORY.matches(r) },
        "puzzle_"        to { r -> LEGACY_FIELD.matches(r) },
        "psession_time_" to { r -> isSlotId(r) },
        "psession_"      to { r -> isSlotId(r) },
        "stat_"          to { r -> STAT_SOLO.matches(r) || ("::" in r && isSlotId(r)) },
        "elapsed_"       to { r -> ELAPSED.matches(r) }
    )

    /**
     * True when [key] belongs to profile [name]. [allNames] is every profile on the
     * device (whether or not it includes [name]; it is added). Pass the list
     * BEFORE removing the profile being deleted.
     */
    fun ownedBy(
        key:            String,
        name:           String,
        allNames:       Collection<String>,
        exactPrefixes:  List<String> = EXACT_PREFIXES,
        scopedPrefixes: List<String> = SCOPED_PREFIXES
    ): Boolean = name.isNotEmpty() && ownerOf(key, allNames + name, exactPrefixes, scopedPrefixes) == name

    /** Every key in [keys] owned by [name] — what deletePlayer should remove. */
    fun keysOwnedBy(keys: Collection<String>, name: String, allNames: Collection<String>): List<String> {
        val names = allNames + name
        return keys.filter { ownedBy(it, name, names) }
    }
}
