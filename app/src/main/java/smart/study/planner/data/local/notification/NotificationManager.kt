package smart.study.planner.data.local.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import smart.study.planner.MainActivity
import smart.study.planner.R

/**
 * Notification Manager for scheduling and managing notifications
 */
class NotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_EVENT_REMINDER = "event_reminder"
        const val CHANNEL_SYNC = "sync_notifications"
        const val NOTIFICATION_ID_EVENT_REMINDER = 100
        const val NOTIFICATION_ID_SYNC = 101
    }

    init {
        createNotificationChannels()
    }

    /**
     * Create notification channels for Android 8+
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Event reminder channel
            val reminderChannel = NotificationChannel(
                CHANNEL_EVENT_REMINDER,
                "Nhắc nhở sự kiện",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo nhắc nhở cho các sự kiện học tập"
                enableVibration(true)
                enableLights(true)
            }

            // Sync channel
            val syncChannel = NotificationChannel(
                CHANNEL_SYNC,
                "Đồng bộ dữ liệu",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Thông báo đồng bộ dữ liệu với máy chủ"
            }

            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(syncChannel)
        }
    }

    /**
     * Show event reminder notification
     */
    fun showEventReminder(
        eventId: String,
        eventTitle: String,
        eventTime: String,
        eventLocation: String?
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("eventId", eventId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            eventId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_EVENT_REMINDER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Sắp tới: $eventTitle")
            .setContentText(eventTime + (eventLocation?.let { " - $it" } ?: ""))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        notificationManager.notify(eventId.hashCode(), notification)
    }

    /**
     * Show sync status notification
     */
    fun showSyncNotification(message: String, isError: Boolean = false) {
        val icon = if (isError) R.drawable.ic_launcher_foreground else R.drawable.ic_launcher_foreground

        val notification = NotificationCompat.Builder(context, CHANNEL_SYNC)
            .setSmallIcon(icon)
            .setContentTitle(if (isError) "Lỗi đồng bộ" else "Đang đồng bộ")
            .setContentText(message)
            .setAutoCancel(!isError)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(NOTIFICATION_ID_SYNC, notification)
    }

    /**
     * Cancel notification
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    /**
     * Cancel all notifications
     */
    fun cancelAll() {
        notificationManager.cancelAll()
    }

    /**
     * Cancel event notification
     */
    fun cancelEventNotification(eventId: String) {
        notificationManager.cancel(eventId.hashCode())
    }
}
