package smart.study.planner.presentation.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import smart.study.planner.presentation.screens.AddEventScreen
import smart.study.planner.presentation.screens.CalendarScreen
import smart.study.planner.presentation.screens.EditProfileScreen
import smart.study.planner.presentation.screens.HomeScreen
import smart.study.planner.presentation.screens.LoginScreen
import smart.study.planner.presentation.screens.ProfileScreen
import smart.study.planner.presentation.screens.RegisterScreen
import smart.study.planner.presentation.screens.SplashScreen
import smart.study.planner.presentation.screens.TaskListScreen

/**
 * Navigation routes
 */
sealed class Screen(val route: String) {
    // Xác thực
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    
    // Chính
    object Home : Screen("home")
    object Calendar : Screen("calendar")
    object AddNew : Screen("add_new")
    object Tasks : Screen("tasks")
    object TaskList : Screen("tasks") // Alias for Tasks
    object Profile : Screen("profile")
    
    object AddEvent : Screen("add_event") {
        fun createRoute(eventId: String? = null) = 
            if (eventId != null) "add_event?eventId=$eventId" else "add_event"
    }
    
    object EditProfile : Screen("edit_profile")
    
    object Settings : Screen("settings/{type}") {
        fun createRoute(type: String) = "settings/$type"
    }
    
    object EventDetail : Screen("event_detail/{eventId}") {
        fun createRoute(eventId: String) = "event_detail/$eventId"
    }
}

/**
 * Navigation graph setup
 */
@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }

        // Xác thực screens
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }

        composable(Screen.Register.route) {
            RegisterScreen(navController = navController)
        }

        // Home screen
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        
        composable(Screen.Calendar.route) {
            CalendarScreen(navController = navController)
        }
        
        composable(Screen.AddNew.route) {
            // AddNewScreen - shows options to add event or task
            AddEventScreen(
                navController = navController,
                eventId = null
            )
        }
        
        composable(
            route = "add_event?eventId={eventId}",
            arguments = listOf(
                navArgument("eventId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId")
            AddEventScreen(
                navController = navController,
                eventId = eventId
            )
        }
        
        composable(Screen.TaskList.route) {
            TaskListScreen(navController = navController)
        }
        
        composable(Screen.Profile.route) {
            Log.d("NavGraph", "Navigating to profile route")
            ProfileScreen(navController = navController)
        }
        
        composable(Screen.EditProfile.route) {
            Log.d("NavGraph", "Navigating to edit profile route")
            EditProfileScreen(navController = navController)
        }
        
        composable(
            route = Screen.Settings.route,
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "notification"
            // SettingsScreen - TODO: implement
            TaskListScreen(navController = navController) // Placeholder
        }
        
        composable(
            route = Screen.EventDetail.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            // EventDetailScreen - TODO: implement
            TaskListScreen(navController = navController) // Placeholder
        }
    }
}

