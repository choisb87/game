package com.shadowdungeon.game.game

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

object GameLogic {

    fun createInitialState(): GameState {
        val gen = DungeonGenerator.generate(1)
        val player = Player(
            x = gen.playerX, y = gen.playerY,
            hp = 20, maxHp = 20,
            atk = 3, def = 1,
            gold = 0, xp = 0, level = 1
        )
        val state = GameState(
            player = player,
            dungeon = gen.dungeon,
            enemies = gen.enemies,
            items = gen.items,
            floor = 1, score = 0, turnCount = 0,
            particles = emptyList(),
            floatingTexts = emptyList(),
            gameOver = false,
            message = "그림자 던전에 진입했다...",
            nextId = gen.nextId,
        )
        return updateVisibility(state)
    }

    fun processSwipe(state: GameState, dx: Int, dy: Int): GameState {
        if (state.gameOver || state.animatingMove) return state

        val nx = state.player.x + dx
        val ny = state.player.y + dy

        if (!state.dungeon.isWalkable(nx, ny)) return state

        val enemyAt = state.enemies.find { it.x == nx && it.y == ny }

        return if (enemyAt != null) {
            processAttack(state, enemyAt)
        } else {
            processMove(state, nx, ny)
        }
    }

    private fun processAttack(state: GameState, target: Enemy): GameState {
        val rng = Random
        val playerDmg = max(1, state.player.atk - target.def + rng.nextInt(-1, 2))
        val newHp = target.hp - playerDmg

        val particles = mutableListOf<Particle>()
        repeat(8) {
            val angle = rng.nextFloat() * 6.28f
            val speed = 1f + rng.nextFloat() * 2f
            particles.add(
                Particle(
                    x = target.x.toFloat(), y = target.y.toFloat(),
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed,
                    life = 0.5f, maxLife = 0.5f,
                    color = 0xFFEF5350, size = 3f + rng.nextFloat() * 3f
                )
            )
        }

        val texts = mutableListOf<FloatingText>()
        texts.add(FloatingText("-$playerDmg", target.x.toFloat(), target.y.toFloat(), 1f, 0xFFFFAB40))

        var newState: GameState
        var msg: String

        if (newHp <= 0) {
            val xpGain = target.type.xpValue
            val scoreGain = target.type.xpValue * 2
            msg = "${target.type.symbol} ${target.type.name} 처치! +${xpGain}XP"
            texts.add(FloatingText("+${xpGain}xp", target.x.toFloat(), target.y - 0.5f, 1.2f, 0xFF4FC3F7))

            var player = state.player.copy(xp = state.player.xp + xpGain)
            val xpNeeded = GameState.xpForLevel(player.level)
            if (player.xp >= xpNeeded) {
                player = player.copy(
                    level = player.level + 1,
                    xp = player.xp - xpNeeded,
                    maxHp = player.maxHp + 3,
                    hp = min(player.hp + 5, player.maxHp + 3),
                    atk = player.atk + 1,
                )
                msg = "레벨 업! Lv.${player.level} ⚔${player.atk}"
                texts.add(FloatingText("LEVEL UP!", state.player.x.toFloat(), state.player.y - 0.5f, 1.5f, 0xFF4FC3F7))
            }

            newState = state.copy(
                player = player,
                enemies = state.enemies.filter { it.id != target.id },
                score = state.score + scoreGain,
                turnCount = state.turnCount + 1,
                particles = state.particles + particles,
                floatingTexts = state.floatingTexts + texts,
                message = msg,
            )
        } else {
            msg = "${target.type.symbol}에게 $playerDmg 데미지!"
            newState = state.copy(
                enemies = state.enemies.map {
                    if (it.id == target.id) it.copy(hp = newHp) else it
                },
                turnCount = state.turnCount + 1,
                particles = state.particles + particles,
                floatingTexts = state.floatingTexts + texts,
                message = msg,
            )
        }

        newState = wakeNearbyEnemies(newState)
        newState = processEnemyTurns(newState)
        newState = updateVisibility(newState)
        return newState
    }

    private fun processMove(state: GameState, nx: Int, ny: Int): GameState {
        var player = state.player.copy(x = nx, y = ny)
        var msg = state.message
        var score = state.score
        val texts = mutableListOf<FloatingText>()
        var items = state.items
        var nextId = state.nextId
        val particles = mutableListOf<Particle>()

        val itemAt = items.find { it.x == nx && it.y == ny }
        if (itemAt != null) {
            items = items.filter { it.id != itemAt.id }
            when (itemAt.type) {
                ItemType.POTION_SMALL -> {
                    val heal = min(3, player.maxHp - player.hp)
                    player = player.copy(hp = player.hp + heal)
                    msg = "작은 물약 +${heal}HP"
                    texts.add(FloatingText("+${heal}HP", nx.toFloat(), ny.toFloat(), 1f, 0xFFFF80AB))
                }
                ItemType.POTION_LARGE -> {
                    val heal = min(6, player.maxHp - player.hp)
                    player = player.copy(hp = player.hp + heal)
                    msg = "큰 물약 +${heal}HP"
                    texts.add(FloatingText("+${heal}HP", nx.toFloat(), ny.toFloat(), 1f, 0xFFFF80AB))
                }
                ItemType.WEAPON_DAGGER -> {
                    player = player.copy(atk = player.atk + 1); msg = "단검 획득! ⚔+1"
                    texts.add(FloatingText("⚔+1", nx.toFloat(), ny.toFloat(), 1f, 0xFFFFAB40))
                }
                ItemType.WEAPON_SWORD -> {
                    player = player.copy(atk = player.atk + 2); msg = "검 획득! ⚔+2"
                    texts.add(FloatingText("⚔+2", nx.toFloat(), ny.toFloat(), 1f, 0xFFFFAB40))
                }
                ItemType.WEAPON_AXE -> {
                    player = player.copy(atk = player.atk + 3); msg = "도끼 획득! ⚔+3"
                    texts.add(FloatingText("⚔+3", nx.toFloat(), ny.toFloat(), 1f, 0xFFFFAB40))
                }
                ItemType.SHIELD_WOOD -> {
                    player = player.copy(def = player.def + 1); msg = "나무 방패! 🛡+1"
                    texts.add(FloatingText("🛡+1", nx.toFloat(), ny.toFloat(), 1f, 0xFF26C6DA))
                }
                ItemType.SHIELD_IRON -> {
                    player = player.copy(def = player.def + 2); msg = "철 방패! 🛡+2"
                    texts.add(FloatingText("🛡+2", nx.toFloat(), ny.toFloat(), 1f, 0xFF26C6DA))
                }
                ItemType.GOLD_SMALL -> {
                    player = player.copy(gold = player.gold + 5); score += 5; msg = "금화 +5"
                    texts.add(FloatingText("+5G", nx.toFloat(), ny.toFloat(), 1f, 0xFFFFD54F))
                }
                ItemType.GOLD_LARGE -> {
                    player = player.copy(gold = player.gold + 15); score += 15; msg = "금화 +15"
                    texts.add(FloatingText("+15G", nx.toFloat(), ny.toFloat(), 1f, 0xFFFFD54F))
                }
                ItemType.SCROLL_REVEAL -> {
                    msg = "지도 공개!"
                    for (row in state.dungeon.revealed) row.fill(true)
                    texts.add(FloatingText("MAP!", nx.toFloat(), ny.toFloat(), 1f, 0xFF7E57C2))
                }
                ItemType.SCROLL_SMITE -> {
                    msg = "천벌의 두루마리!"
                    texts.add(FloatingText("SMITE!", nx.toFloat(), ny.toFloat(), 1.2f, 0xFFFFD54F))
                }
            }
            repeat(5) {
                val angle = Random.nextFloat() * 6.28f
                particles.add(
                    Particle(
                        nx.toFloat(), ny.toFloat(),
                        kotlin.math.cos(angle) * 1.5f, kotlin.math.sin(angle) * 1.5f,
                        0.4f, 0.4f, 0xFFFFD54F, 2f + Random.nextFloat() * 2f
                    )
                )
            }
        }

        if (state.dungeon.tileAt(nx, ny) == TileType.STAIRS_DOWN) {
            return descendFloor(state.copy(player = player, score = score, items = items, nextId = nextId))
        }

        var newState = state.copy(
            player = player,
            items = items,
            score = score,
            turnCount = state.turnCount + 1,
            particles = state.particles + particles,
            floatingTexts = state.floatingTexts + texts,
            message = msg,
            nextId = nextId,
        )

        if (itemAt?.type == ItemType.SCROLL_SMITE) {
            val visibleEnemies = newState.enemies.filter {
                newState.dungeon.visible[it.y][it.x]
            }
            var smiteState = newState
            for (e in visibleEnemies) {
                val dmg = 3 + newState.floor
                val newHp = e.hp - dmg
                smiteState = if (newHp <= 0) {
                    smiteState.copy(
                        enemies = smiteState.enemies.filter { it.id != e.id },
                        score = smiteState.score + e.type.xpValue * 2,
                        player = smiteState.player.copy(xp = smiteState.player.xp + e.type.xpValue),
                    )
                } else {
                    smiteState.copy(
                        enemies = smiteState.enemies.map { if (it.id == e.id) it.copy(hp = newHp) else it }
                    )
                }
            }
            newState = smiteState
        }

        newState = wakeNearbyEnemies(newState)
        newState = processEnemyTurns(newState)
        newState = updateVisibility(newState)
        return newState
    }

    private fun descendFloor(state: GameState): GameState {
        val newFloor = state.floor + 1
        val gen = DungeonGenerator.generate(newFloor)
        val player = state.player.copy(
            x = gen.playerX, y = gen.playerY,
            hp = min(state.player.hp + 3, state.player.maxHp)
        )
        val newState = GameState(
            player = player,
            dungeon = gen.dungeon,
            enemies = gen.enemies,
            items = gen.items,
            floor = newFloor,
            score = state.score + newFloor * 10,
            turnCount = state.turnCount,
            particles = emptyList(),
            floatingTexts = listOf(
                FloatingText("Floor $newFloor", player.x.toFloat(), player.y - 1f, 2f, 0xFF7E57C2)
            ),
            gameOver = false,
            message = "지하 ${newFloor}층에 도착했다.",
            nextId = gen.nextId,
        )
        return updateVisibility(newState)
    }

    private fun wakeNearbyEnemies(state: GameState): GameState {
        val px = state.player.x
        val py = state.player.y
        return state.copy(
            enemies = state.enemies.map { e ->
                if (e.asleep && abs(e.x - px) <= 4 && abs(e.y - py) <= 4 &&
                    state.dungeon.visible[e.y][e.x]
                ) {
                    e.copy(asleep = false)
                } else e
            }
        )
    }

    private fun processEnemyTurns(state: GameState): GameState {
        var currentState = state
        val rng = Random

        for (enemy in state.enemies) {
            if (enemy.asleep) continue
            val current = currentState.enemies.find { it.id == enemy.id } ?: continue
            if (current.hp <= 0) continue

            val px = currentState.player.x
            val py = currentState.player.y
            val dist = abs(current.x - px) + abs(current.y - py)

            if (dist == 1) {
                val dmg = max(1, current.atk - currentState.player.def + rng.nextInt(-1, 1))
                val newHp = currentState.player.hp - dmg
                val texts = listOf(
                    FloatingText("-$dmg", px.toFloat(), py.toFloat(), 0.8f, 0xFFEF5350)
                )
                val shake = min(dmg.toFloat() * 2f, 8f)

                if (newHp <= 0) {
                    return currentState.copy(
                        player = currentState.player.copy(hp = 0),
                        floatingTexts = currentState.floatingTexts + texts,
                        gameOver = true,
                        message = "${current.type.symbol}에게 쓰러졌다...",
                        shakeAmount = shake,
                    )
                }

                currentState = currentState.copy(
                    player = currentState.player.copy(hp = newHp),
                    floatingTexts = currentState.floatingTexts + texts,
                    shakeAmount = shake,
                )
            } else if (dist <= 6) {
                val dx = px.compareTo(current.x)
                val dy = py.compareTo(current.y)

                val moves = mutableListOf<Pair<Int, Int>>()
                if (dx != 0) moves.add(Pair(current.x + dx, current.y))
                if (dy != 0) moves.add(Pair(current.x, current.y + dy))
                if (rng.nextFloat() < 0.3f) {
                    moves.add(Pair(current.x + if (rng.nextBoolean()) 1 else -1, current.y))
                    moves.add(Pair(current.x, current.y + if (rng.nextBoolean()) 1 else -1))
                }

                val validMove = moves.firstOrNull { (mx, my) ->
                    currentState.dungeon.isWalkable(mx, my) &&
                    !(mx == px && my == py) &&
                    currentState.enemies.none { it.id != current.id && it.x == mx && it.y == my }
                }

                if (validMove != null) {
                    currentState = currentState.copy(
                        enemies = currentState.enemies.map {
                            if (it.id == current.id) it.copy(x = validMove.first, y = validMove.second)
                            else it
                        }
                    )
                }
            }
        }

        return currentState
    }

    fun updateVisibility(state: GameState): GameState {
        val d = state.dungeon
        for (row in d.visible) row.fill(false)

        val px = state.player.x
        val py = state.player.y
        val radius = 5

        for (angle in 0 until 360 step 2) {
            val rad = angle * 0.01745329f
            val dirX = kotlin.math.cos(rad)
            val dirY = kotlin.math.sin(rad)
            var cx = px + 0.5f
            var cy = py + 0.5f
            for (step in 0..radius) {
                val tx = cx.toInt()
                val ty = cy.toInt()
                if (tx < 0 || tx >= d.width || ty < 0 || ty >= d.height) break
                d.visible[ty][tx] = true
                d.revealed[ty][tx] = true
                if (d.tiles[ty][tx] == TileType.WALL) break
                cx += dirX * 0.5f
                cy += dirY * 0.5f
            }
        }

        return state
    }

    fun updateParticles(state: GameState, dt: Float): GameState {
        val newParticles = state.particles.mapNotNull { p ->
            val nl = p.life - dt
            if (nl <= 0f) null
            else p.copy(x = p.x + p.vx * dt, y = p.y + p.vy * dt, life = nl)
        }
        val newTexts = state.floatingTexts.mapNotNull { t ->
            val nl = t.life - dt
            if (nl <= 0f) null
            else t.copy(y = t.y - dt * 0.8f, life = nl)
        }
        val newShake = max(0f, state.shakeAmount - dt * 16f)
        return state.copy(particles = newParticles, floatingTexts = newTexts, shakeAmount = newShake)
    }
}
