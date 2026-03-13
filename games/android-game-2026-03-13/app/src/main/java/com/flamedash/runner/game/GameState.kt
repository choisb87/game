package com.flamedash.runner.game

import kotlin.math.abs

// ── Game phases ──
enum class GamePhase { MENU, READY, PLAYING, DEAD, SCORE }

// ── Dash direction ──
enum class DashDir { LEFT, RIGHT }

// ── Platform types ──
enum class PlatformType { NORMAL, CRUMBLING, BOUNCY, ICE }

// ── Collectible types ──
enum class GemType { FIRE, ICE, EMERALD, AMETHYST }

// ── A platform the player can land on ──
data class Platform(
    val x: Float,
    val y: Float,
    val width: Float,
    val type: PlatformType = PlatformType.NORMAL,
    val crumbleTimer: Float = 0f,
    val visible: Boolean = true
)

// ── A collectible gem ──
data class Gem(
    val x: Float,
    val y: Float,
    val type: GemType,
    val collected: Boolean = false,
    val bobPhase: Float = 0f
)

// ── Particle effect ──
data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val life: Float,
    val maxLife: Float,
    val size: Float,
    val colorIndex: Int // 0=orange, 1=red, 2=yellow, 3=white
)

// ── Lava ember (background decoration) ──
data class Ember(
    val x: Float,
    val y: Float,
    val vy: Float,
    val size: Float,
    val alpha: Float,
    val phase: Float
)

// ── The complete game state (immutable) ──
data class GameState(
    // Screen dimensions
    val screenWidth: Float = 0f,
    val screenHeight: Float = 0f,

    // Phase
    val phase: GamePhase = GamePhase.MENU,

    // Player position & velocity
    val playerX: Float = 0f,
    val playerY: Float = 0f,
    val playerVx: Float = 0f,
    val playerVy: Float = 0f,
    val playerOnGround: Boolean = false,
    val playerFacing: DashDir = DashDir.RIGHT,

    // Dash mechanic
    val dashCooldown: Float = 0f,
    val dashTrailAlpha: Float = 0f,
    val isDashing: Boolean = false,
    val dashGhosts: List<Pair<Float, Float>> = emptyList(), // after-image positions

    // Camera
    val cameraY: Float = 0f,
    val cameraTargetY: Float = 0f,
    val highestPlayerY: Float = 0f,

    // Lava (rising danger)
    val lavaY: Float = 0f,
    val lavaBaseSpeed: Float = 80f,
    val lavaAcceleration: Float = 1.5f,

    // World
    val platforms: List<Platform> = emptyList(),
    val gems: List<Gem> = emptyList(),
    val lastPlatformY: Float = 0f,

    // Score
    val score: Int = 0,
    val heightScore: Int = 0,
    val gemCount: Int = 0,
    val combo: Int = 0,
    val comboTimer: Float = 0f,
    val bestScore: Int = 0,
    val totalGems: Int = 0,

    // Effects
    val particles: List<Particle> = emptyList(),
    val embers: List<Ember> = emptyList(),
    val screenShake: Float = 0f,
    val screenFlash: Float = 0f,
    val lavaGlow: Float = 0f,

    // Timing
    val gameTime: Float = 0f,
    val difficultyLevel: Int = 1,

    // Player trail
    val playerTrail: List<Triple<Float, Float, Float>> = emptyList() // x, y, alpha
) {
    val playerScreenY: Float get() = playerY - cameraY
    val lavaScreenY: Float get() = lavaY - cameraY

    fun isPlayerAboveLava(): Boolean = playerY < lavaY - 30f

    val playerSize: Float get() = 28f
    val platformHeight: Float get() = 14f
}
