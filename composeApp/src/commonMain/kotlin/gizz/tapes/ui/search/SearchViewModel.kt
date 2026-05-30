package gizz.tapes.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import gizz.tapes.api.GizzTapesApiClient
import gizz.tapes.api.data.PartialShowData
import gizz.tapes.api.data.ShowTag
import gizz.tapes.util.ForViewModel
import gizz.tapes.util.LC
import gizz.tapes.util.LCE
import gizz.tapes.util.retryUntilSuccessful
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@Inject
@ContributesIntoMap(AppScope::class)
@ViewModelKey
class SearchViewModel(
    private val apiClient: GizzTapesApiClient
) : ViewModel() {
    private val logger = Logger.withTag("SearchViewModel")

    data class State(
        val showTags: LC<List<ShowTag>>,
        val selectedSetTypeIds: Set<UInt>,
        val searchResults: LCE<List<PartialShowData>, Exception>,
        val query: String,
    )

    private val showTagsLce = flow {
        val setsLce = retryUntilSuccessful(
            action = {
                apiClient.showTags()
            },
            onErrorAfter3SecondsAction = { error ->
                logger.e(error) { "Error loading set types" }
            }
        )
        emit(LC.Content(setsLce.value))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.ForViewModel,
        initialValue = LC.Loading
    )

    private val searchQueryStateFlow = MutableStateFlow("")
    private val searchShowTagIds = MutableStateFlow(emptySet<UInt>())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val searchResultsLce = combine(searchQueryStateFlow, searchShowTagIds) { searchQuery, showTagIds ->
        searchQuery to showTagIds
    }.flatMapLatest { (searchQuery, showTagIds) ->
        flow {
            val searchResultsLce = retryUntilSuccessful(
                action = {
                    apiClient.search(searchQuery, showTagIds)
                },
                onErrorAfter3SecondsAction = { error ->
                    logger.e(error) { "Error getting search results" }
                    emit(LCE.Error(error))
                }
            )
            emit(searchResultsLce)
        }
    }

    val state = combine(
        searchResultsLce,
        showTagsLce,
        searchShowTagIds,
        searchQueryStateFlow
    ) { search, showTags, selectedIds, query ->
        State(
            showTags = showTags,
            selectedSetTypeIds = selectedIds,
            searchResults = search,
            query = query,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.ForViewModel,
        initialValue = State(
            showTags = LC.Loading,
            selectedSetTypeIds = emptySet(),
            searchResults = LCE.Loading,
            query = ""
        )
    )

    fun toggleSetTypeId(showTypeId: UInt) {
        searchShowTagIds.update {
            if (it.contains(showTypeId)) {
                it - showTypeId
            } else {
                it + showTypeId
            }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQueryStateFlow.update { query }
    }
}
