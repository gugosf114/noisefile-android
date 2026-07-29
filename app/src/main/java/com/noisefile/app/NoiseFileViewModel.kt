package com.noisefile.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.noisefile.app.audio.NoiseMeter
import com.noisefile.app.data.IncidentStore
import com.noisefile.app.data.RuleCatalog
import com.noisefile.app.model.Incident
import com.noisefile.app.model.Jurisdiction
import com.noisefile.app.model.MeterReading
import com.noisefile.app.model.RuleWorkflow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.max

enum class AppScreen {
    HOME,
    METER,
    REVIEW,
    HISTORY,
}

data class NoiseFileUiState(
    val screen: AppScreen = AppScreen.HOME,
    val selectedJurisdictionId: String = RuleCatalog.SAN_JOSE_ID,
    val selectedRuleId: String = RuleCatalog.DEFAULT_RULE_ID,
    val meterReading: MeterReading = MeterReading(),
    val incidents: List<Incident> = emptyList(),
    val measurementStartedAt: Long? = null,
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
                error = null,
            )
        }
    }

    fun startMeasurement() {
        val startedAt = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                screen = AppScreen.METER,
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
        )
        val incidents = incidentStore.add(incident)
        _uiState.update {
            it.copy(
                screen = AppScreen.HOME,
                incidents = incidents,
                meterReading = MeterReading(),
                measurementStartedAt = null,
                draftNotes = "",
                message = "Incident saved to your private history.",
                error = null,
            )
        }
        return incident
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
}
