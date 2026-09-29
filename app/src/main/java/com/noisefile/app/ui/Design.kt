package com.noisefile.app.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noisefile.app.AppScreen
import com.noisefile.app.ui.theme.BarlowCondensed
import com.noisefile.app.ui.theme.Brass
import com.noisefile.app.ui.theme.BrassDeep
import com.noisefile.app.ui.theme.Chalk
import com.noisefile.app.ui.theme.Danger
import com.noisefile.app.ui.theme.Deck
import com.noisefile.app.ui.theme.DeckHigh
import com.noisefile.app.ui.theme.Hairline
import com.noisefile.app.ui.theme.Ink
import com.noisefile.app.ui.theme.Line
import com.noisefile.app.ui.theme.Muted
import com.noisefile.app.ui.theme.Night
import com.noisefile.app.ui.theme.NoiseFileTypography
import com.noisefile.app.ui.theme.Paper
import com.noisefile.app.ui.theme.PaperBlue
import com.noisefile.app.ui.theme.PaperMuted
import com.noisefile.app.ui.theme.PaperRed
import com.noisefile.app.ui.theme.RecordRed
import com.noisefile.app.ui.theme.RecordRedDeep
import com.noisefile.app.ui.theme.White
import kotlin.math.cos
import kotlin.math.sin

// ---------------------------------------------------------------------------------------------
// Two materials. The instrument: dark metal, brass, fine grain. The file: paper, ink, the city's words.
// ---------------------------------------------------------------------------------------------

/** A sheet of fine grain, made once and tiled. Light specks for metal, dark specks for paper. */
@Composable
fun rememberGrain(light: Boolean, strength: Float): ShaderBrush = remember(light, strength) {
    val size = 128
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val random = java.util.Random(if (light) 11L else 23L)
    val tone = if (light) 255 else 0
    val pixels = IntArray(size * size) {
        val alpha = (random.nextFloat() * random.nextFloat() * strength * 255f).toInt().coerceIn(0, 255)
        android.graphics.Color.argb(alpha, tone, tone, tone)
    }
    bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
    ShaderBrush(ImageShader(bitmap.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
}

/** The night sky behind every screen: a slow fall to black, a little brass light from the top corner, grain. */
@Composable
fun NightBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val grain = rememberGrain(light = true, strength = 0.11f)
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF121C32), Night, Color(0xFF080C16))))
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(Brass.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(size.width * 0.92f, 0f),
                        radius = size.width * 0.95f,
                    ),
                )
                drawRect(grain)
            },
        content = content,
    )
}

/** Inside paper, every Material part (links, fields, dividers) takes ink-on-paper colours on its own. */
private val PaperColors = lightColorScheme(
    primary = PaperBlue,
    onPrimary = White,
    secondary = Ink,
    onSecondary = White,
    error = PaperRed,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFECE8DF),
    onSurfaceVariant = PaperMuted,
    outline = Color(0xFFB9B3A4),
    outlineVariant = Line,
)

@Composable
fun PaperTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PaperColors, typography = NoiseFileTypography, content = content)
}

/** The file: anything that quotes the city or ends up in the report sits on paper. */
@Composable
fun PaperCard(
    modifier: Modifier = Modifier,
    edge: Color = Line,
    edgeWidth: Dp = 1.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val grain = rememberGrain(light = false, strength = 0.10f)
    PaperTheme {
        Surface(
            modifier = modifier.fillMaxWidth().shadow(10.dp, RoundedCornerShape(20.dp), clip = false),
            shape = RoundedCornerShape(20.dp),
            color = Paper,
            contentColor = Ink,
            border = BorderStroke(edgeWidth, edge),
        ) {
            Column(
                modifier = Modifier
                    .drawBehind {
                        drawRect(grain)
                        // a sheet catches light along its top edge
                        drawRect(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.55f), Color.Transparent),
                                endY = 60.dp.toPx(),
                            ),
                        )
                    }
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content,
            )
        }
    }
}

/** A raised panel on the instrument. */
@Composable
fun DeckCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, clip = false)
            .then(if (onClick != null) Modifier.clip(shape).clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = Deck,
        contentColor = Chalk,
        border = BorderStroke(1.dp, Hairline),
    ) {
        Column(
            modifier = Modifier
                .drawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.045f), Color.Transparent),
                            endY = 90.dp.toPx(),
                        ),
                    )
                }
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

/** A quiet label. Sentence case, never shouted. */
@Composable
fun Label(text: String, color: Color = Muted, modifier: Modifier = Modifier) {
    Text(text = text, color = color, style = MaterialTheme.typography.labelMedium, modifier = modifier)
}

/** One row in a list of settings: what it is, what it is set to, and the way in. */
@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    detail: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(44.dp).background(DeckHigh, CircleShape).border(1.dp, Hairline, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = Brass) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Chalk)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Muted)
    }
}

/** Pick one of a few. The chosen key sits raised with a brass edge. */
@Composable
fun Segmented(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0A101D),
        border = BorderStroke(1.dp, Hairline),
    ) {
        Row(modifier = Modifier.padding(5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            options.forEachIndexed { index, option ->
                val chosen = index == selectedIndex
                val shape = RoundedCornerShape(13.dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .then(if (chosen) Modifier.shadow(6.dp, shape, clip = false) else Modifier)
                        .clip(shape)
                        .background(
                            if (chosen) Brush.verticalGradient(listOf(Color(0xFF2A3754), DeckHigh))
                            else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)),
                        )
                        .then(if (chosen) Modifier.border(1.dp, Brass.copy(alpha = 0.75f), shape) else Modifier)
                        .clickable(role = Role.Tab) { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = option,
                        color = if (chosen) Chalk else Muted,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * The dial. 20 to 100 dB around three quarters of a circle, engraved ticks, a brass sweep for the
 * sound, and a red mark where the city's own limit sits (when the code has one).
 */
@Composable
fun InstrumentDial(
    valueDb: Double?,
    limitDb: Double?,
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
    center: @Composable ColumnScope.() -> Unit,
) {
    val target = ((valueDb ?: 20.0).coerceIn(20.0, 100.0)).toFloat()
    val shown by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "DialSweep",
    )
    val over = valueDb != null && limitDb != null && valueDb >= limitDb
    val measurer = rememberTextMeasurer()
    val numeral = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Muted)
    val grain = rememberGrain(light = true, strength = 0.14f)

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2f
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            fun angle(db: Float) = 135f + (db - 20f) / 80f * 270f
            fun at(deg: Float, radius: Float): Offset {
                val rad = Math.toRadians(deg.toDouble())
                return Offset(c.x + radius * cos(rad).toFloat(), c.y + radius * sin(rad).toFloat())
            }

            // the body: a shallow dish
            drawCircle(Color.Black.copy(alpha = 0.55f), radius = r, center = c + Offset(0f, 6.dp.toPx()))
            drawCircle(
                Brush.linearGradient(
                    listOf(Color(0xFFFFE3AA), Brass, BrassDeep, Color(0xFF6A4410), BrassDeep),
                    start = Offset(c.x - r, c.y - r),
                    end = Offset(c.x + r, c.y + r),
                ),
                radius = r,
                center = c,
            )
            drawCircle(Color(0xFF05080F), radius = r - 5.dp.toPx(), center = c)
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFF26324D), Color(0xFF151E31), Color(0xFF0B111E)),
                    center = c + Offset(-r * 0.25f, -r * 0.35f),
                    radius = r * 1.5f,
                ),
                radius = r - 8.dp.toPx(),
                center = c,
            )
            drawCircle(grain, radius = r - 8.dp.toPx(), center = c)

            // the zone above the city's limit
            val tickOuter = r - 22.dp.toPx()
            if (limitDb != null && limitDb in 20.0..100.0) {
                val from = angle(limitDb.toFloat())
                drawArc(
                    color = Danger.copy(alpha = 0.30f),
                    startAngle = from,
                    sweepAngle = 45f + 360f - from,
                    useCenter = false,
                    topLeft = Offset(c.x - tickOuter, c.y - tickOuter),
                    size = Size(tickOuter * 2, tickOuter * 2),
                    style = Stroke(width = 7.dp.toPx()),
                )
            }

            // engraved ticks and numerals
            var db = 20
            while (db <= 100) {
                val major = db % 10 == 0
                val length = if (major) 15.dp.toPx() else 7.dp.toPx()
                val a = angle(db.toFloat())
                drawLine(
                    color = if (major) Chalk.copy(alpha = 0.85f) else Chalk.copy(alpha = 0.32f),
                    start = at(a, tickOuter),
                    end = at(a, tickOuter - length),
                    strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                if (db % 20 == 0) {
                    val layout = measurer.measure(db.toString(), numeral)
                    val p = at(a, tickOuter - 34.dp.toPx())
                    drawText(layout, topLeft = Offset(p.x - layout.size.width / 2f, p.y - layout.size.height / 2f))
                }
                db += 2
            }

            // the city's limit: one red mark
            if (limitDb != null && limitDb in 20.0..100.0) {
                val a = angle(limitDb.toFloat())
                drawLine(Danger, at(a, tickOuter + 6.dp.toPx()), at(a, tickOuter - 22.dp.toPx()), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            }

            // the sound: a brass sweep with its own light
            if (valueDb != null) {
                val sweepRadius = r - 58.dp.toPx()
                val sweepColor = if (over) Danger else Brass
                val box = Offset(c.x - sweepRadius, c.y - sweepRadius)
                val boxSize = Size(sweepRadius * 2, sweepRadius * 2)
                drawArc(Chalk.copy(alpha = 0.07f), 135f, 270f, false, box, boxSize, style = Stroke(9.dp.toPx(), cap = StrokeCap.Round))
                val sweep = (shown - 20f) / 80f * 270f
                drawArc(sweepColor.copy(alpha = 0.22f), 135f, sweep, false, box, boxSize, style = Stroke(22.dp.toPx(), cap = StrokeCap.Round))
                drawArc(sweepColor, 135f, sweep, false, box, boxSize, style = Stroke(9.dp.toPx(), cap = StrokeCap.Round))
                drawCircle(Color.White, radius = 3.dp.toPx(), center = at(135f + sweep, sweepRadius))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, content = center)
    }
}

/** The record button: a red key in a brass ring. It sinks when pressed. */
@Composable
fun RecordButton(onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 72.dp, label: String = "Start recording") {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val sink by animateFloatAsState(if (pressed) 1f else 0f, label = "RecordSink")
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = 1f - 0.04f * sink; scaleY = 1f - 0.04f * sink }
            .shadow((12 - 8 * sink).dp, CircleShape, clip = false)
            .clip(CircleShape)
            .drawBehind {
                val r = this.size.minDimension / 2f
                val c = Offset(this.size.width / 2f, this.size.height / 2f)
                drawCircle(
                    Brush.linearGradient(
                        listOf(Color(0xFFFFE3AA), Brass, BrassDeep, Color(0xFF6A4410)),
                        start = Offset(0f, 0f),
                        end = Offset(this.size.width, this.size.height),
                    ),
                    radius = r,
                    center = c,
                )
                drawCircle(Color(0xFF05080F), radius = r * 0.84f, center = c)
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color(0xFFFF8A8D), RecordRed, RecordRedDeep),
                        center = c + Offset(-r * 0.22f, -r * (0.30f - 0.18f * sink)),
                        radius = r * 1.05f,
                    ),
                    radius = r * (0.74f - 0.02f * sink),
                    center = c,
                )
                drawCircle(Color.Black.copy(alpha = 0.18f * sink), radius = r * 0.74f, center = c)
            }
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
    )
}

/** The five ways around the app. */
data class NavActions(
    val home: () -> Unit,
    val incidents: () -> Unit,
    val record: () -> Unit,
    val rules: () -> Unit,
    val more: () -> Unit,
)

/** The strip at the bottom: four places and, in the middle, the record button, always under the thumb. */
@Composable
fun BottomStrip(selected: AppScreen, nav: NavActions) {
    Box(Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 26.dp)
                .shadow(18.dp, clip = false),
            color = Deck,
            contentColor = Chalk,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(Hairline, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
                        drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.05f), Color.Transparent), endY = 40.dp.toPx()))
                    }
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .height(66.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StripItem(Icons.Default.Home, "Home", selected == AppScreen.HOME, nav.home, Modifier.weight(1f))
                StripItem(Icons.Default.Folder, "Incidents", selected == AppScreen.HISTORY, nav.incidents, Modifier.weight(1f))
                Box(Modifier.weight(1f).height(66.dp), contentAlignment = Alignment.BottomCenter) {
                    Text("Record", style = MaterialTheme.typography.labelSmall, color = Chalk, modifier = Modifier.padding(bottom = 9.dp))
                }
                StripItem(Icons.Default.Gavel, "Rules", selected == AppScreen.RULES, nav.rules, Modifier.weight(1f))
                StripItem(Icons.Default.MoreHoriz, "More", selected == AppScreen.MORE, nav.more, Modifier.weight(1f))
            }
        }
        RecordButton(onClick = nav.record, modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun StripItem(icon: ImageVector, label: String, chosen: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = if (chosen) Brass else Muted
    Column(
        modifier = modifier
            .height(66.dp)
            .clickable(role = Role.Tab, onClick = onClick)
            .drawBehind {
                if (chosen) {
                    val w = 26.dp.toPx()
                    drawLine(Brass, Offset((size.width - w) / 2f, 1.dp.toPx()), Offset((size.width + w) / 2f, 1.dp.toPx()), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(25.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, textAlign = TextAlign.Center, maxLines = 1)
    }
}
