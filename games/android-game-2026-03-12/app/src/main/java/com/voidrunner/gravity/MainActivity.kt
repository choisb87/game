package com.voidrunner.gravity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.voidrunner.gravity.game.GameScreen
import com.voidrunner.gravity.game.MenuScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var playing by remember { mutableStateOf(false) }
            Box(Modifier.fillMaxSize()) {
                if (playing) {
                    GameScreen(onBackToMenu = { playing = false })
                } else {
                    MenuScreen(onStartGame = { playing = true })
                }
            }
        }
    }
}
