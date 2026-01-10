package smart.study.planner.domain.usecase

import smart.study.planner.data.model.Event
import smart.study.planner.domain.repository.EventRepository
import javax.inject.Inject

class ToggleEventCompletionUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(eventId: String): Result<Event> {
        return eventRepository.toggleEventCompletion(eventId)
    }
}

