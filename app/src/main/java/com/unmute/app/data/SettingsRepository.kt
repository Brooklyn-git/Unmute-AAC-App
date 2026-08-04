package com.unmute.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.unmute.app.domain.model.AppLanguage
import com.unmute.app.domain.model.AudioOutputIds
import com.unmute.app.domain.model.CardFontSize
import com.unmute.app.domain.model.SectionLayout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val activeGridProfileId: Long = BoardRepository.BIG_PROFILE_ID,
    val audioOutput: String = AudioOutputIds.AUTO,
    val ttsEngine: String? = null,
    val autospeak: Boolean = false,
    val speakOnAdd: Boolean = true,
    val speechRate: Float = 1f,
    val speechPitch: Float = 1f,
    val cardFontSize: CardFontSize = CardFontSize.NORMAL,
    val secureMode: Boolean = false,
    val secureTapCount: Int = SettingsRepository.DEFAULT_SECURE_TAPS,
    val secureResetSeconds: Int = SettingsRepository.DEFAULT_SECURE_RESET_SECONDS,
    val sectionLayout: SectionLayout = SectionLayout.TABS,
    val speakSectionNames: Boolean = false,
    val showSectionSymbols: Boolean = true,
)

class SettingsRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            language = prefs[KEY_LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
                ?: AppLanguage.SYSTEM,
            activeGridProfileId = prefs[KEY_GRID_PROFILE] ?: BoardRepository.BIG_PROFILE_ID,
            audioOutput = prefs[KEY_AUDIO_OUTPUT] ?: AudioOutputIds.AUTO,
            ttsEngine = prefs[KEY_TTS_ENGINE],
            autospeak = prefs[KEY_AUTOSPEAK] ?: false,
            speakOnAdd = prefs[KEY_SPEAK_ON_ADD] ?: true,
            speechRate = prefs[KEY_SPEECH_RATE] ?: 1f,
            speechPitch = prefs[KEY_SPEECH_PITCH] ?: 1f,
            cardFontSize = prefs[KEY_CARD_FONT_SIZE]
                ?.let { runCatching { CardFontSize.valueOf(it) }.getOrNull() }
                ?: CardFontSize.NORMAL,
            secureMode = prefs[KEY_SECURE_MODE] ?: false,
            secureTapCount = (prefs[KEY_SECURE_TAP_COUNT] ?: DEFAULT_SECURE_TAPS)
                .coerceIn(MIN_SECURE_TAPS, MAX_SECURE_TAPS),
            secureResetSeconds = (prefs[KEY_SECURE_RESET_SECONDS] ?: DEFAULT_SECURE_RESET_SECONDS)
                .coerceIn(MIN_SECURE_RESET_SECONDS, MAX_SECURE_RESET_SECONDS),
            sectionLayout = prefs[KEY_SECTION_LAYOUT]
                ?.let { runCatching { SectionLayout.valueOf(it) }.getOrNull() }
                ?: SectionLayout.TABS,
            speakSectionNames = prefs[KEY_SPEAK_SECTION_NAMES] ?: false,
            showSectionSymbols = prefs[KEY_SHOW_SECTION_SYMBOLS] ?: true,
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[KEY_LANGUAGE] = language.name }
    }

    suspend fun setActiveGridProfile(id: Long) {
        context.dataStore.edit { it[KEY_GRID_PROFILE] = id }
    }

    suspend fun setAudioOutput(outputId: String) {
        context.dataStore.edit { it[KEY_AUDIO_OUTPUT] = outputId }
    }

    suspend fun setTtsEngine(packageName: String?) {
        context.dataStore.edit { prefs ->
            if (packageName == null) {
                prefs.remove(KEY_TTS_ENGINE)
            } else {
                prefs[KEY_TTS_ENGINE] = packageName
            }
        }
    }

    suspend fun setAutospeak(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTOSPEAK] = enabled }
    }

    suspend fun setSpeakOnAdd(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SPEAK_ON_ADD] = enabled }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.dataStore.edit { it[KEY_SPEECH_RATE] = rate }
    }

    suspend fun setSpeechPitch(pitch: Float) {
        context.dataStore.edit { it[KEY_SPEECH_PITCH] = pitch }
    }

    suspend fun setCardFontSize(size: CardFontSize) {
        context.dataStore.edit { it[KEY_CARD_FONT_SIZE] = size.name }
    }

    suspend fun setSecureMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SECURE_MODE] = enabled }
    }

    suspend fun setSecureTapCount(count: Int) {
        val value = count.coerceIn(MIN_SECURE_TAPS, MAX_SECURE_TAPS)
        context.dataStore.edit { it[KEY_SECURE_TAP_COUNT] = value }
    }

    suspend fun setSecureResetSeconds(seconds: Int) {
        val value = seconds.coerceIn(MIN_SECURE_RESET_SECONDS, MAX_SECURE_RESET_SECONDS)
        context.dataStore.edit { it[KEY_SECURE_RESET_SECONDS] = value }
    }

    suspend fun setSectionLayout(layout: SectionLayout) {
        context.dataStore.edit { it[KEY_SECTION_LAYOUT] = layout.name }
    }

    suspend fun setSpeakSectionNames(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SPEAK_SECTION_NAMES] = enabled }
    }

    suspend fun setShowSectionSymbols(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_SECTION_SYMBOLS] = enabled }
    }

    /** Overwrites every stored setting with [appSettings]. */
    suspend fun restore(appSettings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = appSettings.language.name
            prefs[KEY_GRID_PROFILE] = appSettings.activeGridProfileId
            prefs[KEY_AUDIO_OUTPUT] = appSettings.audioOutput
            if (appSettings.ttsEngine == null) {
                prefs.remove(KEY_TTS_ENGINE)
            } else {
                prefs[KEY_TTS_ENGINE] = appSettings.ttsEngine
            }
            prefs[KEY_AUTOSPEAK] = appSettings.autospeak
            prefs[KEY_SPEAK_ON_ADD] = appSettings.speakOnAdd
            prefs[KEY_SPEECH_RATE] = appSettings.speechRate
            prefs[KEY_SPEECH_PITCH] = appSettings.speechPitch
            prefs[KEY_CARD_FONT_SIZE] = appSettings.cardFontSize.name
            prefs[KEY_SECURE_MODE] = appSettings.secureMode
            prefs[KEY_SECURE_TAP_COUNT] = appSettings.secureTapCount
            prefs[KEY_SECURE_RESET_SECONDS] = appSettings.secureResetSeconds
            prefs[KEY_SECTION_LAYOUT] = appSettings.sectionLayout.name
            prefs[KEY_SPEAK_SECTION_NAMES] = appSettings.speakSectionNames
            prefs[KEY_SHOW_SECTION_SYMBOLS] = appSettings.showSectionSymbols
        }
    }

    companion object {
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_GRID_PROFILE = longPreferencesKey("active_grid_profile")
        val KEY_AUDIO_OUTPUT = stringPreferencesKey("audio_output")
        val KEY_TTS_ENGINE = stringPreferencesKey("tts_engine")
        val KEY_AUTOSPEAK = booleanPreferencesKey("autospeak")
        val KEY_SPEAK_ON_ADD = booleanPreferencesKey("speak_on_add")
        val KEY_SPEECH_RATE = floatPreferencesKey("speech_rate")
        val KEY_SPEECH_PITCH = floatPreferencesKey("speech_pitch")
        val KEY_CARD_FONT_SIZE = stringPreferencesKey("card_font_size")
        val KEY_SECURE_MODE = booleanPreferencesKey("secure_mode")
        val KEY_SECURE_TAP_COUNT = intPreferencesKey("secure_tap_count")
        val KEY_SECURE_RESET_SECONDS = intPreferencesKey("secure_reset_seconds")
        val KEY_SECTION_LAYOUT = stringPreferencesKey("section_layout")
        val KEY_SPEAK_SECTION_NAMES = booleanPreferencesKey("speak_section_names")
        val KEY_SHOW_SECTION_SYMBOLS = booleanPreferencesKey("show_section_symbols")

        const val DEFAULT_SECURE_TAPS = 3
        const val MIN_SECURE_TAPS = 1
        const val MAX_SECURE_TAPS = 10
        const val DEFAULT_SECURE_RESET_SECONDS = 2
        const val MIN_SECURE_RESET_SECONDS = 1
        const val MAX_SECURE_RESET_SECONDS = 10
    }
}
