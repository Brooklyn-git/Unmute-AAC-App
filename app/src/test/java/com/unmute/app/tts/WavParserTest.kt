package com.unmute.app.tts

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WavParserTest {

    @Test
    fun `parses a standard 44 byte header wav`() {
        val payload = ByteArray(10) { it.toByte() }
        val wav = buildWav(sampleRate = 22050, channels = 1, data = payload)
        val parsed = parseWav(wav)
        assertEquals(22050, parsed?.sampleRate)
        assertEquals(1, parsed?.channels)
        assertArrayEquals(payload, parsed?.bytes)
    }

    @Test
    fun `reads stereo sample rate and channels`() {
        val payload = ByteArray(4)
        val wav = buildWav(sampleRate = 44100, channels = 2, data = payload)
        val parsed = parseWav(wav)
        assertEquals(44100, parsed?.sampleRate)
        assertEquals(2, parsed?.channels)
    }

    @Test
    fun `handles extra chunks before data`() {
        val payload = byteArrayOf(1, 2, 3)
        val fmt = "fmt ".toByteArray() + littleInt(16) + littleShort(1) + littleShort(1) +
            littleInt(22050) + littleInt(44100) + littleShort(2) + littleShort(16)
        val extra = "junk".toByteArray() + littleInt(0)
        val data = "data".toByteArray() + littleInt(payload.size) + payload
        val wav = "RIFF".toByteArray() + littleInt(36 + extra.size + payload.size + 8) +
            "WAVE".toByteArray() + fmt + extra + data
        val parsed = parseWav(wav)
        assertArrayEquals(payload, parsed?.bytes)
        assertEquals(22050, parsed?.sampleRate)
    }

    @Test
    fun `returns null for non wav bytes`() {
        assertNull(parseWav(byteArrayOf(1, 2, 3, 4)))
        assertNull(parseWav(ByteArray(44)))
    }

    private fun buildWav(sampleRate: Int, channels: Int, data: ByteArray): ByteArray {
        val fmt = "fmt ".toByteArray() + littleInt(16) + littleShort(1) + littleShort(channels) +
            littleInt(sampleRate) + littleInt(sampleRate * channels * 2) + littleShort(channels * 2) +
            littleShort(16)
        val dataChunk = "data".toByteArray() + littleInt(data.size) + data
        return "RIFF".toByteArray() + littleInt(4 + fmt.size + dataChunk.size) + "WAVE".toByteArray() +
            fmt + dataChunk
    }

    private fun littleShort(value: Int): ByteArray = byteArrayOf(
        (value and 0xff).toByte(),
        ((value shr 8) and 0xff).toByte(),
    )

    private fun littleInt(value: Int): ByteArray = byteArrayOf(
        (value and 0xff).toByte(),
        ((value shr 8) and 0xff).toByte(),
        ((value shr 16) and 0xff).toByte(),
        ((value shr 24) and 0xff).toByte(),
    )
}
