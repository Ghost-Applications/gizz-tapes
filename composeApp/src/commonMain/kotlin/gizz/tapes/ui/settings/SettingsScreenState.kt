package gizz.tapes.ui.settings

import gizz.tapes.api.data.Recording
import gizz.tapes.data.DEFAULT_RECORDING_TYPE_RANKING
import gizz.tapes.lastfm.LastFmAuthState
import gizz.tapes.util.LCE

data class SettingsScreenState(
    /** null means "No preference". */
    val rankedRecordingTypes: List<Recording.Type>? = null,
    val favouriteTaperIds: Set<UInt> = emptySet(),
    val tapers: LCE<List<TaperItem>, Throwable> = LCE.Loading,
    val volumeBoostEnabled: Boolean = false,
    val isVolumeBoostSupported: Boolean = false,
    val lastFmState: LastFmAuthState = LastFmAuthState.LoggedOut,
) {
    val recordingTypeRanking: List<Recording.Type> = rankedRecordingTypes ?: DEFAULT_RECORDING_TYPE_RANKING
}

data class TaperItem(
    val id: UInt,
    val name: String,
    val uploadCount: Int,
)
