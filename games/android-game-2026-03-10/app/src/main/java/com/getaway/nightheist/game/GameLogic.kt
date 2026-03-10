package com.getaway.nightheist.game

import androidx.compose.ui.geometry.Offset
import kotlin.math.*
import kotlin.random.Random

object GameLogic {

    fun initLevel(level: Int, bestScore: Int = 0, totalScore: Int = 0, lives: Int = 3): GameState {
        val map = MapGenerator.generate(level)
        val spawnF = map.spawnPoint.toFloat()
        val exitF = map.exitPoint.toFloat()
        val cops = MapGenerator.generateCopPatrols(map, level)
        val lootPositions = mutableListOf<GridPos>()
        for (y in 0 until map.height) {
            for (x in 0 until map.width) {
                if (map.tileAt(x, y) == TileType.LOOT) {
                    lootPositions.add(GridPos(x, y))
                }
            }
        }

        // Generate power-ups
        val powerUpCount = 1 + (level / 2).coerceAtMost(2)
        val powerUps = mutableListOf<PowerUpItem>()
        val types = PowerUpType.values()
        repeat(powerUpCount) { i ->
            for (attempt in 0..100) {
                val x = Random.nextInt(2, map.width - 2)
                val y = Random.nextInt(2, map.height - 2)
                if (map.tileAt(x, y) == TileType.FLOOR) {
                    val gp = GridPos(x, y)
                    if (gp !in lootPositions && gp != map.spawnPoint && gp != map.exitPoint) {
                        powerUps.add(PowerUpItem(
                            x = x + 0.5f, y = y + 0.5f,
                            type = types[i % types.size]
                        ))
                        break
                    }
                }
            }
        }

        val introTime = if (level == 1) 3.5f else 2.0f

        return GameState(
            map = map,
            player = Player(x = spawnF.x, y = spawnF.y),
            cops = cops,
            lootCollected = 0,
            totalLoot = lootPositions.size,
            lootPositions = lootPositions,
            timeElapsed = 0f,
            backupTimer = (50f - level * 3f).coerceAtLeast(25f),
            backupArrived = false,
            level = level,
            phase = GamePhase.LEVEL_INTRO,
            score = 0,
            totalScore = totalScore,
            bestScore = bestScore,
            lives = lives,
            cameraX = exitF.x,
            cameraY = exitF.y,
            exitUnlocked = lootPositions.isEmpty(),
            screenState = ScreenState.PLAYING,
            powerUps = powerUps,
            introTimer = introTime,
            introCamStartX = exitF.x,
            introCamStartY = exitF.y,
            neverSpotted = true
        )
    }

    fun update(state: GameState, deltaTime: Float): GameState {
        val dt = deltaTime.coerceAtMost(0.05f)

        if (state.phase == GamePhase.LEVEL_INTRO) {
            return updateLevelIntro(state, dt)
        }

        if (state.phase != GamePhase.PLAYING) return state

        var s = state.copy(timeElapsed = state.timeElapsed + dt)

        s = updatePlayer(s, dt)
        s = updateCops(s, dt)
        s = checkLootCollection(s)
        s = checkPowerUpCollection(s)
        s = checkExit(s)
        s = checkCaught(s)
        s = updateBackup(s, dt)
        s = updatePowerUp(s, dt)
        s = updateCombo(s, dt)
        s = updateParticles(s, dt)
        s = updateFloatingTexts(s, dt)
        s = updateCamera(s, dt)
        s = updateSpotWarning(s)
        s = updateScreenShake(s, dt)

        return s
    }

    private fun updateLevelIntro(state: GameState, dt: Float): GameState {
        val newTimer = state.introTimer - dt
        if (newTimer <= 0f) {
            val spawnF = state.map.spawnPoint.toFloat()
            return state.copy(
                phase = GamePhase.PLAYING,
                introTimer = 0f,
                cameraX = spawnF.x,
                cameraY = spawnF.y
            )
        }

        val spawnF = state.map.spawnPoint.toFloat()
        val totalIntro = if (state.level == 1) 3.5f else 2.0f
        val progress = 1f - (newTimer / totalIntro)
        val easedProgress = progress * progress * (3f - 2f * progress) // smoothstep

        val camX = state.introCamStartX + (spawnF.x - state.introCamStartX) * easedProgress
        val camY = state.introCamStartY + (spawnF.y - state.introCamStartY) * easedProgress

        // Update cop patrols during intro for visual life
        val updatedCops = state.cops.map { cop -> moveAlongPatrol(cop, state.map, dt) }

        return state.copy(
            introTimer = newTimer,
            cameraX = camX,
            cameraY = camY,
            cops = updatedCops
        )
    }

    private fun updatePlayer(state: GameState, dt: Float): GameState {
        val p = state.player
        val dir = p.moveDir
        if (dir == Offset.Zero) {
            val tileX = p.x.toInt()
            val tileY = p.y.toInt()
            val onHideSpot = state.map.tileAt(tileX, tileY) == TileType.HIDESPOT
            return state.copy(player = p.copy(isHiding = onHideSpot))
        }

        val speed = if (p.isRunning) p.speed else p.sneakSpeed
        val finalSpeed = if (state.activePowerUp == PowerUpType.SPEED) speed * 1.5f else speed
        var newX = p.x + dir.x * finalSpeed * dt
        var newY = p.y + dir.y * finalSpeed * dt

        if (!state.map.isWalkableF(newX, p.y, GameState.PLAYER_RADIUS)) newX = p.x
        if (!state.map.isWalkableF(p.x, newY, GameState.PLAYER_RADIUS)) newY = p.y
        if (newX != p.x && newY != p.y && !state.map.isWalkableF(newX, newY, GameState.PLAYER_RADIUS)) {
            if (state.map.isWalkableF(newX, p.y, GameState.PLAYER_RADIUS)) {
                newY = p.y
            } else {
                newX = p.x
            }
        }

        return state.copy(
            player = p.copy(x = newX, y = newY, isHiding = false)
        )
    }

    private fun updateCops(state: GameState, dt: Float): GameState {
        val updatedCops = state.cops.map { cop ->
            if (cop.stunTimer > 0f) {
                cop.copy(
                    stunTimer = (cop.stunTimer - dt).coerceAtLeast(0f),
                    state = CopState.SEARCH,
                    searchTimer = cop.stunTimer
                )
            } else {
                updateSingleCop(cop, state, dt)
            }
        }
        val spotted = updatedCops.any { it.state == CopState.CHASE || it.state == CopState.ALERT }
        return state.copy(
            cops = updatedCops,
            neverSpotted = if (spotted) false else state.neverSpotted
        )
    }

    private fun updateSingleCop(cop: Cop, state: GameState, dt: Float): Cop {
        val player = state.player
        val isGhost = state.activePowerUp == PowerUpType.GHOST
        val canSeePlayer = !player.isHiding && !isGhost && isInVisionCone(cop, player.x, player.y, state.map)

        return when (cop.state) {
            CopState.PATROL -> {
                if (canSeePlayer) {
                    cop.copy(
                        state = CopState.ALERT,
                        alertTimer = 0.6f,
                        lastKnownPlayerX = player.x,
                        lastKnownPlayerY = player.y
                    )
                } else {
                    moveAlongPatrol(cop, state.map, dt)
                }
            }
            CopState.ALERT -> {
                val newTimer = cop.alertTimer - dt
                if (newTimer <= 0f) {
                    cop.copy(
                        state = CopState.CHASE,
                        lastKnownPlayerX = player.x,
                        lastKnownPlayerY = player.y
                    )
                } else {
                    val angle = atan2(player.y - cop.y, player.x - cop.x)
                    cop.copy(alertTimer = newTimer, facingAngle = angle)
                }
            }
            CopState.CHASE -> {
                if (canSeePlayer) {
                    val angle = atan2(player.y - cop.y, player.x - cop.x)
                    val speed = cop.chaseSpeed
                    var newX = cop.x + cos(angle).toFloat() * speed * dt
                    var newY = cop.y + sin(angle).toFloat() * speed * dt
                    if (!state.map.isWalkableF(newX, cop.y, GameState.COP_RADIUS)) newX = cop.x
                    if (!state.map.isWalkableF(cop.x, newY, GameState.COP_RADIUS)) newY = cop.y
                    cop.copy(
                        x = newX, y = newY,
                        facingAngle = angle,
                        lastKnownPlayerX = player.x,
                        lastKnownPlayerY = player.y
                    )
                } else {
                    val dx = cop.lastKnownPlayerX - cop.x
                    val dy = cop.lastKnownPlayerY - cop.y
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist < 0.5f) {
                        cop.copy(state = CopState.SEARCH, searchTimer = 4f)
                    } else {
                        val angle = atan2(dy, dx)
                        val speed = cop.chaseSpeed * 0.8f
                        var newX = cop.x + cos(angle).toFloat() * speed * dt
                        var newY = cop.y + sin(angle).toFloat() * speed * dt
                        if (!state.map.isWalkableF(newX, cop.y, GameState.COP_RADIUS)) newX = cop.x
                        if (!state.map.isWalkableF(cop.x, newY, GameState.COP_RADIUS)) newY = cop.y
                        cop.copy(x = newX, y = newY, facingAngle = angle)
                    }
                }
            }
            CopState.SEARCH -> {
                if (canSeePlayer) {
                    cop.copy(
                        state = CopState.CHASE,
                        lastKnownPlayerX = player.x,
                        lastKnownPlayerY = player.y
                    )
                } else {
                    val newTimer = cop.searchTimer - dt
                    if (newTimer <= 0f) {
                        cop.copy(state = CopState.PATROL, searchTimer = 0f)
                    } else {
                        val newAngle = cop.facingAngle + 2.5f * dt
                        cop.copy(facingAngle = newAngle, searchTimer = newTimer)
                    }
                }
            }
        }
    }

    private fun moveAlongPatrol(cop: Cop, map: GameMap, dt: Float): Cop {
        if (cop.patrolRoute.isEmpty()) return cop

        val target = cop.patrolRoute[cop.patrolIndex % cop.patrolRoute.size].toFloat()
        val dx = target.x - cop.x
        val dy = target.y - cop.y
        val dist = sqrt(dx * dx + dy * dy)

        return if (dist < 0.3f) {
            val nextIdx = (cop.patrolIndex + 1) % cop.patrolRoute.size
            val nextTarget = cop.patrolRoute[nextIdx].toFloat()
            val newAngle = atan2(nextTarget.y - cop.y, nextTarget.x - cop.x)
            cop.copy(patrolIndex = nextIdx, facingAngle = newAngle)
        } else {
            val angle = atan2(dy, dx)
            val speed = cop.speed
            var newX = cop.x + cos(angle).toFloat() * speed * dt
            var newY = cop.y + sin(angle).toFloat() * speed * dt
            if (!map.isWalkableF(newX, cop.y, GameState.COP_RADIUS)) newX = cop.x
            if (!map.isWalkableF(cop.x, newY, GameState.COP_RADIUS)) newY = cop.y
            cop.copy(x = newX, y = newY, facingAngle = angle)
        }
    }

    fun isInVisionCone(cop: Cop, px: Float, py: Float, map: GameMap): Boolean {
        val dx = px - cop.x
        val dy = py - cop.y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist > cop.visionRange) return false

        val angleToPlayer = atan2(dy, dx)
        var angleDiff = abs(angleToPlayer - cop.facingAngle)
        if (angleDiff > PI) angleDiff = (2 * PI - angleDiff).toFloat()

        val halfAngleRad = cop.visionAngle * PI.toFloat() / 180f
        if (angleDiff > halfAngleRad) return false

        return hasLineOfSight(cop.x, cop.y, px, py, map)
    }

    private fun hasLineOfSight(x1: Float, y1: Float, x2: Float, y2: Float, map: GameMap): Boolean {
        val steps = (maxOf(abs(x2 - x1), abs(y2 - y1)) * 3).toInt().coerceAtLeast(1)
        for (i in 1 until steps) {
            val t = i.toFloat() / steps
            val cx = x1 + (x2 - x1) * t
            val cy = y1 + (y2 - y1) * t
            if (map.tileAt(cx.toInt(), cy.toInt()) == TileType.WALL) return false
        }
        return true
    }

    private fun checkLootCollection(state: GameState): GameState {
        val px = state.player.x.toInt()
        val py = state.player.y.toInt()

        if (state.map.tileAt(px, py) == TileType.LOOT) {
            val newLootPos = state.lootPositions.filter { it.x != px || it.y != py }
            val collected = state.lootCollected + 1
            val allCollected = collected >= state.totalLoot

            val newTiles = state.map.tiles.mapIndexed { y, row ->
                if (y == py) {
                    row.mapIndexed { x, tile -> if (x == px && tile == TileType.LOOT) TileType.FLOOR else tile }
                } else row
            }

            // Combo system
            val newCombo = if (state.comboTimer > 0f) state.comboCount + 1 else 1
            val comboMultiplier = 1f + (newCombo - 1) * 0.5f
            val basePoints = (100 * state.level * comboMultiplier).toInt()

            val particles = state.particles.toMutableList()
            val particleCount = (8 + newCombo * 3).coerceAtMost(24)
            repeat(particleCount) {
                particles.add(
                    Particle(
                        x = state.player.x, y = state.player.y,
                        vx = Random.nextFloat() * 4f - 2f,
                        vy = Random.nextFloat() * -3f - 1f,
                        life = 1f,
                        type = if (newCombo > 1) ParticleType.COMBO else ParticleType.LOOT
                    )
                )
            }

            val comboText = if (newCombo > 1) " x$newCombo!" else ""
            val texts = state.floatingTexts + FloatingText(
                x = state.player.x, y = state.player.y - 0.5f,
                text = if (allCollected) "EXIT OPEN!" else "+$basePoints$comboText",
                life = 1.5f
            )

            return state.copy(
                map = state.map.copy(tiles = newTiles),
                lootCollected = collected,
                lootPositions = newLootPos,
                score = state.score + basePoints,
                particles = particles,
                floatingTexts = texts,
                exitUnlocked = allCollected,
                comboCount = newCombo,
                comboTimer = GameState.COMBO_WINDOW,
                maxCombo = maxOf(state.maxCombo, newCombo)
            )
        }
        return state
    }

    private fun checkPowerUpCollection(state: GameState): GameState {
        val remaining = mutableListOf<PowerUpItem>()
        var collected: PowerUpItem? = null

        for (pu in state.powerUps) {
            val dx = state.player.x - pu.x
            val dy = state.player.y - pu.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < GameState.POWERUP_RADIUS && collected == null) {
                collected = pu
            } else {
                remaining.add(pu)
            }
        }

        if (collected != null) {
            val particles = state.particles.toMutableList()
            repeat(12) {
                particles.add(Particle(
                    x = collected.x, y = collected.y,
                    vx = Random.nextFloat() * 3f - 1.5f,
                    vy = Random.nextFloat() * 3f - 1.5f,
                    life = 1f,
                    type = ParticleType.POWERUP
                ))
            }

            val duration = when (collected.type) {
                PowerUpType.SMOKE -> 3f
                PowerUpType.SPEED -> 5f
                PowerUpType.GHOST -> 3f
            }

            val label = when (collected.type) {
                PowerUpType.SMOKE -> "SMOKE BOMB!"
                PowerUpType.SPEED -> "SPEED BOOST!"
                PowerUpType.GHOST -> "GHOST MODE!"
            }

            val texts = state.floatingTexts + FloatingText(
                x = collected.x, y = collected.y - 0.5f,
                text = label,
                life = 2f
            )

            val newCops = if (collected.type == PowerUpType.SMOKE) {
                state.cops.map { cop ->
                    val dx2 = cop.x - collected.x
                    val dy2 = cop.y - collected.y
                    val dist2 = sqrt(dx2 * dx2 + dy2 * dy2)
                    if (dist2 < 5f) {
                        cop.copy(stunTimer = 3f, state = CopState.SEARCH, searchTimer = 3f)
                    } else cop
                }
            } else state.cops

            return state.copy(
                powerUps = remaining,
                activePowerUp = collected.type,
                powerUpTimer = duration,
                particles = particles,
                floatingTexts = texts,
                cops = newCops
            )
        }
        return state
    }

    private fun checkExit(state: GameState): GameState {
        if (!state.exitUnlocked) return state
        val ex = state.map.exitPoint
        val dx = state.player.x - (ex.x + 0.5f)
        val dy = state.player.y - (ex.y + 0.5f)
        val dist = sqrt(dx * dx + dy * dy)

        if (dist < 0.6f) {
            val timeBonus = ((state.backupTimer - state.timeElapsed).coerceAtLeast(0f) * 10).toInt()
            val stealthBonus = if (state.neverSpotted) 500 else 0
            val comboBonus = state.maxCombo * 200
            val levelScore = state.score + timeBonus + stealthBonus + comboBonus

            val particles = state.particles.toMutableList()
            repeat(20) {
                particles.add(Particle(
                    x = state.player.x, y = state.player.y,
                    vx = Random.nextFloat() * 6f - 3f,
                    vy = Random.nextFloat() * 6f - 3f,
                    life = 1.5f,
                    type = ParticleType.ESCAPE
                ))
            }

            return state.copy(
                phase = GamePhase.LEVEL_COMPLETE,
                score = levelScore,
                totalScore = state.totalScore + levelScore,
                particles = particles,
                floatingTexts = state.floatingTexts + FloatingText(
                    x = state.player.x, y = state.player.y - 1f,
                    text = "ESCAPED!",
                    life = 2f
                )
            )
        }
        return state
    }

    private fun checkCaught(state: GameState): GameState {
        if (state.player.isHiding) return state

        for (cop in state.cops) {
            val dx = state.player.x - cop.x
            val dy = state.player.y - cop.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < GameState.CATCH_DISTANCE && cop.state == CopState.CHASE) {
                val particles = state.particles.toMutableList()
                repeat(15) {
                    particles.add(
                        Particle(
                            x = state.player.x, y = state.player.y,
                            vx = Random.nextFloat() * 6f - 3f,
                            vy = Random.nextFloat() * 6f - 3f,
                            life = 1.2f,
                            type = ParticleType.CAUGHT
                        )
                    )
                }

                val newLives = state.lives - 1
                return state.copy(
                    phase = if (newLives <= 0) GamePhase.GAME_OVER else GamePhase.CAUGHT,
                    lives = newLives,
                    particles = particles,
                    screenShake = 1f
                )
            }
        }
        return state
    }

    private fun updateBackup(state: GameState, dt: Float): GameState {
        if (state.backupArrived) return state
        if (state.timeElapsed >= state.backupTimer) {
            val playerGrid = GridPos(state.player.x.toInt(), state.player.y.toInt())
            val backupCop = MapGenerator.generateBackupCop(state.map, playerGrid, Random)

            val texts = state.floatingTexts + FloatingText(
                x = state.player.x, y = state.player.y - 1f,
                text = "BACKUP INCOMING!",
                life = 2f
            )

            return state.copy(
                cops = state.cops + listOfNotNull(backupCop),
                backupArrived = true,
                floatingTexts = texts,
                screenShake = 0.5f
            )
        }
        return state
    }

    private fun updatePowerUp(state: GameState, dt: Float): GameState {
        if (state.activePowerUp == null) return state
        val newTimer = state.powerUpTimer - dt
        return if (newTimer <= 0f) {
            state.copy(activePowerUp = null, powerUpTimer = 0f)
        } else {
            state.copy(powerUpTimer = newTimer)
        }
    }

    private fun updateCombo(state: GameState, dt: Float): GameState {
        if (state.comboTimer <= 0f) return state
        val newTimer = state.comboTimer - dt
        return if (newTimer <= 0f) {
            state.copy(comboTimer = 0f, comboCount = 0)
        } else {
            state.copy(comboTimer = newTimer)
        }
    }

    private fun updateScreenShake(state: GameState, dt: Float): GameState {
        if (state.screenShake <= 0f) return state
        return state.copy(screenShake = (state.screenShake - dt * 3f).coerceAtLeast(0f))
    }

    private fun updateParticles(state: GameState, dt: Float): GameState {
        val updated = state.particles.mapNotNull { p ->
            val newLife = p.life - dt * 1.5f
            if (newLife <= 0f) return@mapNotNull null
            p.copy(
                x = p.x + p.vx * dt,
                y = p.y + p.vy * dt,
                life = newLife
            )
        }
        return state.copy(particles = updated)
    }

    private fun updateFloatingTexts(state: GameState, dt: Float): GameState {
        val updated = state.floatingTexts.mapNotNull { ft ->
            val newLife = ft.life - dt
            if (newLife <= 0f) return@mapNotNull null
            ft.copy(y = ft.y - dt * 0.5f, life = newLife)
        }
        return state.copy(floatingTexts = updated)
    }

    private fun updateCamera(state: GameState, dt: Float): GameState {
        val targetX = state.player.x
        val targetY = state.player.y
        val lerp = 5f * dt
        val newCamX = state.cameraX + (targetX - state.cameraX) * lerp
        val newCamY = state.cameraY + (targetY - state.cameraY) * lerp
        return state.copy(cameraX = newCamX, cameraY = newCamY)
    }

    private fun updateSpotWarning(state: GameState): GameState {
        var maxWarning = 0f
        for (cop in state.cops) {
            val dx = state.player.x - cop.x
            val dy = state.player.y - cop.y
            val dist = sqrt(dx * dx + dy * dy)
            if (cop.state == CopState.CHASE) {
                maxWarning = maxOf(maxWarning, 1f)
            } else if (cop.state == CopState.ALERT) {
                maxWarning = maxOf(maxWarning, 0.7f)
            } else if (dist < cop.visionRange * 1.2f) {
                val proximity = 1f - (dist / (cop.visionRange * 1.2f))
                maxWarning = maxOf(maxWarning, proximity * 0.4f)
            }
        }
        return state.copy(spotWarning = maxWarning)
    }

    fun processJoystickInput(state: GameState, center: Offset, current: Offset): GameState {
        val dx = current.x - center.x
        val dy = current.y - center.y
        val dist = sqrt(dx * dx + dy * dy)

        return if (dist > 15f) {
            val normalizedDx = dx / dist
            val normalizedDy = dy / dist
            // Sneak: inner drag = sneak, outer drag = run
            val isSneaking = dist < 80f
            state.copy(
                player = state.player.copy(
                    moveDir = Offset(normalizedDx, normalizedDy),
                    isRunning = !isSneaking
                ),
                joystickCenter = center,
                joystickDrag = current,
                joystickActive = true
            )
        } else {
            state.copy(
                player = state.player.copy(moveDir = Offset.Zero),
                joystickCenter = center,
                joystickDrag = center,
                joystickActive = true
            )
        }
    }

    fun releaseJoystick(state: GameState): GameState {
        return state.copy(
            player = state.player.copy(moveDir = Offset.Zero),
            joystickActive = false
        )
    }
}
