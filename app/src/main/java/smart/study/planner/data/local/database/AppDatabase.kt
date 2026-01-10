package smart.study.planner.data.local.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import smart.study.planner.data.local.dao.EventDao
import smart.study.planner.data.local.dao.SubjectDao
import smart.study.planner.data.local.dao.UserCredentialDao
import smart.study.planner.data.local.entity.UserCredentialEntity
import smart.study.planner.data.model.Event
import smart.study.planner.data.model.Subject

private const val TAG = "AppDatabase"

/**
 * Room Database for Smart Study Planner
 * Manages local data persistence for Events, Subjects, and User Credentials
 *
 * Version History:
 * - v1-3: Previous versions
 * - v4: Added UserCredentialEntity table
 * - v5: Added new fields to Subject table (teacherName, schedule, classroom, etc.)
 */
@Database(
    entities = [
        Event::class,
        Subject::class,
        UserCredentialEntity::class
    ],
    version = 5, // ⬆️ Incremented from 4 to 5 for Subject table updates
    exportSchema = true // ✅ Enable schema export for migration testing
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun subjectDao(): SubjectDao
    abstract fun userCredentialDao(): UserCredentialDao

    companion object {
        const val DATABASE_NAME = "ltmobile_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Get database instance with proper migrations
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        /**
         * Build database with migrations
         */
        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(
                    MIGRATION_4_5 // ✅ Add migration from version 4 to 5
                )
                .fallbackToDestructiveMigration() // ⚠️ Keep for development, remove in production
                .build()
        }

        /**
         * Migration from version 4 to version 5
         * Adds new fields to subjects table for enhanced subject management:
         * - teacherName: Name of the instructor
         * - schedule: Class schedule (e.g., "Monday, 7:30 - 9:30")
         * - classroom: Room/location (e.g., "A101")
         * - credits: Number of credits
         * - semester: Academic semester (e.g., "HK1 2024")
         * - description: Additional notes about the subject
         * - updatedAt: Timestamp for last update
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    Log.d(TAG, "🔄 Starting migration 4 -> 5...")

                    // Check if columns already exist (safety check)
                    val cursor = database.query("PRAGMA table_info(subjects)")
                    val existingColumns = mutableSetOf<String>()

                    while (cursor.moveToNext()) {
                        val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                        existingColumns.add(columnName)
                    }
                    cursor.close()

                    // Add teacherName column if not exists
                    if (!existingColumns.contains("teacherName")) {
                        database.execSQL("""
                            ALTER TABLE subjects 
                            ADD COLUMN teacherName TEXT NOT NULL DEFAULT ''
                        """.trimIndent())
                        Log.d(TAG, "✅ Added teacherName column")
                    }

                    // Add schedule column if not exists
                    if (!existingColumns.contains("schedule")) {
                        database.execSQL("""
                            ALTER TABLE subjects 
                            ADD COLUMN schedule TEXT NOT NULL DEFAULT ''
                        """.trimIndent())
                        Log.d(TAG, "✅ Added schedule column")
                    }

                    // Add classroom column if not exists
                    if (!existingColumns.contains("classroom")) {
                        database.execSQL("""
                            ALTER TABLE subjects 
                            ADD COLUMN classroom TEXT NOT NULL DEFAULT ''
                        """.trimIndent())
                        Log.d(TAG, "✅ Added classroom column")
                    }

                    // Add credits column if not exists
                    if (!existingColumns.contains("credits")) {
                        database.execSQL("""
                            ALTER TABLE subjects 
                            ADD COLUMN credits INTEGER NOT NULL DEFAULT 0
                        """.trimIndent())
                        Log.d(TAG, "✅ Added credits column")
                    }

                    // Add semester column if not exists
                    if (!existingColumns.contains("semester")) {
                        database.execSQL("""
                            ALTER TABLE subjects 
                            ADD COLUMN semester TEXT NOT NULL DEFAULT ''
                        """.trimIndent())
                        Log.d(TAG, "✅ Added semester column")
                    }

                    // Add description column if not exists
                    if (!existingColumns.contains("description")) {
                        database.execSQL("""
                            ALTER TABLE subjects 
                            ADD COLUMN description TEXT NOT NULL DEFAULT ''
                        """.trimIndent())
                        Log.d(TAG, "✅ Added description column")
                    }

                    // Add updatedAt column if not exists
                    if (!existingColumns.contains("updatedAt")) {
                        database.execSQL("""
                            ALTER TABLE subjects 
                            ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0
                        """.trimIndent())
                        Log.d(TAG, "✅ Added updatedAt column")

                        // Update existing records with current timestamp
                        val currentTime = System.currentTimeMillis()
                        database.execSQL("""
                            UPDATE subjects 
                            SET updatedAt = $currentTime
                            WHERE updatedAt = 0
                        """.trimIndent())
                        Log.d(TAG, "✅ Updated existing records with timestamp")
                    }

                    Log.d(TAG, "✅ Migration 4 -> 5 completed successfully!")

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Migration 4 -> 5 failed: ${e.message}", e)
                    throw e
                }
            }
        }

        /**
         * Clear database instance (useful for testing)
         */
        fun clearInstance() {
            INSTANCE?.close()
            INSTANCE = null
            Log.d(TAG, "Database instance cleared")
        }

        /**
         * Get database version
         */
        fun getDatabaseVersion(context: Context): Int {
            val db = getDatabase(context)
            return db.openHelper.readableDatabase.version
        }
    }
}

/**
 * Extension function to check if database needs migration
 */
fun Context.isDatabaseMigrationNeeded(): Boolean {
    val db = getDatabasePath(AppDatabase.DATABASE_NAME)
    return db.exists()
}

/**
 * Extension function to get database file size
 */
fun Context.getDatabaseSize(): Long {
    val db = getDatabasePath(AppDatabase.DATABASE_NAME)
    return if (db.exists()) db.length() else 0L
}