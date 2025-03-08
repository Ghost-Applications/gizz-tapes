package gizz.tapes.ui.menu.settings

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import gizz.tapes.api.data.Recording

data class SettingsScreenState(
    val selectedPreferredRecordingType: Recording.Type,
) {
    val preferredRecordingSelections: NonEmptyList<Recording.Type> = Recording.Type.entries
        .filter { it != Recording.Type.UnknownType && it != Recording.Type.None }
        .toNonEmptyListOrNull() ?: error("List should not be empty")
}
