package smart.study.planner.domain.usecase

import smart.study.planner.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting upcoming events
 */
class GetUpcomingEventsUseCase @Inject constructor(
    private val repository: EventRepository
) {
    operator fun invoke(): Flow<kotlin.Result<List<smart.study.planner.data.model.Event>>> {
        return repository.getUpcomingEvents()
    }
}

