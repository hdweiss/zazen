package com.zazen.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.zazen.R
import com.zazen.ui.screens.setup.SetupViewModel

/**
 * Set-and-forget options, kept off the setup screen so it stays focused on the
 * sit itself. Takes the setup screen's [SetupViewModel] (scoped to its back
 * stack entry) so the values Start reads are never stale.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SetupViewModel,
    onBack: () -> Unit,
) {
    val vibrateOnly by viewModel.vibrateOnly.collectAsState()
    val bellVolume by viewModel.bellVolume.collectAsState()
    val dndEnabled by viewModel.dndEnabled.collectAsState()
    val screenAlwaysOn by viewModel.screenAlwaysOn.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.stats_cd_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(stringResource(R.string.settings_section_sound))

            // Stacked rather than a fixed-width label + slider row so it survives
            // large font scales.
            val volumePercent = (bellVolume * 100).toInt()
            val volumeLabel = stringResource(R.string.bell_volume_label)
            val volumeState = stringResource(R.string.bell_volume_percent, volumePercent)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(volumeLabel)
                Text(
                    volumeState,
                    style = MaterialTheme.typography.bodySmall,
                    // The slider already announces the percentage.
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
            Slider(
                value = bellVolume,
                onValueChange = { viewModel.setBellVolume(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = volumeLabel
                        stateDescription = volumeState
                    },
            )

            SwitchRow(
                icon = if (vibrateOnly) Icons.Default.Vibration else Icons.AutoMirrored.Filled.VolumeUp,
                title = stringResource(R.string.toggle_vibrate_title),
                subtitle = stringResource(R.string.toggle_vibrate_subtitle),
                checked = vibrateOnly,
                onToggle = { viewModel.toggleVibrateOnly() },
            )

            SectionHeader(stringResource(R.string.settings_section_during_sit))

            SwitchRow(
                icon = Icons.Default.DoNotDisturbOn,
                title = stringResource(R.string.toggle_dnd_title),
                checked = dndEnabled,
                onToggle = { viewModel.toggleDnd() },
            )
            SwitchRow(
                icon = Icons.Default.Brightness7,
                title = stringResource(R.string.toggle_screen_on_title),
                checked = screenAlwaysOn,
                onToggle = { viewModel.toggleScreenAlwaysOn() },
            )

            SectionHeader(stringResource(R.string.theme_title))

            Column(modifier = Modifier.selectableGroup()) {
                listOf(
                    "system" to stringResource(R.string.theme_system),
                    "light" to stringResource(R.string.theme_light),
                    "dark" to stringResource(R.string.theme_dark),
                    "zen" to stringResource(R.string.theme_zen),
                ).forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = themeMode == value,
                                role = Role.RadioButton,
                                onClick = { viewModel.setThemeMode(value) },
                            )
                            .heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = themeMode == value, onClick = null)
                        Spacer(Modifier.width(16.dp))
                        Text(label)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onToggle: () -> Unit,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = { onToggle() },
            )
            .heightIn(min = 56.dp)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
