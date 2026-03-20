package com.neonbreaker.game.game

import androidx.compose.ui.graphics.Color

// ── Brick ───────────────────────────────────────────────────────────────
enum class BrickType(val hits: Int, val color: Color, val glowColor: Color, val points: Int) {
    NORMAL_CYAN(1, Color(0xFF00DDDD), Color(0xFF00FFFF), 10),
    NORMAL_MAGENTA(1, Color(0xFFDD00DD), Color(0xFFFF00FF), 10),
    NORMAL_GREEN(1, Color(0xFF00DD66), Color(0xFF00FF88), 10),
    NORMAL_ORANGE(1, Color(0xFFDD6600), Color(0xFFFF8800), 10),
    NORMAL_YELLOW(1, Color(0xFFDDDD00), Color(0xFFFFFF00), 10),
    NORMAL_BLUE(1, Color(0xFF3366DD), Color(0xFF4488FF), 10),
    TOUGH_RED(2, Color(0xFFCC2222), Color(0xFFFF4444), 25),
    TOUGH_PURPLE(2, Color(0xFF8822CC), Color(0xFFAA44FF), 25),
    ARMOR(3, Color(0xFF888888), Color(0xFFBBBBBB), 50),
}

data class Brick(
    val row: Int,
    val col: Int,
    val type: BrickType,
    val hitsLeft: Int = type.hits,
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f,
)

// ── Ball ────────────────────────────────────────────────────────────────
data class Ball(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float = 10f,
    val fireball: Boolean = false,
)

// ── Paddle ──────────────────────────────────────────────────────────────
data class Paddle(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float = 14f,
)

// ── Power-ups ───────────────────────────────────────────────────────────
enum class PowerUpType(val symbol: String, val color: Color, val duration: Long) {
    WIDE_PADDLE("⬌", Color(0xFF00FFFF), 8000L),
    MULTI_BALL("✦", Color(0xFFFF00FF), 0L),
    FIREBALL("🔥", Color(0xFFFF6600), 6000L),
    SLOW_BALL("◎", Color(0xFF00FF88), 7000L),
    EXTRA_LIFE("♥", Color(0xFFFF4466), 0L),
}

data class PowerUp(
    val type: PowerUpType,
    val x: Float,
    val y: Float,
    val vy: Float = 150f,
    val size: Float = 24f,
)

// ── Particles ───────────────────────────────────────────────────────────
data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val life: Float = 1f,      // 1.0 → 0.0
    val decay: Float = 2.5f,   // life reduction per second
    val size: Float = 4f,
)

// ── Screen shake ────────────────────────────────────────────────────────
data class ScreenShake(
    val intensity: Float = 0f,
    val decay: Float = 8f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
)

// ── Game phase ──────────────────────────────────────────────────────────
enum class GamePhase { MENU, READY, PLAYING, LEVEL_CLEAR, GAME_OVER }
