package com.hag.mypersonalcrossword.desktop

import com.hag.mypersonalcrossword.core.*
import java.io.File
import java.nio.file.Files
import kotlin.random.Random

object T {
    val entries: List<RawEntry> by lazy { Assets.wordList() }
    fun dir(): File = Files.createTempDirectory("mpc-test").toFile().also { it.deleteOnExit() }
    fun controller(store: ProfileStore = ProfileStore(dir()), seed: Int = 7, now: Long = 1_790_000_000_000L) =
        GameController(store, entries, { Random(seed) }, { now })
    class FakeHost : Host { var fullscreen = 0; var quit = 0
        override fun toggleFullscreen() { fullscreen++ }; override fun quit() { quit++ } }
    fun model(store: ProfileStore = ProfileStore(dir()), host: FakeHost = FakeHost()) =
        AppModel(store, controller(store), host)

    /** Types the whole solution through the controller, the way a player would. */
    fun solve(g: GameController) {
        for (w in g.board!!.clueOrder) {
            if (!g.active) return
            g.selectWord(w)
            for ((i, c) in w.cells().withIndex()) if (g.inputs[c] != w.word[i]) g.type(w.word[i])
        }
    }
}
