package com.gravitypulse.game.game

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MenuScreen(
    highScore: Int,
    lastScore: Int,
    modifier: Modifier = Modifier,
    onStartGame: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "menu")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val orbitalAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit"
    )

    val gridScroll by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 80f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "grid"
    )

    Box(
        modifier = modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onStartGame() }
    ) {
        // Animated background
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Scrolling grid
            val gridSpacing = 80f
            val gridAlpha = 0.05f
            var y = -gridScroll
            while (y < size.height) {
                drawLine(
                    color = NeonCyan.copy(alpha = gridAlpha),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += gridSpacing
            }
            var x = 0f
            while (x < size.width) {
                drawLine(
                    color = NeonCyan.copy(alpha = gridAlpha * 0.5f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
                x += gridSpacing
            }

            // Central orb with pulse
            val cx = size.width / 2
            val cy = size.height * 0.38f
            val baseRadius = 48f

            // Outer glow rings
            drawCircle(
                color = NeonCyan.copy(alpha = 0.08f * pulse),
                radius = baseRadius * 3.5f * pulse,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = NeonPink.copy(alpha = 0.06f * pulse),
                radius = baseRadius * 2.5f * pulse,
                center = Offset(cx, cy)
            )

            // Orbital particles
            val rad = Math.toRadians(orbitalAngle.toDouble())
            val orbitRadius = baseRadius * 2.2f
            for (i in 0..2) {
                val angle = rad + i * (2 * Math.PI / 3)
                val ox = cx + cos(angle).toFloat() * orbitRadius
                val oy = cy + sin(angle).toFloat() * orbitRadius
                val color = when (i) {
                    0 -> NeonCyan
                    1 -> NeonPink
                    else -> NeonGold
                }
                drawCircle(
                    color = color.copy(alpha = 0.6f),
                    radius = 5f,
                    center = Offset(ox, oy)
                )
            }

            // Core orb
            drawCircle(
                color = NeonCyan.copy(alpha = 0.15f),
                radius = baseRadius * 1.5f,
                center = Offset(cx, cy),
                style = Stroke(width = 2f)
            )
            drawCircle(
                color = NeonCyan.copy(alpha = 0.7f),
                radius = baseRadius,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = baseRadius * 0.4f,
                center = Offset(cx, cy)
            )

            // Gravity arrows
            val arrowY1 = cy - baseRadius - 30f
            val arrowY2 = cy + baseRadius + 30f
            drawLine(
                color = NeonCyan.copy(alpha = 0.5f * pulse),
                start = Offset(cx, arrowY1),
                end = Offset(cx, arrowY1 - 20f),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = NeonPink.copy(alpha = 0.5f * pulse),
                start = Offset(cx, arrowY2),
                end = Offset(cx, arrowY2 + 20f),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }

        // Text content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(200.dp))

            Spacer(modifier = Modifier.weight(1f))

            // Title
            Text(
                text = "GRAVITY",
                color = NeonCyan.copy(alpha = 0.95f),
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 12.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "PULSE",
                color = NeonPink.copy(alpha = 0.9f),
                fontSize = 42.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 18.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Scores
            if (highScore > 0) {
                if (lastScore > 0) {
                    Text(
                        text = "SCORE  $lastScore",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = "BEST  $highScore",
                    color = NeonGold.copy(alpha = 0.8f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Tap prompt
            Text(
                text = "TAP ANYWHERE",
                color = Color.White.copy(alpha = 0.4f + (pulse - 0.8f)),
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}
