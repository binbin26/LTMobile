package smart.study.planner.util.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object DebugOverlayManager {
    private val _showOverlay = MutableStateFlow(false)
    val showOverlay = _showOverlay.asStateFlow()

    fun show()
    {
        _showOverlay.value = true
    }

    fun hide()
    {
        _showOverlay.value = false
    }

    fun toggle()
    {
        _showOverlay.value = !_showOverlay.value
    }
}