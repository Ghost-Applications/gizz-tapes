package gizz.tapes.storage

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import gizz.tapes.api.data.Show
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.RecordingId
import gizz.tapes.db.Database
import gizz.tapes.db.Recordings
import gizz.tapes.db.Shows

@Inject
@SingleIn(AppScope::class)
class ShowSaver(
    private val db: Database
) {
    fun saveShow(
        recordingId: RecordingId,
        title: FullShowTitle,
        show: Show
    ) {
        val recording = show.recordings.first { it.id == recordingId.id }

        // download and save recordings

        db.showsQueries.insertShow(show.toDbShow(title))

        val r = Recordings(
            id = recording.id,
            showId = show.id,
            uploadedAt = recording.uploadedAt,
            type = recording.type,
            source = recording.source,
            lineage = recording.lineage,
            taper = recording.taper
        )

        db.recordingsQueries.insertRecording(r)


    }

    private fun Show.toDbShow(title: FullShowTitle) = Shows(
        id = this.id,
        sortOrder = this.order,
        date = this.date,
        posterUrl = this.posterUrl,
        notes = this.notes,
        title = title.title.value,
        permalink = this.kglwNet.permalink
    )
}
