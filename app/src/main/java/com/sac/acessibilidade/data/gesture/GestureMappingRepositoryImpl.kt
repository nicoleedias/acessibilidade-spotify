package com.sac.acessibilidade.data.gesture

import com.sac.acessibilidade.domain.gesture.Gesture
import com.sac.acessibilidade.domain.gesture.GestureMappingRepository
import com.sac.acessibilidade.domain.gesture.SpotifyAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GestureMappingRepositoryImpl
    @Inject
    constructor(
        private val dao: GestureMappingDao,
    ) : GestureMappingRepository {
        override fun observeMappings(): Flow<Map<Gesture, SpotifyAction?>> =
            dao.observeAll().map { entities ->
                if (entities.isEmpty()) {
                    defaultMappings()
                } else {
                    entities.associate { entity ->
                        val gesture = enumValueOf<Gesture>(entity.gesture)
                        val action = entity.action?.let { enumValueOf<SpotifyAction>(it) }
                        gesture to action
                    }
                }
            }

        override suspend fun saveMappings(mappings: Map<Gesture, SpotifyAction?>) {
            val entities =
                mappings.map { (gesture, action) ->
                    GestureMappingEntity(gesture = gesture.name, action = action?.name)
                }
            dao.upsertAll(entities)
        }

        override suspend fun restoreDefaults() {
            val entities =
                defaultMappings().map { (gesture, action) ->
                    GestureMappingEntity(gesture = gesture.name, action = action?.name)
                }
            dao.clearAll()
            dao.upsertAll(entities)
        }

        private fun defaultMappings(): Map<Gesture, SpotifyAction?> =
            mapOf(
                Gesture.TILT_HEAD_RIGHT to SpotifyAction.VOLUME_UP,
                Gesture.TILT_HEAD_LEFT to SpotifyAction.VOLUME_DOWN,
                Gesture.TILT_HEAD_UP to null,
                Gesture.TILT_HEAD_DOWN to null,
                Gesture.TURN_FACE_RIGHT to SpotifyAction.NEXT_TRACK,
                Gesture.TURN_FACE_LEFT to SpotifyAction.PREVIOUS_TRACK,
                Gesture.NOD to SpotifyAction.PLAY_PAUSE,
                Gesture.BLINK_RIGHT_EYE to null,
                Gesture.BLINK_LEFT_EYE to null,
            )
    }
