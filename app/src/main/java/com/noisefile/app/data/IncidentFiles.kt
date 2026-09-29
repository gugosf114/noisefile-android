package com.noisefile.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.security.MessageDigest
import java.util.Locale

/**
 * The files that ride with an incident: a photo or two and the loudest-moment clip.
 * They live in the app's own private folder, never in the public gallery, never online.
 * Draft files wait in cache until the incident is saved, then move under files/incidents/<id>/.
 */
class IncidentFiles(private val context: Context) {
    private val draftDir get() = File(context.cacheDir, "draft").apply { mkdirs() }
    private fun incidentDir(id: Long) = File(context.filesDir, "incidents/$id").apply { mkdirs() }

    /** Copy a picked photo into the draft folder, shrunk to at most [maxSide] px, JPEG. Returns the file. */
    fun stageDraftPhoto(uri: Uri, index: Int, maxSide: Int = 1_600): File? {
        val resolver = context.contentResolver
        // The bounds pass returns no bitmap by design; only its outWidth/outHeight matter.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > maxSide * 2 || bounds.outHeight / sample > maxSide * 2) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        val scale = minOf(1f, maxSide.toFloat() / maxOf(bitmap.width, bitmap.height))
        val scaled = if (scale < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true) else bitmap
        val file = File(draftDir, "photo-$index.jpg")
        file.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        return file
    }

    fun stageDraftClip(wavBytes: ByteArray): File = File(draftDir, "clip.wav").also { it.writeBytes(wavBytes) }

    fun clearDraft() { draftDir.listFiles()?.forEach { it.delete() } }

    /** Move the draft files under the saved incident and return their final names and hashes. */
    fun commit(incidentId: Long, photos: List<File>, clip: File?): Committed {
        val dir = incidentDir(incidentId)
        val photoNames = photos.mapIndexed { i, f ->
            val target = File(dir, "photo-${i + 1}.jpg"); f.copyTo(target, overwrite = true); f.delete(); target.name
        }
        val clipName = clip?.let { val target = File(dir, "clip.wav"); it.copyTo(target, overwrite = true); it.delete(); target.name }
        return Committed(
            photoNames = photoNames,
            photoHashes = photoNames.map { sha256(File(dir, it)) },
            clipName = clipName,
            clipHash = clipName?.let { sha256(File(dir, it)) },
        )
    }

    fun file(incidentId: Long, name: String): File = File(incidentDir(incidentId), name)

    /** Re-hash every file an incident points to; false if any is missing or changed. */
    fun filesIntact(incidentId: Long, photoNames: List<String>, photoHashes: List<String>, clipName: String?, clipHash: String?): Boolean {
        photoNames.forEachIndexed { i, n -> val f = file(incidentId, n); if (!f.isFile || sha256(f) != photoHashes.getOrNull(i)) return false }
        if (clipName != null) { val f = file(incidentId, clipName); if (!f.isFile || sha256(f) != clipHash) return false }
        return true
    }

    data class Committed(val photoNames: List<String>, val photoHashes: List<String>, val clipName: String?, val clipHash: String?)

    companion object {
        fun sha256(file: File): String {
            val md = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { s -> val buf = ByteArray(65_536); while (true) { val n = s.read(buf); if (n < 0) break; md.update(buf, 0, n) } }
            return md.digest().joinToString("") { String.format(Locale.US, "%02x", it) }
        }
    }
}
