package com.sac.acessibilidade.spotify.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.sac.acessibilidade.BuildConfig
import com.sac.acessibilidade.spotify.auth.SpotifyAuthInterceptor
import com.sac.acessibilidade.spotify.auth.SpotifyAuthRepository
import com.sac.acessibilidade.spotify.auth.SpotifyAuthRepositoryImpl
import com.sac.acessibilidade.spotify.player.SpotifyCommandRepository
import com.sac.acessibilidade.spotify.player.SpotifyCommandRepositoryImpl
import com.sac.acessibilidade.spotify.player.SpotifyPlayerApi
import com.sac.acessibilidade.spotify.player.SpotifyPlayerRepository
import com.sac.acessibilidade.spotify.player.SpotifyPlayerRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SpotifyModule {
    @Binds
    @Singleton
    abstract fun bindSpotifyAuthRepository(impl: SpotifyAuthRepositoryImpl): SpotifyAuthRepository

    @Binds
    @Singleton
    abstract fun bindSpotifyPlayerRepository(impl: SpotifyPlayerRepositoryImpl): SpotifyPlayerRepository

    @Binds
    @Singleton
    abstract fun bindSpotifyCommandRepository(impl: SpotifyCommandRepositoryImpl): SpotifyCommandRepository

    companion object {
        @Provides
        @Singleton
        @Named("spotify_client_id")
        fun provideSpotifyClientId(): String = BuildConfig.SPOTIFY_CLIENT_ID

        @Provides
        @Singleton
        @UnauthenticatedOkHttpClient
        fun provideUnauthenticatedOkHttpClient(): OkHttpClient =
            OkHttpClient.Builder()
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

        @Provides
        @Singleton
        @AuthenticatedOkHttpClient
        fun provideAuthenticatedOkHttpClient(
            @UnauthenticatedOkHttpClient base: OkHttpClient,
            authInterceptor: SpotifyAuthInterceptor,
        ): OkHttpClient = base.newBuilder().addInterceptor(authInterceptor).build()

        @Provides
        @Singleton
        fun provideRetrofit(
            @AuthenticatedOkHttpClient client: OkHttpClient,
            json: Json,
        ): Retrofit =
            Retrofit.Builder()
                .client(client)
                .baseUrl("https://api.spotify.com/v1/")
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()

        @Provides
        @Singleton
        fun provideSpotifyPlayerApi(retrofit: Retrofit): SpotifyPlayerApi =
            retrofit.create(SpotifyPlayerApi::class.java)

        @Provides
        @Singleton
        fun provideJson(): Json = Json { ignoreUnknownKeys = true }
    }
}
