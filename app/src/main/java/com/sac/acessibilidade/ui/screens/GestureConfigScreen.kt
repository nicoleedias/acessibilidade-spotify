package com.sac.acessibilidade.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sac.acessibilidade.R
import com.sac.acessibilidade.domain.gesture.Gesture
import com.sac.acessibilidade.domain.gesture.NO_ACTION_LABEL
import com.sac.acessibilidade.domain.gesture.SpotifyAction
import com.sac.acessibilidade.domain.gesture.displayName
import com.sac.acessibilidade.ui.theme.BackgroundDark
import com.sac.acessibilidade.ui.theme.BorderDark
import com.sac.acessibilidade.ui.theme.SacTheme
import com.sac.acessibilidade.ui.theme.SpotifyGreen
import com.sac.acessibilidade.ui.theme.SurfaceDark
import com.sac.acessibilidade.ui.theme.SurfaceVariantDark
import com.sac.acessibilidade.ui.theme.TextMuted
import com.sac.acessibilidade.ui.theme.TextPrimary
import com.sac.acessibilidade.ui.theme.TextSecondary

private val availableActions: List<String> =
    SpotifyAction.entries.map { it.displayName() } + listOf(NO_ACTION_LABEL)

@Composable
fun GestureConfigScreen(
    uiState: GestureConfigUiState,
    onMappingChanged: (Gesture, String) -> Unit,
    onSaveClick: () -> Unit,
    onRestoreDefaults: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = BackgroundDark,
        topBar = { GestureConfigTopBar(onBack = onBack, onRestoreDefaults = onRestoreDefaults) },
        bottomBar = { GestureConfigBottomBar(onSaveClick = onSaveClick, isSaving = uiState.isSaving) },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    top = paddingValues.calculateTopPadding() + 8.dp,
                    bottom = paddingValues.calculateBottomPadding() + 8.dp,
                    start = 24.dp,
                    end = 24.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.gestures_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            itemsIndexed(uiState.mappings) { _, mapping ->
                GestureMappingCard(
                    mapping = mapping,
                    onActionSelected = { action -> onMappingChanged(mapping.gesture, action) },
                )
            }
        }
    }
}

@Composable
private fun GestureConfigTopBar(
    onBack: () -> Unit,
    onRestoreDefaults: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(BackgroundDark)
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
                .border(width = 0.5.dp, color = BorderDark.copy(alpha = 0.5f), shape = RoundedCornerShape(0.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(40.dp).semantics { contentDescription = "Voltar" },
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            stringResource(R.string.gestures_title),
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRestoreDefaults) {
            Text(
                text = stringResource(R.string.gestures_restore_defaults),
                style = MaterialTheme.typography.labelMedium,
                color = SpotifyGreen,
            )
        }
    }
}

@Composable
private fun GestureConfigBottomBar(
    onSaveClick: () -> Unit,
    isSaving: Boolean,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(BackgroundDark)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        val saveLabel = stringResource(R.string.gestures_save)
        Button(
            onClick = onSaveClick,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().height(56.dp).semantics { contentDescription = saveLabel },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(saveLabel, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
        }
    }
}

@Composable
private fun GestureMappingCard(
    mapping: GestureMappingUi,
    onActionSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val borderColor = if (expanded) SpotifyGreen else SurfaceVariantDark

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(BackgroundDark)
                        .border(0.5.dp, BorderDark.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(mapping.icon, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(mapping.gestureName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(if (expanded) RoundedCornerShape(14.dp, 14.dp, 0.dp, 0.dp) else RoundedCornerShape(14.dp))
                        .background(BackgroundDark)
                        .border(
                            1.dp,
                            borderColor,
                            if (expanded) RoundedCornerShape(14.dp, 14.dp, 0.dp, 0.dp) else RoundedCornerShape(14.dp),
                        )
                        .clickable { expanded = !expanded }
                        .semantics {
                            contentDescription =
                                "Ação para ${mapping.gestureName}: ${mapping.selectedAction}. Toque para alterar"
                            role = Role.Button
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(mapping.selectedAction, style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (expanded) SpotifyGreen else TextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(0.dp, 0.dp, 14.dp, 14.dp))
                            .background(BackgroundDark)
                            .border(
                                1.dp,
                                SpotifyGreen,
                                RoundedCornerShape(0.dp, 0.dp, 14.dp, 14.dp),
                            ),
                ) {
                    availableActions.forEachIndexed { i, action ->
                        val isSelected = action == mapping.selectedAction
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onActionSelected(action)
                                        expanded = false
                                    }
                                    .semantics {
                                        contentDescription = action
                                        role = Role.Button
                                    }
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = action,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) SpotifyGreen else TextPrimary,
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = SpotifyGreen,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        if (i < availableActions.lastIndex) {
                            HorizontalDivider(color = SurfaceVariantDark.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
private fun GestureConfigScreenPreview() {
    val previewMappings =
        Gesture.entries.map { gesture ->
            GestureMappingUi(
                gesture = gesture,
                gestureName = gesture.name,
                selectedAction = NO_ACTION_LABEL,
                icon = Icons.Default.Face,
            )
        }
    SacTheme {
        GestureConfigScreen(
            uiState = GestureConfigUiState(mappings = previewMappings),
            onMappingChanged = { _, _ -> },
            onSaveClick = {},
            onRestoreDefaults = {},
            onBack = {},
        )
    }
}
