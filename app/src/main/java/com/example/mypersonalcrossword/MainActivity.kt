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
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.hag.mypersonalcrossword.ui.theme.MyPersonalCrosswordTheme
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

enum class GameMode { SINGLE, TEAM, VINDICTIVE, DAILY }

enum class VindicativePhase { PICK_OWN, ASSIGN_CLUE, OPPONENT_WAIT }

// Word count caps for each difficulty level
enum class Difficulty(val label: String, val wordCount: Int, val emoji: String) {
    EASY("Easy",     8,  "🟢"),
    MEDIUM("Medium", 12, "🟡"),
    HARD("Hard",     15, "🔴"),
    EXPERT("Expert", 25, "🟣"),
    GENIUS("Genius", 50, "⭐")
}

data class RawEntry(val answer: String, val clue: String, val category: String)
data class PlacedWord(val word: String, val clue: String, val startX: Int, val startY: Int, val isHorizontal: Boolean, val number: Int = 0)
data class GridCell(val x: Int, val y: Int, val char: Char, val number: Int? = null)
data class PuzzleSave(
    val words: List<PlacedWord>,
    val inputs: Map<Pair<Int,Int>, Char>,
    val bgArgb: Int,
    val bgImageName: String
)

// Serialised as: category§diff§mode§hints§timeSeconds§score§partner§partnerScore§won
data class StatRecord(
    val category:     String,
    val diffName:     String,
    val gameMode:     String,   // GameMode.name
    val hintsUsed:    Int,
    val timeSeconds:  Long,
    val score:        Int,
    val partner:      String  = "",
    val partnerScore: Int     = 0,
    val won:          Boolean = true
)

// Forbidden delimiter chars in player / partner names. SaveManager serialises
// stat and puzzle records with §, |, and ; — letting the user type any of those
// silently corrupts the next save. Strip them at input time.
private val FORBIDDEN_NAME_CHARS = setOf('§', '|', ';', '\n', '\r', '\t')
fun sanitizeName(s: String): String =
    s.filter { it !in FORBIDDEN_NAME_CHARS }.take(24)

fun pointsForDifficulty(diff: Difficulty): Int = when (diff) {
    Difficulty.EASY   -> 1
    Difficulty.MEDIUM -> 2
    Difficulty.HARD   -> 4
    Difficulty.EXPERT -> 8
    Difficulty.GENIUS -> 20
}

fun checkStreakMilestone(streak: Int): String? = when {
    streak == 3              -> "On Fire! 🔥"
    streak == 5              -> "Unstoppable! ⚡"
    streak == 10             -> "Legendary! 🏆"
    streak == 15             -> "Crossword God! 👑"
    streak > 15 && streak % 5 == 0 -> "Still Going! 🌟 ($streak in a row)"
    else                     -> null
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
        val current = getUsedWords(name, category).toMutableSet()
        current.addAll(words)
        prefs.edit { putStringSet("used_${name}_${category}", current) }
    }

    // ── PUZZLE SAVE / RESUME ──────────────────────────────────────────────────
    // Serialisation format:
    //   PlacedWord fields: word§clue§startX§startY§isHorizontal§number  (words joined by |)
    //   UserInputs:       x|y|char  (entries joined by ;)
    // One save slot per player + category + difficulty combination.

    private fun saveKey(name: String, cat: String, diff: Difficulty) =
        "puzzle_${name}_${cat}_${diff.name}"

    fun savePuzzle(
        name: String, cat: String, diff: Difficulty,
        words: List<PlacedWord>,
        inputs: Map<Pair<Int,Int>, Char>,
        bgArgb: Int,
        bgImageName: String = BG_IMAGE_NONE
    ) {
        val wordsStr = words.joinToString("|") {
            "${it.word}§${it.clue}§${it.startX}§${it.startY}§${it.isHorizontal}§${it.number}"
        }
        val inputsStr = inputs.entries.joinToString(";") { (pos, ch) ->
            "${pos.first}|${pos.second}|$ch"
        }
        val k = saveKey(name, cat, diff)
        // Single atomic edit — prevents index/data mismatch if killed mid-save
        val saved = getSavedKeys(name).toMutableSet()
        saved.add("${cat}__${diff.name}")
        prefs.edit {
            putString("${k}_words",   wordsStr)
            putString("${k}_inputs",  inputsStr)
            putInt("${k}_bg",         bgArgb)
            putString("${k}_bgimg",   bgImageName)
            putLong("${k}_time",      System.currentTimeMillis())
            putStringSet("saves_$name", saved)
        }
    }

    fun loadPuzzle(name: String, cat: String, diff: Difficulty): PuzzleSave? {
        val k = saveKey(name, cat, diff)
        val wordsStr    = prefs.getString("${k}_words",  null) ?: return null
        val inputsStr   = prefs.getString("${k}_inputs", null) ?: return null
        val bgArgb      = prefs.getInt("${k}_bg", android.graphics.Color.LTGRAY)
        val bgImageName = prefs.getString("${k}_bgimg", BG_IMAGE_NONE) ?: BG_IMAGE_NONE
        return try {
            val words = wordsStr.split("|").filter { it.isNotBlank() }.map { seg ->
                val p = seg.split("§")
                PlacedWord(p[0], p[1], p[2].toInt(), p[3].toInt(), p[4].toBoolean(), p[5].toInt())
            }
            val inputs = if (inputsStr.isBlank()) emptyMap() else
                inputsStr.split(";").filter { it.isNotBlank() }.associate { seg ->
                    val p = seg.split("|")
                    Pair(p[0].toInt(), p[1].toInt()) to p[2][0]
                }
            PuzzleSave(words, inputs, bgArgb, bgImageName)
        } catch (_: Exception) { null }
    }

    fun clearPuzzle(name: String, cat: String, diff: Difficulty) {
        val k = saveKey(name, cat, diff)
        val saved = getSavedKeys(name).toMutableSet()
        saved.remove("${cat}__${diff.name}")
        prefs.edit {
            remove("${k}_words"); remove("${k}_inputs")
            remove("${k}_bg");    remove("${k}_bgimg"); remove("${k}_time")
            // Without this the timer carries over from the abandoned run.
            // Belt-and-suspenders — most win paths also call clearElapsed,
            // but back-out / save flows do not.
            remove("elapsed_${name}_${cat}_${diff.name}")
            putStringSet("saves_$name", saved)
        }
    }

    // Returns set of "CATEGORY__DIFFICULTY" strings for which a save exists
    fun getSavedKeys(name: String): Set<String> =
        prefs.getStringSet("saves_$name", emptySet()) ?: emptySet()

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
                        key.startsWith("used_${name}_") ||
                        key.startsWith("puzzle_${name}_") ||
                        key.startsWith("stat_${name}_") ||
                        key.startsWith("elapsed_${name}_")
                if (isPerPlayer) remove(key)
            }
            if (prefs.getString("last_user", "") == name) putString("last_user", "")
        }
    }

    // Returns list of (category, difficulty) pairs for in-progress saves for this player
    fun getInProgressPuzzles(name: String): List<Pair<String, Difficulty>> {
        val keys = getSavedKeys(name)
        return keys.mapNotNull { key ->
            val parts = key.split("__")
            if (parts.size == 2) {
                val cat  = parts[0]
                val diff = runCatching { Difficulty.valueOf(parts[1]) }.getOrNull()
                if (diff != null) Pair(cat, diff) else null
            } else null
        }.sortedWith(compareBy({ it.first }, { it.second.name }))
    }

    // ── STAT RECORDS ──────────────────────────────────────────────────────────
    // One best record per player + category + difficulty.
    // Serialised: category§diffName§gameMode§hints§time§score§partner§partnerScore§won
    private fun statKey(player: String, cat: String, diff: String) =
        "stat_${player}_${cat}_${diff}"

    fun saveStat(player: String, record: StatRecord) {
        val k = statKey(player, record.category, record.diffName)
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
        prefs.edit { putString(k, line) }
        // Track which stat keys this player has
        val keys = (prefs.getStringSet("statkeys_$player", emptySet()) ?: emptySet()).toMutableSet()
        keys.add("${record.category}__${record.diffName}")
        prefs.edit { putStringSet("statkeys_$player", keys) }
    }

    fun getAllStats(player: String): List<StatRecord> {
        val keys = prefs.getStringSet("statkeys_$player", emptySet()) ?: emptySet()
        return keys.mapNotNull { key ->
            // Defensive: a malformed key (legacy data, corrupt prefs) used to crash
            // the StatsScreen with IndexOutOfBoundsException. Skip silently instead.
            runCatching {
                val parts = key.split("__")
                if (parts.size < 2) return@runCatching null
                val (cat, diff) = parts[0] to parts[1]
                prefs.getString(statKey(player, cat, diff), null)?.let { parseStat(it) }
            }.getOrNull()
        }.sortedBy { it.category }
    }

    private fun parseStat(line: String): StatRecord? = try {
        val p = line.split("§")
        StatRecord(p[0], p[1], p[2], p[3].toInt(), p[4].toLong(), p[5].toInt(),
            p.getOrElse(6) { "" }, p.getOrElse(7) { "0" }.toInt(),
            p.getOrElse(8) { "true" }.toBoolean())
    } catch (_: Exception) { null }

    // ── TIMER SAVE ────────────────────────────────────────────────────────────
    fun saveElapsed(name: String, cat: String, diff: Difficulty, seconds: Long) =
        prefs.edit { putLong("elapsed_${name}_${cat}_${diff.name}", seconds) }

    fun loadElapsed(name: String, cat: String, diff: Difficulty): Long =
        prefs.getLong("elapsed_${name}_${cat}_${diff.name}", 0L)

    fun clearElapsed(name: String, cat: String, diff: Difficulty) =
        prefs.edit { remove("elapsed_${name}_${cat}_${diff.name}") }

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


// ── PASSIVE-AGGRESSIVE WRONG ANSWER TAUNTS ───────────────────────────────────
fun vindictiveTaunt(index: Int, playerName: String, player2Name: String): String {
    val taunts = listOf(
        "$player2Name knew you didn't know that. 😏",
        "That was what you thought? Really?",
        "I told $player2Name you'd never figure that out.",
        "Get good, scrub.",
        "Total pleb answer.",
        "Even $player2Name cringed a little.",
        "Was that a guess? Bold strategy.",
        "$playerName, honey… no.",
        "The answer was right there, $playerName.",
        "That's… not it, chief.",
        "$player2Name is going to love this.",
        "Yikes. Just… yikes.",
        "Did you just make that up?",
        "$playerName with the classic wrong answer.",
        "At least you tried. (You didn't try.)",
        "Wrong! $player2Name says thanks for the easy win.",
        "That one hurt to read, $playerName.",
        "Somewhere, $player2Name is smiling.",
        "Not even close. Not even in the same ZIP code.",
        "Confidently incorrect. Respect, kind of."
    )
    return taunts[index % taunts.size]
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

enum class OnlineRole { HOST, GUEST }

object FirebaseGameManager {
    private const val DB_URL = "https://mypersonalcrossword-default-rtdb.firebaseio.com/"
    private val db: DatabaseReference by lazy {
        FirebaseDatabase.getInstance(DB_URL).reference
    }
    private val codeChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun generateCode(): String = (1..6).map { codeChars.random() }.joinToString("")

    private fun gameRef(code: String) = db.child("games").child(code)

    fun signInAnonymously(onDone: () -> Unit) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) { onDone(); return }
        auth.signInAnonymously().addOnCompleteListener { onDone() }
    }

    fun createGame(
        code: String, hostName: String,
        mode: GameMode, category: String, difficulty: Difficulty
    ) {
        gameRef(code).setValue(mapOf(
            "mode" to mode.name, "category" to category, "difficulty" to difficulty.name,
            "hostName" to hostName, "guestName" to "", "status" to "waiting",
            "puzzle" to "", "inputs" to mapOf<String, String>(),
            "turn" to 0, "hostScore" to 0, "guestScore" to 0,
            "vindPhase" to VindicativePhase.PICK_OWN.name,
            "vindAssignedWordIdx" to -1, "vindCurrentPlayer" to 0, "solved" to false
        ))
    }

    fun joinGame(
        code: String, guestName: String,
        onSuccess: (mode: String, category: String, difficulty: String, hostName: String) -> Unit,
        onError: (String) -> Unit
    ) {
        gameRef(code).get()
            .addOnSuccessListener { snap ->
                if (!snap.exists()) { onError("Game not found. Check the code."); return@addOnSuccessListener }
                val status = snap.child("status").getValue(String::class.java) ?: ""
                if (status != "waiting") { onError("Game already started."); return@addOnSuccessListener }
                val existing = snap.child("guestName").getValue(String::class.java) ?: ""
                if (existing.isNotEmpty()) { onError("Game is full."); return@addOnSuccessListener }
                gameRef(code).child("guestName").setValue(guestName)
                onSuccess(
                    snap.child("mode").getValue(String::class.java) ?: "TEAM",
                    snap.child("category").getValue(String::class.java) ?: "",
                    snap.child("difficulty").getValue(String::class.java) ?: "MEDIUM",
                    snap.child("hostName").getValue(String::class.java) ?: ""
                )
            }
            .addOnFailureListener { onError("Connection error. Try again.") }
    }

    fun writePuzzle(code: String, words: List<PlacedWord>) {
        val serial = words.joinToString("|") {
            "${it.word}§${it.clue}§${it.startX}§${it.startY}§${it.isHorizontal}§${it.number}"
        }
        gameRef(code).updateChildren(mapOf("puzzle" to serial, "status" to "playing"))
    }

    fun parsePuzzle(raw: String): List<PlacedWord> {
        if (raw.isBlank()) return emptyList()
        return raw.split("|").mapNotNull { part ->
            val p = part.split("§")
            if (p.size >= 6) runCatching {
                PlacedWord(p[0], p[1], p[2].toInt(), p[3].toInt(), p[4].toBoolean(), p[5].toInt())
            }.getOrNull() else null
        }
    }

    fun writeInput(code: String, x: Int, y: Int, char: Char) {
        gameRef(code).child("inputs").child("${x}_${y}").setValue(char.toString())
    }

    fun writeState(code: String, updates: Map<String, Any>) {
        gameRef(code).updateChildren(updates)
    }

    fun listen(code: String, onUpdate: (DataSnapshot) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) { onUpdate(snap) }
            override fun onCancelled(e: DatabaseError) {}
        }
        gameRef(code).addValueEventListener(listener)
        return listener
    }

    fun stopListening(code: String, listener: ValueEventListener) {
        gameRef(code).removeEventListener(listener)
    }

    fun closeGame(code: String) {
        gameRef(code).child("status").setValue("complete")
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
    var resumePrompt by remember { mutableStateOf<Pair<String,Difficulty>?>(null) }
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
    val tauntIndex              = remember { mutableIntStateOf(0) }
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
    var currentStreak       by remember { mutableIntStateOf(0) }
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
    var dailyEntries        by remember { mutableStateOf(emptyList<RawEntry>()) }

    // Lock to portrait permanently — crossword grid only works in portrait.
    // Suppress lint: orientation lock is intentional for this game.
    @Suppress("SourceLockedOrientationActivity")
    LaunchedEffect(Unit) {
        (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        AmbientMusicPlayer.init(context)
        FirebaseGameManager.signInAnonymously {}   // sign in silently; no-op if already signed in
        profileList = saveManager.getAllPlayerNames()
        if (saveManager.isFirstLaunch()) {
            showHowToPlay = true
            saveManager.markLaunched()
        }
        // Try the renamed file first, fall back to the original name
        // so the app works whether or not test.csv has been renamed yet
        val raw = loadCsv(context, "word-hints.csv").ifEmpty {
            loadCsv(context, "test.csv")
        }
        allEntries = raw
        categories = raw.map { it.category }.distinct().sorted()

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
            appMode = AppMode.CATEGORY_SELECT
        }
    }

    // derivedStateOf means this only recomputes when gridCells or userInputs
    // actually change — not on every recomposition (e.g. while typing in dialog).
    val isWinner by remember { derivedStateOf {
        gridCells.isNotEmpty() && gridCells.all { cell ->
            userInputs[Pair(cell.x, cell.y)] == cell.char
        }
    }}

    LaunchedEffect(isWinner) {
        if (isWinner && !puzzleSolved && !isGenerating) {
            puzzleSolved = true
            timerRunning = false
            showConfetti = true
            if (isOnlineGame) FirebaseGameManager.writeState(onlineCode, mapOf("solved" to true))
            // Clear any lingering multiplayer dialogs so complete screen is unobstructed
            showTurnDialog = false
            vindPassDialogVisible = false
            pendingTurnDialog = false
            wordToInput = null
            if (soundEnabled) SoundPlayer.playCelebration()

            val pts = if (isDailyPuzzle) 20 else pointsForDifficulty(activeDifficulty)
            val netPts = (pts - hintsUsedThisPuzzle).coerceAtLeast(0)

            when (activeGameMode) {
                GameMode.SINGLE, GameMode.DAILY -> {
                    saveManager.addScore(playerName, netPts)
                    saveManager.addCompleted(playerName)
                    saveManager.addUsedWords(playerName, activeCategory, placedWords.map { it.word })
                    saveManager.clearPuzzle(playerName, activeCategory, activeDifficulty)
                    saveManager.clearElapsed(playerName, activeCategory, activeDifficulty)
                    // Save stat record
                    val diffLabel = if (isDailyPuzzle) "DAILY" else activeDifficulty.name
                    saveManager.saveStat(playerName, StatRecord(
                        activeCategory, diffLabel, activeGameMode.name,
                        hintsUsedThisPuzzle, elapsedSeconds, netPts))
                }
                GameMode.TEAM -> {
                    // Both players get full points. Guard against same-name double-credit
                    // (player typed their own name as Player 2, or two profiles share a name).
                    val sameName = player2Name.isNotBlank() && player2Name == playerName
                    saveManager.addScore(playerName, netPts)
                    saveManager.addCompleted(playerName)
                    if (player2Name.isNotBlank() && !sameName) {
                        saveManager.addScore(player2Name, netPts)
                        saveManager.addCompleted(player2Name)
                        saveManager.addUsedWords(player2Name, activeCategory, placedWords.map { it.word })
                    }
                    saveManager.addUsedWords(playerName, activeCategory, placedWords.map { it.word })
                    saveManager.clearPuzzle(playerName, activeCategory, activeDifficulty)
                    saveManager.clearElapsed(playerName, activeCategory, activeDifficulty)
                    // Each player's stat shows their own hint count
                    saveManager.saveStat(playerName, StatRecord(
                        activeCategory, activeDifficulty.name, "TEAM",
                        teamP1Hints, elapsedSeconds, netPts, player2Name))
                    if (player2Name.isNotBlank() && !sameName) {
                        saveManager.saveStat(player2Name, StatRecord(
                            activeCategory, activeDifficulty.name, "TEAM",
                            0, elapsedSeconds, netPts, playerName))   // hint tracking per-player TBD
                    }
                }
                GameMode.VINDICTIVE -> {
                    // Vindictive scoring is accumulated during play. On completion,
                    // bank the per-player score as their contribution to lifetime score
                    // (clamped at zero), credit a completed-puzzle to both, and write a
                    // stat record for each side with the head-to-head outcome.
                    val p1Award = vindP1Score.coerceAtLeast(0)
                    val p2Award = vindP2Score.coerceAtLeast(0)
                    val sameName = player2Name.isNotBlank() && player2Name == playerName
                    saveManager.addScore(playerName, p1Award)
                    saveManager.addCompleted(playerName)
                    saveManager.addUsedWords(playerName, activeCategory, placedWords.map { it.word })
                    if (player2Name.isNotBlank() && !sameName) {
                        saveManager.addScore(player2Name, p2Award)
                        saveManager.addCompleted(player2Name)
                        saveManager.addUsedWords(player2Name, activeCategory, placedWords.map { it.word })
                    }
                    saveManager.clearPuzzle(playerName, activeCategory, activeDifficulty)
                    saveManager.clearElapsed(playerName, activeCategory, activeDifficulty)
                    // Ties count as a win for both players so the StatsScreen doesn't
                    // unfairly mark a tie as DEMOLISHED.
                    val p1Won = vindP1Score >= vindP2Score
                    val p2Won = vindP2Score >= vindP1Score
                    saveManager.saveStat(playerName, StatRecord(
                        activeCategory, activeDifficulty.name, "VINDICTIVE",
                        hintsUsedThisPuzzle, elapsedSeconds, vindP1Score,
                        player2Name, vindP2Score, p1Won))
                    if (player2Name.isNotBlank() && !sameName) {
                        saveManager.saveStat(player2Name, StatRecord(
                            activeCategory, activeDifficulty.name, "VINDICTIVE",
                            0, elapsedSeconds, vindP2Score,
                            playerName, vindP1Score, p2Won))
                    }
                }
            }
            currentScore = saveManager.getScore(playerName)
            currentCompleted = saveManager.getCompleted(playerName)
        }
    }

    // Start/stop ambient music based on enabled state and volume
    LaunchedEffect(musicEnabled, musicVolume) {
        if (musicEnabled) AmbientMusicPlayer.start(musicVolume)
        else AmbientMusicPlayer.stop()
    }

    // Stop music when app is backgrounded; release only on activity destroy.
    // The previous `onDispose { release() }` fired on every config change, which
    // killed audio on rotation / theme change despite contradicting the
    // documented "pauses on background" behavior.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE   -> AmbientMusicPlayer.stop()
                Lifecycle.Event.ON_RESUME  -> if (musicEnabled) AmbientMusicPlayer.start(musicVolume)
                Lifecycle.Event.ON_DESTROY -> AmbientMusicPlayer.release()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Timer — ticks every second while puzzle is active
    LaunchedEffect(timerRunning) {
        while (timerRunning) {
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
                showTurnDialog = true
            }
        }
    }

    // Confetti — auto-clear after 3 seconds
    LaunchedEffect(showConfetti) {
        if (showConfetti) {
            delay(3000L)
            showConfetti = false
        }
    }

    // Daily prompt — shown once per login session on the first visit to the category screen
    LaunchedEffect(appMode) {
        if (appMode == AppMode.CATEGORY_SELECT && !dailyPromptShown) {
            dailyPromptShown = true
            showDailyPrompt  = true
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

    // Green cell highlight — auto-fade after 2 seconds (momentary indicator only)
    LaunchedEffect(highlightedWord) {
        if (highlightedWord != null) {
            delay(2000L)
            highlightedWord = null
        }
    }

    // ── Firebase sync listener — active only during online games ─────────────
    // Fires whenever any field in the game document changes on either device.
    LaunchedEffect(onlineCode, isOnlineGame) {
        val prev = onlineListener
        if (prev != null && onlineCode.isNotEmpty()) {
            FirebaseGameManager.stopListening(onlineCode, prev)
            onlineListener = null
        }
        if (!isOnlineGame || onlineCode.isEmpty()) return@LaunchedEffect

        val listener = FirebaseGameManager.listen(onlineCode) { snap ->
            val status  = snap.child("status").getValue(String::class.java) ?: return@listen
            val guestNm = snap.child("guestName").getValue(String::class.java) ?: ""

            // HOST: guest just joined — signal isGenerating so the existing LaunchedEffect starts the puzzle
            if (onlineRole == OnlineRole.HOST && appMode == AppMode.ONLINE_LOBBY && guestNm.isNotEmpty()) {
                player2Name  = guestNm
                onlineStatus = "$guestNm joined! Starting puzzle…"
                isGenerating = true   // triggers the LaunchedEffect(isGenerating) which calls generateCrossword()
                appMode      = AppMode.DASHBOARD
            }

            // GUEST: puzzle arrived — populate local state and jump into DASHBOARD
            if (onlineRole == OnlineRole.GUEST && status == "playing") {
                val rawPuzzle = snap.child("puzzle").getValue(String::class.java) ?: ""
                val parsed    = FirebaseGameManager.parsePuzzle(rawPuzzle)
                if (parsed.isNotEmpty() && appMode == AppMode.ONLINE_LOBBY) {
                    placedWords    = parsed
                    gridCells      = buildGridCells(parsed)
                    userInputs     = emptyMap()
                    elapsedSeconds = 0L
                    timerRunning   = true
                    isGenerating   = false
                    puzzleSolved   = false
                    appMode        = AppMode.DASHBOARD
                }
            }

            // Sync remote inputs (letters filled in by the other player)
            val rawInputs = snap.child("inputs").children.associate { child ->
                val parts = child.key?.split("_") ?: return@associate Pair(0,0) to ' '
                val x = parts.getOrNull(0)?.toIntOrNull() ?: 0
                val y = parts.getOrNull(1)?.toIntOrNull() ?: 0
                Pair(x, y) to (child.getValue(String::class.java)?.firstOrNull() ?: ' ')
            }.filter { it.value != ' ' }
            if (rawInputs != remoteInputs) {
                remoteInputs = rawInputs
                userInputs   = userInputs + rawInputs   // merge; local wins on same key
            }

            // Sync turn and scores (team + vindictive both use hostScore/guestScore)
            val remoteTurn       = snap.child("turn").getValue(Long::class.java)?.toInt()
            val remoteHostScore  = snap.child("hostScore").getValue(Long::class.java)?.toInt()
            val remoteGuestScore = snap.child("guestScore").getValue(Long::class.java)?.toInt()
            if (remoteTurn != null) teamCurrentPlayer = remoteTurn
            // HOST reads guest score; GUEST reads host score — avoids echo overwriting local writes
            if (remoteHostScore  != null && onlineRole == OnlineRole.GUEST) { teamP1Score = remoteHostScore;  vindP1Score = remoteHostScore }
            if (remoteGuestScore != null && onlineRole == OnlineRole.HOST)  { teamP2Score = remoteGuestScore; vindP2Score = remoteGuestScore }

            // Sync vindictive state
            val remoteVindPhase   = snap.child("vindPhase").getValue(String::class.java)
            val remoteVindPlayer  = snap.child("vindCurrentPlayer").getValue(Long::class.java)?.toInt()
            val remoteVindWordIdx = snap.child("vindAssignedWordIdx").getValue(Long::class.java)?.toInt() ?: -1
            val remoteVindCountdown = snap.child("vindCountdown").getValue(Long::class.java)?.toInt()
            if (remoteVindPhase != null)  runCatching { vindPhase = VindicativePhase.valueOf(remoteVindPhase) }
            if (remoteVindPlayer != null) vindCurrentPlayer = remoteVindPlayer
            vindAssignedWord = if (remoteVindWordIdx >= 0 && remoteVindWordIdx < placedWords.size)
                placedWords[remoteVindWordIdx] else null
            // Only update timer from Firebase if THIS device is NOT the one running it
            if (remoteVindCountdown != null && vindPhase != VindicativePhase.OPPONENT_WAIT) {
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

            // Sync solved
            val remoteSolved = snap.child("solved").getValue(Boolean::class.java) ?: false
            if (remoteSolved && !puzzleSolved) puzzleSolved = true
        }
        onlineListener = listener
    }

    // HOST: after puzzle is generated, write it to Firebase so the guest gets it
    LaunchedEffect(placedWords, isOnlineGame, onlineRole) {
        if (isOnlineGame && onlineRole == OnlineRole.HOST && placedWords.isNotEmpty() && !isGenerating) {
            FirebaseGameManager.writePuzzle(onlineCode, placedWords)
        }
    }

    LaunchedEffect(isGenerating) {
        if (isGenerating) {
            // Daily puzzle: use the pre-shuffled date-seeded pool (all categories).
            // Normal puzzle: filter allEntries by the active category.
            val pool = when {
                isDailyPuzzle                  -> dailyEntries
                combinedCategories.isNotEmpty() -> allEntries.filter { it.category in combinedCategories }
                else                            -> allEntries.filter { it.category == activeCategory }
            }
            val usedWords = saveManager.getUsedWords(playerName, if (isDailyPuzzle) "Daily" else activeCategory)

            val generated = withContext(Dispatchers.Default) {
                generateCrossword(pool, if (isDailyPuzzle) 20 else activeDifficulty.wordCount, usedWords)
            }

            placedWords = generated
            gridCells = buildGridCells(placedWords)
            userInputs = emptyMap()
            elapsedSeconds = 0L
            hintsUsedThisPuzzle = 0
            teamP1Hints = 0
            selectedHintWord = null
            vindP1Score = 0; vindP2Score = 0
            teamP1Score = 0; teamP2Score = 0
            teamCurrentPlayer = 0
            vindPhase = VindicativePhase.PICK_OWN
            // vindCurrentPlayer is set by the player setup dialog — don't overwrite it here
            vindAssignedWord = null
            highlightedWord = null
            selectedHintWord = null
            currentStreak = 0
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
        }
    }

    // Releases all online-game state when leaving DASHBOARD via any path.
    // Without this the Firebase listener leaks, the next puzzle keeps writing
    // to the previous game's room, and the opponent's "X is answering" flag
    // can stick true forever. Idempotent — safe to call when not online.
    fun cleanupOnlineSession() {
        if (isOnlineGame && onlineCode.isNotEmpty()) {
            // Clear our answering flag so the opponent doesn't see stale state.
            val field = if (onlineRole == OnlineRole.HOST) "p0answering" else "p1answering"
            runCatching {
                FirebaseGameManager.writeState(onlineCode, mapOf(field to false))
            }
        }
        onlineListener?.let { listener ->
            if (onlineCode.isNotEmpty()) FirebaseGameManager.stopListening(onlineCode, listener)
        }
        onlineListener = null
        if (onlineRole == OnlineRole.HOST && onlineCode.isNotEmpty()) {
            runCatching { FirebaseGameManager.closeGame(onlineCode) }
        }
        isOnlineGame = false
        onlineRole   = null
        onlineCode   = ""
        remoteIsAnswering   = false
        remoteAnsweringName = ""
    }

    fun launchPuzzle(category: String, difficulty: Difficulty,
                     resume: Boolean = false, daily: Boolean = false,
                     combined: List<String> = emptyList()) {
        activeCategory     = category
        activeDifficulty   = difficulty
        isDailyPuzzle      = daily
        combinedCategories = combined
        appMode            = AppMode.DASHBOARD
        puzzleSolved     = false
        // Ensure player 2 profile exists
        if (activeGameMode != GameMode.SINGLE && player2Name.isNotBlank()) {
            saveManager.ensurePlayer(player2Name)
        }
        if (resume) {
            val saved = saveManager.loadPuzzle(playerName, category, difficulty)
            if (saved != null) {
                placedWords        = saved.words
                gridCells          = buildGridCells(saved.words)
                userInputs         = saved.inputs
                currentBgColor     = Color(saved.bgArgb)
                currentBgImageName = saved.bgImageName
                elapsedSeconds     = saveManager.loadElapsed(playerName, category, difficulty)
                isGenerating       = false
                timerRunning       = true
                return
            }
        }
        isGenerating = true
    }

    fun launchDailyPuzzle() {
        val cal  = Calendar.getInstance()
        val seed = cal.get(Calendar.YEAR) * 10000L +
                cal.get(Calendar.MONTH) * 100L +
                cal.get(Calendar.DAY_OF_MONTH)
        // Shuffle ALL entries with date seed into a separate list — never mutates allEntries
        dailyEntries     = allEntries.shuffled(Random(seed))
        activeCategory   = "Daily"
        activeDifficulty = Difficulty.EXPERT
        isDailyPuzzle    = true
        appMode          = AppMode.DASHBOARD
        puzzleSolved     = false
        isGenerating     = true
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
    // LOGIN: let the system handle (exit app).
    BackHandler(enabled = appMode != AppMode.LOGIN) {
        vibrateLight(context)
        when (appMode) {
            AppMode.STATS -> appMode = AppMode.LOGIN
            AppMode.CATEGORY_SELECT -> {
                saveManager.setLastUser("")
                playerName = ""
                puzzleSolved = false
                showConfetti = false
                showTurnDialog = false
                vindPassDialogVisible = false
                pendingTurnDialog = false
                wordToInput = null
                showWrongFlash = false
                streakMilestone = null
                vindPhase = VindicativePhase.PICK_OWN
                dailyPromptShown = false
                appMode = AppMode.LOGIN
            }
            AppMode.ONLINE_LOBBY -> {
                cleanupOnlineSession()
                appMode = AppMode.CATEGORY_SELECT
            }
            AppMode.DASHBOARD -> {
                if (!puzzleSolved && !isGenerating && placedWords.isNotEmpty()) {
                    saveManager.saveElapsed(playerName, activeCategory, activeDifficulty, elapsedSeconds)
                    saveManager.savePuzzle(
                        playerName, activeCategory, activeDifficulty,
                        placedWords, userInputs,
                        android.graphics.Color.argb(
                            (currentBgColor.alpha * 255).toInt(),
                            (currentBgColor.red   * 255).toInt(),
                            (currentBgColor.green * 255).toInt(),
                            (currentBgColor.blue  * 255).toInt()
                        ),
                        bgImageName = currentBgImageName
                    )
                }
                cleanupOnlineSession()
                if (activeGameMode != GameMode.SINGLE) player2Name = ""
                puzzleSolved = false; showConfetti = false; showTurnDialog = false
                vindPassDialogVisible = false; pendingTurnDialog = false; wordToInput = null
                showWrongFlash = false; streakMilestone = null
                timerRunning = false
                appMode = AppMode.CATEGORY_SELECT
            }
            AppMode.LOGIN -> { /* never reached */ }
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
                            if (playerName.isNotBlank()) {
                                vibrateLight(context)
                                SoundPlayer.playClick()
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
                onResume         = { cat, diff ->
                    launchPuzzle(cat, diff, resume = true)
                },
                onBack           = { appMode = AppMode.LOGIN }
            )
        }

        AppMode.CATEGORY_SELECT -> {
            CategoryScreen(
                categories        = categories,
                playerName        = playerName,
                score             = currentScore,
                completed         = currentCompleted,
                currentStreak     = currentStreak,
                activeGameMode    = activeGameMode,
                // Mode tap = just set the mode. Tutorials/setup fire on START.
                onGameModeChange  = { newMode -> activeGameMode = newMode },
                player2Name       = player2Name,
                allEntries        = allEntries,
                usedWordCounts    = categories.associateWith { cat ->
                    saveManager.getUsedWords(playerName, cat).size
                },
                inProgressList    = saveManager.getInProgressPuzzles(playerName),
                onResume          = { cat, diff -> launchPuzzle(cat, diff, resume = true) },
                onStartPlay       = {
                    when (activeGameMode) {
                        GameMode.SINGLE -> {
                            if (saveManager.isFirstSingle()) {
                                showSingleTutorial = true
                                saveManager.markSingleSeen()
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
                                saveManager.markTeamSeen()
                            } else {
                                showPlayer2SetupDialog = true
                            }
                        }
                        GameMode.VINDICTIVE -> {
                            if (saveManager.isFirstVindictive()) {
                                showVindictiveTutorial = true
                                saveManager.markVindictiveSeen()
                            } else {
                                showPlayer2SetupDialog = true
                            }
                        }
                        GameMode.DAILY -> launchDailyPuzzle()
                    }
                },
                onDailyPuzzle     = { launchDailyPuzzle() },
                onChangeUser      = {
                    saveManager.setLastUser("")
                    playerName = ""
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
                                IconButton(onClick = {
                                    timerRunning = false
                                    if (!puzzleSolved && !isGenerating && placedWords.isNotEmpty()) {
                                        saveManager.saveElapsed(playerName, activeCategory, activeDifficulty, elapsedSeconds)
                                        saveManager.savePuzzle(
                                            playerName, activeCategory, activeDifficulty,
                                            placedWords, userInputs,
                                            android.graphics.Color.argb(
                                                (currentBgColor.alpha * 255).toInt(),
                                                (currentBgColor.red   * 255).toInt(),
                                                (currentBgColor.green * 255).toInt(),
                                                (currentBgColor.blue  * 255).toInt()
                                            ),
                                            bgImageName = currentBgImageName
                                        )
                                    }
                                    // Clear all overlays and multiplayer state on back navigation
                                    cleanupOnlineSession()
                                    if (activeGameMode != GameMode.SINGLE) player2Name = ""
                                    puzzleSolved = false
                                    showConfetti = false
                                    showTurnDialog = false
                                    vindPassDialogVisible = false
                                    pendingTurnDialog = false
                                    wordToInput = null
                                    showWrongFlash = false
                                    streakMilestone = null
                                    appMode = AppMode.CATEGORY_SELECT
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = appBtnColor)
                                }
                                Text("Puzzles: $currentCompleted", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = appBtnColor)
                            }
                        },
                        actions = {
                            // Persistent streak chip — appears once a streak begins
                            if (currentStreak > 0) {
                                Box(
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFFFEBA0))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        "🔥 $currentStreak",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF8A4500)
                                    )
                                }
                            }
                            Text("Score: $currentScore", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = appBtnColor)
                            IconButton(onClick = { showSettings = true }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = appBtnColor)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = appBtnColor.copy(alpha = 0.10f)
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
                    if (!puzzleSolved && !isGenerating) {
                        val hintEnabled = selectedHintWord != null && currentScore > 0
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .shadow(if (hintEnabled) 4.dp else 0.dp, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (hintEnabled) appBtnGradient
                                    else Brush.verticalGradient(listOf(Color.Gray.copy(alpha = 0.22f), Color.Gray.copy(alpha = 0.22f)))
                                )
                                .clickable(enabled = hintEnabled) {
                                    val word = selectedHintWord
                                    if (word != null && currentScore > 0) {
                                        vibrateLight(context)
                                        if (soundEnabled) SoundPlayer.playClick()
                                        val unfilledCells = word.word.indices.mapNotNull { i ->
                                            val pos = if (word.isHorizontal)
                                                Pair(word.startX + i, word.startY)
                                            else
                                                Pair(word.startX, word.startY + i)
                                            val correctChar = word.word[i]
                                            if (userInputs[pos] == correctChar) null else Pair(pos, correctChar)
                                        }
                                        if (unfilledCells.isNotEmpty()) {
                                            val (hintPos, hintChar) = unfilledCells.random()
                                            val newMap = userInputs.toMutableMap()
                                            newMap[hintPos] = hintChar
                                            userInputs = newMap
                                            currentScore = (currentScore - 1).coerceAtLeast(0)
                                            saveManager.addScore(playerName, -1)
                                            hintsUsedThisPuzzle++
                                            if (activeGameMode == GameMode.TEAM) teamP1Hints++
                                        }
                                        if (unfilledCells.size <= 1) selectedHintWord = null
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (hintEnabled) {
                                Box(Modifier.fillMaxWidth().height(1.5.dp)
                                    .background(Color.White.copy(alpha = 0.25f))
                                    .align(Alignment.TopCenter))
                            }
                            Text(
                                if (hintEnabled) "💡  Hint  (${currentScore} pts available)"
                                else if (selectedHintWord == null) "💡  Tap a clue first, then use Hint"
                                else "💡  No points available",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (hintEnabled) Color.White else Color.Gray
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
                        val vPhaseText   = when (vindPhase) {
                            VindicativePhase.PICK_OWN    -> "$vCurrentName — First turn! Double-tap a clue to pick it for yourself."
                            VindicativePhase.ASSIGN_CLUE -> "$vCurrentName — Double-tap a clue to assign to $vOtherName 👆"
                            VindicativePhase.OPPONENT_WAIT -> (
                                    if (showTurnDialog)
                                        "$vCurrentName — Your clue is waiting… hit I'm Ready when you have the phone!"
                                    else if (vindOpponentCountdown > 0)
                                        "$vCurrentName — ${vindAssignedWord?.clue ?: "?"} — Tap to answer! (${vindOpponentCountdown}s)"
                                    else
                                        "$vCurrentName — Time's up! Tap here to answer or pass."
                                    )
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .background(bannerBg)
                                .then(if (isWaiting && vindAssignedWord != null)
                                    Modifier.pointerInput(vindAssignedWord) {
                                        detectTapGestures(
                                            onTap = {
                                                // Single tap also opens answer for convenience
                                                wordToInput = vindAssignedWord
                                                inputText = ""
                                            },
                                            onDoubleTap = {
                                                wordToInput = vindAssignedWord
                                                inputText = ""
                                            }
                                        )
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
                            if (isWaiting && vindAssignedWord != null) {
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
                            Text("🤝 ${tCurrentName}'s turn", fontSize = 12.sp, fontWeight = FontWeight.Bold,
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
                                onSingleTap     = { word ->
                                    if (!isGenerating) {
                                        centreOnWord  = word
                                        highlightedWord = word
                                    }
                                },
                                onDoubleTap     = { word ->
                                    if (!puzzleSolved && !isGenerating) {
                                        // During OPPONENT_WAIT: only allow double-tap on the assigned clue (early answer)
                                        if (activeGameMode == GameMode.VINDICTIVE &&
                                            vindPhase == VindicativePhase.OPPONENT_WAIT &&
                                            word != vindAssignedWord) return@FullHintsList
                                        // If tapping assigned clue early: cancel the timer popup
                                        if (activeGameMode == GameMode.VINDICTIVE &&
                                            vindPhase == VindicativePhase.OPPONENT_WAIT) {
                                            vindPassDialogVisible = false
                                        }
                                        wordToInput     = word
                                        highlightedWord = word
                                        inputText       = ""
                                    }
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
                                    color    = Color.Gray,
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
                            .background(Color.Red.copy(alpha = 0.18f)),
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
                                        vindictiveTaunt(tauntIndex.intValue, playerName, player2Name),
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
                        onValueChange = { inputText = it.uppercase().take(word.word.length) },
                        label = { Text("(${word.word.length} Letters)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                    val isCorrect = inputText.length == word.word.length &&
                            inputText == word.word

                    if (activeGameMode == GameMode.VINDICTIVE) {
                        // ── Vindictive submit ──────────────────────────────────
                        if (vindPhase == VindicativePhase.PICK_OWN) {
                            // Player answered their own chosen clue
                            if (isCorrect) {
                                val newMap = userInputs.toMutableMap()
                                for (i in inputText.indices) {
                                    val c = if (word.isHorizontal) Pair(word.startX + i, word.startY)
                                    else Pair(word.startX, word.startY + i)
                                    newMap[c] = inputText[i]
                                    if (isOnlineGame) FirebaseGameManager.writeInput(onlineCode, c.first, c.second, inputText[i])
                                }
                                userInputs = newMap
                                if (vindCurrentPlayer == 0) vindP1Score++ else vindP2Score++
                                if (isOnlineGame) FirebaseGameManager.writeState(onlineCode, mapOf(
                                    "hostScore" to vindP1Score, "guestScore" to vindP2Score,
                                    "vindPhase" to VindicativePhase.ASSIGN_CLUE.name,
                                    "vindCurrentPlayer" to vindCurrentPlayer
                                ))
                                if (soundEnabled) SoundPlayer.playCorrect()
                                cellsToAnimate = word.word.indices.map { idx ->
                                    if (word.isHorizontal) Pair(word.startX + idx, word.startY)
                                    else Pair(word.startX, word.startY + idx) }
                            } else {
                                if (vindCurrentPlayer == 0) vindP1Score -= 1 else vindP2Score -= 1
                                if (soundEnabled) SoundPlayer.playWrong()
                                showWrongFlash = true
                                tauntIndex.intValue++
                                vibrateWrong(context)
                                pendingTurnDialog = false  // PICK_OWN wrong: no handoff needed, just continue
                            }
                            // Same player now picks clue for opponent
                            vindPhase = VindicativePhase.ASSIGN_CLUE
                        } else if (vindPhase == VindicativePhase.OPPONENT_WAIT) {
                            // Player answered the assigned clue (triggered via pass/answer dialog)
                            if (isCorrect) {
                                val newMap = userInputs.toMutableMap()
                                for (i in inputText.indices) {
                                    val c = if (word.isHorizontal) Pair(word.startX + i, word.startY)
                                    else Pair(word.startX, word.startY + i)
                                    newMap[c] = inputText[i]
                                    if (isOnlineGame) FirebaseGameManager.writeInput(onlineCode, c.first, c.second, inputText[i])
                                }
                                userInputs = newMap
                                if (vindCurrentPlayer == 0) vindP1Score++ else vindP2Score++
                                if (soundEnabled) SoundPlayer.playCorrect()
                                cellsToAnimate = word.word.indices.map { idx ->
                                    if (word.isHorizontal) Pair(word.startX + idx, word.startY)
                                    else Pair(word.startX, word.startY + idx) }
                                val myName = if (vindCurrentPlayer == 0) playerName else player2Name
                                turnDialogMessage = "✅ Correct! +1 pt for $myName\n$myName now picks a clue for the other player!\n$playerName: ${vindP1Score}pts  |  $player2Name: ${vindP2Score}pts"
                            } else {
                                if (vindCurrentPlayer == 0) vindP1Score -= 2 else vindP2Score -= 2
                                if (soundEnabled) SoundPlayer.playWrong()
                                showWrongFlash = true
                                tauntIndex.intValue++
                                vibrateWrong(context)
                                val myName = if (vindCurrentPlayer == 0) playerName else player2Name
                                turnDialogMessage = "❌ Wrong! -2 pts for $myName\n$myName now picks a clue for the other player!\n$playerName: ${vindP1Score}pts  |  $player2Name: ${vindP2Score}pts"
                                pendingTurnDialog = true  // show AFTER wrong flash clears
                            }
                            vindAssignedWord = null
                            vindPhase = VindicativePhase.ASSIGN_CLUE
                            if (isOnlineGame) {
                                FirebaseGameManager.writeState(onlineCode, mapOf(
                                    "vindPhase"           to VindicativePhase.ASSIGN_CLUE.name,
                                    "vindAssignedWordIdx" to -1,
                                    "vindCurrentPlayer"  to vindCurrentPlayer,
                                    "hostScore"          to vindP1Score,
                                    "guestScore"         to vindP2Score
                                ))
                            } else {
                                if (!showWrongFlash) showTurnDialog = true
                            }
                        }
                    } else {
                        // ── Normal / Team submit ───────────────────────────────
                        if (isCorrect) {
                            val newMap = userInputs.toMutableMap()
                            for (i in inputText.indices) {
                                val coords = if (word.isHorizontal) Pair(word.startX + i, word.startY)
                                else Pair(word.startX, word.startY + i)
                                newMap[coords] = inputText[i]
                                // Sync each filled cell to Firebase
                                if (isOnlineGame) FirebaseGameManager.writeInput(onlineCode, coords.first, coords.second, inputText[i])
                            }
                            userInputs = newMap
                            if (soundEnabled) SoundPlayer.playCorrect()
                            vibrateCorrect(context)
                            cellsToAnimate = word.word.indices.map { idx ->
                                if (word.isHorizontal) Pair(word.startX + idx, word.startY)
                                else Pair(word.startX, word.startY + idx) }
                            // Track team scores and switch turns
                            if (activeGameMode == GameMode.TEAM) {
                                if (teamCurrentPlayer == 0) teamP1Score++ else teamP2Score++
                                teamCurrentPlayer = 1 - teamCurrentPlayer
                                val nextTeamName = if (teamCurrentPlayer == 0) onlineP0Name else onlineP1Name
                                turnDialogMessage = "✋ Pass to $nextTeamName\n$onlineP0Name: ${teamP1Score} right  |  $onlineP1Name: ${teamP2Score} right"
                                showTurnDialog = !isOnlineGame   // online: no pass-the-phone dialog
                                // Sync turn + scores to Firebase
                                if (isOnlineGame) FirebaseGameManager.writeState(onlineCode, mapOf(
                                    "turn" to teamCurrentPlayer,
                                    "hostScore" to teamP1Score,
                                    "guestScore" to teamP2Score
                                ))
                            }
                            currentStreak++
                            val milestone = checkStreakMilestone(currentStreak)
                            if (milestone != null) {
                                streakMilestone = milestone
                                if (soundEnabled) SoundPlayer.playClap()
                            }
                        } else {
                            if (soundEnabled) SoundPlayer.playWrong()
                            vibrateWrong(context)
                            currentStreak = 0
                        }
                    }
                    wordToInput     = null
                    highlightedWord = null
                },
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
                    Text("Enter the 6-letter code from the host:", fontSize = 14.sp, color = Color.Gray)
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
                        FirebaseGameManager.signInAnonymously {
                            FirebaseGameManager.joinGame(
                                onlineJoinInput, playerName,
                                onSuccess = { mode, category, difficulty, hostName ->
                                    activeGameMode    = runCatching { GameMode.valueOf(mode) }.getOrDefault(GameMode.TEAM)
                                    activeCategory    = category
                                    activeDifficulty  = runCatching { Difficulty.valueOf(difficulty) }.getOrDefault(Difficulty.MEDIUM)
                                    player2Name       = hostName
                                    isOnlineGame      = true
                                    onlineRole        = OnlineRole.GUEST
                                    onlineCode        = onlineJoinInput
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
                            fontSize = 12.sp, color = Color.Gray
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
                                        fontSize = 11.sp, color = Color.Gray)
                                }
                            }

                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

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
                                        fontSize = 11.sp, color = Color.Gray)
                                }
                            }

                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

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
                                        fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Save & Quit
                Button(
                    onClick = {
                        showSettings = false
                        if (!puzzleSolved && !isGenerating && placedWords.isNotEmpty()) {
                            saveManager.savePuzzle(
                                playerName, activeCategory, activeDifficulty,
                                placedWords, userInputs,
                                android.graphics.Color.argb(
                                    (currentBgColor.alpha * 255).toInt(),
                                    (currentBgColor.red   * 255).toInt(),
                                    (currentBgColor.green * 255).toInt(),
                                    (currentBgColor.blue  * 255).toInt()
                                ),
                                bgImageName = currentBgImageName
                            )
                        }
                        cleanupOnlineSession()
                        if (activeGameMode != GameMode.SINGLE) player2Name = ""
                        puzzleSolved = false
                        showConfetti = false
                        showTurnDialog = false
                        vindPassDialogVisible = false
                        pendingTurnDialog = false
                        wordToInput = null
                        showWrongFlash = false
                        streakMilestone = null
                        appMode = AppMode.CATEGORY_SELECT
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("💾  Save & Quit to Menu", fontSize = 16.sp)
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
    if (vindPassDialogVisible && appMode == AppMode.DASHBOARD &&
        activeGameMode == GameMode.VINDICTIVE && !puzzleSolved) {
        val opName = if (vindCurrentPlayer == 0) playerName else player2Name
        AlertDialog(
            onDismissRequest = { /* can't dismiss — must choose */ },
            title = {
                Text("$opName — Your Clue!", fontWeight = FontWeight.Bold,
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
                        fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(4.dp))
                    Text("Time's up! Answer for +1 pt, or pass for -1 pt",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                }
            },
            confirmButton = {
                GradientBtn("Answer It  ▶", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                    vindPassDialogVisible = false
                    wordToInput = vindAssignedWord
                    inputText = ""
                }, modifier = Modifier.fillMaxWidth())
            },
            dismissButton = {
                GradientBtn("Pass  (-1 pt)", redGradient, onClick = {
                    vibrateLight(context)
                    if (vindCurrentPlayer == 0) vindP1Score-- else vindP2Score--
                    vindPassDialogVisible = false
                    vindAssignedWord = null
                    vindPhase = VindicativePhase.ASSIGN_CLUE
                    pendingTurnDialog = false
                    if (isOnlineGame) {
                        FirebaseGameManager.writeState(onlineCode, mapOf(
                            "vindPhase"           to VindicativePhase.ASSIGN_CLUE.name,
                            "vindAssignedWordIdx" to -1,
                            "vindCurrentPlayer"  to vindCurrentPlayer,
                            "hostScore"          to vindP1Score,
                            "guestScore"         to vindP2Score
                        ))
                    } else {
                        val opponentName = if (vindCurrentPlayer == 0) playerName else player2Name
                        turnDialogMessage = "⏭ $opponentName passed! -1 pt\n$opponentName now picks a clue for the other player!\n$playerName: ${vindP1Score}pts  |  $player2Name: ${vindP2Score}pts"
                        showTurnDialog = true
                    }
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
                            Text(desc, fontSize = 13.sp, color = Color.Gray,
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
                            Text(desc, fontSize = 13.sp, color = Color.Gray,
                                modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {
                GradientBtn("Got it — Set Up Players ▶", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
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
                            Text(desc, fontSize = 13.sp, color = Color.Gray,
                                modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {
                GradientBtn("Got it — Let's Play! 🎉", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
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
                            Text(desc, fontSize = 13.sp, color = Color.Gray,
                                modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {
                GradientBtn("Got it — Set Up Players ▶", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
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
                        fontSize = 12.sp, color = Color.Gray)
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
                            Text(desc, fontSize = 13.sp, color = Color.Gray,
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
            title = { Text("Pass the Phone!", fontWeight = FontWeight.Bold, fontSize = 20.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    turnDialogMessage.split("\n").forEach { line ->
                        Text(line, fontSize = if (line.contains("|")) 14.sp else 18.sp,
                            fontWeight = if (line.contains("✋")) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                            color = if (line.contains("|")) Color.Gray else Color.Unspecified)
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
                        fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Text("Host sets the rules. Guest just enters the code.",
                        fontSize = 12.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GradientBtn("🌐 Host Game", appBtnGradient, modifier = Modifier.weight(1f),
                            onClick = {
                                vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                                val code = FirebaseGameManager.generateCode()
                                isOnlineGame = true
                                onlineRole   = OnlineRole.HOST
                                onlineCode   = code
                                onlineStatus = ""
                                showPlayer2SetupDialog = false
                                FirebaseGameManager.signInAnonymously {
                                    FirebaseGameManager.createGame(code, playerName, activeGameMode, "", activeDifficulty)
                                }
                                if (activeGameMode == GameMode.VINDICTIVE) showVindTimerDialog = true
                                else showVindCategoryDialog = true
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
                        fontWeight = FontWeight.SemiBold, color = Color.Gray)
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
                        fontSize = 13.sp, color = Color.Gray)
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
                        fontSize = 12.sp, color = Color.Gray
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
                GradientBtn("Cancel", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                    showMultiCategoryDialog = false
                    combineSelection = emptyList()
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
                        fontSize = 12.sp, color = Color.Gray
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
                    val catSavedKeys = saveManager.getSavedKeys(playerName)
                    Difficulty.entries.forEach { diff ->
                        val hasSave = catSavedKeys.contains("${cat}__${diff.name}")
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
                                    } else if (activeGameMode != GameMode.SINGLE && player2Name.isBlank()) {
                                        showPlayer2SetupDialog = true
                                    } else {
                                        difficultyPickCategory = null
                                        if (hasSave) resumePrompt = Pair(cat, diff)
                                        else launchPuzzle(cat, diff, resume = false)
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
    resumePrompt?.let { (cat, diff) ->
        AlertDialog(
            onDismissRequest = { resumePrompt = null },
            title = { Text("Resume Puzzle?", fontWeight = FontWeight.Bold) },
            text  = { Text("You have an unfinished ${diff.label} puzzle in $cat. Resume where you left off, or start a new one?") },
            confirmButton = {
                GradientBtn("▶ Resume", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                    resumePrompt = null; launchPuzzle(cat, diff, resume = true)
                }, modifier = Modifier.fillMaxWidth())
            },
            dismissButton = {
                GradientBtn("New Puzzle", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                    resumePrompt = null
                    saveManager.clearPuzzle(playerName, cat, diff)
                    launchPuzzle(cat, diff, resume = false)
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

    // ── VINDICTIVE ASSIGN INTERCEPT ───────────────────────────────────────────
    // Intercept double-tap during ASSIGN_CLUE — store as assigned word, switch to opponent
    LaunchedEffect(wordToInput) {
        if (activeGameMode == GameMode.VINDICTIVE &&
            vindPhase == VindicativePhase.ASSIGN_CLUE &&
            wordToInput != null) {
            vindAssignedWord   = wordToInput
            wordToInput        = null
            vindCurrentPlayer  = 1 - vindCurrentPlayer    // switch to opponent
            vindPassDialogVisible = false
            vindOpponentCountdown = vindTimerSeconds
            vindPhase = VindicativePhase.OPPONENT_WAIT
            if (isOnlineGame) {
                // Sync assignment to the other device — no turn dialog needed online
                val wordIdx = placedWords.indexOf(vindAssignedWord)
                FirebaseGameManager.writeState(onlineCode, mapOf(
                    "vindPhase"          to VindicativePhase.OPPONENT_WAIT.name,
                    "vindAssignedWordIdx" to wordIdx,
                    "vindCurrentPlayer"  to vindCurrentPlayer,
                    "vindCountdown"      to vindTimerSeconds
                ))
            } else {
                val opponentName = if (vindCurrentPlayer == 0) playerName else player2Name
                turnDialogMessage = "✋ Pass to $opponentName!\nYou have a clue waiting — see below."
                showTurnDialog = true
            }
        }
    }

    // 5-second countdown then show pass/answer dialog for opponent
    // 30-second countdown — cancels if player answers early (wordToInput set)
    // Countdown only starts AFTER the "I'm Ready" handoff dialog is dismissed
    LaunchedEffect(vindPhase, vindCurrentPlayer, showTurnDialog) {
        if (activeGameMode == GameMode.VINDICTIVE &&
            vindPhase == VindicativePhase.OPPONENT_WAIT &&
            appMode == AppMode.DASHBOARD && !puzzleSolved &&
            !showTurnDialog) {  // wait until player hit I'm Ready
            vindPassDialogVisible = false
            vindOpponentCountdown = vindTimerSeconds
            for (i in vindTimerSeconds downTo 1) {
                if (vindPhase != VindicativePhase.OPPONENT_WAIT || showTurnDialog) return@LaunchedEffect
                vindOpponentCountdown = i
                if (isOnlineGame) FirebaseGameManager.writeState(onlineCode, mapOf("vindCountdown" to i))
                delay(1000L)
            }
            if (vindPhase == VindicativePhase.OPPONENT_WAIT && !puzzleSolved && !showTurnDialog && wordToInput == null) {
                vindOpponentCountdown = 0
                // Time's up = automatic wrong answer: -2 pts, move to assign-clue phase
                if (vindCurrentPlayer == 0) vindP1Score -= 2 else vindP2Score -= 2
                if (soundEnabled) SoundPlayer.playWrong()
                vibrateWrong(context)
                showWrongFlash = true
                vindAssignedWord = null
                vindPhase = VindicativePhase.ASSIGN_CLUE
                if (isOnlineGame) FirebaseGameManager.writeState(onlineCode, mapOf(
                    "vindPhase"           to VindicativePhase.ASSIGN_CLUE.name,
                    "vindAssignedWordIdx" to -1,
                    "vindCurrentPlayer"   to vindCurrentPlayer,
                    "hostScore"           to vindP1Score,
                    "guestScore"          to vindP2Score,
                    "vindCountdown"       to 0
                ))
            }
        }
    }

    if (puzzleSolved) {
        val winPts    = if (isDailyPuzzle) 20 else pointsForDifficulty(activeDifficulty)
        val winNet    = (winPts - hintsUsedThisPuzzle).coerceAtLeast(0)
        val winMins   = elapsedSeconds / 60
        val winSecs   = elapsedSeconds % 60
        val timeLabel = "%d:%02d".format(winMins, winSecs)
        val hintLabel = if (hintsUsedThisPuzzle == 0) "no hints" else "$hintsUsedThisPuzzle hint(s) used (−${hintsUsedThisPuzzle}pts)"
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Puzzle Complete! 🎉", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Nice work, $playerName!", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("+$winNet point(s) earned", fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                    Text(hintLabel, fontSize = 12.sp, color = Color.Gray)
                    Text("Time: $timeLabel", fontSize = 12.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                GradientBtn("Next Puzzle", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                    showTurnDialog = false; vindPassDialogVisible = false
                    pendingTurnDialog = false; wordToInput = null; showConfetti = false
                    launchPuzzle(activeCategory, activeDifficulty)
                }, modifier = Modifier.fillMaxWidth())
            },
            dismissButton = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GradientBtn("Change Category", appBtnGradient, onClick = {
                        vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                        cleanupOnlineSession()
                        puzzleSolved = false; showConfetti = false; showTurnDialog = false
                        vindPassDialogVisible = false; pendingTurnDialog = false; wordToInput = null
                        appMode = AppMode.CATEGORY_SELECT
                    }, modifier = Modifier.fillMaxWidth())
                    GradientBtn("Main Menu", appBtnGradient, onClick = {
                        vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                        cleanupOnlineSession()
                        saveManager.setLastUser(""); playerName = ""
                        puzzleSolved = false; showConfetti = false; showTurnDialog = false
                        vindPassDialogVisible = false; pendingTurnDialog = false; wordToInput = null
                        showWrongFlash = false; streakMilestone = null
                        vindPhase = VindicativePhase.PICK_OWN; dailyPromptShown = false
                        appMode = AppMode.LOGIN
                    }, modifier = Modifier.fillMaxWidth())
                }
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
                    scale    = (scale * zoom).coerceIn(0.1f, 4f)
                    offsetX += pan.x
                    offsetY += pan.y
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
            LaunchedEffect(maxWidth, maxHeight) {
                scale   = 1f
                offsetX = 0f
                offsetY = 0f
            }

            LaunchedEffect(centreOnWord) {
                val w   = centreOnWord ?: return@LaunchedEffect
                if (cells.isEmpty()) return@LaunchedEffect
                // Reset zoom to 1x first — centering at an arbitrary zoom level
                // looks wrong because the cell size is unexpected.
                scale = 1f
                // graphicsLayer translationX/Y are in PIXELS, so convert cellSize dp→px.
                // With scale=1 the formula simplifies: no scale multiplier needed.
                val csPx    = cellSizeState.value * densityState.value
                val wordPx  = (w.startX - minX + 0.5f) * csPx   // px from grid left edge
                val wordPy  = (w.startY - minY + 0.5f) * csPx   // px from grid top edge
                val gridWpx = gridW * csPx
                val gridHpx = gridH * csPx
                // Shift the grid so the tapped cell's centre lands at the container centre.
                offsetX = -(wordPx - gridWpx / 2f)
                offsetY = -(wordPy - gridHpx / 2f)
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
                            .size((gridW * cellSize).dp, (gridH * cellSize).dp)
                            .graphicsLayer(
                                scaleX       = scale,
                                scaleY       = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
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
    inProgressList:    List<Pair<String, Difficulty>>,
    onResume:          (String, Difficulty) -> Unit,
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
                val (cat, diff) = lastInProgress
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable {
                            vibrateLight(context)
                            if (soundEnabled) SoundPlayer.playClick()
                            onResume(cat, diff)
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
                                "▶  Continue $cat — ${diff.label}",
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
                        .clickable { vibrateLight(context); if (soundEnabled) SoundPlayer.playClick(); onDailyPuzzle() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    BevelHighlight(alpha = 0.35f)
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📅", fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
                            Text("DAILY PUZZLE", fontSize = 14.sp,
                                color = Color.Black, fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp)
                        }
                        Text("All categories • 20 words • 20 pts", fontSize = 11.sp,
                            color = Color.Black.copy(alpha = 0.65f))
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
                    contentPadding      = PaddingValues(horizontal = sidePadding, vertical = 22.dp),
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
            if (musicEnabled || !AmbientMusicPlayer.currentTrackName.isEmpty()) {
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
                        Text("Progress", fontSize = 11.sp, color = Color.Gray)
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
            items(acrossUnsolved) { ClueItem(it, userInputs, highlightedWord, onSingleTap, onDoubleTap) }
        }
        if (downUnsolved.isNotEmpty()) {
            item { SectionHeader("DOWN", Color(0xFF00838F)) }
            items(downUnsolved) { ClueItem(it, userInputs, highlightedWord, onSingleTap, onDoubleTap) }
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
            items(acrossSolved) { ClueItem(it, userInputs, highlightedWord, onSingleTap, onDoubleTap) }
            items(downSolved)   { ClueItem(it, userInputs, highlightedWord, onSingleTap, onDoubleTap) }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

// Returns true when every cell of this word is correctly filled by the player.
fun isWordSolved(word: PlacedWord, userInputs: Map<Pair<Int, Int>, Char>): Boolean =
    word.word.indices.all { i ->
        val pos = if (word.isHorizontal) Pair(word.startX + i, word.startY)
        else                   Pair(word.startX,      word.startY + i)
        userInputs[pos] == word.word[i]
    }

@Composable
fun ClueItem(
    word:            PlacedWord,
    userInputs:      Map<Pair<Int, Int>, Char>,
    highlightedWord: PlacedWord?,
    onSingleTap:     (PlacedWord) -> Unit,
    onDoubleTap:     (PlacedWord) -> Unit
) {
    val solved      = isWordSolved(word, userInputs)
    val isHighlight = word == highlightedWord
    val solvedGreen = Color(0xFF2E7D32)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isHighlight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
            .pointerInput(word) {
                detectTapGestures(
                    onTap       = { onSingleTap(word) },
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
            Text(text = "(${word.word.length} letters)", fontSize = 12.sp, color = Color.Gray)
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp, color = Color.LightGray)
    }
}

// ============================================================
// GENERATOR — BFS EXPANSION + CONSTRAINT INDEX
// Architecture from: VGV "How we developed a scalable, incredibly fast crossword generator"
//
// Key improvements over the old random-restart greedy loop:
//   1. Constraint index (len → pos → char → words): O(1) word lookup vs O(pool × wordLen)
//   2. Grid maps (char + direction): O(wordLen) validation vs O(placed × wordLen)
//   3. BFS expansion from seed: systematic, no wasted random restarts
//   4. Candidate queue with visited set: each cell tried once as a crossing point
// ============================================================

// ── CONSTRAINT INDEX ─────────────────────────────────────────────────────────
// Maps length → positionInWord → character → matching entries.
// Example: wordIndex[5][2]['R'] = all 5-letter words with 'R' at position 2.
// Built once from the full pool; shared across all 50 BFS attempts.
fun buildWordIndex(items: List<RawEntry>): HashMap<Int, HashMap<Int, HashMap<Char, MutableList<RawEntry>>>> {
    val index = HashMap<Int, HashMap<Int, HashMap<Char, MutableList<RawEntry>>>>()
    for (entry in items) {
        val len = entry.answer.length
        for (pos in entry.answer.indices) {
            index
                .getOrPut(len) { HashMap() }
                .getOrPut(pos) { HashMap() }
                .getOrPut(entry.answer[pos]) { mutableListOf() }
                .add(entry)
        }
    }
    return index
}

// ── GRID MAPS ────────────────────────────────────────────────────────────────
// gridChar: cell → character already placed there
// gridDir:  cell → direction of the word that placed it
//           true  = horizontal word only
//           false = vertical word only
//           null  = intersection (one H word + one V word share this cell)
fun addToGrid(
    word:     PlacedWord,
    gridChar: HashMap<Pair<Int,Int>, Char>,
    gridDir:  HashMap<Pair<Int,Int>, Boolean?>
) {
    val h = word.isHorizontal
    for (i in word.word.indices) {
        val pos = if (h) Pair(word.startX + i, word.startY)
        else   Pair(word.startX,      word.startY + i)
        gridChar[pos] = word.word[i]
        gridDir[pos]  = when {
            !gridDir.containsKey(pos) -> h      // first word to claim this cell
            gridDir[pos] != h         -> null    // opposite direction = intersection
            else                      -> h       // same dir (shouldn't occur in valid board)
        }
    }
}

// ── FAST VALIDATION ───────────────────────────────────────────────────────────
// O(wordLen) via grid maps instead of O(placed × wordLen).
// Three rules enforced:
//   1. End-caps before/after the word must be empty.
//   2. Occupied cells in the path must be valid perpendicular intersections
//      (wrong char = conflict; same-direction or already-intersection = co-linear).
//   3. Empty cells must have no perpendicular neighbors (prevents parallel word adjacency).
fun isValidFast(
    word:     String,  sx: Int, sy: Int, h: Boolean,
    gridChar: HashMap<Pair<Int,Int>, Char>,
    gridDir:  HashMap<Pair<Int,Int>, Boolean?>
): Boolean {
    val dx = if (h) 1 else 0
    val dy = if (h) 0 else 1

    // Rule 1 — end-caps
    if (gridChar.containsKey(Pair(sx - dx, sy - dy))) return false
    if (gridChar.containsKey(Pair(sx + word.length * dx, sy + word.length * dy))) return false

    var intersections = 0
    for (i in word.indices) {
        val pos          = Pair(sx + i * dx, sy + i * dy)
        val existingChar = gridChar[pos]

        if (existingChar != null) {
            // Rule 2 — occupied cell must be a valid perpendicular intersection
            if (existingChar != word[i]) return false
            val dir = gridDir[pos]
            if (dir == null || dir == h) return false   // already an intersection, or co-linear
            intersections++
        } else {
            // Rule 3 — empty cell must not be adjacent to a parallel word
            if (gridChar.containsKey(Pair(sx + i * dx - dy, sy + i * dy - dx))) return false
            if (gridChar.containsKey(Pair(sx + i * dx + dy, sy + i * dy + dx))) return false
        }
    }
    return intersections > 0   // word must connect to the existing board
}

// ── CANDIDATE QUEUE ───────────────────────────────────────────────────────────
// Every letter of a newly placed word is a potential crossing point.
// Enqueue all positions in the perpendicular direction; visited set prevents re-tries.
fun enqueueFromWord(
    word:    PlacedWord,
    queue:   ArrayDeque<Triple<Int,Int,Boolean>>,
    visited: HashSet<Triple<Int,Int,Boolean>>
) {
    val hNew = !word.isHorizontal
    for (i in word.word.indices) {
        val cx   = if (word.isHorizontal) word.startX + i else word.startX
        val cy   = if (word.isHorizontal) word.startY     else word.startY + i
        val cand = Triple(cx, cy, hNew)
        if (visited.add(cand)) queue.add(cand)
    }
}

// ── WORD FINDER ───────────────────────────────────────────────────────────────
// For a BFS candidate cell (cx, cy) and direction hNew:
//   1. Try each possible offset of (cx,cy) within the new word (posInWord 0..14).
//   2. Walk forward from the word start, collecting constraints (known chars from
//      existing intersections) and the maximum valid word length.
//   3. Query wordIndex[len][posInWord][crossChar] for matching entries.
//   4. Filter by all collected constraints, validate with isValidFast, then score.
fun findBestPlacement(
    cx:        Int, cy:    Int, hNew:     Boolean, crossChar: Char,
    inPool:    LinkedHashSet<String>,
    wordIndex: HashMap<Int, HashMap<Int, HashMap<Char, MutableList<RawEntry>>>>,
    gridChar:  HashMap<Pair<Int,Int>, Char>,
    gridDir:   HashMap<Pair<Int,Int>, Boolean?>,
    usedWords: Set<String>
): PlacedWord? {
    val dx = if (hNew) 1 else 0
    val dy = if (hNew) 0 else 1

    var bestWord:  PlacedWord? = null
    var bestScore              = Int.MIN_VALUE

    for (posInWord in 0..14) {
        val sx = cx - posInWord * dx
        val sy = cy - posInWord * dy

        // Before-cap must be empty — can't start here if previous cell is occupied
        if (gridChar.containsKey(Pair(sx - dx, sy - dy))) continue

        // Forward pass: collect constraints and determine max valid length
        val knownChars = mutableMapOf(posInWord to crossChar)
        var maxLen = 0

        for (ext in 0..19) {
            val wx    = sx + ext * dx
            val wy    = sy + ext * dy
            val wPos  = Pair(wx, wy)
            val wChar = gridChar[wPos]
            val wDir  = gridDir[wPos]

            when {
                wChar != null -> {
                    if (wDir == hNew) break         // co-linear cell — can't extend
                    knownChars[ext] = wChar          // perpendicular intersection — collect constraint
                }
                else -> {
                    // Empty cell — reject if a parallel word runs alongside
                    if (gridChar.containsKey(Pair(wx - dy, wy - dx))) break
                    if (gridChar.containsKey(Pair(wx + dy, wy + dx))) break
                }
            }

            // After-cap free? Then a word ending here (length ext+1) is a valid candidate.
            if (!gridChar.containsKey(Pair(wx + dx, wy + dy))) maxLen = ext + 1
        }

        if (maxLen < 2 || posInWord >= maxLen) continue

        // Query constraint index — prefer longer words (better grid coverage)
        for (len in maxLen downTo maxOf(posInWord + 1, 2)) {
            val candidates = wordIndex[len]?.get(posInWord)?.get(crossChar) ?: continue
            for (entry in candidates) {
                if (!inPool.contains(entry.answer)) continue
                // All collected constraints must match
                val allMatch = knownChars.all { (p, c) -> p < len && entry.answer[p] == c }
                if (!allMatch) continue
                // Final O(wordLen) validation
                if (!isValidFast(entry.answer, sx, sy, hNew, gridChar, gridDir)) continue

                val isInner     = posInWord > 0 && posInWord < len - 1
                val bridgeBonus = when {
                    knownChars.size >= 3 -> 900_000
                    knownChars.size == 2 -> 400_000
                    else                 -> 0
                }
                val score = (if (isInner) 30_000 else -15_000) +
                        bridgeBonus + len * 500 -
                        (if (usedWords.contains(entry.answer)) 30_000 else 0)

                if (score > bestScore) {
                    bestScore = score
                    bestWord  = PlacedWord(entry.answer, entry.clue, sx, sy, hNew)
                }
            }
        }
    }

    return if (bestScore > -20_000) bestWord else null
}

// ── MAIN GENERATOR ────────────────────────────────────────────────────────────
fun generateCrossword(items: List<RawEntry>, target: Int, usedWords: Set<String>): List<PlacedWord> {
    val unusedItems   = items.filter { !usedWords.contains(it.answer) }
    val usedItems     = items.filter {  usedWords.contains(it.answer) }
    val uniqueItems   = (unusedItems + usedItems).distinctBy { it.answer }

    val wordIndex     = buildWordIndex(uniqueItems)
    val entryByAnswer = uniqueItems.associateBy { it.answer }

    var bestResult = emptyList<PlacedWord>()
    var maxScore   = Long.MIN_VALUE

    repeat(50) { iteration ->
        val inPool = LinkedHashSet<String>().apply {
            unusedItems.shuffled().forEach { add(it.answer) }
            usedItems.shuffled().forEach   { add(it.answer) }
        }

        val gridChar = HashMap<Pair<Int,Int>, Char>(512)
        val gridDir  = HashMap<Pair<Int,Int>, Boolean?>(512)
        val placed   = mutableListOf<PlacedWord>()

        // Seed: place one long word to anchor the BFS expansion
        val anchorH  = (iteration % 2 == 0)
        val topWords = inPool.take(20).mapNotNull { entryByAnswer[it] }
            .sortedByDescending { it.answer.length }.take(5)
        if (topWords.isEmpty()) return@repeat
        val anchor   = topWords[Random.nextInt(topWords.size)]
        val seed     = PlacedWord(anchor.answer, anchor.clue, 0, 0, anchorH)
        placed.add(seed)
        inPool.remove(anchor.answer)
        addToGrid(seed, gridChar, gridDir)

        // BFS: expand from every letter of every placed word
        val visited = HashSet<Triple<Int,Int,Boolean>>(512)
        val queue   = ArrayDeque<Triple<Int,Int,Boolean>>()
        enqueueFromWord(seed, queue, visited)

        while (queue.isNotEmpty() && placed.size < target) {
            val (cx, cy, hNew) = queue.removeFirst()
            val crossChar = gridChar[Pair(cx, cy)] ?: continue

            val newWord = findBestPlacement(
                cx, cy, hNew, crossChar,
                inPool, wordIndex, gridChar, gridDir, usedWords
            ) ?: continue

            placed.add(newWord)
            inPool.remove(newWord.word)
            addToGrid(newWord, gridChar, gridDir)
            enqueueFromWord(newWord, queue, visited)
        }

        if (placed.size > 1) {   // BFS + isValidFast guarantees connectivity
            val finalArea          = calculateArea(placed)
            // Intersections counted directly from gridDir — O(cells) not O(placed×len)
            val finalIntersections = gridDir.values.count { it == null }
            val targetPenalty      = if (placed.size < target) (target - placed.size) * 1_000_000L else 0L

            val boardScore = (placed.size * 500_000L) +
                    (finalIntersections * 150_000L) -
                    (finalArea / 3L) -
                    targetPenalty

            if (boardScore > maxScore) {
                maxScore   = boardScore
                bestResult = placed.toList()
            }

            if (bestResult.size >= target && finalIntersections >= target - 2) return@repeat
        }
    }

    if (bestResult.isEmpty()) return emptyList()

    val minX       = bestResult.minOf { it.startX }
    val minY       = bestResult.minOf { it.startY }
    val normalized = bestResult.map { it.copy(startX = it.startX - minX, startY = it.startY - minY) }
    val sorted     = normalized.sortedWith(compareBy({ it.startY }, { it.startX }))
    val numMap     = mutableMapOf<Pair<Int, Int>, Int>(); var n = 1
    return sorted.map { w -> w.copy(number = numMap.getOrPut(Pair(w.startX, w.startY)) { n++ }) }
}

// ── SHARED HELPERS ────────────────────────────────────────────────────────────



fun getCellsForWord(w: PlacedWord): Set<Pair<Int, Int>> {
    val set = mutableSetOf<Pair<Int, Int>>()
    for (i in w.word.indices) {
        val cx = if (w.isHorizontal) w.startX + i else w.startX
        val cy = if (w.isHorizontal) w.startY     else w.startY + i
        set.add(Pair(cx, cy))
    }
    return set
}



fun calculateArea(words: List<PlacedWord>): Long {
    if (words.isEmpty()) return 0
    val minX = words.minOf { it.startX }
    val maxX = words.maxOf { if (it.isHorizontal) it.startX + it.word.length else it.startX }
    val minY = words.minOf { it.startY }
    val maxY = words.maxOf { if (it.isHorizontal) it.startY else it.startY + it.word.length }
    return (maxX - minX).toLong() * (maxY - minY).toLong()
}
fun buildGridCells(words: List<PlacedWord>): List<GridCell> {
    val map = mutableMapOf<Pair<Int, Int>, GridCell>()
    words.forEach { w ->
        for (i in w.word.indices) {
            val pos      = if (w.isHorizontal) Pair(w.startX + i, w.startY) else Pair(w.startX, w.startY + i)
            val existing = map[pos]
            val newNumber = when {
                i != 0       -> existing?.number          // interior cell: never assign a start number
                existing?.number == null -> w.number      // first word to claim this cell as its start
                else -> minOf(existing.number, w.number)  // two words start here: keep the lower number
            }
            map[pos] = GridCell(pos.first, pos.second, w.word[i], newNumber)
        }
    }
    return map.values.toList()
}

fun loadCsv(context: Context, file: String): List<RawEntry> {
    val list = mutableListOf<RawEntry>()
    try {
        context.assets.open(file).bufferedReader().useLines { lines ->
            lines.drop(1).forEach { line ->
                val parts = line.split(",", limit = 3)
                if (parts.size == 3) list.add(
                    RawEntry(parts[0].uppercase().trim(), parts[1].trim(), parts[2].uppercase().trim())
                )
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
    return list
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
        Text(label, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
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
                Text("Presets", fontSize = 13.sp, color = Color.Gray)
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
                Text("Recent (tap to apply)", fontSize = 13.sp, color = Color.Gray)
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
                    Text("Preview", fontSize = 11.sp, color = Color.Gray)
                    // Show hex-ish ARGB for the curious
                    Text(
                        text     = "#%06X".format(pickedColor.toArgb() and 0xFFFFFF),
                        fontSize = 11.sp,
                        color    = Color.Gray
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
                        Text("No images found", color = Color.Gray, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Add WebP images to\nassets/images/drawable-xxhdpi/",
                            color = Color.Gray, fontSize = 12.sp,
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
    onResume:         (String, Difficulty) -> Unit,  // resume a specific in-progress puzzle
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
                Text("Total Score", fontSize = 12.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$done", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.secondary)
                Text("Puzzles", fontSize = 12.sp, color = Color.Gray)
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
                items(inProgress) { (cat, diff) ->
                    Card(
                        modifier  = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors    = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        elevation = CardDefaults.cardElevation(2.dp),
                        onClick   = { onResume(cat, diff) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("$cat — ${diff.label}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Tap to resume", fontSize = 11.sp, color = Color.Gray)
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
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else if (stats.isNotEmpty()) {
                item {
                    Text("Completed Puzzles",
                        fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
                }
                items(stats) { stat ->
                    val isDiscontinued = stat.gameMode == "SINGLE" && !allCategoryNames.contains(stat.category)
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
                            Text(desc, fontSize = 12.sp, color = Color.Gray)
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