package gizz.tapes.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import dev.zacsweers.metrox.viewmodel.metroViewModel
import gizz.tapes.api.data.PartialShowData
import gizz.tapes.api.data.SetType
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.PosterUrl
import gizz.tapes.data.ShowId
import gizz.tapes.data.Subtitle
import gizz.tapes.data.Title
import gizz.tapes.ui.components.ErrorScreen
import gizz.tapes.ui.components.SelectionRow
import gizz.tapes.ui.components.ShowClick
import gizz.tapes.ui.components.loadingRows
import gizz.tapes.util.LC
import gizz.tapes.util.LCE
import gizz.tapes.util.showTitle

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = metroViewModel(),
    onShowClicked: ShowClick,
) {
    val setTypes by viewModel.state.collectAsState()
    SearchScreen(
        modifier = modifier,
        state = setTypes,
        onSetTypeSelection = { viewModel.toggleSetTypeId(it) },
        onSearch = { viewModel.updateSearchQuery(it) },
        onShowClicked = onShowClicked
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    state: SearchViewModel.State,
    onSetTypeSelection: (Int) -> Unit,
    onSearch: (String) -> Unit,
    onShowClicked: ShowClick,
) {
    var localQuery by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SearchBar(
            windowInsets = WindowInsets(0),
            inputField = {
                SearchBarDefaults.InputField(
                    query = localQuery,
                    onQueryChange = { localQuery = it },
                    onSearch = onSearch,
                    expanded = false,
                    onExpandedChange = { },
                    placeholder = { Text("Search") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    trailingIcon = {
                        if (localQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                localQuery = ""
                                onSearch("")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear"
                                )
                            }
                        }
                    }
                )
            },
            expanded = false,
            onExpandedChange = { },
            content = { }
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            contentPadding = PaddingValues(
                vertical = 16.dp,
                horizontal = 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                when (state.setTypes) {
                    is LC.Content<List<SetType>> -> {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.setTypes.content.fastForEach {
                                FilterChip(
                                    selected = it.id in state.selectedSetTypeIds,
                                    text = "${it.name} (${it.showCount})",
                                    onClick = { onSetTypeSelection(it.id) },
                                )
                            }
                        }
                    }

                    LC.Loading -> Unit
                }
            }
            when (val searchResults = state.searchResults) {
                is LCE.Content<List<PartialShowData>> -> {
                    items(searchResults.value) {
                        SelectionRow(
                            title = Title(it.showTitle),
                            subtitle = Subtitle(it.date),
                            posterUrl = PosterUrl(it.posterUrl),
                            onClick = {
                                onShowClicked(
                                    showId = ShowId(it.id),
                                    showTitle = FullShowTitle(Title(it.showTitle), date = it.date)
                                )
                            }
                        )
                    }
                }

                is LCE.Error<Exception> -> item { ErrorScreen(searchResults.error) }
                LCE.Loading -> loadingRows()
            }
        }
    }
}

@Composable
private fun FilterChip(
    selected: Boolean,
    text: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, contentDescription = "selected") }
        } else null
    )
}
