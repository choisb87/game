package com.neonbreaker.game.game

import kotlin.math.*
import kotlin.random.Random

object GameEngine {

    private const val BALL_SPEED_BASE = 420f   // px / sec
    private const val BALL_SPEED_MAX = 720f
    private const val COMBO_WINDOW = 1.8f      // seconds
    private const val PADDLE_Y_OFFSET = 80f
    private const val TOP_HUD = 64f
    private const val POWERUP_DROP_CHANCE = 0.18f

    // ── initialise / reset ──────────────────────────────────────────────
    fun initLevel(state: GameState, w: Float, h: Float): GameState {
        val paddleW = if (System.currentTimeMillis() < state.wideUntil) 180f else 120f
        val paddleY = h - PADDLE_Y_OFFSET
        val paddle = Paddle(x = w / 2f - paddleW / 2f, y = paddleY, width = paddleW)
        val bricks = LevelGenerator.generate(
            level = state.level, canvasW = w, topOffset = TOP_HUD + 16f
        )
        val ball = Ball(
            x = w / 2f, y = paddleY - 16f,
            vx = 0f, vy = 0f, radius = 9f,
        )
        return state.copy(
            phase = GamePhase.READY,
            balls = listOf(ball),
            bricks = bricks,
            paddle = paddle,
            powerUps = emptyList(),
            particles = emptyList(),
            shake = ScreenShake(),
            combo = 0, comboTimer = 0f,
            canvasW = w, canvasH = h,
            totalBricksThisLevel = bricks.size,
        )
    }

    fun launchBall(state: GameState): GameState {
        if (state.phase != GamePhase.READY) return state
        val speed = ballSpeed(state.level)
        val angle = -PI / 2.0 + (Random.nextFloat() - 0.5f) * 0.6
        val balls = state.balls.map {
            it.copy(vx = (cos(angle) * speed).toFloat(), vy = (sin(angle) * speed).toFloat())
        }
        return state.copy(phase = GamePhase.PLAYING, balls = balls)
    }

    // ── main update tick ────────────────────────────────────────────────
    fun update(state: GameState, dt: Float, now: Long): GameState {
        if (state.phase != GamePhase.PLAYING) return state
        var s = state

        // Move paddle toward touch
        s = movePaddle(s, dt)

        // Update balls
        s = updateBalls(s, dt, now)

        // Update power-up drops
        s = updatePowerUps(s, dt, now)

        // Decay combo timer
        s = if (s.comboTimer > 0f) {
            val ct = (s.comboTimer - dt).coerceAtLeast(0f)
            if (ct <= 0f) s.copy(comboTimer = 0f, combo = 0) else s.copy(comboTimer = ct)
        } else s

        // Particles
        s = updateParticles(s, dt)

        // Screen shake
        s = updateShake(s, dt)

        // Check level clear
        if (s.bricks.isEmpty()) {
            s = s.copy(phase = GamePhase.LEVEL_CLEAR)
        }

        return s
    }

    // ── paddle ──────────────────────────────────────────────────────────
    private fun movePaddle(state: GameState, dt: Float): GameState {
        if (state.touchX < 0f) return state
        val target = state.touchX - state.paddle.width / 2f
        val current = state.paddle.x
        val speed = 2800f * dt
        val dx = (target - current).coerceIn(-speed, speed)
        val newX = (current + dx).coerceIn(0f, state.canvasW - state.paddle.width)
        return state.copy(paddle = state.paddle.copy(x = newX))
    }

    // ── balls ───────────────────────────────────────────────────────────
    private fun updateBalls(state: GameState, dt: Float, now: Long): GameState {
        var s = state
        val speedMul = if (now < s.slowUntil) 0.55f else 1f
        val remaining = mutableListOf<Ball>()

        for (ball in s.balls) {
            var b = ball.copy(
                x = ball.x + ball.vx * dt * speedMul,
                y = ball.y + ball.vy * dt * speedMul,
            )
            // Wall bounces
            if (b.x - b.radius < 0f) { b = b.copy(x = b.radius, vx = abs(b.vx)) }
            if (b.x + b.radius > s.canvasW) { b = b.copy(x = s.canvasW - b.radius, vx = -abs(b.vx)) }
            if (b.y - b.radius < TOP_HUD) { b = b.copy(y = TOP_HUD + b.radius, vy = abs(b.vy)) }

            // Paddle bounce
            b = bouncePaddle(b, s.paddle, s.level)

            // Brick collisions
            val result = collideBricks(b, s.bricks, s, now)
            b = result.ball
            s = result.state

            // Ball lost (below screen)
            if (b.y - b.radius > s.canvasH) {
                // Don't add back to remaining
            } else {
                remaining += b
            }
        }

        s = s.copy(balls = remaining)

        // All balls lost
        if (remaining.isEmpty()) {
            val newLives = s.lives - 1
            s = if (newLives <= 0) {
                s.copy(
                    phase = GamePhase.GAME_OVER,
                    lives = 0,
                    highScore = maxOf(s.highScore, s.score),
                )
            } else {
                val paddle = s.paddle
                val ball = Ball(
                    x = paddle.x + paddle.width / 2f,
                    y = paddle.y - 16f,
                    vx = 0f, vy = 0f, radius = 9f,
                )
                s.copy(phase = GamePhase.READY, lives = newLives, balls = listOf(ball), combo = 0)
            }
        }
        return s
    }

    private fun bouncePaddle(ball: Ball, paddle: Paddle, level: Int): Ball {
        if (ball.vy <= 0) return ball
        val py = paddle.y
        if (ball.y + ball.radius < py || ball.y + ball.radius > py + paddle.height + 4f) return ball
        if (ball.x < paddle.x - ball.radius || ball.x > paddle.x + paddle.width + ball.radius) return ball

        // Hit position -1..1 across paddle
        val hitPos = ((ball.x - paddle.x) / paddle.width * 2f - 1f).coerceIn(-0.95f, 0.95f)
        val angle = -PI / 2.0 + hitPos * (PI / 3.0)   // ±60°
        val speed = ballSpeed(level)
        return ball.copy(
            y = py - ball.radius,
            vx = (cos(angle) * speed).toFloat(),
            vy = (sin(angle) * speed).toFloat(),
        )
    }

    private data class CollisionResult(val ball: Ball, val state: GameState)

    private fun collideBricks(ball: Ball, bricks: List<Brick>, state: GameState, now: Long): CollisionResult {
        var b = ball
        var s = state
        val remaining = mutableListOf<Brick>()
        var hit = false

        for (brick in bricks) {
            if (hit && !b.fireball) { remaining += brick; continue }

            val closestX = b.x.coerceIn(brick.x, brick.x + brick.width)
            val closestY = b.y.coerceIn(brick.y, brick.y + brick.height)
            val dx = b.x - closestX
            val dy = b.y - closestY
            if (dx * dx + dy * dy > b.radius * b.radius) { remaining += brick; continue }

            // Hit!
            hit = true
            val newHits = brick.hitsLeft - 1
            if (newHits > 0 && !b.fireball) {
                remaining += brick.copy(hitsLeft = newHits)
            } else {
                // Brick destroyed — spawn particles
                s = spawnBrickParticles(s, brick)
                // Chance to drop power-up
                if (Random.nextFloat() < POWERUP_DROP_CHANCE) {
                    val pu = spawnPowerUp(brick)
                    s = s.copy(powerUps = s.powerUps + pu)
                }
            }

            // Score + combo
            val combo = s.combo + 1
            val multiplier = 1 + combo / 5
            val pts = brick.type.points * multiplier
            s = s.copy(
                score = s.score + pts,
                combo = combo,
                maxCombo = maxOf(s.maxCombo, combo),
                comboTimer = COMBO_WINDOW,
            )

            // Bounce ball (skip if fireball)
            if (!b.fireball) {
                val overlapX = b.radius - abs(dx)
                val overlapY = b.radius - abs(dy)
                b = if (overlapX < overlapY) {
                    b.copy(vx = -b.vx, x = b.x + if (dx > 0) overlapX else -overlapX)
                } else {
                    b.copy(vy = -b.vy, y = b.y + if (dy > 0) overlapY else -overlapY)
                }
            }

            // Screen shake on combo milestones
            if (combo % 5 == 0 && combo > 0) {
                s = s.copy(shake = ScreenShake(intensity = 6f + combo * 0.5f))
            }
        }

        return CollisionResult(b, s.copy(bricks = remaining))
    }

    // ── power-ups ───────────────────────────────────────────────────────
    private fun spawnPowerUp(brick: Brick): PowerUp {
        val type = PowerUpType.entries[Random.nextInt(PowerUpType.entries.size)]
        return PowerUp(type = type, x = brick.x + brick.width / 2f, y = brick.y + brick.height)
    }

    private fun updatePowerUps(state: GameState, dt: Float, now: Long): GameState {
        var s = state
        val remaining = mutableListOf<PowerUp>()
        for (pu in s.powerUps) {
            val newY = pu.y + pu.vy * dt
            if (newY > s.canvasH) continue   // off screen

            // Catch by paddle
            val p = s.paddle
            if (newY + pu.size / 2 >= p.y && newY - pu.size / 2 <= p.y + p.height
                && pu.x >= p.x && pu.x <= p.x + p.width
            ) {
                s = applyPowerUp(s, pu.type, now)
                s = spawnPowerUpParticles(s, pu)
            } else {
                remaining += pu.copy(y = newY)
            }
        }
        return s.copy(powerUps = remaining)
    }

    private fun applyPowerUp(state: GameState, type: PowerUpType, now: Long): GameState {
        return when (type) {
            PowerUpType.WIDE_PADDLE -> {
                val newW = 180f
                state.copy(
                    wideUntil = now + type.duration,
                    paddle = state.paddle.copy(width = newW),
                )
            }
            PowerUpType.MULTI_BALL -> {
                if (state.balls.isEmpty()) return state
                val base = state.balls.first()
                val extra1 = base.copy(
                    vx = base.vx * cos(0.3f) - base.vy * sin(0.3f),
                    vy = base.vx * sin(0.3f) + base.vy * cos(0.3f),
                )
                val extra2 = base.copy(
                    vx = base.vx * cos(-0.3f) - base.vy * sin(-0.3f),
                    vy = base.vx * sin(-0.3f) + base.vy * cos(-0.3f),
                )
                state.copy(balls = state.balls + extra1 + extra2)
            }
            PowerUpType.FIREBALL -> {
                state.copy(
                    fireballUntil = now + type.duration,
                    balls = state.balls.map { it.copy(fireball = true) },
                )
            }
            PowerUpType.SLOW_BALL -> {
                state.copy(slowUntil = now + type.duration)
            }
            PowerUpType.EXTRA_LIFE -> {
                state.copy(lives = (state.lives + 1).coerceAtMost(5))
            }
        }
    }

    // ── particles ───────────────────────────────────────────────────────
    private fun spawnBrickParticles(state: GameState, brick: Brick): GameState {
        val cx = brick.x + brick.width / 2f
        val cy = brick.y + brick.height / 2f
        val particles = (0 until 12).map {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = 80f + Random.nextFloat() * 200f
            Particle(
                x = cx, y = cy,
                vx = cos(angle) * speed, vy = sin(angle) * speed,
                color = brick.type.glowColor,
                life = 1f, decay = 1.8f + Random.nextFloat(),
                size = 3f + Random.nextFloat() * 4f,
            )
        }
        return state.copy(particles = state.particles + particles)
    }

    private fun spawnPowerUpParticles(state: GameState, pu: PowerUp): GameState {
        val particles = (0 until 8).map {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = 60f + Random.nextFloat() * 120f
            Particle(
                x = pu.x, y = pu.y,
                vx = cos(angle) * speed, vy = sin(angle) * speed,
                color = pu.type.color,
                life = 1f, decay = 2.5f,
                size = 3f + Random.nextFloat() * 3f,
            )
        }
        return state.copy(particles = state.particles + particles)
    }

    private fun updateParticles(state: GameState, dt: Float): GameState {
        val alive = state.particles.mapNotNull { p ->
            val newLife = p.life - p.decay * dt
            if (newLife <= 0f) null
            else p.copy(
                x = p.x + p.vx * dt,
                y = p.y + p.vy * dt,
                vy = p.vy + 180f * dt,   // gravity
                life = newLife,
            )
        }
        return state.copy(particles = alive)
    }

    // ── screen shake ────────────────────────────────────────────────────
    private fun updateShake(state: GameState, dt: Float): GameState {
        val shake = state.shake
        if (shake.intensity <= 0.1f) return state.copy(shake = ScreenShake())
        val newIntensity = shake.intensity * (1f - shake.decay * dt).coerceAtLeast(0f)
        return state.copy(
            shake = shake.copy(
                intensity = newIntensity,
                offsetX = (Random.nextFloat() - 0.5f) * newIntensity * 2f,
                offsetY = (Random.nextFloat() - 0.5f) * newIntensity * 2f,
            )
        )
    }

    // ── power-up expiry check (called each frame) ───────────────────────
    fun checkPowerUpExpiry(state: GameState, now: Long): GameState {
        var s = state
        if (s.wideUntil in 1 until now) {
            s = s.copy(wideUntil = 0L, paddle = s.paddle.copy(width = 120f))
        }
        if (s.fireballUntil in 1 until now) {
            s = s.copy(fireballUntil = 0L, balls = s.balls.map { it.copy(fireball = false) })
        }
        if (s.slowUntil in 1 until now) {
            s = s.copy(slowUntil = 0L)
        }
        return s
    }

    // ── helpers ──────────────────────────────────────────────────────────
    private fun ballSpeed(level: Int): Float =
        (BALL_SPEED_BASE + level * 18f).coerceAtMost(BALL_SPEED_MAX)

    private fun cos(a: Float) = kotlin.math.cos(a)
    private fun sin(a: Float) = kotlin.math.sin(a)
}
