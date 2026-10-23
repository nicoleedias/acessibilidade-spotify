package com.sac.acessibilidade.smoke

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sanidade da infraestrutura de teste de interface.
 *
 * Renderiza um único texto. Se ESTE teste falha, o problema não está em nenhuma
 * tela do app: ou o aparelho está com a tela apagada ou bloqueada — nenhuma
 * Activity sobe, e sem Activity não existe hierarquia Compose para inspecionar —
 * ou a configuração de teste quebrou.
 *
 * Foi escrito ao diagnosticar exatamente isso: 64 testes de interface falhavam
 * com "No compose hierarchies found" e a causa era a tela do celular dormindo.
 * Rodar só esta classe separa, em segundos, defeito de produto de defeito de
 * ambiente.
 */
@RunWith(AndroidJUnit4::class)
class ComposeSmokeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun hierarquiaComposeSobe() {
        composeRule.setContent { Text("ok") }
        composeRule.onNodeWithText("ok").assertIsDisplayed()
    }
}
