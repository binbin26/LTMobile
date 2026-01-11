package smart.study.planner.data.local.notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import smart.study.planner.data.local.database.AppDatabase
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Worker for sending deadline 1-day-before reminder notifications
 * Runs when the deadline is 1 day away
 */
class DeadlineReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "DeadlineReminderWorker"
        const val WORK_TAG_DEADLINE_REMINDER = "deadline_reminder_"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val eventId = inputData.getString("eventId") ?: return@withContext Result.failure()
            val eventTitle = inputData.getString("eventTitle") ?: "Sự kiện"
            val deadlineTime = inputData.getLong("deadlineTime", 0)

            if (deadlineTime == 0L) return@withContext Result.failure()

            Log.d(TAG, "Sending deadline reminder for event: $eventId - $eventTitle")

            // Format deadline time for display
            val deadlineDateTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(deadlineTime),
                ZoneId.systemDefault()
            )
            val deadlineDate = String.format(
                "%02d/%02d/%04d lúc %02d:%02d",
                deadlineDateTime.dayOfMonth,
                deadlineDateTime.monthValue,
                deadlineDateTime.year,
                deadlineDateTime.hour,
                deadlineDateTime.minute
            )

            // Show notification
            val notificationManager = NotificationManager(applicationContext)
            notificationManager.showDeadlineReminderNotification(eventId, eventTitle, deadlineDate)

            Log.d(TAG, "Deadline reminder sent successfully for event: $eventId")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending deadline reminder: ${e.message}", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
