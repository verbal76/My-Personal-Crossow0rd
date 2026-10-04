package com.hag.mypersonalcrossword

import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the Hot Attic Games studio card: exact canonical art, and a sane on-screen time. */
class StudioSplashTest {
    private fun repoFile(path: String) = File("../$path").takeIf { it.exists() } ?: File(path)

    @Test fun shippedLogoIsTheCanonicalBrandingFileByteForByte() {
        val canonical = repoFile("branding/Hot_Attic_Games_Master_Logo.png").readBytes()
        val shipped = repoFile("app/src/main/res/drawable-nodpi/hot_attic_logo.png").readBytes()
        assertTrue("canonical logo missing", canonical.isNotEmpty())
        assertArrayEquals("res copy must not be recoloured, cropped or resized", canonical, shipped)
    }

    @Test fun studioCardIsShownForRoughlyOneAndAHalfSeconds() {
        assertTrue("was $STUDIO_SPLASH_MS ms", STUDIO_SPLASH_MS in 1000L..2000L)
    }
}
