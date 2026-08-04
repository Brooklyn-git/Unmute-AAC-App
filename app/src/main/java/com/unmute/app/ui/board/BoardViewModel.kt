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
import com.unmute.app.domain.model.CardFontSize
import com.unmute.app.domain.model.ImageType
import com.unmute.app.domain.model.SectionLayout
import com.unmute.app.domain.model.SentenceToken
import com.unmute.app.domain.model.label
import com.unmute.app.domain.model.moved
import com.unmute.app.domain.model.resolveLanguage
import com.unmute.app.domain.model.toSentenceText
import com.unmute.app.domain.model.withTextAfter
import com.unmute.app.domain.model.withTextAt
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
        val previous = _selectedCategoryId.value
        _selectedCategoryId.value = id
        if (settings.value.speakSectionNames && previous != id) {
            val name = categories.value
                .firstOrNull { it.id == id }
                ?.let { categoryName(it) }
                ?: return
            speak(name)
        }
    }

    /** Navigates to a section from a shortcut card, always speaking the section name. */
    fun navigateToSection(id: Long) {
        _selectedCategoryId.value = id
        val name = categories.value
            .firstOrNull { it.id == id }
            ?.let { categoryName(it) }
            ?: return
        speak(name)
    }

    fun selectGridProfile(id: Long) {
        viewModelScope.launch { settingsRepository.setActiveGridProfile(id) }
    }

    fun addGridProfile(name: String, columns: Int) {
        viewModelScope.launch {
            boardRepository.insertGridProfile(
                name = name.ifBlank { BoardRepository.DEFAULT_CUSTOM_NAME },
                columns = columns.coerceIn(BoardRepository.MIN_COLUMNS, BoardRepository.MAX_COLUMNS),
            )
        }
    }

    fun updateGridProfile(id: Long, name: String, columns: Int) {
        viewModelScope.launch {
            boardRepository.updateGridProfile(
                id = id,
                name = name.ifBlank { BoardRepository.DEFAULT_CUSTOM_NAME },
                columns = columns.coerceIn(BoardRepository.MIN_COLUMNS, BoardRepository.MAX_COLUMNS),
            )
        }
    }

    fun deleteGridProfile(id: Long) {
        viewModelScope.launch {
            val activeId = settings.value.activeGridProfileId
            boardRepository.deleteGridProfile(id)
            if (activeId == id) {
                settingsRepository.setActiveGridProfile(BoardRepository.BIG_PROFILE_ID)
            }
        }
    }

    fun setCardFontSize(size: CardFontSize) {
        viewModelScope.launch { settingsRepository.setCardFontSize(size) }
    }

    fun setSectionLayout(layout: SectionLayout) {
        viewModelScope.launch { settingsRepository.setSectionLayout(layout) }
    }

    fun setSpeakSectionNames(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSpeakSectionNames(enabled) }
    }

    /** Adds [delta] columns to the active grid profile, creating a custom copy if it is a preset. */
    fun adjustActiveColumns(delta: Int) {
        viewModelScope.launch {
            val activeId = settings.value.activeGridProfileId
            val active = gridProfiles.value.firstOrNull { it.id == activeId }
            val current = active?.columns ?: DEFAULT_COLUMNS
            val newColumns = (current + delta).coerceIn(BoardRepository.MIN_COLUMNS, BoardRepository.MAX_COLUMNS)
            if (newColumns == current) return@launch
            val targetId = if (active != null && !active.isPreset) {
                active.id
            } else {
                val newId = boardRepository.insertGridProfile(
                    name = BoardRepository.DEFAULT_CUSTOM_NAME,
                    columns = current,
                )
                settingsRepository.setActiveGridProfile(newId)
                newId
            }
            boardRepository.updateGridProfile(
                id = targetId,
                name = active?.name ?: BoardRepository.DEFAULT_CUSTOM_NAME,
                columns = newColumns,
            )
        }
    }

    private val _sentence = MutableStateFlow<List<SentenceToken>>(emptyList())
    val sentence: StateFlow<List<SentenceToken>> = _sentence.asStateFlow()

    val sentenceText: StateFlow<String> = _sentence
        .map { it.toSentenceText() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val _editMode = MutableStateFlow(false)
    val editMode: StateFlow<Boolean> = _editMode.asStateFlow()

    val ttsErrors: SharedFlow<TtsIssue> = ttsManager.errors

    fun toggleEditMode() {
        _editMode.value = !_editMode.value
    }

    fun setEditMode(enabled: Boolean) {
        _editMode.value = enabled
    }

    fun onCardClick(card: CardEntity) {
        val phrase = cardPhrase(card)
        _sentence.update { it + card.toSentenceToken(phrase) }
        if (settings.value.autospeak) speak(phrase)
    }

    fun speakSentence() {
        val text = _sentence.value.toSentenceText()
        if (text.isBlank()) return
        speak(text)
        _sentence.value = emptyList()
    }

    fun clearSentence() {
        _sentence.value = emptyList()
    }

    /** Replaces the sentence with freely edited text. */
    fun setSentence(text: String) {
        _sentence.value = if (text.isEmpty()) emptyList() else listOf(SentenceToken.Text(text))
    }

    /** Inserts or updates typed text after [afterIndex] (null = end of the sentence). */
    fun insertTextAt(afterIndex: Int?, text: String) {
        _sentence.update { it.withTextAfter(afterIndex, text) }
    }

    /** Removes the card token at [index] (card bar mode). */
    fun removeTokenAt(index: Int) {
        _sentence.update { tokens -> tokens.filterIndexed { i, _ -> i != index } }
    }

    /** Removes the last card token, keeping any typed text (card bar mode). */
    fun removeLastCard() {
        _sentence.update { tokens ->
            val lastCard = tokens.indexOfLast { it is SentenceToken.Card }
            if (lastCard == -1) tokens else tokens.filterIndexed { i, _ -> i != lastCard }
        }
    }

    /** Replaces the typed text at [index], removing that token when it becomes empty (card bar mode). */
    fun updateTextAt(index: Int, text: String) {
        _sentence.update { it.withTextAt(index, text) }
    }

    /** Moves the token at [from] to [to], used by drag-and-drop reordering (card bar mode). */
    fun moveToken(from: Int, to: Int) {
        _sentence.update { it.moved(from, to) }
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

    fun reorderCards(reordered: List<CardEntity>) {
        viewModelScope.launch {
            boardRepository.updateCardOrder(reordered)
        }
    }

    fun reorderCategories(reordered: List<CategoryEntity>) {
        viewModelScope.launch {
            boardRepository.updateCategoryOrder(reordered)
        }
    }

    fun addCategory(
        name: String,
        color: Long,
        symbolType: ImageType = ImageType.EMOJI,
        symbolValue: String = "",
    ) {
        viewModelScope.launch {
            val boardId = board.value?.id ?: return@launch
            val id = boardRepository.insertCategory(
                boardId = boardId,
                nameEn = name,
                nameEs = name,
                color = color,
                orderIndex = categories.value.size,
                symbolType = symbolType,
                symbolValue = symbolValue,
            )
            _selectedCategoryId.value = id
        }
    }

    fun updateCategory(
        category: CategoryEntity,
        name: String,
        color: Long,
        symbolType: ImageType = ImageType.EMOJI,
        symbolValue: String = "",
    ) {
        viewModelScope.launch {
            boardRepository.updateCategory(
                category.copy(nameEn = name, nameEs = name, color = color,
                    symbolType = symbolType, symbolValue = symbolValue),
            )
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            val deletedCards = boardRepository.deleteCategory(category)
            deletedCards
                .filter { it.imageType == ImageType.PHOTO }
                .forEach { PhotoStore.delete(it.imageValue) }
            if (category.symbolType == ImageType.PHOTO) {
                PhotoStore.delete(category.symbolValue)
            }
            val remaining = categories.value.filterNot { it.id == category.id }
            _selectedCategoryId.value = remaining.firstOrNull()?.id
        }
    }

    private fun cardPhrase(card: CardEntity): String =
        if (language.value == "es") card.phraseEs else card.phraseEn

    private fun CardEntity.toSentenceToken(phrase: String): SentenceToken.Card =
        SentenceToken.Card(
            cardId = id,
            phrase = phrase,
            label = label(language.value),
            imageType = imageType,
            imageValue = imageValue,
            color = color,
        )

    private fun categoryName(category: CategoryEntity): String =
        if (language.value == "es") category.nameEs else category.nameEn

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
    }
}
