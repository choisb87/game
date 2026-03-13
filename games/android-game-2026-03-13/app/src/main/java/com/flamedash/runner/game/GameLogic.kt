package com.flamedash.runner.game

import kotlin.math.*
import kotlin.random.Random

object GameLogic {

    // Track last dt for sub-functions that need it
    private var lastDt: Float = 1f / 60f

    // ── Constants ──
    private const val GRAVITY = 1400f
    private const val JUMP_VELOCITY = -620f
    private const val DASH_SPEED = 800f
    private const val DASH_DURATION = 0.15f
    private const val DASH_COOLDOWN = 0.3f
    private const val BOUNCY_JUMP = -820f
    private const val PLAYER_MOVE_SPEED = 300f
    private const val MAX_FALL_SPEED = 900f
    private const val PLATFORM_MIN_WIDTH = 70f
    private const val PLATFORM_MAX_WIDTH = 140f
    private const val PLATFORM_VERTICAL_GAP = 110f
    private const val PLATFORM_VERTICAL_VARIANCE = 40f
    private const val COMBO_TIMEOUT = 2.0f
    private const val CAMERA_LEAD = 0.35f
    private const val CAMERA_SMOOTH = 4.0f
    private const val CRUMBLE_TIME = 0.5f
    private const val TRAIL_MAX = 8

    // ── Initialize a new game ──
    fun initGame(state: GameState): GameState {
        val w = state.screenWidth
        val h = state.screenHeight
        if (w == 0f) return state

        val startY = h * 0.6f
        val platforms = generateInitialPlatforms(w, h, startY)

        return state.copy(
            phase = GamePhase.READY,
            playerX = w / 2f,
            playerY = startY - 40f,
            playerVx = 0f,
            playerVy = 0f,
            playerOnGround = true,
            playerFacing = DashDir.RIGHT,
            dashCooldown = 0f,
            isDashing = false,
            dashGhosts = emptyList(),
            cameraY = 0f,
            cameraTargetY = 0f,
            highestPlayerY = startY,
            lavaY = h + 200f,
            platforms = platforms,
            gems = emptyList(),
            lastPlatformY = platforms.minOf { it.y },
            score = 0,
            heightScore = 0,
            gemCount = 0,
            combo = 0,
            comboTimer = 0f,
            totalGems = 0,
            particles = emptyList(),
            embers = generateEmbers(w, h),
            screenShake = 0f,
            screenFlash = 0f,
            lavaGlow = 0f,
            gameTime = 0f,
            difficultyLevel = 1,
            playerTrail = emptyList()
        )
    }

    fun startGame(state: GameState): GameState {
        return state.copy(
            phase = GamePhase.PLAYING,
            playerVy = JUMP_VELOCITY * 0.7f,
            playerOnGround = false
        )
    }

    // ── Handle tap input (dash) ──
    fun onTap(state: GameState, tapX: Float): GameState {
        if (state.phase == GamePhase.READY) return startGame(state)
        if (state.phase != GamePhase.PLAYING) return state
        if (state.dashCooldown > 0f) return state

        val dir = if (tapX < state.screenWidth / 2f) DashDir.LEFT else DashDir.RIGHT
        val dashVx = if (dir == DashDir.LEFT) -DASH_SPEED else DASH_SPEED

        // Dash also gives a small vertical boost if falling
        val boostVy = if (state.playerVy > 0f) -250f else state.playerVy * 0.5f

        val ghost = Pair(state.playerX, state.playerY)
        val ghosts = (state.dashGhosts + ghost).takeLast(3)

        return state.copy(
            playerVx = dashVx,
            playerVy = boostVy,
            playerFacing = dir,
            isDashing = true,
            dashCooldown = DASH_COOLDOWN,
            dashTrailAlpha = 1f,
            dashGhosts = ghosts,
            playerOnGround = false,
            particles = state.particles + createDashParticles(state.playerX, state.playerY, dir)
        )
    }

    // ── Main update loop ──
    fun update(state: GameState, dt: Float): GameState {
        if (state.phase != GamePhase.PLAYING) return updateEffectsOnly(state, dt)

        lastDt = dt
        var s = state.copy(gameTime = state.gameTime + dt)

        // Update difficulty
        s = updateDifficulty(s)

        // Physics
        s = updatePlayer(s, dt)

        // Platform collision
        s = checkPlatformCollisions(s)

        // Gem collection
        s = checkGemCollection(s)

        // Camera
        s = updateCamera(s, dt)

        // Lava
        s = updateLava(s, dt)

        // Generate world
        s = generateWorld(s)

        // Effects
        s = updateParticles(s, dt)
        s = updateEmbers(s, dt)
        s = updateEffects(s, dt)

        // Combo timer
        s = updateCombo(s, dt)

        // Score
        s = updateScore(s)

        // Death check
        s = checkDeath(s)

        // Trail
        s = updateTrail(s)

        return s
    }

    // ── Player physics ──
    private fun updatePlayer(state: GameState, dt: Float): GameState {
        var vx = state.playerVx
        var vy = state.playerVy
        var x = state.playerX
        var y = state.playerY
        var dashing = state.isDashing
        var dashCd = state.dashCooldown

        // Gravity
        vy += GRAVITY * dt
        vy = min(vy, MAX_FALL_SPEED)

        // Dash deceleration
        if (dashing) {
            if (dashCd < DASH_COOLDOWN - DASH_DURATION) {
                dashing = false
            }
        }

        // Horizontal drag
        if (!dashing) {
            vx *= (1f - 5f * dt)
            if (abs(vx) < 10f) vx = 0f
        }

        // Dash cooldown
        dashCd = max(0f, dashCd - dt)

        // Apply velocity
        x += vx * dt
        y += vy * dt

        // Screen wrap horizontally
        val w = state.screenWidth
        if (x < -state.playerSize) x = w + state.playerSize
        if (x > w + state.playerSize) x = -state.playerSize

        return state.copy(
            playerX = x,
            playerY = y,
            playerVx = vx,
            playerVy = vy,
            isDashing = dashing,
            dashCooldown = dashCd,
            dashTrailAlpha = max(0f, state.dashTrailAlpha - dt * 4f)
        )
    }

    // ── Platform collision ──
    private fun checkPlatformCollisions(state: GameState): GameState {
        if (state.playerVy < 0f) return state // moving upward, no collision

        val px = state.playerX
        val py = state.playerY
        val pSize = state.playerSize
        val pHalf = pSize / 2f
        val dt = lastDt

        var newState = state
        val updatedPlatforms = state.platforms.toMutableList()
        var landed = false

        for (i in state.platforms.indices) {
            val plat = state.platforms[i]
            if (!plat.visible) continue

            val platTop = plat.y
            val platLeft = plat.x
            val platRight = plat.x + plat.width

            // Check if player feet are near platform top
            val feetY = py + pHalf
            val prevFeetY = feetY - state.playerVy * dt

            if (feetY >= platTop && prevFeetY <= platTop + 20f &&
                px + pHalf > platLeft && px - pHalf < platRight) {

                val jumpV = when (plat.type) {
                    PlatformType.BOUNCY -> BOUNCY_JUMP
                    PlatformType.ICE -> JUMP_VELOCITY * 0.9f
                    else -> JUMP_VELOCITY
                }

                var vx = state.playerVx
                if (plat.type == PlatformType.ICE) {
                    // Ice: keep some horizontal momentum
                    vx *= 0.95f
                }

                newState = newState.copy(
                    playerY = platTop - pHalf,
                    playerVy = jumpV,
                    playerVx = vx,
                    playerOnGround = true,
                    particles = newState.particles + createLandParticles(px, platTop, plat.type)
                )

                // Crumbling platform
                if (plat.type == PlatformType.CRUMBLING) {
                    updatedPlatforms[i] = plat.copy(crumbleTimer = CRUMBLE_TIME)
                }

                // Bouncy platform effect
                if (plat.type == PlatformType.BOUNCY) {
                    newState = newState.copy(
                        screenShake = 0.15f,
                        particles = newState.particles + createBounceParticles(px, platTop)
                    )
                }

                landed = true
                break
            }
        }

        // Update crumbling platforms
        val finalPlatforms = updatedPlatforms.map { p ->
            if (p.crumbleTimer > 0f) {
                val newTimer = p.crumbleTimer - dt
                if (newTimer <= 0f) p.copy(visible = false, crumbleTimer = 0f)
                else p.copy(crumbleTimer = newTimer)
            } else p
        }

        return newState.copy(platforms = finalPlatforms)
    }

    // ── Gem collection ──
    private fun checkGemCollection(state: GameState): GameState {
        val px = state.playerX
        val py = state.playerY
        val collectRadius = 35f
        var gemCount = state.gemCount
        var combo = state.combo
        var comboTimer = state.comboTimer
        var particles = state.particles
        var totalGems = state.totalGems

        val updatedGems = state.gems.map { gem ->
            if (!gem.collected) {
                val dx = px - gem.x
                val dy = py - gem.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < collectRadius) {
                    combo++
                    comboTimer = COMBO_TIMEOUT
                    gemCount++
                    totalGems++
                    particles = particles + createGemParticles(gem.x, gem.y, gem.type)
                    gem.copy(collected = true)
                } else gem
            } else gem
        }

        return state.copy(
            gems = updatedGems,
            gemCount = gemCount,
            combo = combo,
            comboTimer = comboTimer,
            particles = particles,
            totalGems = totalGems
        )
    }

    // ── Camera ──
    private fun updateCamera(state: GameState, dt: Float): GameState {
        val targetY = state.playerY - state.screenHeight * CAMERA_LEAD
        val newHighest = min(state.highestPlayerY, state.playerY)
        val camTarget = min(state.cameraTargetY, targetY)

        val cam = state.cameraY + (camTarget - state.cameraY) * min(1f, CAMERA_SMOOTH * dt)

        return state.copy(
            cameraY = cam,
            cameraTargetY = camTarget,
            highestPlayerY = newHighest
        )
    }

    // ── Lava ──
    private fun updateLava(state: GameState, dt: Float): GameState {
        val speed = state.lavaBaseSpeed + state.lavaAcceleration * state.gameTime
        val newLavaY = state.lavaY - speed * dt
        val glow = (sin(state.gameTime * 3f) * 0.3f + 0.7f).toFloat()

        return state.copy(
            lavaY = newLavaY,
            lavaGlow = glow
        )
    }

    // ── World generation ──
    private fun generateWorld(state: GameState): GameState {
        val visibleTop = state.cameraY - 300f

        if (state.lastPlatformY > visibleTop) {
            var lastY = state.lastPlatformY
            val newPlatforms = mutableListOf<Platform>()
            val newGems = mutableListOf<Gem>()
            val w = state.screenWidth

            repeat(15) {
                val gap = PLATFORM_VERTICAL_GAP +
                    Random.nextFloat() * PLATFORM_VERTICAL_VARIANCE -
                    PLATFORM_VERTICAL_VARIANCE / 2f +
                    state.difficultyLevel * 3f

                lastY -= gap

                val platWidth = Random.nextFloat() *
                    (PLATFORM_MAX_WIDTH - PLATFORM_MIN_WIDTH) + PLATFORM_MIN_WIDTH -
                    state.difficultyLevel * 3f

                val clampedWidth = max(50f, platWidth)
                val platX = Random.nextFloat() * (w - clampedWidth)

                val type = pickPlatformType(state.difficultyLevel)

                newPlatforms.add(Platform(
                    x = platX,
                    y = lastY,
                    width = clampedWidth,
                    type = type
                ))

                // Gems
                if (Random.nextFloat() < 0.35f) {
                    val gemType = GemType.entries[Random.nextInt(GemType.entries.size)]
                    newGems.add(Gem(
                        x = platX + clampedWidth / 2f,
                        y = lastY - 40f,
                        type = gemType,
                        bobPhase = Random.nextFloat() * PI.toFloat() * 2f
                    ))
                }
            }

            // Remove platforms far below lava
            val trimmedPlatforms = (state.platforms + newPlatforms)
                .filter { it.y < state.lavaY + 200f }
            val trimmedGems = (state.gems + newGems)
                .filter { !it.collected && it.y < state.lavaY + 200f }

            return state.copy(
                platforms = trimmedPlatforms,
                gems = trimmedGems,
                lastPlatformY = lastY
            )
        }

        // Still trim old platforms/gems
        return state.copy(
            platforms = state.platforms.filter { it.y < state.lavaY + 200f },
            gems = state.gems.filter { !it.collected && it.y < state.lavaY + 200f }
        )
    }

    // ── Platform type selection ──
    private fun pickPlatformType(difficulty: Int): PlatformType {
        val r = Random.nextFloat()
        return when {
            difficulty < 3 -> {
                if (r < 0.8f) PlatformType.NORMAL
                else if (r < 0.95f) PlatformType.BOUNCY
                else PlatformType.CRUMBLING
            }
            difficulty < 6 -> {
                if (r < 0.55f) PlatformType.NORMAL
                else if (r < 0.75f) PlatformType.BOUNCY
                else if (r < 0.9f) PlatformType.CRUMBLING
                else PlatformType.ICE
            }
            else -> {
                if (r < 0.35f) PlatformType.NORMAL
                else if (r < 0.55f) PlatformType.BOUNCY
                else if (r < 0.8f) PlatformType.CRUMBLING
                else PlatformType.ICE
            }
        }
    }

    // ── Difficulty ──
    private fun updateDifficulty(state: GameState): GameState {
        val level = (state.gameTime / 15f).toInt() + 1
        return state.copy(difficultyLevel = min(level, 10))
    }

    // ── Combo ──
    private fun updateCombo(state: GameState, dt: Float): GameState {
        if (state.comboTimer <= 0f) return state.copy(combo = 0, comboTimer = 0f)
        return state.copy(comboTimer = max(0f, state.comboTimer - dt))
    }

    // ── Score ──
    private fun updateScore(state: GameState): GameState {
        val heightPts = ((state.screenHeight * 0.6f - state.highestPlayerY) / 10f).toInt()
            .coerceAtLeast(0)
        val gemPts = state.totalGems * 50
        val total = heightPts + gemPts

        return state.copy(
            heightScore = heightPts,
            score = total
        )
    }

    // ── Death ──
    private fun checkDeath(state: GameState): GameState {
        val playerBottom = state.playerY + state.playerSize / 2f
        if (playerBottom > state.lavaY - 10f) {
            return state.copy(
                phase = GamePhase.DEAD,
                screenShake = 0.4f,
                screenFlash = 1f,
                bestScore = max(state.bestScore, state.score),
                particles = state.particles + createDeathParticles(state.playerX, state.playerY)
            )
        }
        return state
    }

    // ── Effects-only update (for menus / death) ──
    private fun updateEffectsOnly(state: GameState, dt: Float): GameState {
        return state.copy(
            particles = state.particles.mapNotNull { p ->
                val newLife = p.life - dt
                if (newLife <= 0f) null
                else p.copy(
                    x = p.x + p.vx * dt,
                    y = p.y + p.vy * dt,
                    life = newLife
                )
            },
            embers = updateEmberList(state.embers, state, dt),
            screenShake = max(0f, state.screenShake - dt * 3f),
            screenFlash = max(0f, state.screenFlash - dt * 2f),
            gameTime = state.gameTime + dt
        )
    }

    // ── Particles ──
    private fun updateParticles(state: GameState, dt: Float): GameState {
        val updated = state.particles.mapNotNull { p ->
            val newLife = p.life - dt
            if (newLife <= 0f) null
            else p.copy(
                x = p.x + p.vx * dt,
                y = p.y + p.vy * dt,
                life = newLife
            )
        }.takeLast(100)
        return state.copy(particles = updated)
    }

    // ── Embers ──
    private fun updateEmbers(state: GameState, dt: Float): GameState {
        return state.copy(embers = updateEmberList(state.embers, state, dt))
    }

    private fun updateEmberList(embers: List<Ember>, state: GameState, dt: Float): List<Ember> {
        return embers.map { e ->
            var y = e.y + e.vy * dt
            val phase = e.phase + dt * 2f
            if (y < state.cameraY - 50f) {
                y = state.lavaY + Random.nextFloat() * 50f
            }
            e.copy(y = y, phase = phase)
        }
    }

    // ── Screen effects ──
    private fun updateEffects(state: GameState, dt: Float): GameState {
        return state.copy(
            screenShake = max(0f, state.screenShake - dt * 3f),
            screenFlash = max(0f, state.screenFlash - dt * 2f)
        )
    }

    // ── Player trail ──
    private fun updateTrail(state: GameState): GameState {
        val entry = Triple(state.playerX, state.playerY, 1f)
        val trail = (state.playerTrail + entry).takeLast(TRAIL_MAX).map {
            Triple(it.first, it.second, it.third * 0.85f)
        }
        return state.copy(playerTrail = trail)
    }

    // ══════════════════════════════════════
    //  Particle Generators
    // ══════════════════════════════════════

    private fun createDashParticles(x: Float, y: Float, dir: DashDir): List<Particle> {
        val signX = if (dir == DashDir.LEFT) 1f else -1f
        return List(8) {
            Particle(
                x = x + signX * Random.nextFloat() * 15f,
                y = y + Random.nextFloat() * 20f - 10f,
                vx = signX * (50f + Random.nextFloat() * 100f),
                vy = Random.nextFloat() * 60f - 30f,
                life = 0.3f + Random.nextFloat() * 0.2f,
                maxLife = 0.5f,
                size = 3f + Random.nextFloat() * 4f,
                colorIndex = Random.nextInt(3) // orange, red, yellow
            )
        }
    }

    private fun createLandParticles(x: Float, y: Float, type: PlatformType): List<Particle> {
        val ci = when (type) {
            PlatformType.ICE -> 3
            PlatformType.BOUNCY -> 2
            else -> 0
        }
        return List(5) {
            Particle(
                x = x + Random.nextFloat() * 30f - 15f,
                y = y,
                vx = Random.nextFloat() * 120f - 60f,
                vy = -(30f + Random.nextFloat() * 50f),
                life = 0.2f + Random.nextFloat() * 0.15f,
                maxLife = 0.35f,
                size = 2f + Random.nextFloat() * 3f,
                colorIndex = ci
            )
        }
    }

    private fun createBounceParticles(x: Float, y: Float): List<Particle> {
        return List(10) {
            val angle = Random.nextFloat() * PI.toFloat() * 2f
            Particle(
                x = x, y = y,
                vx = cos(angle) * (80f + Random.nextFloat() * 60f),
                vy = -abs(sin(angle)) * (100f + Random.nextFloat() * 80f),
                life = 0.35f + Random.nextFloat() * 0.2f,
                maxLife = 0.55f,
                size = 3f + Random.nextFloat() * 4f,
                colorIndex = 2
            )
        }
    }

    private fun createGemParticles(x: Float, y: Float, type: GemType): List<Particle> {
        val ci = when (type) {
            GemType.FIRE -> 0
            GemType.ICE -> 3
            GemType.EMERALD -> 2
            GemType.AMETHYST -> 1
        }
        return List(8) {
            val angle = Random.nextFloat() * PI.toFloat() * 2f
            Particle(
                x = x, y = y,
                vx = cos(angle) * (60f + Random.nextFloat() * 80f),
                vy = sin(angle) * (60f + Random.nextFloat() * 80f),
                life = 0.4f + Random.nextFloat() * 0.3f,
                maxLife = 0.7f,
                size = 2f + Random.nextFloat() * 5f,
                colorIndex = ci
            )
        }
    }

    private fun createDeathParticles(x: Float, y: Float): List<Particle> {
        return List(30) {
            val angle = Random.nextFloat() * PI.toFloat() * 2f
            val speed = 80f + Random.nextFloat() * 200f
            Particle(
                x = x, y = y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                life = 0.5f + Random.nextFloat() * 0.5f,
                maxLife = 1f,
                size = 3f + Random.nextFloat() * 6f,
                colorIndex = Random.nextInt(4)
            )
        }
    }

    // ══════════════════════════════════════
    //  World generators
    // ══════════════════════════════════════

    private fun generateInitialPlatforms(w: Float, h: Float, startY: Float): List<Platform> {
        val platforms = mutableListOf<Platform>()
        // Starting platform (wide, centered)
        platforms.add(Platform(
            x = w / 2f - 80f,
            y = startY,
            width = 160f,
            type = PlatformType.NORMAL
        ))

        var y = startY
        repeat(20) {
            y -= PLATFORM_VERTICAL_GAP + Random.nextFloat() * PLATFORM_VERTICAL_VARIANCE / 2f
            val platWidth = 80f + Random.nextFloat() * 60f
            val platX = Random.nextFloat() * (w - platWidth)
            platforms.add(Platform(
                x = platX, y = y, width = platWidth,
                type = if (Random.nextFloat() < 0.15f) PlatformType.BOUNCY else PlatformType.NORMAL
            ))
        }
        return platforms
    }

    private fun generateEmbers(w: Float, h: Float): List<Ember> {
        return List(20) {
            Ember(
                x = Random.nextFloat() * w,
                y = Random.nextFloat() * h * 2f,
                vy = -(20f + Random.nextFloat() * 40f),
                size = 2f + Random.nextFloat() * 4f,
                alpha = 0.3f + Random.nextFloat() * 0.5f,
                phase = Random.nextFloat() * PI.toFloat() * 2f
            )
        }
    }
}
