package com.hag.mypersonalcrossword.desktop

import com.hag.mypersonalcrossword.core.*
import org.junit.Assert.*
import org.junit.Test

class AppModelTest {
    private fun toMenu(m: AppModel) { m.act(Action.CONFIRM); m.act(Action.CONFIRM) }
    private fun focus(m: AppModel, item: AppModel.MenuItem) { while (m.menuItems[m.menuIndex] != item) m.act(Action.DOWN) }

    @Test fun startsOnStudioSplashThenGameSplashThenMenuByTime() {
        val m = T.model(); assertEquals(Screen.STUDIO_SPLASH, m.screen)
        m.tick(AppModel.STUDIO_MS); assertEquals(Screen.GAME_SPLASH, m.screen)
        m.tick(AppModel.GAME_MS); assertEquals(Screen.MENU, m.screen)
    }

    @Test fun splashesAreSkippableWithAnyConfirm() { val m = T.model(); toMenu(m); assertEquals(Screen.MENU, m.screen) }

    @Test fun keyboardPlayThroughToResultsAndBack() {
        val store = ProfileStore(T.dir()); val m = T.model(store); toMenu(m)
        focus(m, AppModel.MenuItem.START); m.act(Action.CONFIRM)
        assertEquals(Screen.PUZZLE, m.screen)
        for (w in m.game.board!!.clueOrder) { m.game.selectWord(w); for ((i, c) in w.cells().withIndex()) if (m.game.inputs[c] != w.word[i]) m.letter(w.word[i]); if (m.screen != Screen.PUZZLE) break }
        assertEquals(Screen.RESULTS, m.screen); assertEquals(1, store.score)
        m.act(Action.CONFIRM); assertEquals(Screen.MENU, m.screen); assertNull(m.game.session)
    }

    @Test fun menuOptionsCycleAndPersist() {
        val store = ProfileStore(T.dir()); val m = T.model(store); toMenu(m)
        focus(m, AppModel.MenuItem.DIFFICULTY); m.act(Action.RIGHT)
        assertEquals(Difficulty.MEDIUM, m.difficulty); assertEquals("MEDIUM", ProfileStore(store.dir).difficulty)
        m.act(Action.LEFT); m.act(Action.LEFT); assertEquals(Difficulty.GENIUS, m.difficulty)
        focus(m, AppModel.MenuItem.CATEGORY); m.act(Action.RIGHT); assertNotEquals("MIXED", m.category)
        focus(m, AppModel.MenuItem.BACKGROUND); val first = m.background.file; m.act(Action.RIGHT)
        assertNotEquals(first, m.background.file); assertEquals(m.background.file, ProfileStore(store.dir).background)
    }

    @Test fun everyCatalogBackgroundHasADesktopImage() {
        for (b in BackgroundCatalog.all) assertNotNull(b.file, Assets.background(b.file))
    }

    @Test fun brandingAssetsPackaged() { assertNotNull(Assets.studioLogo); assertNotNull(Assets.gameSplash); assertNotNull(Assets.icon) }

    @Test fun wordListLoadsAndIsTheSharedOne() {
        assertTrue(T.entries.size > 500)
        assertTrue(T.entries.all { e -> e.answer.all { it in 'A'..'Z' } })
    }

    @Test fun padOpensOnScreenKeyboardAndTypesWithoutAPhysicalKeyboard() {
        val m = T.model(); toMenu(m); focus(m, AppModel.MenuItem.START); m.act(Action.CONFIRM, fromPad = true)
        m.act(Action.CONFIRM, fromPad = true); assertTrue(m.keyboardOpen)
        m.act(Action.CONFIRM, fromPad = true)          // types the key under the cursor
        assertEquals(1, m.game.inputs.size)
        m.act(Action.CANCEL, fromPad = true); assertFalse(m.keyboardOpen); assertEquals(Screen.PUZZLE, m.screen)
    }

    @Test fun oskCanSolveAWordUsingOnlyTheDpad() {
        val m = T.model(); toMenu(m); focus(m, AppModel.MenuItem.START); m.act(Action.CONFIRM, fromPad = true)
        val w = m.game.activeWord!!; m.act(Action.CONFIRM, fromPad = true)
        for ((i, c) in w.cells().withIndex()) {
            if (m.game.inputs[c] == w.word[i]) continue
            val target = w.word[i].toString()
            var guard = 0
            while (m.keyboard.key() != target && guard++ < 40) {
                // pick the first single move that gets us closer on the same row, else go down
                val row = OnScreenKeyboard.ROWS[m.keyboard.row]
                if (target in row) m.act(Action.RIGHT, true) else m.act(Action.DOWN, true)
            }
            assertEquals(target, m.keyboard.key()); m.act(Action.CONFIRM, true)
        }
        assertTrue(isWordSolved(w, m.game.inputs))
    }

    @Test fun pauseSavesAndContinueResumes() {
        val store = ProfileStore(T.dir()); val m = T.model(store); toMenu(m)
        focus(m, AppModel.MenuItem.START); m.act(Action.CONFIRM)
        m.letter('A'); m.act(Action.CANCEL); assertEquals(Screen.PAUSE, m.screen)
        m.act(Action.DOWN); m.act(Action.CONFIRM); assertEquals(Screen.MENU, m.screen)
        assertEquals(1, store.savedSessions().size)
        assertTrue(AppModel.MenuItem.CONTINUE in m.menuItems)
        m.focusMenu(0); m.act(Action.CONFIRM); assertEquals(Screen.PUZZLE, m.screen); assertTrue(m.game.inputs.isNotEmpty())
    }

    @Test fun quitSavesTheActivePuzzleAndNotifiesHost() {
        val store = ProfileStore(T.dir()); val host = T.FakeHost(); val m = T.model(store, host); toMenu(m)
        focus(m, AppModel.MenuItem.START); m.act(Action.CONFIRM); m.letter('A'); m.quit()
        assertEquals(1, host.quit); assertEquals(1, store.savedSessions().size)
    }

    @Test fun fullscreenToggleReachesHostAndPersists() {
        val store = ProfileStore(T.dir()); val host = T.FakeHost(); val m = T.model(store, host)
        m.act(Action.FULLSCREEN); assertEquals(1, host.fullscreen); assertTrue(ProfileStore(store.dir).fullscreen)
        m.act(Action.FULLSCREEN); assertFalse(ProfileStore(store.dir).fullscreen)
    }

    @Test fun lettersIgnoredOutsidePuzzle() { val m = T.model(); toMenu(m); m.letter('A'); assertNull(m.game.session) }

    @Test fun puzzleClockOnlyRunsDuringPlay() {
        val m = T.model(); toMenu(m); focus(m, AppModel.MenuItem.START); m.act(Action.CONFIRM)
        m.tick(3000); assertEquals(3L, m.game.session!!.elapsedSeconds)
        m.act(Action.CANCEL); m.tick(5000); assertEquals(3L, m.game.session!!.elapsedSeconds)
    }

    @Test fun dailyFromMenuIsTheSharedDaily() {
        val m = T.model(); toMenu(m); focus(m, AppModel.MenuItem.DAILY); m.act(Action.CONFIRM)
        assertTrue(m.game.session!!.isDaily); assertEquals(Difficulty.EXPERT, m.game.session!!.difficulty)
    }
}
