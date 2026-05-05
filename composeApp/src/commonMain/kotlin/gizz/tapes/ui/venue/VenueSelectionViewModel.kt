package gizz.tapes.ui.venue

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.toRoute
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactoryKey
import gizz.tapes.api.GizzTapesApiClient
import gizz.tapes.api.data.PartialShowData
import gizz.tapes.api.data.Venue
import gizz.tapes.data.PosterUrl
import gizz.tapes.data.Subtitle
import gizz.tapes.data.Title
import gizz.tapes.nav.Destination
import gizz.tapes.util.ForViewModel
import gizz.tapes.util.LCE
import gizz.tapes.util.flatMap
import gizz.tapes.util.map
import gizz.tapes.util.retryUntilSuccessful
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn

@AssistedInject
class VenueSelectionViewModel(
    private val apiClient: GizzTapesApiClient,
    @Assisted savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val logger = Logger.withTag("VenueSelectionViewModel")

    private val route = savedStateHandle.toRoute<Destination.CountryVenues>()
    private val countryId = route.countryId

    val countryName = route.countryName

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

    val venuesState: StateFlow<LCE<List<VenueSelectionData>, Exception>> = combine(
        venuesFlow,
        showsFlow
    ) { venueLce, showsLce ->
        showsLce.flatMap { shows ->
            venueLce.map { venues ->
                val countryVenues = venues.filter { it.countryId.toInt() == countryId }
                countryVenues.map { venue ->
                    val venueShows = shows.asSequence().filter { it.venueName == venue.name }
                    val totalVenueShows = venueShows.count()

                    val posterUrl = venueShows
                        .sortedByDescending { it.date }
                        .take(1)
                        .map { it.posterUrl }
                        .map { PosterUrl(it) }
                        .first()

                    val subtitleText = if (totalVenueShows > 1) "shows" else "show"

                    VenueSelectionData(
                        title = Title(venue.name),
                        subtitle = Subtitle("$totalVenueShows $subtitleText"),
                        posterUrl = posterUrl
                    )
                }.sortedBy { it.title.value }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.ForViewModel,
        initialValue = LCE.Loading
    )

    @AssistedFactory
    @ViewModelAssistedFactoryKey(VenueSelectionViewModel::class)
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ViewModelAssistedFactory {
        fun create(savedStateHandle: SavedStateHandle): VenueSelectionViewModel
        override fun create(extras: CreationExtras): VenueSelectionViewModel = create(extras.createSavedStateHandle())
    }
}
