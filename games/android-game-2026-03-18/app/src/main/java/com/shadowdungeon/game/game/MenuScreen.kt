package com.shadowdungeon.game.game

import android.content.SharedPreferences
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

@Composable
fun MenuScreen(
    prefs: SharedPreferences,
    showGameOver: Boolean = false,
    lastScore: Int = 0,
    lastFloor: Int = 0,
    onStartGame: () -> Unit,
) {
    val bestScore = prefs.getInt("best_score", 0)
    val bestFloor = prefs.getInt("best_floor", 0)
    val totalRuns = prefs.getInt("total_runs", 0)
    var time by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                time += 0.016f
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E17))
    ) {
        // Animated background particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (i in 0 until 30) {
                val px = ((i * 137 + time * (10 + i * 3)) % size.width)
                val py = ((i * 89 + time * (5 + i * 2)) % size.height)
                val alpha = 0.1f + sin(time + i.toFloat()) * 0.08f
                val radius = 1.5f + sin(time * 0.7f + i * 0.5f) * 0.8f
                drawCircle(
                    Color(0xFF4FC3F7).copy(alpha = alpha),
                    radius = radius,
                    center = Offset(px, py)
                )
            }

            drawRect(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0xFF0A0E17)),
                    startY = size.height * 0.7f,
                    endY = size.height
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.weight(1f))

            // Title
            Text(
                text = "Shadow\nDungeon",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF4FC3F7),
                textAlign = TextAlign.Center,
                lineHeight = 52.sp,
            )

            Text(
                text = "그림자 던전",
                fontSize = 18.sp,
                color = Color(0xFF4FC3F7).copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(Modifier.height(20.dp))

            if (showGameOver) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A1F2E))
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "탐험 종료",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF5350),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("도달 층: B${lastFloor}F", fontSize = 16.sp, color = Color(0xFF7E57C2))
                        Text("점수: $lastScore", fontSize = 16.sp, color = Color(0xFFFFD54F))
                        if (lastScore >= bestScore && totalRuns > 0) {
                            Text("🏆 최고 기록!", fontSize = 14.sp, color = Color(0xFFFFAB40),
                                modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Stats
            if (totalRuns > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF151A26))
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            StatItem("최고 점수", "$bestScore", Color(0xFFFFD54F))
                            StatItem("최고 층", "B${bestFloor}F", Color(0xFF7E57C2))
                            StatItem("탐험 횟수", "$totalRuns", Color(0xFF4FC3F7))
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            Spacer(Modifier.weight(0.5f))

            // Play button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF0288D1), Color(0xFF4FC3F7))
                        )
                    )
                    .clickable { onStartGame() }
                    .padding(horizontal = 48.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (showGameOver) "다시 도전" else "던전 진입",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }

            Spacer(Modifier.height(24.dp))

            // How to play
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF151A26).copy(alpha = 0.7f))
                    .padding(16.dp)
            ) {
                Text("조작법", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4FC3F7))
                Spacer(Modifier.height(8.dp))
                Text("↑↓←→ 스와이프로 이동", fontSize = 12.sp, color = Color(0xFFB0BEC5))
                Text("적에게 걸어가면 공격", fontSize = 12.sp, color = Color(0xFFB0BEC5))
                Text("계단을 찾아 더 깊이 내려가세요", fontSize = 12.sp, color = Color(0xFFB0BEC5))
                Text("아이템을 밟으면 자동 획득", fontSize = 12.sp, color = Color(0xFFB0BEC5))
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "Premium • No Ads • $2.99",
                fontSize = 11.sp,
                color = Color(0xFF4FC3F7).copy(alpha = 0.3f),
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = Color(0xFFB0BEC5))
    }
}
