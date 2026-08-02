package com.unmute.app.tts

/** Raw PCM payload of a RIFF/WAVE file, with the format described by the header. */
internal data class PcmPayload(
    val bytes: ByteArray,
    val sampleRate: Int,
    val channels: Int,
)

/**
 * Extracts the PCM payload and format from a RIFF/WAVE file, or null if [raw]
 * is not a WAV file.
 */
internal fun parseWav(raw: ByteArray): PcmPayload? {
    if (raw.size < 44) return null
    val isRiff = raw[0].toInt() == 'R'.code && raw[1].toInt() == 'I'.code &&
        raw[2].toInt() == 'F'.code && raw[3].toInt() == 'F'.code
    val isWave = raw[8].toInt() == 'W'.code && raw[9].toInt() == 'A'.code &&
        raw[10].toInt() == 'V'.code && raw[11].toInt() == 'E'.code
    if (!isRiff || !isWave) return null

    val channels = raw[22].toInt() and 0xff or ((raw[23].toInt() and 0xff) shl 8)
    val sampleRate = raw[24].toInt() and 0xff or ((raw[25].toInt() and 0xff) shl 8) or
        ((raw[26].toInt() and 0xff) shl 16) or ((raw[27].toInt() and 0xff) shl 24)

    var offset = 12
    while (offset + 8 <= raw.size) {
        val chunkId = raw[offset].toInt() and 0xff or ((raw[offset + 1].toInt() and 0xff) shl 8) or
            ((raw[offset + 2].toInt() and 0xff) shl 16) or ((raw[offset + 3].toInt() and 0xff) shl 24)
        val chunkSize = raw[offset + 4].toInt() and 0xff or ((raw[offset + 5].toInt() and 0xff) shl 8) or
            ((raw[offset + 6].toInt() and 0xff) shl 16) or ((raw[offset + 7].toInt() and 0xff) shl 24)
        if (chunkId == DATA_CHUNK_ID) {
            val dataStart = offset + 8
            val payload = raw.copyOfRange(dataStart, raw.size)
            return PcmPayload(bytes = payload, sampleRate = sampleRate, channels = channels)
        }
        offset += 8 + chunkSize
    }
    return null
}

private const val DATA_CHUNK_ID = 0x61746164
