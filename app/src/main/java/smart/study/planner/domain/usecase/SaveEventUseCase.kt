package smart.study.planner.domain.usecase

import android.util.Log
import smart.study.planner.data.model.Event
import smart.study.planner.domain.repository.EventRepository
import javax.inject.Inject

private const val TAG = "SaveEventUseCase"

/**
 * Use case for saving an event
 */
class SaveEventUseCase @Inject constructor(
    private val repository: EventRepository
) {
    suspend operator fun invoke(event: Event): kotlin.Result<Unit> {
        Log.d(TAG, "============================================")
        Log.d(TAG, "USECASE: invoke() called")
        Log.d(TAG, "Event ID: ${event.id}")
        Log.d(TAG, "Event Title: ${event.title}")
        Log.d(TAG, "Event UserId: ${event.userId}")
        Log.d(TAG, "============================================")
        
        Log.d(TAG, "Calling repository.saveEvent()...")
        val result = repository.saveEvent(event)
        
        Log.d(TAG, "Repository returned: ${if (result.isSuccess) "✅ SUCCESS" else "❌ FAILURE"}")
        
        if (result.isFailure) {
            Log.e(TAG, "Error: ${result.exceptionOrNull()?.message}")
            Log.e(TAG, "Stack trace: ", result.exceptionOrNull())
        } else {
            Log.d(TAG, "✅ USECASE: Event saved successfully!")
        }
        
        Log.d(TAG, "============================================")
        
        return result
    }
}

