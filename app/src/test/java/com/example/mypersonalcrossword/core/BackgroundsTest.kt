package com.hag.mypersonalcrossword.core

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundsTest {

    private fun shippedImages(): List<String> {
        val dir = listOfNotNull(System.getProperty("crossword.assets"), "src/main/assets", "app/src/main/assets")
            .map { File(it, "images/drawable-xxhdpi") }.first { it.isDirectory }
        return dir.list()!!.filter { it.endsWith(".webp") || it.endsWith(".png") || it.endsWith(".jpg") }
    }

    @Test fun everyShippedImageGetsAUniqueCleanName() {
        val files = shippedImages()
        assertTrue(files.size > 10)
        val names = backgroundDisplayNames(files)
        assertEquals(files.size, names.values.toSet().size)
        names.values.forEach { n ->
            assertFalse("raw prompt leaked: $n", n.contains("verbal", ignoreCase = true) || n.contains('_'))
            assertTrue("too long: $n", n.length <= 28)
            assertFalse("uncurated fallback used for $n", n == "Background")
        }
    }

    @Test fun everyShippedImageHasACuratedTitle() {
        // Guards against new art silently falling back to "first three words of the prompt".
        shippedImages().forEach { f ->
            val stem = backgroundStem(f).lowercase()
            val title = backgroundBaseTitle(f)
            val fallback = stem.split('_', '-', ' ').filter { it.isNotBlank() && it.all(Char::isLetter) }.take(3)
                .joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
            assertTrue("no curated title for $f", title != fallback)
        }
    }

    @Test fun duplicatesAreNumberedStably() {
        val files = listOf(
            "verbal28_An_acrylic_painting_in_the_style_of_John_Singer_Sarg_1731bfc3-b890-4610-855c-06a49c4a9b57_2.webp",
            "verbal28_An_acrylic_painting_in_the_style_of_John_Singer_Sarg_5c5310b8-eddf-4a69-b564-41a6444b416c_3.webp",
            "verbal28_Cat_samurai_on_a_Japanese_game_card_inspired_backgro_0f4a1b3e-0000-4000-8000-000000000000.webp"
        )
        val names = backgroundDisplayNames(files.reversed())
        assertEquals("Sargent Study I", names[files[0]])
        assertEquals("Sargent Study II", names[files[1]])
        assertEquals("Samurai Cat", names[files[2]])
    }

    @Test fun unknownArtFallsBackToReadableWords() {
        assertEquals("Moonlit Harbor Scene", backgroundBaseTitle("verbal28_moonlit_harbor_scene_at_night_12345678-1234-1234-1234-123456789abc.webp"))
    }
}
