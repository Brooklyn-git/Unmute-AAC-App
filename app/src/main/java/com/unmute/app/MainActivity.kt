package com.unmute.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unmute.app.ui.AppViewModelFactory
import com.unmute.app.ui.board.BoardScreen
import com.unmute.app.ui.board.BoardViewModel
import com.unmute.app.ui.settings.SettingsScreen
import com.unmute.app.ui.theme.UnmuteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UnmuteTheme {
                AppNavigation()
            }
        }
    }
}

private enum class AppScreen { Board, Settings }

@Composable
private fun AppNavigation() {
    val context = LocalContext.current
    val factory = remember {
        AppViewModelFactory.from(context.applicationContext as UnmuteApplication)
    }
    var screen by rememberSaveable { mutableStateOf<AppScreen>(AppScreen.Board) }

    when (screen) {
        AppScreen.Board -> {
            val boardViewModel: BoardViewModel = viewModel(factory = factory)
            BoardScreen(
                viewModel = boardViewModel,
                onOpenSettings = { screen = AppScreen.Settings },
                onCardClick = boardViewModel::onCardClick,
            )
        }

        AppScreen.Settings -> SettingsScreen(
            viewModel = viewModel(factory = factory),
            onBack = { screen = AppScreen.Board },
        )
    }
}
