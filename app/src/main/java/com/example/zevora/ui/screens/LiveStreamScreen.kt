package com.example.zevora.ui.screens

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zevora.model.LiveComment
import com.example.zevora.model.LiveGuest
import com.example.zevora.model.User
import com.example.zevora.ui.ZevoraViewModel
import com.example.zevora.ui.components.CameraPreviewView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveStreamScreen(
  viewModel: ZevoraViewModel,
  onClose: () -> Unit
) {
  val activeRoom by viewModel.activeLiveRoom.collectAsState()
  val liveComments by viewModel.liveComments.collectAsState()
  val currentUser by viewModel.currentUser.collectAsState()
  val coroutineScope = rememberCoroutineScope()

  // Camera permissions
  var hasCameraPermission by remember { mutableStateOf(false) }
  var hasAudioPermission by remember { mutableStateOf(false) }

  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: false
    hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: false
  }

  LaunchedEffect(Unit) {
    permissionLauncher.launch(
      arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
      )
    )
  }

  // Camera facing state (front/back)
  var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }
  var isMicMuted by remember { mutableStateOf(false) }

  // Modals & Sheets
  var showEndLiveDialog by remember { mutableStateOf(false) }
  var saveRecordingChecked by remember { mutableStateOf(true) }
  var showAddGuestSheet by remember { mutableStateOf(false) }
  var selectedGuestForControl by remember { mutableStateOf<LiveGuest?>(null) }
  var showModerationSheet by remember { mutableStateOf(false) }
  var showLiveGiftSheet by remember { mutableStateOf(false) }

  val wallet by viewModel.userWallet.collectAsState()
  val activeLiveGift by viewModel.activeLiveGift.collectAsState()

  // Chat message input
  var chatInputText by remember { mutableStateOf("") }

  // Floating hearts animation state
  var heartBursts by remember { mutableStateOf(listOf<Long>()) }

  // Stream duration timer
  var elapsedSeconds by remember { mutableIntStateOf(0) }
  LaunchedEffect(activeRoom) {
    while (true) {
      delay(1000L)
      elapsedSeconds += 1
    }
  }

  fun formatTimer(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
  }

  // Hardware Back Handler
  BackHandler {
    showEndLiveDialog = true
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    // 1. Host Video Feed (CameraX Preview or Fallback)
    CameraPreviewView(
      lensFacing = lensFacing,
      modifier = Modifier.fillMaxSize(),
      fallbackGradient = Brush.verticalGradient(
        listOf(Color(0xFF311042), Color(0xFF130924), Color(0xFF07040D))
      )
    )

    // Subtle dark gradient overlay at top and bottom for readable controls
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            0.0f to Color.Black.copy(alpha = 0.65f),
            0.2f to Color.Transparent,
            0.5f to Color.Transparent,
            0.85f to Color.Black.copy(alpha = 0.85f),
            1.0f to Color.Black
          )
        )
    )

    // 2. TOP BAR: Live Badge, Stream Duration, Viewer Count, Close/End Button
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Host Avatar + Title pill
      Surface(
        color = Color.Black.copy(alpha = 0.55f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .background(
                Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))),
                shape = CircleShape
              ),
            contentAlignment = Alignment.Center
          ) {
            Text(
              currentUser.name.take(1),
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Column {
            Text(
              currentUser.name,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              maxLines = 1
            )
            Text(
              formatTimer(elapsedSeconds),
              color = Color(0xFF94A3B8),
              fontSize = 10.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      // 🔴 LIVE Badge
      Surface(
        color = Color(0xFFDC2626),
        shape = RoundedCornerShape(6.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(6.dp)
              .background(Color.White, shape = CircleShape)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            "LIVE",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Viewers Counter
      Surface(
        color = Color.Black.copy(alpha = 0.55f),
        shape = RoundedCornerShape(12.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            Icons.Default.Visibility,
            contentDescription = "Viewers",
            tint = Color(0xFF38BDF8),
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            "${activeRoom?.viewerCount ?: 142}",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
          )
        }
      }

      Spacer(modifier = Modifier.weight(1f))

      // Flip camera button
      IconButton(
        onClick = {
          lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
          } else {
            CameraSelector.LENS_FACING_FRONT
          }
        },
        modifier = Modifier
          .size(36.dp)
          .background(Color.Black.copy(alpha = 0.5f), CircleShape)
          .testTag("live_flip_camera")
      ) {
        Icon(
          Icons.Default.FlipCameraAndroid,
          contentDescription = "Flip Camera",
          tint = Color.White,
          modifier = Modifier.size(18.dp)
        )
      }

      Spacer(modifier = Modifier.width(6.dp))

      // End Live Button (लाल रंग का समाप्त करने का बटन)
      Button(
        onClick = { showEndLiveDialog = true },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.testTag("live_end_button")
      ) {
        Text("समाप्त", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
      }
    }

    // 3. PICTURE-IN-PICTURE (PiP) FLOATING GUEST CARDS (Upper Right)
    val guests = activeRoom?.guests ?: emptyList()
    if (guests.isNotEmpty()) {
      Column(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(top = 70.dp, end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        guests.forEach { guest ->
          GuestPipCard(
            guest = guest,
            onClick = { selectedGuestForControl = guest }
          )
        }
      }
    }

    // 4. FLOATING HEARTS BURST ANIMATION
    heartBursts.forEach { burstId ->
      FloatingHeart(key = burstId)
    }

    // 5. BOTTOM OVERLAY: Live Chat + Action Bar
    Column(
      modifier = Modifier
        .align(Alignment.BottomStart)
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      // Live Chat Stream (Scrollable messages)
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth(0.78f)
          .heightIn(max = 220.dp),
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        items(liveComments.asReversed(), key = { it.id }) { comment ->
          LiveCommentBubble(
            comment = comment,
            onUserClick = {
              viewModel.reportLiveUser(comment.user.username, "Inappropriate comment")
            }
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Quick Emoji Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf("❤️", "🔥", "👏", "💯", "🚀", "😍").forEach { emoji ->
          Surface(
            color = Color.Black.copy(alpha = 0.55f),
            shape = CircleShape,
            modifier = Modifier
              .size(32.dp)
              .clickable {
                viewModel.sendLiveComment(emoji)
                heartBursts = heartBursts + System.currentTimeMillis()
                viewModel.sendLiveHeart()
              }
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(emoji, fontSize = 14.sp)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Bottom Row: Chat text field + Controls (Add Guest, Mute Mic, Like Heart, Moderation)
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Comment Input
        OutlinedTextField(
          value = chatInputText,
          onValueChange = { chatInputText = it },
          placeholder = { Text("कमेंट लिखें... Live Chat", color = Color(0xFF94A3B8), fontSize = 13.sp) },
          trailingIcon = {
            if (chatInputText.isNotBlank()) {
              IconButton(
                onClick = {
                  viewModel.sendLiveComment(chatInputText)
                  chatInputText = ""
                },
                modifier = Modifier.testTag("live_send_comment_btn")
              ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color(0xFF38BDF8))
              }
            }
          },
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.Black.copy(alpha = 0.6f),
            unfocusedContainerColor = Color.Black.copy(alpha = 0.6f),
            focusedBorderColor = Color(0xFF8B5CF6),
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          ),
          shape = RoundedCornerShape(24.dp),
          singleLine = true,
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("live_comment_input")
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 1. Add Guest Button (Co-host invitation)
        IconButton(
          onClick = { showAddGuestSheet = true },
          modifier = Modifier
            .size(44.dp)
            .background(
              Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))),
              CircleShape
            )
            .testTag("live_add_guest_btn")
        ) {
          Icon(Icons.Default.PersonAdd, contentDescription = "Add Guest", tint = Color.White, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(6.dp))

        // 2. Mute Mic Button
        IconButton(
          onClick = { isMicMuted = !isMicMuted },
          modifier = Modifier
            .size(44.dp)
            .background(if (isMicMuted) Color(0xFFDC2626) else Color.Black.copy(alpha = 0.6f), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            .testTag("live_mic_mute_toggle")
        ) {
          Icon(
            if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
            contentDescription = "Mute Mic",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // 3. Virtual Live Gift Button
        IconButton(
          onClick = { showLiveGiftSheet = true },
          modifier = Modifier
            .size(44.dp)
            .background(
              Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))),
              CircleShape
            )
            .testTag("live_send_gift_btn")
        ) {
          Icon(Icons.Default.CardGiftcard, contentDescription = "Send Gift", tint = Color.White, modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.width(6.dp))

        // 4. Like Heart Burst Button
        IconButton(
          onClick = {
            coroutineScope.launch {
              heartBursts = heartBursts + System.currentTimeMillis()
              viewModel.sendLiveHeart()
            }
          },
          modifier = Modifier
            .size(44.dp)
            .background(
              Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFFEF4444))),
              CircleShape
            )
            .testTag("live_heart_like_btn")
        ) {
          Icon(Icons.Default.Favorite, contentDescription = "Like Live", tint = Color.White, modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.width(6.dp))

        // 4. Host Moderation Menu
        IconButton(
          onClick = { showModerationSheet = true },
          modifier = Modifier
            .size(44.dp)
            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            .testTag("live_moderation_menu_btn")
        ) {
          Icon(Icons.Default.MoreVert, contentDescription = "Moderation", tint = Color.White, modifier = Modifier.size(20.dp))
        }
      }
    }

    // 6. MODAL: Add / Invite Guest Sheet
    if (showAddGuestSheet) {
      ModalBottomSheet(
        onDismissRequest = { showAddGuestSheet = false },
        containerColor = Color(0xFF0F172A)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
        ) {
          Text(
            "Guest को Live में जोड़ें • Add Co-Host",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Text(
            "जुड़े हुए Co-Host की वीडियो स्क्रीन पर PiP विंडो में दिखाई देगी",
            fontSize = 12.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
          )

          val candidateUsers = listOf(
            User("u2", "Priya Verma", "priya_travels"),
            User("u3", "Tech Sutra", "techsutra_india"),
            User("u4", "Ananya Rao", "ananya_foodie"),
            User("u7", "Aryan Fitness", "aryan_fit")
          )

          candidateUsers.forEach { candidate ->
            val isAlreadyGuest = guests.any { it.user.id == candidate.id }
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .background(Color(0xFF8B5CF6), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Text(candidate.name.take(1), color = Color.White, fontWeight = FontWeight.Bold)
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(candidate.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("@${candidate.username}", color = Color(0xFF94A3B8), fontSize = 12.sp)
              }
              Button(
                onClick = {
                  if (!isAlreadyGuest) {
                    viewModel.addGuestToLive(candidate)
                  }
                  showAddGuestSheet = false
                },
                enabled = !isAlreadyGuest,
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isAlreadyGuest) Color(0xFF334155) else Color(0xFF8B5CF6)
                ),
                shape = RoundedCornerShape(10.dp)
              ) {
                Text(if (isAlreadyGuest) "जुड़े हैं" else "जोड़ें +", fontSize = 12.sp)
              }
            }
          }
        }
      }
    }

    // 7. MODAL: Guest Control (Mute / Remove Guest)
    selectedGuestForControl?.let { guest ->
      ModalBottomSheet(
        onDismissRequest = { selectedGuestForControl = null },
        containerColor = Color(0xFF0F172A)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
        ) {
          Text(
            "Guest नियंत्रण • ${guest.user.name}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Spacer(modifier = Modifier.height(16.dp))

          // Mute / Unmute guest audio
          OutlinedButton(
            onClick = {
              viewModel.toggleGuestMute(guest.id)
              selectedGuestForControl = null
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(
              if (guest.isAudioMuted) Icons.Default.Mic else Icons.Default.MicOff,
              contentDescription = null,
              tint = Color(0xFF38BDF8)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              if (guest.isAudioMuted) "आवाज चालू करें (Unmute Mic)" else "आवाज बंद करें (Mute Mic)",
              color = Color.White
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Remove guest from live
          Button(
            onClick = {
              viewModel.removeGuestFromLive(guest.id)
              selectedGuestForControl = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.PersonRemove, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Guest को Live से हटाएं (Remove)", color = Color.White, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // 8. MODAL: Moderation Sheet (Block user, Report user, Filter settings)
    if (showModerationSheet) {
      ModalBottomSheet(
        onDismissRequest = { showModerationSheet = false },
        containerColor = Color(0xFF0F172A)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
        ) {
          Text(
            "Live सुरक्षा और मॉडरेशन • Moderation",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Text(
            "खराब कमेंट ब्लॉक करें या अवांछित यूजर को रिपोर्ट करें",
            fontSize = 12.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
          )

          ListItem(
            headlineContent = { Text("कमेंट मॉडरेशन सक्रिय", color = Color.White, fontWeight = FontWeight.SemiBold) },
            supportingContent = { Text("गाली-गलौज और स्पैम कीवर्ड स्वतः ब्लॉक हैं", color = Color(0xFF94A3B8), fontSize = 12.sp) },
            leadingContent = { Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF10B981)) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
          )

          HorizontalDivider(color = Color(0xFF334155))

          ListItem(
            headlineContent = { Text("किसी यूजर को ब्लॉक करें", color = Color.White, fontWeight = FontWeight.SemiBold) },
            leadingContent = { Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFFEF4444)) },
            modifier = Modifier.clickable {
              viewModel.reportLiveUser("anonymous_viewer", "Abusive language")
              showModerationSheet = false
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
          )
        }
      }
    }

    // 8B. MODAL: Live Gift Drawer
    if (showLiveGiftSheet) {
      com.example.zevora.ui.components.LiveGiftModal(
        userCoins = wallet.coinsBalance,
        virtualGifts = viewModel.virtualGifts,
        onDismiss = { showLiveGiftSheet = false },
        onSendGift = { gift ->
          val res = viewModel.sendLiveGift(gift.id, activeRoom?.host?.name ?: "Host")
          if (res.first) {
            showLiveGiftSheet = false
          }
        },
        onBuyCoins = {
          // Quick top up 100 coins
          viewModel.purchaseCoins("pack_100", "LIVE_QUICK_BUY_${System.currentTimeMillis()}")
        }
      )
    }

    // 8C. Real-time Animated Gift Overlay
    activeLiveGift?.let { gift ->
      LaunchedEffect(gift) {
        delay(3200)
        viewModel.clearActiveLiveGift()
      }
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(bottom = 140.dp),
        contentAlignment = Alignment.Center
      ) {
        Card(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B).copy(alpha = 0.92f)),
          border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFBBF24)),
          modifier = Modifier
            .padding(horizontal = 32.dp)
            .animateContentSize()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(gift.icon, fontSize = 42.sp)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
              Text("🎁 LIVE GIFT RECEIVED!", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color(0xFFFBBF24))
              Text("${gift.name} sent to Host", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
              Text("+${gift.coinPrice} Coins Value • 70% Creator Credit", fontSize = 11.sp, color = Color(0xFFA5B4FC))
            }
          }
        }
      }
    }

    // 9. DIALOG: End Live Stream Summary & Save Video
    if (showEndLiveDialog) {
      AlertDialog(
        onDismissRequest = { showEndLiveDialog = false },
        containerColor = Color(0xFF0F172A),
        title = {
          Text(
            "क्या आप Live समाप्त करना चाहते हैं?",
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        },
        text = {
          Column {
            Text(
              "Live ब्रॉडकास्ट की अवधि: ${formatTimer(elapsedSeconds)}\nदर्शकों की संख्या: ${activeRoom?.viewerCount ?: 142}\nलाइक्स: ${activeRoom?.likesCount ?: 380}",
              color = Color(0xFF94A3B8),
              fontSize = 13.sp,
              lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Save video option
            Surface(
              color = Color(0xFF1E293B),
              shape = RoundedCornerShape(12.dp)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    "वीडियो सेव करें (Save Recording)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                  )
                  Text(
                    "लाइव समाप्त होने के बाद वॉच वीडियो में सेव रहेगा",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                  )
                }
                Switch(
                  checked = saveRecordingChecked,
                  onCheckedChange = { saveRecordingChecked = it },
                  colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF8B5CF6))
                )
              }
            }
          }
        },
        confirmButton = {
          Button(
            onClick = {
              showEndLiveDialog = false
              viewModel.endLiveStream(saveRecording = saveRecordingChecked)
              onClose()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
            modifier = Modifier.testTag("live_confirm_end_btn")
          ) {
            Text("समाप्त करें", color = Color.White, fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          TextButton(onClick = { showEndLiveDialog = false }) {
            Text("जारी रखें (Cancel)", color = Color(0xFF94A3B8))
          }
        }
      )
    }
  }
}

// Picture-in-Picture Card for Co-Host Guest
@Composable
fun GuestPipCard(
  guest: LiveGuest,
  onClick: () -> Unit
) {
  Surface(
    modifier = Modifier
      .width(110.dp)
      .height(150.dp)
      .clip(RoundedCornerShape(16.dp))
      .border(2.dp, Color(0xFF8B5CF6), RoundedCornerShape(16.dp))
      .clickable { onClick() }
      .testTag("live_guest_pip_${guest.user.id}"),
    color = Color(0xFF1E1B4B)
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      // Guest Video Preview Gradient simulation
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              listOf(Color(0xFF4C1D95), Color(0xFF1E1B4B), Color(0xFF0F172A))
            )
          )
      )

      // Center Guest Avatar & Name
      Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .background(Color(0xFF7C3AED), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text(
            guest.user.name.take(1),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          guest.user.name,
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(horizontal = 4.dp)
        )
      }

      // Mic Status badge (Top Right)
      Surface(
        color = if (guest.isAudioMuted) Color(0xFFDC2626) else Color(0xFF10B981),
        shape = CircleShape,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(6.dp)
          .size(20.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            if (guest.isAudioMuted) Icons.Default.MicOff else Icons.Default.Mic,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(12.dp)
          )
        }
      }

      // "Co-Host" badge (Bottom)
      Surface(
        color = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(topStart = 8.dp),
        modifier = Modifier.align(Alignment.BottomEnd)
      ) {
        Text(
          "Co-Host",
          color = Color(0xFF38BDF8),
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
      }
    }
  }
}

// Live Comment Bubble
@Composable
fun LiveCommentBubble(
  comment: LiveComment,
  onUserClick: () -> Unit
) {
  Surface(
    color = Color.Black.copy(alpha = 0.55f),
    shape = RoundedCornerShape(16.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    modifier = Modifier.clickable { onUserClick() }
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(22.dp)
          .background(
            if (comment.isHost) Color(0xFFEC4899) else Color(0xFF6366F1),
            CircleShape
          ),
        contentAlignment = Alignment.Center
      ) {
        Text(
          comment.user.name.take(1),
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 10.sp
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            comment.user.name,
            color = if (comment.isHost) Color(0xFFF472B6) else Color(0xFF38BDF8),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
          )
          if (comment.isHost) {
            Spacer(modifier = Modifier.width(4.dp))
            Text("HOST", color = Color(0xFFEC4899), fontSize = 9.sp, fontWeight = FontWeight.Black)
          }
        }
        Text(
          comment.message,
          color = Color.White,
          fontSize = 12.sp,
          lineHeight = 15.sp
        )
      }
    }
  }
}

// Floating Animated Heart
@Composable
fun FloatingHeart(key: Long) {
  val animY = remember { Animatable(0f) }
  val animAlpha = remember { Animatable(1f) }

  LaunchedEffect(key) {
    launch {
      animY.animateTo(
        targetValue = -350f,
        animationSpec = tween(durationMillis = 1800, easing = LinearOutSlowInEasing)
      )
    }
    launch {
      delay(600)
      animAlpha.animateTo(
        targetValue = 0f,
        animationSpec = tween(durationMillis = 1200)
      )
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(bottom = 120.dp, end = 24.dp),
    contentAlignment = Alignment.BottomEnd
  ) {
    Icon(
      Icons.Default.Favorite,
      contentDescription = null,
      tint = Color(0xFFEC4899).copy(alpha = animAlpha.value),
      modifier = Modifier
        .offset(
          x = ((key % 40).toInt() - 20).dp,
          y = animY.value.dp
        )
        .size(28.dp)
    )
  }
}
