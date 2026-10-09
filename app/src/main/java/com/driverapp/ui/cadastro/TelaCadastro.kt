package com.driverapp.ui.cadastro

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.driverapp.dados.ConfiguracaoEntity
import com.driverapp.repositorio
import kotlinx.coroutines.launch

private const val TOTAL_ETAPAS = 6

/**
 * Cadastro inicial em 6 etapas curtas, com barra de progresso e botão voltar.
 * Cada avanço/volta salva no banco: se o app fechar, continua de onde parou.
 */
@Composable
fun TelaCadastro(config: ConfiguracaoEntity) {
    val repo = LocalContext.current.repositorio
    val escopo = rememberCoroutineScope()
    var etapa by rememberSaveable { mutableIntStateOf(config.etapaCadastro.coerceIn(0, TOTAL_ETAPAS - 1)) }

    fun irPara(nova: Int, alteracao: Alteracao) {
        val ultima = etapa == TOTAL_ETAPAS - 1 && nova >= TOTAL_ETAPAS
        escopo.launch {
            repo.alterarConfig { atual ->
                alteracao(atual).copy(
                    etapaCadastro = nova.coerceIn(0, TOTAL_ETAPAS - 1),
                    cadastroConcluido = atual.cadastroConcluido || ultima,
                )
            }
        }
        if (!ultima) etapa = nova.coerceIn(0, TOTAL_ETAPAS - 1)
    }

    BackHandler(enabled = etapa > 0) { etapa -= 1 }

    val topo: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
            Text(
                "Etapa ${etapa + 1} de $TOTAL_ETAPAS",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { (etapa + 1) / TOTAL_ETAPAS.toFloat() },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }

    val acoes = AcoesEtapa(
        textoPrincipal = if (etapa == TOTAL_ETAPAS - 1) "Concluir" else "Continuar",
        aoConfirmar = { alt -> irPara(etapa + 1, alt) },
        aoVoltar = if (etapa > 0) { alt -> irPara(etapa - 1, alt) } else null,
        topo = topo,
    )

    when (etapa) {
        0 -> EtapaPerfil(config, acoes)
        1 -> EtapaCombustivel(config, acoes)
        2 -> EtapaVeiculo(config, acoes)
        3 -> EtapaJornadaTrabalho(config, acoes)
        4 -> EtapaDespesas(config, acoes)
        else -> EtapaMeta(config, acoes)
    }
}
