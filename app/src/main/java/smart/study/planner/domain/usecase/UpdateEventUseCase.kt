package smart.study.planner.domain.usecase

import smart.study.planner.data.model.Event
import smart.study.planner.domain.repository.EventRepository
import javax.inject.Inject

/**
 * Use case for updating an event
 */
class UpdateEventUseCase @Inject constructor(
    private val repository: EventRepository
) {
    suspend operator fun invoke(event: Event): kotlin.Result<Unit> {
        return repository.updateEvent(event)
    }
}

