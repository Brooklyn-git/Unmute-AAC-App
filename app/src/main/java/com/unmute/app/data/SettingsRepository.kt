package com.unmute.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.unmute.app.domain.model.AppLanguage
import com.unmute.app.domain.model.AudioOutputIds
import com.unmute.app.domain.model.CardFontSize
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

    private companion object {
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_GRID_PROFILE = longPreferencesKey("active_grid_profile")
        val KEY_AUDIO_OUTPUT = stringPreferencesKey("audio_output")
        val KEY_TTS_ENGINE = stringPreferencesKey("tts_engine")
        val KEY_AUTOSPEAK = booleanPreferencesKey("autospeak")
        val KEY_SPEAK_ON_ADD = booleanPreferencesKey("speak_on_add")
        val KEY_SPEECH_RATE = floatPreferencesKey("speech_rate")
        val KEY_SPEECH_PITCH = floatPreferencesKey("speech_pitch")
        val KEY_CARD_FONT_SIZE = stringPreferencesKey("card_font_size")
    }
}
