package com.hag.mypersonalcrossword.desktop

import com.hag.mypersonalcrossword.core.RawEntry
import com.hag.mypersonalcrossword.core.parseWordCsv
import java.awt.image.BufferedImage
import java.util.Properties
import javax.imageio.ImageIO

/** Loads resources packaged in the jar. Backgrounds are JPEG copies of the shared webp masters. */
object Assets {
    private fun stream(path: String) = Assets::class.java.classLoader.getResourceAsStream(path)

    fun wordList(): List<RawEntry> =
        stream("test.csv")!!.bufferedReader(Charsets.UTF_8).useLines { parseWordCsv(it).entries }

    fun image(path: String): BufferedImage? = runCatching { stream(path)?.use { ImageIO.read(it) } }.getOrNull()

    private val bgCache = object : LinkedHashMap<String, BufferedImage?>(8, 0.75f, true) {
        override fun removeEldestEntry(e: MutableMap.MutableEntry<String, BufferedImage?>?) = size > 4
    }
    /** Catalog file name (…webp) -> packaged jpg. Small LRU cache, like Android. */
    fun background(file: String): BufferedImage? = synchronized(bgCache) {
        bgCache.getOrPut(file) { image("backgrounds/" + file.removeSuffix(".webp") + ".jpg") }
    }

    val studioLogo: BufferedImage? by lazy { image("branding/Hot_Attic_Games_Master_Logo.png") }
    val gameSplash: BufferedImage? by lazy { image("branding/my_personal_crossword_splash_1080x2280.png") }
    val icon: BufferedImage? by lazy { image("branding/icon.png") }

    val buildInfo: Properties by lazy { Properties().also { p -> stream("build-info.properties")?.use { p.load(it) } } }
    val versionName: String get() = buildInfo.getProperty("version", "dev")
}
