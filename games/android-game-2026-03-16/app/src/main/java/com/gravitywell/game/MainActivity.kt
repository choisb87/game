package com.gravitywell.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.gravitywell.game.game.MenuScreen
import com.gravitywell.game.game.GameScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var showGame by remember { mutableStateOf(false) }

            if (showGame) {
                GameScreen(
                    onBack = { showGame = false }
                )
            } else {
                MenuScreen(
                    onPlay = { showGame = true }
                )
            }
        }
    }
}
