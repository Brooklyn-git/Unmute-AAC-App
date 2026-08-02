package com.unmute.app.ui.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unmute.app.data.AppSettings
import com.unmute.app.data.BoardRepository
import com.unmute.app.data.SettingsRepository
import com.unmute.app.data.local.BoardEntity
import com.unmute.app.data.local.CardEntity
import com.unmute.app.data.local.CategoryEntity
import com.unmute.app.data.local.GridProfileEntity
import com.unmute.app.domain.model.ImageType
import com.unmute.app.domain.model.resolveLanguage
import com.unmute.app.tts.TtsIssue
import com.unmute.app.tts.TtsManager
import com.unmute.app.util.PhotoStore
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BoardViewModel(
    private val boardRepository: BoardRepository,
    private val settingsRepository: SettingsRepository,
    private val ttsManager: TtsManager,
) : ViewModel() {

    val board: StateFlow<BoardEntity?> = boardRepository.observeBoards()
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val categories: StateFlow<List<CategoryEntity>> = board
        .flatMapLatest { b ->
            if (b == null) flowOf(emptyList()) else boardRepository.observeCategories(b.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    val cards: StateFlow<List<CardEntity>> = _selectedCategoryId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else boardRepository.observeCards(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allCards: StateFlow<List<CardEntity>> = board
        .flatMapLatest { b ->
            if (b == null) flowOf(emptyList()) else boardRepository.observeAllCards(b.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val gridProfiles: StateFlow<List<GridProfileEntity>> = boardRepository.observeGridProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val language: StateFlow<String> = settings
        .map { resolveLanguage(it.language, Locale.getDefault().language) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "en")

    val activeColumns: StateFlow<Int> = combine(settings, gridProfiles) { s, profiles ->
        profiles.firstOrNull { it.id == s.activeGridProfileId }?.columns ?: DEFAULT_COLUMNS
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_COLUMNS)

    fun selectCategory(id: Long) {
        _selectedCategoryId.value = id
    }

    private val _sentence = MutableStateFlow<List<String>>(emptyList())
    val sentence: StateFlow<List<String>> = _sentence.asStateFlow()

    private val _editMode = MutableStateFlow(false)
    val editMode: StateFlow<Boolean> = _editMode.asStateFlow()

    val ttsErrors: SharedFlow<TtsIssue> = ttsManager.errors

    fun toggleEditMode() {
        _editMode.value = !_editMode.value
    }

    fun onCardClick(card: CardEntity) {
        val phrase = cardPhrase(card)
        _sentence.update { it + phrase }
        if (settings.value.autospeak) speak(phrase)
    }

    fun speakSentence() {
        val text = _sentence.value.joinToString(" ")
        if (text.isBlank()) return
        speak(text)
        _sentence.value = emptyList()
    }

    fun removeLastWord() {
        _sentence.update { if (it.isEmpty()) it else it.dropLast(1) }
    }

    fun clearSentence() {
        _sentence.value = emptyList()
    }

    fun addTextToSentence(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        _sentence.update { it + trimmed }
    }

    fun speakText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        speak(trimmed)
    }

    /** Vocabulary words (labels in the active language) that start with [prefix]. */
    fun predictWords(prefix: String, limit: Int = PREDICTION_LIMIT): List<String> {
        val query = prefix.trim().lowercase()
        if (query.isEmpty()) return emptyList()
        val isSpanish = language.value == "es"
        return allCards.value
            .asSequence()
            .map { if (isSpanish) it.labelEs else it.labelEn }
            .map { it.lowercase() }
            .distinct()
            .filter { it.startsWith(query) && it != query }
            .take(limit)
            .toList()
    }

    /** Inserts [card] if new, otherwise updates it. */
    fun saveCard(card: CardEntity) {
        viewModelScope.launch {
            if (card.id == 0L) {
                val categoryId = card.categoryId
                val orderIndex = cards.value.size
                boardRepository.insertCard(card.copy(categoryId = categoryId, orderIndex = orderIndex))
            } else {
                boardRepository.updateCard(card)
            }
        }
    }

    fun deleteCard(card: CardEntity) {
        viewModelScope.launch {
            boardRepository.deleteCard(card)
            if (card.imageType == ImageType.PHOTO) {
                PhotoStore.delete(card.imageValue)
            }
        }
    }

    private fun cardPhrase(card: CardEntity): String =
        if (language.value == "es") card.phraseEs else card.phraseEn

    private fun speak(text: String) {
        val s = settings.value
        viewModelScope.launch {
            ttsManager.speak(
                text = text,
                language = language.value,
                outputId = s.audioOutput,
                rate = s.speechRate,
                pitch = s.speechPitch,
            )
        }
    }

    companion object {
        const val DEFAULT_COLUMNS = 3
        const val PREDICTION_LIMIT = 6
    }
}
