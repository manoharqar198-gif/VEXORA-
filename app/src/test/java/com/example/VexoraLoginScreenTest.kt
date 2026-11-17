package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ui.theme.ZevoraTheme
import com.example.zevora.data.PhoneOtpFlowState
import com.example.zevora.ui.screens.VexoraLoginScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VexoraLoginScreenTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun loginScreen_displaysDualAuthAndEnforcesTermsConsent() {
    var googleClickCount = 0
    var sendOtpArgs: Pair<String, String>? = null
    var verifyOtpArgs: Pair<String, String>? = null
    var resendTarget: String? = null
    var errorState by mutableStateOf<String?>(null)
    var otpState by mutableStateOf<PhoneOtpFlowState>(PhoneOtpFlowState.Idle)

    composeTestRule.setContent {
      ZevoraTheme(darkTheme = true) {
        VexoraLoginScreen(
          phoneOtpState = otpState,
          isLoading = false,
          authErrorMessage = errorState,
          onSendOtpClick = { code, digits ->
            sendOtpArgs = code to digits
            otpState = PhoneOtpFlowState.OtpSent(
              e164Phone = "$code$digits",
              maskedPhone = "$code•••••${digits.takeLast(4)}",
              verificationId = "vid_test_123",
              canResendAtMillis = System.currentTimeMillis() + 60_000L
            )
          },
          onResendOtpClick = { e164 ->
            resendTarget = e164
          },
          onVerifyOtpClick = { vid, otp ->
            verifyOtpArgs = vid to otp
          },
          onChangePhoneNumber = {
            otpState = PhoneOtpFlowState.Idle
          },
          onContinueWithGoogleClick = { googleClickCount++ },
          onConsentValidationError = { errorState = it },
          onDismissError = { errorState = null }
        )
      }
    }

    composeTestRule.onNodeWithText("VEXORA").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("country_code_selector").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("+91").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("phone_number_input").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("send_otp_btn").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("google_login_btn").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Continue with Google").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("login_more_info_btn").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Guest Mode").assertDoesNotExist()

    // Unchecking 18+ agreement blocks both Send OTP and Google Login
    composeTestRule.onNodeWithTag("login_terms_checkbox").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("send_otp_btn").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertTrue(sendOtpArgs == null)
    composeTestRule.onNodeWithTag("login_error_banner").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("google_login_btn").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(0, googleClickCount)

    // Re-check 18+ agreement and trigger Google Sign-In
    composeTestRule.onNodeWithTag("login_terms_checkbox").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("google_login_btn").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, googleClickCount)
    assertTrue(errorState == null)

    // Enter +91 10-digit mobile number and Send OTP -> OTP verification stage opens with timer
    composeTestRule.onNodeWithTag("phone_number_input").performScrollTo().performTextInput("9876543210")
    composeTestRule.onNodeWithTag("send_otp_btn").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    assertEquals("+91" to "9876543210", sendOtpArgs)
    composeTestRule.onNodeWithTag("otp_code_input").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("otp_timer_text").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("resend_otp_btn").performScrollTo().assertIsNotEnabled()
    composeTestRule.onNodeWithTag("change_phone_btn").performScrollTo().assertIsDisplayed()

    // Verify 6-digit OTP entry enables and invokes Verify callback
    composeTestRule.onNodeWithTag("otp_code_input").performScrollTo().performTextInput("654321")
    composeTestRule.onNodeWithTag("verify_otp_btn").performScrollTo().assertIsEnabled().performClick()
    composeTestRule.waitForIdle()
    assertEquals("vid_test_123" to "654321", verifyOtpArgs)

    // Once cooldown expires, Resend OTP becomes enabled and invokes resend callback
    otpState = PhoneOtpFlowState.OtpSent(
      e164Phone = "+919876543210",
      maskedPhone = "+9198••••3210",
      verificationId = "vid_test_123",
      canResendAtMillis = System.currentTimeMillis() - 1000L,
      resendCount = 0
    )
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("resend_otp_btn").performScrollTo().assertIsEnabled().performClick()
    composeTestRule.waitForIdle()
    assertEquals("+919876543210", resendTarget)

    // When max resend attempts (3/3) are exhausted, Resend OTP stays disabled even after cooldown
    otpState = PhoneOtpFlowState.OtpSent(
      e164Phone = "+919876543210",
      maskedPhone = "+9198••••3210",
      verificationId = "vid_test_123",
      canResendAtMillis = System.currentTimeMillis() - 1000L,
      resendCount = 3,
      maxResends = 3
    )
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("resend_otp_btn").performScrollTo().assertIsNotEnabled()
  }
}
