package com.noisefile.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.noisefile.app.data.Rooms
import com.noisefile.app.model.RuleWorkflow
import com.noisefile.app.ui.theme.Chalk
import com.noisefile.app.ui.theme.Cobalt
import com.noisefile.app.ui.theme.DeckHigh
import com.noisefile.app.ui.theme.Hairline
import com.noisefile.app.ui.theme.Ink
import com.noisefile.app.ui.theme.Line
import com.noisefile.app.ui.theme.Paper
import com.noisefile.app.ui.theme.White

/** Where the person stands: the common rooms, plus Other with a typed name. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RoomChips(selected: String?, onSelect: (String) -> Unit, onPaper: Boolean = false) {
    val isOther = selected != null && Rooms.common.none { it.equals(selected, ignoreCase = true) }
    var typing by remember(selected) { mutableStateOf(isOther) }
    var otherName by remember { mutableStateOf(if (isOther) selected.orEmpty() else "") }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            (Rooms.common + Rooms.OTHER).forEach { room ->
                val picked = if (room == Rooms.OTHER) isOther else room.equals(selected, ignoreCase = true)
                FilterChip(
                    selected = picked,
                    onClick = { if (room == Rooms.OTHER) typing = true else { typing = false; onSelect(room) } },
                    label = { Text(if (room == Rooms.OTHER && isOther) selected.orEmpty() else room) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = if (onPaper) Paper else DeckHigh,
                        labelColor = if (onPaper) Ink else Chalk,
                        selectedContainerColor = Cobalt,
                        selectedLabelColor = White,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true, selected = picked,
                        borderColor = if (onPaper) Line else Hairline, selectedBorderColor = Cobalt,
                    ),
                )
            }
        }
        if (typing) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = otherName,
                onValueChange = { otherName = it; if (it.isNotBlank()) onSelect(it.trim()) },
                label = { Text("Name the room or spot") },
                placeholder = { Text("Example: Garage, Office window") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )
        }
    }
}

/** The one line that says whether the city's code asks for a baseline, and for how long. */
fun baselineRuleLine(rule: RuleWorkflow, defaultMinutes: Int): String {
    val city = rule.jurisdiction.substringBefore(",")
    val recipe = rule.ambientRecipe
    return if (recipe != null) {
        "$city's code measures the quiet for ${recipe.minutes} minute${if (recipe.minutes == 1) "" else "s"} and judges noise by the jump above it."
    } else {
        val limit = rule.meterLimit
        val how = when {
            limit?.fixedMaximumDb != null -> "sets a flat limit of ${limit.fixedMaximumDb.toInt()} dB"
            limit != null -> "sets day and night limits"
            rule.hoursRule != null -> "sets hours"
            else -> "sets no number"
        }
        "$city's code $how and does not ask for a baseline. One still shows the officer how far above normal the noise was. Our default is $defaultMinutes minutes."
    }
}

/** The words after the minutes on the quiet button: whose number it is. */
fun baselineMinutesSource(rule: RuleWorkflow): String {
    val city = rule.jurisdiction.substringBefore(",")
    return if (rule.ambientRecipe != null) "$city's code" else "not in $city's code; our default"
}
