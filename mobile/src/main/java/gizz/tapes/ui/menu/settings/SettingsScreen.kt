package gizz.tapes.ui.menu.settings

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import gizz.tapes.R
import gizz.tapes.ui.components.LoadingScreen
import gizz.tapes.ui.components.navigationUpIcon
import gizz.tapes.util.LCE

@Composable
fun SettingsScreen(
    viewModel: SettingScreenViewModel = hiltViewModel(),
    navigateUpClick: () -> Unit
) {

    val state by viewModel.settingsScreenState.collectAsState()
    SettingsScreen(
        state = state,
        onRecordingTypeSelected = viewModel::updatePreferredRecordingType,
        navigateUpClick = navigateUpClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: LCE<SettingsScreenState, Nothing>,
    onRecordingTypeSelected: RecordingTypeSelected,
    navigateUpClick: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = navigationUpIcon(navigateUpClick)
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when(state) {
                is LCE.Error -> error("This should never happen, $state")
                is LCE.Content -> SettingsScreenContent(state.value, onRecordingTypeSelected)
                LCE.Loading -> LoadingScreen()
            }
        }
    }
}

@Composable
private fun SettingsScreenContent(
    state: SettingsScreenState,
    onRecordingTypeSelected: RecordingTypeSelected
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Row {
            Text("Preferred recording type:")
            Spacer(modifier = Modifier.width(16.dp))
            Row(
                modifier = Modifier.clickable { showMenu = !showMenu }
            ) {
                Text(state.selectedPreferredRecordingType.name)
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {

                    state.preferredRecordingSelections.forEach {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.MusicNote, null) },
                            onClick = {
                                onRecordingTypeSelected(it)
                                showMenu = false
                            },
                            text = { Text(it.name) }
                        )
                    }
                }
            }
        }
    }
}
