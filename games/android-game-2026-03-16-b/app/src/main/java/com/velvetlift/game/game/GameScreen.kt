package com.velvetlift.game.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.android.awaitFrame

@Composable
fun GameScreen(
    contract: ContractConfig,
    perks: CareerPerks,
    onExit: () -> Unit,
    onComplete: (ContractResult) -> Unit
) {
    BackHandler { onExit() }

    val textMeasurer = rememberTextMeasurer()
    var phase by remember { mutableFloatStateOf(0f) }
    var state by remember(contract.id) { mutableStateOf(GameEngine.newRun(contract)) }

    LaunchedEffect(contract.id, perks) {
        var lastFrame = 0L
        while (true) {
            awaitFrame()
            val now = System.nanoTime()
            if (lastFrame == 0L) {
                lastFrame = now
                continue
            }
            val dt = ((now - lastFrame) / 1_000_000_000f).coerceIn(0f, 0.05f)
            lastFrame = now
            phase += dt * 0.7f
            val updated = GameEngine.step(state, dt, perks)
            state = updated
            val result = updated.result
            if (result != null) {
                onComplete(result)
                break
            }
        }
    }

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
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x33D8B16B), Color.Transparent)
                ),
                radius = size.minDimension * 0.42f,
                center = Offset(size.width * 0.18f, size.height * 0.16f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x2263C6C1), Color.Transparent)
                ),
                radius = size.minDimension * 0.5f,
                center = Offset(size.width * 0.86f, size.height * 0.38f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GameHud(
                state = state,
                contract = contract,
                onExit = onExit
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
                    .padding(14.dp)
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    HotelRenderer.render(
                        scope = this,
                        state = state,
                        phase = phase,
                        textMeasurer = textMeasurer
                    )
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    repeat(contract.totalFloors) { index ->
                        val floor = contract.totalFloors - 1 - index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clickable {
                                    state = GameEngine.queueFloor(state, floor)
                                }
                        )
                    }
                }
            }

            BottomConsole(
                state = state,
                contract = contract,
                perks = perks
            )
        }
    }
}

@Composable
private fun GameHud(
    state: HotelState,
    contract: ContractConfig,
    onExit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = contract.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Target ${contract.targetScore}   Score ${state.score}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .clickable(onClick = onExit)
                ) {
                    Text(
                        text = "Lobby",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricBox(
                    modifier = Modifier.weight(1f),
                    label = "Time",
                    value = "${state.remainingTime.toInt()}s"
                )
                MetricBox(
                    modifier = Modifier.weight(1f),
                    label = "Mood",
                    value = "${(state.mood * 100).toInt()}%"
                )
                MetricBox(
                    modifier = Modifier.weight(1f),
                    label = "Combo",
                    value = if (state.combo > 0) "x${state.combo}" else "--"
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "House Mood",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                LinearProgressIndicator(
                    progress = { state.mood.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    color = if (state.mood > 0.35f) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                )
            }
        }
    }
}

@Composable
private fun MetricBox(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun BottomConsole(
    state: HotelState,
    contract: ContractConfig,
    perks: CareerPerks
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Queued Stops",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val leadStop = state.activeStop?.let { "En route ${floorLabel(it, contract.totalFloors)}" }
                    if (leadStop != null) {
                        ConsoleChip(label = leadStop, accent = MaterialTheme.colorScheme.secondary)
                    }
                    if (state.queuedStops.isEmpty()) {
                        ConsoleChip(label = "Tap floors to queue", accent = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    } else {
                        state.queuedStops.forEach { stop ->
                            ConsoleChip(
                                label = floorLabel(stop, contract.totalFloors),
                                accent = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Cabin Manifest",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(perks.cabinCapacity) { index ->
                        val guest = state.passengers.getOrNull(index)
                        if (guest == null) {
                            EmptySeat()
                        } else {
                            PassengerCard(
                                guest = guest,
                                contract = contract
                            )
                        }
                    }
                }
            }

            Text(
                text = contract.briefing,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
private fun ConsoleChip(
    label: String,
    accent: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(accent.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = accent
        )
    }
}

@Composable
private fun EmptySeat() {
    Box(
        modifier = Modifier
            .size(width = 88.dp, height = 82.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Empty",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.36f)
        )
    }
}

@Composable
private fun PassengerCard(
    guest: Guest,
    contract: ContractConfig
) {
    Column(
        modifier = Modifier
            .size(width = 88.dp, height = 82.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(guest.archetype.accent.copy(alpha = 0.12f))
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = guest.archetype.shortCode,
            style = MaterialTheme.typography.labelLarge,
            color = guest.archetype.accent
        )
        Text(
            text = floorLabel(guest.destination, contract.totalFloors),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        LinearProgressIndicator(
            progress = { guest.patience.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(10.dp)),
            color = guest.archetype.accent,
            trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
        )
    }
}
