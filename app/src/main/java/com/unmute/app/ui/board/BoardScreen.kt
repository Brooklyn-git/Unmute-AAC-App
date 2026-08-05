package com.unmute.app.ui.board

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unmute.app.R
import com.unmute.app.data.local.CardEntity
import com.unmute.app.data.local.CategoryEntity
import com.unmute.app.domain.model.CardFontSize
import com.unmute.app.domain.model.DEFAULT_PREDICTION_LIMIT
import com.unmute.app.domain.model.ImageType
import com.unmute.app.domain.model.PredictionVocabulary
import com.unmute.app.domain.model.SectionLayout
import com.unmute.app.domain.model.SentenceToken
import com.unmute.app.domain.model.applySuggestion
import com.unmute.app.domain.model.currentWordAt
import com.unmute.app.domain.model.deleteWordBefore
import com.unmute.app.domain.model.dropTargetIndex
import com.unmute.app.domain.model.label
import com.unmute.app.domain.model.predict
import com.unmute.app.tts.TtsIssue
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardScreen(
    viewModel: BoardViewModel,
    onOpenSettings: () -> Unit,
    onCardClick: (CardEntity) -> Unit,
) {
    val board by viewModel.board.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val sentence by viewModel.sentence.collectAsStateWithLifecycle()
    val sentenceText by viewModel.sentenceText.collectAsStateWithLifecycle()
    val editMode by viewModel.editMode.collectAsStateWithLifecycle()
    val columns by viewModel.activeColumns.collectAsStateWithLifecycle()
    val gridProfiles by viewModel.gridProfiles.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val predictionVocabulary by viewModel.predictionVocabulary.collectAsStateWithLifecycle()

    LaunchedEffect(categories) {
        if (selectedCategoryId == null && categories.isNotEmpty()) {
            viewModel.selectCategory(categories.first().id)
        }
    }

    val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId }
    val effectiveCategoryId = selectedCategory?.id ?: categories.firstOrNull()?.id
    var editingCard by remember { mutableStateOf<CardEntity?>(null) }
    var addingCard by remember { mutableStateOf(false) }
    var addingSection by remember { mutableStateOf(false) }
    var editingSection by remember { mutableStateOf<CategoryEntity?>(null) }
    var showGridEditor by remember { mutableStateOf(false) }

    var secureUnlocked by rememberSaveable { mutableStateOf(false) }
    var lockPressCount by rememberSaveable { mutableStateOf(0) }
    var showSecureHint by remember { mutableStateOf(false) }
    var showSections by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(settings.secureMode) {
        if (settings.secureMode) {
            secureUnlocked = false
            lockPressCount = 0
            showSecureHint = false
            viewModel.setEditMode(false)
        }
    }
    LaunchedEffect(lockPressCount) {
        if (settings.secureMode && !secureUnlocked && showSecureHint) {
            delay(SECURE_HINT_DISPLAY_MILLIS)
            showSecureHint = false
        }
    }
    LaunchedEffect(lockPressCount, settings.secureResetSeconds) {
        if (settings.secureMode && !secureUnlocked && lockPressCount > 0) {
            delay(settings.secureResetSeconds * 1_000L)
            lockPressCount = 0
        }
    }
    val unlocked = !settings.secureMode || editMode
    val handleCardClick: (CardEntity) -> Unit = { card ->
        val targetId = card.shortcutCategoryId
        if (targetId != null) {
            viewModel.navigateToSection(targetId)
            if (settings.sectionLayout == SectionLayout.GRID) {
                showSections = false
            }
        } else {
            onCardClick(card)
        }
    }
    val onLockClick = {
        if (settings.secureMode && !editMode) {
            showSecureHint = true
            lockPressCount += 1
            if (lockPressCount >= settings.secureTapCount) {
                secureUnlocked = true
                viewModel.setEditMode(true)
                showSecureHint = false
            }
        } else {
            if (settings.secureMode) {
                secureUnlocked = false
                lockPressCount = 0
            }
            viewModel.toggleEditMode()
        }
    }
    val secureTapHint = if (settings.secureMode && !editMode && showSecureHint) {
        val remaining = (settings.secureTapCount - lockPressCount).coerceAtLeast(1)
        if (remaining == 1) {
            stringResource(R.string.secure_tap_hint_one)
        } else {
            stringResource(R.string.secure_tap_hint_many, remaining)
        }
    } else {
        null
    }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.ttsErrors.collect { issue ->
            val message = when (issue) {
                TtsIssue.UNAVAILABLE -> context.getString(R.string.tts_error_unavailable)
                TtsIssue.SPEAK_FAILED -> context.getString(R.string.tts_error_failed)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = board?.label(language) ?: "",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    if (editMode && unlocked) {
                        IconButton(onClick = { showGridEditor = true }) {
                            Icon(
                                Icons.Default.GridView,
                                contentDescription = stringResource(R.string.grid_layout),
                            )
                        }
                    }
                    IconButton(onClick = onLockClick) {
                        Icon(
                            if (editMode) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = stringResource(
                                if (editMode) R.string.done_editing else R.string.edit_board,
                            ),
                        )
                    }
                    if (unlocked) {
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = stringResource(R.string.settings),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            SentenceBar(
                tokens = sentence,
                sentenceText = sentenceText,
                showCards = settings.showSentenceCards,
                wordPredictionEnabled = settings.wordPrediction,
                predictionVocabulary = predictionVocabulary,
                onSentenceChange = viewModel::setSentence,
                onTextChange = viewModel::updateTextAt,
                onInsertText = viewModel::insertTextAt,
                onRemoveToken = viewModel::removeTokenAt,
                onRemoveLastCard = viewModel::removeLastCard,
                onMoveToken = viewModel::moveToken,
                onSpeak = viewModel::speakSentence,
                onClear = viewModel::clearSentence,
            )
            when (settings.sectionLayout) {
                SectionLayout.TABS -> {
                    ReorderableCategoryTabs(
                        categories = categories,
                        selectedCategoryId = effectiveCategoryId,
                        language = language,
                        onSelect = viewModel::selectCategory,
                        onReorder = viewModel::reorderCategories,
                        onDeleteCategory = viewModel::deleteCategory,
                        onEditCategory = { editingSection = it },
                        editable = editMode,
                        onAddSection = { addingSection = true },
                        showSymbols = settings.showSectionSymbols,
                    )
                    if (cards.isEmpty() && !editMode) {
                        EmptyState()
                    } else {
                        ReorderableCardsGrid(
                            cards = cards,
                            categories = categories,
                            selectedCategory = selectedCategory,
                            columns = columns,
                            editMode = editMode,
                            language = language,
                            cardFontSize = settings.cardFontSize,
                            onCardClick = handleCardClick,
                            onEditCard = { editingCard = it },
                            onDeleteCard = viewModel::deleteCard,
                            onAddCard = if (editMode && effectiveCategoryId != null) {
                                { addingCard = true }
                            } else {
                                null
                            },
                            onReorder = viewModel::reorderCards,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                SectionLayout.GRID -> {
                    if (showSections || categories.isEmpty()) {
                        SectionGrid(
                            categories = categories,
                            selectedCategoryId = effectiveCategoryId,
                            language = language,
                            editable = editMode,
                            onSelect = { id ->
                                viewModel.selectCategory(id)
                                showSections = false
                            },
                            onDeleteCategory = viewModel::deleteCategory,
                            onEditCategory = { editingSection = it },
                            onAddSection = { addingSection = true },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                        ) {
                            IconButton(onClick = { showSections = true }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back_to_sections),
                                )
                            }
                            Text(
                                text = selectedCategory?.label(language) ?: "",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                            )
                        }
                        if (cards.isEmpty() && !editMode) {
                            EmptyState()
                        } else {
                            ReorderableCardsGrid(
                                cards = cards,
                                categories = categories,
                                selectedCategory = selectedCategory,
                                columns = columns,
                                editMode = editMode,
                                language = language,
                                cardFontSize = settings.cardFontSize,
                                onCardClick = handleCardClick,
                                onEditCard = { editingCard = it },
                                onDeleteCard = viewModel::deleteCard,
                                onAddCard = if (editMode && effectiveCategoryId != null) {
                                    { addingCard = true }
                                } else {
                                    null
                                },
                                onReorder = viewModel::reorderCards,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            secureTapHint?.let { hint ->
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                )
            }
        }
    }

    editingCard?.let { card ->
        CardEditDialog(
            card = card,
            language = language,
            categories = categories,
            isNew = false,
            onSave = {
                viewModel.saveCard(it)
                editingCard = null
            },
            onDismiss = { editingCard = null },
        )
    }

    val addingCardCategoryId = if (addingCard) effectiveCategoryId else null
    if (addingCardCategoryId != null) {
        CardEditDialog(
            card = CardEntity(
                categoryId = addingCardCategoryId,
                labelEn = "",
                labelEs = "",
                phraseEn = "",
                phraseEs = "",
                imageType = ImageType.EMOJI,
                imageValue = NEW_CARD_EMOJI,
                color = null,
                orderIndex = 0,
            ),
            language = language,
            categories = categories,
            isNew = true,
            onSave = {
                viewModel.saveCard(it)
                addingCard = false
            },
            onDismiss = { addingCard = false },
        )
    }

    if (addingSection) {
        SectionEditDialog(
            onSave = { name, color, symbolType, symbolValue ->
                viewModel.addCategory(name, color, symbolType, symbolValue)
                addingSection = false
            },
            onDismiss = { addingSection = false },
        )
    }

    editingSection?.let { category ->
        SectionEditDialog(
            onSave = { name, color, symbolType, symbolValue ->
                viewModel.updateCategory(category, name, color, symbolType, symbolValue)
                editingSection = null
            },
            onDismiss = { editingSection = null },
            initialCategory = category,
        )
    }

    if (showGridEditor) {
        GridEditorSheet(
            profiles = gridProfiles,
            activeProfileId = settings.activeGridProfileId,
            activeColumns = columns,
            cardFontSize = settings.cardFontSize,
            onCardFontSizeChange = viewModel::setCardFontSize,
            onSelectProfile = viewModel::selectGridProfile,
            onAdjustColumns = viewModel::adjustActiveColumns,
            onAddProfile = viewModel::addGridProfile,
            onEditProfile = viewModel::updateGridProfile,
            onDeleteProfile = viewModel::deleteGridProfile,
            onDismiss = { showGridEditor = false },
        )
    }
}

private const val SECURE_HINT_DISPLAY_MILLIS = 1_000L
private const val NEW_CARD_EMOJI = "❓"

@Composable
private fun SentenceBar(
    tokens: List<SentenceToken>,
    sentenceText: String,
    showCards: Boolean,
    wordPredictionEnabled: Boolean,
    predictionVocabulary: PredictionVocabulary,
    onSentenceChange: (String) -> Unit,
    onTextChange: (Int, String) -> Unit,
    onInsertText: (Int?, String) -> Unit,
    onRemoveToken: (Int) -> Unit,
    onRemoveLastCard: () -> Unit,
    onMoveToken: (Int, Int) -> Unit,
    onSpeak: () -> Unit,
    onClear: () -> Unit,
) {
    var selectedIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val anchorIndex = selectedIndex?.takeIf { it in tokens.indices }
    val composingIndex = when {
        anchorIndex != null -> anchorIndex + 1
        tokens.lastOrNull() is SentenceToken.Text -> tokens.lastIndex
        else -> -1
    }
    val composingText = (tokens.getOrNull(composingIndex) as? SentenceToken.Text)?.text.orEmpty()
    var composingValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(composingText))
    }
    var textValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(sentenceText))
    }

    LaunchedEffect(sentenceText) {
        if (sentenceText != textValue.text) {
            textValue = TextFieldValue(sentenceText, TextRange(sentenceText.length))
        }
    }
    LaunchedEffect(composingIndex, composingText) {
        if (composingText != composingValue.text) {
            composingValue = TextFieldValue(composingText, TextRange(composingText.length))
        }
    }

    val composingFocus = remember { FocusRequester() }
    var requestComposeFocus by remember { mutableStateOf(false) }
    LaunchedEffect(requestComposeFocus) {
        if (requestComposeFocus) {
            requestComposeFocus = false
            delay(50)
            composingFocus.requestFocus()
        }
    }

    val onBackspace = {
        if (showCards) {
            when {
                composingValue.text.isNotEmpty() -> {
                    val (newText, caret) = deleteWordBefore(composingValue.text, composingValue.selection.min)
                    composingValue = TextFieldValue(newText, TextRange(caret))
                    onInsertText(anchorIndex, newText)
                }
                anchorIndex != null -> {
                    onRemoveToken(anchorIndex)
                    selectedIndex = null
                }
                else -> onRemoveLastCard()
            }
        } else {
            val (newText, caret) = deleteWordBefore(textValue.text, textValue.selection.min)
            textValue = TextFieldValue(newText, TextRange(caret))
            onSentenceChange(newText)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showCards) {
                    CardSentenceInput(
                        tokens = tokens,
                        composingValue = composingValue,
                        composingIndex = composingIndex,
                        anchorIndex = anchorIndex,
                        composingFocus = composingFocus,
                        onComposingValueChange = {
                            composingValue = it
                            onInsertText(anchorIndex, it.text)
                        },
                        onTextChange = onTextChange,
                        onSelectIndex = { selectedIndex = it },
                        onRequestComposeFocus = { requestComposeFocus = true },
                        onClearSelection = { selectedIndex = null },
                        onMoveToken = onMoveToken,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Box(modifier = Modifier.weight(1f)) {
                        BasicTextField(
                            value = textValue,
                            onValueChange = {
                                textValue = it
                                onSentenceChange(it.text)
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.headlineSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                                    if (textValue.text.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.sentence_hint),
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                        )
                    }
                }
                IconButton(onClick = onBackspace, enabled = tokens.isNotEmpty()) {
                    Icon(
                        Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = stringResource(R.string.remove_last),
                    )
                }
                IconButton(onClick = onClear, enabled = sentenceText.isNotEmpty()) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                }
                Surface(
                    onClick = onSpeak,
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    enabled = sentenceText.isNotEmpty(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null)
                        Text(
                            text = stringResource(R.string.speak),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
            WordPredictionSuggestions(
                enabled = wordPredictionEnabled && !showCards,
                vocabulary = predictionVocabulary,
                currentText = textValue.text,
                caret = textValue.selection.min,
                onSelect = { suggestion ->
                    val (newText, newCaret) = applySuggestion(textValue.text, textValue.selection.min, suggestion)
                    textValue = TextFieldValue(newText, TextRange(newCaret))
                    onSentenceChange(newText)
                },
            )
        }
    }
}

@Composable
private fun WordPredictionSuggestions(
    enabled: Boolean,
    vocabulary: PredictionVocabulary,
    currentText: String,
    caret: Int,
    onSelect: (String) -> Unit,
) {
    val currentWord = currentWordAt(currentText, caret)
    val suggestions = if (enabled && currentWord.isNotEmpty()) {
        predict(currentWord, vocabulary.words, vocabulary.usage, DEFAULT_PREDICTION_LIMIT)
            .filterNot { it == currentWord }
    } else {
        emptyList()
    }
    if (suggestions.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp)
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        suggestions.forEach { suggestion ->
            Surface(
                onClick = { onSelect(suggestion) },
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Text(
                    text = suggestion,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun CardSentenceInput(
    tokens: List<SentenceToken>,
    composingValue: TextFieldValue,
    composingIndex: Int,
    anchorIndex: Int?,
    composingFocus: FocusRequester,
    onComposingValueChange: (TextFieldValue) -> Unit,
    onTextChange: (Int, String) -> Unit,
    onSelectIndex: (Int) -> Unit,
    onRequestComposeFocus: () -> Unit,
    onClearSelection: () -> Unit,
    onMoveToken: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val tokenBounds = remember { mutableStateMapOf<Int, Rect>() }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(tokens.size, composingValue.text, anchorIndex) {
        if (anchorIndex == null || anchorIndex >= tokens.lastIndex) {
            delay(50)
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    Row(
        modifier = modifier.horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tokens.forEachIndexed { index, token ->
            when (token) {
                is SentenceToken.Card -> {
                    val chipSelected = anchorIndex == index
                    SentenceChip(
                        token = token,
                        selected = chipSelected,
                        dragging = draggingIndex == index,
                        dragOffset = if (draggingIndex == index) dragOffset else Offset.Zero,
                        onClick = {
                            if (chipSelected) onRequestComposeFocus() else onSelectIndex(index)
                        },
                        onGloballyPositioned = { coordinates ->
                            tokenBounds[index] = coordinates.boundsInRoot()
                        },
                        onDragStart = { startPosition ->
                            onClearSelection()
                            draggingIndex = index
                            dragStart = startPosition
                            dragOffset = Offset.Zero
                        },
                        onDrag = { position ->
                            dragOffset = position - (dragStart ?: Offset.Zero)
                        },
                        onDragEnd = {
                            val from = draggingIndex
                            val dropCenter = tokenBounds[from]?.center?.plus(dragOffset)
                            draggingIndex = null
                            dragStart = null
                            dragOffset = Offset.Zero
                            if (from != null && dropCenter != null) {
                                val centerXs = tokens.indices.map { index ->
                                    tokenBounds[index]?.center?.x
                                }
                                if (centerXs.none { it == null }) {
                                    val target = dropTargetIndex(
                                        from = from,
                                        dropX = dropCenter.x,
                                        centerXs = centerXs.mapNotNull { it },
                                    )
                                    if (target != null) onMoveToken(from, target)
                                }
                            }
                        },
                        onDragCancel = {
                            draggingIndex = null
                            dragStart = null
                            dragOffset = Offset.Zero
                        },
                    )
                    if (anchorIndex == index) {
                        ComposingTextField(
                            value = composingValue,
                            minWidth = 12.dp,
                            showCaret = true,
                            showHint = false,
                            onValueChange = onComposingValueChange,
                            modifier = Modifier.focusRequester(composingFocus),
                        )
                    }
                }
                is SentenceToken.Text -> {
                    if (index != composingIndex) {
                        TextTokenField(
                            text = token.text,
                            onTextChange = { onTextChange(index, it) },
                            onGloballyPositioned = { coordinates ->
                                tokenBounds[index] = coordinates.boundsInRoot()
                            },
                        )
                    }
                }
            }
        }
        if (anchorIndex == null) {
            ComposingTextField(
                value = composingValue,
                minWidth = if (tokens.isEmpty()) 100.dp else 12.dp,
                showCaret = tokens.isNotEmpty(),
                showHint = tokens.isEmpty(),
                onValueChange = onComposingValueChange,
                modifier = Modifier.focusRequester(composingFocus),
            )
        }
    }
}

@Composable
private fun TextTokenField(
    text: String,
    onTextChange: (String) -> Unit,
    onGloballyPositioned: (LayoutCoordinates) -> Unit,
) {
    var value by remember { mutableStateOf(TextFieldValue(text)) }
    LaunchedEffect(text) {
        if (text != value.text) {
            value = TextFieldValue(text, TextRange(text.length))
        }
    }
    BasicTextField(
        value = value,
        onValueChange = { newValue ->
            value = newValue
            onTextChange(newValue.text)
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.headlineSmall.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .padding(end = 4.dp)
            .onGloballyPositioned(onGloballyPositioned),
    )
}

@Composable
private fun ComposingTextField(
    value: TextFieldValue,
    minWidth: Dp,
    showCaret: Boolean,
    showHint: Boolean,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.headlineSmall.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier
            .widthIn(min = minWidth)
            .padding(horizontal = 4.dp)
            .onFocusChanged { focused = it.isFocused },
        decorationBox = { innerTextField ->
            Box {
                when {
                    value.text.isEmpty() && showHint -> Text(
                        text = stringResource(R.string.sentence_hint),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    value.text.isEmpty() && showCaret && !focused -> Text(
                        text = "|",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    else -> innerTextField()
                }
            }
        },
    )
}

@Composable
private fun SentenceChip(
    token: SentenceToken.Card,
    selected: Boolean,
    dragging: Boolean,
    dragOffset: Offset,
    onClick: () -> Unit,
    onGloballyPositioned: (LayoutCoordinates) -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        modifier = Modifier
            .padding(end = 6.dp)
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer {
                translationX = if (dragging) dragOffset.x else 0f
                translationY = if (dragging) dragOffset.y else 0f
                scaleX = if (dragging) 1.05f else 1f
                scaleY = if (dragging) 1.05f else 1f
            }
            .pointerInput(token.cardId) {
                detectDragGesturesAfterLongPress(
                    onDragStart = onDragStart,
                    onDrag = { change, _ -> onDrag(change.position) },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel,
                )
            }
            .onGloballyPositioned(onGloballyPositioned),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SymbolImage(
                imageType = token.imageType,
                imageValue = token.imageValue,
                modifier = Modifier.size(32.dp),
                symbolPadding = 4.dp,
                emojiFontSize = 20.sp,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = token.label,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.no_cards),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
