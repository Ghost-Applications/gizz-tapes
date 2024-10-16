package gizz.tapes.util

import android.text.format.DateUtils
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import gizz.tapes.data.ShowId
import gizz.tapes.data.Title

val Long.formatedElapsedTime: String get() = DateUtils.formatElapsedTime(this / 1000L)
val MediaItem?.title: String get() = this?.mediaMetadata?.title?.toString() ?: "--"

/** Must be called on metadata with extras **/
val MediaItem.mediaExtras: Pair<ShowId, Title> get() = mediaMetadata.extras!!.toShowInfo()

fun MediaItem.toReadableString() = """
    mediaId=${this.mediaId}
    localConfiguration=${this.localConfiguration}
    title=${this.mediaMetadata.title}
""".trimIndent()

@JvmInline
value class MediaItemWrapper(private val mediaItem: MediaItem) {
    override fun toString(): String = mediaItem.toReadableString()
}

@JvmInline
value class MediaItemsWrapper(private val mediaItems: List<MediaItem>) {
    override fun toString(): String = mediaItems.joinToString { it.toReadableString() }
}