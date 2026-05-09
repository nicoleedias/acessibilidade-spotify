// Root build file: apenas declara plugins. Dependências ficam em app/build.gradle.kts.
plugins {
    alias(libs.plugins.android.application)     apply false
    alias(libs.plugins.kotlin.android)          apply false
    alias(libs.plugins.kotlin.compose)          apply false
    alias(libs.plugins.kotlin.serialization)    apply false
    alias(libs.plugins.ksp)                     apply false
    alias(libs.plugins.hilt)                    apply false
    alias(libs.plugins.ktlint)                  apply false
    alias(libs.plugins.detekt)                  apply false
}
