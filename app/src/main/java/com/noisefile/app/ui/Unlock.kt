package com.noisefile.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noisefile.app.billing.UnlockDecision
import com.noisefile.app.billing.UnlockState
import com.noisefile.app.ui.theme.Brass
import com.noisefile.app.ui.theme.Chalk
import com.noisefile.app.ui.theme.Cobalt
import com.noisefile.app.ui.theme.Muted
import com.noisefile.app.ui.theme.White

/** The three things the one-time unlock opens. Same words on the card and in the store text. */
internal val unlockItems = listOf(
    "The sealed PDF report, with a chart and the city check on every incident",
    "The city form guide: every box on the form, with the answer next to it",
    "The city email, already filled in",
)

/**
 * Shown the first time a locked door is tapped. Recording, the rule and the verdict stay free;
 * this is the gate at the moment the person needs to hand something to the city.
 */
@Composable
internal fun UnlockDialog(
    state: UnlockState,
    onBuy: (Activity) -> Unit,
    onRestore: () -> Unit,
    onClose: () -> Unit,
) {
    val activity = androidx.compose.ui.platform.LocalContext.current.findActivity()
    AlertDialog(
        onDismissRequest = onClose,
        icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Brass) },
        title = { Text("Unlock NoiseFile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Checking the rule, recording, and saving incidents stay free. This opens the part you hand to the city.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                unlockItems.forEach { line ->
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Brass, modifier = Modifier.width(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(line, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text(
                    UnlockDecision.priceLine(state.priceText),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "No account with us. Google Play remembers the purchase, so a new phone or a reinstall gets it back with Restore.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
                state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Chalk) }
            }
        },
        confirmButton = {
            Button(
                onClick = { activity?.let(onBuy) },
                enabled = !state.busy && activity != null,
                colors = ButtonDefaults.buttonColors(containerColor = Cobalt, contentColor = White),
            ) {
                Text(if (state.busy) "Opening Google Play…" else state.priceText?.let { "Unlock for $it" } ?: "Unlock")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onRestore, enabled = !state.busy) { Text("Restore") }
                TextButton(onClick = onClose) { Text("Not now") }
            }
        },
    )
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
