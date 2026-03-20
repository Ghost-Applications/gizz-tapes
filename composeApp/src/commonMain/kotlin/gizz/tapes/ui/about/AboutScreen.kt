package gizz.tapes.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import gizz.tapes.nav.NavigateUp
import gizz.tapes.ui.components.navigationUpIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navigateUp: NavigateUp) {
    val linkStyles = TextLinkStyles(
        SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("About") },
                navigationIcon = navigationUpIcon(navigateUp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Gizz Tapes") }
                    append(" is your ultimate jukebox for live tapes of ")
                    withLink(LinkAnnotation.Url("https://kinggizzardandthelizardwizard.com/", linkStyles)) {
                        append("King Gizzard and the Lizard Wizard")
                    }
                    append(" shows!")
                }
            )

            Text(
                buildAnnotatedString {
                    append("This ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("unofficial") }
                    append(" app has no affiliation with the band but is built with love for the community.")
                }
            )

            Text(
                buildAnnotatedString {
                    append("It utilizes the ")
                    withLink(LinkAnnotation.Url("https://tapes.kglw.net/api/docs/", linkStyles)) {
                        append("Gizz Tapes Api")
                    }
                    append(" to bring you a treasure trove of live recordings.")
                }
            )

            Text(
                buildAnnotatedString {
                    append(
                        "All audio is hosted by and streamed from the Internet Archive. Artwork, show notes and other metadata are obtained from "
                    )
                    withLink(LinkAnnotation.Url("http://kglw.net", linkStyles)) {
                        append("KGLW.net")
                    }
                    append(".")
                }
            )

            Text(
                buildAnnotatedString {
                    append("Want to receive notifications when we add a new show to the site? Follow Gizz Tapes on ")
                    withLink(LinkAnnotation.Url("https://bsky.app/profile/tapes.kglw.net", linkStyles)) {
                        append("Bluesky")
                    }
                    append(", or check out our handy ")
                    withLink(LinkAnnotation.Url("https://tapes.kglw.net/feed.xml", linkStyles)) {
                        append("Atom")
                    }
                    append(" feed!")
                }
            )

            Text(
                buildAnnotatedString {
                    append("Please consider ")
                    withLink(LinkAnnotation.Url("https://archive.org/donate", linkStyles)) {
                        append("making a donation to the Internet Archive")
                    }
                    append(
                        ", without whom this website would not be possible, to help support all their important work preserving digital culture."
                    )
                }
            )

            Text("Get Involved", style = MaterialTheme.typography.titleLarge)

            Text(
                buildAnnotatedString {
                    append("Want to contribute? Check out the ")
                    withLink(LinkAnnotation.Url("https://github.com/Ghost-Applications/gizz-tapes", linkStyles)) {
                        append("GitHub project")
                    }
                    append("!")
                }
            )

            Text("• Report bugs")
            Text("• Explore the app's source code")
            Text("• Join the development conversation")

            Text("Feedback & Support", style = MaterialTheme.typography.titleLarge)

            Text(
                buildAnnotatedString {
                    append("For feature requests, bug reports, or general inquiries, feel free to email us at ")
                    withLink(LinkAnnotation.Url("mailto:ghost.apps.llc@gmail.com", linkStyles)) {
                        append("ghost.apps.llc@gmail.com")
                    }
                    append(". You can also connect with us on ")
                    withLink(LinkAnnotation.Url("https://www.reddit.com/user/Rough_Host8179/", linkStyles)) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Reddit") }
                    }
                    append(" or ")
                    withLink(LinkAnnotation.Url("https://bsky.app/profile/ghostapps.bsky.social", linkStyles)) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Blue Sky") }
                    }
                    append(" to share your thoughts or support the project.")
                }
            )

            Text("Show Your Love", style = MaterialTheme.typography.titleLarge)

            Text(
                "This app is a volunteer-driven passion project. If you enjoy it, let us know on Reddit or Blue Sky — your support keeps us going!"
            )
        }
    }
}
