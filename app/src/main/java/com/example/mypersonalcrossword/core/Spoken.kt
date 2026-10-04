package com.hag.mypersonalcrossword.core

/**
 * [label] as a screen reader should say it: leading and trailing emoji, arrows
 * and other decoration removed ("🌐 Host Game" was read as "globe with
 * meridians, Host Game"; "Answer It ▶" as "…black right-pointing triangle").
 * Brackets and minus signs are kept, so "Pass (−1 pt)" stays intact.
 */
fun spokenLabel(label: String): String {
    fun keep(c: Char) = c.isLetterOrDigit() || c == '(' || c == ')' || c == '−' || c == '-'
    return label.trim { !keep(it) }.ifEmpty { label }
}
