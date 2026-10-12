package com.hag.mypersonalcrossword.desktop

import com.hag.mypersonalcrossword.core.*
import java.awt.*
import java.awt.event.*
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import javax.swing.JPanel
import javax.swing.Timer
import kotlin.math.max
import kotlin.math.min

/** The single drawing surface. All screens are painted with Java2D; input is routed into [AppModel]. */
class GamePanel(private val model: AppModel) : JPanel() {
    private companion object {
        val OPTION_ROWS = setOf(AppModel.MenuItem.MODE, AppModel.MenuItem.CATEGORY, AppModel.MenuItem.DIFFICULTY, AppModel.MenuItem.TIMER,
            AppModel.MenuItem.BACKGROUND, AppModel.MenuItem.PROMPTS, AppModel.MenuItem.SOUND)
    }
    private val game = model.game
    private var camX = 0; private var camY = 0
    private val clock = Timer(33) { onTick() }
    private var last = System.nanoTime()
    /** Polls the gamepad for held-direction repeats; returns true if it produced an action. */
    var onPadPoll: (Long) -> Boolean = { false }

    // Clue list hit areas, refreshed on every paint.
    private var clueHits: List<Pair<Rect, PlacedWord>> = emptyList()
    private var menuHits: List<Pair<Rect, Int>> = emptyList()
    private var modalHits: List<Rect> = emptyList()
    private var actionHit: Rect? = null
    private var pauseHits: List<Rect> = emptyList()
    private var lastLayout: PuzzleLayout? = null

    init {
        isFocusable = true
        focusTraversalKeysEnabled = false      // Tab must reach us as a key (next clue)
        isDoubleBuffered = true
        background = Color.BLACK
        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                val a = KeyMap.action(e.keyCode, e.isAltDown, e.isShiftDown) ?: return
                model.act(a); repaint()
            }
            override fun keyTyped(e: KeyEvent) {
                if (e.isControlDown || e.isAltDown || e.isMetaDown) return
                KeyMap.letter(e.keyChar)?.let { model.letter(it); repaint() }
            }
        })
        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) { requestFocusInWindow(); click(e.x, e.y) }
        })
        addMouseWheelListener { e -> if (model.screen == Screen.MENU || model.screen == Screen.PAUSE)
            model.act(if (e.wheelRotation > 0) Action.DOWN else Action.UP).also { repaint() } }
        addComponentListener(object : ComponentAdapter() { override fun componentResized(e: ComponentEvent) { repaint() } })
        addHierarchyListener { if (isShowing) { clock.start(); requestFocusInWindow() } else clock.stop() }
    }

    private var lastPaintedSecond = -1L
    private var lastVindSecond = -1L
    private var lastModal: Modal? = null

    /**
     * Event-driven drawing: the 33 ms tick advances the model and polls the gamepad (both cheap) but only
     * asks for a repaint when something visible can have changed. Idle menus and a paused puzzle therefore
     * cost almost nothing (the old unconditional 30 fps repaint kept a core busy; see docs/DESKTOP.md).
     * Input handlers repaint themselves; this covers time-driven changes.
     */
    private fun onTick() {
        val now = System.nanoTime()
        val ms = ((now - last) / 1_000_000L).coerceIn(0, 500); last = now
        val before = model.screen
        model.tick(ms)
        var padActed = false
        onPadPoll(System.currentTimeMillis()).also { padActed = it }
        if (needsRepaint(before, padActed)) repaint()
    }

    /** True when the picture can differ from the last one drawn without any input event. */
    private fun needsRepaint(before: Screen, padActed: Boolean): Boolean = when {
        padActed || model.screen != before -> true
        model.screen == Screen.STUDIO_SPLASH || model.screen == Screen.GAME_SPLASH -> true      // fade animation
        model.screen == Screen.PUZZLE -> {
            val sec = game.session?.elapsedSeconds ?: -1L
            val vsec = if (game.forcedWord != null) (game.vindMsLeft + 999) / 1000 else -1L
            val modalNow = game.modal
            val changed = sec != lastPaintedSecond || vsec != lastVindSecond || modalNow !== lastModal
            lastPaintedSecond = sec; lastVindSecond = vsec; lastModal = modalNow
            changed
        }
        else -> false
    }

    // ── Mouse / touch ────────────────────────────────────────────────────────

    fun click(x: Int, y: Int) {
        when (model.screen) {
            Screen.STUDIO_SPLASH, Screen.GAME_SPLASH, Screen.RESULTS -> model.act(Action.CONFIRM)
            Screen.MENU -> menuHits.firstOrNull { it.first.contains(x, y) }?.let { (r, i) ->
                model.focusMenu(i)
                val item = model.menuItems[i]
                // Tap the left/right third of an option row to step it back/forward; elsewhere activates.
                if (item in OPTION_ROWS) {
                    model.act(if (x < r.x + r.w / 3) Action.LEFT else Action.RIGHT)
                } else model.activate(item)
            }
            Screen.PAUSE -> pauseHits.indexOfFirst { it.contains(x, y) }.takeIf { it >= 0 }?.let { model.act(Action.DOWN); while (model.pauseIndex != it) model.act(Action.DOWN); model.act(Action.CONFIRM) }
            Screen.PUZZLE -> puzzleClick(x, y)
        }
        model.noteMouse()
        repaint()
    }

    private fun puzzleClick(x: Int, y: Int) {
        val lay = lastLayout ?: return
        val b = game.board ?: return
        if (game.modal != null) { modalHits.indexOfFirst { it.contains(x, y) }.takeIf { it >= 0 }?.let { model.answerModal(it) }; return }
        if (lay.keyboard != null && lay.keyboard.contains(x, y)) {
            model.keyboard.hit(lay.keyboard, x, y)?.let { (r, c) -> model.keyboard.set(r, c); model.pressKey(model.keyboard.key()) }
            return
        }
        if (lay.header.contains(x, y)) {
            val fromRight = lay.header.w - x
            when {
                fromRight < (150 * lay.ui).toInt() -> model.act(Action.MENU)
                fromRight < (330 * lay.ui).toInt() -> model.act(Action.HINT)
                fromRight < (480 * lay.ui).toInt() -> model.act(Action.TOGGLE_KEYBOARD)
            }
            return
        }
        actionHit?.let { if (it.contains(x, y)) { if (game.canGive) game.give() else game.pass(); return } }
        clueHits.firstOrNull { it.first.contains(x, y) }?.let { game.selectWord(it.second); return }
        if (lay.board.contains(x, y)) {
            val cx = camX + (x - boardOrigin(lay, b).first) / lay.cell
            val cy = camY + (y - boardOrigin(lay, b).second) / lay.cell
            game.tap(Pair(cx, cy))
        }
    }

    private fun boardOrigin(lay: PuzzleLayout, b: Board): Pair<Int, Int> {
        val cols = (b.words.maxOf { if (it.isHorizontal) it.startX + it.word.length else it.startX + 1 })
        val rows = (b.words.maxOf { if (it.isHorizontal) it.startY + 1 else it.startY + it.word.length })
        val drawnW = min(cols, lay.colsVisible) * lay.cell; val drawnH = min(rows, lay.rowsVisible) * lay.cell
        return (lay.board.x + (lay.board.w - drawnW) / 2) to (lay.board.y + (lay.board.h - drawnH) / 2)
    }

    // ── Painting ─────────────────────────────────────────────────────────────

    override fun paintComponent(g0: Graphics) {
        val g = g0.create() as Graphics2D
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g.color = Color.BLACK; g.fillRect(0, 0, width, height)
            when (model.screen) {
                Screen.STUDIO_SPLASH -> splash(g, Assets.studioLogo, model.screenMillis, AppModel.STUDIO_MS, fit = true)
                Screen.GAME_SPLASH   -> splash(g, Assets.gameSplash, model.screenMillis, AppModel.GAME_MS, fit = true)
                Screen.MENU -> { backdrop(g); menu(g) }
                Screen.PUZZLE, Screen.PAUSE -> { backdrop(g); puzzle(g); if (model.screen == Screen.PAUSE) pause(g) }
                Screen.RESULTS -> { backdrop(g); results(g) }
            }
        } finally { g.dispose() }
    }

    private fun ui() = PuzzleLayout.scale(width, height)
    private fun font(g: Graphics2D, px: Float, bold: Boolean = false): Font =
        Font(Font.SANS_SERIF, if (bold) Font.BOLD else Font.PLAIN, 1).deriveFont(px)

    private fun splash(g: Graphics2D, img: BufferedImage?, t: Long, total: Long, fit: Boolean) {
        if (img == null) return
        val s = min(width.toFloat() / img.width, height.toFloat() / img.height)
        val w = (img.width * s).toInt(); val h = (img.height * s).toInt()
        val fade = when { t < 300 -> t / 300f; t > total - 300 -> max(0f, (total - t) / 300f); else -> 1f }
        g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fade.coerceIn(0f, 1f))
        g.drawImage(img, (width - w) / 2, (height - h) / 2, w, h, null)
        g.composite = AlphaComposite.SrcOver
    }

    private fun backdrop(g: Graphics2D) {
        val bg = model.background
        val img = Assets.background(bg.file)
        if (img != null) {
            val s = max(width.toFloat() / img.width, height.toFloat() / img.height)   // cover
            val w = (img.width * s).toInt(); val h = (img.height * s).toInt()
            g.drawImage(img, (width - w) / 2, (height - h) / 2, w, h, null)
        } else { g.color = Color(0x20, 0x24, 0x30); g.fillRect(0, 0, width, height) }
        val scrim = max(bg.scrim, 0.25f)       // desktop text sits on tiles/panels, but keep the picture calm
        g.color = Color(0, 0, 0, (scrim * 255).toInt()); g.fillRect(0, 0, width, height)
    }

    private fun panel(g: Graphics2D, r: Rect, alpha: Int = 170) {
        g.color = Color(15, 18, 28, alpha)
        g.fill(RoundRectangle2D.Float(r.x.toFloat(), r.y.toFloat(), r.w.toFloat(), r.h.toFloat(), 18f * ui(), 18f * ui()))
    }

    private fun centered(g: Graphics2D, s: String, cx: Int, baseline: Int) {
        g.drawString(s, cx - g.fontMetrics.stringWidth(s) / 2, baseline)
    }

    private var menuOffset = 0

    private fun menu(g: Graphics2D) {
        val u = ui()
        val w = (640 * u).toInt(); val rowH = (56 * u).toInt()
        val items = model.menuItems
        val top = (112 * u).toInt()
        val maxRows = max(((height - (46 * u).toInt() - 2 * (10 * u).toInt() - top - (16 * u).toInt()) / rowH), 3)
        val visible = min(items.size, maxRows)
        menuOffset = PuzzleLayout.camera(model.menuIndex, items.size, visible, menuOffset)      // scroll to keep the focus on screen
        val h = top + visible * rowH + (16 * u).toInt()
        val x = (width - w) / 2; val y = max((height - (46 * u).toInt() - h) / 2, (10 * u).toInt())
        panel(g, Rect(x, y, w, h), 190)
        g.color = Color.WHITE; g.font = font(g, 40 * u, true)
        centered(g, "My Personal Crossword", width / 2, y + (58 * u).toInt())
        g.font = font(g, 18 * u); g.color = Color(200, 205, 220)
        centered(g, "Score ${model.store.score}   ·   ${Assets.versionName}", width / 2, y + (92 * u).toInt())
        if (model.notice.isNotEmpty()) {      // set-aside / recovered save: say so, never hide it
            g.font = font(g, 15 * u); g.color = Color(255, 205, 110)
            centered(g, clip(g, model.notice, width - (32 * u).toInt()), width / 2, y + h + (20 * u).toInt())
        }
        val hits = ArrayList<Pair<Rect, Int>>()
        for (i in menuOffset until menuOffset + visible) {
            val item = items[i]
            val r = Rect(x + (24 * u).toInt(), y + top + (i - menuOffset) * rowH, w - (48 * u).toInt(), rowH - (8 * u).toInt())
            hits += r to i
            val focus = i == model.menuIndex
            g.color = if (focus) Color(88, 101, 242) else Color(255, 255, 255, 28)
            g.fill(RoundRectangle2D.Float(r.x.toFloat(), r.y.toFloat(), r.w.toFloat(), r.h.toFloat(), 14f * u, 14f * u))
            if (focus) { g.color = Color.WHITE; g.stroke = BasicStroke(3f * u); g.draw(RoundRectangle2D.Float(r.x.toFloat(), r.y.toFloat(), r.w.toFloat(), r.h.toFloat(), 14f * u, 14f * u)) }
            g.color = Color.WHITE; g.font = font(g, 25 * u, true)
            val base = r.y + r.h / 2 + (9 * u).toInt()
            g.drawString(item.label, r.x + (20 * u).toInt(), base)
            val value = when (item) {
                AppModel.MenuItem.MODE -> "◀  ${model.modeLabel(model.playMode)}  ▶"
                AppModel.MenuItem.CATEGORY -> "◀  ${model.category}  ▶"
                AppModel.MenuItem.DIFFICULTY -> "◀  ${model.difficulty.label}  ▶"
                AppModel.MenuItem.TIMER -> "◀  ${model.store.vindTimer} s  ▶"
                AppModel.MenuItem.BACKGROUND -> "◀  ${model.background.title}  ▶"
                AppModel.MenuItem.PROMPTS -> "◀  ${model.store.prompts.replaceFirstChar { it.uppercase() }}  ▶"
                AppModel.MenuItem.SOUND -> if (model.store.sound) "On" else "Off"
                AppModel.MenuItem.FULLSCREEN -> if (model.store.fullscreen) "On" else "Off"
                AppModel.MenuItem.DAILY -> if (game.dailyDoneToday) "done today" else game.dailyKeyToday
                AppModel.MenuItem.CONTINUE -> model.savedSessions.firstOrNull()?.let { "${modeShort(it.mode)}${it.category} · ${it.difficulty.label}" } ?: ""
                else -> ""
            }
            if (value.isNotEmpty()) { g.font = font(g, 21 * u); g.drawString(value, r.right - g.fontMetrics.stringWidth(value) - (20 * u).toInt(), base) }
        }
        g.color = Color(255, 255, 255, 150); g.font = font(g, 16 * u)
        if (menuOffset > 0) centered(g, "▲", x + w / 2, y + top - (4 * u).toInt())
        if (menuOffset + visible < items.size) centered(g, "▼", x + w / 2, y + h - (2 * u).toInt())
        menuHits = hits
        promptBar(g)
    }

    private fun modeShort(m: GameMode) = when (m) { GameMode.TEAM -> "Team · "; GameMode.VINDICTIVE -> "Vindictive · "; else -> "" }

    /** Glyph legend along the bottom; follows whichever device was used last. */
    private fun promptBar(g: Graphics2D) {
        val u = ui()
        val hint = when { game.modal != null -> PuzzleHint.MODAL; game.canGive -> PuzzleHint.GIVE; game.canPass -> PuzzleHint.PASS_OPEN; else -> PuzzleHint.NONE }
        val prompts = PromptBar.forState(model.screen, model.promptInput, model.keyboardOpen, hint)
        if (prompts.isEmpty()) return
        val icon = (30 * u).toInt(); val gap = (22 * u).toInt(); val small = (6 * u).toInt()
        g.font = font(g, 18 * u)
        val fm = g.fontMetrics
        fun gw(name: String) = Glyphs.image(name)?.let { (icon * it.width.toFloat() / it.height).toInt() } ?: icon
        val widths = prompts.map { p -> p.glyphs.sumOf { gw(it) + small } + fm.stringWidth(p.label) }
        var x = (width - (widths.sum() + gap * (prompts.size - 1))) / 2
        val y = height - icon - (8 * u).toInt()
        for ((i, p) in prompts.withIndex()) {
            for (name in p.glyphs) { val w = gw(name); Glyphs.image(name)?.let { g.drawImage(it, x, y, w, icon, null) }; x += w + small }
            g.color = Color(235, 238, 248); g.drawString(p.label, x, y + (icon + fm.ascent - fm.descent) / 2)
            x += fm.stringWidth(p.label) + gap
        }
    }

    private fun puzzle(g: Graphics2D) {
        val b = game.board ?: return
        val s = game.session ?: return
        val cols = b.words.maxOf { if (it.isHorizontal) it.startX + it.word.length else it.startX + 1 }
        val rows = b.words.maxOf { if (it.isHorizontal) it.startY + 1 else it.startY + it.word.length }
        val lay = PuzzleLayout.compute(width, height, cols, rows, model.keyboardOpen, statusStrip = s.mode == GameMode.TEAM || s.mode == GameMode.VINDICTIVE)
        lastLayout = lay
        val u = lay.ui
        val sel = game.selection
        if (sel != null) {
            camX = PuzzleLayout.camera(sel.cell.first, cols, lay.colsVisible, camX)
            camY = PuzzleLayout.camera(sel.cell.second, rows, lay.rowsVisible, camY)
        } else { camX = 0; camY = 0 }

        // Header
        g.color = Color(10, 12, 20, 210); g.fillRect(0, 0, lay.header.w, lay.header.h)
        g.color = Color.WHITE; g.font = font(g, 24 * u, true)
        g.drawString("${s.category} · ${s.difficulty.label}", (16 * u).toInt(), (36 * u).toInt())
        val t = s.elapsedSeconds
        g.font = font(g, 22 * u)
        centered(g, "%d:%02d   Hints %d   Score %d".format(t / 60, t % 60, s.hintsUsed, model.store.score), width / 2, (36 * u).toInt())
        g.font = font(g, 20 * u, true); g.color = Color(160, 235, 190)
        g.drawString("Keys", width - (460 * u).toInt(), (36 * u).toInt())
        g.color = Color(250, 214, 96)
        g.drawString("Hint (F1)", width - (320 * u).toInt(), (36 * u).toInt())
        g.color = Color(180, 190, 255); g.drawString("Menu", width - (130 * u).toInt(), (36 * u).toInt())
        if (lay.status.h > 0) {                   // Team / Vindictive: whose turn, what to do, the score
            g.color = Color(40, 44, 90, 235); g.fillRect(0, lay.status.y, lay.status.w, lay.status.h)
            g.font = font(g, 19 * u, true); g.color = Color(255, 235, 170)
            val avail = width - (300 * u).toInt()
            centered(g, clip(g, game.statusLine, avail), width / 2 - (60 * u).toInt(), lay.status.y + (lay.status.h + g.fontMetrics.ascent - g.fontMetrics.descent) / 2)
            if (game.canGive || game.canPass) {   // the Vindictive action of the moment, as a button you can also click or tap
                val r = Rect(width - (240 * u).toInt(), lay.status.y + (4 * u).toInt(), (220 * u).toInt(), lay.status.h - (8 * u).toInt())
                actionHit = r
                g.color = Color(183, 28, 28); g.fillRoundRect(r.x, r.y, r.w, r.h, 12, 12)
                g.color = Color.WHITE; g.font = font(g, 18 * u, true)
                centered(g, if (game.canGive) "Give  (Enter)" else "Pass  (F2)", r.x + r.w / 2, r.y + (r.h + g.fontMetrics.ascent - g.fontMetrics.descent) / 2)
            } else actionHit = null
        } else actionHit = null

        // Board
        val (ox, oy) = boardOrigin(lay, b)
        val locked = game.locked
        val numbers = buildGridCells(s.words).filter { it.number != null }.associate { Pair(it.x, it.y) to it.number!! }
        val activeCells = game.activeWord?.cells()?.toSet() ?: emptySet()
        val cell = lay.cell
        val oldClip = g.clip
        g.clip = Rectangle(lay.board.x, lay.board.y, lay.board.w, lay.board.h)
        for (cy in camY until min(rows, camY + lay.rowsVisible)) for (cx in camX until min(cols, camX + lay.colsVisible)) {
            val c = Pair(cx, cy)
            if (!b.contains(c)) continue
            val px = ox + (cx - camX) * cell; val py = oy + (cy - camY) * cell
            val isSel = sel?.cell == c
            g.color = when {
                isSel -> Color(255, 224, 102)
                c in activeCells -> Color(184, 205, 255)
                c in locked -> Color(196, 235, 200)
                else -> Color(250, 250, 252)
            }
            g.fillRect(px + 1, py + 1, cell - 2, cell - 2)
            g.color = Color(40, 44, 60); g.stroke = BasicStroke(max(1f, 1.5f * u)); g.drawRect(px + 1, py + 1, cell - 2, cell - 2)
            numbers[c]?.let { n ->
                g.font = font(g, max(cell * 0.28f, 12f * u)); g.color = Color(30, 34, 50)
                g.drawString(n.toString(), px + (4 * u).toInt(), py + g.fontMetrics.ascent + (2 * u).toInt())
            }
            game.inputs[c]?.let { ch ->
                val wrong = c !in locked && b.solution[c] != ch
                g.color = when { wrong -> Color(190, 40, 40); c in game.revealed -> Color(30, 90, 170); else -> Color(15, 18, 30) }
                g.font = font(g, cell * 0.58f, true)
                val str = ch.toString(); val fm = g.fontMetrics
                g.drawString(str, px + (cell - fm.stringWidth(str)) / 2, py + (cell + fm.ascent - fm.descent) / 2 + (cell * 0.06f).toInt())
            }
        }
        g.clip = oldClip

        // Clue panel
        panel(g, lay.clues)
        clueHits = cluePanel(g, lay, b)

        if (lay.keyboard != null) osk(g, lay.keyboard)
        if (game.message.isNotEmpty()) {
            g.font = font(g, 19 * u); g.color = Color(255, 255, 255, 235)
            val m = lay.message
            g.drawString(clip(g, game.message, m.w), m.x, m.y + (m.h + g.fontMetrics.ascent - g.fontMetrics.descent) / 2)
        }
        modalOverlay(g)
        promptBar(g)
    }

    /** "Pass the Phone", timeout and welcome-back messages. Everything behind them is held until one is answered. */
    private fun modalOverlay(g: Graphics2D) {
        val m = game.modal
        if (m == null) { modalHits = emptyList(); return }
        val u = ui()
        g.color = Color(0, 0, 0, 165); g.fillRect(0, 0, width, height)
        val w = (760 * u).toInt()
        g.font = font(g, 24 * u)
        val lines = m.message.split('\n').flatMap { wrap(g, it, w - (60 * u).toInt()) }
        val rowH = (30 * u).toInt()
        val h = (150 * u).toInt() + lines.size * rowH
        val x = (width - w) / 2; val y = (height - h) / 2 - (20 * u).toInt()
        panel(g, Rect(x, y, w, h), 240)
        g.color = Color(250, 214, 96); g.font = font(g, 36 * u, true); centered(g, m.title, width / 2, y + (56 * u).toInt())
        g.color = Color.WHITE; g.font = font(g, 24 * u)
        lines.forEachIndexed { i, l -> centered(g, l, width / 2, y + (100 * u).toInt() + i * rowH) }
        val bw = (200 * u).toInt(); val bh = (52 * u).toInt(); val gap = (20 * u).toInt()
        val total = m.buttons.size * bw + (m.buttons.size - 1) * gap
        val hits = ArrayList<Rect>()
        m.buttons.forEachIndexed { i, label ->
            val r = Rect((width - total) / 2 + i * (bw + gap), y + h - bh - (22 * u).toInt(), bw, bh)
            hits += r
            g.color = if (i == model.modalIndex) Color(88, 101, 242) else Color(255, 255, 255, 40)
            g.fillRoundRect(r.x, r.y, r.w, r.h, 14, 14)
            if (i == model.modalIndex) { g.color = Color.WHITE; g.stroke = BasicStroke(3f * u); g.drawRoundRect(r.x, r.y, r.w, r.h, 14, 14) }
            g.color = Color.WHITE; g.font = font(g, 22 * u, true)
            centered(g, label, r.x + r.w / 2, r.y + (r.h + g.fontMetrics.ascent - g.fontMetrics.descent) / 2)
        }
        modalHits = hits
    }

    private fun cluePanel(g: Graphics2D, lay: PuzzleLayout, b: Board): List<Pair<Rect, PlacedWord>> {
        val u = lay.ui; val r = lay.clues
        val active = game.activeWord
        val pad = (14 * u).toInt()
        // Large active clue on top: the legibility anchor on a 7" screen.
        g.color = Color.WHITE; g.font = font(g, 30 * u, true)
        var y = r.y + pad + g.fontMetrics.ascent
        if (active != null) {
            g.color = Color(250, 214, 96)
            g.drawString("${active.number} ${if (active.isHorizontal) "ACROSS" else "DOWN"}  (${active.word.length})", r.x + pad, y)
            g.color = Color.WHITE; g.font = font(g, 26 * u)
            for (line in wrap(g, active.clue, r.w - 2 * pad)) { y += (g.fontMetrics.height * 1.05).toInt(); g.drawString(line, r.x + pad, y) }
            y += (14 * u).toInt()
        }
        g.color = Color(255, 255, 255, 60); g.drawLine(r.x + pad, y, r.right - pad, y); y += (8 * u).toInt()
        // Compact list around the active clue so it is always visible.
        val rowH = (30 * u).toInt()
        val avail = max((r.bottom - y - pad) / rowH, 1)
        val order = b.clueOrder
        val idx = max(order.indexOf(active), 0)
        val first = (idx - avail / 2).coerceIn(0, max(order.size - avail, 0))
        val hits = ArrayList<Pair<Rect, PlacedWord>>()
        g.font = font(g, 19 * u)
        for (i in first until min(order.size, first + avail)) {
            val w = order[i]; val rr = Rect(r.x + pad / 2, y, r.w - pad, rowH)
            if (w == active) { g.color = Color(88, 101, 242, 200); g.fillRoundRect(rr.x, rr.y, rr.w, rr.h, 10, 10) }
            g.color = if (isWordSolved(w, game.inputs)) Color(140, 220, 150) else Color.WHITE
            val label = "${w.number}${if (w.isHorizontal) "A" else "D"}  ${w.clue}"
            g.drawString(clip(g, label, rr.w - pad), rr.x + pad / 2, rr.y + (rowH + g.fontMetrics.ascent - g.fontMetrics.descent) / 2)
            hits += rr to w
            y += rowH
        }
        return hits
    }

    private fun wrap(g: Graphics2D, text: String, maxW: Int): List<String> {
        val out = ArrayList<String>(); var line = ""
        for (word in text.split(' ')) {
            val t = if (line.isEmpty()) word else "$line $word"
            if (g.fontMetrics.stringWidth(t) > maxW && line.isNotEmpty()) { out += line; line = word } else line = t
        }
        if (line.isNotEmpty()) out += line
        return out
    }

    private fun clip(g: Graphics2D, text: String, maxW: Int): String {
        if (g.fontMetrics.stringWidth(text) <= maxW) return text
        var t = text
        while (t.length > 1 && g.fontMetrics.stringWidth("$t…") > maxW) t = t.dropLast(1)
        return "$t…"
    }

    private fun osk(g: Graphics2D, area: Rect) {
        val u = ui()
        panel(g, area, 215)
        val k = model.keyboard
        g.font = font(g, 26 * u, true)
        for (r in OnScreenKeyboard.ROWS.indices) for (c in OnScreenKeyboard.ROWS[r].indices) {
            val kr = k.keyRect(area, r, c)
            val focus = r == k.row && c == k.col
            g.color = if (focus) Color(88, 101, 242) else Color(255, 255, 255, 40)
            g.fillRoundRect(kr.x + 3, kr.y + 3, kr.w - 6, kr.h - 6, 12, 12)
            g.color = Color.WHITE
            val label = OnScreenKeyboard.ROWS[r][c]
            g.font = font(g, if (label.length > 1) 18 * u else 26 * u, true)
            centered(g, label, kr.x + kr.w / 2, kr.y + (kr.h + g.fontMetrics.ascent - g.fontMetrics.descent) / 2)
        }
    }

    private fun pause(g: Graphics2D) {
        val u = ui()
        g.color = Color(0, 0, 0, 150); g.fillRect(0, 0, width, height)
        val w = (460 * u).toInt(); val rowH = (62 * u).toInt(); val h = (90 * u).toInt() + model.pauseItems.size * rowH
        val x = (width - w) / 2; val y = (height - h) / 2
        panel(g, Rect(x, y, w, h), 235)
        g.color = Color.WHITE; g.font = font(g, 32 * u, true); centered(g, "Paused", width / 2, y + (52 * u).toInt())
        val hits = ArrayList<Rect>()
        model.pauseItems.forEachIndexed { i, label ->
            val r = Rect(x + (24 * u).toInt(), y + (72 * u).toInt() + i * rowH, w - (48 * u).toInt(), rowH - (8 * u).toInt())
            hits += r
            g.color = if (i == model.pauseIndex) Color(88, 101, 242) else Color(255, 255, 255, 28)
            g.fillRoundRect(r.x, r.y, r.w, r.h, 14, 14)
            g.color = Color.WHITE; g.font = font(g, 24 * u, true)
            centered(g, label, r.x + r.w / 2, r.y + (r.h + g.fontMetrics.ascent - g.fontMetrics.descent) / 2)
        }
        pauseHits = hits
    }

    private fun results(g: Graphics2D) {
        val u = ui(); val res = game.result ?: return
        val lines = ArrayList<String>()
        val t = res.seconds
        lines += "Time  %d:%02d".format(t / 60, t % 60)
        var title = "Puzzle Complete!"
        when (res.mode) {
            GameMode.VINDICTIVE -> {
                val w = VindictiveRules.winner(res.vind)
                title = if (w == null) "It's a tie!" else "${res.names[w]} wins!"
                lines += "${res.names[0]}: ${res.vind.p1Score} pts   ${res.names[1]}: ${res.vind.p2Score} pts"
                lines += "Banked to your score  +${res.awarded}" + if (res.paid) "" else "   (already paid)"
            }
            GameMode.TEAM -> {
                lines += "${res.names[0]}: ${res.team.p1Correct} solved   ${res.names[1]}: ${res.team.p2Correct} solved"
                TeamRules.mvp(res.team)?.let { lines += "MVP  ${res.names[it]}" }
                lines += "Hints used  ${res.hints}"
                lines += "Reward  +${res.reward}   Net  ${res.net}" + if (res.paid) "" else "   (already paid)"
            }
            else -> {
                lines += "Hints used  ${res.hints}"
                lines += "Reward  +${res.reward}   Net  ${res.net}" + if (res.paid) "" else "   (already paid)"
            }
        }
        lines += "Total score  ${model.store.score}"
        val w = (700 * u).toInt(); val h = (200 * u).toInt() + lines.size * (44 * u).toInt()
        val x = (width - w) / 2; val y = (height - h) / 2
        panel(g, Rect(x, y, w, h), 215)
        g.color = Color(250, 214, 96); g.font = font(g, 44 * u, true); centered(g, title, width / 2, y + (72 * u).toInt())
        g.color = Color.WHITE; g.font = font(g, 26 * u)
        lines.forEachIndexed { i, line -> centered(g, line, width / 2, y + (130 * u).toInt() + i * (44 * u).toInt()) }
        g.font = font(g, 20 * u); g.color = Color(200, 205, 220)
        centered(g, if (model.lastInput == "pad") "Press A to continue" else "Press Enter or click to continue", width / 2, y + h - (28 * u).toInt())
    }
}
