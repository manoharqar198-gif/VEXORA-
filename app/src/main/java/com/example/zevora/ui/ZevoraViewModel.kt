package com.example.zevora.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zevora.data.ZevoraRepository
import com.example.zevora.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class Screen {
  HOME,
  REELS,
  WATCH,
  CHAT,
  PROFILE,
  SECURITY_PRIVACY,
  SETTINGS,
  CREATOR_STUDIO,
  LIVE_STREAM,
  CREATE_POST,
  CREATE_REEL,
  CREATE_STORY,
  CREATOR_WALLET,
  PREMIUM_UPGRADE,
  ADMIN_PANEL
}

class ZevoraViewModel(
  private val repository: ZevoraRepository = ZevoraRepository.instance
) : ViewModel() {

  val authOperationScope: CoroutineScope
    get() = viewModelScope

  private val _currentScreen = MutableStateFlow(Screen.HOME)
  val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

  // Navigation history stack for BackHandler
  private val screenStack = mutableListOf<Screen>(Screen.HOME)

  val currentUser = repository.currentUser
  val isPrivateAccount = repository.isPrivateAccount
  val blockedUsers = repository.blockedUsers
  val commentControl = repository.commentControl
  val filteredKeywords = repository.filteredKeywords
  val appLanguage = repository.appLanguage
  val appThemeMode = repository.appThemeMode
  val notificationSettings = repository.notificationSettings

  val feedPosts = repository.feedPosts
  val stories = repository.stories
  val reels = repository.reels
  val videos = repository.videos
  val chatConversations = repository.chatConversations
  val activeLiveRoom = repository.activeLiveRoom
  val liveComments = repository.liveComments
  val networkQuality = repository.networkQuality
  val isOffline = repository.isOffline
  val dataSaverPreferences = repository.dataSaverPreferences
  val activeUploadTask = repository.activeUploadTask
  val isFeedRefreshing = repository.isFeedRefreshing
  val isInitialFeedLoading = repository.isInitialFeedLoading

  // Monetization Flows
  val userWallet = repository.userWallet
  val transactions = repository.transactions
  val virtualGifts = repository.virtualGifts
  val coinPacks = repository.coinPacks
  val premiumSubscription = repository.premiumSubscription
  val monetizationApplication = repository.monetizationApplication
  val withdrawalRequests = repository.withdrawalRequests
  val adminMetrics = repository.adminMetrics
  val activeLiveGift = repository.activeLiveGift
  val firebaseDiagnostics = repository.firebaseDiagnostics
  val isAuthenticated = repository.isAuthenticated
  val isAuthLoading = repository.isAuthLoading
  val authErrorMessage = repository.authErrorMessage
  val phoneOtpFlowState = repository.phoneOtpFlowState
  val isAccountLinking = repository.isAccountLinking
  val accountLinkMessage = repository.accountLinkMessage
  val accountLinkError = repository.accountLinkError
  val managedUsers = repository.managedUsers
  val contentReports = repository.contentReports
  val sparkMvpConfig = repository.sparkMvpConfig

  fun attachFirestoreManager(manager: com.example.zevora.data.firebase.FirestoreManager) {
    repository.attachFirestoreManager(manager)
  }

  fun setAuthLoading(loading: Boolean) = repository.setAuthLoading(loading)

  fun setAuthError(message: String?) = repository.setAuthError(message)

  fun clearAuthError() = repository.clearAuthError()

  fun markSendingOtp(e164Phone: String) = repository.markSendingOtp(e164Phone)

  fun markOtpSent(
    e164Phone: String,
    maskedPhone: String,
    verificationId: String,
    cooldownMillis: Long = 60_000L,
    resendCount: Int = 0
  ) = repository.markOtpSent(e164Phone, maskedPhone, verificationId, cooldownMillis, resendCount)

  fun markVerifyingOtp(
    e164Phone: String,
    maskedPhone: String,
    verificationId: String,
    resendCount: Int = 0
  ) = repository.markVerifyingOtp(e164Phone, maskedPhone, verificationId, resendCount)

  fun restoreOtpSentState(
    e164Phone: String,
    maskedPhone: String,
    verificationId: String,
    canResendAtMillis: Long,
    resendCount: Int = 0
  ) = repository.restoreOtpSentState(e164Phone, maskedPhone, verificationId, canResendAtMillis, resendCount)

  fun resetPhoneOtpFlow() = repository.resetPhoneOtpFlow()

  fun setAccountLinking(linking: Boolean) = repository.setAccountLinking(linking)

  fun setAccountLinkSuccess(message: String) = repository.setAccountLinkSuccess(message)

  fun setAccountLinkError(message: String?) = repository.setAccountLinkError(message)

  fun clearAccountLinkFeedback() = repository.clearAccountLinkFeedback()

  fun resolveGoogleFirebaseSession(
    uid: String,
    displayName: String?,
    email: String?,
    photoUrl: String?
  ) {
    viewModelScope.launch {
      repository.resolveGoogleFirebaseSession(
        uid = uid,
        displayName = displayName,
        email = email,
        photoUrl = photoUrl
      )
    }
  }

  fun resolveFirebaseSession(
    uid: String,
    displayName: String?,
    email: String?,
    phoneNumber: String?,
    photoUrl: String?,
    providerIds: List<String> = emptyList()
  ) {
    viewModelScope.launch {
      repository.resolveFirebaseSession(
        uid = uid,
        displayName = displayName,
        email = email,
        phoneNumber = phoneNumber,
        photoUrl = photoUrl,
        providerIds = providerIds
      )
    }
  }

  fun clearAuthenticatedSession() {
    repository.clearAuthenticatedSession()
    screenStack.clear()
    screenStack.add(Screen.HOME)
    _currentScreen.value = Screen.HOME
  }

  fun runLiveFirebaseDiagnostics() {
    viewModelScope.launch {
      repository.runLiveFirebaseDiagnostics()
    }
  }

  // Active chat conversation
  private val _activeChat = MutableStateFlow<ChatConversation?>(null)
  val activeChat: StateFlow<ChatConversation?> = _activeChat.asStateFlow()

  // Navigation
  fun navigateTo(screen: Screen) {
    if (_currentScreen.value != screen) {
      screenStack.add(screen)
      _currentScreen.value = screen
    }
  }

  fun navigateBack(): Boolean {
    if (_activeChat.value != null) {
      _activeChat.value = null
      return true
    }
    if (screenStack.size > 1) {
      screenStack.removeAt(screenStack.lastIndex)
      _currentScreen.value = screenStack.last()
      return true
    }
    return false
  }

  fun openChatConversation(conversation: ChatConversation) {
    _activeChat.value = conversation
    navigateTo(Screen.CHAT)
  }

  fun closeChatConversation() {
    _activeChat.value = null
  }

  // Security & Privacy actions
  fun togglePrivateAccount(isPrivate: Boolean) {
    repository.togglePrivateAccount(isPrivate)
    viewModelScope.launch {
      if (repository.hasActiveCloudSession()) {
        repository.syncSecuritySettingsToFirestore()
      }
    }
  }
  fun blockUser(username: String, name: String) = repository.blockUser(username, name)
  fun unblockUser(id: String) = repository.unblockUser(id)
  fun reportUser(userId: String, reason: String, notes: String) = repository.reportUser(userId, reason, notes)
  fun reportPost(postId: String, reason: String, notes: String) = repository.reportPost(postId, reason, notes)
  fun setCommentControl(option: CommentControlOption) = repository.setCommentControl(option)
  fun addFilteredKeyword(word: String) = repository.addFilteredKeyword(word)
  fun removeFilteredKeyword(word: String) = repository.removeFilteredKeyword(word)

  // Settings
  fun setLanguage(lang: AppLanguage) = repository.setLanguage(lang)
  fun setThemeMode(mode: AppThemeMode) = repository.setThemeMode(mode)
  fun updateNotificationSettings(settings: NotificationSettings) = repository.updateNotificationSettings(settings)
  fun deleteAccount(reason: String): Boolean = repository.deleteAccount(reason)
  fun updateCurrentProfile(name: String, bio: String) = repository.updateCurrentProfile(name, bio)

  // Social
  fun togglePostLike(postId: String) = repository.togglePostLike(postId)
  fun togglePostSave(postId: String) = repository.togglePostSave(postId)
  fun toggleFollowUser(userId: String) = repository.toggleFollowUser(userId)
  fun addComment(postId: String, text: String) = repository.addPostComment(postId, text)
  fun toggleReelLike(reelId: String) = repository.toggleReelLike(reelId)
  fun addReelComment(reelId: String, text: String) = repository.addReelComment(reelId, text)
  fun toggleVideoSubscribe(videoId: String) = repository.toggleVideoSubscribe(videoId)
  fun sendMessage(conversationId: String, text: String) = repository.sendChatMessage(conversationId, text)

  // Live Streaming Actions
  fun startLiveStream(title: String) {
    repository.startLiveStream(title)
    navigateTo(Screen.LIVE_STREAM)
  }

  fun endLiveStream(saveRecording: Boolean = true) {
    repository.endLiveStream(saveRecording)
    navigateBack()
  }

  fun addGuestToLive(user: User) = repository.addGuestToLive(user)
  fun removeGuestFromLive(guestId: String) = repository.removeGuestFromLive(guestId)
  fun toggleGuestMute(guestId: String) = repository.toggleGuestMute(guestId)
  fun sendLiveComment(text: String) = repository.sendLiveComment(text, isHost = true)
  fun sendLiveHeart() = repository.sendLiveHeart()
  fun reportLiveUser(username: String, reason: String) = repository.reportLiveUser(username, reason)
  fun blockLiveUser(user: User) = repository.blockLiveUser(user)

  // Publishing Actions
  fun createPost(content: String, mediaUrls: List<String>, location: String, hashtags: List<String>) {
    simulateUpload(title = "पोस्ट सेव हो रही है...", type = "POST") {
      repository.createPost(content, mediaUrls, location, hashtags)
      viewModelScope.launch {
        if (repository.hasActiveCloudSession()) {
          repository.syncPostToFirestore(content)
        }
      }
    }
    _currentScreen.value = Screen.HOME
  }

  fun createReel(caption: String, musicTitle: String) {
    simulateUpload(title = "रील्स वीडियो प्रोसेस हो रहा है...", type = "REEL") {
      repository.createReel(caption, musicTitle)
    }
    _currentScreen.value = Screen.REELS
  }

  fun createStory(caption: String, mediaUrl: String) {
    simulateUpload(title = "स्टोरी अपलोड हो रही है...", type = "STORY") {
      repository.createStory(caption, mediaUrl)
    }
    _currentScreen.value = Screen.HOME
  }

  fun simulateUpload(title: String, type: String, onDone: () -> Unit) {
    viewModelScope.launch {
      val task = UploadTask(
        id = "up_${System.currentTimeMillis()}",
        title = title,
        type = type,
        progress = 0.2f
      )
      repository.updateUploadProgress(task)
      kotlinx.coroutines.delay(350)
      repository.updateUploadProgress(task.copy(progress = 0.55f))
      kotlinx.coroutines.delay(450)
      repository.updateUploadProgress(task.copy(progress = 0.88f))
      kotlinx.coroutines.delay(350)
      repository.updateUploadProgress(task.copy(progress = 1.0f, isCompleted = true))
      kotlinx.coroutines.delay(400)
      repository.cancelUpload()
      onDone()
    }
  }

  fun refreshFeed() = repository.refreshFeed()
  fun loadMorePosts() = repository.loadMorePosts()
  fun updateDataSaverPreferences(prefs: DataSaverPreferences) = repository.updateDataSaverPreferences(prefs)
  fun setSimulatedNetwork(quality: NetworkQuality?) = repository.setSimulatedNetwork(quality)
  fun cancelUpload() = repository.cancelUpload()
  fun clearLocalCache(): String = repository.clearLocalCache()

  fun saveStoryToHighlights(storyId: String) = repository.saveStoryToHighlights(storyId)
  fun recordStoryView(storyId: String) = repository.recordStoryView(storyId)

  // Monetization Actions
  fun purchaseCoins(packId: String, orderId: String): Boolean =
    repository.purchaseCoins(packId, orderId)

  fun sendLiveGift(giftId: String, recipientName: String): Pair<Boolean, String> =
    repository.sendLiveGift(giftId, recipientName)

  fun clearActiveLiveGift() = repository.clearActiveLiveGift()

  fun subscribeToPremium(plan: SubscriptionPlan, orderId: String): Boolean =
    repository.subscribeToPremium(plan, orderId)

  fun cancelPremiumSubscription() = repository.cancelPremiumSubscription()

  fun submitMonetizationApplication(panNumber: String, legalName: String, category: String): Pair<Boolean, String> =
    repository.submitMonetizationApplication(panNumber, legalName, category)

  fun updatePayoutDetails(method: PayoutMethod, upiId: String?, bankDetails: BankAccountDetails?): Boolean =
    repository.updatePayoutDetails(method, upiId, bankDetails)

  fun requestWithdrawal(amountRupees: Double): Pair<Boolean, String> =
    repository.requestWithdrawal(amountRupees)

  fun adminApproveMonetization(applicationId: String) =
    repository.adminApproveMonetization(applicationId)

  fun adminRejectMonetization(applicationId: String, reason: String) =
    repository.adminRejectMonetization(applicationId, reason)

  fun adminApproveWithdrawal(requestId: String, utrNumber: String) =
    repository.adminApproveWithdrawal(requestId, utrNumber)

  fun adminRejectWithdrawal(requestId: String, reason: String) =
    repository.adminRejectWithdrawal(requestId, reason)

  fun adminUpdateCommissionShare(creatorPercent: Double) =
    repository.adminUpdateCommissionShare(creatorPercent)

  fun creditReelsAdShare(viewsCount: Int) =
    repository.creditReelsAdShare(viewsCount)

  // Admin Panel (Users, Posts, Reports, Spark App Settings)
  fun adminToggleUserVerified(userId: String) = repository.adminToggleUserVerified(userId)
  fun adminToggleUserSuspended(userId: String) = repository.adminToggleUserSuspended(userId)

  fun adminSyncUsersFromFirestore(onResult: (Result<Int>) -> Unit = {}) {
    viewModelScope.launch {
      val res = repository.syncAdminUsersFromFirestore()
      onResult(res)
    }
  }

  fun adminDeletePost(postId: String) {
    viewModelScope.launch {
      repository.adminDeletePost(postId)
    }
  }

  fun adminResolveReport(reportId: String, removeTargetPost: Boolean = false) =
    repository.adminResolveReport(reportId, removeTargetPost)

  fun adminDismissReport(reportId: String) =
    repository.adminDismissReport(reportId)

  fun adminUpdateSparkMvpConfig(config: SparkMvpConfig) =
    repository.adminUpdateSparkMvpConfig(config)
}
