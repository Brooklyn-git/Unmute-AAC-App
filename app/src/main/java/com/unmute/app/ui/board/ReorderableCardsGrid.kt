package com.unmute.app.ui.board

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.unmute.app.R
import com.unmute.app.data.local.CardEntity
import com.unmute.app.data.local.CategoryEntity
import com.unmute.app.domain.model.CardFontSize
import com.unmute.app.domain.model.label

private const val ADD_CARD_KEY = "add_card"
private const val EDGE_SCROLL_MARGIN_DP = 32
private const val EDGE_SCROLL_STEP_DP = 8

@Composable
fun ReorderableCardsGrid(
    cards: List<CardEntity>,
    categories: List<CategoryEntity>,
    selectedCategory: CategoryEntity?,
    columns: Int,
    editMode: Boolean,
    language: String,
    cardFontSize: CardFontSize,
    onCardClick: (CardEntity) -> Unit,
    onEditCard: (CardEntity) -> Unit,
    onDeleteCard: (CardEntity) -> Unit,
    onAddCard: (() -> Unit)?,
    onReorder: (List<CardEntity>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val density = LocalDensity.current
    var displayCards by remember { mutableStateOf(cards) }
    var draggingCardId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(cards) {
        if (draggingCardId == null) displayCards = cards
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(displayCards, key = { it.id }) { card ->
            val accentColor = Color(card.color ?: selectedCategory?.color ?: 0xFF9E9E9E)
            val isDragging = draggingCardId == card.id
            Box(
                modifier = Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .then(
                        if (editMode) {
                            Modifier.pointerInput(card.id, editMode) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingCardId = card.id
                                    dragOffset = Offset.Zero
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val layoutInfo = gridState.layoutInfo
                                    val draggedItem = layoutInfo.visibleItemsInfo
                                        .firstOrNull { it.key == card.id }
                                    if (draggedItem != null) {
                                        val finger = Offset(
                                            x = draggedItem.offset.x + change.position.x,
                                            y = draggedItem.offset.y + change.position.y,
                                        )
                                        dragOffset = Offset(
                                            x = finger.x - (draggedItem.offset.x + draggedItem.size.width / 2f),
                                            y = finger.y - (draggedItem.offset.y + draggedItem.size.height / 2f),
                                        )
                                        val edgeMargin = EDGE_SCROLL_MARGIN_DP.dp.toPx()
                                        val viewportTop = layoutInfo.viewportStartOffset.toFloat()
                                        val viewportBottom = layoutInfo.viewportEndOffset.toFloat()
                                        when {
                                            finger.y < viewportTop + edgeMargin ->
                                                gridState.dispatchRawDelta(-EDGE_SCROLL_STEP_DP.dp.toPx())
                                            finger.y > viewportBottom - edgeMargin ->
                                                gridState.dispatchRawDelta(EDGE_SCROLL_STEP_DP.dp.toPx())
                                        }
                                        val targetIndex = layoutInfo.visibleItemsInfo
                                            .firstOrNull { item ->
                                                finger.x >= item.offset.x &&
                                                    finger.x <= item.offset.x + item.size.width &&
                                                    finger.y >= item.offset.y &&
                                                    finger.y <= item.offset.y + item.size.height
                                            }
                                            ?.index
                                        if (targetIndex != null && targetIndex < displayCards.size) {
                                            val fromIndex = displayCards.indexOfFirst { it.id == card.id }
                                            if (fromIndex != targetIndex) {
                                                displayCards = moveItem(displayCards, fromIndex, targetIndex)
                                            }
                                        }
                                    }
                                },
                                onDragEnd = {
                                    if (displayCards.map { it.id } != cards.map { it.id }) {
                                        onReorder(displayCards)
                                    }
                                    draggingCardId = null
                                    dragOffset = Offset.Zero
                                },
                                onDragCancel = {
                                    draggingCardId = null
                                    dragOffset = Offset.Zero
                                },
                            )
                        }
                    } else {
                        Modifier
                    }
                ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            translationX = if (isDragging) dragOffset.x else 0f,
                            translationY = if (isDragging) dragOffset.y else 0f,
                            scaleX = if (isDragging) 1.06f else 1f,
                            scaleY = if (isDragging) 1.06f else 1f,
                            shadowElevation = if (isDragging) with(density) { 8.dp.toPx() } else 0f,
                        ),
                ) {
                    CardButton(
                        card = card,
                        label = card.label(language),
                        accentColor = accentColor,
                        language = language,
                        labelFontSize = when (cardFontSize) {
                            CardFontSize.EXTRA_SMALL -> 12.sp
                            CardFontSize.SMALL -> 14.sp
                            CardFontSize.NORMAL -> 16.sp
                            CardFontSize.LARGE -> 20.sp
                            CardFontSize.EXTRA_LARGE -> 24.sp
                        },
                        shortcutCategory = card.shortcutCategoryId
                            ?.let { id -> categories.firstOrNull { it.id == id } },
                        onClick = { if (editMode) onEditCard(card) else onCardClick(card) },
                    )
                    if (editMode) {
                        Surface(
                            onClick = { onDeleteCard(card) },
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
        }
        if (editMode && onAddCard != null) {
            item(key = ADD_CARD_KEY) {
                AddCardButton(
                    accentColor = Color(selectedCategory?.color ?: 0xFF9E9E9E),
                    onClick = { onAddCard() },
                )
            }
        }
    }
}

private fun moveItem(list: List<CardEntity>, from: Int, to: Int): List<CardEntity> {
    val mutable = list.toMutableList()
    val item = mutable.removeAt(from)
    mutable.add(to, item)
    return mutable
}
