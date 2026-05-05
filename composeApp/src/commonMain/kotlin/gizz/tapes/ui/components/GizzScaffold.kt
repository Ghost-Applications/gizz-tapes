package gizz.tapes.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import gizz.tapes.LocalPlatformActions
import gizz.tapes.data.Title
import gizz.tapes.nav.NavigateUp
import gizz.tapes.ui.player.PlayerError
import kotlinx.coroutines.launch

fun interface GizzScaffoldContent {
    @Composable
    operator fun invoke(innerPadding: PaddingValues, onPlaybackError: PlaybackError)
}

fun interface PlaybackError {
    operator fun invoke(error: PlayerError)
}

fun interface TopAppBar {
    @Composable
    operator fun invoke()
}

fun interface BottomBar {
    @Composable
    operator fun invoke()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GizzScaffold(
    title: Title,
    navigateUp: (NavigateUp)?,
    actions: @Composable RowScope.() -> Unit,
    content: GizzScaffoldContent
) {
    val platformActions = LocalPlatformActions.current

    val topAppBar: @Composable () -> Unit = {
        TopAppBar(
            title = { TopAppBarText(title) },
            navigationIcon = if (navigateUp == null) gizzIcon() else navigationUpIcon(navigateUp),
            actions = {
                platformActions()
                actions()
            }
        )
    }

    GizzScaffold(
        topAppBar = topAppBar,
        bottomBar = null,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GizzScaffold(
    topAppBar: TopAppBar,
    bottomBar: BottomBar?,
    content: GizzScaffoldContent
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        topBar = topAppBar::invoke,
        bottomBar = bottomBar?.let { bottomBar::invoke } ?: {}
    ) { innerPadding ->
        content(innerPadding) { playerError ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = playerError.message,
                    duration = SnackbarDuration.Long
                )
            }
        }
    }
}
