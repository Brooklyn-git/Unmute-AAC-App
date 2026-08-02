package com.unmute.app.ui.board

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.unmute.app.R
import com.unmute.app.data.local.CategoryEntity
import com.unmute.app.domain.model.label

private const val EDGE_SCROLL_MARGIN_DP = 32
private const val EDGE_SCROLL_STEP_DP = 8

@Composable
fun ReorderableCategoryTabs(
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    language: String,
    onSelect: (Long) -> Unit,
    onReorder: (List<CategoryEntity>) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit,
    editable: Boolean,
    onAddSection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var displayCategories by remember { mutableStateOf(categories) }
    var draggingCategoryId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    LaunchedEffect(categories) {
        if (draggingCategoryId == null) displayCategories = categories
    }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        items(displayCategories, key = { it.id }) { category ->
            val selected = category.id == selectedCategoryId
            val isDragging = draggingCategoryId == category.id
            Box(
                modifier = Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .pointerInput(category.id) {                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingCategoryId = category.id
                                dragOffset = 0f
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val layoutInfo = listState.layoutInfo
                                val draggedItem = layoutInfo.visibleItemsInfo
                                    .firstOrNull { it.key == category.id }
                                if (draggedItem != null) {
                                    val fingerX = draggedItem.offset + change.position.x
                                    dragOffset = fingerX - (draggedItem.offset + draggedItem.size / 2f)
                                    val edgeMargin = EDGE_SCROLL_MARGIN_DP.dp.toPx()
                                    when {
                                        fingerX < layoutInfo.viewportStartOffset + edgeMargin ->
                                            listState.dispatchRawDelta(-EDGE_SCROLL_STEP_DP.dp.toPx())
                                        fingerX > layoutInfo.viewportEndOffset - edgeMargin ->
                                            listState.dispatchRawDelta(EDGE_SCROLL_STEP_DP.dp.toPx())
                                    }
                                    val targetIndex = layoutInfo.visibleItemsInfo
                                        .firstOrNull { item ->
                                            fingerX >= item.offset && fingerX <= item.offset + item.size
                                        }
                                        ?.index
                                    if (targetIndex != null && targetIndex < displayCategories.size) {
                                        val fromIndex = displayCategories.indexOfFirst { it.id == category.id }
                                        if (fromIndex != targetIndex) {
                                            displayCategories = moveItem(displayCategories, fromIndex, targetIndex)
                                        }
                                    }
                                }
                            },
                            onDragEnd = {
                                if (displayCategories.map { it.id } != categories.map { it.id }) {
                                    onReorder(displayCategories)
                                }
                                draggingCategoryId = null
                                dragOffset = 0f
                            },
                            onDragCancel = {
                                draggingCategoryId = null
                                dragOffset = 0f
                            },
                        )
                    },
            ) {
                Box(
                    modifier = Modifier.graphicsLayer(
                        translationX = if (isDragging) dragOffset else 0f,
                        scaleX = if (isDragging) 1.06f else 1f,
                        scaleY = if (isDragging) 1.06f else 1f,
                        shadowElevation = if (isDragging) with(density) { 8.dp.toPx() } else 0f,
                    ),
                ) {
                    CategoryChip(
                        name = category.label(language),
                        color = Color(category.color),
                        selected = selected,
                        onClick = { onSelect(category.id) },
                    )
                    if (editable && !category.isPreset) {
                        Surface(
                            onClick = { onDeleteCategory(category) },
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
        if (editable) {
            item(key = "add_section") {
                AddSectionChip(onClick = onAddSection)
            }
        }
    }
}

@Composable
private fun AddSectionChip(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = "+",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.add_section),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun CategoryChip(
    name: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) color else color.copy(alpha = 0.15f),
        contentColor = if (selected) Color.White else color,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}

private fun moveItem(list: List<CategoryEntity>, from: Int, to: Int): List<CategoryEntity> {
    val mutable = list.toMutableList()
    val item = mutable.removeAt(from)
    mutable.add(to, item)
    return mutable
}
