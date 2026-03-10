package com.getaway.nightheist.game

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

    fun render(drawScope: DrawScope, state: GameState) {
        with(drawScope) {
            val canvasW = size.width
            val canvasH = size.height

            // Calculate tile size to fit screen width
            val tileSize = canvasW / 10f // Show ~10 tiles across
            val offsetX = canvasW / 2f - state.cameraX * tileSize
            val offsetY = canvasH / 2f - state.cameraY * tileSize

            // Draw background
            drawRect(COLOR_BG, Offset.Zero, size)

            // Draw tiles
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
                            // Edge highlight
                            if (state.map.tileAt(x, y - 1) != TileType.WALL) {
                                drawRect(COLOR_WALL_EDGE, Offset(screenX, screenY), Size(tileSize, 2f))
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
                            drawRect(exitColor.copy(alpha = 0.3f), Offset(screenX, screenY), tileRect)
                            // Pulsing border
                            if (state.exitUnlocked) {
                                val pulse = (sin(state.timeElapsed * 4f) * 0.3f + 0.7f).toFloat()
                                drawRect(
                                    exitColor.copy(alpha = pulse),
                                    Offset(screenX + 2, screenY + 2),
                                    Size(tileSize - 4, tileSize - 4),
                                    style = Stroke(3f)
                                )
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

            // Draw cop vision cones
            for (cop in state.cops) {
                drawVisionCone(this, cop, tileSize, offsetX, offsetY)
            }

            // Draw cops
            for (cop in state.cops) {
                drawCop(this, cop, tileSize, offsetX, offsetY)
            }

            // Draw player
            drawPlayer(this, state, tileSize, offsetX, offsetY)

            // Draw particles
            for (particle in state.particles) {
                drawParticle(this, particle, tileSize, offsetX, offsetY)
            }

            // Draw floating texts
            // (Simple version - draw as small colored indicators since Canvas text is limited)

            // Draw HUD
            drawHUD(this, state, canvasW, canvasH)

            // Draw joystick
            if (state.joystickActive) {
                drawJoystick(this, state)
            }

            // Draw warning edge glow
            if (state.spotWarning > 0.05f) {
                drawWarningOverlay(this, state, canvasW, canvasH)
            }

            // Draw minimap
            drawMinimap(this, state, canvasW, canvasH)
        }
    }

    private fun drawLoot(scope: DrawScope, cx: Float, cy: Float, tileSize: Float, time: Float) {
        val bobY = sin(time * 3f) * tileSize * 0.05f
        val size = tileSize * 0.25f
        val sparkle = (sin(time * 5f) * 0.3f + 0.7f).toFloat()
        scope.drawCircle(
            COLOR_LOOT.copy(alpha = sparkle),
            radius = size,
            center = Offset(cx, cy + bobY)
        )
        // Inner bright spot
        scope.drawCircle(
            Color.White.copy(alpha = sparkle * 0.6f),
            radius = size * 0.4f,
            center = Offset(cx - size * 0.2f, cy + bobY - size * 0.2f)
        )
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

        val bodyColor = when (cop.state) {
            CopState.PATROL -> COLOR_COP_PATROL
            CopState.ALERT -> COLOR_COP_ALERT
            CopState.CHASE -> COLOR_COP_CHASE
            CopState.SEARCH -> COLOR_COP_SEARCH
        }

        // Body
        scope.drawCircle(bodyColor, radius, Offset(cx, cy))
        // Direction indicator
        val dirX = cos(cop.facingAngle).toFloat() * radius * 0.8f
        val dirY = sin(cop.facingAngle).toFloat() * radius * 0.8f
        scope.drawCircle(Color.White.copy(alpha = 0.8f), radius * 0.25f, Offset(cx + dirX, cy + dirY))

        // Alert/chase indicator
        if (cop.state == CopState.ALERT || cop.state == CopState.CHASE) {
            scope.drawCircle(
                bodyColor.copy(alpha = 0.3f),
                radius * 1.8f,
                Offset(cx, cy),
                style = Stroke(2f)
            )
        }
    }

    private fun drawPlayer(scope: DrawScope, state: GameState, tileSize: Float, ox: Float, oy: Float) {
        val p = state.player
        val cx = p.x * tileSize + ox
        val cy = p.y * tileSize + oy
        val radius = GameState.PLAYER_RADIUS * tileSize

        val color = if (p.isHiding) COLOR_PLAYER_HIDING else COLOR_PLAYER

        // Glow
        if (!p.isHiding) {
            scope.drawCircle(color.copy(alpha = 0.15f), radius * 2f, Offset(cx, cy))
        }
        // Body
        scope.drawCircle(color, radius, Offset(cx, cy))
        // Eye/direction indicator
        if (p.moveDir != Offset.Zero) {
            val dirX = p.moveDir.x * radius * 0.5f
            val dirY = p.moveDir.y * radius * 0.5f
            scope.drawCircle(Color.White.copy(alpha = 0.9f), radius * 0.2f, Offset(cx + dirX, cy + dirY))
        } else {
            scope.drawCircle(Color.White.copy(alpha = 0.7f), radius * 0.15f, Offset(cx, cy))
        }

        // Hiding indicator
        if (p.isHiding) {
            scope.drawCircle(
                COLOR_HIDESPOT_BORDER.copy(alpha = 0.5f),
                radius * 1.5f,
                Offset(cx, cy),
                style = Stroke(2f)
            )
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
        }
        scope.drawCircle(
            color.copy(alpha = p.life.coerceIn(0f, 1f)),
            radius = 3f + p.life * 4f,
            center = Offset(sx, sy)
        )
    }

    private fun drawHUD(scope: DrawScope, state: GameState, w: Float, h: Float) {
        val hudY = 40f
        val barHeight = 4f

        // Loot progress bar
        val barWidth = w * 0.4f
        val barX = (w - barWidth) / 2f
        val progress = if (state.totalLoot > 0) state.lootCollected.toFloat() / state.totalLoot else 0f

        scope.drawRect(Color(0x40FFFFFF), Offset(barX, hudY), Size(barWidth, barHeight))
        scope.drawRect(COLOR_LOOT, Offset(barX, hudY), Size(barWidth * progress, barHeight))

        // Loot count circles
        val dotSize = 8f
        val dotSpacing = 18f
        val dotsStartX = (w - (state.totalLoot * dotSpacing)) / 2f
        for (i in 0 until state.totalLoot) {
            val dotColor = if (i < state.lootCollected) COLOR_LOOT else Color(0x40FFFFFF)
            scope.drawCircle(dotColor, dotSize / 2, Offset(dotsStartX + i * dotSpacing + dotSize / 2, hudY + 20f))
        }

        // Level indicator (left)
        scope.drawCircle(COLOR_PLAYER, 12f, Offset(30f, hudY + 10f))
        // Small level number dots
        for (i in 0 until state.level.coerceAtMost(10)) {
            scope.drawCircle(COLOR_PLAYER.copy(alpha = 0.6f), 3f, Offset(50f + i * 10f, hudY + 10f))
        }

        // Lives (right)
        for (i in 0 until state.lives) {
            scope.drawCircle(COLOR_PLAYER, 8f, Offset(w - 30f - i * 22f, hudY + 10f))
        }

        // Score
        // Draw as a series of dots representing score magnitude
        val scoreDots = (state.score / 100).coerceAtMost(20)
        for (i in 0 until scoreDots) {
            scope.drawCircle(COLOR_LOOT.copy(alpha = 0.5f), 3f, Offset(30f + i * 8f, hudY + 30f))
        }

        // Timer warning (backup approaching)
        if (!state.backupArrived) {
            val remaining = (state.backupTimer - state.timeElapsed).coerceAtLeast(0f)
            val timerProgress = remaining / state.backupTimer
            val timerColor = if (remaining < 10f) COLOR_WARNING.copy(
                alpha = (sin(state.timeElapsed * 6f) * 0.3f + 0.7f).toFloat()
            ) else Color(0x60FFFFFF)
            scope.drawRect(
                timerColor,
                Offset(barX, hudY + 40f),
                Size(barWidth * timerProgress, 3f)
            )
        }

        // Exit unlocked indicator
        if (state.exitUnlocked) {
            val pulse = (sin(state.timeElapsed * 3f) * 0.3f + 0.7f).toFloat()
            scope.drawCircle(
                COLOR_EXIT_OPEN.copy(alpha = pulse),
                15f,
                Offset(w / 2f, hudY + 55f)
            )
            // Arrow pointing toward exit
            val ex = state.map.exitPoint
            val dx = (ex.x + 0.5f) - state.player.x
            val dy = (ex.y + 0.5f) - state.player.y
            val angle = atan2(dy, dx)
            val arrowLen = 10f
            scope.drawLine(
                COLOR_EXIT_OPEN.copy(alpha = pulse),
                Offset(w / 2f + cos(angle) * 8f, hudY + 55f + sin(angle) * 8f),
                Offset(w / 2f + cos(angle) * (8f + arrowLen), hudY + 55f + sin(angle) * (8f + arrowLen)),
                strokeWidth = 2f
            )
        }
    }

    private fun drawJoystick(scope: DrawScope, state: GameState) {
        val center = state.joystickCenter
        val drag = state.joystickDrag
        val maxRadius = 60f

        // Outer ring
        scope.drawCircle(
            Color.White.copy(alpha = 0.12f),
            maxRadius,
            center
        )
        scope.drawCircle(
            Color.White.copy(alpha = 0.2f),
            maxRadius,
            center,
            style = Stroke(2f)
        )

        // Inner thumb
        val dx = drag.x - center.x
        val dy = drag.y - center.y
        val dist = sqrt(dx * dx + dy * dy).coerceAtMost(maxRadius)
        val angle = atan2(dy, dx)
        val thumbX = center.x + cos(angle) * dist
        val thumbY = center.y + sin(angle) * dist

        scope.drawCircle(
            Color.White.copy(alpha = 0.35f),
            22f,
            Offset(thumbX.toFloat(), thumbY.toFloat())
        )
    }

    private fun drawWarningOverlay(scope: DrawScope, state: GameState, w: Float, h: Float) {
        val alpha = (state.spotWarning * 0.25f).coerceAtMost(0.3f)
        // Edge vignette in red
        val edgeWidth = 30f
        // Top
        scope.drawRect(COLOR_WARNING.copy(alpha = alpha), Offset.Zero, Size(w, edgeWidth))
        // Bottom
        scope.drawRect(COLOR_WARNING.copy(alpha = alpha), Offset(0f, h - edgeWidth), Size(w, edgeWidth))
        // Left
        scope.drawRect(COLOR_WARNING.copy(alpha = alpha), Offset.Zero, Size(edgeWidth, h))
        // Right
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

        // Background
        scope.drawRect(
            Color.Black.copy(alpha = 0.6f),
            Offset(miniX - 2, miniY - 2),
            Size(miniW + 4, miniH + 4)
        )

        // Tiles
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
                scope.drawRect(
                    color,
                    Offset(miniX + x * miniScale, miniY + y * miniScale),
                    Size(miniScale, miniScale)
                )
            }
        }

        // Player dot
        scope.drawCircle(
            COLOR_PLAYER,
            3f,
            Offset(miniX + state.player.x * miniScale, miniY + state.player.y * miniScale)
        )

        // Cop dots (only visible ones)
        for (cop in state.cops) {
            val copColor = when (cop.state) {
                CopState.CHASE -> COLOR_COP_CHASE
                CopState.ALERT -> COLOR_COP_ALERT
                else -> COLOR_COP_PATROL.copy(alpha = 0.6f)
            }
            scope.drawCircle(
                copColor,
                2f,
                Offset(miniX + cop.x * miniScale, miniY + cop.y * miniScale)
            )
        }
    }

    fun renderOverlay(scope: DrawScope, state: GameState) {
        with(scope) {
            val w = size.width
            val h = size.height

            when (state.phase) {
                GamePhase.CAUGHT -> {
                    drawRect(Color.Black.copy(alpha = 0.7f), Offset.Zero, size)
                    // Red flash
                    drawRect(COLOR_WARNING.copy(alpha = 0.15f), Offset.Zero, size)
                    // Central indicator
                    drawCircle(COLOR_WARNING, 40f, Offset(w / 2, h / 2 - 50f))
                    // Lives remaining dots
                    for (i in 0 until state.lives) {
                        drawCircle(COLOR_PLAYER, 10f, Offset(w / 2 - 20f + i * 25f, h / 2 + 20f))
                    }
                    // Tap to continue hint
                    drawCircle(Color.White.copy(alpha = 0.3f), 25f, Offset(w / 2, h / 2 + 80f))
                }
                GamePhase.GAME_OVER -> {
                    drawRect(Color.Black.copy(alpha = 0.85f), Offset.Zero, size)
                    drawCircle(COLOR_WARNING, 50f, Offset(w / 2, h / 2 - 80f))
                    // Score dots
                    val dots = (state.totalScore / 100).coerceAtMost(30)
                    for (i in 0 until dots) {
                        drawCircle(COLOR_LOOT, 4f, Offset(w / 2 - dots * 5f + i * 10f, h / 2))
                    }
                    // Tap to menu hint
                    drawCircle(Color.White.copy(alpha = 0.3f), 25f, Offset(w / 2, h / 2 + 80f))
                }
                GamePhase.LEVEL_COMPLETE -> {
                    drawRect(Color.Black.copy(alpha = 0.6f), Offset.Zero, size)
                    drawCircle(COLOR_EXIT_OPEN, 50f, Offset(w / 2, h / 2 - 80f))
                    // Score dots
                    val dots = (state.score / 100).coerceAtMost(30)
                    for (i in 0 until dots) {
                        drawCircle(COLOR_LOOT, 4f, Offset(w / 2 - dots * 5f + i * 10f, h / 2))
                    }
                    // Level dots
                    for (i in 0 until state.level) {
                        drawCircle(COLOR_PLAYER, 6f, Offset(w / 2 - state.level * 8f + i * 16f, h / 2 + 40f))
                    }
                    // Tap to continue
                    drawCircle(Color.White.copy(alpha = 0.3f), 25f, Offset(w / 2, h / 2 + 100f))
                }
                else -> {}
            }
        }
    }
}
