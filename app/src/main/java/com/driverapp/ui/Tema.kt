package com.driverapp.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driverapp.calculo.Cor

/**
 * SISTEMA DE DESIGN — tudo que é cor, forma e espaçamento vem daqui.
 * Nenhuma tela deve usar cores "soltas": use [Paleta] via `LocalPaleta.current` ou o MaterialTheme.
 */
@Immutable
data class Paleta(
    val escuro: Boolean,
    val fundo: Color,
    val fundoBrilho: Color,
    /** Superfície de vidro (translúcida) dos cards. */
    val vidro: Color,
    val vidroForte: Color,
    val bordaVidro: Color,
    val texto: Color,
    val textoSecundario: Color,
    val destaque: Color,
    val ciano: Color,
    val verde: Color,
    val amarelo: Color,
    val vermelho: Color,
    /** Cor neutra para "sem dados" (diferente de desempenho ruim). */
    val neutro: Color,
) {
    val fundoGradiente: Brush
        get() = Brush.verticalGradient(listOf(fundoBrilho, fundo, fundo))

    fun corDe(c: Cor?): Color = when (c) {
        Cor.VERMELHO -> vermelho
        Cor.AMARELO -> amarelo
        Cor.VERDE -> verde
        null -> neutro
    }
}

val PaletaEscura = Paleta(
    escuro = true,
    fundo = Color(0xFF0A0F1A),
    fundoBrilho = Color(0xFF12223A),
    vidro = Color(0x14FFFFFF),
    vidroForte = Color(0x22FFFFFF),
    bordaVidro = Color(0x26FFFFFF),
    texto = Color(0xFFF2F6FC),
    textoSecundario = Color(0xFF9AA8BD),
    destaque = Color(0xFF8FD3FF),
    ciano = Color(0xFF22D3EE),
    verde = Color(0xFF34D399),
    amarelo = Color(0xFFFBBF24),
    vermelho = Color(0xFFF87171),
    neutro = Color(0xFF64748B),
)

val PaletaClara = Paleta(
    escuro = false,
    fundo = Color(0xFFF4F7FB),
    fundoBrilho = Color(0xFFE6F0FC),
    vidro = Color(0xE6FFFFFF),
    vidroForte = Color(0xFFFFFFFF),
    bordaVidro = Color(0xFFE2E8F0),
    texto = Color(0xFF0F172A),
    textoSecundario = Color(0xFF5B6B82),
    destaque = Color(0xFF1D6FE8),
    ciano = Color(0xFF0891B2),
    verde = Color(0xFF059669),
    amarelo = Color(0xFFD97706),
    vermelho = Color(0xFFDC2626),
    neutro = Color(0xFF94A3B8),
)

val LocalPaleta = staticCompositionLocalOf { PaletaEscura }

/** Espaçamentos e raios padronizados. */
object Medidas {
    val margem = 20.dp
    val espaco = 14.dp
    val raioCard = 22.dp
    val raioCampo = 16.dp
    val alturaBotao = 56.dp
}

/** Cores das classificações (usadas também fora do Compose, no card sobreposto). */
object CoresClassificacao {
    val vermelho = PaletaEscura.vermelho
    val amarelo = PaletaEscura.amarelo
    val verde = PaletaEscura.verde
}

/** Resolve a preferência salva: "ESCURO", "CLARO" ou null (seguir o sistema). */
@Composable
fun usarEscuro(preferencia: String?): Boolean = when (preferencia) {
    "ESCURO" -> true
    "CLARO" -> false
    else -> isSystemInDarkTheme()
}

@Composable
fun TemaApp(preferencia: String? = null, content: @Composable () -> Unit) {
    val escuro = usarEscuro(preferencia)
    val p = if (escuro) PaletaEscura else PaletaClara
    val cores = if (escuro) {
        darkColorScheme(
            primary = p.destaque,
            onPrimary = Color(0xFF04263F),
            primaryContainer = Color(0xFF16385C),
            onPrimaryContainer = p.texto,
            secondary = p.ciano,
            background = p.fundo,
            onBackground = p.texto,
            surface = Color(0xFF111A2B),
            onSurface = p.texto,
            surfaceVariant = Color(0xFF1A2538),
            onSurfaceVariant = p.textoSecundario,
            surfaceContainerHigh = Color(0xFF162236),
            outline = Color(0xFF334155),
            outlineVariant = Color(0xFF243044),
            error = p.vermelho,
        )
    } else {
        lightColorScheme(
            primary = p.destaque,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFDCEBFF),
            onPrimaryContainer = Color(0xFF0B2A55),
            secondary = p.ciano,
            background = p.fundo,
            onBackground = p.texto,
            surface = Color.White,
            onSurface = p.texto,
            surfaceVariant = Color(0xFFEFF3F8),
            onSurfaceVariant = p.textoSecundario,
            surfaceContainerHigh = Color.White,
            outline = Color(0xFFCBD5E1),
            outlineVariant = Color(0xFFE2E8F0),
            error = p.vermelho,
        )
    }
    val base = Typography()
    val tipografia = base.copy(
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp),
    )
    CompositionLocalProvider(LocalPaleta provides p) {
        MaterialTheme(colorScheme = cores, typography = tipografia, content = content)
    }
}

/** Estilo para números principais (com brilho neon só no tema escuro). */
@Composable
fun estiloNumeroDestaque(tamanho: Int = 40, cor: Color = LocalPaleta.current.texto): TextStyle {
    val p = LocalPaleta.current
    return TextStyle(
        fontSize = tamanho.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
        color = cor,
        shadow = if (p.escuro) androidx.compose.ui.graphics.Shadow(color = cor.copy(alpha = 0.55f), blurRadius = 24f) else null,
    )
}
