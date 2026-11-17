package com.example.zevora.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zevora.model.CommentControlOption
import com.example.zevora.ui.theme.*

@Composable
fun ReportModal(
  title: String,
  targetName: String,
  onDismiss: () -> Unit,
  onSubmit: (reason: String, notes: String) -> Unit
) {
  val reasons = listOf(
    "Spam or misleading (स्पैम या भ्रामक)",
    "Hate speech or symbols (घृणास्पद भाषण)",
    "Harassment or bullying (उत्पीड़न / बदमाशी)",
    "False information / Misinformation (गलत जानकारी)",
    "Violence or dangerous content (हिंसा)",
    "Intellectual property violation (कॉपीराइट उल्लंघन)"
  )
  var selectedReason by remember { mutableStateOf(reasons[0]) }
  var additionalNotes by remember { mutableStateOf("") }
  var submitted by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Flag, contentDescription = null, tint = ZevoraError)
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
      }
    },
    text = {
      if (submitted) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ZevoraSuccess, modifier = Modifier.size(48.dp))
          Spacer(modifier = Modifier.height(12.dp))
          Text("रिपोर्ट दर्ज कर ली गई है!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            "Thank you for keeping VEXORA safe. Our trust & safety team will review $targetName within 24 hours.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      } else {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
        ) {
          Text(
            text = "Reporting: $targetName",
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp
          )
          Spacer(modifier = Modifier.height(10.dp))
          Text("Select Reason (कारण चुनें):", fontSize = 13.sp, fontWeight = FontWeight.Medium)

          reasons.forEach { reason ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedReason = reason }
                .padding(vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = selectedReason == reason,
                onClick = { selectedReason = reason }
              )
              Text(reason, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = additionalNotes,
            onValueChange = { additionalNotes = it },
            label = { Text("Additional details (optional)") },
            modifier = Modifier.fillMaxWidth().testTag("report_notes_input"),
            maxLines = 3,
            shape = RoundedCornerShape(12.dp)
          )
        }
      }
    },
    confirmButton = {
      if (submitted) {
        Button(onClick = onDismiss) { Text("Done") }
      } else {
        Button(
          onClick = {
            onSubmit(selectedReason, additionalNotes)
            submitted = true
          },
          colors = ButtonDefaults.buttonColors(containerColor = ZevoraError),
          modifier = Modifier.testTag("submit_report_btn")
        ) {
          Text("Submit Report")
        }
      }
    },
    dismissButton = {
      if (!submitted) {
        TextButton(onClick = onDismiss) { Text("Cancel") }
      }
    }
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommentControlSheet(
  currentOption: CommentControlOption,
  bannedKeywords: List<String>,
  onOptionSelected: (CommentControlOption) -> Unit,
  onAddKeyword: (String) -> Unit,
  onRemoveKeyword: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var newKeyword by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Comment Controls (कमेंट नियंत्रण)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
      ) {
        Text("Who can comment on your posts:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(6.dp))

        CommentControlOption.values().forEach { option ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onOptionSelected(option) }
              .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(
              selected = currentOption == option,
              onClick = { onOptionSelected(option) }
            )
            Text(option.labelKey, fontSize = 14.sp, modifier = Modifier.padding(start = 6.dp))
          }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text("Banned Words Filter (ब्लॉक किए गए शब्द):", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text("Comments containing these words will be hidden automatically.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
          OutlinedTextField(
            value = newKeyword,
            onValueChange = { newKeyword = it },
            placeholder = { Text("Add word...") },
            modifier = Modifier.weight(1f).testTag("banned_word_input"),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              if (newKeyword.isNotBlank()) {
                onAddKeyword(newKeyword)
                newKeyword = ""
              }
            },
            modifier = Modifier.testTag("add_banned_word_btn")
          ) {
            Text("Add")
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chip list
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          bannedKeywords.forEach { word ->
            InputChip(
              selected = true,
              onClick = { onRemoveKeyword(word) },
              label = { Text(word, fontSize = 12.sp) },
              trailingIcon = {
                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
              }
            )
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss, modifier = Modifier.testTag("comment_control_done_btn")) {
        Text("Save & Close")
      }
    }
  )
}

@Composable
fun DeleteAccountDialog(
  onDismiss: () -> Unit,
  onConfirmDelete: (reason: String) -> Unit
) {
  var selectedReason by remember { mutableStateOf("Privacy concerns (गोपनीयता चिंताएं)") }
  var confirmCheck by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = ZevoraError)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Account Delete करना", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ZevoraError)
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
      ) {
        Text(
          "चेतावनी: आपका VEXORA खाता स्थायी रूप से हटा दिया जाएगा। आपकी सभी पोस्ट, रील्स, चैट और फॉलोअर्स हमेशा के लिए मिट जाएंगे।",
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
          color = ZevoraWarning.copy(alpha = 0.15f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            "⏳ 30-Day Grace Period: आप अगले 30 दिनों के भीतर सपोर्ट से संपर्क करके अपना खाता पुनः सक्रिय कर सकते हैं।",
            modifier = Modifier.padding(10.dp),
            fontSize = 12.sp,
            color = ZevoraWarning
          )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Reason for leaving (कारण):", fontSize = 13.sp, fontWeight = FontWeight.Medium)

        val reasons = listOf(
          "Privacy concerns (गोपनीयता चिंताएं)",
          "Too distracting (समय ज्यादा खर्च होना)",
          "Creating another account (नया खाता बनाना)",
          "Trouble getting started (उपयोग में परेशानी)"
        )

        reasons.forEach { r ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { selectedReason = r }
              .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(selected = selectedReason == r, onClick = { selectedReason = r })
            Text(r, fontSize = 12.sp)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Checkbox(
            checked = confirmCheck,
            onCheckedChange = { confirmCheck = it },
            modifier = Modifier.testTag("delete_acc_checkbox")
          )
          Text("I understand that this action is irreversible.", fontSize = 12.sp)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirmDelete(selectedReason) },
        enabled = confirmCheck,
        colors = ButtonDefaults.buttonColors(containerColor = ZevoraError),
        modifier = Modifier.testTag("confirm_delete_acc_btn")
      ) {
        Text("Permanently Delete")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Keep Account") }
    }
  )
}
