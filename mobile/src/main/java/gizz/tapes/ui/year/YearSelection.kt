package gizz.tapes.ui.year

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.SortOrder
import gizz.tapes.data.Subtitle
import gizz.tapes.data.Title
import gizz.tapes.data.Year
import gizz.tapes.ui.components.CastButton
import gizz.tapes.ui.components.SelectionData
import gizz.tapes.ui.components.SelectionScreen
import gizz.tapes.ui.player.PlayerState
import gizz.tapes.ui.player.PlayerViewModel
import gizz.tapes.util.LCE
import gizz.tapes.util.map
import gizz.tapes.util.mapCollection

@OptIn(UnstableApi::class)
@Composable
fun YearSelectionScreen(
    viewModel: YearSelectionViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    onYearClicked: (year: Year) -> Unit,
    onMiniPlayerClick: (FullShowTitle) -> Unit,
    navigateToAboutPage: () -> Unit,
    navigateToSettingsPage: () -> Unit,
) {
    val state: LCE<List<YearSelectionData>, Throwable> by viewModel.years.collectAsState()
    val playerState by playerViewModel.playerState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    val sortOrder: SortOrder by viewModel.sortOrder.collectAsState()

    YearSelectionScreen(
        yearData = state,
        sortOrder = sortOrder,
        onYearClicked = onYearClicked,
        onMiniPlayerClick = onMiniPlayerClick,
        playerState = playerState,
        onPauseAction = playerViewModel::pause,
        onPlayAction = playerViewModel::play,
        actions = {
            IconButton(
                onClick = { viewModel.updateSortOrder(!sortOrder) }
            ) {
                Icon(
                    imageVector = Icons.Default.SortByAlpha,
                    contentDescription = "Sort By Year"
                )
            }

            CastButton()

            IconButton(onClick = { showMenu = !showMenu }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Options"
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.Info, null) },
                    onClick = {
                        showMenu = false
                        navigateToAboutPage()
                    },
                    text = { Text("About") }
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.Settings, null) },
                    onClick = {
                        showMenu = false
                        navigateToSettingsPage()
                    },
                    text = { Text("Settings") }
                )
            }
        }
    )
}

@Composable
fun YearSelectionScreen(
    yearData: LCE<List<YearSelectionData>, Throwable>,
    sortOrder: SortOrder,
    onYearClicked: (year: Year) -> Unit,
    onMiniPlayerClick: (title: FullShowTitle) -> Unit,
    playerState: PlayerState,
    onPauseAction: () -> Unit,
    onPlayAction: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    val selectionData = yearData.mapCollection {
        SelectionData(
            title = Title(it.year.value),
            subtitle = Subtitle("${it.showCount} shows"),
            posterUrl = it.randomShowPoster,
        ) {
            onYearClicked(it.year)
        }
    }.let {
        when(sortOrder) {
            SortOrder.Ascending -> it
            SortOrder.Descending -> it.map { data -> data.reversed() }
        }
    }

    SelectionScreen(
        state = selectionData,
        navigateUp = null,
        onMiniPlayerClick = onMiniPlayerClick,
        playerState = playerState,
        onPauseAction = onPauseAction,
        onPlayAction = onPlayAction,
        actions = actions
    )
}
