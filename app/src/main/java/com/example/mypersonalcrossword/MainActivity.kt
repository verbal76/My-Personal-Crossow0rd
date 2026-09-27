package com.hag.mypersonalcrossword

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.awaitCancellation
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context.VIBRATOR_SERVICE
import android.content.pm.ActivityInfo
import java.util.Calendar
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.ui.graphics.drawscope.rotate as canvasRotate
import androidx.compose.runtime.withFrameMillis
import android.content.res.Configuration
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalConfiguration
import kotlin.random.Random
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.atan2
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeoSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import android.media.AudioTrack
import android.media.AudioFormat
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.annotation.SuppressLint
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.Transaction
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.hag.mypersonalcrossword.ui.theme.MyPersonalCrosswordTheme
import com.hag.mypersonalcrossword.core.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlin.math.abs as kabs
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyPersonalCrosswordTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    CrosswordApp()
                }
            }
        }
    }
}

enum class AppMode { LOGIN, STATS, CATEGORY_SELECT, DASHBOARD, ONLINE_LOBBY }

/** Everything the results card needs about the puzzle that was just completed. */
data class PuzzleResult(
    val mode:             GameMode,
    val isDaily:          Boolean,
    val reward:           Int,          // completion reward before hints
    val hints:            Int,
    val net:              Int,          // reward − hints, floored at 0
    val awarded:          Int?,         // points actually added now (null for Vindictive)
    val dailyAlreadyPaid: Boolean,
    val seconds:          Long,
    val team:             TeamState,
    val vind:             VindState,
    val myIndex:          Int
)

private val CATEGORY_DISPLAY = mapOf("VIDEOGAMES" to "Video Games")

/** "VIDEOGAMES" → "Video Games", "FOOD+CITIES" → "Food + Cities". Labels like "3 Categories" pass through. */
fun prettyCategory(raw: String): String =
    if (raw.any { it.isDigit() }) raw
    else raw.split("+").joinToString(" + ") { part ->
        CATEGORY_DISPLAY[part] ?: part.lowercase().replaceFirstChar { it.uppercase() }
    }

/** Player-facing name of a resumable save. */
fun slotTitle(slot: SaveSlot): String = when (slot.mode) {
    GameMode.DAILY      -> "Daily Puzzle (${slot.category})"
    GameMode.TEAM       -> "🤝 Team · ${prettyCategory(slot.category)} · ${slot.difficulty.label}"
    GameMode.VINDICTIVE -> "⚔️ Vindictive · ${prettyCategory(slot.category)} · ${slot.difficulty.label}"
    GameMode.SINGLE     -> "${prettyCategory(slot.category)} · ${slot.difficulty.label}"
}

/** Reads and validates assets/test.csv. Rejected rows are logged, never turned into grid cells. */
fun loadWordData(context: Context): WordData = try {
    context.assets.open("test.csv").bufferedReader().useLines { parseWordCsv(it) }.also { data ->
        data.rejected.forEach { r ->
            android.util.Log.w("CrosswordData", "test.csv line ${r.lineNumber} rejected (${r.reason}): ${r.line}")
        }
    }
} catch (e: Exception) {
    android.util.Log.e("CrosswordData", "Couldn't read test.csv", e)
    WordData(emptyList(), emptyList())
}

fun vibrateLight(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(40, 80))
        } else {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                v?.vibrate(VibrationEffect.createOneShot(40, 80))
            else @Suppress("DEPRECATION") v?.vibrate(40)
        }
    } catch (_: Exception) {}
}

fun vibrate(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v?.vibrate(300)
            }
        }
    } catch (_: Exception) {}
}

fun vibrateCorrect(context: Context) {
    try {
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        else @Suppress("DEPRECATION") context.getSystemService(VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 55, 70, 65), intArrayOf(0, 180, 0, 230), -1))
        else @Suppress("DEPRECATION") v?.vibrate(120)
    } catch (_: Exception) {}
}

fun vibrateWrong(context: Context) {
    try {
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        else @Suppress("DEPRECATION") context.getSystemService(VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            v?.vibrate(VibrationEffect.createOneShot(230, 255))
        else @Suppress("DEPRECATION") v?.vibrate(230)
    } catch (_: Exception) {}
}

class SaveManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("CrosswordSaves", Context.MODE_PRIVATE)

    fun getLastUser(): String = prefs.getString("last_user", "") ?: ""
    fun setLastUser(name: String) = prefs.edit { putString("last_user", name) }

    fun getScore(name: String): Int = prefs.getInt("score_$name", 0)
    fun addScore(name: String, delta: Int = 1) =
        prefs.edit { putInt("score_$name", getScore(name) + delta) }

    fun getCompleted(name: String): Int = prefs.getInt("completed_$name", 0)
    fun addCompleted(name: String, delta: Int = 1) =
        prefs.edit { putInt("completed_$name", getCompleted(name) + delta) }

    fun getUsedWords(name: String, category: String): Set<String> {
        return prefs.getStringSet("used_${name}_${category}", emptySet()) ?: emptySet()
    }
    fun addUsedWords(name: String, category: String, words: List<String>) {
        if (words.isEmpty()) return
        val current = getUsedWords(name, category).toMutableSet()
        current.addAll(words)
        prefs.edit { putStringSet("used_${name}_${category}", current) }
    }

    // ── PUZZLE SAVE / RESUME ──────────────────────────────────────────────────
    // One save per player + SaveSlot. The whole PuzzleSession (words, letters,
    // hints, timer, streak, Player 2, Team/Vindictive state, background) is
    // stored as one SessionCodec string, so resume restores everything and a
    // Team/Vindictive/Daily game can never land in the single-player slot.
    //
    // Legacy (pre-session) single-player saves used separate keys
    // puzzle_<name>_<cat>_<DIFF>_{words,inputs,bg,bgimg,time} + elapsed_…;
    // they are still read, and removed when the slot is cleared.

    private fun sessionKey(name: String, slot: SaveSlot) = "psession_${name}_${slot.id}"
    private fun sessionTimeKey(name: String, slot: SaveSlot) = "psession_time_${name}_${slot.id}"
    private fun legacyKey(name: String, cat: String, diff: Difficulty) = "puzzle_${name}_${cat}_${diff.name}"

    fun savePuzzle(name: String, session: PuzzleSession) {
        val slot  = session.slot
        val saved = getSavedSlotIds(name).toMutableSet().apply { add(slot.id) }
        prefs.edit {
            putString(sessionKey(name, slot), SessionCodec.encode(session))
            putLong(sessionTimeKey(name, slot), System.currentTimeMillis())
            putStringSet("saves_$name", saved)
        }
    }

    fun loadPuzzle(name: String, slot: SaveSlot): PuzzleSession? {
        prefs.getString(sessionKey(name, slot), null)?.let { raw ->
            return SessionCodec.decode(raw)
        }
        if (slot.mode != GameMode.SINGLE) return null
        // Legacy single-player save.
        val k        = legacyKey(name, slot.category, slot.difficulty)
        val wordsStr = prefs.getString("${k}_words", null) ?: return null
        val words    = SessionCodec.decodeWords(wordsStr)
        if (words.isEmpty()) return null
        return PuzzleSession(
            mode           = GameMode.SINGLE,
            category       = slot.category,
            difficulty     = slot.difficulty,
            words          = words,
            inputs         = SessionCodec.decodeInputs(prefs.getString("${k}_inputs", "") ?: ""),
            elapsedSeconds = prefs.getLong("elapsed_${name}_${slot.category}_${slot.difficulty.name}", 0L),
            bgArgb         = prefs.getInt("${k}_bg", android.graphics.Color.LTGRAY),
            bgImage        = prefs.getString("${k}_bgimg", BG_IMAGE_NONE) ?: BG_IMAGE_NONE
        )
    }

    /** Removes the save for [slot] (new and legacy formats) and its index entry, atomically. */
    fun clearPuzzle(name: String, slot: SaveSlot) {
        val saved = getSavedSlotIds(name).toMutableSet().apply { remove(slot.id) }
        val k = legacyKey(name, slot.category, slot.difficulty)
        prefs.edit {
            remove(sessionKey(name, slot)); remove(sessionTimeKey(name, slot))
            if (slot.mode == GameMode.SINGLE) {
                remove("${k}_words"); remove("${k}_inputs")
                remove("${k}_bg");    remove("${k}_bgimg"); remove("${k}_time")
                remove("elapsed_${name}_${slot.category}_${slot.difficulty.name}")
            }
            putStringSet("saves_$name", saved)
        }
    }

    /** Raw slot ids in the save index. */
    fun getSavedSlotIds(name: String): Set<String> =
        prefs.getStringSet("saves_$name", emptySet()) ?: emptySet()

    fun hasSave(name: String, slot: SaveSlot): Boolean = slot.id in getSavedSlotIds(name)

    /**
     * Resumable saves, most recently saved first. Legacy Daily saves (written
     * before Dailies were date-keyed) can't be tied to a date and are dropped.
     */
    fun getInProgressPuzzles(name: String): List<SaveSlot> {
        val slots = getSavedSlotIds(name).mapNotNull { SaveSlot.parse(it) }
        slots.filter { it.mode == GameMode.SINGLE && it.category == DailyPuzzle.CATEGORY }
            .forEach { clearPuzzle(name, it) }
        fun savedAt(slot: SaveSlot): Long =
            prefs.getLong(sessionTimeKey(name, slot), 0L).takeIf { it > 0 }
                ?: prefs.getLong("${legacyKey(name, slot.category, slot.difficulty)}_time", 0L)
        return slots
            .filterNot { it.mode == GameMode.SINGLE && it.category == DailyPuzzle.CATEGORY }
            .sortedByDescending { savedAt(it) }
    }

    // ── DAILY COMPLETION ──────────────────────────────────────────────────────
    // Set of UTC date keys on which this profile solved the Daily (last 60 kept).
    fun isDailyCompleted(name: String, dateKey: String): Boolean =
        (prefs.getStringSet("dailydone_$name", emptySet()) ?: emptySet()).contains(dateKey)

    fun markDailyCompleted(name: String, dateKey: String) {
        val done = (prefs.getStringSet("dailydone_$name", emptySet()) ?: emptySet())
            .plus(dateKey).sortedDescending().take(60).toSet()
        prefs.edit { putStringSet("dailydone_$name", done) }
    }

    // ── PLAYER PROFILES ───────────────────────────────────────────────────────
    fun getAllPlayerNames(): List<String> =
        (prefs.getStringSet("all_players", emptySet()) ?: emptySet()).sorted()

    fun ensurePlayer(name: String) {
        if (name.isBlank()) return
        val current = (prefs.getStringSet("all_players", emptySet()) ?: emptySet()).toMutableSet()
        if (current.add(name)) prefs.edit { putStringSet("all_players", current) }
    }

    fun deletePlayer(name: String) {
        if (name.isBlank()) return
        val current = (prefs.getStringSet("all_players", emptySet()) ?: emptySet()).toMutableSet()
        current.remove(name)
        // Sweep every per-player prefs key. Without this, recreating a deleted
        // profile inherited the previous score, completed count, in-progress
        // saves, used-word lists, and color choices.
        val allKeys = prefs.all.keys.toList()
        prefs.edit {
            putStringSet("all_players", current)
            for (key in allKeys) {
                val isPerPlayer = key == "score_$name" ||
                        key == "completed_$name" ||
                        key == "cellcolor_$name" ||
                        key == "btncolor_$name" ||
                        key == "recentcolors_$name" ||
                        key == "saves_$name" ||
                        key == "statkeys_$name" ||
                        key == "dailydone_$name" ||
                        key.startsWith("used_${name}_") ||
                        key.startsWith("puzzle_${name}_") ||
                        key.startsWith("psession_${name}_") ||
                        key.startsWith("psession_time_${name}_") ||
                        key.startsWith("stat_${name}_") ||
                        key.startsWith("elapsed_${name}_")
                if (isPerPlayer) remove(key)
            }
            if (prefs.getString("last_user", "") == name) putString("last_user", "")
        }
    }

    // ── STAT RECORDS ──────────────────────────────────────────────────────────
    // One best record per player + mode + category + difficulty.
    // Solo records keep the legacy key/index form (`cat__DIFF`); Team and
    // Vindictive are namespaced (`TEAM::cat__DIFF`) so they can't overwrite a
    // solo personal best.
    // Serialised: category§diffName§gameMode§hints§time§score§partner§partnerScore§won
    private fun statIndexId(record: StatRecord): String =
        if (record.gameMode == GameMode.SINGLE.name || record.gameMode == GameMode.DAILY.name)
            "${record.category}__${record.diffName}"
        else "${record.gameMode}::${record.category}__${record.diffName}"

    private fun statKey(player: String, indexId: String): String =
        if ("::" in indexId) "stat_${player}_${indexId}"
        else "stat_${player}_${indexId.substringBeforeLast("__")}_${indexId.substringAfterLast("__")}"

    fun saveStat(player: String, record: StatRecord) {
        val id = statIndexId(record)
        val k  = statKey(player, id)
        // Keep the better record: higher score wins; ties broken by fewer hints, then faster time
        val existing = prefs.getString(k, null)?.let { parseStat(it) }
        if (existing != null) {
            val isImprovement = record.score > existing.score ||
                (record.score == existing.score && record.hintsUsed < existing.hintsUsed) ||
                (record.score == existing.score && record.hintsUsed == existing.hintsUsed &&
                        record.timeSeconds < existing.timeSeconds)
            if (!isImprovement) return
        }
        val line = "${record.category}§${record.diffName}§${record.gameMode}§${record.hintsUsed}" +
                "§${record.timeSeconds}§${record.score}§${record.partner}" +
                "§${record.partnerScore}§${record.won}"
        // One atomic edit so the record and its index entry can't be torn apart.
        val keys = (prefs.getStringSet("statkeys_$player", emptySet()) ?: emptySet()).toMutableSet()
        keys.add(id)
        prefs.edit {
            putString(k, line)
            putStringSet("statkeys_$player", keys)
        }
    }

    fun getAllStats(player: String): List<StatRecord> {
        val keys = prefs.getStringSet("statkeys_$player", emptySet()) ?: emptySet()
        return keys.mapNotNull { id ->
            // A malformed key (legacy data, corrupt prefs) is skipped, never fatal.
            runCatching {
                if (!id.contains("__")) return@runCatching null
                prefs.getString(statKey(player, id), null)?.let { parseStat(it) }
            }.getOrNull()
        }.sortedWith(compareBy({ it.category }, { it.gameMode }, { it.diffName }))
    }

    private fun parseStat(line: String): StatRecord? = try {
        val p = line.split("§")
        StatRecord(p[0], p[1], p[2], p[3].toInt(), p[4].toLong(), p[5].toInt(),
            p.getOrElse(6) { "" }, p.getOrElse(7) { "0" }.toInt(),
            p.getOrElse(8) { "true" }.toBoolean())
    } catch (_: Exception) { null }

    // ── CELL COLOUR PREFERENCES ───────────────────────────────────────────────
    // Cell color stored as ARGB Int per player.
    // Recent colors stored as comma-separated ARGB Int strings, newest first, max 10.

    fun getButtonColorArgb(name: String): Int =
        prefs.getInt("btncolor_$name", 0xFF6650A4.toInt())   // default Material purple

    fun setButtonColor(name: String, argb: Int) =
        prefs.edit { putInt("btncolor_$name", argb) }

    fun getMusicVolume(): Float = prefs.getFloat("music_volume", 0.35f)
    fun setMusicVolume(v: Float) = prefs.edit { putFloat("music_volume", v) }

    fun isFirstLaunch(): Boolean = prefs.getBoolean("first_launch", true)
    fun markLaunched() = prefs.edit { putBoolean("first_launch", false) }
    fun isFirstVindictive(): Boolean = prefs.getBoolean("first_vind", true)
    fun markVindictiveSeen() = prefs.edit { putBoolean("first_vind", false) }
    fun isFirstSingle(): Boolean = prefs.getBoolean("first_single", true)
    fun markSingleSeen() = prefs.edit { putBoolean("first_single", false) }
    fun isFirstTeam(): Boolean = prefs.getBoolean("first_team", true)
    fun markTeamSeen() = prefs.edit { putBoolean("first_team", false) }

    fun getCellColorArgb(name: String): Int =
        prefs.getInt("cellcolor_$name", android.graphics.Color.WHITE)

    fun setCellColor(name: String, color: Color) =
        prefs.edit { putInt("cellcolor_$name", color.toArgb()) }

    fun getRecentColorArgbs(name: String): List<Int> {
        val raw = prefs.getString("recentcolors_$name", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(",").filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }
    }

    fun addRecentColor(name: String, color: Color) {
        val argb    = color.toArgb()
        val current = getRecentColorArgbs(name).toMutableList()
        current.removeAll { it == argb }   // avoid duplicates
        current.add(0, argb)               // newest at front
        prefs.edit {
            putString("recentcolors_$name", current.take(10).joinToString(","))
        }
    }
}


// ── SOUND EFFECTS ─────────────────────────────────────────────────────────────
// Synthesised entirely with AudioTrack — no audio asset files needed.
// All sounds generated in a background thread so they never block the UI.
object SoundPlayer {
    // Build and play a tone sequence on a daemon thread
    private fun play(build: (FloatArray) -> Unit) {
        Thread {
            try {
                val buf   = FloatArray(44100 * 2)  // max 2 sec
                build(buf)
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                            .setSampleRate(44100)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buf.size * 4)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(buf, 0, buf.size, AudioTrack.WRITE_BLOCKING)
                track.play()
                Thread.sleep((buf.size * 1000L / 44100))
                track.stop(); track.release()
            } catch (_: Exception) {}
        }.also { it.isDaemon = true }.start()
    }

    private fun sine(buf: FloatArray, startSample: Int, endSample: Int,
                     freq: Double, amp: Float) {
        for (i in startSample until endSample.coerceAtMost(buf.size)) {
            val t = (i - startSample).toDouble() / 44100
            // Soft envelope: 5 ms fade-in, 20 ms fade-out.
            // All branches return Double so env is unambiguously Double —
            // mixing Float branches (0.005f) and a Double else (1.0) made
            // Kotlin infer env: Number, causing the Float * Number type error.
            val env: Double = when {
                i - startSample < 44100 * 0.005 -> (i - startSample).toDouble() / (44100 * 0.005)
                endSample - i   < 44100 * 0.02  -> (endSample - i).toDouble()   / (44100 * 0.02)
                else -> 1.0
            }
            // All math stays Double; only the final assignment converts to Float.
            buf[i] = (buf[i].toDouble() + amp.toDouble() * env * sin(2.0 * PI * freq * t))
                .toFloat().coerceIn(-1f, 1f)
        }
    }

    /** Very short tick — played on button presses */
    fun playClick() {
        Thread {
            try {
                val n = 1800
                val buf = FloatArray(n)
                sine(buf, 0, n, 1100.0, 0.14f)
                val track = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                    .setAudioFormat(AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(44100)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                    .setBufferSizeInBytes(n * 4)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(buf, 0, n, AudioTrack.WRITE_BLOCKING)
                track.play()
                Thread.sleep(n * 1000L / 44100)
                track.stop(); track.release()
            } catch (_: Exception) {}
        }.also { it.isDaemon = true }.start()
    }

    /** Short ascending chime — played when a word answer is fully correct */
    fun playCorrect() = play { buf ->
        // C5 → E5 → G5 quick arpeggio
        listOf(523.25 to 0, 659.25 to 5000, 783.99 to 10000).forEach { (freq, start) ->
            sine(buf, start, start + 12000, freq, 0.45f)
        }
    }

    /** Low buzz — played when an answer is wrong */
    fun playWrong() = play { buf ->
        // Two descending buzzy tones
        sine(buf, 0,     8000,  180.0, 0.55f)
        sine(buf, 0,     8000,  90.0,  0.3f)
        sine(buf, 6000, 14000,  140.0, 0.55f)
        sine(buf, 6000, 14000,  70.0,  0.3f)
    }

    /** Clapping burst — played on streak milestones */
    fun playClap() = play { buf ->
        // Five short noise bursts simulate a clap rhythm
        for (burst in 0..4) {
            val s = burst * 7500
            val e = s + 3500
            for (i in s until e.coerceAtMost(buf.size)) {
                val env = when {
                    i - s < 400      -> (i - s) / 400f
                    e - i < 800      -> (e - i) / 800f
                    else             -> 1f
                }
                buf[i] = (buf[i] + (Random.nextFloat() * 2f - 1f) * 0.55f * env)
                    .coerceIn(-1f, 1f)
            }
        }
    }

    /** Ascending fanfare — played when the full puzzle is solved */
    fun playCelebration() = play { buf ->
        // C5-E5-G5-C6 fanfare with overlapping harmonics
        val notes = listOf(523.25 to 0, 659.25 to 8000, 783.99 to 16000, 1046.50 to 24000)
        notes.forEach { (freq, start) ->
            sine(buf, start, start + 22000, freq,        0.4f)
            sine(buf, start, start + 22000, freq * 2.0,  0.15f)  // octave harmonic
        }
        // Trailing shimmer
        sine(buf, 36000, 60000, 1046.50, 0.25f)
        sine(buf, 40000, 60000, 1318.51, 0.2f)
        sine(buf, 44000, 60000, 1567.98, 0.15f)
    }
}


// ── AMBIENT MUSIC PLAYER ──────────────────────────────────────────────────────
// Streams MP3 files from assets/Music/ using MediaPlayer.
// Shuffles the playlist once on first start then cycles in that order forever.
// Advances automatically when a track ends; next() skips to the following track.
//
// @SuppressLint: we store applicationContext (process lifetime), not an Activity —
// this is safe. Lint can't distinguish the two, so the warning is a false positive.
@SuppressLint("StaticFieldLeak")
object AmbientMusicPlayer {
    private var player:   MediaPlayer? = null
    @Volatile private var vol          = 0.4f
    @Volatile private var enabled      = false
    private var context:  Context?     = null

    // Shuffled playlist — built once, rotated each time we advance
    private var playlist:     List<String> = emptyList()
    private var trackIndex:   Int          = 0

    // Track name and playing state exposed to UI as Compose state
    var currentTrackName by mutableStateOf("")
        private set
    var isPlaying by mutableStateOf(false)
        private set

    // Discover all MP3 files in assets/Music/, shuffle, store.
    private fun buildPlaylist(ctx: Context) {
        if (playlist.isNotEmpty()) return   // already built this session
        val files = try {
            ctx.assets.list("Music")
                ?.filter { it.endsWith(".mp3", ignoreCase = true) }
                ?.shuffled()
                ?: emptyList()
        } catch (_: Exception) { emptyList() }
        playlist   = files
        trackIndex = 0
    }

    fun init(ctx: Context) {
        context = ctx.applicationContext
        context?.let { buildPlaylist(it) }
    }

    // Play the track at trackIndex; when it completes advance to next.
    private fun playCurrentTrack() {
        val ctx = context ?: return
        if (playlist.isEmpty()) return
        val filename = playlist[trackIndex]
        currentTrackName = filename
            .substringBeforeLast(".")     // strip .mp3
            .replace(Regex("-\\d+"), "") // strip trailing licence numbers
            .replace("-", " ")
            .replaceFirstChar { it.uppercase() }

        try {
            player?.release()
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                ctx.assets.openFd("Music/$filename").use { afd ->
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
                setVolume(vol, vol)
                prepare()
                setOnCompletionListener { advance() }
                if (enabled) start()
            }
        } catch (_: Exception) { advance() }   // skip broken file
    }

    fun start(volume: Float) {
        vol     = volume
        enabled = true
        isPlaying = true
        context?.let { buildPlaylist(it) }
        if (player == null) playCurrentTrack()
        else player?.apply { setVolume(vol, vol); if (!isPlaying) start() }
    }

    fun stop() {
        enabled = false
        isPlaying = false
        try { player?.pause() } catch (_: Exception) {}
    }

    fun release() {
        enabled = false
        isPlaying = false
        try { player?.stop(); player?.release() } catch (_: Exception) {}
        player = null
    }

    fun setVolume(volume: Float) {
        vol = volume.coerceIn(0f, 1f)
        try { player?.setVolume(vol, vol) } catch (_: Exception) {}
    }

    fun next() {
        if (playlist.isEmpty()) return
        trackIndex = (trackIndex + 1) % playlist.size
        playCurrentTrack()
    }

    fun previous() {
        if (playlist.isEmpty()) return
        trackIndex = (trackIndex - 1 + playlist.size) % playlist.size
        playCurrentTrack()
    }

    private fun advance() {
        trackIndex = (trackIndex + 1) % playlist.size
        playCurrentTrack()
    }
}

// ── ONLINE MULTIPLAYER ─────────────────────────────────────────────────────────

object FirebaseGameManager {
    private const val DB_URL = "https://mypersonalcrossword-default-rtdb.firebaseio.com/"
    private val db: DatabaseReference by lazy {
        FirebaseDatabase.getInstance(DB_URL).reference
    }
    // No 0/O/1/I to avoid misreads. 32^6 ≈ 1.07 billion codes.
    private const val CODE_CHARS  = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    const val CODE_LENGTH         = 6

    const val STATUS_WAITING   = "waiting"
    const val STATUS_PLAYING   = "playing"
    const val STATUS_COMPLETE  = "complete"
    const val STATUS_ABANDONED = "abandoned"

    fun generateCode(): String = (1..CODE_LENGTH).map { CODE_CHARS.random() }.joinToString("")

    private fun gameRef(code: String) = db.child("games").child(code)

    val myUid: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    /**
     * Ensures an anonymous Firebase user exists. Reports failure (offline,
     * Anonymous auth disabled in the console, bad config) instead of pretending
     * it worked — the old version called onDone() on failure too, which surfaced
     * later as a confusing "Connection error".
     */
    fun signInAnonymously(onResult: (ok: Boolean, error: String?) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) { onResult(true, null); return }
        auth.signInAnonymously().addOnCompleteListener { task ->
            if (task.isSuccessful) onResult(true, null)
            else onResult(false, "Couldn't reach the game server. Check your connection and try again.")
        }
    }

    /**
     * Creates a game node only if [code] is unused (transaction), retrying with
     * a fresh code on collision, then registers onDisconnect so a host who
     * vanishes marks the game abandoned instead of stranding the guest.
     */
    fun createGame(
        hostName: String, mode: GameMode, difficulty: Difficulty,
        attempt: Int = 0,
        onResult: (code: String?, error: String?) -> Unit
    ) {
        val code = generateCode()
        val uid  = myUid
        gameRef(code).runTransaction(object : Transaction.Handler {
            override fun doTransaction(current: MutableData): Transaction.Result {
                if (current.value != null) return Transaction.abort()
                current.value = mapOf(
                    "mode" to mode.name, "category" to "", "difficulty" to difficulty.name,
                    "hostName" to hostName, "hostUid" to (uid ?: ""),
                    "guestName" to "", "guestUid" to "",
                    "status" to STATUS_WAITING, "createdAt" to ServerValue.TIMESTAMP,
                    "puzzle" to "", "inputs" to mapOf<String, String>(),
                    "turn" to 0, "hostScore" to 0, "guestScore" to 0,
                    "vindPhase" to VindicativePhase.PICK_OWN.name,
                    "vindAssignedWordIdx" to -1, "vindCurrentPlayer" to 0, "solved" to false
                )
                return Transaction.success(current)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, snap: DataSnapshot?) {
                when {
                    error != null      -> onResult(null, "Couldn't create the game (${error.message}).")
                    !committed && attempt < 4 -> createGame(hostName, mode, difficulty, attempt + 1, onResult)
                    !committed         -> onResult(null, "Couldn't create a game code. Try again.")
                    else -> {
                        gameRef(code).child("status").onDisconnect().setValue(STATUS_ABANDONED)
                        onResult(code, null)
                    }
                }
            }
        })
    }

    /**
     * Claims the guest seat atomically: the transaction only succeeds if the game
     * exists, is still waiting, and has no guest — so two phones entering the same
     * code can't both join.
     */
    fun joinGame(
        code: String, guestName: String,
        onSuccess: (mode: String, category: String, difficulty: String, hostName: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = myUid ?: ""
        var reason = "That game is no longer available."
        gameRef(code).runTransaction(object : Transaction.Handler {
            override fun doTransaction(current: MutableData): Transaction.Result {
                // First call may see an empty local cache; pass it through unchanged so the
                // server re-runs the handler with real data (or confirms the node is absent).
                if (current.value == null) return Transaction.success(current)
                val status = current.child("status").getValue(String::class.java) ?: ""
                if (status != STATUS_WAITING) { reason = "That game has already started or ended."; return Transaction.abort() }
                val existing = current.child("guestName").getValue(String::class.java) ?: ""
                if (existing.isNotEmpty()) { reason = "That game is already full."; return Transaction.abort() }
                current.child("guestName").value = guestName
                current.child("guestUid").value  = uid
                return Transaction.success(current)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, snap: DataSnapshot?) {
                when {
                    error != null -> onError("Connection error. Try again.")
                    !committed || snap == null -> onError(reason)
                    snap.child("guestUid").getValue(String::class.java) != uid ||
                        snap.child("guestName").getValue(String::class.java) != guestName ->
                        onError("Game not found. Check the code.")
                    else -> {
                        gameRef(code).child("status").onDisconnect().setValue(STATUS_ABANDONED)
                        onSuccess(
                            snap.child("mode").getValue(String::class.java) ?: "TEAM",
                            snap.child("category").getValue(String::class.java) ?: "",
                            snap.child("difficulty").getValue(String::class.java) ?: "MEDIUM",
                            snap.child("hostName").getValue(String::class.java) ?: ""
                        )
                    }
                }
            }
        })
    }

    fun writePuzzle(code: String, words: List<PlacedWord>) {
        gameRef(code).updateChildren(mapOf("puzzle" to SessionCodec.encodeWords(words), "status" to STATUS_PLAYING))
    }

    fun parsePuzzle(raw: String): List<PlacedWord> = SessionCodec.decodeWords(raw)

    fun writeInput(code: String, x: Int, y: Int, char: Char) {
        gameRef(code).child("inputs").child("${x}_${y}").setValue(char.toString())
    }

    fun writeState(code: String, updates: Map<String, Any>) {
        gameRef(code).updateChildren(updates)
    }

    fun listen(code: String, onUpdate: (DataSnapshot) -> Unit, onError: (String) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) { onUpdate(snap) }
            override fun onCancelled(e: DatabaseError) {
                android.util.Log.w("CrosswordFirebase", "Listener cancelled for game $code: ${e.message}", e.toException())
                onError(if (e.code == DatabaseError.PERMISSION_DENIED)
                    "The game server refused access to this game."
                else "Lost connection to the game.")
            }
        }
        gameRef(code).addValueEventListener(listener)
        return listener
    }

    fun stopListening(code: String, listener: ValueEventListener) {
        gameRef(code).removeEventListener(listener)
    }

    /** Normal end: cancel the abandon-on-disconnect hook, then mark the game complete. */
    fun closeGame(code: String) {
        gameRef(code).child("status").onDisconnect().cancel()
        gameRef(code).child("status").setValue(STATUS_COMPLETE)
    }

    /** A player left mid-game: tell the other device. */
    fun abandonGame(code: String) {
        gameRef(code).child("status").onDisconnect().cancel()
        gameRef(code).child("status").setValue(STATUS_ABANDONED)
    }
}

// ── REUSABLE GRADIENT BUTTON ───────────────────────────────────────────────────

// Top-edge bevel highlight — call inside a BoxScope to draw the thin lighter
// strip used on every gradient surface in the app.
@Composable
fun BoxScope.BevelHighlight(alpha: Float = 0.25f) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.5.dp)
            .background(Color.White.copy(alpha = alpha))
            .align(Alignment.TopCenter)
    )
}

// Animated trailing dots ("…") — used in loading states. Cycles 1→2→3 dots.
@Composable
fun rememberAnimatedDots(): String {
    var dots by remember { mutableStateOf(".") }
    LaunchedEffect(Unit) {
        while (true) {
            for (n in 1..3) { dots = ".".repeat(n); delay(380L) }
        }
    }
    return dots
}

// Reusable gradient button matching the category-button style.
// Pass gradient = redGradient for destructive actions.
@Composable
fun GradientBtn(
    text:     String,
    gradient: Brush,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier,
    enabled:  Boolean  = true
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .shadow(if (enabled) 4.dp else 0.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(gradient)
            .graphicsLayer(alpha = if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        BevelHighlight()
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrosswordApp() {
    val context  = LocalContext.current
    val saveManager = remember { SaveManager(context) }

    // Enums use listSaver — saves as List<String> which is natively bundleable.
    var appMode by rememberSaveable(
        stateSaver = listSaver(save = { listOf(it.name) }, restore = { AppMode.valueOf(it[0]) })
    ) { mutableStateOf(AppMode.LOGIN) }
    var playerName by rememberSaveable { mutableStateOf("") }

    var currentScore by rememberSaveable { mutableIntStateOf(0) }
    var currentCompleted by rememberSaveable { mutableIntStateOf(0) }

    var currentBgColor     by remember { mutableStateOf(Color.LightGray) }
    var currentBgImageName by rememberSaveable { mutableStateOf(BG_IMAGE_NONE) }
    var bgImagePool        by remember { mutableStateOf(emptyList<String>()) }
    var activeCategory     by remember { mutableStateOf("") }
    var combinedCategories by remember { mutableStateOf(emptyList<String>()) }

    var allEntries by remember { mutableStateOf(emptyList<RawEntry>()) }
    var categories by remember { mutableStateOf(emptyList<String>()) }
    var placedWords by remember { mutableStateOf(emptyList<PlacedWord>()) }
    var gridCells by remember { mutableStateOf(emptyList<GridCell>()) }
    var userInputs by remember { mutableStateOf(mapOf<Pair<Int, Int>, Char>()) }

    var wordToInput by remember { mutableStateOf<PlacedWord?>(null) }
    var inputText by remember { mutableStateOf("") }

    var puzzleSolved by remember { mutableStateOf(false) }
    var isGenerating by rememberSaveable { mutableStateOf(false) }

    // ── Online multiplayer state ───────────────────────────────────────────────
    var isOnlineGame     by remember { mutableStateOf(false) }
    var onlineRole       by remember { mutableStateOf<OnlineRole?>(null) }
    var onlineCode       by remember { mutableStateOf("") }
    var onlineJoinInput  by remember { mutableStateOf("") }
    var onlineJoinError  by remember { mutableStateOf("") }
    var onlineStatus     by remember { mutableStateOf("") }  // shown on lobby screen
    var showOnlineJoin   by remember { mutableStateOf(false) }
    var onlineListener   by remember { mutableStateOf<ValueEventListener?>(null) }
    // Remote inputs received from the other device — merged into local userInputs on change
    var remoteInputs        by remember { mutableStateOf<Map<Pair<Int,Int>, Char>>(emptyMap()) }
    var remoteIsAnswering   by remember { mutableStateOf(false) }
    var remoteAnsweringName by remember { mutableStateOf("") }
    var activeDifficulty by rememberSaveable(
        stateSaver = listSaver(save = { listOf(it.name) }, restore = { Difficulty.valueOf(it[0]) })
    ) { mutableStateOf(Difficulty.MEDIUM) }
    var resumePrompt by remember { mutableStateOf<SaveSlot?>(null) }
    var difficultyPickCategory by remember { mutableStateOf<String?>(null) }
    var showSettings      by remember { mutableStateOf(false) }
    var showColorPicker   by remember { mutableStateOf(false) }
    var showBgPicker      by remember { mutableStateOf(false) }
    var showCustomize     by remember { mutableStateOf(false) }
    var soundEnabled       by rememberSaveable { mutableStateOf(true) }
    var musicVolume        by rememberSaveable { mutableFloatStateOf(0.35f) }
    var musicEnabled       by rememberSaveable { mutableStateOf(true) }
    // Cell color stored as ARGB Int so rememberSaveable handles it without a custom saver
    var currentCellColorArgb   by rememberSaveable { mutableIntStateOf(android.graphics.Color.WHITE) }
    var currentBtnColorArgb    by rememberSaveable { mutableIntStateOf(0xFF6650A4.toInt()) }
    var showBtnColorPicker     by remember { mutableStateOf(false) }
    var showHowToPlay          by remember { mutableStateOf(false) }
    var showVindictiveTutorial by remember { mutableStateOf(false) }
    var showSingleTutorial     by remember { mutableStateOf(false) }
    var showTeamTutorial       by remember { mutableStateOf(false) }
    var showDailyPrompt        by remember { mutableStateOf(false) }
    var showDailyInstructions  by remember { mutableStateOf(false) }
    var dailyPromptShown       by remember { mutableStateOf(false) }
    // Derived from the saveable Int every recompose — safe because currentCellColorArgb is State
    val currentCellColor = Color(currentCellColorArgb.toLong() and 0xFFFFFFFFL)
    // Word the player single-tapped — used to pan grid to its start cell
    var centreOnWord by remember { mutableStateOf<PlacedWord?>(null) }

    // ── GAME MODE STATE ───────────────────────────────────────────────────────
    var activeGameMode by rememberSaveable(
        stateSaver = listSaver(save = { listOf(it.name) }, restore = { GameMode.valueOf(it[0]) })
    ) { mutableStateOf(GameMode.SINGLE) }
    var player2Name         by rememberSaveable { mutableStateOf("") }
    var viewingProfile      by remember { mutableStateOf("") }   // profile tapped on login
    var confirmDeletePlayer  by remember { mutableStateOf<String?>(null) }  // name pending deletion
    var showNoPlayer2Dialog  by remember { mutableStateOf(false) }  // missing p2 name warning
    var showPlayer2SetupDialog by remember { mutableStateOf(false) }  // popup to enter p2 name + who is p1
    var showTurnDialog         by remember { mutableStateOf(false) }  // between-turn popup
    var turnDialogMessage      by remember { mutableStateOf("") }     // text shown in turn popup
    // Team mode scoring
    var teamP1Score            by rememberSaveable { mutableIntStateOf(0) }
    var teamP2Score            by rememberSaveable { mutableIntStateOf(0) }
    var teamCurrentPlayer      by rememberSaveable { mutableIntStateOf(0) }  // 0=p1, 1=p2
    var profileList         by remember { mutableStateOf(listOf<String>()) }  // refreshable login list

    // ── VINDICTIVE STATE ──────────────────────────────────────────────────────
    var vindPhase by rememberSaveable(
        stateSaver = listSaver(save = { listOf(it.name) }, restore = { VindicativePhase.valueOf(it[0]) })
    ) { mutableStateOf(VindicativePhase.PICK_OWN) }
    var vindCurrentPlayer   by rememberSaveable { mutableIntStateOf(0) }  // 0=p1, 1=p2
    var vindAssignedWord       by remember { mutableStateOf<PlacedWord?>(null) }
    var vindPassDialogVisible   by remember { mutableStateOf(false) }  // pass/answer popup
    var pendingTurnDialog       by remember { mutableStateOf(false) }  // delayed turn dialog after wrong flash
    var vindOpponentCountdown   by remember { mutableIntStateOf(30) }  // countdown seconds
    var vindTimerSeconds        by rememberSaveable { mutableIntStateOf(30) }  // selected timer (15/30/60)
    var showVindTimerDialog     by remember { mutableStateOf(false) }  // timer selection popup
    var showVindCategoryDialog  by remember { mutableStateOf(false) }  // category selection after p2 setup
    // Multi-category combine flow — modal dialog launched from inside the
    // category-pick dialog. Selection lives here so the difficulty step can
    // read it after the multi-select dialog closes.
    var showMultiCategoryDialog by remember { mutableStateOf(false) }
    var showCombineDiffDialog   by remember { mutableStateOf(false) }
    var combineSelection        by remember { mutableStateOf<List<String>>(emptyList()) }
    // Passive-aggressive wrong responses — cycles through 20 taunts
    val tauntIndex              = rememberSaveable { mutableIntStateOf(0) }
    var vindP1Score         by rememberSaveable { mutableIntStateOf(0) }
    var vindP2Score         by rememberSaveable { mutableIntStateOf(0) }
    var showWrongFlash      by remember { mutableStateOf(false) }

    // ── TEAM STATE ────────────────────────────────────────────────────────────
    var teamP1Hints         by rememberSaveable { mutableIntStateOf(0) }

    // ── TIMER ─────────────────────────────────────────────────────────────────
    var elapsedSeconds      by rememberSaveable { mutableLongStateOf(0L) }
    var timerRunning        by remember { mutableStateOf(false) }

    // ── HINTS ─────────────────────────────────────────────────────────────────
    var hintsUsedThisPuzzle by rememberSaveable { mutableIntStateOf(0) }
    var selectedHintWord    by remember { mutableStateOf<PlacedWord?>(null) }

    // ── STREAK ────────────────────────────────────────────────────────────────
    var currentStreak       by rememberSaveable { mutableIntStateOf(0) }
    var streakMilestone     by remember { mutableStateOf<String?>(null) }

    // ── ANIMATION ─────────────────────────────────────────────────────────────
    var animatingCell       by remember { mutableStateOf<Pair<Int,Int>?>(null) }
    var cellsToAnimate      by remember { mutableStateOf<List<Pair<Int,Int>>>(emptyList()) }

    // ── CONFETTI ──────────────────────────────────────────────────────────────
    var showConfetti        by remember { mutableStateOf(false) }

    // ── HIGHLIGHTED WORD (single tap) ─────────────────────────────────────────
    var highlightedWord     by remember { mutableStateOf<PlacedWord?>(null) }

    // ── DAILY ─────────────────────────────────────────────────────────────────
    var isDailyPuzzle       by rememberSaveable { mutableStateOf(false) }
    // Date key (yyyy-MM-dd, UTC) of the Daily being played; null for every other puzzle.
    // Drives the Daily save slot, the once-per-day reward, and Next Puzzle behaviour.
    var activeDailyKey      by rememberSaveable { mutableStateOf<String?>(null) }
    // Mode selected on the home screen before a Daily was started (Daily is always solo).
    var modeBeforeDaily     by rememberSaveable(
        stateSaver = listSaver(save = { listOf(it.name) }, restore = { GameMode.valueOf(it[0]) })
    ) { mutableStateOf(GameMode.SINGLE) }
    var revealedCells       by remember { mutableStateOf<Set<Pair<Int, Int>>>(emptySet()) }   // hint letters
    var teamP2Hints         by rememberSaveable { mutableIntStateOf(0) }
    var teamFirstPlayer     by rememberSaveable { mutableIntStateOf(0) }   // "who goes first" for Team
    var appResumed          by remember { mutableStateOf(true) }           // Activity in foreground
    var onlineNotice        by remember { mutableStateOf<String?>(null) }  // blocking online message
    var dailyInfoMessage    by remember { mutableStateOf<String?>(null) }
    var homeRefresh         by remember { mutableIntStateOf(0) }           // bump to re-read saves on home
    var showResults         by remember { mutableStateOf(false) }          // results card (after confetti)
    var lastResult          by remember { mutableStateOf<PuzzleResult?>(null) }
    var turnDialogTitle     by remember { mutableStateOf("Pass the Phone!") }
    var wrongAnswererIndex  by remember { mutableIntStateOf(0) }

    // derivedStateOf means this only recomputes when gridCells or userInputs
    // actually change — not on every recomposition (e.g. while typing in dialog).
    val isWinner by remember { derivedStateOf {
        gridCells.isNotEmpty() && gridCells.all { cell ->
            userInputs[Pair(cell.x, cell.y)] == cell.char
        }
    }}

    // ── STATE HELPERS ─────────────────────────────────────────────────────────
    // Player index of THIS device: online HOST = 0, GUEST = 1. Offline the
    // logged-in player is index 0 and Player 2 is index 1.
    fun myIndex(): Int = if (isOnlineGame && onlineRole == OnlineRole.GUEST) 1 else 0

    fun vindSnapshot() = VindState(
        vindPhase, vindCurrentPlayer, vindP1Score, vindP2Score,
        vindAssignedWord?.let { placedWords.indexOf(it) } ?: -1
    )
    fun applyVind(s: VindState) {
        vindPhase         = s.phase
        vindCurrentPlayer = s.currentPlayer
        vindP1Score       = s.p1Score
        vindP2Score       = s.p2Score
        vindAssignedWord  = placedWords.getOrNull(s.assignedIndex)
    }
    fun teamSnapshot() = TeamState(teamP1Score, teamP2Score, teamCurrentPlayer, teamP1Hints, teamP2Hints)
    fun applyTeam(s: TeamState) {
        teamP1Score = s.p1Correct; teamP2Score = s.p2Correct
        teamCurrentPlayer = s.turn
        teamP1Hints = s.p1Hints; teamP2Hints = s.p2Hints
    }

    /** Snapshot of the puzzle in progress, or null when there is nothing resumable. */
    fun currentSession(): PuzzleSession? {
        if (placedWords.isEmpty() || isGenerating || puzzleSolved || isOnlineGame) return null
        val isDaily = activeDailyKey != null
        return PuzzleSession(
            mode           = if (isDaily) GameMode.DAILY else activeGameMode,
            category       = activeCategory,
            difficulty     = activeDifficulty,
            words          = placedWords,
            inputs         = userInputs,
            revealed       = revealedCells,
            combined       = combinedCategories,
            dailyKey       = activeDailyKey,
            elapsedSeconds = elapsedSeconds,
            hintsUsed      = hintsUsedThisPuzzle,
            streak         = currentStreak,
            player2        = if (activeGameMode == GameMode.TEAM || activeGameMode == GameMode.VINDICTIVE) player2Name else "",
            team           = teamSnapshot(),
            vind           = vindSnapshot(),
            vindTimerSecs  = vindTimerSeconds,
            bgArgb         = currentBgColor.toArgb(),
            bgImage        = currentBgImageName
        )
    }

    /** Persists the puzzle in progress to its own mode-specific slot. No-op for online/solved games. */
    fun saveCurrentPuzzle() {
        if (playerName.isBlank()) return
        currentSession()?.let { saveManager.savePuzzle(playerName, it) }
    }

    fun currentSlot(): SaveSlot =
        activeDailyKey?.let { SaveSlot.daily(it) }
            ?: SaveSlot(if (activeGameMode == GameMode.DAILY) GameMode.SINGLE else activeGameMode, activeCategory, activeDifficulty)

    fun resetOverlays() {
        showConfetti = false; showResults = false; showTurnDialog = false
        vindPassDialogVisible = false; pendingTurnDialog = false; wordToInput = null
        showWrongFlash = false; streakMilestone = null
    }

    // Releases all online-game state. The Firebase listener itself is torn down by
    // the LaunchedEffect keyed on (onlineCode, isOnlineGame) as soon as
    // isOnlineGame flips false. Leaving an unfinished game marks it abandoned so
    // the other device is told instead of waiting forever. Idempotent.
    fun cleanupOnlineSession() {
        if (isOnlineGame && onlineCode.isNotEmpty()) {
            val code  = onlineCode
            val field = if (onlineRole == OnlineRole.HOST) "p0answering" else "p1answering"
            runCatching { FirebaseGameManager.writeState(code, mapOf(field to false)) }
            runCatching {
                if (puzzleSolved) FirebaseGameManager.closeGame(code) else FirebaseGameManager.abandonGame(code)
            }
        }
        onlineListener?.let { listener ->
            if (onlineCode.isNotEmpty()) FirebaseGameManager.stopListening(onlineCode, listener)
        }
        onlineListener = null
        if (isOnlineGame) player2Name = ""   // the online opponent isn't a local Player 2
        isOnlineGame = false
        onlineRole   = null
        onlineCode   = ""
        onlineStatus = ""
        remoteInputs = emptyMap()
        remoteIsAnswering   = false
        remoteAnsweringName = ""
    }

    fun restoreSession(s: PuzzleSession) {
        activeGameMode     = if (s.isDaily) GameMode.DAILY else s.mode
        activeCategory     = s.category
        activeDifficulty   = s.difficulty
        combinedCategories = s.combined
        activeDailyKey     = s.dailyKey
        isDailyPuzzle      = s.isDaily
        placedWords        = s.words
        gridCells          = buildGridCells(s.words)
        userInputs         = s.inputs
        revealedCells      = s.revealed
        elapsedSeconds     = s.elapsedSeconds
        hintsUsedThisPuzzle = s.hintsUsed
        currentStreak      = s.streak
        if (s.player2.isNotBlank()) player2Name = s.player2
        applyTeam(s.team)
        applyVind(s.vind)
        vindTimerSeconds   = s.vindTimerSecs
        currentBgColor     = Color(s.bgArgb)
        currentBgImageName = s.bgImage
        selectedHintWord   = null
        highlightedWord    = null
        resetOverlays()
        puzzleSolved       = false
        isGenerating       = false
        timerRunning       = true
        appMode            = AppMode.DASHBOARD
        // A Vindictive save taken while a clue was waiting resumes at the hand-off,
        // so the answer timer restarts only once the right player has the phone.
        if (s.mode == GameMode.VINDICTIVE && s.vind.phase == VindicativePhase.OPPONENT_WAIT) {
            val who = if (s.vind.currentPlayer == 0) playerName else player2Name
            turnDialogMessage = "✋ $who — your clue is waiting!"
            showTurnDialog = true
        }
    }

    /** Resumes a saved slot. Returns false (and drops the unreadable save) if it can't be loaded. */
    fun resumeSlot(slot: SaveSlot): Boolean {
        val s = saveManager.loadPuzzle(playerName, slot)
        if (s == null) { saveManager.clearPuzzle(playerName, slot); homeRefresh++; return false }
        if (isOnlineGame) cleanupOnlineSession()
        restoreSession(s)
        return true
    }

    fun launchPuzzle(category: String, difficulty: Difficulty, combined: List<String> = emptyList()) {
        if (activeGameMode == GameMode.DAILY) activeGameMode = modeBeforeDaily
        activeCategory     = category
        activeDifficulty   = difficulty
        combinedCategories = combined
        activeDailyKey     = null
        isDailyPuzzle      = false
        resetOverlays()
        appMode            = AppMode.DASHBOARD
        puzzleSolved       = false
        if (!isOnlineGame && activeGameMode != GameMode.SINGLE && player2Name.isNotBlank()) {
            saveManager.ensurePlayer(player2Name)
        }
        isGenerating = true
    }

    fun launchDailyPuzzle() {
        val now = System.currentTimeMillis()
        val key = DailyPuzzle.dateKey(now)
        if (playerName.isNotBlank() && saveManager.isDailyCompleted(playerName, key)) {
            val hrs  = DailyPuzzle.millisUntilNext(now) / 3_600_000L
            val mins = (DailyPuzzle.millisUntilNext(now) / 60_000L) % 60
            dailyInfoMessage = "You've already solved today's Daily Puzzle. A new one arrives in " +
                (if (hrs > 0) "${hrs}h ${mins}m." else "${mins}m.")
            return
        }
        if (isOnlineGame) cleanupOnlineSession()
        if (activeGameMode != GameMode.DAILY) modeBeforeDaily = activeGameMode
        val slot = SaveSlot.daily(key)
        if (saveManager.hasSave(playerName, slot) && resumeSlot(slot)) return
        activeGameMode     = GameMode.DAILY
        activeDailyKey     = key
        isDailyPuzzle      = true
        activeCategory     = DailyPuzzle.CATEGORY
        activeDifficulty   = Difficulty.EXPERT
        combinedCategories = emptyList()
        resetOverlays()
        appMode            = AppMode.DASHBOARD
        puzzleSolved       = false
        isGenerating       = true
    }

    /** Leave the puzzle screen for home, saving progress (solo/local modes) first. */
    fun goHome() {
        timerRunning = false
        saveCurrentPuzzle()
        cleanupOnlineSession()
        resetOverlays()
        puzzleSolved = false
        if (activeGameMode == GameMode.DAILY) activeGameMode = modeBeforeDaily
        activeDailyKey = null
        isDailyPuzzle  = false
        homeRefresh++
        appMode = AppMode.CATEGORY_SELECT
    }

    // ── ANSWER / TURN HANDLING ───────────────────────────────────────────────
    // All scoring goes through core TeamRules / VindictiveRules; this layer only
    // renders feedback and syncs the result.
    fun nameOf(idx: Int): String =
        if (isOnlineGame && onlineRole == OnlineRole.GUEST) (if (idx == 0) player2Name else playerName)
        else (if (idx == 0) playerName else player2Name)

    fun commitWord(word: PlacedWord) {
        val cells = word.cells()
        userInputs = userInputs + cells.mapIndexed { i, c -> c to word.word[i] }
        if (isOnlineGame) cells.forEachIndexed { i, c -> FirebaseGameManager.writeInput(onlineCode, c.first, c.second, word.word[i]) }
        cellsToAnimate = cells
    }

    fun onCorrectFeedback() {
        if (soundEnabled) SoundPlayer.playCorrect()
        vibrateCorrect(context)
        currentStreak++
        checkStreakMilestone(currentStreak)?.let { milestone ->
            streakMilestone = milestone
            if (soundEnabled) SoundPlayer.playClap()
        }
    }

    fun onWrongFeedback(answererIndex: Int) {
        if (soundEnabled) SoundPlayer.playWrong()
        vibrateWrong(context)
        currentStreak = 0
        wrongAnswererIndex = answererIndex
        tauntIndex.intValue++
        showWrongFlash = true
    }

    fun syncVind(extra: Map<String, Any> = emptyMap()) {
        if (!isOnlineGame) return
        val s = vindSnapshot()
        val myKey = if (onlineRole == OnlineRole.HOST) "hostScore" else "guestScore"
        val myVal = if (onlineRole == OnlineRole.HOST) s.p1Score else s.p2Score
        FirebaseGameManager.writeState(onlineCode, mapOf(
            "vindPhase"           to s.phase.name,
            "vindAssignedWordIdx" to s.assignedIndex,
            "vindCurrentPlayer"   to s.currentPlayer,
            myKey                 to myVal
        ) + extra)
    }

    fun submitAnswer(word: PlacedWord, answer: String) {
        val correct = answer == word.word
        when (activeGameMode) {
            GameMode.VINDICTIVE -> {
                val before = vindSnapshot()
                val actor  = before.currentPlayer
                val (after, outcome) = when (before.phase) {
                    VindicativePhase.PICK_OWN      -> VindictiveRules.onOwnAnswer(before, correct)
                    VindicativePhase.OPPONENT_WAIT -> VindictiveRules.onAssignedAnswer(before, correct)
                    VindicativePhase.ASSIGN_CLUE   -> return   // assigning never goes through the answer box
                }
                if (correct) { commitWord(word); onCorrectFeedback() } else onWrongFeedback(actor)
                applyVind(after)
                syncVind(mapOf("vindCountdown" to 0))
                // Same player keeps the phone to pick a clue for the other — say so, don't say "pass".
                if (!isOnlineGame && before.phase == VindicativePhase.OPPONENT_WAIT) {
                    val who  = nameOf(actor)
                    val line = if (outcome == VindOutcome.ASSIGNED_CORRECT) "✅ Correct! +${Economy.VIND_ASSIGNED_CORRECT} for $who"
                               else "❌ Wrong! ${Economy.VIND_ASSIGNED_WRONG} for $who"
                    turnDialogTitle   = "$who picks next"
                    turnDialogMessage = "$line\n$who now picks a clue for ${nameOf(1 - actor)}.\n" +
                        "${nameOf(0)}: ${vindP1Score} pts  |  ${nameOf(1)}: ${vindP2Score} pts"
                    if (correct) showTurnDialog = true else pendingTurnDialog = true
                }
            }
            GameMode.TEAM -> {
                val before = teamSnapshot()
                if (correct) { commitWord(word); onCorrectFeedback() } else onWrongFeedback(before.turn)
                val after = TeamRules.onAttempt(before, correct)
                applyTeam(after)
                if (isOnlineGame) {
                    val myKey = if (onlineRole == OnlineRole.HOST) "hostScore" else "guestScore"
                    val myVal = if (onlineRole == OnlineRole.HOST) after.p1Correct else after.p2Correct
                    FirebaseGameManager.writeState(onlineCode, mapOf("turn" to after.turn, myKey to myVal))
                } else {
                    turnDialogTitle   = "Pass the Phone!"
                    turnDialogMessage = (if (correct) "✅ Nice one, ${nameOf(before.turn)}!" else "❌ Not quite, ${nameOf(before.turn)}.") +
                        "\n✋ Pass to ${nameOf(after.turn)}\n" +
                        "${nameOf(0)}: ${after.p1Correct} solved  |  ${nameOf(1)}: ${after.p2Correct} solved"
                    if (correct) showTurnDialog = true else pendingTurnDialog = true
                }
            }
            GameMode.SINGLE, GameMode.DAILY -> {
                if (correct) { commitWord(word); onCorrectFeedback() } else onWrongFeedback(0)
            }
        }
        wordToInput     = null
        highlightedWord = null
        inputText       = ""
    }

    /** Vindictive: the acting player hands [word] to the opponent. */
    fun assignClue(word: PlacedWord) {
        val idx = placedWords.indexOf(word)
        if (idx < 0 || isWordSolved(word, userInputs)) return
        val after = VindictiveRules.onAssign(vindSnapshot(), idx)
        applyVind(after)
        vindPassDialogVisible = false
        vindOpponentCountdown = vindTimerSeconds
        inputText = ""
        syncVind(mapOf("vindCountdown" to vindTimerSeconds))
        if (!isOnlineGame) {
            turnDialogTitle   = "Pass the Phone!"
            turnDialogMessage = "✋ Pass to ${nameOf(after.currentPlayer)}!\n" +
                "A clue is waiting — the ${vindTimerSeconds}s timer starts when they tap I'm Ready."
            showTurnDialog = true
        }
    }

    /** Vindictive: the answerer passes on the assigned clue (−1, per the tutorial). */
    fun passAssigned() {
        val before = vindSnapshot()
        if (before.phase != VindicativePhase.OPPONENT_WAIT) return
        val who = nameOf(before.currentPlayer)
        val (after, _) = VindictiveRules.onPass(before)
        applyVind(after)
        vindPassDialogVisible = false
        wordToInput = null
        inputText   = ""
        syncVind(mapOf("vindCountdown" to 0))
        if (!isOnlineGame) {
            turnDialogTitle   = "$who picks next"
            turnDialogMessage = "⏭ $who passed (${Economy.VIND_PASS})\n$who now picks a clue for ${nameOf(1 - before.currentPlayer)}.\n" +
                "${nameOf(0)}: ${vindP1Score} pts  |  ${nameOf(1)}: ${vindP2Score} pts"
            showTurnDialog = true
        }
    }

    /** Vindictive: answer timer ran out on the answering device — offer Answer It / Pass. */
    fun onVindTimeout() {
        vindOpponentCountdown = 0
        wordToInput = null            // typed text is kept in inputText for "Answer It"
        vindPassDialogVisible = true
        if (soundEnabled) SoundPlayer.playWrong()
        vibrateWrong(context)
        if (isOnlineGame) FirebaseGameManager.writeState(onlineCode, mapOf("vindCountdown" to 0))
    }

    // ── STARTUP ───────────────────────────────────────────────────────────────
    // Lock to portrait permanently — crossword grid only works in portrait.
    // Suppress lint: orientation lock is intentional for this game.
    @Suppress("SourceLockedOrientationActivity")
    LaunchedEffect(Unit) {
        (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        AmbientMusicPlayer.init(context)
        // Firebase sign-in is deferred until the player actually hosts or joins —
        // a purely offline session never touches the network.
        profileList = saveManager.getAllPlayerNames()
        if (saveManager.isFirstLaunch()) {
            showHowToPlay = true
            saveManager.markLaunched()
        }
        // Asset I/O and validation off the main thread.
        val data = withContext(Dispatchers.IO) { loadWordData(context) }
        allEntries = data.entries
        categories = data.categories

        // Discover background images from assets — runs once at startup
        bgImagePool = withContext(Dispatchers.IO) { discoverBgImages(context) }

        val savedUser = saveManager.getLastUser()
        if (savedUser.isNotBlank()) {
            playerName = savedUser
            currentScore = saveManager.getScore(playerName)
            currentCompleted = saveManager.getCompleted(playerName)
            currentCellColorArgb = saveManager.getCellColorArgb(playerName)
            currentBtnColorArgb  = saveManager.getButtonColorArgb(playerName)
            musicVolume = saveManager.getMusicVolume()
            // The Activity was recreated mid-puzzle (process death, theme change):
            // the grid lives in memory, but it was autosaved on pause — resume it.
            val needsRestore = appMode == AppMode.DASHBOARD && placedWords.isEmpty()
            val restored = needsRestore &&
                saveManager.getInProgressPuzzles(playerName).firstOrNull()?.let { resumeSlot(it) } == true
            if (!restored && (appMode == AppMode.LOGIN || needsRestore)) appMode = AppMode.CATEGORY_SELECT
        } else {
            appMode = AppMode.LOGIN
        }
    }

    // ── COMPLETION ────────────────────────────────────────────────────────────
    LaunchedEffect(isWinner) {
        if (!isWinner || puzzleSolved || isGenerating) return@LaunchedEffect
        puzzleSolved = true
        timerRunning = false
        if (isOnlineGame) FirebaseGameManager.writeState(onlineCode, mapOf("solved" to true))
        // Clear any lingering multiplayer dialogs so complete screen is unobstructed
        showTurnDialog = false
        vindPassDialogVisible = false
        pendingTurnDialog = false
        wordToInput = null

        val dailyKey = activeDailyKey
        val isDaily  = dailyKey != null
        val hints    = hintsUsedThisPuzzle
        val reward   = Economy.completionPoints(activeDifficulty, isDaily)
        val net      = Economy.netPoints(activeDifficulty, isDaily, hints)
        // The Daily pays out once per UTC day.
        val dailyAlreadyPaid = dailyKey != null && saveManager.isDailyCompleted(playerName, dailyKey)
        val award    = if (dailyAlreadyPaid) 0 else Economy.lifetimeAwardOnWin(activeDifficulty, isDaily)
        // Online, each device credits only its own player; the opponent is credited on their phone.
        val creditP2 = !isOnlineGame && player2Name.isNotBlank() && player2Name != playerName
        // Credit each word to its real source category (combined puzzles). Daily words
        // never enter per-category history, so playing a Daily can't reshape future puzzles.
        val wordsByCategory: Map<String, List<String>> = if (isDaily) emptyMap() else
            placedWords.groupBy(
                { w -> w.category.ifBlank {
                    if (combinedCategories.isEmpty()) activeCategory
                    else allEntries.firstOrNull { it.answer == w.word && it.category in combinedCategories }?.category
                        ?: activeCategory
                } },
                { it.word }
            )
        fun creditWords(name: String) = wordsByCategory.forEach { (cat, ws) -> saveManager.addUsedWords(name, cat, ws) }

        saveManager.clearPuzzle(playerName, currentSlot())
        val me    = myIndex()
        val team  = teamSnapshot()
        val vind  = vindSnapshot()

        when (activeGameMode) {
            GameMode.SINGLE, GameMode.DAILY -> {
                saveManager.addScore(playerName, award)
                saveManager.addCompleted(playerName)
                creditWords(playerName)
                if (dailyKey != null) saveManager.markDailyCompleted(playerName, dailyKey)
                saveManager.saveStat(playerName, StatRecord(
                    if (isDaily) DailyPuzzle.CATEGORY else activeCategory,
                    if (isDaily) "DAILY" else activeDifficulty.name,
                    if (isDaily) GameMode.DAILY.name else GameMode.SINGLE.name,
                    hints, elapsedSeconds, net))
            }
            GameMode.TEAM -> {
                // Both players earn the full reward.
                saveManager.addScore(playerName, award)
                saveManager.addCompleted(playerName)
                creditWords(playerName)
                val myHints    = if (isOnlineGame) hints else team.p1Hints
                saveManager.saveStat(playerName, StatRecord(
                    activeCategory, activeDifficulty.name, GameMode.TEAM.name,
                    myHints, elapsedSeconds, net, player2Name))
                if (creditP2) {
                    saveManager.addScore(player2Name, award)
                    saveManager.addCompleted(player2Name)
                    creditWords(player2Name)
                    saveManager.saveStat(player2Name, StatRecord(
                        activeCategory, activeDifficulty.name, GameMode.TEAM.name,
                        team.p2Hints, elapsedSeconds, net, playerName))
                }
            }
            GameMode.VINDICTIVE -> {
                // Match scores are banked (floored at 0). Ties count as a win for both.
                val myScore    = if (me == 0) vind.p1Score else vind.p2Score
                val otherScore = if (me == 0) vind.p2Score else vind.p1Score
                saveManager.addScore(playerName, Economy.vindictiveBank(myScore))
                saveManager.addCompleted(playerName)
                creditWords(playerName)
                saveManager.saveStat(playerName, StatRecord(
                    activeCategory, activeDifficulty.name, GameMode.VINDICTIVE.name,
                    hints, elapsedSeconds, myScore, player2Name, otherScore, myScore >= otherScore))
                if (creditP2) {
                    saveManager.addScore(player2Name, Economy.vindictiveBank(otherScore))
                    saveManager.addCompleted(player2Name)
                    creditWords(player2Name)
                    saveManager.saveStat(player2Name, StatRecord(
                        activeCategory, activeDifficulty.name, GameMode.VINDICTIVE.name,
                        0, elapsedSeconds, otherScore, playerName, myScore, otherScore >= myScore))
                }
            }
        }
        currentScore = saveManager.getScore(playerName)
        currentCompleted = saveManager.getCompleted(playerName)
        homeRefresh++
        lastResult = PuzzleResult(
            mode = activeGameMode, isDaily = isDaily, reward = reward, hints = hints, net = net,
            awarded = if (activeGameMode == GameMode.VINDICTIVE) null else award,
            dailyAlreadyPaid = dailyAlreadyPaid, seconds = elapsedSeconds,
            team = team, vind = vind, myIndex = me
        )
        // Celebrate first; the results card follows once the confetti has had a moment.
        showConfetti = true
        if (soundEnabled) SoundPlayer.playCelebration()
        delay(1100L)
        showResults = true
    }

    // Start/stop ambient music based on enabled state and volume
    LaunchedEffect(musicEnabled, musicVolume) {
        if (musicEnabled) AmbientMusicPlayer.start(musicVolume)
        else AmbientMusicPlayer.stop()
    }

    // Lifecycle: pause music and the puzzle clock when backgrounded, autosave the
    // puzzle so a process kill in the background loses nothing, and release the
    // player only on destroy (not on config changes).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE   -> {
                    appResumed = false
                    AmbientMusicPlayer.stop()
                    saveCurrentPuzzle()
                }
                Lifecycle.Event.ON_RESUME  -> {
                    appResumed = true
                    if (musicEnabled) AmbientMusicPlayer.start(musicVolume)
                }
                Lifecycle.Event.ON_DESTROY -> AmbientMusicPlayer.release()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Timer — ticks only while a puzzle is actually being played: not in the
    // background, not while the Settings sheet is covering the grid.
    val timerActive = timerRunning && appResumed && !showSettings && appMode == AppMode.DASHBOARD
    LaunchedEffect(timerActive) {
        while (timerActive) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    // Cell pop animation — cycles through correct word cells 3× in random order
    LaunchedEffect(cellsToAnimate) {
        if (cellsToAnimate.isEmpty()) return@LaunchedEffect
        repeat(3) {
            cellsToAnimate.shuffled().forEach { pos ->
                animatingCell = pos
                delay(60L)
            }
        }
        animatingCell = null
        cellsToAnimate = emptyList()
    }

    // Streak milestone — auto-clear after 2 seconds
    LaunchedEffect(streakMilestone) {
        if (streakMilestone != null) {
            delay(2000L)
            streakMilestone = null
        }
    }

    // Wrong flash — auto-clear after 800ms
    LaunchedEffect(showWrongFlash) {
        if (showWrongFlash) {
            // Vindictive mode: hold longer so taunt is readable before turn dialog
            delay(if (activeGameMode == GameMode.VINDICTIVE) 2500L else 800L)
            showWrongFlash = false
            // Show pending turn dialog AFTER flash clears so it doesn't cover the taunt
            if (pendingTurnDialog) {
                pendingTurnDialog = false
                if (!isOnlineGame) showTurnDialog = true
            }
        }
    }

    // Confetti — auto-clear after 4 seconds
    LaunchedEffect(showConfetti) {
        if (showConfetti) {
            delay(4000L)
            showConfetti = false
        }
    }

    // Daily prompt — once per login session on the first visit home, and only
    // if today's Daily hasn't been solved yet.
    LaunchedEffect(appMode) {
        if (appMode == AppMode.CATEGORY_SELECT && !dailyPromptShown && playerName.isNotBlank()) {
            dailyPromptShown = true
            val today = DailyPuzzle.dateKey(System.currentTimeMillis())
            if (!saveManager.isDailyCompleted(playerName, today)) showDailyPrompt = true
        }
    }

    // Background image pool — refresh every time the picker opens.
    // Also clear the currentBgImageName if the selected image no longer exists,
    // and pass the fresh pool into BgPickerScreen so its thumbnail cache can
    // drop entries for files that have been deleted since last open.
    LaunchedEffect(showBgPicker) {
        if (showBgPicker) {
            val freshPool = withContext(Dispatchers.IO) { discoverBgImages(context) }
            bgImagePool = freshPool
            // If the active background was deleted, reset to none
            if (currentBgImageName != BG_IMAGE_NONE && !freshPool.contains(currentBgImageName)) {
                currentBgImageName = BG_IMAGE_NONE
            }
        }
    }

    // Green cell highlight — auto-fade after 2 seconds (momentary indicator only).
    // The hint target (selectedHintWord) is NOT cleared with it.
    LaunchedEffect(highlightedWord) {
        if (highlightedWord != null) {
            delay(2000L)
            highlightedWord = null
        }
    }

    // ── Firebase sync listener — active only during online games ─────────────
    // Owned by this effect: it is registered for exactly (code) and removed in
    // `finally` when the key changes or the game ends — no leaked listeners.
    LaunchedEffect(onlineCode, isOnlineGame) {
        if (!isOnlineGame || onlineCode.isEmpty()) return@LaunchedEffect
        val code = onlineCode
        val listener = FirebaseGameManager.listen(code, onUpdate = { snap ->
            if (!isOnlineGame || onlineCode != code) return@listen
            val status  = snap.child("status").getValue(String::class.java) ?: return@listen
            val guestNm = snap.child("guestName").getValue(String::class.java) ?: ""

            if (status == FirebaseGameManager.STATUS_ABANDONED && !puzzleSolved) {
                val who = player2Name.ifBlank { if (onlineRole == OnlineRole.HOST) "Your opponent" else "The host" }
                onlineNotice = "$who left the game."
                return@listen
            }

            // HOST: guest just joined — generate and publish the puzzle.
            if (onlineRole == OnlineRole.HOST && appMode == AppMode.ONLINE_LOBBY && guestNm.isNotEmpty()) {
                player2Name  = guestNm
                onlineStatus = "$guestNm joined! Starting puzzle…"
                isGenerating = true
                appMode      = AppMode.DASHBOARD
            }

            // GUEST: puzzle arrived — populate local state and jump into DASHBOARD
            if (onlineRole == OnlineRole.GUEST && status == FirebaseGameManager.STATUS_PLAYING &&
                appMode == AppMode.ONLINE_LOBBY) {
                val parsed = FirebaseGameManager.parsePuzzle(snap.child("puzzle").getValue(String::class.java) ?: "")
                if (parsed.isNotEmpty()) {
                    activeCategory     = snap.child("category").getValue(String::class.java)?.ifBlank { null } ?: activeCategory
                    placedWords        = parsed
                    gridCells          = buildGridCells(parsed)
                    userInputs         = emptyMap()
                    revealedCells      = emptySet()
                    elapsedSeconds     = 0L
                    hintsUsedThisPuzzle = 0
                    currentStreak      = 0
                    selectedHintWord   = null
                    highlightedWord    = null
                    activeDailyKey     = null
                    isDailyPuzzle      = false
                    resetOverlays()
                    timerRunning       = true
                    isGenerating       = false
                    puzzleSolved       = false
                    appMode            = AppMode.DASHBOARD
                }
            }

            // Letters from the other device. Only correct letters are accepted; they
            // override a wrong in-progress local letter but never erase anything.
            val rawInputs = snap.child("inputs").children.mapNotNull { child ->
                val parts = child.key?.split("_") ?: return@mapNotNull null
                val x  = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
                val y  = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
                val ch = child.getValue(String::class.java)?.firstOrNull() ?: return@mapNotNull null
                Pair(x, y) to ch
            }.toMap()
            if (rawInputs != remoteInputs) {
                remoteInputs = rawInputs
                userInputs   = mergeRemoteInputs(userInputs, rawInputs, solutionOf(placedWords))
            }

            // Sync turn and scores (team + vindictive both use hostScore/guestScore).
            // Each device reads only the OTHER side's score — its own side is authoritative locally.
            val remoteTurn       = snap.child("turn").getValue(Long::class.java)?.toInt()
            val remoteHostScore  = snap.child("hostScore").getValue(Long::class.java)?.toInt()
            val remoteGuestScore = snap.child("guestScore").getValue(Long::class.java)?.toInt()
            if (remoteTurn != null) teamCurrentPlayer = remoteTurn
            if (remoteHostScore  != null && onlineRole == OnlineRole.GUEST) { teamP1Score = remoteHostScore;  vindP1Score = remoteHostScore }
            if (remoteGuestScore != null && onlineRole == OnlineRole.HOST)  { teamP2Score = remoteGuestScore; vindP2Score = remoteGuestScore }

            // Sync vindictive state
            val remoteVindPhase     = snap.child("vindPhase").getValue(String::class.java)
            val remoteVindPlayer    = snap.child("vindCurrentPlayer").getValue(Long::class.java)?.toInt()
            val remoteVindWordIdx   = snap.child("vindAssignedWordIdx").getValue(Long::class.java)?.toInt() ?: -1
            val remoteVindCountdown = snap.child("vindCountdown").getValue(Long::class.java)?.toInt()
            if (remoteVindPhase != null)  runCatching { vindPhase = VindicativePhase.valueOf(remoteVindPhase) }
            if (remoteVindPlayer != null) vindCurrentPlayer = remoteVindPlayer
            vindAssignedWord = placedWords.getOrNull(remoteVindWordIdx)
            // The answerer's device owns the countdown; the other device only mirrors it.
            if (remoteVindCountdown != null && !VindictiveRules.ownsTimer(vindSnapshot(), true, myIndex())) {
                vindOpponentCountdown = remoteVindCountdown
            }

            // Sync timer seconds (guest adopts host's chosen timer)
            val remoteTimerSecs = snap.child("timerSeconds").getValue(Long::class.java)?.toInt()
            if (remoteTimerSecs != null && onlineRole == OnlineRole.GUEST) vindTimerSeconds = remoteTimerSecs

            // Sync answering indicators
            val p0answering = snap.child("p0answering").getValue(Boolean::class.java) ?: false
            val p1answering = snap.child("p1answering").getValue(Boolean::class.java) ?: false
            remoteIsAnswering   = if (onlineRole == OnlineRole.HOST) p1answering else p0answering
            remoteAnsweringName = player2Name   // player2Name = other player's name on every device

            // The other device finished the grid. Fill in the solution locally so this
            // device runs its own completion (and credits its own player) — previously
            // it jumped straight to "solved" and never paid out.
            val remoteSolved = snap.child("solved").getValue(Boolean::class.java) ?: false
            if (remoteSolved && !puzzleSolved && placedWords.isNotEmpty()) {
                userInputs = userInputs + solutionOf(placedWords)
            }
        }, onError = { msg -> if (isOnlineGame) onlineNotice = msg })
        onlineListener = listener
        try {
            awaitCancellation()
        } finally {
            FirebaseGameManager.stopListening(code, listener)
        }
    }

    // A host who backs out of the setup dialogs before reaching the lobby leaves an
    // orphaned "waiting" game behind — end it so nothing later is treated as online.
    val inOnlineSetup = showVindTimerDialog || showVindCategoryDialog || showMultiCategoryDialog ||
        showCombineDiffDialog || difficultyPickCategory != null || showOnlineJoin
    LaunchedEffect(isOnlineGame, appMode, inOnlineSetup) {
        if (isOnlineGame && appMode == AppMode.CATEGORY_SELECT && !inOnlineSetup) cleanupOnlineSession()
    }

    // ── PUZZLE GENERATION ─────────────────────────────────────────────────────
    LaunchedEffect(isGenerating) {
        if (!isGenerating) return@LaunchedEffect
        val dailyKey = activeDailyKey
        val combined = combinedCategories
        val category = activeCategory
        val target   = activeDifficulty.wordCount
        // Used words: the Daily ignores history entirely (deterministic for everyone);
        // combined puzzles consult each real category's history.
        val used: Set<String> = when {
            dailyKey != null      -> emptySet()
            combined.isNotEmpty() -> combined.flatMap { saveManager.getUsedWords(playerName, it) }.toSet()
            else                  -> saveManager.getUsedWords(playerName, category)
        }
        val entries = allEntries
        val generated = withContext(Dispatchers.Default) {
            when {
                dailyKey != null      -> DailyPuzzle.generate(entries, dailyKey)
                combined.isNotEmpty() -> generateCrossword(entries.filter { it.category in combined }, target, used)
                else                  -> generateCrossword(entries.filter { it.category == category }, target, used)
            }
        }

        if (generated.isEmpty()) {
            // Never drop the player into an empty, unsolvable grid.
            isGenerating = false
            timerRunning = false
            cleanupOnlineSession()
            if (activeGameMode == GameMode.DAILY) activeGameMode = modeBeforeDaily
            activeDailyKey = null
            isDailyPuzzle  = false
            appMode = AppMode.CATEGORY_SELECT
            dailyInfoMessage = "Couldn't build a puzzle from that selection. Try another category."
            return@LaunchedEffect
        }

        placedWords = generated
        gridCells = buildGridCells(placedWords)
        userInputs = emptyMap()
        revealedCells = emptySet()
        elapsedSeconds = 0L
        hintsUsedThisPuzzle = 0
        teamP1Hints = 0
        teamP2Hints = 0
        selectedHintWord = null
        vindP1Score = 0; vindP2Score = 0
        teamP1Score = 0; teamP2Score = 0
        teamCurrentPlayer = teamFirstPlayer
        vindPhase = VindicativePhase.PICK_OWN
        // vindCurrentPlayer is set by the player setup dialog — don't overwrite it here
        vindAssignedWord = null
        highlightedWord = null
        currentStreak = 0
        tauntIndex.intValue = 0
        lastResult = null
        showResults = false
        timerRunning = true
        // Pick a random background image from the pool.
        // Fall back to a random colour if the pool is empty.
        currentBgImageName = if (bgImagePool.isNotEmpty())
            bgImagePool[Random.nextInt(bgImagePool.size)]
        else BG_IMAGE_NONE
        currentBgColor = Color(
            Random.nextFloat().coerceIn(0.4f, 0.7f),
            Random.nextFloat().coerceIn(0.4f, 0.7f),
            Random.nextFloat().coerceIn(0.4f, 0.7f),
            1f
        )
        puzzleSolved = false
        isGenerating = false
        // Online host publishes the fresh puzzle (and opening turn) exactly once, here —
        // not from an effect that could fire with a previous game's words.
        if (isOnlineGame && onlineRole == OnlineRole.HOST && onlineCode.isNotEmpty()) {
            FirebaseGameManager.writePuzzle(onlineCode, generated)
            FirebaseGameManager.writeState(onlineCode, mapOf(
                "turn" to teamCurrentPlayer,
                "vindCurrentPlayer" to vindCurrentPlayer,
                "vindPhase" to VindicativePhase.PICK_OWN.name,
                "vindAssignedWordIdx" to -1
            ))
        }
    }

    // Online name helpers — player index 0 = HOST, index 1 = GUEST on every device.
    // On the HOST device: p0=playerName, p1=player2Name.
    // On the GUEST device the mapping is inverted so existing display code works correctly.
    val onlineP0Name = if (isOnlineGame && onlineRole == OnlineRole.GUEST) player2Name else playerName
    val onlineP1Name = if (isOnlineGame && onlineRole == OnlineRole.GUEST) playerName   else player2Name

    // App-wide gradient derived from the player's chosen button color —
    // computed here (before AnimatedContent) so all screens and dialogs can use it.
    val appBtnColor = Color(currentBtnColorArgb.toLong() and 0xFFFFFFFFL)
    val appBtnDim   = appBtnColor.copy(
        red   = (appBtnColor.red   * 0.75f).coerceIn(0f, 1f),
        green = (appBtnColor.green * 0.75f).coerceIn(0f, 1f),
        blue  = (appBtnColor.blue  * 0.75f).coerceIn(0f, 1f)
    )
    val appBtnGradient = Brush.verticalGradient(listOf(
        appBtnColor.copy(red = (appBtnColor.red + 0.15f).coerceIn(0f,1f),
            green = (appBtnColor.green + 0.15f).coerceIn(0f,1f),
            blue  = (appBtnColor.blue  + 0.15f).coerceIn(0f,1f)),
        appBtnColor, appBtnDim
    ))
    val redGradient = Brush.verticalGradient(listOf(Color(0xFFE53935), Color(0xFFC62828), Color(0xFF8B0000)))

    // ── SYSTEM BACK HANDLING ────────────────────────────────────────────────────
    // Routes Android's back gesture to the same destinations as in-app back arrows.
    // Back at LOGIN or CATEGORY_SELECT (the home hub) is left to the system so it
    // closes the app like a regular Android home — the user logs out explicitly.
    BackHandler(enabled = appMode != AppMode.LOGIN && appMode != AppMode.CATEGORY_SELECT) {
        vibrateLight(context)
        when (appMode) {
            AppMode.STATS -> {
                viewingProfile = ""
                dailyPromptShown = false
                appMode = AppMode.LOGIN
            }
            AppMode.ONLINE_LOBBY -> {
                cleanupOnlineSession()
                appMode = AppMode.CATEGORY_SELECT
            }
            AppMode.DASHBOARD -> goHome()
            AppMode.CATEGORY_SELECT, AppMode.LOGIN -> { /* unreachable — handler disabled */ }
        }
    }

    AnimatedContent(
        targetState = appMode,
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
        label = "screenTransition"
    ) { mode ->
    when (mode) {
        AppMode.LOGIN -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.background
                        ))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 36.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Logo mark — simple crossword grid drawn with Canvas
                    Canvas(modifier = Modifier.size(72.dp).padding(bottom = 8.dp)) {
                        val cellSz = size.width / 5f
                        val cells = listOf(
                            // H-word: row 1 cols 0-4
                            0 to 0, 1 to 0, 2 to 0, 3 to 0, 4 to 0,
                            // V-word: col 2 rows 0-4
                            2 to 1, 2 to 2, 2 to 3, 2 to 4,
                            // H-word: row 2 cols 0-4
                            0 to 2, 1 to 2, 3 to 2, 4 to 2,
                            // H-word: row 4 cols 0-4
                            0 to 4, 1 to 4, 3 to 4, 4 to 4
                        )
                        cells.forEach { (cx, cy) ->
                            drawRoundRect(
                                color  = Color(0xFF6650A4),
                                topLeft = Offset(cx * cellSz + 1f, cy * cellSz + 1f),
                                size   = GeoSize(cellSz - 2f, cellSz - 2f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
                            )
                        }
                    }
                    // Title
                    Text(
                        "Welcome to",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        "My Personal Crossword",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        "Your crossword, your way!",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(bottom = 36.dp)
                    )

                    // Name field
                    OutlinedTextField(
                        value = playerName,
                        onValueChange = { playerName = sanitizeName(it) },
                        label = { Text("Enter Your Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f),
                            focusedLabelColor    = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor  = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                            focusedTextColor     = MaterialTheme.colorScheme.onPrimaryContainer,
                            unfocusedTextColor   = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    Button(
                        onClick = {
                            // Trim so "Kevin " and "Kevin" are the same profile.
                            playerName = normalizeName(playerName)
                            if (playerName.isNotBlank()) {
                                vibrateLight(context)
                                if (soundEnabled) SoundPlayer.playClick()
                                saveManager.ensurePlayer(playerName)
                                saveManager.setLastUser(playerName)
                                currentScore = saveManager.getScore(playerName)
                                currentCompleted = saveManager.getCompleted(playerName)
                                currentCellColorArgb = saveManager.getCellColorArgb(playerName)
                                currentBtnColorArgb  = saveManager.getButtonColorArgb(playerName)
                                musicVolume = saveManager.getMusicVolume()
                                appMode = AppMode.CATEGORY_SELECT
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp)
                            .height(52.dp)
                    ) { Text("START PLAYING", fontSize = 18.sp, fontWeight = FontWeight.Bold) }

                    // Credits
                    Spacer(Modifier.height(28.dp))
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(0.6f),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Written by Kevin Ernst",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Playtested by Verbal",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    // ── Saved profiles ─────────────────────────────────────────
                    // Use profileList state so deletes refresh the list without recompose tricks
                    val savedProfiles = profileList
                    if (savedProfiles.isNotEmpty()) {
                        Spacer(Modifier.height(20.dp))
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(0.8f),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Continue as...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(savedProfiles) { name ->
                                val score = saveManager.getScore(name)
                                val done  = saveManager.getCompleted(name)
                                Card(
                                    onClick = {
                                        playerName = name
                                        saveManager.setLastUser(name)
                                        currentScore = saveManager.getScore(name)
                                        currentCompleted = saveManager.getCompleted(name)
                                        currentCellColorArgb = saveManager.getCellColorArgb(name)
                                        currentBtnColorArgb  = saveManager.getButtonColorArgb(name)
                                        musicVolume = saveManager.getMusicVolume()
                                        viewingProfile = name
                                        appMode = AppMode.STATS
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = name.take(1).uppercase(),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                            Text(
                                                "Score: $score  •  Puzzles: $done",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                            )
                                        }
                                        IconButton(onClick = { confirmDeletePlayer = name }) {
                                            Icon(
                                                imageVector   = Icons.Default.Delete,
                                                contentDescription = "Delete $name",
                                                tint = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        AppMode.STATS -> {
            StatsScreen(
                playerName       = viewingProfile.ifBlank { playerName },
                saveManager      = saveManager,
                allCategoryNames = categories.toSet(),
                onPlay           = { appMode = AppMode.CATEGORY_SELECT },
                onResume         = { slot -> resumeSlot(slot) },
                onBack           = {
                    viewingProfile = ""
                    dailyPromptShown = false
                    appMode = AppMode.LOGIN
                }
            )
        }

        AppMode.CATEGORY_SELECT -> {
            // Cache derived prefs reads so they don't fire on every recompose
            // (timer tick, score update, etc.). The keys recompute these only
            // when the player changes or completes a puzzle.
            val cachedUsedWordCounts = remember(playerName, currentCompleted, categories, homeRefresh) {
                categories.associateWith { cat ->
                    saveManager.getUsedWords(playerName, cat).size
                }
            }
            val cachedInProgress = remember(playerName, currentCompleted, homeRefresh) {
                saveManager.getInProgressPuzzles(playerName)
            }
            val cachedDailyDone = remember(playerName, currentCompleted, homeRefresh) {
                saveManager.isDailyCompleted(playerName, DailyPuzzle.dateKey(System.currentTimeMillis()))
            }
            CategoryScreen(
                categories        = categories,
                playerName        = playerName,
                score             = currentScore,
                completed         = currentCompleted,
                currentStreak     = currentStreak,
                activeGameMode    = activeGameMode,
                // Mode tap = just set the mode. Tutorials/setup fire on START.
                onGameModeChange  = { newMode ->
                    activeGameMode = newMode
                    // Reset mode-specific state so a stale score from a prior
                    // session doesn't leak into the next puzzle. Without this,
                    // visiting STATS between mode switches stranded leftover
                    // vindP1Score / teamP1Score / tauntIndex values.
                    vindP1Score = 0; vindP2Score = 0
                    teamP1Score = 0; teamP2Score = 0
                    tauntIndex.intValue = 0
                    vindAssignedWord = null
                    vindPhase = VindicativePhase.PICK_OWN
                    teamCurrentPlayer = 0
                    vindCurrentPlayer = 0
                },
                player2Name       = player2Name,
                allEntries        = allEntries,
                usedWordCounts    = cachedUsedWordCounts,
                inProgressList    = cachedInProgress,
                onResume          = { slot -> resumeSlot(slot) },
                dailyDoneToday    = cachedDailyDone,
                onStartPlay       = {
                    when (activeGameMode) {
                        GameMode.SINGLE -> {
                            if (saveManager.isFirstSingle()) {
                                showSingleTutorial = true
                                // mark on dismiss, not on show — see tutorial confirm handlers
                            } else {
                                showVindCategoryDialog = true
                            }
                        }
                        // TEAM and VINDICTIVE: always re-open the player-2 setup
                        // dialog. It's the only place to pick Host / Join / Local
                        // (so we can't skip it just because player2Name happens to
                        // be set from a prior game), and re-confirming "who goes
                        // first" is one tap with the name pre-filled.
                        GameMode.TEAM -> {
                            if (saveManager.isFirstTeam()) {
                                showTeamTutorial = true
                                // mark on dismiss
                            } else {
                                showPlayer2SetupDialog = true
                            }
                        }
                        GameMode.VINDICTIVE -> {
                            if (saveManager.isFirstVindictive()) {
                                showVindictiveTutorial = true
                                // mark on dismiss
                            } else {
                                showPlayer2SetupDialog = true
                            }
                        }
                        GameMode.DAILY -> launchDailyPuzzle()
                    }
                },
                onDailyPuzzle     = { launchDailyPuzzle() },
                onChangeUser      = {
                    cleanupOnlineSession()
                    saveManager.setLastUser("")
                    playerName = ""
                    viewingProfile = ""
                    // Wipe puzzle-side state so the next user doesn't inherit it.
                    placedWords = emptyList()
                    gridCells = emptyList()
                    userInputs = emptyMap()
                    activeCategory = ""
                    combinedCategories = emptyList()
                    isDailyPuzzle = false
                    activeDailyKey = null
                    revealedCells = emptySet()
                    showResults = false
                    lastResult = null
                    if (activeGameMode == GameMode.DAILY) activeGameMode = modeBeforeDaily
                    elapsedSeconds = 0L
                    timerRunning = false
                    hintsUsedThisPuzzle = 0
                    teamP1Hints = 0
                    currentStreak = 0
                    tauntIndex.intValue = 0
                    vindP1Score = 0; vindP2Score = 0
                    teamP1Score = 0; teamP2Score = 0
                    teamCurrentPlayer = 0
                    vindCurrentPlayer = 0
                    vindAssignedWord = null
                    puzzleSolved = false
                    showConfetti = false
                    showTurnDialog = false
                    vindPassDialogVisible = false
                    pendingTurnDialog = false
                    wordToInput = null
                    showWrongFlash = false
                    streakMilestone = null
                    vindPhase = VindicativePhase.PICK_OWN
                    dailyPromptShown = false   // reset so prompt fires again on next login
                    appMode = AppMode.LOGIN
                },
                onQuit                = { (context as? Activity)?.finish() },
                onRequestPlayer2Setup = { showPlayer2SetupDialog = true },
                musicEnabled          = musicEnabled,
                soundEnabled          = soundEnabled,
                musicVolume           = musicVolume,
                onMusicToggle         = { musicEnabled = !musicEnabled },
                onVolumeChange        = { v ->
                    musicVolume = v
                    AmbientMusicPlayer.setVolume(v)
                    saveManager.setMusicVolume(v)
                },
                btnColorArgb          = currentBtnColorArgb
            )
        }

        AppMode.ONLINE_LOBBY -> {
            val lobbyGradient = Brush.verticalGradient(listOf(
                appBtnColor.copy(red = (appBtnColor.red + 0.15f).coerceIn(0f,1f),
                    green = (appBtnColor.green + 0.15f).coerceIn(0f,1f),
                    blue  = (appBtnColor.blue  + 0.15f).coerceIn(0f,1f)),
                appBtnColor, appBtnDim
            ))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                        .shadow(8.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(lobbyGradient)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (onlineRole == OnlineRole.HOST) {
                        Text("🌐 Online Game", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("Share this code with your opponent:", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                        val clipboard = LocalClipboardManager.current
                        var copied by remember { mutableStateOf(false) }
                        LaunchedEffect(copied) {
                            if (copied) { delay(1500L); copied = false }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    clipboard.setText(AnnotatedString(onlineCode))
                                    copied = true
                                    vibrateLight(context)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = onlineCode,
                                fontSize = 44.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 8.sp
                            )
                        }
                        Text(
                            if (copied) "✓ Copied to clipboard" else "Tap code to copy",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = if (copied) 1f else 0.75f),
                            fontWeight = if (copied) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(onlineStatus.ifEmpty { "Waiting for opponent to join…" },
                            fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f))
                    } else {
                        Text("🌐 Joining Game", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text(onlineStatus.ifEmpty { "Connecting…" }, fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                    GradientBtn("Cancel", redGradient, onClick = {
                        vibrateLight(context)
                        cleanupOnlineSession()
                        appMode = AppMode.CATEGORY_SELECT
                    }, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        AppMode.DASHBOARD -> {
            // Pick a top-bar accent that stays readable against the surface
            // even when the user has chosen a near-white or near-black brand
            // color. Used for the back arrow, Puzzles count, Score, and
            // Settings icon.
            val brandLum    = cellLuminance(appBtnColor)
            val safeAccent  = if (brandLum < 0.2f || brandLum > 0.7f)
                MaterialTheme.colorScheme.onSurface
            else appBtnColor
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = {
                            Column {
                                // Title: category + mode label for team/vindictive
                                val modeLabel = when (activeGameMode) {
                                    GameMode.TEAM       -> " • 🤝 Team"
                                    GameMode.VINDICTIVE -> " • ⚔️ Vind"
                                    else                -> ""
                                }
                                val titleText = activeCategory.lowercase().replaceFirstChar { it.uppercase() } + modeLabel
                                Text(titleText, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                // Subtitle: names for multiplayer, word count + time for single
                                val mins = elapsedSeconds / 60
                                val secs = elapsedSeconds % 60
                                val timeStr = "%d:%02d".format(mins, secs)
                                val subtitle = when {
                                    isGenerating -> "Generating…"
                                    activeGameMode == GameMode.TEAM ->
                                        "$playerName vs $player2Name  •  $timeStr"
                                    activeGameMode == GameMode.VINDICTIVE ->
                                        "$playerName ⚔ $player2Name  •  $timeStr"
                                    else ->
                                        "${placedWords.size} words  •  ${activeDifficulty.label}  •  $timeStr"
                                }
                                Text(subtitle, fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1)
                            }
                        },
                        navigationIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { goHome() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Save and go back", tint = safeAccent)
                                }
                                Text("Puzzles: $currentCompleted", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = safeAccent)
                            }
                        },
                        actions = {
                            // Persistent streak chip — appears once a streak begins.
                            // Theme-aware: uses tertiaryContainer/onTertiaryContainer
                            // so the chip stays readable in both light + dark schemes.
                            if (currentStreak > 0) {
                                Box(
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        "🔥 $currentStreak",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                            Text("Score: $currentScore", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = safeAccent)
                            IconButton(onClick = { showSettings = true }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = safeAccent)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            // Stronger 14% wash so the bar is distinguishable
                            // from the page background in dark mode.
                            containerColor = appBtnColor.copy(alpha = 0.14f)
                        )
                    )

                    Box(modifier = Modifier.weight(1.4f).fillMaxWidth()) {
                        if (isGenerating) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 40.dp, vertical = 32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                        val loadingDots = rememberAnimatedDots()
                                        Text(
                                            "Weaving Words$loadingDots",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            "Building your puzzle",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                        )
                                    }
                                }
                            }
                        } else {
                            PuzzleScreenReference(
                                cells            = gridCells,
                                userInputs       = userInputs,
                                bgColor          = currentBgColor,
                                bgImageName      = currentBgImageName,
                                cellColor        = currentCellColor,
                                placedWords      = placedWords,
                                centreOnWord     = centreOnWord,
                                animatingCell    = animatingCell,
                                highlightedWord  = highlightedWord,
                                onCentred        = { centreOnWord = null },
                                onClearWords = { wordsToClear ->
                                    val newMap = userInputs.toMutableMap()
                                    wordsToClear.forEach { w ->
                                        for (i in w.word.indices) {
                                            val pos = if (w.isHorizontal) Pair(w.startX + i, w.startY)
                                            else Pair(w.startX, w.startY + i)
                                            newMap.remove(pos)
                                        }
                                    }
                                    userInputs = newMap
                                }
                            )
                        }
                    }

                    // ── Hint button ───────────────────────────────────────────────
                    // Target = the clue last tapped in the list. Reveals the first wrong/empty
                    // letter of that word, costs Economy.HINT_COST lifetime points (charged once,
                    // right now), and is synced to the other device in online games.
                    if (!puzzleSolved && !isGenerating) {
                        val hintWord    = selectedHintWord?.takeIf { !isWordSolved(it, userInputs) }
                        val canAfford   = currentScore >= Economy.HINT_COST
                        val hintEnabled = hintWord != null && canAfford
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .shadow(if (hintEnabled) 4.dp else 0.dp, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (hintEnabled) appBtnGradient
                                    else Brush.verticalGradient(listOf(Color.Gray.copy(alpha = 0.22f), Color.Gray.copy(alpha = 0.22f)))
                                )
                                .clickable(enabled = hintEnabled, onClickLabel = "Reveal a letter") {
                                    val word = hintWord ?: return@clickable
                                    val (pos, ch) = pickHintCell(word, userInputs) ?: return@clickable
                                    vibrateLight(context)
                                    if (soundEnabled) SoundPlayer.playClick()
                                    userInputs    = userInputs + (pos to ch)
                                    revealedCells = revealedCells + pos
                                    currentScore  = (currentScore - Economy.HINT_COST).coerceAtLeast(0)
                                    saveManager.addScore(playerName, -Economy.HINT_COST)
                                    hintsUsedThisPuzzle++
                                    if (activeGameMode == GameMode.TEAM) {
                                        // Online, the hint belongs to this device's player; locally to whoever has the turn.
                                        val t = teamSnapshot()
                                        applyTeam(TeamRules.onHint(if (isOnlineGame) t.copy(turn = myIndex()) else t)
                                            .copy(turn = t.turn))
                                    }
                                    if (isOnlineGame) FirebaseGameManager.writeInput(onlineCode, pos.first, pos.second, ch)
                                    if (isWordSolved(word, userInputs)) selectedHintWord = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (hintEnabled) {
                                Box(Modifier.fillMaxWidth().height(1.5.dp)
                                    .background(Color.White.copy(alpha = 0.25f))
                                    .align(Alignment.TopCenter))
                            }
                            val dirLabel = if (hintWord?.isHorizontal == true) "Across" else "Down"
                            Text(
                                when {
                                    hintEnabled       -> "💡  Reveal a letter in ${hintWord!!.number} $dirLabel  (−${Economy.HINT_COST} pt · $currentScore available)"
                                    hintWord == null  -> "💡  Tap a clue below to choose a hint"
                                    else              -> "💡  Hints cost ${Economy.HINT_COST} pt — earn points by solving puzzles"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (hintEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // ── Vindictive / Team mode banner — inside the Column layout ──
                    if (activeGameMode == GameMode.VINDICTIVE && !isGenerating && !puzzleSolved) {
                        val vCurrentName = if (vindCurrentPlayer == 0) onlineP0Name else onlineP1Name
                        val vOtherName   = if (vindCurrentPlayer == 0) onlineP1Name else onlineP0Name
                        val isWaiting    = vindPhase == VindicativePhase.OPPONENT_WAIT
                        val bannerBg     = if (isWaiting) Color(0xFFB71C1C) else MaterialTheme.colorScheme.tertiaryContainer
                        val bannerFg     = if (isWaiting) Color.White else MaterialTheme.colorScheme.onTertiaryContainer
                        // Online, the other device watches instead of acting.
                        val iAct         = VindictiveRules.canAct(vindSnapshot(), isOnlineGame, myIndex())
                        val vPhaseText   = when {
                            !iAct -> when (vindPhase) {
                                VindicativePhase.PICK_OWN      -> "$vCurrentName is picking a clue to answer…"
                                VindicativePhase.ASSIGN_CLUE   -> "$vCurrentName is choosing a clue for you 😈"
                                VindicativePhase.OPPONENT_WAIT ->
                                    if (vindOpponentCountdown > 0) "$vCurrentName is answering your clue… (${vindOpponentCountdown}s)"
                                    else "Time's up — $vCurrentName is deciding whether to answer or pass…"
                            }
                            vindPhase == VindicativePhase.PICK_OWN    -> "$vCurrentName — First turn! Double-tap a clue to pick it for yourself."
                            vindPhase == VindicativePhase.ASSIGN_CLUE -> "$vCurrentName — Double-tap a clue to assign to $vOtherName 👆"
                            showTurnDialog -> "$vCurrentName — Your clue is waiting… hit I'm Ready when you have the phone!"
                            vindOpponentCountdown > 0 ->
                                "$vCurrentName — ${vindAssignedWord?.clue ?: "?"} — Tap to answer! (${vindOpponentCountdown}s)"
                            else -> "$vCurrentName — Time's up! Answer or pass."
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .background(bannerBg)
                                .then(if (isWaiting && vindAssignedWord != null && iAct)
                                    Modifier.clickable(onClickLabel = "Answer your clue") {
                                        vindPassDialogVisible = false
                                        wordToInput = vindAssignedWord   // keeps anything already typed
                                    }
                                else Modifier)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(vPhaseText, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = bannerFg, textAlign = TextAlign.Center)
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                Text("$onlineP0Name: ${vindP1Score}pts", fontSize = 11.sp, color = bannerFg)
                                Text("$onlineP1Name: ${vindP2Score}pts", fontSize = 11.sp, color = bannerFg)
                            }
                            if (isWaiting && vindAssignedWord != null && iAct) {
                                Text("👆 Tap this bar to answer",
                                    fontSize = 11.sp, color = bannerFg.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    } else if (activeGameMode == GameMode.TEAM && !isGenerating && !puzzleSolved) {
                        val tCurrentName = if (teamCurrentPlayer == 0) onlineP0Name else onlineP1Name
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val myTurn = !isOnlineGame || teamCurrentPlayer == myIndex()
                            Text(if (myTurn) "🤝 ${tCurrentName}'s turn" else "⏳ Waiting for $tCurrentName…",
                                fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("$onlineP0Name  +  $onlineP1Name",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }

                    // Next Song bar — anchored just above nav bar inside the clue list area
                    Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
                            // In OPPONENT_WAIT: show only assigned clue (hidden while turn dialog is open)
                            val visibleWords = if (activeGameMode == GameMode.VINDICTIVE &&
                                vindPhase == VindicativePhase.OPPONENT_WAIT &&
                                vindAssignedWord != null && !showTurnDialog)
                                listOf(vindAssignedWord!!)
                            else if (activeGameMode == GameMode.VINDICTIVE &&
                                vindPhase == VindicativePhase.OPPONENT_WAIT &&
                                showTurnDialog)
                                emptyList()  // hide all clues while phone is being passed
                            else placedWords
                            FullHintsList(
                                words           = visibleWords,
                                userInputs      = userInputs,
                                highlightedWord = highlightedWord,
                                selectedWord    = selectedHintWord,
                                onSingleTap     = { word ->
                                    if (!isGenerating) {
                                        centreOnWord     = word
                                        highlightedWord  = word
                                        selectedHintWord = word
                                    }
                                },
                                onDoubleTap     = { word ->
                                    if (puzzleSolved || isGenerating || isWordSolved(word, userInputs)) return@FullHintsList
                                    // Online: only the player whose turn it is may answer / pick / assign.
                                    if (isOnlineGame) {
                                        val mine = when (activeGameMode) {
                                            GameMode.VINDICTIVE -> VindictiveRules.canAct(vindSnapshot(), true, myIndex())
                                            GameMode.TEAM       -> teamCurrentPlayer == myIndex()
                                            else                -> true
                                        }
                                        if (!mine) return@FullHintsList
                                    }
                                    // During OPPONENT_WAIT: only the assigned clue can be answered
                                    if (activeGameMode == GameMode.VINDICTIVE &&
                                        vindPhase == VindicativePhase.OPPONENT_WAIT &&
                                        word != vindAssignedWord) return@FullHintsList
                                    // Assigning a clue doesn't open the answer box.
                                    if (activeGameMode == GameMode.VINDICTIVE && vindPhase == VindicativePhase.ASSIGN_CLUE) {
                                        assignClue(word)
                                        return@FullHintsList
                                    }
                                    wordToInput      = word
                                    highlightedWord  = word
                                    selectedHintWord = word
                                    inputText        = ""
                                }
                            )
                        }   // end FullHintsList Box
                        // ── Next Song strip ─────────────────────────────────────
                        if (musicEnabled) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .navigationBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Text(
                                    "♪ ${AmbientMusicPlayer.currentTrackName.ifEmpty { "No track" }.take(32)}",
                                    fontSize = 11.sp,
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
                                )
                                TextButton(
                                    onClick  = { AmbientMusicPlayer.next() },
                                    modifier = Modifier.padding(start = 8.dp)
                                ) { Text("Next ⏭", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                            }
                        } else {
                            // Still need nav bar padding when music is off
                            Spacer(Modifier.navigationBarsPadding())
                        }
                    }   // end Column wrapping FullHintsList + Next Song
                }   // end main Column

                // ── Full-screen overlays — inside the Dashboard Box ──────────────
                // Wrong answer banner — red border-glow around content with a centered card.
                // Less jarring than a full red wash; plays nicely with the rest of the UI.
                AnimatedVisibility(visible = showWrongFlash, enter = fadeIn(), exit = fadeOut()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            // Vivid red wash, alpha tuned to be readable against
                            // both light and dark surfaces. Color.Red at 18%
                            // muddied to a brown-purple in dark mode.
                            .background(Color(0xFFD32F2F).copy(alpha = 0.28f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                            modifier = Modifier.padding(horizontal = 36.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 28.dp, vertical = 22.dp)
                            ) {
                                Text(
                                    "WRONG!",
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    letterSpacing = 2.sp
                                )
                                if (activeGameMode == GameMode.VINDICTIVE) {
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        vindictiveTaunt(tauntIndex.intValue, nameOf(wrongAnswererIndex), nameOf(1 - wrongAnswererIndex)),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.95f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // "X is answering…" status banner (online only)
                if (isOnlineGame && remoteIsAnswering && !puzzleSolved) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .align(Alignment.TopCenter),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = appBtnColor.copy(alpha = 0.88f),
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 4.dp
                        ) {
                            Text(
                                "✏️  $remoteAnsweringName is entering an answer…",
                                fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Vindictive countdown overlay — shown on BOTH devices (assigner waits, answerer races)
                if (activeGameMode == GameMode.VINDICTIVE &&
                    vindOpponentCountdown > 0 && !vindPassDialogVisible && !puzzleSolved &&
                    !showTurnDialog && (vindPhase == VindicativePhase.OPPONENT_WAIT || (isOnlineGame && vindAssignedWord != null))) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 200.dp),  // push above clue list
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Card(
                            modifier  = Modifier.size(80.dp),
                            colors    = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C)),
                            elevation = CardDefaults.cardElevation(8.dp),
                            shape     = androidx.compose.foundation.shape.CircleShape
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("$vindOpponentCountdown",
                                    fontSize = 32.sp, fontWeight = FontWeight.ExtraBold,
                                    color = Color.White)
                            }
                        }
                    }
                }

                // Streak milestone
                AnimatedVisibility(visible = streakMilestone != null, enter = fadeIn(), exit = fadeOut()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Card(modifier = Modifier.padding(40.dp), elevation = CardDefaults.cardElevation(8.dp)) {
                            Text(streakMilestone ?: "", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(24.dp), textAlign = TextAlign.Center)
                        }
                    }
                }

                // Confetti
                if (showConfetti) { ConfettiOverlay() }

            }   // end Dashboard Box
        }
    }
    }   // end AnimatedContent

    if (wordToInput != null && !puzzleSolved && appMode == AppMode.DASHBOARD) {
        val word = wordToInput!!
        AlertDialog(
            onDismissRequest = { wordToInput = null },
            title = { Text("Enter Answer") },
            text = {
                Column {
                    Text("${word.number} ${if (word.isHorizontal) "Across" else "Down"}: ${word.clue}", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { v -> inputText = v.uppercase().filter { it in 'A'..'Z' }.take(word.word.length) },
                        label = { Text("(${word.word.length} Letters)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Password,   // suppresses suggestions/autocorrect on most IMEs
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            if (inputText.length == word.word.length) submitAnswer(word, inputText)
                        }),
                        visualTransformation = VisualTransformation.None,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { submitAnswer(word, inputText) },
                    enabled  = inputText.length == word.word.length,
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = appBtnColor, contentColor = Color.White),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Submit", fontSize = 15.sp, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                GradientBtn("Cancel", appBtnGradient, onClick = { wordToInput = null }, modifier = Modifier.fillMaxWidth())
            }
        )
    }

    // ── ONLINE JOIN DIALOG ────────────────────────────────────────────────────────
    if (showOnlineJoin) {
        AlertDialog(
            onDismissRequest = { showOnlineJoin = false; onlineJoinError = "" },
            title = { Text("🌐 Join Online Game", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter the 6-letter code from the host:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = onlineJoinInput,
                        onValueChange = { onlineJoinInput = it.uppercase().take(6); onlineJoinError = "" },
                        label = { Text("Game Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (onlineJoinError.isNotEmpty()) {
                        Text(onlineJoinError, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                GradientBtn("Join", appBtnGradient,
                    enabled = onlineJoinInput.length == 6,
                    onClick = {
                        vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                        val code = onlineJoinInput
                        FirebaseGameManager.signInAnonymously { ok, err ->
                            if (!ok) { onlineJoinError = err ?: "Couldn't connect."; return@signInAnonymously }
                            FirebaseGameManager.joinGame(
                                code, playerName,
                                onSuccess = { mode, category, difficulty, hostName ->
                                    activeGameMode    = runCatching { GameMode.valueOf(mode) }.getOrDefault(GameMode.TEAM)
                                    activeCategory    = category
                                    activeDifficulty  = runCatching { Difficulty.valueOf(difficulty) }.getOrDefault(Difficulty.MEDIUM)
                                    combinedCategories = emptyList()
                                    activeDailyKey    = null
                                    player2Name       = hostName
                                    isOnlineGame      = true
                                    onlineRole        = OnlineRole.GUEST
                                    onlineCode        = code
                                    onlineStatus      = "Waiting for $hostName to start the puzzle…"
                                    showOnlineJoin    = false
                                    onlineJoinInput   = ""
                                    appMode           = AppMode.ONLINE_LOBBY
                                },
                                onError = { msg -> onlineJoinError = msg }
                            )
                        }
                    }, modifier = Modifier.fillMaxWidth())
            },
            dismissButton = {
                GradientBtn("Cancel", appBtnGradient, onClick = {
                    vibrateLight(context); showOnlineJoin = false; onlineJoinError = ""; onlineJoinInput = ""
                }, modifier = Modifier.fillMaxWidth())
            }
        )
    }

    // ── SETTINGS BOTTOM SHEET ─────────────────────────────────────────────────────
    if (showSettings) {
        val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState       = settingsSheetState,
            modifier         = Modifier.fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp))

                // Sound toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Sound Effects", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text(
                            if (soundEnabled) "Plays taps, correct, wrong, and celebration cues"
                            else "Off",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = {
                            soundEnabled = it
                            vibrateLight(context)
                            if (it) SoundPlayer.playClick()
                        }
                    )
                }

                // Music card — same gold style as category screen
                val settingsGold = Brush.verticalGradient(
                    listOf(Color(0xFFFFE566), Color(0xFFFFD700), Color(0xFFCCAA00))
                )
                val settingsTrackColor = Color(0xFF3A2A00)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(settingsGold)
                ) {
                    Box(Modifier.fillMaxWidth().height(1.5.dp)
                        .background(Color.White.copy(alpha = 0.45f))
                        .align(Alignment.TopCenter))
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text(
                            "♪  ${AmbientMusicPlayer.currentTrackName.ifEmpty { "No track" }.take(36)}",
                            fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            color = settingsTrackColor, maxLines = 1,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(onClick = { AmbientMusicPlayer.previous() }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = settingsTrackColor)
                            }
                            IconButton(onClick = { musicEnabled = !musicEnabled }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = if (AmbientMusicPlayer.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (musicEnabled) "Pause" else "Play",
                                    tint = settingsTrackColor
                                )
                            }
                            IconButton(onClick = { AmbientMusicPlayer.next() }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = settingsTrackColor)
                            }
                            Icon(Icons.Default.VolumeDown, contentDescription = null, tint = settingsTrackColor.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                            Slider(
                                value = musicVolume,
                                onValueChange = { v ->
                                    musicVolume = v
                                    AmbientMusicPlayer.setVolume(v)
                                    saveManager.setMusicVolume(v)
                                    if (!musicEnabled) musicEnabled = true
                                },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = settingsTrackColor,
                                    activeTrackColor = settingsTrackColor,
                                    inactiveTrackColor = settingsTrackColor.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                            )
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = settingsTrackColor.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                HorizontalDivider()

                // ── Customize section ──────────────────────────────────────────
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    // Dropdown trigger button
                    OutlinedButton(
                        onClick = { showCustomize = !showCustomize },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎨  Customize", fontSize = 15.sp)
                            Icon(
                                imageVector = if (showCustomize) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }
                    }

                    // Dropdown contents
                    AnimatedVisibility(visible = showCustomize) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(0.dp, 0.dp, 12.dp, 12.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Cell Color option
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        showSettings  = false
                                        showCustomize = false
                                        showColorPicker = true
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(currentCellColor)
                                        .border(1.5.dp, Color.Gray, CircleShape)
                                )
                                Column {
                                    Text("Cell Color", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    Text("Change the color of crossword cells",
                                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))

                            // Button Color option
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        showSettings  = false
                                        showCustomize = false
                                        showBtnColorPicker = true
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(currentBtnColorArgb.toLong() and 0xFFFFFFFFL))
                                        .border(1.5.dp, Color.Gray, RoundedCornerShape(8.dp))
                                )
                                Column {
                                    Text("Button Color", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    Text("Change the color of category buttons",
                                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))

                            // Background Image option
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        showSettings  = false
                                        showCustomize = false
                                        showBgPicker  = true
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.DarkGray),
                                    contentAlignment = Alignment.Center
                                ) { Text("🖼", fontSize = 16.sp) }
                                Column {
                                    Text("Background Image", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    Text("Choose the puzzle background",
                                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Save & Quit (online: leave — a two-device game can't be resumed)
                Button(
                    onClick = {
                        showSettings = false
                        goHome()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (isOnlineGame) "🚪  Leave Online Game" else "💾  Save & Quit to Menu", fontSize = 16.sp)
                }

                OutlinedButton(
                    onClick = { showSettings = false },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Resume Puzzle") }

                // How to play button
                OutlinedButton(
                    onClick = { showSettings = false; showHowToPlay = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("❓ How to Play") }

                HorizontalDivider()

                // About section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🎮", fontSize = 20.sp)
                        Column {
                            Text(
                                "My Personal Crossword",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Version 1.0",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "A Hot Attic Games production",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Written by Kevin Ernst",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        "Playtested by Verbal",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Your crossword, your way.",
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }

    // ── VINDICTIVE PASS / ANSWER DIALOG ─────────────────────────────────────────────
    // Offered on the answering player's device when their timer runs out.
    if (vindPassDialogVisible && appMode == AppMode.DASHBOARD &&
        activeGameMode == GameMode.VINDICTIVE && !puzzleSolved &&
        vindPhase == VindicativePhase.OPPONENT_WAIT && vindAssignedWord != null) {
        val opName = nameOf(vindCurrentPlayer)
        AlertDialog(
            onDismissRequest = { /* can't dismiss — must choose */ },
            title = {
                Text("⏰ Time's up, $opName!", fontWeight = FontWeight.Bold,
                    fontSize = 18.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("\"${vindAssignedWord?.clue ?: ""}\"",
                        fontSize = 16.sp, fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Text("${vindAssignedWord?.word?.length ?: 0} letters",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("Answer it: +${Economy.VIND_ASSIGNED_CORRECT} if right, ${Economy.VIND_ASSIGNED_WRONG} if wrong.\nOr pass: ${Economy.VIND_PASS}.",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                }
            },
            confirmButton = {
                GradientBtn("Answer It  ▶", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                    vindPassDialogVisible = false
                    wordToInput = vindAssignedWord     // keeps whatever was already typed
                }, modifier = Modifier.fillMaxWidth())
            },
            dismissButton = {
                GradientBtn("Pass  (${Economy.VIND_PASS} pt)", redGradient, onClick = {
                    vibrateLight(context)
                    passAssigned()
                }, modifier = Modifier.fillMaxWidth())
            }
        )
    }

    // ── HOW TO PLAY DIALOG ──────────────────────────────────────────────────────
    if (showHowToPlay) {
        AlertDialog(
            onDismissRequest = { showHowToPlay = false },
            title = { Text("Welcome to My Personal Crossword! 🧩", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        "👆 Tap a clue" to "Single-tap to highlight it on the grid.",
                        "✍️ Answer" to "Double-tap a clue to type your answer.",
                        "💡 Hint" to "Tap the lightbulb to reveal one letter. Costs 1 point.",
                        "🔀 Combine" to "Merge multiple categories into one big puzzle.",
                        "📅 Daily" to "A new puzzle every day using all categories.",
                        "🤝 Team Mode" to "Both players work together and take turns answering.",
                        "⚔️ Vindictive" to "Each player picks clues for the other. Pass or answer!"
                    ).forEach { (title, desc) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                modifier = Modifier.width(100.dp))
                            Text(desc, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {
                GradientBtn("Let's Play! 🎉", appBtnGradient, onClick = { vibrateLight(context); if (soundEnabled) SoundPlayer.playClick(); showHowToPlay = false }, modifier = Modifier.fillMaxWidth())
            },
            dismissButton = {}
        )
    }

    // ── VINDICTIVE MODE TUTORIAL ──────────────────────────────────────────────────
    if (showVindictiveTutorial) {
        AlertDialog(
            onDismissRequest = { showVindictiveTutorial = false },
            title = { Text("⚔️ How Vindictive Mode Works", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        "1️⃣ First turn" to "The starting player picks any clue and tries to answer it. +1 if correct, -1 if wrong.",
                        "2️⃣ Assign" to "That same player picks a clue for the other player and passes the phone.",
                        "3️⃣ Opponent" to "The next player has a timer (you'll pick 15s, 30s, or 60s next) to answer. Double-tap the clue to answer early.",
                        "✅ Correct" to "+1 point. Now pick a clue for the other player.",
                        "❌ Wrong" to "-2 points. Still pick a clue for the other player.",
                        "⏭ Pass" to "-1 point. Still pick a clue for the other player.",
                        "🏆 Win" to "The game ends when the puzzle is complete. Highest score wins!"
                    ).forEach { (title, desc) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                modifier = Modifier.width(90.dp))
                            Text(desc, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {
                GradientBtn("Got it — Set Up Players ▶", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                    saveManager.markVindictiveSeen()
                    showVindictiveTutorial = false; showPlayer2SetupDialog = true
                }, modifier = Modifier.fillMaxWidth())
            },
            dismissButton = {}
        )
    }

    // ── SINGLE PLAYER TUTORIAL ────────────────────────────────────────────────────
    if (showSingleTutorial) {
        AlertDialog(
            onDismissRequest = { showSingleTutorial = false },
            title = { Text("👤 Single Player Mode", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        "👆 Single-tap"  to "Highlight a clue on the grid and scroll to it.",
                        "✍️ Double-tap"  to "Open the answer box for that clue.",
                        "💡 Hint"        to "Reveals one random letter in the selected word. Costs 1 point.",
                        "🔴 Wrong cell"  to "Tap an incorrect cell on the grid to erase it.",
                        "🏆 Scoring"     to "Points depend on difficulty. Hints reduce your score.",
                        "💾 Auto-save"   to "Progress is saved when you hit back or quit."
                    ).forEach { (title, desc) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                modifier = Modifier.width(100.dp))
                            Text(desc, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {
                GradientBtn("Got it — Let's Play! 🎉", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                    saveManager.markSingleSeen()
                    showSingleTutorial = false
                    showVindCategoryDialog = true
                }, modifier = Modifier.fillMaxWidth())
            },
            dismissButton = {}
        )
    }

    // ── TEAM MODE TUTORIAL ────────────────────────────────────────────────────────
    if (showTeamTutorial) {
        AlertDialog(
            onDismissRequest = { showTeamTutorial = false },
            title = { Text("🤝 How Team Mode Works", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        "🤝 Goal"        to "Both players work together to solve the same puzzle.",
                        "🔄 Take turns"  to "Players alternate answering clues — pass the phone each turn.",
                        "✅ Correct"     to "+1 answer credit for that player. Both earn full points at puzzle end.",
                        "💡 Hints"       to "Either player can use hints. Each costs 1 point from the final score.",
                        "🏆 Win"         to "Puzzle complete when all words are solved — both players share the reward!"
                    ).forEach { (title, desc) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                modifier = Modifier.width(90.dp))
                            Text(desc, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {
                GradientBtn("Got it — Set Up Players ▶", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                    saveManager.markTeamSeen()
                    showTeamTutorial = false; showPlayer2SetupDialog = true
                }, modifier = Modifier.fillMaxWidth())
            },
            dismissButton = {}
        )
    }

    // ── DAILY PROMPT (first login each session) ───────────────────────────────────
    if (showDailyPrompt) {
        AlertDialog(
            onDismissRequest = { showDailyPrompt = false },
            title = { Text("📅 Daily Puzzle Available!", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("A new crossword is generated every day using all categories — 20 words, 20 points!")
                    Spacer(Modifier.height(2.dp))
                    Text("Want to jump straight into today's puzzle?", fontWeight = FontWeight.Medium)
                    Text("(You can also find it as the yellow Daily button in the category list.)",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                GradientBtn("📅 Yes, play today's puzzle!", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                    showDailyPrompt = false; showDailyInstructions = true
                }, modifier = Modifier.fillMaxWidth())
            },
            dismissButton = {
                GradientBtn("Maybe later", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick(); showDailyPrompt = false
                }, modifier = Modifier.fillMaxWidth())
            }
        )
    }

    // ── DAILY INSTRUCTIONS ────────────────────────────────────────────────────────
    if (showDailyInstructions) {
        AlertDialog(
            onDismissRequest = { showDailyInstructions = false },
            title = { Text("📅 Daily Puzzle", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        "📅 Fresh daily"   to "A brand-new puzzle every day — everyone gets the same one.",
                        "🗂 All categories" to "Words drawn from every category in your list.",
                        "⭐ 20 words"       to "Expert-level grid worth 20 points.",
                        "🏆 Score"          to "Hints reduce your final score — try to go hint-free!"
                    ).forEach { (title, desc) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                modifier = Modifier.width(105.dp))
                            Text(desc, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {
                GradientBtn("Let's Go! 📅", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                    showDailyInstructions = false; launchDailyPuzzle()
                }, modifier = Modifier.fillMaxWidth())
            },
            dismissButton = {
                GradientBtn("Cancel", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick(); showDailyInstructions = false
                }, modifier = Modifier.fillMaxWidth())
            }
        )
    }

    // ── TURN HANDOFF DIALOG ──────────────────────────────────────────────────────
    if (showTurnDialog && appMode == AppMode.DASHBOARD) {
        AlertDialog(
            onDismissRequest = { showTurnDialog = false },
            title = { Text(turnDialogTitle, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    turnDialogMessage.split("\n").forEachIndexed { idx, line ->
                        Text(line, fontSize = if (line.contains("|")) 14.sp else 18.sp,
                            fontWeight = if (idx == 0) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                            color = if (line.contains("|")) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified)
                    }
                }
            },
            confirmButton = {
                GradientBtn("I'm Ready  ▶", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick(); showTurnDialog = false
                }, modifier = Modifier.fillMaxWidth())
            },
            dismissButton = {}
        )
    }

    // ── PLAYER 2 SETUP DIALOG ────────────────────────────────────────────────────
    if (showPlayer2SetupDialog) {
        var p2NameInput by remember { mutableStateOf(player2Name) }
        var whoIsP1     by remember { mutableIntStateOf(0) }
        val modeLabel = if (activeGameMode == GameMode.TEAM) "🤝 Team Mode Setup" else "⚔️ Vindictive Mode Setup"
        AlertDialog(
            onDismissRequest = { showPlayer2SetupDialog = false },
            title = { Text(modeLabel, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    // ── Two devices (online) — shown first, most prominent ─
                    Text("Two devices (online)", fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Host sets the rules. Guest just enters the code.",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GradientBtn("🌐 Host Game", appBtnGradient, modifier = Modifier.weight(1f),
                            onClick = {
                                vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                                showPlayer2SetupDialog = false
                                val hostMode = activeGameMode
                                val hostDiff = activeDifficulty
                                FirebaseGameManager.signInAnonymously { ok, err ->
                                    if (!ok) { onlineNotice = err; return@signInAnonymously }
                                    FirebaseGameManager.createGame(playerName, hostMode, hostDiff) { code, error ->
                                        if (code == null) { onlineNotice = error; return@createGame }
                                        isOnlineGame      = true
                                        onlineRole        = OnlineRole.HOST
                                        onlineCode        = code
                                        onlineStatus      = ""
                                        vindCurrentPlayer = 0      // host opens
                                        teamFirstPlayer   = 0
                                        if (hostMode == GameMode.VINDICTIVE) showVindTimerDialog = true
                                        else showVindCategoryDialog = true
                                    }
                                }
                            })
                        GradientBtn("🌐 Join Game", appBtnGradient, modifier = Modifier.weight(1f),
                            onClick = {
                                vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                                showPlayer2SetupDialog = false
                                onlineJoinInput = ""
                                onlineJoinError = ""
                                showOnlineJoin  = true
                            })
                    }

                    HorizontalDivider()

                    // ── Same device (pass the phone) ──────────────────────
                    Text("Same device (pass the phone)", fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = p2NameInput,
                        onValueChange = { p2NameInput = sanitizeName(it) },
                        label = { Text("Other Player's Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Who goes first?", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    listOf(0 to playerName, 1 to p2NameInput.ifBlank { "Other Player" }).forEach { (idx, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { whoIsP1 = idx }
                                .background(if (whoIsP1 == idx) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(selected = whoIsP1 == idx, onClick = { whoIsP1 = idx })
                            Text(name, fontSize = 15.sp,
                                color = if (whoIsP1 == idx) MaterialTheme.colorScheme.primary else Color.Unspecified)
                        }
                    }
                }
            },
            confirmButton = {
                GradientBtn("Play Local", appBtnGradient,
                    enabled = p2NameInput.isNotBlank(),
                    onClick = {
                        if (p2NameInput.isNotBlank()) {
                            vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                            player2Name = p2NameInput.trim()
                            vindCurrentPlayer = whoIsP1
                            teamFirstPlayer   = whoIsP1
                            saveManager.ensurePlayer(player2Name)
                            showPlayer2SetupDialog = false
                            if (activeGameMode == GameMode.VINDICTIVE) showVindTimerDialog = true
                            else showVindCategoryDialog = true
                        }
                    }, modifier = Modifier.fillMaxWidth())
            },
            dismissButton = {
                GradientBtn("Cancel", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                    showPlayer2SetupDialog = false
                }, modifier = Modifier.fillMaxWidth())
            }
        )
    }

    // ── VINDICTIVE TIMER SELECTION DIALOG ────────────────────────────────────────────
    if (showVindTimerDialog) {
        AlertDialog(
            onDismissRequest = { showVindTimerDialog = false },
            title = { Text("⏱ Answer Time Limit", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("How long does each player get to answer?",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    listOf(15 to "⚡ 15 seconds — Lightning round",
                        30 to "🕐 30 seconds — Standard",
                        60 to "🐢 60 seconds — Relaxed").forEach { (secs, label) ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth().height(52.dp)
                                .shadow(4.dp, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .background(appBtnGradient)
                                .clickable {
                                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                                    vindTimerSeconds = secs
                                    showVindTimerDialog = false
                                    showVindCategoryDialog = true
                                    if (isOnlineGame) FirebaseGameManager.writeState(onlineCode, mapOf("timerSeconds" to secs))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(Modifier.fillMaxWidth().height(1.5.dp).background(Color.White.copy(alpha = 0.25f)).align(Alignment.TopCenter))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                if (vindTimerSeconds == secs)
                                    Text("✓", fontSize = 14.sp, color = Color.White)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                GradientBtn("Cancel", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick(); showVindTimerDialog = false
                }, modifier = Modifier.fillMaxWidth())
            }
        )
    }

    // ── POST-SETUP CATEGORY SELECTION DIALOG ─────────────────────────────────────
    // Shown after Player 2 setup (team + vindictive): pick category then difficulty
    if (showVindCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showVindCategoryDialog = false },
            title = { Text("Choose Category", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Combine button — opens a multi-select dialog rather than
                    // navigating away from the current setup flow.
                    GradientBtn("🔀 Combine Categories…", appBtnGradient, onClick = {
                        vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                        showVindCategoryDialog = false
                        combineSelection = emptyList()
                        showMultiCategoryDialog = true
                    }, modifier = Modifier.fillMaxWidth())
                    HorizontalDivider()
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(categories) { cat ->
                            GradientBtn(cat, appBtnGradient, onClick = {
                                vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                                showVindCategoryDialog = false
                                difficultyPickCategory = cat
                            }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                GradientBtn("Cancel", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick(); showVindCategoryDialog = false
                }, modifier = Modifier.fillMaxWidth())
            }
        )
    }

    // ── MULTI-CATEGORY SELECT DIALOG ─────────────────────────────────────────────
    // Toggle multiple categories, then advance to the difficulty picker.
    if (showMultiCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showMultiCategoryDialog = false },
            title = { Text("🔀 Combine Categories", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (combineSelection.isEmpty())
                            "Pick two or more categories to combine into one puzzle."
                        else
                            "${combineSelection.size} selected — tap Next to choose difficulty.",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(categories) { cat ->
                            val selected = combineSelection.contains(cat)
                            val icon     = categoryIcons[cat] ?: "📝"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        vibrateLight(context)
                                        combineSelection = if (selected)
                                            combineSelection - cat
                                        else combineSelection + cat
                                    }
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(icon, fontSize = 18.sp)
                                Text(cat, fontSize = 15.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f))
                                if (selected) Text("✓", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                GradientBtn(
                    text     = "Next  ▶",
                    gradient = appBtnGradient,
                    enabled  = combineSelection.size >= 2,
                    onClick  = {
                        vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                        showMultiCategoryDialog = false
                        showCombineDiffDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            dismissButton = {
                GradientBtn("Back", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                    showMultiCategoryDialog = false
                    combineSelection = emptyList()
                    // Restore the category-pick dialog so the user can keep
                    // picking instead of being dropped to a bare home screen.
                    showVindCategoryDialog = true
                }, modifier = Modifier.fillMaxWidth())
            }
        )
    }

    // ── COMBINE DIFFICULTY DIALOG ────────────────────────────────────────────────
    // Final step of the combine flow: pick a difficulty, launch (or hand off to lobby).
    if (showCombineDiffDialog) {
        AlertDialog(
            onDismissRequest = { showCombineDiffDialog = false },
            title = { Text("Choose Difficulty", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "${combineSelection.size} categories selected",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Difficulty.entries.forEach { diff ->
                        val tagline = when (diff) {
                            Difficulty.EASY   -> "Quick warm-up"
                            Difficulty.MEDIUM -> "A pleasant solve"
                            Difficulty.HARD   -> "A real challenge"
                            Difficulty.EXPERT -> "For the dedicated"
                            Difficulty.GENIUS -> "Bring your A-game"
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .shadow(4.dp, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .background(appBtnGradient)
                                .clickable {
                                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                                    showCombineDiffDialog = false
                                    val cats = combineSelection
                                    combineSelection = emptyList()
                                    val label = if (cats.size <= 3) cats.joinToString("+")
                                    else "${cats.size} Categories"
                                    if (isOnlineGame && onlineRole == OnlineRole.HOST) {
                                        activeCategory     = label
                                        activeDifficulty   = diff
                                        combinedCategories = cats
                                        FirebaseGameManager.writeState(onlineCode, mapOf(
                                            "category" to label, "difficulty" to diff.name
                                        ))
                                        appMode = AppMode.ONLINE_LOBBY
                                    } else {
                                        launchPuzzle(label, diff, combined = cats)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            BevelHighlight()
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${diff.emoji}  ${diff.label}", fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(tagline, fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.7f))
                                }
                                Text("${diff.wordCount} words", fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.85f))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                GradientBtn("Back", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                    showCombineDiffDialog = false
                    showMultiCategoryDialog = true
                }, modifier = Modifier.fillMaxWidth())
            }
        )
    }

    // ── NO PLAYER 2 NAME DIALOG ──────────────────────────────────────────────────
    if (showNoPlayer2Dialog) {
        AlertDialog(
            onDismissRequest = { showNoPlayer2Dialog = false },
            title = { Text("Player 2 Name Required", fontWeight = FontWeight.Bold) },
            text  = { Text("Please set up Player 2 before starting.") },
            confirmButton = {
                GradientBtn("Set Up Player 2", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                    showNoPlayer2Dialog = false; showPlayer2SetupDialog = true
                }, modifier = Modifier.fillMaxWidth())
            },
            dismissButton = {
                GradientBtn("Cancel", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick(); showNoPlayer2Dialog = false
                }, modifier = Modifier.fillMaxWidth())
            }
        )
    }

    // ── DELETE PROFILE CONFIRM DIALOG ────────────────────────────────────────────
    confirmDeletePlayer?.let { nameToDelete ->
        AlertDialog(
            onDismissRequest = { confirmDeletePlayer = null },
            title = { Text("Delete Profile?", fontWeight = FontWeight.Bold) },
            text  = { Text("Are you sure you want to delete \"$nameToDelete\"? This cannot be undone.") },
            confirmButton = {
                GradientBtn("Delete", redGradient, onClick = {
                    vibrateLight(context)
                    saveManager.deletePlayer(nameToDelete)
                    profileList = saveManager.getAllPlayerNames()
                    confirmDeletePlayer = null
                    if (playerName == nameToDelete) {
                        saveManager.setLastUser(""); playerName = ""; appMode = AppMode.LOGIN
                    }
                }, modifier = Modifier.fillMaxWidth())
            },
            dismissButton = {
                GradientBtn("Cancel", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick(); confirmDeletePlayer = null
                }, modifier = Modifier.fillMaxWidth())
            }
        )
    }

    // ── DIFFICULTY PICKER DIALOG ────────────────────────────────────────────────
    difficultyPickCategory?.let { cat ->
        AlertDialog(
            onDismissRequest = { difficultyPickCategory = null },
            title = { Text("Choose Difficulty", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Saves are per mode: a Team game never shows up as a solo "Resume".
                    val slotMode = if (activeGameMode == GameMode.DAILY) GameMode.SINGLE else activeGameMode
                    val savedIds = remember(playerName, homeRefresh) { saveManager.getSavedSlotIds(playerName) }
                    Difficulty.entries.forEach { diff ->
                        val slot    = SaveSlot(slotMode, cat, diff)
                        val hasSave = !isOnlineGame && slot.id in savedIds
                        val tagline = when (diff) {
                            Difficulty.EASY   -> "Quick warm-up"
                            Difficulty.MEDIUM -> "A pleasant solve"
                            Difficulty.HARD   -> "A real challenge"
                            Difficulty.EXPERT -> "For the dedicated"
                            Difficulty.GENIUS -> "Bring your A-game"
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .shadow(4.dp, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .background(appBtnGradient)
                                .clickable {
                                    vibrateLight(context)
                                    if (soundEnabled) SoundPlayer.playClick()
                                    if (isOnlineGame && onlineRole == OnlineRole.HOST) {
                                        // Online host: launch puzzle then go to lobby while guest joins.
                                        // Clear combinedCategories — single-category path must override
                                        // any leftover combined state from an earlier flow.
                                        activeCategory     = cat
                                        activeDifficulty   = diff
                                        combinedCategories = emptyList()
                                        FirebaseGameManager.writeState(onlineCode, mapOf("category" to cat, "difficulty" to diff.name))
                                        difficultyPickCategory = null
                                        appMode = AppMode.ONLINE_LOBBY
                                    } else if (activeGameMode != GameMode.SINGLE && activeGameMode != GameMode.DAILY &&
                                               player2Name.isBlank()) {
                                        difficultyPickCategory = null
                                        showPlayer2SetupDialog = true
                                    } else {
                                        difficultyPickCategory = null
                                        if (hasSave) resumePrompt = slot
                                        else launchPuzzle(cat, diff)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            BevelHighlight()
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${diff.emoji}  ${diff.label}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(tagline, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                                }
                                if (hasSave)
                                    Text("▶ Resume", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Bold)
                                else
                                    Text("${diff.wordCount} words", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                GradientBtn("Cancel", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick(); difficultyPickCategory = null
                }, modifier = Modifier.fillMaxWidth())
            }
        )
    }

    // ── RESUME / NEW PUZZLE DIALOG ────────────────────────────────────────────
    resumePrompt?.let { slot ->
        AlertDialog(
            onDismissRequest = { resumePrompt = null },
            title = { Text("Resume Puzzle?", fontWeight = FontWeight.Bold) },
            text  = { Text("You have an unfinished ${slotTitle(slot)} puzzle. Resume where you left off, or start a new one?") },
            confirmButton = {
                GradientBtn("▶ Resume", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                    resumePrompt = null
                    if (!resumeSlot(slot)) launchPuzzle(slot.category, slot.difficulty)
                }, modifier = Modifier.fillMaxWidth())
            },
            dismissButton = {
                GradientBtn("New Puzzle", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                    resumePrompt = null
                    saveManager.clearPuzzle(playerName, slot)
                    homeRefresh++
                    launchPuzzle(slot.category, slot.difficulty)
                }, modifier = Modifier.fillMaxWidth())
            }
        )
    }

    if (showBgPicker) {
        BgPickerScreen(
            bgImagePool     = bgImagePool,
            currentBgImage  = currentBgImageName,
            context         = context,
            onSelect        = { name ->
                currentBgImageName = name
                showBgPicker = false
            },
            onDismiss       = { showBgPicker = false }
        )
    }

    if (showBtnColorPicker) {
        ColorPickerSheet(
            title        = "Button Color",
            currentArgb  = currentBtnColorArgb,
            recentArgbs  = emptyList(),
            presets      = BUTTON_COLOR_PRESETS,
            onApply      = { argb ->
                currentBtnColorArgb = argb
                saveManager.setButtonColor(playerName, argb)
                showBtnColorPicker = false
            },
            onDismiss    = { showBtnColorPicker = false }
        )
    }

    if (showColorPicker) {
        ColorPickerSheet(
            title        = "Cell Color",
            currentArgb  = currentCellColorArgb,
            recentArgbs  = saveManager.getRecentColorArgbs(playerName),
            presets      = CELL_COLOR_PRESETS,
            onApply      = { argb ->
                currentCellColorArgb = argb
                val color = Color(argb.toLong() and 0xFFFFFFFFL)
                saveManager.setCellColor(playerName, color)
                saveManager.addRecentColor(playerName, color)
                showColorPicker = false
            },
            onDismiss    = { showColorPicker = false }
        )
    }

    // Broadcast to Firebase when this device opens/closes the answer dialog
    LaunchedEffect(wordToInput, isOnlineGame) {
        if (isOnlineGame && onlineCode.isNotEmpty()) {
            val field = if (onlineRole == OnlineRole.HOST) "p0answering" else "p1answering"
            FirebaseGameManager.writeState(onlineCode, mapOf(field to (wordToInput != null)))
        }
    }

    // ── VINDICTIVE ANSWER COUNTDOWN ──────────────────────────────────────────
    // Starts once the answerer has the phone ("I'm Ready" dismissed). Runs only on
    // the device that owns the timer (VindictiveRules.ownsTimer): offline the one
    // phone, online the answerer's phone — which mirrors it to the other device.
    // Typing keeps the clock running; when it hits zero the documented
    // Answer It / Pass choice appears.
    LaunchedEffect(vindPhase, showTurnDialog, vindAssignedWord) {
        if (activeGameMode != GameMode.VINDICTIVE || appMode != AppMode.DASHBOARD || puzzleSolved) return@LaunchedEffect
        if (showTurnDialog || vindAssignedWord == null) return@LaunchedEffect
        if (!VindictiveRules.ownsTimer(vindSnapshot(), isOnlineGame, myIndex())) return@LaunchedEffect
        vindPassDialogVisible = false
        for (i in vindTimerSeconds downTo 1) {
            vindOpponentCountdown = i
            if (isOnlineGame) FirebaseGameManager.writeState(onlineCode, mapOf("vindCountdown" to i))
            delay(1000L)
            if (vindPhase != VindicativePhase.OPPONENT_WAIT || puzzleSolved) return@LaunchedEffect
        }
        onVindTimeout()
    }

    // ── RESULTS CARD ─────────────────────────────────────────────────────────
    val result = lastResult
    if (puzzleSolved && showResults && result != null && appMode == AppMode.DASHBOARD) {
        val timeLabel = "%d:%02d".format(result.seconds / 60, result.seconds % 60)
        val p0 = onlineP0Name.ifBlank { "Player 1" }
        val p1 = onlineP1Name.ifBlank { "Player 2" }
        val title = when (result.mode) {
            GameMode.VINDICTIVE -> when (VindictiveRules.winner(result.vind)) {
                0    -> "🏆 $p0 wins!"
                1    -> "🏆 $p1 wins!"
                else -> "🤝 Dead heat!"
            }
            GameMode.TEAM -> "Team Victory! 🤝"
            else          -> if (result.isDaily) "Daily Puzzle Solved! 📅" else "Puzzle Complete! 🎉"
        }
        AlertDialog(
            onDismissRequest = { },
            title = { Text(title, style = MaterialTheme.typography.headlineMedium) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    @Composable
                    fun Line(label: String, value: String, strong: Boolean = false) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(value, style = if (strong) MaterialTheme.typography.titleMedium
                                                else MaterialTheme.typography.bodyMedium,
                                fontWeight = if (strong) FontWeight.Bold else FontWeight.Medium,
                                color = if (strong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    when (result.mode) {
                        GameMode.VINDICTIVE -> {
                            Line(p0, "${result.vind.p1Score} pts", strong = VindictiveRules.winner(result.vind) == 0)
                            Line(p1, "${result.vind.p2Score} pts", strong = VindictiveRules.winner(result.vind) == 1)
                            HorizontalDivider(Modifier.padding(vertical = 4.dp))
                            val mine = if (result.myIndex == 0) result.vind.p1Score else result.vind.p2Score
                            Line("Banked to your score", "+${Economy.vindictiveBank(mine)}")
                        }
                        else -> {
                            if (result.mode == GameMode.TEAM) {
                                Line("$p0 solved", "${result.team.p1Correct} words")
                                Line("$p1 solved", "${result.team.p2Correct} words")
                                TeamRules.mvp(result.team)?.let { mvp ->
                                    Text("⭐ MVP: ${if (mvp == 0) p0 else p1}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.tertiary)
                                }
                                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                            }
                            Line("Completion reward", "+${result.reward}")
                            if (result.hints > 0) Line("Hints (charged when used)", "−${result.hints}")
                            Line("Net for this puzzle", "+${result.net}", strong = true)
                            if (result.dailyAlreadyPaid)
                                Text("Today's Daily was already counted — no extra points this time.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (result.hints == 0)
                                Text("✨ Solved without hints", style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                    Line("Time", timeLabel)
                }
            },
            confirmButton = {
                val primaryLabel = when {
                    isOnlineGame   -> "Done"
                    result.isDaily -> "Play a Category"
                    else           -> "Next Puzzle"
                }
                GradientBtn(primaryLabel, appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                    when {
                        isOnlineGame   -> goHome()
                        // The Daily is one puzzle per day — "next" means pick a category.
                        result.isDaily -> { goHome(); showVindCategoryDialog = true }
                        // Same category (or the same combined set), same difficulty, same mode.
                        else -> launchPuzzle(activeCategory, activeDifficulty, combined = combinedCategories)
                    }
                }, modifier = Modifier.fillMaxWidth())
            },
            dismissButton = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!isOnlineGame && !result.isDaily) {
                        GradientBtn("Change Category", appBtnGradient, onClick = {
                            vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                            goHome()
                            showVindCategoryDialog = true
                        }, modifier = Modifier.fillMaxWidth())
                    }
                    GradientBtn("Home", appBtnGradient, onClick = {
                        vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                        goHome()
                    }, modifier = Modifier.fillMaxWidth())
                }
            }
        )
    }

    // ── BLOCKING NOTICES ─────────────────────────────────────────────────────
    onlineNotice?.let { msg ->
        AlertDialog(
            onDismissRequest = { },
            title = { Text("🌐 Online play", fontWeight = FontWeight.Bold) },
            text  = { Text(msg) },
            confirmButton = {
                GradientBtn("OK", appBtnGradient, onClick = {
                    onlineNotice = null
                    timerRunning = false
                    cleanupOnlineSession()
                    resetOverlays()
                    puzzleSolved = false
                    appMode = AppMode.CATEGORY_SELECT
                }, modifier = Modifier.fillMaxWidth())
            }
        )
    }
    dailyInfoMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { dailyInfoMessage = null },
            title = { Text("📅 Daily Puzzle", fontWeight = FontWeight.Bold) },
            text  = { Text(msg) },
            confirmButton = {
                GradientBtn("OK", appBtnGradient, onClick = { dailyInfoMessage = null }, modifier = Modifier.fillMaxWidth())
            }
        )
    }
}

// ============================================================
// UI COMPOSABLES
// ============================================================

@Composable
fun PuzzleScreenReference(
    cells:           List<GridCell>,
    userInputs:      Map<Pair<Int, Int>, Char>,
    bgColor:         Color,
    bgImageName:     String,
    cellColor:       Color,
    placedWords:     List<PlacedWord>,
    centreOnWord:    PlacedWord?,
    animatingCell:   Pair<Int,Int>?,    // cell currently popping at 125%
    highlightedWord: PlacedWord?,       // word whose start cell glows green
    onCentred:       () -> Unit,
    onClearWords:    (List<PlacedWord>) -> Unit
) {
    var scale         by remember { mutableFloatStateOf(1f) }
    var offsetX       by remember { mutableFloatStateOf(0f) }
    var offsetY       by remember { mutableFloatStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    // Grid footprint in px at scale 1, and the zoom that fits it on screen —
    // published from BoxWithConstraints so the gesture handler can clamp.
    var gridPx        by remember { mutableStateOf(IntSize.Zero) }
    var minScale      by remember { mutableFloatStateOf(0.5f) }

    val latestInputs by rememberUpdatedState(userInputs)
    val latestWords  by rememberUpdatedState(placedWords)
    val latestClear  by rememberUpdatedState(onClearWords)

    // Pre-compute grid bounds once per cells change — stable integers used by both
    // the layout pass and the pointerInput coroutines.
    val minX = if (cells.isEmpty()) 0 else cells.minOf { it.x }
    val maxX = if (cells.isEmpty()) 0 else cells.maxOf { it.x }
    val minY = if (cells.isEmpty()) 0 else cells.minOf { it.y }
    val maxY = if (cells.isEmpty()) 0 else cells.maxOf { it.y }
    val gridW = if (cells.isEmpty()) 1 else maxX - minX + 1
    val gridH = if (cells.isEmpty()) 1 else maxY - minY + 1

    // Load the background image asynchronously; null = still loading or no image.
    val bgPainter = rememberBgPainter(bgImageName)

    // Outer Box is the true full-size container for both background and grid.
    // Keeping the background OUTSIDE BoxWithConstraints is critical — if the
    // Image is a child of BoxWithConstraints, the box measures to its content
    // size (the grid footprint) rather than the available space, so the image
    // only covers the grid area and leaves white strips above and below.
    // Track previous container size — reset pan/zoom if dimensions change significantly
    // (rotation causes width/height to swap, making stale offsets invalid).
    var prevContainerSize by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(containerSize) {
        if (prevContainerSize != IntSize.Zero && containerSize != IntSize.Zero) {
            val widthChanged  = kotlin.math.abs(containerSize.width  - prevContainerSize.width)  > 50
            val heightChanged = kotlin.math.abs(containerSize.height - prevContainerSize.height) > 50
            if (widthChanged || heightChanged) {
                scale   = 1f
                offsetX = 0f
                offsetY = 0f
            }
        }
        prevContainerSize = containerSize
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { containerSize = it }   // captures real pixel dimensions for parallax clamping
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(minScale, 4f)
                    // Keep at least part of the grid on screen.
                    val maxX = (gridPx.width  * scale + containerSize.width)  / 2f - 48f
                    val maxY = (gridPx.height * scale + containerSize.height) / 2f - 48f
                    offsetX = (offsetX + pan.x).coerceIn(-maxX.coerceAtLeast(0f), maxX.coerceAtLeast(0f))
                    offsetY = (offsetY + pan.y).coerceIn(-maxY.coerceAtLeast(0f), maxY.coerceAtLeast(0f))
                }
            }
    ) {
        // Layer 1: solid color fallback — always visible, instant
        Box(modifier = Modifier.fillMaxSize().background(bgColor))

        // Layer 2: background image — scaled 1.25x to guarantee full coverage.
        // ContentScale.Crop alone can leave thin gaps on some image/container aspect ratios
        // due to bitmap sizing or sub-pixel rounding. The 1.25x overbleed ensures the image
        // always bleeds 12.5% past every edge with no parallax movement.
        if (bgPainter != null) {
            Image(
                painter            = bgPainter,
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = 1.25f, scaleY = 1.25f)
            )
        }

        // Layer 3: grid — BoxWithConstraints measures available space correctly
        // now that it is no longer responsible for the background.
        BoxWithConstraints(
            modifier          = Modifier.fillMaxSize(),
            contentAlignment  = Alignment.Center
        ) {
            // Fit the whole grid into the available area.
            // Cap between 18 dp (readable minimum) and 52 dp (comfortable maximum).
            // When cells is empty (generating) default to 45 so nothing crashes.
            val cellSize: Float = if (cells.isEmpty()) 45f else
                minOf(
                    maxWidth.value  / gridW,
                    maxHeight.value / gridH,
                    52f
                ).coerceAtLeast(18f)

            // rememberUpdatedState wraps cellSize so the long-lived pointerInput
            // coroutine below always reads the current value without restarting.
            val cellSizeState   = rememberUpdatedState(cellSize)
            // Screen density — needed to convert dp→px for graphicsLayer translations.
            val screenDensity   = LocalDensity.current.density
            val densityState    = rememberUpdatedState(screenDensity)

            // Centre the grid on a word's start cell when single-tapped in the clue list.
            // Reset pan/zoom to the fitted view whenever screen dimensions change
            // (rotation, multi-window resize, foldable state change).
            // Big grids (Genius) can't fit at the 18dp minimum cell size: let the player
            // zoom out to see the whole board, and start somewhere readable.
            val fitScale = if (cells.isEmpty()) 1f else
                minOf(maxWidth.value / (gridW * cellSize), maxHeight.value / (gridH * cellSize), 1f)
            LaunchedEffect(maxWidth, maxHeight, gridW, gridH) {
                minScale = (fitScale * 0.9f).coerceAtMost(1f)
                gridPx   = IntSize((gridW * cellSize * screenDensity).toInt(), (gridH * cellSize * screenDensity).toInt())
                scale    = maxOf(fitScale, 0.75f).coerceAtMost(1f)
                offsetX  = 0f
                offsetY  = 0f
            }

            LaunchedEffect(centreOnWord) {
                val w   = centreOnWord ?: return@LaunchedEffect
                if (cells.isEmpty()) return@LaunchedEffect
                // Keep the player's zoom (never below a readable level) and bring the
                // MIDDLE of the word to the centre of the view.
                scale = scale.coerceAtLeast(minOf(1f, maxOf(minScale, 0.75f)))
                val csPx    = cellSizeState.value * densityState.value
                val half    = (w.word.length - 1) / 2f
                val midX    = w.startX - minX + 0.5f + (if (w.isHorizontal) half else 0f)
                val midY    = w.startY - minY + 0.5f + (if (w.isHorizontal) 0f else half)
                val gridWpx = gridW * csPx
                val gridHpx = gridH * csPx
                offsetX = -(midX * csPx - gridWpx / 2f) * scale
                offsetY = -(midY * csPx - gridHpx / 2f) * scale
                onCentred()
            }

            // Tap on a grid cell to clear its word's incorrect answer.
            // Uses a separate pointerInput so it doesn't conflict with pan/zoom.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(cells) {
                        detectTapGestures { tapOffset ->
                            if (cells.isEmpty()) return@detectTapGestures
                            val cs      = cellSizeState.value
                            val csPx    = cs * density          // dp → px for this device
                            val gridWpx = gridW * csPx
                            val gridHpx = gridH * csPx

                            // Reverse the graphicsLayer transform to get grid-local coords.
                            val lx = (tapOffset.x - size.width  / 2f - offsetX) / scale + gridWpx / 2f
                            val ly = (tapOffset.y - size.height / 2f - offsetY) / scale + gridHpx / 2f
                            if (lx < 0 || ly < 0 || lx >= gridWpx || ly >= gridHpx) return@detectTapGestures

                            val tappedX   = minX + (lx / csPx).toInt()
                            val tappedY   = minY + (ly / csPx).toInt()
                            val tappedPos = Pair(tappedX, tappedY)
                            if (cells.none { it.x == tappedX && it.y == tappedY }) return@detectTapGestures

                            val inputs    = latestInputs
                            val words     = latestWords
                            val wordsHere = words.filter { w -> getCellsForWord(w).contains(tappedPos) }
                            val wrongWords = wordsHere.filter { w ->
                                w.word.indices.any { idx ->
                                    val pos = if (w.isHorizontal) Pair(w.startX + idx, w.startY)
                                    else Pair(w.startX, w.startY + idx)
                                    val ch  = inputs[pos]
                                    ch != null && ch != w.word[idx]
                                }
                            }
                            if (wrongWords.isNotEmpty()) latestClear(wrongWords)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (cells.isNotEmpty()) {
                    // Scale font sizes proportionally so clue numbers and letters
                    // stay readable at any cell size (small on dense Genius grids,
                    // larger on Easy grids or tablets).
                    val numberFontSp = (cellSize * 0.22f).coerceIn(6f, 11f)
                    val letterFontSp = (cellSize * 0.48f).coerceIn(10f, 22f)

                    Box(
                        modifier = Modifier
                            // requiredSize: a grid wider than the screen must NOT be squeezed to
                            // the incoming constraints, or drawing and tap maths disagree.
                            .requiredSize((gridW * cellSize).dp, (gridH * cellSize).dp)
                            // Lambda form: pan/zoom only re-draws, never recomposes the grid.
                            .graphicsLayer {
                                scaleX       = scale
                                scaleY       = scale
                                translationX = offsetX
                                translationY = offsetY
                            }
                    ) {
                        cells.forEach { cell ->
                            val cellPos      = Pair(cell.x, cell.y)
                            val isAnimCell   = animatingCell == cellPos
                            val isStartCell  = highlightedWord != null && cell.x == highlightedWord.startX && cell.y == highlightedWord.startY
                            val animScale    by animateFloatAsState(
                                targetValue  = if (isAnimCell) 1.25f else 1f,
                                animationSpec = tween(60), label = "cellPop"
                            )
                            val baseBg = when {
                                isStartCell -> Color(0xFF4CAF50)   // green — the numbered start cell
                                else        -> cellColor
                            }
                            // Outer cell box: background + border, absolute position.
                            // absoluteOffset uses raw pixel values — eliminates sub-pixel
                            // rounding gaps that appear with .offset() + dp conversion.
                            val cellOffPx = { v: Int -> (v * cellSize * screenDensity).toInt() }
                            Box(
                                modifier = Modifier
                                    .absoluteOffset { IntOffset(
                                        cellOffPx(cell.x - minX),
                                        cellOffPx(cell.y - minY)
                                    )}
                                    .size(cellSize.dp)
                                    .graphicsLayer(scaleX = animScale, scaleY = animScale)
                                    .shadow(1.dp, RoundedCornerShape(4.dp))
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(baseBg)
                                    .border(0.5.dp, Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            ) {
                                // Clue number — pinned to top-left corner
                                if (cell.number != null) {
                                    Text(
                                        text       = "${cell.number}",
                                        fontSize   = numberFontSp.sp,
                                        lineHeight = numberFontSp.sp,
                                        modifier   = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(start = 1.dp, top = 1.dp),
                                        color      = Color.Black
                                    )
                                }
                                // Player letter — use a fill+center inner Box so centering is
                                // driven by layout, not by alignment hints that break inside
                                // ?.let lambdas or are defeated by font padding.
                                val enteredChar = userInputs[Pair(cell.x, cell.y)]
                                if (enteredChar != null) {
                                    val isCorrect = enteredChar == cell.char
                                    Box(
                                        modifier           = Modifier.fillMaxSize(),
                                        contentAlignment   = Alignment.Center
                                    ) {
                                        Text(
                                            text       = "$enteredChar",
                                            fontWeight = FontWeight.Bold,
                                            fontSize   = letterFontSp.sp,
                                            textAlign  = TextAlign.Center,
                                            // includeFontPadding = false removes the implicit
                                            // ascent/descent space Compose adds around glyphs,
                                            // which otherwise pushes letters above true centre.
                                            style      = TextStyle(
                                                platformStyle = PlatformTextStyle(
                                                    includeFontPadding = false
                                                )
                                            ),
                                            // Pick text colour that contrasts with the cell background
                                            color      = if (isCorrect) {
                                                if (cellLuminance(cellColor) > 0.4f) Color(0xFF006400)
                                                else Color(0xFF90EE90)  // light green on dark cells
                                            } else {
                                                if (cellLuminance(cellColor) > 0.4f) Color.Black
                                                else Color.White        // white text on dark cells
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }   // end BoxWithConstraints
    }   // end outer Box
}


// Category emoji icons — matched by uppercase category name
val categoryIcons = mapOf(
    "ACTORS"        to "🎬",
    "ANIME"         to "⛩️",
    "ATHLETES"      to "🏆",
    "AUTHORS"       to "📚",
    "CARTOONS"      to "🎨",
    "CITIES"        to "🌆",
    "ENTERTAINMENT" to "🎭",
    "FOOD"          to "🍕",
    "JOBS"          to "🔧",
    "NATURE"        to "🌿",
    "OBJECTS"       to "🧩",
    "PETS"          to "🐾",
    "SCHOOL"        to "🏫",
    "SINGERS"       to "🎤",
    "SPORTS"        to "⚽",
    "VIDEOGAMES"    to "🎮"
)

@Composable
fun CategoryScreen(
    categories:       List<String>,
    playerName:       String,
    score:            Int,
    completed:        Int,
    currentStreak:    Int,
    activeGameMode:   GameMode,
    onGameModeChange: (GameMode) -> Unit,
    player2Name:      String,
    allEntries:       List<RawEntry>,
    usedWordCounts:    Map<String, Int>,
    inProgressList:    List<SaveSlot>,
    onResume:          (SaveSlot) -> Unit,
    dailyDoneToday:    Boolean = false,
    onStartPlay:       () -> Unit,
    onDailyPuzzle:          () -> Unit,
    onChangeUser:           () -> Unit,
    onQuit:                 () -> Unit,
    onRequestPlayer2Setup:  () -> Unit = {},
    musicEnabled:           Boolean = true,
    soundEnabled:           Boolean = true,
    musicVolume:            Float   = 0.7f,
    onMusicToggle:          () -> Unit = {},
    onVolumeChange:         (Float) -> Unit = {},
    btnColorArgb:           Int = 0xFF6650A4.toInt()
) {
    val context  = LocalContext.current
    val btnColor    = Color(btnColorArgb.toLong() and 0xFFFFFFFFL)
    val btnColorDim = btnColor.copy(
        red   = (btnColor.red   * 0.75f).coerceIn(0f, 1f),
        green = (btnColor.green * 0.75f).coerceIn(0f, 1f),
        blue  = (btnColor.blue  * 0.75f).coerceIn(0f, 1f)
    )
    val btnGradient = Brush.verticalGradient(
        listOf(
            btnColor.copy(red = (btnColor.red + 0.15f).coerceIn(0f,1f),
                green = (btnColor.green + 0.15f).coerceIn(0f,1f),
                blue = (btnColor.blue + 0.15f).coerceIn(0f,1f)),
            btnColor,
            btnColorDim
        )
    )
    val categoryCounts = remember(allEntries) {
        allEntries.groupBy { it.category }.mapValues { it.value.size }
    }

    // Anchored bottom bar height — accounts for music card + system nav bar inset.
    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomBarHeight = (if (musicEnabled) 100.dp else 16.dp) + navBarInset

    Box(Modifier.fillMaxSize()) {
        // Status bar color bleed — matches top of the gradient header card
        Box(
            Modifier.fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(btnColor)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(
                    start = 16.dp, end = 16.dp,
                    top = 8.dp,
                    bottom = bottomBarHeight + 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── HEADER ───────────────────────────────────────────────────────
            Box(
                Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(btnGradient)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                BevelHighlight()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Welcome, $playerName",
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White,
                            maxLines   = 1
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("🏆 $score", fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text("🧩 $completed", fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.85f))
                            if (currentStreak > 0) {
                                Text("🔥 $currentStreak", fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold, color = Color(0xFFFFD580))
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onChangeUser) {
                            Text("Logout", fontSize = 14.sp, color = Color.White)
                        }
                        IconButton(onClick = onQuit) {
                            Icon(
                                imageVector        = Icons.Default.PowerSettingsNew,
                                contentDescription = "Quit",
                                tint               = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // ── CONTINUE LAST PUZZLE (if any in-progress save) ─────────────
            val lastInProgress = inProgressList.firstOrNull()
            if (lastInProgress != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable {
                            vibrateLight(context)
                            if (soundEnabled) SoundPlayer.playClick()
                            onResume(lastInProgress)
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "▶  Continue ${slotTitle(lastInProgress)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1
                            )
                            Text(
                                "Tap to resume where you left off",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Text("▶", fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // ── DAILY PUZZLE ────────────────────────────────────────────────
            run {
                val dailyGradient = Brush.verticalGradient(
                    listOf(Color(0xFFFFE566), Color(0xFFFFD700), Color(0xFFCCAA00))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(dailyGradient)
                        .graphicsLayer { alpha = if (dailyDoneToday) 0.72f else 1f }
                        .clickable(onClickLabel = if (dailyDoneToday) "Daily puzzle already solved" else "Play today's daily puzzle") {
                            vibrateLight(context); if (soundEnabled) SoundPlayer.playClick(); onDailyPuzzle()
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    BevelHighlight(alpha = 0.35f)
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (dailyDoneToday) "✅" else "📅", fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
                            Text("DAILY PUZZLE", fontSize = 14.sp,
                                color = Color.Black, fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp)
                        }
                        Text(
                            if (dailyDoneToday) "Solved today — a new one arrives at midnight UTC"
                            else "All categories • ${DailyPuzzle.WORD_COUNT} words • ${Economy.DAILY_POINTS} pts • same for everyone",
                            fontSize = 11.sp, color = Color.Black.copy(alpha = 0.72f))
                    }
                }
            }

            // ── PLAY MODE + START ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(btnGradient)
            ) {
                BevelHighlight()
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "PLAY MODE",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color.White.copy(alpha = 0.75f),
                        letterSpacing = 1.5.sp,
                        modifier   = Modifier.padding(bottom = 4.dp)
                    )

                    listOf(
                        GameMode.SINGLE     to "👤  Single Player",
                        GameMode.TEAM       to "🤝  Team Mode",
                        GameMode.VINDICTIVE to "⚔️  Vindictive Mode"
                    ).forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onGameModeChange(mode) }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = activeGameMode == mode,
                                onClick  = { onGameModeChange(mode) },
                                colors   = RadioButtonDefaults.colors(
                                    selectedColor   = Color.White,
                                    unselectedColor = Color.White.copy(alpha = 0.55f)
                                )
                            )
                            Text(
                                label,
                                fontSize = 14.sp,
                                color    = Color.White,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = activeGameMode != GameMode.SINGLE) {
                        if (player2Name.isBlank()) {
                            TextButton(
                                onClick  = { onRequestPlayer2Setup() },
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                            ) {
                                Text(
                                    "👥 Tap to set Player 2 name…",
                                    fontSize = 13.sp,
                                    color    = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .clickable { onRequestPlayer2Setup() }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "👥 Player 2: $player2Name",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                                Text("Change", fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.75f))
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .shadow(6.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .clickable {
                                vibrateLight(context)
                                if (soundEnabled) SoundPlayer.playClick()
                                onStartPlay()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "▶  START",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = btnColor,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }

            // ── Vertically center the carousel in remaining space ──────────
            Spacer(Modifier.weight(1f))

            // ── YOUR CATEGORIES — snap-to-center carousel with focus pop ───
            Text(
                "YOUR CATEGORIES",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )

            val carouselListState = rememberLazyListState()
            val carouselFling     = rememberSnapFlingBehavior(carouselListState)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cardWidth   = 132.dp
                // Side padding so first/last items can sit at viewport center.
                val sidePadding = ((maxWidth - cardWidth) / 2).coerceAtLeast(8.dp)
                LazyRow(
                    state               = carouselListState,
                    flingBehavior       = carouselFling,
                    contentPadding      = PaddingValues(horizontal = sidePadding, vertical = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(count = categories.size) { idx ->
                        val category = categories[idx]
                        val total = categoryCounts[category] ?: 0
                        val used  = usedWordCounts[category] ?: 0
                        val pct   = if (total > 0) (used * 100 / total).coerceIn(0, 100) else 0
                        val icon  = categoryIcons[category] ?: "📝"

                        // Distance from viewport center → scale (0.84 edge → 1.10 center).
                        val targetScale by remember {
                            derivedStateOf {
                                val info = carouselListState.layoutInfo
                                val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                                val item = info.visibleItemsInfo.firstOrNull { it.index == idx }
                                if (item == null) 0.75f
                                else {
                                    val itemCenter = item.offset + item.size / 2f
                                    val maxDist = (info.viewportEndOffset - info.viewportStartOffset) / 2f
                                    val t = (1f - kabs(itemCenter - viewportCenter) / maxDist).coerceIn(0f, 1f)
                                    // 0.75 at the edges → 1.22 dead-center.
                                    // Big focus pop, neighbors visibly recede.
                                    0.75f + 0.47f * t
                                }
                            }
                        }
                        val animatedScale by animateFloatAsState(
                            targetValue   = targetScale,
                            animationSpec = tween(120),
                            label         = "carouselScale"
                        )

                        Box(
                            modifier = Modifier
                                .width(cardWidth)
                                .height(116.dp)
                                .graphicsLayer(scaleX = animatedScale, scaleY = animatedScale)
                                .shadow(8.dp, RoundedCornerShape(14.dp))
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                // Two-tone border: a soft inner halo from the player's
                                // button color plus a crisp outer line for definition.
                                .border(
                                    width = 2.dp,
                                    brush = Brush.verticalGradient(listOf(
                                        btnColor.copy(alpha = 0.55f),
                                        btnColor.copy(alpha = 0.30f)
                                    )),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(icon, fontSize = 18.sp, modifier = Modifier.padding(end = 5.dp))
                                    Text(
                                        category,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "$total words",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { pct / 100f },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                    color = btnColor,
                                    trackColor = btnColor.copy(alpha = 0.18f)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    if (used == 0) "Untouched" else "$pct% explored",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))
        }   // end Column

        // ── ANCHORED BOTTOM BAR ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Music player card ─────────────────────────────────────────
            if (musicEnabled) {
                val dailyGold = Brush.verticalGradient(
                    listOf(Color(0xFFFFE566), Color(0xFFFFD700), Color(0xFFCCAA00))
                )
                val trackColor = Color(0xFF3A2A00)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(dailyGold)
                ) {
                    Box(Modifier.fillMaxWidth().height(1.5.dp)
                        .background(Color.White.copy(alpha = 0.45f))
                        .align(Alignment.TopCenter))
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text(
                            "♪  ${AmbientMusicPlayer.currentTrackName.ifEmpty { "No track" }.take(36)}",
                            fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            color = trackColor, maxLines = 1,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(onClick = { AmbientMusicPlayer.previous() }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = trackColor)
                            }
                            IconButton(
                                onClick = { onMusicToggle() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (AmbientMusicPlayer.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (AmbientMusicPlayer.isPlaying) "Pause" else "Play",
                                    tint = trackColor
                                )
                            }
                            IconButton(onClick = { AmbientMusicPlayer.next() }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = trackColor)
                            }
                            Icon(Icons.Default.VolumeDown, contentDescription = null, tint = trackColor.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                            Slider(
                                value = musicVolume,
                                onValueChange = { onVolumeChange(it) },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = trackColor,
                                    activeTrackColor = trackColor,
                                    inactiveTrackColor = trackColor.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                            )
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = trackColor.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FullHintsList(
    words:           List<PlacedWord>,
    userInputs:      Map<Pair<Int, Int>, Char>,
    highlightedWord: PlacedWord?,
    selectedWord:    PlacedWord? = null,
    onSingleTap:     (PlacedWord) -> Unit,
    onDoubleTap:     (PlacedWord) -> Unit
) {
    val across        = words.filter { it.isHorizontal }.sortedBy { it.number }
    val down          = words.filter { !it.isHorizontal }.sortedBy { it.number }
    val acrossUnsolved = across.filter { !isWordSolved(it, userInputs) }
    val downUnsolved   = down.filter   { !isWordSolved(it, userInputs) }
    val acrossSolved   = across.filter {  isWordSolved(it, userInputs) }
    val downSolved     = down.filter   {  isWordSolved(it, userInputs) }
    val solvedCount    = acrossSolved.size + downSolved.size
    val totalCount     = words.size

    @Composable
    fun SectionHeader(label: String, color: Color) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color.White,
                letterSpacing = 1.5.sp)
        }
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        // Progress summary
        if (totalCount > 0) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Progress", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "$solvedCount / $totalCount",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                    LinearProgressIndicator(
                        progress = { solvedCount.toFloat() / totalCount },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }
        if (acrossUnsolved.isNotEmpty()) {
            item { SectionHeader("ACROSS", Color(0xFF5C6BC0)) }
            items(acrossUnsolved) { ClueItem(it, userInputs, highlightedWord == it || selectedWord == it, onSingleTap, onDoubleTap) }
        }
        if (downUnsolved.isNotEmpty()) {
            item { SectionHeader("DOWN", Color(0xFF00838F)) }
            items(downUnsolved) { ClueItem(it, userInputs, highlightedWord == it || selectedWord == it, onSingleTap, onDoubleTap) }
        }
        // Solved section — compact
        if (solvedCount > 0) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)
                    .clip(RoundedCornerShape(6.dp)).background(Color(0xFF2E7D32))
                    .padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("✓ SOLVED ($solvedCount)", fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp, color = Color.White, letterSpacing = 1.5.sp)
                }
            }
            items(acrossSolved) { ClueItem(it, userInputs, highlightedWord == it || selectedWord == it, onSingleTap, onDoubleTap) }
            items(downSolved)   { ClueItem(it, userInputs, highlightedWord == it || selectedWord == it, onSingleTap, onDoubleTap) }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
fun ClueItem(
    word:            PlacedWord,
    userInputs:      Map<Pair<Int, Int>, Char>,
    isHighlight:     Boolean,
    onSingleTap:     (PlacedWord) -> Unit,
    onDoubleTap:     (PlacedWord) -> Unit
) {
    val solved      = isWordSolved(word, userInputs)
    val solvedGreen = Color(0xFF2E7D32)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isHighlight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
            .pointerInput(word) {
                // onPress fires immediately on touch-down — no waiting for the
                // 300 ms double-tap window. The single-tap action (centre +
                // highlight) is non-destructive, so firing it as the first
                // half of a double-tap is harmless. Double-tap still opens
                // the answer dialog.
                detectTapGestures(
                    onPress     = { onSingleTap(word) },
                    onDoubleTap = { onDoubleTap(word) }
                )
            }
            .padding(vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
                Text(
                    text = "${word.number}.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (solved) solvedGreen else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(28.dp)
                )
                Text(
                    text = word.clue,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (solved) solvedGreen else Color.Unspecified,
                    textDecoration = if (solved) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.weight(1f)
                )
            }
            if (solved) {
                Surface(
                    color = solvedGreen.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "✓ ${word.word}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = solvedGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        if (!solved) {
            Text(text = "(${word.word.length} letters)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp, color = Color.LightGray)
    }
}



// ============================================================
// BACKGROUND IMAGE POOL
// ============================================================
// Available density folders (matching assets/images/ on disk):
//   Portrait : drawable-xxhdpi (1080×1920), drawable-xxxhdpi (1440×2560)
//   Landscape: drawable-land-xxhdpi (1920×1080), drawable-land-xxxhdpi (2560×1440)
//
// All images are WebP. Add or remove files from these folders and they
// are picked up automatically next launch — no code changes needed.

const val BG_IMAGE_NONE = ""   // sentinel meaning "use color background"

// Discovers all WebP/PNG/JPG images in the portrait xxhdpi folder.
// That folder is used as the canonical list — all 4 folders must contain
// the same filenames (the bat file guarantees this).
fun discoverBgImages(context: Context): List<String> {
    return try {
        val files = context.assets.list("images/drawable-xxhdpi") ?: return emptyList()
        files.filter { name ->
            name.endsWith(".webp", ignoreCase = true) ||
                    name.endsWith(".png",  ignoreCase = true) ||
                    name.endsWith(".jpg",  ignoreCase = true) ||
                    name.endsWith(".jpeg", ignoreCase = true)
        }.sorted()
    } catch (_: Exception) { emptyList() }
}

// Loads one image from the best available density folder for this device.
// Only checks the 4 folders that actually exist on disk.
// Falls back from xxxhdpi → xxhdpi so lower-density phones still get an image.
fun loadBgBitmap(context: Context, filename: String, isLandscape: Boolean = false): ImageBitmap? {
    if (filename == BG_IMAGE_NONE) return null
    return try {
        val density = context.resources.displayMetrics.density
        val isLand  = isLandscape   // caller passes Compose orientation — avoids context config lag

        // Portrait folders: drawable-xxhdpi, drawable-xxxhdpi
        // Landscape folders: drawable-land-xxhdpi, drawable-land-xxxhdpi
        // Pick best first, fall back to the other if missing.
        val (best, fallback) = if (density >= 4.0f)
            Pair("xxxhdpi", "xxhdpi")   // flagship — prefer xxxhdpi
        else
            Pair("xxhdpi", "xxxhdpi")   // mid-range — prefer xxhdpi

        val prefix = if (isLand) "drawable-land-" else "drawable-"  // correct folder per orientation

        var bitmap: ImageBitmap? = null
        for (q in listOf(best, fallback)) {
            try {
                context.assets.open("images/$prefix$q/$filename").use { stream ->
                    bitmap = BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
                if (bitmap != null) break
            } catch (_: Exception) {}
        }
        bitmap
    } catch (_: Exception) { null }
}

// Composable that loads a background image asynchronously.
// While loading returns null; caller shows the color background as fallback.
@Composable
fun rememberBgPainter(filename: String): Painter? {
    val context = LocalContext.current
    val config  = LocalConfiguration.current   // triggers recompose on rotation
    var painter by remember(filename, config.orientation) { mutableStateOf<Painter?>(null) }
    LaunchedEffect(filename, config.orientation) {
        if (filename == BG_IMAGE_NONE) { painter = null; return@LaunchedEffect }
        val isLand = config.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val bmp = withContext(Dispatchers.IO) { loadBgBitmap(context, filename, isLand) }
        painter = bmp?.let { BitmapPainter(it) }
    }
    return painter
}

// ============================================================
// CELL COLOUR HELPERS & COMPOSABLES
// ============================================================

// Perceived luminance (0=black, 1=white) — used to pick contrasting text color.
fun cellLuminance(color: Color): Float =
    0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue

// Convert HSV components to a Compose Color.
fun colorFromHsv(hue: Float, saturation: Float, value: Float): Color {
    val argb = android.graphics.Color.HSVToColor(floatArrayOf(
        hue.coerceIn(0f, 360f),
        saturation.coerceIn(0f, 1f),
        value.coerceIn(0f, 1f)
    ))
    return Color(argb.toLong() and 0xFFFFFFFFL)
}

// Extracts HSV from a Compose Color (returns FloatArray [hue, sat, val]).
fun colorToHsv(color: Color): FloatArray {
    val arr = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), arr)
    return arr
}

// ── HUE RING PICKER ───────────────────────────────────────────────────────────
// A circular hue selector drawn with Canvas. Drag around the ring to pick hue.
@Composable
fun HueRingPicker(hue: Float, onHueChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val cx = size.width / 2f;  val cy = size.height / 2f
                    val dx = offset.x - cx;    val dy = offset.y - cy
                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    if (angle < 0f) angle += 360f
                    onHueChange(angle)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val cx = size.width / 2f;  val cy = size.height / 2f
                    val dx = change.position.x - cx
                    val dy = change.position.y - cy
                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    if (angle < 0f) angle += 360f
                    onHueChange(angle)
                }
            }
    ) {
        val center      = Offset(size.width / 2f, size.height / 2f)
        val radius      = size.minDimension / 2f * 0.78f
        val strokeWidth = size.minDimension * 0.2f
        val topLeft     = Offset(center.x - radius, center.y - radius)
        val arcSize     = GeoSize(radius * 2f, radius * 2f)

        // Draw 360 1° arcs to form the full hue ring
        for (deg in 0..359) {
            drawArc(
                color       = colorFromHsv(deg.toFloat(), 1f, 1f),
                startAngle  = deg.toFloat(),
                sweepAngle  = 1.5f,    // slight overlap prevents visible seams
                useCenter   = false,
                topLeft     = topLeft,
                size        = arcSize,
                style       = Stroke(width = strokeWidth)
            )
        }

        // Selection indicator: white outline + current hue fill
        val angleRad = Math.toRadians(hue.toDouble())
        val ix = center.x + radius * cos(angleRad).toFloat()
        val iy = center.y + radius * sin(angleRad).toFloat()
        drawCircle(Color.White, radius = strokeWidth / 2f + 5f, center = Offset(ix, iy))
        drawCircle(colorFromHsv(hue, 1f, 1f),               radius = strokeWidth / 2f + 1f, center = Offset(ix, iy))
    }
}

// ── GRADIENT SLIDER ───────────────────────────────────────────────────────────
// A draggable horizontal bar with a two-stop gradient and a thumb indicator.
@Composable
fun GradientSlider(
    value:         Float,
    onValueChange: (Float) -> Unit,
    startColor:    Color,
    endColor:      Color,
    label:         String,
    modifier:      Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(startColor, endColor)))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onValueChange((offset.x / size.width).coerceIn(0f, 1f))
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        onValueChange((change.position.x / size.width).coerceIn(0f, 1f))
                    }
                }
        ) {
            val thumbOffsetX = (value * (maxWidth.value - 28f)).coerceAtLeast(0f).dp
            Box(
                modifier = Modifier
                    .offset(x = thumbOffsetX, y = 6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, Color.Gray, CircleShape)
            )
        }
    }
}

// ── COLOUR PICKER SHEET ───────────────────────────────────────────────────────
// Full color picker bottom sheet: 10 recent swatches + hue ring + two sliders.
private val CELL_COLOR_PRESETS = listOf(
    0xFFFFFFFF.toInt(), // White
    0xFFFFF9C4.toInt(), // Soft Yellow
    0xFFBBDEFB.toInt(), // Sky Blue
    0xFFC8E6C9.toInt(), // Soft Green
    0xFFF8BBD9.toInt(), // Blush Pink
    0xFFE1BEE7.toInt(), // Lavender
    0xFFFFE0B2.toInt(), // Peach
    0xFFB2DFDB.toInt(), // Mint
    0xFFFFF8E1.toInt(), // Warm Cream
    0xFFECEFF1.toInt()  // Cool Gray
)

private val BUTTON_COLOR_PRESETS = listOf(
    0xFF6650A4.toInt(), // Deep Purple (default)
    0xFF1565C0.toInt(), // Royal Blue
    0xFF00695C.toInt(), // Ocean Teal
    0xFF2E7D32.toInt(), // Forest Green
    0xFFC62828.toInt(), // Crimson
    0xFFE64A19.toInt(), // Burnt Orange
    0xFF880E4F.toInt(), // Deep Magenta
    0xFF283593.toInt(), // Navy Indigo
    0xFF4E342E.toInt(), // Espresso Brown
    0xFF37474F.toInt()  // Gunmetal
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerSheet(
    title:       String,
    currentArgb: Int,
    recentArgbs: List<Int>,
    presets:     List<Int> = emptyList(),
    onApply:     (Int) -> Unit,
    onDismiss:   () -> Unit
) {
    // Initialise HSV from the current cell color.
    // If the current color is white/near-white (sat < 0.1), start at sat=0.8 so
    // the hue ring is immediately responsive — the user sees vivid color changes
    // as soon as they drag. "Reset to White" is right there if they want white back.
    val initHsv = remember(currentArgb) { colorToHsv(Color(currentArgb.toLong() and 0xFFFFFFFFL)) }
    var hue by remember { mutableFloatStateOf(initHsv[0]) }
    var sat by remember { mutableFloatStateOf(if (initHsv[1] < 0.1f) 0.8f else initHsv[1]) }
    var brt by remember { mutableFloatStateOf(if (initHsv[2] < 0.3f) 0.9f else initHsv[2]) }

    // Read hue/sat/brt directly — they are State delegates so reading them
    // here causes recomposition whenever any of them changes.
    val pickedColor = colorFromHsv(hue, sat, brt)

    val pickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = pickerSheetState,
        modifier         = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)

            // ── Preset swatches ────────────────────────────────────────────────
            if (presets.isNotEmpty()) {
                Text("Presets", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presets) { argb ->
                        val swatchColor = Color(argb.toLong() and 0xFFFFFFFFL)
                        val isSelected  = argb == pickedColor.toArgb()
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable {
                                    val hsv = colorToHsv(swatchColor)
                                    hue = hsv[0]
                                    sat = if (hsv[1] < 0.01f) 0f else hsv[1]
                                    brt = hsv[2]
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (cellLuminance(swatchColor) > 0.5f) Color.Black else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                HorizontalDivider()
            }

            // ── Recent colours row ─────────────────────────────────────────────
            if (recentArgbs.isNotEmpty()) {
                Text("Recent (tap to apply)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recentArgbs) { argb ->
                        val swatchColor = Color(argb.toLong() and 0xFFFFFFFFL)
                        val isSelected  = argb == pickedColor.toArgb()
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable {
                                    val hsv = colorToHsv(swatchColor)
                                    hue = hsv[0]; sat = hsv[1].coerceAtLeast(0.01f)
                                    brt = hsv[2].coerceAtLeast(0.3f)
                                }
                        )
                    }
                }
            }

            // ── White reset quick-pick ─────────────────────────────────────────
            TextButton(onClick = { hue = 0f; sat = 0f; brt = 1f }) {
                Text("↺ Reset to White")
            }

            HorizontalDivider()

            // ── Hue ring + live preview side by side ──────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                HueRingPicker(
                    hue         = hue,
                    onHueChange = { hue = it },
                    modifier    = Modifier.size(180.dp)
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(pickedColor)
                            .border(1.5.dp, Color.Gray, RoundedCornerShape(12.dp))
                    )
                    Text("Preview", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    // Show hex-ish ARGB for the curious
                    Text(
                        text     = "#%06X".format(pickedColor.toArgb() and 0xFFFFFF),
                        fontSize = 11.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Brightness slider ─────────────────────────────────────────────
            GradientSlider(
                value         = ((brt - 0.3f) / 0.7f).coerceIn(0f, 1f),
                onValueChange = { brt = 0.3f + it * 0.7f },
                startColor    = colorFromHsv(hue, sat, 0.3f),
                endColor      = colorFromHsv(hue, sat, 1f),
                label         = "Brightness"
            )

            // ── Saturation/Richness slider ────────────────────────────────────
            GradientSlider(
                value         = sat,
                onValueChange = { sat = it.coerceAtLeast(0.01f) },
                startColor    = colorFromHsv(hue, 0f, brt),
                endColor      = colorFromHsv(hue, 1f, brt),
                label         = "Richness  (left = pastel, right = vivid)"
            )

            HorizontalDivider()

            // ── Action buttons ────────────────────────────────────────────────
            Button(
                onClick  = { onApply(pickedColor.toArgb()) },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text("Apply Color", fontSize = 16.sp) }

            TextButton(
                onClick  = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Cancel") }
        }
    }
}

// ── BACKGROUND IMAGE PICKER SCREEN ───────────────────────────────────────────
// Full-screen grid of thumbnails. Tap one to apply immediately. Checkmark on
// the active image. Loads thumbnails from assets on a background thread.
@Composable
fun BgPickerScreen(
    bgImagePool:    List<String>,
    currentBgImage: String,
    context:        Context,
    onSelect:       (String) -> Unit,
    onDismiss:      () -> Unit
) {
    // Load thumbnails asynchronously — small bitmaps for the grid.
    // Purge any cached entries whose files no longer exist in the current pool.
    val thumbnails = remember { mutableStateMapOf<String, ImageBitmap?>() }
    LaunchedEffect(bgImagePool) {
        // Remove thumbnails for deleted images
        val poolSet = bgImagePool.toSet()
        thumbnails.keys.toList().forEach { key -> if (!poolSet.contains(key)) thumbnails.remove(key) }
        bgImagePool.forEach { name ->
            if (!thumbnails.containsKey(name)) {
                // Use inSampleSize=8 — loads image at 1/8 resolution (≈135×240 for 1080p source).
                // More reliable with WebP than manual scaling, and uses far less memory.
                // Mirror the game's folder selection so we read from the same file that's proven to load.
                // Original working approach: plain decodeStream (no Options) then
                // manual createScaledBitmap. BitmapFactory.Options with inSampleSize
                // silently returns null for some WebP variants — this avoids that entirely.
                // Mirror loadBgBitmap's density-aware folder selection so we read
                // from the same file that successfully loads as the game background.
                // High-density devices (Pixel 10 Pro XL etc) may only have clean files
                // in xxxhdpi — always trying xxhdpi first caused silent null returns.
                val bmp = withContext(Dispatchers.IO) {
                    val density = context.resources.displayMetrics.density
                    val folders = if (density >= 4.0f)
                        listOf("drawable-xxxhdpi", "drawable-xxhdpi")
                    else
                        listOf("drawable-xxhdpi", "drawable-xxxhdpi")

                    var result: ImageBitmap? = null
                    for (folder in folders) {
                        try {
                            context.assets.open("images/$folder/$name").use { stream ->
                                val full = BitmapFactory.decodeStream(stream)
                                if (full != null) {
                                    val ratio = 200f / full.width
                                    result = if (ratio < 1f) {
                                        val tw = (full.width  * ratio).toInt().coerceAtLeast(1)
                                        val th = (full.height * ratio).toInt().coerceAtLeast(1)
                                        val scaled = android.graphics.Bitmap
                                            .createScaledBitmap(full, tw, th, true)
                                        full.recycle()
                                        scaled.asImageBitmap()
                                    } else {
                                        full.asImageBitmap()
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                        if (result != null) break
                    }
                    result
                }
                thumbnails[name] = bmp
            }
        }
    }

    val config = LocalConfiguration.current
    val isLand = config.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val columns = if (isLand) 4 else 2
    val cardRatio = if (isLand) 16f / 9f else 9f / 16f  // landscape cards are wide, portrait are tall

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text("Background Image", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp))
                }
            }

            if (bgImagePool.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No images found", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Add WebP images to\nassets/images/drawable-xxhdpi/",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp,
                            textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    gridItems(bgImagePool) { name ->
                        val thumb     = thumbnails[name]
                        val isActive  = name == currentBgImage
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(cardRatio)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isActive) 3.dp else 1.dp,
                                    color = if (isActive) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .background(Color.DarkGray)
                                .clickable { onSelect(name) }
                        ) {
                            if (thumb != null) {
                                Image(
                                    bitmap         = thumb,
                                    contentDescription = name,
                                    contentScale   = ContentScale.Crop,
                                    modifier       = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color    = Color.White
                                    )
                                }
                            }

                            // Checkmark overlay on selected image
                            if (isActive) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✓", color = Color.White,
                                        fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Filename label at bottom
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .padding(4.dp)
                            ) {
                                Text(
                                    name.substringBeforeLast(".").take(28),
                                    color    = Color.White,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// NEW COMPOSABLES — Stats, Confetti, etc.
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    playerName:       String,
    saveManager:      SaveManager,
    allCategoryNames: Set<String>,
    onPlay:           () -> Unit,          // go to category select
    onResume:         (SaveSlot) -> Unit,  // resume a specific in-progress puzzle
    onBack:           () -> Unit
) {
    val stats      = remember(playerName) { saveManager.getAllStats(playerName) }
    val inProgress = remember(playerName) { saveManager.getInProgressPuzzles(playerName) }
    val score      = saveManager.getScore(playerName)
    val done       = saveManager.getCompleted(playerName)

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(playerName, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        )

        // Summary row
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$score", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary)
                Text("Total Score", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$done", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.secondary)
                Text("Puzzles", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        HorizontalDivider()

        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {

            // ── IN-PROGRESS PUZZLES ───────────────────────────────────────────
            if (inProgress.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    Text("▶  In Progress",
                        fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                }
                items(inProgress) { slot ->
                    Card(
                        modifier  = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors    = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        elevation = CardDefaults.cardElevation(2.dp),
                        onClick   = { onResume(slot) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(slotTitle(slot), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Tap to resume", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("▶", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            }

            // ── COMPLETED RECORDS ─────────────────────────────────────────────
            if (stats.isEmpty() && inProgress.isEmpty()) {
                item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text("🧩", fontSize = 56.sp)
                            Text(
                                "No records yet",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Solve a puzzle and your best times and scores will appear here.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else if (stats.isNotEmpty()) {
                item {
                    Text("Completed Puzzles",
                        fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
                }
                items(stats) { stat ->
                    // Exclude Daily records — their pseudo-category "Daily" is
                    // never in allCategoryNames so they used to falsely show
                    // the "★ Legacy" badge on every record.
                    val isDiscontinued = stat.gameMode == "SINGLE" &&
                        stat.diffName != "DAILY" &&
                        !allCategoryNames.contains(stat.category)
                    val bgColor = if (isDiscontinued) Color(0xFFFFF8DC) else MaterialTheme.colorScheme.surface
                    Card(
                        modifier  = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors    = CardDefaults.cardColors(containerColor = bgColor),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isDiscontinued) Text("⭐ ", fontSize = 16.sp)
                                Text(
                                    "${stat.category} — ${stat.diffName}",
                                    fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                    color = if (isDiscontinued) Color(0xFF8B6914) else Color.Unspecified
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            val mins = stat.timeSeconds / 60; val secs = stat.timeSeconds % 60
                            val desc = when {
                                stat.diffName == "DAILY" ->
                                    "$playerName solved the Daily Puzzle using ${stat.hintsUsed} hint(s) in %d:%02d".format(mins, secs)
                                stat.gameMode == "TEAM" ->
                                    "$playerName completed ${stat.category} in Team Mode with ${stat.partner} using ${stat.hintsUsed} hint(s) in %d:%02d".format(mins, secs)
                                stat.gameMode == "VINDICTIVE" ->
                                    if (stat.won)
                                        "$playerName CONQUERED ${stat.partner} in Vindictive Mode — ${stat.score}pts vs ${stat.partnerScore}pts — %d:%02d".format(mins, secs)
                                    else
                                        "$playerName got DEMOLISHED by ${stat.partner} in Vindictive Mode — ${stat.score}pts vs ${stat.partnerScore}pts — %d:%02d".format(mins, secs)
                                else ->
                                    "$playerName completed ${stat.category} at ${stat.diffName} using ${stat.hintsUsed} hint(s) in %d:%02d".format(mins, secs)
                            }
                            Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (isDiscontinued) {
                                Text("★ Legacy record — category no longer available",
                                    fontSize = 10.sp, color = Color(0xFF8B6914),
                                    fontStyle = FontStyle.Italic)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }

        // ── Action buttons — anchored above Android nav bar ──────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick  = onPlay,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("START PLAYING", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

// ── CONFETTI ──────────────────────────────────────────────────────────────────
private enum class ConfettiShape { RECT, CIRCLE, STREAMER }

private data class ConfettiParticle(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var rotation: Float, var rotSpeed: Float,
    val color: Color,
    val shape: ConfettiShape,
    val size:  Float                  // multiplier on base draw dimensions
)

@Composable
fun ConfettiOverlay() {
    val particles = remember {
        val colors = listOf(
            Color(0xFFFF5252), Color(0xFFFFC107), Color(0xFF40C4FF),
            Color(0xFF69F0AE), Color(0xFFE040FB), Color(0xFF00E5FF),
            Color(0xFFFFAB40), Color(0xFFFFFFFF)
        )
        List(110) {
            val shape = when (Random.nextInt(10)) {
                in 0..4 -> ConfettiShape.RECT       // 50% rectangles
                in 5..7 -> ConfettiShape.CIRCLE     // 30% circles
                else    -> ConfettiShape.STREAMER   // 20% long streamers
            }
            ConfettiParticle(
                x        = Random.nextFloat(),
                y        = Random.nextFloat() * -0.3f - 0.1f,
                vx       = (Random.nextFloat() - 0.5f) * 0.012f,
                vy       = Random.nextFloat() * 0.006f + 0.003f,
                rotation = Random.nextFloat() * 360f,
                rotSpeed = (Random.nextFloat() - 0.5f) * 10f,
                color    = colors.random(),
                shape    = shape,
                size     = 0.7f + Random.nextFloat() * 0.9f
            )
        }.toMutableList()
    }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { withFrameMillis { tick++ } } }

    Canvas(modifier = Modifier.fillMaxSize()) {
        @Suppress("UNUSED_EXPRESSION") tick  // read tick to trigger redraw
        particles.forEach { p ->
            p.x        += p.vx
            p.y        += p.vy
            p.vy       += 0.00009f  // gravity
            p.rotation += p.rotSpeed
            if (p.y > 1.1f) { p.y = -0.1f; p.x = Random.nextFloat() }
            val px = p.x * size.width
            val py = p.y * size.height
            canvasRotate(p.rotation, Offset(px, py)) {
                when (p.shape) {
                    ConfettiShape.RECT -> {
                        val w = 14f * p.size; val h = 9f * p.size
                        drawRect(p.color, Offset(px - w/2f, py - h/2f), GeoSize(w, h))
                    }
                    ConfettiShape.CIRCLE -> {
                        drawCircle(p.color, radius = 6f * p.size, center = Offset(px, py))
                    }
                    ConfettiShape.STREAMER -> {
                        val w = 28f * p.size; val h = 4f * p.size
                        drawRect(p.color, Offset(px - w/2f, py - h/2f), GeoSize(w, h))
                    }
                }
            }
        }
    }
}

// ============================================================
// GOOGLE PLAY GAMES SERVICES — INTEGRATION STUB
// ============================================================
// When you are ready to enable Google account sync / leaderboards:
//
// STEP 1 — build.gradle (app module):
//   implementation("com.google.android.gms:play-services-games-v2:19.0.0")
//
// STEP 2 — AndroidManifest.xml inside <application>:
//   <meta-data android:name="com.google.android.gms.games.APP_ID"
//              android:value="@string/app_id" />
//   (Add your numeric Play Console App ID to res/values/strings.xml)
//
// STEP 3 — Google Play Console:
//   Games Services → Add game → Link your app → Create OAuth 2.0 client.
//   You need a RELEASE keystore SHA-1 fingerprint registered in the Console.
//
// STEP 4 — Sign-in (call from MainActivity.onCreate or first composable launch):
//
//   val gamesSignInClient = PlayGames.getGamesSignInClient(activity)
//   gamesSignInClient.isAuthenticated.addOnCompleteListener { task ->
//       val isAuthed = task.isSuccessful && task.result.isAuthenticated
//       if (!isAuthed) gamesSignInClient.signIn()
//   }
//
// STEP 5 — Get player display name (replaces the manual name-entry screen):
//
//   val playersClient = PlayGames.getPlayersClient(activity)
//   playersClient.currentPlayer.addOnSuccessListener { player ->
//       val googleDisplayName = player.displayName
//       // Use googleDisplayName as playerName, skip the LOGIN screen entirely
//   }
//
// STEP 6 — Cloud save (replace SharedPreferences saves):
//
//   val snapshotsClient = PlayGames.getSnapshotsClient(activity)
//
//   // WRITE: serialize your SaveManager data to a ByteArray, then:
//   val metadata = SnapshotMetadataChange.Builder()
//       .setDescription("Crossword progress").build()
//   snapshotsClient.open("crossword_save", true).addOnSuccessListener { result ->
//       val snapshot = result.data
//       snapshot?.snapshotContents?.writeBytes(yourByteArray)
//       snapshotsClient.commitAndClose(snapshot!!, metadata)
//   }
//
//   // READ:
//   snapshotsClient.open("crossword_save", false).addOnSuccessListener { result ->
//       val bytes = result.data?.snapshotContents?.readFully()
//       // Deserialize and restore player state
//   }
//
// STEP 7 — Leaderboard (submit score):
//
//   PlayGames.getLeaderboardsClient(activity)
//       .submitScore("YOUR_LEADERBOARD_ID", score.toLong())
//
// NOTE: All Play Games calls are async (addOnSuccessListener / addOnFailureListener).
// Wrap them in a ViewModel + StateFlow if you want them Compose-friendly.
// ============================================================