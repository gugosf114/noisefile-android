package com.noisefile.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.noisefile.app.BuildConfig
import com.noisefile.app.audio.CalibrationMath
import com.noisefile.app.billing.UnlockState
import com.noisefile.app.billing.UnlockStore
import com.noisefile.app.ui.TourStep
import com.noisefile.app.audio.MicProfile
import com.noisefile.app.audio.MicStatus
import com.noisefile.app.audio.NoiseMeter
import com.noisefile.app.audio.SelfTestOutcome
import com.noisefile.app.audio.SelfTestResult
import com.noisefile.app.data.IncidentFiles
import com.noisefile.app.data.IncidentStore
import com.noisefile.app.data.Baseline
import com.noisefile.app.data.BaselineStore
import android.net.Uri
import java.io.File
import com.noisefile.app.data.LevelTraceRecorder
import com.noisefile.app.data.QuietFloor
import com.noisefile.app.data.EvidenceSeal
import com.noisefile.app.data.RuleCatalog
import com.noisefile.app.model.AmbientReading
import com.noisefile.app.model.Incident
import com.noisefile.app.model.IncidentDetails
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
    RULES,
    MORE,
    /** The city's form box by box, with the app's answers, before the browser opens. */
    FORM_GUIDE,
}

/** What the microphone is measuring right now: the quiet baseline, or the noise. */
enum class CaptureStage {
    AMBIENT,
    NOISE,
    CALIBRATE,
    /** The microphone's own check: the phone plays tones and listens. Runs on Home, in a dialog. */
    SELF_TEST,
}

/** What the phone is being checked against while calibrating. */
enum class CalibrationMode {
    /** Press the smoke alarm's test button 10 feet away; the alarm is 85 dBA by law. */
    SMOKE_ALARM,
    /** A sound level meter held next to the phone; the person types its reading. */
    METER,
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
    val calibrationMode: CalibrationMode = CalibrationMode.SMOKE_ALARM,
    /** True while the once-only "make your numbers count" card should show on Home. */
    val showCalibrationPrompt: Boolean = false,
    /** The finger that points: which stop of the first-run tour is lit, or null. */
    val tourStep: TourStep? = null,
    /** After a quiet run: how many earlier incidents in this room have no quiet and could take this one. */
    val quietAttachCount: Int = 0,
    /** Every room's quiet, measured once and kept. */
    val baselines: List<Baseline> = emptyList(),
    /** The room picked on the quiet card, and the room picked on the review screen. */
    val quietRoom: String? = null,
    val draftRoom: String? = null,
    val selfTestRunning: Boolean = false,
    /** Which of the six tones is playing, 1..6. */
    val selfTestStep: Int = 0,
    val selfTestResult: SelfTestResult? = null,
    val draftLocation: String = "",
    val draftImpact: String = "Interrupted rest or quiet use",
    /** What exactly was heard, one optional tap. */
    val draftSoundKind: String? = null,
    val draftNotes: String = "",
    val message: String? = null,
    val error: String? = null,
    /** Staged files for the incident being reviewed. */
    val draftPhotos: List<File> = emptyList(),
    val draftClip: File? = null,
    val draftClipSeconds: Int = 0,
    /** The saved incident whose form guide is open. */
    val formGuideIncidentId: Long? = null,
)

class NoiseFileViewModel(application: Application) : AndroidViewModel(application) {
    private val ruleCatalog = RuleCatalog.fromAssets(application)
    val jurisdictions: List<Jurisdiction> = ruleCatalog.jurisdictions
    val workflows: List<RuleWorkflow>
        get() = ruleCatalog.forJurisdiction(_uiState.value.selectedJurisdictionId)

    private val incidentStore = IncidentStore(application)
    private val baselineStore = BaselineStore(application)
    private val trace = LevelTraceRecorder()
    /** The quiet run's own second-by-second trace; its 10th percentile is the baseline. */
    private val quietTrace = LevelTraceRecorder()
    private val files = IncidentFiles(application)
    private val noiseMeter = NoiseMeter(application)
    private val tourPrefs = application.getSharedPreferences("noisefile_tour", android.content.Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(
        NoiseFileUiState(
            incidents = incidentStore.load(),
            baselines = baselineStore.load(),
            quietRoom = baselineStore.lastRoom,
            draftRoom = baselineStore.lastRoom,
            // First open ever: the tour runs once on its own. After that, only from More.
            tourStep = if (tourPrefs.getBoolean(KEY_TOUR_SEEN, false)) null else TourStep.CITY,
        ),
    )
    val uiState: StateFlow<NoiseFileUiState> = _uiState.asStateFlow()

    /** The person says this quiet belongs to their earlier incidents at the same spot. */
    fun attachQuietToEarlierIncidents() {
        val state = _uiState.value
        val ambient = state.ambient ?: return
        val incidents = incidentStore.attachLaterQuiet(
            room = state.quietRoom ?: "Home",
            db = ambient.db,
            seconds = ambient.seconds,
            atEpochMillis = System.currentTimeMillis(),
        )
        _uiState.update { it.copy(incidents = incidents, quietAttachCount = 0, message = "Quiet level attached to your earlier incidents, marked as measured later.") }
    }

    fun dismissQuietAttach() = _uiState.update { it.copy(quietAttachCount = 0) }

    fun setQuietRoom(room: String) {
        baselineStore.rememberRoom(room)
        _uiState.update { it.copy(quietRoom = room, draftRoom = it.draftRoom ?: room) }
    }

    fun setDraftRoom(room: String) {
        baselineStore.rememberRoom(room)
        _uiState.update { it.copy(draftRoom = room) }
    }

    /** The quiet on file for a room, if any. */
    fun baselineFor(room: String?): Baseline? = room?.let { r -> _uiState.value.baselines.firstOrNull { it.room.equals(r, ignoreCase = true) } }

    fun startTour() {
        _uiState.update { it.copy(screen = AppScreen.HOME, tourStep = TourStep.CITY) }
    }

    fun tourNext() {
        val current = _uiState.value.tourStep ?: return
        val next = current.next
        if (next == null) endTour() else _uiState.update { it.copy(tourStep = next) }
    }

    fun endTour() {
        tourPrefs.edit().putBoolean(KEY_TOUR_SEEN, true).apply()
        _uiState.update { it.copy(tourStep = null) }
    }

    /** The one-time unlock (PDF report, form guide, filled-in email). Play is asked at start. */
    private val unlockStore = UnlockStore(application).also { it.start() }
    val unlock: StateFlow<UnlockState> = unlockStore.state

    /** The preview build on the phone has no Play listing, so it is always open. */
    fun isUnlocked(): Boolean = BuildConfig.DEBUG || unlock.value.unlocked
    fun buyUnlock(activity: android.app.Activity) = unlockStore.buy(activity)
    fun restoreUnlock() = unlockStore.restore()
    fun clearUnlockMessage() = unlockStore.clearMessage()

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

    /** Open the city's form guide for a saved incident. */
    fun showFormGuide(incidentId: Long) {
        noiseMeter.stop()
        _uiState.update {
            it.copy(
                screen = AppScreen.FORM_GUIDE,
                formGuideIncidentId = incidentId,
                captureStage = CaptureStage.NOISE,
                error = null,
            )
        }
    }

    fun showRules() {
        noiseMeter.stop()
        _uiState.update {
            it.copy(
                screen = AppScreen.RULES,
                captureStage = CaptureStage.NOISE,
                error = null,
            )
        }
    }

    fun showMore() {
        noiseMeter.stop()
        _uiState.update {
            it.copy(
                screen = AppScreen.MORE,
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

        quietTrace.reset()
        noiseMeter.start(
            onReading = { reading ->
                quietTrace.add(reading.elapsedMillis, reading.currentDb)
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
        // The floor the room sits at (L90), never the energy average: a sneeze must not become the baseline.
        val ambient = AmbientReading(
            db = QuietFloor.of(quietTrace.snapshot()) ?: reading.minimumDb,
            seconds = max(1L, reading.elapsedMillis / 1_000L),
            sampleWindows = reading.sampleWindows,
            calibration = reading.calibration,
        )
        val room = _uiState.value.quietRoom ?: "Home"
        val rule = selectedRule()
        val baselines = baselineStore.save(
            Baseline(
                room = room,
                db = ambient.db,
                seconds = ambient.seconds,
                measuredAtEpochMillis = System.currentTimeMillis(),
                codeMinutes = rule.ambientRecipe?.minutes,
                cityName = selectedJurisdiction().displayName,
            ),
        )
        val attachable = _uiState.value.incidents.count { it.room.equals(room, ignoreCase = true) && it.ambientDb == null }
        _uiState.update {
            it.copy(
                screen = AppScreen.HOME,
                captureStage = CaptureStage.NOISE,
                ambient = ambient,
                baselines = baselines,
                quietAttachCount = attachable,
                meterReading = MeterReading(),
                measurementStartedAt = null,
                message = "$room baseline saved: ${ambient.db.roundToInt()} dB over " +
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
    fun startCalibration(mode: CalibrationMode = CalibrationMode.SMOKE_ALARM) {
        val startedAt = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                screen = AppScreen.METER,
                captureStage = CaptureStage.CALIBRATE,
                calibrationMode = mode,
                meterReading = MeterReading(),
                measurementStartedAt = startedAt,
                calibrationReferenceText = "",
                message = null,
                error = null,
            )
        }
        noiseMeter.start(
            listenForAlarm = mode == CalibrationMode.SMOKE_ALARM,
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
        val reading = state.meterReading
        val alarm = state.calibrationMode == CalibrationMode.SMOKE_ALARM
        val reference: Double
        val phoneDb: Double
        if (alarm) {
            // The alarm beeps in bursts; its loudest window is the 85 dBA the standard names.
            // Only the alarm's own tone counts. A loud room, a voice, a TV never calibrate the phone.
            reference = CalibrationMath.SMOKE_ALARM_DBA_AT_10_FT
            phoneDb = reading.alarmToneDb
            if (reading.alarmToneDb <= 0.0) {
                _uiState.update { it.copy(error = "No alarm heard yet. Press and hold the test button, 10 feet from the phone.") }
                return
            }
        } else {
            val typed = state.calibrationReferenceText.trim().toDoubleOrNull()
            if (typed == null || !CalibrationMath.isPlausibleReference(typed)) {
                _uiState.update { it.copy(error = "Type the meter's reading in dB, between 30 and 120.") }
                return
            }
            if (reading.sampleWindows < 20) {
                _uiState.update { it.copy(error = "Let the meter run a few seconds in a steady sound before saving.") }
                return
            }
            reference = typed
            phoneDb = reading.averageDb
        }
        val offset = CalibrationMath.newUserOffset(
            existingUserOffsetDb = reading.userOffsetDb,
            phoneAverageDb = phoneDb,
            referenceDb = reference,
        )
        if (kotlin.math.abs(offset) > com.noisefile.app.audio.AlarmTone.MAX_PLAUSIBLE_OFFSET_DB) {
            _uiState.update {
                it.copy(error = "That would move the phone's numbers by ${CalibrationMath.signed(offset)}, too far to be right. " +
                    if (alarm) "Stand 10 feet from the alarm and try again." else "Check the meter's reading and try again.")
            }
            return
        }
        noiseMeter.stop()
        noiseMeter.saveCalibration(
            MicProfile(
                micKey = reading.micKey,
                offsetDb = offset,
                referenceLabel = if (alarm) "smoke alarm test button, 85 dB at 10 feet" else "sound level meter",
                calibratedAtEpochMillis = System.currentTimeMillis(),
            ),
        )
        noiseMeter.calibrationPromptDismissed = true
        _uiState.update {
            it.copy(
                screen = AppScreen.HOME,
                captureStage = CaptureStage.NOISE,
                meterReading = MeterReading(),
                measurementStartedAt = null,
                calibrationReferenceText = "",
                showCalibrationPrompt = false,
                message = if (alarm) {
                    "Calibrated ${reading.micLabel} with your smoke alarm: the phone heard ${phoneDb.roundToInt()} dB, " +
                        "the alarm is ${reference.roundToInt()} dB, so ${CalibrationMath.signed(offset)} is saved."
                } else {
                    "Calibrated ${reading.micLabel}: this phone averaged ${phoneDb.roundToInt()} dB, " +
                        "the meter said ${reference.roundToInt()} dB, so ${CalibrationMath.signed(offset)} is saved for it."
                },
                error = null,
            )
        }
    }

    /** The microphone checks itself: six tones from the phone's own speaker. */
    fun startSelfTest() {
        _uiState.update { it.copy(selfTestRunning = true, selfTestStep = 1, selfTestResult = null, message = null, error = null) }
        noiseMeter.runSelfTest(
            onProgress = { step -> _uiState.update { it.copy(selfTestStep = step) } },
            onResult = { result ->
                if (result.outcome == SelfTestOutcome.READS_STRAIGHT) noiseMeter.calibrationPromptDismissed = true
                _uiState.update {
                    it.copy(
                        selfTestRunning = false,
                        selfTestResult = result,
                        showCalibrationPrompt = it.showCalibrationPrompt && result.outcome != SelfTestOutcome.READS_STRAIGHT,
                    )
                }
            },
        )
    }

    fun dismissSelfTest() {
        noiseMeter.stop()
        _uiState.update { it.copy(selfTestRunning = false, selfTestResult = null) }
    }

    fun skipCalibrationPrompt() {
        noiseMeter.calibrationPromptDismissed = true
        _uiState.update { it.copy(showCalibrationPrompt = false) }
    }

    /** The card shows once: after the first saved incident, while the numbers are still estimates. */
    private fun shouldPromptCalibration(incidents: List<Incident>): Boolean =
        incidents.isNotEmpty() && !noiseMeter.calibrationPromptDismissed &&
            noiseMeter.inputStatus().let { it.profile == null && it.declaredSensitivity == null && it.selfTest?.passed != true }

    fun clearCalibration() {
        val status = noiseMeter.inputStatus()
        noiseMeter.clearCalibration(status.micKey)
        _uiState.update {
            it.copy(message = "Calibration cleared for ${status.micLabel}.", error = null)
        }
    }

    fun startMeasurement() {
        val startedAt = System.currentTimeMillis()
        trace.reset()
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

        files.clearDraft()
        _uiState.update { it.copy(draftPhotos = emptyList(), draftClip = null, draftClipSeconds = 0) }
        noiseMeter.start(
            keepClip = true,
            onReading = { reading ->
                trace.add(reading.elapsedMillis, reading.currentDb)
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
        val clip = noiseMeter.takeClip()?.let { (bytes, seconds) -> files.stageDraftClip(bytes) to seconds }
        _uiState.update {
            it.copy(
                screen = AppScreen.REVIEW,
                draftImpact = "Interrupted rest or quiet use",
                draftSoundKind = null,
                draftNotes = "",
                draftClip = clip?.first,
                draftClipSeconds = clip?.second ?: 0,
                error = null,
            )
        }
    }

    /** A picked photo joins the draft (at most two). */
    fun addDraftPhoto(uri: Uri) {
        val current = _uiState.value.draftPhotos
        if (current.size >= 2) return
        val staged = runCatching { files.stageDraftPhoto(uri, current.size + 1) }.getOrNull()
        if (staged == null) {
            _uiState.update { it.copy(error = "That photo could not be read.") }
            return
        }
        _uiState.update { it.copy(draftPhotos = current + staged, error = null) }
    }

    fun removeDraftPhoto(file: File) {
        file.delete()
        _uiState.update { it.copy(draftPhotos = it.draftPhotos.filterNot { f -> f == file }) }
    }

    fun dropDraftClip() {
        _uiState.value.draftClip?.delete()
        _uiState.update { it.copy(draftClip = null, draftClipSeconds = 0) }
    }

    fun incidentFile(incident: Incident, name: String): File = files.file(incident.id, name)

    fun setImpact(impact: String) {
        _uiState.update { it.copy(draftImpact = impact) }
    }

    /** Tap a kind to pick it; tap it again to clear. */
    fun setSoundKind(kind: String?) {
        _uiState.update { it.copy(draftSoundKind = if (it.draftSoundKind == kind) null else kind) }
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
        val incidentId = System.currentTimeMillis()
        val committed = files.commit(incidentId, state.draftPhotos, state.draftClip)
        val incident = Incident(
            id = incidentId,
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
            soundKind = state.draftSoundKind,
            room = state.draftRoom,
            ambientDb = baselineFor(state.draftRoom)?.db,
            ambientSeconds = baselineFor(state.draftRoom)?.seconds,
            levelNote = levelNoteFor(reading),
            levelTrace = trace.snapshot(),
            traceSecondsPerSample = trace.secondsPerSample,
            photoNames = committed.photoNames,
            photoHashes = committed.photoHashes,
            clipName = committed.clipName,
            clipHash = committed.clipHash,
            clipSeconds = if (committed.clipName != null) state.draftClipSeconds else 0,
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
                draftPhotos = emptyList(),
                draftClip = null,
                draftClipSeconds = 0,
                showCalibrationPrompt = shouldPromptCalibration(incidents),
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
                "calibrated against a ${profile?.referenceLabel ?: "reference"}" + (day?.let { " on $it" } ?: "") +
                " (${CalibrationMath.signed(reading.userOffsetDb)}), and are included as incident context."
        }
        LevelCalibration.PLATFORM_SPEC ->
            "The sound levels above come from my phone's built-in microphone at the level the phone itself declares " +
                "under Android's compatibility specification; they are included as incident context."
        LevelCalibration.ESTIMATE -> noiseMeter.inputStatus().selfTest?.takeIf { it.passed }?.let { test ->
            val day = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
                .format(Instant.ofEpochMilli(test.atEpochMillis).atZone(ZoneId.systemDefault()))
            "The sound levels above are estimates from my phone's ${reading.micLabel.lowercase(Locale.US)}, which passed " +
                "NoiseFile's microphone check on $day (it hears changes in loudness correctly); " +
                "they are included as incident context."
        }
    }

    fun updateIncidentDetails(
        incidentId: Long,
        details: IncidentDetails,
    ) {
        val incidents = incidentStore.updateDetails(incidentId, details)
        _uiState.update {
            it.copy(
                incidents = incidents,
                message = "Incident details updated.",
                error = null,
            )
        }
    }

    fun ruleForIncident(ruleId: String): RuleWorkflow? = ruleCatalog.byId(ruleId)

    fun verifyEvidence(): EvidenceSeal.Report = incidentStore.verify(files)

    fun incidentCountFor(ruleId: String): Int =
        _uiState.value.incidents.count { it.ruleId == ruleId }

    override fun onCleared() {
        noiseMeter.stop()
        super.onCleared()
    }

    companion object {
        /** Used when the city's code states no ambient recipe. */
        const val DEFAULT_AMBIENT_MINUTES = 5
        const val KEY_TOUR_SEEN = "seen"
    }
}
