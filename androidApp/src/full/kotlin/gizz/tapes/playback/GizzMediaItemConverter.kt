package gizz.tapes.playback

import androidx.media3.cast.DefaultMediaItemConverter
import androidx.media3.cast.MediaItemConverter
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import org.json.JSONObject

/**
 * Wraps [DefaultMediaItemConverter] to also round-trip recording year/month/day through Cast,
 * and to handle null customData gracefully (the default converter crashes in that case,
 * see https://github.com/androidx/media/issues/1961).
 */
@UnstableApi
internal class GizzMediaItemConverter : MediaItemConverter {

    private val default = DefaultMediaItemConverter()

    override fun toMediaQueueItem(mediaItem: MediaItem): MediaQueueItem {
        val defaultItem = default.toMediaQueueItem(mediaItem)
        val mediaInfo = defaultItem.media ?: return defaultItem
        val contentUrl = mediaInfo.contentUrl ?: return defaultItem

        val customData = (mediaInfo.customData ?: JSONObject())
        mediaItem.mediaMetadata.recordingYear?.let { customData.put(KEY_RECORDING_YEAR, it) }
        mediaItem.mediaMetadata.recordingMonth?.let { customData.put(KEY_RECORDING_MONTH, it) }
        mediaItem.mediaMetadata.recordingDay?.let { customData.put(KEY_RECORDING_DAY, it) }

        val newMediaInfo = MediaInfo.Builder(mediaInfo.contentId)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(mediaInfo.contentType ?: MimeTypes.AUDIO_MPEG)
            .setContentUrl(contentUrl)
            .setStreamDuration(mediaInfo.streamDuration)
            .setMetadata(mediaInfo.metadata)
            .setCustomData(customData)
            .build()

        return MediaQueueItem.Builder(newMediaInfo).build()
    }

    override fun toMediaItem(mediaQueueItem: MediaQueueItem): MediaItem {
        val mediaInfo = mediaQueueItem.media
        if (mediaInfo?.customData == null) {
            return buildFallbackMediaItem(mediaInfo)
        }

        val mediaItem = default.toMediaItem(mediaQueueItem)
        val customData = mediaInfo.customData
        val year = customData?.optInt(KEY_RECORDING_YEAR, -1).takeIf { it != -1 } ?: return mediaItem
        val month = customData?.optInt(KEY_RECORDING_MONTH, -1).takeIf { it != -1 } ?: return mediaItem
        val day = customData?.optInt(KEY_RECORDING_DAY, -1).takeIf { it != -1 } ?: return mediaItem

        return mediaItem.buildUpon()
            .setMediaMetadata(
                mediaItem.mediaMetadata.buildUpon()
                    .setRecordingYear(year)
                    .setRecordingMonth(month)
                    .setRecordingDay(day)
                    .build()
            )
            .build()
    }

    private fun buildFallbackMediaItem(mediaInfo: MediaInfo?): MediaItem {
        val uri = mediaInfo?.contentUrl ?: mediaInfo?.contentId ?: return MediaItem.EMPTY
        val builder = MediaItem.Builder().setUri(uri)
        mediaInfo?.contentId?.let { builder.setMediaId(it) }
        mediaInfo?.contentType?.let { builder.setMimeType(it) }
        mediaInfo?.metadata?.let { meta ->
            val metadataBuilder = androidx.media3.common.MediaMetadata.Builder()
            if (meta.containsKey(MediaMetadata.KEY_TITLE)) {
                metadataBuilder.setTitle(meta.getString(MediaMetadata.KEY_TITLE))
            }
            if (meta.containsKey(MediaMetadata.KEY_ALBUM_TITLE)) {
                metadataBuilder.setAlbumTitle(meta.getString(MediaMetadata.KEY_ALBUM_TITLE))
            }
            if (meta.images.isNotEmpty()) {
                metadataBuilder.setArtworkUri(meta.images[0].url)
            }
            builder.setMediaMetadata(metadataBuilder.build())
        }
        return builder.build()
    }

    companion object {
        private const val KEY_RECORDING_YEAR = "recordingYear"
        private const val KEY_RECORDING_MONTH = "recordingMonth"
        private const val KEY_RECORDING_DAY = "recordingDay"
    }
}
