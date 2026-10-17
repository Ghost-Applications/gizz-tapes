package gizz.tapes.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.zacsweers.metrox.viewmodel.metroViewModel
import gizz.tapes.api.data.Recording
import gizz.tapes.lastfm.LastFmAuthState
import gizz.tapes.nav.NavigateUp
import gizz.tapes.ui.components.LoadingScreen
import gizz.tapes.ui.components.navigationUpIcon
import gizz.tapes.util.LC
import gizz.tapes.util.LCE

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = metroViewModel(),
    navigateUp: NavigateUp,
) {
    val state by viewModel.settingsState.collectAsState()
    val uriHandler = LocalUriHandler.current
    SettingsScreen(
        state = state,
        onRecordingTypesRankedChanged = viewModel::updateRecordingTypesRanked,
        onMoveRecordingType = viewModel::moveRecordingType,
        onFavouriteTaperToggled = viewModel::toggleFavouriteTaper,
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
    onRecordingTypesRankedChanged: (Boolean) -> Unit,
    onMoveRecordingType: (from: Int, to: Int) -> Unit,
    onFavouriteTaperToggled: (String) -> Unit,
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
                    onRecordingTypesRankedChanged = onRecordingTypesRankedChanged,
                    onMoveRecordingType = onMoveRecordingType,
                    onFavouriteTaperToggled = onFavouriteTaperToggled,
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
    onRecordingTypesRankedChanged: (Boolean) -> Unit,
    onMoveRecordingType: (from: Int, to: Int) -> Unit,
    onFavouriteTaperToggled: (String) -> Unit,
    onVolumeBoostEnabledChanged: (Boolean) -> Unit,
    onConnectLastFm: () -> Unit,
    onCancelLastFm: () -> Unit,
    onDisconnectLastFm: () -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        item { SectionHeader("Preferred recording type", showDivider = false) }
        item {
            RecordingTypePreference(
                ranking = state.recordingTypeRanking,
                isRanked = state.rankedRecordingTypes != null,
                onRankedChanged = onRecordingTypesRankedChanged,
                onMove = onMoveRecordingType,
            )
        }

        item { SectionHeader("Favourite tapers") }
        item {
            Text(
                text = "When a show has several recordings of the same type, ones by your favourite tapers are picked first.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        when (val tapers = state.tapers) {
            LCE.Loading -> item {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is LCE.Error -> item {
                Text(
                    text = "Unable to load tapers. Check your connection and try again.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
            is LCE.Content -> items(tapers.value) { taper ->
                FavouriteTaperRow(
                    taper = taper,
                    isFavourite = taper.name in state.favouriteTapers,
                    onToggle = { onFavouriteTaperToggled(taper.name) }
                )
            }
        }

        item { SectionHeader("Last.fm") }
        item {
            LastFmSetting(
                state = state.lastFmState,
                onConnect = onConnectLastFm,
                onCancel = onCancelLastFm,
                onDisconnect = onDisconnectLastFm,
            )
        }

        if (state.isVolumeBoostSupported) {
            item { SectionHeader("Playback") }
            item {
                ListItem(
                    headlineContent = { Text("Boost quiet recordings") },
                    supportingContent = {
                        Text("Increases the volume of quiet recordings. May increase distortion.")
                    },
                    trailingContent = {
                        Switch(checked = state.volumeBoostEnabled, onCheckedChange = onVolumeBoostEnabledChanged)
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, showDivider: Boolean = true) {
    Column {
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        )
    }
}

@Composable
private fun RecordingTypePreference(
    ranking: List<Recording.Type>,
    isRanked: Boolean,
    onRankedChanged: (Boolean) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("No preference")
            Switch(checked = isRanked, onCheckedChange = onRankedChanged)
            Text("Rank preferred types")
        }

        if (isRanked) {
            ElevatedCard(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                ranking.forEachIndexed { index, type ->
                    ListItem(
                        leadingContent = { Text("${index + 1}.") },
                        headlineContent = { Text(type.name) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { onMove(index, index - 1) }, enabled = index > 0) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move ${type.name} up")
                                }
                                IconButton(
                                    onClick = { onMove(index, index + 1) },
                                    enabled = index < ranking.lastIndex
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move ${type.name} down")
                                }
                            }
                        }
                    )
                }
            }
        }

        Text(
            text = "SBD: Soundboard · AUD: Audience recording · MTX: Matrix (blend of AUD/SBD sources) · " +
                "Mixed: a mix of source types",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun FavouriteTaperRow(
    taper: TaperItem,
    isFavourite: Boolean,
    onToggle: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onToggle),
        headlineContent = { Text(taper.name) },
        supportingContent = { Text("${taper.uploadCount} uploads") },
        trailingContent = {
            Icon(
                imageVector = if (isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavourite) "Remove favourite" else "Add favourite",
                tint = if (isFavourite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
private fun LastFmSetting(
    state: LastFmAuthState,
    onConnect: () -> Unit,
    onCancel: () -> Unit,
    onDisconnect: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                when (state) {
                    LastFmAuthState.LoggedOut -> "Scrobble what you listen to on Last.fm."
                    LastFmAuthState.Authorizing -> "Approve Gizz Tapes in your browser, then come back here."
                    is LastFmAuthState.LoggedIn -> "Scrobbling as ${state.name}."
                }
            )
        },
        trailingContent = {
            when (state) {
                LastFmAuthState.LoggedOut -> Button(onClick = onConnect) { Text("Connect") }
                LastFmAuthState.Authorizing -> OutlinedButton(onClick = onCancel) { Text("Cancel") }
                is LastFmAuthState.LoggedIn -> OutlinedButton(onClick = onDisconnect) { Text("Disconnect") }
            }
        }
    )
}
