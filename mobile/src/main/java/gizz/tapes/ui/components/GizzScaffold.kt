package gizz.tapes.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import gizz.tapes.R
import gizz.tapes.data.Title
import gizz.tapes.ui.player.PlayerError
import gizz.tapes.util.LCE
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> GizzScaffold(
    title: Title,
    state: LCE<T, Any>,
    upClick: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable (value: T, playerError: (PlayerError) -> Unit) -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = { TopAppBarText(title) },
                navigationIcon = {
                    upClick?.let {
                        IconButton(onClick = upClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.navigate_back)
                            )
                        }

                    } ?: run {
                        AsyncImage(
                            model = R.raw.gizz_tapes_logo,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                },
                actions = actions
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when(state) {
                is LCE.Error -> ErrorScreen(state.userDisplayedMessage)
                is LCE.Content -> content(state.value) {
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
