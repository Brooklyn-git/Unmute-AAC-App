package com.unmute.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.unmute.app.UnmuteApplication
import com.unmute.app.data.AppContainer
import com.unmute.app.ui.board.BoardViewModel
import com.unmute.app.ui.settings.SettingsViewModel

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(BoardViewModel::class.java) ->
            BoardViewModel(
                container.boardRepository,
                container.settingsRepository,
                container.ttsManager,
            ) as T

        modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
            SettingsViewModel(
                container.settingsRepository,
                container.ttsManager,
            ) as T

        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }

    companion object {
        fun from(application: UnmuteApplication): AppViewModelFactory =
            AppViewModelFactory(application.container)
    }
}
