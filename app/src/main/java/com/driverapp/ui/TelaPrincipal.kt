package com.driverapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.driverapp.dados.ConfiguracaoEntity
import com.driverapp.ui.ajustes.TelaAjustes
import com.driverapp.ui.cadastro.TelaCadastro
import com.driverapp.ui.componentes.FundoApp
import com.driverapp.ui.historico.TelaHistorico
import com.driverapp.ui.inicio.TelaInicio

/** Decide o que mostrar: carregando → cadastro inicial → app com abas. */
@Composable
fun RaizApp(config: ConfiguracaoEntity?) {
    FundoApp {
        when {
            config == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            !config.cadastroConcluido -> Box(Modifier.fillMaxSize().safeDrawingPadding()) { TelaCadastro(config) }
            else -> AppComAbas(config)
        }
    }
}

private data class Aba(val titulo: String, val icone: ImageVector)

@Suppress("DEPRECATION") // Icons.Filled.List: há uma versão "AutoMirrored", mas esta é a do pacote básico
private val abas = listOf(
    Aba("Painel", Icons.Filled.Home),
    Aba("Histórico", Icons.Filled.List),
    Aba("Ajustes", Icons.Filled.Settings),
)

@Composable
private fun AppComAbas(config: ConfiguracaoEntity) {
    val p = LocalPaleta.current
    var aba by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(containerColor = if (p.escuro) Color(0xF00D1424) else Color(0xF7FFFFFF)) {
                abas.forEachIndexed { i, a ->
                    NavigationBarItem(
                        selected = aba == i,
                        onClick = { aba = i },
                        icon = { Icon(a.icone, contentDescription = null) },
                        label = { Text(a.titulo) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = p.destaque,
                            selectedTextColor = p.destaque,
                            indicatorColor = p.destaque.copy(alpha = 0.16f),
                            unselectedIconColor = p.textoSecundario,
                            unselectedTextColor = p.textoSecundario,
                        ),
                    )
                }
            }
        },
    ) { espacos ->
        Box(Modifier.fillMaxSize().padding(espacos)) {
            when (aba) {
                0 -> TelaInicio(config)
                1 -> TelaHistorico()
                else -> TelaAjustes(config)
            }
        }
    }
}
