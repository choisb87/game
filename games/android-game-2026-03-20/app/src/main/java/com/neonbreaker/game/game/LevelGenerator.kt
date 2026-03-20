package com.neonbreaker.game.game

import kotlin.math.min
import kotlin.random.Random

object LevelGenerator {

    private val NORMAL_TYPES = listOf(
        BrickType.NORMAL_CYAN, BrickType.NORMAL_MAGENTA, BrickType.NORMAL_GREEN,
        BrickType.NORMAL_ORANGE, BrickType.NORMAL_YELLOW, BrickType.NORMAL_BLUE,
    )

    fun generate(level: Int, cols: Int = 8, canvasW: Float, topOffset: Float): List<Brick> {
        val rows = min(4 + level / 2, 10)
        val brickW = canvasW / cols
        val brickH = brickW * 0.42f
        val bricks = mutableListOf<Brick>()

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (!shouldPlace(level, r, rows, c, cols)) continue
                val type = pickType(level, r, rows)
                bricks += Brick(
                    row = r, col = c, type = type,
                    hitsLeft = type.hits,
                    x = c * brickW,
                    y = topOffset + r * brickH,
                    width = brickW,
                    height = brickH,
                )
            }
        }
        return bricks
    }

    private fun shouldPlace(level: Int, r: Int, rows: Int, c: Int, cols: Int): Boolean {
        // Patterns vary by level for visual variety
        return when (level % 5) {
            1 -> true                                                 // full grid
            2 -> (r + c) % 2 == 0                                    // checkerboard
            3 -> c > 0 && c < cols - 1                               // center columns
            4 -> r < rows - 1 || (c in 2 until cols - 2)             // inverted V gap
            0 -> {                                                    // diamond
                val midR = rows / 2; val midC = cols / 2
                val dr = if (r <= midR) r else rows - 1 - r
                c in (midC - dr - 1)..(midC + dr)
            }
            else -> true
        }
    }

    private fun pickType(level: Int, row: Int, totalRows: Int): BrickType {
        val rand = Random.nextFloat()
        val armorChance = (level - 3).coerceAtLeast(0) * 0.03f
        val toughChance = (level - 1).coerceAtLeast(0) * 0.06f

        // Top rows are tougher
        val topBonus = if (row < totalRows / 3) 0.1f else 0f

        return when {
            rand < armorChance + topBonus * 0.5f -> BrickType.ARMOR
            rand < armorChance + toughChance + topBonus -> {
                if (Random.nextBoolean()) BrickType.TOUGH_RED else BrickType.TOUGH_PURPLE
            }
            else -> NORMAL_TYPES[Random.nextInt(NORMAL_TYPES.size)]
        }
    }
}
