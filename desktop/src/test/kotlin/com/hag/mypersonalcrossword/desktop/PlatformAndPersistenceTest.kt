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

class SaveSafetyTest {
    private fun session(inputsToType: Int = 0): com.hag.mypersonalcrossword.core.PuzzleSession {
        val g = T.controller(); g.startPuzzle("MIXED", com.hag.mypersonalcrossword.core.Difficulty.EASY)
        repeat(inputsToType) { g.type('A') }; return g.session!!
    }
    private fun saveFile(dir: java.io.File) = java.io.File(dir, "saves").listFiles { f -> f.name.endsWith(".save") }!!.single()

    @Test fun aCorruptSaveIsSetAsideNotDeletedAndTheNoticeSaysSo() {
        val d = T.dir(); val s = ProfileStore(d); val ses = session(); s.saveSession(ses)
        saveFile(d).writeText("v2\nmeta:garbage\nwords:\n")
        val fresh = ProfileStore(d)
        assertNull(fresh.loadSession(ses.slot.id))
        assertTrue(fresh.notice.contains("could not be read"))
        val kept = java.io.File(d, "corrupt").listFiles()!!
        assertEquals(1, kept.size); assertTrue(kept.single().readText().contains("garbage"))
        assertTrue("the damaged file left the save list", java.io.File(d, "saves").listFiles { f -> f.name.endsWith(".save") }!!.isEmpty())
    }

    @Test fun theBackupOfThePreviousGoodSaveIsRestoredWhenTheLatestIsDamaged() {
        val d = T.dir(); val s = ProfileStore(d)
        val first = session(1); s.saveSession(first)
        val second = first.copy(inputs = first.inputs + (Pair(99, 99) to 'Q'))
        s.saveSession(second)                                              // first becomes .bak
        saveFile(d).writeText("truncated")
        val fresh = ProfileStore(d)
        val got = fresh.loadSession(first.slot.id)!!
        assertEquals(first.inputs, got.inputs)
        assertTrue(fresh.notice.contains("previous save was restored"))
        assertEquals("a restored save is a normal save again", first.inputs, ProfileStore(d).loadSession(first.slot.id)!!.inputs)
    }

    @Test fun backupIsOnlyEverAGoodSaveAndClearingRemovesIt() {
        val d = T.dir(); val s = ProfileStore(d); val ses = session()
        s.saveSession(ses); s.saveSession(ses)
        assertEquals(1, java.io.File(d, "saves").listFiles { f -> f.name.endsWith(".bak") }!!.size)
        s.clearSession(ses.slot.id)
        assertTrue(java.io.File(d, "saves").listFiles()!!.isEmpty())
    }

    @Test fun aDamagedBackupIsNotCopiedOverAGoodSave() {
        val d = T.dir(); val s = ProfileStore(d); val ses = session(); s.saveSession(ses)
        saveFile(d).writeText("bad"); s.saveSession(ses)                    // existing file is bad: must NOT become the .bak
        assertTrue(java.io.File(d, "saves").listFiles { f -> f.name.endsWith(".bak") }!!.isEmpty())
        assertNotNull(ProfileStore(d).loadSession(ses.slot.id))
    }

    @Test fun listingSavesQuarantinesOnlyTheBadOnesAndReportsOnce() {
        val d = T.dir(); val s = ProfileStore(d)
        val a = session(); s.saveSession(a)
        java.io.File(d, "saves/other.save").writeText("nonsense")
        val fresh = ProfileStore(d)
        assertEquals(1, fresh.savedSessions().size)
        assertEquals(1, java.io.File(d, "corrupt").listFiles()!!.size)
        assertEquals("a second listing finds nothing new to move", 1, fresh.savedSessions().size)
        assertEquals(1, java.io.File(d, "corrupt").listFiles()!!.size)
    }

    @Test fun anUnreadableProfileFileIsKeptNotDiscarded() {
        val d = T.dir(); java.io.File(d, "profile.properties").writeBytes(byteArrayOf(0x5C, 0x75, 0x00, 0x7A))   // malformed \u escape
        val s = ProfileStore(d)
        assertEquals(0, s.score); assertTrue(s.notice.contains("profile file"))
        assertEquals(1, java.io.File(d, "corrupt").listFiles()!!.size)
    }

    @Test fun menuShowsTheNoticeUntilAPuzzleStarts() {
        val d = T.dir(); val s = ProfileStore(d)
        java.io.File(d, "saves/x.save").writeText("junk")
        val m = AppModel(s, T.controller(s), T.FakeHost())
        m.savedSessions
        assertTrue(m.notice.isNotEmpty())
        m.act(Action.CONFIRM); m.act(Action.CONFIRM)
        while (m.menuItems[m.menuIndex] != AppModel.MenuItem.START) m.act(Action.DOWN)
        m.act(Action.CONFIRM); assertEquals("", m.notice)
    }
}
