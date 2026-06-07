package com.rombaro.tv.di

import android.content.Context
import androidx.room.Room
import com.rombaro.tv.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideOkHttp(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "rombaro.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun playlistDao(db: AppDatabase) = db.playlists()
    @Provides fun channelDao(db: AppDatabase) = db.channels()
    @Provides fun programmeDao(db: AppDatabase) = db.programmes()
    @Provides fun favoriteDao(db: AppDatabase) = db.favorites()
}
