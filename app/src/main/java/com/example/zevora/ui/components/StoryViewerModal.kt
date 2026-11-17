package com.example.zevora.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.zevora.model.Story
import com.example.zevora.model.StoryItem
import com.example.zevora.ui.ZevoraViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryViewerModal(
  story: Story,
  viewModel: ZevoraViewModel,
  onClose: () -> Unit
) {
  val items = story.items.ifEmpty {
    listOf(
      StoryItem(
        id = "si_sample_${story.id}",
        user = story.user,
        caption = "Exploring the beauty of nature ✨",
        timestamp = "2h ago",
        gradientStart = 0xFF8B5CF6,
        gradientEnd = 0xFFEC4899
      )
    )
  }

  var currentIndex by remember { mutableIntStateOf(0) }
  val currentItem = items.getOrElse(currentIndex) { items.first() }

  // Record view
  LaunchedEffect(currentItem.id) {
    viewModel.recordStoryView(currentItem.id)
  }

  var isPaused by remember { mutableStateOf(false) }
  var showViewersSheet by remember { mutableStateOf(false) }
  var replyText by remember { mutableStateOf("") }
  var replySentBanner by remember { mutableStateOf<String?>(null) }
  var isSavedHighlight by remember { mutableStateOf(currentItem.isSavedToHighlights) }

  // Auto-progress bar
  val progress = remember { Animatable(0f) }

  LaunchedEffect(currentIndex, isPaused) {
    if (!isPaused) {
      progress.snapTo(0f)
      progress.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 4000, easing = LinearEasing)
      )
      if (currentIndex < items.size - 1) {
        currentIndex += 1
      } else {
        onClose()
      }
    }
  }

  Dialog(
    onDismissRequest = onClose,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .pointerInput(Unit) {
          detectTapGestures(
            onPress = {
              isPaused = true
              tryAwaitRelease()
              isPaused = false
            },
            onTap = { offset ->
              if (offset.x < size.width / 3f) {
                if (currentIndex > 0) currentIndex -= 1
              } else if (offset.x > (size.width * 2f / 3f)) {
                if (currentIndex < items.size - 1) currentIndex += 1 else onClose()
              }
            }
          )
        }
    ) {
      // 1. Story Media Background (Image or Dynamic Cyberpunk Gradient)
      if (currentItem.mediaUrl.isNotBlank()) {
        AsyncImage(
          model = currentItem.mediaUrl,
          contentDescription = "Story Media",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      } else {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                listOf(Color(currentItem.gradientStart), Color(currentItem.gradientEnd), Color(0xFF0F172A))
              )
            )
        )
      }

      // Top Shadow Gradient for readable headers
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(140.dp)
          .background(
            Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent))
          )
      )

      // 2. Story Header (Progress bars + User Info + Close)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .padding(horizontal = 12.dp, vertical = 8.dp)
      ) {
        // Multi-segment progress indicators
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          items.forEachIndexed { index, _ ->
            val segProgress = when {
              index < currentIndex -> 1f
              index == currentIndex -> progress.value
              else -> 0f
            }
            LinearProgressIndicator(
              progress = { segProgress },
              modifier = Modifier
                .weight(1f)
                .height(2.5.dp)
                .clip(RoundedCornerShape(2.dp)),
              color = Color.White,
              trackColor = Color.White.copy(alpha = 0.3f),
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Author info row
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .background(Color(0xFF8B5CF6), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text(
              story.user.name.take(1),
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              story.user.name,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
            Text(
              "${currentItem.timestamp} • 24h Story",
              color = Color.White.copy(alpha = 0.7f),
              fontSize = 11.sp
            )
          }
          Spacer(modifier = Modifier.weight(1f))

          IconButton(onClick = onClose, modifier = Modifier.testTag("story_close_btn")) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
          }
        }
      }

      // 3. Center Caption
      if (currentItem.caption.isNotBlank()) {
        Box(
          modifier = Modifier
            .align(Alignment.Center)
            .padding(horizontal = 24.dp)
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
          Text(
            currentItem.caption,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp
          )
        }
      }

      // 4. Bottom Row: Reactions, Reply, Viewer Count & Save to Highlights
      Column(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .background(
            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f), Color.Black))
          )
          .navigationBarsPadding()
          .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        // Quick emoji reactions
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceAround
        ) {
          listOf("❤️", "🔥", "😮", "😂", "👏", "💯").forEach { emoji ->
            Surface(
              color = Color.Black.copy(alpha = 0.6f),
              shape = CircleShape,
              modifier = Modifier
                .size(34.dp)
                .clickable {
                  viewModel.sendLiveComment(emoji)
                }
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 16.sp)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        replySentBanner?.let { banner ->
          Surface(
            color = Color(0xFF10B981).copy(alpha = 0.85f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(bottom = 8.dp)
          ) {
            Text(
              text = banner,
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Reply input field
          OutlinedTextField(
            value = replyText,
            onValueChange = { replyText = it },
            placeholder = { Text("स्टोरी का जवाब दें... Send reply", color = Color(0xFF94A3B8), fontSize = 12.sp) },
            trailingIcon = {
              if (replyText.isNotBlank()) {
                IconButton(onClick = {
                  val cleanReply = replyText.trim()
                  viewModel.sendLiveComment("Reply to @${story.user.username}: $cleanReply")
                  replySentBanner = "जवाब भेजा गया (@${story.user.username})"
                  replyText = ""
                }) {
                  Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color(0xFF38BDF8))
                }
              }
            },
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = Color.Black.copy(alpha = 0.6f),
              unfocusedContainerColor = Color.Black.copy(alpha = 0.6f),
              focusedBorderColor = Color(0xFF8B5CF6),
              unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(24.dp),
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .height(46.dp)
              .testTag("story_reply_input")
          )

          Spacer(modifier = Modifier.width(8.dp))

          // 🌟 Save to Highlights Button
          IconButton(
            onClick = {
              viewModel.saveStoryToHighlights(currentItem.id)
              isSavedHighlight = true
            },
            modifier = Modifier
              .background(
                if (isSavedHighlight) Color(0xFFF59E0B) else Color.Black.copy(alpha = 0.6f),
                CircleShape
              )
              .size(42.dp)
              .testTag("story_save_highlight_btn")
          ) {
            Icon(
              if (isSavedHighlight) Icons.Default.Star else Icons.Outlined.StarBorder,
              contentDescription = "Save to Highlights",
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          // Viewers count trigger
          Surface(
            modifier = Modifier
              .clip(RoundedCornerShape(20.dp))
              .clickable {
                isPaused = true
                showViewersSheet = true
              }
              .testTag("story_viewers_trigger"),
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(20.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                "${currentItem.viewers.size + 14}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
            }
          }
        }
      }

      // 5. MODAL: Story Viewers List
      if (showViewersSheet) {
        ModalBottomSheet(
          onDismissRequest = {
            showViewersSheet = false
            isPaused = false
          },
          containerColor = Color(0xFF0F172A)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp)
              .padding(bottom = 32.dp)
          ) {
            Text(
              "स्टोरी देखने वाले • Story Viewers (${currentItem.viewers.size + 14})",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Text(
              "24 घंटे में इस स्टोरी को देखने वाले दर्शकों की सूची",
              fontSize = 12.sp,
              color = Color(0xFF94A3B8),
              modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              items(currentItem.viewers) { viewer ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .size(38.dp)
                      .background(Color(0xFF8B5CF6), CircleShape),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(viewer.user.name.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(viewer.user.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("@${viewer.user.username}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                  }
                  Text(viewer.viewedAt, color = Color(0xFF64748B), fontSize = 11.sp)
                }
              }
            }
          }
        }
      }
    }
  }
}
