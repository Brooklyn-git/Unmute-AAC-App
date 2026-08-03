package com.unmute.app.ui.board

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.unmute.app.R
import com.unmute.app.data.DefaultSeed
import com.unmute.app.data.local.CardEntity
import com.unmute.app.domain.model.EMOJI_LIBRARY
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
    val pickerData = remember {
        val seed = DefaultSeed.categories.flatMap { it.cards }
        val labels = buildMap<String, String> {
            seed.forEach { put(it.imageValue, "${it.labelEn} ${it.labelEs}") }
            EMOJI_LIBRARY.forEach { put(it.emoji, "${it.labelEn} ${it.labelEs}") }
        }
        val icons = (
            seed.map { IconOption(it.imageType, it.imageValue) } +
                EMOJI_LIBRARY.map { IconOption(ImageType.EMOJI, it.emoji) }
            ).distinctBy { it.value }
        IconPickerData(icons, labels)
    }
    val dashBorder = remember { PathEffect.dashPathEffect(floatArrayOf(12f, 12f)) }

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
            dashBorder = dashBorder,
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

private data class IconOption(val type: ImageType, val value: String)

private data class IconPickerData(
    val icons: List<IconOption>,
    val labels: Map<String, String>,
)

@Composable
private fun IconPickerDialog(
    icons: List<IconOption>,
    searchLabels: Map<String, String>,
    dashBorder: PathEffect,
    selectedType: ImageType,
    selectedValue: String,
    onSelect: (ImageType, String) -> Unit,
    onChoosePhoto: () -> Unit,
    onDismiss: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val maxGridHeight = (configuration.screenHeightDp * 0.55f).dp
    var query by remember { mutableStateOf("") }
    val filteredIcons = remember(icons, searchLabels, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            icons
        } else {
            icons.filter { icon ->
                searchLabels[icon.value]?.contains(q) == true || icon.value.lowercase().contains(q)
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(),
        title = { Text(stringResource(R.string.change_photo)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.search_icons)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                if (filteredIcons.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_matching_icons),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outline,
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(72.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxGridHeight),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item(key = "gallery") {
                            GalleryTile(
                                onClick = onChoosePhoto,
                                border = dashBorder,
                                modifier = Modifier.aspectRatio(1f),
                            )
                        }
                        items(filteredIcons, key = { it.value }) { icon ->
                            val isSelected = icon.type == selectedType && icon.value == selectedValue
                            IconTile(
                                icon = icon,
                                isSelected = isSelected,
                                onClick = { onSelect(icon.type, icon.value) },
                                modifier = Modifier.aspectRatio(1f),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun GalleryTile(
    onClick: () -> Unit,
    border: PathEffect,
    modifier: Modifier = Modifier,
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    Box(
        modifier = modifier
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
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "+",
                fontSize = 32.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.choose_from_gallery),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
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
    modifier: Modifier = Modifier,
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
        modifier = modifier,
    ) {
        if (icon.type == ImageType.EMOJI) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = icon.value, fontSize = 40.sp)
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
