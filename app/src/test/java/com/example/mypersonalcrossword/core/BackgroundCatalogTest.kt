package com.hag.mypersonalcrossword.core

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundCatalogTest {
    private fun assetsDir(): File = listOfNotNull(System.getProperty("crossword.assets"), "src/main/assets", "app/src/main/assets")
        .map(::File).first { File(it, "test.csv").exists() }

    @Test fun fiveCategoriesOfFive() {
        assertEquals(25, BackgroundCatalog.all.size)
        for (c in BackgroundCategory.entries) assertEquals("${c.label} should have 5", 5, BackgroundCatalog.inCategory(c).size)
        assertEquals(5, BackgroundCategory.entries.size)
    }

    @Test fun filesAreUniqueAndNamedAfterTheirCategory() {
        assertEquals(25, BackgroundCatalog.all.map { it.file }.toSet().size)
        assertEquals(25, BackgroundCatalog.all.map { it.title }.toSet().size)
        for (b in BackgroundCatalog.all) assertTrue(b.file, b.file.startsWith(b.category.id + "_") && b.file.endsWith(".webp"))
    }

    @Test fun everyBackgroundShipsAndIsNotTiny() {
        val dir = File(assetsDir(), BackgroundCatalog.DIR)
        for (b in BackgroundCatalog.all) {
            val f = File(dir, b.file)
            assertTrue("missing ${b.file}", f.exists())
            assertTrue("${b.file} is only ${f.length()} bytes", f.length() > 5_000)
            assertTrue("${b.file} is ${f.length()} bytes: too heavy for a background", f.length() < 400_000)
        }
        assertEquals("assets/backgrounds holds extra or missing files", 25, dir.listFiles { x -> x.extension == "webp" }!!.size)
    }

    @Test fun scrimsStayInASaneRange() {
        for (b in BackgroundCatalog.all) assertTrue("${b.file} scrim ${b.scrim}", b.scrim in 0f..0.5f)
        assertEquals(0f, BackgroundCatalog.scrimFor("some_old_image.webp"), 0f)
    }
}
