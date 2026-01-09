package smart.study.planner.presentation.util

/**
 * Sealed class representing UI states
 * Used for handling loading, success, and error states in ViewModels
 */
sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val error: Throwable) : UiState<Nothing>()

    /**
     * Check if state is loading
     */
    val isLoading: Boolean
        get() = this is Loading

    /**
     * Check if state is success
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * Check if state is error
     */
    val isError: Boolean
        get() = this is Error

    /**
     * Get data if state is success, null otherwise
     */
    fun getDataOrNull(): T? {
        return (this as? Success)?.data
    }
}
