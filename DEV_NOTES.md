# Crossword Dev Notes

Running list of UI/UX issues to address. Add new items at the bottom with the next ID.

Status legend: **Open** · **In Progress** · **Fixed**

---

## 001 — Main menu scroll cuts off Play Mode container
**Area:** Main Menu / Play Mode section
**Status:** Fixed (2026-10 re-audit)

**Resolution:** The layout described here no longer exists (home is now header → Continue → Daily → Play Mode + START → carousel, with Play Mode near the top). The underlying cause — a home column that couldn't scroll — remained: on short screens or with large text the carousel was squeezed. Home now scrolls when its content is taller than the screen and still centres the carousel otherwise.

**Description:**
The main menu scrolls through a 2-column category grid (Athletes, Authors, Cartoons, Cities, etc.). Below the grid sits the Play Mode section — a single full-width rounded purple container with radio options. Single Player and Team Mode are visible, but **Vindictive Mode is cut off below the fold**. The container's bottom edge never comes into view, so there's no visual cue that it has a bottom. Below Play Mode are the yellow music player bar and the Combine Categories button, which are also affected.

**Fix:** Extend the scrollable region so it clears the full Play Mode container plus a small padding gap beneath it, so the user can see the container has a defined bottom edge.

---

## 002 — Puzzle completion popup not scrollable
**Area:** Puzzle completion dialog
**Status:** Fixed (2026-10 re-audit)

**Resolution:** The results card's buttons sit in the dialog's button slots, so they can no longer be pushed off-screen. Its text now also scrolls, so stat lines aren't cut off on short screens or with large text.

**Description:**
When a puzzle is completed, the popup that appears is not scrollable. The "Return to main menu" button is clipped — only the top edge is visible — and the user cannot reach it.

**Fix:** Make the completion dialog content scrollable, or size it so all buttons (including Return to main menu) are fully visible on all supported screen sizes.

---

## General Notes
- Add new issues below with incrementing IDs (003, 004, …).
- For each: **Area**, **Status**, **Description**, and **Fix** (if known).
