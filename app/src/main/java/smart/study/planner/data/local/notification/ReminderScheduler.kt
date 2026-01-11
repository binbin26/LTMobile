package smart.study.planner.data.local.notification

import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.tasks.await
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
     * Schedule deadline 1-day-before reminder notification
     * Shows notification 24 hours before the deadline
     * @param context Application context
     * @param eventId Event ID
     * @param eventTitle Event title
     * @param deadlineTime Deadline time in milliseconds
     */
    fun scheduleDeadlineReminder(
        context: Context,
        eventId: String,
        eventTitle: String,
        deadlineTime: Long
    ) {
        val oneDayBeforeTime = deadlineTime - (24 * 60 * 60 * 1000L) // ✅ Fix: thêm L
        val currentTime = System.currentTimeMillis()

        Log.d("ReminderScheduler", "=== SCHEDULING DEADLINE REMINDER ===")
        Log.d("ReminderScheduler", "Event: $eventTitle (ID: $eventId)")
        Log.d("ReminderScheduler", "Deadline time: $deadlineTime")
        Log.d("ReminderScheduler", "1-day-before time: $oneDayBeforeTime")
        Log.d("ReminderScheduler", "Current time: $currentTime")
        Log.d("ReminderScheduler", "Delay: ${(oneDayBeforeTime - currentTime) / 1000 / 60} minutes")

        if (oneDayBeforeTime <= currentTime) {
            Log.d("ReminderScheduler", "Reminder time is in the past, showing immediately")
            val notificationManager = NotificationManager(context)
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
            notificationManager.showDeadlineReminderNotification(eventId, eventTitle, deadlineDate)
            return
        }

        val delayMillis = oneDayBeforeTime - currentTime

        // ✅ CRITICAL FIX: WorkManager has limit on delay duration
        // Max delay is ~15 days. For longer delays, use different approach
        val maxDelayMinutes = 20160L // 14 days in minutes
        val delayMinutes = (delayMillis / (60 * 1000)).coerceAtMost(maxDelayMinutes)

        Log.d("ReminderScheduler", "Scheduling with delay: $delayMinutes minutes")

        val inputData = workDataOf(
            "eventId" to eventId,
            "eventTitle" to eventTitle,
            "deadlineTime" to deadlineTime
        )

        val reminderRequest = OneTimeWorkRequestBuilder<DeadlineReminderWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(inputData)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                Duration.ofMinutes(15)
            )
            .addTag("${DeadlineReminderWorker.WORK_TAG_DEADLINE_REMINDER}$eventId")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "deadline_reminder_$eventId",
            ExistingWorkPolicy.REPLACE,
            reminderRequest
        )

        Log.d("ReminderScheduler", "✅ Deadline reminder scheduled successfully")
    }

    /**
     * Schedule deadline expired notification (repeating)
     * Shows notification when deadline expires and repeats every hour
     * @param context Application context
     * @param eventId Event ID
     * @param eventTitle Event title
     * @param deadlineTime Deadline time in milliseconds
     */
    fun scheduleDeadlineExpiredReminder(
        context: Context,
        eventId: String,
        eventTitle: String,
        deadlineTime: Long
    ) {
        val currentTime = System.currentTimeMillis()

        Log.d("ReminderScheduler", "=== SCHEDULING DEADLINE EXPIRED REMINDER ===")
        Log.d("ReminderScheduler", "Event: $eventTitle (ID: $eventId)")
        Log.d("ReminderScheduler", "Deadline time: $deadlineTime")
        Log.d("ReminderScheduler", "Current time: $currentTime")

        val startDelay = if (deadlineTime > currentTime) {
            val delayMillis = deadlineTime - currentTime
            val delayMinutes = delayMillis / (60 * 1000)
            Log.d("ReminderScheduler", "Deadline in future, delay: $delayMinutes minutes")
            delayMinutes
        } else {
            Log.d("ReminderScheduler", "Deadline already passed, showing immediately")
            0L
        }

        val inputData = workDataOf(
            "eventId" to eventId,
            "eventTitle" to eventTitle
        )

        // ✅ Create periodic work request
        val expiredRequest = PeriodicWorkRequestBuilder<DeadlineExpiredWorker>(
            1, TimeUnit.HOURS // ✅ Repeat every 1 hour
        )
            .setInitialDelay(startDelay, TimeUnit.MINUTES)
            .setInputData(inputData)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                Duration.ofMinutes(15)
            )
            .addTag("${DeadlineExpiredWorker.WORK_TAG_DEADLINE_EXPIRED}$eventId")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "deadline_expired_$eventId",
            ExistingPeriodicWorkPolicy.REPLACE,
            expiredRequest
        )

        Log.d("ReminderScheduler", "✅ Deadline expired reminder scheduled successfully")
    }

    /**
     * Cancel deadline reminder notification
     */
    fun cancelDeadlineReminder(context: Context, eventId: String) {
        WorkManager.getInstance(context).cancelUniqueWork("deadline_reminder_$eventId")
    }

    /**
     * Cancel deadline expired notification
     */
    fun cancelDeadlineExpiredReminder(context: Context, eventId: String) {
        WorkManager.getInstance(context).cancelUniqueWork("deadline_expired_$eventId")
    }

    /**
     * Schedule all deadline notifications for an event
     * Call this when creating or updating an event
     */
    fun scheduleAllDeadlineNotifications(
        context: Context,
        eventId: String,
        eventTitle: String,
        deadlineTime: Long
    ) {
        // Schedule 1-day-before reminder
        scheduleDeadlineReminder(context, eventId, eventTitle, deadlineTime)
        // Schedule expired notification (repeating)
        scheduleDeadlineExpiredReminder(context, eventId, eventTitle, deadlineTime)
    }

    /**
     * Cancel all deadline notifications for an event
     * Call this when deleting an event
     */
    fun cancelAllDeadlineNotifications(context: Context, eventId: String) {
        cancelDeadlineReminder(context, eventId)
        cancelDeadlineExpiredReminder(context, eventId)
    }

    /**
     * ✅ TEST FUNCTION: Show notification immediately for testing
     */
    fun testDeadlineNotification(context: Context, eventId: String = "test", eventTitle: String = "Test Event") {
        Log.d("ReminderScheduler", "=== TESTING DEADLINE NOTIFICATION ===")
        val notificationManager = NotificationManager(context)
        notificationManager.showDeadlineReminderNotification(
            eventId,
            eventTitle,
            "Ngay bây giờ"
        )
        Log.d("ReminderScheduler", "✅ Test notification shown")
    }

    /**
     * ✅ TEST FUNCTION: Show expired notification immediately for testing
     */
    fun testDeadlineExpiredNotification(context: Context, eventId: String = "test", eventTitle: String = "Test Event") {
        Log.d("ReminderScheduler", "=== TESTING DEADLINE EXPIRED NOTIFICATION ===")
        val notificationManager = NotificationManager(context)
        notificationManager.showDeadlineExpiredNotification(eventId, eventTitle)
        Log.d("ReminderScheduler", "✅ Test expired notification shown")
    }

    /**
     * ✅ DEBUG FUNCTION: Check scheduled notifications status (NO COROUTINES)
     * Can be called from anywhere without coroutine scope
     */
    fun checkScheduledNotificationsSync(context: Context) {
        Log.d("NotificationDebug", "=== CHECKING SCHEDULED NOTIFICATIONS ===")
        val workManager = WorkManager.getInstance(context)
        
        try {
            // Check deadline reminder works (blocking call - but OK for debug)
            val reminderWorksFuture = workManager.getWorkInfosByTag(
                DeadlineReminderWorker.WORK_TAG_DEADLINE_REMINDER
            )
            
            // Wait for result (blocking)
            val reminderWorks = reminderWorksFuture.get(5, TimeUnit.SECONDS)
            
            Log.d("NotificationDebug", "Total Deadline Reminders: ${reminderWorks.size}")
            reminderWorks.forEach { workInfo ->
                Log.d("NotificationDebug", """
                    Deadline Reminder:
                    - ID: ${workInfo.id}
                    - State: ${workInfo.state}
                    - Tags: ${workInfo.tags}
                    - Run Attempt: ${workInfo.runAttemptCount}
                """.trimIndent())
            }
            
            // Check deadline expired works
            val expiredWorksFuture = workManager.getWorkInfosByTag(
                DeadlineExpiredWorker.WORK_TAG_DEADLINE_EXPIRED
            )
            val expiredWorks = expiredWorksFuture.get(5, TimeUnit.SECONDS)
            
            Log.d("NotificationDebug", "Total Deadline Expired: ${expiredWorks.size}")
            expiredWorks.forEach { workInfo ->
                Log.d("NotificationDebug", """
                    Deadline Expired:
                    - ID: ${workInfo.id}
                    - State: ${workInfo.state}
                    - Tags: ${workInfo.tags}
                    - Run Attempt: ${workInfo.runAttemptCount}
                    - Next Schedule Time: ${workInfo.nextScheduleTimeMillis}
                """.trimIndent())
            }
            
            if (reminderWorks.isEmpty() && expiredWorks.isEmpty()) {
                Log.w("NotificationDebug", "⚠️ No scheduled notifications found!")
            }
            
        } catch (e: Exception) {
            Log.e("NotificationDebug", "Error checking scheduled notifications: ${e.message}", e)
        }
    }
    /**
     * ✅ Get all scheduled works status (NO COROUTINES)
     */
    fun getScheduledWorksStatusSync(context: Context): String {
        val workManager = WorkManager.getInstance(context)
        val stringBuilder = StringBuilder()
        
        try {
            // Deadline reminders
            val reminderWorks = workManager.getWorkInfosByTag(
                DeadlineReminderWorker.WORK_TAG_DEADLINE_REMINDER
            ).get(5, TimeUnit.SECONDS)
            
            stringBuilder.append("=== DEADLINE REMINDERS (${reminderWorks.size}) ===\n")
            reminderWorks.forEachIndexed { index, workInfo ->
                stringBuilder.append("${index + 1}. State: ${workInfo.state}\n")
                stringBuilder.append("   ID: ${workInfo.id}\n")
            }
            
            // Deadline expired
            val expiredWorks = workManager.getWorkInfosByTag(
                DeadlineExpiredWorker.WORK_TAG_DEADLINE_EXPIRED
            ).get(5, TimeUnit.SECONDS)
            
            stringBuilder.append("\n=== DEADLINE EXPIRED (${expiredWorks.size}) ===\n")
            expiredWorks.forEachIndexed { index, workInfo ->
                stringBuilder.append("${index + 1}. State: ${workInfo.state}\n")
                stringBuilder.append("   ID: ${workInfo.id}\n")
                stringBuilder.append("   Next run: ${
                    if (workInfo.nextScheduleTimeMillis > 0) {
                        java.time.Instant.ofEpochMilli(workInfo.nextScheduleTimeMillis)
                    } else "N/A"
                }\n")
            }
            
            if (reminderWorks.isEmpty() && expiredWorks.isEmpty()) {
                stringBuilder.append("\n⚠️ No scheduled notifications found!\n")
            }
            
        } catch (e: Exception) {
            stringBuilder.append("Error: ${e.message}\n")
        }
        
        return stringBuilder.toString()
    }

    /**
     * ✅ Cancel ALL scheduled notifications (useful for debugging)
     */
    fun cancelAllScheduledNotifications(context: Context) {
        Log.d("ReminderScheduler", "Cancelling all scheduled notifications")
        val workManager = WorkManager.getInstance(context)

        workManager.cancelAllWorkByTag(DeadlineReminderWorker.WORK_TAG_DEADLINE_REMINDER)
        workManager.cancelAllWorkByTag(DeadlineExpiredWorker.WORK_TAG_DEADLINE_EXPIRED)
        workManager.cancelAllWorkByTag(EventReminderWorker.WORK_TAG_EVENT_REMINDER)

        Log.d("ReminderScheduler", "✅ All scheduled notifications cancelled")
    }

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
