package com.sac.acessibilidade.ui.navigation

import android.content.Intent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.sac.acessibilidade.ui.screens.CalibrationScreen
import com.sac.acessibilidade.ui.screens.CalibrationViewModel
import com.sac.acessibilidade.ui.screens.GestureConfigScreen
import com.sac.acessibilidade.ui.screens.GestureConfigViewModel
import com.sac.acessibilidade.ui.screens.HomeScreen
import com.sac.acessibilidade.ui.screens.HomeViewModel
import com.sac.acessibilidade.ui.screens.LoginScreen
import com.sac.acessibilidade.ui.screens.LoginUiState
import com.sac.acessibilidade.ui.screens.LoginViewModel
import com.sac.acessibilidade.ui.screens.PlayerAtivoScreen
import com.sac.acessibilidade.ui.screens.PlayerAtivoViewModel

@Composable
fun SacNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(220)) },
        exitTransition = { fadeOut(animationSpec = tween(180)) },
        popEnterTransition = { fadeIn(animationSpec = tween(220)) },
        popExitTransition = { fadeOut(animationSpec = tween(180)) },
    ) {
        composable(Screen.Login.route) {
            val viewModel: LoginViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current

            // Se o token ainda é válido, pula a tela de login
            LaunchedEffect(Unit) {
                if (viewModel.isAlreadyLoggedIn) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            }

            // Navega para Home após troca de token bem-sucedida
            LaunchedEffect(uiState) {
                if (uiState is LoginUiState.Success) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            }

            LoginScreen(
                uiState = uiState,
                onConnectClick = {
                    val authUri = viewModel.buildAuthUri()
                    context.startActivity(Intent(Intent.ACTION_VIEW, authUri))
                },
                onErrorDismiss = viewModel::clearError,
            )
        }

        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            HomeScreen(
                uiState = uiState,
                onStartTrackingClick = {
                    navController.navigate(Screen.PlayerAtivo.route)
                },
                onCalibrateClick = {
                    navController.navigate(Screen.Calibration.route)
                },
                onConfigureGesturesClick = {
                    navController.navigate(Screen.GestureConfig.route)
                },
            )
        }

        composable(Screen.Calibration.route) {
            val viewModel: CalibrationViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            CalibrationScreen(
                uiState = uiState,
                poseAnalyzer = viewModel.poseAnalyzer,
                onStartCalibration = viewModel::startNeutralCapture,
                onConfirmPosition = viewModel::confirmPosition,
                onConfirm = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.GestureConfig.route) {
            val viewModel: GestureConfigViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            GestureConfigScreen(
                uiState = uiState,
                onMappingChanged = viewModel::updateMapping,
                onSaveClick = { viewModel.save { navController.popBackStack() } },
                onRestoreDefaults = viewModel::restoreDefaults,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.PlayerAtivo.route) {
            val viewModel: PlayerAtivoViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            PlayerAtivoScreen(
                uiState = uiState,
                gestureProcessor = viewModel.gestureProcessor,
                onStopTracking = { navController.popBackStack() },
                onPlayPauseClick = viewModel::togglePlayPause,
                onSkipNextClick = viewModel::skipToNext,
                onSkipPreviousClick = viewModel::skipToPrevious,
                onVolumeUpClick = viewModel::volumeUp,
                onVolumeDownClick = viewModel::volumeDown,
            )
        }
    }
}
