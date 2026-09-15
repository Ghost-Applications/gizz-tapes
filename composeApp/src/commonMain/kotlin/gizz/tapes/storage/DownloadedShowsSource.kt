package gizz.tapes.storage

import gizz.tapes.api.data.Recording
import gizz.tapes.api.data.Show
import gizz.tapes.data.ShowId

// What MediaItemTree (Android Auto's browse tree) needs to browse/play already-downloaded shows.
// A narrow interface rather than depending on ShowSaver directly so tests can supply a trivial
// fake instead of standing up ShowSaver's full dependency graph (DB, downloader, AppContext,
// HttpClient) - also sidesteps a bug where Metro didn't reliably inject ShowSaver when it was
// only ever bound as a nullable, defaulted constructor parameter.
interface DownloadedShowsSource {
    fun loadDownloadedShows(): List<Show>
    fun loadCachedShow(showId: ShowId): Show?
    fun localFileIfDownloaded(recording: Recording, filename: String): String?
}
