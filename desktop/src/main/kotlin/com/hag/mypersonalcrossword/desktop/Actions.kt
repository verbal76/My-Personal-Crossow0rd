package com.hag.mypersonalcrossword.desktop

import java.awt.event.KeyEvent

/** Every input device (keyboard, mouse shortcuts, gamepad, Steam Input) reduces to these. */
enum class Action {
    UP, DOWN, LEFT, RIGHT,
    CONFIRM, CANCEL,
    NEXT_CLUE, PREV_CLUE, FLIP, HINT,
    BACKSPACE, MENU, TOGGLE_KEYBOARD, FULLSCREEN
}

object KeyMap {
    /** Navigation / command keys. Letters are NOT here: they arrive as typed characters. */
    fun action(keyCode: Int, alt: Boolean = false, shift: Boolean = false): Action? = when (keyCode) {
        KeyEvent.VK_UP    -> Action.UP
        KeyEvent.VK_DOWN  -> Action.DOWN
        KeyEvent.VK_LEFT  -> Action.LEFT
        KeyEvent.VK_RIGHT -> Action.RIGHT
        KeyEvent.VK_ENTER -> if (alt) Action.FULLSCREEN else Action.CONFIRM
        KeyEvent.VK_ESCAPE -> Action.CANCEL
        KeyEvent.VK_TAB   -> if (shift) Action.PREV_CLUE else Action.NEXT_CLUE
        KeyEvent.VK_SPACE -> Action.FLIP
        KeyEvent.VK_BACK_SPACE, KeyEvent.VK_DELETE -> Action.BACKSPACE
        KeyEvent.VK_F1    -> Action.HINT
        KeyEvent.VK_F10   -> Action.MENU
        KeyEvent.VK_F11   -> Action.FULLSCREEN
        else -> null
    }

    /** A–Z entered by the player, layout-independent (uses the typed character). */
    fun letter(ch: Char): Char? = ch.takeIf { it in 'a'..'z' || it in 'A'..'Z' }?.uppercaseChar()
}
