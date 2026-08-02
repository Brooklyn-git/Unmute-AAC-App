package com.unmute.app.data

import android.content.Context
import com.unmute.app.data.local.UnmuteDatabase
import com.unmute.app.tts.TtsManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class AppContainer(context: Context) {

    private val database = UnmuteDatabase.build(context)

    val boardRepository = BoardRepository(
        boardDao = database.boardDao(),
        categoryDao = database.categoryDao(),
        cardDao = database.cardDao(),
        gridProfileDao = database.gridProfileDao(),
    )

    val settingsRepository = SettingsRepository(context)

    val ttsManager = TtsManager(
        context,
        initialEngine = runBlocking { settingsRepository.settings.first().ttsEngine },
    )
}
