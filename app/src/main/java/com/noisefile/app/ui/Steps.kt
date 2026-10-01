package com.noisefile.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noisefile.app.model.Incident
import com.noisefile.app.ui.theme.BarlowCondensed
import com.noisefile.app.ui.theme.Brass
import com.noisefile.app.ui.theme.Chalk
import com.noisefile.app.ui.theme.Hairline
import com.noisefile.app.ui.theme.Muted
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The state of the newest case: baseline, record, file. Pure, so it can be tested. */
data class CaseSteps(
    val baselineDone: Boolean,
    /** Saved incidents not yet marked filed. */
    val openCount: Int,
    /** The newest incident, when it is marked filed and nothing newer is open. */
    val filedAtEpochMillis: Long?,
) {
    val recordDone: Boolean get() = openCount > 0
    val fileDone: Boolean get() = openCount == 0 && filedAtEpochMillis != null
    /** 0 = baseline, 1 = record, 2 = file; the step the person should do next. */
    val nextIndex: Int get() = when {
        !baselineDone && openCount == 0 -> 0
        openCount == 0 -> 1
        else -> 2
    }

    fun nextLine(zoneId: ZoneId = ZoneId.systemDefault()): String = when {
        openCount > 0 -> if (openCount == 1) "Next: file it with the city, then mark it filed." else "Next: file your $openCount incidents, then mark them filed."
        filedAtEpochMillis != null -> "Filed ${DateTimeFormatter.ofPattern("MMM d", Locale.US).format(Instant.ofEpochMilli(filedAtEpochMillis).atZone(zoneId))}. Ready for the next one."
        !baselineDone -> "Next: measure the quiet in your room, on a quiet night."
        else -> "Next: press Record when the noise starts."
    }

    companion object {
        fun of(baselineDone: Boolean, incidents: List<Incident>): CaseSteps {
            val open = incidents.count { it.filedAtEpochMillis == null }
            val newest = incidents.maxByOrNull { it.startedAtEpochMillis }
            return CaseSteps(baselineDone, open, if (open == 0) newest?.filedAtEpochMillis else null)
        }
    }
}

/**
 * 1 Baseline · 2 Record · 3 File. Each is a button. On open, a brass glow sweeps
 * the three once; after that the next step keeps a faint steady edge.
 */
@Composable
internal fun StepsStrip(
    steps: CaseSteps,
    onBaseline: () -> Unit,
    onRecord: () -> Unit,
    onFile: () -> Unit,
    sweepOnce: Boolean = true,
) {
    val sweep = remember { Animatable(-1f) }
    LaunchedEffect(sweepOnce) {
        if (sweepOnce) {
            sweep.snapTo(0f)
            sweep.animateTo(3f, tween(1_800))
            sweep.snapTo(-1f)
        }
    }
    val sweeping = sweep.value
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val items = listOf(
                Triple("Baseline", steps.baselineDone, onBaseline),
                Triple("Record", steps.recordDone, onRecord),
                Triple("File", steps.fileDone, onFile),
            )
            items.forEachIndexed { i, (name, done, onClick) ->
                val lit = sweeping >= i && sweeping < i + 1f
                val isNext = sweeping < 0f && i == steps.nextIndex && !done
                val edge = when {
                    lit -> Brass
                    isNext -> Brass.copy(alpha = 0.55f)
                    else -> Color.Transparent
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, edge, RoundedCornerShape(12.dp))
                        .clickable(onClick = onClick)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (done) "✓" else "${i + 1}",
                        color = if (done) Brass else Muted,
                        fontFamily = BarlowCondensed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = name + if (i == 1 && steps.openCount > 1) " · ${steps.openCount}" else "",
                        color = if (done || isNext) Chalk else Muted,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
        Text(
            text = steps.nextLine(),
            color = Muted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(0.dp).border(0.dp, Hairline))
    }
}
