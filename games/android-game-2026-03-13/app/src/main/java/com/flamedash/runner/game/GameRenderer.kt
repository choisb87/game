package com.flamedash.runner.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.*

object GameRenderer {

    // ── Color palette ──
    private val flameOrange = Color(0xFFFF6B35)
    private val flameRed = Color(0xFFFF1744)
    private val flameYellow = Color(0xFFFFD740)
    private val emberGlow = Color(0xFFFF9100)
    private val ashDark = Color(0xFF1A1A2E)
    private val iceBlue = Color(0xFF00E5FF)
    private val gemGreen = Color(0xFF00E676)
    private val gemPurple = Color(0xFFD500F9)
    private val skyNight = Color(0xFF0D1B2A)
    private val white = Color.White

    private val particleColors = listOf(flameOrange, flameRed, flameYellow, white)

    fun render(scope: DrawScope, state: GameState, textMeasurer: TextMeasurer) {
        val w = scope.size.width
        val h = scope.size.height

        // Screen shake offset
        val shakeX = if (state.screenShake > 0f)
            (sin(state.gameTime * 80f) * state.screenShake * 12f) else 0f
        val shakeY = if (state.screenShake > 0f)
            (cos(state.gameTime * 90f) * state.screenShake * 8f) else 0f

        scope.drawContext.transform.translate(shakeX, shakeY)

        drawBackground(scope, state, w, h)
        drawEmbers(scope, state, w, h)
        drawPlatforms(scope, state, w, h)
        drawGems(scope, state, w, h)
        drawPlayerTrail(scope, state)
        drawPlayer(scope, state)
        drawParticles(scope, state)
        drawLava(scope, state, w, h)
        drawHUD(scope, state, w, h, textMeasurer)

        // Screen flash
        if (state.screenFlash > 0f) {
            scope.drawRect(
                color = white.copy(alpha = state.screenFlash * 0.6f),
                size = Size(w, h)
            )
        }

        scope.drawContext.transform.translate(-shakeX, -shakeY)
    }

    // ── Background ──
    private fun drawBackground(scope: DrawScope, state: GameState, w: Float, h: Float) {
        // Gradient from deep night to slightly warm near lava
        scope.drawRect(color = skyNight, size = Size(w, h))

        // Warm glow near bottom
        val glowIntensity = state.lavaGlow * 0.15f
        scope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, flameRed.copy(alpha = glowIntensity)),
                startY = h * 0.5f,
                endY = h
            ),
            size = Size(w, h)
        )

        // Stars (static, parallax based on camera)
        val starSeed = 42
        for (i in 0 until 40) {
            val sx = ((i * 137 + starSeed) % 1000) / 1000f * w
            val sy = ((i * 251 + starSeed) % 1000) / 1000f * h +
                (state.cameraY * 0.05f) % h
            val sAlpha = ((i * 73 + starSeed) % 100) / 200f + 0.1f +
                sin(state.gameTime * 2f + i.toFloat()) * 0.1f
            val sSize = ((i * 31 + starSeed) % 3) + 1f

            scope.drawCircle(
                color = white.copy(alpha = sAlpha.toFloat().coerceIn(0.05f, 0.6f)),
                radius = sSize,
                center = Offset(sx, (sy % h + h) % h)
            )
        }
    }

    // ── Embers (floating up from lava) ──
    private fun drawEmbers(scope: DrawScope, state: GameState, w: Float, h: Float) {
        for (ember in state.embers) {
            val screenY = ember.y - state.cameraY
            if (screenY < -20f || screenY > h + 20f) continue

            val wobble = sin(ember.phase) * 8f
            val alpha = ember.alpha * (0.5f + 0.5f * sin(ember.phase * 1.5f)).toFloat()

            scope.drawCircle(
                color = emberGlow.copy(alpha = alpha.coerceIn(0f, 1f)),
                radius = ember.size,
                center = Offset(ember.x + wobble, screenY)
            )
            // Glow halo
            scope.drawCircle(
                color = flameOrange.copy(alpha = alpha * 0.3f),
                radius = ember.size * 2.5f,
                center = Offset(ember.x + wobble, screenY)
            )
        }
    }

    // ── Platforms ──
    private fun drawPlatforms(scope: DrawScope, state: GameState, w: Float, h: Float) {
        for (plat in state.platforms) {
            if (!plat.visible) continue
            val screenY = plat.y - state.cameraY
            if (screenY < -30f || screenY > h + 30f) continue

            val (baseColor, glowColor) = when (plat.type) {
                PlatformType.NORMAL -> Pair(Color(0xFF4A6FA5), Color(0xFF6B8FC5))
                PlatformType.CRUMBLING -> {
                    val flash = if (plat.crumbleTimer > 0f)
                        (sin(plat.crumbleTimer * 30f) * 0.5f + 0.5f).toFloat() else 0f
                    Pair(
                        Color(0xFF8B6914).copy(alpha = 0.7f + flash * 0.3f),
                        Color(0xFFD4A017)
                    )
                }
                PlatformType.BOUNCY -> Pair(Color(0xFF00C853), Color(0xFF69F0AE))
                PlatformType.ICE -> Pair(Color(0xFF0091EA), iceBlue)
            }

            // Platform body
            scope.drawRoundRect(
                color = baseColor,
                topLeft = Offset(plat.x, screenY),
                size = Size(plat.width, state.platformHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )

            // Top highlight
            scope.drawRoundRect(
                color = glowColor.copy(alpha = 0.6f),
                topLeft = Offset(plat.x + 2f, screenY),
                size = Size(plat.width - 4f, 3f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
            )

            // Bouncy platform spring indicator
            if (plat.type == PlatformType.BOUNCY) {
                val centerX = plat.x + plat.width / 2f
                scope.drawCircle(
                    color = Color(0xFF69F0AE),
                    radius = 5f,
                    center = Offset(centerX, screenY + state.platformHeight / 2f)
                )
            }

            // Ice platform shimmer
            if (plat.type == PlatformType.ICE) {
                val shimmerX = plat.x + ((state.gameTime * 60f) % plat.width)
                scope.drawCircle(
                    color = white.copy(alpha = 0.5f),
                    radius = 3f,
                    center = Offset(shimmerX, screenY + 4f)
                )
            }
        }
    }

    // ── Gems ──
    private fun drawGems(scope: DrawScope, state: GameState, w: Float, h: Float) {
        for (gem in state.gems) {
            if (gem.collected) continue
            val screenY = gem.y - state.cameraY + sin(state.gameTime * 3f + gem.bobPhase) * 6f
            if (screenY < -30f || screenY > h + 30f) continue

            val color = when (gem.type) {
                GemType.FIRE -> flameOrange
                GemType.ICE -> iceBlue
                GemType.EMERALD -> gemGreen
                GemType.AMETHYST -> gemPurple
            }

            // Glow
            scope.drawCircle(
                color = color.copy(alpha = 0.25f),
                radius = 16f,
                center = Offset(gem.x, screenY)
            )

            // Diamond shape
            val path = Path().apply {
                moveTo(gem.x, screenY - 10f)
                lineTo(gem.x + 8f, screenY)
                lineTo(gem.x, screenY + 10f)
                lineTo(gem.x - 8f, screenY)
                close()
            }
            scope.drawPath(path, color = color)

            // Inner shine
            val innerPath = Path().apply {
                moveTo(gem.x, screenY - 5f)
                lineTo(gem.x + 4f, screenY)
                lineTo(gem.x, screenY + 5f)
                lineTo(gem.x - 4f, screenY)
                close()
            }
            scope.drawPath(innerPath, color = white.copy(alpha = 0.5f))
        }
    }

    // ── Player trail ──
    private fun drawPlayerTrail(scope: DrawScope, state: GameState) {
        for ((i, trail) in state.playerTrail.withIndex()) {
            val screenY = trail.second - state.cameraY
            val alpha = trail.third * (i.toFloat() / state.playerTrail.size) * 0.4f
            if (alpha < 0.01f) continue

            scope.drawCircle(
                color = flameOrange.copy(alpha = alpha),
                radius = state.playerSize / 2f * (0.3f + 0.7f * i / state.playerTrail.size),
                center = Offset(trail.first, screenY)
            )
        }

        // Dash ghosts
        for ((i, ghost) in state.dashGhosts.withIndex()) {
            val screenY = ghost.second - state.cameraY
            val alpha = state.dashTrailAlpha * (i + 1f) / state.dashGhosts.size * 0.35f

            scope.drawCircle(
                color = flameYellow.copy(alpha = alpha),
                radius = state.playerSize / 2f,
                center = Offset(ghost.first, screenY)
            )
        }
    }

    // ── Player ──
    private fun drawPlayer(scope: DrawScope, state: GameState) {
        val screenY = state.playerScreenY
        val px = state.playerX
        val size = state.playerSize

        // Outer glow
        scope.drawCircle(
            color = flameOrange.copy(alpha = 0.2f),
            radius = size * 1.2f,
            center = Offset(px, screenY)
        )

        // Body (flame shape)
        val bodyPath = Path().apply {
            // Teardrop/flame shape
            moveTo(px, screenY - size * 0.7f)  // top
            cubicTo(
                px + size * 0.6f, screenY - size * 0.3f,
                px + size * 0.5f, screenY + size * 0.3f,
                px, screenY + size * 0.5f         // bottom
            )
            cubicTo(
                px - size * 0.5f, screenY + size * 0.3f,
                px - size * 0.6f, screenY - size * 0.3f,
                px, screenY - size * 0.7f         // back to top
            )
        }
        scope.drawPath(bodyPath, color = flameOrange)

        // Inner flame
        val innerPath = Path().apply {
            moveTo(px, screenY - size * 0.4f)
            cubicTo(
                px + size * 0.3f, screenY - size * 0.15f,
                px + size * 0.25f, screenY + size * 0.2f,
                px, screenY + size * 0.3f
            )
            cubicTo(
                px - size * 0.25f, screenY + size * 0.2f,
                px - size * 0.3f, screenY - size * 0.15f,
                px, screenY - size * 0.4f
            )
        }
        scope.drawPath(innerPath, color = flameYellow)

        // Core
        scope.drawCircle(
            color = white.copy(alpha = 0.8f),
            radius = size * 0.15f,
            center = Offset(px, screenY - size * 0.1f)
        )

        // Eyes (small dots)
        val eyeDir = if (state.playerFacing == DashDir.RIGHT) 1f else -1f
        scope.drawCircle(
            color = ashDark,
            radius = 2.5f,
            center = Offset(px + eyeDir * 5f, screenY - size * 0.2f)
        )
        scope.drawCircle(
            color = ashDark,
            radius = 2.5f,
            center = Offset(px + eyeDir * 11f, screenY - size * 0.2f)
        )

        // Dash indicator (cooldown ring)
        if (state.dashCooldown > 0f) {
            val progress = 1f - state.dashCooldown / 0.3f
            scope.drawArc(
                color = flameYellow.copy(alpha = 0.5f),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(px - size * 0.8f, screenY - size * 0.8f),
                size = Size(size * 1.6f, size * 1.6f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
        }
    }

    // ── Particles ──
    private fun drawParticles(scope: DrawScope, state: GameState) {
        for (p in state.particles) {
            val screenY = p.y - state.cameraY
            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            val color = particleColors[p.colorIndex.coerceIn(0, 3)]

            scope.drawCircle(
                color = color.copy(alpha = alpha * 0.8f),
                radius = p.size * alpha,
                center = Offset(p.x, screenY)
            )
        }
    }

    // ── Lava ──
    private fun drawLava(scope: DrawScope, state: GameState, w: Float, h: Float) {
        val lavaScreenY = state.lavaScreenY
        if (lavaScreenY > h + 100f) return

        val lavaTop = max(0f, lavaScreenY)

        // Warning gradient above lava
        if (lavaScreenY < h + 50f) {
            scope.drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, flameRed.copy(alpha = 0.15f * state.lavaGlow)),
                    startY = lavaTop - 150f,
                    endY = lavaTop
                ),
                topLeft = Offset(0f, max(0f, lavaTop - 150f)),
                size = Size(w, 150f)
            )
        }

        // Lava surface wave
        val wavePath = Path().apply {
            moveTo(0f, lavaTop)
            var x = 0f
            while (x <= w) {
                val waveY = sin(x * 0.02f + state.gameTime * 4f) * 8f +
                    sin(x * 0.035f + state.gameTime * 2.5f) * 5f
                lineTo(x, lavaTop + waveY)
                x += 4f
            }
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        // Lava body
        scope.drawPath(wavePath, brush = Brush.verticalGradient(
            colors = listOf(flameOrange, flameRed, Color(0xFF8B0000)),
            startY = lavaTop,
            endY = h
        ))

        // Lava bright top edge
        val edgePath = Path().apply {
            moveTo(0f, lavaTop)
            var x = 0f
            while (x <= w) {
                val waveY = sin(x * 0.02f + state.gameTime * 4f) * 8f +
                    sin(x * 0.035f + state.gameTime * 2.5f) * 5f
                lineTo(x, lavaTop + waveY)
                x += 4f
            }
            lineTo(w, lavaTop + 4f)
            var x2 = w
            while (x2 >= 0f) {
                val waveY = sin(x2 * 0.02f + state.gameTime * 4f) * 8f +
                    sin(x2 * 0.035f + state.gameTime * 2.5f) * 5f
                lineTo(x2, lavaTop + waveY + 4f)
                x2 -= 4f
            }
            close()
        }
        scope.drawPath(edgePath, color = flameYellow.copy(alpha = state.lavaGlow * 0.8f))
    }

    // ── HUD ──
    private fun drawHUD(scope: DrawScope, state: GameState, w: Float, h: Float, tm: TextMeasurer) {
        if (state.phase == GamePhase.PLAYING || state.phase == GamePhase.DEAD) {
            // Score
            val scoreText = "${state.score}"
            val scoreStyle = TextStyle(
                color = white,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            val scoreLayout = tm.measure(scoreText, scoreStyle)
            scope.drawText(
                scoreLayout,
                topLeft = Offset(w / 2f - scoreLayout.size.width / 2f, 40f)
            )

            // Height indicator
            val heightText = "↑ ${state.heightScore}m"
            val heightStyle = TextStyle(
                color = flameYellow.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            val heightLayout = tm.measure(heightText, heightStyle)
            scope.drawText(
                heightLayout,
                topLeft = Offset(w / 2f - heightLayout.size.width / 2f, 80f)
            )

            // Gems
            val gemText = "◆ ${state.gemCount}"
            val gemStyle = TextStyle(
                color = gemGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            val gemLayout = tm.measure(gemText, gemStyle)
            scope.drawText(gemLayout, topLeft = Offset(20f, 45f))

            // Combo
            if (state.combo > 1) {
                val comboText = "${state.combo}x COMBO!"
                val comboAlpha = (state.comboTimer / 2f).coerceIn(0f, 1f)
                val comboStyle = TextStyle(
                    color = flameYellow.copy(alpha = comboAlpha),
                    fontSize = (18 + state.combo * 2).coerceAtMost(32).sp,
                    fontWeight = FontWeight.ExtraBold
                )
                val comboLayout = tm.measure(comboText, comboStyle)
                scope.drawText(
                    comboLayout,
                    topLeft = Offset(w / 2f - comboLayout.size.width / 2f, 105f)
                )
            }

            // Lava proximity warning
            val distToLava = state.lavaY - state.playerY
            if (distToLava < 300f && state.phase == GamePhase.PLAYING) {
                val urgency = (1f - distToLava / 300f).coerceIn(0f, 1f)
                val flashAlpha = (sin(state.gameTime * 10f) * 0.5f + 0.5f).toFloat() * urgency
                val warnText = "⚠ DANGER"
                val warnStyle = TextStyle(
                    color = flameRed.copy(alpha = flashAlpha),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                val warnLayout = tm.measure(warnText, warnStyle)
                scope.drawText(
                    warnLayout,
                    topLeft = Offset(w / 2f - warnLayout.size.width / 2f, h - 80f)
                )
            }
        }

        // Death overlay
        if (state.phase == GamePhase.DEAD) {
            scope.drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                size = Size(w, h)
            )

            val goText = "GAME OVER"
            val goStyle = TextStyle(
                color = flameRed,
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold
            )
            val goLayout = tm.measure(goText, goStyle)
            scope.drawText(
                goLayout,
                topLeft = Offset(w / 2f - goLayout.size.width / 2f, h * 0.3f)
            )

            val finalScoreText = "SCORE: ${state.score}"
            val fsStyle = TextStyle(
                color = white,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            val fsLayout = tm.measure(finalScoreText, fsStyle)
            scope.drawText(
                fsLayout,
                topLeft = Offset(w / 2f - fsLayout.size.width / 2f, h * 0.3f + 60f)
            )

            if (state.score >= state.bestScore && state.score > 0) {
                val bestText = "★ NEW BEST! ★"
                val bStyle = TextStyle(
                    color = flameYellow,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                val bLayout = tm.measure(bestText, bStyle)
                scope.drawText(
                    bLayout,
                    topLeft = Offset(w / 2f - bLayout.size.width / 2f, h * 0.3f + 100f)
                )
            }

            val gemFinalText = "◆ ${state.gemCount} gems collected"
            val gfStyle = TextStyle(
                color = gemGreen.copy(alpha = 0.8f),
                fontSize = 16.sp
            )
            val gfLayout = tm.measure(gemFinalText, gfStyle)
            scope.drawText(
                gfLayout,
                topLeft = Offset(w / 2f - gfLayout.size.width / 2f, h * 0.3f + 140f)
            )

            val retryText = "TAP TO RETRY"
            val rStyle = TextStyle(
                color = white.copy(alpha = (sin(state.gameTime * 3f) * 0.3f + 0.7f).toFloat()),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
            val rLayout = tm.measure(retryText, rStyle)
            scope.drawText(
                rLayout,
                topLeft = Offset(w / 2f - rLayout.size.width / 2f, h * 0.65f)
            )
        }
    }
}
