package smart.study.planner.di

import android.content.Context
import androidx.room.Room
import smart.study.planner.data.local.dao.EventDao
import smart.study.planner.data.local.dao.SubjectDao
import smart.study.planner.data.local.dao.UserCredentialDao
import smart.study.planner.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database dependencies
 * Provides Room database and DAOs for dependency injection
 * Installed in SingletonComponent to ensure single instance throughout app lifecycle
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // For development - remove in production
            .build()
    }
    
    @Provides
    @Singleton
    fun provideEventDao(database: AppDatabase): EventDao {
        return database.eventDao()
    }
    
    @Provides
    @Singleton
    fun provideSubjectDao(database: AppDatabase): SubjectDao {
        return database.subjectDao()
    }
    
    @Provides
    @Singleton
    fun provideUserCredentialDao(database: AppDatabase): UserCredentialDao {
        return database.userCredentialDao()
    }
}

