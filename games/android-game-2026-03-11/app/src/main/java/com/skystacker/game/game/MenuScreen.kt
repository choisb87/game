package com.skystacker.game.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.sin

fun DrawScope.renderMenuOverlay(state: GameState, textMeasurer: TextMeasurer) {
    // Dim overlay
    drawRect(color = Color.Black.copy(alpha = 0.4f), size = size)

    // Title
    val titleText = "SKY STACKER"
    val titleStyle = TextStyle(
        color = Color.White,
        fontSize = 48.sp,
        fontWeight = FontWeight.Bold
    )
    val titleLayout = textMeasurer.measure(titleText, titleStyle)
    drawText(
        textLayoutResult = titleLayout,
        topLeft = Offset(
            (size.width - titleLayout.size.width) / 2f,
            size.height * 0.28f
        )
    )

    // Subtitle
    val subText = "하늘 높이 쌓아라"
    val subStyle = TextStyle(
        color = Color(0xFF4ECDC4),
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium
    )
    val subLayout = textMeasurer.measure(subText, subStyle)
    drawText(
        textLayoutResult = subLayout,
        topLeft = Offset(
            (size.width - subLayout.size.width) / 2f,
            size.height * 0.28f + titleLayout.size.height + 16f
        )
    )

    // Tap to play (pulsing)
    val pulse = (sin(System.currentTimeMillis() / 500.0) * 0.3 + 0.7).toFloat()
    val tapText = "TAP TO PLAY"
    val tapStyle = TextStyle(
        color = Color.White.copy(alpha = pulse),
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold
    )
    val tapLayout = textMeasurer.measure(tapText, tapStyle)
    drawText(
        textLayoutResult = tapLayout,
        topLeft = Offset(
            (size.width - tapLayout.size.width) / 2f,
            size.height * 0.62f
        )
    )

    // Best score
    if (state.bestScore > 0) {
        val bestText = "BEST: ${state.bestScore}"
        val bestStyle = TextStyle(
            color = Color(0xFFFFD700),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        val bestLayout = textMeasurer.measure(bestText, bestStyle)
        drawText(
            textLayoutResult = bestLayout,
            topLeft = Offset(
                (size.width - bestLayout.size.width) / 2f,
                size.height * 0.70f
            )
        )
    }

    // Instructions
    val instrText = "블록이 겹치도록 타이밍에 맞춰 탭하세요"
    val instrStyle = TextStyle(
        color = Color.White.copy(alpha = 0.5f),
        fontSize = 14.sp
    )
    val instrLayout = textMeasurer.measure(instrText, instrStyle)
    drawText(
        textLayoutResult = instrLayout,
        topLeft = Offset(
            (size.width - instrLayout.size.width) / 2f,
            size.height * 0.85f
        )
    )
}

fun DrawScope.renderGameOverOverlay(state: GameState, textMeasurer: TextMeasurer) {
    // Dim overlay
    drawRect(color = Color.Black.copy(alpha = 0.5f), size = size)

    // Game Over
    val goText = "GAME OVER"
    val goStyle = TextStyle(
        color = Color(0xFFFF6B6B),
        fontSize = 42.sp,
        fontWeight = FontWeight.Bold
    )
    val goLayout = textMeasurer.measure(goText, goStyle)
    drawText(
        textLayoutResult = goLayout,
        topLeft = Offset(
            (size.width - goLayout.size.width) / 2f,
            size.height * 0.30f
        )
    )

    // Final score
    val scoreText = "${state.score}"
    val scoreStyle = TextStyle(
        color = Color.White,
        fontSize = 72.sp,
        fontWeight = FontWeight.Bold
    )
    val scoreLayout = textMeasurer.measure(scoreText, scoreStyle)
    drawText(
        textLayoutResult = scoreLayout,
        topLeft = Offset(
            (size.width - scoreLayout.size.width) / 2f,
            size.height * 0.38f
        )
    )

    // New best indicator
    val isNewBest = state.score >= state.bestScore && state.score > 0
    if (isNewBest) {
        val newBestText = "NEW BEST!"
        val newBestStyle = TextStyle(
            color = Color(0xFFFFD700),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        val newBestLayout = textMeasurer.measure(newBestText, newBestStyle)
        drawText(
            textLayoutResult = newBestLayout,
            topLeft = Offset(
                (size.width - newBestLayout.size.width) / 2f,
                size.height * 0.38f + scoreLayout.size.height + 8f
            )
        )
    }

    // Tap to retry (pulsing)
    val pulse = (sin(System.currentTimeMillis() / 500.0) * 0.3 + 0.7).toFloat()
    val tapText = "TAP TO RETRY"
    val tapStyle = TextStyle(
        color = Color.White.copy(alpha = pulse),
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold
    )
    val tapLayout = textMeasurer.measure(tapText, tapStyle)
    drawText(
        textLayoutResult = tapLayout,
        topLeft = Offset(
            (size.width - tapLayout.size.width) / 2f,
            size.height * 0.65f
        )
    )
}
