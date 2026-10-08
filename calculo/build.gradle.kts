import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Módulo de cálculo: Kotlin puro, sem nada de Android.
// Assim as fórmulas podem ser testadas no computador / no GitHub, sem celular.
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
