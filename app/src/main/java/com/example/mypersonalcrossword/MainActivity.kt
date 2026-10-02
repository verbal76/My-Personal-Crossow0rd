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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.ui.graphics.drawscope.rotate as canvasRotate
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalConfiguration
import kotlin.random.Random
import kotlin.math.sin
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.media.AudioManager
import android.media.AudioFocusRequest
import android.os.Handler
import android.os.Looper
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import com.hag.mypersonalcrossword.ui.theme.MyPersonalCrosswordTheme
import com.hag.mypersonalcrossword.core.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlin.math.abs as kabs
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

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

/** Short name of a combined puzzle: up to three categories joined, else a count. */
fun combinedLabel(categories: List<String>): String =
    if (categories.size <= 3) categories.joinToString("+") else "${categories.size} Categories"

/** "2026-09-27" → "Sep 27" (no java.time: minSdk 24). Falls back to the key. */
fun prettyDateKey(key: String): String {
    val parts = key.split('-')
    val month = parts.getOrNull(1)?.toIntOrNull() ?: return key
    val day   = parts.getOrNull(2)?.toIntOrNull() ?: return key
    val names = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    return names.getOrNull(month - 1)?.let { "$it $day" } ?: key
}

/** Player-facing name of a resumable save. */
fun slotTitle(slot: SaveSlot): String {
    // Combined slots are keyed by their full sorted category list; show the short label.
    val category = prettyCategory(if (slot.isCombined) combinedLabel(slot.combinedCategories) else slot.category)
    return when (slot.mode) {
        GameMode.DAILY      -> "Daily Puzzle · ${prettyDateKey(slot.category)}"
        GameMode.TEAM       -> "🤝 Team · $category · ${slot.difficulty.label}"
        GameMode.VINDICTIVE -> "⚔️ Vindictive · $category · ${slot.difficulty.label}"
        GameMode.SINGLE     -> "$category · ${slot.difficulty.label}"
    }
}

// ── BUILD INFORMATION ───────────────────────────────────────────────────────────
// Identifies exactly which build is installed (values injected by app/build.gradle.kts).
object BuildInfo {
    /** One line for footers: "v1.0 (42) · 0f78713 · debug". */
    val short: String get() =
        "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · ${BuildConfig.GIT_SHA} · ${BuildConfig.BUILD_TYPE}"

    fun details(context: Context): List<Pair<String, String>> {
        val pkg = runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }.getOrNull()
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
        return listOfNotNull(
            "Version"                 to BuildConfig.VERSION_NAME,
            "Build number"            to "${BuildConfig.VERSION_CODE} (versionCode)",
            "Source commit"           to BuildConfig.GIT_SHA,
            "Build type"              to BuildConfig.BUILD_TYPE,
            "Built"                   to BuildConfig.BUILD_TIME_UTC.replace('T', ' ').replace("Z", " UTC"),
            "Built by"                to BuildConfig.BUILD_ORIGIN,
            "Package"                 to BuildConfig.APPLICATION_ID,
            pkg?.let { "Installed / updated" to "${fmt.format(java.util.Date(it.firstInstallTime))} / ${fmt.format(java.util.Date(it.lastUpdateTime))}" },
            "Android"                 to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            "Updates"                 to "No over-the-air updates — new builds are installed as APKs"
        )
    }
}

@Composable
fun BuildInfoDialog(onDismiss: () -> Unit) {
    val context   = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val rows      = remember { BuildInfo.details(context) }
    var copied    by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Build details", style = MaterialTheme.typography.headlineSmall) },
        text  = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { (label, value) ->
                    Column(Modifier.semantics(mergeDescendants = true) {}) {
                        Text(label, style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(value, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                clipboard.setText(AnnotatedString(rows.joinToString("\n") { "${it.first}: ${it.second}" }))
                copied = true
            }) { Text(if (copied) "Copied ✓" else "Copy") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
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

// ── HAPTICS ──────────────────────────────────────────────────────────────────
// Short, crisp system presets where available (API 29+), gentle one-shots below.
// Every call respects the player's Haptics setting.
object Haptics {
    @Volatile var enabled = true

    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        else @Suppress("DEPRECATION") (context.getSystemService(VIBRATOR_SERVICE) as? Vibrator)

    fun play(context: Context, predefined: Int, fallbackMs: Long, fallbackAmp: Int) {
        if (!enabled) return
        try {
            val v = vibrator(context) ?: return
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                    v.vibrate(VibrationEffect.createPredefined(predefined))
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                    v.vibrate(VibrationEffect.createOneShot(fallbackMs, fallbackAmp))
                else -> @Suppress("DEPRECATION") v.vibrate(fallbackMs)
            }
        } catch (_: Exception) {}
    }

    fun waveform(context: Context, timings: LongArray, amps: IntArray, fallbackMs: Long) {
        if (!enabled) return
        try {
            val v = vibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(VibrationEffect.createWaveform(timings, amps, -1))
            else @Suppress("DEPRECATION") v.vibrate(fallbackMs)
        } catch (_: Exception) {}
    }
}

/** UI tap / key press. */
fun vibrateLight(context: Context) =
    Haptics.play(context, VibrationEffect.EFFECT_TICK, 18L, 70)

/** Word solved: a quick double pulse. */
fun vibrateCorrect(context: Context) =
    Haptics.waveform(context, longArrayOf(0, 28, 55, 36), intArrayOf(0, 150, 0, 210), 60L)

/** Wrong answer: one firm knock (was a 230 ms full-strength buzz). */
fun vibrateWrong(context: Context) =
    Haptics.play(context, VibrationEffect.EFFECT_HEAVY_CLICK, 70L, 200)

class SaveManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("CrosswordSaves", Context.MODE_PRIVATE)

    fun getLastUser(): String = prefs.getString("last_user", "") ?: ""
    fun setLastUser(name: String) = prefs.edit { putString("last_user", name) }

    fun getScore(name: String): Int = prefs.getInt("score_$name", 0)
    fun addScore(name: String, delta: Int = 1) =
        prefs.edit { putInt("score_$name", getScore(name) + delta) }

    // Boards this profile has been paid for (see core/Ledger.kt).
    fun isPuzzlePaid(name: String, fingerprint: String): Boolean =
        ledgerContains(prefs.getString("paid_$name", "") ?: "", fingerprint)
    fun markPuzzlePaid(name: String, fingerprint: String) = prefs.edit {
        putString("paid_$name", appendToLedger(prefs.getString("paid_$name", "") ?: "", fingerprint))
    }

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
        // Unfinished Dailies from more than a week ago are stale (every day has a new
        // one) and used to pile up in Continue forever.
        val today = DailyPuzzle.epochDayOf(DailyPuzzle.dateKey(System.currentTimeMillis())) ?: Long.MAX_VALUE
        fun staleDaily(slot: SaveSlot) = slot.mode == GameMode.DAILY &&
            (DailyPuzzle.normalizeKey(slot.category)?.let { DailyPuzzle.epochDayOf(it) }?.let { today - it > 7 } ?: true)
        slots.filter { (it.mode == GameMode.SINGLE && it.category == DailyPuzzle.CATEGORY) || staleDaily(it) }
            .forEach { clearPuzzle(name, it) }
        fun savedAt(slot: SaveSlot): Long =
            prefs.getLong(sessionTimeKey(name, slot), 0L).takeIf { it > 0 }
                ?: prefs.getLong("${legacyKey(name, slot.category, slot.difficulty)}_time", 0L)
        return slots
            .filterNot { (it.mode == GameMode.SINGLE && it.category == DailyPuzzle.CATEGORY) || staleDaily(it) }
            .sortedByDescending { savedAt(it) }
    }

    // ── DAILY COMPLETION ──────────────────────────────────────────────────────
    // Set of UTC date keys on which this profile solved the Daily (last 60 kept).
    // Keys are normalised to ASCII yyyy-MM-dd on read and write: builds before the
    // locale fix stored keys in the device's digits (Arabic, Persian, Bengali…), so
    // the same day could be paid twice after a language change.
    fun isDailyCompleted(name: String, dateKey: String): Boolean {
        val key = DailyPuzzle.normalizeKey(dateKey) ?: dateKey
        return key in getDailyCompletedKeys(name)
    }

    fun getDailyCompletedKeys(name: String): Set<String> =
        (prefs.getStringSet("dailydone_$name", emptySet()) ?: emptySet())
            .mapNotNull { DailyPuzzle.normalizeKey(it) }.toSet()

    fun markDailyCompleted(name: String, dateKey: String) {
        val key  = DailyPuzzle.normalizeKey(dateKey) ?: dateKey
        val done = getDailyCompletedKeys(name).plus(key).sortedDescending().take(60).toSet()
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
        val everyone = prefs.getStringSet("all_players", emptySet()) ?: emptySet()
        val current = everyone.toMutableSet()
        current.remove(name)
        // Sweep every key this profile owns. Ownership is exact (core PlayerKeys): a
        // plain prefix match also deleted other profiles whose names start with
        // "<name>_" (deleting "Kev" wiped "Kev_2").
        val owned = PlayerKeys.keysOwnedBy(prefs.all.keys, name, everyone)
        prefs.edit {
            putStringSet("all_players", current)
            owned.forEach { remove(it) }
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
        var existing = prefs.getString(k, null)?.let { parseStat(it) }
        // Older builds stored Team/Vindictive bests under the solo key, where they
        // could block solo personal bests for good. Move such a record to its own
        // namespaced key first, then compare against nothing.
        if (existing != null && existing.gameMode != record.gameMode && statIndexId(existing) != id) {
            saveStat(player, existing)
            existing = null
        }
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

    // Device-wide audio / haptics preferences (previously lost on every restart).
    fun isSoundEnabled(): Boolean   = prefs.getBoolean("sound_enabled", true)
    fun setSoundEnabled(on: Boolean) = prefs.edit { putBoolean("sound_enabled", on) }
    fun isMusicEnabled(): Boolean   = prefs.getBoolean("music_enabled", true)
    fun setMusicEnabled(on: Boolean) = prefs.edit { putBoolean("music_enabled", on) }
    fun isHapticsEnabled(): Boolean = prefs.getBoolean("haptics_enabled", true)
    fun setHapticsEnabled(on: Boolean) = prefs.edit { putBoolean("haptics_enabled", on) }

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
// Effects are synthesised once (core/SoundSynth: normalised, click-free), cached
// as WAVs, and played through a single SoundPool — instant, overlapping, and no
// per-sound thread or AudioTrack (which could exhaust the platform track limit).
object SoundPlayer {
    @Volatile var enabled = true
    private var pool: SoundPool? = null
    private val ids = java.util.concurrent.ConcurrentHashMap<SoundSynth.Effect, Int>()

    fun init(context: Context) {
        if (pool != null) return
        val app = context.applicationContext
        val sp = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            ).build()
        pool = sp
        Thread {
            val keep = HashSet<String>()
            for (effect in SoundSynth.Effect.entries) {
                try {
                    // Named by the rendered content, so a changed effect replaces its cached
                    // WAV after an update; written to a temp file and renamed, so a process
                    // killed mid-write can't leave a truncated file behind for good.
                    val wav = SoundSynth.toWav(SoundSynth.render(effect))
                    val name = "sfx_${effect.name.lowercase()}_${Integer.toHexString(wav.contentHashCode())}.wav"
                    keep += name
                    val f = java.io.File(app.cacheDir, name)
                    if (f.length() != wav.size.toLong()) {
                        val tmp = java.io.File(app.cacheDir, "$name.tmp")
                        tmp.writeBytes(wav)
                        if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
                    }
                    ids[effect] = sp.load(f.path, 1)
                } catch (e: Exception) {
                    android.util.Log.w("CrosswordSound", "Couldn't prepare $effect", e)
                }
            }
            // Drop WAVs from earlier versions.
            app.cacheDir.listFiles { f -> f.name.startsWith("sfx_") && f.name !in keep }?.forEach { it.delete() }
        }.also { it.isDaemon = true }.start()
    }

    private fun play(effect: SoundSynth.Effect, volume: Float = 1f, rate: Float = 1f) {
        if (!enabled) return
        val p  = pool ?: return
        val id = ids[effect] ?: return
        p.play(id, volume, volume, 1, 0, rate)
    }

    /** UI button press. */
    fun playClick()       = play(SoundSynth.Effect.CLICK, 0.7f)
    /** A letter typed into the grid. */
    fun playKey()         = play(SoundSynth.Effect.KEY, 0.55f)
    /** Countdown's last seconds. */
    fun playTick()        = play(SoundSynth.Effect.TICK, 0.6f)
    /** Word solved — pitch climbs with the answer streak. */
    fun playCorrect(streak: Int = 0) = play(SoundSynth.Effect.CORRECT, 1f, SoundSynth.streakRate(streak))
    fun playWrong()       = play(SoundSynth.Effect.WRONG, 0.9f)
    fun playClap()        = play(SoundSynth.Effect.CLAP, 0.9f)
    fun playCelebration() = play(SoundSynth.Effect.CELEBRATION, 1f)
}


// ── AMBIENT MUSIC PLAYER ──────────────────────────────────────────────────────
// Streams MP3 files from assets/Music/ using MediaPlayer, shuffled once, cycling.
//  • Requests audio focus: pauses for calls / other media, ducks for notifications.
//  • Fades in and out (start, stop, skip) instead of cutting hard.
//  • Prepares asynchronously (no main-thread stalls on skip).
//  • A broken file is skipped; if every file fails, playback stops instead of
//    recursing forever.
//
// @SuppressLint: we store applicationContext (process lifetime), not an Activity —
// this is safe. Lint can't distinguish the two, so the warning is a false positive.
@SuppressLint("StaticFieldLeak")
object AmbientMusicPlayer {
    private var player:   MediaPlayer? = null
    @Volatile private var vol          = 0.4f
    @Volatile private var enabled      = false
    private var context:  Context?     = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var hasFocus  = false
    private var ducked    = false
    private var failures  = 0
    private var prepared  = false                    // the current player finished prepareAsync
    private var gain      = 0f                       // volume currently applied to the player
    private val handler   = Handler(Looper.getMainLooper())
    private var fade: Runnable? = null

    // Shuffled playlist — built once, rotated each time we advance
    private var playlist:     List<String> = emptyList()
    private var trackIndex:   Int          = 0

    // Track name and playing state exposed to UI as Compose state
    var currentTrackName by mutableStateOf("")
        private set
    var isPlaying by mutableStateOf(false)
        private set

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {            // another app took over for good
                hasFocus = false
                isPlaying = false                        // the UI offers Play again
                fadeTo(0f) { runCatching { player?.pause() } }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {  // phone call, voice assistant
                fadeTo(0f) { runCatching { player?.pause() } }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> { ducked = true; fadeTo(vol * 0.25f) }
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasFocus = true; ducked = false
                if (enabled) { runCatching { if (player?.isPlaying == false) player?.start() }; fadeTo(vol) }
            }
        }
    }

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
        audioManager = ctx.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        context?.let { buildPlaylist(it) }
    }

    private fun requestFocus(): Boolean {
        if (hasFocus) return true
        val am = audioManager ?: return true
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setOnAudioFocusChangeListener(focusListener, handler)
                .setWillPauseWhenDucked(false)
                .build().also { focusRequest = it }
            am.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        hasFocus = granted == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasFocus
    }

    private fun abandonFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) focusRequest?.let { am.abandonAudioFocusRequest(it) }
        else @Suppress("DEPRECATION") am.abandonAudioFocus(focusListener)
        hasFocus = false
    }

    /** Ramps the applied volume to [target] over ~450 ms, then runs [then]. */
    private fun fadeTo(target: Float, then: (() -> Unit)? = null) {
        fade?.let { handler.removeCallbacks(it) }
        val steps = 15
        val start = gain
        var i = 0
        val r = object : Runnable {
            override fun run() {
                i++
                gain = start + (target - start) * (i.toFloat() / steps)
                runCatching { player?.setVolume(gain, gain) }
                if (i < steps) handler.postDelayed(this, 30L) else { fade = null; then?.invoke() }
            }
        }
        fade = r
        handler.post(r)
    }

    private fun prettyName(filename: String) = filename
        .substringBeforeLast(".")     // strip .mp3
        .replace(Regex("-\\d+"), "") // strip trailing licence numbers
        .replace("-", " ")
        .replaceFirstChar { it.uppercase() }

    // Prepare the track at trackIndex asynchronously; it fades in once ready.
    private fun playCurrentTrack() {
        val ctx = context ?: return
        if (playlist.isEmpty()) return
        val filename = playlist[trackIndex]
        currentTrackName = prettyName(filename)
        runCatching { player?.release() }
        player = null
        prepared = false
        gain = 0f
        try {
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
                setVolume(0f, 0f)
                setOnPreparedListener { mp ->
                    failures = 0
                    prepared = true
                    if (enabled && hasFocus) { mp.start(); fadeTo(if (ducked) vol * 0.25f else vol) }
                }
                setOnCompletionListener { advance() }
                setOnErrorListener { _, _, _ -> onTrackFailed(); true }
                prepareAsync()
            }
        } catch (_: Exception) { onTrackFailed() }
    }

    private fun onTrackFailed() {
        failures++
        if (failures >= playlist.size) {            // nothing playable — stop, don't loop forever
            runCatching { player?.release() }
            player = null
            prepared = false
            isPlaying = false
            currentTrackName = ""
            abandonFocus()                           // let the player's own music resume
            return
        }
        advance()
    }

    fun start(volume: Float) {
        vol     = volume
        enabled = true
        context?.let { buildPlaylist(it) }
        if (!requestFocus()) { isPlaying = false; return }
        isPlaying = true
        val p = player
        when {
            p == null  -> playCurrentTrack()
            !prepared  -> Unit                        // still preparing: it starts itself when ready
            else -> {
                runCatching { if (!p.isPlaying) p.start() }
                fadeTo(vol)
            }
        }
    }

    fun stop() {
        enabled = false
        isPlaying = false
        fadeTo(0f) { runCatching { player?.pause() } }
        abandonFocus()
    }

    fun release() {
        enabled = false
        isPlaying = false
        fade?.let { handler.removeCallbacks(it) }
        runCatching { player?.stop(); player?.release() }
        player = null
        prepared = false
        abandonFocus()
    }

    fun setVolume(volume: Float) {
        vol = volume.coerceIn(0f, 1f)
        if (fade == null && !ducked) { gain = vol; runCatching { player?.setVolume(vol, vol) } }
    }

    fun next() {
        if (playlist.isEmpty()) return
        fadeTo(0f) {
            trackIndex = (trackIndex + 1) % playlist.size
            playCurrentTrack()
        }
    }

    fun previous() {
        if (playlist.isEmpty()) return
        fadeTo(0f) {
            trackIndex = (trackIndex - 1 + playlist.size) % playlist.size
            playCurrentTrack()
        }
    }

    private fun advance() {
        if (playlist.isEmpty()) return
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

    /** Keeps only characters a game code can contain (Firebase paths reject . # $ [ ]). */
    fun sanitizeCode(input: String): String = input.uppercase().filter { it in CODE_CHARS }.take(CODE_LENGTH)
    fun isValidCode(code: String): Boolean = code.length == CODE_LENGTH && code.all { it in CODE_CHARS }

    private fun gameRef(code: String) = db.child("games").child(code)

    /** Set by the UI: told when the server refuses a write, so the two phones can't drift apart silently. */
    var onWriteRefused: ((String) -> Unit)? = null

    private fun <T> com.google.android.gms.tasks.Task<T>.reportFailure(what: String, code: String): com.google.android.gms.tasks.Task<T> =
        addOnFailureListener { e ->
            // Offline writes queue rather than fail, so a failure is the server refusing it.
            android.util.Log.w("CrosswordFirebase", "$what failed for game $code", e)
            onWriteRefused?.invoke("The game server refused an update. The other phone may be out of sync.")
        }

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
        if (!isValidCode(code)) { onError("Codes are 6 letters and numbers, like K7Q2MX."); return }
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
                    // With the shipped security rules, unknown / expired / full games are
                    // refused by the server rather than seen as empty.
                    error != null && error.code == DatabaseError.PERMISSION_DENIED ->
                        onError("That game isn't available — check the code, or ask the host for a new one.")
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
            .reportFailure("writePuzzle", code)
    }

    fun parsePuzzle(raw: String): List<PlacedWord> = SessionCodec.decodeWords(raw)

    /** All letters of a word in one update, so the other device receives them together. */
    fun writeInputs(code: String, letters: Map<Pair<Int, Int>, Char>) {
        if (letters.isEmpty()) return
        gameRef(code).child("inputs").updateChildren(letters.entries.associate { (c, ch) -> "${c.first}_${c.second}" to ch.toString() })
            .reportFailure("writeInputs", code)
    }

    fun writeInput(code: String, x: Int, y: Int, char: Char) {
        gameRef(code).child("inputs").child("${x}_${y}").setValue(char.toString())
            .reportFailure("writeInput", code)
    }

    fun writeState(code: String, updates: Map<String, Any>) {
        gameRef(code).updateChildren(updates).reportFailure("writeState ${updates.keys}", code)
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

// ── TITLE MARK & OPENING CARD ─────────────────────────────────────────────────

// The game's title: crossword-grid logo, "Welcome to / My Personal Crossword" and
// the tagline. Shared by the login screen and the opening card so they match.
@Composable
fun TitleMark() {
    // Theme primary rather than a fixed purple: the fixed one vanished on the dark
    // login gradient (about 1.35:1).
    val logoColor = MaterialTheme.colorScheme.primary
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                    color  = logoColor,
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
            fontWeight = FontWeight.Normal
        )
    }
}

/** Minimum time the opening card stays up, so it reads as intentional rather than a flash. */
const val OPENING_CARD_MIN_MS = 1200L
/** Longest the opening card waits for startup loading after that minimum. */
const val OPENING_CARD_MAX_WAIT_MS = 5000L

// Opening card for returning players: the title screen on the login gradient,
// shown while startup loading finishes. It swallows taps so nothing underneath
// can be pressed before it fades.
@Composable
fun OpeningCard() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.background
                ))
            )
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center
    ) {
        TitleMark()
    }
}

// The Settings gear: custom art, shown as drawn (never tinted), 24 dp inside
// IconButton's 48 dp touch target.
@Composable
fun SettingsGearButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Image(
            painter            = painterResource(R.drawable.ic_settings_gear),
            contentDescription = "Settings",
            modifier           = Modifier.size(24.dp)
        )
    }
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
            // Grows with large font sizes instead of clipping the label.
            .heightIn(min = 52.dp)
            .shadow(if (enabled) 4.dp else 0.dp, RoundedCornerShape(12.dp))
            // Disabled fades the whole button (it used to fade only the label).
            .graphicsLayer(alpha = if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(12.dp))
            .background(gradient)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        BevelHighlight()
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrosswordApp() {
    val context  = LocalContext.current
    val saveManager = remember { SaveManager(context) }
    // Puzzle state lives in a ViewModel (survives rotation, theme change and process death).
    val vm: PuzzleViewModel = viewModel()

    // Enums use listSaver — saves as List<String> which is natively bundleable.
    var appMode by rememberSaveable(
        stateSaver = listSaver(save = { listOf(it.name) }, restore = { AppMode.valueOf(it[0]) })
    ) { mutableStateOf(AppMode.LOGIN) }
    var playerName by rememberSaveable { mutableStateOf("") }
    // Opening card: returning players see the title screen once per cold start
    // while startup loading finishes (older builds showed it because the word
    // list loaded on the main thread). A recreation mid-session skips it.
    var showOpeningCard by rememberSaveable {
        mutableStateOf(appMode == AppMode.LOGIN && saveManager.getLastUser().isNotBlank())
    }
    var startupDone by remember { mutableStateOf(false) }

    var currentScore by rememberSaveable { mutableIntStateOf(0) }
    var currentCompleted by rememberSaveable { mutableIntStateOf(0) }

    var currentBgColor     by vm::currentBgColor
    var currentBgImageName by vm::currentBgImageName
    var bgImagePool        by remember { mutableStateOf(emptyList<String>()) }
    var activeCategory     by vm::activeCategory
    var combinedCategories by vm::combinedCategories

    var allEntries by remember { mutableStateOf(emptyList<RawEntry>()) }
    var categories by remember { mutableStateOf(emptyList<String>()) }
    var placedWords by vm::placedWords
    var gridCells by vm::gridCells
    var userInputs by vm::userInputs


    var puzzleSolved by vm::puzzleSolved
    var isGenerating by rememberSaveable { mutableStateOf(false) }
    // Identifies the current generation request. Leaving the puzzle or starting
    // another one bumps it, so a board that finishes late is thrown away instead
    // of being loaded (and autosaved) behind the player's back.
    var generationId by rememberSaveable { mutableIntStateOf(0) }

    // ── Online multiplayer state ───────────────────────────────────────────────
    var isOnlineGame     by vm::isOnline
    var onlineRole       by vm::onlineRole
    var onlineCode       by vm::onlineCode
    var onlineJoinInput  by remember { mutableStateOf("") }
    var onlineJoinError  by remember { mutableStateOf("") }
    var onlineStatus     by remember { mutableStateOf("") }  // shown on lobby screen
    var showOnlineJoin   by remember { mutableStateOf(false) }
    // Host/Join requests in flight: a newer request (or a cancel) makes older answers stale.
    var onlineRequest    by remember { mutableIntStateOf(0) }
    var onlinePending    by remember { mutableStateOf(false) }
    var onlineListener   by remember { mutableStateOf<ValueEventListener?>(null) }
    // Remote inputs received from the other device — merged into local userInputs on change
    var remoteInputs        by remember { mutableStateOf<Map<Pair<Int,Int>, Char>>(emptyMap()) }
    var remoteIsAnswering   by remember { mutableStateOf(false) }
    var remoteAnsweringName by remember { mutableStateOf("") }
    var activeDifficulty by vm::activeDifficulty
    var resumePrompt by remember { mutableStateOf<SaveSlot?>(null) }
    var difficultyPickCategory by remember { mutableStateOf<String?>(null) }
    var showSettings      by remember { mutableStateOf(false) }
    var showColorPicker   by remember { mutableStateOf(false) }
    var showBgPicker      by remember { mutableStateOf(false) }
    var showCustomize     by remember { mutableStateOf(false) }
    var soundEnabled       by rememberSaveable { mutableStateOf(saveManager.isSoundEnabled()) }
    var musicVolume        by rememberSaveable { mutableFloatStateOf(saveManager.getMusicVolume()) }
    var musicEnabled       by rememberSaveable { mutableStateOf(saveManager.isMusicEnabled()) }
    var hapticsEnabled     by rememberSaveable { mutableStateOf(saveManager.isHapticsEnabled()) }
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
    // Saveable: a restore mid-puzzle must not re-offer the Daily on top of whatever
    // dialog the player opens on the way home.
    var dailyPromptShown       by rememberSaveable { mutableStateOf(false) }
    // Derived from the saveable Int every recompose — safe because currentCellColorArgb is State
    val currentCellColor = Color(currentCellColorArgb.toLong() and 0xFFFFFFFFL)
    // Word the player single-tapped — used to pan grid to its start cell

    // ── GAME MODE STATE ───────────────────────────────────────────────────────
    var activeGameMode by vm::activeGameMode
    var player2Name         by vm::player2Name
    var viewingProfile      by remember { mutableStateOf("") }   // profile tapped on login
    var confirmDeletePlayer  by remember { mutableStateOf<String?>(null) }  // name pending deletion
    var showNoPlayer2Dialog  by remember { mutableStateOf(false) }  // missing p2 name warning
    var showPlayer2SetupDialog by remember { mutableStateOf(false) }  // popup to enter p2 name + who is p1
    var showTurnDialog         by remember { mutableStateOf(false) }  // between-turn popup
    var turnDialogMessage      by remember { mutableStateOf("") }     // text shown in turn popup
    // Team mode scoring
    var teamP1Score            by vm::teamP1Score
    var teamP2Score            by vm::teamP2Score
    var teamCurrentPlayer      by vm::teamCurrentPlayer  // 0=p1, 1=p2
    var profileList         by remember { mutableStateOf(listOf<String>()) }  // refreshable login list

    // ── VINDICTIVE STATE ──────────────────────────────────────────────────────
    var vindPhase by vm::vindPhase
    var vindCurrentPlayer   by vm::vindCurrentPlayer  // 0=p1, 1=p2
    var vindAssignedWord       by vm::vindAssignedWord
    var vindPassDialogVisible   by remember { mutableStateOf(false) }  // pass/answer popup
    var pendingTurnDialog       by remember { mutableStateOf(false) }  // delayed turn dialog after wrong flash
    var vindOpponentCountdown   by remember { mutableIntStateOf(30) }  // countdown seconds
    var vindTimerSeconds        by vm::vindTimerSeconds  // selected timer (15/30/60)
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
    var vindP1Score         by vm::vindP1Score
    var vindP2Score         by vm::vindP2Score
    var showWrongFlash      by remember { mutableStateOf(false) }

    // ── TEAM STATE ────────────────────────────────────────────────────────────
    var teamP1Hints         by vm::teamP1Hints

    // ── TIMER ─────────────────────────────────────────────────────────────────
    var elapsedSeconds      by vm::elapsedSeconds
    var timerRunning        by remember { mutableStateOf(false) }

    // ── HINTS ─────────────────────────────────────────────────────────────────
    var hintsUsedThisPuzzle by vm::hintsUsed

    // ── STREAK ────────────────────────────────────────────────────────────────
    var currentStreak       by vm::streak
    var streakMilestone     by remember { mutableStateOf<String?>(null) }

    // ── ANIMATION ─────────────────────────────────────────────────────────────

    // ── CONFETTI ──────────────────────────────────────────────────────────────
    var showConfetti        by remember { mutableStateOf(false) }

    // ── HIGHLIGHTED WORD (single tap) ─────────────────────────────────────────

    // ── DAILY ─────────────────────────────────────────────────────────────────
    var isDailyPuzzle       by vm::isDailyPuzzle
    // Date key (yyyy-MM-dd, UTC) of the Daily being played; null for every other puzzle.
    // Drives the Daily save slot, the once-per-day reward, and Next Puzzle behaviour.
    var activeDailyKey      by vm::activeDailyKey
    // Mode selected on the home screen before a Daily was started (Daily is always solo).
    var modeBeforeDaily     by rememberSaveable(
        stateSaver = listSaver(save = { listOf(it.name) }, restore = { GameMode.valueOf(it[0]) })
    ) { mutableStateOf(GameMode.SINGLE) }
    var revealedCells       by vm::revealedCells   // hint letters
    var teamP2Hints         by vm::teamP2Hints
    var teamFirstPlayer     by rememberSaveable { mutableIntStateOf(0) }   // "who goes first" for Team
    var appResumed          by remember { mutableStateOf(true) }           // Activity in foreground
    var onlineNotice        by remember { mutableStateOf<String?>(null) }  // blocking online message
    var dailyInfoMessage    by remember { mutableStateOf<String?>(null) }
    var homeRefresh         by remember { mutableIntStateOf(0) }           // bump to re-read saves on home
    var showResults         by remember { mutableStateOf(false) }          // results card (after confetti)
    var lastResult          by remember { mutableStateOf<PuzzleResult?>(null) }
    var turnDialogTitle     by remember { mutableStateOf("Pass the Phone!") }
    // ── Cell input ─────────────────────────────────────────────────────────
    var selection           by vm::selection
    val board               = remember(placedWords) { Board(placedWords) }
    // Derived, so the app root only recomposes when the *result* changes (a word is
    // solved, the cursor moves to another word) — not on every keystroke.
    val lockedCells by remember(board) { derivedStateOf { board.lockedCells(userInputs, revealedCells) } }
    val activeWord  by remember(board) { derivedStateOf { board.activeWord(selection) } }
    var waveFx              by remember { mutableStateOf<GridFx?>(null) }   // solved-word wave
    var shakeFx             by remember { mutableStateOf<GridFx?>(null) }   // wrong-word shake
    var showClueList        by remember { mutableStateOf(false) }
    var remoteToast         by remember { mutableStateOf<String?>(null) }
    var showBuildInfo       by remember { mutableStateOf(false) }   // online: what the opponent just did
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
        if (placedWords.all { isWordSolved(it, userInputs) }) return null   // finished grids are never "in progress"
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

    fun currentSlot(): SaveSlot = SaveSlot.forPuzzle(
        if (activeGameMode == GameMode.DAILY) GameMode.SINGLE else activeGameMode,
        activeCategory, activeDifficulty, combinedCategories, activeDailyKey)

    /** Starts a fresh puzzle for [slot] (a combined slot regenerates from its categories). */
    fun launchFromSlot(slot: SaveSlot) {
        val cats = slot.combinedCategories
        if (cats.isNotEmpty()) launchPuzzle(combinedLabel(cats), slot.difficulty, combined = cats)
        else launchPuzzle(slot.category, slot.difficulty)
    }

    fun resetOverlays() {
        showConfetti = false; showResults = false; showTurnDialog = false
        vindPassDialogVisible = false; pendingTurnDialog = false
        showWrongFlash = false; streakMilestone = null; showClueList = false
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
        // Resuming a Daily from Continue must remember the home mode it replaces.
        if (s.isDaily && activeGameMode != GameMode.DAILY) modeBeforeDaily = activeGameMode
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
        Board(s.words).let { b -> selection = InputEngine.initialSelection(b, s.inputs, b.lockedCells(s.inputs, s.revealed)) }
        elapsedSeconds     = s.elapsedSeconds
        hintsUsedThisPuzzle = s.hintsUsed
        currentStreak      = s.streak
        if (s.player2.isNotBlank()) player2Name = s.player2
        applyTeam(s.team)
        applyVind(s.vind)
        vindTimerSeconds   = s.vindTimerSecs
        currentBgColor     = Color(s.bgArgb)
        currentBgImageName = s.bgImage
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
        // Older builds keyed combined puzzles by their label; move such a save to its
        // canonical slot so it can't linger (or be overwritten) under the old one.
        if (s.slot != slot) { saveManager.savePuzzle(playerName, s); saveManager.clearPuzzle(playerName, slot) }
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
        generationId++
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
        generationId++
        isGenerating       = true
    }

    /** Leave the puzzle screen for home, saving progress (solo/local modes) first. */
    fun goHome() {
        timerRunning = false
        // Abandon any board still being generated for the screen we're leaving.
        if (isGenerating) { isGenerating = false; generationId++ }
        saveCurrentPuzzle()
        cleanupOnlineSession()
        resetOverlays()
        // Unload the puzzle (it's saved above if unfinished). Leaving a solved grid
        // loaded let a later recreation re-run completion and pay out twice.
        placedWords   = emptyList()
        gridCells     = emptyList()
        userInputs    = emptyMap()
        revealedCells = emptySet()
        selection     = null
        lastResult    = null
        puzzleSolved = false
        // The saved session keeps the Vindictive phase; clearing it here stops an
        // answer clock that was running on the puzzle screen.
        vindPhase        = VindicativePhase.PICK_OWN
        vindAssignedWord = null
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

    fun publishWord(word: PlacedWord) {
        if (!isOnlineGame) return
        FirebaseGameManager.writeInputs(onlineCode, word.cells().mapIndexed { i, c -> c to word.word[i] }.toMap())
    }

    /**
     * Writes a solved word into the grid. [publish] = false defers the online write
     * so an attempt's score/turn update reaches the other device BEFORE the letters
     * that might complete the puzzle there (otherwise it could finish with stale scores).
     */
    fun commitWord(word: PlacedWord, publish: Boolean = true) {
        val cells = word.cells()
        userInputs = userInputs + cells.mapIndexed { i, c -> c to word.word[i] }
        if (publish) publishWord(word)
        waveFx = GridFx(cells, System.nanoTime())
    }

    fun onCorrectFeedback() {
        currentStreak++
        if (soundEnabled) SoundPlayer.playCorrect(currentStreak - 1)
        vibrateCorrect(context)
        checkStreakMilestone(currentStreak)?.let { milestone ->
            streakMilestone = milestone
            if (soundEnabled) SoundPlayer.playClap()
        }
    }

    fun onWrongFeedback(answererIndex: Int, word: PlacedWord) {
        if (soundEnabled) SoundPlayer.playWrong()
        vibrateWrong(context)
        currentStreak = 0
        wrongAnswererIndex = answererIndex
        tauntIndex.intValue++
        showWrongFlash = true
        shakeFx = GridFx(word.cells(), System.nanoTime())
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

    /** Word the rules force the player onto (Vindictive: the assigned clue). */
    fun forcedWord(): PlacedWord? =
        if (activeGameMode == GameMode.VINDICTIVE && vindPhase == VindicativePhase.OPPONENT_WAIT) vindAssignedWord else null

    fun moveToNextUnsolved(forward: Boolean = true) {
        forcedWord()?.let { selection = InputEngine.selectWord(it, userInputs, lockedCells); return }
        selection = InputEngine.nextUnsolved(board, selection, userInputs, board.lockedCells(userInputs, revealedCells), forward)
    }

    /**
     * A word was completely filled by the player whose turn it is — judge it.
     * Solo/Daily keep wrong letters so they can be corrected; Team and Vindictive
     * attempts are one-shot, so a wrong attempt's letters are cleared.
     */
    fun onAttempt(word: PlacedWord, correct: Boolean) {
        when (activeGameMode) {
            GameMode.VINDICTIVE -> {
                val before = vindSnapshot()
                val actor  = before.currentPlayer
                val (after, outcome) = when (before.phase) {
                    VindicativePhase.PICK_OWN      -> VindictiveRules.onOwnAnswer(before, correct)
                    VindicativePhase.OPPONENT_WAIT -> VindictiveRules.onAssignedAnswer(before, correct)
                    VindicativePhase.ASSIGN_CLUE   -> return   // typing is blocked while assigning
                }
                if (correct) { commitWord(word, publish = false); onCorrectFeedback() } else onWrongFeedback(actor, word)
                // The turn changes hands: no half-typed letters carry over.
                userInputs = InputEngine.clearUnlocked(userInputs, board.lockedCells(userInputs, revealedCells))
                applyVind(after)
                syncVind(mapOf("vindCountdown" to 0))
                if (correct) publishWord(word)
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
                if (correct) { commitWord(word, publish = false); onCorrectFeedback() }
                else {
                    onWrongFeedback(before.turn, word)
                    userInputs = InputEngine.clearWord(word, userInputs, board.lockedCells(userInputs, revealedCells))
                }
                val after = TeamRules.onAttempt(before, correct)
                applyTeam(after)
                if (isOnlineGame) {
                    val myKey = if (onlineRole == OnlineRole.HOST) "hostScore" else "guestScore"
                    val myVal = if (onlineRole == OnlineRole.HOST) after.p1Correct else after.p2Correct
                    FirebaseGameManager.writeState(onlineCode, mapOf("turn" to after.turn, myKey to myVal))
                    if (correct) publishWord(word)
                } else {
                    turnDialogTitle   = "Pass the Phone!"
                    turnDialogMessage = (if (correct) "✅ Nice one, ${nameOf(before.turn)}!" else "❌ Not quite, ${nameOf(before.turn)}.") +
                        "\n✋ Pass to ${nameOf(after.turn)}\n" +
                        "${nameOf(0)}: ${after.p1Correct} solved  |  ${nameOf(1)}: ${after.p2Correct} solved"
                    if (correct) showTurnDialog = true else pendingTurnDialog = true
                }
            }
            GameMode.SINGLE, GameMode.DAILY -> {
                if (correct) { commitWord(word); onCorrectFeedback() } else onWrongFeedback(0, word)
            }
        }
        if (correct || activeGameMode != GameMode.SINGLE && activeGameMode != GameMode.DAILY) moveToNextUnsolved()
    }

    /** Judges the words a keystroke (or hint) completed. Only the active word is an "attempt". */
    fun judgeFilled(filled: List<PlacedWord>, active: PlacedWord) {
        if (filled.isEmpty()) return
        // A crossing word finished correctly by the same letter simply counts as solved.
        filled.filter { it != active && isWordSolved(it, userInputs) }.forEach { w ->
            commitWord(w)
            if (activeGameMode == GameMode.SINGLE || activeGameMode == GameMode.DAILY) onCorrectFeedback()
        }
        if (active in filled) onAttempt(active, isWordSolved(active, userInputs))
        // Solo: a crossing word completed with a wrong letter isn't an attempt (no
        // penalty), but it must not stay full and silently "unfinished" — shake it.
        if (activeGameMode == GameMode.SINGLE || activeGameMode == GameMode.DAILY) {
            val wrongCrossing = filled.filter { it != active && !isWordSolved(it, userInputs) }
            if (wrongCrossing.isNotEmpty()) {
                val activeWrong = if (active in filled && !isWordSolved(active, userInputs)) active.cells() else emptyList()
                shakeFx = GridFx((activeWrong + wrongCrossing.flatMap { it.cells() }).distinct(), System.nanoTime())
            }
        }
    }

    /** Why the keyboard is disabled right now; null when this player may type. */
    fun typingBlockedReason(): String? = when {
        isGenerating || puzzleSolved || placedWords.isEmpty() -> ""
        showTurnDialog || pendingTurnDialog -> "Pass the phone…"
        activeGameMode == GameMode.VINDICTIVE -> {
            val s = vindSnapshot()
            when {
                !VindictiveRules.canAct(s, isOnlineGame, myIndex()) -> "${nameOf(s.currentPlayer)}'s turn"
                s.phase == VindicativePhase.ASSIGN_CLUE -> "Choose a clue for ${nameOf(1 - s.currentPlayer)}, then tap Give"
                vindPassDialogVisible -> "Time's up — answer or pass"
                else -> null
            }
        }
        activeGameMode == GameMode.TEAM && isOnlineGame && teamCurrentPlayer != myIndex() ->
            "Waiting for ${nameOf(teamCurrentPlayer)}…"
        else -> null
    }

    /** Locked cells computed from live state (not the last composition's snapshot). */
    fun lockedNow(): Set<Pair<Int, Int>> = board.lockedCells(userInputs, revealedCells)

    fun onCellTap(cell: Pair<Int, Int>) {
        val forced = forcedWord()
        selection = if (forced != null) {
            if (cell in forced.cells()) Selection(cell, forced.direction)
            else InputEngine.selectWord(forced, userInputs, lockedNow())
        } else InputEngine.tap(board, selection, cell, lockedNow())   // prefers the unsolved word at a crossing
        vibrateLight(context)
    }

    fun selectClue(word: PlacedWord) {
        val forced = forcedWord()
        if (forced != null && word != forced) return
        selection = InputEngine.selectWord(word, userInputs, lockedNow())
    }

    fun toggleDirection() {
        val sel = selection ?: return
        if (forcedWord() != null) return
        selection = InputEngine.tap(board, sel, sel.cell)
    }

    fun onKey(ch: Char) {
        if (typingBlockedReason() != null) return
        val forced = forcedWord()
        var sel = selection
        if (sel == null || (forced != null && board.activeWord(sel) != forced)) {
            sel = forced?.let { InputEngine.selectWord(it, userInputs, lockedNow()) }
                ?: InputEngine.initialSelection(board, userInputs, lockedNow()) ?: return
        }
        var active = board.activeWord(sel) ?: return
        if (isWordSolved(active, userInputs)) {
            // A solved word takes no letters. Carry the keystroke to the unsolved word
            // crossing this square (or the next unsolved clue) instead of dropping it.
            val here = sel ?: return
            val crossing = board.wordsAt(here.cell).firstOrNull { it != active && !isWordSolved(it, userInputs) }
            sel = if (crossing != null && forced == null) InputEngine.selectWord(crossing, userInputs, lockedNow())
                  else { moveToNextUnsolved(); selection ?: return }
            active = board.activeWord(sel) ?: return
            if (isWordSolved(active, userInputs)) return
        }
        val r = InputEngine.type(board, sel, userInputs, lockedNow(), ch)
        if (r.inputs == userInputs && r.selection == sel) return
        userInputs = r.inputs
        selection  = r.selection
        if (soundEnabled) SoundPlayer.playKey()
        vibrateLight(context)
        judgeFilled(r.filled, active)
    }

    fun onBackspace() {
        if (typingBlockedReason() != null) return
        val r = InputEngine.backspace(board, selection, userInputs, lockedNow())
        userInputs = r.inputs
        selection  = r.selection
        vibrateLight(context)
    }

    /** Vindictive: the acting player hands [word] to the opponent. */
    fun assignClue(word: PlacedWord) {
        val idx = placedWords.indexOf(word)
        if (idx < 0 || isWordSolved(word, userInputs)) return
        val after = VindictiveRules.onAssign(vindSnapshot(), idx)
        applyVind(after)
        vindPassDialogVisible = false
        vindOpponentCountdown = vindTimerSeconds
        syncVind(mapOf("vindCountdown" to vindTimerSeconds))
        selection = InputEngine.selectWord(word, userInputs, lockedCells)
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
        userInputs = InputEngine.clearUnlocked(userInputs, board.lockedCells(userInputs, revealedCells))
        syncVind(mapOf("vindCountdown" to 0))
        moveToNextUnsolved()
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
        vindPassDialogVisible = true  // letters typed so far stay in the grid for "Answer It"
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
        SoundPlayer.init(context)
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
            // Offline puzzles come back through the ViewModel's saved state. A puzzle
            // or lobby screen restored without its grid was an online game, which
            // can't be resumed without the other phone: go home and say so (it used
            // to open whichever offline save was most recent instead).
            val lostOnline = (appMode == AppMode.DASHBOARD && placedWords.isEmpty() && !isGenerating) ||
                (appMode == AppMode.ONLINE_LOBBY && !isOnlineGame)
            if (lostOnline) {
                appMode = AppMode.CATEGORY_SELECT
                if (vm.restoredFromOnline) onlineNotice = "The online game ended when the app was closed."
            }
            if (appMode == AppMode.LOGIN) appMode = AppMode.CATEGORY_SELECT
            // The ViewModel brought the grid back by itself (SavedStateHandle).
            if (appMode == AppMode.DASHBOARD && placedWords.isNotEmpty()) {
                if (puzzleSolved && lastResult == null) {
                    // Rewards were already paid before the process died; unload it fully.
                    goHome()
                } else if (!isGenerating) {
                    timerRunning = true
                    if (!isOnlineGame && activeGameMode == GameMode.VINDICTIVE &&
                        vindPhase == VindicativePhase.OPPONENT_WAIT && vindAssignedWord != null) {
                        turnDialogTitle   = "Pass the Phone!"
                        turnDialogMessage = "✋ ${nameOf(vindCurrentPlayer)} — your clue is waiting!"
                        showTurnDialog    = true
                    }
                }
            }
        } else {
            appMode = AppMode.LOGIN
        }
        startupDone = true
    }

    // Hold the opening card for its minimum time and until startup has finished
    // (capped, so a failed load can never leave the player stuck on it).
    LaunchedEffect(showOpeningCard) {
        if (!showOpeningCard) return@LaunchedEffect
        delay(OPENING_CARD_MIN_MS)
        withTimeoutOrNull(OPENING_CARD_MAX_WAIT_MS) { snapshotFlow { startupDone }.first { it } }
        showOpeningCard = false
    }

    // ── COMPLETION ────────────────────────────────────────────────────────────
    LaunchedEffect(isWinner) {
        if (!isWinner || puzzleSolved || isGenerating || appMode != AppMode.DASHBOARD) return@LaunchedEffect
        puzzleSolved = true
        timerRunning = false
        if (isOnlineGame) FirebaseGameManager.writeState(onlineCode, mapOf("solved" to true))
        // Clear any lingering multiplayer dialogs so complete screen is unobstructed
        showTurnDialog = false
        vindPassDialogVisible = false
        pendingTurnDialog = false
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
        // A board pays out once. A restored copy of a finished puzzle (a saved state
        // taken a frame before its payout) shows the result but credits nothing.
        val fingerprint = puzzleFingerprint(placedWords)
        val boardPaid   = saveManager.isPuzzlePaid(playerName, fingerprint)
        if (!boardPaid) saveManager.markPuzzlePaid(playerName, fingerprint)

        if (!boardPaid) when (activeGameMode) {
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
            awarded = if (activeGameMode == GameMode.VINDICTIVE) null else if (boardPaid) 0 else award,
            dailyAlreadyPaid = dailyAlreadyPaid, seconds = elapsedSeconds,
            team = team, vind = vind, myIndex = me
        )
        // Celebrate first; the results card follows once the confetti has had a moment.
        showConfetti = true
        if (soundEnabled) SoundPlayer.playCelebration()
        delay(1100L)
        showResults = true
    }

    // Start/stop ambient music when the switch changes. Volume changes are applied
    // directly by the sliders; restarting playback on every drag tick restarted the
    // fade and rewrote the whole prefs file each time.
    LaunchedEffect(musicEnabled) {
        if (musicEnabled) AmbientMusicPlayer.start(musicVolume)
        else AmbientMusicPlayer.stop()
        saveManager.setMusicEnabled(musicEnabled)
    }
    // Persist the volume once the slider settles.
    LaunchedEffect(musicVolume) { delay(400L); saveManager.setMusicVolume(musicVolume) }
    // Sound-effect and haptic switches apply everywhere and persist across launches.
    LaunchedEffect(soundEnabled)   { SoundPlayer.enabled = soundEnabled;  saveManager.setSoundEnabled(soundEnabled) }
    LaunchedEffect(hapticsEnabled) { Haptics.enabled = hapticsEnabled;    saveManager.setHapticsEnabled(hapticsEnabled) }

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
    // The puzzle clock also pauses while the phone is being handed over.
    val timerActive = timerRunning && appResumed && !showSettings && !showTurnDialog && appMode == AppMode.DASHBOARD
    LaunchedEffect(timerActive) {
        while (timerActive) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    LaunchedEffect(remoteToast) { if (remoteToast != null) { delay(2600L); remoteToast = null } }

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
    // It waits for the opening card: dialogs draw above it.
    LaunchedEffect(appMode, showOpeningCard) {
        if (showOpeningCard) return@LaunchedEffect
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
                    selection          = InputEngine.initialSelection(Board(parsed), emptyMap(), emptySet())
                    teamP1Score = 0; teamP2Score = 0; teamP1Hints = 0; teamP2Hints = 0
                    vindP1Score = 0; vindP2Score = 0; vindAssignedWord = null
                    tauntIndex.intValue = 0
                    lastResult = null
                    elapsedSeconds     = 0L
                    hintsUsedThisPuzzle = 0
                    currentStreak      = 0
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
                val merged   = mergeRemoteInputs(userInputs, rawInputs, solutionOf(placedWords))
                userInputs   = merged
                // A remote letter outside a solved word is the other player's hint: lock
                // and mark it here too, as it is on their phone, so it can't be erased.
                // (Board built from live state: this listener outlives the composition
                // that created it, so the captured `board` may be a previous puzzle's.)
                val solvedNow = Board(placedWords).lockedCells(merged, emptySet())
                val hinted = rawInputs.keys.filter { merged[it] == rawInputs[it] && it !in solvedNow }
                if (hinted.isNotEmpty()) revealedCells = revealedCells + hinted
            }

            // Sync turn and scores (team + vindictive both use hostScore/guestScore).
            // Each device reads only the OTHER side's score — its own side is authoritative locally.
            val remoteTurn       = snap.child("turn").getValue(Long::class.java)?.toInt()
            val remoteHostScore  = snap.child("hostScore").getValue(Long::class.java)?.toInt()
            val remoteGuestScore = snap.child("guestScore").getValue(Long::class.java)?.toInt()
            if (remoteTurn != null) teamCurrentPlayer = remoteTurn
            // Tell this player what the other one just did (their score moved).
            val remoteScore = if (onlineRole == OnlineRole.GUEST) remoteHostScore else remoteGuestScore
            val prevRemote  = when {
                activeGameMode == GameMode.VINDICTIVE -> if (onlineRole == OnlineRole.GUEST) vindP1Score else vindP2Score
                else                                  -> if (onlineRole == OnlineRole.GUEST) teamP1Score else teamP2Score
            }
            if (remoteScore != null && remoteScore != prevRemote && appMode == AppMode.DASHBOARD && placedWords.isNotEmpty()) {
                val d = remoteScore - prevRemote
                val who = player2Name.ifBlank { "Your opponent" }
                remoteToast = if (d > 0) "✅ $who got one!" else "❌ $who lost ${-d} pt${if (d == -1) "" else "s"}"
            }
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
        FirebaseGameManager.onWriteRefused = { msg -> if (isOnlineGame) remoteToast = msg }
        try {
            awaitCancellation()
        } finally {
            FirebaseGameManager.onWriteRefused = null
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
    LaunchedEffect(isGenerating, generationId) {
        if (!isGenerating) return@LaunchedEffect
        // Restored mid-generation: the word list is still loading.
        if (allEntries.isEmpty()) snapshotFlow { startupDone }.first { it }
        val request  = generationId
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
        // A newer request (or leaving the puzzle) supersedes this one. Keying the
        // effect on generationId already cancels it; this guards the window between.
        if (request != generationId || !isGenerating || appMode != AppMode.DASHBOARD) return@LaunchedEffect

        if (generated.isEmpty()) {
            // Never drop the player into an empty, unsolvable grid — and don't leave the
            // previous (solved) board loaded behind the home screen either.
            isGenerating = false
            timerRunning = false
            placedWords = emptyList(); gridCells = emptyList()
            userInputs = emptyMap(); revealedCells = emptySet(); selection = null
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
        selection = InputEngine.initialSelection(Board(generated), emptyMap(), emptySet())
        waveFx = null; shakeFx = null
        elapsedSeconds = 0L
        hintsUsedThisPuzzle = 0
        teamP1Hints = 0
        teamP2Hints = 0
        vindP1Score = 0; vindP2Score = 0
        teamP1Score = 0; teamP2Score = 0
        teamCurrentPlayer = teamFirstPlayer
        vindPhase = VindicativePhase.PICK_OWN
        // vindCurrentPlayer is set by the player setup dialog — don't overwrite it here
        vindAssignedWord = null
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
    // Every button, header and tile draws white text on this, so a very light pick
    // (white, yellow, pastels) is deepened just enough to stay readable.
    val appBtnColor = Color(readableUnderWhiteText(currentBtnColorArgb))
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
                    TitleMark()
                    Spacer(Modifier.height(36.dp))

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
                    Text(
                        BuildInfo.short,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable(onClickLabel = "Show build details") { showBuildInfo = true }
                            .padding(4.dp)
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
                                fun selectProfile() {
                                    playerName = name
                                    saveManager.setLastUser(name)
                                    currentScore = saveManager.getScore(name)
                                    currentCompleted = saveManager.getCompleted(name)
                                    currentCellColorArgb = saveManager.getCellColorArgb(name)
                                    currentBtnColorArgb  = saveManager.getButtonColorArgb(name)
                                    musicVolume = saveManager.getMusicVolume()
                                }
                                Card(
                                    // "Continue as…" continues — straight to the home screen.
                                    onClick = {
                                        vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                                        selectProfile()
                                        homeRefresh++
                                        appMode = AppMode.CATEGORY_SELECT
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
                                        IconButton(onClick = {
                                            selectProfile()
                                            viewingProfile = name
                                            appMode = AppMode.STATS
                                        }) {
                                            Icon(
                                                imageVector   = Icons.Default.BarChart,
                                                contentDescription = "Stats for $name",
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                                            )
                                        }
                                        IconButton(onClick = { confirmDeletePlayer = name }) {
                                            Icon(
                                                imageVector   = Icons.Default.Delete,
                                                contentDescription = "Delete $name",
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
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
                    showWrongFlash = false
                    streakMilestone = null
                    vindPhase = VindicativePhase.PICK_OWN
                    dailyPromptShown = false   // reset so prompt fires again on next login
                    appMode = AppMode.LOGIN
                },
                onQuit                = { (context as? Activity)?.finish() },
                onOpenSettings        = { showSettings = true },
                onRequestPlayer2Setup = { showPlayer2SetupDialog = true },
                musicEnabled          = musicEnabled,
                soundEnabled          = soundEnabled,
                musicVolume           = musicVolume,
                // Play/pause on the home card pauses playback only. Turning Music off
                // (Settings) made the card vanish and the layout jump, with no way back
                // from home.
                onMusicToggle         = {
                    if (AmbientMusicPlayer.isPlaying) AmbientMusicPlayer.stop()
                    else AmbientMusicPlayer.start(musicVolume)
                },
                onVolumeChange        = { v ->
                    musicVolume = v
                    AmbientMusicPlayer.setVolume(v)
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
            // Pop-ups over the puzzle sit just below the status bar and top bar, and are
            // announced to screen readers when they appear.
            val belowTopBar = Modifier.statusBarsPadding().padding(top = 72.dp)
                .semantics { liveRegion = LiveRegionMode.Polite }
            // Pick a top-bar accent that stays readable against the surface
            // even when the user has chosen a near-white or near-black brand
            // color. Used for the back arrow, Puzzles count, Score, and
            // Settings icon.
            // (Measured against the bar's real background with the WCAG ratio; the
            // default purple fails on the dark bar, so dark mode falls back.)
            val barBg       = appBtnColor.copy(alpha = 0.14f).compositeOver(MaterialTheme.colorScheme.surface)
            val safeAccent  = Color(readableTextColor(appBtnColor.toArgb(), barBg.toArgb(),
                MaterialTheme.colorScheme.onSurface.toArgb()))
            // Physical keyboards (Chromebooks, tablets, emulators): letters, Backspace,
            // Tab / Enter for next clue (Shift+Tab for previous), Space to flip direction.
            val keyFocus = remember { FocusRequester() }
            LaunchedEffect(isGenerating) { if (!isGenerating) runCatching { keyFocus.requestFocus() } }
            Box(modifier = Modifier
                .fillMaxSize()
                .focusRequester(keyFocus)
                .focusable()
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown || showSettings || showClueList) return@onPreviewKeyEvent false
                    val ch = e.utf16CodePoint.toChar().uppercaseChar()
                    when {
                        e.key == Key.Backspace || e.key == Key.Delete -> { onBackspace(); true }
                        e.key == Key.Tab || e.key == Key.Enter -> { moveToNextUnsolved(!e.isShiftPressed); true }
                        e.key == Key.Spacebar -> { toggleDirection(); true }
                        ch in 'A'..'Z' -> { onKey(ch); true }
                        else -> false
                    }
                }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = {
                            Column {
                                // Title: category + mode label for team/vindictive
                                val modeLabel = when (activeGameMode) {
                                    GameMode.TEAM       -> " • 🤝 Team"
                                    GameMode.VINDICTIVE -> " • ⚔️ Vindictive"
                                    else                -> ""
                                }
                                val titleText = (if (activeDailyKey != null) "Daily Puzzle" else prettyCategory(activeCategory)) + modeLabel
                                Text(titleText, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                                // Subtitle: names for multiplayer, word count + time for single
                                val mins = elapsedSeconds / 60
                                val secs = elapsedSeconds % 60
                                val timeStr = "%d:%02d".format(mins, secs)
                                // The clock leads, so it's the last thing to be cut off on narrow screens.
                                val subtitle = when {
                                    isGenerating -> "Weaving words…"
                                    activeGameMode == GameMode.TEAM ->
                                        "$timeStr  •  $playerName & $player2Name"
                                    activeGameMode == GameMode.VINDICTIVE ->
                                        "$timeStr  •  $playerName ⚔ $player2Name"
                                    else ->
                                        "$timeStr  •  ${placedWords.size} words  •  ${activeDifficulty.label}"
                                }
                                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        },
                        navigationIcon = {
                            // (The puzzles-completed count lives on home; here it starved the
                            // title and clock of room on narrow phones.)
                            IconButton(onClick = { goHome() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Save and go back", tint = safeAccent)
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
                            Text("$currentScore pts", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = safeAccent,
                                modifier = Modifier.semantics { contentDescription = "Score $currentScore points" })
                            SettingsGearButton(onClick = { showSettings = true })
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            // Stronger 14% wash so the bar is distinguishable
                            // from the page background in dark mode.
                            containerColor = appBtnColor.copy(alpha = 0.14f)
                        )
                    )

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
                            vindPhase == VindicativePhase.PICK_OWN    -> "$vCurrentName — First turn! Pick any clue and answer it."
                            vindPhase == VindicativePhase.ASSIGN_CLUE -> "$vCurrentName — Choose a clue for $vOtherName, then tap Give 🎯"
                            showTurnDialog -> "$vCurrentName — Your clue is waiting… hit I'm Ready when you have the phone!"
                            vindOpponentCountdown > 0 ->
                                "$vCurrentName — answer your clue! ⏱ ${vindOpponentCountdown}s"
                            else -> "$vCurrentName — Time's up! Answer or pass."
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .background(bannerBg)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(vPhaseText, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = bannerFg, textAlign = TextAlign.Center)
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                Text("$onlineP0Name: ${vindP1Score}pts", fontSize = 11.sp, color = bannerFg)
                                Text("$onlineP1Name: ${vindP2Score}pts", fontSize = 11.sp, color = bannerFg)
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

                    // ── Grid ─────────────────────────────────────────────────────
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
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
                                        // The missing dots are drawn transparent, so the centred
                                        // title keeps its width instead of jittering as they cycle.
                                        Text(
                                            buildAnnotatedString {
                                                append("Weaving Words$loadingDots")
                                                withStyle(SpanStyle(color = Color.Transparent)) {
                                                    append(".".repeat(3 - loadingDots.length))
                                                }
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            modifier = Modifier.semantics { contentDescription = "Weaving words. Building your puzzle." }
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
                            // Layer 1: colour fallback, layer 2: background art, layer 3: grid.
                            val bgPainter = rememberBgPainter(currentBgImageName)
                            Box(Modifier.fillMaxSize().background(currentBgColor))
                            if (bgPainter != null) {
                                Image(
                                    painter = bgPainter, contentDescription = null,
                                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                                )
                            }
                            CrosswordGrid(
                                board        = board,
                                cells        = gridCells,
                                inputs       = userInputs,
                                revealed     = revealedCells,
                                locked       = lockedCells,
                                selection    = selection,
                                activeWord   = activeWord,
                                assignedWord = forcedWord(),
                                cellColor    = currentCellColor,
                                accent       = appBtnColor,
                                wave         = waveFx,
                                shake        = shakeFx,
                                onTapCell    = { onCellTap(it) },
                                modifier     = Modifier.fillMaxSize().padding(6.dp)
                            )
                        }
                    }

                    // ── Clue bar, actions, keyboard ──────────────────────────────
                    if (!isGenerating && !puzzleSolved) {
                        ClueBar(
                            word      = activeWord,
                            inputs    = userInputs,
                            accent    = appBtnColor,
                            onPrev    = { moveToNextUnsolved(false) },
                            onNext    = { moveToNextUnsolved(true) },
                            onTapClue = { toggleDirection() },
                            trailing  = {
                                IconButton(onClick = { showClueList = true }) {
                                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "All clues")
                                }
                            }
                        )
                        val blocked   = typingBlockedReason()
                        val assigning = activeGameMode == GameMode.VINDICTIVE &&
                            vindPhase == VindicativePhase.ASSIGN_CLUE &&
                            VindictiveRules.canAct(vindSnapshot(), isOnlineGame, myIndex())
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Hint: reveals the selected square (or the word's first open one).
                            val hintWord    = activeWord?.takeIf { !isWordSolved(it, userInputs) }
                            // Hints are paid by whoever is playing: this device's player online,
                            // otherwise the player whose turn it is (not always Player 1).
                            val hintPayer = when {
                                isOnlineGame -> playerName
                                activeGameMode == GameMode.TEAM       -> nameOf(teamCurrentPlayer)
                                activeGameMode == GameMode.VINDICTIVE -> nameOf(vindCurrentPlayer)
                                else -> playerName
                            }
                            val payerScore  = if (hintPayer == playerName) currentScore else saveManager.getScore(hintPayer)
                            val canAfford   = payerScore >= Economy.HINT_COST
                            // Vindictive is head-to-head: a hint may help, but the last letter
                            // has to be the player's own, or hints would score the word for them.
                            val lastLetterOwn = activeGameMode == GameMode.VINDICTIVE && hintWord != null &&
                                unrevealedCells(hintWord, userInputs).size <= 1
                            val hintEnabled = hintWord != null && canAfford && !lastLetterOwn && blocked == null
                            OutlinedButton(
                                onClick = {
                                    val word = hintWord ?: return@OutlinedButton
                                    val (pos, ch) = pickHintCell(word, userInputs, selection?.cell) ?: return@OutlinedButton
                                    vibrateLight(context)
                                    if (soundEnabled) SoundPlayer.playClick()
                                    val before = userInputs
                                    userInputs    = userInputs + (pos to ch)
                                    revealedCells = revealedCells + pos
                                    if (hintPayer == playerName) currentScore = (currentScore - Economy.HINT_COST).coerceAtLeast(0)
                                    saveManager.addScore(hintPayer, -Economy.HINT_COST)
                                    hintsUsedThisPuzzle++
                                    if (activeGameMode == GameMode.TEAM) {
                                        // Online the hint belongs to this device's player; locally to whoever has the turn.
                                        val t = teamSnapshot()
                                        applyTeam(TeamRules.onHint(if (isOnlineGame) t.copy(turn = myIndex()) else t).copy(turn = t.turn))
                                    }
                                    if (isOnlineGame) FirebaseGameManager.writeInput(onlineCode, pos.first, pos.second, ch)
                                    // A reveal can complete words — judge them like a typed letter.
                                    val filled = board.wordsAt(pos).filter { isWordFilled(it, userInputs) && !(isWordFilled(it, before) && before[pos] == ch) }
                                    judgeFilled(filled.sortedBy { if (it == word) 0 else 1 }, word)
                                },
                                enabled  = hintEnabled,
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                shape    = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    when {
                                        lastLetterOwn -> "💡 Last letter's yours"
                                        !canAfford    -> "💡 Hint (need ${Economy.HINT_COST} pt)"
                                        else          -> "💡 Reveal square  −${Economy.HINT_COST}"
                                    },
                                    style = MaterialTheme.typography.labelLarge, maxLines = 1
                                )
                            }
                            if (assigning) {
                                val target = activeWord?.takeIf { !isWordSolved(it, userInputs) }
                                Button(
                                    onClick = {
                                        target ?: return@Button
                                        vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                                        assignClue(target)
                                    },
                                    enabled  = target != null,
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                    shape    = RoundedCornerShape(12.dp),
                                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C), contentColor = Color.White)
                                ) {
                                    Text("🎯 Give to ${nameOf(1 - vindCurrentPlayer)}", style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            // After "Answer It" on an expired clock the pass stays available.
                            val passOpen = activeGameMode == GameMode.VINDICTIVE &&
                                vindPhase == VindicativePhase.OPPONENT_WAIT && vindAssignedWord != null &&
                                vindOpponentCountdown == 0 && !showTurnDialog && !vindPassDialogVisible &&
                                VindictiveRules.canAct(vindSnapshot(), isOnlineGame, myIndex())
                            if (passOpen) {
                                Button(
                                    onClick  = { vibrateLight(context); passAssigned() },
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                    shape    = RoundedCornerShape(12.dp),
                                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C), contentColor = Color.White)
                                ) {
                                    Text("Pass  (${Economy.VIND_PASS} pt)", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                                }
                            }
                        }
                        CrosswordKeyboard(
                            enabled        = blocked == null,
                            onKey          = { onKey(it) },
                            onBackspace    = { onBackspace() },
                            disabledReason = blocked?.takeIf { it.isNotEmpty() },
                            modifier       = Modifier.navigationBarsPadding()
                        )
                    } else {
                        Spacer(Modifier.navigationBarsPadding())
                    }
                }   // end main Column

                // ── Full-screen overlays — inside the Dashboard Box ──────────────
                // Wrong answer banner — red border-glow around content with a centered card.
                // Less jarring than a full red wash; plays nicely with the rest of the UI.
                // Vindictive: the full "WRONG!" card with a taunt is part of the mode's
                // personality. Solo / Team: a quick, non-blocking toast is enough.
                AnimatedVisibility(
                    visible = showWrongFlash && activeGameMode == GameMode.VINDICTIVE,
                    enter = fadeIn() + scaleIn(initialScale = 0.85f), exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFD32F2F).copy(alpha = 0.28f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                            modifier = Modifier.padding(horizontal = 36.dp).semantics(mergeDescendants = true) {}
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 28.dp, vertical = 22.dp)
                            ) {
                                Text(
                                    "WRONG!",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = Color.White,
                                    letterSpacing = 2.sp
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    vindictiveTaunt(tauntIndex.intValue, nameOf(wrongAnswererIndex), nameOf(1 - wrongAnswererIndex)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.95f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                AnimatedVisibility(
                    visible = showWrongFlash && activeGameMode != GameMode.VINDICTIVE,
                    enter = fadeIn() + slideInVertically { -it }, exit = fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = RoundedCornerShape(20.dp),
                        shadowElevation = 6.dp,
                        modifier = belowTopBar
                    ) {
                        // Team: a wrong word ends the turn, and its letters are cleared.
                        Text(if (activeGameMode == GameMode.TEAM) "✗  Not quite — next player's turn" else "✗  Not quite — try again",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
                    }
                }

                // What the online opponent just did.
                AnimatedVisibility(
                    visible = remoteToast != null && isOnlineGame,
                    enter = fadeIn() + slideInVertically { -it }, exit = fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter).then(belowTopBar)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        shape = RoundedCornerShape(20.dp), shadowElevation = 6.dp
                    ) {
                        Text(remoteToast ?: "", style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
                    }
                }

                // "X is answering…" status banner (online only)
                if (isOnlineGame && remoteIsAnswering && !puzzleSolved) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .then(belowTopBar)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
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

    // ── ONLINE JOIN DIALOG ────────────────────────────────────────────────────────
    if (showOnlineJoin) {
        AlertDialog(
            onDismissRequest = { showOnlineJoin = false; onlineJoinError = ""; onlineRequest++; onlinePending = false },
            title = { Text("🌐 Join Online Game", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter the 6-letter code from the host:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = onlineJoinInput,
                        onValueChange = { onlineJoinInput = FirebaseGameManager.sanitizeCode(it); onlineJoinError = "" },
                        label = { Text("Game Code") },
                        singleLine = true,
                        enabled = !onlinePending,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters,
                            autoCorrectEnabled = false, keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (onlineJoinError.isNotEmpty()) {
                        Text(onlineJoinError, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                GradientBtn(if (onlinePending) "Joining…" else "Join", appBtnGradient,
                    enabled = FirebaseGameManager.isValidCode(onlineJoinInput) && !onlinePending,
                    onClick = {
                        vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                        val code = onlineJoinInput
                        val request = ++onlineRequest
                        onlinePending = true
                        // A cancelled dialog's late answer must not pull the player into a game.
                        fun stale() = request != onlineRequest || !showOnlineJoin
                        FirebaseGameManager.signInAnonymously { ok, err ->
                            if (stale()) return@signInAnonymously
                            if (!ok) { onlinePending = false; onlineJoinError = err ?: "Couldn't connect."; return@signInAnonymously }
                            FirebaseGameManager.joinGame(
                                code, playerName,
                                onSuccess = { mode, category, difficulty, hostName ->
                                    if (stale()) { FirebaseGameManager.abandonGame(code); return@joinGame }
                                    onlinePending     = false
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
                                onError = { msg -> if (!stale()) { onlinePending = false; onlineJoinError = msg } }
                            )
                        }
                    }, modifier = Modifier.fillMaxWidth())
            },
            dismissButton = {
                GradientBtn("Cancel", appBtnGradient, onClick = {
                    vibrateLight(context); showOnlineJoin = false; onlineJoinError = ""; onlineJoinInput = ""
                    onlineRequest++; onlinePending = false
                }, modifier = Modifier.fillMaxWidth())
            }
        )
    }

    // ── CLUE LIST SHEET ────────────────────────────────────────────────────────
    if (showClueList && appMode == AppMode.DASHBOARD && !isGenerating) {
        val clueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(onDismissRequest = { showClueList = false }, sheetState = clueSheetState) {
            val forced = forcedWord()
            FullHintsList(
                words        = if (forced != null) listOf(forced) else placedWords,
                userInputs   = userInputs,
                selectedWord = activeWord,
                onSelect     = { w -> selectClue(w); showClueList = false }
            )
        }
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
                    // weight(1f): without it the long subtitle squeezed the Switch to
                    // nothing on narrow screens or with large text.
                    Column(Modifier.weight(1f)) {
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
                            SoundPlayer.enabled = it
                            vibrateLight(context)
                            if (it) SoundPlayer.playClick()
                        }
                    )
                }

                // Haptics toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Vibration", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text(if (hapticsEnabled) "Taps, correct and wrong answers" else "Off",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = hapticsEnabled,
                        onCheckedChange = {
                            hapticsEnabled = it
                            Haptics.enabled = it
                            if (it) vibrateLight(context)
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
                            "♪  ${AmbientMusicPlayer.currentTrackName.ifEmpty { "No track" }}",
                            fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            color = settingsTrackColor, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(onClick = { AmbientMusicPlayer.previous() }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = settingsTrackColor)
                            }
                            IconButton(onClick = { musicEnabled = !musicEnabled }, modifier = Modifier.size(48.dp)) {
                                Icon(
                                    imageVector = if (AmbientMusicPlayer.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (AmbientMusicPlayer.isPlaying) "Pause music" else "Play music",
                                    tint = settingsTrackColor
                                )
                            }
                            IconButton(onClick = { AmbientMusicPlayer.next() }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = settingsTrackColor)
                            }
                            Icon(Icons.AutoMirrored.Filled.VolumeDown, contentDescription = null, tint = settingsTrackColor.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                            Slider(
                                value = musicVolume,
                                onValueChange = { v ->
                                    musicVolume = v
                                    AmbientMusicPlayer.setVolume(v)
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
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = settingsTrackColor.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
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
                                    Text("Buttons, header and highlights",
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

                if (appMode == AppMode.DASHBOARD) {
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
                } else {
                    // Opened from home: nothing to save or resume.
                    OutlinedButton(
                        onClick = { showSettings = false },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Close") }
                }

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
                                BuildInfo.short,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            TextButton(
                                onClick = { showSettings = false; showBuildInfo = true },
                                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                            ) { Text("Build details") }
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
                    vindPassDialogVisible = false      // keyboard re-enables; typed letters are kept
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
            title = { Text("Welcome to My Personal Crossword! 🧩", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        "👆 Tap a square" to "Select it and type. Tap it again to switch Across / Down.",
                        "⌨️ Type" to "Letters fill in and skip ahead. A finished word is checked instantly.",
                        "◀ ▶ Clues" to "Arrows jump between unsolved clues; ☰ shows the full list.",
                        "💡 Hint" to "Reveals the selected square. Costs 1 point.",
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
            title = { Text("⚔️ How Vindictive Mode Works", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        "1️⃣ First turn" to "The starting player picks any clue and types an answer. +1 if correct, -1 if wrong.",
                        "2️⃣ Assign" to "That same player selects a clue for the other player, taps 🎯 Give, and passes the phone.",
                        "3️⃣ Opponent" to "The next player types the answer before the timer (15s, 30s or 60s) runs out.",
                        "✅ Correct" to "+1 point. Now pick a clue for the other player.",
                        "❌ Wrong" to "-2 points. Still pick a clue for the other player.",
                        "⏭ Pass" to "When time runs out: answer anyway, or pass for -1 point.",
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
            title = { Text("👤 Single Player Mode", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        "👆 Tap a square" to "Selects it — tap again to switch Across / Down.",
                        "⌨️ Type"        to "Letters fill in and jump to the next gap. Backspace erases.",
                        "✅ Check"       to "A word is checked as soon as it's full. Fix wrong letters and try again.",
                        "💡 Hint"        to "Reveals the selected square. Costs 1 point.",
                        "🔒 Locked"      to "Solved words and revealed squares can't be erased.",
                        "🏆 Scoring"     to "Points depend on difficulty. Hints reduce your score.",
                        "💾 Auto-save"   to "Progress saves automatically, even if you leave the app."
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
            title = { Text("🤝 How Team Mode Works", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        "🤝 Goal"        to "Both players work together to solve the same puzzle.",
                        "🔄 Take turns"  to "Fill in one word per turn — right or wrong, then pass the phone.",
                        "✅ Correct"     to "+1 answer credit for that player. Both earn full points at puzzle end.",
                        "💡 Hints"       to "Either player can use hints. Each costs the player using it 1 point, straight away.",
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
            title = { Text("📅 Daily Puzzle Available!", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("A new crossword is generated every day using all categories — 20 words, 20 points!")
                    Spacer(Modifier.height(2.dp))
                    Text("Want to jump straight into today's puzzle?", fontWeight = FontWeight.Medium)
                    Text("(It's also the Daily card at the top of the home screen.)",
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
            title = { Text("📅 Daily Puzzle", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        "📅 Fresh daily"   to "A brand-new puzzle every day — everyone gets the same one.",
                        "🗂 All categories" to "Words drawn from every category in your list.",
                        "⭐ 20 words"       to "A big 20-word grid worth 20 points.",
                        "🏆 Score"          to "Each hint costs 1 point when you use it — try to go hint-free!"
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
            // Hand-offs need an explicit "I'm Ready": a stray tap outside (or Back)
            // would otherwise start the next player's clock before they have the phone.
            onDismissRequest = { },
            title = { Text(turnDialogTitle, style = MaterialTheme.typography.headlineSmall,
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
            title = { Text(modeLabel, style = MaterialTheme.typography.headlineSmall) },
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
                                val request  = ++onlineRequest
                                // Ignore the answer if the player has since started something else.
                                fun stale() = request != onlineRequest || appMode != AppMode.CATEGORY_SELECT || isOnlineGame
                                FirebaseGameManager.signInAnonymously { ok, err ->
                                    if (stale()) return@signInAnonymously
                                    if (!ok) { onlineNotice = err; return@signInAnonymously }
                                    FirebaseGameManager.createGame(playerName, hostMode, hostDiff) { code, error ->
                                        if (code != null && stale()) { FirebaseGameManager.abandonGame(code); return@createGame }
                                        if (stale()) return@createGame
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
                                // One focus stop per option for screen readers (the row
                                // and its RadioButton used to be two).
                                .selectable(selected = whoIsP1 == idx, role = Role.RadioButton) { whoIsP1 = idx }
                                .background(if (whoIsP1 == idx) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(selected = whoIsP1 == idx, onClick = null)
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
            title = { Text("⏱ Answer Time Limit", style = MaterialTheme.typography.headlineSmall) },
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
            title = { Text("Choose Category", style = MaterialTheme.typography.headlineSmall) },
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
                            GradientBtn("${categoryIcons[cat] ?: "📝"}  ${prettyCategory(cat)}", appBtnGradient, onClick = {
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
            title = { Text("🔀 Combine Categories", style = MaterialTheme.typography.headlineSmall) },
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
                                Text(prettyCategory(cat), fontSize = 15.sp,
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
            title = { Text("Choose Difficulty", style = MaterialTheme.typography.headlineSmall) },
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
                                    val label = combinedLabel(cats)
                                    if (isOnlineGame && onlineRole == OnlineRole.HOST) {
                                        activeCategory     = label
                                        activeDifficulty   = diff
                                        combinedCategories = cats
                                        FirebaseGameManager.writeState(onlineCode, mapOf(
                                            "category" to label, "difficulty" to diff.name
                                        ))
                                        appMode = AppMode.ONLINE_LOBBY
                                    } else {
                                        // Same resume check as a single category: starting a new
                                        // combined puzzle used to overwrite an unfinished one.
                                        val slot = SaveSlot.forPuzzle(activeGameMode, label, diff, cats)
                                        if (saveManager.hasSave(playerName, slot)) resumePrompt = slot
                                        else launchPuzzle(label, diff, combined = cats)
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
            title = { Text("Player 2 Name Required", style = MaterialTheme.typography.headlineSmall) },
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
            title = { Text("Delete Profile?", style = MaterialTheme.typography.headlineSmall) },
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
            title = { Text("Choose Difficulty", style = MaterialTheme.typography.headlineSmall) },
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
            title = { Text("Resume Puzzle?", style = MaterialTheme.typography.headlineSmall) },
            text  = { Text("You have an unfinished ${slotTitle(slot)} puzzle. Resume where you left off, or start a new one?") },
            confirmButton = {
                GradientBtn("▶ Resume", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                    resumePrompt = null
                    if (!resumeSlot(slot)) launchFromSlot(slot)
                }, modifier = Modifier.fillMaxWidth())
            },
            dismissButton = {
                GradientBtn("New Puzzle", appBtnGradient, onClick = {
                    vibrateLight(context); if (soundEnabled) SoundPlayer.playClick()
                    resumePrompt = null
                    saveManager.clearPuzzle(playerName, slot)
                    homeRefresh++
                    launchFromSlot(slot)
                }, modifier = Modifier.fillMaxWidth())
            }
        )
    }

    if (showBgPicker) {
        // A full-screen overlay, not a dialog: Back must close it, not leave the
        // puzzle underneath (or close the app from home).
        BackHandler { showBgPicker = false }
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

    // Tell the other device when this player is the one typing an answer.
    val iAmAnswering = isOnlineGame && appMode == AppMode.DASHBOARD && typingBlockedReason() == null &&
        activeGameMode != GameMode.SINGLE && !(activeGameMode == GameMode.VINDICTIVE && vindPhase == VindicativePhase.ASSIGN_CLUE)
    LaunchedEffect(iAmAnswering, isOnlineGame) {
        if (isOnlineGame && onlineCode.isNotEmpty()) {
            val field = if (onlineRole == OnlineRole.HOST) "p0answering" else "p1answering"
            FirebaseGameManager.writeState(onlineCode, mapOf(field to iAmAnswering))
        }
    }

    // ── VINDICTIVE ANSWER COUNTDOWN ──────────────────────────────────────────
    // Starts once the answerer has the phone ("I'm Ready" dismissed). Runs only on
    // the device that owns the timer (VindictiveRules.ownsTimer): offline the one
    // phone, online the answerer's phone — which mirrors it to the other device.
    // Typing keeps the clock running; when it hits zero the documented
    // Answer It / Pass choice appears.
    LaunchedEffect(vindPhase, showTurnDialog, vindAssignedWord, appMode) {
        if (activeGameMode != GameMode.VINDICTIVE || appMode != AppMode.DASHBOARD || puzzleSolved) return@LaunchedEffect
        if (showTurnDialog || vindAssignedWord == null) return@LaunchedEffect
        if (!VindictiveRules.ownsTimer(vindSnapshot(), isOnlineGame, myIndex())) return@LaunchedEffect
        vindPassDialogVisible = false
        for (i in vindTimerSeconds downTo 1) {
            vindOpponentCountdown = i
            if (isOnlineGame) FirebaseGameManager.writeState(onlineCode, mapOf("vindCountdown" to i))
            if (i <= 5 && soundEnabled) SoundPlayer.playTick()
            delay(1000L)
            // The answer clock pauses with the app (backgrounded, Settings open).
            while (!appResumed || showSettings) delay(200L)
            if (vindPhase != VindicativePhase.OPPONENT_WAIT || puzzleSolved) return@LaunchedEffect
        }
        onVindTimeout()
    }

    // Vindictive: the answerer's cursor lives on the assigned clue.
    LaunchedEffect(vindPhase, vindAssignedWord) {
        forcedWord()?.let { w ->
            if (board.activeWord(selection) != w) selection = InputEngine.selectWord(w, userInputs, lockedCells)
        }
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
            title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
            text  = {
                // Scrolls on short screens / large text instead of silently cutting lines.
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                            if (result.hints > 0) Line("Hints used", "−${result.hints}")
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
                        // Back in Team/Vindictive, home's START runs the player setup first.
                        result.isDaily -> { goHome(); if (activeGameMode == GameMode.SINGLE) showVindCategoryDialog = true }
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

    if (showBuildInfo) BuildInfoDialog(onDismiss = { showBuildInfo = false })

    // ── BLOCKING NOTICES ─────────────────────────────────────────────────────
    onlineNotice?.let { msg ->
        AlertDialog(
            onDismissRequest = { },
            title = { Text("🌐 Online play", style = MaterialTheme.typography.headlineSmall) },
            text  = { Text(msg) },
            confirmButton = {
                GradientBtn("OK", appBtnGradient, onClick = {
                    onlineNotice = null
                    goHome()   // ends the session and unloads the grid (online games aren't saved)
                }, modifier = Modifier.fillMaxWidth())
            }
        )
    }
    dailyInfoMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { dailyInfoMessage = null },
            title = { Text("📅 Daily Puzzle", style = MaterialTheme.typography.headlineSmall) },
            text  = { Text(msg) },
            confirmButton = {
                GradientBtn("OK", appBtnGradient, onClick = { dailyInfoMessage = null }, modifier = Modifier.fillMaxWidth())
            }
        )
    }

    // Drawn last so it covers every screen; fades into home when it's done.
    AnimatedVisibility(visible = showOpeningCard, enter = fadeIn(), exit = fadeOut(tween(400))) {
        OpeningCard()
    }
}

// ============================================================
// UI COMPOSABLES
// ============================================================

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
    onOpenSettings:         () -> Unit = {},
    onRequestPlayer2Setup:  () -> Unit = {},
    musicEnabled:           Boolean = true,
    soundEnabled:           Boolean = true,
    musicVolume:            Float   = 0.7f,
    onMusicToggle:          () -> Unit = {},
    onVolumeChange:         (Float) -> Unit = {},
    btnColorArgb:           Int = 0xFF6650A4.toInt()
) {
    val context  = LocalContext.current
    val btnColor    = Color(readableUnderWhiteText(btnColorArgb))   // white text on it stays readable
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

        // Scrolls when the content is taller than the screen (small phones, large
        // text, a Continue card) instead of squeezing the carousel off the bottom.
        // heightIn(min = viewport) keeps the weighted spacers centring it otherwise.
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = bottomBarHeight)
        ) {
        val viewportHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewportHeight)
                .padding(
                    start = 16.dp, end = 16.dp,
                    top = 8.dp,
                    bottom = 16.dp
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
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
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
                            Text("Log out", fontSize = 14.sp, color = Color.White)
                        }
                        SettingsGearButton(onClick = onOpenSettings)
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
                                "Continue ${slotTitle(lastInProgress)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
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
                        .heightIn(min = 68.dp)
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
                                .selectable(selected = activeGameMode == mode, role = Role.RadioButton) { onGameModeChange(mode) }
                                .heightIn(min = 48.dp)
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = activeGameMode == mode,
                                onClick  = null,
                                modifier = Modifier.padding(horizontal = 12.dp),
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
                                        prettyCategory(category),
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
        }   // end BoxWithConstraints

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
                            "♪  ${AmbientMusicPlayer.currentTrackName.ifEmpty { "No track" }}",
                            fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            color = trackColor, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(onClick = { AmbientMusicPlayer.previous() }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = trackColor)
                            }
                            IconButton(
                                onClick = { onMusicToggle() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (AmbientMusicPlayer.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (AmbientMusicPlayer.isPlaying) "Pause music" else "Play music",
                                    tint = trackColor
                                )
                            }
                            IconButton(onClick = { AmbientMusicPlayer.next() }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = trackColor)
                            }
                            Icon(Icons.AutoMirrored.Filled.VolumeDown, contentDescription = null, tint = trackColor.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
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
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = trackColor.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FullHintsList(
    words:        List<PlacedWord>,
    userInputs:   Map<Pair<Int, Int>, Char>,
    selectedWord: PlacedWord?,
    onSelect:     (PlacedWord) -> Unit
) {
    val across        = words.filter { it.isHorizontal }.sortedBy { it.number }
    val down          = words.filter { !it.isHorizontal }.sortedBy { it.number }
    val acrossUnsolved = across.filter { !isWordSolved(it, userInputs) }
    val downUnsolved   = down.filter   { !isWordSolved(it, userInputs) }
    val solved         = (across + down).filter { isWordSolved(it, userInputs) }
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
            Text(label, style = MaterialTheme.typography.labelLarge, color = Color.White, letterSpacing = 1.5.sp)
        }
    }

    LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        if (totalCount > 0) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Progress", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${solved.size} / $totalCount", style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                    LinearProgressIndicator(
                        progress = { solved.size.toFloat() / totalCount },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }
        if (acrossUnsolved.isNotEmpty()) {
            item(key = "h-across") { SectionHeader("ACROSS", Color(0xFF5C6BC0)) }
            items(acrossUnsolved, key = { "a${it.number}" }) { ClueItem(it, userInputs, it == selectedWord, onSelect) }
        }
        if (downUnsolved.isNotEmpty()) {
            item(key = "h-down") { SectionHeader("DOWN", Color(0xFF00838F)) }
            items(downUnsolved, key = { "d${it.number}" }) { ClueItem(it, userInputs, it == selectedWord, onSelect) }
        }
        if (solved.isNotEmpty()) {
            item(key = "h-solved") {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)
                    .clip(RoundedCornerShape(6.dp)).background(Color(0xFF2E7D32))
                    .padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("✓ SOLVED (${solved.size})", style = MaterialTheme.typography.labelLarge,
                        color = Color.White, letterSpacing = 1.5.sp)
                }
            }
            items(solved, key = { "s${if (it.isHorizontal) "a" else "d"}${it.number}" }) {
                ClueItem(it, userInputs, it == selectedWord, onSelect)
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
fun ClueItem(
    word:        PlacedWord,
    userInputs:  Map<Pair<Int, Int>, Char>,
    isSelected:  Boolean,
    onSelect:    (PlacedWord) -> Unit
) {
    val solved      = isWordSolved(word, userInputs)
    val solvedGreen = Color(0xFF2E7D32)
    val dir         = if (word.isHorizontal) "Across" else "Down"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else Color.Transparent)
            .clickable(onClickLabel = "Select ${word.number} $dir") { onSelect(word) }
            .semantics(mergeDescendants = true) {}
            .heightIn(min = 48.dp)
            .padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
                Text(
                    text = "${word.number}.",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (solved) solvedGreen else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(30.dp)
                )
                Text(
                    text = word.clue,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (solved) solvedGreen else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (solved) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.weight(1f)
                )
            }
            if (solved) {
                Surface(color = solvedGreen.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp)) {
                    Text(
                        text = "✓ ${word.word}",
                        style = MaterialTheme.typography.labelMedium,
                        color = solvedGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        if (!solved) {
            val filled = word.cells().count { userInputs[it] != null }
            Text(text = "${word.word.length} letters" + (if (filled > 0) " · $filled filled" else ""),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 30.dp))
        }
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
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

// Decoded backgrounds are cached (a few screens' worth) so re-entering a puzzle or
// re-opening the picker doesn't re-decode multi-megabyte WebPs.
private object BitmapCache {
    private val cache = object : android.util.LruCache<String, ImageBitmap>(48 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }
    fun get(key: String): ImageBitmap? = cache.get(key)
    fun put(key: String, bmp: ImageBitmap) { cache.put(key, bmp) }
}

/**
 * Decodes an asset scaled down to roughly [targetW]×[targetH] (never upscaled).
 * Uses bounds + inSampleSize so large source images never allocate full-size
 * bitmaps; if a decoder returns null for the sampled path (seen with some WebP
 * encoders) it falls back to a full decode followed by a scale.
 */
private fun decodeAssetScaled(context: Context, path: String, targetW: Int, targetH: Int): ImageBitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= targetW && bounds.outHeight / (sample * 2) >= targetH) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val sampled = context.assets.open(path).use { BitmapFactory.decodeStream(it, null, opts) }
        if (sampled != null) return sampled.asImageBitmap()
        val full = context.assets.open(path).use { BitmapFactory.decodeStream(it) } ?: return null
        val ratio = minOf(1f, maxOf(targetW.toFloat() / full.width, targetH.toFloat() / full.height))
        if (ratio >= 1f) return full.asImageBitmap()
        val scaled = android.graphics.Bitmap.createScaledBitmap(
            full, (full.width * ratio).toInt().coerceAtLeast(1), (full.height * ratio).toInt().coerceAtLeast(1), true)
        if (scaled !== full) full.recycle()
        scaled.asImageBitmap()
    } catch (_: Exception) { null }
}

// Loads a puzzle background sized for this screen (the art ships in portrait
// drawable-xxhdpi / drawable-xxxhdpi folders only).
fun loadBgBitmap(context: Context, filename: String): ImageBitmap? {
    if (filename == BG_IMAGE_NONE) return null
    val dm = context.resources.displayMetrics
    val key = "bg:$filename:${dm.widthPixels}x${dm.heightPixels}"
    BitmapCache.get(key)?.let { return it }
    val folders = if (dm.density >= 4.0f) listOf("drawable-xxxhdpi", "drawable-xxhdpi")
                  else listOf("drawable-xxhdpi", "drawable-xxxhdpi")
    for (folder in folders) {
        val bmp = decodeAssetScaled(context, "images/$folder/$filename", dm.widthPixels, dm.heightPixels)
        if (bmp != null) { BitmapCache.put(key, bmp); return bmp }
    }
    return null
}

/** ~220 px-wide thumbnail for the picker, decoded from the smaller asset. */
fun loadBgThumbnail(context: Context, filename: String): ImageBitmap? {
    val key = "thumb:$filename"
    BitmapCache.get(key)?.let { return it }
    for (folder in listOf("drawable-xxhdpi", "drawable-xxxhdpi")) {
        val bmp = decodeAssetScaled(context, "images/$folder/$filename", 220, 390)
        if (bmp != null) { BitmapCache.put(key, bmp); return bmp }
    }
    return null
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
        val bmp = withContext(Dispatchers.IO) { loadBgBitmap(context, filename) }
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
    val thumbnails   = remember { mutableStateMapOf<String, ImageBitmap?>() }
    val displayNames = remember(bgImagePool) { backgroundDisplayNames(bgImagePool) }
    LaunchedEffect(bgImagePool) {
        // Drop thumbnails for images no longer in the pool, then load the rest
        // one by one (each ~220 px wide, cached app-wide).
        val poolSet = bgImagePool.toSet()
        thumbnails.keys.toList().forEach { key -> if (!poolSet.contains(key)) thumbnails.remove(key) }
        bgImagePool.forEach { name ->
            if (!thumbnails.containsKey(name)) {
                thumbnails[name] = withContext(Dispatchers.IO) { loadBgThumbnail(context, name) }
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
                        Text("Your puzzles use a plain colour background.",
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
                                .clickable(onClickLabel = "Use this background") { onSelect(name) }
                        ) {
                            if (thumb != null) {
                                Image(
                                    bitmap         = thumb,
                                    contentDescription = "Background: ${displayNames[name] ?: ""}",
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
                                    displayNames[name] ?: "",
                                    color    = Color.White,
                                    style    = MaterialTheme.typography.labelMedium,
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
    val stats       = remember(playerName) { saveManager.getAllStats(playerName) }
    val inProgress  = remember(playerName) { saveManager.getInProgressPuzzles(playerName) }
    val score       = remember(playerName) { saveManager.getScore(playerName) }
    val done        = remember(playerName) { saveManager.getCompleted(playerName) }
    val dailyStreak = remember(playerName) {
        DailyPuzzle.streak(saveManager.getDailyCompletedKeys(playerName), DailyPuzzle.dateKey(System.currentTimeMillis()))
    }
    val hintFree    = stats.count { it.hintsUsed == 0 && it.gameMode != GameMode.VINDICTIVE.name }
    val sections = listOf(
        "📅 Daily"       to stats.filter { it.gameMode == GameMode.DAILY.name || it.diffName == "DAILY" },
        "👤 Solo"        to stats.filter { it.gameMode == GameMode.SINGLE.name && it.diffName != "DAILY" },
        "🤝 Team"        to stats.filter { it.gameMode == GameMode.TEAM.name },
        "⚔️ Vindictive"  to stats.filter { it.gameMode == GameMode.VINDICTIVE.name }
    ).filter { it.second.isNotEmpty() }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(playerName, style = MaterialTheme.typography.titleLarge) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        )

        // Summary tiles
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            @Composable
            fun Tile(value: String, label: String, tint: Color, modifier: Modifier) {
                Column(
                    modifier = modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(tint.copy(alpha = 0.12f))
                        .padding(vertical = 12.dp)
                        .semantics(mergeDescendants = true) {},
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(value, style = MaterialTheme.typography.headlineMedium, color = tint)
                    Text(label, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Tile("$score", "Score", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            Tile("$done", "Puzzles", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
            Tile(if (dailyStreak > 0) "🔥$dailyStreak" else "0", "Daily streak", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
        }
        if (hintFree > 0) {
            Text("✨ $hintFree personal best${if (hintFree == 1) "" else "s"} solved without hints",
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 6.dp))
        }

        HorizontalDivider()

        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {

            // ── IN-PROGRESS PUZZLES ───────────────────────────────────────────
            if (inProgress.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    Text("▶  In Progress", style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                }
                items(inProgress, key = { it.id }) { slot ->
                    Card(
                        modifier  = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors    = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        elevation = CardDefaults.cardElevation(2.dp),
                        onClick   = { onResume(slot) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(slotTitle(slot), style = MaterialTheme.typography.titleSmall)
                                Text("Tap to resume", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("▶", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            }

            // ── PERSONAL BESTS ────────────────────────────────────────────────
            if (stats.isEmpty() && inProgress.isEmpty()) {
                item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text("🧩", fontSize = 56.sp)
                            Text("No records yet", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Solve a puzzle and your best times and scores will appear here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            sections.forEach { (header, records) ->
                item(key = "hdr:$header") {
                    Text(header, style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
                }
                items(records, key = { "${it.gameMode}:${it.category}:${it.diffName}" }) { stat ->
                    StatCard(stat, playerName, allCategoryNames)
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }

        // ── Action button — anchored above Android nav bar ──────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick  = onPlay,
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Play", style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@Composable
private fun StatCard(stat: StatRecord, playerName: String, allCategoryNames: Set<String>) {
    // A solo record for a category that's no longer in the word list.
    val isLegacy = stat.gameMode == GameMode.SINGLE.name && stat.diffName != "DAILY" &&
        !allCategoryNames.contains(stat.category)
    val time = "%d:%02d".format(stat.timeSeconds / 60, stat.timeSeconds % 60)
    val title = when {
        stat.diffName == "DAILY" -> "Daily Puzzle"
        else -> "${prettyCategory(stat.category)} · ${stat.diffName.lowercase().replaceFirstChar { it.uppercase() }}"
    }
    Card(
        modifier  = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (isLegacy) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                             else MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(12.dp).semantics(mergeDescendants = true) {}) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (stat.gameMode == GameMode.VINDICTIVE.name) {
                Text(
                    if (stat.won) "$playerName CONQUERED ${stat.partner}" else "$playerName got DEMOLISHED by ${stat.partner}",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (stat.won) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 2.dp)
                )
            } else if (stat.gameMode == GameMode.TEAM.name && stat.partner.isNotBlank()) {
                Text("with ${stat.partner}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                @Composable
                fun Chip(text: String) {
                    Text(text, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 8.dp, vertical = 3.dp))
                }
                Chip("⏱ $time")
                if (stat.gameMode == GameMode.VINDICTIVE.name) {
                    Chip("${stat.score}–${stat.partnerScore}")
                } else {
                    Chip(if (stat.hintsUsed == 0) "✨ no hints" else "💡 ${stat.hintsUsed}")
                    Chip("★ ${stat.score} pts")
                }
            }
            if (isLegacy) {
                Text("★ Legacy record — category no longer available",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontStyle = FontStyle.Italic, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

// ── CONFETTI ──────────────────────────────────────────────────────────────────
// Each particle's position is a pure function of elapsed time (no state mutated
// during draw), so it runs at the same speed on 60 Hz and 120 Hz screens. Sizes
// are in dp, the burst launches from the top, and everything fades out.
private enum class ConfettiShape { RECT, CIRCLE, STREAMER }

private class ConfettiParticle(
    val x0: Float, val delay: Float,          // normalised start x, launch delay (s)
    val vx: Float, val vy: Float,              // normalised units per second
    val spin: Float, val rot0: Float,          // degrees per second, start angle
    val color: Color, val shape: ConfettiShape, val scale: Float
)

private const val CONFETTI_SECONDS = 3.6f

@Composable
fun ConfettiOverlay() {
    val particles = remember {
        val colors = listOf(
            Color(0xFFFF5252), Color(0xFFFFC107), Color(0xFF40C4FF),
            Color(0xFF69F0AE), Color(0xFFE040FB), Color(0xFF00E5FF),
            Color(0xFFFFAB40), Color(0xFFFFFFFF)
        )
        List(120) {
            ConfettiParticle(
                x0    = Random.nextFloat(),
                delay = Random.nextFloat() * 0.6f,
                vx    = (Random.nextFloat() - 0.5f) * 0.35f,
                vy    = 0.15f + Random.nextFloat() * 0.25f,
                spin  = (Random.nextFloat() - 0.5f) * 540f,
                rot0  = Random.nextFloat() * 360f,
                color = colors.random(),
                shape = when (Random.nextInt(10)) { in 0..4 -> ConfettiShape.RECT; in 5..7 -> ConfettiShape.CIRCLE; else -> ConfettiShape.STREAMER },
                scale = 0.7f + Random.nextFloat() * 0.8f
            )
        }
    }
    val elapsed by produceState(0f) {
        val start = withFrameNanos { it }
        while (value < CONFETTI_SECONDS) {
            withFrameNanos { now -> value = (now - start) / 1_000_000_000f }
        }
    }
    val unit = LocalDensity.current.density   // px per dp
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gravity = 0.55f                    // normalised units / s²
        val fade    = ((CONFETTI_SECONDS - elapsed) / 0.8f).coerceIn(0f, 1f)
        particles.forEach { p ->
            val t = elapsed - p.delay
            if (t < 0f) return@forEach
            val px = (p.x0 + p.vx * t) * size.width
            val py = (-0.05f + p.vy * t + 0.5f * gravity * t * t) * size.height
            if (py > size.height + 40f) return@forEach
            canvasRotate(p.rot0 + p.spin * t, Offset(px, py)) {
                val c = p.color.copy(alpha = fade)
                when (p.shape) {
                    ConfettiShape.RECT -> {
                        val w = 7f * unit * p.scale; val h = 4.5f * unit * p.scale
                        drawRect(c, Offset(px - w / 2f, py - h / 2f), GeoSize(w, h))
                    }
                    ConfettiShape.CIRCLE -> drawCircle(c, radius = 3f * unit * p.scale, center = Offset(px, py))
                    ConfettiShape.STREAMER -> {
                        val w = 12f * unit * p.scale; val h = 2f * unit * p.scale
                        drawRect(c, Offset(px - w / 2f, py - h / 2f), GeoSize(w, h))
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