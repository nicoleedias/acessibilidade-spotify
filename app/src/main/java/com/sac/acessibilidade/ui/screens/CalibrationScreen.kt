package com.sac.acessibilidade.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sac.acessibilidade.R
import com.sac.acessibilidade.ui.theme.BackgroundDark
import com.sac.acessibilidade.ui.theme.SacTheme
import com.sac.acessibilidade.ui.theme.SpotifyGreen
import com.sac.acessibilidade.ui.theme.TextPrimary

@Composable
fun CalibrationScreen(
    onBack: () -> Unit = {},
    onConfirm: () -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Placeholder do feed da câmera (substituído por CameraPreview no UC01)
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1A1A1A))
                    .semantics { contentDescription = "Feed da câmera frontal" },
        )

        // Overlay escuro com máscara oval para guiar o posicionamento do rosto
        CameraOverlay(
            modifier = Modifier.fillMaxSize(),
        )

        // Gradiente no topo para legibilidade da status bar
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent),
                            ),
                    ),
        )

        // Header com botão voltar + título (sobreposto ao feed)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
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
                text = stringResource(R.string.calibration_title),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
        }

        // Instrução flutuante centralizada (pill semi-transparente)
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 80.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 17.dp, vertical = 9.dp),
            ) {
                Text(
                    text = stringResource(R.string.calibration_instruction),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary,
                )
            }
        }

        // Botão Confirmar fixado na parte inferior
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(BackgroundDark)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            val confirmLabel = stringResource(R.string.calibration_confirm)
            Button(
                onClick = onConfirm,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .semantics { contentDescription = confirmLabel },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = TextPrimary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = confirmLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                )
            }
        }
    }
}

// Overlay sobre o feed: máscara escura + oval tracejado + 4 setas direcionais
@Composable
private fun CameraOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        // Máscara escura semi-transparente (sobrepõe o feed)
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
        )

        // Contêiner do guia facial com espaço extra para as setas ao redor
        Box(
            modifier = Modifier.size(336.dp, 432.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Oval tracejado — 240×320dp fiel ao Figma
            Canvas(modifier = Modifier.size(240.dp, 320.dp)) {
                drawOval(
                    color = Color.White.copy(alpha = 0.75f),
                    style =
                        Stroke(
                            width = 2.dp.toPx(),
                            pathEffect =
                                PathEffect.dashPathEffect(
                                    intervals = floatArrayOf(12.dp.toPx(), 8.dp.toPx()),
                                    phase = 0f,
                                ),
                        ),
                )
            }

            // Seta para cima
            DirectionArrow(
                icon = Icons.Default.KeyboardArrowUp,
                contentDescription = "Olhe para cima",
                modifier = Modifier.align(Alignment.TopCenter).size(60.dp),
            )
            // Seta para baixo
            DirectionArrow(
                icon = Icons.Default.KeyboardArrowDown,
                contentDescription = "Olhe para baixo",
                modifier = Modifier.align(Alignment.BottomCenter).size(48.dp),
            )
            // Seta para a esquerda
            DirectionArrow(
                icon = Icons.Default.KeyboardArrowLeft,
                contentDescription = "Olhe para a esquerda",
                modifier = Modifier.align(Alignment.CenterStart).size(48.dp),
            )
            // Seta para a direita
            DirectionArrow(
                icon = Icons.Default.KeyboardArrowRight,
                contentDescription = "Olhe para a direita",
                modifier = Modifier.align(Alignment.CenterEnd).size(48.dp),
            )
        }
    }
}

@Composable
private fun DirectionArrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clip(CircleShape)
                .background(SpotifyGreen.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = SpotifyGreen,
            modifier = Modifier.size(32.dp),
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
private fun CalibrationScreenPreview() {
    SacTheme {
        CalibrationScreen()
    }
}
