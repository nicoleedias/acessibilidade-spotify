import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose) // obrigatório com Kotlin 2.0+
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.sac.acessibilidade"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sac.acessibilidade"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Lê credenciais do Spotify em local.properties; nunca expostas em VCS
        val localProps =
            Properties().apply {
                val f = rootProject.file("local.properties")
                if (f.exists()) load(f.inputStream())
            }
        buildConfigField(
            "String",
            "SPOTIFY_CLIENT_ID",
            "\"${localProps.getProperty("SPOTIFY_CLIENT_ID", "")}\"",
        )
        buildConfigField(
            "String",
            "SPOTIFY_REDIRECT_URI",
            "\"${localProps.getProperty("SPOTIFY_REDIRECT_URI", "")}\"",
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

detekt {
    config.setFrom(files("$rootDir/config/detekt.yml"))
    buildUponDefaultConfig = true
}

ktlint {
    android.set(true)
    outputColorName.set("RED")
}

dependencies {
    // Core
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)

    // Compose BOM — todas as versões Compose derivam daqui
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation("androidx.compose.material:material-icons-extended")

    // Lifecycle + ViewModel
    implementation(libs.bundles.lifecycle)

    // Navigation
    implementation(libs.navigation.compose)

    // DI — Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Database — Room
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    // Network — Retrofit + OkHttp + kotlinx.serialization
    implementation(libs.bundles.network)

    // Coroutines
    implementation(libs.coroutines.android)

    // Segurança — tokens OAuth nunca em SharedPreferences simples
    implementation(libs.security.crypto)

    // Imagens — album art do Spotify via Coil + OkHttp
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Câmera — CameraX para preview frontal (calibração e rastreamento)
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    // Testes unitários
    testImplementation(libs.junit4)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)

    // Testes instrumentados
    androidTestImplementation(libs.junit.android)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)

    // Debug
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
