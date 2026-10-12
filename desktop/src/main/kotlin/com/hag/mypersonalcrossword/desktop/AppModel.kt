package com.hag.mypersonalcrossword.desktop

import com.hag.mypersonalcrossword.core.*

enum class Screen { STUDIO_SPLASH, GAME_SPLASH, MENU, PUZZLE, PAUSE, RESULTS }

/** Things only the window can do. */
interface Host {
    val isFullscreen: Boolean
    fun toggleFullscreen()
    fun quit()
}

/**
 * Screen flow + input routing for the desktop build. Pure Kotlin: every input device turns
 * into [Action]s / letters, so keyboard, mouse, touch, gamepad and Steam Input all drive the
 * exact same code and the same tests cover them.
 */
class AppModel(val store: ProfileStore, val game: GameController, private val host: Host) {
    var screen = Screen.STUDIO_SPLASH; private set
    val keyboard = OnScreenKeyboard()
    var keyboardOpen = false; private set
    var menuIndex = 0; private set
    var pauseIndex = 0; private set
    private var autosaveMs = 0L
    var screenMillis = 0L; private set      // time on the current screen (splashes)
    /** Which input family was used last, so the UI can show matching hints (keys vs. pad). */
    var lastInput = "keyboard"; private set

    private val difficulties = listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD, Difficulty.EXPERT, Difficulty.GENIUS)
    var categoryIndex = game.categories.indexOf(store.category).coerceAtLeast(0); private set
    var difficultyIndex = difficulties.indexOfFirst { it.name == store.difficulty }.coerceAtLeast(0); private set
    val backgrounds: List<CatalogBackground> = BackgroundCatalog.all
    var backgroundIndex = backgrounds.indexOfFirst { it.file == store.background }.takeIf { it >= 0 } ?: 0; private set

    val category: String get() = game.categories[categoryIndex]
    val difficulty: Difficulty get() = difficulties[difficultyIndex]
    val background: CatalogBackground get() = backgrounds[backgroundIndex]

    enum class MenuItem(val label: String) {
        CONTINUE("Continue"), DAILY("Daily Puzzle"), CATEGORY("Category"), DIFFICULTY("Difficulty"),
        START("Start Puzzle"), BACKGROUND("Background"), PROMPTS("Button prompts"), FULLSCREEN("Fullscreen"), QUIT("Quit")
    }
    /** Which prompt family to draw: the manual choice, or the device used last (mouse counts as keyboard). */
    val promptInput: String get() = when (store.prompts) { "controller" -> "pad"; "keyboard" -> "keyboard"; else -> lastInput }

    val savedSessions: List<PuzzleSession> get() = store.savedSessions()
    val menuItems: List<MenuItem> get() =
        MenuItem.values().filter { it != MenuItem.CONTINUE || savedSessions.isNotEmpty() }
    val pauseItems = listOf("Resume", "Save and go to menu", "Toggle fullscreen", "Quit game")

    // ── Time ─────────────────────────────────────────────────────────────────

    fun tick(ms: Long) {
        screenMillis += ms
        when (screen) {
            Screen.STUDIO_SPLASH -> if (screenMillis >= STUDIO_MS) go(Screen.GAME_SPLASH)
            Screen.GAME_SPLASH   -> if (screenMillis >= GAME_MS) go(Screen.MENU)
            Screen.PUZZLE        -> {
                game.advance(ms)
                // Crash / suspend / power-loss insurance, like Android's save on pause.
                autosaveMs += ms
                if (autosaveMs >= AUTOSAVE_MS) { autosaveMs = 0; game.save() }
            }
            else -> {}
        }
    }

    private fun go(s: Screen) { screen = s; screenMillis = 0 }

    // ── Input ────────────────────────────────────────────────────────────────

    fun letter(ch: Char) {
        lastInput = "keyboard"
        if (screen == Screen.PUZZLE) { game.type(ch); afterPuzzleChange() }
    }

    fun act(a: Action, fromPad: Boolean = false) {
        lastInput = if (fromPad) "pad" else "keyboard"
        if (a == Action.FULLSCREEN) { toggleFullscreen(); return }
        when (screen) {
            Screen.STUDIO_SPLASH, Screen.GAME_SPLASH -> if (a == Action.CONFIRM || a == Action.CANCEL) go(if (screen == Screen.STUDIO_SPLASH) Screen.GAME_SPLASH else Screen.MENU)
            Screen.MENU    -> menuAct(a)
            Screen.PUZZLE  -> puzzleAct(a)
            Screen.PAUSE   -> pauseAct(a)
            Screen.RESULTS -> if (a == Action.CONFIRM || a == Action.CANCEL || a == Action.MENU) { game.unload(); go(Screen.MENU) }
        }
    }

    private fun menuAct(a: Action) {
        val items = menuItems
        menuIndex = menuIndex.coerceIn(0, items.size - 1)
        when (a) {
            Action.UP   -> menuIndex = Math.floorMod(menuIndex - 1, items.size)
            Action.DOWN -> menuIndex = Math.floorMod(menuIndex + 1, items.size)
            Action.LEFT, Action.RIGHT -> {
                val d = if (a == Action.LEFT) -1 else 1
                when (items[menuIndex]) {
                    MenuItem.CATEGORY   -> { categoryIndex = Math.floorMod(categoryIndex + d, game.categories.size); store.category = category }
                    MenuItem.DIFFICULTY -> { difficultyIndex = Math.floorMod(difficultyIndex + d, difficulties.size); store.difficulty = difficulty.name }
                    MenuItem.PROMPTS    -> { store.prompts = PROMPT_STYLES[Math.floorMod(PROMPT_STYLES.indexOf(store.prompts) + d, PROMPT_STYLES.size)] }
                    MenuItem.BACKGROUND -> { backgroundIndex = Math.floorMod(backgroundIndex + d, backgrounds.size); store.background = background.file }
                    else -> {}
                }
            }
            Action.CONFIRM -> activate(items[menuIndex])
            Action.CANCEL  -> {}
            else -> {}
        }
    }

    fun activate(item: MenuItem) {
        when (item) {
            MenuItem.CONTINUE   -> savedSessions.firstOrNull()?.let { if (game.resume(it.slot.id)) enterPuzzle() }
            MenuItem.DAILY      -> if (game.startDaily()) enterPuzzle()
            MenuItem.START      -> if (game.startPuzzle(category, difficulty)) enterPuzzle()
            MenuItem.FULLSCREEN -> toggleFullscreen()
            MenuItem.QUIT       -> quit()
            MenuItem.CATEGORY, MenuItem.DIFFICULTY, MenuItem.BACKGROUND, MenuItem.PROMPTS -> act(Action.RIGHT)
        }
    }

    /** Index of [item] in the current menu (for mouse hit-testing). */
    fun focusMenu(i: Int) { menuIndex = i.coerceIn(0, menuItems.size - 1) }

    private fun enterPuzzle() { keyboardOpen = false; go(Screen.PUZZLE) }

    private fun puzzleAct(a: Action) {
        if (keyboardOpen) { oskAct(a); return }
        when (a) {
            Action.UP -> game.move(0, -1);   Action.DOWN -> game.move(0, 1)
            Action.LEFT -> game.move(-1, 0); Action.RIGHT -> game.move(1, 0)
            Action.CONFIRM -> if (lastInput == "pad") keyboardOpen = true else game.flip()
            Action.FLIP -> game.flip()
            Action.NEXT_CLUE -> game.nextClue(true)
            Action.PREV_CLUE -> game.nextClue(false)
            Action.HINT -> game.hint()
            Action.BACKSPACE -> game.backspace()
            Action.TOGGLE_KEYBOARD -> keyboardOpen = true
            Action.CANCEL, Action.MENU -> { pauseIndex = 0; go(Screen.PAUSE) }
            Action.FULLSCREEN -> {}
        }
        afterPuzzleChange()
    }

    private fun oskAct(a: Action) {
        when (a) {
            Action.UP -> keyboard.move(0, -1); Action.DOWN -> keyboard.move(0, 1)
            Action.LEFT -> keyboard.move(-1, 0); Action.RIGHT -> keyboard.move(1, 0)
            Action.CONFIRM -> pressKey(keyboard.key())
            Action.CANCEL, Action.TOGGLE_KEYBOARD -> keyboardOpen = false
            Action.NEXT_CLUE -> game.nextClue(true)
            Action.PREV_CLUE -> game.nextClue(false)
            Action.FLIP -> game.flip()
            Action.HINT -> game.hint()
            Action.BACKSPACE -> game.backspace()
            Action.MENU -> { keyboardOpen = false; pauseIndex = 0; go(Screen.PAUSE) }
            Action.FULLSCREEN -> {}
        }
        afterPuzzleChange()
    }

    /** A key tapped/confirmed on the on-screen keyboard. */
    fun pressKey(key: String) {
        when (key) {
            "⌫" -> game.backspace()
            "FLIP" -> game.flip()
            "HINT" -> game.hint()
            "NEXT" -> game.nextClue(true)
            "CLOSE" -> keyboardOpen = false
            else -> if (key.length == 1) game.type(key[0])
        }
        afterPuzzleChange()
    }

    private fun pauseAct(a: Action) {
        when (a) {
            Action.UP -> pauseIndex = Math.floorMod(pauseIndex - 1, pauseItems.size)
            Action.DOWN -> pauseIndex = Math.floorMod(pauseIndex + 1, pauseItems.size)
            Action.CANCEL, Action.MENU -> go(Screen.PUZZLE)
            Action.CONFIRM -> when (pauseIndex) {
                0 -> go(Screen.PUZZLE)
                1 -> { game.unload(); go(Screen.MENU) }
                2 -> toggleFullscreen()
                3 -> quit()
            }
            else -> {}
        }
    }

    private fun afterPuzzleChange() {
        if (screen == Screen.PUZZLE && game.result != null) { keyboardOpen = false; go(Screen.RESULTS) }
    }

    fun setKeyboardOpen(open: Boolean) { keyboardOpen = open }

    /** Mouse or touch was used: prompts go back to the keyboard/mouse style. */
    fun noteMouse() { lastInput = "mouse" }

    /** Toggles the window, then stores what the window actually is (not what we assumed). */
    fun toggleFullscreen() { host.toggleFullscreen(); store.fullscreen = host.isFullscreen }

    /** Clean shutdown: the puzzle in progress is saved, then the host closes. */
    fun quit() { game.save(); host.quit() }

    companion object {
        const val STUDIO_MS = 1800L
        const val GAME_MS = 2200L
        const val AUTOSAVE_MS = 20_000L
        val PROMPT_STYLES = listOf("auto", "controller", "keyboard")
    }
}
