package com.flamedash.runner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.flamedash.runner.game.GameScreen
import com.flamedash.runner.game.MenuScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var playing by remember { mutableStateOf(false) }

            Box(modifier = Modifier.fillMaxSize()) {
                if (playing) {
                    GameScreen(onBackToMenu = { playing = false })
                } else {
                    MenuScreen(onStartGame = { playing = true })
                }
            }
        }
    }
}
