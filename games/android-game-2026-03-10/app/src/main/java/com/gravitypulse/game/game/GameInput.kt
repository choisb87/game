package com.gravitypulse.game.game

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Input handling for Gravity Pulse.
 *
 * Core mechanic: single tap to flip gravity direction.
 * The simplicity is intentional — one-touch controls keep
 * the barrier to entry near zero while the physics-based
 * movement creates emergent depth.
 */
object GameInput {

    fun Modifier.gameTapInput(
        gameState: MutableState<GameState>
    ): Modifier = this.pointerInput(Unit) {
        detectTapGestures(
            onTap = {
                val current = gameState.value
                if (current.isAlive) {
                    gameState.value = current.onTap()
                }
            }
        )
    }
}
