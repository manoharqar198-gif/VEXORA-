package com.example.zevora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zevora.model.AppLanguage
import com.example.zevora.ui.Screen
import com.example.zevora.util.ZevoraStrings

@Composable
fun ZevoraBottomNav(
  currentScreen: Screen,
  onScreenSelected: (Screen) -> Unit,
  onCreateClick: () -> Unit,
  language: AppLanguage
) {
  NavigationBar(
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 6.dp
  ) {
    // 1. Home
    NavigationBarItem(
      selected = currentScreen == Screen.HOME,
      onClick = { onScreenSelected(Screen.HOME) },
      icon = {
        Icon(
          imageVector = if (currentScreen == Screen.HOME) Icons.Default.Home else Icons.Outlined.Home,
          contentDescription = "Home"
        )
      },
      label = { Text(ZevoraStrings.get("nav_home", language), fontSize = 10.sp, maxLines = 1) },
      modifier = Modifier.testTag("nav_item_home")
    )

    // 2. Reels (REELS बटन)
    NavigationBarItem(
      selected = currentScreen == Screen.REELS,
      onClick = { onScreenSelected(Screen.REELS) },
      icon = {
        Icon(
          imageVector = if (currentScreen == Screen.REELS) Icons.Default.VideoLibrary else Icons.Outlined.VideoLibrary,
          contentDescription = "Reels"
        )
      },
      label = { Text(ZevoraStrings.get("nav_reels", language), fontSize = 10.sp, maxLines = 1) },
      modifier = Modifier.testTag("nav_item_reels")
    )

    // 3. Center (+) Create Button
    NavigationBarItem(
      selected = false,
      onClick = onCreateClick,
      icon = {
        Box(
          modifier = Modifier
            .size(42.dp)
            .shadow(6.dp, CircleShape)
            .background(
              Brush.linearGradient(
                listOf(Color(0xFF8B5CF6), Color(0xFF06B6D4), Color(0xFFEC4899))
              ),
              CircleShape
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            Icons.Default.Add,
            contentDescription = "Create Content",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
          )
        }
      },
      label = { Text("बनाएं", fontSize = 10.sp, maxLines = 1, color = MaterialTheme.colorScheme.primary) },
      modifier = Modifier.testTag("nav_item_create_center")
    )

    // 4. Watch / Videos
    NavigationBarItem(
      selected = currentScreen == Screen.WATCH,
      onClick = { onScreenSelected(Screen.WATCH) },
      icon = {
        Icon(
          imageVector = if (currentScreen == Screen.WATCH) Icons.Default.PlayCircle else Icons.Outlined.PlayCircle,
          contentDescription = "Watch"
        )
      },
      label = { Text(ZevoraStrings.get("nav_watch", language), fontSize = 10.sp, maxLines = 1) },
      modifier = Modifier.testTag("nav_item_watch")
    )

    // 5. Security & Settings
    NavigationBarItem(
      selected = currentScreen == Screen.SECURITY_PRIVACY,
      onClick = { onScreenSelected(Screen.SECURITY_PRIVACY) },
      icon = {
        Icon(
          imageVector = if (currentScreen == Screen.SECURITY_PRIVACY) Icons.Default.Shield else Icons.Outlined.Shield,
          contentDescription = "Security"
        )
      },
      label = { Text("सुरक्षा", fontSize = 10.sp, maxLines = 1) },
      modifier = Modifier.testTag("nav_item_security")
    )
  }
}
