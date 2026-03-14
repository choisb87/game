package com.orbitshield.game.game

import kotlin.math.PI

data class Asteroid(
    val x: Float,
    val y: Float,
    val angle: Float,
    val speed: Float,
    val size: Float,
    val type: Int = 0,
    val rotation: Float = 0f,
    val hp: Int = 1
)

data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val life: Float,
    val maxLife: Float,
    val size: Float,
    val r: Float,
    val g: Float,
    val b: Float
)

data class Star(
    val x: Float,
    val y: Float,
    val brightness: Float,
    val twinkleSpeed: Float,
    val size: Float
)

data class ShieldFragment(
    val angle: Float,
    val vAngle: Float,
    val vRadius: Float,
    val radius: Float,
    val life: Float
)

data class ScorePopup(
    val x: Float,
    val y: Float,
    val text: String,
    val life: Float,
    val vy: Float = -2f
)

enum class GamePhase { READY, PLAYING, GAME_OVER }

data class GameState(
    val screenWidth: Float = 0f,
    val screenHeight: Float = 0f,
    val centerX: Float = 0f,
    val centerY: Float = 0f,

    val phase: GamePhase = GamePhase.READY,

    // Shield
    val shieldAngle: Float = 0f,
    val shieldAngularSpeed: Float = 3.0f,
    val shieldDirection: Int = 1,
    val shieldArc: Float = (PI / 3).toFloat(),
    val shieldRadius: Float = 120f,
    val shieldFlash: Float = 0f,
    val shieldPowered: Boolean = false,
    val shieldPowerTimer: Float = 0f,

    // Planet
    val planetRadius: Float = 40f,
    val planetPulse: Float = 0f,
    val planetHitFlash: Float = 0f,

    // Health
    val lives: Int = 3,
    val maxLives: Int = 3,
    val invincibleTimer: Float = 0f,

    // Asteroids
    val asteroids: List<Asteroid> = emptyList(),
    val spawnTimer: Float = 0f,
    val spawnInterval: Float = 1.2f,
    val minSpawnInterval: Float = 0.25f,

    // Particles & Effects
    val particles: List<Particle> = emptyList(),
    val shieldFragments: List<ShieldFragment> = emptyList(),
    val scorePopups: List<ScorePopup> = emptyList(),
    val screenShake: Float = 0f,
    val screenFlash: Float = 0f,

    // Stars (background)
    val stars: List<Star> = emptyList(),

    // Score
    val score: Int = 0,
    val combo: Int = 0,
    val comboTimer: Float = 0f,
    val bestScore: Int = 0,
    val deflections: Int = 0,
    val maxCombo: Int = 0,

    // Timing & Difficulty
    val gameTime: Float = 0f,
    val difficultyLevel: Int = 1,

    // Power-ups
    val powerUpTimer: Float = 0f,
    val powerUpType: Int = -1,
    val powerUpAngle: Float = 0f,
    val powerUpRadius: Float = 0f,
    val powerUpActive: Boolean = false,

    // Slow-mo
    val slowMoTimer: Float = 0f,
    val timeScale: Float = 1f
)
