package com.skystacker.game.game

import androidx.compose.ui.graphics.Color

data class Block(
    val x: Float,
    val width: Float,
    val color: Color
)

data class FallingPiece(
    val x: Float,
    val y: Float,
    val width: Float,
    val color: Color,
    val velocityX: Float,
    val alpha: Float = 1f
)

data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val color: Color,
    val life: Float = 1f
)

enum class Screen { MENU, PLAYING, GAME_OVER }

data class GameState(
    val screen: Screen = Screen.MENU,
    val stack: List<Block> = emptyList(),
    val currentBlock: Block? = null,
    val movingRight: Boolean = true,
    val speed: Float = 4f,
    val score: Int = 0,
    val bestScore: Int = 0,
    val combo: Int = 0,
    val screenWidth: Float = 0f,
    val screenHeight: Float = 0f,
    val blockHeight: Float = 0f,
    val cameraY: Float = 0f,
    val particles: List<Particle> = emptyList(),
    val fallingPieces: List<FallingPiece> = emptyList(),
    val perfectFlash: Float = 0f,
    val shakeAmount: Float = 0f
)

val BLOCK_COLORS = listOf(
    Color(0xFFFF6B35),  // Orange
    Color(0xFF4ECDC4),  // Teal
    Color(0xFFFFE66D),  // Yellow
    Color(0xFFFF6B6B),  // Coral
    Color(0xFF95E1D3),  // Mint
    Color(0xFFF38181),  // Pink
    Color(0xFFAA96DA),  // Lavender
    Color(0xFF45B7D1),  // Sky Blue
    Color(0xFFDDA0DD),  // Plum
    Color(0xFF98D8C8),  // Sage
)
