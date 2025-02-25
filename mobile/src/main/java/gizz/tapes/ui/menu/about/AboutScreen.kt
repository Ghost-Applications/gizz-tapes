package gizz.tapes.ui.menu.about

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import gizz.tapes.R
import gizz.tapes.ui.components.navigationUpIcon
import gizz.tapes.ui.nav.NavigateUp
import gizz.tapes.ui.theme.GizzTheme
import io.noties.markwon.Markwon

@Composable
fun AboutScreen(
    viewModel: AboutViewModel = hiltViewModel(),
    navigateUp: NavigateUp
) {
    val aboutText by viewModel.aboutText.collectAsState()
    AboutScreen(aboutText, navigateUp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    aboutText: AboutText,
    navigateUp: NavigateUp
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.about)) },
                navigationIcon = navigationUpIcon(navigateUp)
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AndroidView(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxSize(),
                factory = { context ->
                    val markwon = Markwon.create(context)
                    TextView(context).also { tv ->
                        markwon.setMarkdown(tv, aboutText.value)
                    }
                }
            )
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun AboutScreenPreview() {
    GizzTheme {
        AboutScreen(
            AboutText("This is an about text")
        ) {

        }
    }
}
