package com.voidrunner.gravity.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.*

object GameRenderer {

    private val cyanColor = Color(0xFF00E5FF)
    private val magentaColor = Color(0xFFFF006E)
    private val goldColor = Color(0xFFFFD700)
    private val redColor = Color(0xFFFF3333)
    private val darkBg = Color(0xFF050510)
    private val wallColor = Color(0xFF1A1A3E)
    private val wallEdge = Color(0xFF2A2A5E)
    private val gridColor = Color(0x15FFFFFF)

    fun render(scope: DrawScope, state: GameState, textMeasurer: TextMeasurer) {
        val w = scope.size.width
        val h = scope.size.height

        // Shake offset
        val shakeX = if (state.screenShake > 0f) (Math.random() * 8 - 4).toFloat() * state.screenShake else 0f
        val shakeY = if (state.screenShake > 0f) (Math.random() * 8 - 4).toFloat() * state.screenShake else 0f

        scope.drawContext.transform.translate(shakeX, shakeY)

        drawBackground(scope, state, w, h)
        drawCorridor(scope, state, w, h)
        drawObstacles(scope, state, w, h)
        drawCrystals(scope, state, w, h)
        drawTrail(scope, state, w, h)

        if (state.phase != GamePhase.DEAD || state.deathTimer < 0.1f) {
            drawPlayer(scope, state, w, h)
        }

        drawParticles(scope, state, w, h)
        drawFloatingTexts(scope, state, w, h, textMeasurer)

        // Reset transform
        scope.drawContext.transform.translate(-shakeX, -shakeY)

        // Screen flash
        if (state.screenFlash > 0f) {
            scope.drawRect(
                cyanColor.copy(alpha = state.screenFlash * 0.15f),
                size = scope.size
            )
        }

        // HUD on top
        drawHUD(scope, state, w, h, textMeasurer)

        // Phase overlays
        when (state.phase) {
            GamePhase.READY -> drawReadyOverlay(scope, state, w, h, textMeasurer)
            GamePhase.DEAD -> drawDeathOverlay(scope, state, w, h, textMeasurer)
            GamePhase.SCORE -> drawScoreOverlay(scope, state, w, h, textMeasurer)
            else -> {}
        }

        // Zone announcement
        if (state.zoneFlash > 0f) {
            drawZoneAnnouncement(scope, state, w, h, textMeasurer)
        }
    }

    // --- Background ---

    private fun drawBackground(scope: DrawScope, state: GameState, w: Float, h: Float) {
        scope.drawRect(darkBg, size = scope.size)

        // Parallax stars
        val parallax = (state.worldOffset * 0.02f) % 1f
        for (star in state.stars) {
            val sx = ((star.x - parallax + 1f) % 1f) * w
            val sy = star.y * h
            val brightness = 0.3f + (sin(state.gameTime * 2f + star.x * 10f) * 0.2f).toFloat()
            scope.drawCircle(
                Color.White.copy(alpha = brightness.coerceIn(0.1f, 0.6f)),
                radius = 1f + star.y * 1.5f,
                center = Offset(sx, sy)
            )
        }
    }

    // --- Corridor ---

    private fun drawCorridor(scope: DrawScope, state: GameState, w: Float, h: Float) {
        val topY = GameState.CORRIDOR_TOP * h
        val bottomY = GameState.CORRIDOR_BOTTOM * h

        // Walls
        scope.drawRect(wallColor, Offset(0f, 0f), Size(w, topY))
        scope.drawRect(wallColor, Offset(0f, bottomY), Size(w, h - bottomY))

        // Wall edges with glow
        scope.drawLine(wallEdge, Offset(0f, topY), Offset(w, topY), strokeWidth = 2f)
        scope.drawLine(wallEdge, Offset(0f, bottomY), Offset(w, bottomY), strokeWidth = 2f)
        scope.drawLine(
            cyanColor.copy(alpha = 0.15f),
            Offset(0f, topY + 1f), Offset(w, topY + 1f), strokeWidth = 1f
        )
        scope.drawLine(
            cyanColor.copy(alpha = 0.15f),
            Offset(0f, bottomY - 1f), Offset(w, bottomY - 1f), strokeWidth = 1f
        )

        // Grid lines
        val gridSpacing = w / 20f
        val offset = (state.worldOffset * w / GameState.VISIBLE_WORLD_UNITS) % gridSpacing
        var gx = -offset
        while (gx < w) {
            scope.drawLine(gridColor, Offset(gx, topY), Offset(gx, bottomY), strokeWidth = 0.5f)
            gx += gridSpacing
        }
        val corridorH = bottomY - topY
        val hGridCount = 6
        for (i in 1 until hGridCount) {
            val gy = topY + corridorH * i / hGridCount
            scope.drawLine(gridColor, Offset(0f, gy), Offset(w, gy), strokeWidth = 0.5f)
        }
    }

    // --- Obstacles ---

    private fun drawObstacles(scope: DrawScope, state: GameState, w: Float, h: Float) {
        val unitPx = w / GameState.VISIBLE_WORLD_UNITS

        for (obs in state.obstacles) {
            val screenX = (obs.x - state.worldOffset) * unitPx
            if (screenX < -unitPx * 2 || screenX > w + unitPx * 2) continue

            val obsW = obs.width * unitPx

            when (obs.type) {
                ObstacleType.SPIKE_BOTTOM -> {
                    drawSpikes(scope, screenX, obsW, h, true, false)
                }
                ObstacleType.SPIKE_TOP -> {
                    drawSpikes(scope, screenX, obsW, h, false, true)
                }
                ObstacleType.SPIKE_BOTH -> {
                    drawSpikes(scope, screenX, obsW, h, true, true)
                }
                ObstacleType.WALL_GAP_TOP, ObstacleType.WALL_GAP_BOTTOM,
                ObstacleType.WALL_GAP_CENTER -> {
                    drawWallGap(scope, screenX, obsW, obs, h)
                }
                ObstacleType.LASER -> {
                    drawLaser(scope, screenX, obsW, obs, w, h)
                }
            }
        }
    }

    private fun drawSpikes(
        scope: DrawScope, x: Float, width: Float, h: Float,
        bottom: Boolean, top: Boolean
    ) {
        val spikeH = GameState.SPIKE_HEIGHT * h
        val spikeCount = (width / 12f).toInt().coerceAtLeast(2)
        val spikeW = width / spikeCount

        if (bottom) {
            val baseY = GameState.FLOOR_Y * h
            for (i in 0 until spikeCount) {
                val path = Path().apply {
                    moveTo(x + i * spikeW, baseY)
                    lineTo(x + (i + 0.5f) * spikeW, baseY - spikeH)
                    lineTo(x + (i + 1) * spikeW, baseY)
                    close()
                }
                scope.drawPath(path, redColor)
                scope.drawPath(path, redColor.copy(alpha = 0.3f), style = Stroke(width = 2f))
            }
            // Glow
            scope.drawRect(
                Brush.verticalGradient(
                    listOf(redColor.copy(alpha = 0.15f), Color.Transparent),
                    startY = baseY - spikeH,
                    endY = baseY - spikeH * 2
                ),
                Offset(x, baseY - spikeH * 2),
                Size(width, spikeH * 2)
            )
        }

        if (top) {
            val baseY = GameState.CEILING_Y * h
            for (i in 0 until spikeCount) {
                val path = Path().apply {
                    moveTo(x + i * spikeW, baseY)
                    lineTo(x + (i + 0.5f) * spikeW, baseY + spikeH)
                    lineTo(x + (i + 1) * spikeW, baseY)
                    close()
                }
                scope.drawPath(path, redColor)
                scope.drawPath(path, redColor.copy(alpha = 0.3f), style = Stroke(width = 2f))
            }
            scope.drawRect(
                Brush.verticalGradient(
                    listOf(Color.Transparent, redColor.copy(alpha = 0.15f)),
                    startY = baseY,
                    endY = baseY + spikeH * 2
                ),
                Offset(x, baseY),
                Size(width, spikeH * 2)
            )
        }
    }

    private fun drawWallGap(scope: DrawScope, x: Float, width: Float, obs: Obstacle, h: Float) {
        val corridorRange = GameState.CORRIDOR_BOTTOM - GameState.CORRIDOR_TOP
        val gapTop = (GameState.CORRIDOR_TOP + corridorRange * (obs.gapCenter - obs.gapSize / 2f)) * h
        val gapBottom = (GameState.CORRIDOR_TOP + corridorRange * (obs.gapCenter + obs.gapSize / 2f)) * h
        val topWall = GameState.CORRIDOR_TOP * h
        val bottomWall = GameState.CORRIDOR_BOTTOM * h

        val wallCol = Color(0xFF8B0000)
        val edgeCol = Color(0xFFFF4444)

        // Top wall section
        if (gapTop > topWall) {
            scope.drawRect(wallCol, Offset(x, topWall), Size(width, gapTop - topWall))
            scope.drawRect(edgeCol.copy(alpha = 0.5f), Offset(x, topWall), Size(width, gapTop - topWall), style = Stroke(1.5f))
        }

        // Bottom wall section
        if (gapBottom < bottomWall) {
            scope.drawRect(wallCol, Offset(x, gapBottom), Size(width, bottomWall - gapBottom))
            scope.drawRect(edgeCol.copy(alpha = 0.5f), Offset(x, gapBottom), Size(width, bottomWall - gapBottom), style = Stroke(1.5f))
        }

        // Gap glow
        scope.drawLine(
            cyanColor.copy(alpha = 0.4f),
            Offset(x, gapTop), Offset(x + width, gapTop), strokeWidth = 2f
        )
        scope.drawLine(
            cyanColor.copy(alpha = 0.4f),
            Offset(x, gapBottom), Offset(x + width, gapBottom), strokeWidth = 2f
        )
    }

    private fun drawLaser(scope: DrawScope, x: Float, width: Float, obs: Obstacle, w: Float, h: Float) {
        val laserY = 0.5f * h
        val emitterSize = 6f

        // Emitter dots on walls
        scope.drawCircle(redColor, emitterSize, Offset(x, laserY))
        scope.drawCircle(redColor, emitterSize, Offset(x + width, laserY))

        if (obs.laserOn) {
            // Laser beam
            scope.drawLine(
                redColor, Offset(x, laserY), Offset(x + width, laserY),
                strokeWidth = 4f, cap = StrokeCap.Round
            )
            // Glow
            scope.drawLine(
                redColor.copy(alpha = 0.3f),
                Offset(x, laserY), Offset(x + width, laserY),
                strokeWidth = 12f, cap = StrokeCap.Round
            )
            scope.drawLine(
                redColor.copy(alpha = 0.1f),
                Offset(x, laserY), Offset(x + width, laserY),
                strokeWidth = 24f, cap = StrokeCap.Round
            )
        } else {
            // Dim indicator
            scope.drawLine(
                redColor.copy(alpha = 0.08f),
                Offset(x, laserY), Offset(x + width, laserY),
                strokeWidth = 2f
            )
        }
    }

    // --- Crystals ---

    private fun drawCrystals(scope: DrawScope, state: GameState, w: Float, h: Float) {
        val unitPx = w / GameState.VISIBLE_WORLD_UNITS

        for (crystal in state.crystals) {
            if (crystal.collected) continue
            val sx = (crystal.x - state.worldOffset) * unitPx
            if (sx < -20f || sx > w + 20f) continue
            val sy = crystal.y * h

            val isRare = crystal.value > 100
            val baseColor = if (isRare) goldColor else cyanColor
            val pulse = 1f + sin(state.gameTime * 5f + crystal.x * 3f).toFloat() * 0.15f
            val radius = GameState.CRYSTAL_SIZE * h * pulse

            // Glow
            scope.drawCircle(baseColor.copy(alpha = 0.15f), radius * 3f, Offset(sx, sy))
            scope.drawCircle(baseColor.copy(alpha = 0.3f), radius * 1.8f, Offset(sx, sy))

            // Diamond shape
            val path = Path().apply {
                moveTo(sx, sy - radius)
                lineTo(sx + radius * 0.7f, sy)
                lineTo(sx, sy + radius)
                lineTo(sx - radius * 0.7f, sy)
                close()
            }
            scope.drawPath(path, baseColor)
            scope.drawPath(path, Color.White.copy(alpha = 0.4f), style = Stroke(1.5f))

            // Highlight
            scope.drawCircle(Color.White.copy(alpha = 0.5f), radius * 0.2f, Offset(sx - radius * 0.15f, sy - radius * 0.3f))
        }
    }

    // --- Player ---

    private fun drawTrail(scope: DrawScope, state: GameState, w: Float, h: Float) {
        val points = state.trailPoints
        if (points.size < 2) return

        for (i in 1 until points.size) {
            val alpha = (1f - i.toFloat() / points.size) * 0.5f
            val width = (1f - i.toFloat() / points.size) * 6f
            scope.drawLine(
                cyanColor.copy(alpha = alpha),
                Offset(points[i - 1].x * w, points[i - 1].y * h),
                Offset(points[i].x * w, points[i].y * h),
                strokeWidth = width,
                cap = StrokeCap.Round
            )
        }
    }

    private fun drawPlayer(scope: DrawScope, state: GameState, w: Float, h: Float) {
        val px = GameState.PLAYER_X_NORM * w
        val py = (state.playerY + GameState.PLAYER_SIZE / 2f) * h
        val size = GameState.PLAYER_SIZE * h

        // Glow
        scope.drawCircle(cyanColor.copy(alpha = 0.1f), size * 3f, Offset(px, py))
        scope.drawCircle(cyanColor.copy(alpha = 0.2f), size * 2f, Offset(px, py))

        // Diamond body
        scope.rotate(state.playerRotation, Offset(px, py)) {
            val path = Path().apply {
                moveTo(px + size, py)
                lineTo(px, py - size * 0.8f)
                lineTo(px - size * 0.6f, py)
                lineTo(px, py + size * 0.8f)
                close()
            }
            drawPath(path, cyanColor)
            drawPath(path, Color.White.copy(alpha = 0.3f), style = Stroke(2f))

            // Inner highlight
            val inner = Path().apply {
                moveTo(px + size * 0.5f, py)
                lineTo(px, py - size * 0.35f)
                lineTo(px - size * 0.25f, py)
                lineTo(px, py + size * 0.35f)
                close()
            }
            drawPath(inner, Color.White.copy(alpha = 0.25f))
        }
    }

    // --- Particles & floating text ---

    private fun drawParticles(scope: DrawScope, state: GameState, w: Float, h: Float) {
        val unitPx = w / GameState.VISIBLE_WORLD_UNITS

        for (p in state.particles) {
            val alpha = (p.life / 1f).coerceIn(0f, 1f)
            // Particles with world X near player use normalized coords
            val isScreenSpace = p.x < 2f
            val sx = if (isScreenSpace) p.x * w else (p.x - state.worldOffset) * unitPx
            val sy = p.y * h

            scope.drawCircle(
                Color(p.color).copy(alpha = alpha * 0.8f),
                radius = p.size * alpha,
                center = Offset(sx, sy)
            )
        }
    }

    private fun drawFloatingTexts(
        scope: DrawScope, state: GameState, w: Float, h: Float,
        textMeasurer: TextMeasurer
    ) {
        val unitPx = w / GameState.VISIBLE_WORLD_UNITS

        for (ft in state.floatingTexts) {
            val alpha = (ft.life / 1.2f).coerceIn(0f, 1f)
            val sx = (ft.x - state.worldOffset) * unitPx
            val sy = ft.y * h

            val style = TextStyle(
                color = Color(ft.color).copy(alpha = alpha),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            val result = textMeasurer.measure(ft.text, style)
            scope.drawText(result, topLeft = Offset(sx - result.size.width / 2f, sy))
        }
    }

    // --- HUD ---

    private fun drawHUD(scope: DrawScope, state: GameState, w: Float, h: Float, textMeasurer: TextMeasurer) {
        if (state.phase == GamePhase.SCORE) return

        val padding = 16f
        val topSafe = 48f

        // Score - top left
        drawTextWithShadow(scope, textMeasurer,
            "${state.score}",
            Offset(padding, topSafe),
            24.sp, cyanColor
        )

        // Distance - top left below score
        val distText = "${state.distance.toInt()}m"
        drawTextWithShadow(scope, textMeasurer,
            distText,
            Offset(padding, topSafe + 32f),
            14.sp, Color.White.copy(alpha = 0.6f)
        )

        // Zone - top right
        val zoneText = "ZONE ${state.zoneNumber}"
        val zoneResult = textMeasurer.measure(zoneText, TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Monospace))
        drawTextWithShadow(scope, textMeasurer,
            zoneText,
            Offset(w - zoneResult.size.width - padding, topSafe),
            14.sp, magentaColor.copy(alpha = 0.8f)
        )

        // Combo - top center
        if (state.combo > 1) {
            val comboText = "x${state.combo} COMBO"
            val comboResult = textMeasurer.measure(comboText, TextStyle(fontSize = 20.sp, fontFamily = FontFamily.Monospace))
            val comboAlpha = (state.comboTimer / GameState.COMBO_WINDOW).coerceIn(0f, 1f)
            drawTextWithShadow(scope, textMeasurer,
                comboText,
                Offset(w / 2f - comboResult.size.width / 2f, topSafe),
                20.sp, goldColor.copy(alpha = comboAlpha)
            )

            // Combo timer bar
            val barW = 120f
            val barH = 4f
            val barX = w / 2f - barW / 2f
            val barY = topSafe + 28f
            scope.drawRect(Color.White.copy(alpha = 0.1f), Offset(barX, barY), Size(barW, barH))
            scope.drawRect(goldColor.copy(alpha = comboAlpha * 0.8f), Offset(barX, barY), Size(barW * comboAlpha, barH))
        }

        // Crystals count - below zone
        val crystalText = "💎 ${state.totalCrystals}"
        val ctResult = textMeasurer.measure(crystalText, TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Monospace))
        drawTextWithShadow(scope, textMeasurer,
            crystalText,
            Offset(w - ctResult.size.width - padding, topSafe + 22f),
            14.sp, goldColor.copy(alpha = 0.7f)
        )
    }

    // --- Overlays ---

    private fun drawReadyOverlay(
        scope: DrawScope, state: GameState, w: Float, h: Float,
        textMeasurer: TextMeasurer
    ) {
        // Title
        val title = "VOID RUNNER"
        val titleStyle = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        val titleResult = textMeasurer.measure(title, titleStyle)
        val titleX = w / 2f - titleResult.size.width / 2f
        val titleY = h * 0.3f

        // Glow behind title
        scope.drawRect(
            Brush.radialGradient(
                listOf(cyanColor.copy(alpha = 0.1f), Color.Transparent),
                center = Offset(w / 2f, titleY + 20f),
                radius = 200f
            ),
            Offset(w / 2f - 200f, titleY - 40f),
            Size(400f, 120f)
        )

        scope.drawText(
            textMeasurer.measure(title, titleStyle.copy(color = cyanColor)),
            topLeft = Offset(titleX, titleY)
        )

        // Subtitle
        val sub = "중력의 끝"
        val subStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
        val subResult = textMeasurer.measure(sub, subStyle.copy(color = magentaColor))
        scope.drawText(subResult, topLeft = Offset(w / 2f - subResult.size.width / 2f, titleY + 50f))

        // Tap to start (pulsing)
        val alpha = (sin(state.readyPulse.toDouble()) * 0.4f + 0.6f).toFloat()
        val tapText = "TAP TO START"
        val tapStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        val tapResult = textMeasurer.measure(tapText, tapStyle.copy(color = Color.White.copy(alpha = alpha)))
        scope.drawText(tapResult, topLeft = Offset(w / 2f - tapResult.size.width / 2f, h * 0.6f))

        // Best score
        if (state.bestScore > 0) {
            val bestText = "BEST: ${state.bestScore}"
            val bestStyle = TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Monospace)
            val bestResult = textMeasurer.measure(bestText, bestStyle.copy(color = goldColor.copy(alpha = 0.7f)))
            scope.drawText(bestResult, topLeft = Offset(w / 2f - bestResult.size.width / 2f, h * 0.67f))
        }

        // Instructions
        val inst = "탭하여 중력 반전"
        val instStyle = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        val instResult = textMeasurer.measure(inst, instStyle.copy(color = Color.White.copy(alpha = 0.4f)))
        scope.drawText(instResult, topLeft = Offset(w / 2f - instResult.size.width / 2f, h * 0.75f))
    }

    private fun drawDeathOverlay(
        scope: DrawScope, state: GameState, w: Float, h: Float,
        textMeasurer: TextMeasurer
    ) {
        val alpha = (state.deathTimer / 1.5f).coerceIn(0f, 0.6f)
        scope.drawRect(Color.Black.copy(alpha = alpha), size = scope.size)
    }

    private fun drawScoreOverlay(
        scope: DrawScope, state: GameState, w: Float, h: Float,
        textMeasurer: TextMeasurer
    ) {
        // Dim background
        scope.drawRect(Color.Black.copy(alpha = 0.75f), size = scope.size)

        val centerX = w / 2f
        var y = h * 0.18f

        // Game Over
        val goText = "GAME OVER"
        val goStyle = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        val goResult = textMeasurer.measure(goText, goStyle.copy(color = magentaColor))
        scope.drawText(goResult, topLeft = Offset(centerX - goResult.size.width / 2f, y))
        y += 60f

        // Score breakdown panel
        val panelLeft = w * 0.1f
        val panelRight = w * 0.9f
        val panelTop = y
        val lineHeight = 36f

        // Distance score
        drawScoreLine(scope, textMeasurer, "거리 점수", "${state.distanceScore}", panelLeft, panelRight, y, cyanColor)
        y += lineHeight

        // Crystal score
        drawScoreLine(scope, textMeasurer, "크리스탈", "${state.crystalScore}", panelLeft, panelRight, y, goldColor)
        y += lineHeight

        // Max combo
        drawScoreLine(scope, textMeasurer, "최대 콤보", "x${state.maxCombo}", panelLeft, panelRight, y, magentaColor)
        y += lineHeight

        // Crystals collected
        drawScoreLine(scope, textMeasurer, "수집 개수", "${state.totalCrystals}개", panelLeft, panelRight, y, goldColor.copy(alpha = 0.8f))
        y += lineHeight

        // Distance
        drawScoreLine(scope, textMeasurer, "비행 거리", "${state.distance.toInt()}m", panelLeft, panelRight, y, cyanColor.copy(alpha = 0.8f))
        y += lineHeight + 8f

        // Divider
        scope.drawLine(Color.White.copy(alpha = 0.2f), Offset(panelLeft, y), Offset(panelRight, y), 1f)
        y += 16f

        // Total score
        val totalText = "TOTAL"
        val totalStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        val totalResult = textMeasurer.measure(totalText, totalStyle.copy(color = Color.White))
        scope.drawText(totalResult, topLeft = Offset(panelLeft, y))

        val scoreText = "${state.score}"
        val scoreStyle = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        val scoreResult = textMeasurer.measure(scoreText, scoreStyle.copy(color = cyanColor))
        scope.drawText(scoreResult, topLeft = Offset(panelRight - scoreResult.size.width, y - 4f))
        y += 50f

        // Best score
        if (state.score >= state.bestScore && state.bestScore > 0) {
            val newBest = "★ NEW BEST! ★"
            val nbStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            val nbResult = textMeasurer.measure(newBest, nbStyle.copy(color = goldColor))
            scope.drawText(nbResult, topLeft = Offset(centerX - nbResult.size.width / 2f, y))
            y += 40f
        } else if (state.bestScore > 0) {
            val bestText = "BEST: ${state.bestScore}"
            val bStyle = TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Monospace)
            val bResult = textMeasurer.measure(bestText, bStyle.copy(color = Color.White.copy(alpha = 0.5f)))
            scope.drawText(bResult, topLeft = Offset(centerX - bResult.size.width / 2f, y))
            y += 40f
        }

        // Tap to retry
        val retryText = "TAP TO RETRY"
        val retryStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        val retryResult = textMeasurer.measure(retryText, retryStyle.copy(color = Color.White.copy(alpha = 0.8f)))
        scope.drawText(retryResult, topLeft = Offset(centerX - retryResult.size.width / 2f, h * 0.82f))
    }

    private fun drawScoreLine(
        scope: DrawScope, textMeasurer: TextMeasurer,
        label: String, value: String,
        left: Float, right: Float, y: Float,
        valueColor: Color
    ) {
        val labelStyle = TextStyle(fontSize = 15.sp, fontFamily = FontFamily.Monospace)
        val valueStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

        val labelResult = textMeasurer.measure(label, labelStyle.copy(color = Color.White.copy(alpha = 0.7f)))
        scope.drawText(labelResult, topLeft = Offset(left, y))

        val valueResult = textMeasurer.measure(value, valueStyle.copy(color = valueColor))
        scope.drawText(valueResult, topLeft = Offset(right - valueResult.size.width, y))
    }

    private fun drawZoneAnnouncement(
        scope: DrawScope, state: GameState, w: Float, h: Float,
        textMeasurer: TextMeasurer
    ) {
        val alpha = (state.zoneFlash / 2.5f).coerceIn(0f, 1f)
        val text = "— ZONE ${state.zoneNumber} —"
        val style = TextStyle(
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        val result = textMeasurer.measure(text, style.copy(color = magentaColor.copy(alpha = alpha)))

        // Glow
        scope.drawRect(
            Brush.radialGradient(
                listOf(magentaColor.copy(alpha = alpha * 0.1f), Color.Transparent),
                center = Offset(w / 2f, h * 0.45f),
                radius = 150f
            ),
            Offset(w / 2f - 150f, h * 0.45f - 40f),
            Size(300f, 80f)
        )

        scope.drawText(result, topLeft = Offset(w / 2f - result.size.width / 2f, h * 0.43f))
    }

    // --- Utility ---

    private fun drawTextWithShadow(
        scope: DrawScope, textMeasurer: TextMeasurer,
        text: String, offset: Offset,
        fontSize: androidx.compose.ui.unit.TextUnit,
        color: Color
    ) {
        val style = TextStyle(
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        // Shadow
        scope.drawText(
            textMeasurer.measure(text, style.copy(color = Color.Black.copy(alpha = 0.5f))),
            topLeft = Offset(offset.x + 1f, offset.y + 1f)
        )
        // Main
        scope.drawText(
            textMeasurer.measure(text, style.copy(color = color)),
            topLeft = offset
        )
    }
}
