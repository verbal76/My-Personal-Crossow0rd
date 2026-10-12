package com.hag.mypersonalcrossword.desktop

import java.io.File

/** The only OS-specific knowledge in the desktop build: where data lives and which OS we are on. */
object Platform {
    enum class Os { WINDOWS, LINUX, MAC }

    val current: Os by lazy {
        val n = System.getProperty("os.name", "").lowercase()
        when { "win" in n -> Os.WINDOWS; "mac" in n || "darwin" in n -> Os.MAC; else -> Os.LINUX }
    }

    const val DATA_DIR_ENV = "MPC_DATA_DIR"

    /**
     * Save directory.
     *  Windows: %APPDATA%\HotAtticGames\MyPersonalCrossword
     *  Linux  : $XDG_DATA_HOME or ~/.local/share, then HotAtticGames/MyPersonalCrossword
     *           (on a Steam Deck that is /home/deck/.local/share/..., which survives game updates)
     * MPC_DATA_DIR overrides it (tests, smoke runs, portable installs).
     */
    fun dataDir(os: Os = current, env: Map<String, String> = System.getenv(),
                home: String = System.getProperty("user.home", ".")): File {
        env[DATA_DIR_ENV]?.takeIf { it.isNotBlank() }?.let { return File(it) }
        val base = when (os) {
            Os.WINDOWS -> env["APPDATA"]?.takeIf { it.isNotBlank() }?.let(::File) ?: File(home, "AppData/Roaming")
            Os.MAC     -> File(home, "Library/Application Support")
            Os.LINUX   -> env["XDG_DATA_HOME"]?.takeIf { it.isNotBlank() }?.let(::File) ?: File(home, ".local/share")
        }
        return File(base, "HotAtticGames/MyPersonalCrossword")
    }
}
