package com.unmute.app.ui.board

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.unmute.app.R
import com.unmute.app.data.DefaultSeed
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

                if (showIconPicker) {
                    IconPickerDialog(
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
                        card.copy(
                            labelEn = if (isNew || isSpanish) trimmedLabel else card.labelEn,
                            labelEs = if (isNew || !isSpanish) trimmedLabel else card.labelEs,
                            phraseEn = if (isNew || isSpanish) trimmedPhrase else card.phraseEn,
                            phraseEs = if (isNew || !isSpanish) trimmedPhrase else card.phraseEs,
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

private data class IconOption(val type: ImageType, val value: String)

@Composable
private fun IconPickerDialog(
    selectedType: ImageType,
    selectedValue: String,
    onSelect: (ImageType, String) -> Unit,
    onChoosePhoto: () -> Unit,
    onDismiss: () -> Unit,
) {
    val icons = remember {
        DefaultSeed.categories
            .flatMap { it.cards }
            .map { IconOption(it.imageType, it.imageValue) }
            .distinctBy { it.value }
    }
    val dashBorder = remember { PathEffect.dashPathEffect(floatArrayOf(12f, 12f)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.change_photo),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel),
                        )
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "gallery") {
                        GalleryTile(onClick = onChoosePhoto, border = dashBorder)
                    }
                    items(icons, key = { it.value }) { icon ->
                        val isSelected = icon.type == selectedType && icon.value == selectedValue
                        IconTile(
                            icon = icon,
                            isSelected = isSelected,
                            onClick = { onSelect(icon.type, icon.value) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryTile(onClick: () -> Unit, border: PathEffect) {
    val outlineColor = MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .drawBehind {
                drawRoundRect(
                    color = outlineColor,
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx(), pathEffect = border),
                )
            }
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "+",
                fontSize = 40.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.choose_from_gallery),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun IconTile(
    icon: IconOption,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
        modifier = Modifier.aspectRatio(1f),
    ) {
        if (icon.type == ImageType.EMOJI) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = icon.value, fontSize = 28.sp)
            }
        } else {
            AsyncImage(
                model = "file:///android_asset/${icon.value}",
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
            )
        }
    }
}
