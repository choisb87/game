package com.getaway.nightheist.game

import android.content.Context
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
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.*
import kotlin.random.Random

@Composable
fun MenuScreen(
    onStartGame: () -> Unit,
    context: Context
) {
    val bestScore = remember { loadBestScore(context) }
    var time by remember { mutableFloatStateOf(0f) }

    // Animate
    LaunchedEffect(Unit) {
        val startTime = System.nanoTime()
        while (true) {
            kotlinx.coroutines.android.awaitFrame()
            time = ((System.nanoTime() - startTime) / 1_000_000_000f)
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

        // Background
        drawRect(Color(0xFF0D1117), Offset.Zero, size)

        // Animated grid lines (city feel)
        val gridSpacing = 60f
        val gridAlpha = 0.06f
        for (i in 0..(w / gridSpacing).toInt()) {
            val x = i * gridSpacing
            drawLine(Color.White.copy(alpha = gridAlpha), Offset(x, 0f), Offset(x, h))
        }
        for (i in 0..(h / gridSpacing).toInt()) {
            val y = i * gridSpacing
            drawLine(Color.White.copy(alpha = gridAlpha), Offset(0f, y), Offset(w, y))
        }

        // Animated "patrol" dots moving on grid
        val patrolDots = 5
        for (i in 0 until patrolDots) {
            val phase = time * 0.5f + i * 1.3f
            val px = ((sin(phase * 0.7f) * 0.4f + 0.5f) * w)
            val py = ((cos(phase * 0.5f) * 0.4f + 0.5f) * h)
            // Vision cone
            val angle = phase * 1.5f
            val coneRange = 80f
            val path = Path().apply {
                moveTo(px, py)
                val halfAngle = 0.5f
                lineTo(px + cos(angle - halfAngle) * coneRange, py + sin(angle - halfAngle) * coneRange)
                lineTo(px + cos(angle + halfAngle) * coneRange, py + sin(angle + halfAngle) * coneRange)
                close()
            }
            drawPath(path, Color(0x103B82F6))
            drawCircle(Color(0xFF3B82F6).copy(alpha = 0.4f), 6f, Offset(px, py))
        }

        // Title area - "GETAWAY" represented as geometric shapes
        val titleY = h * 0.3f
        val titleCenterX = w / 2f

        // Large thief icon (circle with mask-like features)
        val iconRadius = 45f
        drawCircle(Color(0xFF39D353), iconRadius, Offset(titleCenterX, titleY))
        drawCircle(Color(0xFF0D1117), iconRadius * 0.5f, Offset(titleCenterX, titleY + iconRadius * 0.15f))
        // Eyes
        drawCircle(Color.White, 6f, Offset(titleCenterX - 12f, titleY - 5f))
        drawCircle(Color.White, 6f, Offset(titleCenterX + 12f, titleY - 5f))

        // Subtitle dots pattern (represents "NIGHT HEIST")
        val dotPattern = listOf(5, 3, 4, 3, 2, 5, 3, 4, 2, 2) // visual rhythm
        val totalDots = dotPattern.sum()
        val dotStartX = titleCenterX - (totalDots * 4f)
        var dotX = dotStartX
        for (group in dotPattern) {
            for (j in 0 until group) {
                drawCircle(Color(0xFFFBBF24).copy(alpha = 0.7f), 2.5f, Offset(dotX, titleY + 70f))
                dotX += 8f
            }
            dotX += 12f // gap between groups
        }

        // Pulsing "play" circle
        val playY = h * 0.55f
        val pulseSize = 35f + sin(time * 2f) * 5f
        drawCircle(Color(0xFF39D353).copy(alpha = 0.2f), pulseSize + 15f, Offset(titleCenterX, playY))
        drawCircle(Color(0xFF39D353), pulseSize, Offset(titleCenterX, playY), style = Stroke(3f))
        // Play triangle
        val triSize = 18f
        val triPath = Path().apply {
            moveTo(titleCenterX - triSize * 0.4f, playY - triSize * 0.5f)
            lineTo(titleCenterX + triSize * 0.6f, playY)
            lineTo(titleCenterX - triSize * 0.4f, playY + triSize * 0.5f)
            close()
        }
        drawPath(triPath, Color(0xFF39D353))

        // Best score (as dots)
        if (bestScore > 0) {
            val scoreY = h * 0.68f
            val scoreDots = (bestScore / 100).coerceAtMost(30)
            val scoreStartX = titleCenterX - scoreDots * 5f
            for (i in 0 until scoreDots) {
                drawCircle(Color(0xFFFBBF24).copy(alpha = 0.5f), 3f, Offset(scoreStartX + i * 10f, scoreY))
            }
            // Crown icon above score
            drawCircle(Color(0xFFFBBF24).copy(alpha = 0.3f), 10f, Offset(titleCenterX, scoreY - 20f))
        }

        // Bottom info dots (tap to start hint)
        val hintY = h * 0.85f
        val blinkAlpha = (sin(time * 3f) * 0.3f + 0.5f).toFloat()
        for (i in 0 until 3) {
            drawCircle(
                Color.White.copy(alpha = blinkAlpha),
                4f,
                Offset(titleCenterX - 15f + i * 15f, hintY)
            )
        }
    }
}
