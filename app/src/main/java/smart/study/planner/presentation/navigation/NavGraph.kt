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
import smart.study.planner.presentation.screens.ForgotPasswordScreen
import smart.study.planner.presentation.screens.HomeScreen
import smart.study.planner.presentation.screens.LoginScreen
import smart.study.planner.presentation.screens.ProfileScreen
import smart.study.planner.presentation.screens.RegisterScreen
import smart.study.planner.presentation.screens.ResetPasswordScreen
import smart.study.planner.presentation.screens.SplashScreen
import smart.study.planner.presentation.screens.SubjectManagementScreen
import smart.study.planner.presentation.screens.TaskListScreen
import smart.study.planner.presentation.screens.VerifyCodeScreen

/**
 * Navigation routes for the application
 * Centralized route management for type-safe navigation
 */
sealed class Screen(val route: String) {
    // ============================================
    // AUTHENTICATION SCREENS
    // ============================================
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object ForgotPassword : Screen("forgot_password")

    data object VerifyCode : Screen("verify_code/{email}") {
        fun createRoute(email: String) = "verify_code/$email"
    }

    data object ResetPassword : Screen("reset_password/{email}") {
        fun createRoute(email: String) = "reset_password/$email"
    }

    // ============================================
    // MAIN SCREENS (Bottom Navigation)
    // ============================================
    data object Home : Screen("home")
    data object Calendar : Screen("calendar")
    data object Tasks : Screen("tasks")
    data object Profile : Screen("profile")

    // ============================================
    // FEATURE SCREENS
    // ============================================

    /**
     * Add/Edit Event Screen
     * Route: "add_event?eventId={eventId}"
     * @param eventId Optional event ID for editing existing event
     */
    data object AddEvent : Screen("add_event?eventId={eventId}") {
        const val BASE_ROUTE = "add_event"
        fun createRoute(eventId: String? = null): String {
            return if (eventId != null) {
                "$BASE_ROUTE?eventId=$eventId"
            } else {
                BASE_ROUTE
            }
        }
    }

    /**
     * Subject Management Screen
     * Route: "subject_management"
     * Full CRUD operations for subjects
     */
    data object SubjectManagement : Screen("subject_management")

    /**
     * Edit Profile Screen
     * Route: "edit_profile"
     */
    data object EditProfile : Screen("edit_profile")

    /**
     * Settings Screen
     * Route: "settings/{type}"
     * @param type Type of settings (notification, account, privacy, etc.)
     */
    data object Settings : Screen("settings/{type}") {
        const val BASE_ROUTE = "settings"
        fun createRoute(type: String) = "$BASE_ROUTE/$type"
    }

    /**
     * Event Detail Screen
     * Route: "event_detail/{eventId}"
     * @param eventId Event ID to display details
     */
    data object EventDetail : Screen("event_detail/{eventId}") {
        const val BASE_ROUTE = "event_detail"
        fun createRoute(eventId: String) = "$BASE_ROUTE/$eventId"
    }
}

/**
 * Navigation graph setup
 * Defines all navigation routes and their composable destinations
 */
@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // ============================================
        // AUTHENTICATION FLOW
        // ============================================

        composable(route = Screen.Splash.route) {
            Log.d("NavGraph", "→ Splash Screen")
            SplashScreen(navController = navController)
        }

        composable(route = Screen.Login.route) {
            Log.d("NavGraph", "→ Login Screen")
            LoginScreen(navController = navController)
        }

        composable(route = Screen.Register.route) {
            Log.d("NavGraph", "→ Register Screen")
            RegisterScreen(navController = navController)
        }

        composable(route = Screen.ForgotPassword.route) {
            Log.d("NavGraph", "→ Forgot Password Screen")
            ForgotPasswordScreen(navController = navController)
        }

        composable(
            route = Screen.VerifyCode.route,
            arguments = listOf(
                navArgument("email") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            Log.d("NavGraph", "→ Verify Code Screen (email: $email)")
            VerifyCodeScreen(
                navController = navController,
                email = email
            )
        }

        composable(
            route = Screen.ResetPassword.route,
            arguments = listOf(
                navArgument("email") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            Log.d("NavGraph", "→ Reset Password Screen (email: $email)")
            ResetPasswordScreen(
                navController = navController,
                email = email
            )
        }

        // ============================================
        // MAIN NAVIGATION (Bottom Nav Bar)
        // ============================================

        composable(route = Screen.Home.route) {
            Log.d("NavGraph", "→ Home Screen")
            HomeScreen(navController = navController)
        }

        composable(route = Screen.Calendar.route) {
            Log.d("NavGraph", "→ Calendar Screen")
            CalendarScreen(navController = navController)
        }

        composable(route = Screen.Tasks.route) {
            Log.d("NavGraph", "→ Tasks Screen")
            TaskListScreen(navController = navController)
        }

        composable(route = Screen.Profile.route) {
            Log.d("NavGraph", "→ Profile Screen")
            ProfileScreen(navController = navController)
        }

        // ============================================
        // FEATURE SCREENS
        // ============================================

        /**
         * Add/Edit Event Screen
         * Handles both creating new events and editing existing ones
         */
        composable(
            route = Screen.AddEvent.route,
            arguments = listOf(
                navArgument("eventId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val rawEventId = backStackEntry.arguments?.getString("eventId")
            // ✅ ENHANCED VALIDATION: handle placeholder, malformed concatenation, and invalid values
            val actualEventId = when {
                rawEventId.isNullOrBlank() -> null
                rawEventId == "{eventId}" -> null
                rawEventId.contains("{eventId}") -> {
                    // malformed like "{eventId}?eventId=..." -> extract last param value
                    android.util.Log.e("NavGraph", "Malformed eventId detected: $rawEventId")
                    rawEventId.substringAfterLast("=").takeIf { it.isNotBlank() }
                }
                else -> rawEventId
            }
            Log.d("NavGraph", "→ Add/Edit Event Screen (raw: ${rawEventId ?: "null"}, actual: ${actualEventId ?: "new"})")
            AddEventScreen(
                navController = navController,
                eventId = actualEventId
            )
        }

        /**
         * Subject Management Screen
         * Full CRUD operations for managing subjects
         */
        composable(route = Screen.SubjectManagement.route) {
            Log.d("NavGraph", "→ Subject Management Screen")
            SubjectManagementScreen(navController = navController)
        }

        /**
         * Edit Profile Screen
         * Edit user profile information
         */
        composable(route = Screen.EditProfile.route) {
            Log.d("NavGraph", "→ Edit Profile Screen")
            EditProfileScreen(navController = navController)
        }

        /**
         * Settings Screen
         * Different settings pages based on type parameter
         */
        composable(
            route = Screen.Settings.route,
            arguments = listOf(
                navArgument("type") {
                    type = NavType.StringType
                    defaultValue = "general"
                }
            )
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "general"
            Log.d("NavGraph", "→ Settings Screen (type: $type)")
            // TODO: Implement SettingsScreen based on type
            // For now, redirect to Tasks as placeholder
            TaskListScreen(navController = navController)
        }

        /**
         * Event Detail Screen
         * Display detailed information about a specific event
         */
        composable(
            route = Screen.EventDetail.route,
            arguments = listOf(
                navArgument("eventId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            Log.d("NavGraph", "→ Event Detail Screen (eventId: $eventId)")
            // TODO: Implement EventDetailScreen
            // For now, redirect to Tasks as placeholder
            TaskListScreen(navController = navController)
        }
    }
}

// ============================================
// NAVIGATION HELPER EXTENSIONS
// ============================================

/**
 * Navigate with automatic logging
 */
fun NavHostController.navigateWithLog(route: String) {
    Log.d("NavGraph", "Navigation requested: $route")
    navigate(route)
}

/**
 * Navigate and clear back stack up to destination
 */
fun NavHostController.navigateAndClearBackStack(route: String, popUpToRoute: String) {
    Log.d("NavGraph", "Navigate and clear: $route (popUpTo: $popUpToRoute)")
    navigate(route) {
        popUpTo(popUpToRoute) {
            inclusive = true
        }
        launchSingleTop = true
    }
}

/**
 * Safely pop back stack with logging
 */
fun NavHostController.safePopBackStack(): Boolean {
    return if (previousBackStackEntry != null) {
        Log.d("NavGraph", "← Popping back stack")
        popBackStack()
    } else {
        Log.w("NavGraph", "Cannot pop - no previous entry")
        false
    }
}

/**
 * Navigate to Home and clear all back stack
 */
fun NavHostController.navigateToHome() {
    Log.d("NavGraph", "Navigate to Home (clear all)")
    navigate(Screen.Home.route) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}

/**
 * Navigate to Login and clear all back stack
 */
fun NavHostController.navigateToLogin() {
    Log.d("NavGraph", "Navigate to Login (clear all)")
    navigate(Screen.Login.route) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}