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
import android.util.Log
import com.unmute.app.domain.model.AudioOutputIds
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
 * pipe and played back through our own track. If that fails, we fall back to the
 * engine's normal [TextToSpeech.speak] so the user always gets sound.
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

    /** Connected output devices the user can route speech to, as (deviceId, label). */
    fun availableOutputs(): List<Pair<String, String>> {
        val devices = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS).orEmpty()
        return devices
            .filter { it.type in SUPPORTED_DEVICE_TYPES }
            .map { it.id.toString() to deviceLabel(it) }
            .distinctBy { it.first }
            .sortedBy { (id, _) -> if (id == builtInSpeakerId()) 0 else 1 }
    }

    /** Synthesizes [text] and plays it, forcing the given [outputId] device when possible. */
    suspend fun speak(text: String, language: String, outputId: String, rate: Float, pitch: Float): Boolean =
        withContext(Dispatchers.IO) {
            val engine = tts ?: return@withContext false
            engine.language = Locale.forLanguageTag(if (language == "es") "es" else "en")
            engine.setSpeechRate(rate)
            engine.setPitch(pitch)

            val device = resolvePreferredDevice(outputId)
            val spoken = try {
                speakThroughTrack(engine, text, device)
            } catch (t: Throwable) {
                Log.e(TAG, "Custom TTS playback failed", t)
                false
            }
            if (spoken) {
                true
            } else {
                Log.w(TAG, "Custom TTS playback unavailable, falling back to system speech")
                engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
                true
            }
        }

    private suspend fun speakThroughTrack(engine: TextToSpeech, text: String, device: AudioDeviceInfo?): Boolean {
        val pipe = ParcelFileDescriptor.createPipe()
        try {
            val result = engine.synthesizeToFile(text, Bundle(), pipe[1], UTTERANCE_ID)
            pipe[1].close()
            if (result != TextToSpeech.SUCCESS) {
                Log.w(TAG, "synthesizeToFile returned $result")
                return false
            }

            val bytes = withTimeoutOrNull(SYNTHESIS_TIMEOUT_MILLIS) {
                FileInputStream(pipe[0].fileDescriptor).use { it.readBytes() }
            } ?: return false
            if (bytes.isEmpty()) {
                Log.w(TAG, "synthesis produced no audio")
                return false
            }

            playPcm(bytes, device)
            return true
        } finally {
            pipe[0].close()
            pipe[1].close()
        }
    }

    private fun resolvePreferredDevice(outputId: String): AudioDeviceInfo? {
        if (outputId == AudioOutputIds.AUTO) return null
        val devices = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS).orEmpty()
        return devices.firstOrNull { it.id.toString() == outputId }
    }

    private fun builtInSpeakerId(): String? =
        audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            ?.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            ?.id
            ?.toString()

    private fun deviceLabel(device: AudioDeviceInfo): String =
        runCatching { device.productName?.toString()?.takeIf { it.isNotBlank() } }
            .getOrNull() ?: when (device.type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Speaker"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired headset"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired headphones"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB headset"
            AudioDeviceInfo.TYPE_USB_DEVICE -> "USB audio"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth"
            else -> "Audio device"
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
        const val TAG = "TtsManager"
        const val SAMPLE_RATE = 22050
        const val UTTERANCE_ID = "unmute_utterance"
        const val SYNTHESIS_TIMEOUT_MILLIS = 15_000L
        const val PLAYBACK_POLL_MS = 50L

        val SUPPORTED_DEVICE_TYPES = intArrayOf(
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        )
    }
}
