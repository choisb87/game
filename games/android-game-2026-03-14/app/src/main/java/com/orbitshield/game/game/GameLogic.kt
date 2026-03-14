package com.orbitshield.game.game

import kotlin.math.*
import kotlin.random.Random

fun initGame(width: Float, height: Float, bestScore: Int): GameState {
    val cx = width / 2f
    val cy = height / 2f
    val orbitRadius = minOf(width, height) * 0.22f
    val planetR = minOf(width, height) * 0.06f

    val stars = (0 until 120).map {
        Star(
            x = Random.nextFloat() * width,
            y = Random.nextFloat() * height,
            brightness = Random.nextFloat() * 0.6f + 0.4f,
            twinkleSpeed = Random.nextFloat() * 2f + 1f,
            size = Random.nextFloat() * 2.5f + 0.5f
        )
    }

    return GameState(
        screenWidth = width,
        screenHeight = height,
        centerX = cx,
        centerY = cy,
        shieldRadius = orbitRadius,
        planetRadius = planetR,
        stars = stars,
        bestScore = bestScore,
        phase = GamePhase.READY
    )
}

fun startGame(state: GameState): GameState {
    return state.copy(
        phase = GamePhase.PLAYING,
        shieldAngle = 0f,
        shieldDirection = 1,
        lives = 3,
        asteroids = emptyList(),
        particles = emptyList(),
        shieldFragments = emptyList(),
        scorePopups = emptyList(),
        score = 0,
        combo = 0,
        comboTimer = 0f,
        deflections = 0,
        maxCombo = 0,
        gameTime = 0f,
        difficultyLevel = 1,
        spawnTimer = 0f,
        spawnInterval = 1.2f,
        screenShake = 0f,
        screenFlash = 0f,
        shieldPowered = false,
        shieldPowerTimer = 0f,
        slowMoTimer = 0f,
        timeScale = 1f,
        powerUpActive = false,
        powerUpType = -1,
        invincibleTimer = 0f,
        planetHitFlash = 0f,
        shieldFlash = 0f
    )
}

fun onTap(state: GameState): GameState {
    if (state.phase == GamePhase.READY) return startGame(state)
    if (state.phase == GamePhase.GAME_OVER) return initGame(state.screenWidth, state.screenHeight, state.bestScore)
    return state.copy(shieldDirection = -state.shieldDirection)
}

fun update(state: GameState, deltaTime: Float): GameState {
    if (state.phase != GamePhase.PLAYING) return state

    val dt = deltaTime * state.timeScale
    var s = state.copy(gameTime = state.gameTime + dt)

    s = updateDifficulty(s)
    s = updateShield(s, dt)
    s = spawnAsteroids(s, dt)
    s = updateAsteroids(s, dt)
    s = checkCollisions(s)
    s = updatePowerUps(s, dt)
    s = updateParticles(s, dt)
    s = updateEffects(s, dt)
    s = updateScorePopups(s, dt)
    s = updateTimers(s, dt)

    return s
}

private fun updateDifficulty(state: GameState): GameState {
    val level = (state.gameTime / 12f).toInt() + 1
    if (level == state.difficultyLevel) return state

    val interval = maxOf(state.minSpawnInterval, 1.2f - (level - 1) * 0.08f)
    return state.copy(
        difficultyLevel = level,
        spawnInterval = interval
    )
}

private fun updateShield(state: GameState, dt: Float): GameState {
    val speed = if (state.shieldPowered) state.shieldAngularSpeed * 1.5f else state.shieldAngularSpeed
    val newAngle = state.shieldAngle + speed * state.shieldDirection * dt
    return state.copy(
        shieldAngle = newAngle % (2 * PI).toFloat(),
        planetPulse = (state.planetPulse + dt * 2f) % (2 * PI).toFloat()
    )
}

private fun spawnAsteroids(state: GameState, dt: Float): GameState {
    val timer = state.spawnTimer + dt
    if (timer < state.spawnInterval) return state.copy(spawnTimer = timer)

    val count = if (state.difficultyLevel >= 8) Random.nextInt(2, 4)
    else if (state.difficultyLevel >= 4) Random.nextInt(1, 3)
    else 1

    var newAsteroids = state.asteroids.toMutableList()
    repeat(count) {
        val angle = Random.nextFloat() * 2 * PI.toFloat()
        val spawnDist = maxOf(state.screenWidth, state.screenHeight) * 0.7f
        val x = state.centerX + cos(angle) * spawnDist
        val y = state.centerY + sin(angle) * spawnDist

        val baseSpeed = 120f + state.difficultyLevel * 15f
        val speed = baseSpeed + Random.nextFloat() * 60f
        val size = when {
            state.difficultyLevel >= 6 && Random.nextFloat() < 0.15f -> 28f
            state.difficultyLevel >= 3 && Random.nextFloat() < 0.2f -> 22f
            else -> 14f + Random.nextFloat() * 6f
        }

        val type = when {
            state.difficultyLevel >= 10 && Random.nextFloat() < 0.1f -> 3
            state.difficultyLevel >= 7 && Random.nextFloat() < 0.15f -> 2
            state.difficultyLevel >= 4 && Random.nextFloat() < 0.2f -> 1
            else -> 0
        }

        val hp = when (type) {
            3 -> 3
            2 -> 2
            else -> 1
        }

        newAsteroids.add(
            Asteroid(
                x = x, y = y,
                angle = angle + PI.toFloat(),
                speed = speed,
                size = size,
                type = type,
                hp = hp
            )
        )
    }

    return state.copy(
        asteroids = newAsteroids,
        spawnTimer = 0f
    )
}

private fun updateAsteroids(state: GameState, dt: Float): GameState {
    val updated = state.asteroids.map { a ->
        val dx = state.centerX - a.x
        val dy = state.centerY - a.y
        val dist = sqrt(dx * dx + dy * dy)
        val nx = if (dist > 0) dx / dist else 0f
        val ny = if (dist > 0) dy / dist else 0f

        a.copy(
            x = a.x + nx * a.speed * dt,
            y = a.y + ny * a.speed * dt,
            rotation = a.rotation + dt * 3f
        )
    }
    return state.copy(asteroids = updated)
}

private fun checkCollisions(state: GameState): GameState {
    var s = state
    val remaining = mutableListOf<Asteroid>()
    var newParticles = s.particles.toMutableList()
    var newPopups = s.scorePopups.toMutableList()
    var newFragments = s.shieldFragments.toMutableList()

    for (asteroid in s.asteroids) {
        val dx = asteroid.x - s.centerX
        val dy = asteroid.y - s.centerY
        val dist = sqrt(dx * dx + dy * dy)

        // Check shield collision
        if (dist <= s.shieldRadius + asteroid.size && dist >= s.shieldRadius - asteroid.size - 15f) {
            val asteroidAngle = normalizeAngle(atan2(dy, dx))
            val shieldStart = normalizeAngle(s.shieldAngle - s.shieldArc / 2)
            val shieldEnd = normalizeAngle(s.shieldAngle + s.shieldArc / 2)

            if (isAngleInArc(asteroidAngle, shieldStart, shieldEnd, s.shieldArc)) {
                val newHp = asteroid.hp - 1
                if (newHp > 0) {
                    // Bounce back partially
                    val bounceAngle = atan2(dy, dx)
                    remaining.add(
                        asteroid.copy(
                            x = s.centerX + cos(bounceAngle) * (s.shieldRadius + asteroid.size + 20f),
                            y = s.centerY + sin(bounceAngle) * (s.shieldRadius + asteroid.size + 20f),
                            hp = newHp,
                            speed = asteroid.speed * 0.7f
                        )
                    )
                    // Partial deflect particles
                    repeat(5) {
                        newParticles.add(createDeflectParticle(asteroid.x, asteroid.y))
                    }
                } else {
                    // Full deflection
                    val newCombo = s.combo + 1
                    val comboMultiplier = minOf(newCombo, 10)
                    val basePoints = when (asteroid.type) {
                        3 -> 50
                        2 -> 25
                        1 -> 15
                        else -> 10
                    }
                    val points = basePoints * comboMultiplier

                    repeat(12 + asteroid.type * 4) {
                        newParticles.add(createDeflectParticle(asteroid.x, asteroid.y))
                    }

                    val popupText = if (comboMultiplier > 1) "+$points (x$comboMultiplier)" else "+$points"
                    newPopups.add(ScorePopup(asteroid.x, asteroid.y, popupText, 1.5f))

                    s = s.copy(
                        score = s.score + points,
                        combo = newCombo,
                        comboTimer = 3f,
                        maxCombo = maxOf(s.maxCombo, newCombo),
                        deflections = s.deflections + 1,
                        shieldFlash = 0.3f,
                        screenShake = if (asteroid.type >= 2) 8f else 4f
                    )
                }
                continue
            }
        }

        // Check planet collision
        if (dist <= s.planetRadius + asteroid.size * 0.6f) {
            if (s.invincibleTimer <= 0f) {
                val newLives = s.lives - 1

                repeat(20) {
                    newParticles.add(createHitParticle(asteroid.x, asteroid.y))
                }

                // Shield break fragments
                repeat(6) {
                    val fragAngle = Random.nextFloat() * 2 * PI.toFloat()
                    newFragments.add(
                        ShieldFragment(
                            angle = fragAngle,
                            vAngle = (Random.nextFloat() - 0.5f) * 4f,
                            vRadius = Random.nextFloat() * 100f + 50f,
                            radius = s.shieldRadius,
                            life = 1f
                        )
                    )
                }

                if (newLives <= 0) {
                    s = s.copy(
                        lives = 0,
                        phase = GamePhase.GAME_OVER,
                        bestScore = maxOf(s.bestScore, s.score),
                        screenShake = 15f,
                        screenFlash = 0.5f,
                        planetHitFlash = 0.5f
                    )
                } else {
                    s = s.copy(
                        lives = newLives,
                        invincibleTimer = 2f,
                        combo = 0,
                        comboTimer = 0f,
                        screenShake = 10f,
                        screenFlash = 0.3f,
                        planetHitFlash = 0.4f,
                        slowMoTimer = 0.5f,
                        timeScale = 0.3f
                    )
                }
            }
            continue
        }

        // Keep asteroid if still in bounds
        if (dist < maxOf(s.screenWidth, s.screenHeight)) {
            remaining.add(asteroid)
        }
    }

    return s.copy(
        asteroids = remaining,
        particles = newParticles.takeLast(200),
        scorePopups = newPopups,
        shieldFragments = newFragments
    )
}

private fun updatePowerUps(state: GameState, dt: Float): GameState {
    if (state.powerUpActive) {
        val timer = state.shieldPowerTimer - dt
        if (timer <= 0f) {
            return state.copy(
                shieldPowered = false,
                shieldPowerTimer = 0f,
                powerUpActive = false,
                shieldArc = (PI / 3).toFloat()
            )
        }
        return state.copy(shieldPowerTimer = timer)
    }

    // Spawn power-up every 20-30s
    val pTimer = state.powerUpTimer + dt
    val spawnAt = 20f + state.difficultyLevel * 2f
    if (pTimer < spawnAt || state.powerUpType >= 0) return state.copy(powerUpTimer = pTimer)

    val type = Random.nextInt(2)
    val angle = Random.nextFloat() * 2 * PI.toFloat()
    val radius = state.shieldRadius * 1.8f

    return state.copy(
        powerUpTimer = 0f,
        powerUpType = type,
        powerUpAngle = angle,
        powerUpRadius = radius
    )
}

private fun updateParticles(state: GameState, dt: Float): GameState {
    val updated = state.particles.mapNotNull { p ->
        val newLife = p.life - dt
        if (newLife <= 0f) null
        else p.copy(
            x = p.x + p.vx * dt,
            y = p.y + p.vy * dt,
            life = newLife,
            vx = p.vx * 0.98f,
            vy = p.vy * 0.98f
        )
    }
    return state.copy(particles = updated)
}

private fun updateEffects(state: GameState, dt: Float): GameState {
    val fragments = state.shieldFragments.mapNotNull { f ->
        val newLife = f.life - dt
        if (newLife <= 0f) null
        else f.copy(
            angle = f.angle + f.vAngle * dt,
            radius = f.radius + f.vRadius * dt,
            life = newLife
        )
    }

    return state.copy(
        shieldFragments = fragments,
        screenShake = maxOf(0f, state.screenShake - dt * 20f),
        screenFlash = maxOf(0f, state.screenFlash - dt * 2f),
        shieldFlash = maxOf(0f, state.shieldFlash - dt * 3f),
        planetHitFlash = maxOf(0f, state.planetHitFlash - dt * 2f)
    )
}

private fun updateScorePopups(state: GameState, dt: Float): GameState {
    val updated = state.scorePopups.mapNotNull { p ->
        val newLife = p.life - dt
        if (newLife <= 0f) null
        else p.copy(
            y = p.y + p.vy * dt * 60f,
            life = newLife
        )
    }
    return state.copy(scorePopups = updated)
}

private fun updateTimers(state: GameState, dt: Float): GameState {
    var s = state

    // Combo decay
    if (s.comboTimer > 0f) {
        val newTimer = s.comboTimer - dt
        if (newTimer <= 0f) {
            s = s.copy(combo = 0, comboTimer = 0f)
        } else {
            s = s.copy(comboTimer = newTimer)
        }
    }

    // Invincibility
    if (s.invincibleTimer > 0f) {
        s = s.copy(invincibleTimer = maxOf(0f, s.invincibleTimer - dt))
    }

    // Slow-mo recovery
    if (s.slowMoTimer > 0f) {
        val newSlow = s.slowMoTimer - dt
        if (newSlow <= 0f) {
            s = s.copy(slowMoTimer = 0f, timeScale = 1f)
        } else {
            s = s.copy(slowMoTimer = newSlow)
        }
    }

    // Power-up collection check
    if (s.powerUpType >= 0 && !s.powerUpActive) {
        val puX = s.centerX + cos(s.powerUpAngle) * s.powerUpRadius
        val puY = s.centerY + sin(s.powerUpAngle) * s.powerUpRadius

        // Check if shield passes over power-up
        val shieldTipX = s.centerX + cos(s.shieldAngle) * s.shieldRadius
        val shieldTipY = s.centerY + sin(s.shieldAngle) * s.shieldRadius
        val pdx = puX - shieldTipX
        val pdy = puY - shieldTipY
        val pDist = sqrt(pdx * pdx + pdy * pdy)

        if (pDist < 30f) {
            s = when (s.powerUpType) {
                0 -> s.copy(
                    shieldPowered = true,
                    shieldPowerTimer = 8f,
                    shieldArc = (PI / 2).toFloat(),
                    powerUpType = -1,
                    powerUpActive = true
                )
                1 -> s.copy(
                    lives = minOf(s.lives + 1, s.maxLives),
                    powerUpType = -1,
                    powerUpActive = false,
                    powerUpTimer = 0f
                )
                else -> s
            }
        }
    }

    return s
}

// Utility functions

private fun normalizeAngle(angle: Float): Float {
    var a = angle % (2 * PI).toFloat()
    if (a < 0) a += (2 * PI).toFloat()
    return a
}

private fun isAngleInArc(angle: Float, start: Float, end: Float, arc: Float): Boolean {
    val a = normalizeAngle(angle)
    val s = normalizeAngle(start)
    val e = normalizeAngle(end)

    return if (arc >= PI.toFloat()) {
        // Large arc - simplified check
        val mid = normalizeAngle((s + e) / 2f)
        angleDiff(a, mid) <= arc / 2f
    } else if (s <= e) {
        a in s..e
    } else {
        a >= s || a <= e
    }
}

private fun angleDiff(a: Float, b: Float): Float {
    val diff = abs(normalizeAngle(a) - normalizeAngle(b))
    return minOf(diff, (2 * PI).toFloat() - diff)
}

private fun createDeflectParticle(x: Float, y: Float): Particle {
    val angle = Random.nextFloat() * 2 * PI.toFloat()
    val speed = Random.nextFloat() * 200f + 80f
    return Particle(
        x = x, y = y,
        vx = cos(angle) * speed,
        vy = sin(angle) * speed,
        life = 0.6f + Random.nextFloat() * 0.4f,
        maxLife = 1f,
        size = Random.nextFloat() * 4f + 2f,
        r = 0f, g = 0.9f, b = 1f
    )
}

private fun createHitParticle(x: Float, y: Float): Particle {
    val angle = Random.nextFloat() * 2 * PI.toFloat()
    val speed = Random.nextFloat() * 150f + 50f
    return Particle(
        x = x, y = y,
        vx = cos(angle) * speed,
        vy = sin(angle) * speed,
        life = 0.8f + Random.nextFloat() * 0.4f,
        maxLife = 1.2f,
        size = Random.nextFloat() * 5f + 2f,
        r = 1f, g = 0.1f, b = 0.15f
    )
}
