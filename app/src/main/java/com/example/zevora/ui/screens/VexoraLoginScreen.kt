package com.example.zevora.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.zevora.data.PhoneOtpFlowState
import com.example.zevora.data.firebase.PhoneAuthManager
import com.example.zevora.ui.components.LegalDocumentViewerDialog
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VexoraLoginScreen(
  phoneOtpState: PhoneOtpFlowState = PhoneOtpFlowState.Idle,
  isLoading: Boolean,
  authErrorMessage: String?,
  onSendOtpClick: (countryCode: String, phoneDigits: String) -> Unit = { _, _ -> },
  onResendOtpClick: (e164Phone: String) -> Unit = {},
  onVerifyOtpClick: (verificationId: String, otpCode: String) -> Unit = { _, _ -> },
  onChangePhoneNumber: () -> Unit = {},
  onContinueWithGoogleClick: () -> Unit,
  onConsentValidationError: (String) -> Unit,
  onDismissError: () -> Unit
) {
  var isTermsAccepted by rememberSaveable { mutableStateOf(true) }
  var selectedCountryCode by rememberSaveable { mutableStateOf("+91") }
  var phoneDigits by rememberSaveable { mutableStateOf("") }
  var otpCode by rememberSaveable { mutableStateOf("") }
  var showCountryPicker by remember { mutableStateOf(false) }
  var showMoreSheet by remember { mutableStateOf(false) }
  var showTermsDialog by remember { mutableStateOf(false) }
  var showPrivacyDialog by remember { mutableStateOf(false) }
  var revealContent by remember { mutableStateOf(false) }

  val keyboardController = LocalSoftwareKeyboardController.current
  val selectedCountry = remember(selectedCountryCode) {
    PhoneAuthManager.supportedCountries.firstOrNull { it.code == selectedCountryCode }
      ?: PhoneAuthManager.supportedCountries.first()
  }

  LaunchedEffect(Unit) {
    revealContent = true
  }

  // Clear stale OTP digits whenever returning to phone entry
  LaunchedEffect(phoneOtpState is PhoneOtpFlowState.Idle) {
    if (phoneOtpState is PhoneOtpFlowState.Idle) {
      otpCode = ""
    }
  }

  // Live 1-second resend timer when OTP is sent
  var remainingSeconds by remember(phoneOtpState) {
    mutableIntStateOf(
      if (phoneOtpState is PhoneOtpFlowState.OtpSent) {
        ((phoneOtpState.canResendAtMillis - System.currentTimeMillis()) / 1000L)
          .toInt()
          .coerceAtLeast(0)
      } else {
        0
      }
    )
  }

  LaunchedEffect(phoneOtpState) {
    if (phoneOtpState is PhoneOtpFlowState.OtpSent) {
      while (true) {
        val left = ((phoneOtpState.canResendAtMillis - System.currentTimeMillis()) / 1000L)
          .toInt()
          .coerceAtLeast(0)
        remainingSeconds = left
        if (left <= 0) break
        delay(1000L)
      }
    } else {
      remainingSeconds = 0
    }
  }

  val infiniteTransition = rememberInfiniteTransition(label = "vexora_login_ambient")
  val backdropScale by infiniteTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = 1.06f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 12000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "backdrop_scale"
  )
  val glowPulse by infiniteTransition.animateFloat(
    initialValue = 0.35f,
    targetValue = 0.72f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glow_pulse"
  )

  val buttonInteraction = remember { MutableInteractionSource() }
  val isButtonPressed by buttonInteraction.collectIsPressedAsState()
  val buttonScale by animateFloatAsState(
    targetValue = if (isButtonPressed) 0.97f else 1.0f,
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    label = "google_btn_scale"
  )

  fun validateConsent(): Boolean {
    return if (!isTermsAccepted) {
      onConsentValidationError(
        "जारी रखने के लिए कृपया नीचे User Agreement, Privacy Policy और 18+ पुष्टि को स्वीकार करें।"
      )
      false
    } else {
      true
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF060713))
  ) {
    Image(
      painter = painterResource(id = R.drawable.img_vexora_aurora_backdrop),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .fillMaxSize()
        .scale(backdropScale)
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
      drawRect(
        brush = Brush.verticalGradient(
          colorStops = arrayOf(
            0.0f to Color(0xD8060714),
            0.26f to Color(0x52120A2A),
            0.55f to Color(0x700C0822),
            0.80f to Color(0xEA070815),
            1.0f to Color(0xFA050610)
          )
        )
      )

      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            Color(0xFF8B5CF6).copy(alpha = 0.24f * glowPulse),
            Color(0xFFEC4899).copy(alpha = 0.12f * glowPulse),
            Color.Transparent
          ),
          center = Offset(size.width * 0.5f, size.height * 0.16f),
          radius = size.width * 0.65f
        ),
        center = Offset(size.width * 0.5f, size.height * 0.16f),
        radius = size.width * 0.65f
      )

      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            Color(0xFF06B6D4).copy(alpha = 0.20f * glowPulse),
            Color(0xFF7C3AED).copy(alpha = 0.14f * glowPulse),
            Color.Transparent
          ),
          center = Offset(size.width * 0.5f, size.height * 0.78f),
          radius = size.width * 0.72f
        ),
        center = Offset(size.width * 0.5f, size.height * 0.78f),
        radius = size.width * 0.72f
      )
    }

    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .imePadding()
    ) {
      val minBodyHeight = maxHeight
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = minBodyHeight)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 22.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        // Top Brand Medallion
        AnimatedVisibility(
          visible = revealContent,
          enter = fadeIn(tween(650)) + slideInVertically(tween(650)) { -it / 4 }
        ) {
          Column(
            modifier = Modifier.padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Box(
              modifier = Modifier
                .size(62.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                  Brush.linearGradient(
                    listOf(Color(0xFF7C3AED), Color(0xFFEC4899), Color(0xFF06B6D4))
                  )
                )
                .padding(2.dp)
            ) {
              Image(
                painter = painterResource(id = R.drawable.ic_vexora_logo_screenshot),
                contentDescription = "VEXORA Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                  .fillMaxSize()
                  .clip(RoundedCornerShape(16.dp))
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = "VEXORA",
              color = Color.White,
              fontSize = 36.sp,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 5.sp,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
              shape = RoundedCornerShape(50),
              color = Color.White.copy(alpha = 0.12f),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
            ) {
              Text(
                text = "LIVE  •  REELS  •  CREATORS",
                color = Color(0xFFE2E8F0),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Auth Deck: Mobile OTP + Continue with Google
        AnimatedVisibility(
          visible = revealContent,
          enter = fadeIn(tween(750, delayMillis = 100)) + slideInVertically(tween(750, delayMillis = 100)) { it / 3 }
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .widthIn(max = 460.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "मोबाइल OTP या Google से वास्तविक Firebase लॉगिन",
              color = Color(0xFFE2E8F0),
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Honest inline error banner
            AnimatedVisibility(
              visible = !authErrorMessage.isNullOrBlank(),
              enter = fadeIn() + slideInVertically { it / 2 },
              exit = fadeOut()
            ) {
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF450A0A).copy(alpha = 0.94f),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.75f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = 12.dp)
                  .testTag("login_error_banner")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Authentication error",
                    tint = Color(0xFFFCA5A5),
                    modifier = Modifier.size(20.dp)
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = authErrorMessage.orEmpty(),
                    color = Color(0xFFFEE2E2),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.weight(1f)
                  )
                  IconButton(
                    onClick = onDismissError,
                    modifier = Modifier.size(40.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Close,
                      contentDescription = "Dismiss error",
                      tint = Color(0xFFFCA5A5),
                      modifier = Modifier.size(16.dp)
                    )
                  }
                }
              }
            }

            // Card 1: Mobile Number + Real Firebase OTP Verification
            val isOtpStage = phoneOtpState is PhoneOtpFlowState.OtpSent ||
              phoneOtpState is PhoneOtpFlowState.VerifyingOtp
            val isSendingOtp = phoneOtpState is PhoneOtpFlowState.SendingOtp
            val isVerifyingOtp = phoneOtpState is PhoneOtpFlowState.VerifyingOtp

            Surface(
              shape = RoundedCornerShape(22.dp),
              color = Color(0xFF0F172A).copy(alpha = 0.86f),
              border = BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                  listOf(
                    Color(0xFF8B5CF6).copy(alpha = 0.55f),
                    Color(0xFF06B6D4).copy(alpha = 0.45f)
                  )
                )
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("mobile_otp_login_card")
            ) {
              AnimatedContent(
                targetState = isOtpStage,
                transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(180)) },
                label = "otp_stage_switch"
              ) { otpScreenActive ->
                if (!otpScreenActive) {
                  Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        text = "Mobile Number Login (OTP SMS)",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }

                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF475569)),
                        modifier = Modifier
                          .height(56.dp)
                          .clickable(enabled = !isLoading) { showCountryPicker = true }
                          .testTag("country_code_selector")
                      ) {
                        Row(
                          modifier = Modifier.padding(horizontal = 10.dp),
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Text(text = selectedCountry.flag, fontSize = 16.sp)
                          Spacer(modifier = Modifier.width(4.dp))
                          Text(
                            text = selectedCountry.code,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                          )
                          Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select country code",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                          )
                        }
                      }

                      OutlinedTextField(
                        value = phoneDigits,
                        onValueChange = { incoming ->
                          val filtered = incoming.filter { it.isDigit() }
                          val maxLen = if (selectedCountryCode == "+91") 10 else 15
                          phoneDigits = filtered.take(maxLen)
                          if (!authErrorMessage.isNullOrBlank()) onDismissError()
                        },
                        enabled = !isLoading,
                        singleLine = true,
                        placeholder = {
                          Text(
                            text = if (selectedCountryCode == "+91") "10 अंकों का मोबाइल नंबर" else "Mobile number",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                          )
                        },
                        keyboardOptions = KeyboardOptions(
                          keyboardType = KeyboardType.Phone,
                          imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                          onDone = {
                            keyboardController?.hide()
                            if (validateConsent()) {
                              onDismissError()
                              onSendOtpClick(selectedCountryCode, phoneDigits)
                            }
                          }
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                          focusedTextColor = Color.White,
                          unfocusedTextColor = Color.White,
                          focusedBorderColor = Color(0xFF38BDF8),
                          unfocusedBorderColor = Color(0xFF475569),
                          focusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.65f),
                          unfocusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.65f)
                        ),
                        modifier = Modifier
                          .weight(1f)
                          .testTag("phone_number_input")
                      )
                    }

                    Button(
                      onClick = {
                        keyboardController?.hide()
                        if (validateConsent()) {
                          onDismissError()
                          onSendOtpClick(selectedCountryCode, phoneDigits)
                        }
                      },
                      enabled = !isLoading,
                      shape = RoundedCornerShape(14.dp),
                      colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7C3AED),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF4C1D95).copy(alpha = 0.7f),
                        disabledContentColor = Color(0xFFCBD5E1)
                      ),
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("send_otp_btn")
                    ) {
                      if (isSendingOtp) {
                        CircularProgressIndicator(
                          color = Color.White,
                          strokeWidth = 2.dp,
                          modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                          text = "OTP भेजा जा रहा है...",
                          fontWeight = FontWeight.Bold,
                          fontSize = 14.sp
                        )
                      } else {
                        Icon(
                          imageVector = Icons.Default.Sms,
                          contentDescription = null,
                          modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                          text = "Send OTP",
                          fontWeight = FontWeight.Bold,
                          fontSize = 15.sp
                        )
                      }
                    }
                  }
                } else {
                  val e164Phone = when (phoneOtpState) {
                    is PhoneOtpFlowState.OtpSent -> phoneOtpState.e164Phone
                    is PhoneOtpFlowState.VerifyingOtp -> phoneOtpState.e164Phone
                    else -> ""
                  }
                  val maskedPhone = when (phoneOtpState) {
                    is PhoneOtpFlowState.OtpSent -> phoneOtpState.maskedPhone
                    is PhoneOtpFlowState.VerifyingOtp -> phoneOtpState.maskedPhone
                    else -> ""
                  }
                  val verificationId = when (phoneOtpState) {
                    is PhoneOtpFlowState.OtpSent -> phoneOtpState.verificationId
                    is PhoneOtpFlowState.VerifyingOtp -> phoneOtpState.verificationId
                    else -> ""
                  }
                  val resendCount = when (phoneOtpState) {
                    is PhoneOtpFlowState.OtpSent -> phoneOtpState.resendCount
                    is PhoneOtpFlowState.VerifyingOtp -> phoneOtpState.resendCount
                    else -> 0
                  }
                  val maxResends = when (phoneOtpState) {
                    is PhoneOtpFlowState.OtpSent -> phoneOtpState.maxResends
                    is PhoneOtpFlowState.VerifyingOtp -> phoneOtpState.maxResends
                    else -> PhoneAuthManager.MAX_OTP_RESENDS
                  }
                  val resendsLeft = (maxResends - resendCount).coerceAtLeast(0)
                  val canResendNow = remainingSeconds == 0 && resendsLeft > 0 && !isLoading

                  Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = "OTP Verification",
                          color = Color.White,
                          fontSize = 14.sp,
                          fontWeight = FontWeight.Bold
                        )
                        Text(
                          text = "असली SMS भेजा गया: $maskedPhone",
                          color = Color(0xFF38BDF8),
                          fontSize = 12.sp
                        )
                      }
                      TextButton(
                        onClick = {
                          otpCode = ""
                          onChangePhoneNumber()
                        },
                        enabled = !isLoading,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                          .heightIn(min = 48.dp)
                          .testTag("change_phone_btn")
                      ) {
                        Text(
                          text = "नंबर बदलें",
                          color = Color(0xFFF472B6),
                          fontSize = 12.sp,
                          fontWeight = FontWeight.SemiBold
                        )
                      }
                    }

                    OutlinedTextField(
                      value = otpCode,
                      onValueChange = { incoming ->
                        otpCode = incoming.filter { it.isDigit() }.take(6)
                        if (!authErrorMessage.isNullOrBlank()) onDismissError()
                      },
                      enabled = !isLoading,
                      singleLine = true,
                      placeholder = {
                        Text(
                          text = "SMS में आया 6 अंकों का OTP दर्ज करें",
                          fontSize = 13.sp,
                          color = Color(0xFF64748B)
                        )
                      },
                      keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                      ),
                      keyboardActions = KeyboardActions(
                        onDone = {
                          keyboardController?.hide()
                          if (otpCode.length == 6 && !isLoading) {
                            onDismissError()
                            onVerifyOtpClick(verificationId, otpCode)
                          }
                        }
                      ),
                      textStyle = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 6.sp,
                        color = Color.White
                      ),
                      shape = RoundedCornerShape(14.dp),
                      colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF475569),
                        focusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.75f),
                        unfocusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.75f)
                      ),
                      modifier = Modifier
                        .fillMaxWidth()
                        .testTag("otp_code_input")
                    )

                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      val mins = remainingSeconds / 60
                      val secs = remainingSeconds % 60
                      val timerFormatted = "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = when {
                            resendsLeft == 0 -> "Resend सीमा ($maxResends/$maxResends) पूरी हो गई"
                            remainingSeconds > 0 -> "Resend OTP: $timerFormatted"
                            else -> "नया OTP भेजने के लिए तैयार ($resendsLeft/$maxResends शेष)"
                          },
                          color = when {
                            resendsLeft == 0 -> Color(0xFFFCA5A5)
                            remainingSeconds > 0 -> Color(0xFF94A3B8)
                            else -> Color(0xFF34D399)
                          },
                          fontSize = 12.sp,
                          fontWeight = FontWeight.Medium,
                          modifier = Modifier.testTag("otp_timer_text")
                        )
                        if (remainingSeconds == 0 && resendsLeft > 0) {
                          Text(
                            text = "SMS न आए तो नेटवर्क / DND जांचें",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                          )
                        }
                      }

                      Spacer(modifier = Modifier.width(8.dp))

                      OutlinedButton(
                        onClick = {
                          otpCode = ""
                          onDismissError()
                          onResendOtpClick(e164Phone)
                        },
                        enabled = canResendNow,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                          1.dp,
                          if (canResendNow) Color(0xFF38BDF8) else Color(0xFF334155)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                          .heightIn(min = 48.dp)
                          .testTag("resend_otp_btn")
                      ) {
                        Text(
                          text = if (resendsLeft == 0) "सीमा पूर्ण" else "Resend OTP",
                          fontSize = 12.sp,
                          fontWeight = FontWeight.Bold,
                          color = if (canResendNow) Color(0xFF38BDF8) else Color(0xFF64748B)
                        )
                      }
                    }

                    Button(
                      onClick = {
                        keyboardController?.hide()
                        onDismissError()
                        onVerifyOtpClick(verificationId, otpCode)
                      },
                      enabled = otpCode.length == 6 && !isLoading,
                      shape = RoundedCornerShape(14.dp),
                      colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF065F46).copy(alpha = 0.5f),
                        disabledContentColor = Color(0xFF94A3B8)
                      ),
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("verify_otp_btn")
                    ) {
                      if (isVerifyingOtp) {
                        CircularProgressIndicator(
                          color = Color.White,
                          strokeWidth = 2.dp,
                          modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                          text = "OTP सत्यापित हो रहा है...",
                          fontWeight = FontWeight.Bold,
                          fontSize = 14.sp
                        )
                      } else {
                        Text(
                          text = "Verify OTP & Login",
                          fontWeight = FontWeight.Bold,
                          fontSize = 15.sp
                        )
                      }
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // OR Divider
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = Color.White.copy(alpha = 0.22f)
              )
              Text(
                text = "  OR / या  ",
                color = Color(0xFFCBD5E1),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
              )
              HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = Color.White.copy(alpha = 0.22f)
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Option 2: Native Google Login via Credential Manager
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .scale(buttonScale)
                .clip(RoundedCornerShape(50))
                .border(
                  width = 1.5.dp,
                  brush = Brush.horizontalGradient(
                    listOf(
                      Color(0xFF8B5CF6).copy(alpha = 0.85f),
                      Color(0xFFEC4899).copy(alpha = 0.85f),
                      Color(0xFF06B6D4).copy(alpha = 0.85f)
                    )
                  ),
                  shape = RoundedCornerShape(50)
                )
            ) {
              Button(
                onClick = {
                  keyboardController?.hide()
                  if (validateConsent()) {
                    onDismissError()
                    onContinueWithGoogleClick()
                  }
                },
                enabled = !isLoading,
                interactionSource = buttonInteraction,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFFF8FAFC),
                  contentColor = Color(0xFF0F172A),
                  disabledContainerColor = Color(0xFFE2E8F0).copy(alpha = 0.90f),
                  disabledContentColor = Color(0xFF334155)
                ),
                elevation = ButtonDefaults.buttonElevation(
                  defaultElevation = 10.dp,
                  pressedElevation = 4.dp
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(54.dp)
                  .testTag("google_login_btn")
              ) {
                if (isLoading && phoneOtpState is PhoneOtpFlowState.Idle) {
                  CircularProgressIndicator(
                    color = Color(0xFF7C3AED),
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(20.dp)
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = "Google से साइन इन हो रहा है...",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                  )
                } else {
                  GoogleGMarkIcon(modifier = Modifier.size(22.dp))
                  Spacer(modifier = Modifier.width(12.dp))
                  Text(
                    text = "Continue with Google",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(4.dp))

            TextButton(
              onClick = { showMoreSheet = true },
              modifier = Modifier
                .height(48.dp)
                .testTag("login_more_info_btn")
            ) {
              Text(
                text = "More",
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
              )
            }

            // Consent Row (18+ & Legal)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(48.dp)
                  .clip(CircleShape)
                  .clickable(
                    role = Role.Checkbox,
                    onClick = {
                      isTermsAccepted = !isTermsAccepted
                      if (isTermsAccepted) onDismissError()
                    }
                  )
                  .testTag("login_terms_checkbox")
              ) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isTermsAccepted) Color.White else Color.Transparent)
                    .border(
                      width = 1.6.dp,
                      color = if (isTermsAccepted) Color.White else Color.White.copy(alpha = 0.72f),
                      shape = CircleShape
                    )
                ) {
                  if (isTermsAccepted) {
                    Icon(
                      imageVector = Icons.Default.Check,
                      contentDescription = "Agreed to terms and 18+ confirmation",
                      tint = Color(0xFF0F172A),
                      modifier = Modifier.size(13.dp)
                    )
                  }
                }
              }

              val consentText = buildAnnotatedString {
                withStyle(SpanStyle(color = Color(0xFFE2E8F0))) {
                  append("Agree to ")
                }
                withLink(
                  LinkAnnotation.Clickable(
                    tag = "user_agreement",
                    styles = TextLinkStyles(
                      style = SpanStyle(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline
                      )
                    ),
                    linkInteractionListener = { showTermsDialog = true }
                  )
                ) {
                  append("User Agreement")
                }
                withStyle(SpanStyle(color = Color(0xFFE2E8F0))) {
                  append(" & ")
                }
                withLink(
                  LinkAnnotation.Clickable(
                    tag = "privacy_policy",
                    styles = TextLinkStyles(
                      style = SpanStyle(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline
                      )
                    ),
                    linkInteractionListener = { showPrivacyDialog = true }
                  )
                ) {
                  append("Privacy Policy")
                }
                withStyle(SpanStyle(color = Color(0xFFE2E8F0))) {
                  append(" and confirm I'm over 18")
                }
              }

              Text(
                text = consentText,
                style = TextStyle(
                  fontSize = 12.sp,
                  lineHeight = 16.sp,
                  color = Color(0xFFE2E8F0)
                ),
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      }
    }
  }

  // Country Code Picker Dialog
  if (showCountryPicker) {
    AlertDialog(
      onDismissRequest = { showCountryPicker = false },
      containerColor = Color(0xFF0F172A),
      title = {
        Text(
          text = "देश चुनें / Country Code",
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 17.sp
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          PhoneAuthManager.supportedCountries.forEach { option ->
            val isSelected = option.code == selectedCountryCode
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isSelected) Color(0xFF1E293B) else Color.Transparent,
              border = if (isSelected) BorderStroke(1.dp, Color(0xFF38BDF8)) else null,
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable {
                  selectedCountryCode = option.code
                  showCountryPicker = false
                }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(option.flag, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = option.name,
                  color = Color.White,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Medium,
                  modifier = Modifier.weight(1f)
                )
                Text(
                  text = option.code,
                  color = Color(0xFF38BDF8),
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showCountryPicker = false }) {
          Text("बंद करें", color = Color(0xFF38BDF8))
        }
      }
    )
  }

  // "More" Info Bottom Sheet explaining real Mobile OTP + Google Native Credential Manager + Safe Linking
  if (showMoreSheet) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = { showMoreSheet = false },
      sheetState = sheetState,
      containerColor = Color(0xFF0F172A),
      contentColor = Color.White
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp)
          .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Text(
          text = "VEXORA Dual Authentication सुरक्षा",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
        Text(
          text = "VEXORA में आप वास्तविक Firebase Phone OTP SMS (+91) या Android Native Google Account Picker—दोनों से सुरक्षित लॉगिन कर सकते हैं।",
          fontSize = 13.sp,
          color = Color(0xFFCBD5E1),
          lineHeight = 19.sp
        )
        LoginDetailRow(
          icon = Icons.Default.Sms,
          title = "Real Firebase Mobile OTP (+91)",
          subtitle = "10 अंकों का नंबर दर्ज करें और 60-सेकंड टाइमर व Resend विकल्प के साथ असली SMS OTP से लॉगिन करें।"
        )
        LoginDetailRow(
          icon = Icons.Default.VerifiedUser,
          title = "Native Google Credential Manager",
          subtitle = "ब्राउज़र रीडायरेक्ट के बिना सीधे Android Account Chooser में अपना Gmail चुनें।"
        )
        LoginDetailRow(
          icon = Icons.Default.Shield,
          title = "Verified Account Linking",
          subtitle = "लॉगिन के बाद Settings में जाकर Phone और Google दोनों को बिना डेटा खोए एक ही खाते से सुरक्षित लिंक करें।"
        )
        LoginDetailRow(
          icon = Icons.Default.Lock,
          title = "Zero Fake Bypass",
          subtitle = "कोई नकली OTP या गलत मर्ज नहीं—हर लॉगिन और लिंक वास्तविक Firebase सत्यापन के बाद ही होता है।"
        )
        Spacer(modifier = Modifier.height(6.dp))
        Button(
          onClick = { showMoreSheet = false },
          shape = RoundedCornerShape(50),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
        ) {
          Text("ठीक है (Got it)", fontWeight = FontWeight.Bold)
        }
      }
    }
  }

  if (showTermsDialog) {
    LegalDocumentViewerDialog(
      title = "User Agreement",
      onDismiss = { showTermsDialog = false },
      isPrivacyPolicy = false
    )
  }

  if (showPrivacyDialog) {
    LegalDocumentViewerDialog(
      title = "Privacy Policy",
      onDismiss = { showPrivacyDialog = false },
      isPrivacyPolicy = true
    )
  }
}

@Composable
private fun LoginDetailRow(
  icon: ImageVector,
  title: String,
  subtitle: String
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top
  ) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = Color(0xFF1E293B),
      modifier = Modifier.size(40.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(20.dp))
      }
    }
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
      Text(subtitle, fontSize = 12.sp, color = Color(0xFF94A3B8), lineHeight = 17.sp)
    }
  }
}

@Composable
private fun GoogleGMarkIcon(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val strokeWidth = w * 0.19f
    val inset = strokeWidth / 2f
    val arcSize = Size(w - strokeWidth, h - strokeWidth)
    val topLeft = Offset(inset, inset)

    drawArc(
      color = Color(0xFF4285F4),
      startAngle = -35f,
      sweepAngle = 82f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
    )
    drawArc(
      color = Color(0xFF34A853),
      startAngle = 45f,
      sweepAngle = 92f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
    )
    drawArc(
      color = Color(0xFFFBBC05),
      startAngle = 135f,
      sweepAngle = 72f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
    )
    drawArc(
      color = Color(0xFFEA4335),
      startAngle = 205f,
      sweepAngle = 120f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
    )

    val barPath = Path().apply {
      moveTo(w * 0.50f, h * 0.43f)
      lineTo(w * 0.94f, h * 0.43f)
      lineTo(w * 0.94f, h * 0.58f)
      lineTo(w * 0.50f, h * 0.58f)
      close()
    }
    drawPath(barPath, color = Color(0xFF4285F4))
  }
}
