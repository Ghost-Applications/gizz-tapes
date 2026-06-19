package gizz.tapes.storage.id3

import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource

data class Id3CoverArt(val bytes: ByteArray, val mimeType: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Id3CoverArt

        if (!bytes.contentEquals(other.bytes)) return false
        if (mimeType != other.mimeType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
}

data class Id3Tags(
    val title: String,
    val artist: String,
    val album: String,
    val trackNumber: Int,
    val trackCount: Int,
    val year: Int,
    val coverArt: Id3CoverArt?,
)

// Prepends a fresh ID3v2.3 tag built from [tags] onto [input]'s audio data, dropping any
// pre-existing ID3v2 tag first so exported files don't end up with duplicate/stale tags.
fun writeMp3WithId3Tags(input: BufferedSource, output: BufferedSink, tags: Id3Tags) {
    input.skipExistingId3v2Tag()
    output.write(buildId3TagBytes(tags))
    input.readAll(output)
}

private fun BufferedSource.skipExistingId3v2Tag() {
    if (!request(10)) return
    val header = peek().readByteArray(10)
    val isId3 = header[0] == 'I'.code.toByte() && header[1] == 'D'.code.toByte() && header[2] == '3'.code.toByte()
    if (isId3) {
        val tagSize = synchSafeToInt(header[6], header[7], header[8], header[9])
        skip(10L + tagSize)
    }
}

private fun buildId3TagBytes(tags: Id3Tags): ByteArray {
    val frames = Buffer()
    writeTextFrame(frames, "TIT2", tags.title)
    writeTextFrame(frames, "TPE1", tags.artist)
    writeTextFrame(frames, "TALB", tags.album)
    writeTextFrame(frames, "TRCK", "${tags.trackNumber}/${tags.trackCount}")
    writeTextFrame(frames, "TYER", tags.year.toString())
    tags.coverArt?.let { writeApicFrame(frames, it) }
    val frameBytes = frames.readByteArray()

    return Buffer()
        .writeUtf8("ID3")
        .writeByte(0x03).writeByte(0x00) // version 2.3.0
        .writeByte(0x00) // flags
        .write(synchSafeBytes(frameBytes.size))
        .write(frameBytes)
        .readByteArray()
}

// Encoding byte $01 (UTF-16 with BOM) is used instead of $00/ISO-8859-1 so any show/track/venue
// title can be represented, not just ASCII.
private fun writeTextFrame(sink: Buffer, frameId: String, value: String) {
    val body = Buffer().writeByte(0x01).write(byteArrayOf(0xFF.toByte(), 0xFE.toByte()))
    // Kotlin's Char is already a UTF-16 code unit, so iterating value's Chars and writing each as
    // little-endian bytes (matching the FF FE BOM above) is exactly UTF-16LE encoding - no
    // separate encoding step needed, and this naturally preserves surrogate pairs since each
    // surrogate half is its own Char.
    for (char in value) {
        body.writeByte(char.code and 0xFF)
        body.writeByte((char.code shr 8) and 0xFF)
    }
    writeFrame(sink, frameId, body.readByteArray())
}

private fun writeApicFrame(sink: Buffer, coverArt: Id3CoverArt) {
    val body = Buffer()
        .writeByte(0x00) // ISO-8859-1, for the (empty) description field below
        .writeUtf8(coverArt.mimeType).writeByte(0x00)
        .writeByte(0x03) // picture type: cover (front)
        .writeByte(0x00) // empty description + its ISO-8859-1 terminator
        .write(coverArt.bytes)
    writeFrame(sink, "APIC", body.readByteArray())
}

private fun writeFrame(sink: Buffer, frameId: String, body: ByteArray) {
    sink.writeUtf8(frameId)
    // plain big-endian, NOT synchsafe - only the ID3v2.3 tag header's size is synchsafe; per-frame
    // sizes are ordinary 32-bit integers (that changes in ID3v2.4, not used here).
    sink.writeInt(body.size)
    sink.writeShort(0) // flags
    sink.write(body)
}

private fun synchSafeBytes(size: Int): ByteArray = byteArrayOf(
    ((size ushr 21) and 0x7F).toByte(),
    ((size ushr 14) and 0x7F).toByte(),
    ((size ushr 7) and 0x7F).toByte(),
    (size and 0x7F).toByte(),
)

private fun synchSafeToInt(b0: Byte, b1: Byte, b2: Byte, b3: Byte): Int =
    ((b0.toInt() and 0x7F) shl 21) or
        ((b1.toInt() and 0x7F) shl 14) or
        ((b2.toInt() and 0x7F) shl 7) or
        (b3.toInt() and 0x7F)
