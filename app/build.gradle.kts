plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Guarda o "desenho" do banco a cada versão, para criar migrações sem perder dados.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.driverapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.driverapp"
        minSdk = 31          // Android 12 ou superior
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0-fase3"

        // Só os processadores de celulares reais (deixa o APK bem menor).
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }

    // Chave de teste FIXA (fica no repositório). Assim cada APK novo instala por cima
    // do anterior sem precisar desinstalar. NÃO é a chave de produção da Play Store.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
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
    }
}

dependencies {
    implementation(project(":calculo"))
    implementation(project(":leitores"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Leitura de texto em imagens, 100% no celular (usado só no "testar com uma imagem").
    implementation(libs.mlkit.texto)

    testImplementation(libs.junit)
}
