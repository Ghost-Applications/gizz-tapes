package gizz.tapes.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.NonEmptyList
import arrow.core.raise.either
import arrow.core.toNonEmptyListOrNone
import arrow.core.toNonEmptyListOrThrow
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import gizz.tapes.api.GizzTapesApiClient
import gizz.tapes.data.ShowSelectionData
import gizz.tapes.util.ForViewModel
import gizz.tapes.util.LC
import gizz.tapes.util.LCE
import gizz.tapes.util.contentOrNull
import gizz.tapes.util.flatMap
import gizz.tapes.util.map
import gizz.tapes.util.retryUntilSuccessful
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

@Inject
@ContributesIntoMap(AppScope::class)
@ViewModelKey
class HomeViewModel(
    private val apiClient: GizzTapesApiClient
) : ViewModel() {
    private val logger = Logger.withTag("HomeViewModel")

    val state = combine(
        loadHeader(),
        loadLatestShows(),
        loadRandomShows(),
        loadGizztory()
    ) { headerLce, showsLce, randomLc, gizztoryLce ->
        val content: LCE<HomeContent, Exception> = showsLce.flatMap { shows ->
            gizztoryLce.map { gizztory ->
                HomeContent(
                    todayInGizztory = gizztory,
                    latestShows = shows,
                    randomShows = randomLc
                )
            }
        }

        HomeScreenData(
            header = headerLce,
            content = content
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.ForViewModel,
        initialValue = HomeScreenData(LCE.Loading, LCE.Loading)
    )

    private val fetchedShows = fetchShows().stateIn(
        scope = viewModelScope,
        started = SharingStarted.ForViewModel,
        initialValue = LCE.Loading
    )

    private val reloadShowsTrigger = MutableStateFlow(0L)

    fun reloadRandomShows() {
        reloadShowsTrigger.update { Clock.System.now().toEpochMilliseconds() }
    }

    private fun fetchShows() = flow {
        emit(LCE.Loading)
        val result = retryUntilSuccessful(
            action = {
                apiClient.shows().map { shows ->
                    shows.sortedByDescending { it.date }
                }
            },
            onErrorAfter3SecondsAction = { error ->
                logger.d(error) { "Error loading shows" }
                emit(LCE.Error(error))
            }
        )
        emit(result)
    }

    private fun loadLatestShows(): Flow<LCE<NonEmptyList<ShowSelectionData>, Exception>> = flow {
        emit(LCE.Loading)
        fetchedShows.collect { showLce ->
            val latestShows = showLce.map { shows ->
                shows.take(10)
                    .map { show -> ShowSelectionData.Companion(show) }
                    .toNonEmptyListOrThrow()
            }
            emit(latestShows)
        }
    }

    private fun loadHeader(): Flow<LCE<HomeHeader, Exception>> = flow {
        emit(LCE.Loading)
        val header = retryUntilSuccessful(
            action = {
                val stats = viewModelScope.async {
                    apiClient.stats()
                }

                val heroPhoto = viewModelScope.async {
                    apiClient.heroPhotos().map { it.random() }
                }

                either {
                    val heroPhoto = heroPhoto.await().bind()
                    val stats = stats.await().bind()
                    HomeHeader(
                        heroPhoto = heroPhoto.url,
                        heroPhotoAttribution = heroPhoto.credit,
                        shows = stats.totalShows,
                        recordings = stats.totalRecordings,
                        hours = stats.hours
                    )
                }
            },
            onErrorAfter3SecondsAction = { error ->
                logger.d(error) { "Error loading hero photos" }
                emit(LCE.Error(error = error))
            }
        )
        emit(header)
    }

    private fun loadGizztory(): Flow<LCE<Gizztory?, Exception>> = flow {
        emit(LCE.Loading)
        fetchedShows.collect { showsLce ->
            val today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val gizztory = showsLce.map { shows ->
                shows.filter { show ->
                    show.date.month == today.month && show.date.day == today.day
                }.map { show -> ShowSelectionData.Companion(show) }
                    .toNonEmptyListOrNone()
                    .map {
                        Gizztory(todayDate = today, shows = it)
                    }.getOrNull()
            }
            emit(gizztory)
        }
    }

    private fun loadRandomShows(): Flow<LC<NonEmptyList<ShowSelectionData>>> = flow {
        emit(LC.Loading)
        reloadShowsTrigger.combine(fetchedShows) { _, fetchedShows ->
            fetchedShows
        }.collect { showsLce ->
            val randomShows = showsLce.map { shows ->
                shows.shuffled()
                    .take(10)
                    .map { show -> ShowSelectionData(show) }
                    .toNonEmptyListOrThrow()
            }.contentOrNull()
                ?.let { LC.Content(it) } ?: LC.Loading

            emit(randomShows)
        }
    }
}
