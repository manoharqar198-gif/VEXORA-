package com.example.zevora.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zevora.model.Comment
import com.example.zevora.model.NetworkQuality
import com.example.zevora.model.Reel
import com.example.zevora.model.VideoQualityOption
import com.example.zevora.ui.ZevoraViewModel
import com.example.zevora.ui.theme.ZevoraSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsScreen(
  viewModel: ZevoraViewModel,
  onCreateReel: () -> Unit = {}
) {
  val reels by viewModel.reels.collectAsState()
  val networkQuality by viewModel.networkQuality.collectAsState()
  val dataSaverPrefs by viewModel.dataSaverPreferences.collectAsState()
  val coroutineScope = rememberCoroutineScope()

  if (reels.isEmpty()) return

  val pagerState = rememberPagerState(pageCount = { reels.size })
  var selectedReelIdForComments by remember { mutableStateOf<String?>(null) }
  var showShareDialogForReel by remember { mutableStateOf<Reel?>(null) }
  var showQualityPicker by remember { mutableStateOf(false) }
  var commentInput by remember { mutableStateOf("") }

  // Double tap heart animation state
  var showDoubleTapHeart by remember { mutableStateOf(false) }

  val context = androidx.compose.ui.platform.LocalContext.current
  val sub by viewModel.premiumSubscription.collectAsState()

  // Track page changes for video monetization & AdMob Interstitial Ads
  LaunchedEffect(pagerState.currentPage) {
    // Record verified eligible view for creator monetization
    viewModel.creditReelsAdShare(1)
    
    // Show AdMob interstitial video ad every few reels if not VIP Pro
    if (com.example.zevora.monetization.AdMobManager.isAdEligible(sub.isPremium) &&
      pagerState.currentPage > 0 && pagerState.currentPage % 3 == 0
    ) {
      if (context is android.app.Activity) {
        com.example.zevora.monetization.AdMobManager.showInterstitialAd(
          activity = context,
          isPremium = sub.isPremium
        )
      }
    }
  }

  // Rotating audio disk animation for the ACTIVE reel only
  val infiniteTransition = rememberInfiniteTransition(label = "audio_disk")
  val rotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(3500, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "rotation"
  )

  // Double-tap heart scale
  val heartScale = remember { Animatable(0f) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    // 1. High-Performance Hardware Accelerated VerticalPager
    VerticalPager(
      state = pagerState,
      modifier = Modifier
        .fillMaxSize()
        .testTag("reels_vertical_pager"),
      key = { index -> reels[index].id },
      beyondViewportPageCount = 1 // Preload 1 next reel metadata
    ) { pageIndex ->
      val reel = reels[pageIndex]
      val isActivePage = pagerState.currentPage == pageIndex

      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              listOf(
                Color(reel.gradientStart),
                Color(reel.gradientEnd),
                Color(0xFF0B0F19)
              )
            )
          )
          .pointerInput(reel.id) {
            detectTapGestures(
              onDoubleTap = {
                // Optimistic instant like on double tap
                if (!reel.isLiked) {
                  viewModel.toggleReelLike(reel.id)
                }
                coroutineScope.launch {
                  showDoubleTapHeart = true
                  heartScale.snapTo(0f)
                  heartScale.animateTo(
                    targetValue = 1.3f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                  )
                  delay(450)
                  heartScale.animateTo(0f, animationSpec = tween(200))
                  showDoubleTapHeart = false
                }
              }
            )
          }
      ) {
        // Video Playback Center Simulation (Active only when visible to save battery/RAM)
        Column(
          modifier = Modifier
            .align(Alignment.Center)
            .padding(horizontal = 30.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = if (isActivePage) 0.22f else 0.08f),
            modifier = Modifier.size(72.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(44.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = "VEXORA HD Reel • 60 FPS",
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 17.sp
          )
          Text(
            text = "👀 ${reel.viewsCount} views • स्वाइप करके अगला रील देखें",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.85f)
          )
          if (dataSaverPrefs.isDataSaverEnabled || networkQuality == NetworkQuality.SLOW) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
              color = Color(0xFF0284C7).copy(alpha = 0.35f),
              shape = RoundedCornerShape(10.dp)
            ) {
              Text(
                "⚡ Data Saver Active (480p Smooth)",
                color = Color(0xFF38BDF8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
          }
        }

        // Right side Action Bar (Like, Comment, Share, Audio)
        Column(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(bottom = 90.dp, end = 12.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // Like Button (Optimistic instant response)
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
              onClick = { viewModel.toggleReelLike(reel.id) },
              modifier = Modifier.testTag("like_reel_btn_${reel.id}")
            ) {
              Icon(
                imageVector = if (reel.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Like",
                tint = if (reel.isLiked) Color(0xFFEF4444) else Color.White,
                modifier = Modifier.size(32.dp)
              )
            }
            Text("${reel.likesCount}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }

          // Comment Button
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
              onClick = { selectedReelIdForComments = reel.id },
              modifier = Modifier.testTag("comment_reel_btn_${reel.id}")
            ) {
              Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comments", tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Text("${reel.commentsCount}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }

          // Share Button
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
              onClick = { showShareDialogForReel = reel },
              modifier = Modifier.testTag("share_reel_btn_${reel.id}")
            ) {
              Icon(Icons.Outlined.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Text("${reel.sharesCount}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }

          // Rotating Audio Vinyl (Only animates if this page is active)
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(Color.Black.copy(alpha = 0.6f))
              .then(if (isActivePage) Modifier.rotate(rotation) else Modifier)
          ) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = ZevoraSecondary, modifier = Modifier.size(20.dp))
          }
        }

        // Bottom left: Creator metadata & Caption
        Column(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 16.dp, bottom = 90.dp, end = 80.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              shape = CircleShape,
              color = Color.White,
              modifier = Modifier.size(36.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(reel.creator.name.take(1), fontWeight = FontWeight.Bold, color = Color.Black)
              }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text("@${reel.creator.username}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (reel.creator.isVerified) {
              Spacer(modifier = Modifier.width(4.dp))
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ZevoraSecondary, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color.White.copy(alpha = 0.25f),
              modifier = Modifier.clickable {
                viewModel.toggleFollowUser(reel.creator.id)
              }
            ) {
              Text(
                "Follow",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = reel.caption,
            color = Color.White,
            fontSize = 13.sp,
            maxLines = 2,
            lineHeight = 17.sp
          )

          Spacer(modifier = Modifier.height(6.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(reel.musicTitle, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
          }
        }
      }
    }

    // 2. Double-Tap Expanding Heart Overlay
    if (showDoubleTapHeart) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          Icons.Default.Favorite,
          contentDescription = null,
          tint = Color(0xFFEF4444).copy(alpha = 0.9f),
          modifier = Modifier
            .size(110.dp)
            .scale(heartScale.value)
        )
      }
    }

    // 3. Top Bar Overlay
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          "🎬 Reels",
          fontWeight = FontWeight.Black,
          fontSize = 22.sp,
          color = Color.White
        )
        Spacer(modifier = Modifier.width(8.dp))
        // Quality selector pill
        Surface(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { showQualityPicker = true }
            .testTag("reels_quality_pill"),
          color = Color.Black.copy(alpha = 0.5f)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .background(
                  if (networkQuality == NetworkQuality.SLOW) Color(0xFFF59E0B) else Color(0xFF10B981),
                  CircleShape
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              dataSaverPrefs.videoQuality.title.take(10),
              color = Color.White,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
          }
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        Button(
          onClick = onCreateReel,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("reels_create_btn")
        ) {
          Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("बनाएं +", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    // Sponsored Banner Ad inside Reels for non-premium users (non-intrusive strip below top bar)
    if (com.example.zevora.monetization.AdMobManager.isAdEligible(sub.isPremium) && pagerState.currentPage % 2 == 1) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .padding(top = 64.dp, start = 14.dp, end = 14.dp)
      ) {
        com.example.zevora.monetization.AdMobFeedBanner(
          isPremium = sub.isPremium,
          compact = true
        )
      }
    }

    // 4. Modal: Video Quality Selector
    if (showQualityPicker) {
      AlertDialog(
        onDismissRequest = { showQualityPicker = false },
        containerColor = Color(0xFF0F172A),
        title = { Text("वीडियो क्वालिटी • Video Quality", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            VideoQualityOption.values().forEach { option ->
              val isSelected = dataSaverPrefs.videoQuality == option
              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .clickable {
                    viewModel.updateDataSaverPreferences(
                      dataSaverPrefs.copy(
                        videoQuality = option,
                        isDataSaverEnabled = option == VideoQualityOption.DATA_SAVER
                      )
                    )
                    showQualityPicker = false
                  },
                color = if (isSelected) Color(0xFF1E293B) else Color.Transparent,
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6)) else null
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(option.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(option.description, color = Color(0xFF94A3B8), fontSize = 11.sp)
                  }
                  if (isSelected) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF8B5CF6))
                  }
                }
              }
            }
          }
        },
        confirmButton = {
          TextButton(onClick = { showQualityPicker = false }) {
            Text("Done", color = Color(0xFF8B5CF6))
          }
        }
      )
    }

    // 5. Comments Bottom Sheet
    val activeCommentsReel = selectedReelIdForComments?.let { id -> reels.firstOrNull { it.id == id } }
    activeCommentsReel?.let { activeReel ->
      ModalBottomSheet(
        onDismissRequest = { selectedReelIdForComments = null },
        containerColor = Color(0xFF0F172A)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
        ) {
          Text(
            "रील्स कमेंट्स (${activeReel.commentsCount})",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Spacer(modifier = Modifier.height(14.dp))

          if (activeReel.comments.isEmpty()) {
            Text(
              "अभी कोई कमेंट नहीं है — पहला कमेंट लिखें!",
              color = Color(0xFF94A3B8),
              fontSize = 13.sp,
              modifier = Modifier.padding(vertical = 12.dp)
            )
          } else {
            activeReel.comments.forEach { comment ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFF8B5CF6), CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Text(comment.authorName.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(comment.authorName, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(comment.timestamp, color = Color(0xFF64748B), fontSize = 10.sp)
                  }
                  Text(comment.text, color = Color.White, fontSize = 12.sp)
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          OutlinedTextField(
            value = commentInput,
            onValueChange = { commentInput = it },
            placeholder = { Text("कमेंट लिखें... Add a comment", color = Color(0xFF94A3B8), fontSize = 12.sp) },
            trailingIcon = {
              IconButton(
                onClick = {
                  if (commentInput.isNotBlank()) {
                    viewModel.addReelComment(activeReel.id, commentInput)
                    commentInput = ""
                  }
                },
                modifier = Modifier.testTag("reel_comment_send_btn")
              ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color(0xFF38BDF8))
              }
            },
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = Color(0xFF1E293B),
              unfocusedContainerColor = Color(0xFF1E293B),
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(20.dp),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("reel_comment_input")
          )
        }
      }
    }

    // 6. Share Dialog
    showShareDialogForReel?.let { activeReel ->
      AlertDialog(
        onDismissRequest = { showShareDialogForReel = null },
        containerColor = Color(0xFF0F172A),
        title = { Text("Share Reel", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
          Text(
            "Reel link copied: https://vexora.app/reel/${activeReel.id}\nShare with friends and groups!",
            color = Color(0xFF94A3B8)
          )
        },
        confirmButton = {
          Button(onClick = { showShareDialogForReel = null }) {
            Text("Copy Link")
          }
        }
      )
    }
  }
}
