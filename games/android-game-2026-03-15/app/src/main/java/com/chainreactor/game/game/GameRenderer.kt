package com.chainreactor.game.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import kotlin.math.*

// ── Main Draw ──────────────────────────────────────────────────────────
fun DrawScope.drawGame(state: GameState) {
    drawBackground(state)
    drawStars(state)
    drawOrbs(state)
    drawExplosions(state)
    drawProjectiles(state)
    drawParticles(state)
    drawScorePopups(state)
    drawHUD(state)
    drawRoundComplete(state)
    drawGameOver(state)
}

// ── Background ─────────────────────────────────────────────────────────
private fun DrawScope.drawBackground(state: GameState) {
    val brush = Brush.radialGradient(
        colors = listOf(Color(0xFF0B1535), Color(0xFF050A18)),
        center = Offset(state.screenWidth / 2f, state.screenHeight / 2f),
        radius = state.screenHeight * 0.7f
    )
    drawRect(brush = brush)

    // Subtle nebula clouds
    val nebulaAlpha = 0.06f + sin(state.gameTime * 0.3f) * 0.02f
    drawCircle(
        color = Color(0xFF6200EA).copy(alpha = nebulaAlpha),
        radius = state.screenWidth * 0.4f,
        center = Offset(state.screenWidth * 0.3f, state.screenHeight * 0.25f)
    )
    drawCircle(
        color = Color(0xFF00BCD4).copy(alpha = nebulaAlpha * 0.7f),
        radius = state.screenWidth * 0.35f,
        center = Offset(state.screenWidth * 0.7f, state.screenHeight * 0.7f)
    )
}

// ── Stars ──────────────────────────────────────────────────────────────
private fun DrawScope.drawStars(state: GameState) {
    for (star in state.stars) {
        val twinkle = (sin(state.gameTime * star.twinkleSpeed + star.twinklePhase) + 1f) / 2f
        val alpha = star.alpha * (0.4f + twinkle * 0.6f)
        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = star.size,
            center = Offset(star.x, star.y)
        )
    }
}

// ── Orbs ───────────────────────────────────────────────────────────────
private fun DrawScope.drawOrbs(state: GameState) {
    for (orb in state.orbs) {
        if (!orb.alive) continue

        val pulse = 1f + sin(orb.pulsePhase) * 0.08f
        val drawRadius = orb.radius * pulse

        // Detonation flash
        if (orb.detonating) {
            val flash = 1f - orb.detonateTimer * 5f
            drawCircle(
                color = Color.White.copy(alpha = min(0.8f, flash)),
                radius = drawRadius * 1.5f,
                center = Offset(orb.x, orb.y)
            )
        }

        // Glow
        drawCircle(
            color = orb.color.copy(alpha = 0.15f),
            radius = drawRadius * 2f,
            center = Offset(orb.x, orb.y)
        )

        // Orb body
        val gradient = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.9f),
                orb.color,
                orb.color.copy(alpha = 0.6f)
            ),
            center = Offset(orb.x - drawRadius * 0.3f, orb.y - drawRadius * 0.3f),
            radius = drawRadius * 1.2f
        )
        drawCircle(
            brush = gradient,
            radius = drawRadius,
            center = Offset(orb.x, orb.y)
        )

        // Frozen indicator
        if (orb.frozen) {
            drawCircle(
                color = Color(0xFF80DEEA).copy(alpha = 0.3f),
                radius = drawRadius * 1.3f,
                center = Offset(orb.x, orb.y),
                style = Stroke(width = 2f)
            )
        }

        // Type indicators
        when (orb.type) {
            OrbType.MEGA -> {
                // Ring around mega orbs
                drawCircle(
                    color = Color(0xFFD500F9).copy(alpha = 0.5f),
                    radius = drawRadius + 4f,
                    center = Offset(orb.x, orb.y),
                    style = Stroke(width = 2.5f)
                )
            }
            OrbType.SPLITTER -> {
                // Cross pattern
                val lineLen = drawRadius * 0.6f
                for (i in 0 until 4) {
                    val angle = (i * PI.toFloat() / 2f) + state.gameTime * 2f
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f),
                        start = Offset(orb.x, orb.y),
                        end = Offset(
                            orb.x + cos(angle) * lineLen,
                            orb.y + sin(angle) * lineLen
                        ),
                        strokeWidth = 1.5f
                    )
                }
            }
            OrbType.GOLDEN -> {
                // Sparkle effect
                val sparkAngle = state.gameTime * 3f
                for (i in 0 until 3) {
                    val a = sparkAngle + i * PI.toFloat() * 2f / 3f
                    val sx = orb.x + cos(a) * (drawRadius + 5f)
                    val sy = orb.y + sin(a) * (drawRadius + 5f)
                    drawCircle(
                        color = Color(0xFFFFD740).copy(alpha = 0.7f),
                        radius = 2f,
                        center = Offset(sx, sy)
                    )
                }
            }
            else -> {}
        }
    }
}

// ── Explosions ─────────────────────────────────────────────────────────
private fun DrawScope.drawExplosions(state: GameState) {
    for (exp in state.explosions) {
        // Outer ring
        drawCircle(
            color = exp.color.copy(alpha = exp.life * 0.4f),
            radius = exp.radius,
            center = Offset(exp.x, exp.y),
            style = Stroke(width = 3f + exp.life * 4f)
        )

        // Inner fill
        drawCircle(
            color = exp.color.copy(alpha = exp.life * 0.15f),
            radius = exp.radius * 0.8f,
            center = Offset(exp.x, exp.y)
        )

        // Core flash
        if (exp.life > 0.6f) {
            drawCircle(
                color = Color.White.copy(alpha = (exp.life - 0.6f) * 2f),
                radius = exp.radius * 0.3f,
                center = Offset(exp.x, exp.y)
            )
        }
    }
}

// ── Projectiles ────────────────────────────────────────────────────────
private fun DrawScope.drawProjectiles(state: GameState) {
    for (proj in state.projectiles) {
        drawCircle(
            color = Color(0xFF2979FF).copy(alpha = proj.life),
            radius = proj.radius,
            center = Offset(proj.x, proj.y)
        )
        drawCircle(
            color = Color.White.copy(alpha = proj.life * 0.8f),
            radius = proj.radius * 0.5f,
            center = Offset(proj.x, proj.y)
        )
    }
}

// ── Particles ──────────────────────────────────────────────────────────
private fun DrawScope.drawParticles(state: GameState) {
    for (p in state.particles) {
        val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
        drawCircle(
            color = p.color.copy(alpha = alpha),
            radius = p.size * alpha,
            center = Offset(p.x, p.y)
        )
    }
}

// ── Score Popups ───────────────────────────────────────────────────────
private fun DrawScope.drawScorePopups(state: GameState) {
    for (popup in state.scorePopups) {
        val alpha = popup.life.coerceIn(0f, 1f)
        val scale = 0.8f + popup.life * 0.4f

        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(
                    (alpha * 255).toInt(),
                    (popup.color.red * 255).toInt(),
                    (popup.color.green * 255).toInt(),
                    (popup.color.blue * 255).toInt()
                )
                textSize = 28f * scale
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
                setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
            }
            drawText(popup.text, popup.x, popup.y, paint)
        }
    }
}

// ── HUD ────────────────────────────────────────────────────────────────
private fun DrawScope.drawHUD(state: GameState) {
    if (state.phase == GamePhase.GAME_OVER) return

    drawContext.canvas.nativeCanvas.apply {
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 42f
            isFakeBoldText = true
            setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
        }

        val smallPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(200, 255, 255, 255)
            textSize = 28f
            setShadowLayer(3f, 0f, 1f, android.graphics.Color.BLACK)
        }

        // Score (top left)
        drawText("${state.score}", 24f, 70f, textPaint)

        // Round (top center)
        val roundText = "ROUND ${state.round}"
        val roundPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(220, 255, 215, 64)
            textSize = 32f
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            setShadowLayer(3f, 0f, 1f, android.graphics.Color.BLACK)
        }
        drawText(roundText, state.screenWidth / 2f, 70f, roundPaint)

        // Sparks remaining (top right)
        val sparkPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(255, 255, 145, 0)
            textSize = 36f
            textAlign = android.graphics.Paint.Align.RIGHT
            isFakeBoldText = true
            setShadowLayer(3f, 0f, 1f, android.graphics.Color.BLACK)
        }
        drawText("⚡ ${state.sparksRemaining}", state.screenWidth - 24f, 70f, sparkPaint)

        // Orbs remaining (below sparks)
        val aliveOrbs = state.orbs.count { it.alive }
        drawText(
            "남은 오브: $aliveOrbs / ${state.totalOrbsThisRound}",
            state.screenWidth - 24f, 105f,
            android.graphics.Paint().apply {
                color = android.graphics.Color.argb(180, 200, 200, 200)
                textSize = 22f
                textAlign = android.graphics.Paint.Align.RIGHT
                setShadowLayer(2f, 0f, 1f, android.graphics.Color.BLACK)
            }
        )

        // Chain indicator
        if (state.chainLength > 0 && state.phase == GamePhase.CHAIN_ACTIVE) {
            val chainAlpha = min(255, 150 + state.chainLength * 15)
            val chainColor = if (state.chainLength >= 5)
                android.graphics.Color.argb(chainAlpha, 255, 23, 68)
            else
                android.graphics.Color.argb(chainAlpha, 255, 215, 64)

            val chainPaint = android.graphics.Paint().apply {
                color = chainColor
                textSize = 56f + min(state.chainLength * 3f, 24f)
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
                setShadowLayer(6f, 0f, 2f, android.graphics.Color.BLACK)
            }
            drawText(
                "CHAIN x${state.chainLength}",
                state.screenWidth / 2f,
                state.screenHeight - 80f,
                chainPaint
            )
        }

        // Best score
        if (state.bestScore > 0) {
            drawText(
                "BEST: ${state.bestScore}",
                24f, 105f, smallPaint
            )
        }
    }
}

// ── Round Complete Overlay ─────────────────────────────────────────────
private fun DrawScope.drawRoundComplete(state: GameState) {
    if (state.phase != GamePhase.ROUND_COMPLETE) return

    val alpha = min(0.6f, (2.5f - state.roundCompleteTimer) * 2f)
    drawRect(color = Color.Black.copy(alpha = alpha))

    drawContext.canvas.nativeCanvas.apply {
        val titlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(255, 255, 215, 64)
            textSize = 64f
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            setShadowLayer(6f, 0f, 3f, android.graphics.Color.BLACK)
        }

        val statPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 36f
            textAlign = android.graphics.Paint.Align.CENTER
            setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
        }

        val cx = state.screenWidth / 2f
        val cy = state.screenHeight / 2f

        val isPerfect = state.orbsDetonated >= state.totalOrbsThisRound
        val title = if (isPerfect) "PERFECT!" else "ROUND CLEAR!"
        drawText(title, cx, cy - 80f, titlePaint)

        drawText("폭파: ${state.orbsDetonated} / ${state.totalOrbsThisRound}", cx, cy - 20f, statPaint)
        drawText("라운드 점수: ${state.roundScore}", cx, cy + 30f, statPaint)
        drawText("최대 체인: x${state.maxChain}", cx, cy + 80f, statPaint)

        val nextPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(180, 200, 200, 200)
            textSize = 28f
            textAlign = android.graphics.Paint.Align.CENTER
            setShadowLayer(3f, 0f, 1f, android.graphics.Color.BLACK)
        }
        drawText("다음 라운드 준비 중...", cx, cy + 140f, nextPaint)
    }
}

// ── Game Over ──────────────────────────────────────────────────────────
private fun DrawScope.drawGameOver(state: GameState) {
    if (state.phase != GamePhase.GAME_OVER) return

    drawRect(color = Color.Black.copy(alpha = 0.75f))

    drawContext.canvas.nativeCanvas.apply {
        val titlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(255, 255, 23, 68)
            textSize = 72f
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            setShadowLayer(8f, 0f, 3f, android.graphics.Color.BLACK)
        }

        val statPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 36f
            textAlign = android.graphics.Paint.Align.CENTER
            setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
        }

        val cx = state.screenWidth / 2f
        val cy = state.screenHeight / 2f

        drawText("GAME OVER", cx, cy - 120f, titlePaint)

        val scorePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(255, 255, 215, 64)
            textSize = 56f
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            setShadowLayer(6f, 0f, 2f, android.graphics.Color.BLACK)
        }
        drawText("${state.score}", cx, cy - 40f, scorePaint)

        drawText("도달 라운드: ${state.round}", cx, cy + 20f, statPaint)
        drawText("총 폭파: ${state.totalOrbsDetonated}", cx, cy + 65f, statPaint)
        drawText("최대 체인: x${state.maxChain}", cx, cy + 110f, statPaint)

        if (state.score >= state.bestScore && state.score > 0) {
            val newBestPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(255, 255, 215, 64)
                textSize = 32f
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
                setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
            }
            drawText("★ NEW BEST! ★", cx, cy + 160f, newBestPaint)
        } else {
            drawText("최고 기록: ${state.bestScore}", cx, cy + 160f, statPaint)
        }

        val tapPaint = android.graphics.Paint().apply {
            val pulse = (sin(state.gameTime.toDouble() * 3.0) * 0.3 + 0.7).toFloat()
            color = android.graphics.Color.argb(
                (pulse * 200).toInt(), 200, 200, 200
            )
            textSize = 30f
            textAlign = android.graphics.Paint.Align.CENTER
            setShadowLayer(3f, 0f, 1f, android.graphics.Color.BLACK)
        }
        drawText("탭하여 다시 시작", cx, cy + 220f, tapPaint)
    }
}
