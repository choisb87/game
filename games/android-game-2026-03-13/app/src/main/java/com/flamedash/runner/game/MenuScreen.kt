package com.flamedash.runner.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlin.math.sin

@Composable
fun MenuScreen(onStartGame: () -> Unit) {
    val textMeasurer = rememberTextMeasurer()
    var time by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastFrame = 0L
        while (true) {
            withFrameNanos { frameTime ->
                if (lastFrame == 0L) { lastFrame = frameTime; return@withFrameNanos }
                time += ((frameTime - lastFrame) / 1_000_000_000f).coerceAtMost(0.033f)
                lastFrame = frameTime
            }
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
        drawRect(color = Color(0xFF0D1B2A), size = Size(w, h))

        // Animated lava at bottom
        val lavaTop = h * 0.75f
        val lavePath = Path().apply {
            moveTo(0f, lavaTop)
            var x = 0f
            while (x <= w) {
                val wy = sin(x * 0.015f + time * 3f) * 12f +
                    sin(x * 0.025f + time * 2f) * 8f
                lineTo(x, lavaTop + wy)
                x += 4f
            }
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(lavePath, brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFF6B35), Color(0xFFFF1744), Color(0xFF8B0000)),
            startY = lavaTop,
            endY = h
        ))

        // Floating embers
        for (i in 0 until 15) {
            val ex = ((i * 137 + 42) % 1000) / 1000f * w
            val baseY = h * 0.8f - (time * (20f + i * 5f)) % (h * 0.6f)
            val wobble = sin(time * 2f + i.toFloat()) * 10f
            val alpha = 0.3f + 0.4f * sin(time * 1.5f + i * 0.5f).toFloat()

            drawCircle(
                color = Color(0xFFFF9100).copy(alpha = alpha.coerceIn(0.1f, 0.8f)),
                radius = 3f + (i % 3),
                center = Offset(ex + wobble, baseY)
            )
        }

        // Title: "FLAME"
        val flameStyle = TextStyle(
            color = Color(0xFFFF6B35),
            fontSize = 56.sp,
            fontWeight = FontWeight.ExtraBold
        )
        val flameLayout = textMeasurer.measure("FLAME", flameStyle)
        val titleY = h * 0.2f + sin(time * 1.5f) * 8f

        // Title glow
        val glowStyle = TextStyle(
            color = Color(0xFFFF6B35).copy(alpha = 0.3f),
            fontSize = 58.sp,
            fontWeight = FontWeight.ExtraBold
        )
        val glowLayout = textMeasurer.measure("FLAME", glowStyle)
        drawText(glowLayout, topLeft = Offset(
            w / 2f - glowLayout.size.width / 2f - 1f, titleY - 1f
        ))

        drawText(flameLayout, topLeft = Offset(
            w / 2f - flameLayout.size.width / 2f, titleY
        ))

        // Title: "DASH"
        val dashStyle = TextStyle(
            color = Color(0xFFFFD740),
            fontSize = 56.sp,
            fontWeight = FontWeight.ExtraBold
        )
        val dashLayout = textMeasurer.measure("DASH", dashStyle)
        drawText(dashLayout, topLeft = Offset(
            w / 2f - dashLayout.size.width / 2f, titleY + 60f
        ))

        // Subtitle
        val subStyle = TextStyle(
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        val subLayout = textMeasurer.measure("불꽃 질주", subStyle)
        drawText(subLayout, topLeft = Offset(
            w / 2f - subLayout.size.width / 2f, titleY + 130f
        ))

        // Flame character preview
        val charX = w / 2f
        val charY = h * 0.48f + sin(time * 2f) * 10f
        val charSize = 40f

        // Glow
        drawCircle(
            color = Color(0xFFFF6B35).copy(alpha = 0.2f),
            radius = charSize * 1.5f,
            center = Offset(charX, charY)
        )

        // Flame body
        val bodyPath = Path().apply {
            moveTo(charX, charY - charSize * 0.7f)
            cubicTo(
                charX + charSize * 0.6f, charY - charSize * 0.3f,
                charX + charSize * 0.5f, charY + charSize * 0.3f,
                charX, charY + charSize * 0.5f
            )
            cubicTo(
                charX - charSize * 0.5f, charY + charSize * 0.3f,
                charX - charSize * 0.6f, charY - charSize * 0.3f,
                charX, charY - charSize * 0.7f
            )
        }
        drawPath(bodyPath, color = Color(0xFFFF6B35))

        val innerPath = Path().apply {
            moveTo(charX, charY - charSize * 0.4f)
            cubicTo(
                charX + charSize * 0.3f, charY - charSize * 0.15f,
                charX + charSize * 0.25f, charY + charSize * 0.2f,
                charX, charY + charSize * 0.3f
            )
            cubicTo(
                charX - charSize * 0.25f, charY + charSize * 0.2f,
                charX - charSize * 0.3f, charY - charSize * 0.15f,
                charX, charY - charSize * 0.4f
            )
        }
        drawPath(innerPath, color = Color(0xFFFFD740))

        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = charSize * 0.15f,
            center = Offset(charX, charY - charSize * 0.1f)
        )

        // Eyes
        drawCircle(color = Color(0xFF1A1A2E), radius = 3.5f,
            center = Offset(charX + 6f, charY - charSize * 0.2f))
        drawCircle(color = Color(0xFF1A1A2E), radius = 3.5f,
            center = Offset(charX + 14f, charY - charSize * 0.2f))

        // How to play
        val howStyle = TextStyle(
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp
        )
        val how1 = textMeasurer.measure("← 왼쪽 탭: 왼쪽 대시", howStyle)
        val how2 = textMeasurer.measure("오른쪽 탭: 오른쪽 대시 →", howStyle)
        val how3 = textMeasurer.measure("화염을 피해 최대한 높이 올라가세요!", howStyle)

        drawText(how1, topLeft = Offset(w / 2f - how1.size.width / 2f, h * 0.58f))
        drawText(how2, topLeft = Offset(w / 2f - how2.size.width / 2f, h * 0.58f + 22f))
        drawText(how3, topLeft = Offset(w / 2f - how3.size.width / 2f, h * 0.58f + 50f))

        // Tap to start
        val tapAlpha = (sin(time * 3f) * 0.3f + 0.7f).toFloat()
        val tapStyle = TextStyle(
            color = Color(0xFFFFD740).copy(alpha = tapAlpha),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        val tapLayout = textMeasurer.measure("TAP TO START", tapStyle)
        drawText(tapLayout, topLeft = Offset(
            w / 2f - tapLayout.size.width / 2f, h * 0.68f
        ))
    }
}
