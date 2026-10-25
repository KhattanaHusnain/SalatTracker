package com.salattracker.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.salattracker.BuildConfig
import com.salattracker.data.remote.api.DriveApiService
import com.salattracker.data.remote.api.OverpassService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DriveRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OverpassRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val DRIVE_BASE_URL = "https://www.googleapis.com/"
    private const val OVERPASS_BASE_URL = "https://overpass-api.de/"

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    // The shared OkHttpClient logs full request/response bodies whenever ENABLE_LOGGING
    // is on (true for every build type today) — never acceptable for a client carrying
    // a bearer token and the user's backed-up prayer/goal data. Drive gets its own
    // client with no logging interceptor at all, and longer timeouts for the upload.
    @Provides
    @Singleton
    @DriveRetrofit
    fun provideDriveOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    @DriveRetrofit
    fun provideDriveRetrofit(@DriveRetrofit okHttpClient: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(DRIVE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideDriveApiService(@DriveRetrofit retrofit: Retrofit): DriveApiService =
        retrofit.create(DriveApiService::class.java)

    // Own client for the same reason as Drive: the shared one logs full bodies, and these requests
    // carry the user's (rounded) coordinates. Overpass also asks clients to identify themselves.
    @Provides
    @Singleton
    @OverpassRetrofit
    fun provideOverpassOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.ENABLE_LOGGING) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "Istiqamah/${BuildConfig.VERSION_NAME} (Android)")
                        .build()
                )
            }
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(40, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @OverpassRetrofit
    fun provideOverpassRetrofit(@OverpassRetrofit okHttpClient: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(OVERPASS_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideOverpassService(@OverpassRetrofit retrofit: Retrofit): OverpassService =
        retrofit.create(OverpassService::class.java)
}
