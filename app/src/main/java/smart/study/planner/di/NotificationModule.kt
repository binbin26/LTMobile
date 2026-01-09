package smart.study.planner.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import smart.study.planner.data.local.notification.NotificationManager
import javax.inject.Singleton

/**
 * Hilt module for providing notification dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {
    
    @Provides
    @Singleton
    fun provideNotificationManager(
        @ApplicationContext context: Context
    ): NotificationManager {
        return NotificationManager(context)
    }
}
