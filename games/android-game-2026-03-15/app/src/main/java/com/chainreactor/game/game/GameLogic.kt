package com.chainreactor.game.game

import androidx.compose.ui.graphics.Color
import kotlin.math.*
import kotlin.random.Random

// ── Color Palette ──────────────────────────────────────────────────────
private val orbColors = listOf(
    Color(0xFF00E5FF), // cyan
    Color(0xFFFF00E5), // magenta
    Color(0xFFFF6D00), // orange
    Color(0xFF00E676), // green
    Color(0xFF2979FF), // blue
    Color(0xFFFF1744)  // red
)

private val megaColor = Color(0xFFD500F9)
private val splitterColor = Color(0xFF2979FF)
private val freezeColor = Color(0xFF80DEEA)
private val goldenColor = Color(0xFFFFD740)

// ── Round Configuration ────────────────────────────────────────────────
private fun orbCountForRound(round: Int): Int = min(12 + round * 4, 60)
private fun sparksForRound(round: Int): Int = when {
    round <= 3 -> 3
    round <= 6 -> 4
    round <= 10 -> 5
    else -> 6
}

private fun orbRadiusRange(round: Int): ClosedFloatingPointRange<Float> {
    val minR = max(14f, 18f - round * 0.3f)
    val maxR = max(22f, 32f - round * 0.5f)
    return minR..maxR
}

// ── Initialization ─────────────────────────────────────────────────────
fun initGame(width: Float, height: Float, bestScore: Int): GameState {
    val stars = List(100) {
        Star(
            x = Random.nextFloat() * width,
            y = Random.nextFloat() * height,
            size = Random.nextFloat() * 2f + 0.5f,
            alpha = Random.nextFloat() * 0.6f + 0.2f,
            twinkleSpeed = Random.nextFloat() * 2f + 1f,
            twinklePhase = Random.nextFloat() * PI.toFloat() * 2f
        )
    }
    val state = GameState(
        screenWidth = width,
        screenHeight = height,
        phase = GamePhase.PLAYING,
        round = 1,
        sparksRemaining = 3,
        sparksTotal = 3,
        stars = stars,
        bestScore = bestScore
    )
    return spawnOrbs(state)
}

private fun spawnOrbs(state: GameState): GameState {
    val count = orbCountForRound(state.round)
    val margin = 60f
    val radiusRange = orbRadiusRange(state.round)

    val orbs = List(count) { i ->
        val type = when {
            i < count / 8 && state.round >= 2 -> OrbType.MEGA
            i < count / 6 && state.round >= 3 -> OrbType.SPLITTER
            i < count / 5 && state.round >= 4 -> OrbType.FREEZE
            i < count / 4 && state.round >= 2 -> OrbType.GOLDEN
            else -> OrbType.NORMAL
        }

        val radius = when (type) {
            OrbType.MEGA -> Random.nextFloat() * 8f + 28f
            else -> Random.nextFloat() * (radiusRange.endInclusive - radiusRange.start) + radiusRange.start
        }

        val color = when (type) {
            OrbType.NORMAL -> orbColors.random()
            OrbType.MEGA -> megaColor
            OrbType.SPLITTER -> splitterColor
            OrbType.FREEZE -> freezeColor
            OrbType.GOLDEN -> goldenColor
        }

        val speed = Random.nextFloat() * 40f + 15f
        val angle = Random.nextFloat() * PI.toFloat() * 2f

        Orb(
            x = Random.nextFloat() * (state.screenWidth - margin * 2) + margin,
            y = Random.nextFloat() * (state.screenHeight - margin * 2) + margin,
            vx = cos(angle) * speed,
            vy = sin(angle) * speed,
            radius = radius,
            type = type,
            color = color,
            pulsePhase = Random.nextFloat() * PI.toFloat() * 2f
        )
    }

    return state.copy(
        orbs = orbs,
        totalOrbsThisRound = count,
        orbsDetonated = 0,
        roundScore = 0,
        chainLength = 0
    )
}

// ── Player Tap ─────────────────────────────────────────────────────────
fun handleTap(state: GameState, tapX: Float, tapY: Float): GameState {
    if (state.phase != GamePhase.PLAYING && state.phase != GamePhase.CHAIN_ACTIVE) return state
    if (state.sparksRemaining <= 0) return state

    val explosionRadius = 70f
    val explosion = Explosion(
        x = tapX,
        y = tapY,
        radius = 0f,
        maxRadius = explosionRadius,
        color = Color(0xFFFFAB40),
        isPlayerSpark = true
    )

    val sparkParticles = List(20) {
        val angle = Random.nextFloat() * PI.toFloat() * 2f
        val speed = Random.nextFloat() * 200f + 80f
        Particle(
            x = tapX, y = tapY,
            vx = cos(angle) * speed, vy = sin(angle) * speed,
            life = 0.6f, maxLife = 0.6f,
            size = Random.nextFloat() * 4f + 2f,
            color = Color(0xFFFFAB40)
        )
    }

    return state.copy(
        sparksRemaining = state.sparksRemaining - 1,
        explosions = state.explosions + explosion,
        particles = state.particles + sparkParticles,
        phase = GamePhase.CHAIN_ACTIVE,
        shakeTimer = 0.15f,
        shakeIntensity = 6f
    )
}

// ── Main Update Loop ───────────────────────────────────────────────────
fun update(state: GameState, deltaTime: Float): GameState {
    if (state.phase == GamePhase.READY) return state

    // Keep gameTime ticking during GAME_OVER for pulsing animations
    if (state.phase == GamePhase.GAME_OVER) {
        return state.copy(gameTime = state.gameTime + deltaTime)
    }

    val dt = deltaTime * state.timeScale

    var s = state.copy(gameTime = state.gameTime + dt)

    s = updateTimers(s, deltaTime)
    s = updateOrbs(s, dt)
    s = updateExplosions(s, dt)
    s = updateProjectiles(s, dt)
    s = checkExplosionCollisions(s)
    s = checkProjectileCollisions(s)
    s = updateDetonatingOrbs(s, dt)
    s = updateParticles(s, dt)
    s = updateScorePopups(s, dt)
    s = checkRoundEnd(s)

    return s
}

// ── Timer Updates ──────────────────────────────────────────────────────
private fun updateTimers(state: GameState, dt: Float): GameState {
    var slowMo = state.slowMoTimer
    var timeScale = state.timeScale
    var shake = state.shakeTimer
    var shakeI = state.shakeIntensity

    if (slowMo > 0f) {
        slowMo -= dt
        timeScale = if (slowMo > 0f) 0.35f else 1f
    }

    if (shake > 0f) {
        shake -= dt
        if (shake <= 0f) shakeI = 0f
    }

    var roundTimer = state.roundCompleteTimer
    var phase = state.phase
    if (phase == GamePhase.ROUND_COMPLETE) {
        roundTimer -= dt
        if (roundTimer <= 0f) {
            // Next round
            val nextRound = state.round + 1
            val sparks = sparksForRound(nextRound)
            val newState = state.copy(
                round = nextRound,
                sparksRemaining = sparks,
                sparksTotal = sparks,
                phase = GamePhase.PLAYING,
                explosions = emptyList(),
                projectiles = emptyList(),
                slowMoTimer = 0f,
                timeScale = 1f,
                shakeTimer = 0f,
                shakeIntensity = 0f,
                roundCompleteTimer = 0f
            )
            return spawnOrbs(newState)
        }
    }

    return state.copy(
        slowMoTimer = slowMo,
        timeScale = timeScale,
        shakeTimer = shake,
        shakeIntensity = shakeI,
        roundCompleteTimer = roundTimer,
        phase = phase
    )
}

// ── Orb Movement ───────────────────────────────────────────────────────
private fun updateOrbs(state: GameState, dt: Float): GameState {
    val orbs = state.orbs.map { orb ->
        if (!orb.alive || orb.detonating) return@map orb.copy(
            pulsePhase = orb.pulsePhase + dt * 4f
        )

        val speedMult = if (orb.frozen) 0.2f else 1f
        var nx = orb.x + orb.vx * dt * speedMult
        var ny = orb.y + orb.vy * dt * speedMult
        var nvx = orb.vx
        var nvy = orb.vy

        // Bounce off walls
        if (nx - orb.radius < 0f) { nx = orb.radius; nvx = abs(nvx) }
        if (nx + orb.radius > state.screenWidth) { nx = state.screenWidth - orb.radius; nvx = -abs(nvx) }
        if (ny - orb.radius < 0f) { ny = orb.radius; nvy = abs(nvy) }
        if (ny + orb.radius > state.screenHeight) { ny = state.screenHeight - orb.radius; nvy = -abs(nvy) }

        orb.copy(
            x = nx, y = ny, vx = nvx, vy = nvy,
            pulsePhase = orb.pulsePhase + dt * 3f
        )
    }
    return state.copy(orbs = orbs)
}

// ── Explosion Growth ───────────────────────────────────────────────────
private fun updateExplosions(state: GameState, dt: Float): GameState {
    val explosions = state.explosions.mapNotNull { exp ->
        val newRadius = exp.radius + (exp.maxRadius * 3.5f) * dt
        val newLife = exp.life - dt * 1.8f
        if (newLife <= 0f) null
        else exp.copy(radius = min(newRadius, exp.maxRadius), life = newLife)
    }
    return state.copy(explosions = explosions)
}

// ── Projectile Movement ────────────────────────────────────────────────
private fun updateProjectiles(state: GameState, dt: Float): GameState {
    val projectiles = state.projectiles.mapNotNull { proj ->
        val newLife = proj.life - dt * 1.5f
        if (newLife <= 0f) null
        else proj.copy(
            x = proj.x + proj.vx * dt,
            y = proj.y + proj.vy * dt,
            life = newLife
        )
    }
    return state.copy(projectiles = projectiles)
}

// ── Explosion ↔ Orb Collision ──────────────────────────────────────────
private fun checkExplosionCollisions(state: GameState): GameState {
    var orbs = state.orbs
    var newChain = state.chainLength
    var newScore = state.score
    var roundScore = state.roundScore
    var maxChain = state.maxChain
    var newParticles = state.particles
    var newPopups = state.scorePopups
    var detonated = state.orbsDetonated
    var totalDetonated = state.totalOrbsDetonated
    var slowMo = state.slowMoTimer
    var shake = state.shakeTimer
    var shakeI = state.shakeIntensity

    for (exp in state.explosions) {
        if (exp.life < 0.3f) continue // Fading explosions don't trigger

        orbs = orbs.map { orb ->
            if (!orb.alive || orb.detonating) return@map orb

            val dx = orb.x - exp.x
            val dy = orb.y - exp.y
            val dist = sqrt(dx * dx + dy * dy)

            if (dist < exp.radius + orb.radius) {
                // Hit!
                newChain++
                val chainMult = min(newChain, 10)
                val typeBonus = when (orb.type) {
                    OrbType.GOLDEN -> 3
                    OrbType.MEGA -> 2
                    else -> 1
                }
                val points = 10 * chainMult * typeBonus
                newScore += points
                roundScore += points
                maxChain = max(maxChain, newChain)
                detonated++
                totalDetonated++

                // Score popup
                val popupText = if (chainMult > 1) "+$points (x$chainMult)" else "+$points"
                newPopups = newPopups + ScorePopup(
                    x = orb.x, y = orb.y - orb.radius,
                    text = popupText,
                    color = if (chainMult >= 5) Color(0xFFFF1744) else Color(0xFFFFD740)
                )

                // Dramatic effects for big chains
                if (newChain >= 5 && slowMo <= 0f) {
                    slowMo = 1.5f
                }
                if (newChain >= 3) {
                    shake = 0.2f
                    shakeI = min(12f, 4f + newChain * 1.5f)
                }

                // Spawn particles
                val burstCount = min(15, 8 + newChain)
                val burstParticles = List(burstCount) {
                    val angle = Random.nextFloat() * PI.toFloat() * 2f
                    val speed = Random.nextFloat() * 180f + 60f
                    Particle(
                        x = orb.x, y = orb.y,
                        vx = cos(angle) * speed, vy = sin(angle) * speed,
                        life = 0.8f, maxLife = 0.8f,
                        size = Random.nextFloat() * 3f + 1.5f,
                        color = orb.color
                    )
                }
                newParticles = newParticles + burstParticles

                // Mark for detonation with delay based on chain
                val detonateDelay = 0.08f + Random.nextFloat() * 0.12f
                orb.copy(detonating = true, detonateTimer = detonateDelay)
            } else {
                // Freeze effect: slow orbs near freeze explosions
                if (exp.color == freezeColor && dist < exp.radius * 1.8f) {
                    orb.copy(frozen = true)
                } else {
                    orb
                }
            }
        }
    }

    return state.copy(
        orbs = orbs,
        chainLength = newChain,
        score = newScore,
        roundScore = roundScore,
        maxChain = maxChain,
        particles = newParticles.takeLast(300),
        scorePopups = newPopups,
        orbsDetonated = detonated,
        totalOrbsDetonated = totalDetonated,
        slowMoTimer = slowMo,
        shakeTimer = shake,
        shakeIntensity = shakeI
    )
}

// ── Projectile ↔ Orb Collision ─────────────────────────────────────────
private fun checkProjectileCollisions(state: GameState): GameState {
    var orbs = state.orbs
    var chain = state.chainLength
    var score = state.score
    var roundScore = state.roundScore
    var maxChain = state.maxChain
    var particles = state.particles
    var popups = state.scorePopups
    var detonated = state.orbsDetonated
    var totalDetonated = state.totalOrbsDetonated

    for (proj in state.projectiles) {
        orbs = orbs.map { orb ->
            if (!orb.alive || orb.detonating) return@map orb

            val dx = orb.x - proj.x
            val dy = orb.y - proj.y
            val dist = sqrt(dx * dx + dy * dy)

            if (dist < proj.radius + orb.radius) {
                chain++
                val chainMult = min(chain, 10)
                val points = 10 * chainMult
                score += points
                roundScore += points
                maxChain = max(maxChain, chain)
                detonated++
                totalDetonated++

                popups = popups + ScorePopup(
                    x = orb.x, y = orb.y - orb.radius,
                    text = "+$points (x$chainMult)"
                )

                val burstParticles = List(8) {
                    val angle = Random.nextFloat() * PI.toFloat() * 2f
                    val speed = Random.nextFloat() * 120f + 40f
                    Particle(
                        x = orb.x, y = orb.y,
                        vx = cos(angle) * speed, vy = sin(angle) * speed,
                        life = 0.5f, maxLife = 0.5f,
                        size = Random.nextFloat() * 2.5f + 1f,
                        color = orb.color
                    )
                }
                particles = particles + burstParticles

                orb.copy(detonating = true, detonateTimer = 0.1f)
            } else orb
        }
    }

    return state.copy(
        orbs = orbs,
        chainLength = chain,
        score = score,
        roundScore = roundScore,
        maxChain = maxChain,
        particles = particles.takeLast(300),
        scorePopups = popups,
        orbsDetonated = detonated,
        totalOrbsDetonated = totalDetonated
    )
}

// ── Detonating Orbs → Explosions ───────────────────────────────────────
private fun updateDetonatingOrbs(state: GameState, dt: Float): GameState {
    var newExplosions = state.explosions
    var newProjectiles = state.projectiles

    val orbs = state.orbs.map { orb ->
        if (!orb.detonating || !orb.alive) return@map orb

        val newTimer = orb.detonateTimer - dt
        if (newTimer <= 0f) {
            // Explode!
            val explosionRadius = when (orb.type) {
                OrbType.MEGA -> orb.radius * 5f
                OrbType.FREEZE -> orb.radius * 3.5f
                else -> orb.radius * 2.8f
            }

            val expColor = when (orb.type) {
                OrbType.FREEZE -> freezeColor
                else -> orb.color
            }

            newExplosions = newExplosions + Explosion(
                x = orb.x, y = orb.y,
                radius = 0f,
                maxRadius = explosionRadius,
                color = expColor
            )

            // Splitter sends projectiles
            if (orb.type == OrbType.SPLITTER) {
                val projCount = 6
                for (i in 0 until projCount) {
                    val angle = (i.toFloat() / projCount) * PI.toFloat() * 2f
                    newProjectiles = newProjectiles + Projectile(
                        x = orb.x, y = orb.y,
                        vx = cos(angle) * 400f,
                        vy = sin(angle) * 400f
                    )
                }
            }

            orb.copy(alive = false)
        } else {
            orb.copy(detonateTimer = newTimer)
        }
    }

    return state.copy(
        orbs = orbs,
        explosions = newExplosions,
        projectiles = newProjectiles
    )
}

// ── Particle Decay ─────────────────────────────────────────────────────
private fun updateParticles(state: GameState, dt: Float): GameState {
    val particles = state.particles.mapNotNull { p ->
        val newLife = p.life - dt
        if (newLife <= 0f) null
        else p.copy(
            x = p.x + p.vx * dt,
            y = p.y + p.vy * dt,
            vx = p.vx * 0.96f,
            vy = p.vy * 0.96f,
            life = newLife
        )
    }
    return state.copy(particles = particles)
}

// ── Score Popup Fade ───────────────────────────────────────────────────
private fun updateScorePopups(state: GameState, dt: Float): GameState {
    val popups = state.scorePopups.mapNotNull { p ->
        val newLife = p.life - dt * 0.8f
        if (newLife <= 0f) null
        else p.copy(y = p.y - 60f * dt, life = newLife)
    }
    return state.copy(scorePopups = popups)
}

// ── Round End Detection ────────────────────────────────────────────────
private fun checkRoundEnd(state: GameState): GameState {
    if (state.phase == GamePhase.ROUND_COMPLETE || state.phase == GamePhase.GAME_OVER) return state

    val anyActive = state.explosions.isNotEmpty() ||
            state.projectiles.isNotEmpty() ||
            state.orbs.any { it.detonating && it.alive }

    if (anyActive) return state

    // Chain finished — check if sparks remain
    val aliveOrbs = state.orbs.count { it.alive }

    if (state.sparksRemaining <= 0 || (state.phase == GamePhase.CHAIN_ACTIVE && !anyActive)) {
        if (state.phase == GamePhase.CHAIN_ACTIVE && aliveOrbs > 0 && state.sparksRemaining > 0) {
            // Still have sparks, go back to playing
            return state.copy(phase = GamePhase.PLAYING, chainLength = 0)
        }

        if (aliveOrbs == 0) {
            // Perfect round! Bonus points
            val perfectBonus = state.round * 50
            return state.copy(
                phase = GamePhase.ROUND_COMPLETE,
                roundCompleteTimer = 2.5f,
                score = state.score + perfectBonus,
                roundScore = state.roundScore + perfectBonus,
                scorePopups = state.scorePopups + ScorePopup(
                    x = state.screenWidth / 2f,
                    y = state.screenHeight / 2f - 50f,
                    text = "PERFECT! +$perfectBonus",
                    color = Color(0xFFFFD740)
                )
            )
        }

        if (state.sparksRemaining <= 0 && !anyActive && state.phase == GamePhase.CHAIN_ACTIVE) {
            // No sparks left, round result
            val ratio = state.orbsDetonated.toFloat() / max(1, state.totalOrbsThisRound)
            return if (ratio >= 0.5f) {
                // Passed! Move to next round
                state.copy(
                    phase = GamePhase.ROUND_COMPLETE,
                    roundCompleteTimer = 2.5f,
                    bestScore = max(state.bestScore, state.score)
                )
            } else {
                // Failed — game over
                state.copy(
                    phase = GamePhase.GAME_OVER,
                    bestScore = max(state.bestScore, state.score)
                )
            }
        }
    }

    return state
}
