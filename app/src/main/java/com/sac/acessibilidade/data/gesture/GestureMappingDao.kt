package com.sac.acessibilidade.data.gesture

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GestureMappingDao {
    @Query("SELECT * FROM gesture_mappings")
    fun observeAll(): Flow<List<GestureMappingEntity>>

    @Upsert
    suspend fun upsertAll(mappings: List<GestureMappingEntity>)

    @Query("DELETE FROM gesture_mappings")
    suspend fun clearAll()
}
