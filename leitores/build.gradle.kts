import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Módulo de leitura: recebe o TEXTO que aparece na tela da Uber/99
// e devolve os dados da oferta. Kotlin puro, testável sem celular.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    testImplementation(libs.junit)
}
