package gizz.tapes.util

import android.os.Bundle
import gizz.tapes.api.data.Show

//fun Show.toMetadataExtras(): Bundle = Bundle().apply {
//    putString("showId", id)
//    putInt("venueId", venueId.toInt())
//}

fun Bundle.toShowInfo(): Pair<Long, String> = getLong("showId") to getString("venueName", "")

// TODO figure out way to get venue name in with object
// probably requires creating a RenderViewObject
val Show.showTitle get() = "${date.toAlbumFormat()}" // $venue_name"