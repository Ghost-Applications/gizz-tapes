package gizz.tapes.ui.years

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gizz.tapes.GizzTapesTheme
import gizz.tapes.data.Title
import gizz.tapes.data.YearSelectionData
import gizz.tapes.ui.components.ErrorScreen
import gizz.tapes.ui.components.GridItemCard
import gizz.tapes.ui.components.loadingCards
import gizz.tapes.util.LCE

@Composable
fun YearSelectionScreen(
    state: LCE<List<YearSelectionData>, Exception>,
    onYearClicked: YearClicked,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        state = rememberLazyGridState(),
        columns = GridCells.Adaptive(
            minSize = GizzTapesTheme.size.gridCellMinSize
        ),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        when (state) {
            is LCE.Content<List<YearSelectionData>> -> content(state.value, onYearClicked)
            is LCE.Error<Exception> -> item(span = { GridItemSpan(maxLineSpan) }) {
                ErrorScreen(state.error)
            }

            LCE.Loading -> loadingCards()
        }
    }
}

private fun LazyGridScope.content(
    years: List<YearSelectionData>,
    onYearClicked: YearClicked
) {
    items(years) { item ->
        GridItemCard(
            title = Title(item.year.value),
            subtitle = null,
            posterUrl = item.poster,
            onClick = { onYearClicked(item.year) }
        )
    }
}
