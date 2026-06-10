package com.sac.acessibilidade.data.di

import android.content.Context
import androidx.room.Room
import com.sac.acessibilidade.data.SacDatabase
import com.sac.acessibilidade.data.calibration.CalibrationRepository
import com.sac.acessibilidade.data.calibration.CalibrationRepositoryImpl
import com.sac.acessibilidade.data.gesture.GestureMappingDao
import com.sac.acessibilidade.data.gesture.GestureMappingRepositoryImpl
import com.sac.acessibilidade.domain.gesture.GestureMappingRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindCalibrationRepository(impl: CalibrationRepositoryImpl): CalibrationRepository

    @Binds
    @Singleton
    abstract fun bindGestureMappingRepository(impl: GestureMappingRepositoryImpl): GestureMappingRepository

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(
            @ApplicationContext context: Context,
        ): SacDatabase =
            Room
                .databaseBuilder(context, SacDatabase::class.java, "sac.db")
                .fallbackToDestructiveMigration()
                .build()

        @Provides
        @Singleton
        fun provideGestureMappingDao(db: SacDatabase): GestureMappingDao = db.gestureMappingDao()
    }
}
