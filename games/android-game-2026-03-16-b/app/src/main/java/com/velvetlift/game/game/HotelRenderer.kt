package com.velvetlift.game.game

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.sin

object HotelRenderer {
    fun render(
        scope: DrawScope,
        state: HotelState,
        phase: Float,
        textMeasurer: TextMeasurer
    ) {
        with(scope) {
        val outlineColor = Color(0x55D8B16B)
        val buildingTop = size.height * 0.04f
        val buildingBottom = size.height * 0.98f
        val buildingLeft = size.width * 0.06f
        val buildingRight = size.width * 0.94f
        val buildingWidth = buildingRight - buildingLeft
        val floorHeight = (buildingBottom - buildingTop) / state.contract.totalFloors
        val shaftLeft = buildingLeft + buildingWidth * 0.62f
        val shaftRight = buildingRight - buildingWidth * 0.07f
        val roomLeft = buildingLeft + buildingWidth * 0.05f
        val roomRight = shaftLeft - buildingWidth * 0.04f

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xAA16252B),
                    Color(0xAA111D22),
                    Color(0xAA0C161A)
                )
            ),
            topLeft = Offset(buildingLeft, buildingTop),
            size = Size(buildingWidth, buildingBottom - buildingTop),
            cornerRadius = CornerRadius(38f, 38f)
        )
        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(buildingLeft, buildingTop),
            size = Size(buildingWidth, buildingBottom - buildingTop),
            cornerRadius = CornerRadius(38f, 38f),
            style = Stroke(width = 2.5f)
        )

        drawRoundRect(
            color = Color(0x22162228),
            topLeft = Offset(shaftLeft, buildingTop + 12f),
            size = Size(shaftRight - shaftLeft, buildingBottom - buildingTop - 24f),
            cornerRadius = CornerRadius(28f, 28f)
        )

        val floorLineColor = Color(0x22F7F1E3)
        val labelStyle = TextStyle(
            color = Color(0xAAFFF7E5),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif
        )
        val guestStyle = TextStyle(
            color = Color(0xFF10181B),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif
        )
        val destinationStyle = TextStyle(
            color = Color(0xFFF7F1E3),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif
        )

        for (floor in 0 until state.contract.totalFloors) {
            val top = buildingBottom - floorHeight * (floor + 1)
            val centerY = top + floorHeight / 2f
            val highlightFloor = state.activeStop == floor || state.selectedFloor == floor
            val glowAlpha = when {
                state.activeStop == floor -> 0.16f + 0.08f * sin(phase * 2f)
                state.selectedFloor == floor -> 0.08f + state.selectionPulse * 0.18f
                else -> 0f
            }
            if (glowAlpha > 0f) {
                drawRect(
                    color = if (state.activeStop == floor) Color(0x2263C6C1) else Color(0x22D8B16B),
                    topLeft = Offset(buildingLeft + 8f, top + 3f),
                    size = Size(buildingWidth - 16f, floorHeight - 6f)
                )
            }
            drawLine(
                color = floorLineColor,
                start = Offset(buildingLeft + 12f, top),
                end = Offset(buildingRight - 12f, top),
                strokeWidth = 1f
            )

            val label = floorLabel(floor, state.contract.totalFloors)
            val labelResult = textMeasurer.measure(label, style = labelStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = label,
                style = labelStyle,
                topLeft = Offset(buildingLeft + 18f, centerY - labelResult.size.height / 2f)
            )

            val waiting = state.waitingByFloor[floor].orEmpty().take(4)
            waiting.forEachIndexed { index, guest ->
                val cardWidth = (roomRight - roomLeft - 22f) / 4f
                val x = roomLeft + index * (cardWidth + 6f)
                val cardHeight = floorHeight * 0.58f
                val y = centerY - cardHeight / 2f
                drawRoundRect(
                    color = guest.archetype.accent.copy(alpha = 0.92f * (0.35f + guest.patience * 0.65f)),
                    topLeft = Offset(x, y),
                    size = Size(cardWidth, cardHeight),
                    cornerRadius = CornerRadius(18f, 18f)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = guest.archetype.shortCode,
                    style = guestStyle,
                    topLeft = Offset(x + 8f, y + 7f)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = floorLabel(guest.destination, state.contract.totalFloors),
                    style = destinationStyle,
                    topLeft = Offset(x + 8f, y + cardHeight - 21f)
                )
            }

            if (highlightFloor && state.arrivalPulse > 0f) {
                drawRect(
                    color = Color(0x2263C6C1).copy(alpha = state.arrivalPulse),
                    topLeft = Offset(buildingLeft + 10f, top + 4f),
                    size = Size(buildingWidth - 20f, floorHeight - 8f)
                )
            }
        }

        val queueFloors = listOfNotNull(state.activeStop) + state.queuedStops
        queueFloors.forEachIndexed { index, floor ->
            val top = buildingBottom - floorHeight * (floor + 1)
            val markerY = top + floorHeight / 2f
            val color = if (index == 0) Color(0xFF63C6C1) else Color(0xFFD8B16B)
            drawCircle(
                color = color.copy(alpha = 0.95f),
                radius = 8f,
                center = Offset(buildingRight - 20f, markerY)
            )
        }

        val carCenterY = buildingBottom - floorHeight * (state.carFloor + 0.5f)
        val cabinWidth = shaftRight - shaftLeft - 16f
        val cabinHeight = floorHeight * 0.72f
        val cabinLeft = shaftLeft + 8f
        val cabinTop = carCenterY - cabinHeight / 2f
        val openAmount = if (state.doorTimer > 0f) {
            0.18f + 0.14f * sin((phase + state.doorTimer) * 4.5f)
        } else {
            0.02f
        }

        drawRoundRect(
            color = Color(0x33D8B16B),
            topLeft = Offset(cabinLeft - 8f, cabinTop - 8f),
            size = Size(cabinWidth + 16f, cabinHeight + 16f),
            cornerRadius = CornerRadius(28f, 28f)
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFE7D7A9),
                    Color(0xFFC89F59),
                    Color(0xFF73552D)
                )
            ),
            topLeft = Offset(cabinLeft, cabinTop),
            size = Size(cabinWidth, cabinHeight),
            cornerRadius = CornerRadius(24f, 24f)
        )
        drawRoundRect(
            color = Color(0x990B1418),
            topLeft = Offset(cabinLeft + 8f, cabinTop + 8f),
            size = Size(cabinWidth / 2f - cabinWidth * openAmount, cabinHeight - 16f),
            cornerRadius = CornerRadius(18f, 18f)
        )
        drawRoundRect(
            color = Color(0x990B1418),
            topLeft = Offset(cabinLeft + cabinWidth / 2f + cabinWidth * openAmount, cabinTop + 8f),
            size = Size(cabinWidth / 2f - cabinWidth * openAmount - 8f, cabinHeight - 16f),
            cornerRadius = CornerRadius(18f, 18f)
        )

        val cabinStyle = TextStyle(
            color = Color(0xFFF7F1E3),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Serif
        )
        drawText(
            textMeasurer = textMeasurer,
            text = "MERIDIAN",
            style = cabinStyle,
            topLeft = Offset(cabinLeft + 10f, cabinTop + 10f)
        )

        state.floatingTexts.forEach { text ->
            val baseTop = buildingBottom - floorHeight * (text.floor + 1)
            val progress = text.age / 1.3f
            val offsetY = floorHeight * 0.4f * progress
            drawText(
                textMeasurer = textMeasurer,
                text = text.text,
                style = TextStyle(
                    color = text.color.copy(alpha = 1f - progress),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                ),
                topLeft = Offset(shaftLeft - 64f, baseTop + 10f - offsetY)
            )
        }
        }
    }
}
