package com.example.zevora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zevora.model.*
import com.example.zevora.ui.Screen
import com.example.zevora.ui.ZevoraViewModel
import com.example.zevora.ui.theme.ZevoraPrimary
import com.example.zevora.ui.theme.ZevoraSecondary
import com.example.zevora.ui.theme.ZevoraSuccess
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorWalletScreen(
  viewModel: ZevoraViewModel,
  onNavigateBack: () -> Unit,
  onNavigateTo: (Screen) -> Unit
) {
  val wallet by viewModel.userWallet.collectAsState()
  val transactions by viewModel.transactions.collectAsState()
  val coinPacks = viewModel.coinPacks
  val monetizationApp by viewModel.monetizationApplication.collectAsState()

  var showWithdrawSheet by remember { mutableStateOf(false) }
  var showBuyCoinsSheet by remember { mutableStateOf(false) }
  var showPayoutDetailsDialog by remember { mutableStateOf(false) }
  var showKycSheet by remember { mutableStateOf(false) }
  var statusSnackbar by remember { mutableStateOf<String?>(null) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("Creator Wallet & Payouts", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Balances, Coins & UPI/Bank Disbursals", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("wallet_back_button")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = { onNavigateTo(Screen.ADMIN_PANEL) }) {
            Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin Panel", tint = Color(0xFFFBBF24))
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
      // Status message if any
      statusSnackbar?.let { msg ->
        item {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Info, contentDescription = null, tint = ZevoraSecondary)
              Spacer(modifier = Modifier.width(8.dp))
              Text(msg, fontSize = 13.sp, color = Color.White, modifier = Modifier.weight(1f))
              IconButton(onClick = { statusSnackbar = null }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
              }
            }
          }
        }
      }

      // Hero Wallet Card (Withdrawable & Pending)
      item {
        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("AVAILABLE FOR WITHDRAWAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA5B4FC))
              Surface(
                color = when (wallet.kycStatus) {
                  KycStatus.VERIFIED -> ZevoraSuccess.copy(alpha = 0.2f)
                  KycStatus.UNDER_REVIEW -> Color(0xFFFBBF24).copy(alpha = 0.2f)
                  else -> Color(0xFFEF4444).copy(alpha = 0.2f)
                },
                shape = RoundedCornerShape(12.dp)
              ) {
                Text(
                  text = "KYC: ${wallet.kycStatus.name}",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = when (wallet.kycStatus) {
                    KycStatus.VERIFIED -> ZevoraSuccess
                    KycStatus.UNDER_REVIEW -> Color(0xFFFBBF24)
                    else -> Color(0xFFEF4444)
                  },
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "₹${String.format(Locale.getDefault(), "%,.2f", wallet.withdrawableRupees)}",
              fontSize = 34.sp,
              fontWeight = FontWeight.ExtraBold,
              color = Color.White
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFF312E81))
            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Column {
                Text("Pending Clearance", fontSize = 11.sp, color = Color.LightGray)
                Text("₹${String.format(Locale.getDefault(), "%,.2f", wallet.pendingRupees)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
              }
              Column {
                Text("Lifetime Earnings", fontSize = 11.sp, color = Color.LightGray)
                Text("₹${String.format(Locale.getDefault(), "%,.2f", wallet.totalLifetimeEarnings)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ZevoraSuccess)
              }
              Column {
                Text("Eligible Views", fontSize = 11.sp, color = Color.LightGray)
                Text("${wallet.eligibleReelsViews / 1000}K", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
              }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons (Withdraw & Payout Method)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Button(
                onClick = {
                  if (wallet.kycStatus != KycStatus.VERIFIED) {
                    showKycSheet = true
                  } else {
                    showWithdrawSheet = true
                  }
                },
                modifier = Modifier
                  .weight(1f)
                  .testTag("withdraw_funds_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
              ) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Withdraw Funds", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              }

              OutlinedButton(
                onClick = { showPayoutDetailsDialog = true },
                modifier = Modifier
                  .weight(1f)
                  .testTag("payout_settings_button"),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA5B4FC))
              ) {
                Icon(Icons.Default.Payment, contentDescription = null, tint = Color(0xFFA5B4FC), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Payout Method", color = Color(0xFFA5B4FC), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
              }
            }
          }
        }
      }

      // Coins Balance Card (Virtual Gifts Currency)
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
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
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(46.dp)
                  .clip(CircleShape)
                  .background(Color(0xFFFEF3C7)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(28.dp))
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text("Vexora Coins", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                Text("${wallet.coinsBalance} Coins available for Live gifts", fontSize = 12.sp, color = Color.LightGray)
              }
            }

            Button(
              onClick = { showBuyCoinsSheet = true },
              shape = RoundedCornerShape(20.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
              modifier = Modifier.testTag("buy_coins_button")
            ) {
              Text("+ Buy Coins", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
          }
        }
      }

      // Quick Links: VIP Pro & Creator Verification
      item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF281E48)),
            modifier = Modifier
              .weight(1f)
              .clickable { onNavigateTo(Screen.PREMIUM_UPGRADE) }
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(22.dp))
              Spacer(modifier = Modifier.height(6.dp))
              Text("VIP Pro Pass", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
              Text("No ads & 70% rev cut", fontSize = 11.sp, color = Color.LightGray)
            }
          }

          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2E44)),
            modifier = Modifier
              .weight(1f)
              .clickable { showKycSheet = true }
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = ZevoraSecondary, modifier = Modifier.size(22.dp))
              Spacer(modifier = Modifier.height(6.dp))
              Text("Creator Program", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
              Text("Tax & PAN details", fontSize = 11.sp, color = Color.LightGray)
            }
          }
        }
      }

      // Transaction History Title
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Financial Audit Log", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
          Text("${transactions.size} Records", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }

      // Transactions List
      if (transactions.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Box(modifier = Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
              Text("No transactions logged yet", color = Color.LightGray, fontSize = 13.sp)
            }
          }
        }
      } else {
        items(transactions) { txn ->
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                  modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                      when (txn.type) {
                        TransactionType.REELS_AD_SHARE -> Color(0xFF10B981).copy(alpha = 0.2f)
                        TransactionType.GIFT_RECEIVED -> Color(0xFFEC4899).copy(alpha = 0.2f)
                        TransactionType.COIN_PURCHASE -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                        TransactionType.WITHDRAWAL -> Color(0xFF6366F1).copy(alpha = 0.2f)
                        else -> Color(0xFF64748B).copy(alpha = 0.2f)
                      }
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = when (txn.type) {
                      TransactionType.REELS_AD_SHARE -> Icons.AutoMirrored.Filled.TrendingUp
                      TransactionType.GIFT_RECEIVED -> Icons.Default.CardGiftcard
                      TransactionType.COIN_PURCHASE -> Icons.Default.MonetizationOn
                      TransactionType.WITHDRAWAL -> Icons.Default.AccountBalance
                      else -> Icons.Default.Receipt
                    },
                    contentDescription = null,
                    tint = when (txn.type) {
                      TransactionType.REELS_AD_SHARE -> Color(0xFF10B981)
                      TransactionType.GIFT_RECEIVED -> Color(0xFFEC4899)
                      TransactionType.COIN_PURCHASE -> Color(0xFFF59E0B)
                      TransactionType.WITHDRAWAL -> Color(0xFFA5B4FC)
                      else -> Color.White
                    },
                    modifier = Modifier.size(20.dp)
                  )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(txn.description, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White)
                  val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(txn.timestamp))
                  Text(
                    text = "$dateStr • Ref: ${txn.referenceId}",
                    fontSize = 11.sp,
                    color = Color.LightGray
                  )
                }
              }

              Column(horizontalAlignment = Alignment.End) {
                val isPositive = txn.type in listOf(TransactionType.REELS_AD_SHARE, TransactionType.GIFT_RECEIVED)
                Text(
                  text = "${if (isPositive) "+" else "-"}₹${String.format(Locale.getDefault(), "%,.2f", txn.amountRupees)}",
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = if (isPositive) ZevoraSuccess else if (txn.type == TransactionType.WITHDRAWAL) Color(0xFFFBBF24) else Color.White
                )
                Text(
                  text = txn.status.name,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = when (txn.status) {
                    TransactionStatus.SUCCESS -> ZevoraSuccess
                    TransactionStatus.PENDING -> Color(0xFFFBBF24)
                    TransactionStatus.FAILED -> Color(0xFFEF4444)
                  }
                )
              }
            }
          }
        }
      }
    }
  }

  // Withdraw Sheet Modal
  if (showWithdrawSheet) {
    var withdrawAmountText by remember { mutableStateOf("1000") }
    ModalBottomSheet(
      onDismissRequest = { showWithdrawSheet = false },
      containerColor = Color(0xFF131B2E),
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .padding(bottom = 32.dp)
      ) {
        Text("Withdraw Creator Funds", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
        Text("Minimum limit: ₹1,000 INR • Directly deposited via UPI / IMPS", fontSize = 12.sp, color = Color.LightGray)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
          value = withdrawAmountText,
          onValueChange = { withdrawAmountText = it.filter { char -> char.isDigit() } },
          label = { Text("Amount (INR)") },
          leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = Color.White) },
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = ZevoraPrimary,
            unfocusedBorderColor = Color(0xFF334155)
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("withdraw_amount_input")
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
          "Available: ₹${String.format(Locale.getDefault(), "%,.2f", wallet.withdrawableRupees)} • Destination: ${if (wallet.payoutMethod == PayoutMethod.UPI) wallet.upiId ?: "UPI not set" else wallet.bankDetails?.bankName ?: "Bank"}",
          fontSize = 12.sp,
          color = Color(0xFFA5B4FC)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = {
            val amount = withdrawAmountText.toDoubleOrNull() ?: 0.0
            val res = viewModel.requestWithdrawal(amount)
            statusSnackbar = res.second
            if (res.first) showWithdrawSheet = false
          },
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("confirm_withdraw_button")
        ) {
          Text("Confirm Withdrawal", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
      }
    }
  }

  // Buy Coins Modal
  if (showBuyCoinsSheet) {
    ModalBottomSheet(
      onDismissRequest = { showBuyCoinsSheet = false },
      containerColor = Color(0xFF131B2E),
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .padding(bottom = 32.dp)
      ) {
        Text("Recharge Vexora Coins 🪙", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
        Text("Google Play Billing • Instant credit for Live Gifts & Tips", fontSize = 12.sp, color = Color.LightGray)

        Spacer(modifier = Modifier.height(16.dp))

        coinPacks.forEach { pack ->
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = if (pack.isPopular) Color(0xFF312E81) else Color(0xFF1E293B)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .clickable {
                val ok = viewModel.purchaseCoins(pack.id, "GPA.${System.currentTimeMillis().toString().takeLast(10)}")
                if (ok) {
                  statusSnackbar = "Successfully purchased ${pack.coins + pack.bonusCoins} Coins!"
                  showBuyCoinsSheet = false
                }
              }
              .testTag("coin_pack_${pack.id}")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text("${pack.coins} Coins", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                  if (pack.bonusCoins > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(color = Color(0xFF10B981), shape = RoundedCornerShape(6.dp)) {
                      Text("+${pack.bonusCoins} FREE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                  }
                }
                Text("₹0.50 / coin value", fontSize = 11.sp, color = Color.LightGray)
              }

              Button(
                onClick = {
                  val ok = viewModel.purchaseCoins(pack.id, "GPA.${System.currentTimeMillis().toString().takeLast(10)}")
                  if (ok) {
                    statusSnackbar = "Successfully purchased ${pack.coins + pack.bonusCoins} Coins!"
                    showBuyCoinsSheet = false
                  }
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
              ) {
                Text("₹${pack.priceRupees.toInt()}", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
              }
            }
          }
        }
      }
    }
  }

  // Payout Details Configuration Dialog
  if (showPayoutDetailsDialog) {
    var selectedMethod by remember { mutableStateOf(wallet.payoutMethod) }
    var upiInput by remember { mutableStateOf(wallet.upiId ?: "aarav@oksbi") }
    var bankAccountInput by remember { mutableStateOf(wallet.bankDetails?.accountNumber ?: "50100492819201") }
    var ifscInput by remember { mutableStateOf(wallet.bankDetails?.ifscCode ?: "HDFC0001234") }
    var bankNameInput by remember { mutableStateOf(wallet.bankDetails?.bankName ?: "HDFC Bank") }

    AlertDialog(
      onDismissRequest = { showPayoutDetailsDialog = false },
      containerColor = Color(0xFF1E293B),
      title = { Text("Payout Settings", color = Color.White, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Select primary withdrawal method:", fontSize = 12.sp, color = Color.LightGray)
          Row(modifier = Modifier.fillMaxWidth()) {
            FilterChip(
              selected = selectedMethod == PayoutMethod.UPI,
              onClick = { selectedMethod = PayoutMethod.UPI },
              label = { Text("UPI (Instant)") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
              selected = selectedMethod == PayoutMethod.BANK_TRANSFER,
              onClick = { selectedMethod = PayoutMethod.BANK_TRANSFER },
              label = { Text("Bank Account") }
            )
          }

          if (selectedMethod == PayoutMethod.UPI) {
            OutlinedTextField(
              value = upiInput,
              onValueChange = { upiInput = it },
              label = { Text("UPI ID (e.g. mobile@upi)") },
              modifier = Modifier.fillMaxWidth().testTag("upi_id_input"),
              colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
          } else {
            OutlinedTextField(
              value = bankAccountInput,
              onValueChange = { bankAccountInput = it },
              label = { Text("Account Number") },
              modifier = Modifier.fillMaxWidth(),
              colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            OutlinedTextField(
              value = ifscInput,
              onValueChange = { ifscInput = it },
              label = { Text("IFSC Code") },
              modifier = Modifier.fillMaxWidth(),
              colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            OutlinedTextField(
              value = bankNameInput,
              onValueChange = { bankNameInput = it },
              label = { Text("Bank Name") },
              modifier = Modifier.fillMaxWidth(),
              colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val bankDetails = if (selectedMethod == PayoutMethod.BANK_TRANSFER) {
              BankAccountDetails(accountNumber = bankAccountInput, ifscCode = ifscInput, bankName = bankNameInput, accountHolderName = wallet.panNumber)
            } else null
            viewModel.updatePayoutDetails(selectedMethod, upiInput, bankDetails)
            statusSnackbar = "Payout details updated successfully"
            showPayoutDetailsDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = ZevoraPrimary)
        ) {
          Text("Save Payout Method")
        }
      },
      dismissButton = {
        TextButton(onClick = { showPayoutDetailsDialog = false }) {
          Text("Cancel", color = Color.LightGray)
        }
      }
    )
  }

  // KYC & Creator Program Application Sheet
  if (showKycSheet) {
    var panInput by remember { mutableStateOf(wallet.panNumber) }
    var legalNameInput by remember { mutableStateOf("Aarav Sharma") }
    var categoryInput by remember { mutableStateOf("Digital Creator / Tech & Lifestyle") }

    ModalBottomSheet(
      onDismissRequest = { showKycSheet = false },
      containerColor = Color(0xFF131B2E),
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .padding(bottom = 32.dp)
      ) {
        Text("Creator Monetization & Tax KYC", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
        Text("Required for TDS compliance and direct withdrawals in India", fontSize = 12.sp, color = Color.LightGray)

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = legalNameInput,
          onValueChange = { legalNameInput = it },
          label = { Text("Full Legal Name (as on PAN Card)") },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = panInput,
          onValueChange = { panInput = it.uppercase() },
          label = { Text("Permanent Account Number (PAN)") },
          modifier = Modifier.fillMaxWidth().testTag("pan_number_input"),
          colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = categoryInput,
          onValueChange = { categoryInput = it },
          label = { Text("Content Category") },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          "Current Eligibility: 14,200 Followers (Min: 1,000) • 184K Views (Min: 10,000)",
          fontSize = 12.sp,
          color = ZevoraSuccess,
          fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
          onClick = {
            val res = viewModel.submitMonetizationApplication(panInput, legalNameInput, categoryInput)
            statusSnackbar = res.second
            if (res.first) showKycSheet = false
          },
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = ZevoraPrimary),
          modifier = Modifier.fillMaxWidth().testTag("submit_kyc_button")
        ) {
          Text("Submit Application for Review", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
      }
    }
  }
}
