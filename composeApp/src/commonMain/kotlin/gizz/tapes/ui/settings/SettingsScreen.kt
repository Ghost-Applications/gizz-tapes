package gizz.tapes.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import dev.zacsweers.metrox.viewmodel.metroViewModel
import gizz.tapes.api.data.Recording
import gizz.tapes.lastfm.LastFmAuthState
import gizz.tapes.nav.NavigateUp
import gizz.tapes.ui.components.LoadingScreen
import gizz.tapes.ui.components.navigationUpIcon
import gizz.tapes.util.LC

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = metroViewModel(),
    navigateUp: NavigateUp,
) {
    val state by viewModel.settingsState.collectAsState()
    val uriHandler = LocalUriHandler.current
    SettingsScreen(
        state = state,
        onRecordingTypeSelected = viewModel::updatePreferredRecordingType,
        onVolumeBoostEnabledChanged = viewModel::updateVolumeBoostEnabled,
        onConnectLastFm = { viewModel.connectLastFm(uriHandler::openUri) },
        onCancelLastFm = viewModel::cancelLastFm,
        onDisconnectLastFm = viewModel::disconnectLastFm,
        navigateUp = navigateUp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: LC<SettingsScreenState>,
    onRecordingTypeSelected: (Recording.Type) -> Unit,
    onVolumeBoostEnabledChanged: (Boolean) -> Unit,
    onConnectLastFm: () -> Unit,
    onCancelLastFm: () -> Unit,
    onDisconnectLastFm: () -> Unit,
    navigateUp: NavigateUp,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                navigationIcon = navigationUpIcon(navigateUp)
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (state) {
                LC.Loading -> LoadingScreen()
                is LC.Content -> SettingsContent(
                    state = state.content,
                    onRecordingTypeSelected = onRecordingTypeSelected,
                    onVolumeBoostEnabledChanged = onVolumeBoostEnabledChanged,
                    onConnectLastFm = onConnectLastFm,
                    onCancelLastFm = onCancelLastFm,
                    onDisconnectLastFm = onDisconnectLastFm,
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    state: SettingsScreenState,
    onRecordingTypeSelected: (Recording.Type) -> Unit,
    onVolumeBoostEnabledChanged: (Boolean) -> Unit,
    onConnectLastFm: () -> Unit,
    onCancelLastFm: () -> Unit,
    onDisconnectLastFm: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Preferred recording type:")
            Spacer(Modifier.width(16.dp))
            Row(modifier = Modifier.clickable { showMenu = !showMenu }) {
                Text(state.selectedPreferredRecordingType.name)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    state.preferredRecordingSelections.forEach { type ->
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.MusicNote, null) },
                            text = { Text(type.name) },
                            onClick = {
                                onRecordingTypeSelected(type)
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "SBD: Soundboard · AUD: Audience recording · MTX: Matrix (blend of AUD/SBD sources)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (state.isVolumeBoostSupported) {
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Boost quiet recordings")
                Spacer(Modifier.width(16.dp))
                Switch(checked = state.volumeBoostEnabled, onCheckedChange = onVolumeBoostEnabledChanged)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Increases the volume of quiet recordings. May increase distortion.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(24.dp))
        LastFmSetting(
            state = state.lastFmState,
            onConnect = onConnectLastFm,
            onCancel = onCancelLastFm,
            onDisconnect = onDisconnectLastFm,
        )
    }
}

@Composable
private fun LastFmSetting(
    state: LastFmAuthState,
    onConnect: () -> Unit,
    onCancel: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text("Last.fm")
        Spacer(Modifier.width(16.dp))
        when (state) {
            LastFmAuthState.LoggedOut -> Button(onClick = onConnect) { Text("Connect") }
            LastFmAuthState.Authorizing -> OutlinedButton(onClick = onCancel) { Text("Cancel") }
            is LastFmAuthState.LoggedIn -> OutlinedButton(onClick = onDisconnect) { Text("Disconnect") }
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        text = when (state) {
            LastFmAuthState.LoggedOut -> "Scrobble what you listen to on Last.fm."
            LastFmAuthState.Authorizing -> "Approve Gizz Tapes in your browser, then come back here."
            is LastFmAuthState.LoggedIn -> "Scrobbling as ${state.name}."
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
