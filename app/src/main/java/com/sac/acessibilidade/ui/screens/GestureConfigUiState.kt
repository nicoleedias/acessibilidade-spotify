package com.sac.acessibilidade.ui.screens

data class GestureConfigUiState(
    val mappings: List<GestureMappingUi> = emptyList(),
    val isSaving: Boolean = false,
)
