package gizz.tapes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import gizz.tapes.GizzTapesTheme
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.PosterUrl
import gizz.tapes.data.Subtitle
import gizz.tapes.data.Title
import gizz.tapes.nav.NavigateUp
import gizz.tapes.ui.player.MiniPlayer
import gizz.tapes.ui.player.PlayerActions
import gizz.tapes.ui.player.PlayerState
import gizz.tapes.util.LCE
import gizz_tapes.composeapp.generated.resources.Res
import gizz_tapes.composeapp.generated.resources.app_name
import org.jetbrains.compose.resources.stringResource

data class SelectionData(
    val title: Title,
    val subtitle: Subtitle,
    val posterUrl: PosterUrl,
    val onClick: () -> Unit
)

@Composable
fun SelectionScreen(
    title: Title = Title(stringResource(Res.string.app_name)),
    state: LCE<List<SelectionData>, Exception>,
    playerState: PlayerState,
    playerActions: PlayerActions,
    navigateUp: NavigateUp?,
    onMiniPlayerClick: (FullShowTitle) -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    GizzScaffold(
        title = title,
        navigateUp = navigateUp,
        actions = actions
    ) { innerPadding, playerError ->
        Column {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
                    .weight(1f).padding(
                        top = innerPadding.calculateTopPadding(),
                        start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                        end = innerPadding.calculateEndPadding(LayoutDirection.Ltr)
                    ),
                contentPadding = PaddingValues(
                    top = 16.dp,
                    bottom = 16.dp,
                    start = 8.dp,
                    end = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (state) {
                    is LCE.Content<List<SelectionData>> -> selectionList(state.value)
                    is LCE.Error<Exception> -> item {
                        ErrorScreen(
                            error = state.error,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    LCE.Loading -> loadingRows()
                }
            }
            MiniPlayer(
                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
                playerState = playerState,
                onClick = onMiniPlayerClick,
                playerActions = playerActions,
                playerError = playerError
            )
        }
    }
}

private fun LazyListScope.selectionList(
    data: List<SelectionData>,
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

@Composable
fun SelectionRow(
    title: Title,
    subtitle: Subtitle,
    posterUrl: PosterUrl,
    onClick: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(GizzTapesTheme.size.selectionRowCorner))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .fillMaxWidth()
                .height(GizzTapesTheme.size.selectionRowSize)
                .clickable(onClick = onClick)
        ) {
            PosterImage(
                posterUrl = posterUrl,
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
                    style = MaterialTheme.typography.titleMedium,
                    autoSize = TextAutoSize.StepBased(
                        maxFontSize = MaterialTheme.typography.titleMedium.fontSize
                    ),
                    modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally),
                )
                Text(
                    text = subtitle.value,
                    style = MaterialTheme.typography.titleSmall,
                    autoSize = TextAutoSize.StepBased(
                        maxFontSize = MaterialTheme.typography.titleSmall.fontSize
                    ),
                    modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}
