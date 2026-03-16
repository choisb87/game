package com.gravitywell.game.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

object GameRenderer {

    private val starGold = Color(0xFFFFD740)
    private val starSilver = Color(0xFFB0BEC5)
    private val starRuby = Color(0xFFFF5252)
    private val starDiamond = Color(0xFFE0F7FA)
    private val wellCyan = Color(0xFF00E5FF)
    private val wellPink = Color(0xFFFF4081)
    private val targetGreen = Color(0xFF00E676)
    private val targetBlue = Color(0xFF448AFF)
    private val targetOrange = Color(0xFFFF9100)
    private val targetPink = Color(0xFFFF80AB)
    private val spaceBlack = Color(0xFF030812)
    private val deepSpace = Color(0xFF0A1628)

    fun DrawScope.render(state: GameState, textMeasurer: TextMeasurer) {
        val shakeX = if (state.shakeAmount > 0.5f) {
            (Math.random().toFloat() - 0.5f) * state.shakeAmount * 2f
        } else 0f
        val shakeY = if (state.shakeAmount > 0.5f) {
            (Math.random().toFloat() - 0.5f) * state.shakeAmount * 2f
        } else 0f

        translate(shakeX, shakeY) {
            drawBackground(state)
            drawTargetZones(state)
            drawGravityWells(state)
            drawStarTrails(state)
            drawStars(state)
            drawCatchEffects(state)
            drawHUD(state, textMeasurer)
        }
    }

    private fun DrawScope.drawBackground(state: GameState) {
        // Deep space gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(spaceBlack, deepSpace, spaceBlack),
                startY = 0f,
                endY = state.screenHeight
            )
        )

        // Background stars (static decorative)
        val seed = 42
        for (i in 0 until 60) {
            val x = ((seed * (i + 1) * 7919) % state.screenWidth.toInt()).toFloat()
            val y = ((seed * (i + 1) * 6271) % state.screenHeight.toInt()).toFloat()
            val brightness = 0.2f + (i % 5) * 0.15f
            val twinkle = abs(sin(state.gameTime * (1f + i % 3) + i.toFloat()))
            drawCircle(
                color = Color.White.copy(alpha = brightness * twinkle),
                radius = 1f + (i % 3) * 0.5f,
                center = Offset(x, y)
            )
        }
    }

    private fun DrawScope.drawTargetZones(state: GameState) {
        val zoneY = state.screenHeight * 0.85f
        val zoneHeight = state.screenHeight * 0.11f

        for (target in state.targets) {
            val color = colorForStar(target.color)
            val glowAlpha = 0.15f + 0.1f * sin(target.glowPhase)
            val left = target.centerX - target.width / 2

            // Zone background glow
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, color.copy(alpha = glowAlpha), color.copy(alpha = glowAlpha * 0.5f)),
                    startY = zoneY - 40f,
                    endY = zoneY + zoneHeight
                ),
                topLeft = Offset(left, zoneY - 40f),
                size = Size(target.width, zoneHeight + 40f)
            )

            // Zone border
            drawLine(
                color = color.copy(alpha = 0.6f),
                start = Offset(left, zoneY),
                end = Offset(left, zoneY + zoneHeight),
                strokeWidth = 2f
            )
            drawLine(
                color = color.copy(alpha = 0.6f),
                start = Offset(left + target.width, zoneY),
                end = Offset(left + target.width, zoneY + zoneHeight),
                strokeWidth = 2f
            )

            // Zone top line
            drawLine(
                color = color.copy(alpha = 0.8f),
                start = Offset(left, zoneY),
                end = Offset(left + target.width, zoneY),
                strokeWidth = 2f
            )

            // Color indicator star icon in zone
            val iconSize = 8f
            drawStarShape(Offset(target.centerX, zoneY + zoneHeight / 2), iconSize, color.copy(alpha = 0.5f))
        }
    }

    private fun DrawScope.drawStarShape(center: Offset, size: Float, color: Color) {
        val path = Path()
        for (i in 0 until 5) {
            val outerAngle = (i * 72 - 90) * PI.toFloat() / 180f
            val innerAngle = ((i * 72 + 36) - 90) * PI.toFloat() / 180f
            val ox = center.x + cos(outerAngle) * size
            val oy = center.y + sin(outerAngle) * size
            val ix = center.x + cos(innerAngle) * size * 0.45f
            val iy = center.y + sin(innerAngle) * size * 0.45f

            if (i == 0) path.moveTo(ox, oy)
            else path.lineTo(ox, oy)
            path.lineTo(ix, iy)
        }
        path.close()
        drawPath(path, color)
    }

    private fun DrawScope.drawGravityWells(state: GameState) {
        for (well in state.wells) {
            val alpha = (well.lifeRemaining / 8f).coerceIn(0f, 1f)
            val baseColor = if (well.strength > 0) wellCyan else wellPink
            val pulseScale = 1f + 0.15f * sin(well.pulsePhase)

            // Influence radius rings (3 rings)
            for (ring in 3 downTo 1) {
                val ringRadius = well.radius * ring / 3f * pulseScale
                val ringAlpha = alpha * 0.12f * ring / 3f
                drawCircle(
                    color = baseColor.copy(alpha = ringAlpha),
                    radius = ringRadius,
                    center = well.pos
                )
            }

            // Influence border
            drawCircle(
                color = baseColor.copy(alpha = alpha * 0.3f),
                radius = well.radius * pulseScale,
                center = well.pos,
                style = Stroke(width = 1.5f)
            )

            // Well core glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        baseColor.copy(alpha = alpha * 0.9f),
                        baseColor.copy(alpha = alpha * 0.3f),
                        Color.Transparent
                    ),
                    center = well.pos,
                    radius = 30f
                ),
                radius = 30f,
                center = well.pos
            )

            // Core dot
            drawCircle(
                color = baseColor.copy(alpha = alpha),
                radius = 5f,
                center = well.pos
            )

            // Directional arrows
            if (well.strength > 0) {
                // Inward arrows for attract
                for (angle in 0 until 360 step 90) {
                    val rad = angle * PI.toFloat() / 180f
                    val dist = 20f + 8f * sin(well.pulsePhase + angle)
                    val arrowPos = Offset(
                        well.pos.x + cos(rad) * dist,
                        well.pos.y + sin(rad) * dist
                    )
                    drawCircle(
                        color = baseColor.copy(alpha = alpha * 0.6f),
                        radius = 2f,
                        center = arrowPos
                    )
                }
            } else {
                // Outward arrows for repel
                for (angle in 0 until 360 step 90) {
                    val rad = angle * PI.toFloat() / 180f
                    val dist = 25f + 8f * sin(well.pulsePhase + angle)
                    val arrowPos = Offset(
                        well.pos.x + cos(rad) * dist,
                        well.pos.y + sin(rad) * dist
                    )
                    drawCircle(
                        color = baseColor.copy(alpha = alpha * 0.6f),
                        radius = 2.5f,
                        center = arrowPos
                    )
                }
            }
        }
    }

    private fun DrawScope.drawStarTrails(state: GameState) {
        for (star in state.stars) {
            if (star.caught || star.missed) continue
            val color = colorForStar(star.color)

            for ((i, trailPos) in star.trail.withIndex()) {
                val trailAlpha = (1f - i.toFloat() / star.trail.size) * 0.35f
                val trailRadius = star.radius * (1f - i.toFloat() / star.trail.size) * 0.6f
                drawCircle(
                    color = color.copy(alpha = trailAlpha),
                    radius = trailRadius,
                    center = trailPos
                )
            }
        }
    }

    private fun DrawScope.drawStars(state: GameState) {
        for (star in state.stars) {
            if (star.caught || star.missed) continue
            val color = colorForStar(star.color)

            // Outer glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0.5f),
                        color.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = star.pos,
                    radius = star.radius * 2.5f
                ),
                radius = star.radius * 2.5f,
                center = star.pos
            )

            // Star body
            drawStarShape(star.pos, star.radius, color)

            // Inner bright core
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = star.radius * 0.3f,
                center = star.pos
            )
        }
    }

    private fun DrawScope.drawCatchEffects(state: GameState) {
        for (effect in state.catchEffects) {
            val color = colorForStar(effect.color)
            val progress = effect.age / 1.2f
            val alpha = (1f - progress).coerceIn(0f, 1f)
            val scale = 1f + progress * 3f

            // Expanding ring
            drawCircle(
                color = color.copy(alpha = alpha * 0.6f),
                radius = 20f * scale,
                center = effect.pos,
                style = Stroke(width = 3f * (1f - progress))
            )

            // Burst particles
            for (i in 0 until 8) {
                val angle = (i * 45 + effect.age * 200) * PI.toFloat() / 180f
                val dist = 15f * scale
                val particlePos = Offset(
                    effect.pos.x + cos(angle) * dist,
                    effect.pos.y + sin(angle) * dist
                )
                drawCircle(
                    color = color.copy(alpha = alpha * 0.8f),
                    radius = 3f * (1f - progress),
                    center = particlePos
                )
            }

            // Combo text
            if (effect.combo > 1) {
                // Score float-up handled in HUD
            }
        }
    }

    private fun DrawScope.drawHUD(state: GameState, textMeasurer: TextMeasurer) {
        val hudY = 60f
        val padding = 24f

        // Score
        val scoreText = "${state.score}"
        val scoreResult = textMeasurer.measure(
            scoreText,
            TextStyle(
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        )
        drawText(scoreResult, topLeft = Offset(padding, hudY))

        // Level
        val levelText = "LV ${state.level}"
        val levelResult = textMeasurer.measure(
            levelText,
            TextStyle(
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        )
        drawText(levelResult, topLeft = Offset(padding, hudY + 50f))

        // Wells remaining (right side)
        val wellText = "◉ ${state.wellsRemaining}"
        val wellColor = if (state.wellMode == WellMode.ATTRACT) wellCyan else wellPink
        val wellResult = textMeasurer.measure(
            wellText,
            TextStyle(
                color = wellColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        )
        drawText(wellResult, topLeft = Offset(state.screenWidth - wellResult.size.width - padding, hudY))

        // Well mode indicator
        val modeText = if (state.wellMode == WellMode.ATTRACT) "인력" else "척력"
        val modeResult = textMeasurer.measure(
            modeText,
            TextStyle(
                color = wellColor.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        )
        drawText(modeResult, topLeft = Offset(state.screenWidth - modeResult.size.width - padding, hudY + 36f))

        // Progress bar
        val barY = hudY + 90f
        val barWidth = state.screenWidth - padding * 2
        val barHeight = 6f
        val progress = (state.starsCaught + state.starsMissed).toFloat() / state.totalStarsInLevel

        drawRect(
            color = Color.White.copy(alpha = 0.1f),
            topLeft = Offset(padding, barY),
            size = Size(barWidth, barHeight)
        )
        drawRect(
            color = Color(0xFF00E676).copy(alpha = 0.7f),
            topLeft = Offset(padding, barY),
            size = Size(barWidth * progress.coerceIn(0f, 1f), barHeight)
        )

        // Stars caught ratio
        val ratioText = "${state.starsCaught}/${state.totalStarsInLevel}"
        val ratioResult = textMeasurer.measure(
            ratioText,
            TextStyle(
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        )
        drawText(ratioResult, topLeft = Offset(state.screenWidth / 2 - ratioResult.size.width / 2, barY + 10f))

        // Combo display
        if (state.combo >= 2) {
            val comboText = "${state.combo}x COMBO"
            val comboAlpha = if (state.combo >= 5) 1f else 0.8f
            val comboSize = (20 + state.combo * 2).coerceAtMost(36)
            val comboResult = textMeasurer.measure(
                comboText,
                TextStyle(
                    color = starGold.copy(alpha = comboAlpha),
                    fontSize = comboSize.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            drawText(
                comboResult,
                topLeft = Offset(
                    state.screenWidth / 2 - comboResult.size.width / 2,
                    state.screenHeight * 0.15f
                )
            )
        }

        // Slow motion indicator
        if (state.slowMotion) {
            val slowText = "SLOW MOTION"
            val slowResult = textMeasurer.measure(
                slowText,
                TextStyle(
                    color = Color(0xFFE040FB).copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            drawText(
                slowResult,
                topLeft = Offset(
                    state.screenWidth / 2 - slowResult.size.width / 2,
                    state.screenHeight * 0.2f
                )
            )
        }

        // Floating score effects from catches
        for (effect in state.catchEffects) {
            if (effect.combo > 1) {
                val floatY = effect.pos.y - effect.age * 80f
                val alpha = max(0f, 1f - effect.age / 1.2f)
                val pointsText = "+${starPoints(effect.color, effect.combo)}"
                val pointsResult = textMeasurer.measure(
                    pointsText,
                    TextStyle(
                        color = colorForStar(effect.color).copy(alpha = alpha),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                drawText(
                    pointsResult,
                    topLeft = Offset(
                        effect.pos.x - pointsResult.size.width / 2,
                        floatY
                    )
                )
            }
        }
    }

    private fun colorForStar(color: StarColor): Color = when (color) {
        StarColor.GOLD -> starGold
        StarColor.SILVER -> starSilver
        StarColor.RUBY -> starRuby
        StarColor.DIAMOND -> starDiamond
    }
}
