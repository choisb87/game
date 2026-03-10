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

        // Background with subtle gradient
        drawRect(Color(0xFF0D1117), Offset.Zero, size)
        // Subtle top-to-bottom gradient overlay (city night sky feel)
        drawRect(Color(0xFF0A1628).copy(alpha = 0.4f), Offset.Zero, Size(w, h * 0.4f))
        drawRect(Color(0xFF1A0A0A).copy(alpha = 0.15f), Offset(0f, h * 0.7f), Size(w, h * 0.3f))

        // Animated grid lines (city grid feel) - brighter
        val gridSpacing = 50f
        val gridAlpha = 0.06f
        for (i in 0..(w / gridSpacing).toInt()) {
            val x = i * gridSpacing
            val drift = sin(time * 0.3f + i * 0.5f) * 3f
            val lineAlpha = gridAlpha * (0.5f + 0.5f * sin(time * 0.5f + i * 0.8f)).toFloat()
            drawLine(Color(0xFF39D353).copy(alpha = lineAlpha), Offset(x + drift, 0f), Offset(x + drift, h))
        }
        for (i in 0..(h / gridSpacing).toInt()) {
            val y = i * gridSpacing
            val drift = cos(time * 0.2f + i * 0.3f) * 3f
            val lineAlpha = gridAlpha * (0.5f + 0.5f * cos(time * 0.4f + i * 0.6f)).toFloat()
            drawLine(Color(0xFF39D353).copy(alpha = lineAlpha), Offset(0f, y + drift), Offset(w, y + drift))
        }

        // Animated patrol dots with vision cones - more visible
        val patrolDots = 8
        for (i in 0 until patrolDots) {
            val phase = time * 0.35f + i * 1.2f
            val px = ((sin(phase * 0.7f + i) * 0.4f + 0.5f) * w)
            val py = ((cos(phase * 0.5f + i * 0.3f) * 0.4f + 0.5f) * h)
            val angle = phase * 1.3f
            val coneRange = 110f

            val path = Path().apply {
                moveTo(px, py)
                val halfAngle = 0.45f
                for (step in 0..10) {
                    val a = angle - halfAngle + (halfAngle * 2f * step / 10f)
                    lineTo(px + cos(a) * coneRange, py + sin(a) * coneRange)
                }
                close()
            }
            drawPath(path, Color(0x0D3B82F6))
            // Cop body dot
            drawCircle(Color(0xFF3B82F6).copy(alpha = 0.25f), 6f, Offset(px, py))
            drawCircle(Color(0xFF3B82F6).copy(alpha = 0.1f), 12f, Offset(px, py))
        }

        // Floating gem particles in background
        for (i in 0..5) {
            val gx = (sin(time * 0.6f + i * 2f) * 0.3f + 0.15f + i * 0.13f) * w
            val gy = (cos(time * 0.4f + i * 1.7f) * 0.15f + 0.3f + i * 0.1f) * h
            val gemAlpha = (sin(time * 1.5f + i) * 0.15f + 0.15f).toFloat()
            val gs = 8f
            val gemPath = Path().apply {
                moveTo(gx, gy - gs)
                lineTo(gx + gs * 0.7f, gy)
                lineTo(gx, gy + gs)
                lineTo(gx - gs * 0.7f, gy)
                close()
            }
            drawPath(gemPath, Color(0xFFFBBF24).copy(alpha = gemAlpha))
        }

        // ===== TITLE AREA =====
        val titleY = h * 0.22f

        // Large title glow (multiple layers)
        drawCircle(Color(0xFF39D353).copy(alpha = 0.03f), 280f, Offset(w / 2f, titleY + 20f))
        drawCircle(Color(0xFF39D353).copy(alpha = 0.06f), 180f, Offset(w / 2f, titleY + 10f))
        drawCircle(Color(0xFF39D353).copy(alpha = 0.04f), 120f, Offset(w / 2f, titleY + 10f))

        // "GETAWAY" title text — larger, bolder
        titlePaint.apply {
            textSize = 72f
            color = Color(0xFF39D353).toArgb()
            textAlign = Paint.Align.CENTER
            alpha = 255
        }
        // Shadow text behind
        titlePaint.color = Color(0xFF1A6B2A).toArgb()
        nativeCanvas.drawText("GETAWAY", w / 2f + 2f, titleY + 2f, titlePaint)
        titlePaint.color = Color(0xFF39D353).toArgb()
        nativeCanvas.drawText("GETAWAY", w / 2f, titleY, titlePaint)

        // Subtitle with glow
        titlePaint.apply {
            textSize = 22f
            color = Color(0xFFFBBF24).toArgb()
            alpha = 220
            letterSpacing = 0.35f
        }
        nativeCanvas.drawText("N I G H T   H E I S T", w / 2f, titleY + 40f, titlePaint)
        titlePaint.letterSpacing = 0f

        // Decorative line under subtitle
        val lineW = w * 0.45f
        val lineY2 = titleY + 52f
        val lineAlpha = (sin(time * 1.5f) * 0.15f + 0.35f).toFloat()
        drawLine(Color(0xFFFBBF24).copy(alpha = lineAlpha), Offset(w / 2f - lineW / 2, lineY2), Offset(w / 2f + lineW / 2, lineY2), 1f)

        // Thief icon (between title and play button) — improved silhouette
        val iconY = titleY + 105f
        val iconRadius = 40f
        // Body glow
        val iconPulse = (sin(time * 1.8f) * 0.05f + 0.12f).toFloat()
        drawCircle(Color(0xFF39D353).copy(alpha = iconPulse), iconRadius * 2.2f, Offset(w / 2f, iconY))
        // Head
        drawCircle(Color(0xFF39D353), iconRadius, Offset(w / 2f, iconY))
        // Outline
        drawCircle(Color(0xFF2EAA43), iconRadius, Offset(w / 2f, iconY), style = Stroke(2f))
        // Mask band (wider, more defined)
        val maskH = 10f
        drawRect(Color(0xFF0D1117), Offset(w / 2f - iconRadius * 1.1f, iconY - maskH / 2 - 2f), Size(iconRadius * 2.2f, maskH))
        // Eyes (brighter, larger)
        drawCircle(Color.White, 6.5f, Offset(w / 2f - 12f, iconY - 2f))
        drawCircle(Color.White, 6.5f, Offset(w / 2f + 12f, iconY - 2f))
        // Pupils (looking sideways like sneaking)
        val pupilX = sin(time * 1.2f).toFloat() * 2f
        drawCircle(Color(0xFF0D1117), 2.5f, Offset(w / 2f - 12f + pupilX, iconY - 2f))
        drawCircle(Color(0xFF0D1117), 2.5f, Offset(w / 2f + 12f + pupilX, iconY - 2f))
        // Smirk (confident thief)
        drawArc(Color.White.copy(alpha = 0.7f), startAngle = 10f, sweepAngle = 60f, useCenter = false,
            topLeft = Offset(w / 2f - 10f, iconY + 6f), size = Size(20f, 12f), style = Stroke(2f))
        // Loot bag beside icon
        val bagX = w / 2f + iconRadius + 15f
        val bagY = iconY + 10f
        drawCircle(Color(0xFFA0855C), 14f, Offset(bagX, bagY))
        drawCircle(Color(0xFF8B7345), 14f, Offset(bagX, bagY), style = Stroke(1.5f))
        // Dollar sign on bag
        titlePaint.apply { textSize = 12f; color = Color(0xFFFBBF24).toArgb(); alpha = 200 }
        nativeCanvas.drawText("$", bagX, bagY + 4f, titlePaint)

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

        // Background panel
        drawRect(Color.White.copy(alpha = 0.02f), Offset(w * 0.1f, howY - 18f), Size(w * 0.8f, 65f))

        titlePaint.apply {
            textSize = 15f
            color = Color(0xFF667788).toArgb()
            alpha = 255
        }
        nativeCanvas.drawText("DRAG to move  •  Collect gems  •  Escape!", w / 2f, howY, titlePaint)

        // Feature indicators with colored dots
        val featY = howY + 28f
        val features = listOf(
            Pair("Power-ups", Color(0xFF8B5CF6)),
            Pair("Combos", Color(0xFFFF6B6B)),
            Pair("Stealth", Color(0xFF2563EB))
        )
        val totalFeatW = w * 0.7f
        val spacing = totalFeatW / features.size
        val startX = (w - totalFeatW) / 2f + spacing / 2f
        for ((idx, feat) in features.withIndex()) {
            val fx = startX + idx * spacing
            drawCircle(feat.second.copy(alpha = 0.6f), 4f, Offset(fx - 30f, featY - 4f))
            titlePaint.apply { textSize = 13f; color = feat.second.copy(alpha = 0.5f).toArgb() }
            nativeCanvas.drawText(feat.first, fx, featY, titlePaint)
        }

        // ===== VERSION/BRANDING =====
        titlePaint.apply {
            textSize = 13f
            color = Color(0xFF3A4550).toArgb()
        }
        nativeCanvas.drawText("v2.0  •  Premium Edition", w / 2f, h - 30f, titlePaint)
    }
}
