package com.zazen.ui.screens.setup

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.zazen.R
import com.zazen.data.model.IntervalBell
import com.zazen.data.model.Preset
import com.zazen.data.model.Sound

/** Bell-placement ceiling for open-ended sits, which have no duration to bound it. */
private const val OPEN_ENDED_BELL_MAX_MINUTES = 240

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdleContent(
    onNavigateToStats: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val durationSeconds by viewModel.durationSeconds.collectAsState()
    val openEnded by viewModel.openEnded.collectAsState()
    val repeatEverySeconds by viewModel.repeatEverySeconds.collectAsState()
    val repeatSound by viewModel.repeatSound.collectAsState()
    val vibrateOnly by viewModel.vibrateOnly.collectAsState()
    val endSound by viewModel.endSound.collectAsState()
    val dndEnabled by viewModel.dndEnabled.collectAsState()
    val bells by viewModel.bells.collectAsState()
    val presets by viewModel.presets.collectAsState()
    val selectedPreset by viewModel.selectedPreset.collectAsState()

    var showAddBellDialog by remember { mutableStateOf(false) }
    var editingBellIndex by remember { mutableIntStateOf(-1) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var showNewPresetDialog by remember { mutableStateOf(false) }
    var showDurationEditor by remember { mutableStateOf(false) }
    var showRepeatEditor by remember { mutableStateOf(false) }
    var showVolumeWarning by remember { mutableStateOf(false) }
    var presetToDelete by remember { mutableStateOf<Preset?>(null) }

    // Notification permission (Android 13+)
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not — we start the timer either way */ }

    fun attemptStart() {
        // Volume warning: check alarm stream (our SoundPool uses USAGE_ALARM)
        if (!vibrateOnly) {
            val audioManager = context.getSystemService(AudioManager::class.java)
            val volume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            if (volume == 0) {
                showVolumeWarning = true
                return
            }
        }
        // Request notification permission if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // Check DND permission if enabled
        if (dndEnabled) {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (!nm.isNotificationPolicyAccessGranted) {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                return
            }
        }
        viewModel.startTimer()
    }

    var showBellSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
        ) {
            // Menu lives only on the idle screen: a running sit is fullscreen and bare.
            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(8.dp),
            ) {
                IconButton(onClick = onNavigateToStats) {
                    Icon(Icons.Default.History, stringResource(R.string.setup_cd_stats))
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, stringResource(R.string.setup_cd_settings))
                }
            }
            if (presets.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    presets.forEach { preset ->
                            val deleteLabel = stringResource(R.string.preset_cd_delete, preset.name)
                            FilterChip(
                                selected = selectedPreset?.id == preset.id,
                                onClick = { viewModel.loadPreset(preset) },
                                // Deleting via the tiny trailing icon is impractical with a
                                // screen reader, so expose it as a custom action on the chip.
                                modifier = Modifier.semantics {
                                    customActions = listOf(
                                        CustomAccessibilityAction(deleteLabel) {
                                            presetToDelete = preset
                                            true
                                        },
                                    )
                                },
                                label = {
                                    Text(
                                        preset.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                trailingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clearAndSetSemantics { }
                                            .clickable { presetToDelete = preset },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                },
                            )
                        }
                }
            }
        }

        // Same size and position as the running timer's ring, so pressing Start
        // reads as the dial coming to life rather than a screen change.
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                val spoken = if (openEnded) {
                    stringResource(R.string.duration_cd_unlimited)
                } else {
                    spokenDuration(durationSeconds)
                }
                DurationDial(
                    minutes = if (openEnded) 0f else durationSeconds / 60f,
                    onMinutesChange = { minutes ->
                        if (minutes == 0) {
                            viewModel.setOpenEnded(true)
                        } else {
                            viewModel.setOpenEnded(false)
                            viewModel.setDurationSeconds(minutes * 60)
                        }
                    },
                    contentDescription = stringResource(R.string.duration_label),
                    stateDescription = spoken,
                    modifier = Modifier.size(280.dp),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (openEnded) {
                        Text(
                            text = stringResource(R.string.duration_unlimited_symbol),
                            style = MaterialTheme.typography.displayLarge,
                            // The dial already announces "No time limit".
                            modifier = Modifier.clearAndSetSemantics { },
                        )
                        Text(
                            stringResource(R.string.mode_open),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clearAndSetSemantics { },
                        )
                    } else {
                        val editLabel = stringResource(R.string.duration_action_edit)
                        Text(
                            text = formatDuration(durationSeconds),
                            style = MaterialTheme.typography.displayMedium,
                            modifier = Modifier
                                .clickable(
                                    onClickLabel = editLabel,
                                    role = Role.Button,
                                ) { showDurationEditor = true }
                                .padding(8.dp)
                                .semantics { contentDescription = spoken },
                        )
                    }
                }
            }

            Spacer(Modifier.height(48.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(
                    onClick = { showBellSheet = true },
                    modifier = Modifier.size(64.dp),
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        stringResource(R.string.setup_cd_bells),
                        modifier = Modifier.size(32.dp),
                    )
                }
                // Start sits where Pause will be, and the row always holds three
                // buttons, so starting a sit swaps icons without moving anything.
                FilledIconButton(
                    onClick = { attemptStart() },
                    modifier = Modifier.size(64.dp),
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        stringResource(R.string.action_start),
                        modifier = Modifier.size(32.dp),
                    )
                }
                FilledTonalIconButton(
                    onClick = {
                        if (selectedPreset == null) showNewPresetDialog = true
                        else showSavePresetDialog = true
                    },
                    modifier = Modifier.size(64.dp),
                ) {
                    Icon(
                        Icons.Default.Save,
                        stringResource(R.string.preset_save_chip),
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }

    if (showBellSheet) {
        ModalBottomSheet(onDismissRequest = { showBellSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
            ) {
                Text(
                    stringResource(R.string.bells_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(16.dp))

                // --- End bell sound ---
                EndBellSelector(
                    selected = endSound,
                    onSelect = { viewModel.setEndSound(it) },
                    onPreview = { viewModel.previewSound(it) },
                )

                Spacer(Modifier.height(8.dp))

                // --- Repeating bell ---
                // The whole row is the target so the trailing label isn't a separate,
                // unlabelled ("Set") focus stop.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClickLabel = stringResource(R.string.repeat_bell_cd_configure),
                            role = Role.Button,
                        ) { showRepeatEditor = true }
                        .heightIn(min = 48.dp)
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Repeat, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(stringResource(R.string.repeat_bell_title))
                            Text(
                                if (repeatEverySeconds > 0) {
                                    stringResource(
                                        R.string.repeat_bell_summary,
                                        formatDuration(repeatEverySeconds),
                                        stringResource(repeatSound.displayNameRes),
                                    )
                                } else {
                                    stringResource(R.string.repeat_bell_off)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        if (repeatEverySeconds > 0) {
                            stringResource(R.string.action_change)
                        } else {
                            stringResource(R.string.action_set)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clearAndSetSemantics { }
                            .padding(horizontal = 12.dp),
                    )
                }

                Spacer(Modifier.height(8.dp))

                // --- Interval bells ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.interval_bells_label),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    TextButton(onClick = { showAddBellDialog = true }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.padding(end = 4.dp))
                        Text(stringResource(R.string.action_add))
                    }
                }

                bells.forEachIndexed { index, bell ->
                    BellItem(
                        bell = bell,
                        pastEnd = !openEnded && bell.triggerAtMillis >= durationSeconds * 1_000L,
                        onEdit = { editingBellIndex = index; showAddBellDialog = true },
                        onRemove = { viewModel.removeBell(index) },
                        onPreview = {
                            Sound.fromResId(bell.soundResId)?.let { viewModel.previewSound(it) }
                        },
                    )
                }
            }
        }
    }

    // --- Dialogs ---

    if (showDurationEditor) {
        DurationEditorDialog(
            currentSeconds = durationSeconds,
            onConfirm = { totalSec ->
                viewModel.setDurationSeconds(totalSec)
                showDurationEditor = false
            },
            onDismiss = { showDurationEditor = false },
        )
    }

    if (showAddBellDialog) {
        val editBell = if (editingBellIndex >= 0 && editingBellIndex < bells.size) bells[editingBellIndex] else null
        AddBellDialog(
            maxMinutes = if (openEnded) OPEN_ENDED_BELL_MAX_MINUTES else durationSeconds / 60,
            initialMinutes = editBell?.let { (it.triggerAtMillis / 60_000).toInt() },
            initialSound = editBell?.let { Sound.fromResId(it.soundResId) },
            onConfirm = { minutes, sound ->
                if (editingBellIndex >= 0) {
                    viewModel.updateBell(editingBellIndex, minutes, sound)
                } else {
                    viewModel.addBell(minutes, sound)
                }
                showAddBellDialog = false
                editingBellIndex = -1
            },
            onDismiss = { showAddBellDialog = false; editingBellIndex = -1 },
            onPreview = { viewModel.previewSound(it) },
        )
    }

    if (showRepeatEditor) {
        RepeatBellDialog(
            currentSeconds = repeatEverySeconds,
            currentSound = repeatSound,
            onConfirm = { seconds, sound ->
                viewModel.setRepeatEverySeconds(seconds)
                viewModel.setRepeatSound(sound)
                showRepeatEditor = false
            },
            onDismiss = { showRepeatEditor = false },
            onPreview = { viewModel.previewSound(it) },
        )
    }

    val presetToSave = selectedPreset
    if (showSavePresetDialog && presetToSave != null) {
        var saving by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { if (!saving) showSavePresetDialog = false },
            title = { Text("Save \"${presetToSave.name}\"?") },
            text = {
                Column {
                    Text("Overwrite this preset with your current settings, or save a new one.")
                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        saving = true
                        viewModel.overwriteSelectedPreset { message ->
                            saving = false
                            if (message == null) showSavePresetDialog = false else error = message
                        }
                    },
                    enabled = !saving,
                ) { Text("Overwrite") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showSavePresetDialog = false
                            showNewPresetDialog = true
                        },
                        enabled = !saving,
                    ) { Text("Save as new") }
                    TextButton(
                        onClick = { showSavePresetDialog = false },
                        enabled = !saving,
                    ) { Text("Cancel") }
                }
            },
        )
    }

    if (showNewPresetDialog) {
        SavePresetDialog(
            onSave = { name, onResult ->
                viewModel.saveNewPreset(name, onResult)
            },
            onDismiss = { showNewPresetDialog = false },
        )
    }

    if (showVolumeWarning) {
        AlertDialog(
            onDismissRequest = { showVolumeWarning = false },
            title = { Text(stringResource(R.string.volume_muted_title)) },
            text = { Text(stringResource(R.string.volume_muted_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showVolumeWarning = false
                    viewModel.startTimer()
                }) { Text(stringResource(R.string.action_start_anyway)) }
            },
            dismissButton = {
                TextButton(onClick = { showVolumeWarning = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    presetToDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { presetToDelete = null },
            title = { Text(stringResource(R.string.preset_delete_title)) },
            text = { Text(stringResource(R.string.preset_delete_message, preset.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePreset(preset)
                    presetToDelete = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { presetToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EndBellSelector(
    selected: Sound,
    onSelect: (Sound) -> Unit,
    onPreview: (Sound) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = stringResource(selected.displayNameRes)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(1f),
        ) {
            OutlinedTextField(
                value = selectedName,
                onValueChange = {},
                readOnly = true,
                // Labelling the field itself replaces the detached fixed-width
                // "End Bell" text, which clipped at large font scales.
                label = { Text(stringResource(R.string.end_bell_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                singleLine = true,
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                Sound.entries.forEach { sound ->
                    DropdownMenuItem(
                        text = { Text(stringResource(sound.displayNameRes)) },
                        onClick = {
                            onSelect(sound)
                            expanded = false
                        },
                    )
                }
            }
        }
        IconButton(onClick = { onPreview(selected) }) {
            Icon(
                Icons.Default.PlayCircle,
                stringResource(R.string.end_bell_cd_preview),
            )
        }
    }
}

@Composable
private fun BellItem(
    bell: IntervalBell,
    pastEnd: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onPreview: () -> Unit,
) {
    val timeLabel = formatBellTime(bell.triggerAtMillis)
    val soundName = Sound.fromResId(bell.soundResId)
        ?.let { stringResource(it.displayNameRes) }
        ?: stringResource(R.string.sound_unknown)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp)
                // Shortening the sit on the dial can leave bells beyond the end;
                // they'd ring during extra time, so flag rather than drop them.
                .alpha(if (pastEnd) 0.5f else 1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Notifications, null)
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) { },
            ) {
                Text(timeLabel)
                Text(
                    if (pastEnd) stringResource(R.string.interval_bell_past_end, soundName) else soundName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onPreview) {
                Icon(
                    Icons.Default.PlayCircle,
                    stringResource(R.string.interval_bell_cd_preview, timeLabel),
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    stringResource(R.string.interval_bell_cd_edit, timeLabel),
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    stringResource(R.string.interval_bell_cd_remove, timeLabel),
                )
            }
        }
    }
}

@Composable
private fun AddBellDialog(
    maxMinutes: Int,
    initialMinutes: Int? = null,
    initialSound: Sound? = null,
    onConfirm: (minutes: Int, sound: Sound) -> Unit,
    onDismiss: () -> Unit,
    onPreview: (Sound) -> Unit,
) {
    val isEditing = initialMinutes != null
    var sliderValue by remember {
        mutableFloatStateOf((initialMinutes ?: 5).toFloat().coerceIn(1f, maxMinutes.toFloat()))
    }
    var selectedSound by remember { mutableStateOf(initialSound ?: Sound.DEFAULT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEditing) stringResource(R.string.bell_dialog_edit_title)
                else stringResource(R.string.bell_dialog_add_title)
            )
        },
        text = {
            val timeLabel = stringResource(R.string.bell_time_from_start)
            val minutes = sliderValue.toInt()
            val spokenMinutes = pluralStringResource(R.plurals.minutes, minutes, minutes)
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(timeLabel)
                Spacer(Modifier.height(8.dp))
                Text(
                    pluralStringResource(R.plurals.minutes_short, minutes, minutes),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.clearAndSetSemantics { },
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 1f..maxMinutes.toFloat(),
                    steps = (maxMinutes - 2).coerceAtLeast(0),
                    modifier = Modifier.semantics {
                        contentDescription = timeLabel
                        stateDescription = spokenMinutes
                    },
                )

                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.sound_label),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))

                SoundPicker(
                    selected = selectedSound,
                    onSelect = { selectedSound = it },
                    onPreview = onPreview,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(sliderValue.toInt(), selectedSound) }) {
                Text(
                    if (isEditing) stringResource(R.string.action_save)
                    else stringResource(R.string.action_add)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/**
 * Radio list of bell sounds with a preview button per row. Shared by the interval
 * and repeating bell dialogs so the selection semantics and 48dp preview targets
 * only have to be right once.
 */
@Composable
private fun SoundPicker(
    selected: Sound,
    onSelect: (Sound) -> Unit,
    onPreview: (Sound) -> Unit,
) {
    Column(modifier = Modifier.selectableGroup()) {
        Sound.entries.forEach { sound ->
            val name = stringResource(sound.displayNameRes)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selected == sound,
                        role = Role.RadioButton,
                        onClick = { onSelect(sound) },
                    )
                    .heightIn(min = 48.dp)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selected == sound, onClick = null)
                Spacer(Modifier.width(4.dp))
                Text(name, modifier = Modifier.weight(1f))
                IconButton(onClick = { onPreview(sound) }) {
                    Icon(
                        Icons.Default.PlayCircle,
                        stringResource(R.string.sound_cd_preview, name),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RepeatBellDialog(
    currentSeconds: Int,
    currentSound: Sound,
    onConfirm: (seconds: Int, sound: Sound) -> Unit,
    onDismiss: () -> Unit,
    onPreview: (Sound) -> Unit,
) {
    var minutesText by remember {
        mutableStateOf(if (currentSeconds > 0) (currentSeconds / 60).toString() else "")
    }
    var selectedSound by remember { mutableStateOf(currentSound) }
    val parsedMinutes = minutesText.toIntOrNull() ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.repeat_bell_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(R.string.repeat_bell_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { minutesText = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text(stringResource(R.string.repeat_bell_interval_label)) },
                    placeholder = { Text(stringResource(R.string.repeat_bell_interval_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.sound_label),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))

                SoundPicker(
                    selected = selectedSound,
                    onSelect = { selectedSound = it },
                    onPreview = onPreview,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(parsedMinutes * 60, selectedSound) }) {
                Text(
                    if (parsedMinutes > 0) stringResource(R.string.action_save)
                    else stringResource(R.string.action_turn_off)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun SavePresetDialog(
    onSave: (String, (String?) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Save new preset") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text(stringResource(R.string.preset_name_label)) },
                    singleLine = true,
                    isError = error != null,
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    saving = true
                    onSave(name.trim()) { message ->
                        saving = false
                        if (message == null) onDismiss() else error = message
                    }
                },
                enabled = !saving && name.isNotBlank(),
            ) { Text("Save new") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun DurationEditorDialog(
    currentSeconds: Int,
    onConfirm: (totalSeconds: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var minutesText by remember { mutableStateOf((currentSeconds / 60).toString()) }
    var secondsText by remember { mutableStateOf((currentSeconds % 60).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.duration_editor_title)) },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { minutesText = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text(stringResource(R.string.duration_minutes_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    ":",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.clearAndSetSemantics { },
                )
                OutlinedTextField(
                    value = secondsText,
                    onValueChange = { secondsText = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text(stringResource(R.string.duration_seconds_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val m = minutesText.toIntOrNull() ?: 0
                    val s = (secondsText.toIntOrNull() ?: 0).coerceIn(0, 59)
                    val total = (m * 60 + s).coerceAtLeast(10)
                    onConfirm(total)
                },
            ) { Text(stringResource(R.string.action_set)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun formatDuration(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return when {
        h > 0 && s > 0 -> stringResource(R.string.duration_hms, h, m, s)
        h > 0 -> stringResource(R.string.duration_ms, h, m)
        s > 0 -> stringResource(R.string.duration_ms, m, s)
        else -> pluralStringResource(R.plurals.minutes_short, m, m)
    }
}

/** Digit clocks like "5:30" are read out character by character, so spell it out. */
@Composable
private fun spokenDuration(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    val parts = buildList {
        if (m > 0) add(pluralStringResource(R.plurals.minutes, m, m))
        if (s > 0) add(pluralStringResource(R.plurals.seconds, s, s))
    }
    return if (parts.isEmpty()) {
        pluralStringResource(R.plurals.minutes, 0, 0)
    } else {
        parts.joinToString(" ")
    }
}

@Composable
private fun formatBellTime(millis: Long): String {
    val totalMin = (millis / 60_000).toInt()
    return stringResource(
        R.string.interval_bell_at,
        pluralStringResource(R.plurals.minutes_short, totalMin, totalMin),
    )
}
