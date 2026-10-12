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

    /** Plain-language messages about anything recovered or set aside (shown once on the menu). */
    var notice: String = ""; private set

    init {
        savesDir.mkdirs()
        if (profileFile.exists()) {
            val ok = runCatching { profileFile.inputStream().use { props.load(it) } }.isSuccess
            if (!ok) { props.clear(); quarantine(profileFile)?.let { addNotice("Your profile file could not be read; a copy was kept in ${it.parentFile.name}.") } }
        }
    }

    fun clearNotice() { notice = "" }
    private fun addNotice(msg: String) { notice = if (notice.isEmpty()) msg else "$notice  $msg" }

    /** Moves an unreadable file to `<dir>/corrupt/<name>.<time>` instead of deleting it. Returns the new file. */
    private fun quarantine(f: File): File? = runCatching {
        val dest = File(File(dir, "corrupt").also { it.mkdirs() }, "${f.name}.${System.currentTimeMillis()}")
        Files.move(f.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING); dest
    }.getOrNull()

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

    private fun bakFile(slotId: String) = File(savesDir, slotFile(slotId).name + ".bak")

    private fun decodeFile(f: File): PuzzleSession? =
        runCatching { SessionCodec.decode(f.readText(Charsets.UTF_8)) }.getOrNull()?.takeIf { it.words.isNotEmpty() }

    /** Writes the save atomically. The previous GOOD save is kept as `.bak` so one bad write can never cost progress. */
    fun saveSession(s: PuzzleSession) {
        val f = slotFile(s.slot.id)
        if (f.isFile && decodeFile(f) != null) runCatching { Files.copy(f.toPath(), bakFile(s.slot.id).toPath(), StandardCopyOption.REPLACE_EXISTING) }
        atomicWrite(f) { it.write(SessionCodec.encode(s).toByteArray(Charsets.UTF_8)) }
    }

    /**
     * Loads a save. A file that cannot be decoded is NEVER silently dropped: it is moved to `corrupt/`,
     * the `.bak` of the previous good save is tried next, and the player gets a notice either way.
     */
    fun loadSession(slotId: String): PuzzleSession? = loadFile(slotFile(slotId))

    private fun loadFile(f: File): PuzzleSession? {
        if (!f.isFile) return null
        decodeFile(f)?.let { return it }
        val kept = quarantine(f)
        val bak = File(f.path + ".bak")
        decodeFile(bak)?.let { good ->
            runCatching { Files.copy(bak.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING) }
            addNotice("A saved puzzle was damaged; the previous save was restored.")
            return good
        }
        addNotice("A saved puzzle could not be read" + (kept?.let { "; a copy was kept in ${it.parentFile.name}." } ?: "."))
        return null
    }

    fun clearSession(slotId: String) { slotFile(slotId).delete(); bakFile(slotId).delete() }

    /** Every readable save, newest first. Unreadable ones are quarantined (with a notice), not ignored or deleted. */
    fun savedSessions(): List<PuzzleSession> =
        (savesDir.listFiles { f -> f.name.endsWith(".save") } ?: emptyArray())
            .sortedByDescending { it.lastModified() }
            .mapNotNull { f -> loadFile(f) }

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
