package com.noisefile.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** What the screen needs to know about the one-time unlock. */
data class UnlockState(
    val unlocked: Boolean = false,
    /** Play's own price string for the buyer's country, e.g. "$7.99"; null until Play answers. */
    val priceText: String? = null,
    val busy: Boolean = false,
    /** One short line for the buyer after a buy or restore attempt; null when there is nothing to say. */
    val message: String? = null,
)

/**
 * Talks to Google Play Billing for the single one-time product. The phone remembers
 * a finished purchase locally, and Play is asked again every time the app starts,
 * so a reinstall or a new phone restores it with no account of ours.
 */
class UnlockStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("noisefile_unlock", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(UnlockState(unlocked = prefs.getBoolean(KEY_UNLOCKED, false)))
    val state: StateFlow<UnlockState> = _state.asStateFlow()

    private var product: ProductDetails? = null

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener { result, purchases -> onPurchasesUpdated(result, purchases) }
        .enablePendingPurchases(
            com.android.billingclient.api.PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .build()

    /** Connect, learn the price, and ask Play what this account already owns. */
    fun start() {
        if (client.isReady) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) return
                queryPrice()
                restore(quiet = true)
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    /** Open Play's buy sheet on top of [activity]. */
    fun buy(activity: Activity) {
        val details = product
        if (!client.isReady || details == null) {
            _state.update { it.copy(message = "Google Play is not answering. Check that the Play Store app is signed in, then try again.") }
            start()
            return
        }
        // A one-time product carries its price on the details; no offer token needed.
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        _state.update { it.copy(busy = true, message = null) }
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _state.update { it.copy(busy = false, message = "Google Play could not open the purchase. ${result.debugMessage}".trim()) }
        }
    }

    /** Ask Play again what this account owns. [quiet] skips the "nothing found" line at startup. */
    fun restore(quiet: Boolean = false) {
        if (!client.isReady) {
            if (!quiet) _state.update { it.copy(message = "Google Play is not answering yet. Try again in a moment.") }
            start()
            return
        }
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                if (!quiet) _state.update { it.copy(message = "Google Play could not read your purchases right now.") }
                return@queryPurchasesAsync
            }
            apply(purchases, quiet)
        }
    }

    private fun queryPrice() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(UnlockDecision.PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()
        client.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            val details = queryResult.productDetailsList.firstOrNull { it.productId == UnlockDecision.PRODUCT_ID }
                ?: return@queryProductDetailsAsync
            product = details
            _state.update { it.copy(priceText = details.oneTimePurchaseOfferDetails?.formattedPrice) }
        }
    }

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> apply(purchases.orEmpty(), quiet = false)
            BillingClient.BillingResponseCode.USER_CANCELED -> _state.update { it.copy(busy = false, message = null) }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> restore(quiet = false)
            else -> _state.update { it.copy(busy = false, message = "Google Play did not finish the purchase. ${result.debugMessage}".trim()) }
        }
    }

    private fun apply(purchases: List<Purchase>, quiet: Boolean) {
        val facts = purchases.map { purchase ->
            UnlockDecision.PurchaseFacts(
                productIds = purchase.products,
                purchased = purchase.purchaseState == Purchase.PurchaseState.PURCHASED,
                acknowledged = purchase.isAcknowledged,
            )
        }
        purchases.zip(facts).filter { (_, fact) -> UnlockDecision.needsAcknowledgement(fact) }.forEach { (purchase, _) ->
            val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
            client.acknowledgePurchase(params) { }
        }
        val unlocked = UnlockDecision.isUnlocked(facts)
        val pending = facts.any { it.productIds.contains(UnlockDecision.PRODUCT_ID) && !it.purchased }
        if (unlocked) prefs.edit().putBoolean(KEY_UNLOCKED, true).apply()
        _state.update {
            it.copy(
                unlocked = unlocked || it.unlocked,
                busy = false,
                message = when {
                    unlocked && !quiet -> "Unlocked. Thank you."
                    pending -> "Google Play says the payment is still pending. The unlock opens when it clears."
                    quiet -> it.message
                    else -> "Google Play found no NoiseFile purchase on this account."
                },
            )
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    private companion object {
        const val KEY_UNLOCKED = "unlocked"
    }
}
