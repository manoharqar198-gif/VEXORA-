package com.example.zevora.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zevora.model.NetworkQuality
import com.example.zevora.model.UploadTask

@Composable
fun shimmerBrush(targetValue: Float = 1000f): Brush {
  val shimmerColors = listOf(
    Color(0xFF1E293B).copy(alpha = 0.6f),
    Color(0xFF334155).copy(alpha = 0.3f),
    Color(0xFF1E293B).copy(alpha = 0.6f)
  )

  val transition = rememberInfiniteTransition(label = "shimmer_transition")
  val translateAnimation by transition.animateFloat(
    initialValue = 0f,
    targetValue = targetValue,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1100, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "shimmer_translate"
  )

  return Brush.linearGradient(
    colors = shimmerColors,
    start = Offset.Zero,
    end = Offset(x = translateAnimation, y = translateAnimation)
  )
}

@Composable
fun FeedCardSkeleton() {
  val brush = shimmerBrush()
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp)
      .testTag("feed_card_skeleton"),
    shape = RoundedCornerShape(0.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      // Header skeleton
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(brush)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Box(
            modifier = Modifier
              .width(120.dp)
              .height(14.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(brush)
          )
          Spacer(modifier = Modifier.height(6.dp))
          Box(
            modifier = Modifier
              .width(80.dp)
              .height(10.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(brush)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Media Box skeleton
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(240.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(brush)
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Action bar skeleton
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(brush))
          Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(brush))
          Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(brush))
        }
        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(brush))
      }

      Spacer(modifier = Modifier.height(10.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth(0.7f)
          .height(12.dp)
          .clip(RoundedCornerShape(4.dp))
          .background(brush)
      )
    }
  }
}

@Composable
fun StoriesTraySkeleton() {
  val brush = shimmerBrush()
  LazyRow(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 10.dp),
    contentPadding = PaddingValues(horizontal = 14.dp),
    horizontalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    items(5) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
          modifier = Modifier
            .size(62.dp)
            .clip(CircleShape)
            .background(brush)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
          modifier = Modifier
            .width(44.dp)
            .height(10.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(brush)
        )
      }
    }
  }
}

@Composable
fun NetworkStatusBar(
  networkQuality: NetworkQuality,
  isDataSaverActive: Boolean,
  onRetry: () -> Unit,
  onQualityClick: () -> Unit
) {
  val isProblematic = networkQuality == NetworkQuality.OFFLINE || networkQuality == NetworkQuality.SLOW

  AnimatedVisibility(visible = isProblematic || isDataSaverActive) {
    Surface(
      color = when (networkQuality) {
        NetworkQuality.OFFLINE -> Color(0xFFDC2626) // Red
        NetworkQuality.SLOW -> Color(0xFFD97706) // Amber
        else -> Color(0xFF0284C7) // Blue Data Saver
      },
      modifier = Modifier
        .fillMaxWidth()
        .clickable { onQualityClick() }
        .testTag("network_status_banner")
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = when (networkQuality) {
            NetworkQuality.OFFLINE -> Icons.Default.WifiOff
            NetworkQuality.SLOW -> Icons.Default.NetworkCheck
            else -> Icons.Default.DataSaverOn
          },
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = when (networkQuality) {
            NetworkQuality.OFFLINE -> "⚠️ ऑफ़लाइन मोड • कैश्ड कंटेंट दिखाया जा रहा है"
            NetworkQuality.SLOW -> "⚡ कमजोर नेटवर्क (2G/3G) • Data Saver सक्रिय (480p)"
            else -> "⚡ Data Saver सक्रिय • कम इंटरनेट उपयोग"
          },
          color = Color.White,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f)
        )
        if (networkQuality == NetworkQuality.OFFLINE) {
          TextButton(
            onClick = onRetry,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
          ) {
            Text("पुनः प्रयास", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun UploadProgressBanner(
  uploadTask: UploadTask?,
  onCancel: () -> Unit
) {
  if (uploadTask == null || uploadTask.isCompleted) return

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 14.dp, vertical = 6.dp)
      .clip(RoundedCornerShape(12.dp))
      .testTag("upload_progress_banner"),
    color = Color(0xFF1E293B),
    tonalElevation = 6.dp,
    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6))
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          Icons.Default.CloudUpload,
          contentDescription = null,
          tint = Color(0xFF38BDF8),
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "${uploadTask.title} (${(uploadTask.progress * 100).toInt()}%)",
          color = Color.White,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f)
        )
        IconButton(
          onClick = onCancel,
          modifier = Modifier.size(24.dp)
        ) {
          Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
      LinearProgressIndicator(
        progress = { uploadTask.progress },
        modifier = Modifier
          .fillMaxWidth()
          .height(4.dp)
          .clip(RoundedCornerShape(2.dp)),
        color = Color(0xFF8B5CF6),
        trackColor = Color(0xFF334155)
      )
    }
  }
}
