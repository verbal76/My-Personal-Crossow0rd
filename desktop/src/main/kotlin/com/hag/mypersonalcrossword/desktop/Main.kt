package com.hag.mypersonalcrossword.desktop

import java.awt.GraphicsEnvironment
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

/** Command line: --windowed | --fullscreen | --size=WxH | --data-dir=PATH | --smoke=PHASE [--shots=DIR] */
data class Options(
    val fullscreen: Boolean?, val width: Int, val height: Int, val dataDir: File,
    val smoke: String?, val shots: File?, val skipSplash: Boolean
) {
    companion object {
        fun parse(args: Array<String>): Options {
            fun arg(name: String) = args.firstOrNull { it.startsWith("--$name=") }?.substringAfter('=')
            val size = arg("size")?.split('x')?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 2 }
            return Options(
                fullscreen = when { "--fullscreen" in args -> true; "--windowed" in args -> false; else -> null },
                width = size?.get(0) ?: 1280, height = size?.get(1) ?: 800,
                dataDir = arg("data-dir")?.let(::File) ?: Platform.dataDir(),
                smoke = arg("smoke"), shots = arg("shots")?.let(::File), skipSplash = "--skip-splash" in args)
        }
    }
}

/** The window. Holds the model so the smoke harness (and tests) can drive the real thing. */
class DesktopApp(val options: Options, private val onExit: (Int) -> Unit = { exitProcess(it) }) : Host {
    val store = ProfileStore(options.dataDir)
    val game = GameController(store, Assets.wordList())
    val sound = DesktopSound()
    init { game.onSound = { effect, streak -> if (store.sound) sound.play(effect, streak) } }
    val model = AppModel(store, game, this)
    val panel = GamePanel(model)
    val frame = JFrame("My Personal Crossword")
    private val mapper = GamepadMapper()
    private val reader = JoystickReader(
        { e ->
            val now = System.currentTimeMillis()
            SwingUtilities.invokeLater { mapper.onEvent(e, now).forEach { model.act(it, fromPad = true) }; panel.repaint() }
        },
        { SwingUtilities.invokeLater { mapper.reset() } })
    @Volatile private var closing = false
    var fullscreen = false; private set
    override val isFullscreen: Boolean get() = fullscreen

    init {
        Assets.icon?.let { frame.iconImage = it }
        frame.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
        frame.addWindowListener(object : WindowAdapter() { override fun windowClosing(e: WindowEvent) = quit()
            // Focus loss (Alt+Tab, Steam overlay, suspend) is a good moment to save.
            override fun windowDeactivated(e: WindowEvent) { runCatching { game.save() } }
        })
        panel.preferredSize = Dimension(options.width, options.height)
        panel.onPadPoll = { now -> mapper.poll(now).also { acts -> acts.forEach { model.act(it, fromPad = true) } }.isNotEmpty() }
        frame.contentPane.add(panel)
        frame.pack()
        frame.minimumSize = Dimension(640, 400)
        frame.setLocationRelativeTo(null)
        if (options.skipSplash) model.act(Action.CONFIRM).also { model.act(Action.CONFIRM) }
    }

    fun show() {
        val wantFull = options.fullscreen ?: store.fullscreen
        frame.isVisible = true
        if (wantFull) applyFullscreen(true)
        reader.start()
        panel.requestFocusInWindow()
    }

    /** Called by the model; the model already persisted the new preference. */
    override fun toggleFullscreen() = applyFullscreen(!fullscreen)

    fun applyFullscreen(on: Boolean) {
        if (on == fullscreen) return
        fullscreen = on
        val device = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
        // A frame's decoration can only change while it is not displayable.
        frame.dispose()
        frame.isUndecorated = on
        if (on) {
            val b = device.defaultConfiguration.bounds
            frame.setBounds(b.x, b.y, b.width, b.height)
            frame.isVisible = true
            if (device.isFullScreenSupported && System.getProperty("mpc.exclusive") == "true") device.fullScreenWindow = frame
        } else {
            if (device.fullScreenWindow === frame) device.fullScreenWindow = null
            frame.setSize(options.width, options.height)
            frame.setLocationRelativeTo(null)
            frame.isVisible = true
        }
        panel.requestFocusInWindow()
    }

    /** Clean shutdown: save the puzzle, stop the pad reader, close the window, exit. */
    override fun quit() {
        if (closing) return
        closing = true
        runCatching { game.save() }
        reader.stop()
        frame.dispose()
        onExit(0)
    }
}

fun main(args: Array<String>) {
    val opts = Options.parse(args)
    // A packaged Windows GUI launcher has no console: keep a crash log next to the saves.
    Thread.setDefaultUncaughtExceptionHandler { _, t ->
        runCatching { opts.dataDir.mkdirs(); File(opts.dataDir, "crash.log").appendText("${java.time.Instant.now()} $t\n${t.stackTraceToString()}\n") }
    }
    System.setProperty("awt.useSystemAAFontSettings", "on")
    System.setProperty("swing.aatext", "true")
    SwingUtilities.invokeLater {
        val app = DesktopApp(opts)
        app.show()
        if (opts.smoke != null) Thread({ Smoke(app, opts).run() }, "smoke").apply { isDaemon = false }.start()
    }
}
