package gizz.tapes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import gizz.tapes.util.LCE
import gizz.tapes.R
import gizz.tapes.ui.player.MiniPlayer
import gizz.tapes.ui.player.PlayerState

data class SelectionData(
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)

@Composable
fun SelectionScreen(
    title: String = stringResource(R.string.app_name),
    state: LCE<List<SelectionData>, Any>,
    playerState: PlayerState,
    onPauseAction: () -> Unit,
    onPlayAction: () -> Unit,
    upClick: (() -> Unit)?,
    onMiniPlayerClick: (title: String) -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    NesScaffold(
        title = title,
        state = state,
        upClick = upClick,
        actions = actions
    ) { value ->
        Column {
            SelectionList(
                Modifier.weight(1f),
                value
            )
            MiniPlayer(
                playerState = playerState,
                onClick = onMiniPlayerClick,
                onPauseAction = onPauseAction,
                onPlayAction = onPlayAction
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
        items(data) { (title, subtitle, onClick) ->
            SelectionRow(
                title = title,
                subtitle = subtitle,
                onClick = onClick
            )
        }
    }
}

@Composable
fun SelectionRow(
    title: String,
    subtitle: String,
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
        // TODO Load poster image
        Box(modifier = Modifier
            .width(80.dp)
            .fillMaxHeight()
            .background(Color.Cyan))

        Column(
            modifier = Modifier.padding(8.dp)
                .align(Alignment.CenterVertically)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
