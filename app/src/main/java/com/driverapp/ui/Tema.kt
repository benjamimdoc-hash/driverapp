package com.driverapp.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Cores das classificações (iguais no tema claro e escuro). */
object CoresClassificacao {
    val vermelho = Color(0xFFE5484D)
    val amarelo = Color(0xFFF5B83D)
    val verde = Color(0xFF30A46C)
}

/**
 * Tema NEUTRO provisório: acompanha o claro/escuro do celular.
 * A identidade visual definitiva será criada depois.
 */
@Composable
fun TemaApp(content: @Composable () -> Unit) {
    val escuro = isSystemInDarkTheme()
    val cores = if (escuro) {
        darkColorScheme(
            primary = Color(0xFF8AB4F8),
            background = Color(0xFF111418),
            surface = Color(0xFF111418),
            surfaceVariant = Color(0xFF1E2329),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF1F5FBF),
            background = Color(0xFFF7F8FA),
            surface = Color(0xFFF7F8FA),
            surfaceVariant = Color(0xFFE9ECF0),
        )
    }
    MaterialTheme(colorScheme = cores, content = content)
}
