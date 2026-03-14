package com.orbitshield.game.game

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import kotlin.random.Random

@Composable
fun GameScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("orbit_shield", Context.MODE_PRIVATE) }
    var state by remember { mutableStateOf(GameState()) }
    var lastFrameTime by remember { mutableStateOf(0L) }

    BackHandler { onBack() }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    state = onTap(state)
                }
            }
    ) {
        val currentTime = System.nanoTime()
        if (lastFrameTime == 0L) {
            lastFrameTime = currentTime
            val best = prefs.getInt("best_score", 0)
            state = initGame(size.width, size.height, best)
        }

        val dt = ((currentTime - lastFrameTime) / 1_000_000_000f).coerceAtMost(0.033f)
        lastFrameTime = currentTime

        val prevPhase = state.phase
        state = update(state, dt)

        // Save best score
        if (state.phase == GamePhase.GAME_OVER && prevPhase != GamePhase.GAME_OVER) {
            prefs.edit().putInt("best_score", state.bestScore).apply()
        }

        // Apply screen shake
        val shakeX = if (state.screenShake > 0f) (Random.nextFloat() - 0.5f) * state.screenShake * 2 else 0f
        val shakeY = if (state.screenShake > 0f) (Random.nextFloat() - 0.5f) * state.screenShake * 2 else 0f

        translate(shakeX, shakeY) {
            drawGame(state)
        }

    }

    // Continuous recomposition for game loop
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { }
        }
    }
}
