package com.driverapp.ui.ajustes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.driverapp.dados.ConfiguracaoEntity
import com.driverapp.dados.custoCombustivelPorKm
import com.driverapp.dados.listaDias
import com.driverapp.dados.listaPlataformas
import com.driverapp.leitura.EstadoLeitura
import com.driverapp.repositorio
import com.driverapp.ui.Formatos
import com.driverapp.ui.LocalPaleta
import com.driverapp.ui.Medidas
import com.driverapp.ui.cadastro.AcoesEtapa
import com.driverapp.ui.cadastro.EtapaCombustivel
import com.driverapp.ui.cadastro.EtapaDespesas
import com.driverapp.ui.cadastro.EtapaJornadaTrabalho
import com.driverapp.ui.cadastro.EtapaMeta
import com.driverapp.ui.cadastro.EtapaPerfil
import com.driverapp.ui.cadastro.EtapaPlataformas
import com.driverapp.ui.cadastro.EtapaVeiculo
import com.driverapp.ui.cadastro.Plataformas
import com.driverapp.ui.calculadora.TelaCalculadora
import com.driverapp.ui.componentes.CartaoVidro
import com.driverapp.ui.componentes.EstadoSalvamento
import com.driverapp.ui.componentes.IndicadorSalvamento
import com.driverapp.ui.componentes.SeletorSegmentado
import com.driverapp.ui.leitura.TelaCardFlutuante
import com.driverapp.ui.leitura.TelaLeitura
import kotlinx.coroutines.launch

/** Ajustes: editar qualquer parte do cadastro, aparência, leitura de corridas e privacidade. */
@Composable
fun TelaAjustes(config: ConfiguracaoEntity) {
    val p = LocalPaleta.current
    val contexto = LocalContext.current
    val repo = contexto.repositorio
    val escopo = rememberCoroutineScope()
    var aberta by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmarApagar by remember { mutableStateOf(false) }
    var salvamento by remember { mutableStateOf(EstadoSalvamento.NADA) }

    BackHandler(enabled = aberta != null) { aberta = null }

    val acoes = AcoesEtapa(
        textoPrincipal = "Concluído",
        aoConfirmar = { alt ->
            escopo.launch { repo.alterarConfig(alt) }
            aberta = null
            salvamento = EstadoSalvamento.NADA
        },
        aoVoltar = null,
        autoSalvar = { alt ->
            salvamento = EstadoSalvamento.SALVANDO
            escopo.launch {
                repo.alterarConfig(alt)
                salvamento = EstadoSalvamento.SALVO
            }
        },
        rodape = { IndicadorSalvamento(salvamento) },
    )

    when (aberta) {
        "perfil" -> { EtapaPerfil(config, acoes); return }
        "plataformas" -> { EtapaPlataformas(config, acoes); return }
        "combustivel" -> { EtapaCombustivel(config, acoes); return }
        "veiculo" -> { EtapaVeiculo(config, acoes); return }
        "rotina" -> { EtapaJornadaTrabalho(config, acoes); return }
        "despesas" -> { EtapaDespesas(config, acoes); return }
        "meta" -> { EtapaMeta(config, acoes); return }
        "bateria" -> { GuiaBateria { aberta = null }; return }
        "limites" -> { TelaLimites(config) { aberta = null }; return }
        "leitura" -> { TelaLeitura(config) { aberta = null }; return }
        "card" -> { TelaCardFlutuante { aberta = null }; return }
        "calculadora" -> { TelaCalculadora(config); return }
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = Medidas.margem, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineSmall, color = p.texto)

        Secao("Aparência")
        CartaoVidro(Modifier.fillMaxWidth()) {
            Text("Tema", fontWeight = FontWeight.SemiBold, color = p.texto)
            val opcoes = listOf(null, "CLARO", "ESCURO")
            SeletorSegmentado(
                opcoes = listOf("Sistema", "Claro", "Escuro"),
                selecionado = opcoes.indexOf(config.tema).coerceAtLeast(0),
                aoMudar = { i -> escopo.launch { repo.alterarConfig { it.copy(tema = opcoes[i]) } } },
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        Secao("Leitura de corridas")
        val leituraAtiva = EstadoLeitura.servicoAtivo(contexto)
        Item(
            "Leitura automática das ofertas",
            if (leituraAtiva) "Ativa • toque para ver as últimas leituras" else "Desligada • toque para ativar",
            corDetalhe = if (leituraAtiva) p.verde else p.amarelo,
        ) { aberta = "leitura" }
        Item("Card flutuante", "Transparência, posição e card de teste") { aberta = "card" }
        Item("Limites das cores", "Defina o que é corrida ruim, razoável ou boa") { aberta = "limites" }

        Secao("Cadastro")
        Item("Perfil", if (config.perfil == "ENTREGADOR") "Motoboy / entregador" else "Motorista de carro") { aberta = "perfil" }
        Item("Plataformas", config.listaPlataformas().joinToString(", ") { Plataformas.nome(it) }.ifEmpty { "Nenhuma" }) { aberta = "plataformas" }
        Item(
            "Combustível",
            "${Formatos.moeda(config.precoLitro)}/L • ${config.kmPorLitro?.let { Formatos.numero(it, 1) } ?: "—"} km/l • " +
                "${Formatos.moeda(config.custoCombustivelPorKm())}/km",
        ) { aberta = "combustivel" }
        Item("Veículo", when (config.veiculo) {
            "FINANCIADO" -> "Financiado • ${Formatos.moeda(config.parcelaMensal)}/mês"
            "ALUGADO" -> "Alugado • ${Formatos.moeda(config.aluguelSemanal)}/semana"
            "PROPRIO" -> "Próprio"
            else -> "Não informado"
        }) { aberta = "veiculo" }
        Item(
            "Dias e horas de trabalho",
            "${config.listaDias().size} dia(s) • ${config.horasPorDia?.let { Formatos.numero(it, 1) } ?: "—"} h por dia",
        ) { aberta = "rotina" }
        Item("Custos adicionais", "IPVA, seguro, manutenção, pneus…") { aberta = "despesas" }
        Item("Meta da semana", Formatos.moeda(config.metaSemanal)) { aberta = "meta" }

        Secao("Jornada")
        CartaoVidro(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Contar km durante a pausa", fontWeight = FontWeight.SemiBold, color = p.texto)
                    Text(
                        if (config.contarKmNaPausa) "Ligado: os km da pausa entram no total (aparecem separados no resumo)."
                        else "Desligado (padrão): na pausa o GPS desliga e os km não contam.",
                        style = MaterialTheme.typography.bodySmall, color = p.textoSecundario,
                    )
                }
                Switch(
                    checked = config.contarKmNaPausa,
                    onCheckedChange = { marcado -> escopo.launch { repo.alterarConfig { it.copy(contarKmNaPausa = marcado) } } },
                    colors = SwitchDefaults.colors(checkedTrackColor = p.destaque),
                )
            }
        }
        Item("Funcionar com a tela apagada", "Liberar o app na economia de bateria") { aberta = "bateria" }

        Secao("Ferramentas")
        Item("Calculadora de corrida", "Testar os números de uma oferta") { aberta = "calculadora" }

        Secao("Privacidade")
        Text(
            "Todos os seus dados ficam apenas neste celular. Nada é enviado para servidores.",
            style = MaterialTheme.typography.bodySmall, color = p.textoSecundario,
        )
        Item("Apagar todos os meus dados", "Cadastro, jornadas, corridas e leituras", corDetalhe = p.vermelho) { confirmarApagar = true }
    }

    if (confirmarApagar) {
        AlertDialog(
            onDismissRequest = { confirmarApagar = false },
            title = { Text("Apagar todos os dados?") },
            text = { Text("Isso apaga cadastro, custos, jornadas, corridas e leituras deste celular. Não dá para desfazer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarApagar = false
                    escopo.launch { repo.apagarTudo() }
                }) { Text("Apagar tudo", color = p.vermelho) }
            },
            dismissButton = { TextButton(onClick = { confirmarApagar = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Secao(titulo: String) {
    Column(Modifier.padding(top = 12.dp)) {
        Text(
            titulo.uppercase(), style = MaterialTheme.typography.labelMedium, color = LocalPaleta.current.destaque,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        HorizontalDivider(color = LocalPaleta.current.bordaVidro)
    }
}

@Composable
private fun Item(titulo: String, detalhe: String, corDetalhe: androidx.compose.ui.graphics.Color? = null, aoTocar: () -> Unit) {
    val p = LocalPaleta.current
    CartaoVidro(Modifier.fillMaxWidth().clickable(onClick = aoTocar), preenchimento = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(titulo, fontWeight = FontWeight.SemiBold, color = p.texto)
                Text(detalhe, style = MaterialTheme.typography.bodySmall, color = corDetalhe ?: p.textoSecundario)
            }
            @Suppress("DEPRECATION")
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = p.textoSecundario)
        }
    }
}
