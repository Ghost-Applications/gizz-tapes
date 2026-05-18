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
import gizz.tapes.api.data.SetType
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
        val setTypes: LC<List<SetType>>,
        val selectedSetTypeIds: Set<Int>,
        val searchResults: LCE<List<PartialShowData>, Exception>,
        val query: String,
    )

    private val setTypesLc = flow {
        val setsLce = retryUntilSuccessful(
            action = {
                apiClient.setTypes().map {
                    it.filterNot { setType -> setType.name.equals("set", ignoreCase = true) }
                }
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
    private val searchSetTypeIds = MutableStateFlow(emptySet<Int>())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val searchResultsLce = combine(searchQueryStateFlow, searchSetTypeIds) { searchQuery, setIds ->
        searchQuery to setIds
    }.flatMapLatest { (searchQuery, setIds) ->
        flow {
            val searchResultsLce = retryUntilSuccessful(
                action = {
                    apiClient.search(searchQuery, setIds)
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
        setTypesLc,
        searchSetTypeIds,
        searchQueryStateFlow
    ) { search, setTypes, selectedIds, query ->
        State(
            setTypes = setTypes,
            selectedSetTypeIds = selectedIds,
            searchResults = search,
            query = query,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.ForViewModel,
        initialValue = State(
            setTypes = LC.Loading,
            selectedSetTypeIds = emptySet(),
            searchResults = LCE.Loading,
            query = ""
        )
    )

    fun toggleSetTypeId(setTypeId: Int) {
        searchSetTypeIds.update {
            if (it.contains(setTypeId)) {
                it - setTypeId
            } else {
                it + setTypeId
            }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQueryStateFlow.update { query }
    }
}
