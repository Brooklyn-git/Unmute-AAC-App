package com.unmute.app

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unmute.app.domain.model.AppLanguage
import com.unmute.app.ui.AppViewModelFactory
import com.unmute.app.ui.board.BoardScreen
import com.unmute.app.ui.board.BoardViewModel
import com.unmute.app.ui.settings.AboutScreen
import com.unmute.app.ui.settings.SettingsScreen
import com.unmute.app.ui.theme.UnmuteTheme
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private var appliedLanguage: AppLanguage = AppLanguage.SYSTEM

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(withAppLanguage(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UnmuteTheme {
                val container = (application as UnmuteApplication).container
                val languageFlow = remember {
                    container.settingsRepository.settings.mapLatest { it.language }
                }
                val language by languageFlow.collectAsStateWithLifecycle(initialValue = appliedLanguage)

                LaunchedEffect(language) {
                    if (language != appliedLanguage) {
                        recreate()
                    }
                }

                AppNavigation()
            }
        }
    }

    private fun withAppLanguage(base: Context): Context {
        val container = (base.applicationContext as? UnmuteApplication)?.container ?: return base
        val selected = runCatching {
            runBlocking { container.settingsRepository.settings.first().language }
        }.getOrDefault(AppLanguage.SYSTEM)
        appliedLanguage = selected
        val deviceLocale = base.resources.configuration.locales
            .run { if (isEmpty) Locale.getDefault() else this[0] }
        val locale = when (selected) {
            AppLanguage.SYSTEM -> deviceLocale
            AppLanguage.EN -> Locale.ENGLISH
            AppLanguage.ES -> Locale("es")
        }
        Locale.setDefault(locale)
        if (selected == AppLanguage.SYSTEM) return base
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}

private enum class AppScreen { Board, Settings, About }

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
            onOpenAbout = { screen = AppScreen.About },
        )

        AppScreen.About -> AboutScreen(onBack = { screen = AppScreen.Settings })
    }
}
