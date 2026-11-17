package com.example.zevora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zevora.ui.ZevoraViewModel
import com.example.zevora.ui.theme.ZevoraPrimary
import com.example.zevora.ui.theme.ZevoraSecondary
import com.example.zevora.ui.theme.ZevoraSuccess
import com.example.zevora.ui.theme.ZevoraTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorEarningsScreen(
  viewModel: ZevoraViewModel,
  onNavigateBack: () -> Unit
) {
  var showWithdrawDialog by remember { mutableStateOf(false) }
  var withdrawSuccess by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("कमाई का सिस्टम (Creator Studio)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Insights, Monetization & Payouts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
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
      verticalArrangement = Arrangement.spacedBy(14.dp),
      contentPadding = PaddingValues(vertical = 16.dp)
    ) {
      // Total Revenue Card
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Text(
              "Total Creator Earnings (कुल कमाई)",
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                "₹1,55,802.54",
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.primary
              )
              Surface(
                color = ZevoraSuccess.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text(
                  "+18.4% this month",
                  color = ZevoraSuccess,
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Button(
              onClick = { showWithdrawDialog = true },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth().testTag("withdraw_earnings_btn")
            ) {
              Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Withdraw to Bank / UPI")
            }
          }
        }
      }

      // Monetization Revenue Sources
      item {
        Text("VEXORA कमाई के माध्यम (Revenue Channels):", fontWeight = FontWeight.Bold, fontSize = 15.sp)
      }

      item {
        RevenueChannelCard(
          icon = Icons.Default.AdsClick,
          title = "Google AdMob Ads",
          amount = "₹62,321.00 (40%)",
          color = MaterialTheme.colorScheme.primary
        )
      }

      item {
        RevenueChannelCard(
          icon = Icons.Default.OndemandVideo,
          title = "वीडियो के बीच विज्ञापन (In-Stream Ads)",
          amount = "₹38,950.50 (25%)",
          color = ZevoraSecondary
        )
      }

      item {
        RevenueChannelCard(
          icon = Icons.Default.Campaign,
          title = "Sponsored Posts & Brand Promotion",
          amount = "₹23,370.00 (15%)",
          color = ZevoraTertiary
        )
      }

      item {
        RevenueChannelCard(
          icon = Icons.Default.CardGiftcard,
          title = "Live Gifts from Viewers",
          amount = "₹15,580.00 (10%)",
          color = Color(0xFFF59E0B)
        )
      }

      item {
        RevenueChannelCard(
          icon = Icons.Default.CardMembership,
          title = "Paid Membership & Premium Subscriptions",
          amount = "₹15,581.04 (10%)",
          color = ZevoraSuccess
        )
      }
    }
  }

  if (showWithdrawDialog) {
    AlertDialog(
      onDismissRequest = { showWithdrawDialog = false },
      title = { Text("Payout Request", fontWeight = FontWeight.Bold) },
      text = {
        if (withdrawSuccess) {
          Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ZevoraSuccess, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text("निकासी अनुरोध स्वीकार हुआ!", fontWeight = FontWeight.Bold)
            Text("₹1,55,802.54 will be credited to your verified UPI handle within 24 hours.", fontSize = 12.sp)
          }
        } else {
          Column {
            Text("Transfer available balance to verified account:")
            Spacer(modifier = Modifier.height(8.dp))
            Text("UPI ID: aarav@oksbi", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Account Holder: Aarav Sharma", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
          }
        }
      },
      confirmButton = {
        if (withdrawSuccess) {
          Button(onClick = {
            showWithdrawDialog = false
            withdrawSuccess = false
          }) { Text("Done") }
        } else {
          Button(onClick = { withdrawSuccess = true }) { Text("Confirm Transfer") }
        }
      },
      dismissButton = {
        if (!withdrawSuccess) {
          TextButton(onClick = { showWithdrawDialog = false }) { Text("Cancel") }
        }
      }
    )
  }
}

@Composable
private fun RevenueChannelCard(
  icon: ImageVector,
  title: String,
  amount: String,
  color: Color
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.15f),
        modifier = Modifier.size(40.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(amount, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color)
      }
    }
  }
}
