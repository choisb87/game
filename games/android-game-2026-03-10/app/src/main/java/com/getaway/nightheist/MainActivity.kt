package com.getaway.nightheist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.getaway.nightheist.game.MenuScreen
import com.getaway.nightheist.game.GameScreen
import com.getaway.nightheist.game.ScreenState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var screenState by remember { mutableStateOf(ScreenState.MENU) }
            val context = LocalContext.current

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0D1117))
            ) {
                when (screenState) {
                    ScreenState.MENU -> {
                        MenuScreen(
                            onStartGame = { screenState = ScreenState.PLAYING },
                            context = context
                        )
                    }
                    ScreenState.PLAYING -> {
                        GameScreen(
                            onBackToMenu = { screenState = ScreenState.MENU },
                            context = context
                        )
                    }
                    ScreenState.RESULT -> {
                        // Results handled within GameScreen overlay
                        screenState = ScreenState.MENU
                    }
                }
            }
        }
    }
}
