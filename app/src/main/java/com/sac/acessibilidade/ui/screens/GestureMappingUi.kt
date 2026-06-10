package com.sac.acessibilidade.ui.screens

import androidx.compose.ui.graphics.vector.ImageVector
import com.sac.acessibilidade.domain.gesture.Gesture

data class GestureMappingUi(
    val gesture: Gesture,
    val gestureName: String,
    val selectedAction: String,
    val icon: ImageVector,
)
