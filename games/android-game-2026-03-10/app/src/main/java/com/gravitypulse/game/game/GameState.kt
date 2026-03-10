package com.gravitypulse.game.game

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.random.Random

data class Player(
    val x: Float = 0.5f,
    val y: Float = 0.7f,
    val velocityY: Float = 0f,
    val gravityDirection: Int = 1, // 1 = down, -1 = up
    val radius: Float = 18f,
    val trail: List<Offset> = emptyList()
)

data class Obstacle(
    val y: Float,
    val gapStart: Float,
    val gapWidth: Float,
    val speed: Float = 1f,
    val passed: Boolean = false,
    val color: Int = 0 // 0=cyan, 1=pink, 2=gold
)

data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val life: Float = 1f,
    val color: Int = 0
)

data class PulseRing(
    val x: Float,
    val y: Float,
    val radius: Float = 0f,
    val maxRadius: Float = 120f,
    val life: Float = 1f
)

data class GameState(
    val player: Player = Player(),
    val obstacles: List<Obstacle> = emptyList(),
    val particles: List<Particle> = emptyList(),
    val pulseRings: List<PulseRing> = emptyList(),
    val score: Int = 0,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    val screenShake: Float = 0f,
    val gameSpeed: Float = 1f,
    val isAlive: Boolean = true,
    val frameCount: Long = 0,
    val canvasWidth: Float = 1080f,
    val canvasHeight: Float = 1920f
) {
    companion object {
        const val GRAVITY = 2200f
        const val PULSE_BOOST = -680f
        const val MAX_VELOCITY = 1200f
        const val OBSTACLE_BASE_SPEED = 280f
        const val MIN_GAP_WIDTH = 0.28f
        const val MAX_GAP_WIDTH = 0.45f
        const val SPAWN_INTERVAL = 1.6f
    }
}

fun GameState.update(deltaTime: Float): GameState {
    if (!isAlive) return this

    val dt = deltaTime.coerceAtMost(0.033f)
    val speedMultiplier = 1f + (score * 0.008f).coerceAtMost(1.2f)
    val newGameSpeed = speedMultiplier

    // Update player physics
    val gravity = GameState.GRAVITY * player.gravityDirection
    var newVelY = (player.velocityY + gravity * dt)
        .coerceIn(-GameState.MAX_VELOCITY, GameState.MAX_VELOCITY)
    var newY = player.y + newVelY * dt

    // Bounce off top/bottom walls
    var alive = true
    if (newY - player.radius < 0) {
        newY = player.radius
        newVelY = abs(newVelY) * 0.4f
    }
    if (newY + player.radius > canvasHeight) {
        newY = canvasHeight - player.radius
        newVelY = -abs(newVelY) * 0.4f
    }

    // Horizontal drift with slight sine wave
    val sineOffset = kotlin.math.sin(frameCount * 0.02f) * 0.3f
    val newPlayerX = (canvasWidth * 0.35f) + sineOffset * 30f

    // Update trail
    val newTrail = (listOf(Offset(newPlayerX, newY)) + player.trail).take(12)

    // Move obstacles
    val obstacleSpeed = GameState.OBSTACLE_BASE_SPEED * newGameSpeed
    var newScore = score
    var newCombo = combo
    var newMaxCombo = maxCombo
    var shake = (screenShake - dt * 8f).coerceAtLeast(0f)
    val newParticles = particles.toMutableList()
    val newPulseRings = pulseRings.toMutableList()

    val updatedObstacles = obstacles.mapNotNull { obs ->
        val newObsY = obs.y - obstacleSpeed * dt
        if (newObsY < -60f) return@mapNotNull null

        // Check collision
        val playerScreenX = newPlayerX
        val gapStartPx = obs.gapStart * canvasWidth
        val gapEndPx = gapStartPx + obs.gapWidth * canvasWidth
        val obsTop = newObsY - 14f
        val obsBottom = newObsY + 14f

        if (newY + player.radius > obsTop && newY - player.radius < obsBottom) {
            if (playerScreenX - player.radius < gapStartPx || playerScreenX + player.radius > gapEndPx) {
                alive = false
                shake = 6f
                // Death particles
                repeat(20) {
                    newParticles.add(
                        Particle(
                            x = playerScreenX,
                            y = newY,
                            vx = Random.nextFloat() * 400f - 200f,
                            vy = Random.nextFloat() * 400f - 200f,
                            life = 1f,
                            color = Random.nextInt(3)
                        )
                    )
                }
            }
        }

        // Score when passed
        var passed = obs.passed
        if (!passed && newObsY < newY - 40f) {
            passed = true
            newScore++
            newCombo++
            if (newCombo > newMaxCombo) newMaxCombo = newCombo

            // Score particles
            repeat(6) {
                newParticles.add(
                    Particle(
                        x = playerScreenX,
                        y = newY,
                        vx = Random.nextFloat() * 200f - 100f,
                        vy = -Random.nextFloat() * 300f - 50f,
                        life = 0.8f,
                        color = obs.color
                    )
                )
            }
        }

        obs.copy(y = newObsY, passed = passed)
    }

    // Spawn new obstacles
    val spawnThreshold = canvasHeight / (GameState.SPAWN_INTERVAL * obstacleSpeed / GameState.OBSTACLE_BASE_SPEED)
    val finalObstacles = if (updatedObstacles.isEmpty() ||
        (updatedObstacles.lastOrNull()?.y ?: 0f) < canvasHeight - canvasHeight / (2.2f / newGameSpeed.coerceAtMost(1.8f))
    ) {
        val gapWidth = Random.nextFloat() * (GameState.MAX_GAP_WIDTH - GameState.MIN_GAP_WIDTH) + GameState.MIN_GAP_WIDTH
        val shrink = (score * 0.003f).coerceAtMost(0.1f)
        val actualGap = (gapWidth - shrink).coerceAtLeast(0.22f)
        val gapStart = Random.nextFloat() * (1f - actualGap)

        updatedObstacles + Obstacle(
            y = canvasHeight + 60f,
            gapStart = gapStart,
            gapWidth = actualGap,
            speed = newGameSpeed,
            color = Random.nextInt(3)
        )
    } else {
        updatedObstacles
    }

    // Update particles
    val updatedParticles = newParticles.mapNotNull { p ->
        val newLife = p.life - dt * 2f
        if (newLife <= 0f) return@mapNotNull null
        p.copy(
            x = p.x + p.vx * dt,
            y = p.y + p.vy * dt,
            life = newLife
        )
    }

    // Update pulse rings
    val updatedRings = newPulseRings.mapNotNull { ring ->
        val newLife = ring.life - dt * 3f
        if (newLife <= 0f) return@mapNotNull null
        ring.copy(
            radius = ring.radius + 400f * dt,
            life = newLife
        )
    }

    return copy(
        player = player.copy(
            x = newPlayerX,
            y = newY,
            velocityY = newVelY,
            trail = newTrail
        ),
        obstacles = finalObstacles,
        particles = updatedParticles,
        pulseRings = updatedRings,
        score = newScore,
        combo = if (alive) newCombo else 0,
        maxCombo = newMaxCombo,
        screenShake = shake,
        gameSpeed = newGameSpeed,
        isAlive = alive,
        frameCount = frameCount + 1
    )
}

fun GameState.onTap(): GameState {
    if (!isAlive) return this

    val newDirection = -player.gravityDirection
    val boostVelocity = GameState.PULSE_BOOST * newDirection

    val newRings = pulseRings + PulseRing(
        x = player.x,
        y = player.y,
        life = 1f
    )

    // Tap particles
    val tapParticles = particles.toMutableList()
    repeat(4) {
        tapParticles.add(
            Particle(
                x = player.x,
                y = player.y,
                vx = Random.nextFloat() * 160f - 80f,
                vy = newDirection * -(Random.nextFloat() * 200f + 50f),
                life = 0.5f,
                color = if (newDirection == -1) 0 else 1
            )
        )
    }

    return copy(
        player = player.copy(
            velocityY = boostVelocity,
            gravityDirection = newDirection
        ),
        pulseRings = newRings,
        particles = tapParticles
    )
}
