package com.voidrunner.gravity.game

import androidx.compose.ui.geometry.Offset
import kotlin.math.*
import kotlin.random.Random

object GameLogic {

    fun initGame(state: GameState): GameState {
        val stars = List(80) { Offset(Random.nextFloat(), Random.nextFloat()) }
        return state.copy(
            phase = GamePhase.READY,
            playerY = GameState.FLOOR_Y - GameState.PLAYER_SIZE,
            playerVY = 0f,
            gravityDir = 1,
            playerRotation = 0f,
            isGrounded = true,
            trailPoints = emptyList(),
            scrollSpeed = GameState.BASE_SPEED,
            distance = 0f,
            worldOffset = 0f,
            obstacles = emptyList(),
            crystals = emptyList(),
            nextSpawnX = 6f,
            score = 0,
            crystalScore = 0,
            distanceScore = 0,
            combo = 0,
            comboTimer = 0f,
            maxCombo = 0,
            totalCrystals = 0,
            particles = emptyList(),
            floatingTexts = emptyList(),
            screenFlash = 0f,
            screenShake = 0f,
            zoneNumber = 1,
            zoneFlash = 0f,
            deathTimer = 0f,
            readyPulse = 0f,
            gameTime = 0f,
            stars = stars
        )
    }

    fun startGame(state: GameState): GameState {
        return generateWorld(state.copy(phase = GamePhase.PLAYING))
    }

    fun flipGravity(state: GameState): GameState {
        if (state.phase != GamePhase.PLAYING) return state
        val newDir = -state.gravityDir
        val flipVY = -1.2f * state.gravityDir
        return state.copy(
            gravityDir = newDir,
            playerVY = flipVY,
            isGrounded = false,
            screenFlash = 0.25f,
            particles = state.particles + createFlipParticles(state)
        )
    }

    fun update(state: GameState, dt: Float): GameState {
        val safeDt = dt.coerceAtMost(0.05f)
        return when (state.phase) {
            GamePhase.READY -> updateReady(state, safeDt)
            GamePhase.PLAYING -> updatePlaying(state, safeDt)
            GamePhase.DEAD -> updateDead(state, safeDt)
            GamePhase.SCORE -> state
        }
    }

    // --- Phase updates ---

    private fun updateReady(state: GameState, dt: Float): GameState {
        return state.copy(
            readyPulse = state.readyPulse + dt * 3f
        )
    }

    private fun updatePlaying(state: GameState, dt: Float): GameState {
        var s = state.copy(gameTime = state.gameTime + dt)

        // Scroll
        val distDelta = s.scrollSpeed * dt
        s = s.copy(
            distance = s.distance + distDelta,
            worldOffset = s.worldOffset + distDelta,
            distanceScore = (s.distance * 10).toInt()
        )

        // Speed & zone
        val newZone = ((s.distance / GameState.ZONE_DISTANCE).toInt() + 1).coerceAtLeast(1)
        s = s.copy(
            scrollSpeed = (GameState.BASE_SPEED + s.distance * 0.004f)
                .coerceAtMost(GameState.MAX_SPEED),
            zoneNumber = newZone,
            zoneFlash = if (newZone > state.zoneNumber) 2.5f
                else (s.zoneFlash - dt).coerceAtLeast(0f)
        )

        // Physics
        s = updatePlayer(s, dt)

        // World gen & scroll
        s = generateWorld(s)
        s = scrollWorld(s, distDelta)

        // Laser toggle
        s = updateLasers(s, dt)

        // Collisions
        s = checkCrystalCollection(s)
        s = checkObstacleCollision(s)

        // Combo
        s = updateCombo(s, dt)

        // Effects
        s = s.copy(
            particles = updateParticles(s.particles, dt),
            floatingTexts = updateFloatingTexts(s.floatingTexts, dt),
            screenFlash = (s.screenFlash - dt * 4f).coerceAtLeast(0f),
            screenShake = (s.screenShake - dt * 6f).coerceAtLeast(0f),
            score = s.distanceScore + s.crystalScore
        )

        return s
    }

    private fun updateDead(state: GameState, dt: Float): GameState {
        val timer = state.deathTimer + dt
        val s = state.copy(
            deathTimer = timer,
            particles = updateParticles(state.particles, dt),
            screenShake = (state.screenShake - dt * 3f).coerceAtLeast(0f)
        )
        return if (timer > 1.5f) {
            s.copy(phase = GamePhase.SCORE)
        } else s
    }

    // --- Player physics ---

    private fun updatePlayer(state: GameState, dt: Float): GameState {
        var vy = state.playerVY
        var py = state.playerY
        var grounded = false

        vy += GameState.GRAVITY * state.gravityDir * dt
        py += vy * dt

        val bottomLimit = GameState.FLOOR_Y - GameState.PLAYER_SIZE
        val topLimit = GameState.CEILING_Y

        if (state.gravityDir == 1 && py >= bottomLimit) {
            py = bottomLimit
            vy = 0f
            grounded = true
        } else if (state.gravityDir == -1 && py <= topLimit) {
            py = topLimit
            vy = 0f
            grounded = true
        }

        // Clamp
        py = py.coerceIn(topLimit, bottomLimit)

        val targetRot = if (state.gravityDir == 1) 0f else 180f
        val rot = state.playerRotation + (targetRot - state.playerRotation) * (dt * 10f).coerceAtMost(1f)

        // Trail
        val px = GameState.PLAYER_X_NORM
        val newTrail = (listOf(Offset(px, py + GameState.PLAYER_SIZE / 2f)) + state.trailPoints)
            .take(GameState.TRAIL_LENGTH)

        return state.copy(
            playerY = py,
            playerVY = vy,
            isGrounded = grounded,
            playerRotation = rot,
            trailPoints = newTrail
        )
    }

    // --- World generation ---

    private fun generateWorld(state: GameState): GameState {
        var s = state
        val viewRight = s.worldOffset + GameState.VISIBLE_WORLD_UNITS + 4f

        while (s.nextSpawnX < viewRight) {
            val x = s.nextSpawnX
            val zone = s.zoneNumber

            // Obstacle
            val obs = generateObstacle(x, zone)
            s = s.copy(obstacles = s.obstacles + obs)

            // Crystals between obstacles
            val crystalCount = Random.nextInt(1, 3 + (zone / 2).coerceAtMost(3))
            val newCrystals = (0 until crystalCount).map {
                val cx = x - Random.nextFloat() * 1.5f - 0.5f
                val cy = if (Random.nextBoolean()) {
                    Random.nextFloat() * 0.25f + GameState.CEILING_Y + 0.05f
                } else {
                    GameState.FLOOR_Y - 0.05f - Random.nextFloat() * 0.25f
                }
                Crystal(
                    x = cx,
                    y = cy.coerceIn(GameState.CEILING_Y + 0.05f, GameState.FLOOR_Y - 0.05f),
                    value = if (Random.nextFloat() < 0.15f) 500 else 100
                )
            }
            s = s.copy(crystals = s.crystals + newCrystals)

            // Spacing decreases with zone
            val spacing = (3.5f - zone * 0.15f).coerceAtLeast(1.8f) +
                    Random.nextFloat() * 1.5f
            s = s.copy(nextSpawnX = s.nextSpawnX + spacing)
        }
        return s
    }

    private fun generateObstacle(x: Float, zone: Int): Obstacle {
        val types = mutableListOf(
            ObstacleType.SPIKE_BOTTOM,
            ObstacleType.SPIKE_TOP
        )
        if (zone >= 2) {
            types += ObstacleType.SPIKE_BOTH
            types += ObstacleType.WALL_GAP_CENTER
        }
        if (zone >= 3) {
            types += ObstacleType.WALL_GAP_TOP
            types += ObstacleType.WALL_GAP_BOTTOM
            types += ObstacleType.LASER
        }
        if (zone >= 4) {
            types += ObstacleType.LASER
            types += ObstacleType.SPIKE_BOTH
        }

        val type = types[Random.nextInt(types.size)]
        val gapSize = (0.38f - zone * 0.015f).coerceAtLeast(0.22f)

        return when (type) {
            ObstacleType.WALL_GAP_TOP -> Obstacle(x, type, gapCenter = 0.25f, gapSize = gapSize)
            ObstacleType.WALL_GAP_BOTTOM -> Obstacle(x, type, gapCenter = 0.75f, gapSize = gapSize)
            ObstacleType.WALL_GAP_CENTER -> Obstacle(x, type, gapCenter = 0.5f, gapSize = gapSize)
            ObstacleType.LASER -> Obstacle(x, type, laserTimer = Random.nextFloat() * 2f)
            else -> Obstacle(x, type)
        }
    }

    // --- Scrolling & cleanup ---

    private fun scrollWorld(state: GameState, distDelta: Float): GameState {
        val cullX = state.worldOffset - 2f
        return state.copy(
            obstacles = state.obstacles.filter { it.x > cullX },
            crystals = state.crystals.filter { it.x > cullX && !it.collected }
        )
    }

    private fun updateLasers(state: GameState, dt: Float): GameState {
        return state.copy(
            obstacles = state.obstacles.map { obs ->
                if (obs.type == ObstacleType.LASER) {
                    val t = obs.laserTimer + dt
                    val cycle = t % 2.0f
                    obs.copy(laserTimer = t, laserOn = cycle < 1.2f)
                } else obs
            }
        )
    }

    // --- Collision detection ---

    private fun checkCrystalCollection(state: GameState): GameState {
        val playerWorldX = state.worldOffset + GameState.PLAYER_X_NORM * GameState.VISIBLE_WORLD_UNITS
        val playerCenterY = state.playerY + GameState.PLAYER_SIZE / 2f
        val collectRadius = GameState.PLAYER_SIZE + GameState.CRYSTAL_SIZE

        var crystalScore = state.crystalScore
        var combo = state.combo
        var comboTimer = state.comboTimer
        var maxCombo = state.maxCombo
        var total = state.totalCrystals
        val newParticles = mutableListOf<Particle>()
        val newTexts = mutableListOf<FloatingText>()

        val newCrystals = state.crystals.map { crystal ->
            if (crystal.collected) return@map crystal
            val dx = crystal.x - playerWorldX
            val dy = crystal.y - playerCenterY
            val dist = sqrt(dx * dx + dy * dy)

            if (dist < collectRadius) {
                combo++
                comboTimer = GameState.COMBO_WINDOW
                if (combo > maxCombo) maxCombo = combo
                total++

                val multiplier = 1f + (combo - 1) * 0.5f
                val points = (crystal.value * multiplier).toInt()
                crystalScore += points

                // Effects
                newParticles += createCollectParticles(crystal)
                val comboText = if (combo > 1) " x$combo" else ""
                newTexts += FloatingText(
                    crystal.x, crystal.y,
                    "+$points$comboText",
                    1.2f,
                    if (crystal.value > 100) 0xFFFFD700 else 0xFF00E5FF
                )

                crystal.copy(collected = true)
            } else crystal
        }

        return state.copy(
            crystals = newCrystals,
            crystalScore = crystalScore,
            combo = combo,
            comboTimer = comboTimer,
            maxCombo = maxCombo,
            totalCrystals = total,
            particles = state.particles + newParticles,
            floatingTexts = state.floatingTexts + newTexts
        )
    }

    private fun checkObstacleCollision(state: GameState): GameState {
        if (state.phase != GamePhase.PLAYING) return state

        val playerWorldX = state.worldOffset + GameState.PLAYER_X_NORM * GameState.VISIBLE_WORLD_UNITS
        val playerTop = state.playerY
        val playerBottom = state.playerY + GameState.PLAYER_SIZE
        val playerLeft = playerWorldX - GameState.PLAYER_SIZE * 0.4f
        val playerRight = playerWorldX + GameState.PLAYER_SIZE * 0.4f
        val hitShrink = 0.012f

        for (obs in state.obstacles) {
            val obsLeft = obs.x
            val obsRight = obs.x + obs.width

            if (playerRight < obsLeft || playerLeft > obsRight) continue

            val hit = when (obs.type) {
                ObstacleType.SPIKE_BOTTOM -> {
                    playerBottom > GameState.FLOOR_Y - GameState.SPIKE_HEIGHT + hitShrink
                }
                ObstacleType.SPIKE_TOP -> {
                    playerTop < GameState.CEILING_Y + GameState.SPIKE_HEIGHT - hitShrink
                }
                ObstacleType.SPIKE_BOTH -> {
                    playerBottom > GameState.FLOOR_Y - GameState.SPIKE_HEIGHT + hitShrink ||
                    playerTop < GameState.CEILING_Y + GameState.SPIKE_HEIGHT - hitShrink
                }
                ObstacleType.WALL_GAP_TOP, ObstacleType.WALL_GAP_BOTTOM,
                ObstacleType.WALL_GAP_CENTER -> {
                    val corridorRange = GameState.CORRIDOR_BOTTOM - GameState.CORRIDOR_TOP
                    val gapTop = GameState.CORRIDOR_TOP + corridorRange * (obs.gapCenter - obs.gapSize / 2f)
                    val gapBottom = GameState.CORRIDOR_TOP + corridorRange * (obs.gapCenter + obs.gapSize / 2f)
                    playerTop + hitShrink < gapTop || playerBottom - hitShrink > gapBottom
                }
                ObstacleType.LASER -> {
                    if (!obs.laserOn) false
                    else {
                        val laserY = 0.5f
                        val laserHalf = 0.015f
                        playerTop < laserY + laserHalf && playerBottom > laserY - laserHalf
                    }
                }
            }

            if (hit) {
                return state.copy(
                    phase = GamePhase.DEAD,
                    deathTimer = 0f,
                    screenShake = 1f,
                    particles = state.particles + createDeathParticles(state)
                )
            }
        }
        return state
    }

    // --- Combo ---

    private fun updateCombo(state: GameState, dt: Float): GameState {
        if (state.combo == 0) return state
        val timer = state.comboTimer - dt
        return if (timer <= 0f) {
            state.copy(combo = 0, comboTimer = 0f)
        } else {
            state.copy(comboTimer = timer)
        }
    }

    // --- Particles ---

    private fun createFlipParticles(state: GameState): List<Particle> {
        val py = state.playerY + GameState.PLAYER_SIZE / 2f
        return List(8) {
            Particle(
                x = GameState.PLAYER_X_NORM,
                y = py,
                vx = (Random.nextFloat() - 0.5f) * 0.3f,
                vy = (Random.nextFloat() - 0.5f) * 0.3f,
                life = 0.5f + Random.nextFloat() * 0.3f,
                color = 0xFF00E5FF,
                size = 2f + Random.nextFloat() * 3f
            )
        }
    }

    private fun createCollectParticles(crystal: Crystal): List<Particle> {
        val color = if (crystal.value > 100) 0xFFFFD700 else 0xFF00E5FF
        return List(6) {
            Particle(
                x = crystal.x,
                y = crystal.y,
                vx = (Random.nextFloat() - 0.5f) * 0.4f,
                vy = (Random.nextFloat() - 0.5f) * 0.4f,
                life = 0.6f + Random.nextFloat() * 0.3f,
                color = color,
                size = 2f + Random.nextFloat() * 3f
            )
        }
    }

    private fun createDeathParticles(state: GameState): List<Particle> {
        val py = state.playerY + GameState.PLAYER_SIZE / 2f
        return List(20) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = 0.2f + Random.nextFloat() * 0.5f
            Particle(
                x = GameState.PLAYER_X_NORM,
                y = py,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                life = 0.8f + Random.nextFloat() * 0.5f,
                color = if (Random.nextBoolean()) 0xFFFF006E else 0xFF00E5FF,
                size = 3f + Random.nextFloat() * 4f
            )
        }
    }

    private fun updateParticles(particles: List<Particle>, dt: Float): List<Particle> {
        return particles.mapNotNull { p ->
            val newLife = p.life - dt
            if (newLife <= 0f) null
            else p.copy(
                x = p.x + p.vx * dt,
                y = p.y + p.vy * dt,
                life = newLife
            )
        }
    }

    private fun updateFloatingTexts(texts: List<FloatingText>, dt: Float): List<FloatingText> {
        return texts.mapNotNull { t ->
            val newLife = t.life - dt
            if (newLife <= 0f) null
            else t.copy(y = t.y - dt * 0.08f, life = newLife)
        }
    }
}
