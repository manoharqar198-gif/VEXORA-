package com.example.zevora.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ContactSupport
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.zevora.data.firebase.GoogleAuthManager
import com.example.zevora.data.firebase.PhoneAuthManager
import com.example.zevora.model.*
import com.example.zevora.ui.Screen
import com.example.zevora.ui.ZevoraViewModel
import com.example.zevora.ui.components.*
import com.example.zevora.ui.theme.ZevoraError
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  viewModel: ZevoraViewModel,
  onNavigateBack: () -> Unit
) {
  val language by viewModel.appLanguage.collectAsState()
  val themeMode by viewModel.appThemeMode.collectAsState()
  val notifSettings by viewModel.notificationSettings.collectAsState()
  val currentUser by viewModel.currentUser.collectAsState()
  val networkQuality by viewModel.networkQuality.collectAsState()
  val dataSaverPrefs by viewModel.dataSaverPreferences.collectAsState()
  val firebaseDiag by viewModel.firebaseDiagnostics.collectAsState()
  val isAccountLinking by viewModel.isAccountLinking.collectAsState()
  val accountLinkMessage by viewModel.accountLinkMessage.collectAsState()
  val accountLinkError by viewModel.accountLinkError.collectAsState()

  // Dialog states
  var showLanguageDialog by remember { mutableStateOf(false) }
  var showThemeDialog by remember { mutableStateOf(false) }
  var showNotificationDialog by remember { mutableStateOf(false) }
  var showNetworkSimDialog by remember { mutableStateOf(false) }
  var showVideoQualityDialog by remember { mutableStateOf(false) }
  var showClearCacheDialog by remember { mutableStateOf(false) }
  var showDeleteAccountDialog by remember { mutableStateOf(false) }
  var showHelpSupportDialog by remember { mutableStateOf(false) }
  var showTermsDialog by remember { mutableStateOf(false) }
  var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
  var showContactUsDialog by remember { mutableStateOf(false) }
  var showLogoutConfirmDialog by remember { mutableStateOf(false) }
  var snackbarText by remember { mutableStateOf<String?>(null) }

  // Safe Phone OTP Linking dialog state
  var showLinkPhoneDialog by remember { mutableStateOf(false) }
  var linkVerificationId by remember { mutableStateOf<String?>(null) }
  var linkMaskedPhone by remember { mutableStateOf<String?>(null) }
  var linkE164Phone by remember { mutableStateOf<String?>(null) }
  var linkCanResendAt by remember { mutableLongStateOf(0L) }
  var linkResendCount by remember { mutableIntStateOf(0) }
  var linkResendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
  var linkDialogBusy by remember { mutableStateOf(false) }
  var linkDialogError by remember { mutableStateOf<String?>(null) }

  val context = LocalContext.current
  val logoutScope = viewModel.authOperationScope

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = if (language == AppLanguage.HINDI) "सेटिंग्स (Settings)" else "Settings",
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp
            )
            Text(
              text = "Preferences, Security, Support & Policies",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("settings_back_btn")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(vertical = 16.dp)
    ) {
      // User Profile Header preview
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth().clickable { viewModel.navigateTo(Screen.PROFILE) }
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(50.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = currentUser.name.take(1),
                  fontWeight = FontWeight.Bold,
                  fontSize = 20.sp,
                  color = Color.White
                )
              }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(currentUser.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
              val contactSubtitle = currentUser.email.ifBlank { currentUser.phone.ifBlank { "Verified Account" } }
              Text("@${currentUser.username} • $contactSubtitle", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
          }
        }
      }

      // Firebase Connectivity & Live Diagnostics Card (Honest real check)
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_firebase_diagnostics_card")
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              "🔥 Firebase Spark Free Diagnostics",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = Color.White
            )
            Text("Project ID: ${firebaseDiag.projectId}", fontSize = 11.sp, color = Color(0xFFCBD5E1))
            Text("Database ID: ${firebaseDiag.firestoreDatabaseId}", fontSize = 11.sp, color = Color(0xFF94A3B8))
            Text(
              "Access Mode: ${firebaseDiag.authProviderSummary}",
              fontSize = 11.sp,
              color = Color(0xFF10B981)
            )
            Text(
              "Firestore Write: ${firebaseDiag.firestoreWriteStatus}",
              fontSize = 11.sp,
              color = if (firebaseDiag.isFirestoreWriteSuccess) Color(0xFF10B981) else Color(0xFFCBD5E1)
            )
            Text(
              "Firestore Read: ${firebaseDiag.firestoreReadStatus}",
              fontSize = 11.sp,
              color = if (firebaseDiag.isFirestoreReadSuccess) Color(0xFF10B981) else Color(0xFFCBD5E1)
            )
            Text(
              "Firebase Storage: ${firebaseDiag.storageStatus}",
              fontSize = 11.sp,
              color = Color(0xFFFBBF24)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
              onClick = { viewModel.runLiveFirebaseDiagnostics() },
              enabled = !firebaseDiag.isRunning,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("firebase_run_diagnostics_btn")
            ) {
              if (firebaseDiag.isRunning) {
                CircularProgressIndicator(
                  color = Color.White,
                  strokeWidth = 2.dp,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Testing Firestore Read & Write...", fontSize = 12.sp)
              } else {
                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Test Firebase Firestore Read & Write", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // Safe Verified Account Linking Card (Phone OTP + Google on the same Firebase UID)
      item {
        LinkedAccountsCard(
          diagnostics = firebaseDiag,
          isLinking = isAccountLinking,
          linkMessage = accountLinkMessage,
          linkError = accountLinkError,
          onLinkGoogleClick = {
            val cm = try {
              CredentialManager.create(context)
            } catch (_: Exception) {
              null
            }
            if (cm == null) {
              viewModel.setAccountLinkError("इस डिवाइस पर Credential Manager उपलब्ध नहीं है।")
              return@LinkedAccountsCard
            }
            viewModel.setAccountLinking(true)
            GoogleAuthManager.linkGoogleAccount(
              context = context,
              credentialManager = cm,
              scope = logoutScope,
              onLinkSuccess = { fbUser ->
                viewModel.resolveFirebaseSession(
                  uid = fbUser.uid,
                  displayName = fbUser.displayName,
                  email = fbUser.email,
                  phoneNumber = fbUser.phoneNumber,
                  photoUrl = fbUser.photoUrl?.toString(),
                  providerIds = fbUser.providerData.map { it.providerId }
                )
                viewModel.setAccountLinkSuccess(
                  "Google Account (${fbUser.email ?: "Gmail"}) सफलतापूर्वक इस VEXORA खाते से लिंक हो गया!"
                )
              },
              onLinkCancelled = { cancelledMsg ->
                viewModel.setAccountLinking(false)
                if (!cancelledMsg.isNullOrBlank()) {
                  viewModel.setAccountLinkError(cancelledMsg)
                }
              },
              onLinkError = { errMsg ->
                viewModel.setAccountLinkError(errMsg)
              }
            )
          },
          onLinkPhoneClick = {
            viewModel.clearAccountLinkFeedback()
            linkVerificationId = null
            linkMaskedPhone = null
            linkE164Phone = null
            linkCanResendAt = 0L
            linkResendCount = 0
            linkResendToken = null
            linkDialogBusy = false
            linkDialogError = null
            showLinkPhoneDialog = true
          },
          onDismissFeedback = {
            viewModel.clearAccountLinkFeedback()
          }
        )
      }

      // 1. Language बदलना
      item {
        SettingsItemRow(
          icon = Icons.Default.Translate,
          title = "Language बदलना",
          subtitle = "${language.nativeName} (${language.displayName})",
          tag = "setting_language_row",
          onClick = { showLanguageDialog = true }
        )
      }

      // 2. Dark Mode / Light Mode
      item {
        SettingsItemRow(
          icon = Icons.Default.BrightnessMedium,
          title = "Dark Mode / Light Mode",
          subtitle = themeMode.title,
          tag = "setting_theme_row",
          onClick = { showThemeDialog = true }
        )
      }

      // 3. Notification Settings
      item {
        SettingsItemRow(
          icon = Icons.Default.Notifications,
          title = "Notification Settings",
          subtitle = "Push, Likes, Comments, Messages, Alerts",
          tag = "setting_notifications_row",
          onClick = { showNotificationDialog = true }
        )
      }

      // 4. Privacy Settings (Link to Security & Privacy screen)
      item {
        SettingsItemRow(
          icon = Icons.Default.Security,
          title = "Privacy Settings (सुरक्षा और गोपनीयता)",
          subtitle = "Block list, Private Account, Comment Controls",
          tag = "setting_privacy_row",
          onClick = { viewModel.navigateTo(Screen.SECURITY_PRIVACY) }
        )
      }

      // Monetization & Earning Section Header
      item {
        Text(
          text = "💰 कमाई और वॉलेट (Monetization & Wallet)",
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp,
          color = Color(0xFFFBBF24),
          modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
        )
      }

      // Creator Wallet Row
      item {
        SettingsItemRow(
          icon = Icons.Default.AccountBalanceWallet,
          title = "Creator Wallet & UPI/Bank Payouts",
          subtitle = "Available: ₹4,280 • Payout settings & Withdrawal requests",
          tag = "setting_creator_wallet_row",
          onClick = { viewModel.navigateTo(Screen.CREATOR_WALLET) }
        )
      }

      // VIP Pro Membership Row
      item {
        SettingsItemRow(
          icon = Icons.Default.WorkspacePremium,
          title = "VEXORA VIP Pro Membership",
          subtitle = "100% Zero Ads, 4K HDR Streaming & VIP Gold Badge",
          tag = "setting_vip_pro_row",
          onClick = { viewModel.navigateTo(Screen.PREMIUM_UPGRADE) }
        )
      }

      // Admin Panel Row (Users, Posts, Reports, App Settings & Earnings)
      item {
        SettingsItemRow(
          icon = Icons.Default.AdminPanelSettings,
          title = "Admin Panel (Users, Posts, Reports & Settings)",
          subtitle = "Users moderation, posts, reports, Spark ₹0 guardrails & payouts",
          tag = "setting_admin_panel_row",
          onClick = { viewModel.navigateTo(Screen.ADMIN_PANEL) }
        )
      }

      // Performance Section Header
      item {
        Text(
          text = "⚡ स्पीड, नेटवर्क और डेटा सेवर (Performance)",
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
        )
      }

      // 5a. Data Saver Toggle
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth().testTag("setting_data_saver_card")
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
              modifier = Modifier.size(40.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.DataSaverOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text("Data Saver Mode (डेटा सेवर)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
              Text("कम इंटरनेट में वीडियो व फोटो को कम्प्रेस करता है", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
              checked = dataSaverPrefs.isDataSaverEnabled,
              onCheckedChange = { isEnabled ->
                viewModel.updateDataSaverPreferences(
                  dataSaverPrefs.copy(isDataSaverEnabled = isEnabled)
                )
              }
            )
          }
        }
      }

      // 5b. Network Speed & Quality Simulator
      item {
        SettingsItemRow(
          icon = Icons.Default.NetworkCheck,
          title = "नेटवर्क स्पीड और मोड (${networkQuality.label})",
          subtitle = "${networkQuality.speedHint} • 2G/3G/4G/5G टेस्ट करें",
          tag = "setting_network_mode_row",
          onClick = { showNetworkSimDialog = true }
        )
      }

      // 5c. Video Streaming Quality
      item {
        SettingsItemRow(
          icon = Icons.Default.VideoSettings,
          title = "वीडियो प्लेबैक क्वालिटी",
          subtitle = "${dataSaverPrefs.videoQuality.title} • नेटवर्क के अनुसार अनुकूलित",
          tag = "setting_video_quality_row",
          onClick = { showVideoQualityDialog = true }
        )
      }

      // 5d. Clear App Cache & RAM
      item {
        SettingsItemRow(
          icon = Icons.Default.CleaningServices,
          title = "App Cache और RAM साफ़ करें",
          subtitle = "अनावश्यक फाइल्स हटाएं और ऐप को तेज बनाएं (142 MB)",
          tag = "setting_clear_cache_row",
          onClick = { showClearCacheDialog = true }
        )
      }

      // 6. Help और Support
      item {
        SettingsItemRow(
          icon = Icons.AutoMirrored.Filled.HelpOutline,
          title = "Help और Support",
          subtitle = "FAQs, troubleshooting, and user guides",
          tag = "setting_help_row",
          onClick = { showHelpSupportDialog = true }
        )
      }

      // 7. Terms & Conditions
      item {
        SettingsItemRow(
          icon = Icons.Default.Description,
          title = "Terms & Conditions",
          subtitle = "Community guidelines and user terms",
          tag = "setting_terms_row",
          onClick = { showTermsDialog = true }
        )
      }

      // 8. Privacy Policy
      item {
        SettingsItemRow(
          icon = Icons.Default.PrivacyTip,
          title = "Privacy Policy",
          subtitle = "How VEXORA protects and handles your data",
          tag = "setting_privacy_policy_row",
          onClick = { showPrivacyPolicyDialog = true }
        )
      }

      // 9. Contact Us
      item {
        SettingsItemRow(
          icon = Icons.AutoMirrored.Filled.ContactSupport,
          title = "Contact Us",
          subtitle = "support@zevora.app • Toll-free • WhatsApp",
          tag = "setting_contact_us_row",
          onClick = { showContactUsDialog = true }
        )
      }

      // 10. Account Log Out (Phone OTP / Google)
      item {
        SettingsItemRow(
          icon = Icons.AutoMirrored.Filled.Logout,
          title = "लॉगआउट (Sign Out)",
          subtitle = "आपका डेटा और पोस्ट सुरक्षित रहेंगे • लॉगिन स्क्रीन पर जाएं",
          titleColor = Color(0xFFF59E0B),
          iconTint = Color(0xFFF59E0B),
          tag = "setting_logout_row",
          onClick = { showLogoutConfirmDialog = true }
        )
      }

      // 11. Account Delete करना
      item {
        SettingsItemRow(
          icon = Icons.Default.DeleteForever,
          title = "Account Delete करना",
          subtitle = "Permanently remove your account and data",
          titleColor = ZevoraError,
          iconTint = ZevoraError,
          tag = "setting_delete_acc_row",
          onClick = { showDeleteAccountDialog = true }
        )
      }

      item {
        Box(
          modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 24.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            "VEXORA v2.0 (Official Build)\nMade with ❤️ for Indian & Global Creators",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline
          )
        }
      }
    }
  }

  // Modals for Settings
  if (showLanguageDialog) {
    AlertDialog(
      onDismissRequest = { showLanguageDialog = false },
      title = { Text("भाषा चुनें / Choose Language", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
      text = {
        Column {
          AppLanguage.values().forEach { lang ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  viewModel.setLanguage(lang)
                  showLanguageDialog = false
                }
                .padding(vertical = 10.dp, horizontal = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = language == lang,
                onClick = {
                  viewModel.setLanguage(lang)
                  showLanguageDialog = false
                }
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(lang.nativeName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(lang.displayName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showLanguageDialog = false }) { Text("Done") }
      }
    )
  }

  if (showThemeDialog) {
    AlertDialog(
      onDismissRequest = { showThemeDialog = false },
      title = { Text("Theme Selection", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
      text = {
        Column {
          AppThemeMode.values().forEach { mode ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  viewModel.setThemeMode(mode)
                  showThemeDialog = false
                }
                .padding(vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(selected = themeMode == mode, onClick = {
                viewModel.setThemeMode(mode)
                showThemeDialog = false
              })
              Spacer(modifier = Modifier.width(8.dp))
              Text(mode.title, fontSize = 14.sp)
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showThemeDialog = false }) { Text("Close") }
      }
    )
  }

  if (showNotificationDialog) {
    AlertDialog(
      onDismissRequest = { showNotificationDialog = false },
      title = { Text("Notification Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
      text = {
        Column {
          NotificationToggleRow("Push Notifications", "Master toggle", notifSettings.pushNotifications) {
            viewModel.updateNotificationSettings(notifSettings.copy(pushNotifications = it))
          }
          NotificationToggleRow("Likes & Reactions", "Notify on posts & reels", notifSettings.likesReactions) {
            viewModel.updateNotificationSettings(notifSettings.copy(likesReactions = it))
          }
          NotificationToggleRow("Comments", "Replies & mentions", notifSettings.comments) {
            viewModel.updateNotificationSettings(notifSettings.copy(comments = it))
          }
          NotificationToggleRow("Direct Messages", "Incoming chat alerts", notifSettings.directMessages) {
            viewModel.updateNotificationSettings(notifSettings.copy(directMessages = it))
          }
          NotificationToggleRow("Account Safety Alerts", "Blocked users & safety notices", notifSettings.securityAlerts) {
            viewModel.updateNotificationSettings(notifSettings.copy(securityAlerts = it))
          }
        }
      },
      confirmButton = {
        Button(onClick = { showNotificationDialog = false }) { Text("Save") }
      }
    )
  }

  if (showNetworkSimDialog) {
    AlertDialog(
      onDismissRequest = { showNetworkSimDialog = false },
      containerColor = Color(0xFF0F172A),
      title = { Text("नेटवर्क टेस्ट मोड चुनें", fontWeight = FontWeight.Bold, color = Color.White) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(
            NetworkQuality.EXCELLENT to "5G / High-Speed Wi-Fi (Ultra Fast)",
            NetworkQuality.GOOD to "4G LTE (Normal speed)",
            NetworkQuality.SLOW to "2G / 3G Slow Network (कमजोर इंटरनेट)",
            NetworkQuality.OFFLINE to "Offline Mode (इंटरनेट बंद)"
          ).forEach { (quality, label) ->
            val isSelected = networkQuality == quality
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                  viewModel.setSimulatedNetwork(quality)
                  showNetworkSimDialog = false
                },
              color = if (isSelected) Color(0xFF1E293B) else Color.Transparent,
              border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6)) else null
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  label,
                  color = Color.White,
                  fontSize = 13.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF8B5CF6))
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showNetworkSimDialog = false }) { Text("Close", color = Color(0xFF8B5CF6)) }
      }
    )
  }

  if (showVideoQualityDialog) {
    AlertDialog(
      onDismissRequest = { showVideoQualityDialog = false },
      containerColor = Color(0xFF0F172A),
      title = { Text("वीडियो क्वालिटी चुनें", fontWeight = FontWeight.Bold, color = Color.White) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          com.example.zevora.model.VideoQualityOption.values().forEach { option ->
            val isSelected = dataSaverPrefs.videoQuality == option
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                  viewModel.updateDataSaverPreferences(
                    dataSaverPrefs.copy(
                      videoQuality = option,
                      isDataSaverEnabled = option == com.example.zevora.model.VideoQualityOption.DATA_SAVER
                    )
                  )
                  showVideoQualityDialog = false
                },
              color = if (isSelected) Color(0xFF1E293B) else Color.Transparent,
              border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6)) else null
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(option.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                  Text(option.description, color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
                if (isSelected) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF8B5CF6))
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showVideoQualityDialog = false }) { Text("Done", color = Color(0xFF8B5CF6)) }
      }
    )
  }

  if (showClearCacheDialog) {
    AlertDialog(
      onDismissRequest = { showClearCacheDialog = false },
      containerColor = Color(0xFF0F172A),
      title = { Text("App Cache साफ़ करें?", fontWeight = FontWeight.Bold, color = Color.White) },
      text = {
        Text(
          "कैश साफ़ करने से अस्थायी मीडिया फाइल्स हट जाएंगी और 142 MB RAM व स्टोरेज खाली हो जाएगी। आपका कोई भी पोस्ट या डेटा डिलीट नहीं होगा।",
          color = Color(0xFF94A3B8),
          fontSize = 13.sp,
          lineHeight = 18.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.clearLocalCache()
            showClearCacheDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
        ) {
          Text("साफ़ करें (Clear 142 MB)", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearCacheDialog = false }) { Text("Cancel", color = Color(0xFF94A3B8)) }
      }
    )
  }

  if (showLogoutConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showLogoutConfirmDialog = false },
      containerColor = Color(0xFF0F172A),
      title = { Text("VEXORA से लॉगआउट करें?", fontWeight = FontWeight.Bold, color = Color.White) },
      text = {
        Text(
          "लॉगआउट करने पर आपकी कोई भी प्रोफ़ाइल, पोस्ट या वॉलेट डेटा डिलीट नहीं होगा। आप दोबारा Mobile OTP या Google Account से साइन इन कर सकते हैं।",
          color = Color(0xFF94A3B8),
          fontSize = 13.sp,
          lineHeight = 19.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showLogoutConfirmDialog = false
            val cm = try {
              CredentialManager.create(context)
            } catch (_: Exception) {
              null
            }
            if (cm != null) {
              GoogleAuthManager.signOut(
                context = context,
                credentialManager = cm,
                scope = logoutScope,
                onSignOutComplete = {
                  viewModel.clearAuthenticatedSession()
                }
              )
            } else {
              viewModel.clearAuthenticatedSession()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
        ) {
          Text("लॉगआउट करें", color = Color.Black, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showLogoutConfirmDialog = false }) {
          Text("रद्द करें", color = Color(0xFF94A3B8))
        }
      }
    )
  }

  if (showDeleteAccountDialog) {
    DeleteAccountDialog(
      onDismiss = { showDeleteAccountDialog = false },
      onConfirmDelete = { reason ->
        viewModel.deleteAccount(reason)
        showDeleteAccountDialog = false
        snackbarText = "Account deletion initiated. You have 30 days to cancel."
      }
    )
  }

  if (showHelpSupportDialog) {
    HelpAndSupportDialog(
      onDismiss = { showHelpSupportDialog = false },
      onOpenContactUs = { showContactUsDialog = true },
      language = language
    )
  }

  if (showTermsDialog) {
    LegalDocumentViewerDialog(
      title = "Terms & Conditions",
      onDismiss = { showTermsDialog = false },
      isPrivacyPolicy = false
    )
  }

  if (showPrivacyPolicyDialog) {
    LegalDocumentViewerDialog(
      title = "Privacy Policy",
      onDismiss = { showPrivacyPolicyDialog = false },
      isPrivacyPolicy = true
    )
  }

  if (showContactUsDialog) {
    ContactUsDialog(
      onDismiss = { showContactUsDialog = false }
    )
  }

  if (showLinkPhoneDialog) {
    LinkPhoneOtpDialog(
      isBusy = linkDialogBusy,
      verificationId = linkVerificationId,
      maskedPhone = linkMaskedPhone,
      e164Phone = linkE164Phone,
      canResendAtMillis = linkCanResendAt,
      resendCount = linkResendCount,
      errorText = linkDialogError,
      onDismiss = {
        if (!linkDialogBusy) {
          showLinkPhoneDialog = false
        }
      },
      onSendOtp = { countryCode, digits ->
        PhoneAuthManager.validateAndFormatPhone(countryCode, digits)
          .onFailure { err ->
            linkDialogError = err.message
          }
          .onSuccess { formattedPhone ->
            linkDialogBusy = true
            linkDialogError = null
            linkE164Phone = formattedPhone
            linkMaskedPhone = PhoneAuthManager.maskPhoneNumber(formattedPhone)
            PhoneAuthManager.sendOtp(
              context = context,
              e164Phone = formattedPhone,
              resendToken = null,
              onAutoVerified = { credential ->
                logoutScope.launch {
                  PhoneAuthManager.linkPhoneCredential(credential)
                    .onSuccess { fbUser ->
                      linkDialogBusy = false
                      showLinkPhoneDialog = false
                      viewModel.resolveFirebaseSession(
                        uid = fbUser.uid,
                        displayName = fbUser.displayName,
                        email = fbUser.email,
                        phoneNumber = fbUser.phoneNumber,
                        photoUrl = fbUser.photoUrl?.toString(),
                        providerIds = fbUser.providerData.map { it.providerId }
                      )
                      viewModel.setAccountLinkSuccess(
                        "मोबाइल नंबर (${fbUser.phoneNumber ?: formattedPhone}) सफलतापूर्वक इस VEXORA खाते से लिंक हो गया!"
                      )
                    }
                    .onFailure { err ->
                      linkDialogBusy = false
                      linkDialogError = err.message
                    }
                }
              },
              onCodeSent = { vid, token ->
                linkDialogBusy = false
                linkDialogError = null
                linkVerificationId = vid
                linkResendToken = token
                linkResendCount = 0
                linkCanResendAt = System.currentTimeMillis() + PhoneAuthManager.resendCooldownMillis(0)
              },
              onError = { errMsg ->
                linkDialogBusy = false
                linkDialogError = errMsg
              }
            )
          }
      },
      onResendOtp = { formattedPhone ->
        if (linkResendCount >= PhoneAuthManager.MAX_OTP_RESENDS) {
          linkDialogError = "OTP दोबारा भेजने की अधिकतम सीमा (${PhoneAuthManager.MAX_OTP_RESENDS} बार) पूरी हो गई है।"
          return@LinkPhoneOtpDialog
        }
        val nextAttempt = linkResendCount + 1
        linkDialogBusy = true
        linkDialogError = null
        PhoneAuthManager.sendOtp(
          context = context,
          e164Phone = formattedPhone,
          resendToken = linkResendToken,
          onAutoVerified = { credential ->
            logoutScope.launch {
              PhoneAuthManager.linkPhoneCredential(credential)
                .onSuccess { fbUser ->
                  linkDialogBusy = false
                  showLinkPhoneDialog = false
                  viewModel.resolveFirebaseSession(
                    uid = fbUser.uid,
                    displayName = fbUser.displayName,
                    email = fbUser.email,
                    phoneNumber = fbUser.phoneNumber,
                    photoUrl = fbUser.photoUrl?.toString(),
                    providerIds = fbUser.providerData.map { it.providerId }
                  )
                  viewModel.setAccountLinkSuccess(
                    "मोबाइल नंबर (${fbUser.phoneNumber ?: formattedPhone}) सफलतापूर्वक लिंक हो गया!"
                  )
                }
                .onFailure { err ->
                  linkDialogBusy = false
                  linkDialogError = err.message
                }
            }
          },
          onCodeSent = { vid, token ->
            linkDialogBusy = false
            linkDialogError = null
            linkVerificationId = vid
            linkResendToken = token
            linkResendCount = nextAttempt
            linkCanResendAt = System.currentTimeMillis() + PhoneAuthManager.resendCooldownMillis(nextAttempt)
          },
          onError = { errMsg ->
            linkDialogBusy = false
            linkDialogError = errMsg
          }
        )
      },
      onVerifyAndLink = { vid, otpCode ->
        linkDialogBusy = true
        linkDialogError = null
        logoutScope.launch {
          PhoneAuthManager.verifyOtpAndLink(vid, otpCode)
            .onSuccess { fbUser ->
              linkDialogBusy = false
              showLinkPhoneDialog = false
              viewModel.resolveFirebaseSession(
                uid = fbUser.uid,
                displayName = fbUser.displayName,
                email = fbUser.email,
                phoneNumber = fbUser.phoneNumber,
                photoUrl = fbUser.photoUrl?.toString(),
                providerIds = fbUser.providerData.map { it.providerId }
              )
              viewModel.setAccountLinkSuccess(
                "मोबाइल नंबर (${fbUser.phoneNumber ?: linkE164Phone.orEmpty()}) सफलतापूर्वक इस VEXORA खाते से लिंक हो गया!"
              )
            }
            .onFailure { err ->
              linkDialogBusy = false
              linkDialogError = err.message
            }
        }
      },
      onResetToPhoneInput = {
        linkVerificationId = null
        linkResendCount = 0
        linkDialogError = null
      }
    )
  }
}

@Composable
private fun SettingsItemRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  tag: String,
  onClick: () -> Unit,
  titleColor: Color = Color.Unspecified,
  iconTint: Color? = null
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag(tag)
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = CircleShape,
        color = (iconTint ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.15f),
        modifier = Modifier.size(40.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint ?: MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
        }
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          color = if (titleColor != Color.Unspecified) titleColor else MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = subtitle,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
  }
}

@Composable
private fun NotificationToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
      Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}
