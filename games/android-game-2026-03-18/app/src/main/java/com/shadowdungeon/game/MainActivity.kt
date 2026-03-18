package com.shadowdungeon.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.shadowdungeon.game.game.GameScreen
import com.shadowdungeon.game.game.MenuScreen

enum class Screen { MENU, PLAYING, GAME_OVER }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val prefs = remember {
                context.getSharedPreferences("shadow_dungeon", MODE_PRIVATE)
            }
            var screen by remember { mutableStateOf(Screen.MENU) }
            var lastScore by remember { mutableIntStateOf(0) }
            var lastFloor by remember { mutableIntStateOf(0) }

            when (screen) {
                Screen.MENU -> MenuScreen(
                    prefs = prefs,
                    onStartGame = { screen = Screen.PLAYING }
                )
                Screen.PLAYING -> GameScreen(
                    prefs = prefs,
                    onGameOver = { score, floor ->
                        lastScore = score
                        lastFloor = floor
                        val best = prefs.getInt("best_score", 0)
                        val bestFloor = prefs.getInt("best_floor", 0)
                        if (score > best) prefs.edit().putInt("best_score", score).apply()
                        if (floor > bestFloor) prefs.edit().putInt("best_floor", floor).apply()
                        val runs = prefs.getInt("total_runs", 0)
                        prefs.edit().putInt("total_runs", runs + 1).apply()
                        screen = Screen.GAME_OVER
                    }
                )
                Screen.GAME_OVER -> MenuScreen(
                    prefs = prefs,
                    showGameOver = true,
                    lastScore = lastScore,
                    lastFloor = lastFloor,
                    onStartGame = { screen = Screen.PLAYING }
                )
            }
        }
    }
}
