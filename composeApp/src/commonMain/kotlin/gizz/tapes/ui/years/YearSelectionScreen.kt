package gizz.tapes.ui.years

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
import dev.zacsweers.metrox.viewmodel.metroViewModel
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.SortOrder
import gizz.tapes.data.Subtitle
import gizz.tapes.data.Title
import gizz.tapes.data.Year
import gizz.tapes.data.YearSelectionData
import gizz.tapes.ui.components.SelectionData
import gizz.tapes.ui.components.SelectionScreen
import gizz.tapes.ui.player.PlayerState
import gizz.tapes.ui.player.PlayerViewModel
import gizz.tapes.util.LCE
import gizz.tapes.util.map
import gizz.tapes.util.mapCollection

@Composable
fun YearSelectionScreen(
    viewModel: YearSelectionViewModel = metroViewModel(),
    playerViewModel: PlayerViewModel = metroViewModel(),
    onYearClicked: (Year) -> Unit,
    onMiniPlayerClick: (FullShowTitle) -> Unit,
    onAboutClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val state by viewModel.years.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val playerState by playerViewModel.playerState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    YearSelectionScreen(
        yearData = state,
        sortOrder = sortOrder,
        playerState = playerState,
        onYearClicked = onYearClicked,
        onMiniPlayerClick = onMiniPlayerClick,
        onPauseAction = playerViewModel::pause,
        onPlayAction = playerViewModel::play,
        actions = {
            IconButton(onClick = { viewModel.updateSortOrder(!sortOrder) }) {
                Icon(Icons.Default.SortByAlpha, contentDescription = "Sort")
            }
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More")
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("About") },
                    onClick = {
                        showMenu = false
                        onAboutClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Settings") },
                    onClick = {
                        showMenu = false
                        onSettingsClick()
                    }
                )
            }
        }
    )
}

@Composable
fun YearSelectionScreen(
    yearData: LCE<List<YearSelectionData>, Throwable>,
    sortOrder: SortOrder,
    playerState: PlayerState,
    onYearClicked: (year: Year) -> Unit,
    onMiniPlayerClick: (title: FullShowTitle) -> Unit,
    onPauseAction: () -> Unit,
    onPlayAction: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    val selectionData = remember(yearData) {
        yearData.mapCollection {
            SelectionData(
                title = Title(it.year.value),
                subtitle = Subtitle("${it.showCount} shows"),
                posterUrl = it.randomShowPoster,
            ) { onYearClicked(it.year) }
        }.let {
            when (sortOrder) {
                SortOrder.Ascending -> it
                SortOrder.Descending -> it.map { data -> data.reversed() }
            }
        }
    }

    SelectionScreen(
        state = selectionData,
        navigateUp = null,
        onMiniPlayerClick = onMiniPlayerClick,
        onPauseAction = onPauseAction,
        onPlayAction = onPlayAction,
        playerState = playerState,
        actions = actions
    )
}
