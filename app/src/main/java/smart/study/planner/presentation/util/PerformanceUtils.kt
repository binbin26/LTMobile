package smart.study.planner.presentation.util

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
// IMPORTANT: Make sure you have 'implementation("com.google.firebase:firebase-perf-ktx")' in your app/build.gradle.kts file and sync the project.
import com.google.firebase.perf.ktx.trace as firebaseTrace
import kotlin.time.measureTime

// --- 1. Function Execution Time Measurement ---

/**
 * Measures the execution time of a given synchronous code block and logs it.
 */
inline fun <T> measureExecutionTime(tag: String, message: String, block: () -> T): T {
    val result: T
    val duration = measureTime {
        result = block()
    }
    Log.d(tag, "$message executed in ${duration.inWholeMilliseconds} ms")
    return result
}

// --- 2. Recomposition Counting ---

class RecompositionCounter {
    var count = 0
}

/**
 * A Composable that logs the number of times its content recomposes.
 */
@Composable
fun RecompositionTracker(tag: String, content: @Composable () -> Unit) {
    val recompositionCount = remember { RecompositionCounter() }

    SideEffect {
        recompositionCount.count++
    }

    Log.d(tag, "Recomposing ($tag), count: ${recompositionCount.count}")
    content()
}

// --- 3. Automatic Slow Operation Loggers ---

/**
 * Logs a warning if a synchronous block of code is slow.
 * Note: This version does NOT use Firebase to keep it simple and safe for any context.
 */
inline fun <T> logSlowSyncOperation(
    operationName: String,
    warningThresholdMs: Long = 50,
    logTag: String = "SlowSyncOperation",
    block: () -> T
): T {
    val result: T
    val duration = measureTime {
        result = block()
    }
    if (duration.inWholeMilliseconds > warningThresholdMs) {
        Log.w(logTag, "Operation '$operationName' is slow! Took ${duration.inWholeMilliseconds} ms")
    }
    return result
}

/**
 * Traces a suspend (asynchronous) block of code using Firebase Performance.
 * Automatically logs a warning if its execution time exceeds a given threshold.
 */
suspend inline fun <T> traceAndLogSlowSuspendOperation(
    traceName: String,
    warningThresholdMs: Long = 300, // Higher threshold for suspend functions (I/O)
    logTag: String = "SlowSuspendOperation",
    crossinline block: suspend () -> T
): T {
    // Use the KTX top-level `trace` function which is a suspend function itself
    return firebaseTrace(traceName) {
        val result: T
        val duration = measureTime {
            result = block()
        }
        if (duration.inWholeMilliseconds > warningThresholdMs) {
            Log.w(logTag, "Operation '$traceName' is slow! Took ${duration.inWholeMilliseconds} ms")
        }
        result
    }
}
