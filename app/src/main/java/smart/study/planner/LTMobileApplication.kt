package smart.study.planner

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import smart.study.planner.data.sync.SyncScheduler
import javax.inject.Inject

/**
 * Application class for dependency injection setup and background task scheduling.
 */
@HiltAndroidApp
class LTMobileApplication : Application(), Configuration.Provider {

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
        // Schedule the background sync task when the application starts
        syncScheduler.schedule()
    }
}
