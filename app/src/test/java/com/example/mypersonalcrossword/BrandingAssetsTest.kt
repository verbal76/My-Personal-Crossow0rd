package com.hag.mypersonalcrossword

import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the owner-supplied game art: the shipped copies are exact, and have the intended sizes. */
class BrandingAssetsTest {
    private fun repoFile(path: String) = File("../$path").takeIf { it.exists() } ?: File(path)

    @Test fun gameSplashIsTheSuppliedArtByteForByte() {
        val master = repoFile("branding/my_personal_crossword_splash_1080x2280.png").readBytes()
        val shipped = repoFile("app/src/main/res/drawable-nodpi/mpc_splash.png").readBytes()
        assertTrue(master.isNotEmpty())
        assertArrayEquals(master, shipped)
        val img = ImageIO.read(repoFile("branding/my_personal_crossword_splash_1080x2280.png"))
        assertEquals(MPC_SPLASH_WIDTH_PX, img.width)
        assertEquals(MPC_SPLASH_HEIGHT_PX, img.height)
    }

    @Test fun iconMasterIs1024Square() {
        val img = ImageIO.read(repoFile("branding/my_personal_crossword_app_icon.png"))
        assertEquals(1024, img.width)
        assertEquals(1024, img.height)
    }

    @Test fun everyDensityHasAllLauncherLayers() {
        for (d in listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"))
            for (n in listOf("ic_launcher", "ic_launcher_round", "ic_launcher_foreground", "ic_launcher_background", "ic_launcher_monochrome"))
                assertTrue("$d/$n", repoFile("app/src/main/res/mipmap-$d/$n.webp").length() > 0)
    }
}
