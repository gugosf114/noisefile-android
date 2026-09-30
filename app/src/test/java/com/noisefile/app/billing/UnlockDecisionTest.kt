package com.noisefile.app.billing

import com.noisefile.app.billing.UnlockDecision.PurchaseFacts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnlockDecisionTest {
    private val paid = PurchaseFacts(listOf(UnlockDecision.PRODUCT_ID), purchased = true, acknowledged = true)
    private val fresh = PurchaseFacts(listOf(UnlockDecision.PRODUCT_ID), purchased = true, acknowledged = false)
    private val pending = PurchaseFacts(listOf(UnlockDecision.PRODUCT_ID), purchased = false, acknowledged = false)
    private val other = PurchaseFacts(listOf("some_other_thing"), purchased = true, acknowledged = true)

    @Test
    fun onlyAPaidUnlockOpensTheDoor() {
        assertTrue(UnlockDecision.isUnlocked(listOf(paid)))
        assertTrue(UnlockDecision.isUnlocked(listOf(other, fresh)))
        assertFalse(UnlockDecision.isUnlocked(listOf(pending)))
        assertFalse(UnlockDecision.isUnlocked(listOf(other)))
        assertFalse(UnlockDecision.isUnlocked(emptyList()))
    }

    @Test
    fun aFreshPaidPurchaseIsAcknowledgedOnceAndPendingOnesAreNot() {
        assertTrue(UnlockDecision.needsAcknowledgement(fresh))
        assertFalse(UnlockDecision.needsAcknowledgement(paid))
        assertFalse(UnlockDecision.needsAcknowledgement(pending))
        assertFalse(UnlockDecision.needsAcknowledgement(other))
    }

    @Test
    fun theProductIdNeverChanges() {
        assertEquals("noisefile_unlock", UnlockDecision.PRODUCT_ID)
    }

    @Test
    fun thePriceLineUsesPlaysOwnWordsWhenItHasThem() {
        assertEquals("Pay once through Google Play.", UnlockDecision.priceLine(null))
        assertEquals("Pay once: \$7.99 through Google Play.", UnlockDecision.priceLine("\$7.99"))
    }
}
