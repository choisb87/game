package com.shadowdungeon.game.game

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class Room(val x: Int, val y: Int, val w: Int, val h: Int) {
    val cx get() = x + w / 2
    val cy get() = y + h / 2
}

object DungeonGenerator {

    fun generate(floor: Int, rng: Random = Random): GeneratedDungeon {
        val width = 28
        val height = 28
        val tiles = Array(height) { Array(width) { TileType.WALL } }

        val roomCount = min(6 + floor / 2, 10)
        val rooms = mutableListOf<Room>()

        repeat(200) {
            if (rooms.size >= roomCount) return@repeat
            val rw = rng.nextInt(4, 8)
            val rh = rng.nextInt(4, 8)
            val rx = rng.nextInt(1, width - rw - 1)
            val ry = rng.nextInt(1, height - rh - 1)
            val candidate = Room(rx, ry, rw, rh)
            val overlaps = rooms.any { r ->
                candidate.x - 1 < r.x + r.w && candidate.x + candidate.w + 1 > r.x &&
                candidate.y - 1 < r.y + r.h && candidate.y + candidate.h + 1 > r.y
            }
            if (!overlaps) rooms.add(candidate)
        }

        if (rooms.size < 3) {
            rooms.clear()
            rooms.add(Room(2, 2, 6, 6))
            rooms.add(Room(12, 12, 6, 6))
            rooms.add(Room(20, 2, 6, 6))
        }

        for (room in rooms) {
            for (dy in 0 until room.h) {
                for (dx in 0 until room.w) {
                    tiles[room.y + dy][room.x + dx] = TileType.FLOOR
                }
            }
        }

        val sorted = rooms.sortedBy { it.cx + it.cy }
        for (i in 0 until sorted.size - 1) {
            carveCorridor(tiles, sorted[i].cx, sorted[i].cy, sorted[i + 1].cx, sorted[i + 1].cy, width, height, rng)
        }

        val startRoom = sorted.first()
        val endRoom = sorted.last()

        tiles[endRoom.cy][endRoom.cx] = TileType.STAIRS_DOWN

        val revealed = Array(height) { BooleanArray(width) }
        val visible = Array(height) { BooleanArray(width) }

        val dungeonFloor = DungeonFloor(width, height, tiles, revealed, visible)

        val enemies = mutableListOf<Enemy>()
        val items = mutableListOf<Item>()
        var nextId = 0

        val availableEnemyTypes = getEnemyTypesForFloor(floor)
        val enemyCount = min(3 + floor, 12)
        val innerRooms = if (sorted.size > 2) sorted.drop(1) else sorted

        repeat(enemyCount) {
            val room = innerRooms[rng.nextInt(innerRooms.size)]
            val ex = room.x + rng.nextInt(1, max(2, room.w - 1))
            val ey = room.y + rng.nextInt(1, max(2, room.h - 1))
            if (dungeonFloor.isWalkable(ex, ey) &&
                !(ex == startRoom.cx && ey == startRoom.cy) &&
                !(ex == endRoom.cx && ey == endRoom.cy) &&
                enemies.none { it.x == ex && it.y == ey }
            ) {
                val type = availableEnemyTypes[rng.nextInt(availableEnemyTypes.size)]
                val floorScale = 1f + (floor - 1) * 0.12f
                enemies.add(
                    Enemy(
                        type = type,
                        x = ex, y = ey,
                        hp = (type.baseHp * floorScale).toInt(),
                        maxHp = (type.baseHp * floorScale).toInt(),
                        atk = (type.baseAtk * floorScale).toInt(),
                        def = (type.baseDef * floorScale).toInt(),
                        id = nextId++
                    )
                )
            }
        }

        val itemCount = 3 + rng.nextInt(3)
        val itemPool = getItemPoolForFloor(floor)
        repeat(itemCount) {
            val room = rooms[rng.nextInt(rooms.size)]
            val ix = room.x + rng.nextInt(1, max(2, room.w - 1))
            val iy = room.y + rng.nextInt(1, max(2, room.h - 1))
            if (dungeonFloor.isWalkable(ix, iy) &&
                !(ix == startRoom.cx && iy == startRoom.cy) &&
                enemies.none { it.x == ix && it.y == iy } &&
                items.none { it.x == ix && it.y == iy }
            ) {
                items.add(Item(type = itemPool[rng.nextInt(itemPool.size)], x = ix, y = iy, id = nextId++))
            }
        }

        return GeneratedDungeon(
            dungeon = dungeonFloor,
            playerX = startRoom.cx,
            playerY = startRoom.cy,
            enemies = enemies,
            items = items,
            nextId = nextId,
        )
    }

    private fun carveCorridor(
        tiles: Array<Array<TileType>>,
        x1: Int, y1: Int, x2: Int, y2: Int,
        width: Int, height: Int, rng: Random
    ) {
        var cx = x1; var cy = y1
        val horizontal = rng.nextBoolean()
        if (horizontal) {
            while (cx != x2) {
                if (cy in 0 until height && cx in 0 until width) tiles[cy][cx] = TileType.FLOOR
                cx += if (x2 > cx) 1 else -1
            }
            while (cy != y2) {
                if (cy in 0 until height && cx in 0 until width) tiles[cy][cx] = TileType.FLOOR
                cy += if (y2 > cy) 1 else -1
            }
        } else {
            while (cy != y2) {
                if (cy in 0 until height && cx in 0 until width) tiles[cy][cx] = TileType.FLOOR
                cy += if (y2 > cy) 1 else -1
            }
            while (cx != x2) {
                if (cy in 0 until height && cx in 0 until width) tiles[cy][cx] = TileType.FLOOR
                cx += if (x2 > cx) 1 else -1
            }
        }
        if (cy in 0 until height && cx in 0 until width) tiles[cy][cx] = TileType.FLOOR
    }

    private fun getEnemyTypesForFloor(floor: Int): List<EnemyType> {
        return when {
            floor <= 2 -> listOf(EnemyType.RAT, EnemyType.SLIME)
            floor <= 4 -> listOf(EnemyType.RAT, EnemyType.SLIME, EnemyType.SKELETON)
            floor <= 6 -> listOf(EnemyType.SLIME, EnemyType.SKELETON, EnemyType.GHOST, EnemyType.ORC)
            floor <= 9 -> listOf(EnemyType.SKELETON, EnemyType.GHOST, EnemyType.ORC, EnemyType.GOLEM)
            floor <= 12 -> listOf(EnemyType.ORC, EnemyType.GOLEM, EnemyType.DEMON)
            else -> listOf(EnemyType.GOLEM, EnemyType.DEMON, EnemyType.DRAGON)
        }
    }

    private fun getItemPoolForFloor(floor: Int): List<ItemType> {
        val pool = mutableListOf(
            ItemType.POTION_SMALL, ItemType.POTION_SMALL, ItemType.GOLD_SMALL
        )
        if (floor >= 2) pool.addAll(listOf(ItemType.WEAPON_DAGGER, ItemType.SHIELD_WOOD))
        if (floor >= 3) pool.addAll(listOf(ItemType.POTION_LARGE, ItemType.GOLD_LARGE))
        if (floor >= 4) pool.addAll(listOf(ItemType.WEAPON_SWORD, ItemType.SHIELD_IRON))
        if (floor >= 5) pool.add(ItemType.SCROLL_REVEAL)
        if (floor >= 6) pool.addAll(listOf(ItemType.WEAPON_AXE, ItemType.SCROLL_SMITE))
        return pool
    }
}

data class GeneratedDungeon(
    val dungeon: DungeonFloor,
    val playerX: Int,
    val playerY: Int,
    val enemies: List<Enemy>,
    val items: List<Item>,
    val nextId: Int,
)
