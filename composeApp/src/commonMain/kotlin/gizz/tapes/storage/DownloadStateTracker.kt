package gizz.tapes.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

// Shared in-memory DownloadState bookkeeping used by the Desktop and iOS MusicDownloader
// implementations (Android delegates to WorkManager's own persisted state instead, so it has no
// equivalent need for this).
class DownloadStateTracker {
    private val states = MutableStateFlow<Map<String, DownloadState>>(emptyMap())

    fun currentState(localPath: String): DownloadState = states.value[localPath] ?: DownloadState.NOT_STARTED

    fun markState(localPath: String, state: DownloadState) {
        states.update { it + (localPath to state) }
    }

    fun downloadState(localPath: String): Flow<DownloadState> =
        states.map { it[localPath] ?: DownloadState.NOT_STARTED }.distinctUntilChanged()

    // in-memory bookkeeping only (no cross-process persistence like WorkManager), so this just
    // drops terminal-state entries so a file deleted outside a download's normal lifecycle
    // doesn't keep reporting stale state.
    fun forgetFinishedDownloads() {
        states.update { it.filterValues { state -> state == DownloadState.IN_PROGRESS } }
    }
}
