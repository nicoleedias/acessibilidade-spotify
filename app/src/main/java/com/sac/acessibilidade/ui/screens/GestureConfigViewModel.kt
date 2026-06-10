package com.sac.acessibilidade.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sac.acessibilidade.domain.gesture.Gesture
import com.sac.acessibilidade.domain.gesture.GestureMappingRepository
import com.sac.acessibilidade.domain.gesture.NO_ACTION_LABEL
import com.sac.acessibilidade.domain.gesture.SpotifyAction
import com.sac.acessibilidade.domain.gesture.displayName
import com.sac.acessibilidade.domain.gesture.toSpotifyActionOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GestureConfigViewModel
    @Inject
    constructor(
        private val repository: GestureMappingRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(GestureConfigUiState())
        val uiState: StateFlow<GestureConfigUiState> = _uiState.asStateFlow()

        init {
            repository
                .observeMappings()
                .onEach { mappings -> _uiState.update { it.copy(mappings = mappings.toUiList()) } }
                .launchIn(viewModelScope)
        }

        fun updateMapping(
            gesture: Gesture,
            actionLabel: String,
        ) {
            _uiState.update { state ->
                val updated =
                    state.mappings.map { item ->
                        if (item.gesture == gesture) item.copy(selectedAction = actionLabel) else item
                    }
                state.copy(mappings = updated)
            }
        }

        fun save(onDone: () -> Unit) {
            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true) }
                val domainMap =
                    _uiState.value.mappings.associate { item ->
                        item.gesture to item.selectedAction.toSpotifyActionOrNull()
                    }
                repository.saveMappings(domainMap)
                _uiState.update { it.copy(isSaving = false) }
                onDone()
            }
        }

        fun restoreDefaults() {
            viewModelScope.launch {
                repository.restoreDefaults()
            }
        }
    }

private fun Map<Gesture, SpotifyAction?>.toUiList(): List<GestureMappingUi> =
    Gesture.entries.map { gesture ->
        val action = this[gesture]
        GestureMappingUi(
            gesture = gesture,
            gestureName = gesture.displayName(),
            selectedAction = action?.displayName() ?: NO_ACTION_LABEL,
            icon = gesture.defaultIcon(),
        )
    }

private fun Gesture.defaultIcon() =
    when (this) {
        Gesture.TILT_HEAD_RIGHT, Gesture.TURN_FACE_RIGHT -> Icons.Default.KeyboardArrowRight
        Gesture.TILT_HEAD_LEFT, Gesture.TURN_FACE_LEFT -> Icons.Default.KeyboardArrowLeft
        Gesture.TILT_HEAD_UP -> Icons.Default.KeyboardArrowUp
        Gesture.TILT_HEAD_DOWN -> Icons.Default.KeyboardArrowDown
        Gesture.NOD -> Icons.Default.KeyboardArrowDown
        Gesture.BLINK_RIGHT_EYE, Gesture.BLINK_LEFT_EYE -> Icons.Default.Face
    }
