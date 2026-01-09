package smart.study.planner.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import smart.study.planner.data.network.HolidayApiService
import javax.inject.Singleton

/**
 * Hilt module for providing network dependencies (Retrofit)
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    // Base URL for the API
    // Note: Update this to the actual API endpoint
    // The user mentioned: https://github.com/binbin26/API_LTMobile.git
    // If using GitHub raw content: "https://raw.githubusercontent.com/binbin26/API_LTMobile/main/"
    // If using a JSONPlaceholder-style API: "https://your-api-server.com/"
    // If using GitHub Pages: "https://binbin26.github.io/API_LTMobile/"
    private const val BASE_URL = "https://raw.githubusercontent.com/binbin26/API_LTMobile/main/"
    
    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideHolidayApiService(retrofit: Retrofit): HolidayApiService {
        return retrofit.create(HolidayApiService::class.java)
    }
}
