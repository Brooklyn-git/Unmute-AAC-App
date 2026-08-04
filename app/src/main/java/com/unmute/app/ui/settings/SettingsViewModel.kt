package com.unmute.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.unmute.app.R
import com.unmute.app.data.AppSettings
import com.unmute.app.data.SettingsRepository
import com.unmute.app.data.backup.BackupManager
import com.unmute.app.domain.model.AppLanguage
import com.unmute.app.domain.model.SectionLayout
import com.unmute.app.domain.model.resolveLanguage
import com.unmute.app.tts.TtsIssue
import com.unmute.app.tts.TtsManager
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val ttsManager: TtsManager,
    private val backupManager: BackupManager,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val ttsErrors: SharedFlow<TtsIssue> = ttsManager.errors

    private val _backupEvents = MutableSharedFlow<BackupEvent>()
    val backupEvents: SharedFlow<BackupEvent> = _backupEvents

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            runCatching { backupManager.exportTo(uri) }
                .onSuccess { _backupEvents.emit(BackupEvent.Message(R.string.data_exported)) }
                .onFailure { _backupEvents.emit(BackupEvent.Message(R.string.export_failed)) }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            runCatching { backupManager.importFrom(uri) }
                .onSuccess { _backupEvents.emit(BackupEvent.Message(R.string.data_imported)) }
                .onFailure { _backupEvents.emit(BackupEvent.Message(R.string.import_failed)) }
        }
    }

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

    fun setSectionLayout(layout: SectionLayout) {
        viewModelScope.launch { settingsRepository.setSectionLayout(layout) }
    }

    fun setSpeakSectionNames(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSpeakSectionNames(enabled) }
    }

    fun setShowSectionSymbols(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShowSectionSymbols(enabled) }
    }

    fun setSecureMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSecureMode(enabled) }
    }

    fun setSecureTapCount(count: Int) {
        viewModelScope.launch { settingsRepository.setSecureTapCount(count) }
    }

    fun setSecureResetSeconds(seconds: Int) {
        viewModelScope.launch { settingsRepository.setSecureResetSeconds(seconds) }
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

sealed interface BackupEvent {
    data class Message(val resId: Int) : BackupEvent
}
