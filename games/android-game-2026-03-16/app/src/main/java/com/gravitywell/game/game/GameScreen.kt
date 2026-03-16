package com.gravitywell.game.game

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

@Composable
fun GameScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gravity_well", Context.MODE_PRIVATE) }
    var gameState by remember {
        val best = prefs.getInt("best_score", 0)
        mutableStateOf(
            GameLogic.initLevel(
                GameState(bestScore = best),
                1
            )
        )
    }
    val textMeasurer = rememberTextMeasurer()

    // Game loop
    LaunchedEffect(gameState.phase) {
        if (gameState.phase != GamePhase.PLAYING) return@LaunchedEffect

        var lastTime = withFrameNanos { it }
        while (true) {
            val frameTime = withFrameNanos { it }
            val dt = ((frameTime - lastTime) / 1_000_000_000f).coerceIn(0f, 0.05f)
            lastTime = frameTime
            gameState = GameLogic.update(gameState, dt)

            if (gameState.phase != GamePhase.PLAYING) {
                if (gameState.score > gameState.bestScore) {
                    val newBest = max(gameState.bestScore, gameState.score)
                    gameState = gameState.copy(bestScore = newBest)
                    prefs.edit().putInt("best_score", newBest).apply()
                }
                break
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030812))
            .onSizeChanged { size ->
                if (gameState.screenWidth != size.width.toFloat() ||
                    gameState.screenHeight != size.height.toFloat()
                ) {
                    gameState = GameLogic.initLevel(
                        gameState.copy(
                            screenWidth = size.width.toFloat(),
                            screenHeight = size.height.toFloat()
                        ),
                        gameState.level
                    )
                }
            }
    ) {
        // Game canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (gameState.phase == GamePhase.PLAYING) {
                            gameState = GameLogic.placeWell(gameState, offset)
                        }
                    }
                }
        ) {
            with(GameRenderer) {
                render(gameState, textMeasurer)
            }
        }

        // Mode toggle button (bottom left)
        if (gameState.phase == GamePhase.PLAYING) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (gameState.wellMode == WellMode.ATTRACT)
                            Color(0xFF00E5FF).copy(alpha = 0.3f)
                        else
                            Color(0xFFFF4081).copy(alpha = 0.3f)
                    )
                    .clickable {
                        gameState = GameLogic.toggleWellMode(gameState)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (gameState.wellMode == WellMode.ATTRACT) "⊕" else "⊖",
                    color = if (gameState.wellMode == WellMode.ATTRACT)
                        Color(0xFF00E5FF) else Color(0xFFFF4081),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Level complete overlay
        if (gameState.phase == GamePhase.LEVEL_COMPLETE) {
            LevelCompleteOverlay(
                state = gameState,
                onNext = {
                    gameState = GameLogic.initLevel(gameState, gameState.level + 1)
                },
                onMenu = onBack
            )
        }

        // Game over overlay
        if (gameState.phase == GamePhase.GAME_OVER) {
            GameOverOverlay(
                state = gameState,
                onRetry = {
                    gameState = GameLogic.initLevel(
                        gameState.copy(score = 0, combo = 0, maxCombo = 0),
                        1
                    )
                },
                onMenu = onBack
            )
        }
    }
}

@Composable
private fun LevelCompleteOverlay(
    state: GameState,
    onNext: () -> Unit,
    onMenu: () -> Unit
) {
    val catchRatio = state.starsCaught.toFloat() / state.totalStarsInLevel
    val isPerfect = catchRatio >= 1f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isPerfect) {
                Text(
                    "⭐ 퍼펙트! ⭐",
                    color = Color(0xFFFFD740),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Text(
                "레벨 ${state.level} 클리어!",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "포획: ${state.starsCaught}/${state.totalStarsInLevel}",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 18.sp
            )
            Text(
                "최대 콤보: ${state.maxCombo}x",
                color = Color(0xFFFFD740).copy(alpha = 0.8f),
                fontSize = 18.sp
            )
            Text(
                "점수: ${state.score}",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OverlayButton("다음 레벨", Color(0xFF00E676), onClick = onNext)
                OverlayButton("메뉴", Color.White.copy(alpha = 0.5f), onClick = onMenu)
            }
        }
    }
}

@Composable
private fun GameOverOverlay(
    state: GameState,
    onRetry: () -> Unit,
    onMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "게임 종료",
                color = Color(0xFFFF5252),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "레벨 ${state.level}까지 도달",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 18.sp
            )
            Text(
                "최종 점수: ${state.score}",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "최고 점수: ${state.bestScore}",
                color = Color(0xFFFFD740),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )

            if (state.score >= state.bestScore && state.score > 0) {
                Text(
                    "🏆 신기록!",
                    color = Color(0xFFFFD740),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OverlayButton("다시 하기", Color(0xFF448AFF), onClick = onRetry)
                OverlayButton("메뉴", Color.White.copy(alpha = 0.5f), onClick = onMenu)
            }
        }
    }
}

@Composable
private fun OverlayButton(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.2f))
            .clickable(onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
