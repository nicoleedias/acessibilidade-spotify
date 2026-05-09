package com.sac.acessibilidade.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.sac.acessibilidade.ui.screens.CalibrationScreen
import com.sac.acessibilidade.ui.screens.GestureConfigScreen
import com.sac.acessibilidade.ui.screens.HomeScreen
import com.sac.acessibilidade.ui.screens.LoginScreen
import com.sac.acessibilidade.ui.screens.PlayerAtivoScreen

@Composable
fun SacNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        modifier = modifier,
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onConnectClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                // TODO: passar userName real do AuthViewModel [UC01]
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
            CalibrationScreen(
                onBack = { navController.popBackStack() },
                // TODO: onConfirm deve chamar SaveCalibrationUseCase antes de popar [UC02]
                onConfirm = { navController.popBackStack() },
            )
        }

        composable(Screen.GestureConfig.route) {
            GestureConfigScreen(
                onBack = { navController.popBackStack() },
                // TODO: onSaveClick deve chamar SaveGestureMappingsUseCase antes de popar [UC03]
                onSaveClick = { navController.popBackStack() },
            )
        }

        composable(Screen.PlayerAtivo.route) {
            PlayerAtivoScreen(
                onStopTracking = { navController.popBackStack() },
            )
        }
    }
}
