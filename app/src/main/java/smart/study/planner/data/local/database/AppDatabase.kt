package smart.study.planner.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import smart.study.planner.data.local.dao.EventDao
import smart.study.planner.data.local.dao.SubjectDao
import smart.study.planner.data.local.dao.UserCredentialDao
import smart.study.planner.data.local.entity.UserCredentialEntity
import smart.study.planner.data.model.Event
import smart.study.planner.data.model.Subject

/**
 * Room Database for the application
 * Manages local data persistence
 */
@Database(
    entities = [Event::class, Subject::class, UserCredentialEntity::class],
    version = 4, // Incremented to add UserCredentialEntity table
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun subjectDao(): SubjectDao
    abstract fun userCredentialDao(): UserCredentialDao
    
    companion object {
        const val DATABASE_NAME = "ltmobile_database"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
