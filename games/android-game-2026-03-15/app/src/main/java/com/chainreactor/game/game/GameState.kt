package com.chainreactor.game.game

import androidx.compose.ui.graphics.Color

enum class GamePhase { READY, PLAYING, CHAIN_ACTIVE, ROUND_COMPLETE, GAME_OVER }

enum class OrbType {
    NORMAL,     // Standard orb — explodes on contact
    MEGA,       // Large explosion radius
    SPLITTER,   // Sends 4 projectile sparks outward
    FREEZE,     // Slows nearby orbs before detonating
    GOLDEN      // Worth 3x points
}

data class Orb(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    val type: OrbType,
    val color: Color,
    val alive: Boolean = true,
    val detonating: Boolean = false,
    val detonateTimer: Float = 0f,
    val frozen: Boolean = false,
    val pulsePhase: Float = 0f
)

data class Explosion(
    val x: Float,
    val y: Float,
    val radius: Float,
    val maxRadius: Float,
    val life: Float = 1f,
    val color: Color,
    val isPlayerSpark: Boolean = false
)

data class Projectile(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val life: Float = 1f,
    val radius: Float = 6f
)

data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val life: Float,
    val maxLife: Float,
    val size: Float,
    val color: Color
)

data class Star(
    val x: Float,
    val y: Float,
    val size: Float,
    val alpha: Float,
    val twinkleSpeed: Float,
    val twinklePhase: Float
)

data class ScorePopup(
    val x: Float,
    val y: Float,
    val text: String,
    val life: Float = 1f,
    val color: Color = Color(0xFFFFD740)
)

data class GameState(
    val screenWidth: Float = 0f,
    val screenHeight: Float = 0f,
    val phase: GamePhase = GamePhase.READY,
    // Round system
    val round: Int = 1,
    val sparksRemaining: Int = 3,
    val sparksTotal: Int = 3,
    // Orbs
    val orbs: List<Orb> = emptyList(),
    val totalOrbsThisRound: Int = 0,
    // Effects
    val explosions: List<Explosion> = emptyList(),
    val projectiles: List<Projectile> = emptyList(),
    val particles: List<Particle> = emptyList(),
    val scorePopups: List<ScorePopup> = emptyList(),
    val stars: List<Star> = emptyList(),
    // Scoring
    val score: Int = 0,
    val roundScore: Int = 0,
    val chainLength: Int = 0,
    val maxChain: Int = 0,
    val bestScore: Int = 0,
    val orbsDetonated: Int = 0,
    val totalOrbsDetonated: Int = 0,
    // Timing
    val gameTime: Float = 0f,
    val chainTimer: Float = 0f,
    val roundCompleteTimer: Float = 0f,
    // Slow-mo for dramatic chain reactions
    val slowMoTimer: Float = 0f,
    val timeScale: Float = 1f,
    // Screen shake
    val shakeTimer: Float = 0f,
    val shakeIntensity: Float = 0f
)
