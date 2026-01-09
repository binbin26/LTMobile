package smart.study.planner.domain.usecase

import smart.study.planner.domain.repository.EventRepository
import javax.inject.Inject

class SyncPendingEventsUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke() {
        eventRepository.syncPendingEvents()
    }
}
