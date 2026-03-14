package com.orbitshield.game.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import kotlin.math.*

fun DrawScope.drawGame(state: GameState) {
    drawBackground(state)
    drawStars(state)
    drawPowerUp(state)
    drawShieldFragments(state)
    drawAsteroids(state)
    drawPlanet(state)
    drawShield(state)
    drawParticles(state)
    drawScorePopups(state)
    drawHUD(state)

    if (state.screenFlash > 0f) {
        drawRect(
            color = Color.White.copy(alpha = state.screenFlash.coerceIn(0f, 0.6f)),
            size = size
        )
    }

    if (state.phase == GamePhase.GAME_OVER) {
        drawGameOver(state)
    }
}

private fun DrawScope.drawBackground(state: GameState) {
    // Deep space gradient
    val gradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF1A1A3E),
            Color(0xFF0D1B2E),
            Color(0xFF0A0E1A)
        ),
        center = Offset(state.centerX, state.centerY),
        radius = maxOf(state.screenWidth, state.screenHeight) * 0.7f
    )
    drawRect(brush = gradient, size = size)

    // Nebula glow behind planet
    val nebulaAlpha = 0.15f + sin(state.gameTime * 0.5f) * 0.05f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF6200EA).copy(alpha = nebulaAlpha),
                Color.Transparent
            ),
            center = Offset(state.centerX, state.centerY),
            radius = state.shieldRadius * 2.5f
        ),
        center = Offset(state.centerX, state.centerY),
        radius = state.shieldRadius * 2.5f
    )
}

private fun DrawScope.drawStars(state: GameState) {
    for (star in state.stars) {
        val twinkle = (sin(state.gameTime * star.twinkleSpeed) * 0.3f + 0.7f) * star.brightness
        drawCircle(
            color = Color.White.copy(alpha = twinkle),
            radius = star.size,
            center = Offset(star.x, star.y)
        )
    }
}

private fun DrawScope.drawPlanet(state: GameState) {
    val cx = state.centerX
    val cy = state.centerY
    val r = state.planetRadius
    val pulse = sin(state.planetPulse) * 0.15f + 1f

    // Outer glow
    val glowColor = if (state.planetHitFlash > 0f)
        Color(0xFFFF1744).copy(alpha = 0.3f * state.planetHitFlash + 0.1f)
    else
        Color(0xFFB388FF).copy(alpha = 0.15f)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(glowColor, Color.Transparent),
            center = Offset(cx, cy),
            radius = r * 3f * pulse
        ),
        center = Offset(cx, cy),
        radius = r * 3f * pulse
    )

    // Planet body
    val planetGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFFB388FF),
            Color(0xFF7C4DFF),
            Color(0xFF6200EA),
            Color(0xFF311B92)
        ),
        center = Offset(cx - r * 0.3f, cy - r * 0.3f),
        radius = r * 1.5f
    )
    drawCircle(brush = planetGradient, center = Offset(cx, cy), radius = r)

    // Highlight
    drawCircle(
        color = Color.White.copy(alpha = 0.3f),
        radius = r * 0.35f,
        center = Offset(cx - r * 0.25f, cy - r * 0.3f)
    )

    // Hit flash overlay
    if (state.planetHitFlash > 0f) {
        drawCircle(
            color = Color(0xFFFF1744).copy(alpha = state.planetHitFlash * 0.6f),
            center = Offset(cx, cy),
            radius = r
        )
    }

    // Invincibility indicator
    if (state.invincibleTimer > 0f) {
        val alpha = (sin(state.gameTime * 10f) * 0.3f + 0.4f).coerceIn(0f, 1f)
        drawCircle(
            color = Color(0xFF00E5FF).copy(alpha = alpha),
            center = Offset(cx, cy),
            radius = r + 5f,
            style = Stroke(width = 2f)
        )
    }
}

private fun DrawScope.drawShield(state: GameState) {
    val cx = state.centerX
    val cy = state.centerY
    val r = state.shieldRadius
    val arcDeg = Math.toDegrees(state.shieldArc.toDouble()).toFloat()
    val startDeg = Math.toDegrees(state.shieldAngle.toDouble()).toFloat() - arcDeg / 2f

    val shieldColor = when {
        state.shieldPowered -> Color(0xFFFFD740)
        state.shieldFlash > 0f -> Color.White
        else -> Color(0xFF00E5FF)
    }
    val glowAlpha = if (state.shieldFlash > 0f) 0.6f else 0.25f

    // Shield glow
    drawArc(
        color = shieldColor.copy(alpha = glowAlpha),
        startAngle = startDeg,
        sweepAngle = arcDeg,
        useCenter = false,
        topLeft = Offset(cx - r - 8f, cy - r - 8f),
        size = Size((r + 8f) * 2, (r + 8f) * 2),
        style = Stroke(width = 18f, cap = StrokeCap.Round)
    )

    // Shield core
    drawArc(
        color = shieldColor,
        startAngle = startDeg,
        sweepAngle = arcDeg,
        useCenter = false,
        topLeft = Offset(cx - r, cy - r),
        size = Size(r * 2, r * 2),
        style = Stroke(width = 8f, cap = StrokeCap.Round)
    )

    // Shield tips - bright dots at each end
    val tipAngle1 = state.shieldAngle - state.shieldArc / 2f
    val tipAngle2 = state.shieldAngle + state.shieldArc / 2f
    for (tipAngle in listOf(tipAngle1, tipAngle2)) {
        val tx = cx + cos(tipAngle) * r
        val ty = cy + sin(tipAngle) * r
        drawCircle(
            color = Color.White,
            radius = 6f,
            center = Offset(tx, ty)
        )
    }

    // Orbit trail (faint)
    drawCircle(
        color = Color(0xFF00E5FF).copy(alpha = 0.06f),
        center = Offset(cx, cy),
        radius = r,
        style = Stroke(width = 1.5f)
    )
}

private fun DrawScope.drawAsteroids(state: GameState) {
    for (asteroid in state.asteroids) {
        val baseColor = when (asteroid.type) {
            3 -> Color(0xFFFF1744) // Red boss
            2 -> Color(0xFFFF6D00) // Orange heavy
            1 -> Color(0xFFFFAB00) // Yellow medium
            else -> Color(0xFF90A4AE) // Gray normal
        }

        // Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(baseColor.copy(alpha = 0.4f), Color.Transparent),
                center = Offset(asteroid.x, asteroid.y),
                radius = asteroid.size * 2f
            ),
            center = Offset(asteroid.x, asteroid.y),
            radius = asteroid.size * 2f
        )

        // Body - jagged rock shape
        val points = 7
        val path = Path()
        for (i in 0 until points) {
            val a = asteroid.rotation + (i.toFloat() / points) * 2 * PI.toFloat()
            val jag = if (i % 2 == 0) asteroid.size else asteroid.size * 0.7f
            val px = asteroid.x + cos(a) * jag
            val py = asteroid.y + sin(a) * jag
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()

        drawPath(path, color = baseColor)
        drawPath(path, color = baseColor.copy(alpha = 0.5f), style = Stroke(width = 2f))

        // HP indicator for multi-hit
        if (asteroid.hp > 1) {
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = 4f,
                center = Offset(asteroid.x, asteroid.y)
            )
            if (asteroid.hp > 2) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.5f),
                    radius = 3f,
                    center = Offset(asteroid.x - 6f, asteroid.y)
                )
            }
        }
    }
}

private fun DrawScope.drawParticles(state: GameState) {
    for (p in state.particles) {
        val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
        drawCircle(
            color = Color(p.r, p.g, p.b, alpha),
            radius = p.size * alpha,
            center = Offset(p.x, p.y)
        )
    }
}

private fun DrawScope.drawShieldFragments(state: GameState) {
    for (f in state.shieldFragments) {
        val alpha = f.life.coerceIn(0f, 1f)
        val fx = state.centerX + cos(f.angle) * f.radius
        val fy = state.centerY + sin(f.angle) * f.radius
        drawCircle(
            color = Color(0xFF00E5FF).copy(alpha = alpha * 0.6f),
            radius = 4f,
            center = Offset(fx, fy)
        )
    }
}

private fun DrawScope.drawPowerUp(state: GameState) {
    if (state.powerUpType < 0 || state.powerUpActive) return

    val px = state.centerX + cos(state.powerUpAngle) * state.powerUpRadius
    val py = state.centerY + sin(state.powerUpAngle) * state.powerUpRadius
    val pulse = sin(state.gameTime * 4f) * 3f + 15f

    val color = when (state.powerUpType) {
        0 -> Color(0xFFFFD740) // Shield expand
        1 -> Color(0xFF00E676) // Health
        else -> Color.White
    }

    // Glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.5f), Color.Transparent),
            center = Offset(px, py),
            radius = pulse * 2f
        ),
        center = Offset(px, py),
        radius = pulse * 2f
    )

    // Core
    drawCircle(color = color, radius = pulse, center = Offset(px, py))
    drawCircle(color = Color.White.copy(alpha = 0.5f), radius = pulse * 0.5f, center = Offset(px, py))
}

private fun DrawScope.drawScorePopups(state: GameState) {
    for (popup in state.scorePopups) {
        val alpha = (popup.life / 1.5f).coerceIn(0f, 1f)
        val scale = 1f + (1f - alpha) * 0.3f

        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                this.color = android.graphics.Color.argb(
                    (alpha * 255).toInt(),
                    255, 215, 64
                )
                textSize = 28f * scale
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
                isAntiAlias = true
                setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
            }
            drawText(popup.text, popup.x, popup.y, paint)
        }
    }
}

private fun DrawScope.drawHUD(state: GameState) {
    val canvas = drawContext.canvas.nativeCanvas

    // Score (top center)
    canvas.apply {
        val scorePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 56f
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
            setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
        }
        drawText("${state.score}", state.screenWidth / 2f, 90f, scorePaint)
    }

    // Combo (below score)
    if (state.combo > 1) {
        canvas.apply {
            val comboPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(255, 255, 215, 64)
                textSize = 32f
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
                isAntiAlias = true
            }
            drawText("COMBO x${state.combo}", state.screenWidth / 2f, 130f, comboPaint)
        }

        // Combo timer bar
        val barWidth = 120f
        val barFill = (state.comboTimer / 3f).coerceIn(0f, 1f)
        drawRoundRect(
            color = Color(0xFF333333),
            topLeft = Offset(state.screenWidth / 2f - barWidth / 2f, 140f),
            size = Size(barWidth, 4f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f)
        )
        drawRoundRect(
            color = Color(0xFFFFD740),
            topLeft = Offset(state.screenWidth / 2f - barWidth / 2f, 140f),
            size = Size(barWidth * barFill, 4f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f)
        )
    }

    // Lives (top left)
    for (i in 0 until state.maxLives) {
        val heartX = 30f + i * 36f
        val heartY = 70f
        val color = if (i < state.lives) Color(0xFFFF1744) else Color(0xFF333333)
        drawCircle(color = color, radius = 12f, center = Offset(heartX, heartY))
        drawCircle(color = color.copy(alpha = 0.5f), radius = 8f, center = Offset(heartX - 2f, heartY - 3f))
    }

    // Difficulty level (top right)
    canvas.apply {
        val lvlPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(180, 255, 255, 255)
            textSize = 24f
            textAlign = android.graphics.Paint.Align.RIGHT
            isAntiAlias = true
        }
        drawText("LV.${state.difficultyLevel}", state.screenWidth - 20f, 80f, lvlPaint)
    }

    // Shield power indicator
    if (state.shieldPowered) {
        canvas.apply {
            val pwrPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(255, 255, 215, 64)
                textSize = 22f
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            drawText("SHIELD+", state.screenWidth / 2f, state.screenHeight - 60f, pwrPaint)
        }
    }

    // Ready phase instruction
    if (state.phase == GamePhase.READY) {
        val alpha = (sin(state.gameTime * 3f) * 0.3f + 0.7f).coerceIn(0f, 1f)
        canvas.apply {
            val tapPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb((alpha * 255).toInt(), 255, 255, 255)
                textSize = 36f
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
                isAntiAlias = true
            }
            drawText("TAP TO START", state.screenWidth / 2f, state.screenHeight * 0.75f, tapPaint)
        }
    }
}

private fun DrawScope.drawGameOver(state: GameState) {
    // Dark overlay
    drawRect(color = Color.Black.copy(alpha = 0.6f), size = size)

    val canvas = drawContext.canvas.nativeCanvas

    canvas.apply {
        val titlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(255, 255, 23, 68)
            textSize = 64f
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
            setShadowLayer(6f, 0f, 3f, android.graphics.Color.BLACK)
        }
        drawText("GAME OVER", state.screenWidth / 2f, state.screenHeight * 0.3f, titlePaint)

        val scorePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 48f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        drawText("${state.score}", state.screenWidth / 2f, state.screenHeight * 0.42f, scorePaint)

        val labelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(180, 255, 255, 255)
            textSize = 26f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        drawText("BEST: ${state.bestScore}", state.screenWidth / 2f, state.screenHeight * 0.50f, labelPaint)
        drawText("DEFLECTIONS: ${state.deflections}", state.screenWidth / 2f, state.screenHeight * 0.56f, labelPaint)
        drawText("MAX COMBO: x${state.maxCombo}", state.screenWidth / 2f, state.screenHeight * 0.62f, labelPaint)

        val restartPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(
                ((sin(state.gameTime * 3f) * 0.3f + 0.7f).coerceIn(0f, 1f) * 255).toInt(),
                0, 229, 255
            )
            textSize = 32f
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
        drawText("TAP TO RETRY", state.screenWidth / 2f, state.screenHeight * 0.75f, restartPaint)
    }
}
