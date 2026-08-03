package com.unmute.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unmute.app.data.AppSettings
import com.unmute.app.data.SettingsRepository
import com.unmute.app.domain.model.AppLanguage
import com.unmute.app.domain.model.resolveLanguage
import com.unmute.app.tts.TtsIssue
import com.unmute.app.tts.TtsManager
import java.util.Locale
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val ttsManager: TtsManager,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val ttsErrors: SharedFlow<TtsIssue> = ttsManager.errors

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { settingsRepository.setLanguage(language) }
    }

    fun setAudioOutput(outputId: String) {
        viewModelScope.launch { settingsRepository.setAudioOutput(outputId) }
    }

    fun availableOutputs(): List<Pair<String, String>> = ttsManager.availableOutputs()

    fun ttsEngineLabel(): String? = ttsManager.currentEngineLabel()

    fun availableTtsEngines(): List<Pair<String, String>> = ttsManager.engines()

    fun selectTtsEngine(packageName: String?) {
        viewModelScope.launch {
            settingsRepository.setTtsEngine(packageName)
            ttsManager.selectEngine(packageName)
        }
    }

    fun setAutospeak(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutospeak(enabled) }
    }

    fun setSecureMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSecureMode(enabled) }
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
        val TEST_TEXT = mapOf(
            "en" to "Hello! This is how Unmute will speak.",
            "es" to "¡Hola! Así hablará Unmute.",
        )
    }
}
