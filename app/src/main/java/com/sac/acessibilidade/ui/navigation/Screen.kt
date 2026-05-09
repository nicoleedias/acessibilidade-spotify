package com.sac.acessibilidade.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")

    data object Home : Screen("home")

    data object Calibration : Screen("calibration")

    data object GestureConfig : Screen("gesture_config")

    data object PlayerAtivo : Screen("player_ativo")
}
