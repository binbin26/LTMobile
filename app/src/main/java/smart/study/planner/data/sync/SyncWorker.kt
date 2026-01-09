package smart.study.planner.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import smart.study.planner.domain.repository.EventRepository

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val eventRepository: EventRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "SyncWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(WORK_NAME, "Sync worker started")
        return try {
            // First, push local changes to the remote.
            Log.d(WORK_NAME, "Syncing pending events...")
            eventRepository.syncPendingEvents().fold(
                onSuccess = { Log.d(WORK_NAME, "Sync pending events successful.") },
                onFailure = { 
                    Log.e(WORK_NAME, "Sync pending events failed.", it)
                    return Result.retry() 
                }
            )

            // Second, pull remote changes to the local db.
            Log.d(WORK_NAME, "Syncing with Firebase...")
            eventRepository.syncWithFirebase().fold(
                onSuccess = { Log.d(WORK_NAME, "Sync with Firebase successful.") },
                onFailure = {
                    Log.e(WORK_NAME, "Sync with Firebase failed.", it)
                    return Result.retry()
                }
            )

            Log.d(WORK_NAME, "Sync worker finished successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(WORK_NAME, "Sync worker failed with an exception", e)
            Result.failure()
        }
    }
}
