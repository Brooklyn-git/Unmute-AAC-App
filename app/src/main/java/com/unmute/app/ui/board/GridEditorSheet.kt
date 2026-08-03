package com.unmute.app.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.unmute.app.R
import com.unmute.app.data.BoardRepository
import com.unmute.app.data.local.GridProfileEntity
import com.unmute.app.domain.model.CardFontSize
import com.unmute.app.ui.settings.GridProfileDialog
import kotlin.math.roundToInt

private const val MAX_PROFILE_ROWS = 3
private val PROFILE_ROW_HEIGHT = 56.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridEditorSheet(
    profiles: List<GridProfileEntity>,
    activeProfileId: Long,
    activeColumns: Int,
    cardFontSize: CardFontSize,
    onCardFontSizeChange: (CardFontSize) -> Unit,
    onSelectProfile: (Long) -> Unit,
    onAdjustColumns: (Int) -> Unit,
    onAddProfile: (String, Int) -> Unit,
    onEditProfile: (Long, String, Int) -> Unit,
    onDeleteProfile: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var dialog by remember { mutableStateOf<GridEditorDialogState?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
        ) {
            SheetSectionHeader(stringResource(R.string.card_text_size))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Slider(
                    value = sliderValueFor(cardFontSize),
                    onValueChange = { onCardFontSizeChange(cardFontSizeFor(it)) },
                    valueRange = 0f..CardFontSize.entries.lastIndex.toFloat(),
                    steps = CardFontSize.entries.size - 2,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                )
                Stepper(
                    value = cardFontSize.ordinal + 1,
                    range = 1..CardFontSize.entries.size,
                    decreaseLabel = stringResource(R.string.font_size_decrease),
                    increaseLabel = stringResource(R.string.font_size_increase),
                    onChanged = { onCardFontSizeChange(cardFontSizeFor((it - 1).toFloat())) },
                )
            }
            Spacer(Modifier.height(8.dp))

            SheetSectionHeader(stringResource(R.string.grid_layout))
            val listState = rememberLazyListState()
            var rowHeightPx by remember { mutableStateOf(0f) }
            val density = LocalDensity.current
            LaunchedEffect(listState) {
                snapshotFlow { listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size }
                    .collect { measured: Int? ->
                        if (measured != null && measured > 0) rowHeightPx = measured.toFloat()
                    }
            }
            val rowHeightDp = if (rowHeightPx > 0f) with(density) { rowHeightPx.toDp() } else PROFILE_ROW_HEIGHT
            val visibleRows = minOf(profiles.size, MAX_PROFILE_ROWS).coerceIn(2, MAX_PROFILE_ROWS)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeightDp * visibleRows),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                ) {
                    items(profiles, key = { it.id }) { profile ->
                        GridProfileRow(
                            profile = profile,
                            isActive = profile.id == activeProfileId,
                            onSelect = { onSelectProfile(profile.id) },
                            onEdit = { dialog = GridEditorDialogState.Edit(profile) },
                            onDelete = { onDeleteProfile(profile.id) },
                        )
                    }
                }
                LazyListScrollbar(
                    listState = listState,
                    rowHeightPx = rowHeightPx,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .fillMaxHeight(),
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.columns),
                    style = MaterialTheme.typography.titleMedium,
                )
                Stepper(
                    value = activeColumns,
                    range = BoardRepository.MIN_COLUMNS..BoardRepository.MAX_COLUMNS,
                    decreaseLabel = stringResource(R.string.remove_columns),
                    increaseLabel = stringResource(R.string.add_columns),
                    onChanged = { onAdjustColumns(it - activeColumns) },
                )
            }
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val active = profiles.firstOrNull { it.id == activeProfileId }
                OutlinedButton(
                    onClick = {
                        dialog = GridEditorDialogState.Create(
                            baseColumns = active?.columns ?: BoardViewModel.DEFAULT_COLUMNS,
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(
                        text = stringResource(R.string.add_custom_layout),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.done))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    val state = dialog
    when (state) {
        is GridEditorDialogState.Create -> {
            GridProfileDialog(
                title = stringResource(R.string.new_layout),
                initialName = "",
                initialColumns = state.baseColumns,
                columnRange = BoardRepository.MIN_COLUMNS..BoardRepository.MAX_COLUMNS,
                nameLabel = stringResource(R.string.name),
                columnsLabel = stringResource(R.string.columns),
                saveLabel = stringResource(R.string.save),
                cancelLabel = stringResource(R.string.cancel),
                onConfirm = { name, columns ->
                    onAddProfile(name, columns)
                    dialog = null
                },
                onDismiss = { dialog = null },
            )
        }

        is GridEditorDialogState.Edit -> {
            val profile = state.profile
            GridProfileDialog(
                title = stringResource(R.string.edit_layout),
                initialName = profile.name,
                initialColumns = profile.columns,
                columnRange = BoardRepository.MIN_COLUMNS..BoardRepository.MAX_COLUMNS,
                nameLabel = stringResource(R.string.name),
                columnsLabel = stringResource(R.string.columns),
                saveLabel = stringResource(R.string.save),
                cancelLabel = stringResource(R.string.cancel),
                onConfirm = { name, columns ->
                    onEditProfile(profile.id, name, columns)
                    dialog = null
                },
                onDismiss = { dialog = null },
            )
        }

        null -> Unit
    }
}

@Composable
private fun SheetSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun GridProfileRow(
    profile: GridProfileEntity,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = isActive, onClick = onSelect)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            )
            Text(
                text = stringResource(R.string.grid_columns_format, profile.columns),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!profile.isPreset) {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
            }
        }
    }
}

@Composable
private fun LazyListScrollbar(
    listState: LazyListState,
    rowHeightPx: Float,
    modifier: Modifier = Modifier,
) {
    val scrollbarWidth = 4.dp
    val minThumbHeight = 24.dp
    val density = LocalDensity.current
    val widthPx = with(density) { scrollbarWidth.toPx() }
    val minThumbPx = with(density) { minThumbHeight.toPx() }
    val thumbColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    Canvas(modifier = modifier.width(scrollbarWidth)) {
        val totalRows = listState.layoutInfo.totalItemsCount
        val viewportPx = size.height
        val totalContentPx = totalRows * rowHeightPx
        val maxScrollPx = totalContentPx - viewportPx
        if (maxScrollPx <= 0f) return@Canvas

        val currentScrollPx = listState.firstVisibleItemIndex * rowHeightPx +
            listState.firstVisibleItemScrollOffset
        val fraction = (currentScrollPx / maxScrollPx).coerceIn(0f, 1f)
        val thumbHeight = (viewportPx * viewportPx / totalContentPx).coerceAtLeast(minThumbPx)
        val thumbOffset = (viewportPx - thumbHeight) * fraction

        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(0f, thumbOffset),
            size = Size(widthPx, thumbHeight),
            cornerRadius = CornerRadius(widthPx / 2f),
        )
    }
}

@Composable
private fun Stepper(
    value: Int,
    range: IntRange,
    decreaseLabel: String,
    increaseLabel: String,
    onChanged: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { onChanged(value - 1) },
            enabled = value > range.first,
        ) {
            Icon(Icons.Default.Remove, contentDescription = decreaseLabel)
        }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(40.dp),
        )
        IconButton(
            onClick = { onChanged(value + 1) },
            enabled = value < range.last,
        ) {
            Icon(Icons.Default.Add, contentDescription = increaseLabel)
        }
    }
}

private fun sliderValueFor(size: CardFontSize): Float = size.ordinal.toFloat()

private fun cardFontSizeFor(value: Float): CardFontSize =
    CardFontSize.entries[value.roundToInt().coerceIn(0, CardFontSize.entries.lastIndex)]

private sealed interface GridEditorDialogState {
    data class Create(val baseColumns: Int) : GridEditorDialogState
    data class Edit(val profile: GridProfileEntity) : GridEditorDialogState
}
