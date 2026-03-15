package com.chainreactor.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.chainreactor.game.game.MenuScreen
import com.chainreactor.game.game.GameScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var playing by remember { mutableStateOf(false) }
            if (playing) {
                GameScreen(onExit = { playing = false })
            } else {
                MenuScreen(onStart = { playing = true })
            }
        }
    }
}
