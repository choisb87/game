package com.velvetlift.game.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.android.awaitFrame
import kotlin.math.sin

@Composable
fun ResultScreen(
    contract: ContractConfig,
    result: ContractResult,
    profile: CareerProfile,
    onReplay: () -> Unit,
    onContinue: () -> Unit,
    onMenu: () -> Unit
) {
    var phase by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            awaitFrame()
            phase += 0.018f
        }
    }

    val hasNextContract = result.success && contract.id < HOTEL_CONTRACTS.size && contract.id + 1 <= profile.unlockedContractId

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF081116),
                        Color(0xFF101C22),
                        Color(0xFF15282E)
                    )
                )
            )
            .safeDrawingPadding()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val accent = if (result.success) Color(0x44D8B16B) else Color(0x44EF8B6B)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent, Color.Transparent)
                ),
                radius = size.minDimension * 0.42f,
                center = Offset(size.width * 0.5f, size.height * 0.22f)
            )
            val arcColor = if (result.success) Color(0x55D8B16B) else Color(0x55EF8B6B)
            drawCircle(
                color = arcColor,
                radius = size.minDimension * (0.26f + 0.02f * sin(phase)),
                center = Offset(size.width * 0.5f, size.height * 0.18f),
                style = Stroke(width = 5f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(34.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                tonalElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = contract.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (result.success) "HOUSE DELIVERED" else "SERVICE COLLAPSED",
                        style = MaterialTheme.typography.displaySmall,
                        color = if (result.success) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "${result.stars}/3 stars",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (result.success) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ResultMetric(modifier = Modifier.weight(1f), label = "Score", value = result.score.toString())
                        ResultMetric(modifier = Modifier.weight(1f), label = "Delivered", value = result.delivered.toString())
                        ResultMetric(modifier = Modifier.weight(1f), label = "Mood", value = "${(result.moodRemaining * 100).toInt()}%")
                    }

                    Text(
                        text = if (result.success) {
                            "The lobby stayed in motion and the contract line cleared before dawn."
                        } else {
                            "The queue fell behind. Reset the route order and protect critic and VIP patience first."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.76f)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ResultButton(
                            label = "Replay Contract",
                            accent = MaterialTheme.colorScheme.secondary,
                            onClick = onReplay
                        )
                        if (hasNextContract) {
                            ResultButton(
                                label = "Continue to Next Contract",
                                accent = MaterialTheme.colorScheme.primary,
                                onClick = onContinue
                            )
                        }
                        ResultButton(
                            label = "Return to Menu",
                            accent = MaterialTheme.colorScheme.tertiary,
                            onClick = onMenu
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultMetric(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun ResultButton(
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(accent.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = accent
        )
    }
}
