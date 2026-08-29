package com.ani.dailyspacenews

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.IronSourceBannerLayout
import com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAd
import com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.LevelPlayBannerListener
import com.ironsource.mediationsdk.sdk.LevelPlayInterstitialListener
import com.ironsource.mediationsdk.sdk.LevelPlayRewardedVideoListener

class LevelPlayAdManager(private val activity: ComponentActivity) {
    private var onRewardEarnedCallback: (() -> Unit)? = null
    private var onInterstitialDismissed: (() -> Unit)? = null
    private var mNativeAd: LevelPlayNativeAd? = null
    private var mBannerLayout: IronSourceBannerLayout? = null
    
    private val _nativeAd = mutableStateOf<Any?>(null)
    val nativeAd: State<Any?> = _nativeAd

    private val _isBannerLoaded = mutableStateOf(false)
    val isBannerLoaded: State<Boolean> = _isBannerLoaded

    private var lastInterstitialTime = 0L
    private val INTERSTITIAL_COOLDOWN = 45000L // 45 seconds debounce
    var isPremiumUser: Boolean = false

    fun init(canRequestAds: Boolean) {
        if (isPremiumUser || !canRequestAds) return

        IronSource.init(
            activity,
            BuildConfig.LEVELPLAY_APP_KEY,
            IronSource.AD_UNIT.BANNER,
            IronSource.AD_UNIT.REWARDED_VIDEO,
            IronSource.AD_UNIT.INTERSTITIAL,
            IronSource.AD_UNIT.NATIVE_AD
        )

        IronSource.setLevelPlayInterstitialListener(object : LevelPlayInterstitialListener {
            override fun onAdReady(adInfo: AdInfo) {}
            override fun onAdLoadFailed(error: IronSourceError) {
                onInterstitialDismissed?.invoke()
                onInterstitialDismissed = null
            }
            override fun onAdOpened(adInfo: AdInfo) {}
            override fun onAdClosed(adInfo: AdInfo) {
                if (!isPremiumUser) IronSource.loadInterstitial()
                onInterstitialDismissed?.invoke()
                onInterstitialDismissed = null
            }
            override fun onAdShowSucceeded(adInfo: AdInfo) {}
            override fun onAdShowFailed(error: IronSourceError, adInfo: AdInfo) {
                if (!isPremiumUser) IronSource.loadInterstitial()
                onInterstitialDismissed?.invoke()
                onInterstitialDismissed = null
            }
            override fun onAdClicked(adInfo: AdInfo) {}
        })

        IronSource.setLevelPlayRewardedVideoListener(object : LevelPlayRewardedVideoListener {
            override fun onAdAvailable(adInfo: AdInfo) {}
            override fun onAdUnavailable() {}
            override fun onAdOpened(adInfo: AdInfo) {}
            override fun onAdClosed(adInfo: AdInfo) {}
            override fun onAdRewarded(placement: com.ironsource.mediationsdk.model.Placement?, adInfo: AdInfo) {
                activity.runOnUiThread {
                    onRewardEarnedCallback?.invoke()
                    onRewardEarnedCallback = null
                }
            }
            override fun onAdShowFailed(error: IronSourceError, adInfo: AdInfo) {}
            override fun onAdClicked(placement: com.ironsource.mediationsdk.model.Placement?, adInfo: AdInfo) {}
        })

        IronSource.loadInterstitial()
        loadNativeAd()
    }

    fun getOrCreateBannerView(): IronSourceBannerLayout? {
        if (isPremiumUser) return null
        if (mBannerLayout != null) return mBannerLayout

        val banner = IronSource.createBanner(activity, ISBannerSize.BANNER)
        banner.levelPlayBannerListener = object : LevelPlayBannerListener {
            override fun onAdLoaded(adInfo: AdInfo) {
                _isBannerLoaded.value = true
            }
            override fun onAdLoadFailed(error: IronSourceError) {
                _isBannerLoaded.value = false
            }
            override fun onAdClicked(adInfo: AdInfo) {}
            override fun onAdLeftApplication(adInfo: AdInfo) {}
            override fun onAdScreenPresented(adInfo: AdInfo) {}
            override fun onAdScreenDismissed(adInfo: AdInfo) {}
        }
        IronSource.loadBanner(banner)
        mBannerLayout = banner
        return banner
    }

    fun loadNativeAd() {
        if (isPremiumUser) return
        mNativeAd = LevelPlayNativeAd.Builder()
            .withPlacementName(BuildConfig.LP_NATIVE_ID)
            .withListener(object : LevelPlayNativeAdListener {
                override fun onAdLoaded(nativeAd: LevelPlayNativeAd?, adInfo: AdInfo?) {
                    activity.runOnUiThread {
                        _nativeAd.value = nativeAd
                    }
                }
                override fun onAdLoadFailed(nativeAd: LevelPlayNativeAd?, error: IronSourceError?) {
                    activity.runOnUiThread {
                        _nativeAd.value = null
                    }
                }
                override fun onAdClicked(nativeAd: LevelPlayNativeAd?, adInfo: AdInfo?) {}
                override fun onAdImpression(nativeAd: LevelPlayNativeAd?, adInfo: AdInfo?) {
                    if (!isPremiumUser) loadNativeAd()
                }
            })
            .build()
        mNativeAd?.loadAd()
    }

    fun showInterstitial(onFinished: () -> Unit) {
        if (isPremiumUser) {
            onFinished()
            return
        }
        val currentTime = System.currentTimeMillis()
        if (IronSource.isInterstitialReady() && currentTime - lastInterstitialTime >= INTERSTITIAL_COOLDOWN) {
            this.onInterstitialDismissed = onFinished
            lastInterstitialTime = currentTime
            IronSource.showInterstitial(BuildConfig.LP_INTERSTITIAL_ID)
        } else {
            if (!IronSource.isInterstitialReady()) IronSource.loadInterstitial()
            onFinished()
        }
    }

    fun showRewardAd(onRewardEarned: () -> Unit) {
        if (isPremiumUser) {
            onRewardEarned()
            return
        }
        if (IronSource.isRewardedVideoAvailable()) {
            this.onRewardEarnedCallback = onRewardEarned
            IronSource.showRewardedVideo(BuildConfig.LP_REWARDED_ID)
        } else {
            Toast.makeText(activity, "Ad not available, try again later.", Toast.LENGTH_SHORT).show()
        }
    }

    fun onResume() {
        if (!isPremiumUser) IronSource.onResume(activity)
    }

    fun onPause() {
        if (!isPremiumUser) IronSource.onPause(activity)
    }

    fun onDestroy() {
        mBannerLayout?.let {
            IronSource.destroyBanner(it)
            mBannerLayout = null
        }
        mNativeAd?.destroyAd()
        mNativeAd = null
    }
}
