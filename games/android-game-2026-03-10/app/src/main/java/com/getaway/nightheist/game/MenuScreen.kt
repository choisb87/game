package com.getaway.nightheist.game

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.*

@Composable
fun MenuScreen(
    onStartGame: () -> Unit,
    context: Context
) {
    val bestScore = remember { loadBestScore(context) }
    val levelsCleared = remember { loadLevelsCleared(context) }
    var time by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val startTime = System.nanoTime()
        while (true) {
            kotlinx.coroutines.android.awaitFrame()
            time = ((System.nanoTime() - startTime) / 1_000_000_000f)
        }
    }

    val titlePaint = remember {
        Paint().apply {
            isAntiAlias = true
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { onStartGame() }
            }
    ) {
        val w = size.width
        val h = size.height
        val nativeCanvas = drawContext.canvas.nativeCanvas

        // Background
        drawRect(Color(0xFF0D1117), Offset.Zero, size)

        // Animated grid lines (city grid feel)
        val gridSpacing = 60f
        val gridAlpha = 0.04f
        for (i in 0..(w / gridSpacing).toInt()) {
            val x = i * gridSpacing
            val drift = sin(time * 0.3f + i * 0.5f) * 2f
            drawLine(Color.White.copy(alpha = gridAlpha), Offset(x + drift, 0f), Offset(x + drift, h))
        }
        for (i in 0..(h / gridSpacing).toInt()) {
            val y = i * gridSpacing
            val drift = cos(time * 0.2f + i * 0.3f) * 2f
            drawLine(Color.White.copy(alpha = gridAlpha), Offset(0f, y + drift), Offset(w, y + drift))
        }

        // Animated patrol dots with vision cones
        val patrolDots = 6
        for (i in 0 until patrolDots) {
            val phase = time * 0.4f + i * 1.5f
            val px = ((sin(phase * 0.7f) * 0.35f + 0.5f) * w)
            val py = ((cos(phase * 0.5f) * 0.35f + 0.5f) * h)
            val angle = phase * 1.3f
            val coneRange = 90f

            val path = Path().apply {
                moveTo(px, py)
                val halfAngle = 0.5f
                for (step in 0..8) {
                    val a = angle - halfAngle + (halfAngle * 2f * step / 8f)
                    lineTo(px + cos(a) * coneRange, py + sin(a) * coneRange)
                }
                close()
            }
            drawPath(path, Color(0x0A3B82F6))
            drawCircle(Color(0xFF3B82F6).copy(alpha = 0.3f), 5f, Offset(px, py))
        }

        // ===== TITLE AREA =====
        val titleY = h * 0.22f

        // Title glow
        drawCircle(Color(0xFF39D353).copy(alpha = 0.06f), 180f, Offset(w / 2f, titleY + 10f))

        // "GETAWAY" title text
        titlePaint.apply {
            textSize = 64f
            color = Color(0xFF39D353).toArgb()
            textAlign = Paint.Align.CENTER
            alpha = 255
        }
        nativeCanvas.drawText("GETAWAY", w / 2f, titleY, titlePaint)

        // Subtitle
        titlePaint.apply {
            textSize = 22f
            color = Color(0xFFFBBF24).toArgb()
            alpha = 200
            letterSpacing = 0.3f
        }
        nativeCanvas.drawText("N I G H T   H E I S T", w / 2f, titleY + 35f, titlePaint)
        titlePaint.letterSpacing = 0f

        // Thief icon (between title and play button)
        val iconY = titleY + 90f
        val iconRadius = 35f
        drawCircle(Color(0xFF39D353), iconRadius, Offset(w / 2f, iconY))
        drawCircle(Color(0xFF0D1117), iconRadius * 0.45f, Offset(w / 2f, iconY + iconRadius * 0.18f))
        drawCircle(Color.White, 5f, Offset(w / 2f - 10f, iconY - 4f))
        drawCircle(Color.White, 5f, Offset(w / 2f + 10f, iconY - 4f))
        // Mask band
        drawRect(Color(0xFF0D1117), Offset(w / 2f - iconRadius, iconY - 10f), Size(iconRadius * 2, 6f))

        // ===== PLAY BUTTON =====
        val playY = h * 0.52f
        val pulseSize = 40f + sin(time * 2f) * 6f

        // Glow rings
        drawCircle(Color(0xFF39D353).copy(alpha = 0.05f), pulseSize + 40f, Offset(w / 2f, playY))
        drawCircle(Color(0xFF39D353).copy(alpha = 0.1f), pulseSize + 20f, Offset(w / 2f, playY))
        // Outer ring
        drawCircle(Color(0xFF39D353), pulseSize, Offset(w / 2f, playY), style = Stroke(3f))
        // Play triangle
        val triSize = 20f
        val triPath = Path().apply {
            moveTo(w / 2f - triSize * 0.35f, playY - triSize * 0.5f)
            lineTo(w / 2f + triSize * 0.65f, playY)
            lineTo(w / 2f - triSize * 0.35f, playY + triSize * 0.5f)
            close()
        }
        drawPath(triPath, Color(0xFF39D353))

        // "TAP TO PLAY" text
        val blinkAlpha = (sin(time * 2.5f) * 0.3f + 0.6f).toFloat()
        titlePaint.apply {
            textSize = 20f
            color = Color.White.toArgb()
            alpha = (blinkAlpha * 255).toInt()
        }
        nativeCanvas.drawText("TAP TO PLAY", w / 2f, playY + pulseSize + 30f, titlePaint)

        // ===== STATS AREA =====
        val statsY = h * 0.68f

        if (bestScore > 0 || levelsCleared > 0) {
            // Stats background
            drawRect(Color.White.copy(alpha = 0.03f), Offset(w * 0.15f, statsY - 15f), Size(w * 0.7f, 85f))

            titlePaint.apply {
                textSize = 16f
                color = Color(0xFF666666).toArgb()
                alpha = 255
            }
            nativeCanvas.drawText("— RECORDS —", w / 2f, statsY + 5f, titlePaint)

            if (bestScore > 0) {
                // Crown + best score
                drawCircle(Color(0xFFFBBF24).copy(alpha = 0.4f), 8f, Offset(w / 2f - 60f, statsY + 30f))
                titlePaint.apply {
                    textSize = 24f
                    color = Color(0xFFFBBF24).toArgb()
                    alpha = 220
                }
                nativeCanvas.drawText("$bestScore", w / 2f, statsY + 38f, titlePaint)
            }

            if (levelsCleared > 0) {
                titlePaint.apply {
                    textSize = 16f
                    color = Color(0xFF39D353).toArgb()
                    alpha = 180
                }
                nativeCanvas.drawText("$levelsCleared levels cleared", w / 2f, statsY + 62f, titlePaint)
            }
        }

        // ===== HOW TO PLAY =====
        val howY = h * 0.82f

        titlePaint.apply {
            textSize = 14f
            color = Color(0xFF555555).toArgb()
            alpha = 255
        }
        nativeCanvas.drawText("DRAG to move  •  Collect loot  •  Escape!", w / 2f, howY, titlePaint)

        // Feature indicators
        val featY = howY + 25f
        titlePaint.apply { textSize = 12f; color = Color(0xFF444444).toArgb() }
        nativeCanvas.drawText("Power-ups  •  Combo system  •  Stealth bonuses", w / 2f, featY, titlePaint)

        // ===== VERSION/BRANDING =====
        titlePaint.apply {
            textSize = 12f
            color = Color(0xFF333333).toArgb()
        }
        nativeCanvas.drawText("v1.0  •  Premium Edition", w / 2f, h - 30f, titlePaint)
    }
}
