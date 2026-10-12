package com.hag.mypersonalcrossword.desktop

import com.hag.mypersonalcrossword.core.*
import org.junit.Assert.*
import org.junit.Test

class ModeMenuTest {
    private fun toMenu(m: AppModel) { m.act(Action.CONFIRM); m.act(Action.CONFIRM) }
    private fun focus(m: AppModel, item: AppModel.MenuItem) { var n = 0; while (m.menuItems[m.menuIndex] != item && n++ < 30) m.act(Action.DOWN) }

    @Test fun modeRowCyclesSoloTeamVindictiveAndPersists() {
        val store = ProfileStore(T.dir()); val m = T.model(store); toMenu(m); focus(m, AppModel.MenuItem.MODE)
        assertEquals(GameMode.SINGLE, m.playMode)
        m.act(Action.RIGHT); assertEquals(GameMode.TEAM, m.playMode)
        m.act(Action.RIGHT); assertEquals(GameMode.VINDICTIVE, m.playMode)
        m.act(Action.RIGHT); assertEquals(GameMode.SINGLE, m.playMode)
        m.act(Action.LEFT); assertEquals("VINDICTIVE", ProfileStore(store.dir).playMode)
    }

    @Test fun theTimerRowExistsOnlyForVindictiveAndCyclesTheThreeChoices() {
        val store = ProfileStore(T.dir()); val m = T.model(store); toMenu(m)
        assertFalse(AppModel.MenuItem.TIMER in m.menuItems)
        store.playMode = "VINDICTIVE"; assertTrue(AppModel.MenuItem.TIMER in m.menuItems)
        focus(m, AppModel.MenuItem.TIMER)
        val seen = ArrayList<Int>(); repeat(3) { m.act(Action.RIGHT); seen += store.vindTimer }
        assertEquals(setOf(15, 30, 60), seen.toSet())
    }

    @Test fun soundEffectsCanBeSwitchedOffAndStayOff() {
        val store = ProfileStore(T.dir()); val m = T.model(store); toMenu(m); focus(m, AppModel.MenuItem.SOUND)
        assertTrue(store.sound); m.act(Action.CONFIRM); assertFalse(ProfileStore(store.dir).sound)
    }

    @Test fun startingInTeamModeBuildsATeamPuzzleAndDailyStaysSolo() {
        val store = ProfileStore(T.dir()); store.playMode = "TEAM"; val m = T.model(store); toMenu(m)
        focus(m, AppModel.MenuItem.START); m.act(Action.CONFIRM)
        assertEquals(Screen.PUZZLE, m.screen); assertEquals(GameMode.TEAM, m.game.mode)
        m.act(Action.CANCEL); m.act(Action.DOWN); m.act(Action.CONFIRM)               // pause -> save and go to menu
        focus(m, AppModel.MenuItem.DAILY); m.act(Action.CONFIRM); assertEquals(GameMode.DAILY, m.game.mode)
    }

    @Test fun continueShowsTheModeOfTheLatestSave() {
        val store = ProfileStore(T.dir()); store.playMode = "VINDICTIVE"; val m = T.model(store); toMenu(m)
        focus(m, AppModel.MenuItem.START); m.act(Action.CONFIRM); m.act(Action.CANCEL); m.act(Action.DOWN); m.act(Action.CONFIRM)
        assertEquals(GameMode.VINDICTIVE, m.savedSessions.first().mode)
        assertEquals(AppModel.MenuItem.CONTINUE, m.menuItems.first())
    }

    @Test fun messagesAreAnsweredByKeyboardPadAndMouseAndHoldTheGameMeanwhile() {
        val store = ProfileStore(T.dir()); store.playMode = "TEAM"; val m = T.model(store); toMenu(m)
        focus(m, AppModel.MenuItem.START); m.act(Action.CONFIRM)
        val w = m.game.board!!.clueOrder.first(); m.game.selectWord(w)
        for (c in w.word) m.letter(c)                                       // correct word -> "Pass the Phone!"
        assertNotNull(m.game.modal)
        m.act(Action.UP); m.act(Action.LEFT); m.act(Action.CANCEL, fromPad = true)    // Esc/B answers the message
        assertNull(m.game.modal); assertEquals(Screen.PUZZLE, m.screen)
        val w2 = m.game.board!!.clueOrder.first { !isWordSolved(it, m.game.inputs) }; m.game.selectWord(w2)
        for (c in w2.word) m.letter(c); assertNotNull(m.game.modal)
        m.answerModal(0); assertNull("a click answers it too", m.game.modal)
    }

    @Test fun enterGivesTheClueInVindictiveAndTheTimeoutDialogIsNavigable() {
        val store = ProfileStore(T.dir()); store.playMode = "VINDICTIVE"; store.vindTimer = 15; val m = T.model(store); toMenu(m)
        focus(m, AppModel.MenuItem.START); m.act(Action.CONFIRM)
        val first = m.game.board!!.clueOrder.first(); m.game.selectWord(first); for (c in first.word) m.letter(c)
        assertTrue(m.game.assigning)
        m.act(Action.CONFIRM)                                               // Enter = Give
        assertEquals(VindicativePhase.OPPONENT_WAIT, m.game.vind.phase); m.act(Action.CONFIRM)   // "I'm Ready"
        m.tick(15_500)
        assertEquals(ModalKind.TIMEOUT, m.game.modal!!.kind)
        m.act(Action.RIGHT); assertEquals(1, m.modalIndex); m.act(Action.CONFIRM)               // Pass
        assertEquals(Economy.VIND_PASS, m.game.vind.p2Score)
    }

    @Test fun passAndGiveAreReachableFromTheOnScreenKeyboardAndF2() {
        assertEquals(Action.PASS, KeyMap.action(java.awt.event.KeyEvent.VK_F2))
        assertTrue("GIVE" in OnScreenKeyboard.ROWS.flatten() && "PASS" in OnScreenKeyboard.ROWS.flatten())
    }

    @Test fun menuDoesNotReadTheDiskOnEveryDraw() {
        val store = ProfileStore(T.dir()); val m = T.model(store)
        java.io.File(store.dir, "saves/late.save").writeText("v2\nmeta:mode=SINGLE\nwords:\n")
        val before = m.menuItems; assertEquals(before, m.menuItems)
        assertTrue("a bad file added later is not even looked at until the menu is re-entered", m.notice.isEmpty())
    }
}
