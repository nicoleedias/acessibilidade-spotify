package com.sac.acessibilidade.data.di

import com.sac.acessibilidade.data.calibration.CalibrationRepository
import com.sac.acessibilidade.data.calibration.CalibrationRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindCalibrationRepository(impl: CalibrationRepositoryImpl): CalibrationRepository
}
