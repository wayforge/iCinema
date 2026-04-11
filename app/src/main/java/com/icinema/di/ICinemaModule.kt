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
import javax.inject.Named
import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext

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
    @Named("player_prefs")
    fun providePlayerPrefs(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("player_settings", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    @Named("session_prefs")
    fun provideSessionPrefs(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideCmsRepository(
        apiService: CmsApiService,
        categoryDao: com.icinema.data.local.dao.CategoryDao,
        playbackHistoryDao: com.icinema.data.local.dao.PlaybackHistoryDao,
        favoriteDao: com.icinema.data.local.dao.FavoriteDao,
        searchHistoryDao: com.icinema.data.local.dao.SearchHistoryDao,
        downloadTaskDao: com.icinema.data.local.dao.DownloadTaskDao,
        @Named("session_prefs") sessionPrefs: SharedPreferences
    ): ICmsRepository {
        return CmsRepositoryImpl(
            apiService,
            categoryDao,
            playbackHistoryDao,
            favoriteDao,
            searchHistoryDao,
            downloadTaskDao,
            sessionPrefs
        )
    }
}