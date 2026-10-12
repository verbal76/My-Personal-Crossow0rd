package com.hag.mypersonalcrossword.desktop

import org.junit.Assert.*
import org.junit.Test
import java.awt.event.KeyEvent
import java.io.File

class InputDevicesTest {
    private fun ev(v: Int, type: Int, n: Int) = JsEvent.parse(JsEvent.encode(v, type, n))
    private fun mapper(vararg e: JsEvent, now: Long = 0) = GamepadMapper().let { m -> e.flatMap { m.onEvent(it, now) } }

    @Test fun joydevEventsDecodeLittleEndianSigned() {
        val e = JsEvent.parse(byteArrayOf(0x10, 0x27, 0, 0, 0x01, 0x80.toByte(), 0x02, 0x03))
        assertEquals(10_000L, e.timeMs); assertEquals(-32767, e.value); assertTrue(e.isAxis); assertEquals(3, e.number)
        assertTrue(JsEvent.parse(JsEvent.encode(1, 0x81, 0)).isInit)
    }

    @Test fun xboxButtonsMapToActions() {
        assertEquals(listOf(Action.CONFIRM), mapper(ev(1, 1, Pad.A)))
        assertEquals(listOf(Action.CANCEL), mapper(ev(1, 1, Pad.B)))
        assertEquals(listOf(Action.FLIP), mapper(ev(1, 1, Pad.X)))
        assertEquals(listOf(Action.HINT), mapper(ev(1, 1, Pad.Y)))
        assertEquals(listOf(Action.PREV_CLUE), mapper(ev(1, 1, Pad.LB)))
        assertEquals(listOf(Action.NEXT_CLUE), mapper(ev(1, 1, Pad.RB)))
        assertEquals(listOf(Action.MENU), mapper(ev(1, 1, Pad.START)))
        assertEquals(listOf(Action.TOGGLE_KEYBOARD), mapper(ev(1, 1, Pad.BACK)))
        assertTrue("release does nothing", mapper(ev(0, 1, Pad.A)).isEmpty())
    }

    @Test fun dpadAndSticksMapToDirections() {
        assertEquals(listOf(Action.UP), mapper(ev(-32767, 2, Pad.AXIS_HAT_Y)))
        assertEquals(listOf(Action.DOWN), mapper(ev(32767, 2, Pad.AXIS_HAT_Y)))
        assertEquals(listOf(Action.LEFT), mapper(ev(-32767, 2, Pad.AXIS_HAT_X)))
        assertEquals(listOf(Action.RIGHT), mapper(ev(32767, 2, Pad.AXIS_LX)))
        assertEquals(listOf(Action.UP), mapper(ev(-30000, 2, Pad.AXIS_LY)))
        assertEquals(listOf(Action.RIGHT), mapper(ev(30000, 2, Pad.AXIS_RX)))
    }

    @Test fun stickDeadzoneAndEdgeTriggering() {
        assertTrue(mapper(ev(8000, 2, Pad.AXIS_LX)).isEmpty())
        // wobbling past the threshold fires once per crossing, not once per event
        assertEquals(listOf(Action.RIGHT), mapper(ev(20000, 2, Pad.AXIS_LX), ev(25000, 2, Pad.AXIS_LX), ev(32000, 2, Pad.AXIS_LX)))
        assertEquals(2, mapper(ev(20000, 2, Pad.AXIS_LX), ev(0, 2, Pad.AXIS_LX), ev(20000, 2, Pad.AXIS_LX)).size)
    }

    @Test fun initialStateReportTriggersNothing() {
        assertTrue(mapper(ev(1, 0x81, Pad.A), ev(32767, 0x82, Pad.AXIS_HAT_X)).isEmpty())
    }

    @Test fun heldDirectionRepeatsAfterDelayThenSteadily() {
        val m = GamepadMapper(350, 90)
        m.onEvent(ev(32767, 2, Pad.AXIS_HAT_X), 1000)
        assertTrue(m.poll(1200).isEmpty())
        assertEquals(listOf(Action.RIGHT), m.poll(1350))
        assertTrue(m.poll(1400).isEmpty()); assertEquals(listOf(Action.RIGHT), m.poll(1440))
        m.onEvent(ev(0, 2, Pad.AXIS_HAT_X), 1500)
        assertTrue("released: no more repeats", m.poll(5000).isEmpty())
    }

    @Test fun readerDecodesAStreamOfEventsFromADeviceFile() {
        val f = File.createTempFile("jsdev", ".bin"); f.deleteOnExit()
        f.writeBytes(JsEvent.encode(1, 1, Pad.A) + JsEvent.encode(0, 1, Pad.A) + JsEvent.encode(-32767, 2, Pad.AXIS_HAT_Y))
        val got = ArrayList<JsEvent>()
        var dropped = 0
        val r = JoystickReader({ got += it }, { dropped++ }, { emptyList() })
        val rf = JoystickReader::class.java.getDeclaredField("running").also { it.isAccessible = true }; rf.setBoolean(r, true)
        r.read(f)
        assertEquals(3, got.size); assertEquals(Pad.AXIS_HAT_Y, got[2].number)
    }

    @Test fun unplugWhileHoldingStopsRepeatAndForgetsAxes() {
        val m = GamepadMapper(350, 90)
        m.onEvent(ev(32767, 2, Pad.AXIS_HAT_X), 0)
        assertEquals(listOf(Action.RIGHT), m.poll(400))
        m.reset()
        assertTrue("no phantom repeat after the pad disappears", m.poll(10_000).isEmpty())
        assertEquals("a fresh press works again", listOf(Action.RIGHT), m.onEvent(ev(32767, 2, Pad.AXIS_HAT_X), 20_000))
    }

    @Test fun keyboardMapping() {
        assertEquals(Action.NEXT_CLUE, KeyMap.action(KeyEvent.VK_TAB)); assertEquals(Action.PREV_CLUE, KeyMap.action(KeyEvent.VK_TAB, shift = true))
        assertEquals(Action.FLIP, KeyMap.action(KeyEvent.VK_SPACE)); assertEquals(Action.CONFIRM, KeyMap.action(KeyEvent.VK_ENTER))
        assertEquals(Action.FULLSCREEN, KeyMap.action(KeyEvent.VK_ENTER, alt = true)); assertEquals(Action.FULLSCREEN, KeyMap.action(KeyEvent.VK_F11))
        assertEquals(Action.BACKSPACE, KeyMap.action(KeyEvent.VK_BACK_SPACE)); assertEquals(Action.CANCEL, KeyMap.action(KeyEvent.VK_ESCAPE))
        assertEquals(Action.HINT, KeyMap.action(KeyEvent.VK_F1)); assertNull(KeyMap.action(KeyEvent.VK_A))
    }

    @Test fun onlyAToZAreLetters() {
        assertEquals('Q', KeyMap.letter('q')); assertEquals('Q', KeyMap.letter('Q'))
        assertNull(KeyMap.letter('1')); assertNull(KeyMap.letter('é')); assertNull(KeyMap.letter(' ')); assertNull(KeyMap.letter('\n'))
    }
}
