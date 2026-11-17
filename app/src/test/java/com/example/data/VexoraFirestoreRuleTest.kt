package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.zevora.data.firebase.FirestoreManager
import com.example.zevora.model.User
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class VexoraFirestoreRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun syncUserProfile_authenticatedOwner_succeedsAndReadsFromServer() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val manager = FirestoreManager(firestore, auth)
    val user = User(id = uid, name = "Alice Sharma", username = "alice_vexora")

    val syncRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      manager.syncUserProfile(user)
    }
    assertTrue("Expected syncUserProfile to succeed, got ${syncRes.exceptionOrNull()}", syncRes.isSuccess)

    val fetchRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      manager.fetchUserProfile()
    }
    assertTrue(fetchRes.isSuccess)
    assertEquals("Alice Sharma", fetchRes.getOrNull()?.get("name"))
  }

  @Test
  fun createPost_authenticatedOwner_createsAndObservesPost() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val manager = FirestoreManager(firestore, auth)

    val createRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      manager.createPost("Testing VEXORA real Firestore persistence!")
    }
    assertTrue("Expected createPost to succeed, got ${createRes.exceptionOrNull()}", createRes.isSuccess)
    val postId = createRes.getOrThrow()
    assertTrue(postId.isNotEmpty())

    val observed = withTimeout(FLOW_TIMEOUT_MS) {
      manager.observeUserPosts().first { list -> list.any { it.id == postId } }
    }
    assertTrue(observed.any { it.id == postId })
  }

  @Test
  fun saveSecuritySettings_authenticatedOwner_succeedsAndCrossUserWriteDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceManager = FirestoreManager(firestore, auth)
    val saveRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceManager.saveSecuritySettings(
        twoFactorEnabled = true,
        twoFactorMethod = "SMS OTP",
        privateAccount = false,
        suspiciousAlertsEnabled = true,
        commentControl = "EVERYONE"
      )
    }
    assertTrue(saveRes.isSuccess)

    // Switch to Bob and verify Bob cannot overwrite Alice's security config
    signInTestUser(BOB_EMAIL)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(aliceUid)
          .collection("security").document("config")
          .set(
            mapOf(
              "userId" to aliceUid,
              "twoFactorEnabled" to false,
              "privateAccount" to true
            )
          )
          .await()
      }
      fail("Expected PERMISSION_DENIED when Bob writes Alice's security config")
    } catch (e: Exception) {
      val firestoreEx = e.unwrapFirestoreException()
      assertNotNull("Expected FirebaseFirestoreException, got $e", firestoreEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx!!.code)
    }
  }

  @Test
  fun observeUserPosts_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val manager = FirestoreManager(firestore, auth)

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        manager.observeUserPosts().first()
      }
      fail("Expected PERMISSION_DENIED for unauthenticated user")
    } catch (e: Exception) {
      val firestoreEx = e.unwrapFirestoreException()
      assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx!!.code)
    }
  }

  @Test
  fun verifyFirestoreReadAndWrite_authenticatedOwner_verifiesBothWriteAndRead() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val manager = FirestoreManager(firestore, auth)
    val user = User(id = uid, name = "Aarav Spark", username = "aarav_spark")

    val report = withTimeout(DEFAULT_TIMEOUT_MS) {
      manager.verifyFirestoreReadAndWrite(user)
    }
    assertTrue("Expected writeSuccess=true, got ${report.writeMessage}", report.writeSuccess)
    assertTrue("Expected readSuccess=true, got ${report.readMessage}", report.readSuccess)
    assertTrue(report.readMessage.contains("Aarav Spark"))
  }

  @Test
  fun deletePost_authenticatedOwner_deletesDocument() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val manager = FirestoreManager(firestore, auth)

    val postId = withTimeout(DEFAULT_TIMEOUT_MS) {
      manager.createPost("Post to be removed by admin moderation")
    }.getOrThrow()

    val deleteRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      manager.deletePost(postId)
    }
    assertTrue("Expected deletePost to succeed, got ${deleteRes.exceptionOrNull()}", deleteRes.isSuccess)

    val remaining = withTimeout(DEFAULT_TIMEOUT_MS) {
      manager.getUserPosts()
    }.getOrThrow()
    assertTrue(remaining.none { it.id == postId })
  }

  @Test
  fun completeProfileRegistration_reservesUniqueUsernameAndBlocksDuplicateClaim() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceManager = FirestoreManager(firestore, auth)

    val regAlice = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceManager.completeProfileRegistration(
        User(
          id = aliceUid,
          name = "Alice Vexora",
          username = "Alice.Official",
          phone = "+919999999999"
        )
      )
    }
    assertTrue("Expected Alice registration to succeed, got ${regAlice.exceptionOrNull()}", regAlice.isSuccess)
    assertEquals("alice.official", regAlice.getOrThrow().username)

    // Switch to Bob and verify Bob CANNOT claim Alice's username
    val bobUid = signInTestUser(BOB_EMAIL)
    val bobManager = FirestoreManager(firestore, auth)
    val regBob = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobManager.completeProfileRegistration(
        User(
          id = bobUid,
          name = "Bob Copycat",
          username = "alice.official"
        )
      )
    }
    assertTrue("Expected duplicate username claim by Bob to fail", regBob.isFailure)
  }

  private companion object {
    const val ALICE_EMAIL = "alice_vexora@test.com"
    const val BOB_EMAIL = "bob_vexora@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
