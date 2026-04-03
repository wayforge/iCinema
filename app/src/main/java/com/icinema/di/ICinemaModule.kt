package com.icinema.di

import com.google.gson.Gson
import com.icinema.data.api.CmsApiService
import com.icinema.data.repository.CmsRepositoryImpl
import com.icinema.data.repository.ICmsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ICinemaModule {

    private const val BASE_URL = "https://caiji.dyttzyapi.com/"

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

    @Provides
    @Singleton
    fun provideRetrofit(gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideCmsApiService(retrofit: Retrofit): CmsApiService {
        return retrofit.create(CmsApiService::class.java)
    }


    @Provides
    @Singleton
    fun provideCmsRepository(apiService: CmsApiService, categoryDao: com.icinema.data.local.dao.CategoryDao): ICmsRepository {
        return CmsRepositoryImpl(apiService, categoryDao)
    }
}