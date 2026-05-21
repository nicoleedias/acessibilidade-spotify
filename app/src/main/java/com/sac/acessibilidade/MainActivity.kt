package com.sac.acessibilidade

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.sac.acessibilidade.spotify.auth.SpotifyAuthRepository
import com.sac.acessibilidade.ui.navigation.SacNavHost
import com.sac.acessibilidade.ui.theme.SacTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var authRepository: SpotifyAuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Trata o caso de o app ser aberto diretamente via redirect URI
        handleSpotifyCallback(intent)
        setContent {
            SacTheme {
                val navController = rememberNavController()
                SacNavHost(navController = navController)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSpotifyCallback(intent)
    }

    private fun handleSpotifyCallback(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "sac" && uri.host == "callback") {
            val code = uri.getQueryParameter("code") ?: return
            authRepository.notifyAuthCode(code)
        }
    }
}
