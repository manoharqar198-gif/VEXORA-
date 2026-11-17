package com.example.zevora.ui.components

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun CameraPreviewView(
  modifier: Modifier = Modifier,
  lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
  fallbackGradient: Brush = Brush.verticalGradient(
    listOf(Color(0xFF1E1B4B), Color(0xFF0F172A), Color(0xFF020617))
  )
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var hasCameraBindError by remember { mutableStateOf(false) }
  var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
  val previewView = remember(context) {
    PreviewView(context).apply {
      scaleType = PreviewView.ScaleType.FILL_CENTER
    }
  }

  LaunchedEffect(context) {
    val future = ProcessCameraProvider.getInstance(context)
    future.addListener({
      try {
        cameraProvider = future.get()
      } catch (_: Exception) {
        hasCameraBindError = true
      }
    }, ContextCompat.getMainExecutor(context))
  }

  LaunchedEffect(cameraProvider, lensFacing, lifecycleOwner) {
    val provider = cameraProvider ?: return@LaunchedEffect
    try {
      val preview = Preview.Builder().build().also {
        it.surfaceProvider = previewView.surfaceProvider
      }
      val selector = CameraSelector.Builder()
        .requireLensFacing(lensFacing)
        .build()
      provider.unbindAll()
      provider.bindToLifecycle(lifecycleOwner, selector, preview)
      hasCameraBindError = false
    } catch (_: Exception) {
      hasCameraBindError = true
    }
  }

  DisposableEffect(cameraProvider) {
    onDispose {
      try {
        cameraProvider?.unbindAll()
      } catch (_: Exception) {}
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    if (!hasCameraBindError) {
      AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
      )
    }

    if (hasCameraBindError) {
      // Elegant simulated live broadcast backdrop when camera hardware is simulated or restricted
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(fallbackGradient),
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .background(Color.Black.copy(alpha = 0.5f), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
          Text(
            text = "🔴 VEXORA LIVE FEED ACTIVE",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.sp
          )
        }
      }
    }
  }
}
