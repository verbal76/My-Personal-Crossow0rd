package com.hag.mypersonalcrossword.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerKeysTest {

    /** Every key shape SaveManager writes for one profile. */
    private fun keysOf(p: String) = listOf(
        "score_$p", "completed_$p", "cellcolor_$p", "btncolor_$p", "recentcolors_$p",
        "saves_$p", "statkeys_$p", "dailydone_$p", "paid_$p",
        "used_${p}_FOOD",
        "puzzle_${p}_FOOD_EASY_words", "puzzle_${p}_FOOD_EASY_bgimg",
        "psession_${p}_FOOD__EASY", "psession_time_${p}_FOOD__EASY",
        "psession_${p}_TEAM::FOOD+CITIES__HARD", "psession_${p}_DAILY::2026-09-27__EXPERT",
        "psession_${p}_4 Categories__GENIUS",
        "stat_${p}_FOOD_EASY", "stat_${p}_VINDICTIVE::FOOD__EASY",
        "elapsed_${p}_FOOD_EASY"
    )

    private val shared = listOf("all_players", "last_user", "sound_enabled")

    private fun assertIsolated(names: List<String>) {
        val all = names.flatMap(::keysOf) + shared
        for (n in names) {
            assertEquals("keys of '$n' among $names", keysOf(n).sorted(), PlayerKeys.keysOwnedBy(all, n, names).sorted())
        }
        for (k in shared) assertNull(k, PlayerKeys.ownerOf(k, names))
    }

    /** Regression: deleting "Kev" swept every key of "Kev_2" (prefix match). */
    @Test fun underscoreSuffixedNameIsNotAPrefixVictim() {
        assertIsolated(listOf("Kev", "Kev_2"))
        assertFalse(PlayerKeys.ownedBy("psession_Kev_2_FOOD__EASY", "Kev", listOf("Kev", "Kev_2")))
        assertTrue(PlayerKeys.ownedBy("psession_Kev_2_FOOD__EASY", "Kev_2", listOf("Kev", "Kev_2")))
    }

    @Test fun nameThatLooksLikeACategoryStaysSeparate() {
        // "Kev_FOOD" is a legal profile; Kev's FOOD keys must still be Kev's.
        assertIsolated(listOf("Kev", "Kev_FOOD", "Kev_FOOD_EASY"))
    }

    @Test fun namesContainingDoubleUnderscore() {
        assertIsolated(listOf("a", "a__b", "a_", "a__"))
    }

    @Test fun namesThatEqualCommonWordsOrPrefixParts() {
        // "time" collides with the psession_time_ prefix; "FOOD" with a category;
        // "EASY" with a difficulty; "score" with a prefix word.
        assertIsolated(listOf("time", "FOOD", "EASY", "score", "Kev"))
        assertEquals("time", PlayerKeys.ownerOf("psession_time_FOOD__EASY", listOf("time", "FOOD")))
        assertEquals("FOOD", PlayerKeys.ownerOf("psession_time_FOOD_FOOD__EASY", listOf("time", "FOOD")))
    }

    @Test fun deletedNameIsConsideredEvenIfNotInTheList() {
        assertTrue(PlayerKeys.ownedBy("score_Gone", "Gone", emptyList()))
        assertTrue(PlayerKeys.ownedBy("used_Gone_FOOD", "Gone", listOf("Kev")))
        assertFalse(PlayerKeys.ownedBy("score_Gone", "", listOf("Gone")))
    }

    @Test fun legacyAndUnknownShapesStillSweepToTheLongestName() {
        // A corrupt / future key still belongs to someone so deletion cleans it up.
        assertEquals("Kev_2", PlayerKeys.ownerOf("used_Kev_2_X_Y", listOf("Kev", "Kev_2")))
        assertEquals("Kev_2", PlayerKeys.ownerOf("psession_Kev_2_no-slot", listOf("Kev", "Kev_2")))
        assertEquals("Kev", PlayerKeys.ownerOf("puzzle_Kev_Daily_MEDIUM_time", listOf("Kev", "Kev_2")))
        assertNull(PlayerKeys.ownerOf("used_Nobody_FOOD", listOf("Kev")))
        assertNull(PlayerKeys.ownerOf("score_Kev_2", listOf("Kev")))   // exact keys never prefix-match
    }

    @Test fun emojiAndSpacesInNames() {
        assertIsolated(listOf("Ann 😀", "Ann", "Ann 😀_x"))
    }
}
