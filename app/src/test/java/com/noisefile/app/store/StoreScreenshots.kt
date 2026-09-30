package com.noisefile.app.store

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.noisefile.app.NoiseFileUiState
import com.noisefile.app.AppScreen
import com.noisefile.app.audio.MicStatus
import com.noisefile.app.data.EvidenceSeal
import com.noisefile.app.data.RuleCatalog
import com.noisefile.app.model.AmbientReading
import com.noisefile.app.model.Incident
import com.noisefile.app.model.MeterReading
import com.noisefile.app.model.NoiseType
import com.noisefile.app.CaptureStage
import com.noisefile.app.ui.HistoryScreen
import com.noisefile.app.ui.HomeScreen
import com.noisefile.app.ui.MeterScreen
import com.noisefile.app.ui.MoreScreen
import com.noisefile.app.ui.NavActions
import com.noisefile.app.ui.ReviewScreen
import com.noisefile.app.ui.RulesScreen
import com.noisefile.app.ui.theme.NoiseFileTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The store screenshots. The app draws its own screens on the build machine, with made-up
 * incidents on a made-up street, so no real phone, no real address and no real recording is used.
 * Output: app/build/outputs/store-screens/NN-name.png, 1080 x 2400.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h914dp-420dpi")
class StoreScreenshots {
    @get:Rule val compose = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val catalog by lazy { RuleCatalog.fromAssets(context) }
    private val nav = NavActions({}, {}, {}, {}, {})
    private val city get() = catalog.jurisdictions.first { it.id == "richmond" }
    private val noise get() = catalog.retrieve("richmond", NoiseType.PARTY_MUSIC)!!
    private val workflows get() = catalog.forJurisdiction("richmond")

    private fun shot(name: String, content: @Composable () -> Unit) {
        compose.setContent { NoiseFileTheme { content() } }
        compose.waitForIdle()
        val dir = listOf(File("build/outputs/store-screens"), File("app/build/outputs/store-screens"))
            .first { it.parentFile?.parentFile?.exists() == true }
        dir.mkdirs()
        compose.onRoot().captureRoboImage(File(dir, "$name.png").path)
    }

    private fun trace(peak: Int, seconds: Int, seed: Int): List<Int> {
        val r = java.util.Random(seed.toLong())
        return List(seconds) { i ->
            val swell = if (i in seconds / 3..seconds * 2 / 3) peak - 4 else peak - 16
            (swell + r.nextInt(9) - 4).coerceIn(30, peak)
        }.toMutableList().also { it[seconds / 2] = peak }
    }

    private fun incident(id: Long, at: LocalDateTime, peak: Int, average: Int, seconds: Int, notes: String): Incident =
        Incident(
            id = id, ruleId = noise.id, noiseType = NoiseType.PARTY_MUSIC,
            startedAtEpochMillis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            durationSeconds = seconds.toLong(), minimumDb = 38.0, averageDb = average.toDouble(), maximumDb = peak.toDouble(),
            location = "Oak Street, next door", impact = "Woke me or someone in my home", notes = notes,
            ambientDb = 36.0, ambientSeconds = 300L,
            levelTrace = trace(peak, seconds, id.toInt()), clipName = null, clipSeconds = 0,
        )

    private fun incidents(): List<Incident> {
        val raw = listOf(
            incident(1, LocalDateTime.of(2026, 9, 18, 23, 12), 63, 55, 74, "Bass through the wall."),
            incident(2, LocalDateTime.of(2026, 9, 19, 23, 48), 66, 57, 92, "Music and shouting on the patio."),
            incident(3, LocalDateTime.of(2026, 9, 25, 22, 40), 61, 54, 58, "Same speakers, windows open."),
            incident(4, LocalDateTime.of(2026, 9, 26, 23, 5), 68, 59, 120, "Loud music through the wall."),
        )
        val sealed = ArrayList<Incident>()
        raw.forEach { sealed.add(EvidenceSeal.seal(it, sealed.lastOrNull())) }
        return sealed.reversed()
    }

    private fun reading(now: Int, peak: Int, average: Int, seconds: Int) = MeterReading(
        currentDb = now.toDouble(), minimumDb = 39.0, averageDb = average.toDouble(), maximumDb = peak.toDouble(),
        elapsedMillis = seconds * 1_000L, sampleWindows = seconds * 10,
    )

    @Test fun s01_home() = shot("01-home") {
        HomeScreen(
            state = NoiseFileUiState(selectedJurisdictionId = "richmond", selectedRuleId = noise.id),
            workflows = workflows, selectedRule = noise, selectedJurisdiction = city,
            onSelectRule = {}, onShowCityPicker = {}, onBeginCapture = {}, onBeginAmbient = {},
            ambientTargetSeconds = 300, onSkipCalibrationPrompt = {}, onBeginSelfTest = {}, nav = nav,
        )
    }

    @Test fun s02_recording() = shot("02-recording") {
        MeterScreen(
            rule = noise, reading = reading(now = 64, peak = 66, average = 57, seconds = 41), incidentCount = 3,
            stage = CaptureStage.NOISE, ambient = AmbientReading(db = 36.0, seconds = 300L, sampleWindows = 3_000),
            ambientTargetSeconds = 300, calibrationReferenceText = "", error = null,
            onCalibrationReferenceChange = {}, onStop = {}, onCancel = {},
        )
    }

    @Test fun s03_review() = shot("03-review") {
        ReviewScreen(
            state = NoiseFileUiState(
                screen = AppScreen.REVIEW, selectedJurisdictionId = "richmond", selectedRuleId = noise.id,
                meterReading = reading(now = 52, peak = 66, average = 57, seconds = 92),
                ambient = AmbientReading(db = 36.0, seconds = 300L, sampleWindows = 3_000),
                draftLocation = "Oak Street, next door", draftImpact = "Woke me or someone in my home",
            ),
            rule = noise, incidentCount = 3,
            onLocationChange = {}, onImpactChange = {}, onNotesChange = {}, onSave = {}, onSaveAndPrepare = {}, onDiscard = {},
        )
    }

    @Test fun s04_incidents() = shot("04-incidents") {
        val list = incidents()
        HistoryScreen(
            cityName = city.displayName, incidents = list, ruleForIncident = catalog::byId,
            sealReport = EvidenceSeal.verify(list), fileFor = { _, n -> File(n) }, nav = nav,
            onExport = {}, onExportPdf = {}, onUpdateDetails = { _, _, _ -> }, onPrepareComplaint = { _, _ -> },
        )
    }

    @Test fun s05_rules() = shot("05-rules") {
        val rule = catalog.retrieve("daly-city", NoiseType.PARTY_MUSIC)!!
        RulesScreen(
            workflows = catalog.forJurisdiction("daly-city"), selectedRule = rule,
            selectedJurisdiction = catalog.jurisdictions.first { it.id == "daly-city" }, incidentCount = 0,
            onSelectRule = {}, onShowCityPicker = {}, onOpenUri = {}, nav = nav,
        )
    }

    @Test fun s06_more() = shot("06-more") {
        MoreScreen(
            selectedJurisdiction = city,
            micStatus = MicStatus(micKey = "builtin", micLabel = "Built-in microphone", isUsb = false, supportsUnprocessed = false, profile = null),
            onShowCityPicker = {}, onBeginSelfTest = {}, onBeginCalibration = {}, onClearCalibration = {},
            onShareNeighbor = {}, onOpenUri = {}, nav = nav,
        )
    }
}
