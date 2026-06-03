package com.sac.acessibilidade.ui.screens

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.sac.acessibilidade.ui.theme.BackgroundDark
import com.sac.acessibilidade.ui.theme.BorderDark
import com.sac.acessibilidade.ui.theme.SacTheme
import com.sac.acessibilidade.ui.theme.SpotifyGreen
import com.sac.acessibilidade.ui.theme.SurfaceDark
import com.sac.acessibilidade.ui.theme.SurfaceVariantDark
import com.sac.acessibilidade.ui.theme.TextMuted
import com.sac.acessibilidade.ui.theme.TextPrimary
import com.sac.acessibilidade.ui.theme.TextSecondary

private val defaultGestureMappings =
    listOf(
        GestureMappingUi(
            gestureName = "Inclinar para Direita",
            selectedAction = "Próxima Faixa",
            icon = Icons.Default.KeyboardArrowRight,
        ),
        GestureMappingUi(
            gestureName = "Inclinar para Esquerda",
            selectedAction = "Faixa Anterior",
            icon = Icons.Default.KeyboardArrowLeft,
        ),
        GestureMappingUi(
            gestureName = "Inclinar para Cima",
            selectedAction = "Tocar / Pausar",
            icon = Icons.Default.KeyboardArrowUp,
        ),
        GestureMappingUi(
            gestureName = "Piscar Olho Direito",
            selectedAction = "Aumentar Volume",
            icon = Icons.Default.Face,
        ),
        GestureMappingUi(
            gestureName = "Piscar Olho Esquerdo",
            selectedAction = "Diminuir Volume",
            icon = Icons.Default.Face,
        ),
    )

private val availableActions =
    listOf(
        "Próxima Faixa",
        "Faixa Anterior",
        "Tocar / Pausar",
        "Aumentar Volume",
        "Diminuir Volume",
        "(Sem ação)",
    )

@Composable
fun GestureConfigScreen(
    initialMappings: List<GestureMappingUi> = defaultGestureMappings,
    onBack: () -> Unit = {},
    onSaveClick: () -> Unit = {},
) {
    // Estado local — será migrado para GestureConfigViewModel + Room no UC03
    var mappings by remember { mutableStateOf(initialMappings) }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            GestureConfigTopBar(onBack = onBack)
        },
        bottomBar = {
            GestureConfigBottomBar(onSaveClick = onSaveClick)
        },
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
            itemsIndexed(mappings) { index, mapping ->
                GestureMappingCard(
                    mapping = mapping,
                    onActionSelected = { action ->
                        mappings =
                            mappings.toMutableList().also {
                                it[index] = it[index].copy(selectedAction = action)
                            }
                    },
                )
            }
        }
    }
}

@Composable
private fun GestureConfigTopBar(onBack: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(BackgroundDark)
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
                .border(
                    width = 0.5.dp,
                    color = BorderDark.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(0.dp),
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier =
                Modifier
                    .size(40.dp)
                    .semantics { contentDescription = "Voltar" },
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null,
                tint = TextPrimary,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.gestures_title),
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
        )
    }
}

@Composable
private fun GestureConfigBottomBar(onSaveClick: () -> Unit) {
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
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics { contentDescription = saveLabel },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = saveLabel,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary,
            )
        }
    }
}

@Composable
private fun GestureMappingCard(
    mapping: GestureMappingUi,
    onActionSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

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
                Icon(
                    imageVector = mapping.icon,
                    contentDescription = null,
                    tint = SpotifyGreen,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = mapping.gestureName,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
        }

        Box {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(BackgroundDark)
                        .border(1.dp, SurfaceVariantDark, RoundedCornerShape(14.dp))
                        .clickable { expanded = true }
                        .semantics {
                            contentDescription =
                                "Ação para ${mapping.gestureName}: ${mapping.selectedAction}. Toque para alterar"
                            role = Role.Button
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = mapping.selectedAction,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(SurfaceDark),
            ) {
                availableActions.forEach { action ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = action,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (action == mapping.selectedAction) SpotifyGreen else TextPrimary,
                            )
                        },
                        onClick = {
                            onActionSelected(action)
                            expanded = false
                        },
                        modifier =
                            Modifier.semantics {
                                contentDescription = action
                            },
                    )
                }
            }
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
private fun GestureConfigScreenPreview() {
    SacTheme {
        GestureConfigScreen()
    }
}
