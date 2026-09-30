package com.noisefile.app.billing

/**
 * The one paid thing in NoiseFile: a single one-time product on Google Play.
 * Everything about deciding "is this phone unlocked" lives here, with no Play code,
 * so it can be tested on the build machine.
 */
object UnlockDecision {
    /** The product id on Google Play. Never rename: buyers keep it for life. */
    const val PRODUCT_ID = "noisefile_unlock"

    /** One purchase as Play reports it, reduced to the three facts that matter. */
    data class PurchaseFacts(
        val productIds: List<String>,
        val purchased: Boolean,
        val acknowledged: Boolean,
    )

    /** True when any reported purchase is the unlock, paid (not pending), for this product. */
    fun isUnlocked(purchases: List<PurchaseFacts>): Boolean =
        purchases.any { it.productIds.contains(PRODUCT_ID) && it.purchased }

    /** Purchases Play still needs told "yes, the app saw this"; unacknowledged buys refund after three days. */
    fun needsAcknowledgement(purchase: PurchaseFacts): Boolean =
        purchase.productIds.contains(PRODUCT_ID) && purchase.purchased && !purchase.acknowledged

    /** The words on the card, given what the phone knows right now. */
    fun priceLine(priceText: String?): String =
        if (priceText == null) "Pay once through Google Play." else "Pay once: $priceText through Google Play."
}
