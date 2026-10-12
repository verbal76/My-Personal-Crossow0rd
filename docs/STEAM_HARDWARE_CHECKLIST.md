# Steam Deck / Steam Machine checklist (project-specific)

Valve references: steamhardware/recommendations and steamhardware/compat. **Not Deck Verified; never claim it.**
Legend: DONE-U unit-tested, DONE-I integration/packaged test in sandbox, PENDING-HW needs real hardware,
PENDING-SW needs Steamworks/credentials, N/A not applicable.

| Requirement | Status |
|---|---|
| Controller-only play: menu, puzzle, hints, pause, resume, save, exit, Team and Vindictive turns (Give, I'm Ready, timeout Answer/Pass) | DONE-U/I (simulated joydev events through the real window); PENDING-HW real pad |
| No mandatory mouse or keyboard (letters via on-screen keyboard) | DONE-U/I |
| Steam Input / default template; no interference with remapping | PENDING-SW (relies on Steam's Gamepad template -> Xbox pad; untested) |
| Automatic Steam on-screen keyboard | N/A today (no text fields; letters use the in-game keyboard). Steam's own keyboard also works via normal key events: PENDING-HW |
| Mixed mouse + joystick camera | N/A (no camera) |
| 1280x800 and 16:10; other ratios; fullscreen | DONE-U (layout 800x500..4K) / DONE-I (screenshots 1280x800, 1920x1080, 800x500); PENDING-HW real screen |
| Readable handheld text (clue 26px, grid numbers >= 12px, legend 18px at 1280x800) | DONE-I by screenshot; PENDING-HW on the 7" panel |
| Vulkan / graphics API | N/A (Java2D software/X11 path). Not claimed. |
| Portable codecs / audio | Sound effects synthesized in code and played through javax.sound (unit-tested; no audio device in the sandbox, so audible output is PENDING-HW). Music not ported (needs licence evidence first). |
| Graphics settings per device | N/A (no graphics options exist) |
| Offline single-player (launch, play, save, load) | DONE-I (no network code at all); first-launch-offline on a new Steam account PENDING-HW/SW |
| No external launcher | DONE (direct launch; `steam-launch.sh` is a thin exec) |
| Suspend/resume, controller reconnect | Autosave every 20 s + on focus loss, reader re-scans devices and resets held keys (DONE-U); real behaviour PENDING-HW |
| Steam Cloud | PENDING-SW (not integrated; saves are plain files in a stable per-OS directory) |
| Performance (FPS, CPU, battery, thermals) | Idle repaint removed (menu CPU 14.3% -> 1.6% of a core in the sandbox, relative only). Real FPS/CPU/GPU/battery/thermals: NOT MEASURED |
| Window focus loss | Puzzle and Vindictive clocks stop while unfocused; autosave on focus loss (DONE-U); real Alt-Tab/overlay/suspend PENDING-HW |
| Glyph library | Integrated (Xbox ABXY + keyboard keys); manual prompt style in the menu; Start/Menu glyph drawn by code (missing from the pack) |
