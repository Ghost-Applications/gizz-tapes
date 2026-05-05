package gizz.tapes.ui.venue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.fx.coroutines.parMap
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import gizz.tapes.api.GizzTapesApiClient
import gizz.tapes.api.data.Country
import gizz.tapes.api.data.PartialShowData
import gizz.tapes.api.data.Venue
import gizz.tapes.data.PosterUrl
import gizz.tapes.data.Subtitle
import gizz.tapes.data.Title
import gizz.tapes.util.ForViewModel
import gizz.tapes.util.LCE
import gizz.tapes.util.retryUntilSuccessful
import gizz.tapes.util.suspendFlatMap
import gizz.tapes.util.suspendMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn

@Inject
@ContributesIntoMap(AppScope::class)
@ViewModelKey
class CountrySelectionViewModel(
    private val apiClient: GizzTapesApiClient,
) : ViewModel() {
    private val logger = Logger.withTag("CountrySelectionViewModel")

    private val venuesFlow: Flow<LCE<List<Venue>, Exception>> = flow {
        val venues = retryUntilSuccessful(
            action = { apiClient.venues() },
            onErrorAfter3SecondsAction = { error ->
                Logger.d(error) { "Error loading venues" }
                emit(LCE.Error(error))
            }
        )
        emit(venues)
    }.shareIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        replay = 1
    )

    private val countriesFlow: Flow<LCE<List<Country>, Exception>> = flow {
        val countries = retryUntilSuccessful(
            action = { apiClient.countries() },
            onErrorAfter3SecondsAction = { error ->
                logger.d(error) { "Error loading countries" }
                emit(LCE.Error(error))
            }
        )
        emit(countries)
    }.shareIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        replay = 1
    )

    private val showsFlow: Flow<LCE<List<PartialShowData>, Exception>> = flow {
        val shows = retryUntilSuccessful(
            action = { apiClient.shows() },
            onErrorAfter3SecondsAction = { error ->
                logger.d(error) { "Error loading shows" }
                emit(LCE.Error(error))
            }
        )
        emit(shows)
    }.shareIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        replay = 1
    )

    val countriesState: StateFlow<LCE<List<CountrySelectionData>, Exception>> = combine(
        venuesFlow,
        countriesFlow,
        showsFlow,
    ) { venuesLce, countriesLce, showsLce ->
        showsLce.suspendFlatMap { shows ->
            countriesLce.suspendFlatMap { countries ->
                venuesLce.suspendMap { venues ->
                    countries.parMap { country ->
                        val countryVenues = venues.filter { it.countryId == country.id }
                        val countryShowCount = countryVenues.sumOf { it.showCount }
                        val venueNames = countryVenues.map { it.name }.toSet()
                        val posterUrl = shows.asSequence().filter { it.venueName in venueNames }
                            .sortedByDescending { it.date }
                            .take(1)
                            .map { it.posterUrl }
                            .map { PosterUrl(it) }
                            .first()

                        CountrySelectionData(
                            title = Title(country.name),
                            subtitle = Subtitle(
                                "$countryShowCount Shows • ${countryVenues.count()} Venues"
                            ),
                            posterUrl = posterUrl,
                            countryId = country.id
                        )
                    }.sortedBy { it.title.value }
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.ForViewModel,
        initialValue = LCE.Loading
    )
}
