package com.sac.acessibilidade.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import com.sac.acessibilidade.ui.theme.TextMuted
import com.sac.acessibilidade.ui.theme.TextPrimary
import com.sac.acessibilidade.vision.CalibrationPoseAnalyzer
import androidx.camera.core.Preview as CameraPreviewUseCase

private val directionSteps =
    listOf(
        CalibrationStep.TILT_RIGHT,
        CalibrationStep.TILT_LEFT,
        CalibrationStep.TILT_UP,
        CalibrationStep.TILT_DOWN,
        CalibrationStep.TURN_RIGHT,
        CalibrationStep.TURN_LEFT,
    )

@Composable
fun CalibrationScreen(
    uiState: CalibrationUiState = CalibrationUiState(),
    poseAnalyzer: CalibrationPoseAnalyzer? = null,
    onStartCalibration: () -> Unit = {},
    onConfirmPosition: () -> Unit = {},
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

        if (hasCameraPermission) {
            CameraPreview(analyzer = poseAnalyzer, modifier = Modifier.fillMaxSize())
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.calibration_title),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
        }

        // Instrução dinâmica com fade entre etapas
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

        // Indicador de ângulo ao vivo (visível apenas durante os passos de direção)
        if (uiState.step in directionSteps && uiState.currentAngleDeg > 0.5f) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 116.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                uiState.isHolding -> SpotifyGreen.copy(alpha = 0.85f)
                                uiState.isAtLimit -> SpotifyGreen.copy(alpha = 0.55f)
                                else -> Color.Black.copy(alpha = 0.5f)
                            },
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .semantics { contentDescription = "${uiState.currentAngleDeg.toInt()} graus" },
            ) {
                Text(
                    text = "${uiState.currentAngleDeg.toInt()}°",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary,
                )
            }
        }

        // Aviso quando nenhum rosto é detectado (não conflita com o ângulo, que exige rosto)
        if (!uiState.faceDetected) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 116.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFC62828).copy(alpha = 0.85f))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .semantics { contentDescription = "Rosto não detectado" },
            ) {
                Text(
                    text = "Rosto não detectado",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary,
                )
            }
        }

        CalibrationBottomPanel(
            uiState = uiState,
            onStartCalibration = onStartCalibration,
            onConfirmPosition = onConfirmPosition,
            onConfirm = onConfirm,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        )
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
        targetValue = if (uiState.isCapturingNeutral) uiState.neutralProgress else uiState.holdProgress,
        animationSpec = tween(60),
        label = "hold_progress",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color.Black.copy(alpha = 0.50f))
        }

        Box(
            modifier = Modifier.size(ovalWidth + 48.dp, ovalHeight + 48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(ovalWidth, ovalHeight)) {
                val baseStroke = 2.5.dp.toPx()
                val progressStroke = 5.dp.toPx()

                drawOval(
                    color = Color.White.copy(alpha = if (uiState.isHolding) 0.25f else 0.50f),
                    style =
                        Stroke(
                            width = baseStroke,
                            pathEffect =
                                PathEffect.dashPathEffect(
                                    intervals = floatArrayOf(14.dp.toPx(), 8.dp.toPx()),
                                    phase = 0f,
                                ),
                        ),
                )

                if (animatedProgress > 0f) {
                    drawArc(
                        color = SpotifyGreen,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        topLeft = Offset(progressStroke / 2, progressStroke / 2),
                        size = Size(size.width - progressStroke, size.height - progressStroke),
                        style = Stroke(width = progressStroke, cap = StrokeCap.Round),
                    )
                }
            }

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
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Incline ou vire para a esquerda",
                isActive = uiState.step == CalibrationStep.TILT_LEFT || uiState.step == CalibrationStep.TURN_LEFT,
                modifier = Modifier.align(Alignment.CenterStart).size(52.dp),
            )
            DirectionArrow(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Incline ou vire para a direita",
                isActive = uiState.step == CalibrationStep.TILT_RIGHT || uiState.step == CalibrationStep.TURN_RIGHT,
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
            tint = if (isActive) Color.White else Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(30.dp),
        )
    }
}

@Composable
private fun CalibrationBottomPanel(
    uiState: CalibrationUiState,
    onStartCalibration: () -> Unit,
    onConfirmPosition: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .background(BackgroundDark)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        uiState.retryMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFFFB74D),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        when (uiState.step) {
            CalibrationStep.NEUTRAL -> {
                Button(
                    onClick = onStartCalibration,
                    enabled = !uiState.isCapturingNeutral,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                ) {
                    if (uiState.isCapturingNeutral) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = TextPrimary,
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Capturando posição neutra...",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary,
                        )
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, tint = TextPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Começar calibração",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary,
                        )
                    }
                }
            }

            CalibrationStep.TILT_RIGHT,
            CalibrationStep.TILT_LEFT,
            CalibrationStep.TILT_UP,
            CalibrationStep.TILT_DOWN,
            CalibrationStep.TURN_RIGHT,
            CalibrationStep.TURN_LEFT,
            -> {
                val stepIndex = directionSteps.indexOf(uiState.step) + 1
                StepDots(current = uiState.step, modifier = Modifier.align(Alignment.CenterHorizontally))
                Text(
                    text = "Passo $stepIndex de ${directionSteps.size} · ${holdHintFor(uiState)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = onConfirmPosition,
                    enabled = !uiState.isHolding && uiState.isAtLimit && uiState.faceDetected,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                ) {
                    if (uiState.isHolding) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = TextPrimary,
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Mantendo posição...", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, tint = TextPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Confirmar posição atual",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary,
                        )
                    }
                }
            }

            CalibrationStep.DONE -> {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = TextPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, tint = TextPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Concluído", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepDots(
    current: CalibrationStep,
    modifier: Modifier = Modifier,
) {
    val currentIndex = directionSteps.indexOf(current)
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        directionSteps.forEachIndexed { i, _ ->
            Box(
                modifier =
                    Modifier
                        .size(if (i == currentIndex) 10.dp else 7.dp)
                        .clip(CircleShape)
                        .background(if (i <= currentIndex) SpotifyGreen else Color.White.copy(alpha = 0.3f)),
            )
        }
    }
}

@Composable
private fun CameraPreview(
    analyzer: CalibrationPoseAnalyzer?,
    modifier: Modifier = Modifier,
) {
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
                                    it.setSurfaceProvider(surfaceProvider)
                                }
                            provider.unbindAll()
                            if (analyzer != null) {
                                val imageAnalysis =
                                    ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                        .also { it.setAnalyzer(ContextCompat.getMainExecutor(ctx), analyzer) }
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_FRONT_CAMERA,
                                    preview,
                                    imageAnalysis,
                                )
                            } else {
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_FRONT_CAMERA,
                                    preview,
                                )
                            }
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
        CalibrationStep.NEUTRAL -> "Olhe para a frente, fique parado e toque em Começar"
        CalibrationStep.TILT_RIGHT -> "Incline a cabeça para a DIREITA o máximo confortável →"
        CalibrationStep.TILT_LEFT -> "← Incline a cabeça para a ESQUERDA o máximo confortável"
        CalibrationStep.TILT_UP -> "↑ Incline a cabeça para CIMA o máximo confortável"
        CalibrationStep.TILT_DOWN -> "↓ Incline a cabeça para BAIXO o máximo confortável"
        CalibrationStep.TURN_RIGHT -> "Vire o rosto para a DIREITA o máximo confortável →"
        CalibrationStep.TURN_LEFT -> "← Vire o rosto para a ESQUERDA o máximo confortável"
        CalibrationStep.DONE -> "✓ Calibração concluída!"
    }

private fun holdHintFor(uiState: CalibrationUiState): String =
    when {
        uiState.isHolding -> "Mantenha a posição..."
        uiState.isAtLimit -> "Pronto! Confirme a posição"
        else -> "Vá até o limite confortável do movimento"
    }

@Suppress("UnusedPrivateMember")
@Preview(showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
private fun CalibrationScreenPreview() {
    SacTheme {
        CalibrationScreen()
    }
}
