package com.hag.mypersonalcrossword

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import android.os.Bundle
import androidx.core.os.bundleOf
import com.hag.mypersonalcrossword.core.*

/**
 * Owns the puzzle being played. Everything here survives configuration changes
 * (the ViewModel outlives the Activity) and process death (the session is
 * encoded into [SavedStateHandle] via [SessionCodec] when the system saves state).
 *
 * UI-only state (dialogs, animations) and online-session plumbing stay in the
 * composable; an online game is never written to the handle because it can't be
 * resumed without the other device.
 */
class PuzzleViewModel(private val handle: SavedStateHandle) : ViewModel() {

    // ── Puzzle ────────────────────────────────────────────────────────────────
    var placedWords        by mutableStateOf(emptyList<PlacedWord>())
    var gridCells          by mutableStateOf(emptyList<GridCell>())
    var userInputs         by mutableStateOf(emptyMap<Pair<Int, Int>, Char>())
    var revealedCells      by mutableStateOf(emptySet<Pair<Int, Int>>())
    var selection          by mutableStateOf<Selection?>(null)
    var activeCategory     by mutableStateOf("")
    var combinedCategories by mutableStateOf(emptyList<String>())
    var activeDifficulty   by mutableStateOf(Difficulty.MEDIUM)
    var activeGameMode     by mutableStateOf(GameMode.SINGLE)
    var activeDailyKey     by mutableStateOf<String?>(null)
    var isDailyPuzzle      by mutableStateOf(false)
    var elapsedSeconds     by mutableLongStateOf(0L)
    var hintsUsed          by mutableIntStateOf(0)
    var streak             by mutableIntStateOf(0)
    var puzzleSolved       by mutableStateOf(false)
    var currentBgColor     by mutableStateOf(Color.LightGray)
    var currentBgImageName by mutableStateOf(BG_IMAGE_NONE)

    // ── Multiplayer (local rules state; online sync lives in the UI layer) ──
    var player2Name        by mutableStateOf("")
    var teamP1Score        by mutableIntStateOf(0)
    var teamP2Score        by mutableIntStateOf(0)
    var teamCurrentPlayer  by mutableIntStateOf(0)
    var teamP1Hints        by mutableIntStateOf(0)
    var teamP2Hints        by mutableIntStateOf(0)
    var vindPhase          by mutableStateOf(VindicativePhase.PICK_OWN)
    var vindCurrentPlayer  by mutableIntStateOf(0)
    var vindP1Score        by mutableIntStateOf(0)
    var vindP2Score        by mutableIntStateOf(0)
    var vindAssignedWord   by mutableStateOf<PlacedWord?>(null)
    var vindTimerSeconds   by mutableIntStateOf(30)

    // ── Online session — kept here so an Activity recreation (theme, font size,
    // locale…) doesn't silently drop the game; never persisted to SavedStateHandle.
    var isOnline           by mutableStateOf(false)
    var onlineRole         by mutableStateOf<OnlineRole?>(null)
    var onlineCode         by mutableStateOf("")

    /** True when the process was restored from an online game, which can't be resumed. */
    var restoredFromOnline = false
        private set

    init {
        val saved = handle.get<Bundle>(KEY_STATE)
        restoredFromOnline = saved?.getBoolean(KEY_WAS_ONLINE) ?: false
        // Older builds mirrored the session into separate handle keys.
        val raw = saved?.getString(KEY_SESSION) ?: handle.get<String>(KEY_SESSION)
        raw?.let(SessionCodec::decode)?.let { s ->
            restore(s)
            puzzleSolved = saved?.getBoolean(KEY_SOLVED) ?: handle.get<Boolean>(KEY_SOLVED) ?: false
            selection = decodeSelection(saved?.getString(KEY_SELECTION) ?: handle.get<String>(KEY_SELECTION))
        }
        // Encoded at the moment the system saves state, never from a delayed copy:
        // a debounced mirror could miss the last letter, or the payout that followed
        // it, and re-encoded the whole grid every time the clock ticked.
        handle.setSavedStateProvider(KEY_STATE) {
            val session = snapshot()
            when {
                isOnline -> bundleOf(KEY_WAS_ONLINE to true)
                session == null -> Bundle()
                else -> bundleOf(
                    KEY_SESSION   to SessionCodec.encode(session),
                    KEY_SELECTION to encodeSelection(selection),
                    KEY_SOLVED    to puzzleSolved
                )
            }
        }
    }

    /** The puzzle as a PuzzleSession (null when no puzzle is loaded). */
    fun snapshot(): PuzzleSession? {
        if (placedWords.isEmpty()) return null
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
            hintsUsed      = hintsUsed,
            streak         = streak,
            player2        = if (activeGameMode == GameMode.TEAM || activeGameMode == GameMode.VINDICTIVE) player2Name else "",
            team           = TeamState(teamP1Score, teamP2Score, teamCurrentPlayer, teamP1Hints, teamP2Hints),
            vind           = VindState(vindPhase, vindCurrentPlayer, vindP1Score, vindP2Score,
                                       vindAssignedWord?.let { placedWords.indexOf(it) } ?: -1),
            vindTimerSecs  = vindTimerSeconds,
            bgArgb         = currentBgColor.toArgb(),
            bgImage        = currentBgImageName
        )
    }

    /** Loads [s] as the current puzzle (state only — UI overlays are the caller's job). */
    fun restore(s: PuzzleSession) {
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
        hintsUsed          = s.hintsUsed
        streak             = s.streak
        if (s.player2.isNotBlank()) player2Name = s.player2
        teamP1Score = s.team.p1Correct; teamP2Score = s.team.p2Correct
        teamCurrentPlayer = s.team.turn
        teamP1Hints = s.team.p1Hints;   teamP2Hints = s.team.p2Hints
        vindPhase = s.vind.phase; vindCurrentPlayer = s.vind.currentPlayer
        vindP1Score = s.vind.p1Score; vindP2Score = s.vind.p2Score
        vindAssignedWord = s.words.getOrNull(s.vind.assignedIndex)
        vindTimerSeconds = s.vindTimerSecs
        currentBgColor     = Color(s.bgArgb)
        currentBgImageName = s.bgImage
        puzzleSolved       = false
        val board = Board(s.words)
        selection = InputEngine.initialSelection(board, s.inputs, board.lockedCells(s.inputs, s.revealed))
    }

    companion object {
        private const val KEY_STATE     = "puzzle_state"
        private const val KEY_WAS_ONLINE = "was_online"
        private const val KEY_SESSION   = "puzzle_session"
        private const val KEY_SELECTION = "puzzle_selection"
        private const val KEY_SOLVED    = "puzzle_solved"

        fun encodeSelection(s: Selection?): String =
            s?.let { "${it.cell.first},${it.cell.second},${it.direction.name}" } ?: ""

        fun decodeSelection(raw: String?): Selection? {
            val p = raw?.split(',') ?: return null
            if (p.size != 3) return null
            val x = p[0].toIntOrNull() ?: return null
            val y = p[1].toIntOrNull() ?: return null
            val d = runCatching { Direction.valueOf(p[2]) }.getOrNull() ?: return null
            return Selection(Pair(x, y), d)
        }
    }
}
