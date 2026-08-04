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
import com.unmute.app.data.local.CardEntity
import com.unmute.app.domain.model.ImageType
import com.unmute.app.domain.model.withEditedLabels
import com.unmute.app.util.PhotoStore

@Composable
fun CardEditDialog(
    card: CardEntity,
    language: String,
    isNew: Boolean,
    onSave: (CardEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val isSpanish = language == "es"
    var label by remember(card.id) { mutableStateOf(if (isSpanish) card.labelEs else card.labelEn) }
    var phrase by remember(card.id) { mutableStateOf(if (isSpanish) card.phraseEs else card.phraseEn) }
    var selectedColor by remember(card.id) { mutableStateOf(card.color) }
    var imageType by remember(card.id) { mutableStateOf(card.imageType) }
    var imageValue by remember(card.id) { mutableStateOf(card.imageValue) }
    var showIconPicker by remember(card.id) { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                PhotoStore.save(context, uri)?.let { path ->
                    imageType = ImageType.PHOTO
                    imageValue = path
                }
            }
        },
    )
    val pickerData = rememberIconPickerData()

    if (!showIconPicker) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.edit_card)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                CardImage(
                                    card = card.copy(
                                        imageType = imageType,
                                        imageValue = imageValue,
                                    ),
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { showIconPicker = true },
                            modifier = Modifier.padding(start = 12.dp),
                        ) {
                            Text(stringResource(R.string.change_photo))
                        }
                    }

                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = {
                            Text(stringResource(if (isSpanish) R.string.label_es else R.string.label_en))
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = phrase,
                        onValueChange = { phrase = it },
                        label = {
                            Text(stringResource(if (isSpanish) R.string.phrase_es else R.string.phrase_en))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text(
                        text = stringResource(R.string.color),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ColorSwatch(
                            color = null,
                            isSelected = selectedColor == null,
                            onClick = { selectedColor = null },
                        )
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
                TextButton(
                    onClick = {
                        val previousPhoto = card.imageValue.takeIf { card.imageType == ImageType.PHOTO }
                        if (previousPhoto != null && previousPhoto != imageValue) {
                            PhotoStore.delete(previousPhoto)
                        }
                        val trimmedLabel = label.trim()
                        val trimmedPhrase = phrase.trim()
                        onSave(
                            card.withEditedLabels(isNew, language, trimmedLabel, trimmedPhrase)
                                .copy(
                                    color = selectedColor,
                                    imageType = imageType,
                                    imageValue = imageValue,
                                ),
                        )
                    },
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
            selectedType = imageType,
            selectedValue = imageValue,
            onSelect = { type, value ->
                imageType = type
                imageValue = value
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

