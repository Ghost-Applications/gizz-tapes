package gizz.tapes.data

import gizz.tapes.api.data.KglwFile
import gizz.tapes.api.data.PartialShowData
import gizz.tapes.api.data.Recording
import timber.log.Timber
import gizz.tapes.api.data.Show as ApiShow

sealed interface MediaId {
    companion object {
        private const val ROOT_ID = "root"

        fun fromString(id: String): MediaId {
            val idParts = id.split("/")

            if (idParts.isEmpty() || idParts.first() != ROOT_ID) {
                throw IllegalStateException("id $id is not in a known format")
            }

            // id's might need to be html escaped...
            return when (idParts.size) {
                1 -> RootId
                2 -> YearId(idParts[1])
                3 -> ShowId(YearId(idParts[1]), idParts[2])
                4 -> RecordingId(ShowId(YearId(idParts[1]), idParts[2]), idParts[3])
                5 -> TrackId(
                    parent = RecordingId(ShowId(YearId(idParts[1]), idParts[2]), idParts[3]),
                    track = idParts[4]
                )
                else -> {
                    Timber.w("id %s has more parts than expected", id)
                    TrackId(
                        parent = RecordingId(ShowId(YearId(idParts[1]), idParts[2]), idParts[3]),
                        track = idParts.subList(4, idParts.size).joinToString("/")
                    )
                }
            }
        }
    }

    val id: String
    val parent: MediaId?
    val year: String?
    val showId: String?

    data object RootId : MediaId {
        override val parent = null
        override val year = null
        override val showId = null
        override val id = ROOT_ID
    }

    data class YearId(
        override val year: String
    ) : MediaId {
        override val parent = RootId
        override val showId = null
        override val id = "$ROOT_ID/$year"
    }

    data class ShowId(
        override val parent: YearId,
        override val showId: String,
    ) : MediaId {
        override val year = parent.year
        override val id = "${parent.id}/$showId"

        constructor(show: ApiShow) : this(
            YearId(year = show.date.year.toString()),
            showId = show.id
        )

        constructor(show: PartialShowData) : this(
            YearId(year = show.date.year.toString()),
            showId = show.id
        )
    }

    data class RecordingId(
        override val parent: ShowId,
        val recordingId: String,
    ) : MediaId {
        override val showId = parent.showId
        override val year = parent.year
        override val id = "${parent.id}/$recordingId"

        constructor(show: ApiShow, recording: Recording) : this(
            parent = ShowId(show),
            recordingId = recording.id
        )
    }

    data class TrackId(
        override val parent: RecordingId,
        val track: String,
    ) : MediaId {
        override val showId = parent.showId
        override val year = parent.year
        override val id = "${parent.id}/$track"
        val recordingId = parent.recordingId

        constructor(show: ApiShow, recording: Recording, file: KglwFile) : this(
            parent = RecordingId(show, recording),
            track = file.filename
        )
    }
}
