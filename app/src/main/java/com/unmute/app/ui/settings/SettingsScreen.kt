package com.unmute.app.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unmute.app.R
import com.unmute.app.data.local.GridProfileEntity
import com.unmute.app.domain.model.AppLanguage
import com.unmute.app.domain.model.AudioOutput
import com.unmute.app.domain.model.CardFontSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val profiles by viewModel.gridProfiles.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<GridProfileDialogState?>(null) }

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item { SectionHeader(stringResource(R.string.language)) }
            item {
                LanguageRow(
                    label = stringResource(R.string.language_system),
                    selected = settings.language == AppLanguage.SYSTEM,
                    onClick = { viewModel.setLanguage(AppLanguage.SYSTEM) },
                )
                LanguageRow(
                    label = stringResource(R.string.language_english),
                    selected = settings.language == AppLanguage.EN,
                    onClick = { viewModel.setLanguage(AppLanguage.EN) },
                )
                LanguageRow(
                    label = stringResource(R.string.language_spanish),
                    selected = settings.language == AppLanguage.ES,
                    onClick = { viewModel.setLanguage(AppLanguage.ES) },
                )
            }

            item { SectionHeader(stringResource(R.string.grid_layout)) }
            items(profiles, key = { it.id }) { profile ->
                GridProfileRow(
                    profile = profile,
                    isActive = profile.id == settings.activeGridProfileId,
                    onSelect = { viewModel.selectGridProfile(profile.id) },
                    onEdit = {
                        dialog = GridProfileDialogState.Edit(profile)
                    },
                    onDelete = { viewModel.deleteProfile(profile.id) },
                )
            }
            item {
                val active = profiles.firstOrNull { it.id == settings.activeGridProfileId }
                OutlinedButton(
                    onClick = {
                        dialog = GridProfileDialogState.Create(
                            base = active ?: profiles.first(),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(
                        text = stringResource(R.string.add_custom_layout),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            item { SectionHeader(stringResource(R.string.card_text_size)) }
            item {
                FontSizeRow(
                    label = stringResource(R.string.font_size_small),
                    selected = settings.cardFontSize == CardFontSize.SMALL,
                    onClick = { viewModel.setCardFontSize(CardFontSize.SMALL) },
                )
                FontSizeRow(
                    label = stringResource(R.string.font_size_normal),
                    selected = settings.cardFontSize == CardFontSize.NORMAL,
                    onClick = { viewModel.setCardFontSize(CardFontSize.NORMAL) },
                )
                FontSizeRow(
                    label = stringResource(R.string.font_size_large),
                    selected = settings.cardFontSize == CardFontSize.LARGE,
                    onClick = { viewModel.setCardFontSize(CardFontSize.LARGE) },
                )
            }

            item { SectionHeader(stringResource(R.string.speech)) }
            item {
                SpeechRateRow(
                    rate = settings.speechRate,
                    onRateChange = viewModel::setSpeechRate,
                )
                SpeechPitchRow(
                    pitch = settings.speechPitch,
                    onPitchChange = viewModel::setSpeechPitch,
                )
                ToggleRow(
                    label = stringResource(R.string.autospeak),
                    checked = settings.autospeak,
                    onCheckedChange = viewModel::setAutospeak,
                )
                Button(
                    onClick = viewModel::testSpeech,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null)
                    Text(
                        text = stringResource(R.string.test_speech),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            item { SectionHeader(stringResource(R.string.audio_output)) }
            item {
                AudioOutputRow(
                    label = stringResource(R.string.audio_output_auto),
                    selected = settings.audioOutput == AudioOutput.AUTO,
                    onClick = { viewModel.setAudioOutput(AudioOutput.AUTO) },
                )
                AudioOutputRow(
                    label = stringResource(R.string.audio_output_speaker),
                    selected = settings.audioOutput == AudioOutput.SPEAKER,
                    onClick = { viewModel.setAudioOutput(AudioOutput.SPEAKER) },
                )
                AudioOutputRow(
                    label = stringResource(R.string.audio_output_wired),
                    selected = settings.audioOutput == AudioOutput.WIRED,
                    onClick = { viewModel.setAudioOutput(AudioOutput.WIRED) },
                )
                AudioOutputRow(
                    label = stringResource(R.string.audio_output_bluetooth),
                    selected = settings.audioOutput == AudioOutput.BLUETOOTH,
                    onClick = { viewModel.setAudioOutput(AudioOutput.BLUETOOTH) },
                )
            }
        }
    }

    val state = dialog
    when (state) {
        is GridProfileDialogState.Create -> {
            val base = state.base
            GridProfileDialog(
                title = stringResource(R.string.new_layout),
                initialName = "",
                initialColumns = base.columns,
                columnRange = SettingsViewModel.MIN_COLUMNS..SettingsViewModel.MAX_COLUMNS,
                nameLabel = stringResource(R.string.name),
                columnsLabel = stringResource(R.string.columns),
                saveLabel = stringResource(R.string.save),
                cancelLabel = stringResource(R.string.cancel),
                onConfirm = { name, columns ->
                    viewModel.addCustomProfile(base, name, columns)
                    dialog = null
                },
                onDismiss = { dialog = null },
            )
        }

        is GridProfileDialogState.Edit -> {
            val profile = state.profile
            GridProfileDialog(
                title = stringResource(R.string.edit_layout),
                initialName = profile.name,
                initialColumns = profile.columns,
                columnRange = SettingsViewModel.MIN_COLUMNS..SettingsViewModel.MAX_COLUMNS,
                nameLabel = stringResource(R.string.name),
                columnsLabel = stringResource(R.string.columns),
                saveLabel = stringResource(R.string.save),
                cancelLabel = stringResource(R.string.cancel),
                onConfirm = { name, columns ->
                    viewModel.updateProfile(profile.id, name, columns)
                    dialog = null
                },
                onDismiss = { dialog = null },
            )
        }

        null -> Unit
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun LanguageRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
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
    val columnsLabel = stringResource(R.string.grid_columns_format, profile.columns)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 8.dp, vertical = 4.dp),
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
                text = columnsLabel,
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
    HorizontalDivider(modifier = Modifier.padding(start = 8.dp, end = 8.dp))
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun FontSizeRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    AudioOutputRow(label = label, selected = selected, onClick = onClick)
}

@Composable
private fun AudioOutputRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SpeechRateRow(rate: Float, onRateChange: (Float) -> Unit) {
    SliderRow(
        label = stringResource(R.string.speech_rate),
        value = rate,
        valueRange = MIN_SPEECH_RATE..MAX_SPEECH_RATE,
        onValueChange = onRateChange,
    )
}

@Composable
private fun SpeechPitchRow(pitch: Float, onPitchChange: (Float) -> Unit) {
    SliderRow(
        label = stringResource(R.string.speech_pitch),
        value = pitch,
        valueRange = MIN_SPEECH_PITCH..MAX_SPEECH_PITCH,
        onValueChange = onPitchChange,
    )
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private val MIN_SPEECH_RATE = 0.5f
private val MAX_SPEECH_RATE = 2.0f
private val MIN_SPEECH_PITCH = 0.5f
private val MAX_SPEECH_PITCH = 2.0f

private sealed interface GridProfileDialogState {
    data class Create(val base: GridProfileEntity) : GridProfileDialogState
    data class Edit(val profile: GridProfileEntity) : GridProfileDialogState
}
