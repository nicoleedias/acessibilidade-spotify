package com.sac.acessibilidade.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// O SAC usa dark theme como padrão (documento_interface.md §4) para reduzir
// cansaço visual e manter identidade visual alinhada ao Spotify.
private val SacDarkColorScheme =
    darkColorScheme(
        primary = SpotifyGreen,
        onPrimary = TextPrimary,
        primaryContainer = SpotifyGreenDim,
        onPrimaryContainer = TextPrimary,
        background = BackgroundDark,
        onBackground = TextPrimary,
        surface = SurfaceDark,
        onSurface = TextPrimary,
        surfaceVariant = SurfaceVariantDark,
        onSurfaceVariant = TextSecondary,
        outline = BorderDark,
        outlineVariant = SurfaceVariantDark,
        error = ErrorRed,
        onError = TextPrimary,
    )

@Composable
fun SacTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SacDarkColorScheme,
        typography = SacTypography,
        content = content,
    )
}
