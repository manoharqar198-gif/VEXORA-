package com.example

import com.example.zevora.data.ZevoraRepository
import com.example.zevora.data.firebase.PhoneAuthManager
import com.example.zevora.model.CommentControlOption
import com.example.zevora.ui.Screen
import com.example.zevora.ui.ZevoraViewModel
import kotlinx.coroutines.async
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleRobolectricTest {

  @Test
  fun testVexoraGoogleAuthGatingAndVerifiedSession() = kotlinx.coroutines.runBlocking {
    val repo = ZevoraRepository()
    val viewModel = ZevoraViewModel(repo)

    // Starts unauthenticated when no Firebase Google user is present
    assertFalse(viewModel.isAuthenticated.value)

    // Blank UID must never trigger a fake login
    repo.resolveGoogleFirebaseSession(
      uid = "",
      displayName = "Fake User",
      email = "fake@example.com",
      photoUrl = null
    )
    assertFalse(viewModel.isAuthenticated.value)
    assertTrue(!viewModel.authErrorMessage.value.isNullOrBlank())

    // Verified Firebase Google user resolves session cleanly and opens Home Feed
    repo.resolveGoogleFirebaseSession(
      uid = "google_uid_12345",
      displayName = "Manohar Kumar",
      email = "manohar.vexora@gmail.com",
      photoUrl = "https://example.com/avatar.jpg"
    )
    assertTrue(viewModel.isAuthenticated.value)
    assertTrue(viewModel.firebaseDiagnostics.value.isFirebaseSignedIn)
    assertEquals("Manohar Kumar", viewModel.currentUser.value.name)
    assertEquals("manohar.vexora", viewModel.currentUser.value.username)
    assertEquals(Screen.HOME, viewModel.currentScreen.value)
    assertTrue(viewModel.feedPosts.value.isNotEmpty())
    assertTrue(viewModel.stories.value.isNotEmpty())
    assertTrue(viewModel.reels.value.isNotEmpty())
    assertTrue(viewModel.videos.value.isNotEmpty())
    assertTrue(viewModel.chatConversations.value.isNotEmpty())

    // Navigating across Reels, Admin, and Profile works after Google login
    viewModel.navigateTo(Screen.REELS)
    assertEquals(Screen.REELS, viewModel.currentScreen.value)
    viewModel.navigateBack()
    assertEquals(Screen.HOME, viewModel.currentScreen.value)

    viewModel.navigateTo(Screen.ADMIN_PANEL)
    assertEquals(Screen.ADMIN_PANEL, viewModel.currentScreen.value)

    // Sign out preserves content & resets screen stack to HOME
    viewModel.clearAuthenticatedSession()
    assertFalse(viewModel.isAuthenticated.value)
    assertEquals(Screen.HOME, viewModel.currentScreen.value)
    assertTrue(viewModel.feedPosts.value.isNotEmpty())
  }

  @Test
  fun testVexoraSecurityAndPrivacyControls() {
    val repo = ZevoraRepository()
    repo.togglePrivateAccount(true)
    assertTrue(repo.isPrivateAccount.value)

    repo.setCommentControl(CommentControlOption.PEOPLE_YOU_FOLLOW)
    assertEquals(CommentControlOption.PEOPLE_YOU_FOLLOW, repo.commentControl.value)

    repo.blockUser("spam_account", "Spam User")
    assertTrue(repo.blockedUsers.value.any { it.username == "spam_account" })
  }

  @Test
  fun testMonetizationWalletAndLiveGifts() {
    val repo = ZevoraRepository()
    val initialCoins = repo.userWallet.value.coinsBalance
    
    // Test Coin Purchase
    val bought = repo.purchaseCoins("pack_100", "ORDER_TEST_100")
    assertTrue(bought)
    assertEquals(initialCoins + 100, repo.userWallet.value.coinsBalance)

    // Test Live Gift sending with sufficient coins
    val giftResult = repo.sendLiveGift("gift_rose", "Priya Verma")
    assertTrue(giftResult.first)
    assertEquals(initialCoins + 90, repo.userWallet.value.coinsBalance) // 100 - 10 coins

    // Test Withdrawal validation: minimum 1000 INR
    val lowWithdraw = repo.requestWithdrawal(500.0)
    assertFalse(lowWithdraw.first) // Below 1,000 threshold

    val validWithdraw = repo.requestWithdrawal(1000.0)
    assertTrue(validWithdraw.first)
    assertTrue(repo.withdrawalRequests.value.any { it.amountRupees == 1000.0 })
  }

  @Test
  fun testLiveStreamingAndGuestModeration() {
    val repo = ZevoraRepository()
    val room = repo.startLiveStream("Special Announcement Live")
    assertEquals(room.id, repo.activeLiveRoom.value?.id)
    assertTrue(repo.activeLiveRoom.value?.isLive == true)

    // Send comment and heart
    repo.sendLiveComment("Hello live viewers!")
    assertTrue(repo.liveComments.value.any { it.message.contains("Hello live viewers") })

    val initialLikes = repo.activeLiveRoom.value?.likesCount ?: 0
    repo.sendLiveHeart()
    assertEquals(initialLikes + 1, repo.activeLiveRoom.value?.likesCount)

    // Add guest and mute/remove
    val guestUser = com.example.zevora.model.User("u_guest", "Rohan Demo", "rohan_demo")
    repo.addGuestToLive(guestUser)
    assertTrue(repo.activeLiveRoom.value?.guests?.any { it.user.id == "u_guest" } == true)

    val guestId = repo.activeLiveRoom.value!!.guests.first { it.user.id == "u_guest" }.id
    repo.toggleGuestMute(guestId)
    assertTrue(repo.activeLiveRoom.value!!.guests.first { it.id == guestId }.isAudioMuted)

    repo.removeGuestFromLive(guestId)
    assertFalse(repo.activeLiveRoom.value!!.guests.any { it.id == guestId })

    // End stream with save recording
    repo.endLiveStream(saveRecording = true)
    assertTrue(repo.activeLiveRoom.value == null)
  }

  @Test
  fun testPostReelAndStoryCreation() {
    val repo = ZevoraRepository()
    val post = repo.createPost(
      content = "Testing multi-photo post",
      mediaUrls = listOf("file://photo1.jpg", "file://photo2.jpg"),
      location = "Mumbai, India",
      hashtags = listOf("#Vexora", "#Tech")
    )
    assertTrue(repo.feedPosts.value.any { it.id == post.id })
    assertEquals(2, repo.feedPosts.value.first { it.id == post.id }.mediaUrls.size)

    val reel = repo.createReel("Awesome dance reel", "Original Sound - Vexora")
    assertTrue(repo.reels.value.any { it.id == reel.id })

    val story = repo.createStory("Sunset vibes ✨", "file://story.jpg")
    repo.saveStoryToHighlights(story.id)
    val myStory = repo.stories.value.first { it.id == "s1" }
    assertTrue(myStory.items.any { it.id == story.id && it.isSavedToHighlights })
  }

  @Test
  fun testPerformanceAndNetworkOptimization() {
    val repo = ZevoraRepository()

    // 1. Test Pagination
    val initialPostsCount = repo.feedPosts.value.size
    repo.loadMorePosts()
    assertTrue(repo.feedPosts.value.size > initialPostsCount)

    // 2. Test Slow Network (2G/3G) Simulation & Auto Data Saver
    repo.setSimulatedNetwork(com.example.zevora.model.NetworkQuality.SLOW)
    assertEquals(com.example.zevora.model.NetworkQuality.SLOW, repo.networkQuality.value)
    assertTrue(repo.dataSaverPreferences.value.isDataSaverEnabled)
    assertEquals(com.example.zevora.model.VideoQualityOption.DATA_SAVER, repo.dataSaverPreferences.value.videoQuality)

    // 3. Test Offline Simulation & Cache
    repo.setSimulatedNetwork(com.example.zevora.model.NetworkQuality.OFFLINE)
    assertTrue(repo.isOffline.value)
    // Posts remain available in cache while offline
    assertTrue(repo.feedPosts.value.isNotEmpty())

    // 4. Test Cache Clear
    val cacheResult = repo.clearLocalCache()
    assertTrue(cacheResult.contains("cleared"))

    // Reset to normal
    repo.setSimulatedNetwork(null)
    assertFalse(repo.isOffline.value)
  }

  @Test
  fun testAdMobPremiumGateLogic() {
    val repo = ZevoraRepository()
    // Default user starts on Free tier (non-premium), so ads are eligible
    assertFalse(repo.premiumSubscription.value.isPremium)
    assertTrue(com.example.zevora.monetization.AdMobManager.isAdEligible(repo.premiumSubscription.value.isPremium))

    // Upgrading to VIP Pro disables ads immediately
    repo.subscribeToPremium(com.example.zevora.model.SubscriptionPlan.MONTHLY, "GPA.TEST.123")
    assertTrue(repo.premiumSubscription.value.isPremium)
    assertFalse(com.example.zevora.monetization.AdMobManager.isAdEligible(repo.premiumSubscription.value.isPremium))

    // Switching back to Free tier re-enables ads
    repo.cancelPremiumSubscription()
    assertFalse(repo.premiumSubscription.value.isPremium)
    assertTrue(com.example.zevora.monetization.AdMobManager.isAdEligible(repo.premiumSubscription.value.isPremium))
  }

  @Test
  fun testAdminPanelUsersPostsReportsAndSparkConfig() {
    val repo = ZevoraRepository()

    // Spark Free Plan zero-cost defaults
    assertTrue(repo.sparkMvpConfig.value.sparkZeroCostMode)
    assertTrue(repo.sparkMvpConfig.value.mediaUploadPending)
    assertTrue(repo.sparkMvpConfig.value.googleAdMobTestMode)
    assertFalse(repo.sparkMvpConfig.value.maintenanceMode)

    // Admin toggles user verification & suspension
    val targetUser = repo.managedUsers.value.first()
    val wasVerified = targetUser.user.isVerified
    repo.adminToggleUserVerified(targetUser.user.id)
    assertEquals(!wasVerified, repo.managedUsers.value.first { it.user.id == targetUser.user.id }.user.isVerified)

    repo.adminToggleUserSuspended(targetUser.user.id)
    assertTrue(repo.managedUsers.value.first { it.user.id == targetUser.user.id }.isSuspended)

    // Reporting a post creates a PENDING ContentReport and Admin Resolve+Remove deletes it
    val post = repo.createPost("Sample flagged post", emptyList(), "Delhi", emptyList())
    repo.reportPost(post.id, "Spam or misleading", "Reported during moderation test")
    val report = repo.contentReports.value.firstOrNull { it.targetId == post.id }
    assertTrue(report != null)
    assertEquals(com.example.zevora.model.ReportStatus.PENDING, report!!.status)

    repo.adminResolveReport(report.id, removeTargetPost = true)
    assertEquals(
      com.example.zevora.model.ReportStatus.RESOLVED,
      repo.contentReports.value.first { it.id == report.id }.status
    )
    assertFalse(repo.feedPosts.value.any { it.id == post.id })
  }

  @Test
  fun testReelCommentAndIdempotentConcurrentGoogleSession() = kotlinx.coroutines.runBlocking {
    val repo = ZevoraRepository()
    val viewModel = ZevoraViewModel(repo)

    val targetReel = repo.reels.value.first()
    val initialCount = targetReel.commentsCount
    val initialListSize = targetReel.comments.size

    viewModel.addReelComment(targetReel.id, "Superb reel! 🔥")
    val updatedReel = repo.reels.value.first { it.id == targetReel.id }
    assertEquals(initialCount + 1, updatedReel.commentsCount)
    assertEquals(initialListSize + 1, updatedReel.comments.size)
    assertTrue(updatedReel.comments.any { it.text == "Superb reel! 🔥" })

    // Concurrent resolution for the same UID must safely deduplicate without double-adding/corruption
    val job1 = async {
      repo.resolveGoogleFirebaseSession(
        uid = "concurrent_uid_999",
        displayName = "Vexora Creator",
        email = "creator.vexora@gmail.com",
        photoUrl = null
      )
    }
    val job2 = async {
      repo.resolveGoogleFirebaseSession(
        uid = "concurrent_uid_999",
        displayName = "Vexora Creator",
        email = "creator.vexora@gmail.com",
        photoUrl = null
      )
    }
    job1.await()
    job2.await()

    assertTrue(repo.isAuthenticated.value)
    assertEquals("concurrent_uid_999", repo.currentUser.value.id)
    assertEquals("Vexora Creator", repo.currentUser.value.name)
  }

  @Test
  fun testPhoneOtpValidationMaskingAndDualProviderSession() = kotlinx.coroutines.runBlocking {
    // India +91 formatting & validation
    assertEquals(
      "+919876543210",
      PhoneAuthManager.validateAndFormatPhone("+91", "9876543210").getOrNull()
    )
    assertEquals(
      "+919876543210",
      PhoneAuthManager.validateAndFormatPhone("+91", "09876543210").getOrNull()
    )
    assertTrue(PhoneAuthManager.validateAndFormatPhone("+91", "98765").isFailure)
    assertEquals("+9198••••3210", PhoneAuthManager.maskPhoneNumber("+919876543210"))
    assertEquals(60_000L, PhoneAuthManager.resendCooldownMillis(0))
    assertEquals(60_000L, PhoneAuthManager.resendCooldownMillis(1))
    assertEquals(90_000L, PhoneAuthManager.resendCooldownMillis(2))
    assertEquals(3, PhoneAuthManager.MAX_OTP_RESENDS)

    // Phone-only verified Firebase session opens Home without fake Google email
    val repo = ZevoraRepository()
    val viewModel = ZevoraViewModel(repo)
    repo.resolveFirebaseSession(
      uid = "phone_uid_555",
      displayName = null,
      email = null,
      phoneNumber = "+919876543210",
      photoUrl = null,
      providerIds = listOf("phone")
    )

    assertTrue(viewModel.isAuthenticated.value)
    assertTrue(viewModel.firebaseDiagnostics.value.isPhoneLinked)
    assertFalse(viewModel.firebaseDiagnostics.value.isGoogleLinked)
    assertEquals("+919876543210", viewModel.firebaseDiagnostics.value.verifiedPhoneNumber)
    assertEquals("", viewModel.currentUser.value.email)
    assertEquals("+919876543210", viewModel.currentUser.value.phone)
    assertEquals(Screen.HOME, viewModel.currentScreen.value)

    // Verified Google linking on the same Firebase UID upgrades both provider flags cleanly
    repo.resolveFirebaseSession(
      uid = "phone_uid_555",
      displayName = "Manohar Kumar",
      email = "manohar.vexora@gmail.com",
      phoneNumber = "+919876543210",
      photoUrl = "https://example.com/photo.jpg",
      providerIds = listOf("phone", "google.com")
    )

    assertTrue(viewModel.firebaseDiagnostics.value.isPhoneLinked)
    assertTrue(viewModel.firebaseDiagnostics.value.isGoogleLinked)
    assertEquals("Manohar Kumar", viewModel.currentUser.value.name)
    assertEquals("manohar.vexora@gmail.com", viewModel.currentUser.value.email)
    assertEquals("+919876543210", viewModel.currentUser.value.phone)

    // Sign out preserves data and clears linked auth flags
    viewModel.clearAuthenticatedSession()
    assertFalse(viewModel.isAuthenticated.value)
    assertFalse(viewModel.firebaseDiagnostics.value.isPhoneLinked)
    assertFalse(viewModel.firebaseDiagnostics.value.isGoogleLinked)
  }
}
