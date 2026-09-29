package com.noisefile.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConditionPreviewTest {
    @Test
    fun shortTextShowsWhole() {
        assertNull(conditionPreview("Ordinance test: Daly City bans loud noise at night. That is all."))
    }

    @Test
    fun longTextFoldsAtASentenceEnd() {
        val sentence = "Oakland has no blanket hours ban for ordinary building construction; sound limits and any project Conditions of Approval control. "
        val text = sentence.repeat(12).trim()
        val preview = conditionPreview(text)!!
        assertTrue(preview.length <= 420 + 2)
        assertTrue(preview.endsWith("control. …"))
    }

    @Test
    fun nearlyShortTextDoesNotFoldForAFewWords() {
        val text = "A".repeat(400) + ". " + "B".repeat(30) + "."
        assertNull(conditionPreview(text))
    }

    @Test
    fun previewIsAPrefixOfTheText() {
        val text = ("The city uses the specific requirement below and the phone reading is supporting evidence only. ").repeat(10).trim()
        val preview = conditionPreview(text)!!
        assertEquals(preview.removeSuffix(" …"), text.substring(0, preview.length - 2).trimEnd())
    }
}
