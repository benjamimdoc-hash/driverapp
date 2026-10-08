pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "driverapp"

// Módulos do projeto:
// - calculo  → fórmulas financeiras (Kotlin puro, testável sem celular)
// - leitores → transforma o texto da tela da Uber/99 em dados (Kotlin puro)
// - app      → o aplicativo Android (telas, serviços, permissões)
include(":calculo")
include(":leitores")
include(":app")
