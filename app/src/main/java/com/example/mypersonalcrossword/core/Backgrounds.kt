package com.hag.mypersonalcrossword.core

// ============================================================
// BACKGROUND NAMES — player-facing titles for assets/images/*
// ============================================================
// The shipped art files are named after the prompts that produced them
// ("verbal28_An_acrylic_painting_in_the_style_of_John_Singer_Sarg_<uuid>_2.webp").
// Players see a short gallery title instead. Files sharing a title are numbered
// (I, II, III…) in filename order so names are stable between launches.

private val CURATED_TITLES: List<Pair<String, String>> = listOf(
    // More specific prefixes first — the first match wins.
    "asemic_writing_style_green"                 to "Green Script",
    "asemic_writing_style_light"                 to "Pale Script",
    "asemic_writing_style_pink"                  to "Pink Script",
    "asemic_writing_style_yellow"                to "Yellow Script",
    "asemic_writing_style"                       to "Ink Script",
    "jim_mahfood_japanese_woodblock"             to "Woodblock Splatter",
    "jim_mahfood"                                to "Street Splatter",
    "japanese_woodblock_printmaker_style_ancient" to "Cosmic Woodblock",
    "japanese_woodblock_printmaker_style_like"   to "The Great Wave",
    "japanese_woodblock_printmaker_style"        to "Quiet Woodblock",
    "victorian_gothic_fluer-di-lis_blue"         to "Blue Fleur-de-Lis",
    "victorian_gothic_fluer-di-lis"              to "Gothic Fleur-de-Lis",
    "sine_wave_snow_covered"                     to "Snowy Signal Peaks",
    "sine_wave_mountains"                        to "Signal Peaks",
    "an_acrylic_painting_in_the_style_of_john_singer" to "Sargent Study",
    "beautiful_bohemian_frosted_stain_glass"     to "Stained-Glass Butterflies",
    "beautiful_floral_design"                    to "Summer Florals",
    "beautiful_sunflower_design"                 to "Sunflowers",
    "cat_samurai"                                to "Samurai Cat",
    "clip_art_beautiful_singular_butterflies"    to "Butterfly Garden",
    "create_a_mesmerizing_psychedelic"           to "Psychedelia",
    "i_envision_a_book_cover"                    to "Adventurer's Tome",
    "i_want_a_tropical_hawaiian_shirt"           to "Tiki Skulls",
    "single_symmetrical_portuguese_ceramic_tile" to "Azulejo Tile",
    "southwest_desert_watercolor"                to "Desert Watercolor",
    "the_fool_tarot_card"                        to "The Fool",
    "a_surface_pattern_design"                   to "Bold Pattern",
    "abstract_repeating_pattern_patriotic"       to "Crimson & Navy",
    "an_intricate_water_splotch"                 to "Watercolor Sketch",
    "art_deco_geometric_pattern"                 to "Art Deco Lines",
    "bohemian_geometric_design"                  to "Boho Geometry",
    "completely_blacknight_sky"                  to "Starfield",
    "gemstones_poured_out"                       to "Gemstones",
    "looking_through_a_rain"                     to "Rainy Window",
    "rustic_moody_vintage_style_country"         to "Rustic Country",
    "short_messy_hair_lying_on_the_ground"       to "Stargazer",
    "summer_floral_line_art"                     to "Floral Line Art",
    "symmetrical_art_deco_design"                to "Gilded Deco",
    "tilt-shift_textured_seamless_geometrical"   to "Tilt-Shift Geometry",
    "womens_t-shirt_clipart_design"              to "Retro Clip Art"
)

private val UUID_TAIL   = Regex("_[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}.*$", RegexOption.IGNORE_CASE)
private val PARAM_TAIL  = Regex("_--.*$")
private val ROMAN       = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X")

/** Strips the author prefix, UUID/variant suffix, extension and prompt parameters. */
internal fun backgroundStem(filename: String): String =
    filename.substringBeforeLast('.')
        .removePrefix("verbal28_")
        .replace(UUID_TAIL, "")
        .replace(PARAM_TAIL, "")
        .trim('_')

/** Title for one file, without duplicate numbering. */
fun backgroundBaseTitle(filename: String): String {
    val stem = backgroundStem(filename).lowercase()
    CURATED_TITLES.firstOrNull { stem.startsWith(it.first) }?.let { return it.second }
    // Unknown art: first few words of the stem, title-cased.
    val words = stem.split('_', '-', ' ').filter { it.isNotBlank() && it.all(Char::isLetter) }.take(3)
    return if (words.isEmpty()) "Background"
    else words.joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
}

/**
 * Display names for a set of background files: unique, human-readable,
 * numbered with Roman numerals where several files share a title.
 */
fun backgroundDisplayNames(filenames: List<String>): Map<String, String> {
    val sorted  = filenames.sorted()
    val byTitle = sorted.groupBy { backgroundBaseTitle(it) }
    val out = LinkedHashMap<String, String>()
    for (name in sorted) {
        val title = backgroundBaseTitle(name)
        val group = byTitle.getValue(title)
        out[name] = if (group.size == 1) title
                    else "$title ${ROMAN.getOrElse(group.indexOf(name)) { (group.indexOf(name) + 1).toString() }}"
    }
    return out
}
