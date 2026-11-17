package com.example.zevora.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zevora.model.*
import com.example.zevora.ui.ZevoraViewModel
import com.example.zevora.ui.theme.ZevoraError
import com.example.zevora.ui.theme.ZevoraSecondary
import com.example.zevora.ui.theme.ZevoraSuccess
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEarningsPanelScreen(
  viewModel: ZevoraViewModel,
  onNavigateBack: () -> Unit
) {
  val metrics by viewModel.adminMetrics.collectAsState()
  val monetizationApp by viewModel.monetizationApplication.collectAsState()
  val withdrawalRequests by viewModel.withdrawalRequests.collectAsState()
  val managedUsers by viewModel.managedUsers.collectAsState()
  val feedPosts by viewModel.feedPosts.collectAsState()
  val contentReports by viewModel.contentReports.collectAsState()
  val sparkConfig by viewModel.sparkMvpConfig.collectAsState()

  var selectedTab by remember { mutableIntStateOf(0) }
  val pendingReportsCount = contentReports.count { it.status == ReportStatus.PENDING }
  val tabs = listOf(
    "Users (${managedUsers.size})",
    "Posts (${feedPosts.size})",
    "Reports ($pendingReportsCount)",
    "App Settings",
    "Revenue",
    "Approvals & Payouts"
  )

  var userSyncStatus by remember { mutableStateOf<String?>(null) }
  var isSyncingUsers by remember { mutableStateOf(false) }

  var showUtrDialogForRequest by remember { mutableStateOf<WithdrawalRequest?>(null) }
  var utrInput by remember { mutableStateOf("") }
  var showRejectDialogForRequest by remember { mutableStateOf<WithdrawalRequest?>(null) }
  var rejectReasonInput by remember { mutableStateOf("Incorrect Bank IFSC / Account mismatch") }

  BackHandler {
    onNavigateBack()
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("VEXORA Admin Panel 🛡️", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
              "Users, Posts, Reports, Spark ₹0 Settings & Payouts",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("admin_back_button")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      ScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color(0xFF0F172A),
        contentColor = Color.White,
        edgePadding = 12.dp
      ) {
        tabs.forEachIndexed { index, title ->
          Tab(
            selected = selectedTab == index,
            onClick = { selectedTab = index },
            modifier = Modifier.testTag("admin_tab_$index"),
            text = {
              Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
              )
            }
          )
        }
      }

      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
      ) {
        when (selectedTab) {
          0 -> {
            // TAB 0: USERS DIRECTORY & MODERATION
            item {
              Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(
                  modifier = Modifier.padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text("User Directory (${managedUsers.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                      Text("Verify creators, suspend policy violators or sync Cloud Firestore users.", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                    Button(
                      onClick = {
                        isSyncingUsers = true
                        userSyncStatus = null
                        viewModel.adminSyncUsersFromFirestore { res ->
                          isSyncingUsers = false
                          userSyncStatus = res.fold(
                            onSuccess = { count -> "Synced $count user(s) from Firestore server." },
                            onFailure = { err -> "Sync failed: ${err.localizedMessage ?: "Protected by Firestore Security Rules"}" }
                          )
                        }
                      },
                      enabled = !isSyncingUsers,
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier
                        .height(48.dp)
                        .testTag("admin_sync_users_btn")
                    ) {
                      if (isSyncingUsers) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                      } else {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sync Firestore", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                  userSyncStatus?.let { statusMsg ->
                    Text(
                      text = statusMsg,
                      fontSize = 11.sp,
                      color = if (statusMsg.startsWith("Synced")) ZevoraSuccess else Color(0xFFFBBF24)
                    )
                  }
                }
              }
            }

            items(managedUsers, key = { it.user.id }) { managed ->
              AdminUserCard(
                managed = managed,
                onToggleVerified = { viewModel.adminToggleUserVerified(managed.user.id) },
                onToggleSuspended = { viewModel.adminToggleUserSuspended(managed.user.id) }
              )
            }
          }

          1 -> {
            // TAB 1: POSTS MODERATION
            item {
              Text(
                text = "Posts Moderation (${feedPosts.size} Active Posts)",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
              )
            }

            if (feedPosts.isEmpty()) {
              item {
                EmptyAdminStateCard(
                  icon = Icons.Default.DynamicFeed,
                  title = "No posts in feed",
                  subtitle = "All posts have been moderated or removed."
                )
              }
            } else {
              items(feedPosts, key = { it.id }) { post ->
                AdminPostModerationCard(
                  post = post,
                  onDeletePost = { viewModel.adminDeletePost(post.id) }
                )
              }
            }
          }

          2 -> {
            // TAB 2: USER & POST REPORTS
            item {
              Text(
                text = "Safety & Content Reports ($pendingReportsCount Pending)",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
              )
            }

            if (contentReports.isEmpty()) {
              item {
                EmptyAdminStateCard(
                  icon = Icons.Default.VerifiedUser,
                  title = "Abhi koi report nahi hai",
                  subtitle = "Jab users kisi Post ya Account ko report karenge, to wo yahan review ke liye dikhegi."
                )
              }
            } else {
              items(contentReports, key = { it.id }) { report ->
                AdminReportCard(
                  report = report,
                  onResolveAndRemove = { viewModel.adminResolveReport(report.id, removeTargetPost = true) },
                  onResolveOnly = { viewModel.adminResolveReport(report.id, removeTargetPost = false) },
                  onDismiss = { viewModel.adminDismissReport(report.id) }
                )
              }
            }
          }

          3 -> {
            // TAB 3: SPARK FREE MVP APP SETTINGS & GUARDRAILS
            item {
              Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, ZevoraSuccess.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  Text("🔥 Spark Free Plan Guardrails (₹0 Mode)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                  Text(
                    "Project: gen-lang-client-0433994845 • No Blaze Billing • No Paid Third-Party APIs",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                  )

                  AdminSettingSwitchRow(
                    title = "Spark Zero-Cost Guard Mode",
                    subtitle = "Blocks paid Cloud Functions/paid Storage; uses free Google Sign-In & Firestore quotas only",
                    checked = sparkConfig.sparkZeroCostMode,
                    tag = "admin_spark_zero_cost_switch",
                    onCheckedChange = {
                      viewModel.adminUpdateSparkMvpConfig(sparkConfig.copy(sparkZeroCostMode = it))
                    }
                  )
                  AdminSettingSwitchRow(
                    title = "Keep Cloud Media Upload Pending",
                    subtitle = "Keeps Firebase Storage photo/video uploads disabled to protect ₹0 Spark Plan",
                    checked = sparkConfig.mediaUploadPending,
                    tag = "admin_media_pending_switch",
                    onCheckedChange = {
                      viewModel.adminUpdateSparkMvpConfig(sparkConfig.copy(mediaUploadPending = it))
                    }
                  )
                  AdminSettingSwitchRow(
                    title = "Google AdMob Test Ads Mode",
                    subtitle = "Serves official Google test Ad Units so no invalid click penalty happens during testing",
                    checked = sparkConfig.googleAdMobTestMode,
                    tag = "admin_admob_test_switch",
                    onCheckedChange = {
                      viewModel.adminUpdateSparkMvpConfig(sparkConfig.copy(googleAdMobTestMode = it))
                    }
                  )
                  AdminSettingSwitchRow(
                    title = "Maintenance Banner Mode",
                    subtitle = "Show maintenance notice across feed when performing database updates",
                    checked = sparkConfig.maintenanceMode,
                    tag = "admin_maintenance_switch",
                    onCheckedChange = {
                      viewModel.adminUpdateSparkMvpConfig(sparkConfig.copy(maintenanceMode = it))
                    }
                  )
                }
              }
            }

            item {
              Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                  Text("Platform Monetization Controls", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                  Text(
                    "Revenue Sharing Ratio: ${metrics.creatorRevenueSharePercent.toInt()}% Creator / ${(100 - metrics.creatorRevenueSharePercent).toInt()}% Platform",
                    fontSize = 13.sp,
                    color = ZevoraSecondary
                  )
                  Slider(
                    value = metrics.creatorRevenueSharePercent.toFloat(),
                    onValueChange = { viewModel.adminUpdateCommissionShare(it.toDouble()) },
                    valueRange = 50f..90f,
                    steps = 7,
                    modifier = Modifier.fillMaxWidth()
                  )
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("50% Min", fontSize = 11.sp, color = Color.LightGray)
                    Text("70% Recommended", fontSize = 11.sp, color = ZevoraSuccess, fontWeight = FontWeight.Bold)
                    Text("90% Max", fontSize = 11.sp, color = Color.LightGray)
                  }
                  HorizontalDivider(color = Color(0xFF334155))
                  Text("Google AdMob Integration Status", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                  Text(
                    "• App ID: ca-app-pub-3940256099942544~3347511713\n" +
                      "• Banner AdUnit: ca-app-pub-3940256099942544/6300978111\n" +
                      "• Interstitial Video: ca-app-pub-3940256099942544/1033173712\n" +
                      "• Anti-Fraud Rate Limiter: Active (45s cooldown)",
                    fontSize = 12.sp,
                    color = Color.LightGray
                  )
                }
              }
            }
          }

          4 -> {
            // TAB 4: REVENUE ANALYTICS
            item {
              Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(20.dp)) {
                  Text("TOTAL PLATFORM EARNINGS (INR)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA5B4FC))
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = "₹${String.format(Locale.getDefault(), "%,.2f", metrics.totalRevenueRupees)}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                  )
                  Spacer(modifier = Modifier.height(14.dp))
                  HorizontalDivider(color = Color(0xFF312E81))
                  Spacer(modifier = Modifier.height(14.dp))

                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                      Text("Today", fontSize = 11.sp, color = Color.LightGray)
                      Text("₹${String.format(Locale.getDefault(), "%,.2f", metrics.todayRevenueRupees)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ZevoraSuccess)
                    }
                    Column {
                      Text("This Month", fontSize = 11.sp, color = Color.LightGray)
                      Text("₹${String.format(Locale.getDefault(), "%,.2f", metrics.monthRevenueRupees)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                    }
                    Column {
                      Text("Paid to Creators", fontSize = 11.sp, color = Color.LightGray)
                      Text("₹${String.format(Locale.getDefault(), "%,.2f", metrics.totalPaidToCreatorsRupees)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA5B4FC))
                    }
                  }
                }
              }
            }

            item { Text("Revenue by Channel", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White) }
            item {
              RevenueChannelCard(
                title = "Google AdMob Ads",
                amount = metrics.adMobRevenueRupees,
                desc = "Feed Sponsored Banners & Reels Video Ads (eCPM ₹142.50)",
                icon = Icons.Default.AdsClick,
                color = Color(0xFF10B981)
              )
            }
            item {
              RevenueChannelCard(
                title = "Live Streaming Virtual Gifts",
                amount = metrics.liveGiftRevenueRupees,
                desc = "30% Platform Net Commission on Roses, Rockets & Diamonds",
                icon = Icons.Default.CardGiftcard,
                color = Color(0xFFEC4899)
              )
            }
            item {
              RevenueChannelCard(
                title = "VIP Pro Subscriptions",
                amount = metrics.premiumSubRevenueRupees,
                desc = "Monthly (₹199) and Annual (₹1,799) VIP Pro Pass billings",
                icon = Icons.Default.WorkspacePremium,
                color = Color(0xFFF59E0B)
              )
            }
          }

          5 -> {
            // TAB 5: CREATOR APPLICATIONS & WITHDRAWALS
            item {
              Text(
                "Monetization Applications (${if (monetizationApp.status == MonetizationStatus.UNDER_REVIEW) "1 Pending" else "Reviewed"})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
              )
            }
            item {
              Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column {
                      Text(monetizationApp.userName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                      Text("@${monetizationApp.userHandle} • ${monetizationApp.category}", fontSize = 12.sp, color = Color.LightGray)
                    }
                    StatusPill(
                      text = monetizationApp.status.name,
                      color = when (monetizationApp.status) {
                        MonetizationStatus.APPROVED -> ZevoraSuccess
                        MonetizationStatus.UNDER_REVIEW -> Color(0xFFFBBF24)
                        else -> Color(0xFFEF4444)
                      }
                    )
                  }
                  HorizontalDivider(color = Color(0xFF334155))
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Followers: ${monetizationApp.followersCount}", fontSize = 12.sp, color = Color.White)
                    Text("Views: ${monetizationApp.eligibleViews}", fontSize = 12.sp, color = Color.White)
                    Text("PAN: ${monetizationApp.panNumber}", fontSize = 12.sp, color = Color(0xFFA5B4FC), fontWeight = FontWeight.Bold)
                  }
                  if (monetizationApp.status == MonetizationStatus.UNDER_REVIEW) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                      Button(
                        onClick = { viewModel.adminApproveMonetization(monetizationApp.id) },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("admin_approve_kyc_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ZevoraSuccess)
                      ) {
                        Text("Approve Creator")
                      }
                      OutlinedButton(
                        onClick = { viewModel.adminRejectMonetization(monetizationApp.id, "PAN KYC verification failed") },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF4444))
                      ) {
                        Text("Reject", color = Color(0xFFEF4444))
                      }
                    }
                  }
                }
              }
            }

            item {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text("Creator Withdrawal Requests", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                Text("${withdrawalRequests.size} Total", fontSize = 12.sp, color = Color.LightGray)
              }
            }

            items(withdrawalRequests, key = { it.id }) { req ->
              Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column {
                      Text("₹${String.format(Locale.getDefault(), "%,.2f", req.amountRupees)}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.White)
                      Text(req.payoutDestination, fontSize = 12.sp, color = Color(0xFFA5B4FC))
                    }
                    StatusPill(
                      text = req.status.name,
                      color = when (req.status) {
                        WithdrawalStatus.PAID -> ZevoraSuccess
                        WithdrawalStatus.PENDING -> Color(0xFFFBBF24)
                        else -> Color(0xFFEF4444)
                      }
                    )
                  }
                  val reqDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(req.requestedAt))
                  Text("Requested by: ${req.creatorName} • $reqDate", fontSize = 11.sp, color = Color.LightGray)
                  if (req.utrNumber.isNotBlank()) {
                    Text("UTR Ref: ${req.utrNumber} (${req.remarks})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = ZevoraSuccess)
                  }
                  if (req.status == WithdrawalStatus.PENDING) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                      Button(
                        onClick = {
                          utrInput = "UTR${System.currentTimeMillis().toString().takeLast(10)}"
                          showUtrDialogForRequest = req
                        },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("disburse_button_${req.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                      ) {
                        Text("Disburse & Enter UTR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                      }
                      OutlinedButton(
                        onClick = { showRejectDialogForRequest = req },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF4444))
                      ) {
                        Text("Reject", color = Color(0xFFEF4444), fontSize = 12.sp)
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  showUtrDialogForRequest?.let { req ->
    AlertDialog(
      onDismissRequest = { showUtrDialogForRequest = null },
      containerColor = Color(0xFF1E293B),
      title = { Text("Enter Bank Transfer UTR", color = Color.White, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Disbursing ₹${req.amountRupees} to ${req.payoutDestination}", fontSize = 12.sp, color = Color.LightGray)
          OutlinedTextField(
            value = utrInput,
            onValueChange = { utrInput = it },
            label = { Text("Bank UTR Reference Number") },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.adminApproveWithdrawal(req.id, utrInput)
            showUtrDialogForRequest = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = ZevoraSuccess)
        ) { Text("Confirm Disbursal") }
      },
      dismissButton = {
        TextButton(onClick = { showUtrDialogForRequest = null }) { Text("Cancel", color = Color.LightGray) }
      }
    )
  }

  showRejectDialogForRequest?.let { req ->
    AlertDialog(
      onDismissRequest = { showRejectDialogForRequest = null },
      containerColor = Color(0xFF1E293B),
      title = { Text("Reject Withdrawal Request", color = Color.White, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Funds of ₹${req.amountRupees} will be refunded back to creator's available wallet balance.", fontSize = 12.sp, color = Color.LightGray)
          OutlinedTextField(
            value = rejectReasonInput,
            onValueChange = { rejectReasonInput = it },
            label = { Text("Reason for Rejection") },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.adminRejectWithdrawal(req.id, rejectReasonInput)
            showRejectDialogForRequest = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
        ) { Text("Reject Request") }
      },
      dismissButton = {
        TextButton(onClick = { showRejectDialogForRequest = null }) { Text("Cancel", color = Color.LightGray) }
      }
    )
  }
}

@Composable
private fun AdminUserCard(
  managed: AdminManagedUser,
  onToggleVerified: () -> Unit,
  onToggleSuspended: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(managed.user.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
            if (managed.user.isVerified) {
              Icon(Icons.Default.Verified, contentDescription = "Verified", tint = ZevoraSecondary, modifier = Modifier.size(16.dp))
            }
          }
          Text("@${managed.user.username} • ${managed.authProvider}", fontSize = 11.sp, color = Color(0xFF94A3B8))
        }
        StatusPill(
          text = if (managed.isSuspended) "SUSPENDED" else if (managed.isFirestoreSynced) "CLOUD" else "ACTIVE",
          color = if (managed.isSuspended) ZevoraError else if (managed.isFirestoreSynced) ZevoraSuccess else ZevoraSecondary
        )
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
          onClick = onToggleVerified,
          modifier = Modifier.weight(1f).height(48.dp).testTag("admin_verify_user_${managed.user.id}"),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text(if (managed.user.isVerified) "Unverify" else "Verify Badge", fontSize = 12.sp)
        }
        Button(
          onClick = onToggleSuspended,
          colors = ButtonDefaults.buttonColors(
            containerColor = if (managed.isSuspended) ZevoraSuccess else ZevoraError
          ),
          modifier = Modifier.weight(1f).height(48.dp).testTag("admin_suspend_user_${managed.user.id}"),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text(if (managed.isSuspended) "Restore User" else "Suspend", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun AdminPostModerationCard(
  post: Post,
  onDeletePost: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "${post.author.name} (@${post.author.username})",
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp,
          color = Color.White
        )
        Text(post.timestamp, fontSize = 11.sp, color = Color(0xFF94A3B8))
      }
      Text(
        text = post.content,
        fontSize = 12.sp,
        color = Color(0xFFCBD5E1),
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "❤️ ${post.likesCount} • 💬 ${post.commentsCount}" +
            if (post.mediaUrls.isNotEmpty()) " • 📎 Local media (Upload Pending)" else "",
          fontSize = 11.sp,
          color = Color(0xFF94A3B8)
        )
        Button(
          onClick = onDeletePost,
          colors = ButtonDefaults.buttonColors(containerColor = ZevoraError),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.height(48.dp).testTag("admin_delete_post_${post.id}")
        ) {
          Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Delete", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun AdminReportCard(
  report: ContentReport,
  onResolveAndRemove: () -> Unit,
  onResolveOnly: () -> Unit,
  onDismiss: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("${report.targetType}: ${report.targetLabel}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White, modifier = Modifier.weight(1f))
        StatusPill(
          text = report.status.name,
          color = when (report.status) {
            ReportStatus.PENDING -> Color(0xFFFBBF24)
            ReportStatus.RESOLVED -> ZevoraSuccess
            ReportStatus.DISMISSED -> Color(0xFF94A3B8)
          }
        )
      }
      Text("Reason: ${report.reason}", fontSize = 12.sp, color = Color(0xFFFCA5A5), fontWeight = FontWeight.SemiBold)
      if (report.notes.isNotBlank()) {
        Text("Notes: ${report.notes}", fontSize = 11.sp, color = Color(0xFFCBD5E1))
      }
      if (report.actionTaken.isNotBlank()) {
        Text("Action: ${report.actionTaken}", fontSize = 11.sp, color = ZevoraSuccess)
      }
      if (report.status == ReportStatus.PENDING) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (report.targetType == "POST") {
            Button(
              onClick = onResolveAndRemove,
              colors = ButtonDefaults.buttonColors(containerColor = ZevoraError),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("admin_resolve_remove_${report.id}")
            ) { Text("Remove Post", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
          }
          Button(
            onClick = onResolveOnly,
            colors = ButtonDefaults.buttonColors(containerColor = ZevoraSuccess),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f).height(48.dp).testTag("admin_resolve_report_${report.id}")
          ) { Text("Resolve", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
          OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(48.dp).testTag("admin_dismiss_report_${report.id}")
          ) { Text("Dismiss", fontSize = 11.sp) }
        }
      }
    }
  }
}

@Composable
private fun AdminSettingSwitchRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  tag: String,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
      Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White)
      Text(subtitle, fontSize = 11.sp, color = Color(0xFF94A3B8), lineHeight = 15.sp)
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.testTag(tag)
    )
  }
}

@Composable
private fun EmptyAdminStateCard(
  icon: ImageVector,
  title: String,
  subtitle: String
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Icon(icon, contentDescription = null, tint = ZevoraSecondary, modifier = Modifier.size(36.dp))
      Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
      Text(subtitle, fontSize = 12.sp, color = Color(0xFF94A3B8))
    }
  }
}

@Composable
private fun StatusPill(text: String, color: Color) {
  Surface(
    color = color.copy(alpha = 0.18f),
    shape = RoundedCornerShape(10.dp)
  ) {
    Text(
      text = text,
      color = color,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
  }
}

@Composable
private fun RevenueChannelCard(
  title: String,
  amount: Double,
  desc: String,
  icon: ImageVector,
  color: Color
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
          Text(desc, fontSize = 11.sp, color = Color.LightGray)
        }
      }
      Text(
        text = "₹${String.format(Locale.getDefault(), "%,.2f", amount)}",
        fontWeight = FontWeight.ExtraBold,
        fontSize = 16.sp,
        color = Color.White
      )
    }
  }
}
