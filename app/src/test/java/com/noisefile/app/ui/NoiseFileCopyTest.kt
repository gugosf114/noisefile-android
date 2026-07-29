package com.noisefile.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NoiseFileCopyTest {
    @Test
    fun interfaceDoesNotDeclareViolationsFromAGenericDecibelCutoff() {
        val source = sourceFile().readText()

        assertFalse(source.contains("typical residential ordinance thresholds"))
        assertFalse(source.contains("Clear violation in most zones"))
        assertFalse(source.contains("avg >= 55"))
        assertFalse(source.contains("legally admissible", ignoreCase = true))
        assertTrue(source.contains("MORE EVIDENCE NEEDED"))
        assertTrue(source.contains("City enforcement uses the required equipment"))
    }

    private fun sourceFile(): File {
        val relativePath = "app/src/main/java/com/noisefile/app/ui/NoiseFileApp.kt"
        return listOf(
            File(relativePath),
            File("../$relativePath"),
        ).firstOrNull { it.isFile }
            ?: error("Could not find $relativePath")
    }
}
