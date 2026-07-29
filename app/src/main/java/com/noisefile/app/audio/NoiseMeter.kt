package com.noisefile.app.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import com.noisefile.app.model.MeterReading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class NoiseMeter(private val context: Context) {
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
                while (isActive && record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val count = record.read(samples, 0, samples.size, AudioRecord.READ_BLOCKING)
                    if (count <= 0) continue

                    val filteredSamples = filter.process(samples, count)
                    val current = NoiseMath.rmsToEstimatedDbA(filteredSamples, count)
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
