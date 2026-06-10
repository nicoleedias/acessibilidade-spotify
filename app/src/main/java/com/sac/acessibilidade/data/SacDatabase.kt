package com.sac.acessibilidade.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sac.acessibilidade.data.gesture.GestureMappingDao
import com.sac.acessibilidade.data.gesture.GestureMappingEntity

@Database(
    entities = [GestureMappingEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class SacDatabase : RoomDatabase() {
    abstract fun gestureMappingDao(): GestureMappingDao
}
