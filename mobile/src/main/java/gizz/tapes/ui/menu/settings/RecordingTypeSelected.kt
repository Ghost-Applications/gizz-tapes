package gizz.tapes.ui.menu.settings

import gizz.tapes.api.data.Recording

fun interface RecordingTypeSelected {
    operator fun invoke(selectedType: Recording.Type)
}
