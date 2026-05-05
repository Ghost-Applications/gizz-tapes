package gizz.tapes.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import arrow.core.NonEmptyList
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import gizz.tapes.GizzTapesTheme
import gizz.tapes.data.ShowSelectionData
import gizz.tapes.ui.components.ErrorScreen
import gizz.tapes.ui.components.GridItemCard
import gizz.tapes.ui.components.HomeHeaderShimmer
import gizz.tapes.ui.components.ShowCardShimmer
import gizz.tapes.ui.components.ShowClick
import gizz.tapes.ui.components.TextShimmer
import gizz.tapes.util.LC
import gizz.tapes.util.LCE
import gizz.tapes.util.toSimpleFormat

@Composable
fun HomeScreen(
    gridState: LazyGridState,
    viewModel: HomeViewModel = assistedMetroViewModel(),
    onShowClicked: ShowClick
) {
    val state: HomeScreenData by viewModel.state.collectAsState()

    HomeScreen(
        gridState = gridState,
        state = state,
        reloadRandomShows = {
            viewModel.reloadRandomShows()
        },
        onShowClicked = onShowClicked
    )
}

@Composable
fun HomeScreen(
    gridState: LazyGridState,
    state: HomeScreenData,
    reloadRandomShows: ReloadRandomShows,
    onShowClicked: ShowClick
) {
    BoxWithConstraints {
        val columnWidth = GizzTapesTheme.size.gridCellMinSize
        val columnCount = ((maxWidth - 32.dp) / columnWidth).toInt().coerceAtLeast(1)

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(columnCount),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                when (val header = state.header) {
                    is LCE.Content<HomeHeader> -> HeroHeader(
                        header = header.value,
                        modifier = Modifier.fullBleed(horizontal = 16.dp)
                    )

                    is LCE.Error<*> -> ErrorHeaderImage(
                        modifier = Modifier.fullBleed(horizontal = 16.dp)
                            .height(GizzTapesTheme.size.headerSize)
                    )

                    LCE.Loading -> HomeHeaderShimmer(
                        modifier = Modifier.fullBleed(
                            horizontal = 16.dp
                        )
                    )
                }
            }

            when (val content = state.content) {
                is LCE.Content<HomeContent> -> content(
                    content = content.value,
                    reloadRandomShows = reloadRandomShows,
                    onShowClicked = onShowClicked,
                    columnCount = columnCount
                )

                is LCE.Error<Exception> -> item(span = { GridItemSpan(maxLineSpan) }) {
                    ErrorScreen(content.error)
                }

                LCE.Loading -> loading(columnCount)
            }
        }
    }
}

fun interface ReloadRandomShows {
    operator fun invoke()
}

private fun LazyGridScope.content(
    content: HomeContent,
    columnCount: Int,
    reloadRandomShows: ReloadRandomShows,
    onShowClicked: ShowClick,
) {
    val showsToDisplay = maxOf(4, columnCount)

    content.todayInGizztory?.let { gizztory ->
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionTitle(
                text = "Today in Gizztory - ${gizztory.todayDate.toSimpleFormat()}"
            )
        }
        items(gizztory.shows) { item ->
            GridItemCard(
                title = item.showTitle,
                subtitle = item.showSubTitle,
                posterUrl = item.posterUrl,
                onClick = { onShowClicked(item.showId, item.fullShowTitle) }
            )
        }
    }

    item(span = { GridItemSpan(maxLineSpan) }) {
        SectionTitle(text = "Latest Shows")
    }
    items(content.latestShows.take(showsToDisplay)) { item ->
        GridItemCard(
            title = item.showTitle,
            subtitle = item.showSubTitle,
            posterUrl = item.posterUrl,
            onClick = { onShowClicked(item.showId, item.fullShowTitle) }
        )
    }

    item(span = { GridItemSpan(maxLineSpan) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(
                text = "Random Shows",
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(reloadRandomShows::invoke) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    when (val randomShows = content.randomShows) {
        is LC.Content<NonEmptyList<ShowSelectionData>> -> items(
            randomShows.content.take(
                showsToDisplay
            )
        ) { item ->
            GridItemCard(
                title = item.showTitle,
                subtitle = item.showSubTitle,
                posterUrl = item.posterUrl,
                onClick = { onShowClicked(item.showId, item.fullShowTitle) }
            )
        }

        LC.Loading -> items(showsToDisplay) {
            ShowCardShimmer()
        }
    }
}

private fun LazyGridScope.loading(columnCount: Int) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        TextShimmer(
            text = "Latest Shows",
            style = MaterialTheme.typography.titleMedium,
        )
    }
    items(maxOf(4, columnCount)) {
        ShowCardShimmer()
    }

    // Random shows section
    item(span = { GridItemSpan(maxLineSpan) }) {
        TextShimmer(
            text = "Random Shows",
            style = MaterialTheme.typography.titleMedium,
        )
    }
    items(maxOf(4, columnCount)) {
        ShowCardShimmer()
    }
}

/**
 * Cancels the parent LazyGrid's horizontal contentPadding so this item spans
 * edge-to-edge. Pass the same dp value used in the grid's contentPadding.
 */
private fun Modifier.fullBleed(horizontal: Dp): Modifier = this.layout { measurable, constraints ->
    val extra = horizontal.roundToPx() * 2
    val newWidth = constraints.maxWidth + extra
    val placeable = measurable.measure(
        constraints.copy(minWidth = newWidth, maxWidth = newWidth)
    )
    // Report the original cell width to the grid; let the placeable render past it.
    layout(constraints.maxWidth, placeable.height) {
        placeable.place(-extra / 2, 0)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
    )
}
