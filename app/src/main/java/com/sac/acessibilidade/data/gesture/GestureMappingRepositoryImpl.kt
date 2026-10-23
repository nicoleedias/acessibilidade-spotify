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

        /**
         * Nenhum gesto vem associado de fábrica.
         *
         * Um padrão pronto parece conveniente, mas presume quais movimentos a
         * pessoa consegue executar — exatamente o que este app não pode presumir.
         * Começar vazio obriga a passar pela configuração uma vez e sair de lá com
         * um mapeamento que funciona para ela.
         */
        private fun defaultMappings(): Map<Gesture, SpotifyAction?> = Gesture.entries.associateWith { null }
    }
