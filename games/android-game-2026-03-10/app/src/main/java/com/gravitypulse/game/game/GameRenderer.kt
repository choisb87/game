package com.gravitypulse.game.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.sin
import kotlin.random.Random

val NeonCyan = Color(0xFF00FFDD)
val NeonPink = Color(0xFFFF0088)
val NeonGold = Color(0xFFFFDD00)
val DeepBg = Color(0xFF0A0014)
val DimCyan = Color(0xFF004444)
val DimPink = Color(0xFF440022)

fun obstacleColor(colorIndex: Int): Color = when (colorIndex) {
    0 -> NeonCyan
    1 -> NeonPink
    else -> NeonGold
}

@Composable
fun GameRenderer(
    state: GameState,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val shakeX = if (state.screenShake > 0f) (Random.nextFloat() - 0.5f) * state.screenShake * 8f else 0f
        val shakeY = if (state.screenShake > 0f) (Random.nextFloat() - 0.5f) * state.screenShake * 8f else 0f

        translate(shakeX, shakeY) {
            drawBackground(state)
            drawObstacles(state)
            drawPulseRings(state)
            drawPlayer(state)
            drawParticles(state)
            drawHUD(state)
        }
    }
}

private fun DrawScope.drawBackground(state: GameState) {
    // Subtle grid lines
    val gridSpacing = 80f
    val scrollOffset = (state.frameCount * 0.5f) % gridSpacing
    val gridAlpha = 0.06f

    var y = -scrollOffset
    while (y < size.height) {
        drawLine(
            color = NeonCyan.copy(alpha = gridAlpha),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
        y += gridSpacing
    }

    var x = 0f
    while (x < size.width) {
        drawLine(
            color = NeonCyan.copy(alpha = gridAlpha * 0.5f),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )
        x += gridSpacing
    }
}

private fun DrawScope.drawObstacles(state: GameState) {
    for (obs in state.obstacles) {
        val color = obstacleColor(obs.color)
        val gapStartPx = obs.gapStart * size.width
        val gapEndPx = gapStartPx + obs.gapWidth * size.width
        val barHeight = 28f
        val glowAlpha = 0.3f + sin(state.frameCount * 0.1f) * 0.1f

        // Left wall
        if (gapStartPx > 0f) {
            // Glow
            drawRect(
                color = color.copy(alpha = glowAlpha),
                topLeft = Offset(0f, obs.y - barHeight),
                size = Size(gapStartPx, barHeight * 2)
            )
            // Solid bar
            drawRect(
                color = color.copy(alpha = 0.85f),
                topLeft = Offset(0f, obs.y - barHeight / 2),
                size = Size(gapStartPx, barHeight)
            )
            // Edge highlight
            drawLine(
                color = Color.White.copy(alpha = 0.6f),
                start = Offset(0f, obs.y),
                end = Offset(gapStartPx, obs.y),
                strokeWidth = 2f
            )
        }

        // Right wall
        if (gapEndPx < size.width) {
            drawRect(
                color = color.copy(alpha = glowAlpha),
                topLeft = Offset(gapEndPx, obs.y - barHeight),
                size = Size(size.width - gapEndPx, barHeight * 2)
            )
            drawRect(
                color = color.copy(alpha = 0.85f),
                topLeft = Offset(gapEndPx, obs.y - barHeight / 2),
                size = Size(size.width - gapEndPx, barHeight)
            )
            drawLine(
                color = Color.White.copy(alpha = 0.6f),
                start = Offset(gapEndPx, obs.y),
                end = Offset(size.width, obs.y),
                strokeWidth = 2f
            )
        }

        // Gap edge markers
        drawCircle(
            color = color,
            radius = 6f,
            center = Offset(gapStartPx, obs.y)
        )
        drawCircle(
            color = color,
            radius = 6f,
            center = Offset(gapEndPx, obs.y)
        )
    }
}

private fun DrawScope.drawPlayer(state: GameState) {
    val player = state.player
    val px = player.x
    val py = player.y
    val dirColor = if (player.gravityDirection == 1) NeonCyan else NeonPink

    // Trail
    player.trail.forEachIndexed { index, pos ->
        val alpha = (1f - index / player.trail.size.toFloat()) * 0.4f
        val trailRadius = player.radius * (1f - index / player.trail.size.toFloat()) * 0.7f
        drawCircle(
            color = dirColor.copy(alpha = alpha),
            radius = trailRadius,
            center = pos
        )
    }

    // Outer glow
    drawCircle(
        color = dirColor.copy(alpha = 0.2f),
        radius = player.radius * 2.5f,
        center = Offset(px, py)
    )

    // Main body
    drawCircle(
        color = dirColor.copy(alpha = 0.9f),
        radius = player.radius,
        center = Offset(px, py)
    )

    // Inner bright core
    drawCircle(
        color = Color.White.copy(alpha = 0.8f),
        radius = player.radius * 0.45f,
        center = Offset(px, py)
    )

    // Direction indicator (small arrow)
    val arrowDir = player.gravityDirection * 8f
    drawLine(
        color = Color.White.copy(alpha = 0.9f),
        start = Offset(px, py - arrowDir),
        end = Offset(px, py + arrowDir),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawParticles(state: GameState) {
    for (p in state.particles) {
        val color = when (p.color) {
            0 -> NeonCyan
            1 -> NeonPink
            else -> NeonGold
        }
        drawCircle(
            color = color.copy(alpha = p.life * 0.8f),
            radius = 4f * p.life,
            center = Offset(p.x, p.y)
        )
    }
}

private fun DrawScope.drawPulseRings(state: GameState) {
    for (ring in state.pulseRings) {
        val color = NeonCyan.copy(alpha = ring.life * 0.3f)
        drawCircle(
            color = color,
            radius = ring.radius,
            center = Offset(ring.x, ring.y),
            style = Stroke(width = 3f * ring.life)
        )
    }
}

private fun DrawScope.drawHUD(state: GameState) {
    // Score is rendered as Compose Text overlay, not in canvas
    // This just draws subtle decorative HUD elements

    // Top gradient overlay
    drawRect(
        color = DeepBg.copy(alpha = 0.4f),
        topLeft = Offset(0f, 0f),
        size = Size(size.width, 100f)
    )

    // Gravity direction indicator at bottom
    val dirColor = if (state.player.gravityDirection == 1) NeonCyan else NeonPink
    drawLine(
        color = dirColor.copy(alpha = 0.4f),
        start = Offset(size.width / 2 - 40f, size.height - 20f),
        end = Offset(size.width / 2 + 40f, size.height - 20f),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
}
