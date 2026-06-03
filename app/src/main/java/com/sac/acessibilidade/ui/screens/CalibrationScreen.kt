package com.sac.acessibilidade.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sac.acessibilidade.R
import com.sac.acessibilidade.ui.theme.BackgroundDark
import com.sac.acessibilidade.ui.theme.SacTheme
import com.sac.acessibilidade.ui.theme.SpotifyGreen
import com.sac.acessibilidade.ui.theme.TextPrimary
import androidx.camera.core.Preview as CameraPreviewUseCase

@Composable
fun CalibrationScreen(
    uiState: CalibrationUiState = CalibrationUiState(),
    onAdvance: () -> Unit = {},
    onConfirm: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasCameraPermission = granted
        }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            CameraPreview(modifier = Modifier.fillMaxSize())
        } else {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1A1A1A))
                        .semantics { contentDescription = "Câmera não autorizada" },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Permissão de câmera necessária",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                )
            }
        }

        CameraOverlay(
            modifier = Modifier.fillMaxSize(),
            uiState = uiState,
        )

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
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.calibration_title),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
        }

        // Instrução dinâmica por etapa
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
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(
                    text = instructionFor(uiState.step),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary,
                )
            }
        }

        CalibrationBottomBar(
            uiState = uiState,
            onAdvance = onAdvance,
            onConfirm = onConfirm,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun CalibrationBottomBar(
    uiState: CalibrationUiState,
    onAdvance: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(BackgroundDark)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        if (uiState.step == CalibrationStep.DONE) {
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth().height(56.dp).semantics { contentDescription = "Concluído" },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TextPrimary, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, tint = TextPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Concluído", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                }
            }
        } else {
            val label = if (uiState.step == CalibrationStep.NEUTRAL) "Começar" else "Próximo"
            val confirmLabel = stringResource(R.string.calibration_confirm)
            Button(
                onClick = onAdvance,
                modifier = Modifier.fillMaxWidth().height(56.dp).semantics { contentDescription = confirmLabel },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = TextPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            }
        }
    }
}

@Composable
private fun CameraPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener(
                    {
                        runCatching {
                            val provider = future.get()
                            val preview =
                                CameraPreviewUseCase.Builder().build().also {
                                    it.setSurfaceProvider(
                                        surfaceProvider,
                                    )
                                }
                            provider.unbindAll()
                            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview)
                        }
                    },
                    ContextCompat.getMainExecutor(ctx),
                )
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun CameraOverlay(
    modifier: Modifier = Modifier,
    uiState: CalibrationUiState,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))

        Box(modifier = Modifier.size(336.dp, 432.dp), contentAlignment = Alignment.Center) {
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

            DirectionArrow(
                icon = Icons.Default.KeyboardArrowUp,
                contentDescription = "Incline para cima",
                isActive = uiState.step == CalibrationStep.TILT_UP,
                modifier = Modifier.align(Alignment.TopCenter).size(60.dp),
            )
            DirectionArrow(
                icon = Icons.Default.KeyboardArrowDown,
                contentDescription = "Incline para baixo",
                isActive = uiState.step == CalibrationStep.TILT_DOWN,
                modifier = Modifier.align(Alignment.BottomCenter).size(48.dp),
            )
            DirectionArrow(
                icon = Icons.Default.KeyboardArrowLeft,
                contentDescription = "Incline para a esquerda",
                isActive = uiState.step == CalibrationStep.TILT_LEFT,
                modifier = Modifier.align(Alignment.CenterStart).size(48.dp),
            )
            DirectionArrow(
                icon = Icons.Default.KeyboardArrowRight,
                contentDescription = "Incline para a direita",
                isActive = uiState.step == CalibrationStep.TILT_RIGHT,
                modifier = Modifier.align(Alignment.CenterEnd).size(48.dp),
            )
        }
    }
}

@Composable
private fun DirectionArrow(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(
        targetValue = if (isActive) SpotifyGreen else SpotifyGreen.copy(alpha = 0.15f),
        animationSpec = tween(300),
        label = "arrow_bg",
    )
    Box(
        modifier = modifier.clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) Color.White else SpotifyGreen,
            modifier = Modifier.size(32.dp),
        )
    }
}

private fun instructionFor(step: CalibrationStep): String =
    when (step) {
        CalibrationStep.NEUTRAL -> "Olhe para a frente e toque em Começar"
        CalibrationStep.TILT_RIGHT -> "Incline a cabeça para a DIREITA →"
        CalibrationStep.TILT_LEFT -> "← Incline a cabeça para a ESQUERDA"
        CalibrationStep.TILT_UP -> "↑ Incline a cabeça para CIMA"
        CalibrationStep.TILT_DOWN -> "↓ Incline a cabeça para BAIXO"
        CalibrationStep.DONE -> "✓ Calibração concluída!"
    }

@Suppress("UnusedPrivateMember")
@Preview(showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
private fun CalibrationScreenPreview() {
    SacTheme {
        CalibrationScreen()
    }
}
