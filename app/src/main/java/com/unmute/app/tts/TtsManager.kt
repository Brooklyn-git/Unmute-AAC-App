package com.unmute.app.tts

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.unmute.app.domain.model.AudioOutputIds
import java.io.FileInputStream
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

enum class TtsIssue { UNAVAILABLE, SPEAK_FAILED }

/**
 * Speaks through the user's default TTS engine. Android's TextToSpeech cannot
 * route to a specific output device, so when the user picks a concrete device we
 * synthesize into a pipe, play it through our own [AudioTrack] and force the
 * device. Otherwise we use the engine's normal [TextToSpeech.speak], which is
 * the most reliable path and always honours the system's default engine.
 */
class TtsManager(
    context: Context,
    initialEngine: String? = null,
) {

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(AudioManager::class.java)

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _errors = MutableSharedFlow<TtsIssue>(extraBufferCapacity = 8)
    val errors: SharedFlow<TtsIssue> = _errors.asSharedFlow()

    private var tts: TextToSpeech? = null
    private var initStatus = TextToSpeech.ERROR
    private var selectedEngine: String? = initialEngine
    private val startedUtterance = AtomicReference<String?>(null)

    init {
        initEngine(initialEngine)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    /** Installed TTS engines as (packageName, label), independent of which one is selected. */
    fun engines(): List<Pair<String, String>> {
        val intent = Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE)
        return appContext.packageManager.queryIntentServices(intent, 0).map { service ->
            val packageName = service.serviceInfo.packageName
            packageName to (resolveEngineLabel(packageName) ?: packageName)
        }
    }

    /** Re-initializes speech with [packageName], or the system default when null. */
    fun selectEngine(packageName: String?) {
        initEngine(packageName)
    }

    private fun initEngine(packageName: String?) {
        tts?.stop()
        tts?.shutdown()
        tts = null
        selectedEngine = packageName
        _isReady.value = false
        tts = if (packageName == null) {
            TextToSpeech(appContext, ::onInit)
        } else {
            TextToSpeech(appContext, ::onInit, packageName)
        }
    }

    private fun onInit(status: Int) {
        initStatus = status
        _isReady.value = status == TextToSpeech.SUCCESS
        Log.i(TAG, "TTS init status=$status engine=$selectedEngine")
    }

    /** Display name of the engine in use, or null if unavailable. */
    fun currentEngineLabel(): String? {
        val packageName = selectedEngine ?: tts?.defaultEngine ?: return null
        return resolveEngineLabel(packageName) ?: packageName
    }

    private fun resolveEngineLabel(packageName: String): String? =
        runCatching {
            val info = appContext.packageManager.getApplicationInfo(packageName, 0)
            appContext.packageManager.getApplicationLabel(info).toString()
        }.getOrNull()

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
            val engine = tts
            if (engine == null) {
                Log.e(TAG, "TTS engine missing")
                _errors.emit(TtsIssue.UNAVAILABLE)
                return@withContext false
            }
            if (withTimeoutOrNull(ENGINE_READY_TIMEOUT_MILLIS) { _isReady.first { it } } == null) {
                Log.e(TAG, "TTS engine not ready, init status=$initStatus")
                _errors.emit(TtsIssue.UNAVAILABLE)
                return@withContext false
            }

            configureLanguage(engine, language)
            engine.setSpeechRate(rate)
            engine.setPitch(pitch)
            installListener(engine)

            val device = resolvePreferredDevice(outputId)
            if (device != null && speakThroughTrackSafely(engine, text, device)) {
                return@withContext true
            }

            if (speakViaEngine(engine, text)) {
                return@withContext true
            }

            Log.w(TAG, "engine.speak reported success but produced no audio, trying own playback")
            if (speakThroughTrackSafely(engine, text, device)) {
                return@withContext true
            }

            Log.e(TAG, "No TTS playback path produced sound")
            _errors.emit(TtsIssue.SPEAK_FAILED)
            false
        }

    private suspend fun speakThroughTrackSafely(engine: TextToSpeech, text: String, device: AudioDeviceInfo?): Boolean =
        try {
            speakThroughTrack(engine, text, device)
        } catch (t: Throwable) {
            Log.e(TAG, "Custom TTS playback failed", t)
            false
        }

    /** Speaks natively and returns true only if the engine actually started the utterance. */
    private suspend fun speakViaEngine(engine: TextToSpeech, text: String): Boolean {
        startedUtterance.set(null)
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, speechParams(), UTTERANCE_ID)
        if (result != TextToSpeech.SUCCESS) {
            Log.e(TAG, "engine.speak returned $result")
            return false
        }
        return withTimeoutOrNull(UTTERANCE_START_TIMEOUT_MILLIS) {
            while (startedUtterance.get() != UTTERANCE_ID) {
                delay(UTTERANCE_POLL_MS)
            }
        } != null
    }

    private fun configureLanguage(engine: TextToSpeech, language: String) {
        val locale = Locale.forLanguageTag(if (language == "es") "es" else "en")
        val result = engine.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Language $locale unsupported by engine, falling back to default")
            engine.setLanguage(Locale.getDefault())
        }
    }

    private fun installListener(engine: TextToSpeech) {
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                startedUtterance.set(utteranceId)
            }

            override fun onDone(utteranceId: String?) = Unit

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e(TAG, "Utterance error: $utteranceId")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e(TAG, "Utterance error $utteranceId code=$errorCode")
            }
        })
    }

    private fun speechParams(): Bundle =
        Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f)
        }

    private suspend fun speakThroughTrack(engine: TextToSpeech, text: String, device: AudioDeviceInfo?): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Log.i(TAG, "Custom track playback needs API 30+, falling back to engine routing")
            return false
        }
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
        const val ENGINE_READY_TIMEOUT_MILLIS = 5_000L
        const val UTTERANCE_START_TIMEOUT_MILLIS = 2_500L
        const val UTTERANCE_POLL_MS = 50L
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
