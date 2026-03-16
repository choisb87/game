package com.gravitywell.game.game

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MenuScreen(onPlay: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gravity_well", Context.MODE_PRIVATE) }
    val bestScore = remember { prefs.getInt("best_score", 0) }

    var time by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            time += (now - last) / 1_000_000_000f
            last = now
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030812))
    ) {
        // Animated background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Space gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF030812),
                        Color(0xFF0A1628),
                        Color(0xFF030812)
                    )
                )
            )

            // Decorative stars
            for (i in 0 until 40) {
                val x = ((42 * (i + 1) * 7919) % w.toInt()).toFloat()
                val y = ((42 * (i + 1) * 6271) % h.toInt()).toFloat()
                val twinkle = abs(sin(time * (0.5f + i % 4) + i.toFloat()))
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f + 0.2f * twinkle),
                    radius = 1f + (i % 3) * 0.5f,
                    center = Offset(x, y)
                )
            }

            // Animated gravity well in center
            val cx = w / 2
            val cy = h * 0.35f

            // Rotating influence rings
            for (ring in 4 downTo 1) {
                val radius = 40f + ring * 30f + sin(time * 0.8f + ring) * 10f
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.08f * ring),
                    radius = radius,
                    center = Offset(cx, cy)
                )
            }

            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.25f),
                radius = 160f,
                center = Offset(cx, cy),
                style = Stroke(width = 1f)
            )

            // Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = 0.8f),
                        Color(0xFF00E5FF).copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = 40f
                ),
                radius = 40f,
                center = Offset(cx, cy)
            )

            // Orbiting stars
            for (i in 0 until 5) {
                val angle = time * (0.6f + i * 0.15f) + i * (2 * PI.toFloat() / 5)
                val orbitR = 80f + i * 20f
                val sx = cx + cos(angle) * orbitR
                val sy = cy + sin(angle) * orbitR * 0.6f
                val starColor = when (i % 4) {
                    0 -> Color(0xFFFFD740)
                    1 -> Color(0xFFB0BEC5)
                    2 -> Color(0xFFFF5252)
                    else -> Color(0xFFE0F7FA)
                }
                drawCircle(
                    color = starColor.copy(alpha = 0.8f),
                    radius = 5f - i * 0.5f,
                    center = Offset(sx, sy)
                )
                // Trail
                for (t in 1..4) {
                    val trailAngle = angle - t * 0.1f
                    val tx = cx + cos(trailAngle) * orbitR
                    val ty = cy + sin(trailAngle) * orbitR * 0.6f
                    drawCircle(
                        color = starColor.copy(alpha = 0.15f * (1f - t / 5f)),
                        radius = 3f - t * 0.5f,
                        center = Offset(tx, ty)
                    )
                }
            }
        }

        // UI content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Title
            Text(
                "GRAVITY\nWELL",
                color = Color(0xFF00E5FF),
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 52.sp
            )

            Text(
                "중력의 우물",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Light
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Play button
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                    .clickable(onClick = onPlay)
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "플레이",
                    color = Color(0xFF00E5FF),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (bestScore > 0) {
                Text(
                    "최고 점수: $bestScore",
                    color = Color(0xFFFFD740).copy(alpha = 0.7f),
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // How to play
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "게임 방법",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "• 화면을 탭하여 중력 우물을 배치하세요\n" +
                    "• 떨어지는 별을 같은 색 영역으로 유도하세요\n" +
                    "• ⊕ 인력 / ⊖ 척력 모드를 전환하세요\n" +
                    "• 연속 포획으로 콤보 보너스를 얻으세요\n" +
                    "• 40% 이상 포획해야 다음 레벨로!",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
