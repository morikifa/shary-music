package com.example.sharedmusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import com.example.sharedmusic.ui.navigation.SharyNavigation
import com.example.sharedmusic.viewmodel.AppViewModel
import com.example.sharedmusic.viewmodel.PlayerViewModel
import com.example.sharedmusic.viewmodel.RoomViewModel

/**
 * Единственная Activity приложения.
 * ViewModel создаются здесь и переживают смену конфигурации.
 */
class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()
    private val roomViewModel: RoomViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SharyNavigation(
                appViewModel = appViewModel,
                playerViewModel = playerViewModel,
                roomViewModel = roomViewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            )
        }
    }
}
