package com.unmute.app.ui.board

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.unmute.app.R
import com.unmute.app.data.local.CategoryEntity
import com.unmute.app.domain.model.ImageType
import com.unmute.app.util.PhotoStore

@Composable
fun SectionEditDialog(
    onSave: (name: String, color: Long, symbolType: ImageType, symbolValue: String) -> Unit,
    onDismiss: () -> Unit,
    initialCategory: CategoryEntity? = null,
) {
    val isEditing = initialCategory != null
    val context = LocalContext.current
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
    var symbolType by remember { mutableStateOf(initialCategory?.symbolType ?: ImageType.EMOJI) }
    var symbolValue by remember { mutableStateOf(initialCategory?.symbolValue ?: "👋") }
    var showIconPicker by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                PhotoStore.save(context, uri)?.let { path ->
                    symbolType = ImageType.PHOTO
                    symbolValue = path
                }
            }
        },
    )
    val pickerData = rememberIconPickerData()
    val previewCategory = remember(name, symbolType, symbolValue) {
        CategoryEntity(
            boardId = 0,
            nameEn = name,
            nameEs = name,
            color = selectedColor,
            orderIndex = 0,
            symbolType = symbolType,
            symbolValue = symbolValue,
        )
    }

    if (!showIconPicker) {
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Box(
                                modifier = Modifier.size(64.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                SectionImage(category = previewCategory)
                            }
                        }
                        OutlinedButton(
                            onClick = { showIconPicker = true },
                            modifier = Modifier.padding(start = 12.dp),
                        ) {
                            Text(stringResource(R.string.change_photo))
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
                        val previousPhoto = initialCategory?.symbolValue
                            .takeIf { isEditing && initialCategory?.symbolType == ImageType.PHOTO }
                        if (previousPhoto != null && previousPhoto != symbolValue) {
                            PhotoStore.delete(previousPhoto)
                        }
                        onSave(name.trim(), selectedColor, symbolType, symbolValue)
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

    if (showIconPicker) {
        IconPickerDialog(
            icons = pickerData.icons,
            searchLabels = pickerData.labels,
            selectedType = symbolType,
            selectedValue = symbolValue,
            onSelect = { type, value ->
                symbolType = type
                symbolValue = value
                showIconPicker = false
            },
            onChoosePhoto = {
                showIconPicker = false
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onDismiss = { showIconPicker = false },
        )
    }
}
