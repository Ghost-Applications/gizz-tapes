package gizz.tapes.ui.venue

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import gizz.tapes.GizzTapesTheme
import gizz.tapes.ui.components.ErrorScreen
import gizz.tapes.ui.components.GridItemCard
import gizz.tapes.ui.components.loadingCards
import gizz.tapes.util.LCE

fun interface CountryClick {
    operator fun invoke(
        countryId: UInt,
        countryName: String,
    )
}

@Composable
fun CountrySelectionScreen(
    modifier: Modifier = Modifier,
    viewModel: CountrySelectionViewModel = assistedMetroViewModel(),
    onCountryClick: CountryClick,
) {
    val state by viewModel.countriesState.collectAsState()

    CountrySelectionScreen(
        modifier = modifier,
        state = state,
        onCountryClick = onCountryClick
    )
}

@Composable
fun CountrySelectionScreen(
    modifier: Modifier = Modifier,
    state: LCE<List<CountrySelectionData>, Exception>,
    onCountryClick: CountryClick,
) {
    LazyVerticalGrid(
        state = rememberLazyGridState(),
        columns = GridCells.Adaptive(minSize = GizzTapesTheme.size.gridCellMinSize),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        when (state) {
            is LCE.Content<List<CountrySelectionData>> -> content(state.value, onCountryClick)
            is LCE.Error<Exception> -> item(span = { GridItemSpan(maxLineSpan) }) {
                ErrorScreen(state.error)
            }

            LCE.Loading -> loadingCards()
        }
    }
}

private fun LazyGridScope.content(
    countries: List<CountrySelectionData>,
    onCountryClick: CountryClick,
) {
    items(countries) { item ->
        GridItemCard(
            title = item.title,
            subtitle = item.subtitle,
            posterUrl = item.posterUrl,
            onClick = {
                onCountryClick(
                    countryId = item.countryId,
                    countryName = item.title.value
                )
            }
        )
    }
}
