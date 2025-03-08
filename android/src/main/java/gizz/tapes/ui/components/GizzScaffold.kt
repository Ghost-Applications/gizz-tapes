package gizz.tapes.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import gizz.tapes.data.Title
import gizz.tapes.ui.nav.NavigateUp
import gizz.tapes.ui.player.PlayerError
import gizz.tapes.util.LCE
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> GizzScaffold(
    title: Title,
    state: LCE<T, Any>,
    navigateUp: (NavigateUp)?,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable (value: T, playerError: (PlayerError) -> Unit) -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val titleComposable: @Composable () -> Unit = { TopAppBarText(title) }

    val appBar: @Composable () -> Unit = {
        TopAppBar(
            title = titleComposable,
            navigationIcon = if (navigateUp == null) gizzIcon() else navigationUpIcon(navigateUp),
            actions = actions
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        topBar = appBar,
    ) { innerPadding ->
        AnimatedContent(
            label = "Gizz Scaffold",
            targetState = state,
            contentKey = { current ->
                when(current) {
                    is LCE.Content -> "Content"
                    is LCE.Error -> "Error"
                    LCE.Loading -> "Loading"
                }
            },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) { s ->
            when(s) {
                is LCE.Error -> ErrorScreen(s.userDisplayedMessage)
                is LCE.Content -> content(s.value) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            it.message,
                            duration = SnackbarDuration.Long
                        )
                    }
                }
                LCE.Loading -> LoadingScreen()
            }
        }
    }
}
