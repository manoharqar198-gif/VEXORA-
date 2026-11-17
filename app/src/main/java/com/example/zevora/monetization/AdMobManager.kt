package com.example.zevora.monetization

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdMobManager {

  // Official Google AdMob Test Ad Unit IDs
  const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
  const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
  const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

  private var isInitialized = false
  private var interstitialAd: InterstitialAd? = null
  private var rewardedAd: RewardedAd? = null

  // Anti-Fraud & Invalid Click Prevention
  private var lastInterstitialShownTime: Long = 0
  private const val MIN_INTERSTITIAL_INTERVAL_MS: Long = 45_000 // Min 45s between interstitial ads

  /**
   * Central Premium Gate Logic Layer:
   * Ads are fetched and displayed ONLY when the user is NOT a premium member.
   */
  fun isAdEligible(isPremium: Boolean): Boolean = !isPremium

  fun hasLoadedInterstitial(): Boolean = interstitialAd != null

  fun clearAdsCache() {
    interstitialAd = null
    rewardedAd = null
  }

  /**
   * Synchronizes SDK initialization and ad preloading with the user's premium status.
   * When isPremium == true, cached ads are purged and no ad network requests are made.
   */
  fun syncPremiumStatusAndFetchAds(context: Context, isPremium: Boolean) {
    if (!isAdEligible(isPremium)) {
      clearAdsCache()
      return
    }
    initialize(context, isPremium = false)
  }

  fun initialize(context: Context, isPremium: Boolean = false) {
    if (!isAdEligible(isPremium)) {
      clearAdsCache()
      return
    }
    if (!isInitialized) {
      try {
        MobileAds.initialize(context) {
          isInitialized = true
        }
      } catch (_: Exception) {
        // Graceful fallback in JVM/Robolectric unit test environments
      }
    }
    loadInterstitialAd(context, isPremium = false)
    loadRewardedAd(context, isPremium = false)
  }

  fun loadInterstitialAd(context: Context, isPremium: Boolean = false) {
    if (!isAdEligible(isPremium)) {
      interstitialAd = null
      return
    }
    try {
      val adRequest = AdRequest.Builder().build()
      InterstitialAd.load(
        context,
        TEST_INTERSTITIAL_AD_UNIT_ID,
        adRequest,
        object : InterstitialAdLoadCallback() {
          override fun onAdLoaded(ad: InterstitialAd) {
            interstitialAd = ad
          }

          override fun onAdFailedToLoad(error: LoadAdError) {
            interstitialAd = null
          }
        }
      )
    } catch (_: Exception) {
      interstitialAd = null
    }
  }

  fun showInterstitialAd(
    activity: Activity,
    isPremium: Boolean = false,
    onAdDismissed: () -> Unit = {}
  ) {
    showInterstitialAdIfReady(activity, isPremium, onAdDismissed)
  }

  fun showInterstitialAdIfReady(
    activity: Activity,
    isPremium: Boolean = false,
    onAdDismissed: () -> Unit
  ) {
    if (!isAdEligible(isPremium)) {
      clearAdsCache()
      onAdDismissed()
      return
    }

    val currentTime = System.currentTimeMillis()
    // Check rate limiter to prevent user fatigue & invalid click penalties
    if (currentTime - lastInterstitialShownTime < MIN_INTERSTITIAL_INTERVAL_MS) {
      onAdDismissed()
      return
    }

    val ad = interstitialAd
    if (ad != null) {
      ad.fullScreenContentCallback = object : FullScreenContentCallback() {
        override fun onAdDismissedFullScreenContent() {
          lastInterstitialShownTime = System.currentTimeMillis()
          interstitialAd = null
          loadInterstitialAd(activity, isPremium = false)
          onAdDismissed()
        }

        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
          interstitialAd = null
          loadInterstitialAd(activity, isPremium = false)
          onAdDismissed()
        }
      }
      ad.show(activity)
    } else {
      loadInterstitialAd(activity, isPremium = false)
      onAdDismissed()
    }
  }

  fun loadRewardedAd(context: Context, isPremium: Boolean = false) {
    if (!isAdEligible(isPremium)) {
      rewardedAd = null
      return
    }
    try {
      val adRequest = AdRequest.Builder().build()
      RewardedAd.load(
        context,
        TEST_REWARDED_AD_UNIT_ID,
        adRequest,
        object : RewardedAdLoadCallback() {
          override fun onAdLoaded(ad: RewardedAd) {
            rewardedAd = ad
          }

          override fun onAdFailedToLoad(adError: LoadAdError) {
            rewardedAd = null
          }
        }
      )
    } catch (_: Exception) {
      rewardedAd = null
    }
  }

  fun showRewardedAd(
    activity: Activity,
    onUserEarnedReward: (amount: Int) -> Unit,
    onAdClosed: () -> Unit
  ) {
    val ad = rewardedAd
    if (ad != null) {
      ad.fullScreenContentCallback = object : FullScreenContentCallback() {
        override fun onAdDismissedFullScreenContent() {
          rewardedAd = null
          loadRewardedAd(activity)
          onAdClosed()
        }

        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
          rewardedAd = null
          loadRewardedAd(activity)
          onAdClosed()
        }
      }
      ad.show(activity) { rewardItem ->
        onUserEarnedReward(rewardItem.amount.coerceAtLeast(20))
      }
    } else {
      loadRewardedAd(activity)
      onAdClosed()
    }
  }
}

/**
 * Sponsored Native Banner Card for Home Feed & Reels.
 * Strictly skips inflating AdView and loading ads when isPremium == true.
 */
@Composable
fun AdMobFeedBanner(
  isPremium: Boolean = false,
  modifier: Modifier = Modifier,
  adUnitId: String = AdMobManager.TEST_BANNER_AD_UNIT_ID,
  compact: Boolean = false
) {
  if (!AdMobManager.isAdEligible(isPremium)) {
    return
  }

  var hasLoadError by remember { mutableStateOf(false) }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = if (compact) 4.dp else 8.dp)
      .testTag("admob_feed_banner_card"),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = if (compact) 0.9f else 1f)),
    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
  ) {
    Column(modifier = Modifier.padding(if (compact) 8.dp else 12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          color = Color(0xFFF59E0B),
          shape = RoundedCornerShape(4.dp)
        ) {
          Text(
            "Ad • प्रायोजित",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          "Google AdMob Test Banner",
          color = Color(0xFF94A3B8),
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium
        )
      }

      Spacer(modifier = Modifier.height(if (compact) 6.dp else 10.dp))

      if (!hasLoadError) {
        AndroidView(
          factory = { ctx ->
            AdView(ctx).apply {
              setAdSize(AdSize.BANNER)
              this.adUnitId = adUnitId
              adListener = object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                  hasLoadError = true
                }
              }
              loadAd(AdRequest.Builder().build())
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
        )
      }

      if (hasLoadError) {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp)),
          color = Color(0xFF0F172A)
        ) {
          Row(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.horizontalGradient(listOf(Color(0xFF1E1B4B), Color(0xFF0F172A)))
              )
              .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                "Go Ad-Free with VEXORA VIP Pro",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
              Text(
                "Ad unit unavailable offline • Upgrade to hide ads",
                color = Color(0xFF38BDF8),
                fontSize = 10.sp
              )
            }
          }
        }
      }
    }
  }
}
