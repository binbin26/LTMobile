package smart.study.planner.data.local.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
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
        const val CHANNEL_DEADLINE_REMINDER = "deadline_reminder"
        const val CHANNEL_DEADLINE_EXPIRED = "deadline_expired"
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
            Log.d("NotificationManager", "Creating notification channels...")
            
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

            // Deadline 1-day-before reminder channel
            val deadlineReminderChannel = NotificationChannel(
                CHANNEL_DEADLINE_REMINDER,
                "Nhắc nhở Deadline",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo nhắc nhở Deadline 1 ngày trước"
                enableVibration(true)
                enableLights(true)
            }

            // Deadline expired channel
            val deadlineExpiredChannel = NotificationChannel(
                CHANNEL_DEADLINE_EXPIRED,
                "Deadline Hết Hạn",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo Deadline đã hết hạn"
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
            notificationManager.createNotificationChannel(deadlineReminderChannel)
            notificationManager.createNotificationChannel(deadlineExpiredChannel)
            notificationManager.createNotificationChannel(syncChannel)
            
            Log.d("NotificationManager", "✅ All notification channels created")
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
        Log.d("NotificationManager", "=== SHOWING EVENT REMINDER ===")
        Log.d("NotificationManager", "Event: $eventTitle at $eventTime")
        
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

        // Build detailed message
        val detailedMessage = StringBuilder()
        detailedMessage.append("⏱️ Thời gian: $eventTime\n")
        if (!eventLocation.isNullOrEmpty()) {
            detailedMessage.append("📍 Địa điểm: $eventLocation")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_EVENT_REMINDER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("📌 $eventTitle")
            .setContentText(eventTime + (eventLocation?.let { " • $it" } ?: ""))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(detailedMessage.toString()))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setColor(0xFF1976D2.toInt()) // Material Blue 500
            .setLights(0xFF1976D2.toInt(), 1000, 1000)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .build()

        try {
            notificationManager.notify(eventId.hashCode(), notification)
            Log.d("NotificationManager", "✅ Event reminder notification posted successfully")
        } catch (e: Exception) {
            Log.e("NotificationManager", "❌ Failed to post event reminder: ${e.message}", e)
        }
    }

    /**
     * Show deadline 1-day-before reminder notification
     */
    fun showDeadlineReminderNotification(
        eventId: String,
        eventTitle: String,
        deadlineDate: String
    ) {
        Log.d("NotificationManager", "=== SHOWING DEADLINE REMINDER ===")
        Log.d("NotificationManager", "Event: $eventTitle")
        Log.d("NotificationManager", "Deadline: $deadlineDate")
        Log.d("NotificationManager", "Notification ID: ${"deadline_reminder_$eventId".hashCode()}")
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("eventId", eventId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            "deadline_reminder_$eventId".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build detailed deadline message
        val detailedMessage = """
            📌 Sự kiện: $eventTitle
            
            ⏰ Deadline: $deadlineDate
            
            ⚠️ Hãy hoàn thành sự kiện này trước thời hạn!
        """.trimIndent()

        val notification = NotificationCompat.Builder(context, CHANNEL_DEADLINE_REMINDER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⏰ Deadline Sắp Tới!")
            .setContentText(eventTitle)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(detailedMessage))
            .setSubText("Còn 24 giờ nữa")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setColor(0xFFFF9800.toInt()) // Material Orange 500
            .setLights(0xFFFF9800.toInt(), 1000, 1000)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setShowWhen(true)
            .build()

        try {
            notificationManager.notify("deadline_reminder_$eventId".hashCode(), notification)
            Log.d("NotificationManager", "✅ Deadline reminder notification posted successfully")
        } catch (e: Exception) {
            Log.e("NotificationManager", "❌ Failed to post deadline reminder: ${e.message}", e)
        }
    }

    /**
     * Show deadline expired notification (repeating notification)
     */
    fun showDeadlineExpiredNotification(
        eventId: String,
        eventTitle: String
    ) {
        Log.d("NotificationManager", "=== SHOWING DEADLINE EXPIRED NOTIFICATION ===")
        Log.d("NotificationManager", "Event: $eventTitle (ID: $eventId)")
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("eventId", eventId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            "deadline_expired_$eventId".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build detailed expired message
        val detailedMessage = """
            ❌ Sự kiện: $eventTitle
            
            Deadline đã hết hạn!
            
            ⚠️ Hãy cập nhật trạng thái sự kiện này ngay
        """.trimIndent()

        val notification = NotificationCompat.Builder(context, CHANNEL_DEADLINE_EXPIRED)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("❌ Deadline Hết Hạn!")
            .setContentText(eventTitle)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(detailedMessage))
            .setSubText("Cần xử lý ngay")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setColor(0xFFD32F2F.toInt()) // Material Red 600
            .setLights(0xFFD32F2F.toInt(), 1000, 1000)
            .setVibrate(longArrayOf(0, 500, 250, 500, 250, 500))
            .setShowWhen(true)
            .build()

        try {
            notificationManager.notify("deadline_expired_$eventId".hashCode(), notification)
            Log.d("NotificationManager", "✅ Deadline expired notification posted successfully")
        } catch (e: Exception) {
            Log.e("NotificationManager", "❌ Failed to post deadline expired notification: ${e.message}", e)
        }
    }

    /**
     * Show sync status notification
     */
    fun showSyncNotification(message: String, isError: Boolean = false) {
        Log.d("NotificationManager", "=== SHOWING SYNC NOTIFICATION ===")
        Log.d("NotificationManager", "Status: ${if (isError) "ERROR" else "SYNCING"}")
        Log.d("NotificationManager", "Message: $message")
        
        val syncMessage = if (isError) {
            "❌ Lỗi đồng bộ dữ liệu\n\n$message\n\nVui lòng thử lại."
        } else {
            "⏳ Đang đồng bộ dữ liệu...\n\n$message"
        }

        val icon = if (isError) R.drawable.ic_launcher_foreground else R.drawable.ic_launcher_foreground

        val notification = NotificationCompat.Builder(context, CHANNEL_SYNC)
            .setSmallIcon(icon)
            .setContentTitle(if (isError) "❌ Lỗi Đồng Bộ" else "⏳ Đang Đồng Bộ")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(syncMessage))
            .setAutoCancel(!isError)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setColor(if (isError) 0xFFD32F2F.toInt() else 0xFF4CAF50.toInt())
            .setProgress(0, 0, !isError) // Show indeterminate progress for syncing
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID_SYNC, notification)
            Log.d("NotificationManager", "✅ Sync notification posted successfully")
        } catch (e: Exception) {
            Log.e("NotificationManager", "❌ Failed to post sync notification: ${e.message}", e)
        }
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

    /**
     * Cancel deadline reminder notification
     */
    fun cancelDeadlineReminderNotification(eventId: String) {
        notificationManager.cancel("deadline_reminder_$eventId".hashCode())
    }

    /**
     * Cancel deadline expired notification
     */
    fun cancelDeadlineExpiredNotification(eventId: String) {
        notificationManager.cancel("deadline_expired_$eventId".hashCode())
    }
}
