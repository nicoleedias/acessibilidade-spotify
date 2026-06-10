package com.sac.acessibilidade.domain.gesture

import kotlinx.coroutines.flow.Flow

interface GestureMappingRepository {
    fun observeMappings(): Flow<Map<Gesture, SpotifyAction?>>

    suspend fun saveMappings(mappings: Map<Gesture, SpotifyAction?>)

    suspend fun restoreDefaults()
}
