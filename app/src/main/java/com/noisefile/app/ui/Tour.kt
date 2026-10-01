package com.noisefile.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.noisefile.app.ui.theme.BarlowCondensed
import com.noisefile.app.ui.theme.Brass
import com.noisefile.app.ui.theme.Chalk
import com.noisefile.app.ui.theme.Cobalt
import com.noisefile.app.ui.theme.Deck
import com.noisefile.app.ui.theme.Muted
import com.noisefile.app.ui.theme.White
import kotlin.math.roundToInt

/**
 * The finger that points. The first time the app opens, the screen dims and one
 * real control stays lit, with a short note. Five stops, in the order a person
 * actually uses the app. Skip is on every card; a person with noise going on
 * right now must reach the red button in one tap.
 */
enum class TourStep(val title: String) {
    CITY("Pick your city"),
    TYPES("Pick what you hear"),
    QUIET("Measure the quiet in your room"),
    RECORD("Press this when the noise starts"),
    INCIDENTS("Your incidents live here"),
    ;

    /** The note under the title. [priceText] is Play's own price string when the phone has it. */
    fun body(priceText: String?): String = when (this) {
        CITY -> "Every rule in this app is that city's own law. Tap the city name to change it."
        TYPES -> "Animal, noise, or construction. The rule under the dial changes with your pick."
        QUIET -> "Pick the room where the noise hits you. Measure its quiet once, on a quiet night. Some cities' laws set the minutes, 6 or 10, and judge noise by the jump above the quiet. Others set a flat limit; the baseline still shows the jump. Skip it if the noise is already going."
        RECORD -> "Record until it stops. Then type where it came from, and save."
        INCIDENTS -> "Saved incidents stack up here. Looking and recording stay free. The PDF report and the city form guide are the one paid part: " +
            (priceText?.let { "$it, once." } ?: "paid once.")
    }

    val next: TourStep? get() = entries.getOrNull(ordinal + 1)
}

/** Where each lit control sits on screen, reported by the real controls as they lay out. */
typealias TourTargets = SnapshotStateMap<TourStep, Rect>

val LocalTourTargets = compositionLocalOf<TourTargets?> { null }

fun newTourTargets(): TourTargets = mutableStateMapOf()

/** Put this on the real control so the tour can light it. Costs nothing when no tour is running. */
@Composable
fun Modifier.tourTarget(step: TourStep): Modifier {
    val targets = LocalTourTargets.current ?: return this
    return onGloballyPositioned { targets[step] = it.boundsInRoot() }
}

@Composable
internal fun TourOverlay(
    step: TourStep,
    priceText: String?,
    targets: TourTargets,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val hole = targets[step]
    val density = LocalDensity.current
    val pad = with(density) { 8.dp.toPx() }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            // Swallow every touch: the only ways out are Next and Skip.
            .pointerInput(step) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawRect(Color(0xCC070B16))
                if (hole != null) {
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset(hole.left - pad, hole.top - pad),
                        size = Size(hole.width + pad * 2, hole.height + pad * 2),
                        cornerRadius = CornerRadius(with(density) { 22.dp.toPx() }),
                        blendMode = BlendMode.Clear,
                    )
                    drawRoundRect(
                        color = Brass,
                        topLeft = Offset(hole.left - pad, hole.top - pad),
                        size = Size(hole.width + pad * 2, hole.height + pad * 2),
                        cornerRadius = CornerRadius(with(density) { 22.dp.toPx() }),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = with(density) { 2.dp.toPx() }),
                    )
                }
                drawContent()
            },
    ) {
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val cardMargin = with(density) { 18.dp.toPx() }
        val cardHeightGuess = with(density) { 230.dp.toPx() }
        // The card sits under the hole when there is room, else above it.
        val cardTop = when {
            hole == null -> screenHeightPx * 0.4f
            hole.bottom + pad + cardMargin + cardHeightGuess < screenHeightPx -> hole.bottom + pad + cardMargin
            else -> (hole.top - pad - cardMargin - cardHeightGuess).coerceAtLeast(cardMargin)
        }
        Column(
            modifier = Modifier
                .offset { IntOffset(0, cardTop.roundToInt()) }
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .background(Deck, RoundedCornerShape(20.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "${step.ordinal + 1} of ${TourStep.entries.size}",
                style = MaterialTheme.typography.labelMedium,
                color = Brass,
            )
            Text(
                text = step.title,
                fontFamily = BarlowCondensed,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                color = Chalk,
            )
            Text(step.body(priceText), style = MaterialTheme.typography.bodyLarge, color = Chalk)
            Spacer(Modifier.height(2.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onSkip) { Text("Skip", color = Muted) }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onNext,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Cobalt, contentColor = White),
                ) {
                    Text(if (step.next == null) "Done" else "Next")
                    Spacer(Modifier.width(2.dp))
                }
            }
        }
        Box(Modifier.fillMaxSize()) // keeps the BoxWithConstraints full-size
    }
}
