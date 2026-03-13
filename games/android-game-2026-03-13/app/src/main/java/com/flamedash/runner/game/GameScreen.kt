package com.flamedash.runner.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.rememberTextMeasurer

@Composable
fun GameScreen(onBackToMenu: () -> Unit) {
    var gameState by remember { mutableStateOf(GameState()) }
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current

    // Load best score from SharedPreferences
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("flame_dash", android.content.Context.MODE_PRIVATE)
        val savedBest = prefs.getInt("best_score", 0)
        gameState = gameState.copy(bestScore = savedBest)
    }

    // Save best score when it changes
    LaunchedEffect(gameState.bestScore) {
        if (gameState.bestScore > 0) {
            val prefs = context.getSharedPreferences("flame_dash", android.content.Context.MODE_PRIVATE)
            prefs.edit().putInt("best_score", gameState.bestScore).apply()
        }
    }

    // Back button returns to menu
    BackHandler {
        onBackToMenu()
    }

    // Game loop
    LaunchedEffect(Unit) {
        var lastFrameTime = 0L
        while (true) {
            withFrameNanos { frameTime ->
                if (lastFrameTime == 0L) {
                    lastFrameTime = frameTime
                    return@withFrameNanos
                }
                val dt = ((frameTime - lastFrameTime) / 1_000_000_000f).coerceAtMost(0.033f)
                lastFrameTime = frameTime
                gameState = GameLogic.update(gameState, dt)
            }
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                if (gameState.screenWidth == 0f) {
                    gameState = GameLogic.initGame(
                        gameState.copy(
                            screenWidth = size.width.toFloat(),
                            screenHeight = size.height.toFloat()
                        )
                    )
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    when (gameState.phase) {
                        GamePhase.READY -> {
                            gameState = GameLogic.startGame(gameState)
                        }
                        GamePhase.PLAYING -> {
                            gameState = GameLogic.onTap(gameState, offset.x)
                        }
                        GamePhase.DEAD -> {
                            gameState = GameLogic.initGame(gameState.copy(
                                bestScore = gameState.bestScore
                            ))
                        }
                        else -> {}
                    }
                }
            }
    ) {
        GameRenderer.render(this, gameState, textMeasurer)
    }
}
