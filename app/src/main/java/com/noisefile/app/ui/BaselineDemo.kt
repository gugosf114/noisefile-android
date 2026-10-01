package com.noisefile.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noisefile.app.ui.theme.BarlowCondensed
import com.noisefile.app.ui.theme.Brass
import com.noisefile.app.ui.theme.Chalk
import com.noisefile.app.ui.theme.Cobalt
import com.noisefile.app.ui.theme.Deck
import com.noisefile.app.ui.theme.DeckHigh
import com.noisefile.app.ui.theme.Hairline
import com.noisefile.app.ui.theme.Muted
import com.noisefile.app.ui.theme.Night
import com.noisefile.app.ui.theme.White
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The baseline, shown instead of told: a small live scene drawn by the app itself.
 * A brass finger taps a room, taps Measure, the dial runs five minutes in three
 * seconds, and the baseline lands in its card. Loops. No video file, nothing to go stale.
 */
private enum class Scene { PICK_ROOM, TAP_MEASURE, MEASURING, SAVED, HOLD }

@Composable
internal fun BaselineDemo(modifier: Modifier = Modifier) {
    var scene by remember { mutableStateOf(Scene.PICK_ROOM) }
    var roomPicked by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    var clockSeconds by remember { mutableIntStateOf(0) }
    var wobble by remember { mutableIntStateOf(0) }
    // Where the finger is, as a fraction of the scene box.
    var fingerX by remember { mutableStateOf(0.95f) }
    var fingerY by remember { mutableStateOf(1.05f) }
    var fingerShown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            scene = Scene.PICK_ROOM; roomPicked = false; pressed = false; clockSeconds = 0
            fingerShown = true; fingerX = 0.95f; fingerY = 1.05f
            delay(300)
            fingerX = 0.17f; fingerY = 0.46f        // the Bedroom chip
            delay(900)
            pressed = true; delay(160); pressed = false; roomPicked = true
            delay(700)
            scene = Scene.TAP_MEASURE
            fingerX = 0.5f; fingerY = 0.84f         // the Measure button
            delay(900)
            pressed = true; delay(160); pressed = false
            delay(300)
            fingerShown = false
            scene = Scene.MEASURING
            val start = System.currentTimeMillis()
            while (clockSeconds < 300) {
                val t = (System.currentTimeMillis() - start).toInt()
                clockSeconds = (t / 10).coerceAtMost(300)   // 3 s of real time = 5:00 on the clock
                wobble = t
                delay(40)
            }
            delay(400)
            scene = Scene.SAVED
            delay(2_000)
            scene = Scene.HOLD
            delay(1_200)
        }
    }

    val fx by animateFloatAsState(fingerX, tween(800), label = "fx")
    val fy by animateFloatAsState(fingerY, tween(800), label = "fy")
    val press by animateFloatAsState(if (pressed) 0.8f else 1f, tween(120), label = "press")

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Night)
                .border(1.dp, Hairline, RoundedCornerShape(16.dp)),
        ) {
            // The scene: the quiet card, then the dial, then the baselines card.
            androidx.compose.animation.AnimatedVisibility(visible = scene == Scene.PICK_ROOM || scene == Scene.TAP_MEASURE, enter = fadeIn(), exit = fadeOut()) {
                MiniQuietCard(roomPicked = roomPicked, buttonLit = scene == Scene.TAP_MEASURE && pressed)
            }
            androidx.compose.animation.AnimatedVisibility(visible = scene == Scene.MEASURING, enter = fadeIn(), exit = fadeOut()) {
                MiniMeter(clockSeconds = clockSeconds, db = 28.0 + 3.0 * sin(wobble / 180.0))
            }
            androidx.compose.animation.AnimatedVisibility(visible = scene == Scene.SAVED || scene == Scene.HOLD, enter = fadeIn(), exit = fadeOut()) {
                MiniBaselinesCard()
            }
            // The finger.
            if (fingerShown) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                ) {
                    androidx.compose.foundation.layout.BoxWithConstraints {
                        val w = maxWidth
                        val h = maxHeight
                        Box(
                            modifier = Modifier
                                .offset { IntOffset((fx * w.toPx()).roundToInt() - 14.dp.roundToPx(), (fy * h.toPx()).roundToInt() - 14.dp.roundToPx()) }
                                .size(28.dp)
                                .scale(press)
                                .background(Brass.copy(alpha = 0.35f), CircleShape)
                                .border(2.dp, Brass, CircleShape),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = when (scene) {
                Scene.PICK_ROOM -> "Pick the room where the noise hits you."
                Scene.TAP_MEASURE -> "Tap Measure. Noise off, phone on a table."
                Scene.MEASURING -> "Keep still. Five minutes, or your city's minutes."
                Scene.SAVED, Scene.HOLD -> "Saved under Baselines. Once per room. It stays."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Chalk,
        )
    }
}

@Composable
private fun MiniQuietCard(roomPicked: Boolean, buttonLit: Boolean) {
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Quiet baseline (ambient)", color = Chalk, fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text("Where will you stand?", color = Muted, fontSize = 11.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MiniChip("Bedroom", lit = roomPicked)
            MiniChip("Kitchen", lit = false)
            MiniChip("Back yard", lit = false)
        }
        Text(
            text = if (roomPicked) "No baseline for Bedroom yet. Measure it once, with the noise off." else "Pick the room where the noise hits you.",
            color = Muted,
            fontSize = 11.sp,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (buttonLit) DeckHigh else Deck)
                .border(1.dp, if (buttonLit) Brass else Hairline, RoundedCornerShape(10.dp)),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Brass, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (roomPicked) "Measure the quiet in Bedroom, 5 min" else "Measure the quiet, 5 min", color = Chalk, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MiniChip(text: String, lit: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (lit) Cobalt else DeckHigh)
            .border(1.dp, if (lit) Cobalt else Hairline, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) { Text(text, color = if (lit) White else Chalk, fontSize = 11.sp) }
}

@Composable
private fun MiniMeter(clockSeconds: Int, db: Double) {
    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        InstrumentDial(valueDb = db, limitDb = null, size = 150.dp) {
            Text("${db.roundToInt()}", color = Chalk, fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold, fontSize = 30.sp)
            Text("dB", color = Muted, fontSize = 10.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text("Quiet baseline in Bedroom", color = Muted, fontSize = 11.sp)
            Text("${clockSeconds / 60}:${"%02d".format(clockSeconds % 60)} / 5:00", color = Chalk, fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold, fontSize = 26.sp)
            Text("Keep still and quiet.", color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun MiniBaselinesCard() {
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Baselines", color = Chalk, fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text("Each room's quiet, measured once and kept. Not incidents.", color = Muted, fontSize = 11.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(26.dp).background(DeckHigh, CircleShape).border(1.dp, Brass, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.Check, contentDescription = null, tint = Brass, modifier = Modifier.size(16.dp)) }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Bedroom", color = Chalk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("28 dB over 5:00, measured today", color = Muted, fontSize = 11.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.alpha(0.5f)) {
            Box(modifier = Modifier.size(26.dp).border(1.dp, Hairline, CircleShape))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Kitchen", color = Chalk, fontSize = 13.sp)
                Text("not measured yet", color = Muted, fontSize = 11.sp)
            }
        }
    }
}
