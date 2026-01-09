package smart.study.planner.di

import android.content.Context
import androidx.room.Room
import smart.study.planner.data.local.dao.EventDao
import smart.study.planner.data.local.dao.SubjectDao
import smart.study.planner.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database dependencies
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
    fun provideEventDao(database: AppDatabase): EventDao {
        return database.eventDao()
    }
    
    @Provides
    fun provideSubjectDao(database: AppDatabase): SubjectDao {
        return database.subjectDao()
    }
}

