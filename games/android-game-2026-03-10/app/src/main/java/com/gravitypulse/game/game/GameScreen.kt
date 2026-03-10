package com.gravitypulse.game.game

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    onGameOver: (Int) -> Unit
) {
    val gameState = remember {
        mutableStateOf(GameState())
    }

    GameLoop(
        gameState = gameState,
        onGameOver = onGameOver
    )

    Box(
        modifier = modifier
            .onSizeChanged { size ->
                val current = gameState.value
                if (current.canvasWidth != size.width.toFloat() || current.canvasHeight != size.height.toFloat()) {
                    gameState.value = current.copy(
                        canvasWidth = size.width.toFloat(),
                        canvasHeight = size.height.toFloat(),
                        player = current.player.copy(
                            x = size.width * 0.35f,
                            y = size.height * 0.7f
                        )
                    )
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (gameState.value.isAlive) {
                            gameState.value = gameState.value.onTap()
                        }
                    }
                )
            }
    ) {
        // Game canvas
        GameRenderer(
            state = gameState.value,
            modifier = Modifier.fillMaxSize()
        )

        // Score overlay
        Text(
            text = "${gameState.value.score}",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 32.dp)
        )

        // Combo display
        if (gameState.value.combo > 1) {
            Text(
                text = "${gameState.value.combo}x COMBO",
                color = NeonGold.copy(alpha = 0.9f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 88.dp)
            )
        }

        // Death overlay
        if (!gameState.value.isAlive) {
            Text(
                text = "SHATTERED",
                color = NeonPink.copy(alpha = 0.8f),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
