package smart.study.planner.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import smart.study.planner.domain.repository.EventRepository
import java.io.IOException

/**
 * A background worker responsible for synchronizing local data with the remote Firebase database.
 * It pushes pending local changes (creates, updates, deletes) and pulls the latest data from remote.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    // Using a repository provides a clean abstraction over the data sources.
    private val eventRepository: EventRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "SyncWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Sync work started.")

        return try {
            // Step 1: Push local changes (creations, updates, deletions) to the remote.
            Log.d(TAG, "Attempting to sync pending events to remote...")
            eventRepository.syncPendingEvents().getOrThrow()
            Log.d(TAG, "Successfully synced pending events.")

            // Step 2: Pull latest changes from the remote to the local database.
            Log.d(TAG, "Attempting to sync from remote...")
            eventRepository.syncWithFirebase().getOrThrow()
            Log.d(TAG, "Successfully synced from remote.")

            Log.d(TAG, "Sync work finished successfully.")
            Result.success()

        } catch (e: IOException) {
            // IOException typically indicates a network problem. Retrying is a good strategy.
            Log.e(TAG, "Sync failed due to network error. Retrying...", e)
            Result.retry()

        } catch (e: Exception) {
            // For other unexpected errors, we might not want to retry indefinitely.
            // Logging the error and failing the work allows for inspection.
            Log.e(TAG, "Sync failed with an unexpected error. Work will not be retried.", e)
            Result.failure()
        }
    }
}
