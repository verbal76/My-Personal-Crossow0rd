package com.hag.mypersonalcrossword.desktop

import java.io.File
import java.io.FileInputStream

/**
 * Gamepad support without native libraries.
 *
 * On Linux (the Steam Deck) Steam Input presents a virtual Xbox-style pad; the kernel exposes
 * it through the joystick interface, /dev/input/js*, as 8-byte events. [JsEvent] decodes
 * them, [GamepadMapper] turns them into [Action]s (with held-direction auto-repeat), and
 * [JoystickReader] feeds them in from a device file. The decoder and mapper are pure and
 * are unit-tested with simulated events; the reader itself is only verified against a
 * regular file in tests, not against real hardware.
 */
data class JsEvent(val timeMs: Long, val value: Int, val type: Int, val number: Int) {
    val isInit: Boolean get() = (type and 0x80) != 0
    val isButton: Boolean get() = (type and 0x7F) == 1
    val isAxis: Boolean get() = (type and 0x7F) == 2

    companion object {
        const val SIZE = 8
        fun parse(b: ByteArray, off: Int = 0): JsEvent {
            fun u8(i: Int) = b[off + i].toInt() and 0xFF
            val time = u8(0).toLong() or (u8(1).toLong() shl 8) or (u8(2).toLong() shl 16) or (u8(3).toLong() shl 24)
            val value = ((u8(5) shl 8) or u8(4)).toShort().toInt()      // signed 16-bit little endian
            return JsEvent(time, value, u8(6), u8(7))
        }
        fun encode(value: Int, type: Int, number: Int, timeMs: Long = 0): ByteArray = byteArrayOf(
            (timeMs and 0xFF).toByte(), ((timeMs shr 8) and 0xFF).toByte(), ((timeMs shr 16) and 0xFF).toByte(), ((timeMs shr 24) and 0xFF).toByte(),
            (value and 0xFF).toByte(), ((value shr 8) and 0xFF).toByte(), type.toByte(), number.toByte())
    }
}

/** Xbox-layout indices as presented by the Linux xpad driver / Steam Input virtual pad. */
object Pad {
    const val A = 0; const val B = 1; const val X = 2; const val Y = 3
    const val LB = 4; const val RB = 5; const val BACK = 6; const val START = 7
    const val AXIS_LX = 0; const val AXIS_LY = 1; const val AXIS_RX = 3; const val AXIS_RY = 4
    const val AXIS_HAT_X = 6; const val AXIS_HAT_Y = 7
    const val THRESHOLD = 16_000          // of 32767: stick must move about half way
}

class GamepadMapper(private val repeatDelayMs: Long = 350, private val repeatMs: Long = 90) {
    private val axes = HashMap<Int, Int>()          // axis -> -1, 0, +1 (digital state)
    private var held: Action? = null
    private var nextRepeatAt = 0L

    /** Handles one event; returns the actions it triggers right now. */
    fun onEvent(e: JsEvent, nowMs: Long): List<Action> {
        if (e.isInit) { // initial state report: record it, trigger nothing
            if (e.isAxis) axes[e.number] = digital(e.value)
            return emptyList()
        }
        if (e.isButton) {
            if (e.value == 0) return emptyList()
            return listOfNotNull(when (e.number) {
                Pad.A -> Action.CONFIRM; Pad.B -> Action.CANCEL
                Pad.X -> Action.FLIP;    Pad.Y -> Action.HINT
                Pad.LB -> Action.PREV_CLUE; Pad.RB -> Action.NEXT_CLUE
                Pad.BACK -> Action.TOGGLE_KEYBOARD; Pad.START -> Action.MENU
                else -> null
            })
        }
        if (e.isAxis && e.number in listOf(Pad.AXIS_LX, Pad.AXIS_LY, Pad.AXIS_RX, Pad.AXIS_RY, Pad.AXIS_HAT_X, Pad.AXIS_HAT_Y)) {
            val d = digital(e.value)
            val old = axes.put(e.number, d) ?: 0
            if (d == old) return emptyList()
            val vertical = e.number == Pad.AXIS_LY || e.number == Pad.AXIS_RY || e.number == Pad.AXIS_HAT_Y
            val act = when {
                d == 0 -> null
                vertical -> if (d < 0) Action.UP else Action.DOWN
                else -> if (d < 0) Action.LEFT else Action.RIGHT
            }
            held = act ?: heldFromAxes()
            nextRepeatAt = nowMs + repeatDelayMs
            return listOfNotNull(act)
        }
        return emptyList()
    }

    /** Forget held directions and axis state (device unplugged / reader restarted). */
    fun reset() { axes.clear(); held = null }

    /** Call regularly (e.g. every 16 ms) to produce auto-repeat while a direction is held. */
    fun poll(nowMs: Long): List<Action> {
        val h = held ?: return emptyList()
        if (nowMs < nextRepeatAt) return emptyList()
        nextRepeatAt = nowMs + repeatMs
        return listOf(h)
    }

    private fun digital(v: Int) = when { v > Pad.THRESHOLD -> 1; v < -Pad.THRESHOLD -> -1; else -> 0 }

    private fun heldFromAxes(): Action? {
        for ((axis, d) in axes) if (d != 0) {
            val vertical = axis == Pad.AXIS_LY || axis == Pad.AXIS_RY || axis == Pad.AXIS_HAT_Y
            return if (vertical) (if (d < 0) Action.UP else Action.DOWN) else (if (d < 0) Action.LEFT else Action.RIGHT)
        }
        return null
    }
}

/** Reads joystick events from every /dev/input/js* that exists (re-scanned, so hot-plugging works). */
class JoystickReader(private val onEvent: (JsEvent) -> Unit,
                     private val onDisconnect: () -> Unit = {},
                     private val devices: () -> List<File> = { defaultDevices() }) {
    @Volatile private var running = false
    private var thread: Thread? = null

    fun start() {
        if (running) return
        running = true
        thread = Thread({
            while (running) {
                val dev = devices().firstOrNull { it.canRead() }
                if (dev == null) { sleep(2000); continue }
                runCatching { read(dev) }
                onDisconnect()          // a held direction must not keep repeating after an unplug
                sleep(1000)
            }
        }, "gamepad-reader").apply { isDaemon = true; start() }
    }

    fun stop() { running = false; thread?.interrupt() }

    /** Reads whole 8-byte events until EOF or stop. Public so tests can drive it from a file. */
    fun read(dev: File) {
        FileInputStream(dev).use { input ->
            val buf = ByteArray(JsEvent.SIZE)
            while (running) {
                var got = 0
                while (got < JsEvent.SIZE) {
                    val n = input.read(buf, got, JsEvent.SIZE - got)
                    if (n < 0) return
                    got += n
                }
                onEvent(JsEvent.parse(buf))
            }
        }
    }

    private fun sleep(ms: Long) = try { Thread.sleep(ms) } catch (_: InterruptedException) {}

    companion object {
        fun defaultDevices(): List<File> = (0..7).map { File("/dev/input/js$it") }.filter { it.exists() }
    }
}
