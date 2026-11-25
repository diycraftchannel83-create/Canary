package com.example.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.R
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

object AdMobManager : DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {

    private const val TAG = "AdMobManager"
    private const val APP_OPEN_AD_EXPIRY_HOURS = 4L

    private val initialized = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var appContext: Context? = null
    private var currentActivityRef: WeakReference<Activity>? = null

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAppOpenAd = false
    private var appOpenLoadTimeMs = 0L
    private var appIsForeground = false
    private var appOpenAttemptedThisForeground = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var isLoadingRewardedInterstitial = false

    private val _isRewardedInterstitialReady = MutableStateFlow(false)
    val isRewardedInterstitialReady: StateFlow<Boolean> = _isRewardedInterstitialReady.asStateFlow()

    @Volatile
    var isShowingAd: Boolean = false
        private set

    @Volatile
    private var suppressNextAppOpen: Boolean = false

    fun initialize(application: Application) {
        if (!initialized.compareAndSet(false, true)) return
        appContext = application.applicationContext
        application.registerActivityLifecycleCallbacks(this)

        try {
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        } catch (t: Throwable) {
            Log.w(TAG, "Unable to attach ProcessLifecycleOwner observer", t)
        }

        scope.launch {
            try {
                withContext(Dispatchers.Main) {
                    MobileAds.initialize(application) {
                        loadAppOpenAd(application)
                        loadRewardedInterstitialAd(application)
                        Log.d(TAG, "MobileAds initialization complete")
                    }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "MobileAds initialization skipped or failed", t)
            }
        }
    }

    /**
     * Suppresses the next App Open ad when returning from an in-app system launcher
     * such as Google Credential Manager, Camera capture, or Share chooser.
     */
    fun suppressNextAppOpen() {
        suppressNextAppOpen = true
    }

    fun isAppOpenAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(APP_OPEN_AD_EXPIRY_HOURS)
    }

    fun isRewardedInterstitialAvailable(): Boolean {
        return rewardedInterstitialAd != null
    }

    private fun wasLoadTimeLessThanNHoursAgo(hours: Long): Boolean {
        val elapsedMs = Date().time - appOpenLoadTimeMs
        val maxMs = hours * 3_600_000L
        return elapsedMs < maxMs
    }

    fun loadAppOpenAd(context: Context? = null) {
        val ctx = context ?: appContext ?: return
        if (isLoadingAppOpenAd || isAppOpenAdAvailable()) return
        val unitId = try {
            ctx.getString(R.string.admob_app_open_unit_id)
        } catch (_: Exception) {
            return
        }
        if (unitId.isBlank()) return

        isLoadingAppOpenAd = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            ctx,
            unitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAppOpenAd = false
                    appOpenLoadTimeMs = Date().time
                    Log.d(TAG, "App Open ad loaded")
                    if (appIsForeground && !appOpenAttemptedThisForeground) {
                        currentActivityRef?.get()?.let { activity ->
                            mainHandler.post { showAppOpenAdIfAvailable(activity) }
                        }
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAppOpenAd = false
                    appOpenAd = null
                    Log.w(TAG, "App Open ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    fun showAppOpenAdIfAvailable(activity: Activity) {
        if (isShowingAd || activity.isFinishing || activity.isDestroyed) return
        if (!appIsForeground || appOpenAttemptedThisForeground) return
        appOpenAttemptedThisForeground = true
        if (suppressNextAppOpen) {
            suppressNextAppOpen = false
            return
        }
        val ad = appOpenAd
        if (ad == null || !wasLoadTimeLessThanNHoursAgo(APP_OPEN_AD_EXPIRY_HOURS)) {
            appOpenAd = null
            loadAppOpenAd(activity.applicationContext)
            appOpenAttemptedThisForeground = false
            return
        }

        isShowingAd = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                loadAppOpenAd(activity.applicationContext)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false
                loadAppOpenAd(activity.applicationContext)
                Log.w(TAG, "App Open ad failed to show: ${adError.message}")
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "App Open ad shown")
            }
        }
        ad.show(activity)
    }

    fun loadRewardedInterstitialAd(context: Context? = null) {
        val ctx = context ?: appContext ?: return
        if (isLoadingRewardedInterstitial || rewardedInterstitialAd != null) return
        val unitId = try {
            ctx.getString(R.string.admob_rewarded_interstitial_unit_id)
        } catch (_: Exception) {
            return
        }
        if (unitId.isBlank()) return

        isLoadingRewardedInterstitial = true
        val request = AdRequest.Builder().build()
        RewardedInterstitialAd.load(
            ctx,
            unitId,
            request,
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    rewardedInterstitialAd = ad
                    isLoadingRewardedInterstitial = false
                    _isRewardedInterstitialReady.value = true
                    Log.d(TAG, "Rewarded Interstitial ad loaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedInterstitialAd = null
                    isLoadingRewardedInterstitial = false
                    _isRewardedInterstitialReady.value = false
                    Log.w(TAG, "Rewarded Interstitial failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    fun showRewardedInterstitialAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onDismissedWithoutReward: () -> Unit,
        onAdUnavailableOrFailed: () -> Unit
    ) {
        if (isShowingAd) {
            onAdUnavailableOrFailed()
            return
        }
        val ad = rewardedInterstitialAd
        if (ad == null) {
            _isRewardedInterstitialReady.value = false
            loadRewardedInterstitialAd(activity.applicationContext)
            onAdUnavailableOrFailed()
            return
        }

        var earnedReward = false
        isShowingAd = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedInterstitialAd = null
                _isRewardedInterstitialReady.value = false
                isShowingAd = false
                loadRewardedInterstitialAd(activity.applicationContext)
                if (earnedReward) {
                    onRewardEarned()
                } else {
                    onDismissedWithoutReward()
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedInterstitialAd = null
                _isRewardedInterstitialReady.value = false
                isShowingAd = false
                loadRewardedInterstitialAd(activity.applicationContext)
                Log.w(TAG, "Rewarded Interstitial failed to show: ${adError.message}")
                onAdUnavailableOrFailed()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded Interstitial ad shown")
            }
        }
        ad.show(activity) {
            earnedReward = true
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        appIsForeground = true
        appOpenAttemptedThisForeground = false
        currentActivityRef?.get()?.let { activity ->
            if (!activity.isFinishing && !activity.isDestroyed) showAppOpenAdIfAvailable(activity)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        appIsForeground = false
        appOpenAttemptedThisForeground = false
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) {
        if (!isShowingAd) {
            currentActivityRef = WeakReference(activity)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (!isShowingAd) {
            currentActivityRef = WeakReference(activity)
        }
    }

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivityRef?.get() === activity) {
            currentActivityRef = null
        }
    }
}

tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}
