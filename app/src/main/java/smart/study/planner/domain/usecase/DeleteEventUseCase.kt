package smart.study.planner.domain.usecase

import smart.study.planner.domain.repository.EventRepository
import javax.inject.Inject

/**
 * Use case for deleting an event
 */
class DeleteEventUseCase @Inject constructor(
    private val repository: EventRepository
) {
    suspend operator fun invoke(eventId: String): kotlin.Result<Unit> {
        return repository.deleteEvent(eventId)
    }
}

