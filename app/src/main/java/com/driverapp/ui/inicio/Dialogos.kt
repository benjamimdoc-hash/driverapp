package com.driverapp.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.driverapp.calculo.Financeiro
import com.driverapp.dados.JornadaEntity
import com.driverapp.dados.TotalPorJornada
import com.driverapp.dados.paraDominio
import com.driverapp.repositorio
import com.driverapp.ui.Formatos
import com.driverapp.ui.cadastro.Plataformas
import com.driverapp.ui.componentes.CampoNumero
import com.driverapp.ui.componentes.Chips
import com.driverapp.ui.componentes.LinhaValor

/** Lançamento manual de uma corrida (a leitura automática chega na Fase 3). */
@Composable
fun DialogoCorrida(
    plataformas: List<String>,
    aoCancelar: () -> Unit,
    aoSalvar: (plataforma: String, valor: Double, km: Double?, minutos: Double?) -> Unit,
) {
    var plataforma by remember { mutableStateOf(plataformas.firstOrNull() ?: "OUTRA") }
    var valor by remember { mutableStateOf("") }
    var km by remember { mutableStateOf("") }
    var minutos by remember { mutableStateOf("") }
    val valorNum = Formatos.lerNumero(valor)

    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text("Lançar corrida") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (plataformas.size > 1) {
                    Chips(plataformas.map { it to Plataformas.nome(it) }, setOf(plataforma)) { plataforma = it }
                }
                CampoNumero("Valor recebido (R$)", valor, { valor = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoNumero("km (opcional)", km, { km = it }, Modifier.weight(1f))
                    CampoNumero("min (opcional)", minutos, { minutos = it }, Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valorNum != null && valorNum > 0,
                onClick = { aoSalvar(plataforma, valorNum!!, Formatos.lerNumero(km), Formatos.lerNumero(minutos)) },
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoCancelar) { Text("Cancelar") } },
    )
}

/** Resumo mostrado ao finalizar a jornada (e ao tocar numa jornada do histórico). */
@Composable
fun DialogoResumo(jornadaId: Long, aoFechar: () -> Unit) {
    val repo = LocalContext.current.repositorio
    var jornada by remember { mutableStateOf<JornadaEntity?>(null) }
    var total by remember { mutableStateOf<TotalPorJornada?>(null) }
    LaunchedEffect(jornadaId) {
        jornada = repo.obterJornada(jornadaId)
        total = repo.totalDaJornada(jornadaId)
    }
    val j = jornada ?: return

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Resumo da jornada") },
        text = { ConteudoResumo(j, total) },
        confirmButton = { TextButton(onClick = aoFechar) { Text("Fechar") } },
    )
}

@Composable
fun ConteudoResumo(j: JornadaEntity, total: TotalPorJornada?) {
    val d = j.paraDominio()
    val ms = d.msProdutivos(j.finalizadaEm ?: System.currentTimeMillis())
    val r = Financeiro.resumir(
        faturamentoBruto = total?.total ?: 0.0,
        km = d.km,
        horasProdutivas = ms / 3_600_000.0,
        custoCombustivelPorKm = j.custoKmUsado,
        custoFixoPorHora = j.custoFixoHoraUsado ?: 0.0,
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LinhaValor("Tempo trabalhado", Formatos.duracao(ms))
        LinhaValor("Distância", Formatos.km(d.km))
        if (d.metrosNaPausa > 0) LinhaValor("  (dos quais na pausa)", Formatos.km(d.metrosNaPausa / 1000))
        LinhaValor("Corridas", (total?.quantidade ?: 0).toString())
        LinhaValor("Faturamento bruto", Formatos.moeda(r.faturamentoBruto))
        LinhaValor("Combustível", Formatos.moeda(r.custoCombustivel?.let { -it }))
        LinhaValor("Outros custos", Formatos.moeda(-r.outrosCustos))
        LinhaValor("Resultado estimado", Formatos.moeda(r.resultadoEstimado), destaque = true)
        if (ms > 0) {
            LinhaValor("Faturamento por hora", Formatos.moeda(r.faturamentoBruto / (ms / 3_600_000.0)) + "/h")
        }
        Text(
            "Valores de custo e resultado são estimativas.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
