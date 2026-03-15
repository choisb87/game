package com.chainreactor.game.game

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import kotlin.math.*
import kotlin.random.Random

@Composable
fun MenuScreen(onStart: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("chain_reactor", Context.MODE_PRIVATE) }
    val bestScore = remember { prefs.getInt("best_score", 0) }
    var time by remember { mutableFloatStateOf(0f) }

    // Demo orbs for decoration
    val demoOrbs = remember {
        List(15) {
            DemoOrb(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                radius = Random.nextFloat() * 12f + 8f,
                color = listOf(
                    Color(0xFF00E5FF),
                    Color(0xFFFF00E5),
                    Color(0xFFFF6D00),
                    Color(0xFF00E676),
                    Color(0xFFFFD740),
                    Color(0xFF2979FF)
                ).random(),
                speed = Random.nextFloat() * 0.015f + 0.005f,
                phase = Random.nextFloat() * PI.toFloat() * 2f
            )
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { nanos ->
                time += 0.016f
            }
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { onStart() }
            }
    ) {
        val w = size.width
        val h = size.height

        // Background
        val bgBrush = Brush.radialGradient(
            colors = listOf(Color(0xFF0B1535), Color(0xFF050A18)),
            center = Offset(w / 2f, h / 2f),
            radius = h * 0.7f
        )
        drawRect(brush = bgBrush)

        // Background stars
        val starCount = 60
        for (i in 0 until starCount) {
            val sx = (i * 137.508f) % w
            val sy = (i * 223.71f) % h
            val twinkle = (sin(time * (1f + i % 3) + i * 0.5f) + 1f) / 2f
            drawCircle(
                color = Color.White.copy(alpha = 0.2f + twinkle * 0.5f),
                radius = 1f + (i % 3) * 0.5f,
                center = Offset(sx, sy)
            )
        }

        // Demo orbs floating
        for (orb in demoOrbs) {
            val ox = orb.x * w + sin(time * orb.speed * 60f + orb.phase) * 30f
            val oy = orb.y * h + cos(time * orb.speed * 40f + orb.phase) * 20f

            // Glow
            drawCircle(
                color = orb.color.copy(alpha = 0.12f),
                radius = orb.radius * 2.5f,
                center = Offset(ox, oy)
            )
            // Body
            val gradient = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.8f), orb.color, orb.color.copy(alpha = 0.4f)),
                center = Offset(ox - orb.radius * 0.3f, oy - orb.radius * 0.3f),
                radius = orb.radius * 1.2f
            )
            drawCircle(brush = gradient, radius = orb.radius, center = Offset(ox, oy))
        }

        // Demo explosion ring (pulsing)
        val ringRadius = 60f + sin(time * 2f) * 15f
        val ringAlpha = 0.3f + sin(time * 2f) * 0.1f
        drawCircle(
            color = Color(0xFFFFAB40).copy(alpha = ringAlpha),
            radius = ringRadius,
            center = Offset(w / 2f, h * 0.38f),
            style = Stroke(width = 3f)
        )
        drawCircle(
            color = Color(0xFFFFAB40).copy(alpha = ringAlpha * 0.3f),
            radius = ringRadius * 0.6f,
            center = Offset(w / 2f, h * 0.38f)
        )

        // Title
        drawContext.canvas.nativeCanvas.apply {
            val titlePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(255, 255, 215, 64)
                textSize = 72f
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
                setShadowLayer(10f, 0f, 4f, android.graphics.Color.argb(180, 255, 145, 0))
            }
            drawText("CHAIN", w / 2f, h * 0.18f, titlePaint)
            drawText("REACTOR", w / 2f, h * 0.24f, titlePaint)

            // Subtitle
            val subPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(200, 200, 200, 200)
                textSize = 26f
                textAlign = android.graphics.Paint.Align.CENTER
                setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
            }
            drawText("연쇄 폭발 퍼즐 아케이드", w / 2f, h * 0.29f, subPaint)

            // Instructions
            val instrPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(220, 255, 255, 255)
                textSize = 28f
                textAlign = android.graphics.Paint.Align.CENTER
                setShadowLayer(3f, 0f, 1f, android.graphics.Color.BLACK)
            }

            val startY = h * 0.50f
            val lineHeight = 44f

            drawText("🎯 화면을 탭하여 폭발을 일으키세요", w / 2f, startY, instrPaint)
            drawText("💥 오브에 폭발이 닿으면 연쇄 반응!", w / 2f, startY + lineHeight, instrPaint)
            drawText("⚡ 제한된 스파크로 최대 체인 달성", w / 2f, startY + lineHeight * 2f, instrPaint)
            drawText("🟣 메가 오브 = 거대 폭발", w / 2f, startY + lineHeight * 3f, instrPaint)
            drawText("🔵 스플리터 = 파편 발사", w / 2f, startY + lineHeight * 3.9f, instrPaint)
            drawText("🟡 골든 오브 = 3배 점수", w / 2f, startY + lineHeight * 4.8f, instrPaint)

            // Best score
            if (bestScore > 0) {
                val bestPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(255, 255, 215, 64)
                    textSize = 34f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                    setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
                }
                drawText("최고 기록: $bestScore", w / 2f, h * 0.82f, bestPaint)
            }

            // Tap to start
            val pulse = (sin(time.toDouble() * 3.0) * 0.3 + 0.7).toFloat()
            val tapPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(
                    (pulse * 255).toInt(), 255, 255, 255
                )
                textSize = 38f
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
                setShadowLayer(6f, 0f, 2f, android.graphics.Color.BLACK)
            }
            drawText("TAP TO START", w / 2f, h * 0.90f, tapPaint)
        }
    }
}

private data class DemoOrb(
    val x: Float,
    val y: Float,
    val radius: Float,
    val color: Color,
    val speed: Float,
    val phase: Float
)
