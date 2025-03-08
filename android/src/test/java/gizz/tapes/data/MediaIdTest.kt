package gizz.tapes.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MediaIdTest {

    @Test
    fun `test fromIdString with root id`() {
        val mediaId = MediaId.fromString("root")
        assertThat(mediaId).isInstanceOf(MediaId.RootId::class.java)
    }

    @Test
    fun `test fromIdString with year id`() {
        val mediaId = MediaId.fromString("root/2024")
        assertThat(mediaId).isInstanceOf(MediaId.YearId::class.java)
        assertThat((mediaId as MediaId.YearId).year).isEqualTo("2024")
    }

    @Test
    fun `test fromIdString with show id`() {
        val mediaId = MediaId.fromString("root/2024/showId")
        assertThat(mediaId).isInstanceOf(MediaId.ShowId::class.java)
        assertThat((mediaId as MediaId.ShowId).year).isEqualTo("2024")
        assertThat(mediaId.showId).isEqualTo("showId")
    }

    @Test
    fun `test fromIdString with recording id`() {
        val mediaId = MediaId.fromString("root/2024/showId/recordingId")
        assertThat(mediaId).isInstanceOf(MediaId.RecordingId::class.java)
        assertThat((mediaId as MediaId.RecordingId).year).isEqualTo("2024")
        assertThat(mediaId.showId).isEqualTo("showId")
        assertThat(mediaId.recordingId).isEqualTo("recordingId")
    }

    @Test
    fun `test fromIdString with track id`() {
        val mediaId = MediaId.fromString("root/2024/showId/recordingId/trackId")
        assertThat(mediaId).isInstanceOf(MediaId.TrackId::class.java)
        assertThat((mediaId as MediaId.TrackId).year).isEqualTo("2024")
        assertThat(mediaId.showId).isEqualTo("showId")
        assertThat(mediaId.track).isEqualTo("trackId")
    }

    @Test
    fun `test fromIdString for trackId with extra parts in id`() {
        val mediaId = MediaId.fromString("root/2024/showId/recordingId/trackId/extra")
        assertThat(mediaId).isInstanceOf(MediaId.TrackId::class.java)
        assertThat((mediaId as MediaId.TrackId).year).isEqualTo("2024")
        assertThat(mediaId.showId).isEqualTo("showId")
        assertThat(mediaId.recordingId).isEqualTo("recordingId")
        assertThat(mediaId.track).isEqualTo("trackId/extra")
    }

    @Test(expected = IllegalStateException::class)
    fun `test fromIdString with invalid id`() {
        MediaId.fromString("invalid")
    }
}
