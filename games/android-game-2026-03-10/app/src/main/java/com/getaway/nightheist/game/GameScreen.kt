package com.getaway.nightheist.game

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.android.awaitFrame

@Composable
fun GameScreen(
    onBackToMenu: () -> Unit,
    context: Context
) {
    var gameState by remember { mutableStateOf(GameLogic.initLevel(1, loadBestScore(context))) }
    var lastFrameTime by remember { mutableLongStateOf(0L) }

    // Game loop
    LaunchedEffect(gameState.phase) {
        if (gameState.phase == GamePhase.PLAYING) {
            lastFrameTime = awaitFrame()
            while (gameState.phase == GamePhase.PLAYING) {
                val frameTime = awaitFrame()
                val dt = ((frameTime - lastFrameTime) / 1_000_000_000f).coerceAtMost(0.05f)
                lastFrameTime = frameTime
                gameState = GameLogic.update(gameState, dt)
            }
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(gameState.phase) {
                if (gameState.phase == GamePhase.PLAYING) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            gameState = gameState.copy(
                                joystickCenter = offset,
                                joystickDrag = offset,
                                joystickActive = true
                            )
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            gameState = GameLogic.processJoystickInput(
                                gameState,
                                gameState.joystickCenter,
                                change.position
                            )
                        },
                        onDragEnd = {
                            gameState = GameLogic.releaseJoystick(gameState)
                        },
                        onDragCancel = {
                            gameState = GameLogic.releaseJoystick(gameState)
                        }
                    )
                }
            }
            .pointerInput(gameState.phase) {
                detectTapGestures {
                    when (gameState.phase) {
                        GamePhase.CAUGHT -> {
                            // Retry level with remaining lives
                            gameState = GameLogic.initLevel(
                                gameState.level,
                                gameState.bestScore,
                                gameState.totalScore,
                                gameState.lives
                            )
                        }
                        GamePhase.GAME_OVER -> {
                            val best = maxOf(gameState.bestScore, gameState.totalScore)
                            saveBestScore(context, best)
                            onBackToMenu()
                        }
                        GamePhase.LEVEL_COMPLETE -> {
                            val newTotal = gameState.totalScore
                            val best = maxOf(gameState.bestScore, newTotal)
                            saveBestScore(context, best)
                            gameState = GameLogic.initLevel(
                                gameState.level + 1,
                                best,
                                newTotal,
                                gameState.lives
                            )
                        }
                        else -> {}
                    }
                }
            }
    ) {
        gameState = gameState.copy(
            canvasWidth = size.width,
            canvasHeight = size.height
        )
        GameRenderer.render(this, gameState)
        if (gameState.phase != GamePhase.PLAYING) {
            GameRenderer.renderOverlay(this, gameState)
        }
    }
}

private const val PREFS_NAME = "getaway_prefs"
private const val KEY_BEST_SCORE = "best_score"

fun loadBestScore(context: Context): Int {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getInt(KEY_BEST_SCORE, 0)
}

fun saveBestScore(context: Context, score: Int) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putInt(KEY_BEST_SCORE, score)
        .apply()
}
