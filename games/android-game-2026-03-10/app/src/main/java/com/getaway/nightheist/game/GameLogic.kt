package com.getaway.nightheist.game

import androidx.compose.ui.geometry.Offset
import kotlin.math.*
import kotlin.random.Random

object GameLogic {

    fun initLevel(level: Int, bestScore: Int = 0, totalScore: Int = 0, lives: Int = 3): GameState {
        val map = MapGenerator.generate(level)
        val spawnF = map.spawnPoint.toFloat()
        val cops = MapGenerator.generateCopPatrols(map, level)
        val lootPositions = mutableListOf<GridPos>()
        for (y in 0 until map.height) {
            for (x in 0 until map.width) {
                if (map.tileAt(x, y) == TileType.LOOT) {
                    lootPositions.add(GridPos(x, y))
                }
            }
        }

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
            phase = GamePhase.PLAYING,
            score = 0,
            totalScore = totalScore,
            bestScore = bestScore,
            lives = lives,
            cameraX = spawnF.x,
            cameraY = spawnF.y,
            exitUnlocked = lootPositions.isEmpty(),
            screenState = ScreenState.PLAYING
        )
    }

    fun update(state: GameState, deltaTime: Float): GameState {
        if (state.phase != GamePhase.PLAYING) return state
        val dt = deltaTime.coerceAtMost(0.05f)

        var s = state.copy(timeElapsed = state.timeElapsed + dt)

        // Update player
        s = updatePlayer(s, dt)

        // Update cops
        s = updateCops(s, dt)

        // Check loot collection
        s = checkLootCollection(s)

        // Check exit
        s = checkExit(s)

        // Check caught
        s = checkCaught(s)

        // Backup timer
        s = updateBackup(s, dt)

        // Update particles
        s = updateParticles(s, dt)

        // Update floating texts
        s = updateFloatingTexts(s, dt)

        // Update camera
        s = updateCamera(s, dt)

        // Spot warning
        s = updateSpotWarning(s)

        return s
    }

    private fun updatePlayer(state: GameState, dt: Float): GameState {
        val p = state.player
        val dir = p.moveDir
        if (dir == Offset.Zero) {
            // Check if on hide spot
            val tileX = p.x.toInt()
            val tileY = p.y.toInt()
            val onHideSpot = state.map.tileAt(tileX, tileY) == TileType.HIDESPOT
            return state.copy(player = p.copy(isHiding = onHideSpot))
        }

        val speed = if (p.isRunning) p.speed else p.sneakSpeed
        var newX = p.x + dir.x * speed * dt
        var newY = p.y + dir.y * speed * dt

        // Wall collision
        if (!state.map.isWalkableF(newX, p.y, GameState.PLAYER_RADIUS)) {
            newX = p.x
        }
        if (!state.map.isWalkableF(p.x, newY, GameState.PLAYER_RADIUS)) {
            newY = p.y
        }
        // Try diagonal if both axes blocked
        if (newX == p.x && newY == p.y) {
            // No movement
        } else if (!state.map.isWalkableF(newX, newY, GameState.PLAYER_RADIUS)) {
            // One axis only
            if (state.map.isWalkableF(newX, p.y, GameState.PLAYER_RADIUS)) {
                newY = p.y
            } else {
                newX = p.x
            }
        }

        return state.copy(
            player = p.copy(
                x = newX,
                y = newY,
                isHiding = false
            )
        )
    }

    private fun updateCops(state: GameState, dt: Float): GameState {
        val updatedCops = state.cops.map { cop ->
            updateSingleCop(cop, state, dt)
        }
        return state.copy(cops = updatedCops)
    }

    private fun updateSingleCop(cop: Cop, state: GameState, dt: Float): Cop {
        val player = state.player
        val canSeePlayer = !player.isHiding && isInVisionCone(cop, player.x, player.y, state.map)

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
                    // Turn toward player
                    val angle = atan2(player.y - cop.y, player.x - cop.x)
                    cop.copy(alertTimer = newTimer, facingAngle = angle)
                }
            }
            CopState.CHASE -> {
                if (canSeePlayer) {
                    // Chase directly toward player
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
                    // Move to last known position
                    val dx = cop.lastKnownPlayerX - cop.x
                    val dy = cop.lastKnownPlayerY - cop.y
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist < 0.5f) {
                        // Reached last known pos, start searching
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
                        // Return to patrol
                        cop.copy(state = CopState.PATROL, searchTimer = 0f)
                    } else {
                        // Rotate and look around
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

        // Simple line-of-sight check
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

            // Modify map tile
            val newTiles = state.map.tiles.mapIndexed { y, row ->
                if (y == py) {
                    row.mapIndexed { x, tile -> if (x == px && tile == TileType.LOOT) TileType.FLOOR else tile }
                } else row
            }

            val particles = state.particles.toMutableList()
            repeat(8) {
                particles.add(
                    Particle(
                        x = state.player.x, y = state.player.y,
                        vx = Random.nextFloat() * 4f - 2f,
                        vy = Random.nextFloat() * -3f - 1f,
                        life = 1f,
                        type = ParticleType.LOOT
                    )
                )
            }

            val texts = state.floatingTexts + FloatingText(
                x = state.player.x, y = state.player.y - 0.5f,
                text = if (allCollected) "탈출구 개방!" else "+${100 * state.level}",
                life = 1.5f
            )

            return state.copy(
                map = state.map.copy(tiles = newTiles),
                lootCollected = collected,
                lootPositions = newLootPos,
                score = state.score + 100 * state.level,
                particles = particles,
                floatingTexts = texts,
                exitUnlocked = allCollected
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
            // Time bonus: faster = more points
            val timeBonus = ((state.backupTimer - state.timeElapsed).coerceAtLeast(0f) * 10).toInt()
            // Stealth bonus: never spotted
            val stealthBonus = if (state.cops.none { it.state == CopState.CHASE || it.state == CopState.SEARCH }) 500 else 0
            val levelScore = state.score + timeBonus + stealthBonus

            return state.copy(
                phase = GamePhase.LEVEL_COMPLETE,
                score = levelScore,
                totalScore = state.totalScore + levelScore,
                floatingTexts = state.floatingTexts + FloatingText(
                    x = state.player.x, y = state.player.y - 1f,
                    text = "탈출 성공!",
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
                    particles = particles
                )
            }
        }
        return state
    }

    private fun updateBackup(state: GameState, dt: Float): GameState {
        if (state.backupArrived) return state
        if (state.timeElapsed >= state.backupTimer) {
            // Spawn backup cop
            val playerGrid = GridPos(state.player.x.toInt(), state.player.y.toInt())
            val backupCop = MapGenerator.generateBackupCop(state.map, playerGrid, Random)

            val texts = state.floatingTexts + FloatingText(
                x = state.player.x, y = state.player.y - 1f,
                text = "⚠ 증원 도착!",
                life = 2f
            )

            return state.copy(
                cops = state.cops + listOfNotNull(backupCop),
                backupArrived = true,
                floatingTexts = texts
            )
        }
        return state
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
            state.copy(
                player = state.player.copy(moveDir = Offset(normalizedDx, normalizedDy)),
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
