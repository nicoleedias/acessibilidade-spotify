package com.sac.acessibilidade.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sac.acessibilidade.R
import com.sac.acessibilidade.ui.theme.BackgroundDark
import com.sac.acessibilidade.ui.theme.ErrorRed
import com.sac.acessibilidade.ui.theme.SacTheme
import com.sac.acessibilidade.ui.theme.SpotifyGreen
import com.sac.acessibilidade.ui.theme.SurfaceDark
import com.sac.acessibilidade.ui.theme.SurfaceVariantDark
import com.sac.acessibilidade.ui.theme.TextMuted
import com.sac.acessibilidade.ui.theme.TextPrimary

@Composable
fun PlayerAtivoScreen(
    uiState: PlayerAtivoUiState = PlayerAtivoUiState(),
    onStopTracking: () -> Unit = {},
    onPlayPauseClick: () -> Unit = {},
    onSkipNextClick: () -> Unit = {},
    onSkipPreviousClick: () -> Unit = {},
    onVolumeUpClick: () -> Unit = {},
    onVolumeDownClick: () -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxSize()) {
        CameraFeedPlaceholder()

        // Gradiente no topo para legibilidade do header
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent),
                            ),
                    ),
        )

        // Header com indicador de rastreamento ativo
        TrackingHeader(
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
        )

        // Pill de feedback de gesto (visível apenas quando um gesto é detectado)
        if (uiState.hasDetectedGesture) {
            GestureFeedbackPill(
                gestureName = uiState.lastGestureName,
                gestureAction = uiState.lastGestureAction,
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 32.dp),
            )
        }

        // Painel inferior: NowPlayingCard + botão Parar
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(BackgroundDark)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(76.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = SpotifyGreen,
                            strokeWidth = 2.dp,
                        )
                    }
                }
                uiState.error != null -> {
                    Text(
                        text = uiState.error,
                        style = MaterialTheme.typography.labelMedium,
                        color = ErrorRed,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> {
                    NowPlayingCard(
                        trackTitle = uiState.trackTitle,
                        trackArtist = uiState.trackArtist,
                        albumArtUrl = uiState.albumArtUrl,
                        isPlaying = uiState.isPlaying,
                        onPlayPauseClick = onPlayPauseClick,
                    )
                    PlaybackControlsRow(
                        volumePercent = uiState.volumePercent,
                        onSkipPreviousClick = onSkipPreviousClick,
                        onSkipNextClick = onSkipNextClick,
                        onVolumeDownClick = onVolumeDownClick,
                        onVolumeUpClick = onVolumeUpClick,
                    )
                    if (uiState.commandError != null) {
                        Text(
                            text = uiState.commandError,
                            style = MaterialTheme.typography.labelSmall,
                            color = ErrorRed,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            val stopLabel = stringResource(R.string.player_ativo_stop_button)
            val stopDesc = stringResource(R.string.player_ativo_stop_button_description)
            Button(
                onClick = onStopTracking,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .semantics { contentDescription = stopDesc },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stopLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun CameraFeedPlaceholder() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A1A))
                .semantics { contentDescription = "Feed da câmera frontal para rastreamento de gestos" },
    )
}

@Composable
private fun TrackingHeader(modifier: Modifier = Modifier) {
    val isInPreview = LocalInspectionMode.current
    val infiniteTransition = rememberInfiniteTransition(label = "dot_pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1000),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "dot_alpha",
    )

    val statusDesc = stringResource(R.string.player_ativo_status_tracking)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .semantics { stateDescription = statusDesc },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(10.dp)
                            .alpha(if (isInPreview) 1f else dotAlpha)
                            .background(SpotifyGreen, CircleShape),
                )
                Text(
                    text = stringResource(R.string.player_ativo_header),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun GestureFeedbackPill(
    gestureName: String,
    gestureAction: String,
    modifier: Modifier = Modifier,
) {
    val pillDesc = "$gestureName → $gestureAction"
    Box(
        modifier =
            modifier
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .semantics { contentDescription = pillDesc },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = pillDesc,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
        )
    }
}

@Composable
private fun NowPlayingCard(
    trackTitle: String,
    trackArtist: String,
    albumArtUrl: String?,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
) {
    val playDesc = stringResource(R.string.player_ativo_play_button_description)
    val pauseDesc = stringResource(R.string.player_ativo_pause_button_description)
    val albumDesc = stringResource(R.string.player_ativo_album_art_description)

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = albumArtUrl,
            contentDescription = albumDesc,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceVariantDark),
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = trackTitle,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = trackArtist,
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        IconButton(onClick = onPlayPauseClick) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) pauseDesc else playDesc,
                tint = SpotifyGreen,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun PlaybackControlsRow(
    volumePercent: Int,
    onSkipPreviousClick: () -> Unit,
    onSkipNextClick: () -> Unit,
    onVolumeDownClick: () -> Unit,
    onVolumeUpClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onSkipPreviousClick) {
            Icon(Icons.Default.SkipPrevious, contentDescription = "Faixa anterior", tint = TextPrimary)
        }
        IconButton(onClick = onSkipNextClick) {
            Icon(Icons.Default.SkipNext, contentDescription = "Próxima faixa", tint = TextPrimary)
        }
        IconButton(onClick = onVolumeDownClick) {
            Icon(Icons.Default.VolumeDown, contentDescription = "Volume -5", tint = TextPrimary)
        }
        Text(
            text = "$volumePercent%",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
        )
        IconButton(onClick = onVolumeUpClick) {
            Icon(Icons.Default.VolumeUp, contentDescription = "Volume +5", tint = TextPrimary)
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
private fun PlayerAtivoScreenPreview() {
    SacTheme {
        PlayerAtivoScreen()
    }
}

@Suppress("UnusedPrivateMember")
@Preview(showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
private fun PlayerAtivoScreenWithGesturePreview() {
    SacTheme {
        PlayerAtivoScreen(
            uiState =
                PlayerAtivoUiState(
                    trackTitle = "Bohemian Rhapsody",
                    trackArtist = "Queen",
                    isPlaying = true,
                    lastGestureName = "Inclinar para Direita",
                    lastGestureAction = "Próxima Faixa",
                    hasDetectedGesture = true,
                ),
        )
    }
}
