package com.example.zevora.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zevora.data.FirebaseDiagnosticsReport
import com.example.zevora.data.firebase.PhoneAuthManager
import kotlinx.coroutines.delay

@Composable
fun LinkedAccountsCard(
  diagnostics: FirebaseDiagnosticsReport,
  isLinking: Boolean,
  linkMessage: String?,
  linkError: String?,
  onLinkGoogleClick: () -> Unit,
  onLinkPhoneClick: () -> Unit,
  onDismissFeedback: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
    border = BorderStroke(1.dp, Color(0xFF334155)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("settings_linked_accounts_card")
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Link,
          contentDescription = null,
          tint = Color(0xFF38BDF8),
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "लिंक्ड लॉगिन विकल्प (Phone + Google)",
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp
        )
      }

      Text(
        text = "सत्यापित (Verified) करने के बाद आपका मोबाइल नंबर और Gmail दोनों इसी एक VEXORA खाते से सुरक्षित जुड़ जाते हैं।",
        color = Color(0xFF94A3B8),
        fontSize = 11.sp,
        lineHeight = 16.sp
      )

      // Feedback banner
      if (!linkError.isNullOrBlank()) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0xFF450A0A),
          border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.7f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = linkError,
              color = Color(0xFFFEE2E2),
              fontSize = 11.sp,
              lineHeight = 16.sp,
              modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismissFeedback, modifier = Modifier.size(32.dp)) {
              Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFFCA5A5), modifier = Modifier.size(15.dp))
            }
          }
        }
      } else if (!linkMessage.isNullOrBlank()) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0xFF064E3B),
          border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.7f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = linkMessage,
              color = Color(0xFFD1FAE5),
              fontSize = 11.sp,
              lineHeight = 16.sp,
              modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismissFeedback, modifier = Modifier.size(32.dp)) {
              Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF6EE7B7), modifier = Modifier.size(15.dp))
            }
          }
        }
      }

      // Google Account row
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E293B),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            tint = if (diagnostics.isGoogleLinked) Color(0xFF10B981) else Color(0xFF94A3B8),
            modifier = Modifier.size(22.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Google Account",
              color = Color.White,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = if (diagnostics.isGoogleLinked) {
                "Verified: ${diagnostics.verifiedEmail.ifBlank { "Linked" }}"
              } else {
                "लिंक नहीं है"
              },
              color = if (diagnostics.isGoogleLinked) Color(0xFF34D399) else Color(0xFF94A3B8),
              fontSize = 11.sp
            )
          }
          if (diagnostics.isGoogleLinked) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = "Google verified",
              tint = Color(0xFF10B981),
              modifier = Modifier.size(20.dp)
            )
          } else {
            Button(
              onClick = onLinkGoogleClick,
              enabled = !isLinking,
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("link_google_account_btn")
            ) {
              if (isLinking) {
                CircularProgressIndicator(
                  color = Color.White,
                  strokeWidth = 2.dp,
                  modifier = Modifier.size(14.dp)
                )
              } else {
                Text("Link Google", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // Mobile Number row
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E293B),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.PhoneAndroid,
            contentDescription = null,
            tint = if (diagnostics.isPhoneLinked) Color(0xFF10B981) else Color(0xFF94A3B8),
            modifier = Modifier.size(22.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Mobile Number (OTP)",
              color = Color.White,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = if (diagnostics.isPhoneLinked) {
                "Verified: ${diagnostics.verifiedPhoneNumber.ifBlank { "Linked" }}"
              } else {
                "लिंक नहीं है"
              },
              color = if (diagnostics.isPhoneLinked) Color(0xFF34D399) else Color(0xFF94A3B8),
              fontSize = 11.sp
            )
          }
          if (diagnostics.isPhoneLinked) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = "Mobile verified",
              tint = Color(0xFF10B981),
              modifier = Modifier.size(20.dp)
            )
          } else {
            OutlinedButton(
              onClick = onLinkPhoneClick,
              enabled = !isLinking,
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, Color(0xFF38BDF8)),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("link_phone_account_btn")
            ) {
              Text(
                text = "Link Mobile",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun LinkPhoneOtpDialog(
  isBusy: Boolean,
  verificationId: String?,
  maskedPhone: String?,
  e164Phone: String?,
  canResendAtMillis: Long,
  resendCount: Int = 0,
  errorText: String?,
  onDismiss: () -> Unit,
  onSendOtp: (countryCode: String, digits: String) -> Unit,
  onResendOtp: (e164Phone: String) -> Unit,
  onVerifyAndLink: (verificationId: String, otpCode: String) -> Unit,
  onResetToPhoneInput: () -> Unit
) {
  var selectedCountryCode by remember { mutableStateOf("+91") }
  var phoneDigits by remember { mutableStateOf("") }
  var otpCode by remember { mutableStateOf("") }
  var showCountryPicker by remember { mutableStateOf(false) }

  val selectedCountry = remember(selectedCountryCode) {
    PhoneAuthManager.supportedCountries.firstOrNull { it.code == selectedCountryCode }
      ?: PhoneAuthManager.supportedCountries.first()
  }

  var remainingSeconds by remember(canResendAtMillis, verificationId) {
    mutableIntStateOf(
      if (!verificationId.isNullOrBlank()) {
        ((canResendAtMillis - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(0)
      } else {
        0
      }
    )
  }

  LaunchedEffect(canResendAtMillis, verificationId) {
    if (!verificationId.isNullOrBlank()) {
      while (true) {
        val left = ((canResendAtMillis - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(0)
        remainingSeconds = left
        if (left <= 0) break
        delay(1000L)
      }
    } else {
      remainingSeconds = 0
    }
  }

  AlertDialog(
    onDismissRequest = { if (!isBusy) onDismiss() },
    containerColor = Color(0xFF0F172A),
    title = {
      Text(
        text = if (verificationId.isNullOrBlank()) "मोबाइल नंबर लिंक करें" else "OTP सत्यापित करें",
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!errorText.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF450A0A),
            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = errorText,
              color = Color(0xFFFEE2E2),
              fontSize = 11.sp,
              lineHeight = 16.sp,
              modifier = Modifier.padding(10.dp)
            )
          }
        }

        if (verificationId.isNullOrBlank()) {
          Text(
            text = "SMS OTP से नंबर सत्यापित करने के बाद आप इसी VEXORA खाते में मोबाइल नंबर से भी लॉगिन कर सकेंगे।",
            color = Color(0xFFCBD5E1),
            fontSize = 12.sp,
            lineHeight = 17.sp
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFF1E293B),
              border = BorderStroke(1.dp, Color(0xFF475569)),
              modifier = Modifier
                .height(54.dp)
                .clickable(enabled = !isBusy) { showCountryPicker = true }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(selectedCountry.flag, fontSize = 15.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(selectedCountry.code, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
              }
            }

            OutlinedTextField(
              value = phoneDigits,
              onValueChange = { incoming ->
                val filtered = incoming.filter { it.isDigit() }
                val maxLen = if (selectedCountryCode == "+91") 10 else 15
                phoneDigits = filtered.take(maxLen)
              },
              enabled = !isBusy,
              singleLine = true,
              placeholder = { Text("10 अंकों का नंबर", fontSize = 12.sp, color = Color(0xFF64748B)) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              shape = RoundedCornerShape(12.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color(0xFF475569)
              ),
              modifier = Modifier.weight(1f)
            )
          }
        } else {
          val resendsLeft = (PhoneAuthManager.MAX_OTP_RESENDS - resendCount).coerceAtLeast(0)
          val mins = remainingSeconds / 60
          val secs = remainingSeconds % 60
          val timerFormatted = "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"

          Text(
            text = "SMS OTP भेजा गया: ${maskedPhone ?: e164Phone.orEmpty()}",
            color = Color(0xFF38BDF8),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )

          OutlinedTextField(
            value = otpCode,
            onValueChange = { incoming ->
              otpCode = incoming.filter { it.isDigit() }.take(6)
            },
            enabled = !isBusy,
            singleLine = true,
            placeholder = { Text("6 अंकों का OTP", fontSize = 13.sp, color = Color(0xFF64748B)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 17.sp,
              letterSpacing = 5.sp
            ),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedBorderColor = Color(0xFF10B981),
              unfocusedBorderColor = Color(0xFF475569)
            ),
            modifier = Modifier.fillMaxWidth()
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = when {
                resendsLeft == 0 -> "सीमा पूर्ण (${PhoneAuthManager.MAX_OTP_RESENDS}/${PhoneAuthManager.MAX_OTP_RESENDS})"
                remainingSeconds > 0 -> timerFormatted
                else -> "Resend ($resendsLeft शेष)"
              },
              color = when {
                resendsLeft == 0 -> Color(0xFFFCA5A5)
                remainingSeconds > 0 -> Color(0xFF94A3B8)
                else -> Color(0xFF34D399)
              },
              fontSize = 12.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              TextButton(
                onClick = {
                  otpCode = ""
                  onResetToPhoneInput()
                },
                enabled = !isBusy
              ) {
                Text("बदलें", fontSize = 11.sp, color = Color(0xFFF472B6))
              }
              TextButton(
                onClick = {
                  otpCode = ""
                  e164Phone?.let(onResendOtp)
                },
                enabled = remainingSeconds == 0 && resendsLeft > 0 && !isBusy && !e164Phone.isNullOrBlank()
              ) {
                Text(if (resendsLeft == 0) "सीमा पूर्ण" else "Resend OTP", fontSize = 11.sp)
              }
            }
          }
        }
      }
    },
    confirmButton = {
      if (verificationId.isNullOrBlank()) {
        Button(
          onClick = { onSendOtp(selectedCountryCode, phoneDigits) },
          enabled = !isBusy,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
        ) {
          if (isBusy) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
          } else {
            Text("Send OTP", fontWeight = FontWeight.Bold)
          }
        }
      } else {
        Button(
          onClick = { onVerifyAndLink(verificationId, otpCode) },
          enabled = otpCode.length == 6 && !isBusy,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
        ) {
          if (isBusy) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
          } else {
            Text("Verify & Link", fontWeight = FontWeight.Bold)
          }
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isBusy) {
        Text("रद्द करें", color = Color(0xFF94A3B8))
      }
    }
  )

  if (showCountryPicker) {
    AlertDialog(
      onDismissRequest = { showCountryPicker = false },
      containerColor = Color(0xFF0F172A),
      title = { Text("Country Code", color = Color.White, fontWeight = FontWeight.Bold) },
      text = {
        Column {
          PhoneAuthManager.supportedCountries.forEach { option ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable {
                  selectedCountryCode = option.code
                  showCountryPicker = false
                }
                .padding(horizontal = 8.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("${option.flag}  ${option.name}", color = Color.White, modifier = Modifier.weight(1f))
              Text(option.code, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showCountryPicker = false }) { Text("Close") }
      }
    )
  }
}
