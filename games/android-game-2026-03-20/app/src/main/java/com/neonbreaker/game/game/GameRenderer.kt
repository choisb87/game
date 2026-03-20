package com.neonbreaker.game.game

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object GameRenderer {

    private val BG_COLOR = Color(0xFF0A0A1A)
    private val GRID_COLOR = Color(0xFF111128)
    private val PADDLE_COLOR = Color(0xFF00FFFF)
    private val BALL_COLOR = Color(0xFFFFFFFF)
    private val FIREBALL_COLOR = Color(0xFFFF6600)
    private val HUD_TEXT = Color(0xFFCCCCDD)
    private val COMBO_COLOR = Color(0xFFFFFF00)

    fun draw(scope: DrawScope, state: GameState, textMeasurer: TextMeasurer) {
        val shake = state.shake
        scope.translate(shake.offsetX, shake.offsetY) {
            drawBackground(this, state)
            drawBricks(this, state)
            drawPowerUps(this, state)
            drawPaddle(this, state)
            drawBalls(this, state)
            drawParticles(this, state)
        }
        // HUD drawn without shake
        drawHUD(scope, state, textMeasurer)
        drawComboIndicator(scope, state, textMeasurer)

        // Overlay messages
        when (state.phase) {
            GamePhase.READY -> drawCenterText(scope, "탭하여 시작", Color(0xFF00FFFF), textMeasurer)
            GamePhase.LEVEL_CLEAR -> drawCenterText(scope, "레벨 ${state.level} 클리어!", Color(0xFF00FF88), textMeasurer)
            GamePhase.GAME_OVER -> drawGameOver(scope, state, textMeasurer)
            else -> {}
        }
    }

    // ── background ──────────────────────────────────────────────────────
    private fun drawBackground(scope: DrawScope, state: GameState) {
        scope.drawRect(BG_COLOR)
        // Subtle grid
        val step = 36f
        var x = 0f
        while (x < state.canvasW) {
            scope.drawLine(GRID_COLOR, Offset(x, 0f), Offset(x, state.canvasH), strokeWidth = 0.5f)
            x += step
        }
        var y = 0f
        while (y < state.canvasH) {
            scope.drawLine(GRID_COLOR, Offset(0f, y), Offset(state.canvasW, y), strokeWidth = 0.5f)
            y += step
        }
        // Top gradient accent
        scope.drawRect(
            Brush.verticalGradient(
                listOf(Color(0x2200AAFF), Color.Transparent),
                startY = 0f, endY = 120f,
            )
        )
    }

    // ── bricks ──────────────────────────────────────────────────────────
    private fun drawBricks(scope: DrawScope, state: GameState) {
        val gap = 2f
        for (brick in state.bricks) {
            val alpha = if (brick.hitsLeft < brick.type.hits) 0.65f else 1f
            val brickColor = brick.type.color.copy(alpha = alpha)
            val glowColor = brick.type.glowColor.copy(alpha = alpha * 0.3f)

            // Glow
            scope.drawRoundRect(
                color = glowColor,
                topLeft = Offset(brick.x + gap - 2f, brick.y + gap - 2f),
                size = Size(brick.width - gap * 2 + 4f, brick.height - gap * 2 + 4f),
                cornerRadius = CornerRadius(5f),
                style = Stroke(width = 3f),
            )
            // Fill
            scope.drawRoundRect(
                color = brickColor,
                topLeft = Offset(brick.x + gap, brick.y + gap),
                size = Size(brick.width - gap * 2, brick.height - gap * 2),
                cornerRadius = CornerRadius(4f),
            )
            // Inner highlight
            scope.drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.15f * alpha), Color.Transparent),
                    startY = brick.y + gap, endY = brick.y + brick.height / 2f,
                ),
                topLeft = Offset(brick.x + gap + 2f, brick.y + gap + 1f),
                size = Size(brick.width - gap * 2 - 4f, brick.height * 0.4f),
                cornerRadius = CornerRadius(3f),
            )
            // Hit indicator
            if (brick.hitsLeft > 1) {
                val cx = brick.x + brick.width / 2f
                val cy = brick.y + brick.height / 2f
                for (i in 0 until brick.hitsLeft) {
                    val dotX = cx + (i - brick.hitsLeft / 2f) * 8f + 4f
                    scope.drawCircle(Color.White.copy(alpha = 0.7f), 2.5f, Offset(dotX, cy))
                }
            }
        }
    }

    // ── paddle ──────────────────────────────────────────────────────────
    private fun drawPaddle(scope: DrawScope, state: GameState) {
        val p = state.paddle
        // Outer glow
        scope.drawRoundRect(
            color = PADDLE_COLOR.copy(alpha = 0.25f),
            topLeft = Offset(p.x - 4f, p.y - 3f),
            size = Size(p.width + 8f, p.height + 6f),
            cornerRadius = CornerRadius(9f),
        )
        // Main paddle
        scope.drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(Color(0xFF0088AA), PADDLE_COLOR, Color(0xFF0088AA)),
                startX = p.x, endX = p.x + p.width,
            ),
            topLeft = Offset(p.x, p.y),
            size = Size(p.width, p.height),
            cornerRadius = CornerRadius(7f),
        )
        // Top highlight
        scope.drawRoundRect(
            color = Color.White.copy(alpha = 0.3f),
            topLeft = Offset(p.x + 4f, p.y + 1f),
            size = Size(p.width - 8f, 3f),
            cornerRadius = CornerRadius(2f),
        )
    }

    // ── balls ───────────────────────────────────────────────────────────
    private fun drawBalls(scope: DrawScope, state: GameState) {
        for (ball in state.balls) {
            val color = if (ball.fireball) FIREBALL_COLOR else BALL_COLOR
            // Glow
            scope.drawCircle(
                color = color.copy(alpha = 0.2f),
                radius = ball.radius * 2.5f,
                center = Offset(ball.x, ball.y),
            )
            // Trail (simple)
            if (ball.vx != 0f || ball.vy != 0f) {
                val speed = kotlin.math.sqrt(ball.vx * ball.vx + ball.vy * ball.vy)
                val nx = -ball.vx / speed
                val ny = -ball.vy / speed
                for (i in 1..4) {
                    val t = i * 3f
                    scope.drawCircle(
                        color = color.copy(alpha = 0.15f - i * 0.03f),
                        radius = ball.radius * (1f - i * 0.15f),
                        center = Offset(ball.x + nx * t, ball.y + ny * t),
                    )
                }
            }
            // Core
            scope.drawCircle(color, ball.radius, Offset(ball.x, ball.y))
            // Specular
            scope.drawCircle(
                Color.White.copy(alpha = 0.5f),
                ball.radius * 0.35f,
                Offset(ball.x - ball.radius * 0.25f, ball.y - ball.radius * 0.25f),
            )
        }
    }

    // ── power-ups ───────────────────────────────────────────────────────
    private fun drawPowerUps(scope: DrawScope, state: GameState) {
        for (pu in state.powerUps) {
            val half = pu.size / 2f
            // Glow circle
            scope.drawCircle(
                color = pu.type.color.copy(alpha = 0.3f),
                radius = half * 1.5f,
                center = Offset(pu.x, pu.y),
            )
            // Background
            scope.drawCircle(
                color = pu.type.color.copy(alpha = 0.8f),
                radius = half,
                center = Offset(pu.x, pu.y),
            )
            // Border
            scope.drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                radius = half,
                center = Offset(pu.x, pu.y),
                style = Stroke(width = 1.5f),
            )
        }
    }

    // ── particles ───────────────────────────────────────────────────────
    private fun drawParticles(scope: DrawScope, state: GameState) {
        for (p in state.particles) {
            scope.drawCircle(
                color = p.color.copy(alpha = p.life * 0.8f),
                radius = p.size * p.life,
                center = Offset(p.x, p.y),
                blendMode = BlendMode.Plus,
            )
        }
    }

    // ── HUD ─────────────────────────────────────────────────────────────
    private fun drawHUD(scope: DrawScope, state: GameState, tm: TextMeasurer) {
        val y = 18f
        // Score
        val scoreText = "SCORE ${state.score}"
        scope.drawText(
            tm.measure(scoreText, TextStyle(
                color = HUD_TEXT, fontSize = 16.sp,
                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
            )),
            topLeft = Offset(12f, y),
        )
        // Level
        val levelText = "LV ${state.level}"
        val levelLayout = tm.measure(levelText, TextStyle(
            color = Color(0xFF00FF88), fontSize = 16.sp,
            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
        ))
        scope.drawText(levelLayout, topLeft = Offset(state.canvasW / 2f - levelLayout.size.width / 2f, y))

        // Lives
        val livesText = "♥".repeat(state.lives)
        val livesLayout = tm.measure(livesText, TextStyle(
            color = Color(0xFFFF4466), fontSize = 18.sp, fontWeight = FontWeight.Bold,
        ))
        scope.drawText(livesLayout, topLeft = Offset(state.canvasW - livesLayout.size.width - 12f, y))

        // Separator line
        scope.drawLine(
            Color(0xFF222244), Offset(0f, 60f), Offset(state.canvasW, 60f), strokeWidth = 1f,
        )
    }

    // ── combo indicator ─────────────────────────────────────────────────
    private fun drawComboIndicator(scope: DrawScope, state: GameState, tm: TextMeasurer) {
        if (state.combo < 2) return
        val text = "${state.combo}x COMBO"
        val alpha = (state.comboTimer / 1.8f).coerceIn(0f, 1f)
        val layout = tm.measure(text, TextStyle(
            color = COMBO_COLOR.copy(alpha = alpha),
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
        ))
        scope.drawText(
            layout,
            topLeft = Offset(state.canvasW / 2f - layout.size.width / 2f, 68f),
        )
    }

    // ── center messages ─────────────────────────────────────────────────
    private fun drawCenterText(scope: DrawScope, text: String, color: Color, tm: TextMeasurer) {
        val layout = tm.measure(text, TextStyle(
            color = color, fontSize = 28.sp,
            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
        ))
        val x = scope.size.width / 2f - layout.size.width / 2f
        val y = scope.size.height / 2f - layout.size.height / 2f
        // Shadow
        scope.drawText(
            tm.measure(text, TextStyle(
                color = color.copy(alpha = 0.2f), fontSize = 28.sp,
                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
            )),
            topLeft = Offset(x, y + 40f),
        )
        scope.drawText(layout, topLeft = Offset(x, y))
    }

    private fun drawGameOver(scope: DrawScope, state: GameState, tm: TextMeasurer) {
        // Dim overlay
        scope.drawRect(Color.Black.copy(alpha = 0.6f))

        val cx = scope.size.width / 2f
        val cy = scope.size.height / 2f

        val goLayout = tm.measure("GAME OVER", TextStyle(
            color = Color(0xFFFF4466), fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace,
        ))
        scope.drawText(goLayout, topLeft = Offset(cx - goLayout.size.width / 2f, cy - 80f))

        val scoreLayout = tm.measure("점수: ${state.score}", TextStyle(
            color = HUD_TEXT, fontSize = 20.sp,
            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
        ))
        scope.drawText(scoreLayout, topLeft = Offset(cx - scoreLayout.size.width / 2f, cy - 20f))

        val highLayout = tm.measure("최고: ${state.highScore}", TextStyle(
            color = COMBO_COLOR, fontSize = 18.sp,
            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
        ))
        scope.drawText(highLayout, topLeft = Offset(cx - highLayout.size.width / 2f, cy + 20f))

        val tapLayout = tm.measure("탭하여 재시작", TextStyle(
            color = Color(0xFF00FFFF), fontSize = 20.sp,
            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
        ))
        scope.drawText(tapLayout, topLeft = Offset(cx - tapLayout.size.width / 2f, cy + 80f))
    }
}
