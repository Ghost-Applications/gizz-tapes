package gizz.tapes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import gizz.tapes.util.LCE
import gizz.tapes.R
import gizz.tapes.data.PosterUrl
import gizz.tapes.data.Subtitle
import gizz.tapes.data.Title
import gizz.tapes.ui.player.MiniPlayer
import gizz.tapes.ui.player.PlayerState

data class SelectionData(
    val title: Title,
    val subtitle: Subtitle,
    val posterUrl: PosterUrl,
    val onClick: () -> Unit
)

@Composable
fun SelectionScreen(
    title: Title = Title(stringResource(R.string.app_name)),
    state: LCE<List<SelectionData>, Any>,
    playerState: PlayerState,
    onPauseAction: () -> Unit,
    onPlayAction: () -> Unit,
    upClick: (() -> Unit)?,
    onMiniPlayerClick: (Title) -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    GizzScaffold(
        title = title,
        state = state,
        upClick = upClick,
        actions = actions
    ) { value, playerError ->
        Column {
            SelectionList(
                Modifier.weight(1f),
                value
            )
            MiniPlayer(
                playerState = playerState,
                onClick = onMiniPlayerClick,
                onPauseAction = onPauseAction,
                onPlayAction = onPlayAction,
                playerError = playerError
            )
        }
    }
}

@Composable
fun SelectionList(
    modifier: Modifier = Modifier,
    data: List<SelectionData>,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        items(data) { (title, subtitle, imageUrl, onClick) ->
            SelectionRow(
                title = title,
                subtitle = subtitle,
                posterUrl = imageUrl,
                onClick = onClick
            )
        }
    }
}

@Composable
fun SelectionRow(
    title: Title,
    subtitle: Subtitle,
    posterUrl: PosterUrl,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .fillMaxWidth()
            .requiredHeight(96.dp)
            .height(IntrinsicSize.Max)
            .clickable {
                onClick()
            }
    ) {
        AsyncImage(
            model = posterUrl.value,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight()
        )

        Column(
            modifier = Modifier
                .padding(8.dp)
                .align(Alignment.CenterVertically)
        ) {
            Text(
                text = title.value,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = subtitle.value,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
