package com.unmute.app.ui.board

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unmute.app.R
import com.unmute.app.data.local.CardEntity
import com.unmute.app.data.local.CategoryEntity
import com.unmute.app.domain.model.CardFontSize
import com.unmute.app.domain.model.ImageType
import com.unmute.app.domain.model.label

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
    val language by viewModel.language.collectAsStateWithLifecycle()
    val cardFontSize by viewModel.settings.collectAsStateWithLifecycle()

    LaunchedEffect(categories) {
        if (selectedCategoryId == null && categories.isNotEmpty()) {
            viewModel.selectCategory(categories.first().id)
        }
    }

    val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId }
    val effectiveCategoryId = selectedCategory?.id ?: categories.firstOrNull()?.id
    var showTextInput by rememberSaveable { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<CardEntity?>(null) }
    var addingCard by remember { mutableStateOf(false) }

    Scaffold(
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
                    IconButton(onClick = { showTextInput = true }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.type_text),
                        )
                    }
                    IconButton(onClick = viewModel::toggleEditMode) {
                        Icon(
                            if (editMode) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = stringResource(
                                if (editMode) R.string.done_editing else R.string.edit_board,
                            ),
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
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
            CategoryTabs(
                categories = categories,
                selectedCategoryId = effectiveCategoryId,
                language = language,
                onSelect = viewModel::selectCategory,
            )
            if (cards.isEmpty() && !editMode) {
                EmptyState()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    items(cards, key = { it.id }) { card ->
                        val accentColor = Color(card.color ?: selectedCategory?.color ?: 0xFF9E9E9E)
                        Box {
                            CardButton(
                                card = card,
                                label = card.label(language),
                                accentColor = accentColor,
                                labelFontSize = when (cardFontSize.cardFontSize) {
                                    CardFontSize.SMALL -> 14.sp
                                    CardFontSize.LARGE -> 20.sp
                                    else -> 16.sp
                                },
                                onClick = {
                                    if (editMode) editingCard = card else onCardClick(card)
                                },
                            )
                            if (editMode) {
                                Surface(
                                    onClick = { viewModel.deleteCard(card) },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(28.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.delete),
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(4.dp),
                                    )
                                }
                            }
                        }
                    }
                    if (editMode && effectiveCategoryId != null) {
                        item(key = ADD_CARD_KEY) {
                            AddCardButton(
                                accentColor = Color(selectedCategory?.color ?: 0xFF9E9E9E),
                                onClick = { addingCard = true },
                            )
                        }
                    }
                }
            }
            SentenceBar(
                words = sentence,
                onSpeak = viewModel::speakSentence,
                onClear = viewModel::clearSentence,
                onRemoveLast = viewModel::removeLastWord,
            )
        }
    }

    if (showTextInput) {
        TextInputDialog(
            viewModel = viewModel,
            onDismiss = { showTextInput = false },
        )
    }

    editingCard?.let { card ->
        CardEditDialog(
            card = card,
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
            onSave = {
                viewModel.saveCard(it)
                addingCard = false
            },
            onDismiss = { addingCard = false },
        )
    }
}

private const val ADD_CARD_KEY = "add_card"
private const val NEW_CARD_EMOJI = "❓"

@Composable
private fun CategoryTabs(
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    language: String,
    onSelect: (Long) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        lazyItems(categories, key = { it.id }) { category ->
            val selected = category.id == selectedCategoryId
            val color = Color(category.color)
            Surface(
                onClick = { onSelect(category.id) },
                shape = RoundedCornerShape(50),
                color = if (selected) color else color.copy(alpha = 0.15f),
                contentColor = if (selected) Color.White else color,
            ) {
                Text(
                    text = category.label(language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun SentenceBar(
    words: List<String>,
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
            val text = words.joinToString(" ")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
            ) {
                Text(
                    text = text.ifEmpty { stringResource(R.string.sentence_hint) },
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (text.isEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            IconButton(onClick = onRemoveLast, enabled = words.isNotEmpty()) {
                Icon(Icons.Default.Backspace, contentDescription = stringResource(R.string.remove_last))
            }
            IconButton(onClick = onClear, enabled = words.isNotEmpty()) {
                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
            }
            Surface(
                onClick = onSpeak,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                enabled = words.isNotEmpty(),
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
