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
import androidx.compose.material.icons.automirrored.filled.SpeakerNotes
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
import com.example.zevora.model.AppLanguage
import com.example.zevora.model.CommentControlOption
import com.example.zevora.ui.ZevoraViewModel
import com.example.zevora.ui.components.CommentControlSheet
import com.example.zevora.ui.components.ReportModal
import com.example.zevora.ui.theme.ZevoraError
import com.example.zevora.ui.theme.ZevoraSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityPrivacyScreen(
  viewModel: ZevoraViewModel,
  onNavigateBack: () -> Unit
) {
  val language by viewModel.appLanguage.collectAsState()
  val isPrivate by viewModel.isPrivateAccount.collectAsState()
  val blockedUsers by viewModel.blockedUsers.collectAsState()
  val commentControl by viewModel.commentControl.collectAsState()
  val filteredKeywords by viewModel.filteredKeywords.collectAsState()

  // Dialog triggers
  var showBlockUserDialog by remember { mutableStateOf(false) }
  var showReportUserDialog by remember { mutableStateOf(false) }
  var showReportPostDialog by remember { mutableStateOf(false) }
  var showCommentControlDialog by remember { mutableStateOf(false) }
  var newBlockUsername by remember { mutableStateOf("") }
  var newBlockName by remember { mutableStateOf("") }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = if (language == AppLanguage.HINDI) "सुरक्षा और गोपनीयता" else "Security & Privacy",
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp
            )
            Text(
              text = "Privacy Controls, Blocking & Safety",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("sec_back_btn")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      contentPadding = PaddingValues(vertical = 16.dp)
    ) {
      // 1. Privacy Health Banner
      item {
        PrivacyHealthCard(
          isPrivate = isPrivate,
          blockedCount = blockedUsers.size,
          keywordFilterCount = filteredKeywords.size
        )
      }

      // 2. Private Account Toggle
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Surface(
                shape = CircleShape,
                color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Default.LockPerson, contentDescription = null, tint = Color(0xFFF59E0B))
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text("Private Account (निजी खाता)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                  "केवल स्वीकृत फॉलोअर्स ही आपकी पोस्ट और रील्स देख पाएंगे।",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
            Switch(
              checked = isPrivate,
              onCheckedChange = { viewModel.togglePrivateAccount(it) },
              modifier = Modifier.testTag("private_acc_switch")
            )
          }
        }
      }

      // 3. Comment Control Card
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier.fillMaxWidth().clickable { showCommentControlDialog = true }
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.AutoMirrored.Filled.SpeakerNotes, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text("Comment Control (टिप्पणी नियंत्रण)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Allowed: ${commentControl.labelKey} • ${filteredKeywords.size} words blocked", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
          }
        }
      }

      // 4. Block User Section
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = CircleShape,
                  color = ZevoraError.copy(alpha = 0.15f),
                  modifier = Modifier.size(40.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = ZevoraError)
                  }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text("Block User (${blockedUsers.size} Blocked)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                  Text("Manage blocked contacts", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
              TextButton(
                onClick = { showBlockUserDialog = true },
                modifier = Modifier.testTag("block_new_user_btn")
              ) {
                Text("+ Block User")
              }
            }

            Spacer(modifier = Modifier.height(10.dp))
            if (blockedUsers.isEmpty()) {
              Text("No users blocked yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                blockedUsers.forEach { bUser ->
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                      .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Column {
                      Text("@${bUser.username}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                      Text("${bUser.name} • ${bUser.blockedDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(
                      onClick = { viewModel.unblockUser(bUser.id) },
                      modifier = Modifier.testTag("unblock_${bUser.username}")
                    ) {
                      Text("Unblock", color = ZevoraError, fontSize = 12.sp)
                    }
                  }
                }
              }
            }
          }
        }
      }

      // 5. Report User & Report Post Row
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.weight(1f).clickable { showReportUserDialog = true }
          ) {
            Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Default.PersonRemove, contentDescription = null, tint = ZevoraError, modifier = Modifier.size(28.dp))
              Spacer(modifier = Modifier.height(6.dp))
              Text("Report User", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              Text("यूजर की रिपोर्ट करें", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }

          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.weight(1f).clickable { showReportPostDialog = true }
          ) {
            Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Default.ReportProblem, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(28.dp))
              Spacer(modifier = Modifier.height(6.dp))
              Text("Report Post", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              Text("पोस्ट की रिपोर्ट करें", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
    }
  }

  if (showBlockUserDialog) {
    AlertDialog(
      onDismissRequest = { showBlockUserDialog = false },
      title = { Text("Block a User", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          OutlinedTextField(
            value = newBlockUsername,
            onValueChange = { newBlockUsername = it },
            label = { Text("Username (@handle)") },
            modifier = Modifier.fillMaxWidth().testTag("block_username_input"),
            singleLine = true
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = newBlockName,
            onValueChange = { newBlockName = it },
            label = { Text("Name / Description") },
            modifier = Modifier.fillMaxWidth().testTag("block_name_input"),
            singleLine = true
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newBlockUsername.isNotBlank()) {
              viewModel.blockUser(newBlockUsername, newBlockName.ifBlank { "Unknown User" })
              newBlockUsername = ""
              newBlockName = ""
              showBlockUserDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = ZevoraError),
          modifier = Modifier.testTag("confirm_block_user_btn")
        ) {
          Text("Block")
        }
      },
      dismissButton = {
        TextButton(onClick = { showBlockUserDialog = false }) { Text("Cancel") }
      }
    )
  }

  if (showReportUserDialog) {
    ReportModal(
      title = "Report User (यूजर की रिपोर्ट)",
      targetName = "Selected Profile / User",
      onDismiss = { showReportUserDialog = false },
      onSubmit = { reason, notes ->
        viewModel.reportUser("user_target", reason, notes)
      }
    )
  }

  if (showReportPostDialog) {
    ReportModal(
      title = "Report Post (पोस्ट की रिपोर्ट)",
      targetName = "Selected Post",
      onDismiss = { showReportPostDialog = false },
      onSubmit = { reason, notes ->
        viewModel.reportPost("post_target", reason, notes)
      }
    )
  }

  if (showCommentControlDialog) {
    CommentControlSheet(
      currentOption = commentControl,
      bannedKeywords = filteredKeywords,
      onOptionSelected = { viewModel.setCommentControl(it) },
      onAddKeyword = { viewModel.addFilteredKeyword(it) },
      onRemoveKeyword = { viewModel.removeFilteredKeyword(it) },
      onDismiss = { showCommentControlDialog = false }
    )
  }
}

@Composable
private fun PrivacyHealthCard(
  isPrivate: Boolean,
  blockedCount: Int,
  keywordFilterCount: Int
) {
  val score = 70 + (if (isPrivate) 15 else 5) + (if (keywordFilterCount > 0) 15 else 0)
  val healthColor = ZevoraSuccess

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(56.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.surface)
          .border(3.dp, healthColor, CircleShape)
      ) {
        Text("$score%", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = healthColor)
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column {
        Text("Privacy Protection: Active", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(
          text = "$blockedCount Blocked • $keywordFilterCount Filtered Words • Direct Access Mode",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
      }
    }
  }
}
