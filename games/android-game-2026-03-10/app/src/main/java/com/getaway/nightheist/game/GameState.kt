package com.getaway.nightheist.game

import androidx.compose.ui.geometry.Offset
import kotlin.math.atan2

// --- Tile types ---
enum class TileType {
    FLOOR, WALL, LOOT, EXIT, HIDESPOT
}

// --- Grid position ---
data class GridPos(val x: Int, val y: Int) {
    fun toFloat() = Offset(x.toFloat() + 0.5f, y.toFloat() + 0.5f)
}

// --- Game map ---
data class GameMap(
    val width: Int = 16,
    val height: Int = 22,
    val tiles: List<List<TileType>> = emptyList(),
    val spawnPoint: GridPos = GridPos(1, 1),
    val exitPoint: GridPos = GridPos(14, 20)
) {
    fun tileAt(x: Int, y: Int): TileType {
        if (x < 0 || x >= width || y < 0 || y >= height) return TileType.WALL
        return tiles.getOrNull(y)?.getOrNull(x) ?: TileType.WALL
    }

    fun isWalkable(x: Int, y: Int): Boolean = tileAt(x, y) != TileType.WALL

    fun isWalkableF(fx: Float, fy: Float, radius: Float = 0.3f): Boolean {
        val checks = listOf(
            Pair(fx - radius, fy - radius),
            Pair(fx + radius, fy - radius),
            Pair(fx - radius, fy + radius),
            Pair(fx + radius, fy + radius)
        )
        return checks.all { (cx, cy) ->
            val ix = cx.toInt().coerceIn(0, width - 1)
            val iy = cy.toInt().coerceIn(0, height - 1)
            tileAt(ix, iy) != TileType.WALL
        }
    }
}

// --- Cop AI ---
enum class CopState { PATROL, ALERT, CHASE, SEARCH }

data class Cop(
    val x: Float,
    val y: Float,
    val patrolRoute: List<GridPos>,
    val patrolIndex: Int = 0,
    val state: CopState = CopState.PATROL,
    val facingAngle: Float = 0f,
    val speed: Float = 2.0f,
    val alertTimer: Float = 0f,
    val chaseSpeed: Float = 3.2f,
    val visionRange: Float = 4.5f,
    val visionAngle: Float = 70f, // half-angle in degrees
    val lastKnownPlayerX: Float = 0f,
    val lastKnownPlayerY: Float = 0f,
    val searchTimer: Float = 0f
)

// --- Player ---
data class Player(
    val x: Float = 1.5f,
    val y: Float = 1.5f,
    val isHiding: Boolean = false,
    val moveDir: Offset = Offset.Zero,
    val speed: Float = 3.8f,
    val sneakSpeed: Float = 1.8f,
    val isRunning: Boolean = true
)

// --- Visual effects ---
data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val life: Float = 1f,
    val type: ParticleType = ParticleType.LOOT
)

enum class ParticleType { LOOT, ALERT, ESCAPE, CAUGHT }

data class FloatingText(
    val x: Float,
    val y: Float,
    val text: String,
    val life: Float = 1f
)

// --- Game phase ---
enum class GamePhase { PLAYING, CAUGHT, ESCAPED, LEVEL_COMPLETE, GAME_OVER }

// --- Screen state ---
enum class ScreenState { MENU, PLAYING, RESULT }

// --- Complete game state ---
data class GameState(
    val map: GameMap = GameMap(),
    val player: Player = Player(),
    val cops: List<Cop> = emptyList(),
    val lootCollected: Int = 0,
    val totalLoot: Int = 0,
    val lootPositions: List<GridPos> = emptyList(),
    val timeElapsed: Float = 0f,
    val backupTimer: Float = 45f, // seconds until backup arrives
    val backupArrived: Boolean = false,
    val level: Int = 1,
    val phase: GamePhase = GamePhase.PLAYING,
    val score: Int = 0,
    val totalScore: Int = 0,
    val bestScore: Int = 0,
    val lives: Int = 3,
    val particles: List<Particle> = emptyList(),
    val floatingTexts: List<FloatingText> = emptyList(),
    val cameraX: Float = 0f,
    val cameraY: Float = 0f,
    val screenState: ScreenState = ScreenState.MENU,
    val canvasWidth: Float = 1080f,
    val canvasHeight: Float = 1920f,
    val tileSize: Float = 60f,
    val joystickCenter: Offset = Offset.Zero,
    val joystickDrag: Offset = Offset.Zero,
    val joystickActive: Boolean = false,
    val spotWarning: Float = 0f, // 0-1 edge glow when cop is close
    val exitUnlocked: Boolean = false
) {
    companion object {
        const val PLAYER_RADIUS = 0.3f
        const val COP_RADIUS = 0.35f
        const val LOOT_RADIUS = 0.3f
        const val CATCH_DISTANCE = 0.6f
        const val HIDE_CHECK_RADIUS = 0.4f
    }
}
