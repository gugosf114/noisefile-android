package com.noisefile.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.noisefile.app.audio.CalibrationMath
import com.noisefile.app.audio.MicProfile
import com.noisefile.app.audio.MicStatus
import com.noisefile.app.audio.NoiseMeter
import com.noisefile.app.data.IncidentStore
import com.noisefile.app.data.RuleCatalog
import com.noisefile.app.model.AmbientReading
import com.noisefile.app.model.Incident
import com.noisefile.app.model.Jurisdiction
import com.noisefile.app.model.LevelCalibration
import com.noisefile.app.model.MeterReading
import com.noisefile.app.model.RuleWorkflow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

enum class AppScreen {
    HOME,
    METER,
    REVIEW,
    HISTORY,
}

/** What the microphone is measuring right now: the quiet baseline, or the noise. */
enum class CaptureStage {
    AMBIENT,
    NOISE,
    CALIBRATE,
}

data class NoiseFileUiState(
    val screen: AppScreen = AppScreen.HOME,
    val selectedJurisdictionId: String = RuleCatalog.SAN_JOSE_ID,
    val selectedRuleId: String = RuleCatalog.DEFAULT_RULE_ID,
    val meterReading: MeterReading = MeterReading(),
    val incidents: List<Incident> = emptyList(),
    val measurementStartedAt: Long? = null,
    val captureStage: CaptureStage = CaptureStage.NOISE,
    val ambient: AmbientReading? = null,
    val ambientTargetSeconds: Int = 0,
    val calibrationReferenceText: String = "",
    val draftLocation: String = "",
    val draftImpact: String = "Interrupted rest or quiet use",
    val draftNotes: String = "",
    val message: String? = null,
    val error: String? = null,
)

class NoiseFileViewModel(application: Application) : AndroidViewModel(application) {
    private val ruleCatalog = RuleCatalog.fromAssets(application)
    val jurisdictions: List<Jurisdiction> = ruleCatalog.jurisdictions
    val workflows: List<RuleWorkflow>
        get() = ruleCatalog.forJurisdiction(_uiState.value.selectedJurisdictionId)

    private val incidentStore = IncidentStore(application)
    private val noiseMeter = NoiseMeter(application)
    private val _uiState = MutableStateFlow(
        NoiseFileUiState(incidents = incidentStore.load()),
    )
    val uiState: StateFlow<NoiseFileUiState> = _uiState.asStateFlow()

    fun selectedRule(): RuleWorkflow =
        checkNotNull(ruleCatalog.byId(_uiState.value.selectedRuleId)) {
            "Selected rule is not present in the verified catalog."
        }

    fun selectedJurisdiction(): Jurisdiction =
        checkNotNull(ruleCatalog.jurisdictionById(_uiState.value.selectedJurisdictionId)) {
            "Selected jurisdiction is not present in the verified catalog."
        }

    /** Which microphone the next measurement would use, and how it is calibrated. */
    fun micStatus(): MicStatus = noiseMeter.inputStatus()

    /** The city's own ambient minutes when its code states them, else NoiseFile's default. */
    fun ambientTargetSecondsFor(rule: RuleWorkflow): Int =
        (rule.ambientRecipe?.minutes ?: DEFAULT_AMBIENT_MINUTES) * 60

    fun selectJurisdiction(jurisdictionId: String) {
        val jurisdiction = ruleCatalog.jurisdictionById(jurisdictionId) ?: return
        if (!jurisdiction.isAvailable) return
        _uiState.update {
            val currentNoiseType = ruleCatalog.byId(it.selectedRuleId)?.noiseType
            val matchingRule = currentNoiseType?.let { noiseType ->
                ruleCatalog.retrieve(jurisdiction.id, noiseType)
            }
            val selectedRule = matchingRule
                ?: ruleCatalog.forJurisdiction(jurisdiction.id).firstOrNull()
                ?: return@update it
            it.copy(
                selectedJurisdictionId = jurisdiction.id,
                selectedRuleId = selectedRule.id,
                // A baseline belongs to a spot; a new city is a new spot.
                ambient = null,
                message = null,
                error = null,
            )
        }
    }

    fun selectRule(ruleId: String) {
        val state = _uiState.value
        val rule = ruleCatalog.byId(ruleId) ?: return
        if (rule.jurisdictionId != state.selectedJurisdictionId) {
            _uiState.update {
                it.copy(
                    error = "That rule does not belong to the selected city.",
                    message = null,
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                selectedRuleId = ruleId,
                message = null,
                error = null,
            )
        }
    }

    fun showHome() {
        noiseMeter.stop()
        _uiState.update {
            it.copy(
                screen = AppScreen.HOME,
                captureStage = CaptureStage.NOISE,
                meterReading = MeterReading(),
                measurementStartedAt = null,
                error = null,
            )
        }
    }

    fun showHistory() {
        noiseMeter.stop()
        _uiState.update {
            it.copy(
                screen = AppScreen.HISTORY,
                captureStage = CaptureStage.NOISE,
                error = null,
            )
        }
    }

    /**
     * The quiet baseline: the same spot, the source silent, for the city's own
     * minutes. Ends on its own when the minutes are up, or early on the button.
     */
    fun startAmbientMeasurement() {
        val rule = selectedRule()
        val targetSeconds = ambientTargetSecondsFor(rule)
        val startedAt = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                screen = AppScreen.METER,
                captureStage = CaptureStage.AMBIENT,
                meterReading = MeterReading(),
                measurementStartedAt = startedAt,
                ambientTargetSeconds = targetSeconds,
                message = null,
                error = null,
            )
        }

        noiseMeter.start(
            onReading = { reading ->
                _uiState.update { state -> state.copy(meterReading = reading) }
                if (reading.elapsedMillis >= targetSeconds * 1_000L) finishAmbient()
            },
            onError = { error ->
                _uiState.update { state ->
                    state.copy(
                        screen = AppScreen.HOME,
                        captureStage = CaptureStage.NOISE,
                        error = error,
                        measurementStartedAt = null,
                    )
                }
            },
        )
    }

    fun finishAmbient() {
        val state = _uiState.value
        if (state.captureStage != CaptureStage.AMBIENT) return
        noiseMeter.stop()
        val reading = state.meterReading
        if (reading.sampleWindows == 0) {
            _uiState.update {
                it.copy(
                    screen = AppScreen.HOME,
                    captureStage = CaptureStage.NOISE,
                    error = "No sound samples were captured for the quiet baseline. Try again and keep the app open.",
                    measurementStartedAt = null,
                )
            }
            return
        }
        val ambient = AmbientReading(
            db = reading.averageDb,
            seconds = max(1L, reading.elapsedMillis / 1_000L),
            sampleWindows = reading.sampleWindows,
            calibration = reading.calibration,
        )
        _uiState.update {
            it.copy(
                screen = AppScreen.HOME,
                captureStage = CaptureStage.NOISE,
                ambient = ambient,
                meterReading = MeterReading(),
                measurementStartedAt = null,
                message = "Quiet baseline saved: ${ambient.db.roundToInt()} dB over " +
                    "${ambient.seconds / 60}:${"%02d".format(ambient.seconds % 60)}. " +
                    "Now record the noise from the same spot.",
                error = null,
            )
        }
    }

    /**
     * Calibrate this microphone against a reference meter: the meter runs, the
     * user reads the reference and types it, the difference is saved per mic.
     */
    fun startCalibration() {
        val startedAt = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                screen = AppScreen.METER,
                captureStage = CaptureStage.CALIBRATE,
                meterReading = MeterReading(),
                measurementStartedAt = startedAt,
                calibrationReferenceText = "",
                message = null,
                error = null,
            )
        }
        noiseMeter.start(
            onReading = { reading ->
                _uiState.update { state -> state.copy(meterReading = reading) }
            },
            onError = { error ->
                _uiState.update { state ->
                    state.copy(
                        screen = AppScreen.HOME,
                        captureStage = CaptureStage.NOISE,
                        error = error,
                        measurementStartedAt = null,
                    )
                }
            },
        )
    }

    fun setCalibrationReference(text: String) {
        _uiState.update { it.copy(calibrationReferenceText = text, error = null) }
    }

    fun finishCalibration() {
        val state = _uiState.value
        if (state.captureStage != CaptureStage.CALIBRATE) return
        val reference = state.calibrationReferenceText.trim().toDoubleOrNull()
        if (reference == null || !CalibrationMath.isPlausibleReference(reference)) {
            _uiState.update {
                it.copy(error = "Type the reference meter's reading in dB, between 30 and 120.")
            }
            return
        }
        val reading = state.meterReading
        if (reading.sampleWindows < 20) {
            _uiState.update {
                it.copy(error = "Let the meter run a few seconds in a steady sound before saving.")
            }
            return
        }
        noiseMeter.stop()
        val offset = CalibrationMath.newUserOffset(
            existingUserOffsetDb = reading.userOffsetDb,
            phoneAverageDb = reading.averageDb,
            referenceDb = reference,
        )
        noiseMeter.saveCalibration(
            MicProfile(
                micKey = reading.micKey,
                offsetDb = offset,
                referenceLabel = "reference meter",
                calibratedAtEpochMillis = System.currentTimeMillis(),
            ),
        )
        _uiState.update {
            it.copy(
                screen = AppScreen.HOME,
                captureStage = CaptureStage.NOISE,
                meterReading = MeterReading(),
                measurementStartedAt = null,
                calibrationReferenceText = "",
                message = "Calibrated ${reading.micLabel}: this phone averaged ${reading.averageDb.roundToInt()} dB, " +
                    "the meter said ${reference.roundToInt()} dB, so ${CalibrationMath.signed(offset)} is saved for it.",
                error = null,
            )
        }
    }

    fun clearCalibration() {
        val status = noiseMeter.inputStatus()
        noiseMeter.clearCalibration(status.micKey)
        _uiState.update {
            it.copy(message = "Calibration cleared for ${status.micLabel}.", error = null)
        }
    }

    fun startMeasurement() {
        val startedAt = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                screen = AppScreen.METER,
                captureStage = CaptureStage.NOISE,
                meterReading = MeterReading(),
                measurementStartedAt = startedAt,
                message = null,
                error = null,
            )
        }

        noiseMeter.start(
            onReading = { reading ->
                _uiState.update { state -> state.copy(meterReading = reading) }
            },
            onError = { error ->
                _uiState.update { state ->
                    state.copy(
                        screen = AppScreen.HOME,
                        captureStage = CaptureStage.NOISE,
                        error = error,
                        measurementStartedAt = null,
                    )
                }
            },
        )
    }

    fun microphonePermissionDenied() {
        _uiState.update {
            it.copy(
                error = "Microphone access is required to measure and document an incident.",
                message = null,
            )
        }
    }

    fun stopMeasurement() {
        if (_uiState.value.captureStage == CaptureStage.AMBIENT) {
            finishAmbient()
            return
        }
        if (_uiState.value.captureStage == CaptureStage.CALIBRATE) {
            finishCalibration()
            return
        }
        noiseMeter.stop()
        if (_uiState.value.meterReading.sampleWindows == 0) {
            _uiState.update {
                it.copy(
                    screen = AppScreen.HOME,
                    error = "No sound samples were captured. Try again and keep the app open.",
                    measurementStartedAt = null,
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                screen = AppScreen.REVIEW,
                draftImpact = "Interrupted rest or quiet use",
                draftNotes = "",
                error = null,
            )
        }
    }

    fun setImpact(impact: String) {
        _uiState.update { it.copy(draftImpact = impact) }
    }

    fun setLocation(location: String) {
        _uiState.update { it.copy(draftLocation = location, error = null) }
    }

    fun setNotes(notes: String) {
        _uiState.update { it.copy(draftNotes = notes) }
    }

    fun saveIncident(): Incident? {
        val state = _uiState.value
        val location = state.draftLocation.trim()
        if (location.isBlank()) {
            _uiState.update {
                it.copy(
                    error = "Add the location of the disturbance before saving.",
                    message = null,
                )
            }
            return null
        }
        val startedAt = state.measurementStartedAt ?: System.currentTimeMillis()
        val reading = state.meterReading
        val rule = ruleCatalog.byId(state.selectedRuleId)
        if (rule == null || rule.jurisdictionId != state.selectedJurisdictionId) {
            _uiState.update {
                it.copy(
                    screen = AppScreen.HOME,
                    error = "The selected rule is no longer available for this city.",
                    message = null,
                )
            }
            return null
        }
        val incident = Incident(
            id = System.currentTimeMillis(),
            ruleId = rule.id,
            noiseType = rule.noiseType,
            startedAtEpochMillis = startedAt,
            durationSeconds = max(1L, reading.elapsedMillis / 1_000L),
            minimumDb = reading.minimumDb,
            averageDb = reading.averageDb,
            maximumDb = reading.maximumDb,
            location = location,
            impact = state.draftImpact,
            notes = state.draftNotes.trim(),
            ambientDb = state.ambient?.db,
            ambientSeconds = state.ambient?.seconds,
            levelNote = levelNoteFor(reading),
        )
        val incidents = incidentStore.add(incident)
        _uiState.update {
            it.copy(
                screen = AppScreen.HOME,
                incidents = incidents,
                meterReading = MeterReading(),
                measurementStartedAt = null,
                ambient = null,
                draftNotes = "",
                message = "Incident saved to your private history.",
                error = null,
            )
        }
        return incident
    }

    /** For the complaint: which microphone made the numbers and how it was calibrated. */
    private fun levelNoteFor(reading: MeterReading): String? = when (reading.calibration) {
        LevelCalibration.USER_CALIBRATED -> {
            val profile = noiseMeter.inputStatus().profile
            val day = profile?.let {
                DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
                    .format(Instant.ofEpochMilli(it.calibratedAtEpochMillis).atZone(ZoneId.systemDefault()))
            }
            "The sound levels above come from my phone's ${reading.micLabel.lowercase(Locale.US)}, " +
                "calibrated against a reference meter" + (day?.let { " on $it" } ?: "") +
                " (${CalibrationMath.signed(reading.userOffsetDb)}), and are included as incident context."
        }
        LevelCalibration.PLATFORM_SPEC ->
            "The sound levels above come from my phone's built-in microphone on its unprocessed path, whose level " +
                "Android's compatibility specification pins (94 dB SPL reads -36 dBFS); they are included as incident context."
        LevelCalibration.ESTIMATE -> null
    }

    fun updateIncidentDetails(
        incidentId: Long,
        location: String,
        notes: String,
    ) {
        val incidents = incidentStore.updateDetails(incidentId, location, notes)
        _uiState.update {
            it.copy(
                incidents = incidents,
                message = "Incident details updated.",
                error = null,
            )
        }
    }

    fun ruleForIncident(ruleId: String): RuleWorkflow? = ruleCatalog.byId(ruleId)

    fun incidentCountFor(ruleId: String): Int =
        _uiState.value.incidents.count { it.ruleId == ruleId }

    override fun onCleared() {
        noiseMeter.stop()
        super.onCleared()
    }

    private companion object {
        /** Used when the city's code states no ambient recipe. */
        const val DEFAULT_AMBIENT_MINUTES = 5
    }
}
