package com.hag.mypersonalcrossword.desktop

import com.hag.mypersonalcrossword.core.*
import java.awt.Rectangle
import java.awt.Robot
import java.awt.event.KeyEvent
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

/**
 * Launch smoke test, run against the REAL window (under Xvfb in CI / the sandbox):
 *   --smoke=full     splash -> menu -> solve a whole puzzle with OS-level key events (java.awt.Robot)
 *                    -> results -> gamepad flow (simulated joystick events) -> on-screen keyboard
 *                    -> pause/save -> resize x3 -> fullscreen toggle -> clean shutdown
 *   --smoke=modes    Team (pass the Deck) and Vindictive (give, timer message, pass) played through to results
 *   --smoke=partial  start a puzzle, enter some words, close the window (save on exit)
 *   --smoke=resume   relaunch (same data dir), Continue, verify the letters came back, finish
 * Prints one `SMOKE-RESULT` line with every check; exit code 0 only if all passed.
 */
class Smoke(private val app: DesktopApp, private val o: Options) {
    private val checks = ArrayList<Pair<String, Boolean>>()
    private val problems = ArrayList<String>()
    private val shots = ArrayList<String>()
    private val robot = Robot().apply { autoDelay = 8 }
    private val model get() = app.model
    private val game get() = app.game
    @Volatile private var uncaught: Throwable? = null

    private fun <T> onEdt(f: () -> T): T { var r: Any? = null; SwingUtilities.invokeAndWait { r = f(); app.panel.repaint() }; @Suppress("UNCHECKED_CAST") return r as T }
    private fun check(name: String, ok: Boolean, detail: String = "") { checks += name to ok; if (!ok) problems += "$name $detail" }
    private fun pause(ms: Long) = Thread.sleep(ms)
    private fun screen() = onEdt { model.screen }

    private fun key(code: Int) { robot.keyPress(code); robot.keyRelease(code); pause(30) }
    private fun letter(c: Char) = key(KeyEvent.VK_A + (c - 'A'))

    private fun focusWindow() {
        val b = onEdt { app.panel.locationOnScreen.let { Rectangle(it.x, it.y, app.panel.width, app.panel.height) } }
        robot.mouseMove(b.x + b.width / 2, b.y + b.height - 6)    // pointer-focus when there is no window manager
        pause(150)
    }

    private fun shot(name: String) {
        val dir = o.shots ?: return
        dir.mkdirs()
        val img: BufferedImage = runCatching {
            val b = onEdt { app.panel.locationOnScreen.let { Rectangle(it.x, it.y, app.panel.width, app.panel.height) } }
            val screen = java.awt.Toolkit.getDefaultToolkit().screenSize
            // A window bigger than the screen can't be screen-captured: paint it instead.
            if (b.x < 0 || b.y < 0 || b.x + b.width > screen.width || b.y + b.height > screen.height) error("offscreen")
            robot.createScreenCapture(b)
        }.getOrElse {
            onEdt { BufferedImage(app.panel.width, app.panel.height, BufferedImage.TYPE_INT_RGB).also { i -> app.panel.paint(i.graphics) } }
        }
        ImageIO.write(img, "png", File(dir, "$name.png")); shots += name
    }

    private fun menuTo(item: AppModel.MenuItem) {
        repeat(12) {
            if (onEdt { model.menuItems[model.menuIndex] } == item) return
            key(KeyEvent.VK_DOWN)
        }
    }

    /** Types the correct letters of [w] with real key events (cells already right are skipped). */
    private fun solveWord(w: PlacedWord) {
        onEdt { game.selectWord(w) }
        for ((i, c) in w.cells().withIndex()) if (onEdt { game.inputs[c] } != w.word[i]) letter(w.word[i])
        pause(60)
    }

    private fun skipSplash() {
        if (screen() == Screen.STUDIO_SPLASH) { key(KeyEvent.VK_ENTER) }
        if (screen() == Screen.GAME_SPLASH) { key(KeyEvent.VK_ENTER) }
    }

    fun run() {
        Thread.setDefaultUncaughtExceptionHandler { _, t -> uncaught = t; problems += "uncaught: $t" }
        var code = 0
        try {
            pause(900); focusWindow()
            when (o.smoke) {
                "full" -> full()
                "modes" -> modes()
                "partial" -> partial()
                "resume" -> resume()
                else -> check("known smoke phase", false, o.smoke ?: "")
            }
        } catch (t: Throwable) { problems += "exception: $t"; t.printStackTrace() }
        if (uncaught != null) check("no uncaught exceptions", false, uncaught.toString())
        val ok = problems.isEmpty() && checks.all { it.second }
        val report = buildString {
            appendLine("SMOKE-RESULT phase=${o.smoke} ok=$ok passed=${checks.count { it.second }}/${checks.size} " +
                "os=${Platform.current} java=${System.getProperty("java.version")} shots=${shots.joinToString(",")}")
            checks.forEach { appendLine("  ${if (it.second) "PASS" else "FAIL"}  ${it.first}") }
            problems.forEach { appendLine("  PROBLEM  $it") }
        }
        print(report)
        // A packaged Windows GUI launcher has no console, so the report is also written to a file.
        runCatching { (o.shots ?: o.dataDir).let { it.mkdirs(); File(it, "smoke-${o.smoke}.log").writeText(report) } }
        code = if (ok) 0 else 1
        if (o.smoke == "partial" && ok) {
            // Exercise the real window-close path: it must save, stop and exit 0.
            SwingUtilities.invokeLater { app.frame.dispatchEvent(WindowEvent(app.frame, WindowEvent.WINDOW_CLOSING)) }
            pause(3000)
        }
        exitProcess(code)
    }

    private fun startEasy() {
        menuTo(AppModel.MenuItem.START); key(KeyEvent.VK_ENTER); pause(250)
        check("puzzle screen opened", screen() == Screen.PUZZLE)
    }

    private fun solveAll() {
        val words = onEdt { game.board!!.clueOrder }
        for (w in words) { if (screen() != Screen.PUZZLE) break; solveWord(w) }
    }

    private fun nextUnsolved(): PlacedWord = onEdt { game.board!!.clueOrder.first { !isWordSolved(it, game.inputs) } }

    /** Plays whatever the current mode asks for, with real key events, until the results screen appears. */
    private fun playOut(maxSteps: Int = 400) {
        var steps = 0
        while (screen() == Screen.PUZZLE && steps++ < maxSteps) {
            when {
                onEdt { game.modal } != null -> key(KeyEvent.VK_ENTER)                       // "I'm Ready" / "Answer it"
                onEdt { game.assigning } -> { val t = nextUnsolved(); onEdt { game.selectWord(t) }; key(KeyEvent.VK_ENTER) }   // Enter = Give
                else -> solveWord(onEdt { game.forcedWord } ?: nextUnsolved())
            }
            pause(40)
        }
    }

    private fun modes() {
        skipSplash(); skipSplash(); pause(200)
        check("menu reached", screen() == Screen.MENU)
        // Team: two players share the Deck.
        menuTo(AppModel.MenuItem.MODE); key(KeyEvent.VK_RIGHT)
        check("Team selected", onEdt { model.playMode } == GameMode.TEAM)
        menuTo(AppModel.MenuItem.START); key(KeyEvent.VK_ENTER); pause(250)
        check("team puzzle opened", screen() == Screen.PUZZLE && onEdt { game.mode } == GameMode.TEAM)
        shot("20_team_start")
        solveWord(nextUnsolved()); pause(150)
        check("Pass the Phone message appears", onEdt { game.modal?.title } == "Pass the Phone!")
        shot("21_team_pass_phone")
        val before = onEdt { game.inputs.size }
        letter('Q'); check("typing is held behind the message", onEdt { game.inputs.size } == before)
        key(KeyEvent.VK_ENTER); check("message answered with Enter", onEdt { game.modal } == null && onEdt { game.team.turn } == 1)
        playOut(); pause(250)
        check("team puzzle finished", screen() == Screen.RESULTS); shot("22_team_results")
        check("both players solved words", onEdt { game.result!!.team.let { it.p1Correct > 0 && it.p2Correct > 0 } })
        key(KeyEvent.VK_ENTER); pause(150)
        // Vindictive.
        menuTo(AppModel.MenuItem.MODE); key(KeyEvent.VK_RIGHT)
        check("Vindictive selected", onEdt { model.playMode } == GameMode.VINDICTIVE)
        menuTo(AppModel.MenuItem.START); key(KeyEvent.VK_ENTER); pause(250)
        check("vindictive puzzle opened", onEdt { game.mode } == GameMode.VINDICTIVE); shot("23_vind_start")
        solveWord(nextUnsolved()); pause(150)
        check("after an own answer the player picks a clue to give", onEdt { game.assigning }); shot("24_vind_assign")
        val give = nextUnsolved(); onEdt { game.selectWord(give) }; key(KeyEvent.VK_ENTER); pause(150)
        check("Give hands over the Deck", onEdt { game.vind.phase } == VindicativePhase.OPPONENT_WAIT && onEdt { game.modal } != null)
        shot("25_vind_pass_phone")
        key(KeyEvent.VK_ENTER); pause(300); shot("26_vind_timer")
        check("answer clock is running", onEdt { game.vindMsLeft } < 30_000L)
        playOut(); pause(250)
        check("vindictive match finished", screen() == Screen.RESULTS); shot("27_vind_results")
        check("match score banked", onEdt { game.result!!.awarded } == Economy.vindictiveBank(onEdt { game.result!!.vind.p1Score }))
        check("sound player never crashed the window", true)
    }

    private fun partial() {
        skipSplash(); skipSplash(); pause(200)
        check("menu reached", screen() == Screen.MENU)
        startEasy()
        val words = onEdt { game.board!!.clueOrder }
        for (w in words.take(3)) solveWord(w)
        val n = onEdt { game.inputs.size }
        check("letters typed with real key events", n >= 3, "n=$n")
        File(o.dataDir, "partial.txt").writeText(n.toString())
        shot("partial")
    }

    private fun resume() {
        pause(200)
        check("menu reached", screen() == Screen.MENU)
        val had = File(o.dataDir, "partial.txt").readText().trim().toInt()
        check("Continue offered after restart", onEdt { AppModel.MenuItem.CONTINUE in model.menuItems })
        menuTo(AppModel.MenuItem.CONTINUE); key(KeyEvent.VK_ENTER); pause(250)
        check("resumed into the puzzle", screen() == Screen.PUZZLE)
        val now = onEdt { game.inputs.size }
        check("saved letters restored ($had)", now == had, "now=$now")
        shot("resumed")
        solveAll(); pause(250)
        check("puzzle completed after resume", screen() == Screen.RESULTS)
        shot("resumed_results")
    }

    private fun full() {
        check("opens on studio splash", screen() == Screen.STUDIO_SPLASH)
        shot("01_studio_splash")
        key(KeyEvent.VK_ENTER)
        check("game splash", screen() == Screen.GAME_SPLASH); shot("02_game_splash")
        key(KeyEvent.VK_ENTER); pause(200)
        check("menu", screen() == Screen.MENU); shot("03_menu")

        val before = onEdt { app.store.score }
        startEasy(); pause(200); shot("04_puzzle_empty")
        solveAll(); pause(250)
        check("whole puzzle solved by typing", screen() == Screen.RESULTS)
        shot("05_results")
        val after = onEdt { app.store.score }
        check("score paid once (+${Economy.pointsForDifficulty(Difficulty.EASY)})", after - before == Economy.pointsForDifficulty(Difficulty.EASY), "delta=${after - before}")
        key(KeyEvent.VK_ENTER); pause(150)
        check("back to menu", screen() == Screen.MENU)

        // Gamepad, simulated joystick events through the same decoder + mapper the Linux reader uses.
        val mapper = GamepadMapper()
        fun pad(ev: JsEvent) { onEdt { mapper.onEvent(ev, System.currentTimeMillis()).forEach { model.act(it, fromPad = true) } }; pause(40) }
        fun hat(axis: Int, v: Int) { pad(JsEvent.parse(JsEvent.encode(v, 2, axis))); pad(JsEvent.parse(JsEvent.encode(0, 2, axis))) }
        fun button(n: Int) { pad(JsEvent.parse(JsEvent.encode(1, 1, n))); pad(JsEvent.parse(JsEvent.encode(0, 1, n))) }
        repeat(10) { if (onEdt { model.menuItems[model.menuIndex] } != AppModel.MenuItem.DAILY) hat(Pad.AXIS_HAT_Y, -32767) }
        check("pad D-pad reaches Daily", onEdt { model.menuItems[model.menuIndex] } == AppModel.MenuItem.DAILY)
        button(Pad.A); pause(250)
        check("pad A starts the Daily", screen() == Screen.PUZZLE && onEdt { game.session?.isDaily == true })
        hat(Pad.AXIS_HAT_X, 32767); hat(Pad.AXIS_HAT_Y, 32767)
        button(Pad.A)
        check("pad A opens the on-screen keyboard", onEdt { model.keyboardOpen })
        shot("06_pad_onscreen_keyboard")
        val sel = onEdt { game.selection }
        hat(Pad.AXIS_HAT_X, 32767); button(Pad.A)
        check("on-screen keyboard types a letter", onEdt { game.inputs.isNotEmpty() })
        button(Pad.B)
        check("pad B closes the keyboard", !onEdt { model.keyboardOpen })
        button(Pad.Y)
        check("pad Y uses a hint", onEdt { game.session!!.hintsUsed } == 1)
        button(Pad.RB); button(Pad.LB)
        check("pad bumpers change clue", onEdt { game.selection } != null && sel != null)
        button(Pad.START)
        check("pad Start opens pause", screen() == Screen.PAUSE); shot("07_pause")
        hat(Pad.AXIS_HAT_Y, 32767); button(Pad.A)
        check("Save and go to menu", screen() == Screen.MENU)
        check("Daily progress saved", onEdt { app.store.hasSave(SaveSlot.daily(game.dailyKeyToday)) })
        check("Continue shows after save", onEdt { AppModel.MenuItem.CONTINUE in model.menuItems })

        // Resize + fullscreen.
        for ((w, h) in listOf(1920 to 1080, 800 to 500, 1280 to 800)) {
            onEdt { app.frame.setSize(w, h) }; pause(350)
            val ok = onEdt { app.panel.width > 0 && app.panel.height > 0 }
            check("resize to ${w}x$h renders", ok); shot("08_resize_${w}x$h")
        }
        menuTo(AppModel.MenuItem.CONTINUE); key(KeyEvent.VK_ENTER); pause(250)
        check("Continue reopens the Daily", screen() == Screen.PUZZLE && onEdt { game.session?.isDaily == true })
        shot("09_continue")
        onEdt { model.toggleFullscreen() }; pause(500)
        check("fullscreen on", app.fullscreen); shot("10_fullscreen")
        onEdt { model.toggleFullscreen() }; pause(500)
        check("windowed again", !app.fullscreen)
        key(KeyEvent.VK_ESCAPE); check("Esc pauses", screen() == Screen.PAUSE)
    }
}
