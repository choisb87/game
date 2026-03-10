package com.gravitypulse.game

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.gravitypulse.game.game.GameScreen
import com.gravitypulse.game.game.MenuScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            var screen by remember { mutableStateOf<Screen>(Screen.Menu) }
            var lastScore by remember { mutableStateOf(0) }
            var highScore by remember { mutableStateOf(loadHighScore()) }

            when (val current = screen) {
                is Screen.Menu -> {
                    MenuScreen(
                        highScore = highScore,
                        lastScore = lastScore,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0A0014)),
                        onStartGame = { screen = Screen.Playing }
                    )
                }

                is Screen.Playing -> {
                    GameScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0A0014)),
                        onGameOver = { score ->
                            lastScore = score
                            if (score > highScore) {
                                highScore = score
                                saveHighScore(score)
                            }
                            screen = Screen.Menu
                        }
                    )
                }
            }
        }
    }

    private fun loadHighScore(): Int {
        val prefs = getSharedPreferences("gravity_pulse", MODE_PRIVATE)
        return prefs.getInt("high_score", 0)
    }

    private fun saveHighScore(score: Int) {
        val prefs = getSharedPreferences("gravity_pulse", MODE_PRIVATE)
        prefs.edit().putInt("high_score", score).apply()
    }
}

sealed class Screen {
    data object Menu : Screen()
    data object Playing : Screen()
}
