package gizz.tapes.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.zacsweers.metrox.viewmodel.metroViewModel
import gizz.tapes.api.data.Recording
import gizz.tapes.nav.NavigateUp
import gizz.tapes.ui.components.LoadingScreen
import gizz.tapes.ui.components.navigationUpIcon
import gizz.tapes.util.LCE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = metroViewModel(),
    navigateUp: NavigateUp,
) {
    val state by viewModel.settingsState.collectAsState()
    SettingsScreen(
        state = state,
        onRecordingTypeSelected = viewModel::updatePreferredRecordingType,
        navigateUp = navigateUp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: LCE<SettingsScreenState, Nothing>,
    onRecordingTypeSelected: (Recording.Type) -> Unit,
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
                is LCE.Error -> error("This should never happen: $state")
                LCE.Loading -> LoadingScreen()
                is LCE.Content -> SettingsContent(
                    state = state.value,
                    onRecordingTypeSelected = onRecordingTypeSelected
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    state: SettingsScreenState,
    onRecordingTypeSelected: (Recording.Type) -> Unit,
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
    }
}
