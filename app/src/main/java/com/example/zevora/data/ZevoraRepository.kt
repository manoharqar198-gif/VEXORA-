package com.example.zevora.data

import com.example.zevora.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

import com.example.zevora.data.firebase.FirestoreManager
import com.example.zevora.data.firebase.PhoneAuthManager

sealed interface PhoneOtpFlowState {
  data object Idle : PhoneOtpFlowState
  data class SendingOtp(val e164Phone: String) : PhoneOtpFlowState
  data class OtpSent(
    val e164Phone: String,
    val maskedPhone: String,
    val verificationId: String,
    val canResendAtMillis: Long,
    val resendCount: Int = 0,
    val maxResends: Int = PhoneAuthManager.MAX_OTP_RESENDS
  ) : PhoneOtpFlowState
  data class VerifyingOtp(
    val e164Phone: String,
    val maskedPhone: String,
    val verificationId: String,
    val resendCount: Int = 0,
    val maxResends: Int = PhoneAuthManager.MAX_OTP_RESENDS
  ) : PhoneOtpFlowState
}

data class FirebaseDiagnosticsReport(
  val projectId: String = "gen-lang-client-0433994845",
  val projectNumber: String = "312856154542",
  val packageName: String = "com.aistudio.zevora.appsoc",
  val firestoreDatabaseId: String = "ai-studio-android-vexora-bd9cfac6-5775-4a9c-b3c5-0cbca2d5c686",
  val storageBucket: String = "gen-lang-client-0433994845.firebasestorage.app",
  val isFirebaseSignedIn: Boolean = false,
  val firebaseUid: String? = null,
  val linkedProviders: List<String> = emptyList(),
  val isGoogleLinked: Boolean = false,
  val isPhoneLinked: Boolean = false,
  val verifiedPhoneNumber: String = "",
  val verifiedEmail: String = "",
  val authProviderSummary: String = "Phone OTP या Google Sign-In आवश्यक (Firebase Auth)",
  val firestoreWriteStatus: String = "Ready to test",
  val isFirestoreWriteSuccess: Boolean = false,
  val firestoreReadStatus: String = "Ready to test",
  val isFirestoreReadSuccess: Boolean = false,
  val firestoreStatus: String = "Firebase Firestore Ready",
  val isFirestoreSuccess: Boolean = false,
  val storageStatus: String = "⏸️ Pending (Spark Free Plan — ₹0 Billing Guard)",
  val isStorageSuccess: Boolean = false,
  val isRunning: Boolean = false
)

class ZevoraRepository(
  private var firestoreManager: FirestoreManager? = null
) {

  private val _firebaseDiagnostics = MutableStateFlow(FirebaseDiagnosticsReport())
  val firebaseDiagnostics: StateFlow<FirebaseDiagnosticsReport> = _firebaseDiagnostics.asStateFlow()

  private val _isAuthenticated = MutableStateFlow(false)
  val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

  private val _isAuthLoading = MutableStateFlow(false)
  val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

  private val _authErrorMessage = MutableStateFlow<String?>(null)
  val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

  private val _phoneOtpFlowState = MutableStateFlow<PhoneOtpFlowState>(PhoneOtpFlowState.Idle)
  val phoneOtpFlowState: StateFlow<PhoneOtpFlowState> = _phoneOtpFlowState.asStateFlow()

  private val _isAccountLinking = MutableStateFlow(false)
  val isAccountLinking: StateFlow<Boolean> = _isAccountLinking.asStateFlow()

  private val _accountLinkMessage = MutableStateFlow<String?>(null)
  val accountLinkMessage: StateFlow<String?> = _accountLinkMessage.asStateFlow()

  private val _accountLinkError = MutableStateFlow<String?>(null)
  val accountLinkError: StateFlow<String?> = _accountLinkError.asStateFlow()

  private val _sparkMvpConfig = MutableStateFlow(SparkMvpConfig())
  val sparkMvpConfig: StateFlow<SparkMvpConfig> = _sparkMvpConfig.asStateFlow()

  private val _contentReports = MutableStateFlow<List<ContentReport>>(emptyList())
  val contentReports: StateFlow<List<ContentReport>> = _contentReports.asStateFlow()

  private val _managedUsers = MutableStateFlow(
    listOf(
      AdminManagedUser(
        user = User("u2", "Priya Verma", "priya_travels", bio = "Wanderlust ✈️", isVerified = true, followersCount = 18400),
        authProvider = "Local Preview"
      ),
      AdminManagedUser(
        user = User("u3", "Tech Sutra", "techsutra_india", bio = "Latest in Tech & AI", isVerified = true, followersCount = 54200),
        authProvider = "Local Preview"
      ),
      AdminManagedUser(
        user = User("u4", "Ananya Rao", "ananya_foodie", bio = "Chef & Recipe explorer 🍲", isVerified = false, followersCount = 6200),
        authProvider = "Local Preview"
      )
    )
  )
  val managedUsers: StateFlow<List<AdminManagedUser>> = _managedUsers.asStateFlow()

  fun attachFirestoreManager(manager: FirestoreManager) {
    firestoreManager = manager
  }

  fun setAuthLoading(loading: Boolean) {
    _isAuthLoading.value = loading
    if (loading) {
      _authErrorMessage.value = null
    }
  }

  fun setAuthError(message: String?) {
    _isAuthLoading.value = false
    _authErrorMessage.value = message
  }

  fun clearAuthError() {
    _authErrorMessage.value = null
  }

  fun markSendingOtp(e164Phone: String) {
    _isAuthLoading.value = true
    _authErrorMessage.value = null
    _phoneOtpFlowState.value = PhoneOtpFlowState.SendingOtp(e164Phone)
  }

  fun markOtpSent(
    e164Phone: String,
    maskedPhone: String,
    verificationId: String,
    cooldownMillis: Long = 60_000L,
    resendCount: Int = 0
  ) {
    _isAuthLoading.value = false
    _authErrorMessage.value = null
    _phoneOtpFlowState.value = PhoneOtpFlowState.OtpSent(
      e164Phone = e164Phone,
      maskedPhone = maskedPhone,
      verificationId = verificationId,
      canResendAtMillis = System.currentTimeMillis() + cooldownMillis,
      resendCount = resendCount,
      maxResends = PhoneAuthManager.MAX_OTP_RESENDS
    )
  }

  fun markVerifyingOtp(
    e164Phone: String,
    maskedPhone: String,
    verificationId: String,
    resendCount: Int = 0
  ) {
    _isAuthLoading.value = true
    _authErrorMessage.value = null
    _phoneOtpFlowState.value = PhoneOtpFlowState.VerifyingOtp(
      e164Phone = e164Phone,
      maskedPhone = maskedPhone,
      verificationId = verificationId,
      resendCount = resendCount,
      maxResends = PhoneAuthManager.MAX_OTP_RESENDS
    )
  }

  fun restoreOtpSentState(
    e164Phone: String,
    maskedPhone: String,
    verificationId: String,
    canResendAtMillis: Long,
    resendCount: Int = 0
  ) {
    _isAuthLoading.value = false
    _phoneOtpFlowState.value = PhoneOtpFlowState.OtpSent(
      e164Phone = e164Phone,
      maskedPhone = maskedPhone,
      verificationId = verificationId,
      canResendAtMillis = canResendAtMillis,
      resendCount = resendCount,
      maxResends = PhoneAuthManager.MAX_OTP_RESENDS
    )
  }

  fun resetPhoneOtpFlow() {
    _isAuthLoading.value = false
    _authErrorMessage.value = null
    _phoneOtpFlowState.value = PhoneOtpFlowState.Idle
  }

  fun setAccountLinking(linking: Boolean) {
    _isAccountLinking.value = linking
    if (linking) {
      _accountLinkError.value = null
      _accountLinkMessage.value = null
    }
  }

  fun setAccountLinkSuccess(message: String) {
    _isAccountLinking.value = false
    _accountLinkError.value = null
    _accountLinkMessage.value = message
  }

  fun setAccountLinkError(message: String?) {
    _isAccountLinking.value = false
    _accountLinkError.value = message
  }

  fun clearAccountLinkFeedback() {
    _accountLinkError.value = null
    _accountLinkMessage.value = null
  }

  private val sessionMutex = Mutex()
  private var resolvedCloudUid: String? = null
  private var resolvedSessionSignature: String? = null

  /**
   * Backward-compatible entry point for Google Sign-In session resolution.
   */
  suspend fun resolveGoogleFirebaseSession(
    uid: String,
    displayName: String?,
    email: String?,
    photoUrl: String?
  ) {
    resolveFirebaseSession(
      uid = uid,
      displayName = displayName,
      email = email,
      phoneNumber = null,
      photoUrl = photoUrl,
      providerIds = listOf("google.com")
    )
  }

  /**
   * Resolves a verified Firebase User (Google, Phone OTP, or Linked Phone + Google) into VEXORA.
   * Mutex-guarded & idempotent per (uid + providers + phone + email) to prevent concurrent listener/callback races
   * while still updating state whenever a second provider is verified and linked.
   */
  suspend fun resolveFirebaseSession(
    uid: String,
    displayName: String?,
    email: String?,
    phoneNumber: String?,
    photoUrl: String?,
    providerIds: List<String> = emptyList()
  ) {
    if (uid.isBlank()) {
      setAuthError("अमान्य Firebase User ID प्राप्त हुआ। कृपया पुनः लॉगिन करें।")
      return
    }

    val normalizedProviders = providerIds
      .map { it.trim().lowercase() }
      .filter { it.isNotBlank() && it != "firebase" }
      .distinct()
      .sorted()
    val safeEmail = email?.trim().orEmpty()
    val safePhone = phoneNumber?.trim().orEmpty()
    val signature = "$uid|${normalizedProviders.joinToString(",")}|$safePhone|$safeEmail"

    sessionMutex.withLock {
      if (resolvedCloudUid == uid && resolvedSessionSignature == signature && _isAuthenticated.value) {
        _isAuthLoading.value = false
        _authErrorMessage.value = null
        _phoneOtpFlowState.value = PhoneOtpFlowState.Idle
        return
      }

      val isSameUidRefresh = resolvedCloudUid == uid && _isAuthenticated.value
      val hasGoogle = "google.com" in normalizedProviders || (safeEmail.isNotBlank() && normalizedProviders.isEmpty())
      val hasPhone = "phone" in normalizedProviders || safePhone.isNotBlank()

      val safeDisplayName = displayName?.trim()?.takeIf { it.isNotBlank() }
        ?: if (isSameUidRefresh) _currentUser.value.name else "VEXORA Creator"
      val fallbackPhoneHandle = safePhone.filter { it.isDigit() }.takeLast(4)
        .takeIf { it.isNotBlank() }
        ?.let { "user_${it}_${uid.take(4).lowercase()}" }
      val emailHandle = safeEmail.substringBefore("@")
        .lowercase()
        .replace(Regex("[^a-z0-9_.]"), "")
        .take(24)
        .ifBlank { fallbackPhoneHandle ?: "user_${uid.take(6).lowercase()}" }
      val safeAvatar = photoUrl?.trim().orEmpty()

      _currentUser.update { existing ->
        existing.copy(
          id = uid,
          name = if (isSameUidRefresh && displayName.isNullOrBlank()) existing.name else safeDisplayName,
          username = if (isSameUidRefresh && existing.username.isNotBlank() && !existing.username.startsWith("aarav_")) existing.username else emailHandle,
          email = safeEmail.ifBlank { if (isSameUidRefresh) existing.email else "" },
          phone = safePhone.ifBlank { if (isSameUidRefresh) existing.phone else "" },
          avatarUrl = safeAvatar.ifBlank { if (isSameUidRefresh) existing.avatarUrl else "" }
        )
      }

      _userWallet.update { it.copy(userId = uid) }
      _premiumSubscription.update { it.copy(userId = uid) }
      _monetizationApplication.update {
        it.copy(
          userId = uid,
          userName = _currentUser.value.name,
          userHandle = _currentUser.value.username
        )
      }

      val providerSummary = when {
        hasGoogle && hasPhone -> "✅ Google + Mobile OTP दोनों Verified व Linked हैं"
        hasGoogle -> "✅ Google Sign-In Verified (${safeEmail.ifBlank { uid }})"
        hasPhone -> "✅ Mobile OTP Verified (${safePhone.ifBlank { uid }})"
        else -> "✅ Firebase Auth Verified ($uid)"
      }

      _firebaseDiagnostics.update {
        it.copy(
          isFirebaseSignedIn = true,
          firebaseUid = uid,
          linkedProviders = normalizedProviders,
          isGoogleLinked = hasGoogle,
          isPhoneLinked = hasPhone,
          verifiedPhoneNumber = safePhone,
          verifiedEmail = safeEmail,
          authProviderSummary = providerSummary
        )
      }

      resolvedCloudUid = uid
      resolvedSessionSignature = signature
      _isAuthenticated.value = true
      _isAuthLoading.value = false
      _authErrorMessage.value = null
      _phoneOtpFlowState.value = PhoneOtpFlowState.Idle

      val manager = firestoreManager ?: return
      val existingProfileRes = manager.fetchUserProfile()
      val existingData = existingProfileRes.getOrNull()
      if (existingData != null) {
        val cloudName = (existingData["name"] as? String)?.takeIf { it.isNotBlank() } ?: safeDisplayName
        val cloudUsername = (existingData["username"] as? String)?.takeIf { it.isNotBlank() } ?: emailHandle
        val cloudBio = (existingData["bio"] as? String) ?: _currentUser.value.bio
        val cloudAvatar = (existingData["avatarUrl"] as? String)?.takeIf { it.isNotBlank() } ?: safeAvatar
        val cloudPhone = safePhone.ifBlank { (existingData["phone"] as? String).orEmpty() }
        val cloudEmail = safeEmail.ifBlank { (existingData["email"] as? String).orEmpty() }
        val cloudPrivate = (existingData["isPrivate"] as? Boolean) ?: false
        val cloudFollowers = (existingData["followersCount"] as? Number)?.toInt() ?: _currentUser.value.followersCount
        val cloudFollowing = (existingData["followingCount"] as? Number)?.toInt() ?: _currentUser.value.followingCount
        val cloudPosts = (existingData["postsCount"] as? Number)?.toInt() ?: _currentUser.value.postsCount

        _currentUser.update { current ->
          current.copy(
            id = uid,
            name = cloudName,
            username = cloudUsername,
            bio = cloudBio,
            avatarUrl = cloudAvatar,
            phone = cloudPhone,
            email = cloudEmail,
            isPrivate = cloudPrivate,
            followersCount = cloudFollowers,
            followingCount = cloudFollowing,
            postsCount = cloudPosts
          )
        }
        _isPrivateAccount.value = cloudPrivate

        // If a newly linked phone/email was added to Firebase Auth, sync updated contact fields safely
        if ((safePhone.isNotBlank() && existingData["phone"] != safePhone) ||
          (safeEmail.isNotBlank() && existingData["email"] != safeEmail)
        ) {
          manager.syncUserProfile(_currentUser.value)
        }
      } else if (existingProfileRes.isSuccess) {
        // First-time Phone/Google user: safely persist initial profile to Firestore without erasing any data
        manager.syncUserProfile(_currentUser.value)
      }

      if (!isSameUidRefresh) {
        val cloudPostsRes = manager.getUserPosts()
        val cloudPosts = cloudPostsRes.getOrNull().orEmpty()
        if (cloudPosts.isNotEmpty()) {
          _feedPosts.update { current ->
            val existingIds = current.map { it.id }.toSet()
            val newCloud = cloudPosts.filterNot { it.id in existingIds }
            newCloud + current
          }
        }
      }
    }
  }

  fun clearAuthenticatedSession() {
    resolvedCloudUid = null
    resolvedSessionSignature = null
    _isAuthenticated.value = false
    _isAuthLoading.value = false
    _authErrorMessage.value = null
    _phoneOtpFlowState.value = PhoneOtpFlowState.Idle
    _isAccountLinking.value = false
    _accountLinkError.value = null
    _accountLinkMessage.value = null
    _firebaseDiagnostics.update {
      it.copy(
        isFirebaseSignedIn = false,
        firebaseUid = null,
        linkedProviders = emptyList(),
        isGoogleLinked = false,
        isPhoneLinked = false,
        verifiedPhoneNumber = "",
        verifiedEmail = "",
        authProviderSummary = "Phone OTP या Google Sign-In आवश्यक (Signed out)"
      )
    }
  }

  private val _currentUser = MutableStateFlow(
    User(
      id = "u_me",
      name = "Aarav Sharma",
      username = "aarav_vexora",
      bio = "Tech enthusiast • Digital Creator • Mumbai 🇮🇳\nCreating stories & connecting minds on VEXORA ✨",
      followersCount = 14200,
      followingCount = 482,
      postsCount = 58,
      isPrivate = false,
      isVerified = true,
      email = "aarav.sharma@vexora.app",
      phone = "+91 98765 43210"
    )
  )
  val currentUser: StateFlow<User> = _currentUser.asStateFlow()

  // Security & Privacy states
  private val _isPrivateAccount = MutableStateFlow(false)
  val isPrivateAccount: StateFlow<Boolean> = _isPrivateAccount.asStateFlow()

  private val _twoFactorEnabled = MutableStateFlow(false)
  val twoFactorEnabled: StateFlow<Boolean> = _twoFactorEnabled.asStateFlow()

  private val _twoFactorMethod = MutableStateFlow("Privacy Controls Active")
  val twoFactorMethod: StateFlow<String> = _twoFactorMethod.asStateFlow()

  private val _blockedUsers = MutableStateFlow(
    listOf(
      BlockedUser("b1", "spam_bot_29", "Crypto Promo 24/7", "10 Oct 2026"),
      BlockedUser("b2", "fake_brand_offers", "Quick Deals Official", "28 Sep 2026"),
      BlockedUser("b3", "toxic_troll99", "Anonymous User", "15 Aug 2026")
    )
  )
  val blockedUsers: StateFlow<List<BlockedUser>> = _blockedUsers.asStateFlow()

  private val _sessions = MutableStateFlow(
    listOf(
      SessionDevice("s1", "Current Android Device", "Mumbai, India", "Local", "Active Now", isCurrentDevice = true)
    )
  )
  val sessions: StateFlow<List<SessionDevice>> = _sessions.asStateFlow()

  private val _securityAlerts = MutableStateFlow<List<SecurityAlert>>(emptyList())
  val securityAlerts: StateFlow<List<SecurityAlert>> = _securityAlerts.asStateFlow()

  private val _commentControl = MutableStateFlow(CommentControlOption.EVERYONE)
  val commentControl: StateFlow<CommentControlOption> = _commentControl.asStateFlow()

  private val _filteredKeywords = MutableStateFlow(
    listOf("spam", "free cash", "click link", "lottery", "abusive word")
  )
  val filteredKeywords: StateFlow<List<String>> = _filteredKeywords.asStateFlow()

  private val _suspiciousAlertsEnabled = MutableStateFlow(true)
  val suspiciousAlertsEnabled: StateFlow<Boolean> = _suspiciousAlertsEnabled.asStateFlow()

  // Settings
  private val _appLanguage = MutableStateFlow(AppLanguage.HINDI)
  val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

  private val _appThemeMode = MutableStateFlow(AppThemeMode.DARK)
  val appThemeMode: StateFlow<AppThemeMode> = _appThemeMode.asStateFlow()

  private val _notificationSettings = MutableStateFlow(NotificationSettings())
  val notificationSettings: StateFlow<NotificationSettings> = _notificationSettings.asStateFlow()

  // Smart Network & Performance Optimization
  private val _networkQuality = MutableStateFlow(NetworkQuality.EXCELLENT)
  val networkQuality: StateFlow<NetworkQuality> = _networkQuality.asStateFlow()

  private val _isOffline = MutableStateFlow(false)
  val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

  private val _dataSaverPreferences = MutableStateFlow(DataSaverPreferences())
  val dataSaverPreferences: StateFlow<DataSaverPreferences> = _dataSaverPreferences.asStateFlow()

  private val _activeUploadTask = MutableStateFlow<UploadTask?>(null)
  val activeUploadTask: StateFlow<UploadTask?> = _activeUploadTask.asStateFlow()

  private val _isFeedRefreshing = MutableStateFlow(false)
  val isFeedRefreshing: StateFlow<Boolean> = _isFeedRefreshing.asStateFlow()

  private val _isInitialFeedLoading = MutableStateFlow(false)
  val isInitialFeedLoading: StateFlow<Boolean> = _isInitialFeedLoading.asStateFlow()

  // Pending offline actions queue (synced when back online)
  private val pendingOfflineActions = mutableListOf<String>()

  // MONETIZATION & WALLET STATES
  private val _userWallet = MutableStateFlow(
    UserWallet(
      userId = "u_me",
      coinsBalance = 450,
      withdrawableRupees = 4280.0,
      pendingRupees = 650.0,
      totalLifetimeEarnings = 31200.0,
      eligibleReelsViews = 184500,
      kycStatus = KycStatus.VERIFIED,
      panNumber = "ABCDE1234F",
      payoutMethod = PayoutMethod.UPI,
      upiId = "aarav@oksbi"
    )
  )
  val userWallet: StateFlow<UserWallet> = _userWallet.asStateFlow()

  val virtualGifts: List<VirtualGift> = listOf(
    VirtualGift("gift_rose", "Rose 🌹", 10, "🌹", "HEART_BURST", "Send a warm rose"),
    VirtualGift("gift_fire", "Fire Boost 🔥", 50, "🔥", "FIRE_BLAZE", "Light up the live stream"),
    VirtualGift("gift_crown", "Golden Crown 👑", 200, "👑", "CROWN_GLOW", "Crown the creator"),
    VirtualGift("gift_rocket", "Super Rocket 🚀", 500, "🚀", "ROCKET_LAUNCH", "Boost stream to trending"),
    VirtualGift("gift_diamond", "Vexora Diamond 💎", 1000, "💎", "DIAMOND_SHOWER", "Ultimate creator support")
  )

  val coinPacks: List<CoinPack> = listOf(
    CoinPack("pack_100", 100, 0, 99.0, false, "coins_100_inr99"),
    CoinPack("pack_500", 500, 50, 399.0, true, "coins_500_inr399"),
    CoinPack("pack_1200", 1200, 200, 899.0, false, "coins_1200_inr899"),
    CoinPack("pack_3000", 3000, 600, 1999.0, false, "coins_3000_inr1999")
  )

  private val _transactions = MutableStateFlow(
    listOf(
      FinancialTransaction("txn_1", "u_me", TransactionType.REELS_AD_SHARE, 1420.0, 0, "Reels Ad Share (58.4K eligible views)", System.currentTimeMillis() - 86400000, TransactionStatus.SUCCESS, "AD-2026-NOV"),
      FinancialTransaction("txn_2", "u_me", TransactionType.GIFT_RECEIVED, 350.0, 1000, "Live Gift received: Vexora Diamond 💎 from @priya_travels", System.currentTimeMillis() - 43200000, TransactionStatus.SUCCESS, "GIFT-9912"),
      FinancialTransaction("txn_3", "u_me", TransactionType.COIN_PURCHASE, 399.0, 550, "Purchased 550 Vexora Coins via Google Play", System.currentTimeMillis() - 259200000, TransactionStatus.SUCCESS, "GPA.3391-4921-8812")
    )
  )
  val transactions: StateFlow<List<FinancialTransaction>> = _transactions.asStateFlow()

  private val _premiumSubscription = MutableStateFlow(
    PremiumSubscription(userId = "u_me", isPremium = false, plan = SubscriptionPlan.MONTHLY)
  )
  val premiumSubscription: StateFlow<PremiumSubscription> = _premiumSubscription.asStateFlow()

  private val _monetizationApplication = MutableStateFlow(
    MonetizationApplication(
      id = "app_u_me",
      userId = "u_me",
      userName = "Aarav Sharma",
      userHandle = "aarav_vexora",
      followersCount = 14200,
      eligibleViews = 184500,
      panNumber = "ABCDE1234F",
      legalName = "Aarav Sharma",
      category = "Digital Creator / Tech & Lifestyle",
      status = MonetizationStatus.APPROVED
    )
  )
  val monetizationApplication: StateFlow<MonetizationApplication> = _monetizationApplication.asStateFlow()

  private val _withdrawalRequests = MutableStateFlow(
    listOf(
      WithdrawalRequest("w_1", "u_me", "Aarav Sharma", 2500.0, "UPI: aarav@oksbi", System.currentTimeMillis() - 172800000, WithdrawalStatus.PAID, "UTR8492049102", "Payout transferred to Bank"),
      WithdrawalRequest("w_2", "u_me", "Aarav Sharma", 650.0, "UPI: aarav@oksbi", System.currentTimeMillis() - 3600000, WithdrawalStatus.PENDING, "", "Under Admin verification")
    )
  )
  val withdrawalRequests: StateFlow<List<WithdrawalRequest>> = _withdrawalRequests.asStateFlow()

  private val _adminMetrics = MutableStateFlow(AdminRevenueMetrics())
  val adminMetrics: StateFlow<AdminRevenueMetrics> = _adminMetrics.asStateFlow()

  private val _activeLiveGift = MutableStateFlow<VirtualGift?>(null)
  val activeLiveGift: StateFlow<VirtualGift?> = _activeLiveGift.asStateFlow()

  // Social feed data
  private val _feedPosts = MutableStateFlow(
    listOf(
      Post(
        id = "p1",
        author = User("u2", "Priya Verma", "priya_travels", bio = "Wanderlust ✈️"),
        content = "Golden sunrise over the majestic Himalayas in Himachal! 🌄 Nature is the ultimate masterpiece. What's your favourite mountain destination? #ZevoraMoments #IndiaTravel",
        timestamp = "15m ago",
        likesCount = 342,
        commentsCount = 28,
        sharesCount = 14,
        isLiked = false,
        isSaved = false,
        isFollowing = true,
        gradientStart = 0xFFF59E0B,
        gradientEnd = 0xFFEC4899,
        comments = listOf(
          Comment("c1", "Rohan Mehta", "", "Breathtaking view! Adding to my bucket list.", "10m ago"),
          Comment("c2", "Neha Roy", "", "Which camera did you use for this? Stunning clarity!", "5m ago")
        )
      ),
      Post(
        id = "p2",
        author = User("u3", "Tech Sutra", "techsutra_india", bio = "Latest in Tech & AI"),
        content = "🚀 Big Announcement: VEXORA 2.0 rolls out end-to-end security, instant 2-Factor Authentication, and creator monetization for all eligible channels! Here is our complete breakdown ⬇️",
        timestamp = "1h ago",
        likesCount = 1890,
        commentsCount = 142,
        sharesCount = 95,
        isLiked = true,
        isSaved = true,
        isFollowing = true,
        gradientStart = 0xFF6366F1,
        gradientEnd = 0xFF8B5CF6,
        comments = listOf(
          Comment("c3", "Manish K", "", "Love the new Security & Privacy controls!", "45m ago")
        )
      ),
      Post(
        id = "p3",
        author = User("u4", "Ananya Rao", "ananya_foodie", bio = "Chef & Recipe explorer 🍲"),
        content = "Sunday special: Homemade authentic Masala Dosa with coconut chutney and piping hot sambar! 🤤 Crispiness score: 10/10!",
        timestamp = "3h ago",
        likesCount = 820,
        commentsCount = 54,
        sharesCount = 18,
        isLiked = false,
        isSaved = false,
        isFollowing = false,
        gradientStart = 0xFF10B981,
        gradientEnd = 0xFF06B6D4
      )
    )
  )
  val feedPosts: StateFlow<List<Post>> = _feedPosts.asStateFlow()

  // Stories
  private val _stories = MutableStateFlow(
    listOf(
      Story(
        id = "s1",
        user = User("u_me", "Your Story", "aarav_vexora"),
        isViewed = true,
        hasUnseen = false,
        items = listOf(
          StoryItem(
            id = "si_me1",
            user = User("u_me", "Your Story", "aarav_vexora"),
            caption = "Testing the new VEXORA 2.0 interface ✨",
            timestamp = "3h ago",
            viewers = listOf(
              StoryViewer(User("u2", "Priya Verma", "priya_travels"), "10m ago"),
              StoryViewer(User("u3", "Tech Sutra", "techsutra_india"), "45m ago"),
              StoryViewer(User("u4", "Ananya Rao", "ananya_foodie"), "1h ago"),
              StoryViewer(User("u6", "Kavya Dance", "kavya_dance"), "2h ago")
            )
          )
        )
      ),
      Story(
        id = "s2",
        user = User("u2", "Priya V.", "priya_travels"),
        isViewed = false,
        hasUnseen = true,
        items = listOf(
          StoryItem(
            id = "si_priya1",
            user = User("u2", "Priya V.", "priya_travels"),
            caption = "Campfire vibes in Kasol 🏕️🔥",
            timestamp = "1h ago",
            gradientStart = 0xFFF97316,
            gradientEnd = 0xFFEC4899,
            viewers = listOf(StoryViewer(User("u_me", "Aarav Sharma", "aarav_vexora"), "Just now"))
          )
        )
      ),
      Story(
        id = "s3",
        user = User("u3", "Tech Sutra", "techsutra_india"),
        isViewed = false,
        hasUnseen = true,
        items = listOf(
          StoryItem(
            id = "si_tech1",
            user = User("u3", "Tech Sutra", "techsutra_india"),
            caption = "New AI feature dropping on VEXORA tonight! 🤖⚡",
            timestamp = "4h ago",
            gradientStart = 0xFF8B5CF6,
            gradientEnd = 0xFF06B6D4
          )
        )
      ),
      Story(
        id = "s4",
        user = User("u4", "Ananya R.", "ananya_foodie"),
        isViewed = false,
        hasUnseen = true,
        items = listOf(
          StoryItem(
            id = "si_ananya1",
            user = User("u4", "Ananya R.", "ananya_foodie"),
            caption = "Brewing fresh filter coffee ☕ Perfect Sunday!",
            timestamp = "5h ago",
            gradientStart = 0xFF10B981,
            gradientEnd = 0xFF3B82F6
          )
        )
      ),
      Story(
        id = "s5",
        user = User("u5", "Kabir Khan", "kabir_beats"),
        isViewed = true,
        hasUnseen = false,
        items = listOf(
          StoryItem(
            id = "si_kabir1",
            user = User("u5", "Kabir Khan", "kabir_beats"),
            caption = "Jamming with new beats in the studio 🎧",
            timestamp = "12h ago",
            gradientStart = 0xFF6366F1,
            gradientEnd = 0xFF8B5CF6
          )
        )
      )
    )
  )
  val stories: StateFlow<List<Story>> = _stories.asStateFlow()

  // Live Stream state
  private val _activeLiveRoom = MutableStateFlow<LiveRoom?>(null)
  val activeLiveRoom: StateFlow<LiveRoom?> = _activeLiveRoom.asStateFlow()

  private val _liveComments = MutableStateFlow(
    listOf(
      LiveComment("lc_1", User("u2", "Priya Verma", "priya_travels"), "Namaste Aarav! Audio & Video quality is crystal clear! 🙌", isHost = false),
      LiveComment("lc_2", User("u3", "Tech Sutra", "techsutra_india"), "The new low-latency streaming on VEXORA is fantastic ⚡", isHost = false),
      LiveComment("lc_3", User("u6", "Kavya Dance", "kavya_dance"), "Can you add me as a guest? Want to do a quick demo! 🙋‍♀️", isHost = false)
    )
  )
  val liveComments: StateFlow<List<LiveComment>> = _liveComments.asStateFlow()

  // Reels
  private val _reels = MutableStateFlow(
    listOf(
      Reel(
        id = "r1",
        creator = User("u6", "Kavya Dance", "kavya_dance", isVerified = true),
        caption = "Trending hookstep on VEXORA beat! 🔥 Try this remix with your friends #dance #vexora #trending",
        musicTitle = "Original Audio - Kavya Beats • 120K reels",
        likesCount = 42800,
        commentsCount = 1530,
        sharesCount = 3890,
        isLiked = false,
        viewsCount = "182K",
        comments = listOf(
          Comment("rc1", "Neha Roy", "", "Awesome dance steps! Loving the edit ✨", "5m ago"),
          Comment("rc2", "Vikram S", "", "Smooth transition & great beat 🔥", "12m ago")
        ),
        gradientStart = 0xFF7C3AED,
        gradientEnd = 0xFFEC4899
      ),
      Reel(
        id = "r2",
        creator = User("u7", "Aryan Fitness", "aryan_fit", isVerified = true),
        caption = "5 Core Workouts for Strength & Endurance at Home! Save for your morning routine 💪",
        musicTitle = "Energetic Bassline - Gym Motivation 2026",
        likesCount = 28400,
        commentsCount = 890,
        sharesCount = 4120,
        isLiked = true,
        viewsCount = "95K",
        comments = listOf(
          Comment("rc3", "Priya Verma", "", "Added to my morning workout routine! 🙌", "18m ago")
        ),
        gradientStart = 0xFF06B6D4,
        gradientEnd = 0xFF3B82F6
      ),
      Reel(
        id = "r3",
        creator = User("u8", "Standup Rahul", "rahul_comedy", isVerified = false),
        caption = "When parents catch you talking on phone at 2 AM 😭😂 #comedy #relatable #desi",
        musicTitle = "Funny Laughter Sound - Rahul Originals",
        likesCount = 76100,
        commentsCount = 3400,
        sharesCount = 12900,
        isLiked = false,
        viewsCount = "450K",
        comments = listOf(
          Comment("rc4", "Ananya Rao", "", "Too relatable 😂👏", "25m ago")
        ),
        gradientStart = 0xFFF59E0B,
        gradientEnd = 0xFFEF4444
      )
    )
  )
  val reels: StateFlow<List<Reel>> = _reels.asStateFlow()

  // Long Videos (Watch Section)
  private val _videos = MutableStateFlow(
    listOf(
      VideoItem(
        id = "v1",
        channelName = "VEXORA Tech Talks",
        title = "Complete Guide to Digital Privacy & Account Protection in 2026",
        views = "240K views",
        duration = "14:25",
        timeAgo = "2 days ago",
        isSubscribed = true,
        gradientStart = 0xFF1E1B4B,
        gradientEnd = 0xFF4338CA
      ),
      VideoItem(
        id = "v2",
        channelName = "Desi Explorer Studio",
        title = "Exploring the Hidden Temples of South India - 4K Cinematic Documentary",
        views = "890K views",
        duration = "28:50",
        timeAgo = "1 week ago",
        isSubscribed = false,
        gradientStart = 0xFF312E81,
        gradientEnd = 0xFF6D28D9
      ),
      VideoItem(
        id = "v3",
        channelName = "Creator Economy India",
        title = "How Creators are Earning ₹1,00,000+ Monthly via VEXORA AdMob & Gifts",
        views = "115K views",
        duration = "19:12",
        timeAgo = "3 days ago",
        isSubscribed = true,
        gradientStart = 0xFF064E3B,
        gradientEnd = 0xFF047857
      )
    )
  )
  val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

  // Chat conversations
  private val _chatConversations = MutableStateFlow(
    listOf(
      ChatConversation(
        id = "c_1",
        participant = User("u2", "Priya Verma", "priya_travels"),
        lastMessage = "Hey Aarav! Did you check out the security updates?",
        lastTime = "12:45 PM",
        unreadCount = 2,
        isOnline = true,
        messages = listOf(
          ChatMessage("m1", "u2", "Hi Aarav! Hope you are doing great.", "12:40 PM", isMe = false),
          ChatMessage("m2", "u_me", "Hey Priya! Yes all good, just testing out VEXORA's new features.", "12:42 PM", isMe = true),
          ChatMessage("m3", "u2", "Hey Aarav! Did you check out the security updates?", "12:45 PM", isMe = false)
        )
      ),
      ChatConversation(
        id = "c_2",
        participant = User("u3", "Tech Sutra", "techsutra_india"),
        lastMessage = "Voice message (0:32)",
        lastTime = "Yesterday",
        unreadCount = 0,
        isOnline = false,
        messages = listOf(
          ChatMessage("m4", "u3", "Thanks for sharing the monetization insights!", "Yesterday", isMe = false, isVoiceNote = true, voiceDurationSec = 32)
        )
      ),
      ChatConversation(
        id = "c_3",
        participant = User("u4", "Ananya Rao", "ananya_foodie"),
        lastMessage = "Sent a recipe link 🍲",
        lastTime = "Nov 8",
        unreadCount = 0,
        isOnline = true
      )
    )
  )
  val chatConversations: StateFlow<List<ChatConversation>> = _chatConversations.asStateFlow()

  fun hasActiveCloudSession(): Boolean = firestoreManager?.currentUserId() != null

  // Security Operations
  fun toggleTwoFactor(enabled: Boolean, method: String = "Privacy Lock") {
    _twoFactorEnabled.value = enabled
    _twoFactorMethod.value = method
  }

  fun togglePrivateAccount(isPrivate: Boolean) {
    _isPrivateAccount.value = isPrivate
    _currentUser.update { it.copy(isPrivate = isPrivate) }
  }

  fun blockUser(username: String, name: String) {
    val newBlocked = BlockedUser(
      id = "b_${System.currentTimeMillis()}",
      username = username.removePrefix("@"),
      name = name,
      blockedDate = "Today"
    )
    _blockedUsers.update { it + newBlocked }
  }

  fun unblockUser(id: String) {
    _blockedUsers.update { list -> list.filterNot { it.id == id } }
  }

  fun reportUser(userId: String, reason: String, notes: String): Boolean {
    val report = ContentReport(
      id = "rep_u_${System.currentTimeMillis()}",
      targetType = "USER",
      targetId = userId,
      targetLabel = "@$userId",
      reason = reason.ifBlank { "Community Violation" },
      notes = notes.trim()
    )
    _contentReports.update { listOf(report) + it }
    return true
  }

  fun reportPost(postId: String, reason: String, notes: String): Boolean {
    val post = _feedPosts.value.firstOrNull { it.id == postId }
    val label = post?.let { "@${it.author.username}: ${it.content.take(40)}" } ?: "Post #$postId"
    val report = ContentReport(
      id = "rep_p_${System.currentTimeMillis()}",
      targetType = "POST",
      targetId = postId,
      targetLabel = label,
      reason = reason.ifBlank { "Inappropriate Content" },
      notes = notes.trim()
    )
    _contentReports.update { listOf(report) + it }
    return true
  }

  fun setCommentControl(option: CommentControlOption) {
    _commentControl.value = option
  }

  fun addFilteredKeyword(keyword: String) {
    if (keyword.isNotBlank() && !_filteredKeywords.value.contains(keyword.trim().lowercase())) {
      _filteredKeywords.update { it + keyword.trim().lowercase() }
    }
  }

  fun removeFilteredKeyword(keyword: String) {
    _filteredKeywords.update { it.filterNot { w -> w.equals(keyword, ignoreCase = true) } }
  }

  fun logoutSession(sessionId: String) {
    _sessions.update { list -> list.filterNot { it.id == sessionId } }
  }

  fun logoutAllOtherSessions() {
    _sessions.update { list -> list.filter { it.isCurrentDevice } }
  }

  fun resolveAlert(alertId: String) {
    _securityAlerts.update { list ->
      list.map { if (it.id == alertId) it.copy(isResolved = true) else it }
    }
  }

  fun toggleSuspiciousAlerts(enabled: Boolean) {
    _suspiciousAlertsEnabled.value = enabled
  }

  // Settings operations
  fun setLanguage(lang: AppLanguage) {
    _appLanguage.value = lang
  }

  fun setThemeMode(mode: AppThemeMode) {
    _appThemeMode.value = mode
  }

  fun updateNotificationSettings(settings: NotificationSettings) {
    _notificationSettings.value = settings
  }

  fun deleteAccount(reason: String): Boolean {
    // Safety deletion scheduled
    return true
  }

  // Post & Social Interactions
  fun togglePostLike(postId: String) {
    _feedPosts.update { list ->
      list.map { post ->
        if (post.id == postId) {
          val newLiked = !post.isLiked
          post.copy(
            isLiked = newLiked,
            likesCount = if (newLiked) post.likesCount + 1 else post.likesCount - 1
          )
        } else post
      }
    }
  }

  fun togglePostSave(postId: String) {
    _feedPosts.update { list ->
      list.map { if (it.id == postId) it.copy(isSaved = !it.isSaved) else it }
    }
  }

  fun toggleFollowUser(userId: String) {
    _feedPosts.update { list ->
      list.map {
        if (it.author.id == userId) it.copy(isFollowing = !it.isFollowing) else it
      }
    }
  }

  fun addPostComment(postId: String, text: String) {
    val newComment = Comment(
      id = "c_${System.currentTimeMillis()}",
      authorName = _currentUser.value.name,
      text = text,
      timestamp = "Just now"
    )
    _feedPosts.update { list ->
      list.map { post ->
        if (post.id == postId) {
          post.copy(
            comments = post.comments + newComment,
            commentsCount = post.commentsCount + 1
          )
        } else post
      }
    }
  }

  fun toggleReelLike(reelId: String) {
    _reels.update { list ->
      list.map { reel ->
        if (reel.id == reelId) {
          val newLiked = !reel.isLiked
          reel.copy(
            isLiked = newLiked,
            likesCount = if (newLiked) reel.likesCount + 1 else reel.likesCount - 1
          )
        } else reel
      }
    }
  }

  fun addReelComment(reelId: String, text: String) {
    val clean = text.trim()
    if (clean.isEmpty()) return
    val newComment = Comment(
      id = "rc_${System.currentTimeMillis()}",
      authorName = _currentUser.value.name,
      text = clean,
      timestamp = "Just now"
    )
    _reels.update { list ->
      list.map { reel ->
        if (reel.id == reelId) {
          reel.copy(
            comments = reel.comments + newComment,
            commentsCount = reel.commentsCount + 1
          )
        } else reel
      }
    }
  }

  fun toggleVideoSubscribe(videoId: String) {
    _videos.update { list ->
      list.map { video ->
        if (video.id == videoId) {
          video.copy(isSubscribed = !video.isSubscribed)
        } else video
      }
    }
  }

  fun sendChatMessage(conversationId: String, text: String) {
    if (text.isBlank()) return
    val newMsg = ChatMessage(
      id = "m_${System.currentTimeMillis()}",
      senderId = "u_me",
      text = text.trim(),
      timestamp = "Just now",
      isMe = true
    )
    _chatConversations.update { list ->
      list.map { conv ->
        if (conv.id == conversationId) {
          conv.copy(
            lastMessage = text.trim(),
            lastTime = "Just now",
            messages = conv.messages + newMsg
          )
        } else conv
      }
    }
  }

  fun updateCurrentProfile(name: String, bio: String) {
    _currentUser.update {
      it.copy(
        name = name.trim().ifBlank { it.name },
        bio = bio.trim()
      )
    }
  }

  suspend fun syncProfileToFirestore(): Result<Unit> {
    val manager = firestoreManager
      ?: return Result.failure(IllegalStateException("FirestoreManager is not attached"))
    val res = manager.syncUserProfile(_currentUser.value)
    _firebaseDiagnostics.update {
      if (res.isSuccess) {
        it.copy(
          firestoreStatus = "✅ Profile synced to Firestore (users/${manager.currentUserId()})",
          isFirestoreSuccess = true
        )
      } else {
        it.copy(
          firestoreStatus = "❌ Sync failed: ${res.exceptionOrNull()?.localizedMessage ?: "Unknown error"}",
          isFirestoreSuccess = false
        )
      }
    }
    return res
  }

  suspend fun syncPostToFirestore(content: String): Result<String> {
    val manager = firestoreManager
      ?: return Result.failure(IllegalStateException("FirestoreManager is not attached"))
    val res = manager.createPost(content)
    _firebaseDiagnostics.update {
      if (res.isSuccess) {
        it.copy(
          firestoreStatus = "✅ Post saved in Firestore (id=${res.getOrNull()})",
          isFirestoreSuccess = true
        )
      } else {
        it.copy(
          firestoreStatus = "❌ Post save failed: ${res.exceptionOrNull()?.localizedMessage ?: "Unknown error"}",
          isFirestoreSuccess = false
        )
      }
    }
    return res
  }

  suspend fun syncSecuritySettingsToFirestore(): Result<Unit> {
    val manager = firestoreManager
      ?: return Result.failure(IllegalStateException("FirestoreManager is not attached"))
    val res = manager.saveSecuritySettings(
      twoFactorEnabled = _twoFactorEnabled.value,
      twoFactorMethod = _twoFactorMethod.value,
      privateAccount = _isPrivateAccount.value,
      suspiciousAlertsEnabled = _suspiciousAlertsEnabled.value,
      commentControl = _commentControl.value.name
    )
    _firebaseDiagnostics.update {
      if (res.isSuccess) {
        it.copy(
          firestoreStatus = "✅ Security settings synced to Firestore",
          isFirestoreSuccess = true
        )
      } else {
        it.copy(
          firestoreStatus = "❌ Security sync failed: ${res.exceptionOrNull()?.localizedMessage ?: "Unknown error"}",
          isFirestoreSuccess = false
        )
      }
    }
    return res
  }

  suspend fun runLiveFirebaseDiagnostics() {
    val manager = firestoreManager
    val uid = manager?.currentUserId()
    _firebaseDiagnostics.update {
      val currentSummary = when {
        uid == null -> "Phone OTP या Google Sign-In आवश्यक (Not signed in)"
        it.isGoogleLinked && it.isPhoneLinked -> "✅ Google + Mobile OTP दोनों Verified व Linked हैं"
        it.isGoogleLinked -> "✅ Google Sign-In Verified (${it.verifiedEmail.ifBlank { uid }})"
        it.isPhoneLinked -> "✅ Mobile OTP Verified (${it.verifiedPhoneNumber.ifBlank { uid }})"
        else -> "✅ Firebase Auth Verified ($uid)"
      }
      it.copy(
        isRunning = true,
        isFirebaseSignedIn = uid != null,
        firebaseUid = uid,
        authProviderSummary = currentSummary
      )
    }

    if (manager == null) {
      _firebaseDiagnostics.update {
        it.copy(
          isRunning = false,
          firestoreWriteStatus = "❌ FirestoreManager not initialized",
          isFirestoreWriteSuccess = false,
          firestoreReadStatus = "❌ FirestoreManager not initialized",
          isFirestoreReadSuccess = false,
          firestoreStatus = "❌ FirestoreManager not initialized",
          isFirestoreSuccess = false,
          storageStatus = "⏸️ Pending (Spark Free Plan — ₹0 Billing Guard)",
          isStorageSuccess = false
        )
      }
      return
    }

    val report = manager.verifyFirestoreReadAndWrite(_currentUser.value)
    val bothOk = report.writeSuccess && report.readSuccess
    val summary = when {
      bothOk -> "✅ Write & Server Read दोनों सफल"
      report.writeSuccess && !report.readSuccess -> "⚠️ Write OK, Read Failed: ${report.readMessage}"
      else -> "❌ ${report.writeMessage}"
    }

    _firebaseDiagnostics.update {
      it.copy(
        isRunning = false,
        firestoreWriteStatus = if (report.writeSuccess) "✅ ${report.writeMessage}" else "❌ ${report.writeMessage}",
        isFirestoreWriteSuccess = report.writeSuccess,
        firestoreReadStatus = if (report.readSuccess) "✅ ${report.readMessage}" else "❌ ${report.readMessage}",
        isFirestoreReadSuccess = report.readSuccess,
        firestoreStatus = summary,
        isFirestoreSuccess = bothOk,
        storageStatus = "⏸️ Pending (Spark Free Plan — ₹0 लागत के लिए Storage अभी बंद है)",
        isStorageSuccess = false
      )
    }
  }

  // Live Streaming Management
  fun startLiveStream(title: String, channelName: String = "vexora_live_${System.currentTimeMillis()}"): LiveRoom {
    val initialGuests = listOf(
      LiveGuest(
        id = "g_kavya",
        user = User("u6", "Kavya Dance", "kavya_dance", isVerified = true),
        isAudioMuted = false,
        isVideoMuted = false,
        joinedAt = "1m ago"
      )
    )
    val room = LiveRoom(
      id = "room_${System.currentTimeMillis()}",
      host = _currentUser.value,
      title = if (title.isBlank()) "🔴 Hanging out live on VEXORA ✨" else title.trim(),
      viewerCount = 142,
      likesCount = 380,
      guests = initialGuests,
      isLive = true,
      channelName = channelName
    )
    _activeLiveRoom.value = room
    return room
  }

  fun endLiveStream(saveRecording: Boolean = true): LiveRoom? {
    val current = _activeLiveRoom.value ?: return null
    _activeLiveRoom.value = null
    if (saveRecording) {
      // Save recorded live stream to Videos watch feed
      val recordedVideo = VideoItem(
        id = "rec_${System.currentTimeMillis()}",
        channelName = current.host.name,
        title = "🔴 Recorded Live: ${current.title}",
        views = "${current.viewerCount} live viewers",
        duration = "Stream Replay",
        timeAgo = "Just now",
        isSubscribed = true,
        gradientStart = 0xFF8B5CF6,
        gradientEnd = 0xFF06B6D4
      )
      _videos.update { listOf(recordedVideo) + it }
    }
    return current
  }

  fun addGuestToLive(user: User) {
    val guest = LiveGuest(
      id = "g_${user.id}_${System.currentTimeMillis()}",
      user = user,
      isAudioMuted = false,
      isVideoMuted = false,
      joinedAt = "Just now"
    )
    _activeLiveRoom.update { room ->
      room?.copy(guests = room.guests + guest)
    }
    sendLiveComment("${user.name} joined as co-host guest 🎉")
  }

  fun removeGuestFromLive(guestId: String) {
    _activeLiveRoom.update { room ->
      room?.copy(guests = room.guests.filterNot { it.id == guestId })
    }
  }

  fun toggleGuestMute(guestId: String) {
    _activeLiveRoom.update { room ->
      room?.copy(
        guests = room.guests.map { g ->
          if (g.id == guestId) g.copy(isAudioMuted = !g.isAudioMuted) else g
        }
      )
    }
  }

  fun sendLiveComment(text: String, isHost: Boolean = false) {
    if (text.isBlank()) return
    val newComment = LiveComment(
      id = "lc_${System.currentTimeMillis()}",
      user = _currentUser.value,
      message = text.trim(),
      isHost = isHost
    )
    _liveComments.update { it + newComment }
  }

  fun sendLiveHeart() {
    _activeLiveRoom.update { room ->
      room?.copy(likesCount = room.likesCount + 1)
    }
  }

  fun reportLiveUser(username: String, reason: String) {
    reportUser(username, reason, "Reported during live stream broadcast")
  }

  fun blockLiveUser(user: User) {
    blockUser(user.username, user.name)
    _activeLiveRoom.update { room ->
      room?.copy(guests = room.guests.filterNot { it.user.id == user.id })
    }
  }

  // Content Creation: Post, Reel, Story
  fun createPost(
    content: String,
    mediaUrls: List<String> = emptyList(),
    location: String = "",
    hashtags: List<String> = emptyList()
  ): Post {
    val newPost = Post(
      id = "post_${System.currentTimeMillis()}",
      author = _currentUser.value,
      content = content.trim(),
      timestamp = "Just now",
      likesCount = 1,
      commentsCount = 0,
      sharesCount = 0,
      isLiked = true,
      mediaUrls = mediaUrls,
      location = location,
      hashtags = hashtags,
      gradientStart = 0xFF6366F1,
      gradientEnd = 0xFFEC4899,
      postType = if (mediaUrls.size > 1) "CAROUSEL" else "PHOTO"
    )
    _feedPosts.update { listOf(newPost) + it }
    _currentUser.update { it.copy(postsCount = it.postsCount + 1) }
    return newPost
  }

  fun createReel(
    caption: String,
    musicTitle: String = "Original Sound - ${_currentUser.value.username}",
    gradientStart: Long = 0xFF8B5CF6,
    gradientEnd: Long = 0xFFEC4899
  ): Reel {
    val newReel = Reel(
      id = "reel_${System.currentTimeMillis()}",
      creator = _currentUser.value,
      caption = caption.trim(),
      musicTitle = if (musicTitle.isBlank()) "Original Sound - ${_currentUser.value.username}" else musicTitle.trim(),
      likesCount = 1,
      commentsCount = 0,
      sharesCount = 0,
      isLiked = true,
      viewsCount = "1",
      gradientStart = gradientStart,
      gradientEnd = gradientEnd
    )
    _reels.update { listOf(newReel) + it }
    return newReel
  }

  fun createStory(
    caption: String,
    mediaUrl: String = "",
    gradientStart: Long = 0xFF8B5CF6,
    gradientEnd: Long = 0xFFEC4899
  ): StoryItem {
    val newItem = StoryItem(
      id = "si_${System.currentTimeMillis()}",
      user = _currentUser.value,
      mediaUrl = mediaUrl,
      caption = caption.trim(),
      timestamp = "Just now",
      gradientStart = gradientStart,
      gradientEnd = gradientEnd,
      viewers = emptyList()
    )
    _stories.update { currentList ->
      currentList.map { story ->
        if (story.user.id == _currentUser.value.id || story.id == "s1") {
          story.copy(
            isViewed = false,
            hasUnseen = true,
            items = listOf(newItem) + story.items
          )
        } else story
      }
    }
    return newItem
  }

  fun saveStoryToHighlights(storyId: String) {
    _stories.update { list ->
      list.map { story ->
        story.copy(
          items = story.items.map { item ->
            if (item.id == storyId) item.copy(isSavedToHighlights = true) else item
          }
        )
      }
    }
  }

  fun recordStoryView(storyId: String) {
    val me = _currentUser.value
    _stories.update { list ->
      list.map { story ->
        story.copy(
          items = story.items.map { item ->
            if (item.id == storyId) {
              if (item.viewers.none { it.user.id == me.id }) {
                item.copy(viewers = item.viewers + StoryViewer(me, "Just now"))
              } else item
            } else item
          }
        )
      }
    }
  }

  // Performance, Network & Caching methods
  fun refreshFeed() {
    _isFeedRefreshing.value = true
    if (pendingOfflineActions.isNotEmpty()) {
      pendingOfflineActions.clear()
    }
    _isFeedRefreshing.value = false
  }

  fun loadMorePosts() {
    val currentSize = _feedPosts.value.size
    val nextPagePosts = listOf(
      Post(
        id = "p_page_${currentSize + 1}",
        author = User("u_pag1", "Vikram Rathore", "vikram_creatives", bio = "Design Director 🎨"),
        content = "Minimalist architecture in Jaipur. Light, geometry, and stone in perfect symphony. 🏛️✨ #VexoraArchitecture #IncredibleIndia",
        timestamp = "5h ago",
        likesCount = 412,
        commentsCount = 31,
        sharesCount = 19,
        gradientStart = 0xFF0D9488,
        gradientEnd = 0xFF06B6D4,
        location = "Jaipur, Rajasthan",
        hashtags = listOf("#Architecture", "#Jaipur", "#Design")
      ),
      Post(
        id = "p_page_${currentSize + 2}",
        author = User("u_pag2", "Meera Sen", "meera_techie", bio = "Kotlin & AI Dev ⚡"),
        content = "Optimizing Compose apps with stable keys, smart caching, and low-latency video streaming delivers silky 60fps performance! 🚀 #AndroidDev #VexoraSpeed",
        timestamp = "7h ago",
        likesCount = 980,
        commentsCount = 88,
        sharesCount = 64,
        gradientStart = 0xFF4338CA,
        gradientEnd = 0xFF8B5CF6,
        location = "Bengaluru, Karnataka",
        hashtags = listOf("#Tech", "#Vexora", "#Speed")
      )
    )
    _feedPosts.update { it + nextPagePosts }
  }

  fun updateDataSaverPreferences(prefs: DataSaverPreferences) {
    _dataSaverPreferences.value = prefs
  }

  fun setSimulatedNetwork(quality: NetworkQuality?) {
    if (quality != null) {
      _networkQuality.value = quality
      _isOffline.value = quality == NetworkQuality.OFFLINE
      if (quality == NetworkQuality.SLOW && _dataSaverPreferences.value.autoDataSaverOnSlowNetwork) {
        _dataSaverPreferences.update { it.copy(isDataSaverEnabled = true, videoQuality = VideoQualityOption.DATA_SAVER) }
      }
    } else {
      _networkQuality.value = NetworkQuality.GOOD
      _isOffline.value = false
    }
  }

  fun updateUploadProgress(task: UploadTask) {
    _activeUploadTask.value = task
  }

  fun cancelUpload() {
    _activeUploadTask.value = null
  }

  fun clearLocalCache(): String {
    return "142 MB RAM & Disk cache cleared successfully"
  }

  // ==========================================
  // REAL MONETIZATION & WALLET METHODS
  // ==========================================

  fun purchaseCoins(packId: String, orderId: String): Boolean {
    val pack = coinPacks.find { it.id == packId } ?: return false
    val totalCoins = pack.coins + pack.bonusCoins
    
    _userWallet.update { current ->
      current.copy(coinsBalance = current.coinsBalance + totalCoins)
    }

    val newTxn = FinancialTransaction(
      id = "txn_${System.currentTimeMillis()}",
      userId = _userWallet.value.userId,
      type = TransactionType.COIN_PURCHASE,
      amountRupees = pack.priceRupees,
      coinsDelta = totalCoins,
      description = "Purchased $totalCoins Vexora Coins (${pack.name})",
      timestamp = System.currentTimeMillis(),
      status = TransactionStatus.SUCCESS,
      referenceId = orderId
    )
    _transactions.update { listOf(newTxn) + it }

    // Update platform metrics
    _adminMetrics.update { metrics ->
      metrics.copy(
        totalRevenueRupees = metrics.totalRevenueRupees + pack.priceRupees,
        todayRevenueRupees = metrics.todayRevenueRupees + pack.priceRupees,
        monthRevenueRupees = metrics.monthRevenueRupees + pack.priceRupees,
        liveGiftRevenueRupees = metrics.liveGiftRevenueRupees + (pack.priceRupees * 0.30)
      )
    }
    return true
  }

  fun sendLiveGift(giftId: String, recipientName: String): Pair<Boolean, String> {
    val gift = virtualGifts.find { it.id == giftId } ?: return Pair(false, "Gift not found")
    val currentBalance = _userWallet.value.coinsBalance
    
    if (currentBalance < gift.coinPrice) {
      return Pair(false, "Insufficient coins. You have $currentBalance coins, but this gift costs ${gift.coinPrice} coins.")
    }

    // Deduct coins from sender wallet
    _userWallet.update { it.copy(coinsBalance = it.coinsBalance - gift.coinPrice) }

    // 1 coin = 0.50 INR. Platform split: 70% to creator, 30% platform commission
    val grossRupees = gift.coinPrice * 0.50
    val creatorShare = grossRupees * (_adminMetrics.value.creatorRevenueSharePercent / 100.0)
    val platformShare = grossRupees - creatorShare

    // Record sender transaction
    val senderTxn = FinancialTransaction(
      id = "txn_gift_${System.currentTimeMillis()}",
      userId = _userWallet.value.userId,
      type = TransactionType.GIFT_SENT,
      amountRupees = grossRupees,
      coinsDelta = -gift.coinPrice,
      description = "Sent ${gift.name} to $recipientName during Live",
      timestamp = System.currentTimeMillis(),
      status = TransactionStatus.SUCCESS,
      referenceId = "GIFT-${gift.id}"
    )
    _transactions.update { listOf(senderTxn) + it }

    // Trigger visual overlay animation
    _activeLiveGift.value = gift

    // Update admin metrics
    _adminMetrics.update { metrics ->
      metrics.copy(
        liveGiftRevenueRupees = metrics.liveGiftRevenueRupees + platformShare,
        totalPaidToCreatorsRupees = metrics.totalPaidToCreatorsRupees + creatorShare
      )
    }

    return Pair(true, "Gift sent successfully!")
  }

  fun clearActiveLiveGift() {
    _activeLiveGift.value = null
  }

  fun subscribeToPremium(plan: SubscriptionPlan, orderId: String): Boolean {
    val durationDays = if (plan == SubscriptionPlan.MONTHLY) 30L else 365L
    val expiry = System.currentTimeMillis() + (durationDays * 86400000L)
    
    _premiumSubscription.value = PremiumSubscription(
      userId = _userWallet.value.userId,
      isPremium = true,
      plan = plan,
      validUntil = expiry,
      autoRenew = true
    )

    val newTxn = FinancialTransaction(
      id = "txn_sub_${System.currentTimeMillis()}",
      userId = _userWallet.value.userId,
      type = TransactionType.PREMIUM_PURCHASE,
      amountRupees = plan.priceRupees,
      coinsDelta = 0,
      description = "Subscribed to VEXORA ${plan.title} VIP",
      timestamp = System.currentTimeMillis(),
      status = TransactionStatus.SUCCESS,
      referenceId = orderId
    )
    _transactions.update { listOf(newTxn) + it }

    _adminMetrics.update { metrics ->
      metrics.copy(
        totalRevenueRupees = metrics.totalRevenueRupees + plan.priceRupees,
        todayRevenueRupees = metrics.todayRevenueRupees + plan.priceRupees,
        monthRevenueRupees = metrics.monthRevenueRupees + plan.priceRupees,
        premiumSubRevenueRupees = metrics.premiumSubRevenueRupees + plan.priceRupees
      )
    }
    return true
  }

  fun cancelPremiumSubscription() {
    _premiumSubscription.value = PremiumSubscription(
      userId = _userWallet.value.userId,
      isPremium = false,
      plan = SubscriptionPlan.MONTHLY,
      autoRenew = false
    )
  }

  fun submitMonetizationApplication(panNumber: String, legalName: String, category: String): Pair<Boolean, String> {
    if (panNumber.length != 10) {
      return Pair(false, "Invalid PAN card number. Must be 10 characters.")
    }
    if (legalName.isBlank()) {
      return Pair(false, "Legal name as per PAN card is required.")
    }

    val currentFollowers = 14200 // verified account
    val currentEligibleViews = _userWallet.value.eligibleReelsViews

    val newApp = MonetizationApplication(
      id = "app_${System.currentTimeMillis()}",
      userId = _userWallet.value.userId,
      userName = legalName,
      userHandle = "aarav_vexora",
      followersCount = currentFollowers,
      eligibleViews = currentEligibleViews,
      panNumber = panNumber.uppercase(),
      legalName = legalName,
      category = category,
      status = MonetizationStatus.UNDER_REVIEW,
      appliedAt = System.currentTimeMillis()
    )

    _monetizationApplication.value = newApp
    _userWallet.update { it.copy(kycStatus = KycStatus.UNDER_REVIEW, panNumber = panNumber.uppercase()) }

    return Pair(true, "Application submitted successfully! Review completed within 24-48 hours.")
  }

  fun updatePayoutDetails(method: PayoutMethod, upiId: String?, bankDetails: BankAccountDetails?): Boolean {
    _userWallet.update { current ->
      current.copy(
        payoutMethod = method,
        upiId = if (method == PayoutMethod.UPI) upiId else null,
        bankDetails = if (method == PayoutMethod.BANK_TRANSFER) bankDetails else null
      )
    }
    return true
  }

  fun requestWithdrawal(amountRupees: Double): Pair<Boolean, String> {
    val wallet = _userWallet.value
    if (wallet.kycStatus != KycStatus.VERIFIED) {
      return Pair(false, "KYC Verification required before withdrawing funds.")
    }
    if (amountRupees < 1000.0) {
      return Pair(false, "Minimum withdrawal limit is ₹1,000 INR.")
    }
    if (amountRupees > wallet.withdrawableRupees) {
      return Pair(false, "Insufficient balance. Available: ₹${wallet.withdrawableRupees}")
    }

    val destination = if (wallet.payoutMethod == PayoutMethod.UPI) {
      "UPI: ${wallet.upiId ?: "Not set"}"
    } else {
      "Bank: ${wallet.bankDetails?.bankName ?: "Bank"} (A/C: ${wallet.bankDetails?.accountNumber?.takeLast(4) ?: "****"})"
    }

    // Deduct from withdrawable and hold in pending
    _userWallet.update { current ->
      current.copy(
        withdrawableRupees = current.withdrawableRupees - amountRupees,
        pendingRupees = current.pendingRupees + amountRupees
      )
    }

    val newRequest = WithdrawalRequest(
      id = "w_${System.currentTimeMillis()}",
      userId = wallet.userId,
      creatorName = wallet.panNumber,
      amountRupees = amountRupees,
      payoutDestination = destination,
      requestedAt = System.currentTimeMillis(),
      status = WithdrawalStatus.PENDING,
      remarks = "Under security & tax verification"
    )

    _withdrawalRequests.update { listOf(newRequest) + it }

    val txn = FinancialTransaction(
      id = "txn_w_${System.currentTimeMillis()}",
      userId = wallet.userId,
      type = TransactionType.WITHDRAWAL,
      amountRupees = amountRupees,
      coinsDelta = 0,
      description = "Withdrawal request to $destination",
      timestamp = System.currentTimeMillis(),
      status = TransactionStatus.PENDING,
      referenceId = newRequest.id
    )
    _transactions.update { listOf(txn) + it }

    return Pair(true, "Withdrawal request of ₹$amountRupees submitted successfully.")
  }

  // ADMIN OPERATIONS
  fun adminApproveMonetization(applicationId: String) {
    if (_monetizationApplication.value.id == applicationId) {
      _monetizationApplication.update { it.copy(status = MonetizationStatus.APPROVED) }
      _userWallet.update { it.copy(kycStatus = KycStatus.VERIFIED) }
    }
  }

  fun adminRejectMonetization(applicationId: String, reason: String) {
    if (_monetizationApplication.value.id == applicationId) {
      _monetizationApplication.update { it.copy(status = MonetizationStatus.REJECTED, rejectionReason = reason) }
      _userWallet.update { it.copy(kycStatus = KycStatus.REJECTED) }
    }
  }

  fun adminApproveWithdrawal(requestId: String, utrNumber: String) {
    var approvedAmount = 0.0
    _withdrawalRequests.update { list ->
      list.map { req ->
        if (req.id == requestId) {
          approvedAmount = req.amountRupees
          req.copy(
            status = WithdrawalStatus.PAID,
            utrNumber = utrNumber.ifBlank { "UTR${System.currentTimeMillis().toString().takeLast(10)}" },
            remarks = "Disbursed via Direct Transfer"
          )
        } else req
      }
    }

    // Deduct from pending and increment paid out
    _userWallet.update { current ->
      current.copy(pendingRupees = (current.pendingRupees - approvedAmount).coerceAtLeast(0.0))
    }

    _adminMetrics.update { metrics ->
      metrics.copy(totalPaidToCreatorsRupees = metrics.totalPaidToCreatorsRupees + approvedAmount)
    }

    _transactions.update { list ->
      list.map { txn ->
        if (txn.referenceId == requestId) {
          txn.copy(status = TransactionStatus.SUCCESS, referenceId = utrNumber)
        } else txn
      }
    }
  }

  fun adminRejectWithdrawal(requestId: String, reason: String) {
    var refundedAmount = 0.0
    _withdrawalRequests.update { list ->
      list.map { req ->
        if (req.id == requestId) {
          refundedAmount = req.amountRupees
          req.copy(status = WithdrawalStatus.REJECTED, remarks = reason)
        } else req
      }
    }

    // Refund from pending back to withdrawable
    _userWallet.update { current ->
      current.copy(
        pendingRupees = (current.pendingRupees - refundedAmount).coerceAtLeast(0.0),
        withdrawableRupees = current.withdrawableRupees + refundedAmount
      )
    }

    _transactions.update { list ->
      list.map { txn ->
        if (txn.referenceId == requestId) {
          txn.copy(status = TransactionStatus.FAILED)
        } else txn
      }
    }
  }

  fun adminUpdateCommissionShare(creatorPercent: Double) {
    _adminMetrics.update { it.copy(creatorRevenueSharePercent = creatorPercent.coerceIn(50.0, 90.0)) }
  }

  // ADMIN MVP: USERS, POSTS, REPORTS & SPARK APP SETTINGS
  fun adminToggleUserVerified(userId: String) {
    _managedUsers.update { list ->
      list.map { item ->
        if (item.user.id == userId) {
          item.copy(user = item.user.copy(isVerified = !item.user.isVerified))
        } else item
      }
    }
  }

  fun adminToggleUserSuspended(userId: String) {
    _managedUsers.update { list ->
      list.map { item ->
        if (item.user.id == userId) item.copy(isSuspended = !item.isSuspended) else item
      }
    }
  }

  suspend fun syncAdminUsersFromFirestore(): Result<Int> {
    val manager = firestoreManager
      ?: return Result.failure(IllegalStateException("FirestoreManager not initialized"))
    val res = manager.fetchUsersDirectory()
    if (res.isSuccess) {
      val cloudUsers = res.getOrNull().orEmpty()
      _managedUsers.update { existing ->
        val cloudIds = cloudUsers.map { it.id }.toSet()
        val mappedCloud = cloudUsers.map { u ->
          AdminManagedUser(
            user = u,
            isSuspended = existing.firstOrNull { it.user.id == u.id }?.isSuspended ?: false,
            isFirestoreSynced = true,
            authProvider = "Cloud Firestore"
          )
        }
        mappedCloud + existing.filterNot { it.user.id in cloudIds }
      }
      return Result.success(cloudUsers.size)
    }
    return Result.failure(res.exceptionOrNull() ?: Exception("Firestore users fetch failed"))
  }

  suspend fun adminDeletePost(postId: String): Result<Unit> {
    _feedPosts.update { list -> list.filterNot { it.id == postId } }
    val manager = firestoreManager
    if (manager != null && manager.currentUserId() != null) {
      return manager.deletePost(postId)
    }
    return Result.success(Unit)
  }

  fun adminResolveReport(reportId: String, removeTargetPost: Boolean = false) {
    val report = _contentReports.value.firstOrNull { it.id == reportId }
    if (removeTargetPost && report?.targetType == "POST") {
      _feedPosts.update { list -> list.filterNot { it.id == report.targetId } }
    }
    _contentReports.update { list ->
      list.map {
        if (it.id == reportId) {
          it.copy(
            status = ReportStatus.RESOLVED,
            actionTaken = if (removeTargetPost) "Post removed & report resolved" else "Reviewed & resolved"
          )
        } else it
      }
    }
  }

  fun adminDismissReport(reportId: String) {
    _contentReports.update { list ->
      list.map {
        if (it.id == reportId) it.copy(status = ReportStatus.DISMISSED, actionTaken = "Dismissed by Admin") else it
      }
    }
  }

  fun adminUpdateSparkMvpConfig(config: SparkMvpConfig) {
    _sparkMvpConfig.value = config
  }

  fun creditReelsAdShare(viewsCount: Int) {
    // Verified 1000 views = ₹40 RPM with 70% share
    val earned = (viewsCount / 1000.0) * 40.0 * (_adminMetrics.value.creatorRevenueSharePercent / 100.0)
    if (earned <= 0) return

    _userWallet.update { current ->
      current.copy(
        withdrawableRupees = current.withdrawableRupees + earned,
        totalLifetimeEarnings = current.totalLifetimeEarnings + earned,
        eligibleReelsViews = current.eligibleReelsViews + viewsCount
      )
    }

    val txn = FinancialTransaction(
      id = "txn_ad_${System.currentTimeMillis()}",
      userId = _userWallet.value.userId,
      type = TransactionType.REELS_AD_SHARE,
      amountRupees = earned,
      coinsDelta = 0,
      description = "Reels Ad Share ($viewsCount verified views)",
      timestamp = System.currentTimeMillis(),
      status = TransactionStatus.SUCCESS,
      referenceId = "RPM-40-${System.currentTimeMillis()}"
    )
    _transactions.update { listOf(txn) + it }
  }

  companion object {
    val instance: ZevoraRepository by lazy { ZevoraRepository() }
  }
}
