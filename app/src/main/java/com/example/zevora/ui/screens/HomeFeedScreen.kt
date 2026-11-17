package com.example.zevora.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.zevora.model.AppLanguage
import com.example.zevora.model.NetworkQuality
import com.example.zevora.model.Post
import com.example.zevora.model.Story
import com.example.zevora.ui.Screen
import com.example.zevora.ui.ZevoraViewModel
import com.example.zevora.ui.components.*
import com.example.zevora.ui.theme.ZevoraPrimary
import com.example.zevora.ui.theme.ZevoraSecondary
import com.example.zevora.ui.theme.ZevoraTertiary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedScreen(
  viewModel: ZevoraViewModel,
  onNavigateTo: (Screen) -> Unit
) {
  val feedPosts by viewModel.feedPosts.collectAsState()
  val stories by viewModel.stories.collectAsState()
  val activeLiveRoom by viewModel.activeLiveRoom.collectAsState()
  val language by viewModel.appLanguage.collectAsState()
  val networkQuality by viewModel.networkQuality.collectAsState()
  val isOffline by viewModel.isOffline.collectAsState()
  val dataSaverPrefs by viewModel.dataSaverPreferences.collectAsState()
  val activeUploadTask by viewModel.activeUploadTask.collectAsState()
  val isFeedRefreshing by viewModel.isFeedRefreshing.collectAsState()
  val isInitialLoading by viewModel.isInitialFeedLoading.collectAsState()
  val chatConversations by viewModel.chatConversations.collectAsState()
  val premiumSub by viewModel.premiumSubscription.collectAsState()
  val totalUnreadChats = remember(chatConversations) {
    chatConversations.sumOf { it.unreadCount }
  }

  val coroutineScope = rememberCoroutineScope()
  val listState = rememberLazyListState()

  // Pagination observer (Loads next page smoothly as user scrolls)
  val shouldLoadMore by remember {
    derivedStateOf {
      val totalItems = listState.layoutInfo.totalItemsCount
      val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
      totalItems > 0 && lastVisible >= totalItems - 2
    }
  }

  LaunchedEffect(shouldLoadMore) {
    if (shouldLoadMore) {
      viewModel.loadMorePosts()
    }
  }

  var selectedPostIdForComments by remember { mutableStateOf<String?>(null) }
  var postToReport by remember { mutableStateOf<Post?>(null) }
  var newCommentInput by remember { mutableStateOf("") }
  var shareToastPost by remember { mutableStateOf<Post?>(null) }
  var activeStoryToView by remember { mutableStateOf<Story?>(null) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
              painter = painterResource(id = R.drawable.ic_vexora_logo_screenshot),
              contentDescription = "VEXORA Logo",
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              "VEXORA",
              fontWeight = FontWeight.Black,
              fontSize = 21.sp,
              color = MaterialTheme.colorScheme.primary,
              letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                "Social",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        },
        actions = {
          // 🔴 LIVE Header Action Button
          Surface(
            modifier = Modifier
              .clip(RoundedCornerShape(20.dp))
              .clickable {
                viewModel.startLiveStream("🔴 Hanging out Live with Friends ✨")
              }
              .testTag("home_header_live_btn"),
            color = Color(0xFFDC2626),
            shape = RoundedCornerShape(20.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .background(Color.White, CircleShape)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                "LIVE 🔴",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
              )
            }
          }

          Spacer(modifier = Modifier.width(4.dp))

          // Instant Feed Refresh button
          IconButton(
            onClick = { viewModel.refreshFeed() },
            modifier = Modifier.testTag("home_refresh_btn")
          ) {
            Icon(
              Icons.Default.Refresh,
              contentDescription = "Refresh",
              tint = MaterialTheme.colorScheme.primary
            )
          }

          IconButton(onClick = { onNavigateTo(Screen.SECURITY_PRIVACY) }, modifier = Modifier.testTag("home_security_shortcut")) {
            Icon(Icons.Default.Shield, contentDescription = "Security", tint = MaterialTheme.colorScheme.primary)
          }
          IconButton(onClick = { onNavigateTo(Screen.CHAT) }, modifier = Modifier.testTag("home_chat_btn")) {
            BadgedBox(
              badge = {
                if (totalUnreadChats > 0) {
                  Badge { Text("$totalUnreadChats") }
                }
              }
            ) {
              Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Chat")
            }
          }
          IconButton(onClick = { onNavigateTo(Screen.SETTINGS) }, modifier = Modifier.testTag("home_settings_btn")) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
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
      // 1. Network Status Bar (2G/3G Slow network warning, Data Saver, Offline status)
      NetworkStatusBar(
        networkQuality = networkQuality,
        isDataSaverActive = dataSaverPrefs.isDataSaverEnabled,
        onRetry = { viewModel.refreshFeed() },
        onQualityClick = { onNavigateTo(Screen.SETTINGS) }
      )

      // 2. Active Upload Progress Banner (Floating progress with percentage)
      UploadProgressBanner(
        uploadTask = activeUploadTask,
        onCancel = { viewModel.cancelUpload() }
      )

      // 3. Lazy Feed with Pagination & Skeletons
      if (isInitialLoading) {
        Column(modifier = Modifier.fillMaxSize()) {
          StoriesTraySkeleton()
          FeedCardSkeleton()
          FeedCardSkeleton()
        }
      } else {
        LazyColumn(
          state = listState,
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(bottom = 80.dp)
        ) {
          // A. Stories Tray
          item(key = "stories_tray") {
            StoriesTray(
              stories = stories,
              onStoryClick = { story -> activeStoryToView = story },
              onAddStory = { onNavigateTo(Screen.CREATE_STORY) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
          }

          // B. LIVE Broadcast Banner
          item(key = "live_banner") {
            LiveBroadcastBanner(
              activeLiveRoom = activeLiveRoom,
              onStartLive = { viewModel.startLiveStream("🔴 Cyber Live Stream on VEXORA") },
              onJoinLive = { onNavigateTo(Screen.LIVE_STREAM) }
            )
          }

          // C. Feed Posts with stable keys and Sponsored AdMob card
          itemsIndexed(feedPosts, key = { _, post -> post.id }) { index, post ->
            PostCard(
              post = post,
              onLike = { viewModel.togglePostLike(post.id) },
              onComment = { selectedPostIdForComments = post.id },
              onSave = { viewModel.togglePostSave(post.id) },
              onFollowToggle = { viewModel.toggleFollowUser(post.author.id) },
              onReport = { postToReport = post },
              onShare = { shareToastPost = post }
            )

            // Sponsored AdMob native feed card after post 2 (non-premium users only)
            if (index == 1 && com.example.zevora.monetization.AdMobManager.isAdEligible(premiumSub.isPremium)) {
              com.example.zevora.monetization.AdMobFeedBanner(isPremium = premiumSub.isPremium)
            }
          }

          // D. Pagination Footer
          item(key = "pagination_footer") {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                "✨ You are all caught up on VEXORA!",
                color = MaterialTheme.colorScheme.outline,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }
      }
    }
  }

  // Story Viewer Modal
  activeStoryToView?.let { story ->
    StoryViewerModal(
      story = story,
      viewModel = viewModel,
      onClose = { activeStoryToView = null }
    )
  }

  // Comments Bottom Sheet
  val selectedPostForComments = selectedPostIdForComments?.let { id ->
    feedPosts.firstOrNull { it.id == id }
  }
  selectedPostForComments?.let { post ->
    ModalBottomSheet(
      onDismissRequest = { selectedPostIdForComments = null }
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
          .padding(bottom = 24.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Comments (${post.comments.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
          IconButton(onClick = { selectedPostIdForComments = null }) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (post.comments.isEmpty()) {
          Text(
            "No comments yet. Start the conversation!",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 20.dp)
          )
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            post.comments.forEach { comment ->
              Row(verticalAlignment = Alignment.Top) {
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.primaryContainer,
                  modifier = Modifier.size(32.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(comment.authorName.take(1), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                  }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(comment.authorName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(comment.timestamp, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                  }
                  Text(comment.text, fontSize = 13.sp)
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
          OutlinedTextField(
            value = newCommentInput,
            onValueChange = { newCommentInput = it },
            placeholder = { Text("Add a comment...") },
            modifier = Modifier.weight(1f).testTag("post_comment_input"),
            singleLine = true,
            shape = RoundedCornerShape(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          IconButton(
            onClick = {
              if (newCommentInput.isNotBlank()) {
                viewModel.addComment(post.id, newCommentInput)
                newCommentInput = ""
              }
            },
            modifier = Modifier.testTag("post_comment_send_btn")
          ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
          }
        }
      }
    }
  }

  // Report Modal
  postToReport?.let { post ->
    ReportModal(
      title = "Report Post (पोस्ट की शिकायत करें)",
      targetName = "@${post.author.username}",
      onDismiss = { postToReport = null },
      onSubmit = { reason, notes ->
        viewModel.reportPost(post.id, reason, notes)
        postToReport = null
      }
    )
  }

  // Share Dialog
  shareToastPost?.let { post ->
    AlertDialog(
      onDismissRequest = { shareToastPost = null },
      title = { Text("Share Post") },
      text = { Text("Link copied: https://vexora.app/p/${post.id}\nShare with friends on WhatsApp, Telegram, or Messages.") },
      confirmButton = {
        Button(onClick = { shareToastPost = null }) {
          Text("Copy Link")
        }
      }
    )
  }
}

@Composable
fun LiveBroadcastBanner(
  activeLiveRoom: com.example.zevora.model.LiveRoom?,
  onStartLive: () -> Unit,
  onJoinLive: () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 14.dp, vertical = 8.dp)
      .clip(RoundedCornerShape(16.dp))
      .clickable {
        if (activeLiveRoom != null) onJoinLive() else onStartLive()
      }
      .testTag("live_broadcast_banner"),
    color = Color(0xFF1E1B4B),
    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.horizontalGradient(
            listOf(Color(0xFF3B0764), Color(0xFF1E1B4B), Color(0xFF0F172A))
          )
        )
        .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(38.dp)
          .background(Color(0xFFDC2626), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Default.LiveTv, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text("🔴 LIVE STREAMING", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Black)
          Spacer(modifier = Modifier.width(6.dp))
          Text("• Co-Host & PiP", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Text(
          if (activeLiveRoom != null) "लाइव चालू है: ${activeLiveRoom.title}" else "अपना Live ब्रॉडकास्ट शुरू करें या दोस्तों के साथ जुड़ें",
          color = Color.White,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1
        )
      }

      Button(
        onClick = { if (activeLiveRoom != null) onJoinLive() else onStartLive() },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp)
      ) {
        Text(if (activeLiveRoom != null) "देखें" else "Go Live", fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun StoriesTray(
  stories: List<Story>,
  onStoryClick: (Story) -> Unit,
  onAddStory: () -> Unit
) {
  LazyRow(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 10.dp),
    contentPadding = PaddingValues(horizontal = 14.dp),
    horizontalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    items(stories, key = { it.id }) { story ->
      val isMyStory = story.id == "s1" || story.user.id == "u_me"
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .width(68.dp)
          .clickable {
            if (isMyStory && story.items.isEmpty()) onAddStory() else onStoryClick(story)
          }
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(62.dp)
            .border(
              width = if (story.hasUnseen) 2.5.dp else 1.dp,
              brush = if (story.hasUnseen) Brush.linearGradient(listOf(ZevoraTertiary, ZevoraPrimary, ZevoraSecondary))
              else Brush.linearGradient(listOf(Color.Gray, Color.LightGray)),
              shape = CircleShape
            )
            .padding(3.dp)
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxSize()
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                story.user.name.take(1),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }
          }

          if (isMyStory) {
            Box(
              modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(18.dp)
                .background(Color(0xFF06B6D4), CircleShape)
                .border(1.5.dp, Color.Black, CircleShape)
                .clickable { onAddStory() },
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Add, contentDescription = "Add Story", tint = Color.White, modifier = Modifier.size(12.dp))
            }
          }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = if (isMyStory) "Your Story" else story.user.name.split(" ").first(),
          fontSize = 11.sp,
          maxLines = 1,
          fontWeight = FontWeight.Medium
        )
      }
    }
  }
}

@Composable
private fun PostCard(
  post: Post,
  onLike: () -> Unit,
  onComment: () -> Unit,
  onSave: () -> Unit,
  onFollowToggle: () -> Unit,
  onReport: () -> Unit,
  onShare: () -> Unit
) {
  var showMenu by remember { mutableStateOf(false) }
  var showPopHeart by remember { mutableStateOf(false) }
  val heartScale = remember { Animatable(0f) }
  val coroutineScope = rememberCoroutineScope()

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp),
    shape = RoundedCornerShape(0.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column {
      // Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(post.author.name.take(1), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(post.author.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
              if (post.author.isVerified) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = ZevoraSecondary, modifier = Modifier.size(14.dp))
              }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("@${post.author.username} • ${post.timestamp}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
              if (!post.location.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text("📍 ${post.location}", fontSize = 10.sp, color = Color(0xFF06B6D4), maxLines = 1)
              }
            }
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (!post.isFollowing) {
            Button(
              onClick = onFollowToggle,
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.height(30.dp)
            ) {
              Text("Follow", fontSize = 11.sp)
            }
          }

          Box {
            IconButton(onClick = { showMenu = true }) {
              Icon(Icons.Default.MoreVert, contentDescription = "Options")
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
              DropdownMenuItem(
                text = { Text("Report Post (रिपोर्ट)") },
                onClick = {
                  showMenu = false
                  onReport()
                },
                leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) }
              )
              DropdownMenuItem(
                text = { Text(if (post.isFollowing) "Unfollow" else "Follow") },
                onClick = {
                  showMenu = false
                  onFollowToggle()
                },
                leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) }
              )
            }
          }
        }
      }

      // Visual Content Media Card with Double-Tap to Like
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(260.dp)
          .background(
            Brush.linearGradient(
              listOf(Color(post.gradientStart), Color(post.gradientEnd))
            )
          )
          .pointerInput(post.id) {
            detectTapGestures(
              onDoubleTap = {
                if (!post.isLiked) {
                  onLike()
                }
                coroutineScope.launch {
                  showPopHeart = true
                  heartScale.snapTo(0f)
                  heartScale.animateTo(1.2f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                  delay(400)
                  heartScale.animateTo(0f, tween(180))
                  showPopHeart = false
                }
              }
            )
          }
      ) {
        if (post.mediaUrls.isNotEmpty()) {
          AsyncImage(
            model = post.mediaUrls.first(),
            contentDescription = "Post Media",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
          if (post.mediaUrls.size > 1) {
            Surface(
              color = Color.Black.copy(alpha = 0.7f),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.Collections, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("1/${post.mediaUrls.size}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        } else {
          Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(Icons.Default.Image, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(44.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "VEXORA Visuals",
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp
            )
          }
        }

        // Double tap heart pop
        if (showPopHeart) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              Icons.Default.Favorite,
              contentDescription = null,
              tint = Color(0xFFEF4444).copy(alpha = 0.95f),
              modifier = Modifier
                .size(90.dp)
                .scale(heartScale.value)
            )
          }
        }
      }

      // Action Bar (Like, Comment, Share, Save)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onLike, modifier = Modifier.testTag("like_post_${post.id}")) {
            Icon(
              imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
              contentDescription = "Like",
              tint = if (post.isLiked) Color.Red else MaterialTheme.colorScheme.onSurface
            )
          }
          Text("${post.likesCount}", fontWeight = FontWeight.Bold, fontSize = 13.sp)

          Spacer(modifier = Modifier.width(8.dp))

          IconButton(onClick = onComment, modifier = Modifier.testTag("comment_post_${post.id}")) {
            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comment")
          }
          Text("${post.commentsCount}", fontWeight = FontWeight.Bold, fontSize = 13.sp)

          Spacer(modifier = Modifier.width(8.dp))

          IconButton(onClick = onShare, modifier = Modifier.testTag("share_post_${post.id}")) {
            Icon(Icons.Outlined.Share, contentDescription = "Share")
          }
        }

        IconButton(onClick = onSave, modifier = Modifier.testTag("save_post_${post.id}")) {
          Icon(
            imageVector = if (post.isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = "Bookmark",
            tint = if (post.isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
          )
        }
      }

      // Caption & Hashtags
      Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
        Text(
          text = post.content,
          fontSize = 13.sp,
          lineHeight = 18.sp
        )
        if (post.hashtags.isNotEmpty()) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = post.hashtags.joinToString(" "),
            color = Color(0xFF38BDF8),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    }
  }
}
