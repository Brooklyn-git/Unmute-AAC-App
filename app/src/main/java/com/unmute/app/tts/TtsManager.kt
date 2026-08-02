package com.unmute.app.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.tts.TextToSpeech
import com.unmute.app.domain.model.AudioOutput
import java.io.FileInputStream
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Speaks text through an [AudioTrack] we own, so the output device can be forced
 * (e.g. to the speakers while headphones are plugged in). Android's TextToSpeech
 * cannot route to a specific device directly, so speech is synthesized into a
 * pipe and played back through our own track.
 */
class TtsManager(context: Context) {

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(AudioManager::class.java)

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(appContext) { status ->
            _isReady.value = status == TextToSpeech.SUCCESS
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    /** Synthesizes [text] and plays it, forcing the given [output] device when possible. */
    suspend fun speak(text: String, language: String, output: AudioOutput, rate: Float, pitch: Float): Boolean =
        withContext(Dispatchers.IO) {
            val engine = tts ?: return@withContext false
            engine.language = Locale.forLanguageTag(if (language == "es") "es" else "en")
            engine.setSpeechRate(rate)
            engine.setPitch(pitch)

            val pipe = ParcelFileDescriptor.createPipe()
            try {
                val result = engine.synthesizeToFile(text, Bundle(), pipe[1], UTTERANCE_ID)
                pipe[1].close()
                if (result != TextToSpeech.SUCCESS) return@withContext false

                val bytes = withTimeoutOrNull(SYNTHESIS_TIMEOUT_MILLIS) {
                    FileInputStream(pipe[0].fileDescriptor).use { it.readBytes() }
                } ?: return@withContext false
                if (bytes.isEmpty()) return@withContext false

                val device = resolvePreferredDevice(output)
                playPcm(bytes, device)
                true
            } finally {
                pipe[0].close()
                pipe[1].close()
            }
        }

    private fun resolvePreferredDevice(output: AudioOutput): AudioDeviceInfo? {
        val devices = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ?: return null
        val types = when (output) {
            AudioOutput.AUTO -> return null
            AudioOutput.SPEAKER -> intArrayOf(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
            AudioOutput.WIRED -> intArrayOf(
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_USB_DEVICE,
            )
            AudioOutput.BLUETOOTH -> intArrayOf(
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            )
        }
        return devices.firstOrNull { it.type in types }
    }

    private fun playPcm(raw: ByteArray, preferredDevice: AudioDeviceInfo?) {
        val wav = parseWav(raw)
        if (wav != null) {
            playPcmBytes(wav.bytes, wav.sampleRate, wav.channels, preferredDevice)
        } else {
            playPcmBytes(raw, SAMPLE_RATE, 1, preferredDevice)
        }
    }

    private fun playPcmBytes(
        bytes: ByteArray,
        sampleRate: Int,
        channels: Int,
        preferredDevice: AudioDeviceInfo?,
    ) {
        if (bytes.isEmpty()) return
        val channelMask = if (channels >= 2) AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBuffer = AudioTrack.getMinBufferSize(sampleRate, channelMask, encoding)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(encoding)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelMask)
                    .build(),
            )
            .setBufferSizeInBytes(maxOf(minBuffer, bytes.size))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        if (preferredDevice != null) {
            track.preferredDevice = preferredDevice
        }
        track.write(bytes, 0, bytes.size)
        track.play()
        while (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
            Thread.sleep(PLAYBACK_POLL_MS)
        }
        track.stop()
        track.release()
    }

    private companion object {
        const val SAMPLE_RATE = 22050
        const val UTTERANCE_ID = "unmute_utterance"
        const val SYNTHESIS_TIMEOUT_MILLIS = 15_000L
        const val PLAYBACK_POLL_MS = 50L
    }
}
