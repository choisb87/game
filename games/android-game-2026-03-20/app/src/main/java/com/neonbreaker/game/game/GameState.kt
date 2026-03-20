package com.neonbreaker.game.game

data class GameState(
    val phase: GamePhase = GamePhase.MENU,
    val level: Int = 1,
    val lives: Int = 3,
    val score: Int = 0,
    val highScore: Int = 0,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    val comboTimer: Float = 0f,       // seconds remaining before combo resets
    val balls: List<Ball> = emptyList(),
    val bricks: List<Brick> = emptyList(),
    val paddle: Paddle = Paddle(0f, 0f, 120f),
    val powerUps: List<PowerUp> = emptyList(),
    val particles: List<Particle> = emptyList(),
    val shake: ScreenShake = ScreenShake(),
    // Active power-up timers (expiry timestamp in ms)
    val wideUntil: Long = 0L,
    val fireballUntil: Long = 0L,
    val slowUntil: Long = 0L,
    // Canvas dimensions
    val canvasW: Float = 0f,
    val canvasH: Float = 0f,
    // Touch state
    val touchX: Float = -1f,
    // Stats
    val totalBricksThisLevel: Int = 0,
)
