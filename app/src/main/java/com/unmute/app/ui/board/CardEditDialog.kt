package com.unmute.app.ui.board

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.unmute.app.R
import com.unmute.app.data.local.CardEntity
import com.unmute.app.domain.model.ImageType
import com.unmute.app.util.PhotoStore
import java.io.File

private val COLOR_SWATCHES = listOf(
    0xFF9E9E9E to Color(0xFF9E9E9E),
    0xFFEF5350 to Color(0xFFEF5350),
    0xFFFF7043 to Color(0xFFFF7043),
    0xFFFDD835 to Color(0xFFFDD835),
    0xFF66BB6A to Color(0xFF66BB6A),
    0xFF42A5F5 to Color(0xFF42A5F5),
    0xFFAB47BC to Color(0xFFAB47BC),
    0xFF8D6E63 to Color(0xFF8D6E63),
)

@Composable
fun CardEditDialog(
    card: CardEntity,
    onSave: (CardEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var labelEn by remember(card.id) { mutableStateOf(card.labelEn) }
    var labelEs by remember(card.id) { mutableStateOf(card.labelEs) }
    var phraseEn by remember(card.id) { mutableStateOf(card.phraseEn) }
    var phraseEs by remember(card.id) { mutableStateOf(card.phraseEs) }
    var selectedColor by remember(card.id) { mutableStateOf(card.color) }
    var photoPath by remember(card.id) {
        mutableStateOf(card.imageValue.takeIf { card.imageType == ImageType.PHOTO })
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                PhotoStore.save(context, uri)?.let { photoPath = it }
            }
        },
    )

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
                                    imageType = if (photoPath != null) ImageType.PHOTO else card.imageType,
                                    imageValue = photoPath ?: card.imageValue,
                                ),
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.padding(start = 12.dp),
                    ) {
                        Text(stringResource(R.string.change_photo))
                    }
                }

                OutlinedTextField(
                    value = labelEn,
                    onValueChange = { labelEn = it },
                    label = { Text(stringResource(R.string.label_en)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = labelEs,
                    onValueChange = { labelEs = it },
                    label = { Text(stringResource(R.string.label_es)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = phraseEn,
                    onValueChange = { phraseEn = it },
                    label = { Text(stringResource(R.string.phrase_en)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = phraseEs,
                    onValueChange = { phraseEs = it },
                    label = { Text(stringResource(R.string.phrase_es)) },
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
                    if (previousPhoto != null && previousPhoto != photoPath) {
                        PhotoStore.delete(previousPhoto)
                    }
                    onSave(
                        card.copy(
                            labelEn = labelEn.trim(),
                            labelEs = labelEs.trim(),
                            phraseEn = phraseEn.trim(),
                            phraseEs = phraseEs.trim(),
                            color = selectedColor,
                            imageType = if (photoPath != null) ImageType.PHOTO else card.imageType,
                            imageValue = photoPath ?: card.imageValue,
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

@Composable
private fun ColorSwatch(
    color: Color?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val border = if (isSelected) {
        BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = color ?: Color(0xFFE0E0E0),
        border = border,
        modifier = Modifier.size(36.dp),
    ) {}
}
