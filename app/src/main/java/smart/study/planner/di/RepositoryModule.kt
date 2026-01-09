package smart.study.planner.di

import smart.study.planner.data.repository.EventRepositoryImpl
import smart.study.planner.data.repository.UserRepositoryImpl
import smart.study.planner.data.repository.AuthRepositoryImpl
import smart.study.planner.data.repository.PreferencesRepositoryImpl
import smart.study.planner.data.repository.SubjectRepositoryImpl
import smart.study.planner.domain.repository.EventRepository
import smart.study.planner.domain.repository.UserRepository
import smart.study.planner.domain.repository.AuthRepository
import smart.study.planner.domain.repository.PreferencesRepository
import smart.study.planner.domain.repository.SubjectRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing repository implementations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindEventRepository(
        eventRepositoryImpl: EventRepositoryImpl
    ): EventRepository
    
    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository
    
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
    
    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(
        preferencesRepositoryImpl: PreferencesRepositoryImpl
    ): PreferencesRepository
    
    @Binds
    @Singleton
    abstract fun bindSubjectRepository(
        subjectRepositoryImpl: SubjectRepositoryImpl
    ): SubjectRepository
}

