package com.unmute.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unmute.app.data.AppSettings
import com.unmute.app.data.BoardRepository
import com.unmute.app.data.SettingsRepository
import com.unmute.app.data.local.GridProfileEntity
import com.unmute.app.domain.model.AppLanguage
import com.unmute.app.domain.model.CardFontSize
import com.unmute.app.domain.model.resolveLanguage
import com.unmute.app.tts.TtsManager
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val boardRepository: BoardRepository,
    private val ttsManager: TtsManager,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val gridProfiles: StateFlow<List<GridProfileEntity>> = boardRepository.observeGridProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectGridProfile(id: Long) {
        viewModelScope.launch { settingsRepository.setActiveGridProfile(id) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { settingsRepository.setLanguage(language) }
    }

    fun addCustomProfile(base: GridProfileEntity, name: String, columns: Int) {
        viewModelScope.launch {
            boardRepository.insertGridProfile(
                name = name.ifBlank { DEFAULT_CUSTOM_NAME },
                columns = columns.coerceIn(MIN_COLUMNS, MAX_COLUMNS),
            )
        }
    }

    fun updateProfile(id: Long, name: String, columns: Int) {
        viewModelScope.launch {
            boardRepository.updateGridProfile(
                id = id,
                name = name.ifBlank { DEFAULT_CUSTOM_NAME },
                columns = columns.coerceIn(MIN_COLUMNS, MAX_COLUMNS),
            )
        }
    }

    fun deleteProfile(id: Long) {
        viewModelScope.launch {
            val activeId = settings.value.activeGridProfileId
            boardRepository.deleteGridProfile(id)
            if (activeId == id) {
                settingsRepository.setActiveGridProfile(BoardRepository.BIG_PROFILE_ID)
            }
        }
    }

    fun setAudioOutput(outputId: String) {
        viewModelScope.launch { settingsRepository.setAudioOutput(outputId) }
    }

    fun availableOutputs(): List<Pair<String, String>> = ttsManager.availableOutputs()

    fun setAutospeak(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutospeak(enabled) }
    }

    fun setCardFontSize(size: CardFontSize) {
        viewModelScope.launch { settingsRepository.setCardFontSize(size) }
    }

    fun setSpeechRate(rate: Float) {
        viewModelScope.launch { settingsRepository.setSpeechRate(rate) }
    }

    fun setSpeechPitch(pitch: Float) {
        viewModelScope.launch { settingsRepository.setSpeechPitch(pitch) }
    }

    fun testSpeech() {
        val s = settings.value
        val lang = resolveLanguage(s.language, Locale.getDefault().language)
        viewModelScope.launch {
            ttsManager.speak(
                text = TEST_TEXT[lang] ?: TEST_TEXT.getValue("en"),
                language = lang,
                outputId = s.audioOutput,
                rate = s.speechRate,
                pitch = s.speechPitch,
            )
        }
    }

    companion object {
        const val DEFAULT_CUSTOM_NAME = "Custom"
        const val MIN_COLUMNS = 1
        const val MAX_COLUMNS = 10

        val TEST_TEXT = mapOf(
            "en" to "Hello! This is how Unmute will speak.",
            "es" to "¡Hola! Así hablará Unmute.",
        )
    }
}
