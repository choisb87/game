package com.gravitywell.game.game

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

object GameLogic {

    private const val GRAVITY_CONSTANT = 800f
    private const val BASE_FALL_SPEED = 80f
    private const val SPAWN_INTERVAL_BASE = 1.8f
    private const val CATCH_ZONE_Y_RATIO = 0.88f
    private const val MISS_Y_RATIO = 0.96f
    private const val MAX_TRAILS = 12
    private const val WELL_DECAY_RATE = 1f
    private const val SHAKE_DECAY = 6f
    private const val SLOW_MO_FACTOR = 0.4f
    private const val SLOW_MO_DURATION = 1.5f

    fun initLevel(state: GameState, level: Int): GameState {
        val totalStars = 10 + level * 3
        val wellCount = 4 + level
        val targets = generateTargets(state.screenWidth, level)

        return state.copy(
            stars = emptyList(),
            wells = emptyList(),
            targets = targets,
            catchEffects = emptyList(),
            level = level,
            wellsRemaining = wellCount,
            starsCaught = 0,
            starsMissed = 0,
            totalStarsInLevel = totalStars,
            starsSpawned = 0,
            combo = 0,
            spawnTimer = 0.5f,
            gameTime = 0f,
            phase = GamePhase.PLAYING,
            nextStarId = state.nextStarId,
            nextWellId = state.nextWellId,
            shakeAmount = 0f,
            slowMotion = false,
            slowMotionTimer = 0f
        )
    }

    private fun generateTargets(screenWidth: Float, level: Int): List<TargetZone> {
        val count = min(2 + level / 2, 4)
        val zoneWidth = screenWidth / count
        val colors = StarColor.entries.shuffled().take(count)

        return List(count) { i ->
            TargetZone(
                id = i,
                centerX = zoneWidth * i + zoneWidth / 2,
                width = zoneWidth,
                color = colors[i]
            )
        }
    }

    fun update(state: GameState, deltaTime: Float): GameState {
        if (state.phase != GamePhase.PLAYING) return state

        val dt = if (state.slowMotion) deltaTime * SLOW_MO_FACTOR else deltaTime
        var s = state.copy(gameTime = state.gameTime + dt)

        // Update slow motion timer
        if (s.slowMotion) {
            val newTimer = s.slowMotionTimer - deltaTime
            s = if (newTimer <= 0f) {
                s.copy(slowMotion = false, slowMotionTimer = 0f)
            } else {
                s.copy(slowMotionTimer = newTimer)
            }
        }

        // Spawn stars
        s = spawnStars(s, dt)

        // Apply gravity from wells to stars
        s = applyGravity(s, dt)

        // Move stars
        s = moveStars(s, dt)

        // Check catches and misses
        s = checkCatches(s)

        // Update well lifetimes
        s = updateWells(s, dt)

        // Update effects
        s = updateEffects(s, dt)

        // Decay shake
        s = s.copy(shakeAmount = max(0f, s.shakeAmount - SHAKE_DECAY * dt))

        // Update target glow
        s = s.copy(targets = s.targets.map { it.copy(glowPhase = (it.glowPhase + dt * 2f) % (2f * PI.toFloat())) })

        // Check level end
        s = checkLevelEnd(s)

        return s
    }

    private fun spawnStars(state: GameState, dt: Float): GameState {
        if (state.starsSpawned >= state.totalStarsInLevel) return state

        val newTimer = state.spawnTimer - dt
        if (newTimer > 0f) return state.copy(spawnTimer = newTimer)

        val spawnInterval = max(0.6f, SPAWN_INTERVAL_BASE - state.level * 0.08f)
        val color = pickStarColor(state.level)
        val starRadius = when (color) {
            StarColor.GOLD -> 11f
            StarColor.SILVER -> 9f
            StarColor.RUBY -> 13f
            StarColor.DIAMOND -> 7f
        }
        val xPadding = 60f
        val x = xPadding + Random.nextFloat() * (state.screenWidth - xPadding * 2)
        val baseSpeed = BASE_FALL_SPEED + state.level * 8f
        val speed = when (color) {
            StarColor.GOLD -> baseSpeed
            StarColor.SILVER -> baseSpeed * 1.4f
            StarColor.RUBY -> baseSpeed * 0.7f
            StarColor.DIAMOND -> baseSpeed * 1.6f
        }
        val horizontalDrift = (Random.nextFloat() - 0.5f) * 30f

        val star = Star(
            id = state.nextStarId,
            pos = Offset(x, -20f),
            vel = Offset(horizontalDrift, speed),
            radius = starRadius,
            color = color
        )

        return state.copy(
            stars = state.stars + star,
            starsSpawned = state.starsSpawned + 1,
            spawnTimer = spawnInterval + Random.nextFloat() * 0.3f,
            nextStarId = state.nextStarId + 1
        )
    }

    private fun pickStarColor(level: Int): StarColor {
        val r = Random.nextFloat()
        return when {
            level < 3 -> if (r < 0.7f) StarColor.GOLD else StarColor.SILVER
            level < 5 -> when {
                r < 0.45f -> StarColor.GOLD
                r < 0.75f -> StarColor.SILVER
                else -> StarColor.RUBY
            }
            else -> when {
                r < 0.3f -> StarColor.GOLD
                r < 0.55f -> StarColor.SILVER
                r < 0.8f -> StarColor.RUBY
                else -> StarColor.DIAMOND
            }
        }
    }

    private fun applyGravity(state: GameState, dt: Float): GameState {
        val updatedStars = state.stars.map { star ->
            if (star.caught || star.missed) return@map star

            var vx = star.vel.x
            var vy = star.vel.y

            for (well in state.wells) {
                val dist = star.pos.distanceTo(well.pos)
                if (dist > well.radius || dist < 5f) continue

                val force = GRAVITY_CONSTANT * well.strength / (dist * dist + 100f)
                val dx = well.pos.x - star.pos.x
                val dy = well.pos.y - star.pos.y
                val norm = dist.coerceAtLeast(1f)

                val massMultiplier = when (star.color) {
                    StarColor.RUBY -> 1.8f
                    StarColor.DIAMOND -> 0.6f
                    else -> 1f
                }

                vx += (dx / norm) * force * dt * massMultiplier
                vy += (dy / norm) * force * dt * massMultiplier
            }

            // Clamp velocity
            val maxSpeed = 600f
            val speed = kotlin.math.sqrt(vx * vx + vy * vy)
            if (speed > maxSpeed) {
                vx = vx / speed * maxSpeed
                vy = vy / speed * maxSpeed
            }

            star.copy(vel = Offset(vx, vy))
        }

        return state.copy(stars = updatedStars)
    }

    private fun moveStars(state: GameState, dt: Float): GameState {
        val updatedStars = state.stars.map { star ->
            if (star.caught || star.missed) return@map star

            val newPos = Offset(
                star.pos.x + star.vel.x * dt,
                star.pos.y + star.vel.y * dt
            )

            // Bounce off side walls
            var vx = star.vel.x
            val clampedX = newPos.x.coerceIn(star.radius, state.screenWidth - star.radius)
            if (clampedX != newPos.x) {
                vx = -vx * 0.7f
            }

            val trail = (listOf(star.pos) + star.trail).take(MAX_TRAILS)

            star.copy(
                pos = Offset(clampedX, newPos.y),
                vel = Offset(vx, star.vel.y),
                trail = trail
            )
        }

        return state.copy(stars = updatedStars)
    }

    private fun checkCatches(state: GameState): GameState {
        val catchY = state.screenHeight * CATCH_ZONE_Y_RATIO
        val missY = state.screenHeight * MISS_Y_RATIO
        var score = state.score
        var combo = state.combo
        var maxCombo = state.maxCombo
        var caught = state.starsCaught
        var missed = state.starsMissed
        var shake = state.shakeAmount
        var slowMo = state.slowMotion
        var slowTimer = state.slowMotionTimer
        val newEffects = mutableListOf<CatchEffect>()

        val updatedStars = state.stars.map { star ->
            if (star.caught || star.missed) return@map star

            if (star.pos.y >= catchY && star.pos.y < missY) {
                // Check if star matches any target zone
                val matchedTarget = state.targets.find { target ->
                    star.pos.x >= target.centerX - target.width / 2 &&
                    star.pos.x <= target.centerX + target.width / 2 &&
                    star.color == target.color
                }

                if (matchedTarget != null) {
                    combo++
                    maxCombo = max(maxCombo, combo)
                    val points = starPoints(star.color, combo)
                    score += points
                    caught++

                    newEffects.add(CatchEffect(star.pos, star.color, combo = combo))

                    if (combo >= 5) {
                        slowMo = true
                        slowTimer = SLOW_MO_DURATION
                    }
                    if (combo >= 3) {
                        shake = 8f + combo * 2f
                    }

                    return@map star.copy(caught = true)
                }

                // Wrong zone = miss
                if (star.pos.y >= missY - 10f) {
                    combo = 0
                    missed++
                    return@map star.copy(missed = true)
                }
            }

            if (star.pos.y >= missY) {
                combo = 0
                missed++
                return@map star.copy(missed = true)
            }

            star
        }

        // Remove stars that are far below screen
        val filteredStars = updatedStars.filter { !it.missed || it.pos.y < state.screenHeight + 50f }

        return state.copy(
            stars = filteredStars,
            score = score,
            combo = combo,
            maxCombo = maxCombo,
            starsCaught = caught,
            starsMissed = missed,
            catchEffects = state.catchEffects + newEffects,
            shakeAmount = shake,
            slowMotion = slowMo,
            slowMotionTimer = slowTimer
        )
    }

    private fun updateWells(state: GameState, dt: Float): GameState {
        val updated = state.wells.mapNotNull { well ->
            val newLife = well.lifeRemaining - dt * WELL_DECAY_RATE
            if (newLife <= 0f) null
            else well.copy(
                lifeRemaining = newLife,
                pulsePhase = (well.pulsePhase + dt * 4f) % (2f * PI.toFloat())
            )
        }
        return state.copy(wells = updated)
    }

    private fun updateEffects(state: GameState, dt: Float): GameState {
        val updated = state.catchEffects.mapNotNull { effect ->
            val newAge = effect.age + dt
            if (newAge > 1.2f) null else effect.copy(age = newAge)
        }
        return state.copy(catchEffects = updated)
    }

    private fun checkLevelEnd(state: GameState): GameState {
        val allDone = state.starsSpawned >= state.totalStarsInLevel &&
            state.stars.all { it.caught || it.missed }

        if (!allDone) return state

        val catchRatio = state.starsCaught.toFloat() / state.totalStarsInLevel
        return if (catchRatio >= 0.4f) {
            val perfectBonus = if (catchRatio >= 1f) 1000 * state.level else 0
            state.copy(
                phase = GamePhase.LEVEL_COMPLETE,
                score = state.score + perfectBonus
            )
        } else {
            state.copy(
                phase = GamePhase.GAME_OVER,
                bestScore = max(state.bestScore, state.score)
            )
        }
    }

    fun placeWell(state: GameState, pos: Offset): GameState {
        if (state.wellsRemaining <= 0 || state.phase != GamePhase.PLAYING) return state

        val strength = when (state.wellMode) {
            WellMode.ATTRACT -> 1f
            WellMode.REPEL -> -0.8f
        }

        val well = GravityWell(
            id = state.nextWellId,
            pos = pos,
            strength = strength,
            radius = 140f + state.level * 5f
        )

        return state.copy(
            wells = state.wells + well,
            wellsRemaining = state.wellsRemaining - 1,
            nextWellId = state.nextWellId + 1
        )
    }

    fun toggleWellMode(state: GameState): GameState {
        return state.copy(
            wellMode = when (state.wellMode) {
                WellMode.ATTRACT -> WellMode.REPEL
                WellMode.REPEL -> WellMode.ATTRACT
            }
        )
    }
}
