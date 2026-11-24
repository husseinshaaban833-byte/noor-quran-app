package com.example.memorization

import com.example.data.local.MemorizationPlanEntity
import kotlin.math.ceil
import kotlin.math.roundToInt

data class KhatmahDailyPlan(
    val pagesPerDay: Int,
    val remainingPages: Int,
    val estimatedDaysLeft: Int
)

object SpacedRepetitionEngine {
    private const val DAY_MS = 86_400_000L

    /**
     * Updates a memorization plan's spaced repetition interval after a review session.
     * quality: 1 = Hard/Needs review, 3 = Good, 5 = Mastered
     */
    fun scheduleNextReview(plan: MemorizationPlanEntity, quality: Int, nowMs: Long = System.currentTimeMillis()): MemorizationPlanEntity {
        val q = quality.coerceIn(1, 5)
        val newEase = (plan.easeFactor + (0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f)))
            .coerceIn(1.3f, 3.0f)
        val newInterval = when {
            q < 3 -> 1
            plan.intervalDays <= 1 -> 2
            plan.intervalDays == 2 -> 6
            else -> (plan.intervalDays * newEase).roundToInt().coerceIn(1, 90)
        }
        return plan.copy(
            easeFactor = newEase,
            intervalDays = newInterval,
            lastReviewedAt = nowMs,
            nextReviewAt = nowMs + newInterval * DAY_MS
        )
    }

    fun calculateKhatmahWird(totalPages: Int = 604, pagesCompleted: Int, targetDays: Int): KhatmahDailyPlan {
        val safeDays = targetDays.coerceAtLeast(1)
        val remaining = (totalPages - pagesCompleted).coerceAtLeast(0)
        val pagesPerDay = if (remaining == 0) 0 else ceil(remaining.toDouble() / safeDays).toInt().coerceAtLeast(1)
        val estDays = if (pagesPerDay == 0) 0 else ceil(remaining.toDouble() / pagesPerDay).toInt()
        return KhatmahDailyPlan(
            pagesPerDay = pagesPerDay,
            remainingPages = remaining,
            estimatedDaysLeft = estDays
        )
    }
}
