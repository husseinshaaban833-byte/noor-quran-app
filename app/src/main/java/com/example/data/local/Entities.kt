package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val surahNumber: Int,
    val ayahNumber: Int,
    val pageNumber: Int,
    val label: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "memorization_plans")
data class MemorizationPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val surahNumber: Int,
    val surahName: String,
    val dailyAyahTarget: Int,
    val completedAyahs: Int,
    val totalAyahs: Int,
    val status: String = "ACTIVE", // ACTIVE, COMPLETED
    val reviewDaysMask: Int = 127,
    val reminderHour: Int = 7,
    val easeFactor: Float = 2.5f,
    val intervalDays: Int = 1,
    val lastReviewedAt: Long = System.currentTimeMillis(),
    val nextReviewAt: Long = System.currentTimeMillis() + 86_400_000L,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recitation_attempts")
data class RecitationAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val surahNumber: Int,
    val fromAyah: Int,
    val toAyah: Int,
    val accuracyPercent: Int,
    val omittedWords: Int,
    val addedWords: Int,
    val replacedWords: Int,
    val detailsJson: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "khatmah_plans")
data class KhatmahPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val targetDays: Int,
    val pagesCompleted: Int,
    val totalPages: Int = 604,
    val completed: Boolean = false,
    val startDateMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloaded_items")
data class DownloadedItemEntity(
    @PrimaryKey val id: String,
    val type: String, // "AUDIO_SURAH", "TAFSIR", "MUSHAF_PAGE"
    val title: String,
    val reciterOrSourceId: String,
    val surahOrPage: Int,
    val localPath: String,
    val sizeBytes: Long,
    val sha256: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasbeeh_logs")
data class TasbeehLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dhikr: String,
    val count: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "adhkar_progress")
data class AdhkarProgressEntity(
    @PrimaryKey val id: String, // "${dateKey}_${dhikrId}"
    val dhikrId: String,
    val dateKey: String,
    val currentCount: Int,
    val completed: Boolean
)
