package com.sac.acessibilidade.spotify.di

import com.sac.acessibilidade.BuildConfig
import com.sac.acessibilidade.spotify.auth.SpotifyAuthRepository
import com.sac.acessibilidade.spotify.auth.SpotifyAuthRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SpotifyModule {
    @Binds
    @Singleton
    abstract fun bindSpotifyAuthRepository(impl: SpotifyAuthRepositoryImpl): SpotifyAuthRepository

    companion object {
        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .apply {
                    if (BuildConfig.DEBUG) {
                        // BASIC: registra URL e código de resposta, nunca o body com tokens
                        addInterceptor(
                            HttpLoggingInterceptor().apply {
                                level = HttpLoggingInterceptor.Level.BASIC
                            },
                        )
                    }
                }
                .build()
        }

        @Provides
        @Singleton
        fun provideJson(): Json = Json { ignoreUnknownKeys = true }
    }
}
