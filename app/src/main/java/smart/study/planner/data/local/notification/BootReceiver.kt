package smart.study.planner.data.local.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver to reschedule notifications after device reboot
 * This ensures notifications continue to work after the device is restarted
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device rebooted, rescheduling notifications")
            // WorkManager sẽ tự động reschedule các work đã enqueue
            // No additional action needed - WorkManager handles persistence automatically
        }
    }
}
