package smart.study.planner.data.local.notification

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import smart.study.planner.data.local.database.AppDatabase
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Worker for checking and scheduling event notifications
 */
class EventReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val eventDao = AppDatabase.getDatabase(context).eventDao()
    private val notificationManager = NotificationManager(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val eventId = inputData.getString("eventId") ?: return@withContext Result.failure()
            val eventTitle = inputData.getString("eventTitle") ?: "Sự kiện"
            val eventTime = inputData.getString("eventTime") ?: ""
            val eventLocation = inputData.getString("eventLocation")

            // Show notification
            notificationManager.showEventReminder(eventId, eventTitle, eventTime, eventLocation)

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_TAG_EVENT_REMINDER = "event_reminder_"
    }
}

/**
 * Manager for scheduling event notifications and reminders
 */
object ReminderScheduler {

    /**
     * Schedule event reminder notification
     * @param context Application context
     * @param eventId Event ID
     * @param eventTitle Event title
     * @param eventStartTime Event start time in milliseconds
     * @param reminderMinutesBefore Minutes before event to show reminder
     * @param eventLocation Event location (optional)
     */
    fun scheduleEventReminder(
        context: Context,
        eventId: String,
        eventTitle: String,
        eventStartTime: Long,
        reminderMinutesBefore: Int,
        eventLocation: String? = null
    ) {
        val reminderTime = eventStartTime - (reminderMinutesBefore * 60 * 1000)
        val currentTime = System.currentTimeMillis()

        if (reminderTime <= currentTime) {
            // Reminder time is in the past, show immediately
            val notificationManager = NotificationManager(context)
            val eventTime = formatEventTime(eventStartTime)
            notificationManager.showEventReminder(eventId, eventTitle, eventTime, eventLocation)
            return
        }

        val delayMillis = reminderTime - currentTime
        val delayMinutes = delayMillis / (60 * 1000)

        val inputData = workDataOf(
            "eventId" to eventId,
            "eventTitle" to eventTitle,
            "eventTime" to formatEventTime(eventStartTime),
            "eventLocation" to (eventLocation ?: "")
        )

        val reminderRequest = OneTimeWorkRequestBuilder<EventReminderWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(inputData)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                Duration.ofMinutes(15)
            )
            .addTag("${EventReminderWorker.WORK_TAG_EVENT_REMINDER}$eventId")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "reminder_$eventId",
            ExistingWorkPolicy.REPLACE,
            reminderRequest
        )
    }

    /**
     * Cancel event reminder
     */
    fun cancelEventReminder(context: Context, eventId: String) {
        WorkManager.getInstance(context).cancelUniqueWork("reminder_$eventId")
    }

    /**
     * Schedule periodic sync check
     */
    fun schedulePeriodicSync(context: Context) {
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            Duration.ofMinutes(15)
        ).setBackoffCriteria(
            BackoffPolicy.EXPONENTIAL,
            Duration.ofMinutes(15)
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "periodic_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    /**
     * Format event time for display
     */
    private fun formatEventTime(timeMillis: Long): String {
        val dateTime = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(timeMillis),
            ZoneId.systemDefault()
        )
        return "${dateTime.dayOfMonth}/${dateTime.monthValue} ${dateTime.hour}:${String.format("%02d", dateTime.minute)}"
    }
}

/**
 * Worker for syncing data with Firebase
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            // Sync logic will be called from repository
            // This is a placeholder for the periodic sync check
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
