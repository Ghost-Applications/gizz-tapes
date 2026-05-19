package gizz.tapes.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import co.touchlab.kermit.Logger
import coil3.compose.AsyncImage
import gizz.tapes.data.PosterUrl
import gizz_tapes.composeapp.generated.resources.Res
import gizz_tapes.composeapp.generated.resources.missing
import org.jetbrains.compose.resources.painterResource

private val logger = Logger.withTag("PosterImage")

@Composable
fun PosterImage(
    posterUrl: PosterUrl?,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = posterUrl?.value,
        contentDescription = contentDescription,
        contentScale = contentScale,
        onError = {
            logger.e(it.result.throwable) {
                "Error loading image: ${it.result.request.data}"
            }
        },
        error = painterResource(Res.drawable.missing),
        modifier = modifier
    )
}
