package gizz.tapes.ui.settings

import gizz.tapes.api.data.Recording

data class SettingsScreenState(
    val selectedPreferredRecordingType: Recording.Type,
    val volumeBoostEnabled: Boolean = false,
    val isVolumeBoostSupported: Boolean = false,
    val preferredRecordingSelections: List<Recording.Type> = Recording.Type.entries
        .filter { it != Recording.Type.None && it != Recording.Type.UnknownType },
)
