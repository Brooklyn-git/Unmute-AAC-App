package com.unmute.app.ui.board

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unmute.app.R
import com.unmute.app.data.local.CardEntity
import com.unmute.app.data.local.CategoryEntity
import com.unmute.app.domain.model.CardFontSize
import com.unmute.app.domain.model.ImageType
import com.unmute.app.domain.model.label
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
    val editMode by viewModel.editMode.collectAsStateWithLifecycle()
    val columns by viewModel.activeColumns.collectAsStateWithLifecycle()
    val gridProfiles by viewModel.gridProfiles.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

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
    var showGridEditor by remember { mutableStateOf(false) }

    var secureUnlocked by rememberSaveable { mutableStateOf(false) }
    var lockPressCount by rememberSaveable { mutableStateOf(0) }
    var showSecureHint by remember { mutableStateOf(false) }
    LaunchedEffect(settings.secureMode) {
        if (settings.secureMode) {
            secureUnlocked = false
            lockPressCount = 0
            showSecureHint = false
        }
    }
    LaunchedEffect(lockPressCount) {
        if (settings.secureMode && !secureUnlocked && showSecureHint) {
            delay(SECURE_HINT_DISPLAY_MILLIS)
            showSecureHint = false
        }
    }
    val unlocked = !settings.secureMode || secureUnlocked
    val effectiveEditMode = editMode && unlocked
    val onLockClick = {
        if (settings.secureMode && !secureUnlocked) {
            showSecureHint = true
            lockPressCount += 1
            if (lockPressCount >= SECURE_UNLOCK_PRESSES) {
                secureUnlocked = true
                viewModel.setEditMode(true)
                showSecureHint = false
            }
        } else {
            viewModel.toggleEditMode()
        }
    }
    val secureTapHint = if (settings.secureMode && !secureUnlocked && showSecureHint) {
        val remaining = SECURE_UNLOCK_PRESSES - lockPressCount
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
                            if (effectiveEditMode) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = stringResource(
                                if (effectiveEditMode) R.string.done_editing else R.string.edit_board,
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
                sentence = sentence,
                onSentenceChange = viewModel::setSentence,
                onSpeak = viewModel::speakSentence,
                onClear = viewModel::clearSentence,
                onRemoveLast = viewModel::removeLastWord,
            )
            ReorderableCategoryTabs(
                categories = categories,
                selectedCategoryId = effectiveCategoryId,
                language = language,
                onSelect = viewModel::selectCategory,
                onReorder = viewModel::reorderCategories,
                onDeleteCategory = viewModel::deleteCategory,
                editable = effectiveEditMode,
                onAddSection = { addingSection = true },
            )
            if (cards.isEmpty() && !effectiveEditMode) {
                EmptyState()
            } else {
                ReorderableCardsGrid(
                    cards = cards,
                    selectedCategory = selectedCategory,
                    columns = columns,
                    editMode = effectiveEditMode,
                    language = language,
                    cardFontSize = settings.cardFontSize,
                    onCardClick = onCardClick,
                    onEditCard = { editingCard = it },
                    onDeleteCard = viewModel::deleteCard,
                    onAddCard = if (effectiveEditMode && effectiveCategoryId != null) {
                        { addingCard = true }
                    } else {
                        null
                    },
                    onReorder = viewModel::reorderCards,
                    modifier = Modifier.weight(1f),
                )
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
            onSave = { name, color ->
                viewModel.addCategory(name, color)
                addingSection = false
            },
            onDismiss = { addingSection = false },
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

private const val SECURE_UNLOCK_PRESSES = 3
private const val SECURE_HINT_DISPLAY_MILLIS = 1_000L
private const val NEW_CARD_EMOJI = "❓"

@Composable
private fun SentenceBar(
    sentence: String,
    onSentenceChange: (String) -> Unit,
    onSpeak: () -> Unit,
    onClear: () -> Unit,
    onRemoveLast: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = sentence,
                onValueChange = onSentenceChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                        if (sentence.isEmpty()) {
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
            IconButton(onClick = onRemoveLast, enabled = sentence.isNotEmpty()) {
                Icon(Icons.Default.Backspace, contentDescription = stringResource(R.string.remove_last))
            }
            IconButton(onClick = onClear, enabled = sentence.isNotEmpty()) {
                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
            }
            Surface(
                onClick = onSpeak,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                enabled = sentence.isNotEmpty(),
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
