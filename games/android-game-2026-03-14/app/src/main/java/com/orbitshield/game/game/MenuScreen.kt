package com.orbitshield.game.game

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import kotlin.math.*
import kotlin.random.Random

@Composable
fun MenuScreen(onStart: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("orbit_shield", Context.MODE_PRIVATE) }
    val bestScore = remember { prefs.getInt("best_score", 0) }
    var time by remember { mutableStateOf(0f) }
    var lastFrameTime by remember { mutableStateOf(0L) }

    val stars = remember {
        (0 until 80).map {
            Star(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                brightness = Random.nextFloat() * 0.6f + 0.4f,
                twinkleSpeed = Random.nextFloat() * 2f + 1f,
                size = Random.nextFloat() * 2f + 0.5f
            )
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
        val cx = w / 2f
        val cy = h * 0.42f

        // Background
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1A1A3E), Color(0xFF0D1B2E), Color(0xFF0A0E1A)),
                center = Offset(cx, cy),
                radius = maxOf(w, h) * 0.7f
            ),
            size = size
        )

        // Stars
        for (star in stars) {
            val twinkle = (sin(time * star.twinkleSpeed) * 0.3f + 0.7f) * star.brightness
            drawCircle(
                color = Color.White.copy(alpha = twinkle),
                radius = star.size,
                center = Offset(star.x * w, star.y * h)
            )
        }

        // Decorative orbit ring
        val orbitR = minOf(w, h) * 0.22f
        drawCircle(
            color = Color(0xFF00E5FF).copy(alpha = 0.08f),
            center = Offset(cx, cy),
            radius = orbitR,
            style = Stroke(width = 1.5f)
        )

        // Animated shield arc
        val shieldAngle = time * 2f
        val arcDeg = 60f
        val startDeg = Math.toDegrees(shieldAngle.toDouble()).toFloat() - arcDeg / 2f

        drawArc(
            color = Color(0xFF00E5FF).copy(alpha = 0.5f),
            startAngle = startDeg,
            sweepAngle = arcDeg,
            useCenter = false,
            topLeft = Offset(cx - orbitR - 6f, cy - orbitR - 6f),
            size = Size((orbitR + 6f) * 2, (orbitR + 6f) * 2),
            style = Stroke(width = 12f, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0xFF00E5FF),
            startAngle = startDeg,
            sweepAngle = arcDeg,
            useCenter = false,
            topLeft = Offset(cx - orbitR, cy - orbitR),
            size = Size(orbitR * 2, orbitR * 2),
            style = Stroke(width = 6f, cap = StrokeCap.Round)
        )

        // Planet
        val planetR = minOf(w, h) * 0.06f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFB388FF).copy(alpha = 0.2f), Color.Transparent),
                center = Offset(cx, cy),
                radius = planetR * 3f
            ),
            center = Offset(cx, cy),
            radius = planetR * 3f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFB388FF), Color(0xFF7C4DFF), Color(0xFF6200EA)),
                center = Offset(cx - planetR * 0.3f, cy - planetR * 0.3f),
                radius = planetR * 1.5f
            ),
            center = Offset(cx, cy),
            radius = planetR
        )

        // Demo asteroids floating
        for (i in 0 until 5) {
            val a = time * 0.3f + i * 1.3f
            val dist = orbitR * 1.6f + sin(a * 0.7f) * 30f
            val ax = cx + cos(a) * dist
            val ay = cy + sin(a) * dist
            val aSize = 8f + i * 2f
            val aColor = Color(0xFF90A4AE).copy(alpha = 0.5f)

            val path = Path()
            for (j in 0 until 6) {
                val pa = (j.toFloat() / 6f) * 2 * PI.toFloat() + time
                val jag = if (j % 2 == 0) aSize else aSize * 0.7f
                val px = ax + cos(pa) * jag
                val py = ay + sin(pa) * jag
                if (j == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            drawPath(path, color = aColor)
        }

        // Title
        drawContext.canvas.nativeCanvas.apply {
            val titlePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 72f
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
                isAntiAlias = true
                setShadowLayer(8f, 0f, 4f, android.graphics.Color.argb(150, 0, 229, 255))
            }
            drawText("ORBIT", cx, h * 0.12f, titlePaint)
            drawText("SHIELD", cx, h * 0.12f + 80f, titlePaint)

            // Subtitle
            val subPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(200, 0, 229, 255)
                textSize = 28f
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            drawText("궤도 방패", cx, h * 0.12f + 120f, subPaint)

            // Best score
            if (bestScore > 0) {
                val bestPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(200, 255, 215, 64)
                    textSize = 30f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                drawText("BEST: $bestScore", cx, h * 0.65f, bestPaint)
            }

            // Instructions
            val instrPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(160, 255, 255, 255)
                textSize = 24f
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            drawText("탭하여 방패 방향 전환", cx, h * 0.72f, instrPaint)
            drawText("소행성으로부터 행성을 지켜라!", cx, h * 0.76f, instrPaint)

            // Tap to start
            val startAlpha = ((sin(time * 3f) * 0.3f + 0.7f) * 255).toInt().coerceIn(0, 255)
            val startPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(startAlpha, 255, 255, 255)
                textSize = 38f
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
                isAntiAlias = true
            }
            drawText("TAP TO START", cx, h * 0.88f, startPaint)
        }

        // Update time with actual frame delta
        val currentTime = System.nanoTime()
        if (lastFrameTime == 0L) {
            lastFrameTime = currentTime
        }
        val dt = ((currentTime - lastFrameTime) / 1_000_000_000f).coerceAtMost(0.033f)
        lastFrameTime = currentTime
        time += dt
    }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { }
        }
    }
}
