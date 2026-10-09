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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import com.driverapp.dados.listaPlataformas
import com.driverapp.repositorio
import com.driverapp.ui.Formatos
import com.driverapp.ui.cadastro.AcoesEtapa
import com.driverapp.ui.cadastro.EtapaCombustivel
import com.driverapp.ui.cadastro.EtapaDespesas
import com.driverapp.ui.cadastro.EtapaJornadaTrabalho
import com.driverapp.ui.cadastro.EtapaMeta
import com.driverapp.ui.cadastro.EtapaPerfil
import com.driverapp.ui.cadastro.EtapaVeiculo
import com.driverapp.ui.cadastro.Plataformas
import com.driverapp.ui.calculadora.TelaCalculadora
import kotlinx.coroutines.launch

/** Ajustes: editar qualquer parte do cadastro e opções do app. */
@Composable
fun TelaAjustes(config: ConfiguracaoEntity) {
    val repo = LocalContext.current.repositorio
    val escopo = rememberCoroutineScope()
    var aberta by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmarApagar by remember { mutableStateOf(false) }

    BackHandler(enabled = aberta != null) { aberta = null }

    val acoes = AcoesEtapa(
        textoPrincipal = "Salvar",
        aoConfirmar = { alt ->
            escopo.launch { repo.alterarConfig(alt) }
            aberta = null
        },
        aoVoltar = { _ -> aberta = null }, // nos ajustes, "Voltar" cancela sem salvar
    )

    when (aberta) {
        "perfil" -> return EtapaPerfil(config, acoes)
        "combustivel" -> return EtapaCombustivel(config, acoes)
        "veiculo" -> return EtapaVeiculo(config, acoes)
        "rotina" -> return EtapaJornadaTrabalho(config, acoes)
        "despesas" -> return EtapaDespesas(config, acoes)
        "meta" -> return EtapaMeta(config, acoes)
        "bateria" -> return GuiaBateria { aberta = null }
        "calculadora" -> return TelaCalculadora()
    }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
        Text(
            "Ajustes",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Secao("Cadastro")
        Item("Perfil e plataformas", config.listaPlataformas().joinToString(", ") { Plataformas.nome(it) }) { aberta = "perfil" }
        Item("Combustível", "${Formatos.moeda(config.precoLitro)}/L • ${Formatos.campo(config.kmPorLitro)} km/L • ${Formatos.moeda(config.custoCombustivelPorKm())}/km") { aberta = "combustivel" }
        Item("Veículo", when (config.veiculo) {
            "FINANCIADO" -> "Financiado • ${Formatos.moeda(config.parcelaMensal)}/mês"
            "ALUGADO" -> "Alugado • ${Formatos.moeda(config.aluguelSemanal)}/semana"
            "PROPRIO" -> "Próprio"
            else -> "Não informado"
        }) { aberta = "veiculo" }
        Item("Dias e horas de trabalho", "${Formatos.campo(config.horasPorDia)} h por dia") { aberta = "rotina" }
        Item("Custos adicionais", "IPVA, seguro, manutenção…") { aberta = "despesas" }
        Item("Meta da semana", Formatos.moeda(config.metaSemanal)) { aberta = "meta" }

        Secao("Jornada")
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Contar km durante a pausa", fontWeight = FontWeight.SemiBold)
                Text(
                    if (config.contarKmNaPausa) "Ligado: os km rodados na pausa entram no total (aparecem separados no resumo)."
                    else "Desligado (padrão): na pausa o GPS desliga e os km não contam.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = config.contarKmNaPausa,
                onCheckedChange = { marcado -> escopo.launch { repo.alterarConfig { it.copy(contarKmNaPausa = marcado) } } },
            )
        }
        Item("Funcionar com a tela apagada", "Liberar o app na economia de bateria") { aberta = "bateria" }

        Secao("Ferramentas")
        Item("Calculadora de corrida", "Testar valores de uma oferta") { aberta = "calculadora" }

        Secao("Privacidade")
        Text(
            "Todos os seus dados ficam apenas neste celular. Nada é enviado para servidores.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Item("Apagar todos os meus dados", "Cadastro, jornadas e corridas") { confirmarApagar = true }
    }

    if (confirmarApagar) {
        AlertDialog(
            onDismissRequest = { confirmarApagar = false },
            title = { Text("Apagar todos os dados?") },
            text = { Text("Isso apaga cadastro, custos, jornadas e corridas deste celular. Não dá para desfazer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarApagar = false
                    escopo.launch { repo.apagarTudo() }
                }) { Text("Apagar tudo", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmarApagar = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Secao(titulo: String) {
    Column(Modifier.padding(top = 20.dp)) {
        Text(
            titulo.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        )
        HorizontalDivider()
    }
}

@Composable
private fun Item(titulo: String, detalhe: String, aoTocar: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = aoTocar)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(titulo, fontWeight = FontWeight.SemiBold)
        Text(detalhe, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
