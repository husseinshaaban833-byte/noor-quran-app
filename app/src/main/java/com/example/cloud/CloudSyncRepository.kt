package com.example.cloud

import android.content.Context
import androidx.annotation.Keep
import com.example.R
import com.example.core.PrefsDataStore
import com.example.data.local.BookmarkEntity
import com.example.data.local.MemorizationPlanEntity
import com.example.data.local.NoorDao
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

@Keep
@IgnoreExtraProperties
data class CloudUserProfile(
    @com.google.firebase.firestore.DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val lastSurah: Long = 1L,
    val lastAyah: Long = 1L,
    val lastPage: Long = 1L,
    val khatmahCount: Long = 0L,
    val tasbeehTotal: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudBookmark(
    @com.google.firebase.firestore.DocumentId val id: String = "",
    val userId: String = "",
    val surahNumber: Long = 1L,
    val ayahNumber: Long = 1L,
    val pageNumber: Long = 1L,
    val label: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudMemorizationPlan(
    @com.google.firebase.firestore.DocumentId val id: String = "",
    val userId: String = "",
    val surahNumber: Long = 1L,
    val surahName: String = "",
    val dailyAyahTarget: Long = 5L,
    val completedAyahs: Long = 0L,
    val totalAyahs: Long = 1L,
    val status: String = "ACTIVE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

class CloudSyncRepository(
    private val db: FirebaseFirestore,
    private val dao: NoorDao,
    private val prefs: PrefsDataStore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context, dao: NoorDao, prefs: PrefsDataStore) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        dao = dao,
        prefs = prefs
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول بحساب Google قبل المزامنة السحابية.")

    fun observeCloudProfile(userId: String): Flow<CloudUserProfile?> = flow {
        val path = "users/$userId/profile/main"
        emitAll(
            db.collection("users").document(userId).collection("profile").document("main")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        CloudUserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
        )
    }

    fun observeCloudBookmarks(userId: String): Flow<List<CloudBookmark>> = flow {
        emitAll(
            db.collection("users").document(userId).collection("bookmarks")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudBookmark::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
        )
    }

    suspend fun backupLocalToCloud(): Result<Unit> {
        var currentPath = "users/unknown/profile/main"
        return try {
            val uid = requireUserId()
            val userPrefs = prefs.preferencesFlow.first()
            val displayName = (auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "مستخدم نور القرآن").take(100)

            val profileRef = db.collection("users").document(uid).collection("profile").document("main")
            currentPath = profileRef.path
            val existingProfile = profileRef.get().await()
            if (existingProfile.exists()) {
                profileRef.update(
                    mapOf(
                        "displayName" to displayName,
                        "lastSurah" to userPrefs.lastSurah.coerceIn(1, 114),
                        "lastAyah" to userPrefs.lastAyah.coerceIn(1, 286),
                        "lastPage" to userPrefs.lastPage.coerceIn(1, 604),
                        "khatmahCount" to userPrefs.khatmahCount.coerceIn(0, 100000),
                        "tasbeehTotal" to userPrefs.tasbeehTotal.coerceIn(0, 100000000),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                profileRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to displayName,
                        "lastSurah" to userPrefs.lastSurah.coerceIn(1, 114),
                        "lastAyah" to userPrefs.lastAyah.coerceIn(1, 286),
                        "lastPage" to userPrefs.lastPage.coerceIn(1, 604),
                        "khatmahCount" to userPrefs.khatmahCount.coerceIn(0, 100000),
                        "tasbeehTotal" to userPrefs.tasbeehTotal.coerceIn(0, 100000000),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Sync bookmarks
            val localBookmarks = dao.getAllBookmarksOnce()
            for (bm in localBookmarks) {
                val docId = "bm_${bm.surahNumber}_${bm.ayahNumber}"
                val bmRef = db.collection("users").document(uid).collection("bookmarks").document(docId)
                currentPath = bmRef.path
                val existingBm = bmRef.get().await()
                val label = bm.label.ifBlank { "سورة ${bm.surahNumber} آية ${bm.ayahNumber}" }.take(100)
                if (existingBm.exists()) {
                    bmRef.update(
                        mapOf(
                            "surahNumber" to bm.surahNumber.coerceIn(1, 114),
                            "ayahNumber" to bm.ayahNumber.coerceIn(1, 286),
                            "pageNumber" to bm.pageNumber.coerceIn(1, 604),
                            "label" to label,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    bmRef.set(
                        mapOf(
                            "userId" to uid,
                            "surahNumber" to bm.surahNumber.coerceIn(1, 114),
                            "ayahNumber" to bm.ayahNumber.coerceIn(1, 286),
                            "pageNumber" to bm.pageNumber.coerceIn(1, 604),
                            "label" to label,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            // Sync memorization plans
            val localPlans = dao.getAllPlansOnce()
            for (plan in localPlans) {
                val docId = "plan_${plan.id}"
                val planRef = db.collection("users").document(uid).collection("memorization_plans").document(docId)
                currentPath = planRef.path
                val existingPlan = planRef.get().await()
                val safeTotal = plan.totalAyahs.coerceIn(1, 286)
                val safeCompleted = plan.completedAyahs.coerceIn(0, safeTotal)
                val status = if (safeCompleted >= safeTotal) "COMPLETED" else "ACTIVE"
                if (existingPlan.exists()) {
                    val prevStatus = existingPlan.getString("status") ?: "ACTIVE"
                    if (prevStatus != "COMPLETED") {
                        planRef.update(
                            mapOf(
                                "dailyAyahTarget" to plan.dailyAyahTarget.coerceIn(1, 286),
                                "completedAyahs" to safeCompleted,
                                "status" to status,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                } else {
                    planRef.set(
                        mapOf(
                            "userId" to uid,
                            "surahNumber" to plan.surahNumber.coerceIn(1, 114),
                            "surahName" to plan.surahName.ifBlank { "سورة ${plan.surahNumber}" }.take(80),
                            "dailyAyahTarget" to plan.dailyAyahTarget.coerceIn(1, 286),
                            "completedAyahs" to safeCompleted,
                            "totalAyahs" to safeTotal,
                            "status" to status,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, currentPath)
            Result.failure(e)
        }
    }

    suspend fun restoreFromCloud(): Result<Int> {
        var currentPath = "users/unknown"
        return try {
            val uid = requireUserId()
            val profileRef = db.collection("users").document(uid).collection("profile").document("main")
            currentPath = profileRef.path
            val profileSnap = profileRef.get().await()
            var restoredCount = 0

            profileSnap.toObject(CloudUserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.let { p ->
                prefs.setKhatmahAndTasbeehFromCloud(
                    lastSurah = p.lastSurah.toInt(),
                    lastAyah = p.lastAyah.toInt(),
                    lastPage = p.lastPage.toInt(),
                    khatmahCount = p.khatmahCount.toInt(),
                    tasbeehTotal = p.tasbeehTotal.toInt()
                )
                restoredCount++
            }

            val bmCol = db.collection("users").document(uid).collection("bookmarks")
            currentPath = bmCol.path
            val bmDocs = bmCol.get().await().toObjects(CloudBookmark::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val existingLocal = dao.getAllBookmarksOnce().map { it.surahNumber to it.ayahNumber }.toSet()
            bmDocs.forEach { bm ->
                val pair = bm.surahNumber.toInt() to bm.ayahNumber.toInt()
                if (pair !in existingLocal) {
                    dao.insertBookmark(
                        BookmarkEntity(
                            surahNumber = pair.first,
                            ayahNumber = pair.second,
                            pageNumber = bm.pageNumber.toInt(),
                            label = bm.label
                        )
                    )
                    restoredCount++
                }
            }

            val planCol = db.collection("users").document(uid).collection("memorization_plans")
            currentPath = planCol.path
            val planDocs = planCol.get().await().toObjects(CloudMemorizationPlan::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val existingPlans = dao.getAllPlansOnce().map { it.surahNumber }.toSet()
            planDocs.forEach { cp ->
                if (cp.surahNumber.toInt() !in existingPlans) {
                    dao.insertPlan(
                        MemorizationPlanEntity(
                            surahNumber = cp.surahNumber.toInt(),
                            surahName = cp.surahName,
                            dailyAyahTarget = cp.dailyAyahTarget.toInt(),
                            completedAyahs = cp.completedAyahs.toInt(),
                            totalAyahs = cp.totalAyahs.toInt(),
                            status = cp.status
                        )
                    )
                    restoredCount++
                }
            }
            Result.success(restoredCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, currentPath)
            Result.failure(e)
        }
    }

    suspend fun deleteAllCloudUserData(): Result<Unit> {
        var currentPath = "users/unknown"
        return try {
            val uid = requireUserId()
            val userDoc = db.collection("users").document(uid)
            for (col in listOf("bookmarks", "memorization_plans", "profile")) {
                currentPath = "users/$uid/$col"
                val docs = userDoc.collection(col).get().await()
                for (doc in docs.documents) {
                    doc.reference.delete().await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, currentPath)
            Result.failure(e)
        }
    }

    suspend fun exportLocalBackupJson(): String {
        val userPrefs = prefs.preferencesFlow.first()
        val bookmarks = dao.getAllBookmarksOnce()
        val plans = dao.getAllPlansOnce()

        val root = JSONObject().apply {
            put("app", "Noor Al-Quran")
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("lastSurah", userPrefs.lastSurah)
            put("lastAyah", userPrefs.lastAyah)
            put("lastPage", userPrefs.lastPage)
            put("khatmahCount", userPrefs.khatmahCount)
            put("tasbeehTotal", userPrefs.tasbeehTotal)
            put("bookmarks", JSONArray().apply {
                bookmarks.forEach { bm ->
                    put(JSONObject().apply {
                        put("surahNumber", bm.surahNumber)
                        put("ayahNumber", bm.ayahNumber)
                        put("pageNumber", bm.pageNumber)
                        put("label", bm.label)
                    })
                }
            })
            put("memorizationPlans", JSONArray().apply {
                plans.forEach { p ->
                    put(JSONObject().apply {
                        put("surahNumber", p.surahNumber)
                        put("surahName", p.surahName)
                        put("dailyAyahTarget", p.dailyAyahTarget)
                        put("completedAyahs", p.completedAyahs)
                        put("totalAyahs", p.totalAyahs)
                        put("status", p.status)
                    })
                }
            })
        }
        return root.toString(2)
    }

    suspend fun importLocalBackupJson(jsonString: String): Result<Int> = try {
        val root = JSONObject(jsonString)
        var count = 0
        prefs.setKhatmahAndTasbeehFromCloud(
            lastSurah = root.optInt("lastSurah", 1),
            lastAyah = root.optInt("lastAyah", 1),
            lastPage = root.optInt("lastPage", 1),
            khatmahCount = root.optInt("khatmahCount", 0),
            tasbeehTotal = root.optInt("tasbeehTotal", 0)
        )
        val bmArr = root.optJSONArray("bookmarks")
        if (bmArr != null) {
            for (i in 0 until bmArr.length()) {
                val o = bmArr.getJSONObject(i)
                dao.insertBookmark(
                    BookmarkEntity(
                        surahNumber = o.getInt("surahNumber"),
                        ayahNumber = o.getInt("ayahNumber"),
                        pageNumber = o.getInt("pageNumber"),
                        label = o.getString("label")
                    )
                )
                count++
            }
        }
        val plansArr = root.optJSONArray("memorizationPlans")
        if (plansArr != null) {
            for (i in 0 until plansArr.length()) {
                val o = plansArr.getJSONObject(i)
                dao.insertPlan(
                    MemorizationPlanEntity(
                        surahNumber = o.getInt("surahNumber"),
                        surahName = o.getString("surahName"),
                        dailyAyahTarget = o.getInt("dailyAyahTarget"),
                        completedAyahs = o.getInt("completedAyahs"),
                        totalAyahs = o.getInt("totalAyahs"),
                        status = o.optString("status", "ACTIVE")
                    )
                )
                count++
            }
        }
        Result.success(count)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
