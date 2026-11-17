package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.credentials.CredentialManager
import com.example.ui.theme.ZevoraTheme
import com.example.zevora.data.PhoneOtpFlowState
import com.example.zevora.data.firebase.FirestoreManager
import com.example.zevora.data.firebase.GoogleAuthManager
import com.example.zevora.data.firebase.PhoneAuthManager
import com.example.zevora.model.AppThemeMode
import com.example.zevora.monetization.AdMobManager
import com.example.zevora.ui.Screen
import com.example.zevora.ui.ZevoraViewModel
import com.example.zevora.ui.components.CreateBottomSheet
import com.example.zevora.ui.components.ZevoraBottomNav
import com.example.zevora.ui.screens.*
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  private val viewModel: ZevoraViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val firebaseAuth: FirebaseAuth? = try {
      viewModel.attachFirestoreManager(FirestoreManager(applicationContext))
      Firebase.auth
    } catch (_: Exception) {
      // Safe fallback if FirebaseApp is unavailable in isolated unit tests
      null
    }

    setContent {
      val themeMode by viewModel.appThemeMode.collectAsState()
      val systemInDark = isSystemInDarkTheme()
      val isDarkTheme = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> systemInDark
      }

      val premiumSub by viewModel.premiumSubscription.collectAsState()
      val isAuthenticated by viewModel.isAuthenticated.collectAsState()
      val isAuthLoading by viewModel.isAuthLoading.collectAsState()
      val authErrorMessage by viewModel.authErrorMessage.collectAsState()
      val phoneOtpState by viewModel.phoneOtpFlowState.collectAsState()
      val currentUser by viewModel.currentUser.collectAsState()

      val authScope = viewModel.authOperationScope
      val credentialManager = remember(this@MainActivity) {
        CredentialManager.create(this@MainActivity)
      }
      var firebaseUid by remember { mutableStateOf(firebaseAuth?.currentUser?.uid) }
      var phoneResendToken by remember {
        mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null)
      }

      val syncFirebaseUser: (FirebaseUser) -> Unit = { fbUser ->
        viewModel.resolveFirebaseSession(
          uid = fbUser.uid,
          displayName = fbUser.displayName,
          email = fbUser.email,
          phoneNumber = fbUser.phoneNumber,
          photoUrl = fbUser.photoUrl?.toString(),
          providerIds = fbUser.providerData.map { it.providerId }
        )
      }

      // Keep FirebaseAuth session synchronized across cold starts and sign-out
      DisposableEffect(firebaseAuth) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
          val fbUser = auth.currentUser
          firebaseUid = fbUser?.uid
          if (fbUser != null) {
            syncFirebaseUser(fbUser)
          } else {
            viewModel.clearAuthenticatedSession()
          }
        }
        firebaseAuth?.addAuthStateListener(listener)
        onDispose {
          firebaseAuth?.removeAuthStateListener(listener)
        }
      }

      // Attempt silent Credential Manager sign-in once on startup if no Firebase user cached yet
      LaunchedEffect(Unit) {
        if (firebaseAuth != null && firebaseAuth.currentUser == null) {
          GoogleAuthManager.attemptAutoSignIn(
            context = this@MainActivity,
            credentialManager = credentialManager,
            scope = authScope,
            onAuthSuccess = syncFirebaseUser,
            onUnauthenticated = {
              // Stay cleanly on VexoraLoginScreen for Phone OTP or Google Sign-In
            }
          )
        }
      }

      // Premium-gated AdMob SDK initialization & ad preloading directly on launch
      LaunchedEffect(premiumSub.isPremium) {
        AdMobManager.syncPremiumStatusAndFetchAds(
          context = this@MainActivity,
          isPremium = premiumSub.isPremium
        )
      }

      ZevoraTheme(darkTheme = isDarkTheme) {
        if (!isAuthenticated) {
          VexoraLoginScreen(
            phoneOtpState = phoneOtpState,
            isLoading = isAuthLoading,
            authErrorMessage = authErrorMessage,
            onSendOtpClick = { countryCode, rawPhoneDigits ->
              PhoneAuthManager.validateAndFormatPhone(countryCode, rawPhoneDigits)
                .onFailure { validationErr ->
                  viewModel.setAuthError(validationErr.message)
                }
                .onSuccess { e164Phone ->
                  viewModel.markSendingOtp(e164Phone)
                  PhoneAuthManager.sendOtp(
                    context = this@MainActivity,
                    e164Phone = e164Phone,
                    resendToken = null,
                    onAutoVerified = { credential ->
                      authScope.launch {
                        PhoneAuthManager.signInWithPhoneCredential(credential)
                          .onSuccess(syncFirebaseUser)
                          .onFailure { err ->
                            viewModel.resetPhoneOtpFlow()
                            viewModel.setAuthError(err.message)
                          }
                      }
                    },
                    onCodeSent = { verificationId, token ->
                      phoneResendToken = token
                      viewModel.markOtpSent(
                        e164Phone = e164Phone,
                        maskedPhone = PhoneAuthManager.maskPhoneNumber(e164Phone),
                        verificationId = verificationId,
                        cooldownMillis = PhoneAuthManager.resendCooldownMillis(0),
                        resendCount = 0
                      )
                    },
                    onError = { errorMsg ->
                      viewModel.resetPhoneOtpFlow()
                      viewModel.setAuthError(errorMsg)
                    }
                  )
                }
            },
            onResendOtpClick = { e164Phone ->
              val previousSent = phoneOtpState as? PhoneOtpFlowState.OtpSent
              val currentResends = previousSent?.resendCount ?: 0
              val maxResends = previousSent?.maxResends ?: PhoneAuthManager.MAX_OTP_RESENDS
              if (currentResends >= maxResends) {
                viewModel.setAuthError(
                  "इस नंबर पर OTP दोबारा भेजने की अधिकतम सीमा ($maxResends बार) पूरी हो चुकी है। कृपया 'नंबर बदलें' चुनें या थोड़ी देर बाद प्रयास करें।"
                )
                return@VexoraLoginScreen
              }
              val nextResendAttempt = currentResends + 1
              viewModel.markSendingOtp(e164Phone)
              PhoneAuthManager.sendOtp(
                context = this@MainActivity,
                e164Phone = e164Phone,
                resendToken = phoneResendToken,
                onAutoVerified = { credential ->
                  authScope.launch {
                    PhoneAuthManager.signInWithPhoneCredential(credential)
                      .onSuccess(syncFirebaseUser)
                      .onFailure { err ->
                        viewModel.setAuthError(err.message)
                      }
                  }
                },
                onCodeSent = { verificationId, token ->
                  phoneResendToken = token
                  viewModel.markOtpSent(
                    e164Phone = e164Phone,
                    maskedPhone = PhoneAuthManager.maskPhoneNumber(e164Phone),
                    verificationId = verificationId,
                    cooldownMillis = PhoneAuthManager.resendCooldownMillis(nextResendAttempt),
                    resendCount = nextResendAttempt
                  )
                },
                onError = { errorMsg ->
                  if (previousSent != null) {
                    viewModel.restoreOtpSentState(
                      e164Phone = previousSent.e164Phone,
                      maskedPhone = previousSent.maskedPhone,
                      verificationId = previousSent.verificationId,
                      canResendAtMillis = previousSent.canResendAtMillis,
                      resendCount = previousSent.resendCount
                    )
                  } else {
                    viewModel.resetPhoneOtpFlow()
                  }
                  viewModel.setAuthError(errorMsg)
                }
              )
            },
            onVerifyOtpClick = { verificationId, code ->
              val sentSnapshot = phoneOtpState as? PhoneOtpFlowState.OtpSent
              val targetPhone = sentSnapshot?.e164Phone.orEmpty()
              val masked = sentSnapshot?.maskedPhone.orEmpty()
              val resendDeadline = sentSnapshot?.canResendAtMillis ?: System.currentTimeMillis()
              val resendCount = sentSnapshot?.resendCount ?: 0
              viewModel.markVerifyingOtp(targetPhone, masked, verificationId, resendCount)
              authScope.launch {
                PhoneAuthManager.verifyOtpAndSignIn(verificationId, code)
                  .onSuccess(syncFirebaseUser)
                  .onFailure { err ->
                    viewModel.restoreOtpSentState(
                      e164Phone = targetPhone,
                      maskedPhone = masked,
                      verificationId = verificationId,
                      canResendAtMillis = resendDeadline,
                      resendCount = resendCount
                    )
                    viewModel.setAuthError(err.message)
                  }
              }
            },
            onChangePhoneNumber = {
              phoneResendToken = null
              viewModel.resetPhoneOtpFlow()
            },
            onContinueWithGoogleClick = {
              viewModel.setAuthLoading(true)
              GoogleAuthManager.signInWithGoogle(
                context = this@MainActivity,
                credentialManager = credentialManager,
                scope = authScope,
                onAuthSuccess = syncFirebaseUser,
                onAuthCancelled = { diagnosticMessage ->
                  viewModel.setAuthError(diagnosticMessage)
                },
                onAuthError = { errorMsg ->
                  viewModel.setAuthError(errorMsg)
                }
              )
            },
            onConsentValidationError = { msg ->
              viewModel.setAuthError(msg)
            },
            onDismissError = {
              viewModel.clearAuthError()
            }
          )
        } else {
          key(firebaseUid ?: currentUser.id) {
            ZevoraMainContent(viewModel = viewModel)
          }
        }
      }
    }
  }
}

@Composable
fun ZevoraMainContent(viewModel: ZevoraViewModel) {
  val currentScreen by viewModel.currentScreen.collectAsState()
  val language by viewModel.appLanguage.collectAsState()
  var showCreateSheet by remember { mutableStateOf(false) }

  // Handle hardware back button navigation
  BackHandler(enabled = currentScreen != Screen.HOME) {
    viewModel.navigateBack()
  }

  val isFullScreen = currentScreen in listOf(
    Screen.LIVE_STREAM,
    Screen.CREATE_POST,
    Screen.CREATE_REEL,
    Screen.CREATE_STORY,
    Screen.CREATOR_STUDIO
  )

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    bottomBar = {
      // Show bottom navigation on primary screens
      if (!isFullScreen) {
        ZevoraBottomNav(
          currentScreen = currentScreen,
          onScreenSelected = { screen -> viewModel.navigateTo(screen) },
          onCreateClick = { showCreateSheet = true },
          language = language
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .then(if (!isFullScreen) Modifier.padding(innerPadding) else Modifier)
    ) {
      when (currentScreen) {
        Screen.HOME -> HomeFeedScreen(
          viewModel = viewModel,
          onNavigateTo = { viewModel.navigateTo(it) }
        )
        Screen.REELS -> ReelsScreen(
          viewModel = viewModel,
          onCreateReel = { viewModel.navigateTo(Screen.CREATE_REEL) }
        )
        Screen.WATCH -> VideosScreen(
          viewModel = viewModel,
          onNavigateTo = { viewModel.navigateTo(it) }
        )
        Screen.CHAT -> ChatScreen(
          viewModel = viewModel,
          onNavigateBack = { viewModel.navigateBack() }
        )
        Screen.PROFILE -> ProfileScreen(
          viewModel = viewModel,
          onNavigateTo = { viewModel.navigateTo(it) },
          onNavigateBack = { viewModel.navigateBack() }
        )
        Screen.SECURITY_PRIVACY -> SecurityPrivacyScreen(
          viewModel = viewModel,
          onNavigateBack = { viewModel.navigateBack() }
        )
        Screen.SETTINGS -> SettingsScreen(
          viewModel = viewModel,
          onNavigateBack = { viewModel.navigateBack() }
        )
        Screen.CREATOR_STUDIO -> CreatorEarningsScreen(
          viewModel = viewModel,
          onNavigateBack = { viewModel.navigateBack() }
        )
        Screen.CREATOR_WALLET -> CreatorWalletScreen(
          viewModel = viewModel,
          onNavigateBack = { viewModel.navigateBack() },
          onNavigateTo = { viewModel.navigateTo(it) }
        )
        Screen.PREMIUM_UPGRADE -> PremiumUpgradeScreen(
          viewModel = viewModel,
          onNavigateBack = { viewModel.navigateBack() }
        )
        Screen.ADMIN_PANEL -> AdminEarningsPanelScreen(
          viewModel = viewModel,
          onNavigateBack = { viewModel.navigateBack() }
        )
        Screen.LIVE_STREAM -> LiveStreamScreen(
          viewModel = viewModel,
          onClose = { viewModel.navigateBack() }
        )
        Screen.CREATE_POST -> CreateMediaScreen(
          viewModel = viewModel,
          initialMode = CreateMode.POST,
          onClose = { viewModel.navigateBack() }
        )
        Screen.CREATE_REEL -> CreateMediaScreen(
          viewModel = viewModel,
          initialMode = CreateMode.REEL,
          onClose = { viewModel.navigateBack() }
        )
        Screen.CREATE_STORY -> CreateMediaScreen(
          viewModel = viewModel,
          initialMode = CreateMode.STORY,
          onClose = { viewModel.navigateBack() }
        )
      }
    }
  }

  // Center (+) Create Bottom Sheet
  if (showCreateSheet) {
    CreateBottomSheet(
      onDismiss = { showCreateSheet = false },
      onGoLive = {
        viewModel.startLiveStream("🔴 Live Stream on VEXORA ✨")
      },
      onCreateReel = {
        viewModel.navigateTo(Screen.CREATE_REEL)
      },
      onCreatePost = {
        viewModel.navigateTo(Screen.CREATE_POST)
      },
      onAddStory = {
        viewModel.navigateTo(Screen.CREATE_STORY)
      }
    )
  }
}
