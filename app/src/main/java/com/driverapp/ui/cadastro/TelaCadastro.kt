package com.driverapp.ui.cadastro

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.driverapp.dados.ConfiguracaoEntity
import com.driverapp.repositorio
import com.driverapp.ui.componentes.EstadoSalvamento
import com.driverapp.ui.componentes.IndicadorSalvamento
import com.driverapp.ui.componentes.ProgressoEtapas
import kotlinx.coroutines.launch

private const val TOTAL_ETAPAS = 7

/**
 * Cadastro inicial em 7 etapas curtas, com barra de progresso, botão voltar e salvamento
 * automático: se o app fechar, ele reabre na etapa em que o motorista parou, com tudo preenchido.
 */
@Composable
fun TelaCadastro(config: ConfiguracaoEntity) {
    val repo = LocalContext.current.repositorio
    val escopo = rememberCoroutineScope()
    var etapa by rememberSaveable { mutableIntStateOf(config.etapaCadastro.coerceIn(0, TOTAL_ETAPAS - 1)) }
    var salvamento by remember { mutableStateOf(EstadoSalvamento.NADA) }

    fun irPara(nova: Int, alteracao: Alteracao) {
        val concluiu = etapa == TOTAL_ETAPAS - 1 && nova >= TOTAL_ETAPAS
        escopo.launch {
            repo.alterarConfig { atual ->
                alteracao(atual).copy(
                    etapaCadastro = nova.coerceIn(0, TOTAL_ETAPAS - 1),
                    cadastroConcluido = atual.cadastroConcluido || concluiu,
                )
            }
        }
        salvamento = EstadoSalvamento.NADA
        if (!concluiu) etapa = nova.coerceIn(0, TOTAL_ETAPAS - 1)
    }

    BackHandler(enabled = etapa > 0) { etapa -= 1 }

    val acoes = AcoesEtapa(
        textoPrincipal = if (etapa == TOTAL_ETAPAS - 1) "Concluir" else "Continuar",
        aoConfirmar = { alt -> irPara(etapa + 1, alt) },
        aoVoltar = if (etapa > 0) { alt -> irPara(etapa - 1, alt) } else null,
        autoSalvar = { alt ->
            salvamento = EstadoSalvamento.SALVANDO
            escopo.launch {
                repo.alterarConfig(alt)
                salvamento = EstadoSalvamento.SALVO
            }
        },
        topo = { ProgressoEtapas(etapa, TOTAL_ETAPAS) },
        rodape = { IndicadorSalvamento(salvamento) },
    )

    AnimatedContent(
        targetState = etapa,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "etapas",
    ) { atual ->
        when (atual) {
            0 -> EtapaPerfil(config, acoes)
            1 -> EtapaPlataformas(config, acoes)
            2 -> EtapaCombustivel(config, acoes)
            3 -> EtapaVeiculo(config, acoes)
            4 -> EtapaJornadaTrabalho(config, acoes)
            5 -> EtapaDespesas(config, acoes)
            else -> EtapaMeta(config, acoes)
        }
    }
}
