package com.sac.acessibilidade.data.gesture

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gesture_mappings")
data class GestureMappingEntity(
    @PrimaryKey val gesture: String,
    val action: String?,
)
