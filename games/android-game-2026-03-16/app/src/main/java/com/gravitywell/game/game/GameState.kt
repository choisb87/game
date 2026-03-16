package com.gravitywell.game.game

import androidx.compose.ui.geometry.Offset
import kotlin.math.sqrt

// A falling star that the player must guide into targets
data class Star(
    val id: Int,
    val pos: Offset,
    val vel: Offset = Offset(0f, 0f),
    val radius: Float = 10f,
    val color: StarColor = StarColor.GOLD,
    val caught: Boolean = false,
    val missed: Boolean = false,
    val trail: List<Offset> = emptyList()
)

enum class StarColor {
    GOLD,      // normal - 100 pts
    SILVER,    // fast - 150 pts
    RUBY,      // heavy (more gravity influence) - 200 pts
    DIAMOND    // tiny + fast - 300 pts
}

// Player-placed gravity well
data class GravityWell(
    val id: Int,
    val pos: Offset,
    val strength: Float = 1f,   // positive = attract, negative = repel
    val radius: Float = 120f,   // influence radius
    val lifeRemaining: Float = 8f, // seconds before fade
    val pulsePhase: Float = 0f
)

// Target zone at the bottom where stars must land
data class TargetZone(
    val id: Int,
    val centerX: Float,
    val width: Float,
    val color: StarColor,
    val glowPhase: Float = 0f
)

data class CatchEffect(
    val pos: Offset,
    val color: StarColor,
    val age: Float = 0f,
    val combo: Int = 1
)

data class GameState(
    val stars: List<Star> = emptyList(),
    val wells: List<GravityWell> = emptyList(),
    val targets: List<TargetZone> = emptyList(),
    val catchEffects: List<CatchEffect> = emptyList(),
    val score: Int = 0,
    val bestScore: Int = 0,
    val level: Int = 1,
    val wellsRemaining: Int = 5,
    val starsCaught: Int = 0,
    val starsMissed: Int = 0,
    val totalStarsInLevel: Int = 12,
    val starsSpawned: Int = 0,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    val spawnTimer: Float = 0f,
    val gameTime: Float = 0f,
    val phase: GamePhase = GamePhase.PLAYING,
    val screenWidth: Float = 1080f,
    val screenHeight: Float = 1920f,
    val nextStarId: Int = 0,
    val nextWellId: Int = 0,
    val shakeAmount: Float = 0f,
    val slowMotion: Boolean = false,
    val slowMotionTimer: Float = 0f,
    val wellMode: WellMode = WellMode.ATTRACT
)

enum class GamePhase {
    PLAYING,
    LEVEL_COMPLETE,
    GAME_OVER
}

enum class WellMode {
    ATTRACT,
    REPEL
}

fun starPoints(color: StarColor, combo: Int): Int {
    val base = when (color) {
        StarColor.GOLD -> 100
        StarColor.SILVER -> 150
        StarColor.RUBY -> 200
        StarColor.DIAMOND -> 300
    }
    return base * combo.coerceAtLeast(1)
}

fun Offset.distanceTo(other: Offset): Float {
    val dx = x - other.x
    val dy = y - other.y
    return sqrt(dx * dx + dy * dy)
}
