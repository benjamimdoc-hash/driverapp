package com.driverapp.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.driverapp.calculo.FonteTotal
import com.driverapp.calculo.TotalCombinado
import com.driverapp.dados.SaldoEntity
import com.driverapp.leitores.PeriodoSaldo
import com.driverapp.leitores.Plataforma
import com.driverapp.leitura.ProcessadorSaldo
import com.driverapp.repositorio
import com.driverapp.ui.Formatos
import com.driverapp.ui.LocalPaleta
import com.driverapp.ui.cadastro.Plataformas
import com.driverapp.ui.componentes.BotaoSecundario
import com.driverapp.ui.componentes.CampoMoeda
import com.driverapp.ui.componentes.CampoNumero
import com.driverapp.ui.componentes.CartaoVidro
import com.driverapp.ui.componentes.Chips
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm")

/**
 * Card das plataformas: de onde vem cada número (saldo lido da plataforma ou lançamento manual),
 * leituras pendentes de confirmação e correção manual.
 */
@Composable
fun CartaoPlataformas(total: TotalCombinado, pendentes: List<SaldoEntity>, plataformasDoPerfil: List<String>) {
    val p = LocalPaleta.current
    val contexto = LocalContext.current
    val repo = contexto.repositorio
    val escopo = rememberCoroutineScope()
    var corrigindo by remember { mutableStateOf(false) }

    val comLeitura = plataformasDoPerfil.mapNotNull { n -> Plataforma.entries.firstOrNull { it.name == n } }
    if (total.linhas.isEmpty() && pendentes.isEmpty() && comLeitura.isEmpty()) return

    CartaoVidro(Modifier.fillMaxWidth()) {
        Text("Plataformas", fontWeight = FontWeight.Bold, color = p.texto)
        if (total.linhas.isEmpty()) {
            Text(
                "Nenhum ganho neste período. Com a leitura ativa, abra a tela inicial da Uber ou o Painel da 99 " +
                    "para o app ler seus ganhos automaticamente.",
                style = MaterialTheme.typography.bodySmall, color = p.textoSecundario,
            )
        }
        total.linhas.forEach { l ->
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(Plataformas.nome(l.plataforma), fontWeight = FontWeight.SemiBold, color = p.texto)
                    Text(
                        (if (l.fonte == FonteTotal.SALDO_DA_PLATAFORMA) "Lido da plataforma" else "Lançado à mão") +
                            if (l.parcial) " • parcial" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (l.parcial) p.amarelo else p.textoSecundario,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(Formatos.moeda(l.valor), fontWeight = FontWeight.Bold, color = p.texto)
                    Text("${l.corridas} corrida(s)", style = MaterialTheme.typography.bodySmall, color = p.textoSecundario)
                }
            }
        }
        if (total.parcial) {
            Text(
                "Parcial: corridas feitas antes da primeira leitura do dia podem não estar incluídas.",
                style = MaterialTheme.typography.bodySmall, color = p.textoSecundario, modifier = Modifier.padding(top = 6.dp),
            )
        }

        pendentes.take(3).forEach { s ->
            CartaoVidro(Modifier.fillMaxWidth().padding(top = 10.dp), brilho = p.amarelo, preenchimento = 12.dp) {
                Text("Confirme esta leitura", fontWeight = FontWeight.SemiBold, color = p.amarelo)
                Text(
                    "${Plataformas.nome(s.plataforma)}: ${Formatos.moeda(s.valor)} • ${s.corridas ?: "—"} corrida(s) " +
                        "(${if (s.semanal) "semana" else "dia"}), lida em ${HORA.format(Instant.ofEpochMilli(s.lidoEm).atZone(ZoneId.systemDefault()))}",
                    style = MaterialTheme.typography.bodySmall, color = p.texto,
                )
                s.motivo?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = p.textoSecundario) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { escopo.launch { repo.confirmarSaldo(s.id) } }) { Text("Está certo") }
                    TextButton(onClick = { escopo.launch { repo.descartarSaldo(s.id) } }) { Text("Descartar") }
                }
            }
        }

        if (comLeitura.isNotEmpty()) {
            BotaoSecundario("Corrigir total de uma plataforma", { corrigindo = true }, Modifier.fillMaxWidth().padding(top = 10.dp))
        }
    }

    if (corrigindo) {
        DialogoCorrecao(comLeitura, aoFechar = { corrigindo = false })
    }
}

@Composable
private fun DialogoCorrecao(plataformas: List<Plataforma>, aoFechar: () -> Unit) {
    val contexto = LocalContext.current
    val escopo = rememberCoroutineScope()
    var plataforma by remember { mutableStateOf(plataformas.first()) }
    var valor by remember { mutableStateOf<Double?>(null) }
    var corridas by remember { mutableStateOf("") }
    val periodo = if (ProcessadorSaldo.periodoDe(plataforma) == PeriodoSaldo.SEMANA) "da semana" else "de hoje"

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Corrigir total") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (plataformas.size > 1) {
                    Chips(plataformas.map { it.name to it.nome }, setOf(plataforma.name)) { n ->
                        plataformas.firstOrNull { it.name == n }?.let { plataforma = it }
                    }
                }
                Text("Informe o total $periodo como aparece na ${plataforma.nome}. A correção fica registrada no histórico.")
                CampoMoeda("Total $periodo", valor, { valor = it })
                CampoNumero("Corridas $periodo", corridas, { corridas = it.filter { c -> c.isDigit() } })
            }
        },
        confirmButton = {
            TextButton(
                enabled = valor != null || corridas.isNotEmpty(),
                onClick = {
                    val v = valor
                    val c = corridas.toIntOrNull()
                    escopo.launch { ProcessadorSaldo.corrigir(contexto, plataforma, v, c, System.currentTimeMillis()) }
                    aoFechar()
                },
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
    )
}
