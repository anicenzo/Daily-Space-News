package com.ani.dailyspacenews.billing

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.ani.dailyspacenews.BuildConfig
import com.ani.dailyspacenews.util.dataStore
import com.qonversion.android.sdk.Qonversion
import com.qonversion.android.sdk.QonversionConfig
import com.qonversion.android.sdk.dto.QLaunchMode
import com.qonversion.android.sdk.dto.QonversionError
import com.qonversion.android.sdk.dto.entitlements.QEntitlement
import com.qonversion.android.sdk.dto.offerings.QOfferings
import com.qonversion.android.sdk.dto.products.QProduct
import com.qonversion.android.sdk.dto.QPurchaseResult
import com.qonversion.android.sdk.listeners.QonversionPurchaseCallback
import com.qonversion.android.sdk.listeners.QonversionOfferingsCallback
import com.qonversion.android.sdk.listeners.QonversionEntitlementsCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Repository managing Qonversion In-App Purchases and Subscriptions.
 *
 * Architecture:
 * - Backed by a single `isPremiumUser: StateFlow<Boolean>` that gates ads, HD NASA images, etc.
 * - Handles Offerings -> QProduct -> purchase(activity, product, callback) -> Entitlement validation.
 * - Automatically checks entitlements & restores on app startup.
 */
class BillingRepository(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "BillingRepository"
        const val ENTITLEMENT_PREMIUM = "premium"
        const val PRODUCT_ID_PREMIUM = "premium_monthly"
        val PREF_IS_PREMIUM = booleanPreferencesKey("is_premium_user")
    }

    private val _isPremiumUser = MutableStateFlow(false)
    val isPremiumUser: StateFlow<Boolean> = _isPremiumUser.asStateFlow()

    private val _selectedProduct = MutableStateFlow<QProduct?>(null)
    val selectedProduct: StateFlow<QProduct?> = _selectedProduct.asStateFlow()

    private val _formattedPrice = MutableStateFlow("$2.99 / month")
    val formattedPrice: StateFlow<String> = _formattedPrice.asStateFlow()

    private val _billingStatusMessage = MutableStateFlow<String?>(null)
    val billingStatusMessage: StateFlow<String?> = _billingStatusMessage.asStateFlow()

    init {
        // 1. Initial cached entitlement read from DataStore for instant offline startup
        coroutineScope.launch(Dispatchers.IO) {
            val prefs = context.dataStore.data.first()
            val cachedPremium = prefs[PREF_IS_PREMIUM] ?: false
            _isPremiumUser.value = cachedPremium
        }

        // 2. Initialize Qonversion SDK
        try {
            val app = context.applicationContext as Application
            val config = QonversionConfig.Builder(
                app,
                BuildConfig.QONVERSION_PROJECT_KEY,
                QLaunchMode.SubscriptionManagement
            ).build()
            Qonversion.initialize(config)
            Log.d(TAG, "Qonversion initialized successfully.")

            // 3. Reconcile entitlements & load offerings
            checkEntitlements()
            fetchOfferings()
        } catch (e: Exception) {
            Log.e(TAG, "Qonversion initialization error: ${e.message}")
        }
    }

    fun checkEntitlements() {
        try {
            Qonversion.shared.checkEntitlements(object : QonversionEntitlementsCallback {
                override fun onSuccess(entitlements: Map<String, QEntitlement>) {
                    val isPremium = entitlements.values.any { it.isActive }
                    updatePremiumStatus(isPremium)
                }

                override fun onError(error: QonversionError) {
                    Log.e(TAG, "checkEntitlements error: ${error.description}")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "checkEntitlements exception: ${e.message}")
        }
    }

    fun fetchOfferings() {
        try {
            Qonversion.shared.offerings(object : QonversionOfferingsCallback {
                override fun onSuccess(offerings: QOfferings) {
                    val mainOffering = offerings.main ?: offerings.availableOfferings.firstOrNull()
                    val product = mainOffering?.products?.firstOrNull { it.qonversionId == PRODUCT_ID_PREMIUM || it.storeId == PRODUCT_ID_PREMIUM }
                        ?: mainOffering?.products?.firstOrNull()

                    if (product != null) {
                        _selectedProduct.value = product
                        val prettyPrice = product.prettyPrice
                        if (!prettyPrice.isNullOrEmpty()) {
                            _formattedPrice.value = "$prettyPrice / month"
                        }
                    }
                }

                override fun onError(error: QonversionError) {
                    Log.e(TAG, "fetchOfferings error: ${error.description}")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "fetchOfferings exception: ${e.message}")
        }
    }

    fun launchBillingFlow(activity: Activity, onComplete: ((Boolean) -> Unit)? = null) {
        val product = _selectedProduct.value
        if (product == null) {
            Log.w(TAG, "No QProduct available for checkout. Fetching offerings again.")
            _billingStatusMessage.value = "Preparing checkout... please try again in a moment."
            fetchOfferings()
            onComplete?.invoke(false)
            return
        }

        try {
            Qonversion.shared.purchase(
                activity,
                product,
                object : QonversionPurchaseCallback {
                    override fun onResult(result: QPurchaseResult) {
                        if (result.isSuccessful) {
                            val isPremium = result.entitlements.values.any { it.isActive }
                            updatePremiumStatus(isPremium)
                            onComplete?.invoke(isPremium)
                        } else {
                            val errorMsg = result.error?.description
                            if (!result.isCanceledByUser && errorMsg != null) {
                                Log.e(TAG, "purchase error: $errorMsg")
                                _billingStatusMessage.value = errorMsg
                            }
                            onComplete?.invoke(false)
                        }
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "purchase exception: ${e.message}")
            _billingStatusMessage.value = e.message
            onComplete?.invoke(false)
        }
    }

    fun restorePurchases(onComplete: ((Boolean) -> Unit)? = null) {
        try {
            Qonversion.shared.restore(object : QonversionEntitlementsCallback {
                override fun onSuccess(entitlements: Map<String, QEntitlement>) {
                    val isPremium = entitlements.values.any { it.isActive }
                    updatePremiumStatus(isPremium)
                    onComplete?.invoke(isPremium)
                }

                override fun onError(error: QonversionError) {
                    Log.e(TAG, "restore error: ${error.description}")
                    _billingStatusMessage.value = "Restore failed: ${error.description}"
                    onComplete?.invoke(_isPremiumUser.value)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "restore exception: ${e.message}")
            onComplete?.invoke(_isPremiumUser.value)
        }
    }

    private fun updatePremiumStatus(isPremium: Boolean) {
        _isPremiumUser.value = isPremium
        coroutineScope.launch(Dispatchers.IO) {
            context.dataStore.edit { prefs ->
                prefs[PREF_IS_PREMIUM] = isPremium
            }
        }
    }

    fun clearStatusMessage() {
        _billingStatusMessage.value = null
    }
}
