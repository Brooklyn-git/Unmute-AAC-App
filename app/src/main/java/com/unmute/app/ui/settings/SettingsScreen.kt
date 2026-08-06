package com.unmute.app.ui.settings

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unmute.app.R
import com.unmute.app.data.SettingsRepository
import com.unmute.app.domain.model.AppLanguage
import com.unmute.app.domain.model.AudioOutputIds
import com.unmute.app.domain.model.SectionLayout
import com.unmute.app.tts.TtsIssue
import com.unmute.app.ui.components.Stepper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showSecureConfirm by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current
    var ttsEngineLabel by remember { mutableStateOf<String?>(null) }
    var ttsEngines by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                ttsEngineLabel = viewModel.ttsEngineLabel()
                ttsEngines = viewModel.availableTtsEngines()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.ttsErrors.collect { issue ->
            val message = when (issue) {
                TtsIssue.UNAVAILABLE -> context.getString(R.string.tts_error_unavailable)
                TtsIssue.SPEAK_FAILED -> context.getString(R.string.tts_error_failed)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.backupEvents.collect { event ->
            when (event) {
                is BackupEvent.Message -> snackbarHostState.showSnackbar(context.getString(event.resId))
            }
        }
    }

    val exportFileName = remember {
        val date = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        "unmute-backup-$date.unmute"
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        uri?.let(viewModel::exportData)
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        pendingImportUri = uri
    }

    BackHandler(onBack = onBack)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            item { SectionHeader(stringResource(R.string.general)) }
            item {
                Column {
                    SettingsDropdown(
                        label = stringResource(R.string.language),
                        currentLabel = when (settings.language) {
                            AppLanguage.SYSTEM -> stringResource(R.string.language_system)
                            AppLanguage.EN -> stringResource(R.string.language_english)
                            AppLanguage.ES -> stringResource(R.string.language_spanish)
                        },
                        options = AppLanguage.entries,
                        optionLabel = { language ->
                            when (language) {
                                AppLanguage.SYSTEM -> stringResource(R.string.language_system)
                                AppLanguage.EN -> stringResource(R.string.language_english)
                                AppLanguage.ES -> stringResource(R.string.language_spanish)
                            }
                        },
                        onSelect = viewModel::setLanguage,
                    )
                    SettingsDropdown(
                        label = stringResource(R.string.sections),
                        currentLabel = when (settings.sectionLayout) {
                            SectionLayout.TABS -> stringResource(R.string.section_layout_tabs)
                            SectionLayout.GRID -> stringResource(R.string.section_layout_grid)
                        },
                        options = SectionLayout.entries,
                        optionLabel = { layout ->
                            when (layout) {
                                SectionLayout.TABS -> stringResource(R.string.section_layout_tabs)
                                SectionLayout.GRID -> stringResource(R.string.section_layout_grid)
                            }
                        },
                        onSelect = viewModel::setSectionLayout,
                    )
                    ToggleRow(
                        label = stringResource(R.string.word_prediction),
                        checked = settings.wordPrediction,
                        onCheckedChange = viewModel::setWordPrediction,
                    )
                    ToggleRow(
                        label = stringResource(R.string.show_sentence_cards),
                        checked = settings.showSentenceCards,
                        onCheckedChange = viewModel::setShowSentenceCards,
                    )
                    ToggleRow(
                        label = stringResource(R.string.speak_section_names),
                        checked = settings.speakSectionNames,
                        onCheckedChange = viewModel::setSpeakSectionNames,
                    )
                    ToggleRow(
                        label = stringResource(R.string.autospeak),
                        checked = settings.autospeak,
                        onCheckedChange = viewModel::setAutospeak,
                    )
                    if (settings.sectionLayout == SectionLayout.TABS) {
                        ToggleRow(
                            label = stringResource(R.string.show_section_symbols),
                            checked = settings.showSectionSymbols,
                            onCheckedChange = viewModel::setShowSectionSymbols,
                        )
                    }
                }
            }

            item { SectionHeader(stringResource(R.string.audio_output)) }
            item {
                val outputs = viewModel.availableOutputs()
                SettingsDropdown(
                    label = stringResource(R.string.audio_output),
                    currentLabel = audioOutputLabel(settings.audioOutput, outputs),
                    options = listOf(AudioOutputIds.AUTO) + outputs.map { it.first },
                    optionLabel = { id -> audioOutputLabel(id, outputs) },
                    onSelect = viewModel::setAudioOutput,
                )
            }

            item { SectionHeader(stringResource(R.string.secure_mode)) }
            item {
                Column {
                    SecureModeRow(
                        checked = settings.secureMode,
                        onCheckedChange = { enabled ->
                            if (enabled && !settings.secureMode) {
                                showSecureConfirm = true
                            } else {
                                viewModel.setSecureMode(enabled)
                            }
                        },
                    )
                    if (settings.secureMode) {
                        SecureOptionRow(
                            label = stringResource(R.string.secure_taps_label),
                            value = settings.secureTapCount,
                            range = SettingsRepository.MIN_SECURE_TAPS..SettingsRepository.MAX_SECURE_TAPS,
                            decreaseLabel = stringResource(R.string.secure_taps_decrease),
                            increaseLabel = stringResource(R.string.secure_taps_increase),
                            onChanged = viewModel::setSecureTapCount,
                        )
                        SecureOptionRow(
                            label = stringResource(R.string.secure_reset_seconds_label),
                            value = settings.secureResetSeconds,
                            range = SettingsRepository.MIN_SECURE_RESET_SECONDS..SettingsRepository.MAX_SECURE_RESET_SECONDS,
                            decreaseLabel = stringResource(R.string.secure_reset_seconds_decrease),
                            increaseLabel = stringResource(R.string.secure_reset_seconds_increase),
                            onChanged = viewModel::setSecureResetSeconds,
                        )
                    }
                }
            }

            item { SectionHeader(stringResource(R.string.speech)) }
            item {
                val ttsOptions = listOf(null as String?) + ttsEngines.map { it.first }
                SettingsDropdown(
                    label = stringResource(R.string.tts),
                    currentLabel = ttsEngineLabel ?: stringResource(R.string.tts_system_default),
                    options = ttsOptions,
                    optionLabel = { packageName ->
                        if (packageName == null) {
                            stringResource(R.string.tts_system_default)
                        } else {
                            ttsEngines.firstOrNull { it.first == packageName }?.second ?: packageName
                        }
                    },
                    onSelect = { packageName ->
                        viewModel.selectTtsEngine(packageName)
                        ttsEngineLabel = viewModel.ttsEngineLabel()
                    },
                )
                SpeechRateRow(
                    rate = settings.speechRate,
                    onRateChange = viewModel::setSpeechRate,
                )
                SpeechPitchRow(
                    pitch = settings.speechPitch,
                    onPitchChange = viewModel::setSpeechPitch,
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

            item { SectionHeader(stringResource(R.string.data)) }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { exportLauncher.launch(exportFileName) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Text(
                            text = stringResource(R.string.export_data),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    Button(
                        onClick = {
                            importLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Text(
                            text = stringResource(R.string.import_data),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }

            item { SectionHeader(stringResource(R.string.about)) }
            item {
                Button(
                    onClick = onOpenAbout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Default.Info, contentDescription = null)
                    Text(
                        text = stringResource(R.string.about_open),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }

    if (showSecureConfirm) {
        AlertDialog(
            onDismissRequest = { showSecureConfirm = false },
            title = { Text(stringResource(R.string.secure_mode)) },
            text = { Text(stringResource(R.string.secure_mode_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setSecureMode(true)
                    showSecureConfirm = false
                }) {
                    Text(stringResource(R.string.accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSecureConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text(stringResource(R.string.import_data)) },
            text = { Text(stringResource(R.string.import_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importData(uri)
                    pendingImportUri = null
                }) {
                    Text(stringResource(R.string.accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
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
private fun <T> SettingsDropdown(
    label: String,
    currentLabel: String,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp)) {
        OutlinedTextField(
            value = currentLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true },
        )
    }
}

@Composable
private fun SecureModeRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = stringResource(R.string.secure_mode_toggle), style = MaterialTheme.typography.bodyLarge)
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SecureOptionRow(
    label: String,
    value: Int,
    range: IntRange,
    decreaseLabel: String,
    increaseLabel: String,
    onChanged: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Stepper(
            value = value,
            range = range,
            decreaseLabel = decreaseLabel,
            increaseLabel = increaseLabel,
            onChanged = onChanged,
        )
    }
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
private fun audioOutputLabel(id: String, outputs: List<Pair<String, String>>): String =
    if (id == AudioOutputIds.AUTO) {
        stringResource(R.string.audio_output_auto)
    } else {
        outputs.firstOrNull { it.first == id }?.second ?: id
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
