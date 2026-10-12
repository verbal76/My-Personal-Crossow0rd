package com.hag.mypersonalcrossword.desktop

import com.hag.mypersonalcrossword.core.*
import org.junit.Assert.*
import org.junit.Test

/** Local Team ("pass the Deck") and Vindictive on desktop: the same core rules, sequenced like the Android screen. */
class ModesTest {
    private fun wrongLetter(c: Char) = if (c == 'Z') 'Q' else 'Z'
    private fun typeRight(g: GameController, w: PlacedWord) {
        g.selectWord(w)
        for ((i, c) in w.cells().withIndex()) if (g.inputs[c] != w.word[i]) g.type(w.word[i])
    }
    private fun typeWrong(g: GameController, w: PlacedWord) {
        g.selectWord(w)
        for ((i, c) in w.cells().withIndex()) if (g.inputs[c] == null) g.type(wrongLetter(w.word[i]))
    }
    private fun unsolved(g: GameController) = g.board!!.clueOrder.filter { !isWordSolved(it, g.inputs) }
    private fun team(store: ProfileStore = ProfileStore(T.dir())) = T.controller(store).also { it.startPuzzle("MIXED", Difficulty.MEDIUM, GameMode.TEAM) }
    private fun vind(timer: Int = 30, store: ProfileStore = ProfileStore(T.dir())) = T.controller(store).also { it.startPuzzle("MIXED", Difficulty.MEDIUM, GameMode.VINDICTIVE, timer) }
    private fun ready(g: GameController) { while (g.modal != null) g.resolveModal(0) }

    // ── Team ─────────────────────────────────────────────────────────────────
    @Test fun teamStartsWithPlayerOneAndTwoNamedPlayers() {
        val g = team(); assertEquals(GameMode.TEAM, g.mode); assertEquals(listOf("Player 1", "Player 2"), g.names)
        assertEquals(0, g.team.turn); assertNull(g.modal); assertTrue(g.statusLine.contains("Player 1's turn"))
    }

    @Test fun aCorrectAttemptScoresPassesTheTurnAndHoldsInputUntilReady() {
        val g = team(); typeRight(g, unsolved(g).first())
        assertEquals(1, g.team.p1Correct); assertEquals(1, g.team.turn)
        assertEquals("Pass the Phone!", g.modal!!.title)
        val before = g.inputs; g.type('A'); assertEquals("typing is held while the message is open", before, g.inputs)
        assertNotNull(g.blockedReason); ready(g); assertNull(g.blockedReason)
    }

    @Test fun aWrongAttemptIsOneShotClearedAndStillPassesTheTurn() {
        val g = team(); val w = unsolved(g).first(); typeWrong(g, w)
        assertEquals(0, g.team.p1Correct); assertEquals(1, g.team.turn)
        assertTrue("a failed attempt leaves no letters", w.cells().all { g.inputs[it] == null })
        assertTrue(g.modal!!.message.contains("Not quite"))
    }

    @Test fun teamTurnsAlternateAndPlayerTwoIsCreditedForTheirWords() {
        val g = team(); val ws = unsolved(g)
        typeRight(g, ws[0]); ready(g); typeRight(g, ws[1]); ready(g)
        assertEquals(1, g.team.p1Correct); assertEquals(1, g.team.p2Correct); assertEquals(0, g.team.turn)
    }

    @Test fun teamCompletionPaysTheFullRewardOnceAndShowsTheMvp() {
        val store = ProfileStore(T.dir()); val g = team(store)
        while (g.active) { typeRight(g, unsolved(g).first()); ready(g) }
        val r = g.result!!
        assertEquals(GameMode.TEAM, r.mode); assertEquals(Economy.pointsForDifficulty(Difficulty.MEDIUM), store.score)
        assertTrue(r.paid); assertEquals(g.board!!.words.size, r.team.p1Correct + r.team.p2Correct)
        assertEquals(TeamRules.mvp(r.team), if (r.team.p1Correct > r.team.p2Correct) 0 else if (r.team.p2Correct > r.team.p1Correct) 1 else null)
    }

    @Test fun playerTwoCannotBuyHintsOnThisDeviceAndPlayerOnePays() {
        val store = ProfileStore(T.dir()); store.score = 5; val g = team(store)
        assertTrue(g.canHint); assertTrue(g.hint()); assertEquals(4, store.score); assertEquals(1, g.team.p1Hints)
        typeRight(g, unsolved(g).first()); ready(g)            // now player 2's turn
        assertEquals(1, g.team.turn); assertFalse("Player 2 has no profile score to pay with (as on Android)", g.canHint)
    }

    @Test fun teamSavesAndWelcomesTheNextPlayerBack() {
        val store = ProfileStore(T.dir()); val a = team(store)
        typeRight(a, unsolved(a).first()); ready(a); a.unload()
        val b = T.controller(store); assertTrue(b.resume(store.savedSessions().first().slot.id))
        assertEquals(GameMode.TEAM, b.mode); assertEquals(1, b.team.p1Correct); assertEquals(1, b.team.turn)
        assertEquals("Welcome back", b.modal!!.title)
    }

    // ── Vindictive ───────────────────────────────────────────────────────────
    @Test fun vindictiveOpensWithAnOwnAnswerThenAssignsAClue() {
        val g = vind(); assertEquals(VindicativePhase.PICK_OWN, g.vind.phase)
        typeRight(g, unsolved(g).first())
        assertEquals(VindicativePhase.ASSIGN_CLUE, g.vind.phase); assertEquals(Economy.VIND_OWN_CORRECT, g.vind.p1Score)
        assertNull("the same player keeps the Deck to pick", g.modal)
        assertTrue(g.assigning); assertNotNull("typing is held while assigning", g.blockedReason)
        val before = g.inputs; g.type('A'); assertEquals(before, g.inputs)
    }

    @Test fun aWrongOwnAnswerCostsAPointShowsATauntAndClearsTheLetters() {
        val g = vind(); val w = unsolved(g).first(); typeWrong(g, w)
        assertEquals(Economy.VIND_OWN_WRONG, g.vind.p1Score); assertEquals(VindicativePhase.ASSIGN_CLUE, g.vind.phase)
        assertTrue(w.cells().all { g.inputs[it] == null })
        assertTrue("a taunt is shown", (0 until TAUNT_COUNT).any { vindictiveTaunt(it, "Player 1", "Player 2") == g.message })
    }

    @Test fun givingAClueHandsOverTheDeckAndTheClockWaitsForReady() {
        val g = vind(timer = 15); typeRight(g, unsolved(g).first())
        val target = unsolved(g).first(); g.selectWord(target); assertTrue(g.canGive); assertTrue(g.give())
        assertEquals(VindicativePhase.OPPONENT_WAIT, g.vind.phase); assertEquals(1, g.vind.currentPlayer); assertEquals(target, g.forcedWord)
        assertEquals("Pass the Phone!", g.modal!!.title)
        g.advance(5_000); assertEquals("the clock holds while the message is open", 15_000L, g.vindMsLeft)
        ready(g); g.advance(4_000); assertEquals(11_000L, g.vindMsLeft)
    }

    @Test fun theAnswererIsForcedOntoTheAssignedClue() {
        val g = vind(); typeRight(g, unsolved(g).first())
        val target = unsolved(g).first(); g.selectWord(target); g.give(); ready(g)
        val other = unsolved(g).first { it != target }
        g.selectWord(other); assertEquals(target, g.activeWord)
        g.tap(other.cells().first { it !in target.cells() }); assertEquals(target, g.activeWord)
        g.nextClue(); assertEquals(target, g.activeWord); g.flip(); assertEquals(target.direction, g.selection!!.direction)
    }

    @Test fun aCorrectAssignedAnswerScoresAndTheAnswererPicksNext() {
        val g = vind(); typeRight(g, unsolved(g).first())
        val target = unsolved(g).first(); g.selectWord(target); g.give(); ready(g)
        typeRight(g, target)
        assertEquals(Economy.VIND_ASSIGNED_CORRECT, g.vind.p2Score); assertEquals(VindicativePhase.ASSIGN_CLUE, g.vind.phase)
        assertEquals(1, g.vind.currentPlayer); assertEquals("Player 2 picks next", g.modal!!.title)
    }

    @Test fun aWrongAssignedAnswerCostsTwo() {
        val g = vind(); typeRight(g, unsolved(g).first())
        val target = unsolved(g).first(); g.selectWord(target); g.give(); ready(g); typeWrong(g, target)
        assertEquals(Economy.VIND_ASSIGNED_WRONG, g.vind.p2Score); assertTrue(g.modal!!.message.contains("Wrong!"))
    }

    @Test fun whenTheClockRunsOutTheAnswererMayStillAnswerOrPassForMinusOne() {
        val g = vind(timer = 15); typeRight(g, unsolved(g).first())
        val target = unsolved(g).first(); g.selectWord(target); g.give(); ready(g)
        g.advance(15_500)
        assertEquals(ModalKind.TIMEOUT, g.modal!!.kind); assertEquals(listOf("Answer it", "Pass (${Economy.VIND_PASS})"), g.modal!!.buttons)
        g.resolveModal(0)                                          // Answer it: still forced, pass stays open
        assertNull(g.modal); assertTrue(g.canPass); assertEquals(target, g.forcedWord)
        assertTrue(g.pass())
        assertEquals(Economy.VIND_PASS, g.vind.p2Score); assertEquals(VindicativePhase.ASSIGN_CLUE, g.vind.phase); assertEquals(1, g.vind.currentPlayer)
    }

    @Test fun choosingPassOnTheTimeoutDialogPassesImmediately() {
        val g = vind(timer = 15); typeRight(g, unsolved(g).first())
        g.selectWord(unsolved(g).first()); g.give(); ready(g); g.advance(15_500); g.resolveModal(1)
        assertEquals(Economy.VIND_PASS, g.vind.p2Score); assertEquals(VindicativePhase.ASSIGN_CLUE, g.vind.phase)
    }

    @Test fun theLastLetterOfAWordCannotBeRevealedByAHintInVindictive() {
        val store = ProfileStore(T.dir()); store.score = 20; val g = vind(store = store)
        val w = unsolved(g).first(); g.selectWord(w)
        repeat(w.word.length - 1) { assertTrue("hint ${it + 1}", g.hint()) }
        assertFalse("one open square left: it must be the player's own", g.canHint)
    }

    @Test fun theAnswerClockIsSavedAndRestoredWithTheRemainingSeconds() {
        val store = ProfileStore(T.dir()); val a = vind(timer = 60, store = store); typeRight(a, unsolved(a).first())
        a.selectWord(unsolved(a).first()); a.give(); ready(a); a.advance(20_000); a.unload()
        val b = T.controller(store); assertTrue(b.resume(store.savedSessions().first().slot.id))
        assertEquals(VindicativePhase.OPPONENT_WAIT, b.vind.phase); assertEquals(40_000L, b.vindMsLeft)
        assertEquals("Welcome back", b.modal!!.title); b.advance(5_000); assertEquals("clock waits for Ready", 40_000L, b.vindMsLeft)
    }

    @Test fun aFullVindictiveGameBanksTheMatchScoreOnce() {
        val store = ProfileStore(T.dir()); val g = vind(store = store)
        typeRight(g, unsolved(g).first())
        while (g.active) {
            val target = unsolved(g).first(); g.selectWord(target); assertTrue(g.give()); ready(g)
            typeRight(g, target); ready(g)
            if (g.active && g.vind.phase == VindicativePhase.ASSIGN_CLUE) { /* answerer now picks */ }
        }
        val r = g.result!!
        assertEquals(GameMode.VINDICTIVE, r.mode); assertEquals(Economy.vindictiveBank(r.vind.p1Score), r.awarded)
        assertEquals(Economy.vindictiveBank(r.vind.p1Score), store.score); assertTrue(r.paid)
    }

    @Test fun aNegativeMatchScoreBanksZeroAndNeverReducesTheLifetimeScore() {
        val store = ProfileStore(T.dir()); store.score = 7; val g = vind(store = store)
        typeWrong(g, unsolved(g).first())                                  // Player 1: -1
        while (g.active) {
            val target = unsolved(g).first(); g.selectWord(target); assertTrue(g.give()); ready(g)
            // Player 2 always answers right; Player 1 answers wrong (-2) unless it is the last word
            if (g.vind.currentPlayer == 1 || unsolved(g).size == 1) typeRight(g, target) else typeWrong(g, target)
            ready(g)
        }
        val r = g.result!!
        assertTrue("player 1 finished below zero: ${r.vind.p1Score}", r.vind.p1Score < 0)
        assertEquals(0, r.awarded); assertEquals(7, store.score)
        assertEquals(1, VindictiveRules.winner(r.vind))
    }

    @Test fun dailyIsAlwaysSoloEvenWhenAnotherModeIsSelected() {
        val g = T.controller(); g.startDaily(); assertEquals(GameMode.DAILY, g.mode); assertEquals("", g.statusLine)
    }
}
