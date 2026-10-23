package com.sac.acessibilidade.ui.screens

import com.sac.acessibilidade.domain.gesture.Gesture
import com.sac.acessibilidade.domain.gesture.NO_ACTION_LABEL
import com.sac.acessibilidade.domain.gesture.SpotifyAction

data class GestureConfigUiState(
    val mappings: List<GestureMappingUi> = emptyList(),
    val isSaving: Boolean = false,
) {
    /**
     * Rótulo da ação para o nome do gesto que já a utiliza.
     *
     * Uma ação pertence a no máximo um gesto (RN02): se "Próxima Faixa" está em
     * "Inclinar para Direita", nenhum outro gesto pode escolhê-la enquanto ela
     * não for liberada. "(Sem ação)" fica de fora, porque vários gestos podem
     * estar sem ação ao mesmo tempo.
     */
    val actionOwners: Map<String, String>
        get() =
            mappings
                .filter { it.selectedAction != NO_ACTION_LABEL }
                .associate { it.selectedAction to it.gestureName }

    /** Quantas das ações disponíveis já têm um gesto. */
    val assignedCount: Int get() = actionOwners.size

    val totalActions: Int get() = SpotifyAction.entries.size

    /** Nenhum gesto configurado: o rastreamento não teria o que disparar. */
    val hasNoMapping: Boolean get() = mappings.isNotEmpty() && assignedCount == 0

    /**
     * Gesto que impede este de escolher [actionLabel], ou null se estiver livre.
     * A ação que o próprio gesto já usa continua selecionável, senão o usuário
     * não conseguiria ver qual é a escolha atual dele.
     */
    fun blockedBy(
        gesture: Gesture,
        actionLabel: String,
    ): String? {
        if (actionLabel == NO_ACTION_LABEL) return null
        val dono = actionOwners[actionLabel] ?: return null
        val nomeDoGesto = mappings.firstOrNull { it.gesture == gesture }?.gestureName
        return if (dono == nomeDoGesto) null else dono
    }
}
