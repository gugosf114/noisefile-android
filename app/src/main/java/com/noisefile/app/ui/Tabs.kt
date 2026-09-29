package com.noisefile.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noisefile.app.AppScreen
import com.noisefile.app.CalibrationMode
import com.noisefile.app.NoiseFileUiState
import com.noisefile.app.audio.MicStatus
import com.noisefile.app.model.Jurisdiction
import com.noisefile.app.model.NoiseType
import com.noisefile.app.model.RuleWorkflow
import com.noisefile.app.ui.theme.BarlowCondensed
import com.noisefile.app.ui.theme.Brass
import com.noisefile.app.ui.theme.Chalk
import com.noisefile.app.ui.theme.Danger
import com.noisefile.app.ui.theme.Deck
import com.noisefile.app.ui.theme.DeckHigh
import com.noisefile.app.ui.theme.Hairline
import com.noisefile.app.ui.theme.Ink
import com.noisefile.app.ui.theme.Line
import com.noisefile.app.ui.theme.Muted
import com.noisefile.app.ui.theme.PaperBlue
import com.noisefile.app.ui.theme.PaperGreen
import com.noisefile.app.ui.theme.PaperMuted
import com.noisefile.app.ui.theme.Success
import com.noisefile.app.ui.theme.White
import java.time.LocalTime
import kotlin.math.roundToInt

private const val SOURCES_PAGE = "https://mybakingcreations.com/noisefile-android/sources.html"

/** The city's dB limit at this hour, when its code has one. */
internal fun limitAt(rule: RuleWorkflow, hour: Int): Double? {
    val limit = rule.meterLimit ?: return null
    limit.fixedMaximumDb?.let { return it }
    val day = limit.daytimeStartsHour ?: return null
    val night = limit.nighttimeStartsHour ?: return null
    val isDay = if (day < night) hour in day until night else hour >= day || hour < night
    return if (isDay) limit.daytimeMaximumDb else limit.nighttimeMaximumDb
}

internal fun shortName(type: NoiseType): String = when (type) {
    NoiseType.BARKING_DOG -> "Animal"
    NoiseType.PARTY_MUSIC -> "Noise"
    NoiseType.CONSTRUCTION -> "Construction"
}

@Composable
internal fun AppScaffold(
    selectedScreen: AppScreen,
    nav: NavActions,
    content: @Composable (PaddingValues) -> Unit,
) {
    NightBackground {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = Color.Transparent,
            contentColor = Chalk,
            bottomBar = { BottomStrip(selectedScreen, nav) },
            content = content,
        )
    }
}

@Composable
internal fun BrandHeader(
    cityName: String,
    onCityClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Deck, RoundedCornerShape(13.dp))
                    .border(1.dp, Hairline, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Brass) }
            Spacer(Modifier.width(11.dp))
            Text(
                text = "NoiseFile",
                color = Chalk,
                fontFamily = BarlowCondensed,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
            )
        }
        val chip = RoundedCornerShape(50)
        Surface(
            modifier = Modifier
                .clip(chip)
                .then(if (onCityClick != null) Modifier.clickable(onClickLabel = "Change city", onClick = onCityClick) else Modifier),
            shape = chip,
            color = Deck,
            contentColor = Chalk,
            border = BorderStroke(1.dp, Hairline),
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = if (onCityClick != null) 8.dp else 14.dp, top = 9.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Brass, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text(cityName, style = MaterialTheme.typography.titleSmall)
                if (onCityClick != null) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Muted, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
internal fun HomeScreen(
    state: NoiseFileUiState,
    workflows: List<RuleWorkflow>,
    selectedRule: RuleWorkflow,
    selectedJurisdiction: Jurisdiction,
    onSelectRule: (String) -> Unit,
    onShowCityPicker: () -> Unit,
    onBeginCapture: () -> Unit,
    onBeginAmbient: () -> Unit,
    ambientTargetSeconds: Int,
    onSkipCalibrationPrompt: () -> Unit,
    onBeginSelfTest: () -> Unit,
    nav: NavActions,
) {
    val city = selectedJurisdiction.displayName
    val limit = limitAt(selectedRule, LocalTime.now().hour)
    AppScaffold(selectedScreen = AppScreen.HOME, nav = nav) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item { BrandHeader(cityName = city, onCityClick = onShowCityPicker) }

            if (state.message != null) {
                item { StatusMessage(text = state.message, color = Success, icon = Icons.Default.CheckCircle) }
            }
            if (state.error != null) {
                item { StatusMessage(text = state.error, color = Danger, icon = Icons.Default.Shield) }
            }
            if (state.showCalibrationPrompt) {
                item { CalibrationPromptCard(onCalibrate = onBeginSelfTest, onSkip = onSkipCalibrationPrompt) }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("What are you hearing?", style = MaterialTheme.typography.headlineMedium, color = Chalk)
                    Segmented(
                        options = workflows.map { shortName(it.noiseType) },
                        selectedIndex = workflows.indexOfFirst { it.id == selectedRule.id }.coerceAtLeast(0),
                        onSelect = { onSelectRule(workflows[it].id) },
                    )
                }
            }

            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    InstrumentDial(
                        valueDb = null,
                        limitDb = limit,
                        size = 268.dp,
                        modifier = Modifier.clip(CircleShape).clickable(onClickLabel = "Start recording", onClick = onBeginCapture),
                    ) {
                        Text("Ready", style = MaterialTheme.typography.displayMedium, color = Chalk)
                        Text(
                            text = if (limit != null) "$city limit now\n${limit.roundToInt()} dB" else "No dB limit in\n$city's rule",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (limit != null) Danger else Muted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            item {
                PaperCard {
                    Label("The rule in $city", color = PaperMuted)
                    Text(selectedRule.title, style = MaterialTheme.typography.titleLarge, color = Ink)
                    Text(
                        text = conditionPreview(selectedRule.summary, maxChars = 190) ?: selectedRule.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink,
                    )
                    TextButton(onClick = nav.rules, contentPadding = PaddingValues(0.dp)) {
                        Text("Read the rule", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            item {
                DeckCard {
                    Text("Quiet baseline", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = state.ambient?.let {
                            "Ready: ${it.db.roundToInt()} dB over ${formatElapsed(it.seconds * 1_000L)}. " +
                                "Record the noise from the same spot."
                        } ?: "Measure the quiet room first. Then each incident shows how far above the quiet it was.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted,
                    )
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        onClick = onBeginAmbient,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Hairline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Chalk),
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Brass)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            (if (state.ambient != null) "Measure the quiet again" else "Measure the quiet") +
                                ", ${ambientTargetSeconds / 60} min",
                        )
                    }
                }
            }
        }
    }
}

/** The rule as the city wrote it, on paper. */
@Composable
internal fun RuleSheet(rule: RuleWorkflow, incidentCount: Int, onOpenUri: (String) -> Unit) {
    var open by remember(rule.id) { mutableStateOf(false) }
    val preview = conditionPreview(rule.summary, maxChars = 360)
    PaperCard {
        Text(rule.title, style = MaterialTheme.typography.headlineSmall, color = Ink)
        Text(
            text = if (open || preview == null) rule.summary else preview,
            style = MaterialTheme.typography.bodyLarge,
            color = Ink,
        )
        if (preview != null) {
            TextButton(onClick = { open = !open }, contentPadding = PaddingValues(0.dp)) {
                Text(if (open) "Show less" else "Read the full rule", style = MaterialTheme.typography.labelLarge)
            }
        }
        rule.hoursRule?.let { CodeQuote(quote = it.sourceQuote, citation = it.sourceCitation) }
        rule.ambientRecipe?.let { CodeQuote(quote = it.sourceQuote, citation = it.sourceCitation) }

        rule.requiredIncidentCount?.let { required ->
            val progress = (incidentCount.toFloat() / required).coerceIn(0f, 1f)
            val tone = if (progress >= 1f) PaperGreen else PaperBlue
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Your incidents so far", style = MaterialTheme.typography.labelLarge, color = Ink)
                Text("${incidentCount.coerceAtMost(required)} of $required", style = MaterialTheme.typography.labelLarge, color = tone)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(9.dp).clip(CircleShape),
                color = tone,
                trackColor = Line,
            )
        }

        HorizontalDivider(color = Line)
        Label("What to write down", color = PaperMuted)
        Text(rule.captureInstruction, style = MaterialTheme.typography.bodyMedium, color = Ink)

        HorizontalDivider(color = Line)
        Label("Next step", color = PaperMuted)
        Text(rule.nextAction, style = MaterialTheme.typography.bodyMedium, color = Ink)
        Button(
            modifier = Modifier.fillMaxWidth().height(54.dp),
            onClick = { onOpenUri(rule.actionUri) },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = White),
        ) { Text(rule.actionLabel, maxLines = 1) }
        if (rule.secondaryActionLabel != null && rule.secondaryActionUri != null) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth().height(54.dp),
                onClick = { onOpenUri(rule.secondaryActionUri) },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
            ) { Text(rule.secondaryActionLabel, maxLines = 1) }
        }
        TextButton(onClick = { onOpenUri(rule.officialSourceUrl) }, contentPadding = PaddingValues(0.dp)) {
            Text("Official source, checked ${rule.verifiedDate}", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
internal fun RulesScreen(
    workflows: List<RuleWorkflow>,
    selectedRule: RuleWorkflow,
    selectedJurisdiction: Jurisdiction,
    incidentCount: Int,
    onSelectRule: (String) -> Unit,
    onShowCityPicker: () -> Unit,
    onOpenUri: (String) -> Unit,
    nav: NavActions,
) {
    AppScaffold(selectedScreen = AppScreen.RULES, nav = nav) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item { BrandHeader(cityName = selectedJurisdiction.displayName, onCityClick = onShowCityPicker) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("The rule in ${selectedJurisdiction.displayName}", style = MaterialTheme.typography.headlineMedium, color = Chalk)
                    Segmented(
                        options = workflows.map { shortName(it.noiseType) },
                        selectedIndex = workflows.indexOfFirst { it.id == selectedRule.id }.coerceAtLeast(0),
                        onSelect = { onSelectRule(workflows[it].id) },
                    )
                }
            }
            item { RuleSheet(rule = selectedRule, incidentCount = incidentCount, onOpenUri = onOpenUri) }
        }
    }
}

@Composable
internal fun MoreScreen(
    selectedJurisdiction: Jurisdiction,
    micStatus: MicStatus,
    onShowCityPicker: () -> Unit,
    onBeginSelfTest: () -> Unit,
    onBeginCalibration: (CalibrationMode) -> Unit,
    onClearCalibration: () -> Unit,
    onShareNeighbor: () -> Unit,
    onOpenUri: (String) -> Unit,
    nav: NavActions,
) {
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }
    AppScaffold(selectedScreen = AppScreen.MORE, nav = nav) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item { BrandHeader(cityName = selectedJurisdiction.displayName, onCityClick = onShowCityPicker) }
            item { Text("More", style = MaterialTheme.typography.headlineMedium, color = Chalk) }

            item {
                DeckCard {
                    SettingRow(Icons.Default.LocationOn, "City", selectedJurisdiction.displayName, onShowCityPicker)
                    HorizontalDivider(color = Hairline)
                    SettingRow(
                        Icons.Default.Groups,
                        "Ask a neighbor",
                        "Send a note asking what they heard, and when",
                        onShareNeighbor,
                    )
                    HorizontalDivider(color = Hairline)
                    SettingRow(Icons.Default.Link, "Official sources", "Every city's own code and pages") { onOpenUri(SOURCES_PAGE) }
                }
            }

            item {
                MicrophoneCard(
                    status = micStatus,
                    onBeginSelfTest = onBeginSelfTest,
                    onBeginCalibration = onBeginCalibration,
                    onClearCalibration = onClearCalibration,
                )
            }

            item {
                DeckCard {
                    Text("How it works", style = MaterialTheme.typography.titleMedium)
                    listOf(
                        "Pick your city and what you hear. Read the rule.",
                        "Press record when the noise starts. Save the incident.",
                        "When your incidents add up, export the report and file it with the city.",
                    ).forEachIndexed { index, step ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier.size(30.dp).background(DeckHigh, CircleShape).border(1.dp, Brass.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${index + 1}", color = Brass, fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(step, style = MaterialTheme.typography.bodyMedium, color = Chalk, modifier = Modifier.padding(top = 3.dp))
                        }
                    }
                }
            }

            item {
                DeckCard {
                    Text("Your privacy", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Everything stays on this phone. The app has no internet access and no account. " +
                            "It listens only while you record.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Chalk,
                    )
                    Text(
                        "Deleting the app deletes your incidents, photos and sound clips for good. " +
                            "Export the PDF report first if you want to keep them.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted,
                    )
                }
            }

            item {
                Text(
                    text = "NoiseFile $version. An independent app by WiM Labs, not a government app. " +
                        "Set in Barlow and Crimson Text, used under the SIL Open Font License.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
        }
    }
}
