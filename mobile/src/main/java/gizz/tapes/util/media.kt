package gizz.tapes.util

import android.text.format.DateUtils
import androidx.media3.common.MediaItem
import gizz.tapes.ui.nav.Show
import kotlin.math.max

val Long.formatedElapsedTime: String get() = DateUtils.formatElapsedTime(max(this, 0) / 1000L)
val MediaItem?.title: String get() = this?.mediaMetadata?.title?.toString() ?: "--"

/** Must be called on metadata with extras **/
val MediaItem.showExtras: Show? get() = mediaMetadata.extras?.toShowInfo()

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
