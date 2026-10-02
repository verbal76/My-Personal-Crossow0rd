package com.hag.mypersonalcrossword

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hag.mypersonalcrossword.core.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// ============================================================
// CROSSWORD BOARD UI — Canvas grid, clue bar, on-screen keyboard.
// ============================================================

/** A one-shot grid animation (solved-word wave or wrong-word shake). */
data class GridFx(val cells: List<Cell>, val id: Long)

private val SolvedGreen = Color(0xFF2E7D32)
private val AssignedAmber = Color(0xFFFFB300)
private val WrongRed = Color(0xFFD32F2F)

/**
 * The puzzle grid. One Canvas draws every cell; pan and zoom are applied in the
 * draw phase only (reading gesture state there never recomposes), so Genius
 * grids stay smooth. Scale 1 fits the whole grid; boards whose cells would be
 * smaller than ~30dp open zoomed in on the selection.
 */
@Composable
fun CrosswordGrid(
    board:        Board,
    cells:        List<GridCell>,
    inputs:       Map<Cell, Char>,
    revealed:     Set<Cell>,
    locked:       Set<Cell>,
    selection:    Selection?,
    activeWord:   PlacedWord?,
    assignedWord: PlacedWord?,
    cellColor:    Color,
    accent:       Color,
    wave:         GridFx?,
    shake:        GridFx?,
    onTapCell:    (Cell) -> Unit,
    modifier:     Modifier = Modifier
) {
    if (cells.isEmpty()) { Box(modifier); return }
    val density = LocalDensity.current
    val tapCell by rememberUpdatedState(onTapCell)
    val minX = remember(cells) { cells.minOf { it.x } }
    val minY = remember(cells) { cells.minOf { it.y } }
    val gridW = remember(cells) { cells.maxOf { it.x } - minX + 1 }
    val gridH = remember(cells) { cells.maxOf { it.y } - minY + 1 }

    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var zoom     by remember { mutableFloatStateOf(1f) }
    var panOff   by remember { mutableStateOf(Offset.Zero) }

    // Cell edge at zoom 1 = fit to view, capped at 56dp.
    val maxCellPx = with(density) { 56.dp.toPx() }
    val readablePx = with(density) { 30.dp.toPx() }
    val baseCell = if (viewSize == IntSize.Zero) 1f else
        min(min(viewSize.width.toFloat() / gridW, viewSize.height.toFloat() / gridH), maxCellPx)
    val maxScale = max(3f, readablePx / baseCell * 1.8f)

    fun clampOffset(o: Offset, s: Float): Offset {
        val gw = gridW * baseCell * s; val gh = gridH * baseCell * s
        val mx = max(0f, (gw - viewSize.width) / 2f) + baseCell
        val my = max(0f, (gh - viewSize.height) / 2f) + baseCell
        return Offset(o.x.coerceIn(-mx, mx), o.y.coerceIn(-my, my))
    }
    fun cellCenterView(c: Cell, s: Float, o: Offset): Offset = Offset(
        viewSize.width / 2f + o.x + ((c.first - minX + 0.5f) - gridW / 2f) * baseCell * s,
        viewSize.height / 2f + o.y + ((c.second - minY + 0.5f) - gridH / 2f) * baseCell * s
    )

    // New board: start zoomed so letters are readable. The view also resizes when the
    // clue bar or a banner changes height; that keeps the player's zoom and only
    // re-clamps the pan, instead of snapping the board back.
    var fittedBoard by remember { mutableStateOf<Board?>(null) }
    LaunchedEffect(board, viewSize) {
        if (viewSize == IntSize.Zero) return@LaunchedEffect
        if (fittedBoard === board) {
            zoom = zoom.coerceIn(1f, maxScale)
            panOff = clampOffset(panOff, zoom)
            return@LaunchedEffect
        }
        fittedBoard = board
        zoom = if (baseCell < readablePx) min(readablePx / baseCell, maxScale) else 1f
        panOff = Offset.Zero
        selection?.let { sel ->
            val p = cellCenterView(sel.cell, zoom, Offset.Zero)
            panOff = clampOffset(Offset(viewSize.width / 2f - p.x, viewSize.height / 2f - p.y), zoom)
        }
    }
    // Keep the selected cell on screen as the cursor moves.
    LaunchedEffect(selection?.cell) {
        val sel = selection ?: return@LaunchedEffect
        if (viewSize == IntSize.Zero) return@LaunchedEffect
        val p = cellCenterView(sel.cell, zoom, panOff)
        val margin = baseCell * zoom * 1.2f
        var dx = 0f; var dy = 0f
        if (p.x < margin) dx = margin - p.x else if (p.x > viewSize.width - margin) dx = viewSize.width - margin - p.x
        if (p.y < margin) dy = margin - p.y else if (p.y > viewSize.height - margin) dy = viewSize.height - margin - p.y
        if (dx != 0f || dy != 0f) panOff = clampOffset(panOff + Offset(dx, dy), zoom)
    }

    // One-shot animations.
    val waveProgress  = remember { Animatable(1f) }
    val shakeProgress = remember { Animatable(1f) }
    LaunchedEffect(wave?.id) { if (wave != null) { waveProgress.snapTo(0f); waveProgress.animateTo(1f, tween(650 + wave.cells.size * 45, easing = FastOutSlowInEasing)) } }
    LaunchedEffect(shake?.id) { if (shake != null) { shakeProgress.snapTo(0f); shakeProgress.animateTo(1f, tween(420)) } }

    // Pre-measured glyphs, re-measured only when the cell size changes.
    val measurer = rememberTextMeasurer(cacheSize = 64)
    val letterStyle = TextStyle(fontSize = with(density) { (baseCell * 0.56f).toSp() }, fontWeight = FontWeight.Bold)
    val numberStyle = TextStyle(fontSize = with(density) { (baseCell * 0.27f).coerceAtLeast(6f).toSp() }, fontWeight = FontWeight.SemiBold)
    val letterLayouts: Map<Char, TextLayoutResult> = remember(baseCell) {
        ('A'..'Z').associateWith { measurer.measure(it.toString(), letterStyle) }
    }
    val numberLayouts = remember(baseCell, cells) {
        cells.mapNotNull { it.number }.distinct().associateWith { measurer.measure(it.toString(), numberStyle) }
    }

    val activeCells   = remember(activeWord) { activeWord?.cells()?.toHashSet() ?: hashSetOf() }
    val assignedCells = remember(assignedWord) { assignedWord?.cells()?.toHashSet() ?: hashSetOf() }
    val solvedCells   = remember(board, inputs) {
        board.words.filter { isWordSolved(it, inputs) }.flatMap { it.cells() }.toHashSet()
    }
    val waveOrder  = remember(wave) { wave?.cells?.withIndex()?.associate { it.value to it.index } ?: emptyMap() }
    val shakeCells = remember(shake) { shake?.cells?.toHashSet() ?: hashSetOf() }

    val light      = cellLuminanceOf(cellColor) > 0.45f
    val ink        = if (light) Color(0xFF1B1B1F) else Color(0xFFF4F4F6)
    val subInk     = ink.copy(alpha = 0.72f)
    val solvedInk  = if (light) Color(0xFF1B5E20) else Color(0xFFA5D6A7)
    val outline    = if (light) Color.Black.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.22f)
    val wordFill   = lerp(cellColor, accent, 0.26f)
    val selFill    = lerp(cellColor, accent, 0.58f)
    val solvedFill = lerp(cellColor, SolvedGreen, if (light) 0.16f else 0.30f)
    val assignFill = lerp(cellColor, AssignedAmber, 0.40f)

    val gridDescription = remember(board, inputs, activeWord) {
        val solved = board.words.count { isWordSolved(it, inputs) }
        val sel = activeWord?.let { w ->
            val filled = w.cells().count { inputs[it] != null }
            " Selected: ${w.number} ${if (w.isHorizontal) "Across" else "Down"}, ${w.clue}, $filled of ${w.word.length} letters."
        } ?: ""
        "Crossword grid. $solved of ${board.words.size} words solved.$sel"
    }

    Box(
        modifier
            .clipToBounds()
            .onSizeChanged { viewSize = it }
            .semantics { contentDescription = gridDescription }
            .pointerInput(board, baseCell) {
                detectTransformGestures { centroid, pan, zoomBy, _ ->
                    val newScale = (zoom * zoomBy).coerceIn(1f, maxScale)
                    // Keep the grid point under the fingers fixed while zooming.
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val gridPoint = (centroid - center - panOff) / zoom
                    val o = centroid - center - gridPoint * newScale + pan
                    zoom = newScale
                    panOff = clampOffset(o, newScale)
                }
            }
            .pointerInput(board, baseCell) {
                // Single taps only: a double-tap handler would delay every tap by the
                // double-tap timeout and turn "tap again to switch direction" into a
                // zoom. Pinch zooms.
                detectTapGestures(
                    onTap = { p ->
                        val gx = ((p.x - size.width / 2f - panOff.x) / (baseCell * zoom) + gridW / 2f)
                        val gy = ((p.y - size.height / 2f - panOff.y) / (baseCell * zoom) + gridH / 2f)
                        if (gx < 0 || gy < 0) return@detectTapGestures
                        val cell = Pair(minX + gx.toInt(), minY + gy.toInt())
                        if (board.contains(cell)) tapCell(cell)
                    }
                )
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val s = zoom
            val cellPx = baseCell
            val left = size.width / 2f + panOff.x - gridW * cellPx * s / 2f
            val top  = size.height / 2f + panOff.y - gridH * cellPx * s / 2f
            val corner = CornerRadius(cellPx * 0.12f)
            val inset = max(1f, cellPx * 0.04f)
            val shakeDx = if (shakeProgress.value < 1f)
                sin(shakeProgress.value * Math.PI * 6).toFloat() * cellPx * 0.12f * (1f - shakeProgress.value) else 0f
            val waveT = waveProgress.value
            val waveSpan = max(1, waveOrder.size)

            translate(left, top) {
                scale(s, pivot = Offset.Zero) {
                    for (gc in cells) {
                        val cell = Pair(gc.x, gc.y)
                        val x = (gc.x - minX) * cellPx
                        val y = (gc.y - minY) * cellPx
                        val isSel      = selection?.cell == cell
                        val inWord     = cell in activeCells
                        val isAssigned = cell in assignedCells
                        val isSolved   = cell in solvedCells
                        val fill = when {
                            isSel      -> selFill
                            isAssigned -> assignFill
                            inWord     -> wordFill
                            isSolved   -> solvedFill
                            else       -> cellColor
                        }
                        // Solved-word wave: each cell bumps in turn.
                        val bump = waveOrder[cell]?.let { i ->
                            val t = (waveT * (1f + 0.5f)) - i.toFloat() / waveSpan * 0.5f
                            if (t in 0f..1f) sin(t * Math.PI).toFloat() * 0.18f else 0f
                        } ?: 0f
                        val dx = if (cell in shakeCells) shakeDx else 0f
                        val cx = x + cellPx / 2f + dx
                        val cy = y + cellPx / 2f
                        scale(1f + bump, pivot = Offset(cx, cy)) {
                            val tl = Offset(x + inset + dx, y + inset)
                            val sz = Size(cellPx - inset * 2, cellPx - inset * 2)
                            drawRoundRect(fill, tl, sz, corner)
                            if (cell in shakeCells && shakeProgress.value < 1f)
                                drawRoundRect(WrongRed.copy(alpha = 0.35f * (1f - shakeProgress.value)), tl, sz, corner)
                            drawRoundRect(if (isSel) accent else outline, tl, sz, corner,
                                style = Stroke(width = if (isSel) cellPx * 0.07f else max(1f, cellPx * 0.02f)))
                            // Hint marker: small corner triangle.
                            if (cell in revealed) {
                                val tri = Path().apply {
                                    moveTo(x + cellPx - inset - cellPx * 0.26f + dx, y + inset)
                                    lineTo(x + cellPx - inset + dx, y + inset)
                                    lineTo(x + cellPx - inset + dx, y + inset + cellPx * 0.26f)
                                    close()
                                }
                                drawPath(tri, AssignedAmber)
                            }
                            gc.number?.let { n ->
                                numberLayouts[n]?.let { l ->
                                    drawText(l, color = subInk, topLeft = Offset(x + inset + cellPx * 0.06f + dx, y + inset + cellPx * 0.02f))
                                }
                            }
                            inputs[cell]?.let { ch ->
                                letterLayouts[ch]?.let { l ->
                                    val color = when {
                                        isSolved         -> solvedInk
                                        cell in revealed -> if (light) Color(0xFF8D5A00) else AssignedAmber
                                        else             -> ink
                                    }
                                    drawText(l, color = color, topLeft = Offset(
                                        cx - l.size.width / 2f,
                                        cy - l.size.height / 2f + cellPx * 0.06f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun cellLuminanceOf(color: Color): Float = 0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue

/**
 * The current clue, with previous/next-unsolved arrows. Tapping the clue flips
 * direction where possible; the whole bar is one accessible element.
 */
@Composable
fun ClueBar(
    word:        PlacedWord?,
    inputs:      Map<Cell, Char>,
    accent:      Color,
    onPrev:      () -> Unit,
    onNext:      () -> Unit,
    onTapClue:   () -> Unit,
    modifier:    Modifier = Modifier,
    trailing:    (@Composable () -> Unit)? = null
) {
    val container = lerp(MaterialTheme.colorScheme.surfaceVariant, accent, 0.18f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(container)
            .heightIn(min = 56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous unsolved clue")
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClickLabel = "Switch direction", role = Role.Button, onClick = onTapClue)
                .padding(vertical = 6.dp, horizontal = 4.dp)
        ) {
            if (word == null) {
                Text("Tap a square to start", style = MaterialTheme.typography.bodyMedium)
            } else {
                val dir = if (word.isHorizontal) "ACROSS" else "DOWN"
                val pattern = word.word.indices.joinToString(" ") { i -> (inputs[word.cellAt(i)] ?: '_').toString() }
                Text("${word.number} $dir  ·  ${word.word.length} letters",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(word.clue, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (word.word.length <= 16) {
                    val filled = word.word.indices.count { inputs[word.cellAt(it)] != null }
                    // Screen readers read "underscore underscore A…"; give them the count.
                    Text(pattern, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                        modifier = Modifier.semantics { contentDescription = "$filled of ${word.word.length} letters filled" })
                }
            }
        }
        trailing?.invoke()
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next unsolved clue")
        }
    }
}

private val KEY_ROWS = listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")

/**
 * In-app letter keyboard: always A–Z, no autocorrect or suggestions, never
 * resizes the layout. Keys are ≥ 48dp tall. Disabled keys explain why via
 * [disabledReason].
 */
@Composable
fun CrosswordKeyboard(
    enabled:        Boolean,
    onKey:          (Char) -> Unit,
    onBackspace:    () -> Unit,
    modifier:       Modifier = Modifier,
    disabledReason: String? = null
) {
    Box(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer)) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KEY_ROWS.forEachIndexed { rowIdx, row ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = if (rowIdx == 1) 14.dp else 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row.forEach { ch ->
                        Key(ch.toString(), "Letter $ch", enabled, Modifier.weight(1f)) { onKey(ch) }
                    }
                    if (rowIdx == 2) {
                        Surface(
                            modifier = Modifier
                                .weight(1.6f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = enabled, onClickLabel = "Delete letter", role = Role.Button, onClick = onBackspace),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = if (enabled) 1f else 0.4f))
                            }
                        }
                    }
                }
            }
        }
        if (!enabled && disabledReason != null) {
            Box(
                Modifier.matchParentSize().background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f)),
                contentAlignment = Alignment.Center
            ) {
                Text(disabledReason, style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
private fun Key(label: String, description: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .semantics { contentDescription = description }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceBright,
        shadowElevation = 1.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.4f))
        }
    }
}
