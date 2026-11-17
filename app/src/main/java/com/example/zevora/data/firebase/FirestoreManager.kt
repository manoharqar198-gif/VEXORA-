package com.example.zevora.data.firebase

import android.content.Context
import android.util.Log
import com.example.R
import com.example.zevora.model.Post
import com.example.zevora.model.User
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class FirestoreRoundTripReport(
  val writeSuccess: Boolean,
  val writeMessage: String,
  val readSuccess: Boolean,
  val readMessage: String
)

class FirestoreManager(
  val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth
) {

  constructor(context: Context) : this(
    db = FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    auth = Firebase.auth
  )

  enum class OperationType {
    CREATE,
    UPDATE,
    DELETE,
    GET,
    LIST,
    WRITE
  }

  fun currentUserId(): String? = auth.currentUser?.uid

  private fun handleFirestoreError(
    error: Exception,
    operationType: OperationType,
    path: String?
  ) {
    val user = auth.currentUser
    val errJson = JSONObject().apply {
      put("error", error.localizedMessage ?: error.toString())
      put("operationType", operationType.name.lowercase())
      put("path", path ?: "")
      put("authInfo", JSONObject().apply {
        put("userId", user?.uid ?: "")
        put("email", user?.email ?: "")
        put("phoneNumber", user?.phoneNumber ?: "")
        put("isEmailVerified", user?.isEmailVerified ?: false)
      })
    }
    Log.e("FirestoreError", errJson.toString(), error)
  }

  suspend fun syncUserProfile(user: User): Result<Unit> {
    val uid = currentUserId()
      ?: return Result.failure(IllegalStateException("Not authenticated with Firebase Auth"))
    val path = "users/$uid"
    val docRef = db.collection("users").document(uid)
    val safeName = user.name.ifBlank { "VEXORA User" }.take(100)
    val safeUsername = normalizeUsername(user.username).ifBlank { "user_${uid.take(6)}" }.take(50)
    val data = mapOf(
      "userId" to uid,
      "name" to safeName,
      "username" to safeUsername,
      "avatarUrl" to user.avatarUrl,
      "bio" to user.bio,
      "phone" to user.phone,
      "email" to user.email,
      "isPrivate" to user.isPrivate,
      "followersCount" to user.followersCount,
      "followingCount" to user.followingCount,
      "postsCount" to user.postsCount,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    return try {
      withTimeout(8000L) {
        docRef.set(data, SetOptions.merge()).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  fun normalizeUsername(raw: String): String =
    raw.trim().lowercase().replace(Regex("[^a-z0-9_.]"), "").take(30)

  suspend fun isUsernameAvailable(rawUsername: String): Result<Boolean> {
    val uid = currentUserId()
      ?: return Result.failure(IllegalStateException("Not authenticated with Firebase Auth"))
    val clean = normalizeUsername(rawUsername)
    if (clean.length < 3) {
      return Result.success(false)
    }
    val path = "usernames/$clean"
    return try {
      val snapshot = withTimeout(8000L) {
        db.collection("usernames").document(clean).get(Source.SERVER).await()
      }
      val owner = snapshot.getString("userId")
      Result.success(!snapshot.exists() || owner == uid)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, path)
      Result.failure(e)
    }
  }

  suspend fun reserveUsername(rawUsername: String): Result<Unit> {
    val uid = currentUserId()
      ?: return Result.failure(IllegalStateException("Not authenticated with Firebase Auth"))
    val clean = normalizeUsername(rawUsername)
    if (clean.length < 3) {
      return Result.failure(IllegalArgumentException("Username कम से कम 3 अक्षर (a-z, 0-9, _, .) का होना चाहिए।"))
    }
    val path = "usernames/$clean"
    val data = mapOf(
      "userId" to uid,
      "username" to clean,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    return try {
      withTimeout(8000L) {
        db.collection("usernames").document(clean).set(data).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun completeProfileRegistration(user: User): Result<User> {
    val uid = currentUserId()
      ?: return Result.failure(IllegalStateException("Not authenticated with Firebase Auth"))
    val cleanUsername = normalizeUsername(user.username)
    if (cleanUsername.length < 3) {
      return Result.failure(IllegalArgumentException("Username कम से कम 3 अक्षर (a-z, 0-9, _, .) का होना चाहिए।"))
    }
    val availability = isUsernameAvailable(cleanUsername)
    if (availability.isFailure) {
      return Result.failure(availability.exceptionOrNull() ?: IllegalStateException("Username जांच विफल रही।"))
    }
    if (availability.getOrNull() != true) {
      return Result.failure(IllegalStateException("यह Username (@$cleanUsername) पहले से किसी और ने लिया है। कृपया दूसरा चुनें।"))
    }
    val reserveRes = reserveUsername(cleanUsername)
    if (reserveRes.isFailure) {
      return Result.failure(reserveRes.exceptionOrNull() ?: IllegalStateException("Username सुरक्षित नहीं हो सका।"))
    }
    val updatedUser = user.copy(
      id = uid,
      name = user.name.trim().ifBlank { "VEXORA User" },
      username = cleanUsername
    )
    val syncRes = syncUserProfile(updatedUser)
    return if (syncRes.isSuccess) {
      Result.success(updatedUser)
    } else {
      Result.failure(syncRes.exceptionOrNull() ?: IllegalStateException("Profile Cloud Firestore में सेव नहीं हो सका।"))
    }
  }

  suspend fun fetchUserProfile(): Result<Map<String, Any>?> {
    val uid = currentUserId()
      ?: return Result.failure(IllegalStateException("Not authenticated with Firebase Auth"))
    val path = "users/$uid"
    return try {
      val snapshot = withTimeout(8000L) {
        db.collection("users").document(uid).get(Source.SERVER).await()
      }
      Result.success(if (snapshot.exists()) snapshot.data else null)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, path)
      Result.failure(e)
    }
  }

  suspend fun fetchUsersDirectory(limit: Long = 20L): Result<List<User>> {
    if (currentUserId() == null) {
      return Result.failure(IllegalStateException("Not authenticated with Firebase Auth"))
    }
    return try {
      val snapshot = withTimeout(8000L) {
        db.collection("users").limit(limit).get(Source.SERVER).await()
      }
      val users = snapshot.documents.mapNotNull { doc ->
        val userId = doc.getString("userId") ?: doc.id
        val name = doc.getString("name") ?: return@mapNotNull null
        val username = doc.getString("username") ?: "user_${userId.take(6)}"
        User(
          id = userId,
          name = name,
          username = username,
          avatarUrl = doc.getString("avatarUrl") ?: "",
          bio = doc.getString("bio") ?: "",
          phone = doc.getString("phone") ?: "",
          email = doc.getString("email") ?: "",
          isPrivate = doc.getBoolean("isPrivate") ?: false,
          followersCount = (doc.getLong("followersCount") ?: 0L).toInt(),
          followingCount = (doc.getLong("followingCount") ?: 0L).toInt(),
          postsCount = (doc.getLong("postsCount") ?: 0L).toInt()
        )
      }
      Result.success(users)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, "users")
      Result.failure(e)
    }
  }

  suspend fun createPost(content: String): Result<String> {
    val uid = currentUserId()
      ?: return Result.failure(IllegalStateException("Not authenticated with Firebase Auth"))
    val trimmed = content.trim()
    if (trimmed.isEmpty() || trimmed.length > 2000) {
      return Result.failure(IllegalArgumentException("Post content must be 1..2000 characters"))
    }
    val docRef = db.collection("users").document(uid).collection("posts").document()
    val path = "users/$uid/posts/${docRef.id}"
    val payload = mapOf(
      "userId" to uid,
      "content" to trimmed,
      "authorName" to (auth.currentUser?.displayName ?: "VEXORA Creator"),
      "authorUsername" to (auth.currentUser?.email?.substringBefore("@") ?: "creator_${uid.take(5)}"),
      "likesCount" to 0,
      "commentsCount" to 0,
      "createdAt" to FieldValue.serverTimestamp()
    )
    return try {
      withTimeout(8000L) {
        docRef.set(payload).await()
      }
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun deletePost(postId: String): Result<Unit> {
    val uid = currentUserId()
      ?: return Result.failure(IllegalStateException("Not authenticated with Firebase Auth"))
    val path = "users/$uid/posts/$postId"
    return try {
      withTimeout(8000L) {
        db.collection("users").document(uid).collection("posts").document(postId).delete().await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  suspend fun getUserPosts(): Result<List<Post>> {
    val uid = currentUserId() ?: "unauthenticated"
    val path = "users/$uid/posts"
    return try {
      val snapshot = withTimeout(8000L) {
        db.collection("users").document(uid).collection("posts")
          .orderBy("createdAt", Query.Direction.DESCENDING)
          .get()
          .await()
      }
      val posts = snapshot.documents.mapNotNull { doc ->
        val content = doc.getString("content") ?: return@mapNotNull null
        val authorName = doc.getString("authorName") ?: "VEXORA Creator"
        val authorUsername = doc.getString("authorUsername") ?: "creator"
        val likes = (doc.getLong("likesCount") ?: 0L).toInt()
        val comments = (doc.getLong("commentsCount") ?: 0L).toInt()
        Post(
          id = doc.id,
          author = User(id = doc.getString("userId") ?: uid, name = authorName, username = authorUsername),
          content = content,
          timestamp = "Cloud synced",
          likesCount = likes,
          commentsCount = comments
        )
      }
      Result.success(posts)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, path)
      Result.failure(e)
    }
  }

  fun observeUserPosts(): Flow<List<Post>> {
    val uid = currentUserId() ?: "unauthenticated"
    return db.collection("users").document(uid).collection("posts")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .snapshots()
      .map { snapshot ->
        snapshot.documents.mapNotNull { doc ->
          val content = doc.getString("content") ?: return@mapNotNull null
          val authorName = doc.getString("authorName") ?: "VEXORA Creator"
          val authorUsername = doc.getString("authorUsername") ?: "creator"
          val likes = (doc.getLong("likesCount") ?: 0L).toInt()
          val comments = (doc.getLong("commentsCount") ?: 0L).toInt()
          Post(
            id = doc.id,
            author = User(id = doc.getString("userId") ?: uid, name = authorName, username = authorUsername),
            content = content,
            timestamp = "Cloud synced",
            likesCount = likes,
            commentsCount = comments
          )
        }
      }
  }

  suspend fun saveSecuritySettings(
    twoFactorEnabled: Boolean,
    twoFactorMethod: String,
    privateAccount: Boolean,
    suspiciousAlertsEnabled: Boolean,
    commentControl: String
  ): Result<Unit> {
    val uid = currentUserId()
      ?: return Result.failure(IllegalStateException("Not authenticated with Firebase Auth"))
    val path = "users/$uid/security/config"
    val docRef = db.collection("users").document(uid).collection("security").document("config")
    val data = mapOf(
      "userId" to uid,
      "twoFactorEnabled" to twoFactorEnabled,
      "twoFactorMethod" to twoFactorMethod,
      "privateAccount" to privateAccount,
      "suspiciousAlertsEnabled" to suspiciousAlertsEnabled,
      "commentControl" to commentControl,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    return try {
      withTimeout(8000L) {
        docRef.set(data, SetOptions.merge()).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  /**
   * Executes genuine live Firestore Write + Server Read verification.
   * Never fakes success: reports separate Write and Server Read outcomes.
   */
  suspend fun verifyFirestoreReadAndWrite(user: User): FirestoreRoundTripReport {
    val uid = currentUserId()
    if (uid == null) {
      val msg = "Google Sign-In required for live Firestore write/read test"
      return FirestoreRoundTripReport(
        writeSuccess = false,
        writeMessage = msg,
        readSuccess = false,
        readMessage = msg
      )
    }

    val writeRes = syncUserProfile(user)
    if (writeRes.isFailure) {
      val err = writeRes.exceptionOrNull()?.localizedMessage ?: "Firestore write failed"
      return FirestoreRoundTripReport(
        writeSuccess = false,
        writeMessage = err,
        readSuccess = false,
        readMessage = "Skipped because write failed"
      )
    }

    val writeOkMsg = "Write verified at users/$uid"
    return try {
      val snapshot = withTimeout(8000L) {
        db.collection("users").document(uid).get(Source.SERVER).await()
      }
      if (snapshot.exists()) {
        val project = try {
          FirebaseApp.getInstance().options.projectId ?: "gen-lang-client-0433994845"
        } catch (_: Exception) {
          "emulator"
        }
        FirestoreRoundTripReport(
          writeSuccess = true,
          writeMessage = writeOkMsg,
          readSuccess = true,
          readMessage = "Server Read verified (project=$project, name=${snapshot.getString("name")})"
        )
      } else {
        FirestoreRoundTripReport(
          writeSuccess = true,
          writeMessage = writeOkMsg,
          readSuccess = false,
          readMessage = "Document missing on server after write"
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: FirebaseFirestoreException) {
      handleFirestoreError(e, OperationType.GET, "users/$uid")
      FirestoreRoundTripReport(
        writeSuccess = true,
        writeMessage = writeOkMsg,
        readSuccess = false,
        readMessage = "${e.code.name}: ${e.localizedMessage}"
      )
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, "users/$uid")
      FirestoreRoundTripReport(
        writeSuccess = true,
        writeMessage = writeOkMsg,
        readSuccess = false,
        readMessage = e.localizedMessage ?: e.javaClass.simpleName
      )
    }
  }
}
