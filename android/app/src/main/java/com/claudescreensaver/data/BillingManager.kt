package com.claudescreensaver.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ProStatus {
    TRIAL,      // Within 14-day trial
    PRO,        // Active subscription or lifetime purchase
    FREE,       // Trial expired, no subscription
}

class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    private val _proStatus = MutableStateFlow(ProStatus.FREE)
    val proStatus: StateFlow<ProStatus> = _proStatus.asStateFlow()

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products.asStateFlow()

    private var billingClient: BillingClient? = null

    companion object {
        private const val PREFS_KEY = "claude_screensaver"
        private const val TRIAL_START_KEY = "trial_start_ms"
        private const val TRIAL_DURATION_MS = 14L * 24 * 60 * 60 * 1000 // 14 days

        val SUBSCRIPTION_IDS = listOf("monthly_pro", "annual_pro")
        const val LIFETIME_ID = "lifetime_pro"
    }

    fun initialize() {
        // Check trial status first
        initTrialIfNeeded()
        updateProStatus()

        // Connect billing client
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                    queryPurchases()
                }
            }
            override fun onBillingServiceDisconnected() {
                // Will retry on next app launch
            }
        })
    }

    private fun initTrialIfNeeded() {
        val prefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
        if (!prefs.contains(TRIAL_START_KEY)) {
            prefs.edit().putLong(TRIAL_START_KEY, System.currentTimeMillis()).apply()
        }
    }

    private fun isTrialActive(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
        val trialStart = prefs.getLong(TRIAL_START_KEY, 0L)
        if (trialStart == 0L) return false
        return (System.currentTimeMillis() - trialStart) < TRIAL_DURATION_MS
    }

    fun trialDaysRemaining(): Int {
        val prefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
        val trialStart = prefs.getLong(TRIAL_START_KEY, 0L)
        if (trialStart == 0L) return 0
        val elapsed = System.currentTimeMillis() - trialStart
        val remaining = TRIAL_DURATION_MS - elapsed
        return if (remaining > 0) (remaining / (24 * 60 * 60 * 1000)).toInt() + 1 else 0
    }

    private fun updateProStatus() {
        // This is called before billing check -- set trial/free based on local state
        // Will be upgraded to PRO if billing query finds active purchase
        _proStatus.value = if (isTrialActive()) ProStatus.TRIAL else ProStatus.FREE
    }

    private fun queryProducts() {
        val subParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                SUBSCRIPTION_IDS.map { id ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(id)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }
            )
            .build()

        billingClient?.queryProductDetailsAsync(subParams) { result, queryResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val allProducts = queryResult.productDetailsList.toMutableList()
                // Also query lifetime (one-time)
                val otpParams = QueryProductDetailsParams.newBuilder()
                    .setProductList(
                        listOf(
                            QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(LIFETIME_ID)
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build()
                        )
                    )
                    .build()
                billingClient?.queryProductDetailsAsync(otpParams) { otpResult, otpQueryResult ->
                    if (otpResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        allProducts.addAll(otpQueryResult.productDetailsList)
                    }
                    _products.value = allProducts
                }
            }
        }
    }

    private fun queryPurchases() {
        // Check subscriptions
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasActiveSub = purchases.any {
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                if (hasActiveSub) {
                    _proStatus.value = ProStatus.PRO
                    // Acknowledge unacknowledged purchases
                    purchases.filter { !it.isAcknowledged }.forEach { acknowledgePurchase(it) }
                    return@queryPurchasesAsync
                }
            }
            // Check lifetime purchase
            billingClient?.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            ) { otpResult, otpPurchases ->
                if (otpResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val hasLifetime = otpPurchases.any {
                        it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                            it.products.contains(LIFETIME_ID)
                    }
                    if (hasLifetime) {
                        _proStatus.value = ProStatus.PRO
                        otpPurchases.filter { !it.isAcknowledged }.forEach { acknowledgePurchase(it) }
                    }
                }
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient?.acknowledgePurchase(params) { /* fire and forget */ }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    _proStatus.value = ProStatus.PRO
                    if (!purchase.isAcknowledged) acknowledgePurchase(purchase)
                }
            }
        }
    }

    fun launchPurchase(activity: Activity, productDetails: ProductDetails) {
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken

        val productDetailsParams = if (offerToken != null) {
            // Subscription
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        } else {
            // One-time purchase (lifetime)
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        billingClient?.launchBillingFlow(activity, flowParams)
    }

    fun destroy() {
        billingClient?.endConnection()
    }
}
