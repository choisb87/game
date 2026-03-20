package com.neonbreaker.game.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameScreen() {
    var state by remember { mutableStateOf(GameState()) }
    val textMeasurer = rememberTextMeasurer()

    // Game loop
    LaunchedEffect(state.phase) {
        if (state.phase != GamePhase.PLAYING) return@LaunchedEffect
        var lastFrame = withFrameMillis { it }
        while (state.phase == GamePhase.PLAYING) {
            val now = withFrameMillis { it }
            val dt = ((now - lastFrame) / 1000f).coerceAtMost(0.05f)
            lastFrame = now
            state = GameEngine.checkPowerUpExpiry(state, now)
            state = GameEngine.update(state, dt, now)
        }
    }

    // Level transition
    LaunchedEffect(state.phase) {
        if (state.phase == GamePhase.LEVEL_CLEAR) {
            kotlinx.coroutines.delay(1200L)
            val newLevel = state.level + 1
            state = GameEngine.initLevel(
                state.copy(level = newLevel),
                state.canvasW, state.canvasH,
            )
        }
    }

    when (state.phase) {
        GamePhase.MENU -> MenuScreen(
            highScore = state.highScore,
            onStart = {
                if (state.canvasW > 0f) {
                    state = GameEngine.initLevel(
                        GameState(highScore = state.highScore),
                        state.canvasW, state.canvasH,
                    )
                }
            },
        )
        else -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0A0A1A))
                    .onSizeChanged { size ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        if (state.canvasW == 0f && w > 0f) {
                            state = state.copy(canvasW = w, canvasH = h)
                            if (state.phase == GamePhase.MENU || state.bricks.isEmpty()) {
                                state = GameEngine.initLevel(state, w, h)
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset -> state = state.copy(touchX = offset.x) },
                            onDrag = { change, _ ->
                                change.consume()
                                state = state.copy(touchX = change.position.x)
                            },
                            onDragEnd = { state = state.copy(touchX = -1f) },
                        )
                    }
                    .pointerInput(state.phase) {
                        detectTapGestures {
                            when (state.phase) {
                                GamePhase.READY -> {
                                    state = state.copy(touchX = it.x)
                                    state = GameEngine.launchBall(state)
                                }
                                GamePhase.GAME_OVER -> {
                                    state = GameEngine.initLevel(
                                        GameState(highScore = state.highScore),
                                        state.canvasW, state.canvasH,
                                    )
                                }
                                else -> {
                                    state = state.copy(touchX = it.x)
                                }
                            }
                        }
                    },
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    GameRenderer.draw(this, state, textMeasurer)
                }
            }
        }
    }
}

@Composable
private fun MenuScreen(highScore: Int, onStart: () -> Unit) {
    var hasSize by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A1A))
            .pointerInput(Unit) { detectTapGestures { onStart() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                "NEON",
                style = TextStyle(
                    color = Color(0xFF00FFFF),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 12.sp,
                ),
            )
            Text(
                "BREAKER",
                style = TextStyle(
                    color = Color(0xFFFF00FF),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 8.sp,
                ),
            )
            Spacer(Modifier.height(32.dp))

            // Game description
            Text(
                "네온 빛 벽돌을 부수고\n콤보를 쌓아 최고 점수를 달성하세요",
                style = TextStyle(
                    color = Color(0xFF8888AA),
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))

            // Controls
            Text(
                "◆ 드래그: 패들 이동\n◆ 탭: 공 발사\n◆ 파워업을 모아 강해지세요",
                style = TextStyle(
                    color = Color(0xFF6666AA),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(40.dp))

            if (highScore > 0) {
                Text(
                    "최고 기록: $highScore",
                    style = TextStyle(
                        color = Color(0xFFFFFF00),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    ),
                )
                Spacer(Modifier.height(16.dp))
            }

            Text(
                "[ 탭하여 시작 ]",
                style = TextStyle(
                    color = Color(0xFF00FFFF),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                ),
            )
        }
    }
}
