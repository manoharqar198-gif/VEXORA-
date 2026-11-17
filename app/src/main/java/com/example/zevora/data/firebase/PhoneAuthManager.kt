package com.example.zevora.data.firebase

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

data class CountryDialOption(
  val code: String,
  val name: String,
  val flag: String
)

object PhoneAuthManager {

  private const val TAG = "VexoraPhoneAuth"
  const val OTP_TIMEOUT_SECONDS = 60L
  const val MAX_OTP_RESENDS = 3

  fun resendCooldownMillis(resendAttempt: Int): Long = when {
    resendAttempt >= 2 -> 90_000L
    else -> OTP_TIMEOUT_SECONDS * 1000L
  }

  val supportedCountries = listOf(
    CountryDialOption("+91", "India", "🇮🇳"),
    CountryDialOption("+1", "USA / Canada", "🇺🇸"),
    CountryDialOption("+44", "United Kingdom", "🇬🇧"),
    CountryDialOption("+971", "UAE", "🇦🇪"),
    CountryDialOption("+65", "Singapore", "🇸🇬"),
    CountryDialOption("+61", "Australia", "🇦🇺")
  )

  private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }

  /**
   * Validates and formats local phone input into strict E.164 format (+91XXXXXXXXXX).
   */
  fun validateAndFormatPhone(countryCode: String, rawNumber: String): Result<String> {
    val cleanCode = countryCode.trim().let { if (it.startsWith("+")) it else "+$it" }
    val digitsOnly = rawNumber.filter { it.isDigit() }
    val normalizedDigits = if (cleanCode == "+91" && digitsOnly.length == 11 && digitsOnly.startsWith("0")) {
      digitsOnly.drop(1)
    } else {
      digitsOnly
    }

    if (cleanCode == "+91") {
      if (normalizedDigits.length != 10) {
        return Result.failure(
          IllegalArgumentException("कृपया सही 10 अंकों का मोबाइल नंबर दर्ज करें।")
        )
      }
    } else if (normalizedDigits.length !in 7..15) {
      return Result.failure(
        IllegalArgumentException("कृपया सही मोबाइल नंबर (7 से 15 अंक) दर्ज करें।")
      )
    }

    return Result.success("$cleanCode$normalizedDigits")
  }

  fun maskPhoneNumber(e164Phone: String): String {
    if (e164Phone.length <= 6) return e164Phone
    val prefix = e164Phone.take(5)
    val suffix = e164Phone.takeLast(4)
    val hiddenCount = (e164Phone.length - 9).coerceAtLeast(2)
    return "$prefix${"•".repeat(hiddenCount)}$suffix"
  }

  /**
   * Sends a real Firebase Phone Authentication OTP SMS.
   * Never creates a fake OTP or bypasses verification.
   */
  fun sendOtp(
    context: Context,
    e164Phone: String,
    resendToken: PhoneAuthProvider.ForceResendingToken? = null,
    onAutoVerified: (PhoneAuthCredential) -> Unit,
    onCodeSent: (verificationId: String, token: PhoneAuthProvider.ForceResendingToken) -> Unit,
    onError: (String) -> Unit
  ) {
    val activity = context.findActivity()
    if (activity == null) {
      onError("OTP भेजने के लिए सक्रिय Android Activity नहीं मिली।")
      return
    }

    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
      override fun onVerificationCompleted(credential: PhoneAuthCredential) {
        Log.d(TAG, "Phone verification completed automatically for ${maskPhoneNumber(e164Phone)}")
        onAutoVerified(credential)
      }

      override fun onVerificationFailed(e: FirebaseException) {
        Log.e(TAG, "Phone verification failed: ${e.javaClass.simpleName} - ${e.message}", e)
        onError(mapPhoneAuthException(e))
      }

      override fun onCodeSent(
        verificationId: String,
        token: PhoneAuthProvider.ForceResendingToken
      ) {
        Log.d(TAG, "OTP SMS sent for ${maskPhoneNumber(e164Phone)}")
        onCodeSent(verificationId, token)
      }

      override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
        Log.d(TAG, "SMS auto-retrieval window timed out; user can enter 6-digit OTP manually or resend.")
      }
    }

    try {
      val builder = PhoneAuthOptions.newBuilder(Firebase.auth)
        .setPhoneNumber(e164Phone)
        .setTimeout(OTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .setActivity(activity)
        .setCallbacks(callbacks)

      if (resendToken != null) {
        builder.setForceResendingToken(resendToken)
      }

      PhoneAuthProvider.verifyPhoneNumber(builder.build())
    } catch (e: Exception) {
      Log.e(TAG, "Failed to start verifyPhoneNumber", e)
      onError(mapPhoneAuthException(e))
    }
  }

  suspend fun verifyOtpAndSignIn(verificationId: String, otpCode: String): Result<FirebaseUser> {
    val cleanOtp = otpCode.filter { it.isDigit() }
    if (verificationId.isBlank()) {
      return Result.failure(IllegalStateException("OTP सेशन समाप्त हो गया है। कृपया नया OTP भेजें।"))
    }
    if (cleanOtp.length != 6) {
      return Result.failure(IllegalArgumentException("कृपया 6 अंकों का सही OTP दर्ज करें।"))
    }
    val credential = PhoneAuthProvider.getCredential(verificationId, cleanOtp)
    return signInWithPhoneCredential(credential)
  }

  suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): Result<FirebaseUser> {
    return try {
      val result = Firebase.auth.signInWithCredential(credential).await()
      val user = result.user
      if (user != null) {
        Result.success(user)
      } else {
        Result.failure(IllegalStateException("Firebase Authentication से यूज़र खाता प्राप्त नहीं हुआ।"))
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "signInWithPhoneCredential failed", e)
      Result.failure(IllegalStateException(mapPhoneAuthException(e), e))
    }
  }

  /**
   * Securely links a verified PhoneAuthCredential to the currently signed-in Firebase user.
   * Refuses unverified merges or cross-account collisions.
   */
  suspend fun verifyOtpAndLink(verificationId: String, otpCode: String): Result<FirebaseUser> {
    val cleanOtp = otpCode.filter { it.isDigit() }
    if (verificationId.isBlank()) {
      return Result.failure(IllegalStateException("OTP सेशन समाप्त हो गया है। कृपया नया OTP भेजें।"))
    }
    if (cleanOtp.length != 6) {
      return Result.failure(IllegalArgumentException("कृपया 6 अंकों का सही OTP दर्ज करें।"))
    }
    val credential = PhoneAuthProvider.getCredential(verificationId, cleanOtp)
    return linkPhoneCredential(credential)
  }

  suspend fun linkPhoneCredential(credential: PhoneAuthCredential): Result<FirebaseUser> {
    val currentUser = try {
      Firebase.auth.currentUser
    } catch (_: Exception) {
      null
    } ?: return Result.failure(IllegalStateException("मोबाइल नंबर लिंक करने के लिए पहले लॉगिन होना आवश्यक है।"))

    return try {
      val result = currentUser.linkWithCredential(credential).await()
      val linkedUser = result.user ?: currentUser
      try {
        linkedUser.reload().await()
      } catch (_: Exception) {
        // Non-fatal if profile reload delays briefly
      }
      Result.success(Firebase.auth.currentUser ?: linkedUser)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "linkPhoneCredential failed", e)
      Result.failure(IllegalStateException(mapPhoneAuthException(e), e))
    }
  }

  fun mapPhoneAuthException(e: Exception): String {
    val rawMessage = e.message.orEmpty()
    return when (e) {
      is FirebaseAuthUserCollisionException -> {
        "यह मोबाइल नंबर पहले से किसी दूसरे VEXORA Firebase खाते से जुड़ा है। बिना सत्यापन के दो अलग खाते आपस में मर्ज नहीं किए जा सकते।"
      }
      is FirebaseAuthInvalidCredentialsException -> {
        when (e.errorCode) {
          "ERROR_INVALID_VERIFICATION_CODE" ->
            "गलत OTP दर्ज किया गया है। कृपया SMS में आया 6 अंकों का कोड जाँचकर दोबारा डालें।"
          "ERROR_SESSION_EXPIRED" ->
            "OTP की समय-सीमा समाप्त हो गई है। कृपया 'Resend OTP' पर टैप करके नया कोड मंगाएँ।"
          "ERROR_INVALID_PHONE_NUMBER" ->
            "दर्ज किया गया मोबाइल नंबर अमान्य है। कृपया +91 के साथ सही 10 अंकों का नंबर डालें।"
          else ->
            "मोबाइल नंबर या OTP अमान्य है (${e.errorCode}): कृपया जाँचकर पुनः प्रयास करें।"
        }
      }
      is FirebaseAuthMissingActivityForRecaptchaException -> {
        "SMS सुरक्षा सत्यापन (Play Integrity / reCAPTCHA) प्रारंभ नहीं हो सका। कृपया इंटरनेट और Google Play Services जाँचें।"
      }
      is FirebaseTooManyRequestsException -> {
        "इस नंबर पर बार-बार OTP अनुरोध या SMS कोटा सीमा पार हो गई है। कृपया थोड़ी देर बाद प्रयास करें।"
      }
      is FirebaseNetworkException -> {
        "इंटरनेट कनेक्शन बंद या बहुत धीमा है। नेटवर्क जाँचकर पुनः प्रयास करें।"
      }
      is FirebaseAuthException -> {
        when (e.errorCode) {
          "ERROR_OPERATION_NOT_ALLOWED" -> {
            if (rawMessage.contains("region", ignoreCase = true) || rawMessage.contains("17006")) {
              "Firebase Console में भारत (+91) के लिए SMS Region Policy सक्षम नहीं है। कृपया Authentication → Settings → SMS region policy में India (+91) को Allow करें।"
            } else {
              "Firebase Console में Phone Sign-In Provider बंद है। कृपया Authentication → Sign-in method में Phone को Enable करें।"
            }
          }
          "ERROR_SESSION_EXPIRED" ->
            "OTP की समय-सीमा समाप्त हो गई है। कृपया 'Resend OTP' दबाएँ।"
          "ERROR_QUOTA_EXCEEDED" ->
            "Firebase प्रोजेक्ट की दैनिक SMS OTP कोटा सीमा पूरी हो चुकी है। कृपया कुछ समय बाद प्रयास करें या Firebase Billing/Quota जाँचें।"
          "ERROR_PROVIDER_ALREADY_LINKED" ->
            "इस खाते से पहले ही एक मोबाइल नंबर जुड़ा हुआ है।"
          "ERROR_USER_DISABLED" ->
            "यह खाता Firebase द्वारा अस्थायी रूप से निलंबित (Disabled) है।"
          "ERROR_APP_NOT_AUTHORIZED" ->
            "इस ऐप का Package Name (com.aistudio.zevora.appsoc) या SHA-1/SHA-256 Firebase Console से मेल नहीं खा रहा है।"
          "ERROR_CAPTCHA_CHECK_FAILED" ->
            "reCAPTCHA / सुरक्षा सत्यापन पूरा नहीं हो सका। कृपया स्थिर इंटरनेट के साथ 'Send OTP' दोबारा दबाएँ।"
          "ERROR_MISSING_CLIENT_IDENTIFIER" ->
            "ऐप सत्यापन पहचान (Play Integrity / SHA-256) नहीं मिल सकी। कृपया Google Play Services व Firebase में SHA-1/SHA-256 जाँचें।"
          "ERROR_WEB_CONTEXT_CANCELED" ->
            "सुरक्षा सत्यापन स्क्रीन बीच में बंद कर दी गई। SMS प्राप्त करने के लिए सत्यापन पूरा करें।"
          else ->
            "Firebase Phone Auth त्रुटि (${e.errorCode}): ${e.localizedMessage ?: "पुनः प्रयास करें"}"
        }
      }
      else -> {
        when {
          rawMessage.contains("17006") || rawMessage.contains("region", ignoreCase = true) ->
            "Firebase SMS Region Policy में भारत (+91) चालू नहीं है। कृपया Firebase Console → Authentication → Settings → SMS region policy जाँचें।"
          rawMessage.contains("play_integrity", ignoreCase = true) ||
            rawMessage.contains("app verification", ignoreCase = true) ||
            rawMessage.contains("missing client identifier", ignoreCase = true) ->
            "Play Integrity / reCAPTCHA ऐप सत्यापन विफल रहा। कृपया Firebase Console में SHA-1, SHA-256 और Play Integrity API जाँचें।"
          rawMessage.contains("quota", ignoreCase = true) ||
            rawMessage.contains("blocked all requests", ignoreCase = true) ->
            "इस डिवाइस या नंबर पर बार-बार अनुरोध के कारण SMS अस्थायी रूप से रोका गया है। कृपया थोड़ी देर बाद प्रयास करें।"
          else ->
            e.localizedMessage?.takeIf { it.isNotBlank() }
              ?: "SMS OTP भेजा या सत्यापित नहीं हो सका। कृपया मोबाइल नेटवर्क, DND सेटिंग, SHA-1/SHA-256 और Firebase Phone Provider जाँचें।"
        }
      }
    }
  }
}
