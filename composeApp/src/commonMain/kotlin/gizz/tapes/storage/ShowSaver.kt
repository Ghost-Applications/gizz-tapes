package gizz.tapes.storage

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import gizz.tapes.api.data.Recording
import gizz.tapes.api.data.Show
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.RecordingId
import gizz.tapes.db.Database
import gizz.tapes.db.Files
import gizz.tapes.db.Recordings
import gizz.tapes.db.Shows

@Inject
@SingleIn(AppScope::class)
class ShowSaver(
    private val db: Database,
    private val musicDownloader: MusicDownloader,
) {

    private val logger = Logger.withTag("ShowSaver")

    fun saveShow(
        recordingId: RecordingId,
        title: FullShowTitle,
        show: Show
    ) {
        val recording: Recording = show.recordings.first { it.id == recordingId.id }

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

        logger.d { "recoding $recording" }
        recording.files.forEach {
            logger.d { "Downloading ${it.title}" }
            val filepath = (recording.filesPathPrefix + it.filename)

            db.filesQueries.insertFile(
                Files(
                    recordingId = recording.id,
                    filepath = filepath,
                )
            )

            musicDownloader.download(filepath)
        }
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
