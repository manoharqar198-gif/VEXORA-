package com.example.zevora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.zevora.model.ChatConversation
import com.example.zevora.ui.ZevoraViewModel
import com.example.zevora.ui.theme.ZevoraSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
  viewModel: ZevoraViewModel,
  onNavigateBack: () -> Unit
) {
  val conversations by viewModel.chatConversations.collectAsState()
  val activeChat by viewModel.activeChat.collectAsState()

  var messageInput by remember { mutableStateOf("") }
  var callTypeMock by remember { mutableStateOf<String?>(null) }

  if (activeChat != null) {
    // 1-to-1 Chat Room
    val chat = activeChat!!
    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box {
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.primaryContainer,
                  modifier = Modifier.size(36.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(chat.participant.name.take(1), fontWeight = FontWeight.Bold)
                  }
                }
                if (chat.isOnline) {
                  Box(
                    modifier = Modifier
                      .size(10.dp)
                      .clip(CircleShape)
                      .background(ZevoraSuccess)
                      .align(Alignment.BottomEnd)
                  )
                }
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(chat.participant.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(if (chat.isOnline) "Active Now" else "Offline", fontSize = 11.sp, color = if (chat.isOnline) ZevoraSuccess else MaterialTheme.colorScheme.outline)
              }
            }
          },
          navigationIcon = {
            IconButton(onClick = { viewModel.closeChatConversation() }) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
          },
          actions = {
            IconButton(onClick = { callTypeMock = "Audio Call" }, modifier = Modifier.testTag("audio_call_btn")) {
              Icon(Icons.Default.Phone, contentDescription = "Call")
            }
            IconButton(onClick = { callTypeMock = "Video Call" }, modifier = Modifier.testTag("video_call_btn")) {
              Icon(Icons.Default.Videocam, contentDescription = "Video Call")
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
        // Message list
        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .padding(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(chat.messages) { msg ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = if (msg.isMe) Arrangement.End else Arrangement.Start
            ) {
              Surface(
                shape = RoundedCornerShape(
                  topStart = 16.dp,
                  topEnd = 16.dp,
                  bottomStart = if (msg.isMe) 16.dp else 4.dp,
                  bottomEnd = if (msg.isMe) 4.dp else 16.dp
                ),
                color = if (msg.isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.widthIn(max = 280.dp)
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  if (msg.isVoiceNote) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(Icons.Default.PlayArrow, contentDescription = "Play voice note", modifier = Modifier.size(24.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      LinearProgressIndicator(
                        progress = { 0.4f },
                        modifier = Modifier.width(120.dp).height(4.dp).clip(CircleShape)
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("${msg.voiceDurationSec}s", fontSize = 11.sp)
                    }
                  } else {
                    Text(
                      text = msg.text,
                      fontSize = 14.sp,
                      color = if (msg.isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = msg.timestamp,
                    fontSize = 10.sp,
                    color = if (msg.isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.align(Alignment.End)
                  )
                }
              }
            }
          }
        }

        // Bottom input bar
        Surface(
          tonalElevation = 2.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(onClick = { /* Photo */ }) {
              Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Send Photo", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = {
              viewModel.sendMessage(chat.id, "🎤 Voice note (0:15)")
            }) {
              Icon(Icons.Default.Mic, contentDescription = "Voice note", tint = MaterialTheme.colorScheme.primary)
            }
            OutlinedTextField(
              value = messageInput,
              onValueChange = { messageInput = it },
              placeholder = { Text("Message...") },
              modifier = Modifier.weight(1f).testTag("chat_input_field"),
              singleLine = true,
              shape = RoundedCornerShape(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
              onClick = {
                if (messageInput.isNotBlank()) {
                  viewModel.sendMessage(chat.id, messageInput)
                  messageInput = ""
                }
              },
              modifier = Modifier.testTag("send_chat_msg_btn")
            ) {
              Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
            }
          }
        }
      }
    }
  } else {
    // Conversation List
    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text("Messages & Chat", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
          navigationIcon = {
            IconButton(onClick = onNavigateBack) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
          },
          actions = {
            IconButton(onClick = { /* New conversation */ }) {
              Icon(Icons.Default.EditNote, contentDescription = "New Chat")
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )
      }
    ) { padding ->
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
        contentPadding = PaddingValues(bottom = 80.dp)
      ) {
        items(conversations) { conv ->
          ChatConversationItem(
            conversation = conv,
            onClick = { viewModel.openChatConversation(conv) }
          )
        }
      }
    }
  }

  // Call Mockup Dialog
  callTypeMock?.let { callType ->
    AlertDialog(
      onDismissRequest = { callTypeMock = null },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(if (callType == "Video Call") Icons.Default.Videocam else Icons.Default.Phone, contentDescription = null, tint = ZevoraSuccess)
          Spacer(modifier = Modifier.width(8.dp))
          Text(callType, fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(64.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(activeChat?.participant?.name?.take(1) ?: "U", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          Text("Calling ${activeChat?.participant?.name ?: "User"}...", fontWeight = FontWeight.SemiBold)
          Text("End-to-End Encrypted via VEXORA Secure Protocol", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        }
      },
      confirmButton = {
        Button(
          onClick = { callTypeMock = null },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("End Call")
        }
      }
    )
  }
}

@Composable
private fun ChatConversationItem(
  conversation: ChatConversation,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(50.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(conversation.participant.name.take(1), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
      }
      if (conversation.isOnline) {
        Box(
          modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(ZevoraSuccess)
            .align(Alignment.BottomEnd)
        )
      }
    }

    Spacer(modifier = Modifier.width(14.dp))

    Column(modifier = Modifier.weight(1f)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(conversation.participant.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(conversation.lastTime, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
      }
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = conversation.lastMessage,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1
      )
    }

    if (conversation.unreadCount > 0) {
      Spacer(modifier = Modifier.width(8.dp))
      Badge {
        Text("${conversation.unreadCount}")
      }
    }
  }
}
