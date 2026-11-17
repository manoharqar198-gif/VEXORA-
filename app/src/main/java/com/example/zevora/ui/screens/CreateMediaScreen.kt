package com.example.zevora.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.zevora.ui.ZevoraViewModel
import com.example.zevora.ui.components.CameraPreviewView

enum class CreateMode(val label: String, val icon: String) {
  POST("पोस्ट (Post)", "📸"),
  REEL("रील्स (Reel)", "🎬"),
  STORY("स्टोरी (Story)", "🌟")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateMediaScreen(
  viewModel: ZevoraViewModel,
  initialMode: CreateMode = CreateMode.POST,
  onClose: () -> Unit
) {
  var currentMode by remember { mutableStateOf(initialMode) }
  var isCameraActive by remember { mutableStateOf(false) }
  var cameraLensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }

  // Media selections (Photos/Videos)
  var selectedMediaUris by remember {
    mutableStateOf(
      listOf<Uri>()
    )
  }

  // Gallery multi-photo / video picker
  val galleryLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
  ) { uris ->
    if (uris.isNotEmpty()) {
      selectedMediaUris = selectedMediaUris + uris
      isCameraActive = false
    }
  }

  // Form states
  var captionText by remember { mutableStateOf("") }
  var locationText by remember { mutableStateOf("Mumbai, Maharashtra") }
  var selectedHashtags by remember {
    mutableStateOf(listOf("#Vexora", "#Trending"))
  }
  var selectedMusicTrack by remember { mutableStateOf("VEXORA Cyber Beats 2026 • Trending Audio") }

  // Music selector dialog
  var showMusicPicker by remember { mutableStateOf(false) }

  BackHandler {
    onClose()
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = when (currentMode) {
              CreateMode.POST -> "📸 नई पोस्ट बनाएं"
              CreateMode.REEL -> "🎬 रील्स अपलोड करें"
              CreateMode.STORY -> "🌟 24 घंटे की स्टोरी"
            },
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White
          )
        },
        navigationIcon = {
          IconButton(onClick = onClose, modifier = Modifier.testTag("create_media_back_btn")) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
          }
        },
        actions = {
          Button(
            onClick = {
              val uriStrings = selectedMediaUris.map { it.toString() }
              when (currentMode) {
                CreateMode.POST -> {
                  viewModel.createPost(
                    content = captionText.ifBlank { "Sharing a special moment on VEXORA ✨" },
                    mediaUrls = uriStrings,
                    location = locationText,
                    hashtags = selectedHashtags
                  )
                }
                CreateMode.REEL -> {
                  viewModel.createReel(
                    caption = captionText.ifBlank { "Check out my new reel on VEXORA 🔥" },
                    musicTitle = selectedMusicTrack
                  )
                }
                CreateMode.STORY -> {
                  viewModel.createStory(
                    caption = captionText.ifBlank { "My day ✨" },
                    mediaUrl = uriStrings.firstOrNull() ?: ""
                  )
                }
              }
              onClose()
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = when (currentMode) {
                CreateMode.POST -> Color(0xFF06B6D4)
                CreateMode.REEL -> Color(0xFF8B5CF6)
                CreateMode.STORY -> Color(0xFFF59E0B)
              }
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .padding(end = 8.dp)
              .testTag("publish_media_submit_btn")
          ) {
            Text(
              text = when (currentMode) {
                CreateMode.POST -> "पोस्ट करें"
                CreateMode.REEL -> "रील्स शेयर करें"
                CreateMode.STORY -> "स्टोरी लगाएं"
              },
              fontWeight = FontWeight.Bold,
              color = Color.White,
              fontSize = 13.sp
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B0F19))
      )
    },
    containerColor = Color(0xFF0B0F19)
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      // 1. Mode Selector Tabs
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFF1E293B), RoundedCornerShape(14.dp))
          .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        CreateMode.values().forEach { mode ->
          val isSelected = currentMode == mode
          Surface(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .clickable { currentMode = mode }
              .testTag("create_tab_${mode.name.lowercase()}"),
            color = if (isSelected) Color(0xFF8B5CF6) else Color.Transparent,
            shape = RoundedCornerShape(10.dp)
          ) {
            Box(
              modifier = Modifier.padding(vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "${mode.icon} ${mode.label.take(8)}",
                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp,
                maxLines = 1
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 2. Media Viewfinder / Preview Box
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .height(if (currentMode == CreateMode.REEL || currentMode == CreateMode.STORY) 320.dp else 220.dp)
          .clip(RoundedCornerShape(20.dp))
          .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp)),
        color = Color(0xFF1E1B4B)
      ) {
        Box(modifier = Modifier.fillMaxSize()) {
          if (isCameraActive) {
            CameraPreviewView(
              lensFacing = cameraLensFacing,
              modifier = Modifier.fillMaxSize()
            )
            // Camera controls overlay
            Row(
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
            ) {
              IconButton(
                onClick = {
                  cameraLensFacing = if (cameraLensFacing == CameraSelector.LENS_FACING_FRONT) {
                    CameraSelector.LENS_FACING_BACK
                  } else {
                    CameraSelector.LENS_FACING_FRONT
                  }
                },
                modifier = Modifier
                  .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                  .size(36.dp)
              ) {
                Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Flip", tint = Color.White, modifier = Modifier.size(18.dp))
              }
              Spacer(modifier = Modifier.width(8.dp))
              IconButton(
                onClick = { isCameraActive = false },
                modifier = Modifier
                  .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                  .size(36.dp)
              ) {
                Icon(Icons.Default.Close, contentDescription = "Close Camera", tint = Color.White, modifier = Modifier.size(18.dp))
              }
            }
          } else if (selectedMediaUris.isNotEmpty()) {
            AsyncImage(
              model = selectedMediaUris.first(),
              contentDescription = "Selected media",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
            Surface(
              color = Color.Black.copy(alpha = 0.6f),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
            ) {
              Text(
                "${selectedMediaUris.size} media selected",
                color = Color.White,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          } else {
            // Placeholder when no media selected yet
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Icon(
                when (currentMode) {
                  CreateMode.POST -> Icons.Default.AddPhotoAlternate
                  CreateMode.REEL -> Icons.Default.VideoLibrary
                  CreateMode.STORY -> Icons.Default.AutoAwesome
                },
                contentDescription = null,
                tint = Color(0xFF8B5CF6),
                modifier = Modifier.size(48.dp)
              )
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                "कैमरा या गैलरी से फोटो/वीडियो चुनें",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
              Text(
                "एक पोस्ट में कई फोटो या रील्स वीडियो जोड़ सकते हैं",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // 3. Capture & Gallery buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedButton(
          onClick = { isCameraActive = true },
          modifier = Modifier
            .weight(1f)
            .testTag("open_live_camera_btn"),
          colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF1E293B)),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6)),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("लाइव कैमरा", color = Color.White, fontSize = 13.sp)
        }

        Button(
          onClick = {
            galleryLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
            )
          },
          modifier = Modifier
            .weight(1f)
            .testTag("open_gallery_picker_btn"),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("गैलरी से चुनें", color = Color.White, fontSize = 13.sp)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("spark_media_pending_banner")
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Icon(Icons.Default.CloudOff, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(20.dp))
          Text(
            text = "Cloud Media Upload Pending (Spark Free Plan • ₹0): फोटो/वीडियो क्लाउड स्टोरेज अभी सुरक्षित रूप से Pending है ताकि कोई Paid/Blaze खर्च न हो। कैप्शन और पोस्ट विवरण मुफ्त Firestore में सेव होंगे।",
            color = Color(0xFFE2E8F0),
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f)
          )
        }
      }

      // 4. Selected Multi-Photo Carousel Strip
      if (selectedMediaUris.size > 1) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          "चयनित मीडिया (${selectedMediaUris.size} फोटो/वीडियो)",
          color = Color(0xFF94A3B8),
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(selectedMediaUris) { uri ->
            Box(
              modifier = Modifier
                .size(68.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF8B5CF6), RoundedCornerShape(12.dp))
            ) {
              AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
              )
              IconButton(
                onClick = { selectedMediaUris = selectedMediaUris - uri },
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .size(20.dp)
                  .background(Color.Black.copy(alpha = 0.7f), CircleShape)
              ) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 5. Caption Input
      Text(
        "कैप्शन और विवरण • Caption",
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
      )
      Spacer(modifier = Modifier.height(6.dp))
      OutlinedTextField(
        value = captionText,
        onValueChange = { captionText = it },
        placeholder = {
          Text(
            text = when (currentMode) {
              CreateMode.POST -> "पोस्ट के बारे में लिखें... What's on your mind?"
              CreateMode.REEL -> "रील्स का आकर्षक कैप्शन और हैशटैग जोड़ें..."
              CreateMode.STORY -> "स्टोरी में कोई संदेश लिखें..."
            },
            color = Color(0xFF64748B),
            fontSize = 13.sp
          )
        },
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = Color(0xFF1E293B),
          unfocusedContainerColor = Color(0xFF1E293B),
          focusedBorderColor = Color(0xFF8B5CF6),
          unfocusedBorderColor = Color(0xFF334155),
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        minLines = 3,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("create_caption_input")
      )

      // 6. Reels Audio / Music Selector (If REEL mode)
      if (currentMode == CreateMode.REEL) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          "संगीत / ऑडियो जोड़ें • Music & Audio",
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { showMusicPicker = true }
            .testTag("reels_music_picker_trigger"),
          color = Color(0xFF1E293B),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFFEC4899))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              selectedMusicTrack,
              color = Color.White,
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium,
              modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8))
          }
        }
      }

      // 7. Location Tagging (If POST mode)
      if (currentMode == CreateMode.POST) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          "स्थान जोड़ें • Location",
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = locationText,
          onValueChange = { locationText = it },
          leadingIcon = {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF06B6D4))
          },
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF1E293B),
            unfocusedContainerColor = Color(0xFF1E293B),
            focusedBorderColor = Color(0xFF06B6D4),
            unfocusedBorderColor = Color(0xFF334155),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          ),
          shape = RoundedCornerShape(12.dp),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("create_location_input")
        )
      }

      // 8. Hashtags Chips
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        "ट्रेंडिंग हैशटैग • Trending Hashtags",
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
      )
      Spacer(modifier = Modifier.height(8.dp))
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf("#Vexora", "#Trending", "#India", "#Viral", "#Photography", "#Creativity").forEach { tag ->
          val isAdded = selectedHashtags.contains(tag)
          FilterChip(
            selected = isAdded,
            onClick = {
              selectedHashtags = if (isAdded) selectedHashtags - tag else selectedHashtags + tag
            },
            label = { Text(tag, fontSize = 12.sp) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Color(0xFF8B5CF6),
              selectedLabelColor = Color.White,
              containerColor = Color(0xFF1E293B),
              labelColor = Color(0xFF94A3B8)
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(30.dp))
    }

    // Modal: Music Selector for Reels
    if (showMusicPicker) {
      ModalBottomSheet(
        onDismissRequest = { showMusicPicker = false },
        containerColor = Color(0xFF0F172A)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
        ) {
          Text(
            "रील्स के लिए संगीत चुनें • Choose Music",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Spacer(modifier = Modifier.height(16.dp))

          val musicList = listOf(
            "VEXORA Cyber Beats 2026 • Trending Audio",
            "Desi Dholak HipHop Mix • Viral Sound",
            "Acoustic Sunset Breeze • Chill Vibes",
            "Energetic Bass Gym Workout • High Tempo",
            "Bollywood Nostalgia Instrumental"
          )

          musicList.forEach { track ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  selectedMusicTrack = track
                  showMusicPicker = false
                }
                .padding(vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .background(Color(0xFFEC4899), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
              }
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                track,
                color = if (selectedMusicTrack == track) Color(0xFF38BDF8) else Color.White,
                fontWeight = if (selectedMusicTrack == track) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
              )
              if (selectedMusicTrack == track) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF38BDF8))
              }
            }
            HorizontalDivider(color = Color(0xFF1E293B))
          }
        }
      }
    }
  }
}
