package com.noisefile.app.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.MicrophoneInfo
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import com.noisefile.app.model.LevelCalibration
import com.noisefile.app.model.MeterReading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class NoiseMeter(private val context: Context) {
    private companion object {
        const val TAG = "NoiseMeter"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var recordingJob: Job? = null
    private var audioRecord: AudioRecord? = null

    @SuppressLint("MissingPermission")
    fun start(
        onReading: (MeterReading) -> Unit,
        onError: (String) -> Unit,
    ) {
        stop()

        val sampleRate = 48_000
        val minimumBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minimumBuffer <= 0) {
            onError("This phone could not initialize its microphone.")
            return
        }

        val bufferSize = max(minimumBuffer * 2, 4096)
        val audioSource = preferredAudioSource()
        // The UNPROCESSED path is level-calibrated by the Android CDD (5.11 C-1-5);
        // every other path carries the phone's own gain and stays an estimate.
        val calibration = if (audioSource == MediaRecorder.AudioSource.UNPROCESSED) {
            LevelCalibration.PLATFORM_SPEC
        } else {
            LevelCalibration.ESTIMATE
        }
        val offsetDb = if (calibration == LevelCalibration.PLATFORM_SPEC) {
            NoiseMath.CDD_UNPROCESSED_OFFSET_DBA
        } else {
            NoiseMath.ESTIMATE_OFFSET_DBA
        }
        val record = runCatching {
            AudioRecord.Builder()
                .setAudioSource(audioSource)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(bufferSize)
                .build()
        }.getOrElse {
            onError("The microphone is unavailable. Close other recording apps and try again.")
            return
        }

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            onError("This phone could not initialize its microphone.")
            return
        }

        audioRecord = record
        recordingJob = scope.launch {
            val samples = ShortArray(bufferSize / 2)
            val startedAt = SystemClock.elapsedRealtime()
            var minimum = Double.MAX_VALUE
            var maximum = 0.0
            var energyTotal = 0.0
            var windows = 0

            try {
                val filter = AWeightingFilter()
                record.startRecording()
                logMicrophoneFacts(record, audioSource, calibration, offsetDb)
                while (isActive && record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val count = record.read(samples, 0, samples.size, AudioRecord.READ_BLOCKING)
                    when (classifyAudioReadResult(count)) {
                        AudioReadAction.PROCESS -> Unit
                        AudioReadAction.RETRY -> continue
                        AudioReadAction.FAIL -> {
                            onError("Measurement stopped because the microphone became unavailable.")
                            break
                        }
                    }

                    val filteredSamples = filter.process(samples, count)
                    val current = NoiseMath.rmsToEstimatedDbA(filteredSamples, count, offsetDb)
                    minimum = min(minimum, current)
                    maximum = max(maximum, current)
                    energyTotal += 10.0.pow(current / 10.0)
                    windows += 1
                    val average = 10.0 * log10(energyTotal / windows)

                    onReading(
                        MeterReading(
                            currentDb = current,
                            minimumDb = if (minimum == Double.MAX_VALUE) current else minimum,
                            averageDb = average,
                            maximumDb = maximum,
                            elapsedMillis = SystemClock.elapsedRealtime() - startedAt,
                            sampleWindows = windows,
                            calibration = calibration,
                        ),
                    )
                }
            } catch (_: Throwable) {
                if (isActive) onError("Measurement stopped because the microphone became unavailable.")
            } finally {
                runCatching { record.stop() }
                record.release()
                if (audioRecord === record) audioRecord = null
            }
        }
    }

    fun stop() {
        recordingJob?.cancel()
        recordingJob = null
        runCatching { audioRecord?.stop() }
        audioRecord = null
    }

    /**
     * One log line per measurement with what the platform says about its own
     * microphone: the capture path, the offset in use, and MicrophoneInfo's
     * sensitivity (dBFS at 94 dB SPL, CDD 5.4.1 C-1-4 says devices must fill it).
     * Read it with `adb logcat -s NoiseMeter` on any phone under test.
     */
    private fun logMicrophoneFacts(
        record: AudioRecord,
        audioSource: Int,
        calibration: LevelCalibration,
        offsetDb: Double,
    ) {
        val sourceName = if (audioSource == MediaRecorder.AudioSource.UNPROCESSED) "UNPROCESSED" else "VOICE_RECOGNITION"
        val micFacts = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            microphoneFacts(record)
        } else {
            "microphones=api<28"
        }
        Log.i(
            TAG,
            "source=$sourceName calibration=$calibration offset=$offsetDb " +
                "model=${Build.MANUFACTURER} ${Build.MODEL} sdk=${Build.VERSION.SDK_INT} " +
                micFacts,
        )
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun microphoneFacts(record: AudioRecord): String {
        val microphones = runCatching { record.activeMicrophones }.getOrDefault(emptyList())
        if (microphones.isEmpty()) return "microphones=none reported"
        return microphones.joinToString(" | ") { info ->
            val sensitivity = if (info.sensitivity == MicrophoneInfo.SENSITIVITY_UNKNOWN) {
                "unknown"
            } else {
                String.format(Locale.US, "%.1f dBFS@94", info.sensitivity)
            }
            val maxSpl = if (info.maxSpl == MicrophoneInfo.SPL_UNKNOWN) "unknown" else String.format(Locale.US, "%.0f", info.maxSpl)
            val minSpl = if (info.minSpl == MicrophoneInfo.SPL_UNKNOWN) "unknown" else String.format(Locale.US, "%.0f", info.minSpl)
            "mic ${info.id} sensitivity=$sensitivity maxSpl=$maxSpl minSpl=$minSpl"
        }
    }

    private fun preferredAudioSource(): Int {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val supportsUnprocessed = audioManager
            .getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED)
            ?.toBooleanStrictOrNull() == true
        return if (supportsUnprocessed) {
            MediaRecorder.AudioSource.UNPROCESSED
        } else {
            MediaRecorder.AudioSource.VOICE_RECOGNITION
        }
    }
}

internal enum class AudioReadAction {
    PROCESS,
    RETRY,
    FAIL,
}

internal fun classifyAudioReadResult(sampleCount: Int): AudioReadAction = when {
    sampleCount > 0 -> AudioReadAction.PROCESS
    sampleCount == 0 -> AudioReadAction.RETRY
    else -> AudioReadAction.FAIL
}
