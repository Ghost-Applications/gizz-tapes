package gizz.tapes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import gizz.tapes.GizzTapesTheme
import gizz.tapes.data.PosterUrl
import gizz.tapes.data.Subtitle
import gizz.tapes.data.Title

fun interface GridItemCardClick {
    operator fun invoke()
}

@Composable
fun GridItemCard(
    title: Title,
    subtitle: Subtitle?,
    posterUrl: PosterUrl,
    modifier: Modifier = Modifier,
    onClick: GridItemCardClick,
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick::invoke),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.height(GizzTapesTheme.size.gridCardSize).fillMaxWidth()) {
            PosterImage(
                modifier = Modifier.matchParentSize(),
                posterUrl = posterUrl,
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier.matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.80f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = title.value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                subtitle?.let {
                    Text(
                        text = subtitle.value,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}
