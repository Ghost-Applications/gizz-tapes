package gizz.tapes.ui.menu

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import gizz.tapes.R
import gizz.tapes.ui.components.navigationUpIcon
import gizz.tapes.ui.theme.GizzTheme
import io.noties.markwon.Markwon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navigateUpClick: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.about)) },
                navigationIcon = navigationUpIcon(navigateUpClick)
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
                         markwon.setMarkdown(
                            tv,
"The Gizz Tapes Android app is a jukebox for tapes of [King Gizzard and the Lizard Wizard](https://kinggizzardandthelizardwizard.com/) shows! It\\'s a **strictly unofficial** app with no ties to the band. This app uses the [Gizz Tapes Api](https://tapes.kglw.net/api/docs/) to get all it\\'s data! All audio is hosted by and streamed from the [Internet Archive](https://archive.org/), and artwork, show notes and other metadata are obtained from [KGLW.net](https://kglw.net/).\n" +
"\n" +
"If you'd like to get involved with the project check out the github project! You can report bugs and take a look at all the code used for this project there!\n" +
"\n" +
"If you have feature requests, would like to report bugs, or other inquiries shoot me an email at [gizztapes@andrew.cash](mailto:gizztapes@andrew.cash)"
                        )
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
        AboutScreen {}
    }
}