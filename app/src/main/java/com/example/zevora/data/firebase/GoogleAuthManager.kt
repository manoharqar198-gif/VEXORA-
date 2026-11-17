package com.example.zevora.data.firebase

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

object GoogleAuthManager {

  private const val TAG = "VexoraGoogleAuth"
  private val isInteractiveSignInActive = AtomicBoolean(false)

  private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }

  /**
   * Silent Auto-Sign-In on startup using GetGoogleIdOption (one option per request).
   * Never creates a fake session; falls back cleanly to onUnauthenticated() so the user
   * sees the interactive Login screen (Mobile OTP + Continue with Google).
   */
  fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager = CredentialManager.create(context),
    scope: CoroutineScope,
    onAuthSuccess: (FirebaseUser) -> Unit,
    onUnauthenticated: () -> Unit
  ) {
    val existingUser = try {
      Firebase.auth.currentUser
    } catch (_: Exception) {
      null
    }
    if (existingUser != null) {
      onAuthSuccess(existingUser)
      return
    }

    val clientId = try {
      context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      Log.w(TAG, "default_web_client_id missing during auto sign-in check", e)
      onUnauthenticated()
      return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()

    val request = GetCredentialRequest.Builder()
      .addCredentialOption(googleIdOption)
      .build()

    scope.launch {
      try {
        val activity = context.findActivity()
        val requestContext: Context = activity ?: context
        val result = credentialManager.getCredential(requestContext, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
          val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
          val authResult = Firebase.auth.signInWithCredential(authCredential).await()
          val firebaseUser = authResult.user
          if (firebaseUser != null) {
            onAuthSuccess(firebaseUser)
          } else {
            onUnauthenticated()
          }
        } else {
          onUnauthenticated()
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.d(TAG, "Silent Google auto sign-in unavailable (${e.javaClass.simpleName})")
        onUnauthenticated()
      }
    }
  }

  /**
   * Interactive Google Sign-In using native Credential Manager bottom sheet
   * ("Choose an account") via GetSignInWithGoogleOption.
   * Reports honest, actionable diagnostics if GMS / Chrome Custom Tab / SHA-1 / Firebase Auth interrupts the flow.
   */
  fun signInWithGoogle(
    context: Context,
    credentialManager: CredentialManager = CredentialManager.create(context),
    scope: CoroutineScope,
    onAuthSuccess: (FirebaseUser) -> Unit,
    onAuthCancelled: (String?) -> Unit,
    onAuthError: (String) -> Unit
  ) {
    val clientId = try {
      context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      Log.e(TAG, "Missing R.string.default_web_client_id", e)
      onAuthError("Firebase Google Sign-In कॉन्फ़िगरेशन (Web Client ID) उपलब्ध नहीं है।")
      return
    }

    val activity = context.findActivity()
    if (activity == null) {
      onAuthError("Google Sign-In शुरू करने के लिए सक्रिय Activity नहीं मिली।")
      return
    }

    if (!isInteractiveSignInActive.compareAndSet(false, true)) {
      return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
      .addCredentialOption(signInOption)
      .build()

    scope.launch {
      try {
        val result = credentialManager.getCredential(activity, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
          val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
          val authResult = Firebase.auth.signInWithCredential(authCredential).await()
          val firebaseUser = authResult.user
          if (firebaseUser != null) {
            onAuthSuccess(firebaseUser)
          } else {
            onAuthError("Firebase Authentication से यूज़र प्रोफ़ाइल प्राप्त नहीं हुई।")
          }
        } else {
          onAuthError("अमान्य Google Credential प्राप्त हुआ। कृपया पुनः प्रयास करें।")
        }
      } catch (e: GoogleIdTokenParsingException) {
        Log.e(TAG, "Invalid Google ID token response", e)
        onAuthError("Google ID Token सत्यापित नहीं हो सका। कृपया Play Services अपडेट करें और पुनः प्रयास करें।")
      } catch (e: GetCredentialCancellationException) {
        Log.w(TAG, "Google Sign-In cancelled or Chrome/GMS consent interrupted: ${e.message}", e)
        onAuthCancelled(
          "Google Sign-In पूरा नहीं हो सका। यदि Gmail चुनने पर Chrome Welcome स्क्रीन खुली थी, तो Chrome सेटअप पूरा करके दोबारा प्रयास करें (या Firebase Console में SHA-1 व Google Provider जांचें)।"
        )
      } catch (e: NoCredentialException) {
        Log.w(TAG, "No Google credential found on device: ${e.message}", e)
        onAuthError("इस डिवाइस पर कोई सक्रिय Google Account नहीं मिला। Phone Settings → Accounts → Google में लॉगिन करके पुनः प्रयास करें।")
      } catch (e: GetCredentialProviderConfigurationException) {
        Log.e(TAG, "Credential Manager Play Services provider unavailable", e)
        onAuthError("इस डिवाइस/Emulator में Google Play Services Credential Provider सक्रिय नहीं है। कृपया वास्तविक Android फोन पर टेस्ट करें।")
      } catch (e: GetCredentialUnsupportedException) {
        Log.e(TAG, "Credential Manager unsupported on device", e)
        onAuthError("इस डिवाइस का Google Play Services संस्करण Credential Manager को सपोर्ट नहीं करता।")
      } catch (e: GetCredentialInterruptedException) {
        Log.w(TAG, "Credential Manager flow interrupted", e)
        onAuthError("Google Sign-In प्रक्रिया बीच में बाधित हो गई। कृपया कुछ सेकंड बाद पुनः प्रयास करें।")
      } catch (e: GetCredentialException) {
        Log.e(TAG, "Credential Manager error (${e.type})", e)
        val detail = e.localizedMessage?.takeIf { it.isNotBlank() } ?: e.type
        onAuthError("Google Account सत्यापन विफल रहा: $detail")
      } catch (e: FirebaseAuthException) {
        onAuthError(mapGoogleFirebaseException(e))
      } catch (e: FirebaseNetworkException) {
        Log.e(TAG, "Firebase network failure during sign-in", e)
        onAuthError("इंटरनेट कनेक्शन धीमा या बंद है। नेटवर्क जांचकर पुनः प्रयास करें।")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Firebase Google Sign-In failed", e)
        onAuthError(
          e.localizedMessage?.takeIf { it.isNotBlank() }
            ?: "Firebase Google लॉगिन पूरा नहीं हो सका। कृपया इंटरनेट, Chrome और Play Services जांचें।"
        )
      } finally {
        isInteractiveSignInActive.set(false)
      }
    }
  }

  /**
   * Securely links a Google Account (via Native Credential Manager verification)
   * to the currently signed-in Firebase user (e.g. a user who signed in with Mobile OTP).
   * Refuses unverified merges on collision so separate accounts are never overwritten.
   */
  fun linkGoogleAccount(
    context: Context,
    credentialManager: CredentialManager = CredentialManager.create(context),
    scope: CoroutineScope,
    onLinkSuccess: (FirebaseUser) -> Unit,
    onLinkCancelled: (String?) -> Unit,
    onLinkError: (String) -> Unit
  ) {
    val currentUser = try {
      Firebase.auth.currentUser
    } catch (_: Exception) {
      null
    }
    if (currentUser == null) {
      onLinkError("Google Account लिंक करने के लिए पहले लॉगिन होना आवश्यक है।")
      return
    }

    val clientId = try {
      context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      Log.e(TAG, "Missing R.string.default_web_client_id during linking", e)
      onLinkError("Firebase Web Client ID उपलब्ध नहीं है।")
      return
    }

    val activity = context.findActivity()
    if (activity == null) {
      onLinkError("Google Account चुनने के लिए सक्रिय Activity नहीं मिली।")
      return
    }

    if (!isInteractiveSignInActive.compareAndSet(false, true)) {
      return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
      .addCredentialOption(signInOption)
      .build()

    scope.launch {
      try {
        val result = credentialManager.getCredential(activity, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
          val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
          val linkResult = currentUser.linkWithCredential(authCredential).await()
          val updatedUser = linkResult.user ?: currentUser
          try {
            updatedUser.reload().await()
          } catch (_: Exception) {
            // Non-fatal if profile reload delays briefly
          }
          onLinkSuccess(Firebase.auth.currentUser ?: updatedUser)
        } else {
          onLinkError("अमान्य Google Credential प्राप्त हुआ।")
        }
      } catch (e: GetCredentialCancellationException) {
        Log.w(TAG, "Google link cancelled: ${e.message}", e)
        onLinkCancelled("Google Account लिंक प्रक्रिया रद्द की गई।")
      } catch (e: NoCredentialException) {
        onLinkError("इस डिवाइस पर कोई सक्रिय Google Account नहीं मिला।")
      } catch (e: GetCredentialException) {
        val detail = e.localizedMessage?.takeIf { it.isNotBlank() } ?: e.type
        onLinkError("Google Account सत्यापन विफल रहा: $detail")
      } catch (e: FirebaseAuthUserCollisionException) {
        Log.w(TAG, "Google account already linked to another Firebase UID", e)
        onLinkError(
          "यह Gmail खाता पहले से किसी दूसरे VEXORA Firebase अकाउंट से जुड़ा है। बिना सत्यापन के अलग खाते मर्ज नहीं किए जा सकते—इस खाते में जाने के लिए Settings से लॉगआउट करके उस Google खाते से लॉगिन करें।"
        )
      } catch (e: FirebaseAuthException) {
        onLinkError(mapGoogleFirebaseException(e))
      } catch (e: FirebaseNetworkException) {
        onLinkError("इंटरनेट कनेक्शन बंद या धीमा है। कृपया पुनः प्रयास करें।")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "linkGoogleAccount failed", e)
        onLinkError(e.localizedMessage ?: "Google Account लिंक नहीं हो सका।")
      } finally {
        isInteractiveSignInActive.set(false)
      }
    }
  }

  private fun mapGoogleFirebaseException(e: FirebaseAuthException): String {
    Log.e(TAG, "FirebaseAuth error code=${e.errorCode}", e)
    return when (e.errorCode) {
      "ERROR_OPERATION_NOT_ALLOWED" ->
        "Firebase Console में Google Sign-In Provider चालू नहीं है या Support Email सेट नहीं है।"
      "ERROR_USER_DISABLED" ->
        "यह Google खाता Firebase में अस्थायी रूप से निलंबित (Disabled) है।"
      "ERROR_INVALID_CREDENTIAL" ->
        "Google Token अमान्य है। Firebase Console में Package Name (com.aistudio.zevora.appsoc) और SHA-1/SHA-256 जांचें।"
      "ERROR_PROVIDER_ALREADY_LINKED" ->
        "इस खाते से पहले ही एक Google Account लिंक है।"
      "ERROR_CREDENTIAL_ALREADY_IN_USE", "ERROR_EMAIL_ALREADY_IN_USE" ->
        "यह Gmail खाता पहले से किसी दूसरे VEXORA खाते से जुड़ा है। बिना सत्यापन अलग खाते मर्ज नहीं किए जाते।"
      else ->
        "Firebase Auth त्रुटि (${e.errorCode}): ${e.localizedMessage ?: "पुनः प्रयास करें"}"
    }
  }

  /**
   * Signs out from Credential Manager & Firebase Authentication cleanly without deleting user cloud data.
   */
  fun signOut(
    context: Context,
    credentialManager: CredentialManager = CredentialManager.create(context),
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
  ) {
    scope.launch {
      withContext(NonCancellable) {
        try {
          credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
          Log.w(TAG, "Failed to clear credential state: ${e.message}")
        } finally {
          try {
            Firebase.auth.signOut()
          } catch (e: Exception) {
            Log.w(TAG, "Firebase signOut notice: ${e.message}")
          }
          onSignOutComplete()
        }
      }
    }
  }
}
