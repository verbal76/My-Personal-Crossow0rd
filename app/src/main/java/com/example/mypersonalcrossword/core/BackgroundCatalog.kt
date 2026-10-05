package com.hag.mypersonalcrossword.core

// ============================================================
// BACKGROUND CATALOGUE — the 25 puzzle backgrounds, 5 per category
// ============================================================
// Original art made by tools/make_backgrounds.py (noise, gradients, drawn shapes: no
// photos, text or logos). Files live in assets/backgrounds/. `scrim` is how much black
// the app lays over an image behind the board (0 = none) so the tiles and letters always
// stay the most readable thing on screen; brighter pictures get more.
// The player's choice is stored as the file name, like the older art in assets/images.

enum class BackgroundCategory(val id: String, val label: String) {
    SPACE("space", "SPACE"),
    SKY("sky", "SKY"),
    WOODS("woods", "WOODS"),
    MOTION_CITY("motioncity", "MOTION CITY"),
    OCEAN("ocean", "OCEAN")
}

data class CatalogBackground(
    val file: String,
    val title: String,
    val category: BackgroundCategory,
    val scrim: Float
)

object BackgroundCatalog {
    const val DIR = "backgrounds"

    private fun bg(c: BackgroundCategory, file: String, title: String, scrim: Float) =
        CatalogBackground("$file.webp", title, c, scrim)

    val all: List<CatalogBackground> = listOf(
        bg(BackgroundCategory.SPACE, "space_1_nebula_bloom",      "Nebula Bloom",      0.22f),
        bg(BackgroundCategory.SPACE, "space_2_deep_starfield",    "Deep Star Field",   0.00f),
        bg(BackgroundCategory.SPACE, "space_3_planet_horizon",    "Planet Horizon",    0.05f),
        bg(BackgroundCategory.SPACE, "space_4_distant_galaxy",    "Distant Galaxy",    0.05f),
        bg(BackgroundCategory.SPACE, "space_5_cosmic_clouds",     "Cosmic Clouds",     0.22f),

        bg(BackgroundCategory.SKY, "sky_1_bright_blue_clouds",    "Bright Blue Clouds", 0.34f),
        bg(BackgroundCategory.SKY, "sky_2_sunrise",               "Sunrise",           0.28f),
        bg(BackgroundCategory.SKY, "sky_3_storm_clouds",          "Storm Clouds",      0.12f),
        bg(BackgroundCategory.SKY, "sky_4_golden_sunset",         "Golden Sunset",     0.28f),
        bg(BackgroundCategory.SKY, "sky_5_twilight_cirrus",       "Twilight Clouds",   0.18f),

        bg(BackgroundCategory.WOODS, "woods_1_sunlit_forest",     "Sunlit Forest",     0.30f),
        bg(BackgroundCategory.WOODS, "woods_2_misty_woodland",    "Misty Woodland",    0.34f),
        bg(BackgroundCategory.WOODS, "woods_3_autumn_forest",     "Autumn Forest",     0.30f),
        bg(BackgroundCategory.WOODS, "woods_4_dark_evergreen",    "Dark Evergreens",   0.05f),
        bg(BackgroundCategory.WOODS, "woods_5_light_shafts",      "Shafts of Light",   0.18f),

        bg(BackgroundCategory.MOTION_CITY, "motioncity_1_traffic_trails",   "Traffic Trails",   0.08f),
        bg(BackgroundCategory.MOTION_CITY, "motioncity_2_blurred_downtown", "Blurred Downtown", 0.12f),
        bg(BackgroundCategory.MOTION_CITY, "motioncity_3_city_through_glass", "City Through Glass", 0.10f),
        bg(BackgroundCategory.MOTION_CITY, "motioncity_4_neon_streaks",     "Neon Streaks",     0.12f),
        bg(BackgroundCategory.MOTION_CITY, "motioncity_5_urban_motion",     "Urban Motion",     0.15f),

        bg(BackgroundCategory.OCEAN, "ocean_1_tropical_water",    "Tropical Water",    0.30f),
        bg(BackgroundCategory.OCEAN, "ocean_2_underwater_blue",   "Underwater Blue",   0.08f),
        bg(BackgroundCategory.OCEAN, "ocean_3_wave_level",        "Wave Level",        0.28f),
        bg(BackgroundCategory.OCEAN, "ocean_4_reef_depth",        "Reef Depth",        0.12f),
        bg(BackgroundCategory.OCEAN, "ocean_5_sunset_water",      "Sunset Over Water", 0.22f)
    )

    fun inCategory(c: BackgroundCategory): List<CatalogBackground> = all.filter { it.category == c }
    fun find(file: String): CatalogBackground? = all.firstOrNull { it.file == file }
    fun isCatalog(file: String): Boolean = find(file) != null
    /** Dimming laid over [file] behind the board; 0 for older art and "no image". */
    fun scrimFor(file: String): Float = find(file)?.scrim ?: 0f
}
