package smart.study.planner.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import smart.study.planner.data.manager.AuthStateManager
import smart.study.planner.debug.FirebasePermissionTester

/**
 * Hilt module for providing Firebase dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    
    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase {
        return FirebaseDatabase.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideDatabaseReference(database: FirebaseDatabase): DatabaseReference {
        return database.reference
    }
    
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideAuthStateManager(
        firebaseAuth: FirebaseAuth
    ): AuthStateManager {
        return AuthStateManager(firebaseAuth)
    }
    
    @Provides
    @Singleton
    fun provideFirebasePermissionTester(
        firebaseAuth: FirebaseAuth,
        databaseReference: DatabaseReference
    ): FirebasePermissionTester {
        return FirebasePermissionTester(firebaseAuth, databaseReference)
    }
}

