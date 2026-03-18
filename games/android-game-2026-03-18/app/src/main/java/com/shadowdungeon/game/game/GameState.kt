package com.shadowdungeon.game.game

import androidx.compose.ui.geometry.Offset

enum class TileType { FLOOR, WALL, STAIRS_DOWN, DOOR }

enum class ItemType {
    POTION_SMALL,   // +3 HP
    POTION_LARGE,   // +6 HP
    WEAPON_DAGGER,  // +1 ATK
    WEAPON_SWORD,   // +2 ATK
    WEAPON_AXE,     // +3 ATK
    SHIELD_WOOD,    // +1 DEF
    SHIELD_IRON,    // +2 DEF
    GOLD_SMALL,     // +5 gold
    GOLD_LARGE,     // +15 gold
    SCROLL_REVEAL,  // reveal map
    SCROLL_SMITE,   // damage all visible enemies
}

enum class EnemyType(val symbol: String, val baseHp: Int, val baseAtk: Int, val baseDef: Int, val xpValue: Int) {
    RAT("🐀", 2, 1, 0, 5),
    SLIME("💧", 3, 1, 0, 8),
    SKELETON("💀", 4, 2, 1, 12),
    GHOST("👻", 3, 3, 0, 15),
    ORC("👹", 6, 3, 1, 20),
    GOLEM("🗿", 10, 2, 3, 30),
    DEMON("😈", 8, 5, 2, 40),
    DRAGON("🐉", 15, 6, 3, 60),
}

data class Enemy(
    val type: EnemyType,
    val x: Int,
    val y: Int,
    val hp: Int,
    val maxHp: Int,
    val atk: Int,
    val def: Int,
    val asleep: Boolean = true,
    val id: Int = 0,
)

data class Item(
    val type: ItemType,
    val x: Int,
    val y: Int,
    val id: Int = 0,
)

data class Player(
    val x: Int,
    val y: Int,
    val hp: Int,
    val maxHp: Int,
    val atk: Int,
    val def: Int,
    val gold: Int,
    val xp: Int,
    val level: Int,
)

data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val life: Float,
    val maxLife: Float,
    val color: Long,
    val size: Float,
)

data class FloatingText(
    val text: String,
    val x: Float,
    val y: Float,
    val life: Float,
    val color: Long,
)

data class DungeonFloor(
    val width: Int,
    val height: Int,
    val tiles: Array<Array<TileType>>,
    val revealed: Array<BooleanArray>,
    val visible: Array<BooleanArray>,
) {
    fun tileAt(x: Int, y: Int): TileType {
        if (x < 0 || x >= width || y < 0 || y >= height) return TileType.WALL
        return tiles[y][x]
    }

    fun isWalkable(x: Int, y: Int): Boolean {
        val t = tileAt(x, y)
        return t == TileType.FLOOR || t == TileType.STAIRS_DOWN || t == TileType.DOOR
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DungeonFloor) return false
        return width == other.width && height == other.height
    }

    override fun hashCode(): Int = width * 31 + height
}

data class GameState(
    val player: Player,
    val dungeon: DungeonFloor,
    val enemies: List<Enemy>,
    val items: List<Item>,
    val floor: Int,
    val score: Int,
    val turnCount: Int,
    val particles: List<Particle>,
    val floatingTexts: List<FloatingText>,
    val gameOver: Boolean,
    val message: String,
    val shakeAmount: Float = 0f,
    val nextId: Int = 0,
    val animatingMove: Boolean = false,
    val playerAnimOffset: Offset = Offset.Zero,
) {
    companion object {
        fun xpForLevel(level: Int): Int = 15 + (level - 1) * 12
    }
}
