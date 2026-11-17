package com.example.zevora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
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
import com.example.zevora.model.SubscriptionPlan
import com.example.zevora.ui.ZevoraViewModel
import com.example.zevora.ui.theme.ZevoraPrimary
import com.example.zevora.ui.theme.ZevoraSecondary
import com.example.zevora.ui.theme.ZevoraSuccess
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumUpgradeScreen(
  viewModel: ZevoraViewModel,
  onNavigateBack: () -> Unit
) {
  val currentSub by viewModel.premiumSubscription.collectAsState()
  var selectedPlan by remember { mutableStateOf(SubscriptionPlan.MONTHLY) }
  var successMsg by remember { mutableStateOf<String?>(null) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("VEXORA VIP Pro 👑", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        navigationIcon = {
          IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("vip_back_button")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    }
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(vertical = 16.dp)
    ) {
      // Golden VIP Hero Banner
      item {
        Card(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = Color.Transparent),
          modifier = Modifier
            .fillMaxWidth()
            .background(
              brush = Brush.linearGradient(
                colors = listOf(Color(0xFF311042), Color(0xFF1E1B4B), Color(0xFF0F172A))
              ),
              shape = RoundedCornerShape(24.dp)
            )
            .border(width = 1.dp, color = Color(0xFFFBBF24).copy(alpha = 0.5f), shape = RoundedCornerShape(24.dp))
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Box(
              modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFFFEF3C7)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = Color(0xFFD97706),
                modifier = Modifier.size(36.dp)
              )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
              "VEXORA VIP Pro Membership",
              fontWeight = FontWeight.ExtraBold,
              fontSize = 20.sp,
              color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              "Unlock 100% Ad-Free Experience, 4K HDR Uploads, Golden VIP Badge & Exclusive Creator Features",
              fontSize = 12.sp,
              color = Color(0xFFCBD5E1),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (currentSub.isPremium) {
              Spacer(modifier = Modifier.height(14.dp))
              Surface(
                color = Color(0xFF10B981).copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ZevoraSuccess)
              ) {
                val expiryDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(currentSub.validUntil))
                Text(
                  text = "Active Subscription: ${currentSub.plan.title} (Valid till $expiryDate)",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = ZevoraSuccess,
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              TextButton(
                onClick = {
                  viewModel.cancelPremiumSubscription()
                  successMsg = "Switched to Free Tier — AdMob Banner & Interstitial ads enabled."
                },
                modifier = Modifier.testTag("cancel_vip_button")
              ) {
                Text("Switch to Free Tier (Test Ad-Supported Mode)", color = Color(0xFFCBD5E1), fontSize = 11.sp)
              }
            }
          }
        }
      }

      // Notification
      successMsg?.let { msg ->
        item {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF065F46)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
              Spacer(modifier = Modifier.width(10.dp))
              Text(msg, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // Plans Selection: Monthly vs Yearly
      item {
        Text("Choose Your VIP Plan", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
      }

      item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          // Monthly Plan
          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (selectedPlan == SubscriptionPlan.MONTHLY) Color(0xFF2E1065) else Color(0xFF1E293B)
            ),
            modifier = Modifier
              .weight(1f)
              .border(
                width = if (selectedPlan == SubscriptionPlan.MONTHLY) 2.dp else 1.dp,
                color = if (selectedPlan == SubscriptionPlan.MONTHLY) Color(0xFFA855F7) else Color(0xFF334155),
                shape = RoundedCornerShape(18.dp)
              )
              .clickable { selectedPlan = SubscriptionPlan.MONTHLY }
              .testTag("plan_monthly_card")
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text("MONTHLY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA855F7))
              Spacer(modifier = Modifier.height(4.dp))
              Text("₹199", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
              Text("billed monthly", fontSize = 11.sp, color = Color.LightGray)
              Spacer(modifier = Modifier.height(10.dp))
              Text("• Auto-renews every 30 days\n• Cancel anytime", fontSize = 11.sp, color = Color(0xFFCBD5E1))
            }
          }

          // Yearly Plan (Best Value)
          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (selectedPlan == SubscriptionPlan.YEARLY) Color(0xFF2E1065) else Color(0xFF1E293B)
            ),
            modifier = Modifier
              .weight(1f)
              .border(
                width = if (selectedPlan == SubscriptionPlan.YEARLY) 2.dp else 1.dp,
                color = if (selectedPlan == SubscriptionPlan.YEARLY) Color(0xFFFBBF24) else Color(0xFF334155),
                shape = RoundedCornerShape(18.dp)
              )
              .clickable { selectedPlan = SubscriptionPlan.YEARLY }
              .testTag("plan_yearly_card")
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text("YEARLY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                Spacer(modifier = Modifier.width(6.dp))
                Surface(color = Color(0xFF10B981), shape = RoundedCornerShape(4.dp)) {
                  Text("SAVE 25%", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text("₹1,799", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
              Text("₹149/month equivalent", fontSize = 11.sp, color = Color.LightGray)
              Spacer(modifier = Modifier.height(10.dp))
              Text("• 365 Days uninterrupted VIP\n• Priority creator payout", fontSize = 11.sp, color = Color(0xFFCBD5E1))
            }
          }
        }
      }

      // VIP Benefits Breakdown List
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("VIP Pro Benefits Included", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)

            BenefitRow(icon = Icons.Default.Block, title = "Zero Ads Everywhere", desc = "No sponsored feed cards, no banner ads, and no video interstitials in Reels.")
            BenefitRow(icon = Icons.Default.HighQuality, title = "4K HDR Video Streaming", desc = "High bitrate uploads and crystal clear playback even in high traffic.")
            BenefitRow(icon = Icons.Default.Verified, title = "Golden VIP Profile Badge", desc = "Distinctive golden badge next to your handle in comments and live chat.")
            BenefitRow(icon = Icons.AutoMirrored.Filled.TrendingUp, title = "70% Creator Revenue Share", desc = "Higher payout percentage on all Live Gifts and eligible Reels video ads.")
            BenefitRow(icon = Icons.Default.SupportAgent, title = "24x7 Priority Support", desc = "Dedicated creator assistance with fast-track withdrawal processing.")
          }
        }
      }

      // Purchase Button
      item {
        Button(
          onClick = {
            val orderId = "GPA.${System.currentTimeMillis().toString().takeLast(12)}"
            val ok = viewModel.subscribeToPremium(selectedPlan, orderId)
            if (ok) {
              successMsg = "Congratulations! VEXORA VIP Pro activated successfully via Google Play."
            }
          },
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("subscribe_vip_button")
        ) {
          Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color.Black)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Upgrade with Google Play (₹${selectedPlan.priceRupees.toInt()})",
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          )
        }
      }
    }
  }
}

@Composable
private fun BenefitRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
  Row(verticalAlignment = Alignment.Top) {
    Box(
      modifier = Modifier
        .size(32.dp)
        .clip(CircleShape)
        .background(Color(0xFF312E81)),
      contentAlignment = Alignment.Center
    ) {
      Icon(icon, contentDescription = null, tint = Color(0xFFA5B4FC), modifier = Modifier.size(16.dp))
    }
    Spacer(modifier = Modifier.width(12.dp))
    Column {
      Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
      Text(desc, fontSize = 11.sp, color = Color.LightGray)
    }
  }
}
