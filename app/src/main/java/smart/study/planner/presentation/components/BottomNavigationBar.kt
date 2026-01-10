package smart.study.planner.presentation.components

import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import smart.study.planner.presentation.navigation.Screen

/**
 * Bottom navigation bar component
 */
@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem("Trang Chủ", Icons.Default.Home, Screen.Home.route),
        BottomNavItem("Lịch", Icons.Default.Settings, Screen.Calendar.route), // TODO: Thay bằng DateRange sau khi sync Gradle
        BottomNavItem("Thêm Mới", Icons.Default.Add, Screen.AddEvent.route),
        BottomNavItem("Nhiệm vụ", Icons.Default.List, Screen.Tasks.route),
        BottomNavItem("Tài khoản", Icons.Default.AccountCircle, Screen.Profile.route)
    )
    
    NavigationBar(modifier = modifier) {
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    if (item.route == Screen.Profile.route) {
                        Log.d("BottomNavigation", "Profile icon clicked")
                    }
                    onNavigate(item.route)
                }
            )
        }
    }
}

private data class BottomNavItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String
)

