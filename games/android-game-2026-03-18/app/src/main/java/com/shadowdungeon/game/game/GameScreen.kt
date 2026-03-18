package com.shadowdungeon.game.game

import android.content.SharedPreferences
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

@Composable
fun GameScreen(
    @Suppress("UNUSED_PARAMETER") prefs: SharedPreferences,
    onGameOver: (score: Int, floor: Int) -> Unit,
) {
    var gameState by remember { mutableStateOf(GameLogic.createInitialState()) }
    var time by remember { mutableFloatStateOf(0f) }
    var pendingSwipe by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var gameOverNotified by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        var lastFrame = System.nanoTime()
        while (true) {
            withFrameNanos { frameNanos ->
                val dt = ((frameNanos - lastFrame) / 1_000_000_000f).coerceIn(0f, 0.05f)
                lastFrame = frameNanos
                time += dt

                gameState = GameLogic.updateParticles(gameState, dt)

                pendingSwipe?.let { (dx, dy) ->
                    gameState = GameLogic.processSwipe(gameState, dx, dy)
                    pendingSwipe = null
                }

                if (gameState.gameOver && !gameOverNotified) {
                    gameOverNotified = true
                    onGameOver(gameState.score, gameState.floor)
                }
            }
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var dragDx = 0f
                var dragDy = 0f
                var swiped = false
                detectDragGestures(
                    onDragStart = {
                        dragDx = 0f
                        dragDy = 0f
                        swiped = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragDx += dragAmount.x
                        dragDy += dragAmount.y
                        val threshold = 40f
                        if (!swiped && (abs(dragDx) > threshold || abs(dragDy) > threshold)) {
                            swiped = true
                            val dx: Int
                            val dy: Int
                            if (abs(dragDx) > abs(dragDy)) {
                                dx = if (dragDx > 0) 1 else -1
                                dy = 0
                            } else {
                                dx = 0
                                dy = if (dragDy > 0) 1 else -1
                            }
                            pendingSwipe = Pair(dx, dy)
                        }
                    },
                    onDragEnd = {},
                    onDragCancel = {}
                )
            }
    ) {
        with(GameRenderer) {
            renderGame(gameState, time)
            renderHUD(gameState, size.width)
        }
    }
}
