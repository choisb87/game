package com.gravitypulse.game.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.withFrameNanos

@Composable
fun GameLoop(
    gameState: MutableState<GameState>,
    onGameOver: (Int) -> Unit
) {
    LaunchedEffect(Unit) {
        var lastTime = 0L

        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (lastTime == 0L) {
                    lastTime = frameTimeNanos
                    return@withFrameNanos
                }

                val deltaTime = ((frameTimeNanos - lastTime) / 1_000_000_000f)
                lastTime = frameTimeNanos

                val current = gameState.value
                if (current.isAlive) {
                    gameState.value = current.update(deltaTime)
                } else {
                    // Brief death delay handled by screen
                    gameState.value = current.update(deltaTime)
                }
            }
        }
    }

    // Detect death and trigger callback after a short delay
    LaunchedEffect(gameState.value.isAlive) {
        if (!gameState.value.isAlive) {
            kotlinx.coroutines.delay(1200)
            onGameOver(gameState.value.score)
        }
    }
}
