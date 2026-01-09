package smart.study.planner

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.initialize
import dagger.hilt.android.HiltAndroidApp
import smart.study.planner.data.sync.SyncScheduler
import javax.inject.Inject

/**
 * Application class for dependency injection setup and background task scheduling.
 */
@HiltAndroidApp
class LTMobileApplication : Application(), Configuration.Provider {

    companion object {
        private const val TAG = "LTMobileApplication"
    }

    // Inject Hilt's worker factory to enable dependency injection in Workers
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var syncScheduler: SyncScheduler

    // Provide the WorkManager configuration with the Hilt worker factory
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase App Check
        initializeFirebaseAppCheck()
        
        // Schedule the background sync task when the application starts
        syncScheduler.schedule()
    }

    /**
     * Initialize Firebase App Check with DebugAppCheckProviderFactory for debug builds
     * This helps prevent Firebase rate limiting by validating legitimate app requests
     * and enabling reCAPTCHA tokens to be properly generated
     */
    private fun initializeFirebaseAppCheck() {
        try {
            Firebase.initialize(this)
            Firebase.appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "Firebase App Check initialized successfully with DebugAppCheckProviderFactory")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase App Check", e)
        }
    }
}
