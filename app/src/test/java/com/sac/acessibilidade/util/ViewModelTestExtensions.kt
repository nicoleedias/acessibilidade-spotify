package com.sac.acessibilidade.util

import androidx.lifecycle.ViewModel

/**
 * Invoca `onCleared()` numa ViewModel a partir do teste.
 *
 * `ViewModel.clear()` é `internal` ao módulo androidx.lifecycle e `onCleared()`
 * é `protected`, então não há API pública para simular a destruição da tela.
 * A reflexão fica isolada aqui em vez de espalhada pelos testes.
 */
fun ViewModel.invokeOnCleared() {
    val method = ViewModel::class.java.getDeclaredMethod("onCleared")
    method.isAccessible = true
    method.invoke(this)
}
