package com.noisefile.app.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noisefile.app.AppScreen
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
import com.noisefile.app.audio.CalibrationMath
import com.noisefile.app.model.AmbientReading
import com.noisefile.app.model.LevelCalibration
import com.noisefile.app.model.MeterReading
import com.noisefile.app.model.NoiseType
import com.noisefile.app.model.RuleWorkflow
import com.noisefile.app.ui.theme.Cobalt
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
    val startStage = { stage: CaptureStage ->
        when (stage) {
            CaptureStage.AMBIENT -> viewModel.startAmbientMeasurement()
            CaptureStage.CALIBRATE -> viewModel.startCalibration()
            CaptureStage.NOISE -> viewModel.startMeasurement()
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
    val beginCalibration = { beginStage(CaptureStage.CALIBRATE) }

    when (state.screen) {
        AppScreen.HOME -> HomeScreen(
            state = state,
            workflows = viewModel.workflows,
            selectedRule = viewModel.selectedRule(),
            selectedJurisdiction = viewModel.selectedJurisdiction(),
            incidentCount = viewModel.incidentCountFor(state.selectedRuleId),
            onSelectRule = viewModel::selectRule,
            onShowCityPicker = { showCityPicker = true },
            onBeginCapture = beginCapture,
            onBeginAmbient = beginAmbient,
            ambientTargetSeconds = viewModel.ambientTargetSecondsFor(viewModel.selectedRule()),
            micStatus = viewModel.micStatus(),
            onBeginCalibration = beginCalibration,
            onClearCalibration = viewModel::clearCalibration,
            onShareNeighbor = {
                shareNeighborInvite(context, viewModel.selectedRule())
            },
            onShowHome = viewModel::showHome,
            onShowHistory = viewModel::showHistory,
            onOpenUri = { openUri(context, it) },
        )

        AppScreen.METER -> MeterScreen(
            rule = viewModel.selectedRule(),
            reading = state.meterReading,
            incidentCount = viewModel.incidentCountFor(state.selectedRuleId),
            stage = state.captureStage,
            ambient = state.ambient,
            ambientTargetSeconds = state.ambientTargetSeconds,
            calibrationReferenceText = state.calibrationReferenceText,
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
                onSaveAndPrepare = {
                    viewModel.saveIncident()?.let { incident ->
                        copyComplaintAndOpenDestination(context, incident, rule)
                    }
                },
                onDiscard = viewModel::showHome,
            )
        }

        AppScreen.HISTORY -> HistoryScreen(
            incidents = state.incidents,
            ruleForIncident = viewModel::ruleForIncident,
            onShowHome = viewModel::showHome,
            onShowHistory = viewModel::showHistory,
            onExport = { shareHistory(context, state.incidents) },
            onUpdateDetails = viewModel::updateIncidentDetails,
            onPrepareComplaint = { incident, rule ->
                copyComplaintAndOpenDestination(context, incident, rule)
            },
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
private fun HomeScreen(
    state: NoiseFileUiState,
    workflows: List<RuleWorkflow>,
    selectedRule: RuleWorkflow,
    selectedJurisdiction: Jurisdiction,
    incidentCount: Int,
    onSelectRule: (String) -> Unit,
    onShowCityPicker: () -> Unit,
    onBeginCapture: () -> Unit,
    onBeginAmbient: () -> Unit,
    ambientTargetSeconds: Int,
    micStatus: MicStatus,
    onBeginCalibration: () -> Unit,
    onClearCalibration: () -> Unit,
    onShareNeighbor: () -> Unit,
    onShowHome: () -> Unit,
    onShowHistory: () -> Unit,
    onOpenUri: (String) -> Unit,
) {
    AppScaffold(
        selectedScreen = AppScreen.HOME,
        onShowHome = onShowHome,
        onShowHistory = onShowHistory,
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                BrandHeader(
                    cityName = selectedJurisdiction.displayName,
                    onCityClick = onShowCityPicker,
                )
            }

            if (state.message != null) {
                item {
                    StatusMessage(
                        text = state.message,
                        color = Success,
                        icon = Icons.Default.CheckCircle,
                    )
                }
            }

            if (state.error != null) {
                item {
                    StatusMessage(
                        text = state.error,
                        color = Danger,
                        icon = Icons.Default.Shield,
                    )
                }
            }

            item {
                SectionTitle(
                    eyebrow = "${selectedJurisdiction.displayName.uppercase(Locale.US)} · VERIFIED WORKFLOWS",
                    title = "What are you hearing?",
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    workflows.forEach { workflow ->
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = workflow.id == selectedRule.id,
                            onClick = { onSelectRule(workflow.id) },
                            label = {
                                Text(
                                    text = when (workflow.noiseType) {
                                        NoiseType.BARKING_DOG -> "Animal"
                                        NoiseType.PARTY_MUSIC -> "Noise"
                                        NoiseType.CONSTRUCTION -> "Construction"
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Ink,
                                selectedLabelColor = White,
                            ),
                        )
                    }
                }
            }

            item {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    onClick = onBeginCapture,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Cobalt,
                        contentColor = White,
                    ),
                ) {
                    Icon(Icons.Default.RadioButtonChecked, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text("Start Recording", style = MaterialTheme.typography.titleMedium)
                }
            }

            item {
                AmbientBaselineCard(
                    rule = selectedRule,
                    ambient = state.ambient,
                    targetSeconds = ambientTargetSeconds,
                    onBeginAmbient = onBeginAmbient,
                )
            }

            item {
                MicrophoneCard(
                    status = micStatus,
                    onBeginCalibration = onBeginCalibration,
                    onClearCalibration = onClearCalibration,
                )
            }

            item {
                RuleCard(
                    rule = selectedRule,
                    incidentCount = incidentCount,
                    onOpenUri = onOpenUri,
                )
            }

            item {
                MicrophoneNotice()
            }

            item {
                HeroCard()
            }

            item {
                NeighborVerifyCard(onShare = onShareNeighbor)
            }

            item {
                ThreeStepStrip()
            }
        }
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
        title = { Text("Choose your city") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Only verified city packets can be selected. More Bay Area cities are added with the ordinance library.",
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
                                    color = if (jurisdiction.isAvailable) Ink else Muted,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = if (isSelected) "Selected · verified" else jurisdiction.region,
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
private fun BrandHeader(
    cityName: String = "San Jose",
    onCityClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(14.dp),
                color = Ink,
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = Signal,
                    modifier = Modifier.padding(10.dp),
                )
            }
            Spacer(Modifier.width(11.dp))
            Column {
                Text(
                    text = "NoiseFile",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "KNOW · LOG · FILE",
                    color = Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
            }
        }
        Surface(
            modifier = if (onCityClick != null) {
                Modifier.clickable(onClick = onCityClick)
            } else {
                Modifier
            },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Cobalt,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(cityName, style = MaterialTheme.typography.labelLarge)
                if (onCityClick != null) {
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Change city",
                        tint = Muted,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Ink),
    ) {
        Box(Modifier.fillMaxSize()) {
            WaveDecoration(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth(0.58f)
                    .fillMaxHeight(),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Surface(
                    shape = CircleShape,
                    color = Signal,
                ) {
                    Text(
                        text = "LOCAL RULES · PRIVATE RECORD",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        color = Ink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                    )
                }
                Text(
                    text = "Know what counts.\nBe ready when it happens.",
                    color = White,
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    text = "Your city’s process, your incident history, your next step.",
                    color = White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun WaveDecoration(
    modifier: Modifier = Modifier,
    color: Color = Signal,
    alpha: Float = 0.26f,
    variant: Int = 0,
    horizontalBars: Boolean = false,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = alpha * 0.4f,
        targetValue = alpha,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "WaveAlpha"
    )
    Canvas(modifier = modifier) {
        val factors = when (variant) {
            1 -> floatArrayOf(0.88f, 0.74f, 0.58f, 0.43f, 0.30f, 0.20f, 0.12f, 0.06f, 0.02f)
            2 -> floatArrayOf(0.08f, 0.20f, 0.42f, 0.70f, 0.94f, 0.82f, 0.55f, 0.28f, 0.10f)
            3 -> floatArrayOf(0.03f, 0.08f, 0.17f, 0.32f, 0.51f, 0.73f, 0.91f, 0.70f, 0.42f)
            else -> floatArrayOf(0.18f, 0.35f, 0.55f, 0.78f, 0.95f, 0.78f, 0.55f, 0.35f, 0.18f)
        }

        factors.forEachIndexed { index, factor ->
            if (horizontalBars) {
                val y = index * (size.height / (factors.size - 1))
                drawLine(
                    color = color.copy(alpha = animatedAlpha),
                    start = Offset(size.width * (1f - factor), y),
                    end = Offset(size.width, y),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            } else {
                val x = index * (size.width / (factors.size - 1))
                drawLine(
                    color = color.copy(alpha = animatedAlpha),
                    start = Offset(x, size.height * (1f - factor) / 2f),
                    end = Offset(x, size.height * (1f + factor) / 2f),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(eyebrow: String, title: String) {
    Column {
        Text(
            text = eyebrow,
            color = Cobalt,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp,
        )
        Spacer(Modifier.height(5.dp))
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun RuleCard(
    rule: RuleWorkflow,
    incidentCount: Int,
    onOpenUri: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box {
            WaveDecoration(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(158.dp)
                    .height(132.dp),
                color = Cobalt,
                alpha = 0.08f,
                variant = 1,
                horizontalBars = true,
            )
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp),
            ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Signal.copy(alpha = 0.22f),
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.padding(10.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "WHAT HAPPENS HERE",
                        color = Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                    Text(text = rule.title, style = MaterialTheme.typography.titleLarge)
                }
            }

            Text(text = rule.summary, style = MaterialTheme.typography.bodyLarge)

            rule.requiredIncidentCount?.let { required ->
                val progress = (incidentCount.toFloat() / required).coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Your documented history", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "${incidentCount.coerceAtMost(required)} of $required",
                            color = if (progress >= 1f) Success else Cobalt,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(9.dp)
                            .clip(CircleShape),
                        color = if (progress >= 1f) Success else Cobalt,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = rule.captureInstruction,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenUri(rule.actionUri) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Ink,
                        contentColor = White,
                    ),
                ) {
                    Text(rule.actionLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(
                    onClick = { onOpenUri(rule.officialSourceUrl) },
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = "Open official source")
                }
            }
            Text(
                text = "Official source verified ${rule.verifiedDate}",
                color = Muted,
                fontSize = 12.sp,
            )
            }
        }
    }
}

@Composable
private fun MicrophoneNotice() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Cobalt.copy(alpha = 0.09f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Cobalt.copy(alpha = 0.22f)),
    ) {
        Box {
            WaveDecoration(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(104.dp)
                    .height(64.dp),
                color = Cobalt,
                alpha = 0.10f,
                variant = 3,
            )
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = null,
                    tint = Cobalt,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(11.dp))
                Text(
                    text = "Microphone permission is requested only when you start measuring. NoiseFile does not listen while idle.",
                    modifier = Modifier.padding(end = 22.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun NeighborVerifyCard(onShare: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Ink),
    ) {
        Box {
            WaveDecoration(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .width(184.dp)
                    .height(148.dp),
                color = Signal,
                alpha = 0.12f,
                variant = 2,
                horizontalBars = true,
            )
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp),
            ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Signal,
                ) {
                    Icon(
                        Icons.Default.GroupAdd,
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.padding(10.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "NEIGHBOR VERIFY",
                        color = Signal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                    Text(
                        text = "Someone else hears it too?",
                        color = White,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
            Text(
                text = "Send a private invite asking a nearby resident to independently confirm the time and impact. Secure expiring verification links are the next backend step.",
                color = White.copy(alpha = 0.74f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onShare,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Signal,
                    contentColor = Ink,
                ),
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(9.dp))
                Text("Share a private invite")
            }
            }
        }
    }
}

@Composable
private fun ThreeStepStrip() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionTitle(eyebrow = "ONE CLEAR PATH", title = "From noise to next action")
        listOf(
            "1" to "Know the local process before you start.",
            "2" to "Measure and save each incident privately.",
            "3" to "File or escalate when your history is ready.",
        ).forEach { (number, text) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = CircleShape,
                    color = Ink,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(number, color = Signal, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(text, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
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
                        isCalibrating -> "CALIBRATING THIS MICROPHONE"
                        isAmbient -> "MEASURING THE QUIET FIRST"
                        else -> "MEASURING NOW"
                    },
                    color = Signal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
                Text(
                    text = when {
                        isCalibrating -> reading.micLabel
                        isAmbient -> "Quiet baseline · ${rule.jurisdiction.substringBefore(",")}"
                        else -> "${rule.jurisdiction.substringBefore(",")} · ${rule.noiseType.displayName}"
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
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(White, CircleShape),
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
        MeterGauge(reading)
        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MeterStat("MIN", reading.minimumDb, Modifier.weight(1f))
            MeterStat("AVERAGE", reading.averageDb, Modifier.weight(1f))
            MeterStat("MAX", reading.maximumDb, Modifier.weight(1f))
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = when (reading.calibration) {
                LevelCalibration.USER_CALIBRATED ->
                    "${reading.micLabel}, calibrated by you against a reference meter (${CalibrationMath.signed(reading.userOffsetDb)}). Still not a certified meter."
                LevelCalibration.PLATFORM_SPEC ->
                    "Level set by the Android compatibility spec for this phone's unprocessed microphone path (94 dB SPL = -36 dBFS). Still not a certified meter."
                LevelCalibration.ESTIMATE ->
                    "${reading.micLabel}, phone estimate. The microphone path carries its own gain, so the number can sit several dB off a real meter."
            },
            color = Muted,
            fontSize = 12.sp,
        )

        Spacer(Modifier.height(16.dp))

        if (isCalibrating) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Paper,
                border = androidx.compose.foundation.BorderStroke(2.dp, Cobalt),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "AGAINST A REFERENCE METER",
                        color = Cobalt,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.9.sp,
                    )
                    Text(
                        text = "Hold a real sound level meter next to this phone's microphone in a steady sound, " +
                            "a fan or radio hiss. Wait until both numbers settle, then type what the meter reads.",
                        color = Ink,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Phone average so far: ${reading.averageDb.roundToInt()} dB",
                        color = Ink,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = calibrationReferenceText,
                        onValueChange = onCalibrationReferenceChange,
                        label = { Text("Reference meter reading, dB") },
                        placeholder = { Text("Example: 62") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                    )
                    if (error != null) {
                        Text(text = error, color = Danger, style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(onClick = onCancel) { Text("Cancel") }
                }
            }
        } else if (isAmbient) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Paper,
                border = androidx.compose.foundation.BorderStroke(2.dp, Cobalt),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Text(
                        text = "QUIET BASELINE · ${rule.jurisdiction.substringBefore(",").uppercase(Locale.US)}",
                        color = Cobalt,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.9.sp,
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
                        color = Muted,
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
                    text = "CAPTURE COACH",
                    color = Signal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(7.dp))
                
                Text(
                    text = if (isCalibrating) {
                        "• A steady sound works best: a fan, a shower, radio static.\n" +
                            "• Meter and phone microphone side by side, same height, same direction.\n" +
                            "• The saved offset applies to this microphone only."
                    } else if (isAmbient) {
                        "• Ask for the noise to stop, or wait for a pause.\n" +
                            "• Stand exactly where you will measure the noise.\n" +
                            "• Keep still and quiet for the whole ${ambientTargetSeconds / 60} minutes; the capture ends on its own."
                    } else {
                        "• Hold the phone steady with its microphone uncovered.\n" +
                            "• Stay quiet while measuring.\n\n${rule.captureInstruction}"
                    },
                    color = White,
                    style = MaterialTheme.typography.bodyLarge,
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
                    isCalibrating -> "Calibration · Saved for ${reading.micLabel}"
                    isAmbient -> "Quiet baseline · The phone's offset cancels out of the difference"
                    else -> "Estimated sound level · Keep the microphone uncovered"
                },
                color = White.copy(alpha = 0.58f),
                fontSize = 12.sp,
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
                    containerColor = White,
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

@Composable
private fun MeterGauge(reading: MeterReading) {
    val sweep = ((reading.currentDb / 100.0).coerceIn(0.0, 1.0) * 260.0).toFloat()
    val animatedSweep by animateFloatAsState(targetValue = sweep, label = "GaugeSweep")
    
    val targetColor = when {
        reading.currentDb >= 75 -> Danger
        reading.currentDb >= 60 -> Signal
        else -> Cobalt
    }
    val animatedColor by animateColorAsState(targetValue = targetColor, label = "GaugeColor")

    Box(
        modifier = Modifier.size(250.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawArc(
                color = White.copy(alpha = 0.12f),
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                color = animatedColor,
                startAngle = 140f,
                sweepAngle = animatedSweep,
                useCenter = false,
                style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = reading.currentDb.roundToInt().toString(),
                color = White,
                fontSize = 72.sp,
                lineHeight = 76.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-2).sp,
            )
            Text(
                text = "estimated dB",
                color = White.copy(alpha = 0.62f),
                style = MaterialTheme.typography.titleMedium,
            )
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
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
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
        MeterAssessmentStatus.LISTENING -> Muted
        MeterAssessmentStatus.REACHES_LISTED_CONDITION -> Danger
        MeterAssessmentStatus.DOES_NOT_REACH_LISTED_CONDITION -> Signal
        MeterAssessmentStatus.NEEDS_INFORMATION -> Cobalt
    }
    val statusLabel = when (assessment.status) {
        MeterAssessmentStatus.LISTENING -> "CHECKING CITY RULE"
        MeterAssessmentStatus.REACHES_LISTED_CONDITION -> "LISTED CONDITION REACHED"
        MeterAssessmentStatus.DOES_NOT_REACH_LISTED_CONDITION -> "CONDITION NOT YET REACHED"
        MeterAssessmentStatus.NEEDS_INFORMATION -> "METER CANNOT DECIDE"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Paper,
        border = androidx.compose.foundation.BorderStroke(2.dp, statusColor),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(
                text = "$statusLabel · ${rule.jurisdiction.substringBefore(",").uppercase(Locale.US)}",
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.9.sp,
            )
            Text(
                text = assessment.headline,
                color = Ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = assessment.detail,
                color = Muted,
                style = MaterialTheme.typography.bodyMedium,
            )
            assessment.conditions.forEach { condition ->
                val conditionColor = when (condition.outcome) {
                    RuleConditionOutcome.REACHED -> Danger
                    RuleConditionOutcome.NOT_REACHED -> Signal
                    RuleConditionOutcome.NEEDS_INFORMATION -> Cobalt
                }
                val marker = when (condition.outcome) {
                    RuleConditionOutcome.REACHED -> "✓"
                    RuleConditionOutcome.NOT_REACHED -> "○"
                    RuleConditionOutcome.NEEDS_INFORMATION -> "•"
                }
                Text(
                    text = "$marker ${condition.text}",
                    color = conditionColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            HorizontalDivider(color = Line)
            if (assessment.status == MeterAssessmentStatus.NEEDS_INFORMATION) {
                Text(
                    text = "The phone reading remains useful evidence. The city makes the final determination.",
                    color = Muted,
                    fontSize = 12.sp,
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
                    color = Muted,
                    fontSize = 12.sp,
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
) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
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
                    IconButton(onClick = onDiscard) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Discard and go back")
                    }
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(
                            "REVIEW INCIDENT",
                            color = Cobalt,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp,
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
                            text = "CITY-SPECIFIC NEXT STEP",
                            color = Cobalt,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = rule.nextAction,
                            color = Muted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Save the incident below. NoiseFile will copy a completed complaint and open the best available city route.",
                            color = Ink,
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
                SectionTitle(
                    eyebrow = "LOCATION",
                    title = "Where was the noise?",
                )
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
                SectionTitle(
                    eyebrow = "IMPACT",
                    title = "How did it affect you?",
                )
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

@Composable
private fun MeasurementSummary(reading: MeterReading, rule: RuleWorkflow, ambient: AmbientReading? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Ink),
        shape = RoundedCornerShape(26.dp),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = rule.noiseType.displayName,
                color = Signal,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        text = reading.maximumDb.roundToInt().toString(),
                        color = White,
                        fontSize = 54.sp,
                        lineHeight = 56.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("maximum estimated dB", color = White.copy(alpha = 0.62f))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatElapsed(reading.elapsedMillis), color = White)
                    Text(
                        "AVERAGE ${reading.averageDb.roundToInt()} dB",
                        color = White.copy(alpha = 0.62f),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (ambient != null) {
                Text(
                    text = "Quiet baseline ${ambient.db.roundToInt()} dB over ${formatElapsed(ambient.seconds * 1_000L)} · " +
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

/** Which microphone the meter will use and how it is calibrated; the way in to calibrate it. */
@Composable
private fun MicrophoneCard(
    status: MicStatus,
    onBeginCalibration: () -> Unit,
    onClearCalibration: () -> Unit,
) {
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
                text = "MICROPHONE",
                color = Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
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
                        "Calibrated by you against a reference meter (${CalibrationMath.signed(status.profile?.offsetDb ?: 0.0)})."
                    LevelCalibration.PLATFORM_SPEC ->
                        "Level set by the Android compatibility spec for this phone's unprocessed microphone path. A reference meter can still tighten it."
                    LevelCalibration.ESTIMATE ->
                        "Estimate. This phone gives no calibration of its own. Hold a real meter next to it once and the app remembers the offset. " +
                            "Plug in a USB-C measurement microphone and it is picked up automatically."
                },
                color = Muted,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onBeginCalibration,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Calibrate against a meter", fontWeight = FontWeight.SemiBold)
                }
                if (status.profile != null) {
                    TextButton(onClick = onClearCalibration) { Text("Clear") }
                }
            }
        }
    }
}

/**
 * The jump's first half: measure the quiet room before the noise. Shown under
 * Start Recording; once a baseline exists it says so and offers a remeasure.
 */
@Composable
private fun AmbientBaselineCard(
    rule: RuleWorkflow,
    ambient: AmbientReading?,
    targetSeconds: Int,
    onBeginAmbient: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Cobalt.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Cobalt.copy(alpha = 0.25f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "MEASURE THE QUIET FIRST",
                color = Cobalt,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Text(
                text = if (ambient != null) {
                    "Quiet baseline ready: ${ambient.db.roundToInt()} dB over ${formatElapsed(ambient.seconds * 1_000L)}. " +
                        "Record the noise from the same spot and the app shows how far above the quiet it is."
                } else {
                    "A phone's dB number can sit several dB off. The difference between the quiet room and the noise does not, " +
                        "because both come from the same phone in the same spot. " +
                        (rule.ambientRecipe?.let { "${rule.jurisdiction.substringBefore(",")}'s code measures ambient over ${it.minutes} minutes." }
                            ?: "${rule.jurisdiction.substringBefore(",")}'s code sets no ambient recipe; NoiseFile records ${targetSeconds / 60} minutes.")
                },
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onBeginAmbient,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.GraphicEq, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (ambient != null) "Remeasure the quiet · ${targetSeconds / 60} min" else "Measure the quiet first · ${targetSeconds / 60} min",
                    fontWeight = FontWeight.SemiBold,
                )
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
    incidents: List<Incident>,
    ruleForIncident: (String) -> RuleWorkflow?,
    onShowHome: () -> Unit,
    onShowHistory: () -> Unit,
    onExport: () -> Unit,
    onUpdateDetails: (Long, String, String) -> Unit,
    onPrepareComplaint: (Incident, RuleWorkflow) -> Unit,
) {
    AppScaffold(
        selectedScreen = AppScreen.HISTORY,
        onShowHome = onShowHome,
        onShowHistory = onShowHistory,
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                BrandHeader()
                Spacer(Modifier.height(26.dp))
                SectionTitle(
                    eyebrow = "PRIVATE · ON THIS PHONE",
                    title = "Incident history",
                )
            }

            if (incidents.isNotEmpty()) {
                item {
                    Button(
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        onClick = onExport,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Cobalt, contentColor = White)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share incident history", style = MaterialTheme.typography.titleSmall)
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
                        onUpdateDetails = onUpdateDetails,
                        onPrepareComplaint = onPrepareComplaint,
                    )
                }
            }
        }
    }
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
                    tint = Cobalt,
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
) {
    var isEditingDetails by remember(incident.id) { mutableStateOf(false) }
    var locationDraft by remember(incident.id, incident.location) { mutableStateOf(incident.location) }
    var noteDraft by remember(incident.id, incident.notes) { mutableStateOf(incident.notes) }
    val date = DateTimeFormatter
        .ofPattern("EEE, MMM d · h:mm a", Locale.US)
        .format(
            Instant.ofEpochMilli(incident.startedAtEpochMillis)
                .atZone(ZoneId.systemDefault()),
        )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
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
                    Text(date, color = Muted, style = MaterialTheme.typography.bodyMedium)
                }
                Surface(
                    shape = CircleShape,
                    color = Signal.copy(alpha = 0.20f),
                ) {
                    Text(
                        text = "${incident.maximumDb.roundToInt()} dB max",
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        color = Ink,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
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
                        text = "LOCATION",
                        color = Cobalt,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = incident.location.ifBlank { "No location added." },
                        color = if (incident.location.isBlank()) Muted else Ink,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "NOTES",
                        color = Cobalt,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = incident.notes.ifBlank { "No notes added." },
                        color = if (incident.notes.isBlank()) Muted else Ink,
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
                        color = Danger,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                Text(
                    text = "This incident's city rule is no longer available.",
                    color = Danger,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(formatElapsed(incident.durationSeconds * 1_000), color = Muted)
                Text("Average ${incident.averageDb.roundToInt()} dB", color = Muted)
            }
        }
    }
}

@Composable
private fun StatusMessage(
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

@Composable
private fun AppScaffold(
    selectedScreen: AppScreen,
    onShowHome: () -> Unit,
    onShowHistory: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = Ink,
                tonalElevation = 0.dp,
            ) {
                NavigationBarItem(
                    selected = selectedScreen == AppScreen.HOME,
                    onClick = onShowHome,
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") },
                    colors = navColors(),
                )
                NavigationBarItem(
                    selected = selectedScreen == AppScreen.HISTORY,
                    onClick = onShowHistory,
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("History") },
                    colors = navColors(),
                )
            }
        },
        content = content,
    )
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Ink,
    selectedTextColor = White,
    indicatorColor = Signal,
    unselectedIconColor = White.copy(alpha = 0.58f),
    unselectedTextColor = White.copy(alpha = 0.58f),
)

private fun formatElapsed(millis: Long): String {
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
