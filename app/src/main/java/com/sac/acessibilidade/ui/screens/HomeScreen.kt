package com.sac.acessibilidade.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

@Composable
fun HomeScreen(
    uiState: HomeUiState = HomeUiState(),
    onStartTrackingClick: () -> Unit = {},
    onCalibrateClick: () -> Unit = {},
    onConfigureGesturesClick: () -> Unit = {},
) {
    val userName = uiState.userName ?: "Usuário"
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Header: logo SAC + badge "Spotify Conectado"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SacLogoMini()
            SpotifyConnectedBadge()
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Saudação
        Text(
            text = stringResource(R.string.home_greeting, userName),
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Cartões de ação
        HomeActionCard(
            title = stringResource(R.string.home_action_start_tracking),
            subtitle = stringResource(R.string.home_action_start_tracking_desc),
            icon = Icons.Default.PlayArrow,
            iconTint = SpotifyGreen,
            iconBackground = SpotifyGreen.copy(alpha = 0.10f),
            onClick = onStartTrackingClick,
        )
        Spacer(modifier = Modifier.height(12.dp))
        HomeActionCard(
            title = stringResource(R.string.home_action_calibrate),
            subtitle = stringResource(R.string.home_action_calibrate_desc),
            icon = Icons.Default.Face,
            iconTint = Color(0xFF2B7FFF),
            iconBackground = Color(0xFF2B7FFF).copy(alpha = 0.10f),
            onClick = onCalibrateClick,
        )
        Spacer(modifier = Modifier.height(12.dp))
        HomeActionCard(
            title = stringResource(R.string.home_action_configure_gestures),
            subtitle = stringResource(R.string.home_action_configure_gestures_desc),
            icon = Icons.Default.Settings,
            iconTint = Color(0xFFAD46FF),
            iconBackground = Color(0xFFAD46FF).copy(alpha = 0.10f),
            onClick = onConfigureGesturesClick,
        )

        if (uiState.nowPlayingTitle != null) {
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Tocando agora",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
            )
            Spacer(modifier = Modifier.height(8.dp))
            NowPlayingMiniCard(
                title = uiState.nowPlayingTitle,
                artist = uiState.nowPlayingArtist.orEmpty(),
                isPlaying = uiState.isPlaying,
            )
        }
    }
}

@Composable
private fun SacLogoMini() {
    Box(
        modifier =
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderDark, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "SAC",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
        )
    }
}

@Composable
private fun SpotifyConnectedBadge() {
    Row(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(SurfaceDark)
                .border(1.dp, BorderDark, CircleShape)
                .padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .background(SpotifyGreen, CircleShape),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.home_status_connected),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
        )
    }
}

@Composable
private fun HomeActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .clickable(onClick = onClick)
                .semantics {
                    contentDescription = title
                    role = Role.Button
                }
                .padding(horizontal = 21.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Ícone em círculo colorido
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .background(iconBackground, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(26.dp),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                )
            }
        }

        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = SurfaceVariantDark,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun NowPlayingMiniCard(
    title: String,
    artist: String,
    isPlaying: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .background(SurfaceVariantDark, RoundedCornerShape(10.dp)),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                maxLines = 1,
            )
            if (artist.isNotBlank()) {
                Text(
                    text = artist,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                    maxLines = 1,
                )
            }
        }
        if (isPlaying) {
            Box(
                modifier =
                    Modifier
                        .size(10.dp)
                        .background(SpotifyGreen, CircleShape),
            )
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
private fun HomeScreenPreview() {
    SacTheme {
        HomeScreen()
    }
}
