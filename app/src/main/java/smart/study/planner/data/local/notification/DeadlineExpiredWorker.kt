package smart.study.planner.data.local.notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker for sending deadline expired notifications (repeating)
 * Notifies user when the deadline has passed
 * This worker is designed to run periodically to check expired deadlines
 */
class DeadlineExpiredWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "DeadlineExpiredWorker"
        const val WORK_TAG_DEADLINE_EXPIRED = "deadline_expired_"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val eventId = inputData.getString("eventId") ?: return@withContext Result.failure()
            val eventTitle = inputData.getString("eventTitle") ?: "Sự kiện"

            Log.d(TAG, "Sending deadline expired notification for event: $eventId - $eventTitle")

            // Show notification
            val notificationManager = NotificationManager(applicationContext)
            notificationManager.showDeadlineExpiredNotification(eventId, eventTitle)

            Log.d(TAG, "Deadline expired notification sent successfully for event: $eventId")
            
            // Return success - notification will be shown immediately
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending deadline expired notification: ${e.message}", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
