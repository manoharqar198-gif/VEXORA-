package com.example.zevora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.zevora.ui.Screen
import com.example.zevora.ui.ZevoraViewModel
import com.example.zevora.ui.theme.ZevoraPrimary
import com.example.zevora.ui.theme.ZevoraSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
  viewModel: ZevoraViewModel,
  onNavigateTo: (Screen) -> Unit,
  onNavigateBack: () -> Unit
) {
  val currentUser by viewModel.currentUser.collectAsState()
  val isPrivate by viewModel.isPrivateAccount.collectAsState()
  val feedPosts by viewModel.feedPosts.collectAsState()
  val reels by viewModel.reels.collectAsState()
  var selectedTab by remember { mutableIntStateOf(0) } // 0: Posts, 1: Reels, 2: Saved
  var showEditProfileDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("@${currentUser.username}", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = { onNavigateTo(Screen.SECURITY_PRIVACY) }) {
            Icon(Icons.Default.Security, contentDescription = "Security")
          }
          IconButton(onClick = { onNavigateTo(Screen.SETTINGS) }) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
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
      // Cover banner + Profile avatar overlay
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
        ) {
          // Cover Banner
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(130.dp)
              .background(
                Brush.horizontalGradient(
                  listOf(Color(0xFF6366F1), Color(0xFF06B6D4), Color(0xFFEC4899))
                )
              )
          )

          // Avatar
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
              .size(90.dp)
              .align(Alignment.BottomStart)
              .padding(start = 16.dp)
              .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
          ) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center
            ) {
              if (currentUser.avatarUrl.isNotBlank()) {
                AsyncImage(
                  model = currentUser.avatarUrl,
                  contentDescription = "Profile Photo",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize()
                )
              } else {
                Text(
                  currentUser.name.take(1).ifBlank { "V" },
                  fontWeight = FontWeight.Black,
                  fontSize = 32.sp,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }
          }

          // Private / Public Badge
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isPrivate) Color(0xFFF59E0B) else ZevoraSecondary,
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(end = 16.dp, bottom = 10.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = if (isPrivate) Icons.Default.Lock else Icons.Default.Public,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (isPrivate) "Private Account" else "Public Account",
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      // Name & Bio
      item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(currentUser.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            if (currentUser.isVerified) {
              Spacer(modifier = Modifier.width(6.dp))
              Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = ZevoraSecondary, modifier = Modifier.size(18.dp))
            }
          }
          Text("@${currentUser.username}", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)

          Spacer(modifier = Modifier.height(8.dp))
          Text(currentUser.bio, fontSize = 13.sp, lineHeight = 18.sp)

          Spacer(modifier = Modifier.height(14.dp))

          // Stats: Posts, Followers, Following
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
          ) {
            ProfileStatColumn("${currentUser.postsCount}", "Posts")
            ProfileStatColumn("${currentUser.followersCount}", "Followers")
            ProfileStatColumn("${currentUser.followingCount}", "Following")
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Edit profile & Creator Wallet buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = { showEditProfileDialog = true },
              modifier = Modifier.weight(1f).testTag("edit_profile_btn"),
              shape = RoundedCornerShape(10.dp)
            ) {
              Text("Edit Profile")
            }
            OutlinedButton(
              onClick = { onNavigateTo(Screen.CREATOR_WALLET) },
              modifier = Modifier.weight(1f).testTag("profile_wallet_btn"),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Wallet (₹)")
            }
            IconButton(
              onClick = { onNavigateTo(Screen.PREMIUM_UPGRADE) },
              modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF2E1065), RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFFFBBF24), RoundedCornerShape(10.dp))
            ) {
              Icon(Icons.Default.WorkspacePremium, contentDescription = "VIP Pro", tint = Color(0xFFFBBF24))
            }
          }
        }
      }

      // Story Highlights Row (🌟 स्टोरी हाइलाइट्स)
      item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
          Text("Story Highlights 🌟", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Spacer(modifier = Modifier.height(6.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            listOf("Travel ✈️", "Studio 🎧", "Tech 🚀", "Life ✨").forEach { highlightTitle ->
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                  modifier = Modifier
                    .size(54.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(3.dp)
                    .background(Color(0xFF1E293B), CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(highlightTitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
              }
            }
          }
        }
      }

      // Tabs: Posts, Reels, Saved
      item {
        TabRow(selectedTabIndex = selectedTab) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            modifier = Modifier.testTag("profile_tab_posts"),
            icon = { Icon(Icons.Default.GridOn, contentDescription = "Posts") }
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            modifier = Modifier.testTag("profile_tab_reels"),
            icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "Reels") }
          )
          Tab(
            selected = selectedTab == 2,
            onClick = { selectedTab = 2 },
            modifier = Modifier.testTag("profile_tab_saved"),
            icon = { Icon(Icons.Default.BookmarkBorder, contentDescription = "Saved") }
          )
        }
      }

      // Live Grid for Selected Tab
      item {
        when (selectedTab) {
          0 -> {
            if (feedPosts.isEmpty()) {
              ProfileEmptyTabState(title = "कोई पोस्ट नहीं", subtitle = "होम फ़ीड पर अपनी पहली पोस्ट साझा करें।")
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(4.dp)) {
                feedPosts.chunked(3).forEach { rowPosts ->
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    rowPosts.forEach { post ->
                      Box(
                        modifier = Modifier
                          .weight(1f)
                          .height(120.dp)
                          .clip(RoundedCornerShape(6.dp))
                          .background(Brush.linearGradient(listOf(Color(post.gradientStart), Color(post.gradientEnd))))
                          .clickable { onNavigateTo(Screen.HOME) }
                          .padding(6.dp)
                      ) {
                        if (post.mediaUrls.isNotEmpty()) {
                          AsyncImage(
                            model = post.mediaUrls.first(),
                            contentDescription = "Post Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                          )
                        } else {
                          Text(
                            text = post.content,
                            color = Color.White,
                            fontSize = 10.sp,
                            maxLines = 4,
                            lineHeight = 13.sp,
                            modifier = Modifier.align(Alignment.Center)
                          )
                        }
                        Surface(
                          color = Color.Black.copy(alpha = 0.55f),
                          shape = RoundedCornerShape(6.dp),
                          modifier = Modifier.align(Alignment.BottomEnd)
                        ) {
                          Text(
                            text = "♥ ${post.likesCount}",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                          )
                        }
                      }
                    }
                    repeat(3 - rowPosts.size) {
                      Spacer(modifier = Modifier.weight(1f))
                    }
                  }
                }
              }
            }
          }

          1 -> {
            if (reels.isEmpty()) {
              ProfileEmptyTabState(title = "कोई रील्स नहीं", subtitle = "अपनी पहली VEXORA Reel बनाएं।")
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(4.dp)) {
                reels.chunked(3).forEach { rowReels ->
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    rowReels.forEach { reel ->
                      Box(
                        modifier = Modifier
                          .weight(1f)
                          .height(145.dp)
                          .clip(RoundedCornerShape(6.dp))
                          .background(Brush.verticalGradient(listOf(Color(reel.gradientStart), Color(reel.gradientEnd))))
                          .clickable { onNavigateTo(Screen.REELS) }
                          .padding(6.dp)
                      ) {
                        Icon(
                          Icons.Default.PlayArrow,
                          contentDescription = "Reel",
                          tint = Color.White.copy(alpha = 0.9f),
                          modifier = Modifier.align(Alignment.Center).size(28.dp)
                        )
                        Surface(
                          color = Color.Black.copy(alpha = 0.55f),
                          shape = RoundedCornerShape(6.dp),
                          modifier = Modifier.align(Alignment.BottomStart)
                        ) {
                          Text(
                            text = "▶ ${reel.viewsCount}",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                          )
                        }
                      }
                    }
                    repeat(3 - rowReels.size) {
                      Spacer(modifier = Modifier.weight(1f))
                    }
                  }
                }
              }
            }
          }

          else -> {
            val savedPosts = feedPosts.filter { it.isSaved }
            if (savedPosts.isEmpty()) {
              ProfileEmptyTabState(title = "कोई सेव की गई पोस्ट नहीं", subtitle = "किसी पोस्ट पर Bookmark आइकन दबाकर यहाँ सेव करें।")
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(4.dp)) {
                savedPosts.chunked(3).forEach { rowPosts ->
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    rowPosts.forEach { post ->
                      Box(
                        modifier = Modifier
                          .weight(1f)
                          .height(120.dp)
                          .clip(RoundedCornerShape(6.dp))
                          .background(Brush.linearGradient(listOf(Color(post.gradientStart), Color(post.gradientEnd))))
                          .clickable { onNavigateTo(Screen.HOME) }
                          .padding(6.dp)
                      ) {
                        Text(
                          text = post.content,
                          color = Color.White,
                          fontSize = 10.sp,
                          maxLines = 4,
                          lineHeight = 13.sp,
                          modifier = Modifier.align(Alignment.Center)
                        )
                        Icon(
                          Icons.Default.Bookmark,
                          contentDescription = "Saved",
                          tint = Color.White,
                          modifier = Modifier.align(Alignment.TopEnd).size(14.dp)
                        )
                      }
                    }
                    repeat(3 - rowPosts.size) {
                      Spacer(modifier = Modifier.weight(1f))
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Edit Profile Dialog
  if (showEditProfileDialog) {
    var editName by remember { mutableStateOf(currentUser.name) }
    var editBio by remember { mutableStateOf(currentUser.bio) }

    AlertDialog(
      onDismissRequest = { showEditProfileDialog = false },
      title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          OutlinedTextField(
            value = editName,
            onValueChange = { editName = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = editBio,
            onValueChange = { editBio = it },
            label = { Text("Bio") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
          )
        }
      },
      confirmButton = {
        Button(onClick = {
          viewModel.updateCurrentProfile(editName, editBio)
          showEditProfileDialog = false
        }) { Text("Save") }
      },
      dismissButton = {
        TextButton(onClick = { showEditProfileDialog = false }) { Text("Cancel") }
      }
    )
  }
}

@Composable
private fun ProfileStatColumn(count: String, label: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(count, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
  }
}

@Composable
private fun ProfileEmptyTabState(title: String, subtitle: String) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp, vertical = 32.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
      Spacer(modifier = Modifier.height(4.dp))
      Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
    }
  }
}
