package com.voidrunner.gravity.game

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
fun MenuScreen(onStartGame: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("void_runner", Context.MODE_PRIVATE) }
    val textMeasurer = rememberTextMeasurer()
    val bestScore = remember { prefs.getInt("best_score", 0) }
    val bestDist = remember { prefs.getFloat("best_distance", 0f) }

    var time by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            time += (now - last) / 1_000_000_000f
            last = now
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
        drawRect(Color(0xFF050510))

        // Animated grid
        val gridSpacing = 40f
        val gridOffset = (time * 20f) % gridSpacing
        val gridAlpha = 0.06f
        var gx = -gridOffset
        while (gx < w) {
            drawLine(Color(0xFF00E5FF).copy(alpha = gridAlpha), Offset(gx, 0f), Offset(gx, h), 0.5f)
            gx += gridSpacing
        }
        var gy = -gridOffset
        while (gy < h) {
            drawLine(Color(0xFF00E5FF).copy(alpha = gridAlpha), Offset(0f, gy), Offset(w, gy), 0.5f)
            gy += gridSpacing
        }

        // Floating orbs
        for (i in 0 until 5) {
            val ox = w * (0.2f + 0.15f * i) + sin(time * 0.8f + i * 1.3f).toFloat() * 30f
            val oy = h * (0.3f + 0.1f * i) + cos(time * 0.6f + i * 0.9f).toFloat() * 40f
            val color = if (i % 2 == 0) Color(0xFF00E5FF) else Color(0xFFFF006E)
            drawCircle(color.copy(alpha = 0.08f), 60f + i * 10f, Offset(ox, oy))
            drawCircle(color.copy(alpha = 0.15f), 20f + i * 5f, Offset(ox, oy))
        }

        // Diamond player icon (large, animated)
        val iconX = w / 2f
        val iconY = h * 0.28f
        val iconSize = 40f + sin(time * 2f).toFloat() * 5f
        val iconPath = Path().apply {
            moveTo(iconX + iconSize, iconY)
            lineTo(iconX, iconY - iconSize * 0.8f)
            lineTo(iconX - iconSize * 0.6f, iconY)
            lineTo(iconX, iconY + iconSize * 0.8f)
            close()
        }
        drawCircle(Color(0xFF00E5FF).copy(alpha = 0.1f), iconSize * 3f, Offset(iconX, iconY))
        drawPath(iconPath, Color(0xFF00E5FF))
        drawPath(iconPath, Color.White.copy(alpha = 0.3f), style = Stroke(2f))

        // Speed lines
        for (i in 0 until 3) {
            val ly = iconY + (i - 1) * 15f
            val lx = iconX - iconSize - 15f - i * 8f
            val lLen = 25f + sin(time * 4f + i.toFloat()).toFloat() * 10f
            drawLine(
                Color(0xFF00E5FF).copy(alpha = 0.4f),
                Offset(lx - lLen, ly), Offset(lx, ly), strokeWidth = 2f
            )
        }

        // Title
        val title = "VOID RUNNER"
        val titleStyle = TextStyle(
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF00E5FF)
        )
        val titleResult = textMeasurer.measure(title, titleStyle)
        // Glow
        drawRect(
            Brush.radialGradient(
                listOf(Color(0xFF00E5FF).copy(alpha = 0.08f), Color.Transparent),
                center = Offset(w / 2f, h * 0.43f),
                radius = 200f
            ),
            Offset(w / 2f - 200f, h * 0.41f - 30f),
            Size(400f, 80f)
        )
        drawText(titleResult, topLeft = Offset(w / 2f - titleResult.size.width / 2f, h * 0.4f))

        // Subtitle
        val sub = "중력의 끝"
        val subStyle = TextStyle(
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFFFF006E)
        )
        val subResult = textMeasurer.measure(sub, subStyle)
        drawText(subResult, topLeft = Offset(w / 2f - subResult.size.width / 2f, h * 0.46f))

        // Tap to play (pulsing)
        val tapAlpha = (sin(time * 3.0) * 0.35 + 0.65).toFloat()
        val tapText = "TAP TO PLAY"
        val tapStyle = TextStyle(
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.White.copy(alpha = tapAlpha)
        )
        val tapResult = textMeasurer.measure(tapText, tapStyle)
        drawText(tapResult, topLeft = Offset(w / 2f - tapResult.size.width / 2f, h * 0.58f))

        // Best score
        if (bestScore > 0) {
            val bText = "BEST SCORE: $bestScore"
            val bStyle = TextStyle(
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFFFD700).copy(alpha = 0.7f)
            )
            val bResult = textMeasurer.measure(bText, bStyle)
            drawText(bResult, topLeft = Offset(w / 2f - bResult.size.width / 2f, h * 0.65f))

            val dText = "BEST DISTANCE: ${bestDist.toInt()}m"
            val dStyle = TextStyle(
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White.copy(alpha = 0.4f)
            )
            val dResult = textMeasurer.measure(dText, dStyle)
            drawText(dResult, topLeft = Offset(w / 2f - dResult.size.width / 2f, h * 0.69f))
        }

        // How to play
        val howTitle = "HOW TO PLAY"
        val howStyle = TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF00E5FF).copy(alpha = 0.6f)
        )
        val howResult = textMeasurer.measure(howTitle, howStyle)
        drawText(howResult, topLeft = Offset(w / 2f - howResult.size.width / 2f, h * 0.78f))

        val instructions = listOf(
            "화면을 탭하여 중력을 반전시키세요",
            "크리스탈을 수집하고 장애물을 피하세요",
            "콤보로 높은 점수를 노리세요!"
        )
        instructions.forEachIndexed { i, text ->
            val iStyle = TextStyle(
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White.copy(alpha = 0.35f)
            )
            val iResult = textMeasurer.measure(text, iStyle)
            drawText(iResult, topLeft = Offset(w / 2f - iResult.size.width / 2f, h * 0.82f + i * 22f))
        }

        // Version
        val verText = "v1.0.0"
        val verStyle = TextStyle(
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = Color.White.copy(alpha = 0.2f)
        )
        val verResult = textMeasurer.measure(verText, verStyle)
        drawText(verResult, topLeft = Offset(w / 2f - verResult.size.width / 2f, h * 0.95f))
    }
}
