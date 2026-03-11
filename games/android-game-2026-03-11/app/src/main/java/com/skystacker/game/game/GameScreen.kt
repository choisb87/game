package com.skystacker.game.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.rememberTextMeasurer
import com.skystacker.game.game.GameRenderer.renderGame
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun GameScreen(
    loadBestScore: () -> Int = { 0 },
    saveBestScore: (Int) -> Unit = {}
) {
    var gameState by remember {
        mutableStateOf(GameState(bestScore = loadBestScore()))
    }
    val textMeasurer = rememberTextMeasurer()
    var lastFrameTime by remember { mutableLongStateOf(0L) }

    // Save best score when it changes
    LaunchedEffect(gameState.bestScore) {
        if (gameState.bestScore > 0) {
            saveBestScore(gameState.bestScore)
        }
    }

    // Back button: return to menu from playing/game-over
    BackHandler(enabled = gameState.screen != Screen.MENU) {
        gameState = gameState.copy(
            screen = Screen.MENU,
            currentBlock = null,
            bestScore = maxOf(gameState.bestScore, gameState.score)
        )
    }

    LaunchedEffect(gameState.screen) {
        if (gameState.screen == Screen.PLAYING) {
            lastFrameTime = System.currentTimeMillis()
            while (isActive) {
                val now = System.currentTimeMillis()
                val delta = (now - lastFrameTime).coerceIn(1, 32)
                lastFrameTime = now
                gameState = GameLogic.update(gameState, delta)
                delay(16L)
            }
        }
    }

    // Keep effects running on game over
    LaunchedEffect(gameState.screen) {
        if (gameState.screen == Screen.GAME_OVER) {
            lastFrameTime = System.currentTimeMillis()
            while (isActive && (gameState.particles.isNotEmpty() || gameState.fallingPieces.isNotEmpty())) {
                val now = System.currentTimeMillis()
                val delta = (now - lastFrameTime).coerceIn(1, 32)
                lastFrameTime = now
                gameState = GameLogic.update(gameState, delta)
                delay(16L)
            }
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                if (gameState.screenWidth == 0f) {
                    gameState = gameState.copy(
                        screenWidth = size.width.toFloat(),
                        screenHeight = size.height.toFloat(),
                        blockHeight = size.height.toFloat() / 18f
                    )
                }
            }
            .pointerInput(Unit) {
                detectTapGestures {
                    gameState = GameLogic.onTap(gameState)
                }
            }
    ) {
        renderGame(gameState, textMeasurer)

        when (gameState.screen) {
            Screen.MENU -> renderMenuOverlay(gameState, textMeasurer)
            Screen.GAME_OVER -> renderGameOverOverlay(gameState, textMeasurer)
            else -> {}
        }
    }
}
