package com.skystacker.game.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.sin

object GameRenderer {

    private val SKY_TOP = Color(0xFF0B0E2D)
    private val SKY_BOTTOM = Color(0xFF1A1A4E)
    private val STAR_COLOR = Color(0x88FFFFFF)

    fun DrawScope.renderGame(state: GameState, textMeasurer: TextMeasurer) {
        drawBackground(state)
        drawStars(state)

        withTransform({
            translate(
                left = (kotlin.random.Random.nextFloat() - 0.5f) * state.shakeAmount * 2f,
                top = (kotlin.random.Random.nextFloat() - 0.5f) * state.shakeAmount * 2f
            )
        }) {
            drawStack(state)
            drawCurrentBlock(state)
            drawFallingPieces(state)
            drawParticles(state)
        }

        if (state.perfectFlash > 0f) {
            drawRect(
                color = Color(0xFFFFD700).copy(alpha = state.perfectFlash * 0.15f),
                size = size
            )
        }

        drawHUD(state, textMeasurer)
    }

    private fun DrawScope.drawBackground(state: GameState) {
        val progress = (state.stack.size.toFloat() / 50f).coerceIn(0f, 1f)
        val topColor = lerp(SKY_BOTTOM, Color(0xFF2D1B69), progress)
        val bottomColor = lerp(SKY_TOP, Color(0xFF0D0221), progress)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(topColor, bottomColor)
            )
        )
    }

    private fun DrawScope.drawStars(state: GameState) {
        val seed = 42
        for (i in 0 until 60) {
            val hash = (i * 7919 + seed) % 10000
            val sx = (hash % 1000) / 1000f * size.width
            val sy = (hash / 1000f % 1f) * size.height
            val twinkle = (sin((System.currentTimeMillis() / 1000.0 + i * 0.3).toFloat()) * 0.3f + 0.7f)
            val starSize = ((hash % 3) + 1).toFloat()
            drawCircle(
                color = STAR_COLOR.copy(alpha = twinkle * 0.6f),
                radius = starSize,
                center = Offset(sx, sy)
            )
        }
    }

    private fun DrawScope.drawStack(state: GameState) {
        val bh = state.blockHeight
        val baseY = size.height - bh

        state.stack.forEachIndexed { index, block ->
            val y = baseY - index * bh + state.cameraY
            if (y > -bh && y < size.height + bh) {
                // Block body
                drawRect(
                    color = block.color,
                    topLeft = Offset(block.x, y),
                    size = Size(block.width, bh)
                )
                // Highlight on top edge
                drawRect(
                    color = Color.White.copy(alpha = 0.2f),
                    topLeft = Offset(block.x, y),
                    size = Size(block.width, bh * 0.15f)
                )
                // Shadow on bottom
                drawRect(
                    color = Color.Black.copy(alpha = 0.15f),
                    topLeft = Offset(block.x, y + bh * 0.85f),
                    size = Size(block.width, bh * 0.15f)
                )
                // Left edge highlight
                drawRect(
                    color = Color.White.copy(alpha = 0.1f),
                    topLeft = Offset(block.x, y),
                    size = Size(3f, bh)
                )
            }
        }
    }

    private fun DrawScope.drawCurrentBlock(state: GameState) {
        val block = state.currentBlock ?: return
        val bh = state.blockHeight
        val y = size.height - state.stack.size * bh - bh + state.cameraY

        // Shadow below
        drawRect(
            color = Color.Black.copy(alpha = 0.3f),
            topLeft = Offset(block.x + 4f, y + 4f),
            size = Size(block.width, bh)
        )
        // Block body
        drawRect(
            color = block.color,
            topLeft = Offset(block.x, y),
            size = Size(block.width, bh)
        )
        // Glow effect
        drawRect(
            color = Color.White.copy(alpha = 0.25f),
            topLeft = Offset(block.x, y),
            size = Size(block.width, bh * 0.2f)
        )
    }

    private fun DrawScope.drawFallingPieces(state: GameState) {
        val bh = state.blockHeight
        state.fallingPieces.forEach { fp ->
            drawRect(
                color = fp.color.copy(alpha = fp.alpha),
                topLeft = Offset(fp.x, fp.y + size.height * 0.4f),
                size = Size(fp.width, bh)
            )
        }
    }

    private fun DrawScope.drawParticles(state: GameState) {
        val baseY = size.height - state.stack.size * state.blockHeight + state.cameraY
        state.particles.forEach { p ->
            drawCircle(
                color = p.color.copy(alpha = p.life),
                radius = p.size * p.life,
                center = Offset(p.x, baseY + p.y)
            )
        }
    }

    private fun DrawScope.drawHUD(state: GameState, textMeasurer: TextMeasurer) {
        // Score
        val scoreText = "${state.score}"
        val scoreStyle = TextStyle(
            color = Color.White,
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold
        )
        val scoreLayout = textMeasurer.measure(scoreText, scoreStyle)
        drawText(
            textLayoutResult = scoreLayout,
            topLeft = Offset(
                (size.width - scoreLayout.size.width) / 2f,
                size.height * 0.08f
            )
        )

        // Combo indicator
        if (state.combo >= 1) {
            val comboText = if (state.combo == 1) "PERFECT!" else "PERFECT x${state.combo}"
            val comboStyle = TextStyle(
                color = Color(0xFFFFD700),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            val comboLayout = textMeasurer.measure(comboText, comboStyle)
            drawText(
                textLayoutResult = comboLayout,
                topLeft = Offset(
                    (size.width - comboLayout.size.width) / 2f,
                    size.height * 0.08f + scoreLayout.size.height + 8f
                )
            )
        }

        // Best score (small, top-right)
        if (state.bestScore > 0) {
            val bestText = "BEST ${state.bestScore}"
            val bestStyle = TextStyle(
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            val bestLayout = textMeasurer.measure(bestText, bestStyle)
            drawText(
                textLayoutResult = bestLayout,
                topLeft = Offset(size.width - bestLayout.size.width - 20f, 40f)
            )
        }
    }

    private fun lerp(a: Color, b: Color, t: Float): Color {
        return Color(
            red = a.red + (b.red - a.red) * t,
            green = a.green + (b.green - a.green) * t,
            blue = a.blue + (b.blue - a.blue) * t,
            alpha = 1f
        )
    }
}
