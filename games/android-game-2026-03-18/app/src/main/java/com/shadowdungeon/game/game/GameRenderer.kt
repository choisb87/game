package com.shadowdungeon.game.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object GameRenderer {

    private val colorDungeonBg = Color(0xFF0A0E17)
    private val colorWallDark = Color(0xFF1A1F2E)
    private val colorWallLight = Color(0xFF2A3040)
    private val colorWallAccent = Color(0xFF353D50)
    private val colorFloorDark = Color(0xFF151A26)
    private val colorFloorLight = Color(0xFF1E2433)
    private val colorFloorDot = Color(0xFF252D3E)
    private val colorPlayerGlow = Color(0xFF4FC3F7)
    private val colorPlayerCore = Color(0xFFBBDEFB)
    private val colorPlayerOutline = Color(0xFF0288D1)
    private val colorEnemyRed = Color(0xFFEF5350)
    private val colorEnemyPurple = Color(0xFFAB47BC)
    private val colorPotionPink = Color(0xFFFF80AB)
    private val colorWeaponOrange = Color(0xFFFFAB40)
    private val colorShieldCyan = Color(0xFF26C6DA)
    private val colorGoldYellow = Color(0xFFFFD54F)
    private val colorStairsGlow = Color(0xFF7E57C2)
    private val colorFog = Color(0xFF050810)
    private val colorFogRevealed = Color(0xFF0A0E17)

    private val enemySymbolPaint = android.graphics.Paint().apply {
        textAlign = android.graphics.Paint.Align.CENTER
        color = android.graphics.Color.WHITE
    }

    private val floatingTextPaint = android.graphics.Paint().apply {
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
        setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
    }

    fun DrawScope.renderGame(state: GameState, time: Float) {
        val d = state.dungeon
        val canvasW = size.width
        val canvasH = size.height

        val viewTilesX = 13
        val viewTilesY = 17
        val tileSize = min(canvasW / viewTilesX, canvasH / viewTilesY)
        val offsetX = (canvasW - viewTilesX * tileSize) / 2f
        val offsetY = (canvasH - viewTilesY * tileSize) / 2f

        val camX = state.player.x - viewTilesX / 2
        val camY = state.player.y - viewTilesY / 2

        val shake = state.shakeAmount
        val shakeX = if (shake > 0) (sin(time * 50f) * shake) else 0f
        val shakeY = if (shake > 0) (sin(time * 43f + 1f) * shake * 0.7f) else 0f

        drawRect(colorDungeonBg, Offset.Zero, size)

        for (ty in -1..viewTilesY) {
            for (tx in -1..viewTilesX) {
                val worldX = camX + tx
                val worldY = camY + ty

                val screenX = offsetX + tx * tileSize + shakeX
                val screenY = offsetY + ty * tileSize + shakeY
                val tileRect = Size(tileSize + 1f, tileSize + 1f)

                if (worldX < 0 || worldX >= d.width || worldY < 0 || worldY >= d.height) {
                    drawRect(colorFog, Offset(screenX, screenY), tileRect)
                    continue
                }

                val visible = d.visible[worldY][worldX]
                val revealed = d.revealed[worldY][worldX]

                if (!revealed) {
                    drawRect(colorFog, Offset(screenX, screenY), tileRect)
                    continue
                }

                val tile = d.tiles[worldY][worldX]
                val fogAlpha = if (visible) 0f else 0.6f

                when (tile) {
                    TileType.WALL -> {
                        val checker = (worldX + worldY) % 2 == 0
                        drawRect(
                            if (checker) colorWallDark else colorWallLight,
                            Offset(screenX, screenY), tileRect
                        )
                        if (visible) {
                            drawRect(colorWallAccent, Offset(screenX, screenY), Size(tileSize, 2f))
                        }
                    }
                    TileType.FLOOR, TileType.DOOR -> {
                        val checker = (worldX + worldY) % 2 == 0
                        drawRect(
                            if (checker) colorFloorDark else colorFloorLight,
                            Offset(screenX, screenY), tileRect
                        )
                        if (visible) {
                            val dotOff = tileSize * 0.5f
                            drawCircle(
                                colorFloorDot,
                                radius = 1.2f,
                                center = Offset(screenX + dotOff, screenY + dotOff)
                            )
                        }
                    }
                    TileType.STAIRS_DOWN -> {
                        drawRect(colorFloorDark, Offset(screenX, screenY), tileRect)
                        if (visible) {
                            val pulse = 0.5f + sin(time * 3f) * 0.3f
                            val center = Offset(screenX + tileSize / 2, screenY + tileSize / 2)
                            drawCircle(
                                colorStairsGlow.copy(alpha = pulse * 0.3f),
                                radius = tileSize * 0.6f,
                                center = center
                            )
                            drawCircle(
                                colorStairsGlow.copy(alpha = pulse),
                                radius = tileSize * 0.25f,
                                center = center
                            )
                            drawCircle(
                                Color.White.copy(alpha = pulse * 0.5f),
                                radius = tileSize * 0.1f,
                                center = center
                            )
                        }
                    }
                }

                if (fogAlpha > 0f) {
                    drawRect(colorFog.copy(alpha = fogAlpha), Offset(screenX, screenY), tileRect)
                }
            }
        }

        // Items
        for (item in state.items) {
            val sx = item.x - camX
            val sy = item.y - camY
            if (sx < -1 || sx > viewTilesX || sy < -1 || sy > viewTilesY) continue
            if (item.x < 0 || item.x >= d.width || item.y < 0 || item.y >= d.height) continue
            if (!d.visible[item.y][item.x]) continue

            val cx = offsetX + sx * tileSize + tileSize / 2 + shakeX
            val cy = offsetY + sy * tileSize + tileSize / 2 + shakeY
            val bobY = sin(time * 2.5f + item.id * 1.3f) * tileSize * 0.05f

            val (color, _) = when (item.type) {
                ItemType.POTION_SMALL, ItemType.POTION_LARGE -> Pair(colorPotionPink, "♥")
                ItemType.WEAPON_DAGGER, ItemType.WEAPON_SWORD, ItemType.WEAPON_AXE -> Pair(colorWeaponOrange, "⚔")
                ItemType.SHIELD_WOOD, ItemType.SHIELD_IRON -> Pair(colorShieldCyan, "🛡")
                ItemType.GOLD_SMALL, ItemType.GOLD_LARGE -> Pair(colorGoldYellow, "◆")
                ItemType.SCROLL_REVEAL, ItemType.SCROLL_SMITE -> Pair(colorStairsGlow, "✦")
            }

            drawCircle(color.copy(alpha = 0.2f), radius = tileSize * 0.35f, center = Offset(cx, cy + bobY))
            drawCircle(color, radius = tileSize * 0.18f, center = Offset(cx, cy + bobY))
            drawCircle(Color.White.copy(alpha = 0.4f), radius = tileSize * 0.08f, center = Offset(cx - tileSize * 0.04f, cy + bobY - tileSize * 0.04f))
        }

        // Enemies
        for (enemy in state.enemies) {
            val sx = enemy.x - camX
            val sy = enemy.y - camY
            if (sx < -1 || sx > viewTilesX || sy < -1 || sy > viewTilesY) continue
            if (enemy.x < 0 || enemy.x >= d.width || enemy.y < 0 || enemy.y >= d.height) continue
            if (!d.visible[enemy.y][enemy.x]) continue

            val cx = offsetX + sx * tileSize + tileSize / 2 + shakeX
            val cy = offsetY + sy * tileSize + tileSize / 2 + shakeY

            val enemyColor = when (enemy.type) {
                EnemyType.RAT, EnemyType.SLIME -> colorEnemyRed.copy(alpha = 0.8f)
                EnemyType.SKELETON, EnemyType.GHOST -> colorEnemyPurple.copy(alpha = 0.9f)
                EnemyType.ORC, EnemyType.GOLEM -> colorEnemyRed
                EnemyType.DEMON, EnemyType.DRAGON -> Color(0xFFFF1744)
            }

            val breathe = 1f + sin(time * 2f + enemy.id * 0.7f) * 0.08f

            if (!enemy.asleep) {
                drawCircle(
                    enemyColor.copy(alpha = 0.15f),
                    radius = tileSize * 0.45f * breathe,
                    center = Offset(cx, cy)
                )
            }

            drawCircle(
                enemyColor,
                radius = tileSize * 0.3f * breathe,
                center = Offset(cx, cy)
            )

            if (enemy.asleep) {
                drawCircle(
                    Color.Gray.copy(alpha = 0.3f),
                    radius = tileSize * 0.32f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5f)
                )
            }

            // HP bar
            if (enemy.hp < enemy.maxHp) {
                val barW = tileSize * 0.7f
                val barH = tileSize * 0.08f
                val barX = cx - barW / 2
                val barY = cy - tileSize * 0.42f
                drawRect(Color(0xFF1A1A1A), Offset(barX, barY), Size(barW, barH))
                val hpFrac = enemy.hp.toFloat() / enemy.maxHp
                val hpColor = if (hpFrac > 0.5f) Color(0xFF66BB6A) else if (hpFrac > 0.25f) colorWeaponOrange else colorEnemyRed
                drawRect(hpColor, Offset(barX, barY), Size(barW * hpFrac, barH))
            }

            // Symbol
            enemySymbolPaint.textSize = tileSize * 0.35f
            drawContext.canvas.nativeCanvas.drawText(
                enemy.type.symbol,
                cx - tileSize * 0.15f,
                cy + tileSize * 0.12f,
                enemySymbolPaint
            )
        }

        // Player
        run {
            val px = state.player.x - camX
            val py = state.player.y - camY
            val cx = offsetX + px * tileSize + tileSize / 2 + shakeX + state.playerAnimOffset.x
            val cy = offsetY + py * tileSize + tileSize / 2 + shakeY + state.playerAnimOffset.y

            val glowPulse = 0.7f + sin(time * 4f) * 0.3f

            drawCircle(
                colorPlayerGlow.copy(alpha = 0.1f * glowPulse),
                radius = tileSize * 0.8f,
                center = Offset(cx, cy)
            )
            drawCircle(
                colorPlayerGlow.copy(alpha = 0.2f * glowPulse),
                radius = tileSize * 0.5f,
                center = Offset(cx, cy)
            )
            drawCircle(
                colorPlayerOutline,
                radius = tileSize * 0.32f,
                center = Offset(cx, cy)
            )
            drawCircle(
                colorPlayerCore,
                radius = tileSize * 0.24f,
                center = Offset(cx, cy)
            )
            drawCircle(
                Color.White,
                radius = tileSize * 0.1f,
                center = Offset(cx - tileSize * 0.06f, cy - tileSize * 0.06f)
            )
        }

        // Particles
        for (p in state.particles) {
            val sx = p.x - camX
            val sy = p.y - camY
            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            drawCircle(
                Color(p.color).copy(alpha = alpha),
                radius = p.size * alpha,
                center = Offset(
                    offsetX + sx * tileSize + tileSize / 2 + shakeX,
                    offsetY + sy * tileSize + tileSize / 2 + shakeY
                )
            )
        }

        // Floating texts
        floatingTextPaint.textSize = tileSize * 0.32f
        for (ft in state.floatingTexts) {
            val sx = ft.x - camX
            val sy = ft.y - camY
            val alpha = (ft.life * 255).toInt().coerceIn(0, 255)
            floatingTextPaint.color = android.graphics.Color.argb(alpha, 255, 255, 255)
            drawContext.canvas.nativeCanvas.drawText(
                ft.text,
                offsetX + sx * tileSize + tileSize / 2 + shakeX,
                offsetY + sy * tileSize + tileSize / 2 + shakeY,
                floatingTextPaint
            )
        }

        // Vignette
        drawRect(
            Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)),
                center = Offset(canvasW / 2, canvasH / 2),
                radius = canvasW * 0.8f,
            ),
            Offset.Zero, size
        )
    }

    private val hudPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        isFakeBoldText = true
    }

    private val msgPaint = android.graphics.Paint().apply {
        isAntiAlias = true
    }

    fun DrawScope.renderHUD(state: GameState, canvasWidth: Float) {
        val scale = canvasWidth / 480f  // normalize to 480dp base width
        val hudH = 52f * scale
        val textSize = 28f * scale
        val textY = 35f * scale
        val pad = 16f * scale

        drawRect(Color(0xCC0D1117), Offset.Zero, Size(canvasWidth, hudH))
        drawRect(Color(0xFF4FC3F7).copy(alpha = 0.3f), Offset(0f, hudH - 1f), Size(canvasWidth, 1f))

        hudPaint.textSize = textSize
        val canvas = drawContext.canvas.nativeCanvas

        hudPaint.color = android.graphics.Color.argb(255, 126, 87, 194)
        canvas.drawText("B${state.floor}F", pad, textY, hudPaint)

        val hpText = "♥${state.player.hp}/${state.player.maxHp}"
        hudPaint.color = when {
            state.player.hp > state.player.maxHp * 0.6f -> android.graphics.Color.argb(255, 255, 128, 171)
            state.player.hp > state.player.maxHp * 0.3f -> android.graphics.Color.argb(255, 255, 171, 64)
            else -> android.graphics.Color.argb(255, 239, 83, 80)
        }
        canvas.drawText(hpText, 100f * scale, textY, hudPaint)

        hudPaint.color = android.graphics.Color.argb(255, 255, 171, 64)
        canvas.drawText("⚔${state.player.atk}", 260f * scale, textY, hudPaint)
        hudPaint.color = android.graphics.Color.argb(255, 38, 198, 218)
        canvas.drawText("🛡${state.player.def}", 340f * scale, textY, hudPaint)
        hudPaint.color = android.graphics.Color.argb(255, 79, 195, 247)
        canvas.drawText("Lv${state.player.level}", 420f * scale, textY, hudPaint)

        val goldText = "${state.player.gold}G"
        hudPaint.color = android.graphics.Color.argb(255, 255, 213, 79)
        val goldW = hudPaint.measureText(goldText)
        canvas.drawText(goldText, canvasWidth - goldW - pad, textY, hudPaint)

        // Message bar at bottom
        if (state.message.isNotEmpty()) {
            val msgH = 36f * scale
            val msgY = size.height - msgH
            drawRect(Color(0xCC0D1117), Offset(0f, msgY), Size(canvasWidth, msgH))
            msgPaint.textSize = 22f * scale
            msgPaint.color = android.graphics.Color.argb(200, 200, 210, 230)
            canvas.drawText(state.message, pad, msgY + 26f * scale, msgPaint)
        }
    }
}
