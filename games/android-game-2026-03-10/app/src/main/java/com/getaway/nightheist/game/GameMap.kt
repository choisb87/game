package com.getaway.nightheist.game

import kotlin.math.atan2
import kotlin.random.Random

object MapGenerator {

    fun generate(level: Int, seed: Long = System.nanoTime()): GameMap {
        val rng = Random(seed)
        val w = 16
        val h = 22

        // Start with all walls
        val tiles = Array(h) { Array(w) { TileType.WALL } }

        // Carve rooms and corridors using BSP-like approach
        carveRooms(tiles, w, h, rng)

        // Ensure border walls
        for (x in 0 until w) {
            tiles[0][x] = TileType.WALL
            tiles[h - 1][x] = TileType.WALL
        }
        for (y in 0 until h) {
            tiles[y][0] = TileType.WALL
            tiles[y][w - 1] = TileType.WALL
        }

        // Place spawn (top-left area)
        val spawn = findFloorTile(tiles, w, h, 1, 1, 5, 5, rng)
        // Place exit (bottom-right area)
        val exit = findFloorTile(tiles, w, h, w - 6, h - 6, w - 2, h - 2, rng)
        tiles[exit.y][exit.x] = TileType.EXIT

        // Place loot
        val lootCount = 3 + level.coerceAtMost(5)
        val lootPositions = mutableListOf<GridPos>()
        repeat(lootCount) {
            val loot = findRandomFloor(tiles, w, h, rng, exclude = lootPositions + listOf(spawn, exit))
            if (loot != null) {
                tiles[loot.y][loot.x] = TileType.LOOT
                lootPositions.add(loot)
            }
        }

        // Place hide spots
        val hideCount = 4 + level.coerceAtMost(3)
        repeat(hideCount) {
            val spot = findRandomFloor(tiles, w, h, rng, exclude = lootPositions + listOf(spawn, exit))
            if (spot != null) {
                tiles[spot.y][spot.x] = TileType.HIDESPOT
            }
        }

        val tileList = tiles.map { row -> row.toList() }
        return GameMap(
            width = w,
            height = h,
            tiles = tileList,
            spawnPoint = spawn,
            exitPoint = exit
        )
    }

    private fun carveRooms(tiles: Array<Array<TileType>>, w: Int, h: Int, rng: Random) {
        // Create a grid of rooms connected by corridors
        val roomW = 3
        val roomH = 3
        val roomsX = (w - 2) / (roomW + 1)
        val roomsY = (h - 2) / (roomH + 1)

        data class Room(val cx: Int, val cy: Int, val rw: Int, val rh: Int)
        val rooms = mutableListOf<Room>()

        for (ry in 0 until roomsY) {
            for (rx in 0 until roomsX) {
                val startX = 1 + rx * (roomW + 1)
                val startY = 1 + ry * (roomH + 1)
                val actualW = if (rng.nextFloat() > 0.3f) roomW else roomW - 1
                val actualH = if (rng.nextFloat() > 0.3f) roomH else roomH - 1

                // Carve room
                for (dy in 0 until actualH) {
                    for (dx in 0 until actualW) {
                        val tx = startX + dx
                        val ty = startY + dy
                        if (tx in 1 until w - 1 && ty in 1 until h - 1) {
                            tiles[ty][tx] = TileType.FLOOR
                        }
                    }
                }
                rooms.add(Room(startX + actualW / 2, startY + actualH / 2, actualW, actualH))
            }
        }

        // Connect adjacent rooms with corridors
        for (ry in 0 until roomsY) {
            for (rx in 0 until roomsX) {
                val idx = ry * roomsX + rx
                // Connect right
                if (rx < roomsX - 1) {
                    val a = rooms[idx]
                    val b = rooms[idx + 1]
                    carveHCorridor(tiles, a.cx, b.cx, a.cy, w, h)
                }
                // Connect down
                if (ry < roomsY - 1) {
                    val a = rooms[idx]
                    val bIdx = (ry + 1) * roomsX + rx
                    if (bIdx < rooms.size) {
                        val b = rooms[bIdx]
                        carveVCorridor(tiles, a.cy, b.cy, a.cx, w, h)
                    }
                }
            }
        }

        // Add some random extra corridors for alternate routes
        repeat(3 + rng.nextInt(3)) {
            if (rooms.size >= 2) {
                val a = rooms[rng.nextInt(rooms.size)]
                val b = rooms[rng.nextInt(rooms.size)]
                if (a != b) {
                    carveHCorridor(tiles, a.cx, b.cx, a.cy, w, h)
                    carveVCorridor(tiles, a.cy, b.cy, b.cx, w, h)
                }
            }
        }
    }

    private fun carveHCorridor(tiles: Array<Array<TileType>>, x1: Int, x2: Int, y: Int, w: Int, h: Int) {
        val minX = minOf(x1, x2)
        val maxX = maxOf(x1, x2)
        for (x in minX..maxX) {
            if (x in 1 until w - 1 && y in 1 until h - 1) {
                tiles[y][x] = TileType.FLOOR
            }
        }
    }

    private fun carveVCorridor(tiles: Array<Array<TileType>>, y1: Int, y2: Int, x: Int, w: Int, h: Int) {
        val minY = minOf(y1, y2)
        val maxY = maxOf(y1, y2)
        for (y in minY..maxY) {
            if (x in 1 until w - 1 && y in 1 until h - 1) {
                tiles[y][x] = TileType.FLOOR
            }
        }
    }

    private fun findFloorTile(
        tiles: Array<Array<TileType>>, w: Int, h: Int,
        minX: Int, minY: Int, maxX: Int, maxY: Int, rng: Random
    ): GridPos {
        repeat(100) {
            val x = rng.nextInt(minX.coerceAtLeast(1), maxX.coerceAtMost(w - 1) + 1)
            val y = rng.nextInt(minY.coerceAtLeast(1), maxY.coerceAtMost(h - 1) + 1)
            if (tiles[y][x] == TileType.FLOOR) return GridPos(x, y)
        }
        // Fallback: find any floor in range
        for (y in minY.coerceAtLeast(1)..maxY.coerceAtMost(h - 2)) {
            for (x in minX.coerceAtLeast(1)..maxX.coerceAtMost(w - 2)) {
                if (tiles[y][x] == TileType.FLOOR) return GridPos(x, y)
            }
        }
        // Absolute fallback
        tiles[2][2] = TileType.FLOOR
        return GridPos(2, 2)
    }

    private fun findRandomFloor(
        tiles: Array<Array<TileType>>, w: Int, h: Int, rng: Random,
        exclude: List<GridPos> = emptyList()
    ): GridPos? {
        repeat(200) {
            val x = rng.nextInt(1, w - 1)
            val y = rng.nextInt(1, h - 1)
            if (tiles[y][x] == TileType.FLOOR && GridPos(x, y) !in exclude) {
                return GridPos(x, y)
            }
        }
        return null
    }

    fun generateCopPatrols(map: GameMap, level: Int, rng: Random = Random): List<Cop> {
        val copCount = 2 + (level - 1).coerceAtMost(4) // 2-6 cops
        val cops = mutableListOf<Cop>()

        repeat(copCount) { i ->
            val patrol = generatePatrolRoute(map, rng)
            if (patrol.isNotEmpty()) {
                val startPos = patrol[0].toFloat()
                val nextPos = if (patrol.size > 1) patrol[1].toFloat() else startPos
                val angle = atan2(nextPos.y - startPos.y, nextPos.x - startPos.x)

                cops.add(
                    Cop(
                        x = startPos.x,
                        y = startPos.y,
                        patrolRoute = patrol,
                        patrolIndex = 0,
                        facingAngle = angle,
                        speed = 1.5f + (level * 0.1f).coerceAtMost(0.8f),
                        chaseSpeed = 2.8f + (level * 0.15f).coerceAtMost(1.0f),
                        visionRange = 4.0f + (level * 0.2f).coerceAtMost(1.5f),
                        visionAngle = 65f - (level * 2f).coerceAtMost(15f)
                    )
                )
            }
        }
        return cops
    }

    private fun generatePatrolRoute(map: GameMap, rng: Random): List<GridPos> {
        val route = mutableListOf<GridPos>()
        // Find a starting floor tile away from spawn
        repeat(50) {
            val x = rng.nextInt(3, map.width - 3)
            val y = rng.nextInt(3, map.height - 3)
            if (map.tileAt(x, y) != TileType.WALL) {
                route.add(GridPos(x, y))

                // Add 3-5 waypoints
                val waypointCount = 3 + rng.nextInt(3)
                repeat(waypointCount) {
                    val dx = rng.nextInt(-4, 5)
                    val dy = rng.nextInt(-4, 5)
                    val nx = (x + dx).coerceIn(2, map.width - 3)
                    val ny = (y + dy).coerceIn(2, map.height - 3)
                    if (map.tileAt(nx, ny) != TileType.WALL) {
                        route.add(GridPos(nx, ny))
                    }
                }
                return@repeat
            }
        }
        return route
    }

    fun generateBackupCop(map: GameMap, playerPos: GridPos, rng: Random): Cop? {
        // Spawn from map edges
        val edge = rng.nextInt(4)
        val pos = when (edge) {
            0 -> GridPos(1, rng.nextInt(1, map.height - 1))
            1 -> GridPos(map.width - 2, rng.nextInt(1, map.height - 1))
            2 -> GridPos(rng.nextInt(1, map.width - 1), 1)
            else -> GridPos(rng.nextInt(1, map.width - 1), map.height - 2)
        }
        if (map.tileAt(pos.x, pos.y) == TileType.WALL) return null

        val startPos = pos.toFloat()
        return Cop(
            x = startPos.x,
            y = startPos.y,
            patrolRoute = listOf(pos, playerPos),
            patrolIndex = 0,
            facingAngle = 0f,
            speed = 2.5f,
            chaseSpeed = 3.5f,
            visionRange = 5.0f,
            state = CopState.CHASE,
            lastKnownPlayerX = playerPos.x + 0.5f,
            lastKnownPlayerY = playerPos.y + 0.5f
        )
    }
}
