package smart.study.planner.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class DebugOverlayState(
    val saveState: String? = null,
    val lastError: String? = null,
    val networkStatus: String? = null,
    val dbStats: String? = null
)

@HiltViewModel
class DebugOverlayViewModel @Inject constructor() : ViewModel() {
    private val _overlayState = MutableStateFlow(DebugOverlayState())
    val overlayState = _overlayState.asStateFlow()

    fun updateSaveState(state: String) {
        _overlayState.value = _overlayState.value.copy(saveState = state)
    }

    fun updateLastError(error: String) {
        _overlayState.value = _overlayState.value.copy(lastError = error)
    }

    fun updateNetworkStatus(status: String) {
        _overlayState.value = _overlayState.value.copy(networkStatus = status)
    }

    fun updateDbStats(stats: String) {
        _overlayState.value = _overlayState.value.copy(dbStats = stats)
    }
}

@Composable
fun DebugOverlay(viewModel: DebugOverlayViewModel = hiltViewModel()) {
    val state by viewModel.overlayState.collectAsState()

    Box(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(8.dp)
    ) {
        Column {
            state.saveState?.let { Text("Save State: $it", color = Color.White) }
            state.lastError?.let { Text("Last Error: $it", color = Color.Red) }
            state.networkStatus?.let { Text("Network: $it", color = Color.White) }
            state.dbStats?.let { Text("DB Stats: $it", color = Color.White) }
        }
    }
}
