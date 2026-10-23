package com.sac.acessibilidade.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sac.acessibilidade.R
import com.sac.acessibilidade.ui.theme.BackgroundDark
import com.sac.acessibilidade.ui.theme.BorderDark
import com.sac.acessibilidade.ui.theme.ErrorRed
import com.sac.acessibilidade.ui.theme.NodifyTheme
import com.sac.acessibilidade.ui.theme.SpotifyGreen
import com.sac.acessibilidade.ui.theme.SurfaceDark
import com.sac.acessibilidade.ui.theme.TextMuted
import com.sac.acessibilidade.ui.theme.TextPrimary
import com.sac.acessibilidade.ui.theme.TextSecondary

/**
 * Navegação da biblioteca com um item por vez em foco.
 *
 * O item ocupa quase a tela inteira de propósito: quem usa o app controla por
 * gesto da cabeça, muitas vezes a certa distância do aparelho, e não tem como
 * mirar num alvo pequeno. Os botões embaixo existem como caminho alternativo
 * para quem puder tocar na tela.
 */
@Composable
fun LibraryScreen(
    uiState: LibraryUiState = LibraryUiState(),
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    onSelect: () -> Unit = {},
    onBack: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
    ) {
        LibraryHeader(
            title = uiState.openPlaylistName ?: stringResource(R.string.library_title),
            onBack = onBack,
        )

        if (!uiState.blinkReliable) BlinkBlockedWarning()

        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when {
                uiState.isLoading -> LoadingIndicator()
                uiState.errorMessage != null -> ErrorState(uiState.errorMessage, onRetry)
                uiState.isEmpty -> EmptyState()
                else -> FocusedItem(uiState)
            }
        }

        if (!uiState.isLoading && uiState.itemCount > 0) {
            GestureHint(uiState.level)
            Spacer(modifier = Modifier.height(12.dp))
            LibraryControls(level = uiState.level, onPrevious = onPrevious, onNext = onNext, onSelect = onSelect)
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun LibraryHeader(
    title: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark)
                    .clickable(onClick = onBack)
                    .semantics {
                        contentDescription = "Voltar"
                        role = Role.Button
                    },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** O item em foco: capa grande, título grande e a posição na lista. */
@Composable
private fun FocusedItem(uiState: LibraryUiState) {
    AnimatedContent(
        targetState = uiState.focusedIndex,
        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
        label = "focused_item",
    ) { _ ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            CoverArt(uiState.focusedImageUrl)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = uiState.focusedTitle.orEmpty(),
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            uiState.focusedSubtitle?.let { subtitulo ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            uiState.positionLabel?.let { posicao ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = posicao, style = MaterialTheme.typography.labelMedium, color = TextMuted)
            }
        }
    }
}

@Composable
private fun CoverArt(imageUrl: String?) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth(0.72f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderDark, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = stringResource(R.string.library_cover),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(64.dp),
            )
        }
    }
}

@Composable
private fun GestureHint(level: LibraryLevel) {
    val texto =
        if (level == LibraryLevel.PLAYLISTS) {
            stringResource(R.string.library_hint_playlists)
        } else {
            stringResource(R.string.library_hint_tracks)
        }
    Text(
        text = texto,
        style = MaterialTheme.typography.labelSmall,
        color = TextMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Caminho alternativo ao gesto — exigência de acessibilidade, não enfeite. */
@Composable
private fun LibraryControls(
    level: LibraryLevel,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelect: () -> Unit,
) {
    val selectLabel =
        if (level == LibraryLevel.PLAYLISTS) {
            stringResource(R.string.library_open)
        } else {
            stringResource(R.string.library_play)
        }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            label = stringResource(R.string.library_previous),
            onClick = onPrevious,
        )
        ControlButton(
            icon = Icons.Default.PlayArrow,
            label = selectLabel,
            onClick = onSelect,
            highlighted = true,
            size = 72.dp,
        )
        ControlButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            label = stringResource(R.string.library_next),
            onClick = onNext,
        )
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    highlighted: Boolean = false,
    size: androidx.compose.ui.unit.Dp = 56.dp,
) {
    Box(
        modifier =
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(if (highlighted) SpotifyGreen else SurfaceDark)
                .clickable(onClick = onClick)
                .semantics {
                    contentDescription = label
                    role = Role.Button
                },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(if (highlighted) 36.dp else 28.dp),
        )
    }
}

@Composable
private fun LoadingIndicator() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = SpotifyGreen, strokeWidth = 3.dp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.library_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )
    }
}

@Composable
private fun EmptyState() {
    Text(
        text = stringResource(R.string.library_empty),
        style = MaterialTheme.typography.bodyLarge,
        color = TextMuted,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = ErrorRed,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Box(
            modifier =
                Modifier
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark)
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 28.dp)
                    .semantics {
                        contentDescription = "Tentar de novo"
                        role = Role.Button
                    },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.library_retry),
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary,
            )
        }
    }
}

/** Avisa que a piscada parou de funcionar, em vez de deixar o usuário repetindo em vão. */
@Composable
private fun BlinkBlockedWarning() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .semantics(mergeDescendants = true) {},
    ) {
        Text(
            text = stringResource(R.string.library_blink_blocked),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
private fun LibraryScreenPreview() {
    NodifyTheme {
        LibraryScreen(uiState = LibraryUiState(isLoading = false))
    }
}
