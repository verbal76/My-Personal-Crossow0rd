#!/bin/sh
# Steam launch wrapper. In Steam: Properties > Launch Options is empty and the executable is this
# file, or set the executable to bin/MyPersonalCrossword directly. Steam Deck Game Mode: run this
# with "--fullscreen" (Steam Input's default Gamepad template works; see README.txt).
DIR="$(cd "$(dirname "$0")" && pwd)"
# Saves go to $XDG_DATA_HOME/HotAtticGames/MyPersonalCrossword (Deck: /home/deck/.local/share/...).
# Gamescope already fills the screen; the game also fills it when asked.
exec "$DIR/bin/MyPersonalCrossword" "$@"
