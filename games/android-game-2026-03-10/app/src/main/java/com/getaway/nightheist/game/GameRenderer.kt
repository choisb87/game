package com.getaway.nightheist.game

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import kotlin.math.*

object GameRenderer {

    // Color palette — dark urban neon
    private val COLOR_BG = Color(0xFF0D1117)
    private val COLOR_WALL = Color(0xFF21262D)
    private val COLOR_WALL_EDGE = Color(0xFF30363D)
    private val COLOR_FLOOR = Color(0xFF161B22)
    private val COLOR_FLOOR_ALT = Color(0xFF1A1F27)
    private val COLOR_PLAYER = Color(0xFF39D353)
    private val COLOR_PLAYER_HIDING = Color(0xFF1A6B2A)
    private val COLOR_COP_PATROL = Color(0xFF3B82F6)
    private val COLOR_COP_ALERT = Color(0xFFF59E0B)
    private val COLOR_COP_CHASE = Color(0xFFEF4444)
    private val COLOR_COP_SEARCH = Color(0xFFF97316)
    private val COLOR_VISION_PATROL = Color(0x203B82F6)
    private val COLOR_VISION_ALERT = Color(0x30F59E0B)
    private val COLOR_VISION_CHASE = Color(0x30EF4444)
    private val COLOR_LOOT = Color(0xFFFBBF24)
    private val COLOR_EXIT_LOCKED = Color(0xFF6B7280)
    private val COLOR_EXIT_OPEN = Color(0xFF10B981)
    private val COLOR_HIDESPOT = Color(0xFF1E3A5F)
    private val COLOR_HIDESPOT_BORDER = Color(0xFF2563EB)
    private val COLOR_PARTICLE_LOOT = Color(0xFFFBBF24)
    private val COLOR_PARTICLE_CAUGHT = Color(0xFFEF4444)
    private val COLOR_TEXT = Color(0xFFE6EDF3)
    private val COLOR_WARNING = Color(0xFFEF4444)
    private val COLOR_COMBO = Color(0xFFFF6B6B)
    private val COLOR_POWERUP_SMOKE = Color(0xFF8B5CF6)
    private val COLOR_POWERUP_SPEED = Color(0xFF06B6D4)
    private val COLOR_POWERUP_GHOST = Color(0xFFA78BFA)

    // Text paint (reused)
    private val textPaint = Paint().apply {
        isAntiAlias = true
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }

    private fun drawText(
        canvas: android.graphics.Canvas,
        text: String, x: Float, y: Float,
        size: Float, color: Color,
        align: Paint.Align = Paint.Align.CENTER,
        alpha: Float = 1f
    ) {
        textPaint.apply {
            textSize = size
            this.color = color.toArgb()
            textAlign = align
            this.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        }
        canvas.drawText(text, x, y, textPaint)
    }

    fun render(drawScope: DrawScope, state: GameState) {
        with(drawScope) {
            val canvasW = size.width
            val canvasH = size.height
            val nativeCanvas = drawContext.canvas.nativeCanvas

            val tileSize = canvasW / 10f
            // Screen shake offset
            val shakeX = if (state.screenShake > 0f) (Math.random().toFloat() - 0.5f) * state.screenShake * 20f else 0f
            val shakeY = if (state.screenShake > 0f) (Math.random().toFloat() - 0.5f) * state.screenShake * 20f else 0f
            val offsetX = canvasW / 2f - state.cameraX * tileSize + shakeX
            val offsetY = canvasH / 2f - state.cameraY * tileSize + shakeY

            drawRect(COLOR_BG, Offset.Zero, size)

            // Tiles
            val startTileX = ((state.cameraX - canvasW / tileSize / 2f) - 1).toInt().coerceAtLeast(0)
            val endTileX = ((state.cameraX + canvasW / tileSize / 2f) + 1).toInt().coerceAtMost(state.map.width - 1)
            val startTileY = ((state.cameraY - canvasH / tileSize / 2f) - 1).toInt().coerceAtLeast(0)
            val endTileY = ((state.cameraY + canvasH / tileSize / 2f) + 1).toInt().coerceAtMost(state.map.height - 1)

            for (y in startTileY..endTileY) {
                for (x in startTileX..endTileX) {
                    val tile = state.map.tileAt(x, y)
                    val screenX = x * tileSize + offsetX
                    val screenY = y * tileSize + offsetY
                    val tileRect = Size(tileSize, tileSize)

                    when (tile) {
                        TileType.WALL -> {
                            drawRect(COLOR_WALL, Offset(screenX, screenY), tileRect)
                            // Top edge highlight (light hits from above)
                            if (state.map.tileAt(x, y - 1) != TileType.WALL) {
                                drawRect(COLOR_WALL_EDGE, Offset(screenX, screenY), Size(tileSize, 2.5f))
                            }
                            // Bottom shadow (depth illusion)
                            if (state.map.tileAt(x, y + 1) != TileType.WALL) {
                                drawRect(Color(0xFF0A0D12), Offset(screenX, screenY + tileSize - 3f), Size(tileSize, 3f))
                            }
                            // Right shadow
                            if (state.map.tileAt(x + 1, y) != TileType.WALL) {
                                drawRect(Color(0xFF0A0D12).copy(alpha = 0.5f), Offset(screenX + tileSize - 2f, screenY), Size(2f, tileSize))
                            }
                            // Inner noise texture (subtle pattern)
                            if ((x + y * 7) % 3 == 0) {
                                drawRect(Color.White.copy(alpha = 0.015f), Offset(screenX + 2f, screenY + 2f), Size(tileSize - 4f, tileSize - 4f))
                            }
                        }
                        TileType.FLOOR -> {
                            val alt = (x + y) % 2 == 0
                            drawRect(if (alt) COLOR_FLOOR else COLOR_FLOOR_ALT, Offset(screenX, screenY), tileRect)
                        }
                        TileType.LOOT -> {
                            drawRect(COLOR_FLOOR, Offset(screenX, screenY), tileRect)
                            drawLoot(this, screenX + tileSize / 2, screenY + tileSize / 2, tileSize, state.timeElapsed)
                        }
                        TileType.EXIT -> {
                            val exitColor = if (state.exitUnlocked) COLOR_EXIT_OPEN else COLOR_EXIT_LOCKED
                            drawRect(exitColor.copy(alpha = 0.2f), Offset(screenX, screenY), tileRect)
                            if (state.exitUnlocked) {
                                val pulse = (sin(state.timeElapsed * 4f) * 0.3f + 0.7f).toFloat()
                                // Outer glow ring
                                drawCircle(exitColor.copy(alpha = pulse * 0.1f), tileSize * 1.2f, Offset(screenX + tileSize / 2, screenY + tileSize / 2))
                                // Pulsing border (double)
                                drawRect(exitColor.copy(alpha = pulse), Offset(screenX + 2, screenY + 2), Size(tileSize - 4, tileSize - 4), style = Stroke(3f))
                                drawRect(exitColor.copy(alpha = pulse * 0.5f), Offset(screenX + 5, screenY + 5), Size(tileSize - 10, tileSize - 10), style = Stroke(1.5f))
                                // Door icon (arch shape)
                                val cx = screenX + tileSize / 2
                                val cy = screenY + tileSize / 2
                                val doorW = tileSize * 0.3f
                                val doorH = tileSize * 0.35f
                                drawRect(exitColor.copy(alpha = pulse * 0.6f), Offset(cx - doorW / 2, cy - doorH / 4), Size(doorW, doorH))
                                // Arch top
                                drawArc(exitColor.copy(alpha = pulse * 0.6f), startAngle = 180f, sweepAngle = 180f, useCenter = true, topLeft = Offset(cx - doorW / 2, cy - doorH / 2 - doorH / 4), size = Size(doorW, doorH / 2))
                            } else {
                                // Locked: X mark
                                val cx = screenX + tileSize / 2
                                val cy = screenY + tileSize / 2
                                val s = tileSize * 0.15f
                                drawLine(exitColor.copy(alpha = 0.4f), Offset(cx - s, cy - s), Offset(cx + s, cy + s), 2f)
                                drawLine(exitColor.copy(alpha = 0.4f), Offset(cx + s, cy - s), Offset(cx - s, cy + s), 2f)
                            }
                        }
                        TileType.HIDESPOT -> {
                            drawRect(COLOR_HIDESPOT, Offset(screenX, screenY), tileRect)
                            drawRect(
                                COLOR_HIDESPOT_BORDER.copy(alpha = 0.4f),
                                Offset(screenX + 1, screenY + 1),
                                Size(tileSize - 2, tileSize - 2),
                                style = Stroke(1.5f)
                            )
                        }
                    }
                }
            }

            // Power-ups on map
            for (pu in state.powerUps) {
                drawPowerUp(this, pu, tileSize, offsetX, offsetY, state.timeElapsed)
            }

            // Vision cones
            for (cop in state.cops) {
                if (cop.stunTimer <= 0f) {
                    drawVisionCone(this, cop, tileSize, offsetX, offsetY)
                }
            }

            // Cops
            for (cop in state.cops) {
                drawCop(this, cop, tileSize, offsetX, offsetY)
            }

            // Player
            drawPlayer(this, state, tileSize, offsetX, offsetY)

            // Particles
            for (particle in state.particles) {
                drawParticle(this, particle, tileSize, offsetX, offsetY)
            }

            // Floating texts
            for (ft in state.floatingTexts) {
                val ftX = ft.x * tileSize + offsetX
                val ftY = ft.y * tileSize + offsetY
                val alpha = ft.life.coerceIn(0f, 1f)
                val ftSize = tileSize * 0.35f
                drawText(nativeCanvas, ft.text, ftX, ftY, ftSize, COLOR_TEXT, alpha = alpha)
            }

            // HUD
            drawHUD(this, state, canvasW, canvasH, nativeCanvas)

            // Joystick
            if (state.joystickActive) {
                drawJoystick(this, state)
            }

            // Warning overlay
            if (state.spotWarning > 0.05f) {
                drawWarningOverlay(this, state, canvasW, canvasH)
            }

            // Minimap
            drawMinimap(this, state, canvasW, canvasH)

            // Active power-up indicator
            if (state.activePowerUp != null) {
                drawActivePowerUp(this, state, canvasW, canvasH, nativeCanvas)
            }

            // Combo indicator
            if (state.comboCount > 1 && state.comboTimer > 0f) {
                drawComboIndicator(this, state, canvasW, canvasH, nativeCanvas)
            }
        }
    }

    private fun drawLoot(scope: DrawScope, cx: Float, cy: Float, tileSize: Float, time: Float) {
        val bobY = sin(time * 3f) * tileSize * 0.05f
        val size = tileSize * 0.3f
        val sparkle = (sin(time * 5f) * 0.3f + 0.7f).toFloat()
        val y = cy + bobY

        // Outer glow
        scope.drawCircle(COLOR_LOOT.copy(alpha = sparkle * 0.15f), radius = size * 2.2f, center = Offset(cx, y))

        // Diamond shape (gem cut look)
        val gemPath = Path().apply {
            moveTo(cx, y - size)           // top
            lineTo(cx + size * 0.8f, y - size * 0.2f) // top-right facet
            lineTo(cx + size, y + size * 0.1f)  // right
            lineTo(cx, y + size)           // bottom point
            lineTo(cx - size, y + size * 0.1f)  // left
            lineTo(cx - size * 0.8f, y - size * 0.2f) // top-left facet
            close()
        }
        scope.drawPath(gemPath, COLOR_LOOT.copy(alpha = sparkle))

        // Inner facet (left half lighter)
        val facetLeft = Path().apply {
            moveTo(cx, y - size)
            lineTo(cx, y + size)
            lineTo(cx - size, y + size * 0.1f)
            lineTo(cx - size * 0.8f, y - size * 0.2f)
            close()
        }
        scope.drawPath(facetLeft, Color.White.copy(alpha = sparkle * 0.15f))

        // Top facet highlight
        val facetTop = Path().apply {
            moveTo(cx, y - size)
            lineTo(cx + size * 0.3f, y - size * 0.3f)
            lineTo(cx - size * 0.3f, y - size * 0.3f)
            close()
        }
        scope.drawPath(facetTop, Color.White.copy(alpha = sparkle * 0.4f))

        // Sparkle star
        val starAlpha = ((sin(time * 7f + cx) + 1f) / 2f * 0.8f).toFloat()
        val starSize = size * 0.2f
        scope.drawLine(Color.White.copy(alpha = starAlpha), Offset(cx - starSize, y - size * 0.4f), Offset(cx + starSize, y - size * 0.4f), 1.5f)
        scope.drawLine(Color.White.copy(alpha = starAlpha), Offset(cx, y - size * 0.4f - starSize), Offset(cx, y - size * 0.4f + starSize), 1.5f)
    }

    private fun drawPowerUp(scope: DrawScope, pu: PowerUpItem, tileSize: Float, ox: Float, oy: Float, time: Float) {
        val cx = pu.x * tileSize + ox
        val cy = pu.y * tileSize + oy
        val bobY = sin(time * 2.5f + pu.x) * tileSize * 0.06f
        val size = tileSize * 0.3f
        val pulse = (sin(time * 4f) * 0.2f + 0.8f).toFloat()

        val color = when (pu.type) {
            PowerUpType.SMOKE -> COLOR_POWERUP_SMOKE
            PowerUpType.SPEED -> COLOR_POWERUP_SPEED
            PowerUpType.GHOST -> COLOR_POWERUP_GHOST
        }

        // Glow ring
        scope.drawCircle(color.copy(alpha = 0.15f * pulse), radius = size * 2.2f, center = Offset(cx, cy + bobY))
        // Outer ring
        scope.drawCircle(color.copy(alpha = 0.5f * pulse), radius = size * 1.2f, center = Offset(cx, cy + bobY), style = Stroke(2f))
        // Body
        scope.drawCircle(color.copy(alpha = pulse), radius = size, center = Offset(cx, cy + bobY))
        // Icon indicator (diamond shape for power-ups)
        val iconSize = size * 0.5f
        val path = Path().apply {
            moveTo(cx, cy + bobY - iconSize)
            lineTo(cx + iconSize, cy + bobY)
            lineTo(cx, cy + bobY + iconSize)
            lineTo(cx - iconSize, cy + bobY)
            close()
        }
        scope.drawPath(path, Color.White.copy(alpha = 0.8f * pulse))
    }

    private fun drawVisionCone(scope: DrawScope, cop: Cop, tileSize: Float, ox: Float, oy: Float) {
        val cx = cop.x * tileSize + ox
        val cy = cop.y * tileSize + oy
        val range = cop.visionRange * tileSize
        val halfAngle = cop.visionAngle * PI.toFloat() / 180f

        val coneColor = when (cop.state) {
            CopState.PATROL -> COLOR_VISION_PATROL
            CopState.ALERT -> COLOR_VISION_ALERT
            CopState.CHASE -> COLOR_VISION_CHASE
            CopState.SEARCH -> COLOR_VISION_ALERT
        }

        val path = Path().apply {
            moveTo(cx, cy)
            val startAngle = cop.facingAngle - halfAngle
            val endAngle = cop.facingAngle + halfAngle
            val steps = 12
            for (i in 0..steps) {
                val a = startAngle + (endAngle - startAngle) * i / steps
                lineTo(cx + cos(a) * range, cy + sin(a) * range)
            }
            close()
        }
        scope.drawPath(path, coneColor)
    }

    private fun drawCop(scope: DrawScope, cop: Cop, tileSize: Float, ox: Float, oy: Float) {
        val cx = cop.x * tileSize + ox
        val cy = cop.y * tileSize + oy
        val radius = GameState.COP_RADIUS * tileSize

        val bodyColor = when {
            cop.stunTimer > 0f -> COLOR_POWERUP_SMOKE.copy(alpha = 0.5f)
            else -> when (cop.state) {
                CopState.PATROL -> COLOR_COP_PATROL
                CopState.ALERT -> COLOR_COP_ALERT
                CopState.CHASE -> COLOR_COP_CHASE
                CopState.SEARCH -> COLOR_COP_SEARCH
            }
        }

        scope.drawCircle(bodyColor, radius, Offset(cx, cy))
        // Direction indicator
        val dirX = cos(cop.facingAngle).toFloat() * radius * 0.8f
        val dirY = sin(cop.facingAngle).toFloat() * radius * 0.8f
        scope.drawCircle(Color.White.copy(alpha = 0.8f), radius * 0.25f, Offset(cx + dirX, cy + dirY))

        // Alert/chase ring
        if (cop.state == CopState.ALERT || cop.state == CopState.CHASE) {
            scope.drawCircle(
                bodyColor.copy(alpha = 0.3f),
                radius * 1.8f,
                Offset(cx, cy),
                style = Stroke(2f)
            )
        }

        // Stun visual
        if (cop.stunTimer > 0f) {
            val stunAlpha = (cop.stunTimer / 3f).coerceIn(0f, 0.6f)
            scope.drawCircle(COLOR_POWERUP_SMOKE.copy(alpha = stunAlpha), radius * 2.5f, Offset(cx, cy))
        }
    }

    private fun drawPlayer(scope: DrawScope, state: GameState, tileSize: Float, ox: Float, oy: Float) {
        val p = state.player
        val cx = p.x * tileSize + ox
        val cy = p.y * tileSize + oy
        val radius = GameState.PLAYER_RADIUS * tileSize

        val isGhost = state.activePowerUp == PowerUpType.GHOST
        val isSpeed = state.activePowerUp == PowerUpType.SPEED
        val color = when {
            isGhost -> COLOR_POWERUP_GHOST.copy(alpha = 0.5f)
            p.isHiding -> COLOR_PLAYER_HIDING
            else -> COLOR_PLAYER
        }

        // Glow
        if (!p.isHiding) {
            val glowColor = if (isSpeed) COLOR_POWERUP_SPEED else if (isGhost) COLOR_POWERUP_GHOST else color
            scope.drawCircle(glowColor.copy(alpha = 0.12f), radius * 2.5f, Offset(cx, cy))
            scope.drawCircle(glowColor.copy(alpha = 0.08f), radius * 3.5f, Offset(cx, cy))
        }

        // Sneak indicator (crouching visual)
        if (!p.isRunning && !p.isHiding && p.moveDir != Offset.Zero) {
            scope.drawCircle(COLOR_PLAYER.copy(alpha = 0.06f), radius * 1.3f, Offset(cx, cy))
        }

        // Speed trail
        if (isSpeed && p.moveDir != Offset.Zero) {
            for (i in 1..4) {
                val trailX = cx - p.moveDir.x * radius * i * 0.7f
                val trailY = cy - p.moveDir.y * radius * i * 0.7f
                scope.drawCircle(COLOR_POWERUP_SPEED.copy(alpha = 0.25f / i), radius * (1f - i * 0.12f), Offset(trailX, trailY))
            }
        }

        // Body (head)
        scope.drawCircle(color, radius, Offset(cx, cy))
        // Body outline for definition
        scope.drawCircle(color.copy(alpha = 0.6f), radius, Offset(cx, cy), style = Stroke(1.5f))

        // Mask band (thief look)
        val maskY = cy - radius * 0.1f
        val maskH = radius * 0.35f
        scope.drawRect(
            Color(0xFF0D1117).copy(alpha = if (isGhost) 0.3f else 0.85f),
            Offset(cx - radius * 0.85f, maskY - maskH / 2),
            Size(radius * 1.7f, maskH)
        )

        // Eyes on mask
        val eyeRadius = radius * 0.13f
        val eyeY = maskY
        val eyeSpacing = radius * 0.35f
        val eyeColor = if (isGhost) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.95f)

        if (p.moveDir != Offset.Zero) {
            val lookOffsetX = p.moveDir.x * radius * 0.08f
            val lookOffsetY = p.moveDir.y * radius * 0.08f
            scope.drawCircle(eyeColor, eyeRadius, Offset(cx - eyeSpacing + lookOffsetX, eyeY + lookOffsetY))
            scope.drawCircle(eyeColor, eyeRadius, Offset(cx + eyeSpacing + lookOffsetX, eyeY + lookOffsetY))
        } else {
            scope.drawCircle(eyeColor, eyeRadius, Offset(cx - eyeSpacing, eyeY))
            scope.drawCircle(eyeColor, eyeRadius, Offset(cx + eyeSpacing, eyeY))
        }

        // Loot bag (small sack on back when carrying loot)
        if (state.lootCollected > 0) {
            val bagOffsetX = if (p.moveDir != Offset.Zero) -p.moveDir.x * radius * 0.4f else radius * 0.5f
            val bagOffsetY = if (p.moveDir != Offset.Zero) -p.moveDir.y * radius * 0.4f else radius * 0.3f
            val bagSize = radius * (0.35f + state.lootCollected * 0.03f).coerceAtMost(0.55f)
            scope.drawCircle(Color(0xFFA0855C), bagSize, Offset(cx + bagOffsetX, cy + bagOffsetY))
            scope.drawCircle(Color(0xFF8B7345), bagSize, Offset(cx + bagOffsetX, cy + bagOffsetY), style = Stroke(1f))
        }

        // Ghost shimmer
        if (isGhost) {
            val shimmer = (sin(state.timeElapsed * 8f) * 0.3f + 0.3f).toFloat()
            scope.drawCircle(Color.White.copy(alpha = shimmer), radius * 1.3f, Offset(cx, cy), style = Stroke(2f))
        }

        // Hiding indicator
        if (p.isHiding) {
            val hidePulse = (sin(state.timeElapsed * 3f) * 0.2f + 0.4f).toFloat()
            scope.drawCircle(
                COLOR_HIDESPOT_BORDER.copy(alpha = hidePulse),
                radius * 1.6f,
                Offset(cx, cy),
                style = Stroke(2.5f)
            )
            // "Z" sleep indicator
            val zAlpha = (sin(state.timeElapsed * 2f) * 0.3f + 0.5f).toFloat()
            val zOffset = sin(state.timeElapsed * 1.5f).toFloat() * 3f
            scope.drawContext.canvas.nativeCanvas.let { canvas ->
                drawText(canvas, "z", cx + radius * 1.2f, cy - radius * 0.8f + zOffset, radius * 0.5f, COLOR_HIDESPOT_BORDER, alpha = zAlpha)
            }
        }
    }

    private fun drawParticle(scope: DrawScope, p: Particle, tileSize: Float, ox: Float, oy: Float) {
        val sx = p.x * tileSize + ox
        val sy = p.y * tileSize + oy
        val color = when (p.type) {
            ParticleType.LOOT -> COLOR_PARTICLE_LOOT
            ParticleType.CAUGHT -> COLOR_PARTICLE_CAUGHT
            ParticleType.ALERT -> COLOR_COP_ALERT
            ParticleType.ESCAPE -> COLOR_EXIT_OPEN
            ParticleType.POWERUP -> COLOR_POWERUP_SMOKE
            ParticleType.COMBO -> COLOR_COMBO
        }
        scope.drawCircle(
            color.copy(alpha = p.life.coerceIn(0f, 1f)),
            radius = 3f + p.life * 5f,
            center = Offset(sx, sy)
        )
    }

    private fun drawHUD(scope: DrawScope, state: GameState, w: Float, h: Float, canvas: android.graphics.Canvas) {
        val hudY = 50f
        val barHeight = 5f

        // Top bar background
        scope.drawRect(Color.Black.copy(alpha = 0.4f), Offset.Zero, Size(w, hudY + 80f))

        // Level text (left)
        drawText(canvas, "LV.${state.level}", 20f, hudY + 8f, 32f, COLOR_PLAYER, Paint.Align.LEFT)

        // Lives (right)
        val livesText = when {
            state.lives >= 3 -> "♥♥♥"
            state.lives == 2 -> "♥♥♡"
            state.lives == 1 -> "♥♡♡"
            else -> "♡♡♡"
        }
        drawText(canvas, livesText, w - 20f, hudY + 8f, 28f, COLOR_WARNING, Paint.Align.RIGHT)

        // Loot progress bar (center)
        val barWidth = w * 0.4f
        val barX = (w - barWidth) / 2f
        val progress = if (state.totalLoot > 0) state.lootCollected.toFloat() / state.totalLoot else 0f

        scope.drawRect(Color(0x40FFFFFF), Offset(barX, hudY), Size(barWidth, barHeight))
        if (progress > 0f) {
            scope.drawRect(COLOR_LOOT, Offset(barX, hudY), Size(barWidth * progress, barHeight))
        }

        // Loot count text
        drawText(canvas, "${state.lootCollected}/${state.totalLoot}", w / 2f, hudY + 26f, 24f, COLOR_LOOT)

        // Score
        drawText(canvas, "${state.score}", w / 2f, hudY + 50f, 20f, Color(0xFFAAAAAA))

        // Backup timer
        if (!state.backupArrived) {
            val remaining = (state.backupTimer - state.timeElapsed).coerceAtLeast(0f)
            val timerProgress = remaining / state.backupTimer
            val isUrgent = remaining < 10f
            val timerColor = if (isUrgent) {
                COLOR_WARNING.copy(alpha = (sin(state.timeElapsed * 6f) * 0.3f + 0.7f).toFloat())
            } else {
                Color(0x60FFFFFF)
            }
            scope.drawRect(timerColor, Offset(barX, hudY + 60f), Size(barWidth * timerProgress, 3f))
            if (isUrgent) {
                drawText(canvas, "BACKUP: ${remaining.toInt()}s", w / 2f, hudY + 78f, 18f, COLOR_WARNING,
                    alpha = (sin(state.timeElapsed * 5f) * 0.3f + 0.7f).toFloat())
            }
        } else {
            drawText(canvas, "BACKUP ACTIVE", w / 2f, hudY + 78f, 16f, COLOR_WARNING, alpha = 0.6f)
        }

        // Exit indicator
        if (state.exitUnlocked) {
            val pulse = (sin(state.timeElapsed * 3f) * 0.3f + 0.7f).toFloat()
            val ex = state.map.exitPoint
            val dx = (ex.x + 0.5f) - state.player.x
            val dy = (ex.y + 0.5f) - state.player.y
            val dist = sqrt(dx * dx + dy * dy)
            val angle = atan2(dy, dx)

            // Arrow at top of screen pointing to exit
            val arrowX = w / 2f + cos(angle).toFloat() * 40f
            val arrowY = hudY + 92f
            scope.drawCircle(COLOR_EXIT_OPEN.copy(alpha = pulse), 8f, Offset(arrowX, arrowY))
            drawText(canvas, "EXIT →${dist.toInt()}m", w / 2f, hudY + 96f, 16f, COLOR_EXIT_OPEN, alpha = pulse)
        }

        // Sneak indicator
        if (!state.player.isRunning && state.player.moveDir != Offset.Zero) {
            drawText(canvas, "SNEAK", 20f, hudY + 30f, 16f, COLOR_HIDESPOT_BORDER, Paint.Align.LEFT, alpha = 0.7f)
        }
    }

    private fun drawActivePowerUp(scope: DrawScope, state: GameState, w: Float, h: Float, canvas: android.graphics.Canvas) {
        val pu = state.activePowerUp ?: return
        val timer = state.powerUpTimer

        val color = when (pu) {
            PowerUpType.SMOKE -> COLOR_POWERUP_SMOKE
            PowerUpType.SPEED -> COLOR_POWERUP_SPEED
            PowerUpType.GHOST -> COLOR_POWERUP_GHOST
        }
        val label = when (pu) {
            PowerUpType.SMOKE -> "SMOKE"
            PowerUpType.SPEED -> "SPEED"
            PowerUpType.GHOST -> "GHOST"
        }

        val y = h - 180f
        val barW = 120f
        val maxDuration = when (pu) {
            PowerUpType.SMOKE -> 3f
            PowerUpType.SPEED -> 5f
            PowerUpType.GHOST -> 3f
        }

        // Background
        scope.drawRect(Color.Black.copy(alpha = 0.5f), Offset(20f, y - 5f), Size(barW + 10f, 35f))
        // Progress bar
        scope.drawRect(color.copy(alpha = 0.3f), Offset(25f, y + 18f), Size(barW, 4f))
        scope.drawRect(color, Offset(25f, y + 18f), Size(barW * (timer / maxDuration), 4f))
        // Label
        drawText(canvas, label, 25f + barW / 2f, y + 14f, 18f, color)
    }

    private fun drawComboIndicator(scope: DrawScope, state: GameState, w: Float, h: Float, canvas: android.graphics.Canvas) {
        val combo = state.comboCount
        val timer = state.comboTimer
        val alpha = (timer / GameState.COMBO_WINDOW).coerceIn(0f, 1f)

        val y = h * 0.15f
        val scale = 1f + (combo - 1) * 0.1f
        val size = 36f * scale

        // Combo text with glow
        drawText(canvas, "x$combo COMBO!", w / 2f, y, size, COLOR_COMBO, alpha = alpha)

        // Timer bar under combo
        val barW = 80f
        scope.drawRect(COLOR_COMBO.copy(alpha = alpha * 0.3f), Offset(w / 2f - barW / 2, y + 8f), Size(barW, 3f))
        scope.drawRect(COLOR_COMBO.copy(alpha = alpha), Offset(w / 2f - barW / 2, y + 8f), Size(barW * (timer / GameState.COMBO_WINDOW), 3f))
    }

    private fun drawJoystick(scope: DrawScope, state: GameState) {
        val center = state.joystickCenter
        val drag = state.joystickDrag
        val maxRadius = 60f

        val dx = drag.x - center.x
        val dy = drag.y - center.y
        val dist = sqrt(dx * dx + dy * dy)
        val isSneaking = dist < 80f && dist > 15f

        // Outer ring
        val ringColor = if (isSneaking) COLOR_HIDESPOT_BORDER.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.12f)
        scope.drawCircle(ringColor, maxRadius, center)
        scope.drawCircle(
            if (isSneaking) COLOR_HIDESPOT_BORDER.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.2f),
            maxRadius,
            center,
            style = Stroke(2f)
        )

        // Sneak zone indicator (inner circle)
        if (dist > 15f) {
            scope.drawCircle(
                COLOR_HIDESPOT_BORDER.copy(alpha = if (isSneaking) 0.15f else 0.05f),
                maxRadius * 0.6f,
                center,
                style = Stroke(1f)
            )
        }

        // Inner thumb
        val clampedDist = dist.coerceAtMost(maxRadius)
        val angle = atan2(dy, dx)
        val thumbX = center.x + cos(angle) * clampedDist
        val thumbY = center.y + sin(angle) * clampedDist
        val thumbColor = if (isSneaking) COLOR_HIDESPOT_BORDER.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.35f)

        scope.drawCircle(thumbColor, 22f, Offset(thumbX.toFloat(), thumbY.toFloat()))
    }

    private fun drawWarningOverlay(scope: DrawScope, state: GameState, w: Float, h: Float) {
        val alpha = (state.spotWarning * 0.25f).coerceAtMost(0.3f)
        val edgeWidth = 30f
        scope.drawRect(COLOR_WARNING.copy(alpha = alpha), Offset.Zero, Size(w, edgeWidth))
        scope.drawRect(COLOR_WARNING.copy(alpha = alpha), Offset(0f, h - edgeWidth), Size(w, edgeWidth))
        scope.drawRect(COLOR_WARNING.copy(alpha = alpha), Offset.Zero, Size(edgeWidth, h))
        scope.drawRect(COLOR_WARNING.copy(alpha = alpha), Offset(w - edgeWidth, 0f), Size(edgeWidth, h))
    }

    private fun drawMinimap(scope: DrawScope, state: GameState, canvasW: Float, canvasH: Float) {
        val mapW = state.map.width
        val mapH = state.map.height
        val miniScale = 4f
        val miniW = mapW * miniScale
        val miniH = mapH * miniScale
        val miniX = canvasW - miniW - 12f
        val miniY = canvasH - miniH - 12f

        scope.drawRect(Color.Black.copy(alpha = 0.6f), Offset(miniX - 2, miniY - 2), Size(miniW + 4, miniH + 4))

        for (y in 0 until mapH) {
            for (x in 0 until mapW) {
                val tile = state.map.tileAt(x, y)
                val color = when (tile) {
                    TileType.WALL -> Color(0xFF333333)
                    TileType.FLOOR -> Color(0xFF1A1A1A)
                    TileType.LOOT -> COLOR_LOOT.copy(alpha = 0.8f)
                    TileType.EXIT -> if (state.exitUnlocked) COLOR_EXIT_OPEN.copy(alpha = 0.8f) else COLOR_EXIT_LOCKED.copy(alpha = 0.5f)
                    TileType.HIDESPOT -> COLOR_HIDESPOT.copy(alpha = 0.5f)
                }
                scope.drawRect(color, Offset(miniX + x * miniScale, miniY + y * miniScale), Size(miniScale, miniScale))
            }
        }

        // Power-ups on minimap
        for (pu in state.powerUps) {
            val puColor = when (pu.type) {
                PowerUpType.SMOKE -> COLOR_POWERUP_SMOKE
                PowerUpType.SPEED -> COLOR_POWERUP_SPEED
                PowerUpType.GHOST -> COLOR_POWERUP_GHOST
            }
            scope.drawCircle(puColor, 2.5f, Offset(miniX + pu.x * miniScale, miniY + pu.y * miniScale))
        }

        scope.drawCircle(COLOR_PLAYER, 3f, Offset(miniX + state.player.x * miniScale, miniY + state.player.y * miniScale))

        for (cop in state.cops) {
            val copColor = when (cop.state) {
                CopState.CHASE -> COLOR_COP_CHASE
                CopState.ALERT -> COLOR_COP_ALERT
                else -> COLOR_COP_PATROL.copy(alpha = 0.6f)
            }
            scope.drawCircle(copColor, 2f, Offset(miniX + cop.x * miniScale, miniY + cop.y * miniScale))
        }
    }

    fun renderOverlay(scope: DrawScope, state: GameState) {
        with(scope) {
            val w = size.width
            val h = size.height
            val nativeCanvas = drawContext.canvas.nativeCanvas

            when (state.phase) {
                GamePhase.LEVEL_INTRO -> {
                    val totalIntro = if (state.level == 1) 3.5f else 2.0f
                    val progress = 1f - (state.introTimer / totalIntro)

                    // Cinematic letterbox bars (top and bottom)
                    val barHeight = 60f * (1f - progress).coerceIn(0f, 1f)
                    drawRect(Color.Black, Offset.Zero, Size(w, barHeight))
                    drawRect(Color.Black, Offset(0f, h - barHeight), Size(w, barHeight))

                    // Dim overlay that fades as camera pans
                    drawRect(Color.Black.copy(alpha = 0.25f * (1f - progress)), Offset.Zero, size)

                    // Level number with dramatic scale
                    val levelAlpha = if (progress < 0.7f) 1f else ((1f - progress) / 0.3f).coerceIn(0f, 1f)
                    val levelScale = if (progress < 0.1f) 1f + (0.1f - progress) * 5f else 1f
                    val levelSize = 60f * levelScale

                    // Glow behind level text
                    drawCircle(COLOR_PLAYER.copy(alpha = levelAlpha * 0.08f), 120f, Offset(w / 2f, h * 0.38f))

                    drawText(nativeCanvas, "LEVEL ${state.level}", w / 2f, h * 0.4f, levelSize, COLOR_PLAYER, alpha = levelAlpha)

                    // Subtitle with more personality
                    val subtitle = when {
                        state.level == 1 -> "The heist begins..."
                        state.level == 2 -> "They won't make it easy."
                        state.level == 3 -> "Security is tightening."
                        state.level <= 5 -> "They're getting faster..."
                        state.level <= 7 -> "Only the best survive."
                        else -> "Good luck. You'll need it."
                    }
                    val subAlpha = if (progress > 0.15f) levelAlpha * 0.8f else 0f
                    drawText(nativeCanvas, subtitle, w / 2f, h * 0.4f + 42f, 22f, COLOR_TEXT, alpha = subAlpha)

                    // Mission info bar
                    if (progress > 0.2f && progress < 0.85f) {
                        val infoAlpha = ((progress - 0.2f) / 0.15f).coerceAtMost(1f) * ((0.85f - progress) / 0.15f).coerceAtMost(1f)
                        val infoY = h * 0.48f

                        // Gem count + cop count
                        drawText(nativeCanvas, "${state.totalLoot} gems  •  ${state.cops.size} cops", w / 2f, infoY, 18f, COLOR_LOOT, alpha = infoAlpha * 0.7f)
                    }

                    // Tutorial on level 1
                    if (state.level == 1 && state.introTimer > 1f) {
                        val tutAlpha = ((state.introTimer - 1f) / 1f).coerceIn(0f, 0.8f)
                        val baseY = h * 0.56f

                        // Tutorial background panel
                        drawRect(Color.Black.copy(alpha = tutAlpha * 0.5f), Offset(w * 0.08f, baseY - 20f), Size(w * 0.84f, 155f))

                        drawText(nativeCanvas, "DRAG to move", w / 2f, baseY, 20f, COLOR_TEXT, alpha = tutAlpha)
                        drawText(nativeCanvas, "Short drag = SNEAK  |  Long drag = RUN", w / 2f, baseY + 28f, 16f, COLOR_HIDESPOT_BORDER, alpha = tutAlpha)
                        drawText(nativeCanvas, "Avoid vision cones", w / 2f, baseY + 56f, 20f, COLOR_COP_ALERT, alpha = tutAlpha)
                        drawText(nativeCanvas, "Stand on blue tiles to HIDE", w / 2f, baseY + 84f, 20f, COLOR_HIDESPOT_BORDER, alpha = tutAlpha)
                        drawText(nativeCanvas, "Collect items for POWER-UPS", w / 2f, baseY + 112f, 20f, COLOR_POWERUP_SMOKE, alpha = tutAlpha)
                    }

                    // "TAP TO SKIP" hint
                    val skipAlpha = (sin(state.introTimer * 3f) * 0.2f + 0.3f).toFloat()
                    drawText(nativeCanvas, "TAP TO SKIP", w / 2f, h - barHeight - 15f, 14f, COLOR_TEXT, alpha = skipAlpha)
                }

                GamePhase.CAUGHT -> {
                    drawRect(Color.Black.copy(alpha = 0.75f), Offset.Zero, size)
                    drawRect(COLOR_WARNING.copy(alpha = 0.1f), Offset.Zero, size)

                    drawText(nativeCanvas, "CAUGHT!", w / 2f, h / 2f - 60f, 52f, COLOR_WARNING)

                    // Lives remaining
                    drawText(nativeCanvas, "${state.lives} LIVES LEFT", w / 2f, h / 2f, 28f, COLOR_PLAYER)

                    // Lives visual
                    for (i in 0 until 3) {
                        val heartColor = if (i < state.lives) COLOR_PLAYER else Color(0xFF444444)
                        drawCircle(heartColor, 12f, Offset(w / 2 - 30f + i * 30f, h / 2 + 40f))
                    }

                    drawText(nativeCanvas, "Tap to retry", w / 2f, h / 2f + 90f, 22f, COLOR_TEXT, alpha = 0.5f)
                }

                GamePhase.GAME_OVER -> {
                    drawRect(Color.Black.copy(alpha = 0.9f), Offset.Zero, size)

                    // Red warning glow
                    drawCircle(COLOR_WARNING.copy(alpha = 0.05f), 180f, Offset(w / 2f, h / 2f - 80f))

                    drawText(nativeCanvas, "GAME OVER", w / 2f, h / 2f - 100f, 56f, COLOR_WARNING)

                    // Decorative line
                    drawLine(COLOR_WARNING.copy(alpha = 0.25f), Offset(w * 0.3f, h / 2f - 75f), Offset(w * 0.7f, h / 2f - 75f), 1f)

                    // Final score panel
                    drawRect(Color.White.copy(alpha = 0.03f), Offset(w * 0.2f, h / 2f - 55f), Size(w * 0.6f, 115f))

                    drawText(nativeCanvas, "FINAL SCORE", w / 2f, h / 2f - 30f, 16f, Color(0xFF888888))
                    drawText(nativeCanvas, "${state.totalScore}", w / 2f, h / 2f + 10f, 48f, COLOR_LOOT)

                    // Best score
                    val best = maxOf(state.bestScore, state.totalScore)
                    if (state.totalScore >= state.bestScore && state.bestScore > 0) {
                        drawText(nativeCanvas, "★ NEW BEST! ★", w / 2f, h / 2f + 48f, 24f, COLOR_EXIT_OPEN)
                    } else if (best > 0) {
                        drawText(nativeCanvas, "BEST: $best", w / 2f, h / 2f + 48f, 18f, Color(0xFF666666))
                    }

                    // Level reached
                    drawText(nativeCanvas, "Level ${state.level} reached", w / 2f, h / 2f + 82f, 20f, COLOR_TEXT, alpha = 0.6f)

                    // Prompt
                    val tapAlpha = (sin(state.timeElapsed * 2f) * 0.2f + 0.5f).toFloat()
                    drawText(nativeCanvas, "TAP TO CONTINUE", w / 2f, h / 2f + 130f, 20f, COLOR_TEXT, alpha = tapAlpha)
                }

                GamePhase.LEVEL_COMPLETE -> {
                    drawRect(Color.Black.copy(alpha = 0.75f), Offset.Zero, size)

                    // Victory glow
                    drawCircle(COLOR_EXIT_OPEN.copy(alpha = 0.06f), 200f, Offset(w / 2f, h / 2f - 100f))

                    drawText(nativeCanvas, "ESCAPED!", w / 2f, h / 2f - 130f, 56f, COLOR_EXIT_OPEN)

                    // Decorative line
                    val escLineW = w * 0.35f
                    drawLine(COLOR_EXIT_OPEN.copy(alpha = 0.3f), Offset(w / 2f - escLineW / 2, h / 2f - 105f), Offset(w / 2f + escLineW / 2, h / 2f - 105f), 1f)

                    // Score breakdown panel
                    val panelY = h / 2f - 80f
                    val panelH = 200f
                    drawRect(Color.White.copy(alpha = 0.03f), Offset(w * 0.12f, panelY), Size(w * 0.76f, panelH))

                    val baseScore = state.lootCollected * 100 * state.level
                    val timeBonus = ((state.backupTimer - state.timeElapsed).coerceAtLeast(0f) * 10).toInt()
                    val stealthBonus = if (state.neverSpotted) 500 else 0
                    val comboBonus = state.maxCombo * 200

                    var lineY = h / 2f - 55f

                    // Gem icon + loot score
                    val gemIconPath = Path().apply {
                        val gx = w * 0.25f; val gy = lineY - 6f; val gs = 8f
                        moveTo(gx, gy - gs); lineTo(gx + gs * 0.7f, gy); lineTo(gx, gy + gs); lineTo(gx - gs * 0.7f, gy); close()
                    }
                    drawPath(gemIconPath, COLOR_LOOT)
                    drawText(nativeCanvas, "Loot", w * 0.32f, lineY, 20f, COLOR_LOOT, Paint.Align.LEFT)
                    drawText(nativeCanvas, "+$baseScore", w * 0.75f, lineY, 20f, COLOR_LOOT, Paint.Align.RIGHT)
                    lineY += 30f

                    if (timeBonus > 0) {
                        drawText(nativeCanvas, "Time bonus", w * 0.32f, lineY, 20f, COLOR_TEXT, Paint.Align.LEFT, alpha = 0.7f)
                        drawText(nativeCanvas, "+$timeBonus", w * 0.75f, lineY, 20f, COLOR_TEXT, Paint.Align.RIGHT, alpha = 0.7f)
                        lineY += 30f
                    }
                    if (stealthBonus > 0) {
                        drawText(nativeCanvas, "STEALTH", w * 0.32f, lineY, 20f, COLOR_PLAYER, Paint.Align.LEFT)
                        drawText(nativeCanvas, "+$stealthBonus", w * 0.75f, lineY, 20f, COLOR_PLAYER, Paint.Align.RIGHT)
                        lineY += 30f
                    }
                    if (comboBonus > 0) {
                        drawText(nativeCanvas, "Combo (x${state.maxCombo})", w * 0.32f, lineY, 20f, COLOR_COMBO, Paint.Align.LEFT)
                        drawText(nativeCanvas, "+$comboBonus", w * 0.75f, lineY, 20f, COLOR_COMBO, Paint.Align.RIGHT)
                        lineY += 30f
                    }

                    // Divider line
                    drawLine(Color.White.copy(alpha = 0.15f), Offset(w * 0.25f, lineY - 5f), Offset(w * 0.75f, lineY - 5f), 1f)

                    lineY += 15f
                    drawText(nativeCanvas, "TOTAL", w * 0.32f, lineY, 28f, COLOR_LOOT, Paint.Align.LEFT)
                    drawText(nativeCanvas, "${state.score}", w * 0.75f, lineY, 32f, COLOR_LOOT, Paint.Align.RIGHT)

                    // Next level prompt
                    val nextAlpha = (sin(state.timeElapsed * 2.5f) * 0.2f + 0.6f).toFloat()
                    drawText(nativeCanvas, "TAP FOR NEXT LEVEL  →", w / 2f, h / 2f + 160f, 20f, COLOR_EXIT_OPEN, alpha = nextAlpha)
                }
                else -> {}
            }
        }
    }
}
