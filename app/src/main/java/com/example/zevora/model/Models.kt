package com.example.zevora.model

// User & Profile
data class User(
  val id: String,
  val name: String,
  val username: String,
  val avatarUrl: String = "",
  val bio: String = "",
  val isVerified: Boolean = true,
  val followersCount: Int = 14200,
  val followingCount: Int = 380,
  val postsCount: Int = 42,
  val isPrivate: Boolean = false,
  val phone: String = "+91 98765 43210",
  val email: String = "aarav.sharma@vexora.io"
)

// Posts, Reels & Media
data class Post(
  val id: String,
  val author: User,
  val content: String,
  val timestamp: String,
  val likesCount: Int = 0,
  val commentsCount: Int = 0,
  val sharesCount: Int = 0,
  val isLiked: Boolean = false,
  val isSaved: Boolean = false,
  val isFollowing: Boolean = false,
  val mediaUrls: List<String> = emptyList(),
  val imageUrls: List<String> = emptyList(),
  val videoUrl: String? = null,
  val location: String? = null,
  val hashtags: List<String> = emptyList(),
  val comments: List<Comment> = emptyList(),
  val gradientStart: Long = 0xFF8B5CF6,
  val gradientEnd: Long = 0xFF06B6D4,
  val postType: String = "PHOTO"
)

data class Reel(
  val id: String,
  val creator: User,
  val caption: String,
  val musicTitle: String = "Original Audio",
  val musicTrack: String = "Original Audio",
  val likesCount: Int = 0,
  val commentsCount: Int = 0,
  val sharesCount: Int = 0,
  val viewsCount: String = "10K",
  val isLiked: Boolean = false,
  val isSaved: Boolean = false,
  val isFollowing: Boolean = false,
  val videoUrl: String = "",
  val hashtags: List<String> = emptyList(),
  val comments: List<Comment> = emptyList(),
  val gradientStart: Long = 0xFFEC4899,
  val gradientEnd: Long = 0xFF8B5CF6
)

data class StoryViewer(
  val user: User,
  val viewedAt: String = "Just now"
)

data class StoryItem(
  val id: String,
  val user: User,
  val caption: String = "",
  val timestamp: String = "Just now",
  val mediaUrl: String = "",
  val gradientStart: Long = 0xFF8B5CF6,
  val gradientEnd: Long = 0xFFEC4899,
  val isSavedToHighlights: Boolean = false,
  val viewers: List<StoryViewer> = emptyList()
)

data class Story(
  val id: String,
  val user: User,
  val mediaUrl: String = "",
  val caption: String = "",
  val timestamp: String = "Now",
  val viewersCount: Int = 0,
  val isViewed: Boolean = false,
  val hasUnseen: Boolean = false,
  val isLiveBroadcast: Boolean = false,
  val isSavedToHighlight: Boolean = false,
  val items: List<StoryItem> = emptyList(),
  val viewers: List<StoryViewer> = emptyList()
)

data class VideoItem(
  val id: String,
  val title: String,
  val channelName: String,
  val views: String,
  val timestamp: String = "Recently",
  val timeAgo: String = "2 hours ago",
  val duration: String,
  val isLiked: Boolean = false,
  val isSubscribed: Boolean = false,
  val likesCount: Int = 240,
  val commentsCount: Int = 38,
  val gradientStart: Long = 0xFF06B6D4,
  val gradientEnd: Long = 0xFF3B82F6
)

data class Comment(
  val id: String,
  val authorName: String = "Anonymous",
  val userName: String = "Anonymous",
  val userAvatar: String = "",
  val text: String = "",
  val timestamp: String = "Just now",
  val likesCount: Int = 0,
  val isLiked: Boolean = false
) {
  // Support 5-argument constructor: (id, authorName, userAvatar, text, timestamp)
  constructor(id: String, authorName: String, userAvatar: String, text: String, timestamp: String) :
    this(id = id, authorName = authorName, userName = authorName, userAvatar = userAvatar, text = text, timestamp = timestamp)
}

// Live Streaming
data class LiveRoom(
  val id: String,
  val host: User,
  val title: String,
  val viewerCount: Int = 0,
  val likesCount: Int = 0,
  val isLive: Boolean = true,
  val isBroadcasting: Boolean = true,
  val channelName: String = "vexora_live",
  val guests: List<LiveGuest> = emptyList(),
  val startTimeMillis: Long = System.currentTimeMillis()
)

data class LiveGuest(
  val id: String,
  val user: User,
  val isVideoMuted: Boolean = false,
  val isAudioMuted: Boolean = false,
  val joinedAt: String = "Just now",
  val joinedAtMillis: Long = System.currentTimeMillis()
)

data class LiveComment(
  val id: String,
  val user: User,
  val message: String,
  val isHost: Boolean = false,
  val timestampMillis: Long = System.currentTimeMillis()
)

// Chat
data class ChatMessage(
  val id: String,
  val senderId: String,
  val text: String,
  val timestamp: String,
  val isRead: Boolean = true,
  val isMe: Boolean = false,
  val isFromMe: Boolean = false,
  val isVoiceNote: Boolean = false,
  val voiceDurationSec: Int = 0
)

data class ChatConversation(
  val id: String,
  val participant: User = User("u_default", "User", "user"),
  val recipient: User = User("u_default", "User", "user"),
  val lastMessage: String,
  val lastTime: String = "Just now",
  val lastMessageTime: String = "Just now",
  val unreadCount: Int = 0,
  val isOnline: Boolean = false,
  val messages: List<ChatMessage> = emptyList()
)

// Privacy & Security
enum class TwoFactorMethod(val displayName: String, val description: String) {
  SMS_OTP("SMS OTP", "6 अंकों का OTP आपके फोन पर भेजा जाएगा"),
  AUTHENTICATOR("Google Authenticator", "Time-based OTP ऐप के ज़रिए"),
  EMAIL_OTP("Email OTP", "सुरक्षा कोड आपके रजिस्टर्ड ईमेल पर भेजा जाएगा")
}

data class BlockedUser(
  val id: String,
  val username: String,
  val name: String,
  val blockedDate: String
)

data class SessionDevice(
  val id: String,
  val deviceName: String,
  val location: String,
  val ipAddress: String = "192.168.1.1",
  val lastActive: String,
  val isCurrentDevice: Boolean = false
)

enum class AlertSeverity {
  LOW,
  MEDIUM,
  HIGH
}

data class SecurityAlert(
  val id: String,
  val title: String,
  val description: String,
  val timestamp: String,
  val severity: AlertSeverity = AlertSeverity.MEDIUM,
  val isResolved: Boolean = false,
  val isSuspicious: Boolean = true,
  val actionRequired: Boolean = false
)

enum class CommentControlOption(val title: String, val subtitle: String, val labelKey: String = "EVERYONE") {
  EVERYONE("Everyone (सभी लोग)", "कोई भी आपकी पोस्ट पर कमेंट कर सकता है", "EVERYONE"),
  PEOPLE_YOU_FOLLOW("People You Follow (जिन्हें आप फॉलो करते हैं)", "केवल आपके फॉलोअर्स व फॉलोइंग", "PEOPLE_YOU_FOLLOW"),
  NO_ONE("No One (कोई नहीं)", "कमेंट्स पूरी तरह से बंद रहेंगे", "NO_ONE")
}

// Settings & Preferences
enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
  HINDI("hi", "Hindi", "हिंदी"),
  ENGLISH("en", "English", "English"),
  HINGLISH("hng", "Hinglish", "Hinglish"),
  PUNJABI("pa", "Punjabi", "ਪੰਜਾਬੀ"),
  BENGALI("bn", "Bengali", "বাংলা"),
  GUJARATI("gu", "Gujarati", "ગુજરાતી"),
  MARATHI("mr", "Marathi", "मराठी"),
  TAMIL("ta", "Tamil", "தமிழ்"),
  TELUGU("te", "Telugu", "తెలుగు")
}

enum class AppThemeMode(val title: String) {
  SYSTEM("System Default (सिस्टम अनुसार)"),
  DARK("Dark Mode (डार्क थीम)"),
  LIGHT("Light Mode (लाइट थीम)")
}

data class NotificationSettings(
  val pushNotifications: Boolean = true,
  val likesNotifications: Boolean = true,
  val likesReactions: Boolean = true,
  val commentsNotifications: Boolean = true,
  val comments: Boolean = true,
  val directMessages: Boolean = true,
  val liveAlerts: Boolean = true,
  val securityAlerts: Boolean = true
)

// Smart Network & Offline Engine Models
enum class NetworkQuality(val label: String, val speedHint: String) {
  EXCELLENT("5G / High Speed Fiber", "Optimal 4K/60fps streaming & full quality"),
  GOOD("4G LTE", "Smooth 1080p, preloads 2 future reels"),
  MODERATE("3G / Low Bandwidth", "720p, lightweight images, throttles preloading"),
  SLOW("2G / Edge", "Low Data Mode active, compressed 480p, audio prioritized"),
  OFFLINE("No Internet Connection", "Viewing cached content, offline actions queued")
}

enum class VideoQualityOption(val title: String, val resolution: String, val description: String = "") {
  AUTO("Auto (Adaptive)", "Dynamically adjusts according to network speed", "नेटवर्क के अनुसार स्वयं बदलता है"),
  DATA_SAVER("Data Saver (कम डेटा)", "Uses up to 60% less mobile data (480p)", "कम इंटरनेट डेटा खर्च (480p)"),
  BALANCED("Balanced (मध्यम)", "Standard 720p HD with efficient buffering", "संतुलित 720p HD क्वालिटी"),
  HIGH_DEFINITION("High Quality (1080p / 4K)", "Best visual clarity for fast Wi-Fi / 5G", "सर्वश्रेष्ठ स्पष्टता 1080p / 4K")
}

data class DataSaverPreferences(
  val isDataSaverEnabled: Boolean = false,
  val autoDataSaverOnSlowNetwork: Boolean = true,
  val videoQuality: VideoQualityOption = VideoQualityOption.AUTO,
  val compressUploads: Boolean = true,
  val preloadMediaOnWifiOnly: Boolean = false
)

data class UploadTask(
  val id: String,
  val title: String,
  val type: String, // "POST", "REEL", "STORY"
  val progress: Float = 0.0f,
  val isCompleted: Boolean = false,
  val isFailed: Boolean = false,
  val errorMessage: String? = null
)

// ==========================================
// REAL MONETIZATION & WALLET MODELS
// ==========================================

enum class KycStatus {
  NOT_SUBMITTED,
  UNDER_REVIEW,
  VERIFIED,
  REJECTED
}

enum class PayoutMethod {
  UPI,
  BANK_TRANSFER
}

data class BankAccountDetails(
  val accountHolderName: String = "",
  val accountNumber: String = "",
  val ifscCode: String = "",
  val bankName: String = ""
)

data class UserWallet(
  val userId: String,
  val coinsBalance: Int = 450,
  val withdrawableRupees: Double = 4280.0,
  val pendingRupees: Double = 650.0,
  val totalLifetimeEarnings: Double = 31200.0,
  val eligibleReelsViews: Long = 184500L,
  val kycStatus: KycStatus = KycStatus.VERIFIED,
  val panNumber: String = "ABCDE1234F",
  val payoutMethod: PayoutMethod = PayoutMethod.UPI,
  val upiId: String? = "aarav@oksbi",
  val bankDetails: BankAccountDetails? = BankAccountDetails("Aarav Sharma", "50100492817291", "HDFC0001234", "HDFC Bank Ltd")
)

data class VirtualGift(
  val id: String,
  val name: String,
  val coinPrice: Int,
  val icon: String,
  val animationType: String,
  val description: String
)

data class CoinPack(
  val id: String,
  val coins: Int,
  val bonusCoins: Int = 0,
  val priceRupees: Double,
  val isPopular: Boolean = false,
  val googlePlaySku: String
) {
  val name: String get() = "$coins Coins Pack"
}

enum class TransactionType {
  COIN_PURCHASE,
  GIFT_SENT,
  GIFT_RECEIVED,
  REELS_AD_SHARE,
  PREMIUM_PURCHASE,
  WITHDRAWAL
}

enum class TransactionStatus {
  SUCCESS,
  PENDING,
  FAILED
}

data class FinancialTransaction(
  val id: String,
  val userId: String,
  val type: TransactionType,
  val amountRupees: Double = 0.0,
  val coinsDelta: Int = 0,
  val description: String,
  val timestamp: Long = System.currentTimeMillis(),
  val status: TransactionStatus = TransactionStatus.SUCCESS,
  val referenceId: String = "",
  val idempotencyKey: String = ""
)

enum class MonetizationStatus {
  NOT_APPLIED,
  UNDER_REVIEW,
  APPROVED,
  REJECTED
}

data class MonetizationApplication(
  val id: String,
  val userId: String,
  val userName: String,
  val userHandle: String,
  val followersCount: Int,
  val eligibleViews: Long,
  val panNumber: String,
  val legalName: String,
  val category: String,
  val appliedAt: Long = System.currentTimeMillis(),
  val status: MonetizationStatus = MonetizationStatus.APPROVED,
  val rejectionReason: String = ""
)

enum class WithdrawalStatus {
  PENDING,
  PAID,
  REJECTED
}

data class WithdrawalRequest(
  val id: String,
  val userId: String,
  val creatorName: String,
  val amountRupees: Double,
  val payoutDestination: String,
  val requestedAt: Long = System.currentTimeMillis(),
  val status: WithdrawalStatus = WithdrawalStatus.PENDING,
  val utrNumber: String = "",
  val remarks: String = ""
)

enum class SubscriptionPlan(val title: String, val priceRupees: Double, val validityDays: Int) {
  MONTHLY("Monthly VIP", 199.0, 30),
  YEARLY("Yearly VIP Pro", 1799.0, 365)
}

data class PremiumSubscription(
  val userId: String,
  val isPremium: Boolean = false,
  val plan: SubscriptionPlan = SubscriptionPlan.MONTHLY,
  val validUntil: Long = System.currentTimeMillis() + (25L * 24 * 3600 * 1000),
  val autoRenew: Boolean = true,
  val googlePlayOrderId: String = "GPA.3391-4921-9214-88910"
)

data class AdminRevenueMetrics(
  val totalUsersCount: Int = 18450,
  val activeCreatorsCount: Int = 1420,
  val totalRevenueRupees: Double = 849200.0,
  val todayRevenueRupees: Double = 18420.0,
  val monthRevenueRupees: Double = 312500.0,
  val adMobRevenueRupees: Double = 168900.0,
  val liveGiftRevenueRupees: Double = 284500.0,
  val premiumSubRevenueRupees: Double = 395800.0,
  val totalPaidToCreatorsRupees: Double = 412000.0,
  val pendingPayoutsAmountRupees: Double = 48500.0,
  val creatorRevenueSharePercent: Double = 70.0, // 70% Creator, 30% Platform
  val minWithdrawalLimitRupees: Double = 1000.0
)

enum class ReportStatus {
  PENDING,
  RESOLVED,
  DISMISSED
}

data class ContentReport(
  val id: String,
  val targetType: String, // "POST" or "USER"
  val targetId: String,
  val targetLabel: String,
  val reason: String,
  val notes: String,
  val reportedAt: Long = System.currentTimeMillis(),
  val status: ReportStatus = ReportStatus.PENDING,
  val actionTaken: String = ""
)

data class AdminManagedUser(
  val user: User,
  val isSuspended: Boolean = false,
  val isFirestoreSynced: Boolean = false,
  val authProvider: String = "Local"
)

data class SparkMvpConfig(
  val sparkZeroCostMode: Boolean = true,
  val mediaUploadPending: Boolean = true,
  val googleAdMobTestMode: Boolean = true,
  val maintenanceMode: Boolean = false,
  val smsOtpBlazeWarningVisible: Boolean = true
)

