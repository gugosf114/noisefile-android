package com.noisefile.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noisefile.app.AppScreen
import com.noisefile.app.data.FormAnswerRow
import com.noisefile.app.data.complaintDestination
import com.noisefile.app.data.formAnswers
import com.noisefile.app.model.Incident
import com.noisefile.app.model.RuleWorkflow
import com.noisefile.app.ui.theme.BarlowCondensed
import com.noisefile.app.ui.theme.Brass
import com.noisefile.app.ui.theme.Chalk
import com.noisefile.app.ui.theme.Cobalt
import com.noisefile.app.ui.theme.DeckHigh
import com.noisefile.app.ui.theme.Hairline
import com.noisefile.app.ui.theme.Ink
import com.noisefile.app.ui.theme.Line
import com.noisefile.app.ui.theme.Muted
import com.noisefile.app.ui.theme.PaperAmber
import com.noisefile.app.ui.theme.PaperBlue
import com.noisefile.app.ui.theme.PaperMuted
import com.noisefile.app.ui.theme.White

/**
 * The city's form, box by box, with the app's answer next to each box.
 * Every answer has a copy button; the form itself opens in the browser.
 */
@Composable
internal fun FormGuideScreen(
    incident: Incident,
    rule: RuleWorkflow,
    nav: NavActions,
    onBack: () -> Unit,
    onOpenForm: () -> Unit,
) {
    val guide = checkNotNull(rule.formGuide)
    val context = LocalContext.current
    val rows = remember(incident, rule) { formAnswers(incident, rule, guide) }
    val destination = complaintDestination(rule)
    val copy: (String, String) -> Unit = { label, text ->
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("NoiseFile $label", text))
        Toast.makeText(context, "$label copied.", Toast.LENGTH_SHORT).show()
    }

    AppScaffold(selectedScreen = AppScreen.HISTORY, nav = nav) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Chalk)
                    }
                    Column {
                        Text(
                            text = "File it with ${guide.portal}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = BarlowCondensed,
                            color = Chalk,
                        )
                        Text(guide.access, style = MaterialTheme.typography.bodyMedium, color = Muted)
                    }
                }
            }

            item {
                DeckCard {
                    Label("The form's screens, in order")
                    guide.steps.forEachIndexed { index, step ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(DeckHigh, CircleShape)
                                    .border(1.dp, Hairline, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Brass,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(step, style = MaterialTheme.typography.bodyLarge, color = Chalk)
                        }
                    }
                    if (guide.tips.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        guide.tips.forEach { tip ->
                            Text(tip, style = MaterialTheme.typography.bodyMedium, color = Muted)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Each box on the form, and what to put in it",
                    style = MaterialTheme.typography.titleMedium,
                    color = Chalk,
                )
            }

            itemsIndexed(rows) { _, row ->
                AnswerCard(row = row, onCopy = { copy(row.label, it) })
            }

            item {
                Spacer(Modifier.height(4.dp))
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp),
                    onClick = onOpenForm,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Cobalt, contentColor = White),
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = if (destination.isDocumentPacket) "Open the city packet" else "Open the city form",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    onClick = onBack,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("Back to the incident", color = Chalk)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "The description is on your clipboard already. Come back here for the rest; the app stays open behind the browser.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
        }
    }
}

@Composable
private fun AnswerCard(row: FormAnswerRow, onCopy: (String) -> Unit) {
    PaperCard(edge = if (row.required) PaperAmber.copy(alpha = 0.45f) else Line) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = row.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = PaperMuted,
                    )
                }
                if (row.required || row.maxLength != null) {
                    Text(
                        text = listOfNotNull(
                            if (row.required) "Required" else null,
                            row.maxLength?.let { "up to $it characters" },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (row.required) PaperAmber else PaperMuted,
                    )
                }
                Spacer(Modifier.height(6.dp))
                when {
                    row.text == null -> Text(
                        text = row.hint ?: "Type it in.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PaperMuted,
                    )
                    row.isChoice -> Text(
                        text = "Pick: ${row.text}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = PaperBlue,
                        fontWeight = FontWeight.SemiBold,
                    )
                    else -> Text(
                        text = row.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink,
                    )
                }
                if (row.text != null && row.hint != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(row.hint, style = MaterialTheme.typography.bodySmall, color = PaperMuted)
                }
            }
            if (row.text != null && !row.isChoice) {
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { onCopy(row.text) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy ${row.label}", tint = PaperBlue)
                }
            }
        }
    }
}
