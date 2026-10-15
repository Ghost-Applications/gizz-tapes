package gizz.tapes.playback

import kotlinx.coroutines.test.runTest
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

class StoredMediaSessionSerializerTest {

    @Test
    fun `reads sessions saved before remoteUrl existed`() = runTest {
        val json = """
            {"currentTrack":1,"currentTime":42,"items":[{"id":"1","url":"https://example.com/a.mp3",
            "title":"Rattlesnake","albumTitle":"Flying Microtonal Banana","artworkUrl":null,
            "showId":"123","showTitle":{"title":"Red Rocks","date":"2024-09-08"},
            "durationMs":1000,"showDate":"2024-09-08"}]}
        """.trimIndent()

        val session = StoredMediaSessionSerializer().readFrom(Buffer().writeUtf8(json))

        assertEquals(1, session.currentTrack)
        assertEquals("https://example.com/a.mp3", session.items.single().remoteUrl)
    }
}
