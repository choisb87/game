package com.voidrunner.gravity.game

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.rememberTextMeasurer

@Composable
fun GameScreen(onBackToMenu: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("void_runner", Context.MODE_PRIVATE) }
    val textMeasurer = rememberTextMeasurer()

    var gameState by remember {
        val bestScore = prefs.getInt("best_score", 0)
        val bestDist = prefs.getFloat("best_distance", 0f)
        mutableStateOf(
            GameLogic.initGame(
                GameState(bestScore = bestScore, bestDistance = bestDist)
            )
        )
    }

    // Game loop
    LaunchedEffect(Unit) {
        var lastTime = withFrameNanos { it }
        while (true) {
            val time = withFrameNanos { it }
            val dt = ((time - lastTime) / 1_000_000_000f).coerceAtMost(0.1f)
            lastTime = time

            val newState = GameLogic.update(gameState, dt)

            // Persist best score on death
            if (newState.phase == GamePhase.SCORE && gameState.phase != GamePhase.SCORE) {
                if (newState.score > newState.bestScore) {
                    prefs.edit()
                        .putInt("best_score", newState.score)
                        .putFloat("best_distance", newState.distance)
                        .apply()
                    gameState = newState.copy(bestScore = newState.score, bestDistance = newState.distance)
                } else {
                    gameState = newState
                }
            } else {
                gameState = newState
            }
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    val s = gameState
                    when (s.phase) {
                        GamePhase.READY -> {
                            gameState = GameLogic.startGame(s.copy(
                                screenWidth = size.width.toFloat(),
                                screenHeight = size.height.toFloat()
                            ))
                        }
                        GamePhase.PLAYING -> {
                            gameState = GameLogic.flipGravity(s)
                        }
                        GamePhase.SCORE -> {
                            // Retry
                            val best = gameState.bestScore
                            val bestD = gameState.bestDistance
                            gameState = GameLogic.initGame(
                                GameState(
                                    screenWidth = s.screenWidth,
                                    screenHeight = s.screenHeight,
                                    bestScore = best,
                                    bestDistance = bestD
                                )
                            )
                        }
                        GamePhase.DEAD -> { /* wait for timer */ }
                    }
                }
            }
    ) {
        val s = gameState.let {
            if (it.screenWidth == 0f) it.copy(
                screenWidth = size.width,
                screenHeight = size.height
            ) else it
        }
        if (s.screenWidth != gameState.screenWidth) {
            gameState = s
        }
        GameRenderer.render(this, s, textMeasurer)
    }
}
