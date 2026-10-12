package com.hag.mypersonalcrossword.desktop

import com.hag.mypersonalcrossword.core.PuzzleSession
import com.hag.mypersonalcrossword.core.SaveSlot
import com.hag.mypersonalcrossword.core.SessionCodec
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties

/**
 * File-backed profile for the desktop build: lifetime score, Daily completions, the
 * payout ledger, a few settings, and one `PuzzleSession` per save slot (encoded by the
 * SAME shared `SessionCodec` Android uses, so a save is the identical text format).
 * Every write goes to a temp file first and is moved into place, so a crash or power
 * loss mid-write can't leave a half-written save.
 */
class ProfileStore(val dir: File) {
    private val profileFile = File(dir, "profile.properties")
    private val savesDir    = File(dir, "saves")
    private val props       = Properties()

    init {
        savesDir.mkdirs()
        if (profileFile.exists()) runCatching { profileFile.inputStream().use { props.load(it) } }
    }

    var score: Int
        get() = props.getProperty("score")?.toIntOrNull() ?: 0
        set(v) { props.setProperty("score", v.toString()); flush() }

    var ledger: String
        get() = props.getProperty("paid").orEmpty()
        set(v) { props.setProperty("paid", v); flush() }

    var dailyDone: Set<String>
        get() = props.getProperty("dailydone").orEmpty().split(',').filter { it.isNotBlank() }.toSet()
        set(v) { props.setProperty("dailydone", v.sorted().joinToString(",")); flush() }

    var fullscreen: Boolean
        get() = props.getProperty("fullscreen") == "true"
        set(v) { props.setProperty("fullscreen", v.toString()); flush() }

    var background: String
        get() = props.getProperty("background").orEmpty()
        set(v) { props.setProperty("background", v); flush() }

    /** Prompt glyph style: "auto" (follow the last device used), "controller" or "keyboard". */
    var prompts: String
        get() = props.getProperty("prompts") ?: "auto"
        set(v) { props.setProperty("prompts", v); flush() }

    var category: String
        get() = props.getProperty("category") ?: "MIXED"
        set(v) { props.setProperty("category", v); flush() }

    var difficulty: String
        get() = props.getProperty("difficulty") ?: "EASY"
        set(v) { props.setProperty("difficulty", v); flush() }

    private fun flush() = atomicWrite(profileFile) { out -> props.store(out, "My Personal Crossword (desktop) profile") }

    private fun slotFile(slotId: String) =
        File(savesDir, slotId.replace(Regex("[^A-Za-z0-9+_.-]"), "~") + ".save")

    fun saveSession(s: PuzzleSession) {
        atomicWrite(slotFile(s.slot.id)) { it.write(SessionCodec.encode(s).toByteArray(Charsets.UTF_8)) }
    }

    fun loadSession(slotId: String): PuzzleSession? =
        slotFile(slotId).takeIf { it.isFile }?.let { f -> runCatching { SessionCodec.decode(f.readText(Charsets.UTF_8)) }.getOrNull() }

    fun clearSession(slotId: String) { slotFile(slotId).delete() }

    /** Every readable save, newest first. Unreadable files are ignored, never thrown on. */
    fun savedSessions(): List<PuzzleSession> =
        (savesDir.listFiles { f -> f.name.endsWith(".save") } ?: emptyArray())
            .sortedByDescending { it.lastModified() }
            .mapNotNull { f -> runCatching { SessionCodec.decode(f.readText(Charsets.UTF_8)) }.getOrNull() }
            .filter { it.words.isNotEmpty() }

    fun hasSave(slot: SaveSlot) = loadSession(slot.id) != null

    private fun atomicWrite(target: File, body: (java.io.OutputStream) -> Unit) {
        runCatching {
            target.parentFile?.mkdirs()
            val tmp = File(target.parentFile, target.name + ".tmp")
            tmp.outputStream().use { body(it) }
            try { Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) }
            catch (_: Exception) { Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING) }
        }
    }
}
