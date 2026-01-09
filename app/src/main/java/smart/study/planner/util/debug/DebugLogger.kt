package smart.study.planner.util.debug

import smart.study.planner.data.model.Event
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLogger {
    private val logs = mutableListOf<String>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    private fun addLog(logMessage: String) {
        val timestamp = dateFormat.format(Date())
        logs.add("$timestamp - $logMessage")
        // Optional: Print to Logcat for real-time debugging
        println("DebugLogger: $logMessage")
    }

    fun logSaveStart(event: Event) {
        addLog("Save Start: eventId=${event.id}, title=${event.title}")
    }

    fun logRoomInsert(result: Long) {
        addLog("Room Insert: result=$result")
    }

    fun logFirebaseSave(success: Boolean) {
        addLog("Repository Save Finished: success=$success")
    }

    fun logError(stage: String, error: Throwable) {
        addLog("Error at $stage: ${error.message}")
    }

    fun logNavigationEvent(route: String) {
        addLog("Navigation: to $route")
    }

    fun exportLogs(): String {
        return logs.joinToString("\n")
    }

    fun clearLogs() {
        logs.clear()
        addLog("Logs cleared")
    }
}
