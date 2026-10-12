package com.hag.mypersonalcrossword.desktop

import com.hag.mypersonalcrossword.core.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class PlatformAndPersistenceTest {
    @Test fun windowsUsesAppData() {
        val d = Platform.dataDir(Platform.Os.WINDOWS, mapOf("APPDATA" to "C:\\Users\\k\\AppData\\Roaming"), "C:\\Users\\k")
        assertEquals(File("C:\\Users\\k\\AppData\\Roaming", "HotAtticGames/MyPersonalCrossword"), d)
    }
    @Test fun windowsFallsBackWithoutAppData() {
        val d = Platform.dataDir(Platform.Os.WINDOWS, emptyMap(), "/home/k")
        assertTrue(d.path.replace('\\', '/').endsWith("AppData/Roaming/HotAtticGames/MyPersonalCrossword"))
    }
    @Test fun linuxUsesXdgThenHome() {
        assertEquals(File("/x/data", "HotAtticGames/MyPersonalCrossword"), Platform.dataDir(Platform.Os.LINUX, mapOf("XDG_DATA_HOME" to "/x/data"), "/home/deck"))
        assertEquals(File("/home/deck/.local/share", "HotAtticGames/MyPersonalCrossword"), Platform.dataDir(Platform.Os.LINUX, emptyMap(), "/home/deck"))
    }
    @Test fun overrideWins() {
        assertEquals(File("/tmp/x"), Platform.dataDir(Platform.Os.LINUX, mapOf(Platform.DATA_DIR_ENV to "/tmp/x"), "/home/deck"))
    }

    @Test fun settingsSurviveRestart() {
        val d = T.dir(); val a = ProfileStore(d)
        a.score = 42; a.fullscreen = true; a.background = "sky_2_sunrise.webp"; a.category = "FOOD"; a.difficulty = "HARD"
        a.dailyDone = setOf("2026-10-11", "2026-10-12"); a.ledger = "abc,def"
        val b = ProfileStore(d)
        assertEquals(42, b.score); assertTrue(b.fullscreen); assertEquals("sky_2_sunrise.webp", b.background)
        assertEquals("FOOD", b.category); assertEquals("HARD", b.difficulty)
        assertEquals(setOf("2026-10-11", "2026-10-12"), b.dailyDone); assertEquals("abc,def", b.ledger)
    }

    @Test fun sessionRoundTripsThroughSharedCodec() {
        val g = T.controller(); assertTrue(g.startPuzzle("MIXED", Difficulty.EASY))
        g.type('A'); g.advance(5_000); g.save()
        val s = g.session!!
        val back = ProfileStore(g.let { T.dir() })
        back.saveSession(s)
        val loaded = back.loadSession(s.slot.id)!!
        assertEquals(s.words, loaded.words); assertEquals(s.inputs, loaded.inputs); assertEquals(5L, loaded.elapsedSeconds)
    }

    @Test fun corruptSaveIsIgnoredNotFatal() {
        val d = T.dir(); val store = ProfileStore(d)
        File(d, "saves/bad.save").writeText("garbage\nnot a session")
        File(d, "profile.properties").writeText("score=notanumber")
        assertTrue(ProfileStore(d).savedSessions().isEmpty())
        assertEquals(0, ProfileStore(d).score)
        assertNull(store.loadSession("nope"))
    }

    @Test fun clearRemovesSave() {
        val g = T.controller(); g.startPuzzle("MIXED", Difficulty.EASY); g.save()
        val store = ProfileStore(T.dir()); store.saveSession(g.session!!)
        assertEquals(1, store.savedSessions().size)
        store.clearSession(g.session!!.slot.id)
        assertEquals(0, store.savedSessions().size)
    }

    @Test fun writesLeaveNoTempFiles() {
        val d = T.dir(); val s = ProfileStore(d); s.score = 3
        assertTrue(d.listFiles()!!.none { it.name.endsWith(".tmp") })
    }
}
