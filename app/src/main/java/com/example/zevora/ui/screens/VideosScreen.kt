package com.example.zevora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zevora.model.VideoItem
import com.example.zevora.ui.Screen
import com.example.zevora.ui.ZevoraViewModel
import com.example.zevora.ui.theme.ZevoraPrimary
import com.example.zevora.ui.theme.ZevoraSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(
  viewModel: ZevoraViewModel,
  onNavigateTo: (Screen) -> Unit
) {
  val videos by viewModel.videos.collectAsState()
  var selectedCategory by remember { mutableStateOf("All") }
  var isSearchVisible by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  val categories = listOf("All", "Tech", "Podcasts", "Creator", "Travel")

  val filteredVideos = remember(videos, selectedCategory, searchQuery) {
    videos.filter { video ->
      val matchesCategory = when (selectedCategory) {
        "All" -> true
        "Tech" -> video.title.contains("Privacy", ignoreCase = true) ||
          video.title.contains("Tech", ignoreCase = true) ||
          video.channelName.contains("Tech", ignoreCase = true)
        "Podcasts" -> video.title.contains("Talks", ignoreCase = true) ||
          video.channelName.contains("Talks", ignoreCase = true) ||
          video.title.contains("Creator", ignoreCase = true)
        "Creator" -> video.title.contains("Creator", ignoreCase = true) ||
          video.channelName.contains("Creator", ignoreCase = true) ||
          video.title.contains("Live", ignoreCase = true)
        "Travel" -> video.title.contains("Temples", ignoreCase = true) ||
          video.title.contains("India", ignoreCase = true) ||
          video.channelName.contains("Explorer", ignoreCase = true)
        else -> true
      }
      val q = searchQuery.trim()
      val matchesQuery = q.isEmpty() ||
        video.title.contains(q, ignoreCase = true) ||
        video.channelName.contains(q, ignoreCase = true)
      matchesCategory && matchesQuery
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("VEXORA Watch", fontWeight = FontWeight.Bold, fontSize = 18.sp)
          }
        },
        actions = {
          IconButton(onClick = { onNavigateTo(Screen.CREATOR_STUDIO) }, modifier = Modifier.testTag("open_creator_studio_btn")) {
            Icon(Icons.Default.MonetizationOn, contentDescription = "Creator Studio", tint = MaterialTheme.colorScheme.secondary)
          }
          IconButton(
            onClick = {
              isSearchVisible = !isSearchVisible
              if (!isSearchVisible) searchQuery = ""
            },
            modifier = Modifier.testTag("videos_search_btn")
          ) {
            Icon(
              imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
              contentDescription = "Search Videos"
            )
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
      if (isSearchVisible) {
        item {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("वीडियो या चैनल खोजें... Search videos") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 8.dp)
              .testTag("videos_search_input")
          )
        }
      }

      // Creator Studio Banner
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onNavigateTo(Screen.CREATOR_STUDIO) }
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(44.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AutoGraph, contentDescription = null, tint = Color.White)
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text("Creator Studio & कमाई का सिस्टम", fontWeight = FontWeight.Bold, fontSize = 14.sp)
              Text("Check video ad revenue, Gifts & Channel Subscriptions", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
          }
        }
      }

      // Filter chips
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          categories.forEach { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { selectedCategory = cat },
              label = { Text(cat, fontSize = 12.sp) }
            )
          }
        }
      }

      // Video Items
      if (filteredVideos.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text("इस श्रेणी में कोई वीडियो नहीं मिला", fontWeight = FontWeight.Bold, fontSize = 14.sp)
              Spacer(modifier = Modifier.height(8.dp))
              OutlinedButton(onClick = {
                selectedCategory = "All"
                searchQuery = ""
              }) {
                Text("सभी वीडियो दिखाएं")
              }
            }
          }
        }
      } else {
        items(filteredVideos, key = { it.id }) { video ->
          VideoCard(
            video = video,
            onSubscribeToggle = { viewModel.toggleVideoSubscribe(video.id) }
          )
        }
      }
    }
  }
}

@Composable
private fun VideoCard(
  video: VideoItem,
  onSubscribeToggle: () -> Unit
) {
  var isPlaying by remember { mutableStateOf(false) }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp),
    shape = RoundedCornerShape(0.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column {
      // Thumbnail / Player Canvas
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(200.dp)
          .background(
            Brush.linearGradient(
              listOf(Color(video.gradientStart), Color(video.gradientEnd))
            )
          )
          .clickable { isPlaying = !isPlaying }
      ) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.6f),
            modifier = Modifier.size(54.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play/Pause",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
              )
            }
          }
        }

        // Duration Badge
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = Color.Black.copy(alpha = 0.8f),
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(8.dp)
        ) {
          Text(
            text = video.duration,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      // Meta details
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalAlignment = Alignment.Top
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(40.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(video.channelName.take(1), fontWeight = FontWeight.Bold)
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = video.title,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 2
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "${video.channelName} • ${video.views} • ${video.timeAgo}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.outline
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
          onClick = onSubscribeToggle,
          colors = ButtonDefaults.buttonColors(
            containerColor = if (video.isSubscribed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary
          ),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          modifier = Modifier.height(32.dp)
        ) {
          Text(
            text = if (video.isSubscribed) "Subscribed" else "Subscribe",
            fontSize = 11.sp,
            color = if (video.isSubscribed) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
          )
        }
      }
    }
  }
}
