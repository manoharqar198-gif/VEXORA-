package com.example.zevora.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zevora.model.AppLanguage

@Composable
fun HelpAndSupportDialog(
  onDismiss: () -> Unit,
  onOpenContactUs: () -> Unit,
  language: AppLanguage
) {
  val faqs = listOf(
    "How does 2FA protect my VEXORA account?" to "Two-Factor Authentication adds an extra layer of protection by requiring a 4-digit OTP alongside your password whenever you log in from a new device.",
    "How do I earn money on VEXORA (कमाई का सिस्टम)?" to "Eligible creators can monetize through AdMob video ads, sponsored posts, live stream gifts from fans, and channel paid subscriptions via Creator Studio.",
    "What happens when I block someone?" to "Blocked users cannot see your profile, posts, reels, or send you direct messages. They are never notified that they have been blocked.",
    "How do I make my account private?" to "Go to Security & Privacy settings and toggle 'Private Account'. Only people you approve will be able to follow you and view your content."
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (language == AppLanguage.HINDI) "Help और Support" else "Help & Support", fontWeight = FontWeight.Bold, fontSize = 18.sp)
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
      ) {
        Text("Frequently Asked Questions (अक्सर पूछे जाने वाले सवाल):", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(8.dp))

        faqs.forEach { (q, a) ->
          FaqExpandableItem(question = q, answer = a)
          Spacer(modifier = Modifier.height(6.dp))
        }

        Spacer(modifier = Modifier.height(14.dp))
        Button(
          onClick = {
            onDismiss()
            onOpenContactUs()
          },
          modifier = Modifier.fillMaxWidth().testTag("open_contact_us_from_help_btn")
        ) {
          Icon(Icons.Default.SupportAgent, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Contact Support Team (संपर्क करें)")
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Close") }
    }
  )
}

@Composable
private fun FaqExpandableItem(question: String, answer: String) {
  var expanded by remember { mutableStateOf(false) }

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(question, fontWeight = FontWeight.Medium, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Icon(
          imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
          contentDescription = null,
          modifier = Modifier.size(20.dp)
        )
      }
      AnimatedVisibility(visible = expanded) {
        Column {
          Spacer(modifier = Modifier.height(6.dp))
          Text(answer, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }
  }
}

@Composable
fun LegalDocumentViewerDialog(
  title: String,
  onDismiss: () -> Unit,
  isPrivacyPolicy: Boolean
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = if (isPrivacyPolicy) Icons.Default.Policy else Icons.Default.Gavel,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
      ) {
        if (isPrivacyPolicy) {
          Text("VEXORA Privacy Policy (गोपनीयता नीति)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
          Text("Last updated: November 2026\n", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

          Text("1. Information We Collect\nWe collect account information (username, email, phone number for OTP verification), profile details, uploaded media, and device session metadata to protect your security.\n", fontSize = 12.sp)
          Text("2. Two-Factor Authentication & OTP Security\nPhone numbers and authenticators are strictly utilized for authentication and fraud prevention. We never sell your personal numbers to third-party advertisers.\n", fontSize = 12.sp)
          Text("3. Content Privacy & Private Accounts\nWhen you set your account to Private, your media, reels, and stories are visible strictly to approved connections. You have complete comment and blocking controls.\n", fontSize = 12.sp)
          Text("4. Account Deletion\nYou can request permanent account deletion at any time with a 30-day grace cancellation window.", fontSize = 12.sp)
        } else {
          Text("VEXORA Terms & Conditions (नियम व शर्तें)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
          Text("Effective: 2026\n", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

          Text("1. Acceptance of Terms\nBy creating a VEXORA account, you agree to comply with our community standards and guidelines for respectful interaction.\n", fontSize = 12.sp)
          Text("2. Prohibited Conduct\nHarassment, hate speech, spam, unauthorized commercial solicitation, and copyright infringement are strictly prohibited and subject to immediate suspension.\n", fontSize = 12.sp)
          Text("3. Creator Monetization Terms\nCreators participating in AdMob and Gift monetization must comply with Indian advertising laws and maintain active community guidelines standing.", fontSize = 12.sp)
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) { Text("I Understand") }
    }
  )
}

@Composable
fun ContactUsDialog(
  onDismiss: () -> Unit
) {
  var feedbackText by remember { mutableStateOf("") }
  var messageSent by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.HeadsetMic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Contact Us (संपर्क करें)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
      }
    },
    text = {
      if (messageSent) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
          Spacer(modifier = Modifier.height(10.dp))
          Text("Message Sent Successfully!", fontWeight = FontWeight.Bold)
          Text("Our team will reply to support@zevora.app within 2 hours.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      } else {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
        ) {
          Text("Reach our 24/7 Support Team:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
          Spacer(modifier = Modifier.height(8.dp))

          ContactChannelRow(Icons.Default.Email, "Email Support", "support@zevora.app")
          ContactChannelRow(Icons.Default.Phone, "Toll-Free Helpline", "1800-123-VEXORA")
          ContactChannelRow(Icons.AutoMirrored.Filled.Chat, "WhatsApp Official", "+91 98765 00000")

          HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

          Text("Send Quick Message:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = feedbackText,
            onValueChange = { feedbackText = it },
            placeholder = { Text("How can we help you today?") },
            modifier = Modifier.fillMaxWidth().testTag("contact_message_input"),
            maxLines = 3,
            shape = RoundedCornerShape(10.dp)
          )
        }
      }
    },
    confirmButton = {
      if (messageSent) {
        Button(onClick = onDismiss) { Text("Done") }
      } else {
        Button(
          onClick = { messageSent = true },
          enabled = feedbackText.isNotBlank(),
          modifier = Modifier.testTag("send_contact_msg_btn")
        ) {
          Text("Send Message")
        }
      }
    },
    dismissButton = {
      if (!messageSent) {
        TextButton(onClick = onDismiss) { Text("Close") }
      }
    }
  )
}

@Composable
private fun ContactChannelRow(icon: ImageVector, title: String, subtitle: String) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = MaterialTheme.colorScheme.primaryContainer,
      modifier = Modifier.size(36.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
      }
    }
    Spacer(modifier = Modifier.width(10.dp))
    Column {
      Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
      Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
