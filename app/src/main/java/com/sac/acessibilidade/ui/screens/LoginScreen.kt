package com.sac.acessibilidade.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sac.acessibilidade.R
import com.sac.acessibilidade.ui.theme.BackgroundDark
import com.sac.acessibilidade.ui.theme.BorderDark
import com.sac.acessibilidade.ui.theme.SacTheme
import com.sac.acessibilidade.ui.theme.SpotifyGreen
import com.sac.acessibilidade.ui.theme.SurfaceDark
import com.sac.acessibilidade.ui.theme.TextDisabled
import com.sac.acessibilidade.ui.theme.TextMuted
import com.sac.acessibilidade.ui.theme.TextPrimary
import com.sac.acessibilidade.ui.theme.TextSecondary

@Composable
fun LoginScreen(onConnectClick: () -> Unit = {}) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .systemBarsPadding(),
    ) {
        // Glow verde simulado via radialGradient (sem Modifier.blur — compatível API 29+)
        Box(
            modifier =
                Modifier
                    .size(320.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 80.dp)
                    .background(
                        brush =
                            Brush.radialGradient(
                                colors = listOf(SpotifyGreen.copy(alpha = 0.13f), Color.Transparent),
                            ),
                        shape = CircleShape,
                    ),
        )

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(96.dp))

            // Logo SAC — cartão com gradiente escuro, fiel ao Figma
            Box(
                modifier =
                    Modifier
                        .size(112.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            brush =
                                Brush.linearGradient(
                                    colors = listOf(SurfaceDark, BackgroundDark),
                                    start = Offset(0f, 0f),
                                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                                ),
                        )
                        .border(1.dp, BorderDark, RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "SAC",
                    style = MaterialTheme.typography.displayLarge,
                    color = TextPrimary,
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text =
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = TextMuted)) {
                            append("Acesse sua conta para começar. Utilizamos o padrão ")
                        }
                        withStyle(SpanStyle(color = TextSecondary, fontWeight = FontWeight.Bold)) {
                            append("OAuth 2.0")
                        }
                        withStyle(SpanStyle(color = TextMuted)) {
                            append(" para uma autenticação 100% segura.")
                        }
                    },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.weight(1f))

            val connectLabel = stringResource(R.string.login_button_connect)
            Button(
                onClick = onConnectClick,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .semantics { contentDescription = connectLabel },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
            ) {
                // TODO: substituir por ícone vetorial oficial do Spotify (assets/ic_spotify.svg)
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = TextPrimary,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = connectLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.login_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = TextDisabled,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
private fun LoginScreenPreview() {
    SacTheme {
        LoginScreen()
    }
}
