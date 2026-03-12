package com.voidrunner.gravity.game

import androidx.compose.ui.geometry.Offset

enum class GamePhase { READY, PLAYING, DEAD, SCORE }

enum class ObstacleType {
    SPIKE_BOTTOM, SPIKE_TOP, SPIKE_BOTH,
    WALL_GAP_BOTTOM, WALL_GAP_TOP, WALL_GAP_CENTER,
    LASER
}

data class Obstacle(
    val x: Float,
    val type: ObstacleType,
    val width: Float = 0.8f,
    val gapCenter: Float = 0.5f,
    val gapSize: Float = 0.35f,
    val laserOn: Boolean = true,
    val laserTimer: Float = 0f
)

data class Crystal(
    val x: Float,
    val y: Float,
    val value: Int = 100,
    val collected: Boolean = false
)

data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val life: Float,
    val color: Long,
    val size: Float
)

data class FloatingText(
    val x: Float,
    val y: Float,
    val text: String,
    val life: Float,
    val color: Long
)

data class GameState(
    val screenWidth: Float = 0f,
    val screenHeight: Float = 0f,

    val phase: GamePhase = GamePhase.READY,

    // Player
    val playerY: Float = FLOOR_Y - PLAYER_SIZE,
    val playerVY: Float = 0f,
    val gravityDir: Int = 1,
    val playerRotation: Float = 0f,
    val isGrounded: Boolean = true,
    val trailPoints: List<Offset> = emptyList(),

    // World scrolling
    val scrollSpeed: Float = BASE_SPEED,
    val distance: Float = 0f,
    val worldOffset: Float = 0f,

    // Entities
    val obstacles: List<Obstacle> = emptyList(),
    val crystals: List<Crystal> = emptyList(),
    val nextSpawnX: Float = 10f,

    // Score
    val score: Int = 0,
    val crystalScore: Int = 0,
    val distanceScore: Int = 0,
    val combo: Int = 0,
    val comboTimer: Float = 0f,
    val maxCombo: Int = 0,
    val bestScore: Int = 0,
    val bestDistance: Float = 0f,
    val totalCrystals: Int = 0,

    // Effects
    val particles: List<Particle> = emptyList(),
    val floatingTexts: List<FloatingText> = emptyList(),
    val screenFlash: Float = 0f,
    val screenShake: Float = 0f,
    val zoneNumber: Int = 1,
    val zoneFlash: Float = 0f,

    // Stars background
    val stars: List<Offset> = emptyList(),

    // Timers
    val deathTimer: Float = 0f,
    val readyPulse: Float = 0f,
    val gameTime: Float = 0f
) {
    companion object {
        const val PLAYER_X_NORM = 0.2f
        const val PLAYER_SIZE = 0.035f
        const val GRAVITY = 3.2f
        const val FLOOR_Y = 0.85f
        const val CEILING_Y = 0.15f
        const val CORRIDOR_TOP = 0.12f
        const val CORRIDOR_BOTTOM = 0.88f
        const val BASE_SPEED = 3.5f
        const val MAX_SPEED = 9f
        const val COMBO_WINDOW = 2.5f
        const val ZONE_DISTANCE = 400f
        const val SPIKE_HEIGHT = 0.07f
        const val CRYSTAL_SIZE = 0.02f
        const val TRAIL_LENGTH = 12
        const val VISIBLE_WORLD_UNITS = 12f
    }
}
