package com.noisefile.app.data

import android.content.Context
import android.graphics.BitmapFactory
import com.noisefile.app.model.Incident
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Draws a [ReportPlan] as a Letter-size PDF and returns the file (in the app's private cache). */
fun writeIncidentPdf(context: Context, plan: ReportPlan, fileFor: (Incident, String) -> File = { _, n -> File(n) }): File {
    val dir = File(context.cacheDir, "reports").apply { mkdirs() }
    val name = "NoiseFile-Report-" + DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US).format(LocalDate.now()) + ".pdf"
    val file = File(dir, name)
    val doc = PdfDocument()
    val painter = ReportPainter(doc, fileFor)
    painter.cover(plan)
    painter.summary(plan)
    painter.pattern(plan)
    plan.exhibits.forEach { painter.exhibit(it, plan) }
    painter.sourcesAndDeclaration(plan)
    painter.finish()
    file.outputStream().use { doc.writeTo(it) }
    doc.close()
    return file
}

private class ReportPainter(private val doc: PdfDocument, private val fileFor: (Incident, String) -> File) {
    private val width = 612
    private val height = 792
    private val margin = 48f
    private val bottom = height - 56f
    private var page: PdfDocument.Page? = null
    private var canvas: Canvas? = null
    private var y = 0f
    private var pageNumber = 0

    private val ink = Color.rgb(23, 32, 51)
    private val muted = Color.rgb(108, 116, 133)
    private val cobalt = Color.rgb(65, 105, 225)
    private val danger = Color.rgb(196, 48, 48)
    private val amber = Color.rgb(217, 140, 0)
    private val line = Color.rgb(214, 210, 200)

    private fun paint(size: Float, color: Int = ink, bold: Boolean = false, italic: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = Typeface.create(Typeface.SANS_SERIF, when {
            bold && italic -> Typeface.BOLD_ITALIC
            bold -> Typeface.BOLD
            italic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        })
    }

    private fun newPage() {
        page?.let { doc.finishPage(it) }
        pageNumber += 1
        val info = PdfDocument.PageInfo.Builder(width, height, pageNumber).create()
        page = doc.startPage(info)
        canvas = page!!.canvas
        y = margin
        // footer
        val f = paint(8f, muted)
        canvas!!.drawText("NoiseFile · phone estimates, not a certified meter · not a government document", margin, height - 28f, f)
        canvas!!.drawText("Page $pageNumber", width - margin - f.measureText("Page $pageNumber"), height - 28f, f)
    }

    private fun need(space: Float) { if (page == null || y + space > bottom) newPage() }

    private fun gap(px: Float) { y += px }

    private fun rule() { need(8f); canvas!!.drawLine(margin, y, width - margin, y, Paint().apply { color = line; strokeWidth = 0.8f }); y += 10f }

    private fun text(s: String, size: Float = 10.5f, color: Int = ink, bold: Boolean = false, italic: Boolean = false, indent: Float = 0f, lineGap: Float = 1.35f) {
        val p = paint(size, color, bold, italic)
        val maxWidth = width - 2 * margin - indent
        wrap(s, p, maxWidth).forEach { l ->
            need(size * lineGap)
            canvas!!.drawText(l, margin + indent, y + size, p)
            y += size * lineGap
        }
    }

    private fun wrap(s: String, p: Paint, maxWidth: Float): List<String> {
        val out = ArrayList<String>()
        s.split('\n').forEach { para ->
            var current = StringBuilder()
            para.split(' ').forEach { word ->
                val trial = if (current.isEmpty()) word else "$current $word"
                if (p.measureText(trial) <= maxWidth || current.isEmpty()) current = StringBuilder(trial)
                else { out.add(current.toString()); current = StringBuilder(word) }
            }
            out.add(current.toString())
        }
        return out
    }

    private fun label(s: String) { text(s.uppercase(Locale.US), 8.5f, cobalt, bold = true); gap(1f) }

    private fun heightOf(s: String, size: Float, indent: Float = 0f, lineGap: Float = 1.35f): Float =
        wrap(s, paint(size), width - 2 * margin - indent).size * size * lineGap

    /** Everything on an exhibit page below the chart, at a given body size. */
    private fun exhibitTailHeight(e: ReportExhibit, body: Float): Float {
        var h = 0f
        e.assessment?.let { a ->
            h += 14f + heightOf(a.headline, body + 2.5f) + 2f + heightOf(a.detail, body) + 4f
            a.conditions.forEach { cnd ->
                h += heightOf("• ${cnd.text}", body, indent = 4f) + 2f
                if (cnd.sourceQuote != null && cnd.sourceCitation != null) {
                    h += heightOf("The code says · ${cnd.sourceCitation}", body - 1.5f, indent = 16f) +
                        heightOf("“${cnd.sourceQuote}”", body - 1f, indent = 16f) + 4f
                }
            }
        } ?: run { h += heightOf("No city rule on file for this take.", body) }
        h += personBlockHeight(e, body)
        return h
    }

    private fun personBlockHeight(e: ReportExhibit, body: Float): Float {
        val inc = e.incident
        var h = 6f + 10f + 14f
        h += heightOf("Location: ${inc.location.ifBlank { "not added" }}", body + 0.5f)
        h += heightOf("Impact: ${inc.impact}", body + 0.5f)
        h += heightOf("Notes: ${inc.notes.ifBlank { "none" }}", body + 0.5f)
        inc.levelNote?.let { h += 2f + heightOf(it, body - 0.5f) }
        if (inc.photoNames.isNotEmpty()) h += 6f + 14f + 150f + 6f
        h += 6f + 10f + 14f + heightOf("SHA-256 ${inc.evidenceHash ?: "unsealed (saved before sealing existed)"}", 8f) + 12f
        h += (inc.photoNames.size + (if (inc.clipName != null) 1 else 0)) * 11f
        return h
    }

    fun cover(plan: ReportPlan) {
        newPage()
        gap(120f)
        text(plan.title, 26f, bold = true); gap(6f)
        text("Incident log with the city's own rule applied to each take", 12f, muted); gap(28f)
        val rows = listOf(
            "Prepared on" to plan.preparedOn,
            "City" to (plan.cities.joinToString(", ").ifBlank { "—" }),
            "Period" to plan.dateRange.ifBlank { "—" },
            "Takes" to plan.exhibits.size.toString(),
            "Evidence seal" to sealWords(plan.seal),
        )
        rows.forEach { (k, v) ->
            need(18f)
            canvas!!.drawText(k, margin, y + 11f, paint(10.5f, muted, bold = true))
            text(v, 11f, indent = 120f); gap(2f)
        }
        gap(30f); rule()
        label("Prepared by")
        text("Name: ______________________________________     Phone / email: ______________________________", 10.5f); gap(6f)
        text("Address where the noise was heard: _______________________________________________________", 10.5f)
        gap(30f)
        text("How to read this report", 12f, bold = true); gap(4f)
        text("Each take is one recording made on a phone with NoiseFile. The phone's sound level is an estimate; a city officer's calibrated meter at the code's measurement point decides. NoiseFile compares each take to the city's published rule and prints the rule's own words. Where the code lists a decibel limit, hours, or a required count, the take is checked against it; where it does not, the report says what the city still needs.", 10f, muted)
        gap(10f)
        text("NoiseFile is an independent app by WiM Labs. It is not affiliated with, endorsed by, or representing any city, county, or government agency.", 9f, muted, italic = true)
    }

    private fun sealWords(seal: EvidenceSeal.Report): String = when {
        seal.sealedCount == 0 && seal.unsealedCount > 0 -> "not sealed (saved before sealing existed)"
        seal.intact && seal.unsealedCount == 0 -> "intact · ${seal.sealedCount} sealed takes"
        seal.intact -> "intact · ${seal.sealedCount} sealed, ${seal.unsealedCount} earlier unsealed"
        else -> "BROKEN at take id ${seal.brokenAtId} · edited, removed or reordered after saving"
    }

    fun summary(plan: ReportPlan) {
        newPage()
        text("Summary of takes", 16f, bold = true); gap(6f)
        val counts = plan.statusCounts
        val reached = counts[MeterAssessmentStatus.REACHES_LISTED_CONDITION] ?: 0
        text("${plan.exhibits.size} takes · $reached reached a listed condition · ${counts[MeterAssessmentStatus.DOES_NOT_REACH_LISTED_CONDITION] ?: 0} did not · ${counts[MeterAssessmentStatus.NEEDS_INFORMATION] ?: 0} need the city's own test", 10f, muted)
        gap(10f)
        val cols = floatArrayOf(0f, 28f, 190f, 300f, 350f, 410f)
        val head = listOf("#", "When", "Type", "Length", "Avg / Max", "City check")
        need(16f)
        head.forEachIndexed { i, h -> canvas!!.drawText(h, margin + cols[i], y + 9f, paint(8.5f, cobalt, bold = true)) }
        y += 14f; rule()
        plan.exhibits.forEach { e ->
            need(16f)
            val p = paint(9.5f)
            val status = e.statusWord()
            val statusColor = when (e.assessment?.status) {
                MeterAssessmentStatus.REACHES_LISTED_CONDITION -> danger
                MeterAssessmentStatus.DOES_NOT_REACH_LISTED_CONDITION -> amber
                else -> muted
            }
            val c = canvas!!
            c.drawText(e.number.toString(), margin + cols[0], y + 9f, p)
            c.drawText(e.whenLabel, margin + cols[1], y + 9f, p)
            c.drawText(e.incident.noiseType.displayName.substringBefore(" (").take(18), margin + cols[2], y + 9f, p)
            c.drawText(lengthWords(e.incident.durationSeconds), margin + cols[3], y + 9f, p)
            c.drawText("${e.incident.averageDb.roundToInt()} / ${e.incident.maximumDb.roundToInt()} dB", margin + cols[4], y + 9f, p)
            c.drawText(status, margin + cols[5], y + 9f, paint(9.5f, statusColor, bold = true))
            y += 15f
        }
    }

    private fun lengthWords(seconds: Long): String = if (seconds < 60) "${seconds}s" else "${seconds / 60}m ${seconds % 60}s"

    fun pattern(plan: ReportPlan) {
        if (plan.exhibits.size < 3) return
        newPage()
        text("When it happens", 16f, bold = true); gap(4f)
        plan.patternSentence?.let { text(it, 11f); gap(8f) }
        text("Takes by day of week and hour of day (darker = more takes)", 9f, muted); gap(8f)
        val cell = 19f; val rowH = 18f; val left = margin + 34f
        val maxCount = plan.patternGrid.maxOf { it.max() }.coerceAtLeast(1)
        need(rowH * 8 + 20f)
        val c = canvas!!
        val hp = paint(7f, muted)
        for (h in 0 until 24 step 3) c.drawText(IncidentPatterns.hourLabel(h), left + h * cell, y + 8f, hp)
        y += 12f
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        for (d in 0 until 7) {
            c.drawText(days[d], margin, y + 12f, paint(8.5f, muted, bold = true))
            for (h in 0 until 24) {
                val n = plan.patternGrid[d][h]
                val shade = if (n == 0) 245 else (225 - 160 * n / maxCount).coerceIn(40, 225)
                val fill = Paint().apply { color = if (n == 0) Color.rgb(245, 243, 236) else Color.rgb(shade, shade + 10, 255.coerceAtMost(shade + 60)) }
                c.drawRect(RectF(left + h * cell, y + 1f, left + (h + 1) * cell - 1f, y + rowH - 1f), fill)
                if (n > 0) c.drawText(n.toString(), left + h * cell + 6f, y + 12f, paint(7.5f, if (shade < 120) Color.WHITE else ink, bold = true))
            }
            y += rowH
        }
        gap(10f)
    }

    fun exhibit(e: ReportExhibit, plan: ReportPlan) {
        newPage()
        val inc = e.incident
        // Header + facts + chart take about 250 pt; shrink the body type until the rest fits the page, down to 8 pt.
        val available = bottom - margin - 250f
        var body = 9.5f
        while (body > 8f && exhibitTailHeight(e, body) > available) body -= 0.5f
        label("Exhibit ${e.number} of ${plan.exhibits.size}")
        text("${inc.noiseType.displayName} · ${e.rule?.jurisdiction?.substringBefore(",") ?: "city not on file"}", 15f, bold = true); gap(2f)
        text(e.whenLabel, 11f, muted); gap(8f)
        rule()
        // facts row
        val facts = listOf(
            "Length" to lengthWords(inc.durationSeconds),
            "Highest" to "${inc.maximumDb.roundToInt()} dB",
            "Average" to "${inc.averageDb.roundToInt()} dB",
            "Lowest" to "${inc.minimumDb.roundToInt()} dB",
            "Quiet baseline" to (inc.ambientDb?.let { "${it.roundToInt()} dB over ${lengthWords(inc.ambientSeconds ?: 0L)}" } ?: "not taken"),
        )
        need(34f)
        var x = margin
        facts.forEach { (k, v) ->
            canvas!!.drawText(k.uppercase(Locale.US), x, y + 8f, paint(7.5f, muted, bold = true))
            canvas!!.drawText(v, x, y + 24f, paint(12f, bold = true))
            x += 103f
        }
        y += 36f
        chart(inc, e.limitDbAtThatTime)
        gap(6f)
        // the city's check
        e.assessment?.let { a ->
            val headColor = when (a.status) {
                MeterAssessmentStatus.REACHES_LISTED_CONDITION -> danger
                MeterAssessmentStatus.DOES_NOT_REACH_LISTED_CONDITION -> amber
                else -> cobalt
            }
            label("City check · ${e.statusWord()}")
            text(a.headline, body + 2.5f, headColor, bold = true); gap(2f)
            text(a.detail, body, muted); gap(4f)
            a.conditions.forEach { cnd ->
                val marker = when (cnd.outcome) {
                    RuleConditionOutcome.REACHED -> "■"
                    RuleConditionOutcome.NOT_REACHED -> "□"
                    RuleConditionOutcome.NEEDS_INFORMATION -> "•"
                }
                val color = when (cnd.outcome) {
                    RuleConditionOutcome.REACHED -> danger
                    RuleConditionOutcome.NOT_REACHED -> amber
                    RuleConditionOutcome.NEEDS_INFORMATION -> ink
                }
                text("$marker ${cnd.text}", body, color, indent = 4f); gap(2f)
                if (cnd.sourceQuote != null && cnd.sourceCitation != null) {
                    text("The code says · ${cnd.sourceCitation}", body - 1.5f, muted, bold = true, indent = 16f)
                    text("“${cnd.sourceQuote}”", body - 1f, muted, italic = true, indent = 16f); gap(4f)
                }
            }
        } ?: run { text("No city rule on file for this take.", body, muted) }
        // the person's words and the seal stay together: never three orphan lines on a page of their own
        need(personBlockHeight(e, body))
        gap(6f); rule()
        label("What the person recorded")
        text("Location: ${inc.location.ifBlank { "not added" }}", body + 0.5f)
        text("Impact: ${inc.impact}", body + 0.5f)
        text("Notes: ${inc.notes.ifBlank { "none" }}", body + 0.5f)
        inc.levelNote?.let { gap(2f); text(it, body - 0.5f, muted, italic = true) }
        if (inc.photoNames.isNotEmpty()) {
            gap(6f); label("Photos")
            need(150f)
            var px = margin
            inc.photoNames.forEach { name ->
                val f = fileFor(inc, name)
                val bmp = runCatching { BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = 2 }) }.getOrNull()
                if (bmp != null) {
                    val boxW = 240f; val boxH = 150f
                    val scale = minOf(boxW / bmp.width, boxH / bmp.height)
                    val w = bmp.width * scale; val hh = bmp.height * scale
                    canvas!!.drawBitmap(bmp, null, RectF(px, y, px + w, y + hh), Paint(Paint.FILTER_BITMAP_FLAG))
                    px += boxW + 16f
                }
            }
            y += 150f + 6f
        }
        gap(6f); rule()
        label("Evidence seal")
        text("SHA-256 ${inc.evidenceHash ?: "unsealed (saved before sealing existed)"}", 8f, muted)
        inc.previousHash?.let { text("links to previous ${EvidenceSeal.short(it)}", 8f, muted) }
        inc.photoNames.forEachIndexed { i, n -> text("Photo ${i + 1}: $n · SHA-256 ${inc.photoHashes.getOrNull(i) ?: "?"}", 7.5f, muted) }
        inc.clipName?.let { text("Sound clip: $it · ${inc.clipSeconds} s, loudest moment, kept on the phone · SHA-256 ${inc.clipHash ?: "?"}", 7.5f, muted) }
    }

    private fun chart(inc: com.noisefile.app.model.Incident, limit: Double?) {
        val trace = inc.levelTrace
        val boxH = 130f
        need(boxH + 24f)
        val c = canvas!!
        val left = margin + 26f; val right = width - margin
        val top = y; val bot = y + boxH
        c.drawRect(RectF(left, top, right, bot), Paint().apply { color = Color.rgb(250, 249, 245) })
        val lo = 20.0; val hi = 100.0
        fun yFor(db: Double) = (bot - ((db - lo) / (hi - lo)).coerceIn(0.0, 1.0) * boxH).toFloat()
        for (g in listOf(20, 40, 60, 80, 100)) {
            c.drawLine(left, yFor(g.toDouble()), right, yFor(g.toDouble()), Paint().apply { color = line; strokeWidth = 0.5f })
            c.drawText("$g", margin, yFor(g.toDouble()) + 3f, paint(7f, muted))
        }
        if (trace.size >= 2) {
            val path = Path()
            val stepX = (right - left) / (trace.size - 1).toFloat()
            trace.forEachIndexed { i, v -> val px = left + i * stepX; val py = yFor(v.toDouble()); if (i == 0) path.moveTo(px, py) else path.lineTo(px, py) }
            c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cobalt; style = Paint.Style.STROKE; strokeWidth = 1.4f })
        } else {
            // older takes have no trace: show min / avg / max as bars
            val p = Paint().apply { color = cobalt }
            listOf(inc.minimumDb, inc.averageDb, inc.maximumDb).forEachIndexed { i, v ->
                val bx = left + 40f + i * 120f
                c.drawRect(RectF(bx, yFor(v), bx + 60f, bot), p)
                c.drawText(listOf("lowest", "average", "highest")[i], bx, bot + 10f, paint(7.5f, muted))
            }
        }
        inc.ambientDb?.let { c.drawLine(left, yFor(it), right, yFor(it), Paint().apply { color = Color.rgb(60, 160, 90); strokeWidth = 1f; pathEffect = DashPathEffect(floatArrayOf(3f, 3f), 0f) }) }
        limit?.let { c.drawLine(left, yFor(it), right, yFor(it), Paint().apply { color = danger; strokeWidth = 1.2f; pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f) }) }
        y = bot + 4f
        val legend = buildString {
            append(if (trace.size >= 2) "Blue: highest estimate each ${if (inc.traceSecondsPerSample == 1) "second" else "${inc.traceSecondsPerSample} seconds"}" else "Bars: lowest, average, highest estimate")
            inc.ambientDb?.let { append(" · green dashes: quiet baseline ${it.roundToInt()} dB") }
            limit?.let { append(" · red dashes: city's listed limit ${it.roundToInt()} dB") }
        }
        text(legend, 7.5f, muted); gap(2f)
    }

    fun sourcesAndDeclaration(plan: ReportPlan) {
        newPage()
        text("Sources", 16f, bold = true); gap(4f)
        text("Every rule in this report is quoted from the city's own published code or page. Links checked on the dates shown.", 9.5f, muted); gap(8f)
        plan.sources.forEach { s ->
            text("${s.city} · ${s.label} · verified ${s.verifiedDate}", 10f, bold = true)
            text(s.url, 8.5f, cobalt, indent = 8f); gap(4f)
        }
        gap(10f); rule()
        text("Method", 13f, bold = true); gap(4f)
        text("Sound levels are A-weighted estimates from a phone microphone, computed by NoiseFile on the device. The phone is not a Type 1 or Type 2 sound level meter; unless the report says an incident was calibrated, the numbers may sit several dB from a certified meter. Where a take includes a quiet baseline, the difference between the noise and the baseline comes from the same phone at the same spot, so the phone's own offset cancels out of that difference. No audio is stored; the second-by-second line is a list of level numbers. Each take is sealed with a SHA-256 hash over its measured facts and the previous take's seal; an intact seal means the numbers, times and order were not changed after saving.", 9.5f, muted)
        gap(14f); rule()
        text("Declaration", 13f, bold = true); gap(6f)
        text("I declare that the takes in this report were recorded by me, at the times shown, at the location stated, and that the notes are my own observations.", 10.5f)
        gap(26f)
        text("Signature: ________________________________________          Date: ____________________", 10.5f)
        gap(18f)
        text("Printed name: _____________________________________", 10.5f)
    }

    fun finish() { page?.let { doc.finishPage(it) }; page = null }
}
