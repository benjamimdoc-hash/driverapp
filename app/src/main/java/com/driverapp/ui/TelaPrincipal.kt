package com.driverapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.driverapp.dados.ConfiguracaoEntity
import com.driverapp.repositorio
import com.driverapp.ui.ajustes.TelaAjustes
import com.driverapp.ui.cadastro.TelaCadastro
import com.driverapp.ui.historico.TelaHistorico
import com.driverapp.ui.inicio.TelaInicio

/** Decide o que mostrar: carregando → cadastro inicial → app com abas. */
@Composable
fun RaizApp() {
    val repo = LocalContext.current.repositorio
    val config by repo.config.collectAsStateWithLifecycle<ConfiguracaoEntity?>(initialValue = null)
    val c = config

    when {
        c == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        !c.cadastroConcluido -> Box(Modifier.fillMaxSize().safeDrawingPadding()) { TelaCadastro(c) }
        else -> AppComAbas(c)
    }
}

private data class Aba(val titulo: String, val icone: ImageVector)

@Suppress("DEPRECATION") // Icons.Filled.List: a versão "AutoMirrored" não existe no pacote básico de ícones
private val abas = listOf(
    Aba("Início", Icons.Filled.Home),
    Aba("Histórico", Icons.Filled.List),
    Aba("Ajustes", Icons.Filled.Settings),
)

@Composable
private fun AppComAbas(config: ConfiguracaoEntity) {
    var aba by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                abas.forEachIndexed { i, a ->
                    NavigationBarItem(
                        selected = aba == i,
                        onClick = { aba = i },
                        icon = { Icon(a.icone, contentDescription = null) },
                        label = { Text(a.titulo) },
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
