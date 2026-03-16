package com.velvetlift.game.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
fun MenuScreen(
    profile: CareerProfile,
    onStart: (ContractConfig) -> Unit
) {
    val scrollState = rememberScrollState()
    var phase by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            awaitFrame()
            phase += 0.012f
        }
    }

    val perks = remember(profile) { perkCatalog(profile) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF071015),
                        Color(0xFF0E1B21),
                        Color(0xFF14262B)
                    )
                )
            )
            .safeDrawingPadding()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val glow = 0.18f + 0.08f * sin(phase)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x44D8B16B), Color.Transparent)
                ),
                radius = size.minDimension * 0.38f,
                center = Offset(size.width * 0.2f, size.height * 0.18f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x3363C6C1), Color.Transparent)
                ),
                radius = size.minDimension * 0.42f,
                center = Offset(size.width * 0.84f, size.height * 0.36f)
            )
            val lineColor = Color(0x33F7F1E3).copy(alpha = glow)
            val spacing = size.width / 9f
            for (index in 0..8) {
                val x = spacing * index
                drawLine(
                    color = lineColor,
                    start = Offset(x, size.height * 0.12f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "VELVET LIFT",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Premium concierge strategy for one hand and a busy tower.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
            )

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                tonalElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "House Summary",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCell(
                            modifier = Modifier.weight(1f),
                            label = "Unlocked",
                            value = "${profile.unlockedContractId}/${HOTEL_CONTRACTS.size}"
                        )
                        SummaryCell(
                            modifier = Modifier.weight(1f),
                            label = "High Score",
                            value = profile.highScore.toString()
                        )
                        SummaryCell(
                            modifier = Modifier.weight(1f),
                            label = "Guests",
                            value = profile.lifetimeGuests.toString()
                        )
                    }
                    Text(
                        text = "Tap a contract to begin. Queue up to four stops, preserve mood, and hit the nightly revenue line.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
                    )
                }
            }

            if (perks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Permanent Amenities",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        perks.forEach { perk ->
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                tonalElevation = 4.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = perk.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = perk.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = "Service Season",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            HOTEL_CONTRACTS.forEach { contract ->
                val unlocked = contract.id <= profile.unlockedContractId
                ContractCard(
                    contract = contract,
                    unlocked = unlocked,
                    stars = profile.bestStars[contract.id] ?: 0,
                    bestScore = profile.bestScores[contract.id] ?: 0,
                    onClick = { if (unlocked) onStart(contract) }
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SummaryCell(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun ContractCard(
    contract: ContractConfig,
    unlocked: Boolean,
    stars: Int,
    bestScore: Int,
    onClick: () -> Unit
) {
    val gold = MaterialTheme.colorScheme.primary
    val teal = MaterialTheme.colorScheme.secondary
    val featuredGuests = buildList {
        if (contract.vipWeight > 0) add("VIP")
        if (contract.courierWeight > 0) add("Courier")
        if (contract.criticWeight > 0) add("Critic")
        if (contract.socialiteWeight > 0) add("Salon")
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable(enabled = unlocked, onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = if (unlocked) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        tonalElevation = if (unlocked) 8.dp else 0.dp
    ) {
        Box {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0x33122024),
                            Color.Transparent,
                            Color(0x221B3437)
                        )
                    )
                )
                drawCircle(
                    color = gold.copy(alpha = 0.08f),
                    radius = size.minDimension * 0.42f,
                    center = Offset(size.width * 0.12f, size.height * 0.18f)
                )
                drawCircle(
                    color = teal.copy(alpha = 0.08f),
                    radius = size.minDimension * 0.36f,
                    center = Offset(size.width * 0.88f, size.height * 0.74f)
                )
                drawRoundRect(
                    color = gold.copy(alpha = if (unlocked) 0.35f else 0.12f),
                    style = Stroke(width = 2f)
                )
            }

            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Contract ${contract.id}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = contract.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (unlocked) {
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (unlocked) "Unlocked" else "Locked",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (unlocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                        )
                    }
                }
                Text(
                    text = contract.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.74f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ContractPill(label = "${contract.durationSeconds}s")
                    ContractPill(label = "Target ${contract.targetScore}")
                    ContractPill(label = "${stars}/3 stars")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    featuredGuests.forEach { label ->
                        ContractPill(label = label, accent = MaterialTheme.colorScheme.secondary)
                    }
                }
                if (bestScore > 0) {
                    Text(
                        text = "Best house take: $bestScore",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.64f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ContractPill(
    label: String,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = accent
        )
    }
}
