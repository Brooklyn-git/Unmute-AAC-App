package com.unmute.app.ui.board

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.unmute.app.R
import com.unmute.app.data.BoardRepository
import com.unmute.app.data.local.GridProfileEntity
import com.unmute.app.ui.settings.GridProfileDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridEditorSheet(
    profiles: List<GridProfileEntity>,
    activeProfileId: Long,
    activeColumns: Int,
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
            Text(
                text = stringResource(R.string.grid_layout),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            profiles.forEach { profile ->
                GridProfileRow(
                    profile = profile,
                    isActive = profile.id == activeProfileId,
                    onSelect = { onSelectProfile(profile.id) },
                    onEdit = { dialog = GridEditorDialogState.Edit(profile) },
                    onDelete = { onDeleteProfile(profile.id) },
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
                ColumnStepper(
                    value = activeColumns,
                    range = BoardRepository.MIN_COLUMNS..BoardRepository.MAX_COLUMNS,
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
private fun ColumnStepper(
    value: Int,
    range: IntRange,
    onChanged: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { onChanged(value - 1) },
            enabled = value > range.first,
        ) {
            Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.remove_columns))
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
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_columns))
        }
    }
}

private sealed interface GridEditorDialogState {
    data class Create(val baseColumns: Int) : GridEditorDialogState
    data class Edit(val profile: GridProfileEntity) : GridEditorDialogState
}
