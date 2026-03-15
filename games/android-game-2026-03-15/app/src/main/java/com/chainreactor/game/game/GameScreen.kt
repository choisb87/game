package com.chainreactor.game.game

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import kotlin.random.Random

@Composable
fun GameScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("chain_reactor", Context.MODE_PRIVATE) }
    var bestScore by remember { mutableIntStateOf(prefs.getInt("best_score", 0)) }
    var state by remember { mutableStateOf(GameState()) }
    var lastFrameTime by remember { mutableLongStateOf(0L) }
    var initialized by remember { mutableStateOf(false) }

    BackHandler { onExit() }

    // Game loop
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (!initialized) return@withFrameNanos

                if (lastFrameTime == 0L) {
                    lastFrameTime = frameTimeNanos
                    return@withFrameNanos
                }

                val dt = ((frameTimeNanos - lastFrameTime) / 1_000_000_000f).coerceAtMost(0.033f)
                lastFrameTime = frameTimeNanos

                state = update(state, dt)

                // Save best score
                if (state.score > bestScore) {
                    bestScore = state.score
                    prefs.edit().putInt("best_score", bestScore).apply()
                }
            }
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (!initialized) return@detectTapGestures

                    when (state.phase) {
                        GamePhase.PLAYING, GamePhase.CHAIN_ACTIVE -> {
                            state = handleTap(state, offset.x, offset.y)
                        }
                        GamePhase.GAME_OVER -> {
                            lastFrameTime = 0L
                            state = initGame(state.screenWidth, state.screenHeight, bestScore)
                        }
                        else -> {}
                    }
                }
            }
    ) {
        if (!initialized) {
            state = initGame(size.width, size.height, bestScore)
            initialized = true
            return@Canvas
        }

        // Screen shake
        val shakeX = if (state.shakeTimer > 0f) (Random.nextFloat() - 0.5f) * state.shakeIntensity * 2f else 0f
        val shakeY = if (state.shakeTimer > 0f) (Random.nextFloat() - 0.5f) * state.shakeIntensity * 2f else 0f

        translate(shakeX, shakeY) {
            drawGame(state)
        }
    }
}
