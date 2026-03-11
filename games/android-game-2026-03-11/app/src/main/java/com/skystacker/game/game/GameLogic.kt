package com.skystacker.game.game

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object GameLogic {

    private const val PERFECT_THRESHOLD = 4f
    private const val INITIAL_BLOCK_WIDTH_RATIO = 0.4f
    private const val MIN_BLOCK_WIDTH = 20f
    private const val BASE_SPEED = 3.5f
    private const val SPEED_INCREMENT = 0.25f
    private const val MAX_SPEED = 14f
    private const val CAMERA_LERP = 0.08f

    fun initGame(screenWidth: Float, screenHeight: Float): GameState {
        val blockHeight = screenHeight / 18f
        val initialWidth = screenWidth * INITIAL_BLOCK_WIDTH_RATIO
        val baseBlock = Block(
            x = (screenWidth - initialWidth) / 2f,
            width = initialWidth,
            color = BLOCK_COLORS[0]
        )
        val startBlock = Block(
            x = 0f,
            width = initialWidth,
            color = BLOCK_COLORS[1]
        )
        return GameState(
            screen = Screen.PLAYING,
            stack = listOf(baseBlock),
            currentBlock = startBlock,
            movingRight = true,
            speed = BASE_SPEED,
            score = 0,
            combo = 0,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            blockHeight = blockHeight,
            cameraY = 0f
        )
    }

    fun update(state: GameState, deltaMs: Long): GameState {
        if (state.screen != Screen.PLAYING || state.currentBlock == null) {
            return updateEffects(state, deltaMs)
        }

        val dt = deltaMs / 16f
        val block = state.currentBlock
        val newX = if (state.movingRight) {
            block.x + state.speed * dt
        } else {
            block.x - state.speed * dt
        }

        val shouldReverse = if (state.movingRight) {
            newX + block.width > state.screenWidth
        } else {
            newX < 0f
        }

        val clampedX = newX.coerceIn(0f, state.screenWidth - block.width)
        val newDirection = if (shouldReverse) !state.movingRight else state.movingRight

        val targetCameraY = if (state.stack.size > 8) {
            (state.stack.size - 8) * state.blockHeight
        } else 0f
        val newCameraY = state.cameraY + (targetCameraY - state.cameraY) * CAMERA_LERP

        return updateEffects(
            state.copy(
                currentBlock = block.copy(x = clampedX),
                movingRight = newDirection,
                cameraY = newCameraY
            ),
            deltaMs
        )
    }

    private fun updateEffects(state: GameState, deltaMs: Long): GameState {
        val dt = deltaMs / 16f
        val updatedParticles = state.particles.mapNotNull { p ->
            val newLife = p.life - 0.02f * dt
            if (newLife <= 0f) null
            else p.copy(
                x = p.x + p.vx * dt,
                y = p.y + p.vy * dt,
                vy = p.vy + 0.15f * dt,
                life = newLife
            )
        }
        val updatedFalling = state.fallingPieces.mapNotNull { fp ->
            val newY = fp.y + 6f * dt
            val newAlpha = fp.alpha - 0.015f * dt
            if (newAlpha <= 0f || newY > state.screenHeight + 100f) null
            else fp.copy(y = newY, x = fp.x + fp.velocityX * dt, alpha = newAlpha)
        }
        return state.copy(
            particles = updatedParticles,
            fallingPieces = updatedFalling,
            perfectFlash = max(0f, state.perfectFlash - 0.03f * (deltaMs / 16f)),
            shakeAmount = max(0f, state.shakeAmount * 0.9f)
        )
    }

    fun onTap(state: GameState): GameState {
        return when (state.screen) {
            Screen.MENU -> initGame(state.screenWidth, state.screenHeight)
                .copy(bestScore = state.bestScore)
            Screen.GAME_OVER -> initGame(state.screenWidth, state.screenHeight)
                .copy(bestScore = state.bestScore)
            Screen.PLAYING -> placeBlock(state)
        }
    }

    private fun placeBlock(state: GameState): GameState {
        val current = state.currentBlock ?: return state
        val topBlock = state.stack.lastOrNull() ?: return state

        val overlap = calculateOverlap(current, topBlock)

        if (overlap <= 0f) {
            return state.copy(
                screen = Screen.GAME_OVER,
                currentBlock = null,
                bestScore = max(state.bestScore, state.score),
                shakeAmount = 8f,
                fallingPieces = state.fallingPieces + FallingPiece(
                    x = current.x,
                    y = 0f,
                    width = current.width,
                    color = current.color,
                    velocityX = if (current.x > topBlock.x) 2f else -2f
                )
            )
        }

        val overlapStart = max(current.x, topBlock.x)
        val overlapEnd = min(current.x + current.width, topBlock.x + topBlock.width)
        val overlapWidth = overlapEnd - overlapStart

        val isPerfect = abs(current.x - topBlock.x) < PERFECT_THRESHOLD &&
                abs(current.width - topBlock.width) < PERFECT_THRESHOLD

        val newCombo = if (isPerfect) state.combo + 1 else 0
        val perfectBonus = if (isPerfect) min(newCombo, 5) else 0

        val placedWidth = if (isPerfect) topBlock.width else overlapWidth
        val placedX = if (isPerfect) topBlock.x else overlapStart

        val newBlock = Block(
            x = placedX,
            width = placedWidth,
            color = current.color
        )

        val newStack = state.stack + newBlock
        val newScore = state.score + 1 + perfectBonus

        val cutWidth = current.width - overlapWidth
        val newFalling = if (!isPerfect && cutWidth > 2f) {
            val cutX = if (current.x < topBlock.x) current.x else overlapEnd
            val cutVelocity = if (current.x < topBlock.x) -3f else 3f
            state.fallingPieces + FallingPiece(
                x = cutX,
                y = 0f,
                width = cutWidth,
                color = current.color.copy(alpha = 0.7f),
                velocityX = cutVelocity
            )
        } else state.fallingPieces

        val newParticles = if (isPerfect) {
            state.particles + generatePerfectParticles(placedX, placedWidth, state.screenHeight, state.blockHeight, newStack.size)
        } else state.particles

        val newSpeed = min(MAX_SPEED, BASE_SPEED + (newStack.size - 1) * SPEED_INCREMENT)
        val colorIndex = newStack.size % BLOCK_COLORS.size
        val nextBlock = Block(
            x = if (Random.nextBoolean()) 0f else state.screenWidth - placedWidth,
            width = placedWidth,
            color = BLOCK_COLORS[colorIndex]
        )

        return state.copy(
            stack = newStack,
            currentBlock = nextBlock,
            movingRight = nextBlock.x < state.screenWidth / 2,
            speed = newSpeed,
            score = newScore,
            combo = newCombo,
            particles = newParticles,
            fallingPieces = newFalling,
            perfectFlash = if (isPerfect) 1f else 0f,
            shakeAmount = if (isPerfect) 3f else 1.5f
        )
    }

    private fun calculateOverlap(current: Block, top: Block): Float {
        val overlapStart = max(current.x, top.x)
        val overlapEnd = min(current.x + current.width, top.x + top.width)
        return overlapEnd - overlapStart
    }

    private fun generatePerfectParticles(
        blockX: Float, blockWidth: Float,
        screenHeight: Float, blockHeight: Float,
        stackSize: Int
    ): List<Particle> {
        val centerX = blockX + blockWidth / 2f
        return (0 until 12).map {
            Particle(
                x = centerX + Random.nextFloat() * blockWidth * 0.6f - blockWidth * 0.3f,
                y = 0f,
                vx = (Random.nextFloat() - 0.5f) * 6f,
                vy = -(Random.nextFloat() * 4f + 2f),
                size = Random.nextFloat() * 4f + 2f,
                color = Color(0xFFFFD700),
                life = 1f
            )
        }
    }
}
