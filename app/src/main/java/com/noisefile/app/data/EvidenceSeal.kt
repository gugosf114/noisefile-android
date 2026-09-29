package com.noisefile.app.data

import com.noisefile.app.model.Incident
import java.security.MessageDigest
import java.util.Locale

/**
 * The evidence seal. Every saved take gets a SHA-256 over the facts the phone
 * measured (when, how long, the levels, the baseline, the microphone note, the
 * second-by-second trace) plus the seal of the take saved before it. Change a
 * number, delete a take, or reorder them, and the chain no longer verifies.
 * The user's own words (location, impact, notes) stay outside the seal on
 * purpose: they are declared, not measured, and may be edited later.
 */
object EvidenceSeal {
    const val GENESIS = "GENESIS"

    /**
     * The first seal recipe (0.12.x), before photos and clips existed. Seals made then must keep
     * verifying, so a recipe is never changed in place: a new one is added and the old one stays.
     */
    fun canonicalV1(incident: Incident, previousHash: String): String = listOf(
        incident.id.toString(),
        incident.ruleId,
        incident.noiseType.name,
        incident.startedAtEpochMillis.toString(),
        incident.durationSeconds.toString(),
        fmt(incident.minimumDb),
        fmt(incident.averageDb),
        fmt(incident.maximumDb),
        incident.ambientDb?.let(::fmt) ?: "",
        incident.ambientSeconds?.toString() ?: "",
        incident.levelNote ?: "",
        incident.traceSecondsPerSample.toString(),
        incident.levelTrace.joinToString(","),
        previousHash,
    ).joinToString("|")

    /** True when [incident]'s own seal matches its facts under any recipe this app has ever used. */
    fun matches(incident: Incident, previousHash: String): Boolean {
        val own = incident.evidenceHash ?: return false
        if (hash(incident, previousHash) == own) return true
        val noFiles = incident.photoHashes.isEmpty() && incident.clipHash == null
        return noFiles && sha256(canonicalV1(incident, previousHash)) == own
    }

    fun canonical(incident: Incident, previousHash: String): String = listOf(
        incident.id.toString(),
        incident.ruleId,
        incident.noiseType.name,
        incident.startedAtEpochMillis.toString(),
        incident.durationSeconds.toString(),
        fmt(incident.minimumDb),
        fmt(incident.averageDb),
        fmt(incident.maximumDb),
        incident.ambientDb?.let(::fmt) ?: "",
        incident.ambientSeconds?.toString() ?: "",
        incident.levelNote ?: "",
        incident.traceSecondsPerSample.toString(),
        incident.levelTrace.joinToString(","),
        incident.photoHashes.joinToString(","),
        incident.clipHash ?: "",
        incident.clipSeconds.toString(),
        previousHash,
    ).joinToString("|")

    fun hash(incident: Incident, previousHash: String): String =
        sha256(canonical(incident, previousHash))

    /** Seal a fresh take against the newest sealed take (or GENESIS). */
    fun seal(incident: Incident, newest: Incident?): Incident {
        val previous = newest?.evidenceHash ?: GENESIS
        return incident.copy(previousHash = previous, evidenceHash = hash(incident, previous))
    }

    data class Report(
        val sealedCount: Int,
        val unsealedCount: Int,
        val brokenAtId: Long?,
    ) {
        val intact: Boolean get() = brokenAtId == null
    }

    /** Walk the chain oldest to newest. Takes saved before sealing existed are counted, not judged. */
    fun verify(incidents: List<Incident>): Report {
        val ordered = incidents.sortedBy { it.id }
        var expectedPrevious = GENESIS
        var sealed = 0
        var unsealed = 0
        for (incident in ordered) {
            val own = incident.evidenceHash
            if (own == null) { unsealed += 1; continue }
            val previous = incident.previousHash ?: GENESIS
            if (previous != expectedPrevious) return Report(sealed, unsealed, incident.id)
            if (!matches(incident, previous)) return Report(sealed, unsealed, incident.id)
            sealed += 1
            expectedPrevious = own
        }
        return Report(sealed, unsealed, null)
    }

    fun short(hash: String?): String = hash?.take(16) ?: "unsealed"

    private fun fmt(value: Double) = String.format(Locale.US, "%.2f", value)

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { String.format(Locale.US, "%02x", it) }
}
