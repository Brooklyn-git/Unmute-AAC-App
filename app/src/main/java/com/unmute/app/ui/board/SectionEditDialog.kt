package com.unmute.app.ui.board

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unmute.app.R
import com.unmute.app.data.local.CategoryEntity
import com.unmute.app.domain.model.ImageType

private val SECTION_SYMBOLS = listOf(
    "👋", "🧑", "😊", "🍎", "⚽", "🏠",
    "🧸", "🧍", "🐶", "🚗", "🎵", "🎨",
)

@Composable
fun SectionEditDialog(
    onSave: (name: String, color: Long, symbolType: ImageType, symbolValue: String) -> Unit,
    onDismiss: () -> Unit,
    initialCategory: CategoryEntity? = null,
) {
    val isEditing = initialCategory != null
    var name by remember { mutableStateOf(initialCategory?.nameEn ?: "") }
    var selectedColor by remember {
        mutableStateOf(
            initialCategory?.color
                ?.let { existing ->
                    COLOR_SWATCHES.firstOrNull { it.first == existing }?.first
                        ?: existing
                }
                ?: COLOR_SWATCHES[0].first,
        )
    }
    var selectedSymbol by remember {
        mutableStateOf(
            if (isEditing && initialCategory!!.symbolType == ImageType.EMOJI) {
                initialCategory.symbolValue
            } else {
                SECTION_SYMBOLS.first()
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (isEditing) R.string.edit_section else R.string.new_section,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.section_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.section_symbol),
                    style = MaterialTheme.typography.labelLarge,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(SECTION_SYMBOLS) { symbol ->
                        Surface(
                            onClick = { selectedSymbol = symbol },
                            shape = CircleShape,
                            color = if (selectedSymbol == symbol) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = if (selectedSymbol == symbol) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        ) {
                            Text(
                                text = symbol,
                                fontSize = 22.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.color),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    COLOR_SWATCHES.forEach { (value, color) ->
                        ColorSwatch(
                            color = color,
                            isSelected = selectedColor == value,
                            onClick = { selectedColor = value },
                        )
                    }
                }
            }
        },
        confirmButton = {
            val canSave = name.trim().isNotEmpty()
            TextButton(
                onClick = {
                    onSave(name.trim(), selectedColor, ImageType.EMOJI, selectedSymbol)
                },
                enabled = canSave,
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
