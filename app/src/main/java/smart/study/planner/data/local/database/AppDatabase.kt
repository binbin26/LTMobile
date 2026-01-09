package smart.study.planner.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import smart.study.planner.data.local.dao.EventDao
import smart.study.planner.data.local.dao.SubjectDao
import smart.study.planner.data.model.Event
import smart.study.planner.data.model.Subject

/**
 * Room Database for the application
 * Manages local data persistence
 */
@Database(
    entities = [Event::class, Subject::class],
    version = 3, // Incremented to add Subject table
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun subjectDao(): SubjectDao
    
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
