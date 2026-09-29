package com.noisefile.app.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File
import com.noisefile.app.data.EvidenceSeal
import com.noisefile.app.data.IncidentPatterns
import com.noisefile.app.data.buildReportPlan
import com.noisefile.app.data.writeIncidentPdf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noisefile.app.AppScreen
import com.noisefile.app.CalibrationMode
import com.noisefile.app.CaptureStage
import com.noisefile.app.NoiseFileUiState
import com.noisefile.app.NoiseFileViewModel
import com.noisefile.app.data.MeterAssessmentStatus
import com.noisefile.app.data.RuleConditionOutcome
import com.noisefile.app.data.assessMeterReading
import com.noisefile.app.data.buildComplaintDraft
import com.noisefile.app.data.buildIncidentHistoryReport
import com.noisefile.app.data.complaintDestination
import com.noisefile.app.model.Incident
import com.noisefile.app.model.Jurisdiction
import com.noisefile.app.audio.MicStatus
import com.noisefile.app.audio.SelfTestMath
import com.noisefile.app.audio.SelfTestOutcome
import com.noisefile.app.audio.SelfTestResult
import com.noisefile.app.audio.CalibrationMath
import com.noisefile.app.model.AmbientReading
import com.noisefile.app.model.LevelCalibration
import com.noisefile.app.model.MeterReading
import com.noisefile.app.model.NoiseType
import com.noisefile.app.model.RuleWorkflow
import com.noisefile.app.ui.theme.Brass
import com.noisefile.app.ui.theme.Chalk
import com.noisefile.app.ui.theme.DeckHigh
import com.noisefile.app.ui.theme.Cobalt
import com.noisefile.app.ui.theme.CodeQuoteStyle
import com.noisefile.app.ui.theme.PaperAmber
import com.noisefile.app.ui.theme.PaperBlue
import com.noisefile.app.ui.theme.PaperMuted
import com.noisefile.app.ui.theme.PaperRed
import com.noisefile.app.ui.theme.Sky
import com.noisefile.app.ui.theme.Danger
import com.noisefile.app.ui.theme.Ink
import com.noisefile.app.ui.theme.Line
import com.noisefile.app.ui.theme.Muted
import com.noisefile.app.ui.theme.Paper
import com.noisefile.app.ui.theme.Signal
import com.noisefile.app.ui.theme.Success
import com.noisefile.app.ui.theme.White
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val impactOptions = listOf(
    "Interrupted rest or quiet use",
    "Woke me or someone in my home",
    "Prevented work or concentration",
    "Shook walls, windows, or furniture",
)

@Composable
fun NoiseFileRoot(viewModel: NoiseFileViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showCityPicker by remember { mutableStateOf(false) }
    var pendingStage by remember { mutableStateOf(CaptureStage.NOISE) }
    var pendingCalibrationMode by remember { mutableStateOf(CalibrationMode.SMOKE_ALARM) }
    val startStage = { stage: CaptureStage ->
        when (stage) {
            CaptureStage.AMBIENT -> viewModel.startAmbientMeasurement()
            CaptureStage.CALIBRATE -> viewModel.startCalibration(pendingCalibrationMode)
            CaptureStage.NOISE -> viewModel.startMeasurement()
            CaptureStage.SELF_TEST -> viewModel.startSelfTest()
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) startStage(pendingStage) else viewModel.microphonePermissionDenied()
        pendingStage = CaptureStage.NOISE
    }

    val beginStage = { stage: CaptureStage ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startStage(stage)
        } else {
            pendingStage = stage
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    val beginCapture = { beginStage(CaptureStage.NOISE) }
    val beginAmbient = { beginStage(CaptureStage.AMBIENT) }
    val beginCalibration = { mode: CalibrationMode -> pendingCalibrationMode = mode; beginStage(CaptureStage.CALIBRATE) }
    val beginSelfTest = { beginStage(CaptureStage.SELF_TEST) }

    val nav = NavActions(
        home = viewModel::showHome,
        incidents = viewModel::showHistory,
        record = beginCapture,
        rules = viewModel::showRules,
        more = viewModel::showMore,
    )

    when (state.screen) {
        AppScreen.HOME -> HomeScreen(
            state = state,
            workflows = viewModel.workflows,
            selectedRule = viewModel.selectedRule(),
            selectedJurisdiction = viewModel.selectedJurisdiction(),
            onSelectRule = viewModel::selectRule,
            onShowCityPicker = { showCityPicker = true },
            onBeginCapture = beginCapture,
            onBeginAmbient = beginAmbient,
            ambientTargetSeconds = viewModel.ambientTargetSecondsFor(viewModel.selectedRule()),
            onSkipCalibrationPrompt = viewModel::skipCalibrationPrompt,
            onBeginSelfTest = beginSelfTest,
            nav = nav,
        )

        AppScreen.RULES -> RulesScreen(
            workflows = viewModel.workflows,
            selectedRule = viewModel.selectedRule(),
            selectedJurisdiction = viewModel.selectedJurisdiction(),
            incidentCount = viewModel.incidentCountFor(state.selectedRuleId),
            onSelectRule = viewModel::selectRule,
            onShowCityPicker = { showCityPicker = true },
            onOpenUri = { openUri(context, it) },
            nav = nav,
        )

        AppScreen.MORE -> MoreScreen(
            selectedJurisdiction = viewModel.selectedJurisdiction(),
            micStatus = viewModel.micStatus(),
            onShowCityPicker = { showCityPicker = true },
            onBeginSelfTest = beginSelfTest,
            onBeginCalibration = beginCalibration,
            onClearCalibration = viewModel::clearCalibration,
            onShareNeighbor = { shareNeighborInvite(context, viewModel.selectedRule()) },
            onOpenUri = { openUri(context, it) },
            nav = nav,
        )

        AppScreen.METER -> MeterScreen(
            rule = viewModel.selectedRule(),
            reading = state.meterReading,
            incidentCount = viewModel.incidentCountFor(state.selectedRuleId),
            stage = state.captureStage,
            ambient = state.ambient,
            ambientTargetSeconds = state.ambientTargetSeconds,
            calibrationReferenceText = state.calibrationReferenceText,
            calibrationMode = state.calibrationMode,
            error = state.error,
            onCalibrationReferenceChange = viewModel::setCalibrationReference,
            onStop = viewModel::stopMeasurement,
            onCancel = viewModel::showHome,
        )

        AppScreen.REVIEW -> {
            val rule = viewModel.selectedRule()
            ReviewScreen(
                state = state,
                rule = rule,
                incidentCount = viewModel.incidentCountFor(state.selectedRuleId),
                onLocationChange = viewModel::setLocation,
                onImpactChange = viewModel::setImpact,
                onNotesChange = viewModel::setNotes,
                onSave = { viewModel.saveIncident() },
                onAddPhoto = viewModel::addDraftPhoto,
                onRemovePhoto = viewModel::removeDraftPhoto,
                onDropClip = viewModel::dropDraftClip,
                onSaveAndPrepare = {
                    viewModel.saveIncident()?.let { incident ->
                        copyComplaintAndOpenDestination(context, incident, rule)
                    }
                },
                onDiscard = viewModel::showHome,
            )
        }

        AppScreen.HISTORY -> HistoryScreen(
            cityName = viewModel.selectedJurisdiction().displayName,
            incidents = state.incidents,
            ruleForIncident = viewModel::ruleForIncident,
            sealReport = remember(state.incidents) { viewModel.verifyEvidence() },
            fileFor = viewModel::incidentFile,
            nav = nav,
            onExport = { shareHistory(context, state.incidents) },
            onExportPdf = { shareHistoryPdf(context, state.incidents, viewModel::ruleForIncident, viewModel::incidentFile) },
            onUpdateDetails = viewModel::updateIncidentDetails,
            onPrepareComplaint = { incident, rule ->
                copyComplaintAndOpenDestination(context, incident, rule)
            },
        )
    }

    if (state.selfTestRunning || state.selfTestResult != null) {
        SelfTestDialog(
            running = state.selfTestRunning,
            step = state.selfTestStep,
            result = state.selfTestResult,
            onClose = viewModel::dismissSelfTest,
            onAgain = beginSelfTest,
        )
    }

    if (showCityPicker) {
        CityPickerDialog(
            jurisdictions = viewModel.jurisdictions,
            selectedJurisdiction = viewModel.selectedJurisdiction(),
            onSelect = { jurisdiction ->
                viewModel.selectJurisdiction(jurisdiction.id)
                showCityPicker = false
            },
            onDismiss = { showCityPicker = false },
        )
    }
}

@Composable
private fun CityPickerDialog(
    jurisdictions: List<Jurisdiction>,
    selectedJurisdiction: Jurisdiction,
    onSelect: (Jurisdiction) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your city") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Pick the city where the noise is.",
                    color = Muted,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(4.dp))
                jurisdictions.forEach { jurisdiction ->
                    val isSelected = jurisdiction.id == selectedJurisdiction.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                enabled = jurisdiction.isAvailable,
                                onClick = { onSelect(jurisdiction) },
                            ),
                        shape = RoundedCornerShape(16.dp),
                        color = when {
                            isSelected -> Signal.copy(alpha = 0.28f)
                            jurisdiction.isAvailable -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
                        },
                        border = if (isSelected) {
                            androidx.compose.foundation.BorderStroke(1.dp, Signal)
                        } else {
                            null
                        },
                    ) {
                        Row(
                            modifier = Modifier.padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    text = jurisdiction.displayName,
                                    color = if (jurisdiction.isAvailable) MaterialTheme.colorScheme.onSurface else Muted,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = if (isSelected) "Selected" else jurisdiction.region,
                                    color = Muted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = Success,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
    )
}

@Composable
private fun MeterScreen(
    rule: RuleWorkflow,
    reading: MeterReading,
    incidentCount: Int,
    stage: CaptureStage,
    ambient: AmbientReading?,
    ambientTargetSeconds: Int,
    calibrationReferenceText: String,
    calibrationMode: CalibrationMode = CalibrationMode.SMOKE_ALARM,
    error: String?,
    onCalibrationReferenceChange: (String) -> Unit,
    onStop: () -> Unit,
    onCancel: () -> Unit,
) {
    val isAmbient = stage == CaptureStage.AMBIENT
    val isCalibrating = stage == CaptureStage.CALIBRATE
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val scrollState = rememberScrollState()

    NightBackground {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 14.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
            ) {
                Text(
                    text = when {
                        isCalibrating -> "Calibrating this microphone"
                        isAmbient -> "Measuring the quiet first"
                        else -> "Measuring now"
                    },
                    color = Signal,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = when {
                        isCalibrating -> reading.micLabel
                        isAmbient -> "Quiet baseline in ${rule.jurisdiction.substringBefore(",")}"
                        else -> "${rule.jurisdiction.substringBefore(",")}, ${rule.noiseType.displayName}"
                    },
                    color = White,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                shape = CircleShape,
                color = Danger,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val breath by rememberInfiniteTransition(label = "Recording").animateFloat(
                        initialValue = 1f,
                        targetValue = 0.25f,
                        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                        label = "RecordingLight",
                    )
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(White.copy(alpha = breath), CircleShape),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        if (isAmbient) {
                            "${formatElapsed(reading.elapsedMillis)} / ${formatElapsed(ambientTargetSeconds * 1_000L)}"
                        } else {
                            formatElapsed(reading.elapsedMillis)
                        },
                        color = White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        val cityLimit = if (isAmbient || isCalibrating) null else limitAt(rule, LocalTime.now().hour)
        InstrumentDial(valueDb = reading.currentDb, limitDb = cityLimit, size = 264.dp) {
            Text(
                text = reading.currentDb.roundToInt().toString(),
                color = if (cityLimit != null && reading.currentDb >= cityLimit) Danger else Chalk,
                style = MaterialTheme.typography.displayLarge,
            )
            Text("estimated dB", color = Muted, style = MaterialTheme.typography.labelMedium)
            if (cityLimit != null) {
                Text("city limit now ${cityLimit.roundToInt()}", color = Danger, style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MeterStat("Min", reading.minimumDb, Modifier.weight(1f))
            MeterStat("Average", reading.averageDb, Modifier.weight(1f))
            MeterStat("Max", reading.maximumDb, Modifier.weight(1f))
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = when (reading.calibration) {
                LevelCalibration.USER_CALIBRATED ->
                    "${reading.micLabel}, calibrated by you (${CalibrationMath.signed(reading.userOffsetDb)}). Still not a certified meter."
                LevelCalibration.PLATFORM_SPEC ->
                    "Level set by the Android compatibility spec for this phone's unprocessed microphone path (94 dB SPL = -36 dBFS). Still not a certified meter."
                LevelCalibration.ESTIMATE ->
                    "${reading.micLabel}, phone estimate. The microphone path carries its own gain, so the number can sit several dB off a sound level meter."
            },
            color = Muted,
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(Modifier.height(16.dp))

        if (isCalibrating) {
            PaperCard(edge = PaperBlue, edgeWidth = 2.dp) {
                run {
                    val alarm = calibrationMode == CalibrationMode.SMOKE_ALARM
                    Text(
                        text = if (alarm) "With your smoke alarm" else "With a sound level meter",
                        color = PaperBlue,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = if (alarm) {
                            "Stand 10 feet from the smoke alarm. Press and hold its test button until it sounds a few times. " +
                                "The alarm is 85 dB at 10 feet by law. The phone listens for the alarm's own tone; other sounds do not count."
                        } else {
                            "Hold the meter next to this phone's microphone in a steady sound, a fan or radio hiss. " +
                                "Wait until both numbers settle, then type what the meter reads."
                        },
                        color = Ink,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = when {
                            alarm && reading.alarmToneDb > 0.0 -> "Alarm heard: ${reading.alarmToneDb.roundToInt()} dB. Tap Save calibration."
                            alarm -> "Listening for the alarm's tone…"
                            else -> "Phone average so far: ${reading.averageDb.roundToInt()} dB"
                        },
                        color = Ink,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (!alarm) OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = calibrationReferenceText,
                        onValueChange = onCalibrationReferenceChange,
                        label = { Text("Meter reading, dB") },
                        placeholder = { Text("Example: 62") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                    )
                    if (error != null) {
                        Text(text = error, color = PaperRed, style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(onClick = onCancel) { Text("Cancel") }
                }
            }
        } else if (isAmbient) {
            PaperCard(edge = PaperBlue, edgeWidth = 2.dp) {
                run {
                    Text(
                        text = "Quiet baseline in ${rule.jurisdiction.substringBefore(",")}",
                        color = PaperBlue,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = "Running average so far: ${reading.averageDb.roundToInt()} dB",
                        color = Ink,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "The noise you will record next is compared to this number. The phone's own " +
                            "offset is the same on both, so it cancels out of the difference. " +
                            (rule.ambientRecipe?.note
                                ?: "${rule.jurisdiction.substringBefore(",")}'s code sets no ambient recipe; " +
                                    "NoiseFile records ${ambientTargetSeconds / 60} minutes."),
                        color = PaperMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else {
            RuleAssessmentCard(
                rule = rule,
                reading = reading,
                incidentCount = incidentCount,
                ambient = ambient,
            )
        }

        Spacer(Modifier.height(22.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = White.copy(alpha = 0.08f),
            border = androidx.compose.foundation.BorderStroke(1.dp, White.copy(alpha = 0.12f)),
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    text = "While you record",
                    color = Signal,
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(7.dp))
                
                Text(
                    text = if (isCalibrating && calibrationMode == CalibrationMode.SMOKE_ALARM) {
                        "• 10 feet from the alarm, phone held up, microphone uncovered.\n" +
                            "• Hold the test button through two or three beeps.\n" +
                            "• The saved offset applies to this microphone only."
                    } else if (isCalibrating) {
                        "• A steady sound works best: a fan, a shower, radio static.\n" +
                            "• Meter and phone microphone side by side, same height, same direction.\n" +
                            "• The saved offset applies to this microphone only."
                    } else if (isAmbient) {
                        "• Ask for the noise to stop, or wait for a pause.\n" +
                            "• Stand exactly where you will measure the noise.\n" +
                            "• Keep still and quiet for the whole ${ambientTargetSeconds / 60} minutes; the capture ends on its own."
                    } else {
                        "• Hold the phone steady with its microphone uncovered.\n" +
                            "• Stay quiet while measuring."
                    },
                    color = Chalk,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = when {
                    isCalibrating -> "Saved for ${reading.micLabel}"
                    isAmbient -> "The phone's offset cancels out of the difference"
                    else -> "Keep the microphone uncovered"
                },
                color = White.copy(alpha = 0.58f),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                onClick = onStop,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Chalk,
                    contentColor = Ink,
                ),
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(Modifier.width(9.dp))
                Text(
                    when {
                        isCalibrating -> "Save calibration"
                        isAmbient -> "Finish early, keep this baseline"
                        else -> "Stop and review"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
    }
}

@Composable
private fun MeterStat(label: String, value: Double, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        color = White.copy(alpha = 0.08f),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 13.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                color = White.copy(alpha = 0.52f),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = value.roundToInt().toString(),
                color = White,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun RuleAssessmentCard(
    rule: RuleWorkflow,
    reading: MeterReading,
    incidentCount: Int,
    localDateTime: LocalDateTime = LocalDateTime.now(),
    ambient: AmbientReading? = null,
) {
    val assessment = assessMeterReading(
        rule = rule,
        reading = reading,
        incidentCount = incidentCount,
        localDateTime = localDateTime,
        ambient = ambient,
    )
    val statusColor = when (assessment.status) {
        MeterAssessmentStatus.LISTENING -> PaperMuted
        MeterAssessmentStatus.REACHES_LISTED_CONDITION -> PaperRed
        MeterAssessmentStatus.DOES_NOT_REACH_LISTED_CONDITION -> PaperAmber
        MeterAssessmentStatus.NEEDS_INFORMATION -> PaperBlue
    }
    val statusLabel = when (assessment.status) {
        MeterAssessmentStatus.LISTENING -> "Checking the city rule"
        MeterAssessmentStatus.REACHES_LISTED_CONDITION -> "Listed condition reached"
        MeterAssessmentStatus.DOES_NOT_REACH_LISTED_CONDITION -> "Condition not yet reached"
        MeterAssessmentStatus.NEEDS_INFORMATION -> "The meter cannot decide"
    }

    PaperCard(edge = statusColor, edgeWidth = 2.dp) {
        run {
            Text(
                text = "$statusLabel in ${rule.jurisdiction.substringBefore(",")}",
                color = statusColor,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = assessment.headline,
                color = Ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = assessment.detail,
                color = PaperMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
            assessment.conditions.forEach { condition ->
                val conditionColor = when (condition.outcome) {
                    RuleConditionOutcome.REACHED -> PaperRed
                    RuleConditionOutcome.NOT_REACHED -> PaperAmber
                    RuleConditionOutcome.NEEDS_INFORMATION -> PaperBlue
                }
                val marker = when (condition.outcome) {
                    RuleConditionOutcome.REACHED -> "✓"
                    RuleConditionOutcome.NOT_REACHED -> "○"
                    RuleConditionOutcome.NEEDS_INFORMATION -> "•"
                }
                val preview = conditionPreview(condition.text)
                var expanded by remember(condition.text) { mutableStateOf(false) }
                val shown = if (expanded || preview == null) condition.text else preview
                val head = shown.substringBefore(": ", "")
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = conditionColor, fontWeight = FontWeight.Bold)) {
                            append("$marker ")
                            if (head.isNotEmpty() && head.length <= 18) append("$head. ")
                        }
                        append(if (head.isNotEmpty() && head.length <= 18) shown.substringAfter(": ") else shown)
                    },
                    color = Ink,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (preview != null) {
                    TextButton(
                        onClick = { expanded = !expanded },
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(if (expanded) "Show less" else "Read the full rule")
                    }
                }
                if (condition.sourceQuote != null && condition.sourceCitation != null) {
                    CodeQuote(quote = condition.sourceQuote, citation = condition.sourceCitation)
                }
            }
            HorizontalDivider(color = Line)
            if (assessment.status == MeterAssessmentStatus.NEEDS_INFORMATION) {
                Text(
                    text = "The phone reading remains useful evidence. The city makes the final determination.",
                    color = PaperMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Text(
                    text = rule.title,
                    color = Ink,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Phone estimate only. City enforcement uses the required equipment, position, duration, and other rule conditions.",
                    color = PaperMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ReviewScreen(
    state: NoiseFileUiState,
    rule: RuleWorkflow,
    incidentCount: Int,
    onLocationChange: (String) -> Unit,
    onImpactChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onSaveAndPrepare: () -> Unit,
    onDiscard: () -> Unit,
    onAddPhoto: (Uri) -> Unit = {},
    onRemovePhoto: (File) -> Unit = {},
    onDropClip: () -> Unit = {},
) {
    // The recording is not saved yet. Back (arrow or phone key) must ask before dropping it.
    var confirmDiscard by remember { mutableStateOf(false) }
    BackHandler { confirmDiscard = true }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Throw this recording away?") },
            text = { Text("This recording is not saved yet. Going back drops it for good.") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onDiscard() }) { Text("Throw it away") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text("Keep it") }
            },
        )
    }
    NightBackground {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = Color.Transparent,
        contentColor = Chalk,
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { confirmDiscard = true }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Discard and go back")
                    }
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(
                            "Review incident",
                            color = Sky,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text("What happened?", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }

            item {
                MeasurementSummary(state.meterReading, rule, state.ambient)
            }

            item {
                // The review judges the recording that just ended, so its clock
                // is the moment the recording started, not the moment of review.
                RuleAssessmentCard(
                    rule = rule,
                    reading = state.meterReading,
                    incidentCount = incidentCount,
                    localDateTime = state.measurementStartedAt
                        ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }
                        ?: LocalDateTime.now(),
                    ambient = state.ambient,
                )
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Cobalt.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Cobalt.copy(alpha = 0.25f)),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = "City-specific next step",
                            color = Sky,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = rule.nextAction,
                            color = Muted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Save the incident below. NoiseFile will copy a completed complaint and open the best available city route.",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            state.error?.let { error ->
                item {
                    StatusMessage(
                        text = error,
                        color = Danger,
                        icon = Icons.Default.Shield,
                    )
                }
            }

            item {
                Text("Where was the noise?", style = MaterialTheme.typography.headlineSmall, color = Chalk)
            }

            item {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.draftLocation,
                    onValueChange = onLocationChange,
                    label = { Text("Address or approximate location") },
                    placeholder = { Text("Example: 440 Price Avenue, next-door property") },
                    supportingText = {
                        Text("Required for the prepared complaint. Stored only on this phone.")
                    },
                    isError = state.error != null && state.draftLocation.isBlank(),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                )
            }

            item {
                Text("How did it affect you?", style = MaterialTheme.typography.headlineSmall, color = Chalk)
            }

            items(impactOptions) { impact ->
                ImpactOption(
                    text = impact,
                    selected = impact == state.draftImpact,
                    onClick = { onImpactChange(impact) },
                )
            }

            item {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.draftNotes,
                    onValueChange = onNotesChange,
                    label = { Text("Incident notes") },
                    placeholder = { Text("Describe the sound, source, and anything you observed.") },
                    minLines = 3,
                    shape = RoundedCornerShape(18.dp),
                )
            }

            item {
                AttachmentsBlock(
                    photos = state.draftPhotos,
                    clip = state.draftClip,
                    clipSeconds = state.draftClipSeconds,
                    onAddPhoto = onAddPhoto,
                    onRemovePhoto = onRemovePhoto,
                    onDropClip = onDropClip,
                )
            }

            item {
                val destination = complaintDestination(rule)
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp),
                    onClick = onSaveAndPrepare,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Cobalt,
                        contentColor = White,
                    ),
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = when {
                            destination.isOnlineForm -> "Save, copy & open city form"
                            destination.isDocumentPacket -> "Save, copy & open city packet"
                            else -> "Save, copy & open city contact"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    onClick = onSave,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(9.dp))
                    Text("Save only", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
    }
}

@Composable
private fun MeasurementSummary(reading: MeterReading, rule: RuleWorkflow, ambient: AmbientReading? = null) {
    DeckCard {
        run {
            Text(
                text = rule.noiseType.displayName,
                color = Signal,
                style = MaterialTheme.typography.labelLarge,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        text = reading.maximumDb.roundToInt().toString(),
                        color = Chalk,
                        style = MaterialTheme.typography.displayLarge,
                    )
                    Text("highest estimated dB", color = Muted)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatElapsed(reading.elapsedMillis), color = White)
                    Text(
                        "Average ${reading.averageDb.roundToInt()} dB",
                        color = Muted,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (ambient != null) {
                Text(
                    text = "Quiet baseline ${ambient.db.roundToInt()} dB over ${formatElapsed(ambient.seconds * 1_000L)}; " +
                        "this recording ${(reading.averageDb - ambient.db).roundToInt()} dB above it on average, " +
                        "${(reading.maximumDb - ambient.db).roundToInt()} dB above at peak",
                    color = Signal,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/**
 * Long rule text folds to its first sentences on the phone; the reader opens the rest.
 * Returns null when the text is short enough to show whole.
 */
internal fun conditionPreview(text: String, maxChars: Int = 420): String? {
    if (text.length <= maxChars) return null
    val sentenceEnds = Regex("""[.!?](\s|$)""").findAll(text).map { it.range.first + 1 }.toList()
    val cut = sentenceEnds.filter { it in 60..maxChars }.lastOrNull()
        ?: sentenceEnds.firstOrNull { it > 60 }
        ?: maxChars
    val head = text.substring(0, cut).trimEnd()
    return if (head.length >= text.length - 40) null else "$head …"
}

/** The ordinance's own sentence, shown under the line it justifies. Not our words. */
@Composable
internal fun CodeQuote(quote: String, citation: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind { drawLine(Line, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 3.dp.toPx()) }
            .padding(start = 14.dp, top = 2.dp, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("The code says, $citation", color = PaperMuted, style = MaterialTheme.typography.labelMedium)
        Text("\u201C$quote\u201D", color = Ink, style = CodeQuoteStyle)
    }
}

@Composable
internal fun CalibrationPromptCard(onCalibrate: () -> Unit, onSkip: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Signal.copy(alpha = 0.14f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Signal.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Calibrate your microphone", color = Signal, style = MaterialTheme.typography.titleMedium)
            Text(
                "Highly recommended. The phone plays six short tones from its own speaker and listens to itself. " +
                    "It sets the volume for the test and puts it back after. 10 seconds. Nothing to buy. Or skip.",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onCalibrate,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Cobalt, contentColor = White),
                ) { Text("Calibrate my microphone", fontWeight = FontWeight.SemiBold) }
                TextButton(onClick = onSkip) { Text("Skip") }
            }
        }
    }
}

@Composable
private fun SelfTestDialog(
    running: Boolean,
    step: Int,
    result: SelfTestResult?,
    onClose: () -> Unit,
    onAgain: () -> Unit,
) {
    var showDetails by remember(result) { mutableStateOf(false) }
    val passed = result?.outcome == SelfTestOutcome.READS_STRAIGHT
    val steps = result?.measuredDropsDb.orEmpty().map { "${it.roundToInt()}" }
    val stepWords = when (steps.size) {
        0 -> "none"
        1 -> "${steps[0]} dB"
        else -> steps.dropLast(1).joinToString(", ") + " and ${steps.last()} dB"
    }
    val today = DateTimeFormatter.ofPattern("MMM d", Locale.US).format(Instant.now().atZone(ZoneId.systemDefault()))
    val details = if (result == null) "" else buildString {
        append("Six tones, each 10 dB quieter than the last. ")
        append("Tones heard: ${result.stepsHeard} of ${SelfTestMath.TONES}. ")
        append("Steps heard: $stepWords. ")
        append("Checked range: ${result.rangeDb} dB.")
        when (result.squeezedLoudSteps) {
            0 -> Unit
            1 -> append(" The loudest tone came out squeezed and was left out.")
            else -> append(" The loudest tones came out squeezed and were left out.")
        }
    }
    AlertDialog(
        onDismissRequest = { if (!running) onClose() },
        title = {
            Text(
                when {
                    running -> "Checking your microphone"
                    passed -> "Microphone check passed"
                    result?.outcome == SelfTestOutcome.TONE_NOT_HEARD -> "The microphone did not hear the tones"
                    else -> "Microphone check did not pass"
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    when {
                        running -> "Tone $step of ${SelfTestMath.TONES}. The tones play from the phone's own speaker, even with headphones on. " +
                            "The volume is set for the test and put back after. Put the phone on a table and keep the room quiet."
                        passed -> "Your microphone hears changes in loudness correctly. Checked $today."
                        result?.outcome == SelfTestOutcome.TONE_NOT_HEARD ->
                            "Uncover the speaker and the microphone, take the phone out of its case if it has one, then try again."
                        result != null && result.stepsHeard >= 3 ->
                            "The microphone did not hear the steps correctly this time. " +
                                "Put the phone on a table in a quiet room, then try again."
                        else -> "Only ${result?.stepsHeard ?: 0} of the six tones were heard. Find a quieter room, then try again."
                    },
                )
                if (!running && result != null && result.outcome != SelfTestOutcome.TONE_NOT_HEARD) {
                    TextButton(onClick = { showDetails = !showDetails }, contentPadding = PaddingValues(0.dp)) {
                        Text(if (showDetails) "Hide details" else "Details")
                    }
                    if (showDetails) Text(details, color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            if (running) {
                TextButton(onClick = onClose) { Text("Cancel") }
            } else {
                TextButton(onClick = onClose) { Text("Done") }
            }
        },
        dismissButton = {
            if (!running && !passed) {
                TextButton(onClick = onAgain) { Text("Try again") }
            }
        },
    )
}

@Composable
internal fun MicrophoneCard(
    status: MicStatus,
    onBeginSelfTest: () -> Unit,
    onBeginCalibration: (CalibrationMode) -> Unit,
    onClearCalibration: () -> Unit,
) {
    var showOtherWays by remember { mutableStateOf(false) }
    val selfTestLine = status.selfTest?.takeIf { it.passed }?.let { test ->
        val day = DateTimeFormatter.ofPattern("MMM d", Locale.US)
            .format(Instant.ofEpochMilli(test.atEpochMillis).atZone(ZoneId.systemDefault()))
        " Microphone check passed $day."
    }.orEmpty()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Microphone",
                color = Muted,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = status.micLabel,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = when (status.calibration) {
                    LevelCalibration.USER_CALIBRATED ->
                        "Calibrated by you with a ${status.profile?.referenceLabel ?: "reference"} (${CalibrationMath.signed(status.profile?.offsetDb ?: 0.0)})."
                    LevelCalibration.PLATFORM_SPEC ->
                        "This phone declares its own microphone level, so the numbers are set by the phone itself."
                    LevelCalibration.ESTIMATE ->
                        "Estimate."
                } + selfTestLine,
                color = Muted,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onBeginSelfTest,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Calibrate my microphone", fontWeight = FontWeight.SemiBold)
                }
                if (status.profile != null) {
                    TextButton(onClick = onClearCalibration) { Text("Clear") }
                }
            }
            TextButton(onClick = { showOtherWays = !showOtherWays }, contentPadding = PaddingValues(0.dp)) {
                Text(if (showOtherWays) "Hide other ways" else "Other ways", color = Muted)
            }
            if (showOtherWays) {
                TextButton(onClick = { onBeginCalibration(CalibrationMode.SMOKE_ALARM) }, contentPadding = PaddingValues(0.dp)) {
                    Text("Use my smoke alarm")
                }
                TextButton(onClick = { onBeginCalibration(CalibrationMode.METER) }, contentPadding = PaddingValues(0.dp)) {
                    Text("I have a sound level meter")
                }
            }
        }
    }
}

@Composable
private fun ImpactOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Cobalt else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(18.dp),
            ),
        color = if (selected) Cobalt.copy(alpha = 0.09f) else MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(17.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .border(
                        2.dp,
                        if (selected) Cobalt else Muted,
                        CircleShape,
                    )
                    .padding(4.dp),
            ) {
                if (selected) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Cobalt, CircleShape),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun HistoryScreen(
    cityName: String,
    incidents: List<Incident>,
    ruleForIncident: (String) -> RuleWorkflow?,
    sealReport: EvidenceSeal.Report,
    fileFor: (Incident, String) -> File,
    nav: NavActions,
    onExport: () -> Unit,
    onExportPdf: () -> Unit,
    onUpdateDetails: (Long, String, String) -> Unit,
    onPrepareComplaint: (Incident, RuleWorkflow) -> Unit,
) {
    AppScaffold(selectedScreen = AppScreen.HISTORY, nav = nav) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                BrandHeader(cityName = cityName)
                Spacer(Modifier.height(26.dp))
                Text("Incidents", style = MaterialTheme.typography.headlineMedium, color = Chalk)
                Label("Saved on this phone only")
            }

            if (incidents.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            onClick = onExportPdf,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Cobalt, contentColor = White)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Export PDF report", style = MaterialTheme.typography.titleSmall)
                        }
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            onClick = onExport,
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text("Share as plain text", style = MaterialTheme.typography.titleSmall)
                        }
                        EvidenceSealLine(sealReport)
                        IncidentPatterns.sentence(incidents, ZoneId.systemDefault())?.let { sentence ->
                            PatternCard(sentence = sentence, grid = IncidentPatterns.grid(incidents, ZoneId.systemDefault()))
                        }
                    }
                }
            }

            if (incidents.isEmpty()) {
                item {
                    EmptyHistory()
                }
            } else {
                items(incidents, key = { it.id }) { incident ->
                    IncidentCard(
                        incident = incident,
                        rule = ruleForIncident(incident.ruleId),
                        fileFor = fileFor,
                        onUpdateDetails = onUpdateDetails,
                        onPrepareComplaint = onPrepareComplaint,
                    )
                }
            }
        }
    }
}

@Composable
private fun EvidenceSealLine(report: EvidenceSeal.Report) {
    val (text, color) = when {
        !report.intact -> "Evidence seal broken at incident ${report.brokenAtId}: a measured number, an incident, or the order changed after saving." to Danger
        report.sealedCount == 0 -> "Evidence seal: incidents saved before sealing existed are unsealed." to Muted
        report.intact -> "Evidence seal intact on ${report.sealedCount} incident${if (report.sealedCount == 1) "" else "s"}. Numbers, times and order unchanged since saving." to Success
        else -> "Evidence seal: nothing to check yet." to Muted
    }
    Text(text, color = color, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun PatternCard(sentence: String, grid: Array<IntArray>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("When it happens", color = Sky, style = MaterialTheme.typography.labelLarge)
            Text(sentence, style = MaterialTheme.typography.bodyLarge)
            val maxCount = grid.maxOf { it.max() }.coerceAtLeast(1)
            val days = listOf("M", "T", "W", "T", "F", "S", "S")
            val cellColor = MaterialTheme.colorScheme.surfaceVariant
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(126.dp)) {
                val labelW = 18.dp.toPx()
                val cellW = (size.width - labelW) / 24f
                val cellH = size.height / 7f
                for (d in 0 until 7) for (h in 0 until 24) {
                    val n = grid[d][h]
                    val c = if (n == 0) cellColor else Cobalt.copy(alpha = 0.25f + 0.75f * n / maxCount)
                    drawRect(
                        color = c,
                        topLeft = androidx.compose.ui.geometry.Offset(labelW + h * cellW + 1f, d * cellH + 1f),
                        size = androidx.compose.ui.geometry.Size(cellW - 2f, cellH - 2f),
                    )
                }
            }
            Text("Rows are Monday to Sunday. Columns are midnight to 11 PM. Darker means more incidents.", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun shareHistoryPdf(context: Context, incidents: List<Incident>, ruleFor: (String) -> RuleWorkflow?, fileFor: (Incident, String) -> File) {
    val result = runCatching {
        val plan = buildReportPlan(incidents, ruleFor)
        writeIncidentPdf(context, plan, fileFor)
    }
    val file = result.getOrElse {
        Toast.makeText(context, "Could not build the PDF: ${it.message}", Toast.LENGTH_LONG).show()
        return
    }
    val uri = FileProvider.getUriForFile(context, context.packageName + ".reports", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Share PDF report"))
}

@Composable
private fun EmptyHistory() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = Cobalt.copy(alpha = 0.12f),
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = Sky,
                    modifier = Modifier.padding(17.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Nothing logged yet", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(7.dp))
            Text(
                text = "Your documented incidents will appear here. Nothing is uploaded automatically.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun IncidentCard(
    incident: Incident,
    rule: RuleWorkflow?,
    onUpdateDetails: (Long, String, String) -> Unit,
    onPrepareComplaint: (Incident, RuleWorkflow) -> Unit,
    fileFor: (Incident, String) -> File = { _, name -> File(name) },
) {
    var isEditingDetails by remember(incident.id) { mutableStateOf(false) }
    var locationDraft by remember(incident.id, incident.location) { mutableStateOf(incident.location) }
    var noteDraft by remember(incident.id, incident.notes) { mutableStateOf(incident.notes) }
    val date = DateTimeFormatter
        .ofPattern("EEE, MMM d, h:mm a", Locale.US)
        .format(
            Instant.ofEpochMilli(incident.startedAtEpochMillis)
                .atZone(ZoneId.systemDefault()),
        )
    val limit = rule?.let {
        limitAt(it, Instant.ofEpochMilli(incident.startedAtEpochMillis).atZone(ZoneId.systemDefault()).hour)
    }
    val overLimit = limit != null && incident.maximumDb >= limit
    PaperCard(edge = if (overLimit) PaperRed else Line, edgeWidth = if (overLimit) 2.dp else 1.dp) {
        run {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = incident.noiseType.displayName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(date, color = PaperMuted, style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${incident.maximumDb.roundToInt()} dB max",
                        color = if (overLimit) PaperRed else Ink,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    if (limit != null) {
                        Text(
                            text = if (overLimit) "at or above the ${limit.roundToInt()} dB limit" else "below the ${limit.roundToInt()} dB limit",
                            color = if (overLimit) PaperRed else PaperMuted,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            if (incident.levelTrace.size >= 2) {
                TraceStrip(trace = incident.levelTrace, limitDb = limit)
            }
            HorizontalDivider(color = Line)
            Text(incident.impact, style = MaterialTheme.typography.bodyLarge)
            if (isEditingDetails) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = locationDraft,
                    onValueChange = { locationDraft = it },
                    label = { Text("Address or approximate location") },
                    placeholder = { Text("Where did the disturbance come from?") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = noteDraft,
                    onValueChange = { noteDraft = it },
                    label = { Text("Incident notes") },
                    placeholder = { Text("Describe the sound, source, and anything you observed.") },
                    minLines = 3,
                    shape = RoundedCornerShape(16.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = {
                            locationDraft = incident.location
                            noteDraft = incident.notes
                            isEditingDetails = false
                        },
                    ) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            onUpdateDetails(incident.id, locationDraft, noteDraft)
                            isEditingDetails = false
                        },
                        enabled = locationDraft.isNotBlank(),
                    ) {
                        Text("Save details")
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = "Location",
                        color = PaperMuted,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = incident.location.ifBlank { "No location added." },
                        color = if (incident.location.isBlank()) PaperMuted else Ink,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "Notes",
                        color = PaperMuted,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = incident.notes.ifBlank { "No notes added." },
                        color = if (incident.notes.isBlank()) PaperMuted else Ink,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(
                        modifier = Modifier.align(Alignment.End),
                        onClick = { isEditingDetails = true },
                    ) {
                        Text("Edit details")
                    }
                }
            }
            if (rule != null) {
                val destination = complaintDestination(rule)
                Button(
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    onClick = { onPrepareComplaint(incident, rule) },
                    enabled = incident.location.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Cobalt,
                        contentColor = White,
                    ),
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when {
                            destination.isOnlineForm -> "Copy complaint & open city form"
                            destination.isDocumentPacket -> "Copy complaint & open city packet"
                            else -> "Copy complaint & open city contact"
                        },
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                if (incident.location.isBlank()) {
                    Text(
                        text = "Add the incident location before preparing the complaint.",
                        color = PaperRed,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                Text(
                    text = "This incident's city rule is no longer available.",
                    color = PaperRed,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Length ${formatElapsed(incident.durationSeconds * 1_000)}", color = PaperMuted, style = MaterialTheme.typography.bodySmall)
                Text("Average ${incident.averageDb.roundToInt()} dB", color = PaperMuted, style = MaterialTheme.typography.bodySmall)
            }
            if (incident.photoNames.isNotEmpty() || incident.clipName != null) {
                SavedAttachments(incident = incident, fileFor = fileFor)
            }
            Text(
                text = "Seal ${EvidenceSeal.short(incident.evidenceHash)}" +
                    if (incident.levelTrace.size >= 2) ", ${incident.levelTrace.size} trace points" else "",
                color = PaperMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** The sound of one incident, second by second, with the city's limit as a red line. */
@Composable
private fun TraceStrip(trace: List<Int>, limitDb: Double?) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(54.dp)) {
        val low = 20f; val high = 100f
        fun y(db: Float) = size.height - ((db - low) / (high - low)).coerceIn(0f, 1f) * size.height
        drawLine(Line, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
        val step = size.width / (trace.size - 1).coerceAtLeast(1)
        val path = androidx.compose.ui.graphics.Path()
        trace.forEachIndexed { i, v -> if (i == 0) path.moveTo(0f, y(v.toFloat())) else path.lineTo(i * step, y(v.toFloat())) }
        drawPath(path, PaperBlue, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), join = androidx.compose.ui.graphics.StrokeJoin.Round))
        if (limitDb != null) {
            drawLine(
                PaperRed,
                Offset(0f, y(limitDb.toFloat())),
                Offset(size.width, y(limitDb.toFloat())),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            )
        }
    }
}


@Composable
private fun PhotoThumb(file: File, size: androidx.compose.ui.unit.Dp, onRemove: (() -> Unit)? = null) {
    val bitmap = remember(file.path, file.lastModified()) {
        runCatching { BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = 4 }) }.getOrNull()
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(size)) {
            bitmap?.let { Image(bitmap = it.asImageBitmap(), contentDescription = "Photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
        onRemove?.let { TextButton(onClick = it, contentPadding = PaddingValues(0.dp)) { Text("Remove", style = MaterialTheme.typography.labelMedium) } }
    }
}

@Composable
private fun AttachmentsBlock(
    photos: List<File>,
    clip: File?,
    clipSeconds: Int,
    onAddPhoto: (Uri) -> Unit,
    onRemovePhoto: (File) -> Unit,
    onDropClip: () -> Unit,
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(onAddPhoto) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Photos and sound", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (clip != null) "The loudest $clipSeconds seconds were kept as a sound clip. It stays on this phone."
                else "No sound clip was kept for this incident.",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (clip != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ClipPlayButton(clip)
                    OutlinedButton(onClick = onDropClip, shape = RoundedCornerShape(14.dp)) { Text("Drop the clip") }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                photos.forEach { PhotoThumb(it, 88.dp) { onRemovePhoto(it) } }
                if (photos.size < 2) {
                    OutlinedButton(
                        onClick = { picker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        shape = RoundedCornerShape(14.dp),
                    ) { Text(if (photos.isEmpty()) "Add a photo" else "Add another") }
                }
            }
            Text("Up to two photos. They stay on this phone, go on the PDF, and sit under the seal.", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ClipPlayButton(file: File) {
    var playing by remember(file.path) { mutableStateOf(false) }
    val player = remember(file.path) { MediaPlayer() }
    androidx.compose.runtime.DisposableEffect(file.path) { onDispose { runCatching { player.release() } } }
    Button(
        onClick = {
            if (playing) { runCatching { player.stop() }; playing = false }
            else runCatching {
                player.reset(); player.setDataSource(file.path); player.prepare()
                player.setOnCompletionListener { playing = false }
                player.start(); playing = true
            }
        },
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Cobalt, contentColor = White),
    ) { Text(if (playing) "Stop" else "Play the clip") }
}

@Composable
private fun SavedAttachments(incident: Incident, fileFor: (Incident, String) -> File) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        incident.photoNames.forEach { name -> PhotoThumb(fileFor(incident, name), 64.dp) }
        incident.clipName?.let { name ->
            val f = fileFor(incident, name)
            if (f.isFile) Column { ClipPlayButton(f); Text("${incident.clipSeconds} s, loudest moment", color = PaperMuted, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
internal fun StatusMessage(
    text: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.30f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(Modifier.width(11.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

internal fun formatElapsed(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0L) / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}

private fun copyComplaintAndOpenDestination(
    context: Context,
    incident: Incident,
    rule: RuleWorkflow,
) {
    val complaint = buildComplaintDraft(incident, rule)
    val destination = complaintDestination(rule)
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText("NoiseFile complaint", complaint))
    Toast.makeText(
        context,
        when {
            destination.isOnlineForm -> "Complaint copied. Paste it into the city form."
            destination.isDocumentPacket ->
                "Incident summary copied. Complete and sign the city packet."
            else -> "Complaint copied. The city contact is opening."
        },
        Toast.LENGTH_LONG,
    ).show()
    openUri(context, destination.uri)
}

private fun openUri(context: Context, uri: String) {
    val parsed = Uri.parse(uri)
    val action = if (parsed.scheme == "tel") Intent.ACTION_DIAL else Intent.ACTION_VIEW
    runCatching {
        context.startActivity(Intent(action, parsed))
    }
}

private fun shareNeighborInvite(context: Context, rule: RuleWorkflow) {
    val message = """
        I am documenting a ${rule.noiseType.displayName.lowercase(Locale.US)} disturbance in ${rule.jurisdiction}.

        If you are hearing the same event, please reply with:
        • the approximate time you heard it
        • where you heard it from
        • how it affected you

        Please describe only what you personally observed. NoiseFile keeps each person's account separate.
    """.trimIndent()

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, message)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share with Neighbor"))
}

private fun shareHistory(context: Context, incidents: List<Incident>) {
    val report = buildIncidentHistoryReport(incidents)
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, report)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Incident Log"))
}
