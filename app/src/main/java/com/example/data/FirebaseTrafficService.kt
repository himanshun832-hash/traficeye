package com.example.data

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class FirebaseProjectConfig(
    val projectId: String = "trafficeye-99d78",
    val projectNumber: String = "1059894291798",
    val firebaseRtdbUrl: String = "https://trafficeye-99d78-default-rtdb.firebaseio.com",
    val storageBucket: String = "trafficeye-99d78.firebasestorage.app",
    val appId: String = "1:1059894291798:android:da5dc6e20516a70f2efaf7",
    val packageName: String = "com.trafficeye.app"
)

data class FirebaseStatus(
    val isConnected: Boolean = false,
    val projectId: String = "trafficeye-99d78",
    val currentUid: String? = null,
    val currentEmail: String? = null,
    val lastSyncMessage: String = "Local Database Ready",
    val lastSyncTimestamp: Long = System.currentTimeMillis()
)

data class FirebaseUserData(
    val uid: String,
    val email: String,
    val role: String, // "ADMIN_POLICE" or "VEHICLE_OWNER"
    val fullName: String,
    val badgeOrVehicleNo: String,
    val isActive: Boolean = true
)

class FirebaseTrafficService(private val context: Context? = null) {

    private val tag = "FirebaseTraffic"
    val config = FirebaseProjectConfig()

    private fun getFirebaseAuthSafe(): FirebaseAuth? {
        return try {
            if (context != null && FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            Log.w(tag, "Firebase Auth unavailable: ${e.message}")
            null
        }
    }

    private fun getFirestoreSafe(): FirebaseFirestore? {
        return try {
            if (context != null && FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            Log.w(tag, "Firestore unavailable: ${e.message}")
            null
        }
    }

    private val _status = MutableStateFlow(
        FirebaseStatus(
            isConnected = false,
            projectId = config.projectId,
            currentUid = null,
            currentEmail = null,
            lastSyncMessage = "System Initialized (Local Mode Ready)"
        )
    )
    val status = _status.asStateFlow()

    init {
        try {
            val safeAuth = getFirebaseAuthSafe()
            if (safeAuth != null) {
                val user = safeAuth.currentUser
                _status.value = FirebaseStatus(
                    isConnected = true,
                    projectId = config.projectId,
                    currentUid = user?.uid,
                    currentEmail = user?.email,
                    lastSyncMessage = if (user != null) "Cloud Synced (${user.email})" else "Firebase Cloud Connected"
                )
            }
        } catch (e: Throwable) {
            Log.w(tag, "Firebase status check skipped: ${e.message}")
        }
    }

    fun getCurrentFirebaseUser(): FirebaseUser? {
        return try {
            getFirebaseAuthSafe()?.currentUser
        } catch (e: Throwable) {
            null
        }
    }

    // Helper to await GMS tasks with coroutines
    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener { exception ->
            if (cont.isActive) cont.resumeWithException(exception)
        }
        addOnCanceledListener {
            if (cont.isActive) cont.cancel()
        }
    }

    suspend fun loginWithFirebase(email: String, passwordEntered: String): Result<FirebaseUserData> {
        val safeAuth = getFirebaseAuthSafe() ?: return Result.failure(Exception("Firebase Auth is not available"))
        return try {
            val authResult = safeAuth.signInWithEmailAndPassword(email.trim(), passwordEntered.trim()).awaitTask()
            val fbUser = authResult.user ?: throw Exception("Firebase authentication returned no user session")

            _status.value = _status.value.copy(
                isConnected = true,
                currentUid = fbUser.uid,
                currentEmail = fbUser.email,
                lastSyncMessage = "Authenticated via Firebase (${fbUser.email})",
                lastSyncTimestamp = System.currentTimeMillis()
            )

            // Attempt to retrieve profile from Firestore
            val userDoc = try {
                getFirestoreSafe()?.collection("users")?.document(fbUser.uid)?.get()?.awaitTask()
            } catch (e: Exception) {
                Log.w(tag, "Firestore get profile skipped: ${e.message}")
                null
            }

            val role = userDoc?.getString("role") ?: if (email.contains("admin") || email.contains("police") || email.contains("officer")) "ADMIN_POLICE" else "VEHICLE_OWNER"
            val fullName = userDoc?.getString("fullName") ?: fbUser.displayName ?: email.substringBefore("@")
            val badgeOrVehicle = userDoc?.getString("badgeOrVehicleNo") ?: if (role == "ADMIN_POLICE") "OFFICER-001" else "OD 02 AB 1234"
            val isActive = userDoc?.getBoolean("isActive") ?: true

            Result.success(
                FirebaseUserData(
                    uid = fbUser.uid,
                    email = fbUser.email ?: email,
                    role = role,
                    fullName = fullName,
                    badgeOrVehicleNo = badgeOrVehicle,
                    isActive = isActive
                )
            )
        } catch (e: Exception) {
            Log.e(tag, "Firebase login failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun registerWithFirebase(
        email: String,
        passwordEntered: String,
        role: String,
        fullName: String,
        badgeOrVehicle: String
    ): Result<FirebaseUserData> {
        val safeAuth = getFirebaseAuthSafe() ?: return Result.failure(Exception("Firebase Auth is not available"))
        return try {
            val authResult = safeAuth.createUserWithEmailAndPassword(email.trim(), passwordEntered.trim()).awaitTask()
            val fbUser = authResult.user ?: throw Exception("Firebase user registration returned null")

            val userData = hashMapOf(
                "uid" to fbUser.uid,
                "email" to (fbUser.email ?: email.trim()),
                "role" to role,
                "fullName" to fullName.trim(),
                "badgeOrVehicleNo" to badgeOrVehicle.trim(),
                "isActive" to true,
                "createdAt" to System.currentTimeMillis(),
                "projectId" to config.projectId
            )

            try {
                getFirestoreSafe()?.collection("users")?.document(fbUser.uid)?.set(userData, SetOptions.merge())?.awaitTask()
            } catch (e: Exception) {
                Log.w(tag, "Firestore set user failed: ${e.message}")
            }

            _status.value = _status.value.copy(
                isConnected = true,
                currentUid = fbUser.uid,
                currentEmail = fbUser.email,
                lastSyncMessage = "New user registered in Firebase ($email)",
                lastSyncTimestamp = System.currentTimeMillis()
            )

            Result.success(
                FirebaseUserData(
                    uid = fbUser.uid,
                    email = fbUser.email ?: email,
                    role = role,
                    fullName = fullName,
                    badgeOrVehicleNo = badgeOrVehicle,
                    isActive = true
                )
            )
        } catch (e: Exception) {
            Log.e(tag, "Firebase register failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<String> {
        val safeAuth = getFirebaseAuthSafe() ?: return Result.failure(Exception("Firebase Auth not connected"))
        return try {
            safeAuth.sendPasswordResetEmail(email.trim()).awaitTask()
            Result.success("Firebase password reset email dispatched to ${email.trim()}.")
        } catch (e: Exception) {
            Log.e(tag, "Firebase sendPasswordReset failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun syncViolationToFirestore(violation: ViolationRecord): Result<String> {
        val safeFirestore = getFirestoreSafe() ?: return Result.failure(Exception("Firestore not connected"))
        return try {
            val doc = hashMapOf(
                "challanNumber" to violation.challanNumber,
                "numberPlate" to violation.numberPlate,
                "vehicleModel" to violation.vehicleModel,
                "ownerName" to violation.ownerName,
                "violationType" to violation.violationType,
                "fineAmount" to violation.fineAmount,
                "isPaid" to violation.isPaid,
                "location" to violation.location,
                "timestamp" to violation.timestamp,
                "noiseDb" to violation.noiseDb,
                "speedKmh" to violation.speedKmh,
                "evidenceImageUrl" to violation.evidenceImageUrl,
                "verifiedByAdmin" to violation.verifiedByAdmin,
                "projectId" to config.projectId
            )
            safeFirestore.collection("violations").document(violation.challanNumber)
                .set(doc, SetOptions.merge())
                .awaitTask()
            Result.success("Violation #${violation.challanNumber} synced to Firestore.")
        } catch (e: Exception) {
            Log.w(tag, "Violation cloud sync: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateUserRoleInFirestore(uid: String, role: String, isActive: Boolean): Result<String> {
        val safeFirestore = getFirestoreSafe() ?: return Result.failure(Exception("Firestore not connected"))
        return try {
            val updateData = hashMapOf<String, Any>(
                "role" to role,
                "isActive" to isActive,
                "updatedAt" to System.currentTimeMillis()
            )
            safeFirestore.collection("users").document(uid).set(updateData, SetOptions.merge()).awaitTask()
            Result.success("User $uid permissions updated to $role in Firestore.")
        } catch (e: Exception) {
            Log.w(tag, "Update user role failed: ${e.message}")
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            getFirebaseAuthSafe()?.signOut()
            _status.value = _status.value.copy(
                currentUid = null,
                currentEmail = null,
                lastSyncMessage = "Firebase signed out"
            )
        } catch (e: Exception) {
            Log.e(tag, "Sign out error: ${e.message}")
        }
    }
}
