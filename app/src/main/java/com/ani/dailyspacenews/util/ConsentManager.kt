package com.ani.dailyspacenews.util

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.ironsource.mediationsdk.IronSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConsentManager(private val context: Context) {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    private val _canRequestAds = MutableStateFlow(consentInformation.canRequestAds())
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    fun gatherConsent(activity: Activity, onConsentCompleted: (canRequest: Boolean) -> Unit) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    val canRequest = consentInformation.canRequestAds()
                    _canRequestAds.value = canRequest
                    
                    // Inform ironSource of consent status
                    IronSource.setConsent(canRequest)
                    
                    onConsentCompleted(canRequest)
                }
            },
            { requestConsentError ->
                // On error, check fallback status
                val canRequest = consentInformation.canRequestAds()
                _canRequestAds.value = canRequest
                onConsentCompleted(canRequest)
            }
        )
    }

    val isPrivacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptionsForm(activity: Activity, onDismiss: () -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            val canRequest = consentInformation.canRequestAds()
            _canRequestAds.value = canRequest
            IronSource.setConsent(canRequest)
            onDismiss()
        }
    }
}
