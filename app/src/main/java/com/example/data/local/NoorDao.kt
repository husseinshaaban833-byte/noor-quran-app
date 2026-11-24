package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoorDao {
    // Bookmarks
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    suspend fun getAllBookmarksOnce(): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    // Memorization plans
    @Query("SELECT * FROM memorization_plans ORDER BY createdAt DESC")
    fun observeMemorizationPlans(): Flow<List<MemorizationPlanEntity>>

    @Query("SELECT * FROM memorization_plans ORDER BY createdAt DESC")
    suspend fun getAllPlansOnce(): List<MemorizationPlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: MemorizationPlanEntity): Long

    @Update
    suspend fun updatePlan(plan: MemorizationPlanEntity)

    @Query("DELETE FROM memorization_plans WHERE id = :id")
    suspend fun deletePlanById(id: Long)

    // Recitation attempts
    @Query("SELECT * FROM recitation_attempts ORDER BY timestamp DESC LIMIT 50")
    fun observeRecitationAttempts(): Flow<List<RecitationAttemptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: RecitationAttemptEntity): Long

    // Khatmah plans
    @Query("SELECT * FROM khatmah_plans ORDER BY startDateMillis DESC")
    fun observeKhatmahPlans(): Flow<List<KhatmahPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKhatmahPlan(plan: KhatmahPlanEntity): Long

    @Update
    suspend fun updateKhatmahPlan(plan: KhatmahPlanEntity)

    @Query("DELETE FROM khatmah_plans WHERE id = :id")
    suspend fun deleteKhatmahPlanById(id: Long)

    // Downloaded items
    @Query("SELECT * FROM downloaded_items ORDER BY createdAt DESC")
    fun observeDownloads(): Flow<List<DownloadedItemEntity>>

    @Query("SELECT * FROM downloaded_items WHERE id = :id LIMIT 1")
    suspend fun getDownloadById(id: String): DownloadedItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(item: DownloadedItemEntity)

    @Query("DELETE FROM downloaded_items WHERE id = :id")
    suspend fun deleteDownloadById(id: String)

    // Tasbeeh logs
    @Query("SELECT * FROM tasbeeh_logs ORDER BY timestamp DESC LIMIT 30")
    fun observeTasbeehLogs(): Flow<List<TasbeehLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasbeehLog(log: TasbeehLogEntity)

    // Adhkar progress
    @Query("SELECT * FROM adhkar_progress WHERE dateKey = :dateKey")
    fun observeAdhkarProgress(dateKey: String): Flow<List<AdhkarProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAdhkarProgress(progress: AdhkarProgressEntity)
}
