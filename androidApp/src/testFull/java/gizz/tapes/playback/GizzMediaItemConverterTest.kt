package gizz.tapes.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.google.common.truth.Truth.assertThat
import gizz.tapes.util.putRemoteUrl
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GizzMediaItemConverterTest {

    private val converter = GizzMediaItemConverter()

    @Test
    fun `toMediaQueueItem uses remote url for a downloaded local file`() {
        val remoteUrl = "https://tapes.kglw.net/files/recording1/track1.mp3"
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse("file:///data/data/gizz.tapes/files/downloads/recording1/track1.mp3"))
            .setMediaId("track1")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setExtras(android.os.Bundle().putRemoteUrl(remoteUrl))
                    .build()
            )
            .build()

        val queueItem = converter.toMediaQueueItem(mediaItem)

        assertThat(queueItem.media?.contentUrl).isEqualTo(remoteUrl)
    }

    @Test
    fun `toMediaQueueItem leaves an already-remote url untouched`() {
        val remoteUrl = "https://tapes.kglw.net/files/recording1/track1.mp3"
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(remoteUrl))
            .setMediaId("track1")
            .setMediaMetadata(MediaMetadata.Builder().build())
            .build()

        val queueItem = converter.toMediaQueueItem(mediaItem)

        assertThat(queueItem.media?.contentUrl).isEqualTo(remoteUrl)
    }

    @Test
    fun `toMediaItem reconciles a stale local uri after reconnecting to an active cast session`() {
        val remoteUrl = "https://tapes.kglw.net/files/recording1/track1.mp3"
        val original = MediaItem.Builder()
            .setUri(Uri.parse("file:///data/data/gizz.tapes/files/downloads/recording1/track1.mp3"))
            .setMediaId("track1")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setExtras(android.os.Bundle().putRemoteUrl(remoteUrl))
                    .build()
            )
            .build()
        val queueItem = converter.toMediaQueueItem(original)

        // A fresh converter/player instance, as created after the app process restarts and
        // reconnects to a cast session that kept running in the background.
        val reconnected = GizzMediaItemConverter().toMediaItem(queueItem)

        assertThat(reconnected.localConfiguration?.uri.toString()).isEqualTo(remoteUrl)
    }
}
