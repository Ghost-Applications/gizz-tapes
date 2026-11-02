package gizz.tapes.ui.show

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.ShowId
import gizz.tapes.nav.NavigateUp
import gizz.tapes.ui.components.ErrorScreen
import gizz.tapes.ui.components.LoadingScreen
import gizz.tapes.ui.components.navigationUpIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowLinkScreen(
    viewModel: ShowLinkViewModel = assistedMetroViewModel(),
    navigateUp: NavigateUp,
    onShowResolved: (ShowId, FullShowTitle) -> Unit,
    onViewDownloadsClicked: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        (state as? ShowLinkViewModel.State.Resolved)?.let { onShowResolved(it.id, it.title) }
    }

    Scaffold(
        topBar = { TopAppBar(title = {}, navigationIcon = navigationUpIcon(navigateUp)) }
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (val s = state) {
            ShowLinkViewModel.State.Loading,
            is ShowLinkViewModel.State.Resolved -> LoadingScreen(modifier)

            is ShowLinkViewModel.State.Error -> ErrorScreen(
                error = s.error,
                modifier = modifier,
                onViewDownloadsClicked = onViewDownloadsClicked,
            )

            ShowLinkViewModel.State.NotFound -> Box(
                modifier = modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Show not found")
            }
        }
    }
}
