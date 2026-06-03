package com.sac.acessibilidade.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val ovalWidth = maxWidth * 0.78f
        val ovalHeight = maxHeight * 0.56f

        // Câmera ou fundo escuro
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
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Overlay + oval + setas + progresso
        FaceGuideOverlay(
            uiState = uiState,
            ovalWidth = ovalWidth,
            ovalHeight = ovalHeight,
            modifier = Modifier.fillMaxSize(),
        )

        // Header
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
                        .background(Color.Black.copy(alpha = 0.45f))
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

        // Instrução animada
        AnimatedContent(
            targetState = instructionFor(uiState.step),
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            label = "instruction",
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 72.dp),
        ) { text ->
            Box(
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 22.dp, vertical = 11.dp),
            ) {
                Text(text = text, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
            }
        }

        // Indicador de etapas
        if (uiState.step != CalibrationStep.NEUTRAL && uiState.step != CalibrationStep.DONE) {
            StepDots(
                current = uiState.step,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp),
            )
        }

        // Botão apenas em NEUTRAL e DONE
        if (uiState.step == CalibrationStep.NEUTRAL || uiState.step == CalibrationStep.DONE) {
            CalibrationButton(
                uiState = uiState,
                onAdvance = onAdvance,
                onConfirm = onConfirm,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(BackgroundDark)
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun FaceGuideOverlay(
    uiState: CalibrationUiState,
    ovalWidth: Dp,
    ovalHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.holdProgress,
        animationSpec = tween(80),
        label = "hold_progress",
    )
    val progressColor by animateColorAsState(
        targetValue = if (uiState.step == CalibrationStep.DONE) SpotifyGreen else SpotifyGreen,
        label = "progress_color",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Máscara escura
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color.Black.copy(alpha = 0.50f))
        }

        // Oval guide + progresso
        Box(
            modifier = Modifier.size(ovalWidth + 48.dp, ovalHeight + 48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(ovalWidth, ovalHeight)) {
                val strokeWidth = 2.5.dp.toPx()
                val progressStroke = 5.dp.toPx()

                // Oval tracejado base
                drawOval(
                    color = Color.White.copy(alpha = 0.45f),
                    style =
                        Stroke(
                            width = strokeWidth,
                            pathEffect =
                                PathEffect.dashPathEffect(
                                    intervals = floatArrayOf(14.dp.toPx(), 8.dp.toPx()),
                                    phase = 0f,
                                ),
                        ),
                )

                // Arco de progresso sólido (fill clockwise)
                if (animatedProgress > 0f) {
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        topLeft = Offset(progressStroke / 2, progressStroke / 2),
                        size = Size(size.width - progressStroke, size.height - progressStroke),
                        style = Stroke(width = progressStroke, cap = StrokeCap.Round),
                    )
                }
            }

            // Setas direcionais nos quatro lados
            DirectionArrow(
                icon = Icons.Default.KeyboardArrowUp,
                contentDescription = "Incline para cima",
                isActive = uiState.step == CalibrationStep.TILT_UP,
                modifier = Modifier.align(Alignment.TopCenter).size(56.dp),
            )
            DirectionArrow(
                icon = Icons.Default.KeyboardArrowDown,
                contentDescription = "Incline para baixo",
                isActive = uiState.step == CalibrationStep.TILT_DOWN,
                modifier = Modifier.align(Alignment.BottomCenter).size(52.dp),
            )
            DirectionArrow(
                icon = Icons.Default.KeyboardArrowLeft,
                contentDescription = "Incline para a esquerda",
                isActive = uiState.step == CalibrationStep.TILT_LEFT,
                modifier = Modifier.align(Alignment.CenterStart).size(52.dp),
            )
            DirectionArrow(
                icon = Icons.Default.KeyboardArrowRight,
                contentDescription = "Incline para a direita",
                isActive = uiState.step == CalibrationStep.TILT_RIGHT,
                modifier = Modifier.align(Alignment.CenterEnd).size(52.dp),
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
        targetValue = if (isActive) SpotifyGreen else Color.White.copy(alpha = 0.12f),
        animationSpec = tween(300),
        label = "arrow_bg",
    )
    Box(modifier = modifier.clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) Color.White else Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(30.dp),
        )
    }
}

@Composable
private fun StepDots(
    current: CalibrationStep,
    modifier: Modifier = Modifier,
) {
    val steps =
        listOf(
            CalibrationStep.TILT_RIGHT,
            CalibrationStep.TILT_LEFT,
            CalibrationStep.TILT_UP,
            CalibrationStep.TILT_DOWN,
        )
    val currentIndex = steps.indexOf(current)
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        steps.forEachIndexed { i, _ ->
            val filled = i <= currentIndex
            Box(
                modifier =
                    Modifier
                        .size(if (i == currentIndex) 10.dp else 7.dp)
                        .clip(CircleShape)
                        .background(if (filled) SpotifyGreen else Color.White.copy(alpha = 0.3f)),
            )
        }
    }
}

@Composable
private fun CalibrationButton(
    uiState: CalibrationUiState,
    onAdvance: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (uiState.step == CalibrationStep.DONE) {
            val doneLabel = "Concluído"
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth().height(56.dp).semantics { contentDescription = doneLabel },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TextPrimary, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, tint = TextPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(doneLabel, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                }
            }
        } else {
            val confirmLabel = stringResource(R.string.calibration_confirm)
            Button(
                onClick = onAdvance,
                modifier = Modifier.fillMaxWidth().height(56.dp).semantics { contentDescription = confirmLabel },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = TextPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Começar", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
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

private fun instructionFor(step: CalibrationStep): String =
    when (step) {
        CalibrationStep.NEUTRAL -> "Posicione o rosto no oval e toque em Começar"
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
