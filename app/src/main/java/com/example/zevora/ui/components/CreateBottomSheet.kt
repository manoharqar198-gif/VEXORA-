package com.example.zevora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBottomSheet(
  onDismiss: () -> Unit,
  onGoLive: () -> Unit,
  onCreateReel: () -> Unit,
  onCreatePost: () -> Unit,
  onAddStory: () -> Unit
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF0F172A),
    dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF64748B)) }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .padding(bottom = 32.dp)
    ) {
      Text(
        text = "कंटेंट बनाएं • Create on VEXORA",
        fontSize = 20.sp,
        fontWeight = FontWeight.Black,
        color = Color.White,
        modifier = Modifier.padding(bottom = 6.dp)
      )
      Text(
        text = "लाइव वीडियो, रील्स, पोस्ट या 24 घंटे की स्टोरी शेयर करें",
        fontSize = 13.sp,
        color = Color(0xFF94A3B8),
        modifier = Modifier.padding(bottom = 20.dp)
      )

      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. LIVE Button
        CreateOptionRow(
          icon = Icons.Default.LiveTv,
          title = "🔴 Go Live • लाइव ब्रॉडकास्ट",
          subtitle = "दर्शकों और Co-Host Guests के साथ लाइव वीडियो शुरू करें",
          gradient = Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFFDC2626))),
          tag = "create_option_live",
          onClick = {
            onDismiss()
            onGoLive()
          }
        )

        // 2. REELS Button
        CreateOptionRow(
          icon = Icons.Default.VideoLibrary,
          title = "🎬 Create Reel • रील्स बनाएं",
          subtitle = "15s/30s/60s वर्टिकल वीडियो, म्यूजिक और इफेक्ट्स",
          gradient = Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))),
          tag = "create_option_reel",
          onClick = {
            onDismiss()
            onCreateReel()
          }
        )

        // 3. POST Button
        CreateOptionRow(
          icon = Icons.Default.AddPhotoAlternate,
          title = "📸 Create Post • नई पोस्ट",
          subtitle = "कई फोटो और वीडियो, कैप्शन और लोकेशन टैग करें",
          gradient = Brush.horizontalGradient(listOf(Color(0xFF06B6D4), Color(0xFF3B82F6))),
          tag = "create_option_post",
          onClick = {
            onDismiss()
            onCreatePost()
          }
        )

        // 4. STORY Button
        CreateOptionRow(
          icon = Icons.Default.AutoAwesome,
          title = "🌟 Add Story • 24 घंटे की स्टोरी",
          subtitle = "फोटो/वीडियो स्टोरी लगाएं जो 24 घंटे में गायब होगी",
          gradient = Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFEA580C))),
          tag = "create_option_story",
          onClick = {
            onDismiss()
            onAddStory()
          }
        )
      }
    }
  }
}

@Composable
private fun CreateOptionRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  gradient: Brush,
  tag: String,
  onClick: () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable { onClick() }
      .testTag(tag),
    color = Color(0xFF1E293B),
    tonalElevation = 2.dp,
    shape = RoundedCornerShape(16.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(48.dp)
          .background(gradient, shape = CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = Color.White,
          modifier = Modifier.size(24.dp)
        )
      }

      Spacer(modifier = Modifier.width(16.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          fontSize = 12.sp,
          color = Color(0xFF94A3B8),
          lineHeight = 16.sp
        )
      }
    }
  }
}
