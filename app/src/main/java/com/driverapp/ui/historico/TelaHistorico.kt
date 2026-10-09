package com.driverapp.ui.historico

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.driverapp.calculo.Financeiro
import com.driverapp.dados.paraDominio
import com.driverapp.repositorio
import com.driverapp.ui.Formatos
import com.driverapp.ui.inicio.DialogoResumo
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val FORMATO_DATA = DateTimeFormatter.ofPattern("EEE, dd/MM/yyyy 'às' HH:mm", Formatos.BR)

/** Lista das jornadas finalizadas, da mais recente para a mais antiga. */
@Composable
fun TelaHistorico() {
    val repo = LocalContext.current.repositorio
    val jornadas by repo.jornadasFinalizadas.collectAsStateWithLifecycle(initialValue = emptyList())
    val totais by repo.totaisPorJornada.collectAsStateWithLifecycle(initialValue = emptyList())
    var aberta by remember { mutableStateOf<Long?>(null) }

    if (jornadas.isEmpty()) {
        Column(Modifier.padding(24.dp)) {
            Text("Histórico", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Nenhuma jornada finalizada ainda. Quando você finalizar uma jornada, ela aparece aqui.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    } else {
        val porJornada = totais.associateBy { it.jornadaId }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Histórico", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
            items(jornadas, key = { it.id }) { j ->
                val d = j.paraDominio()
                val ms = d.msProdutivos(j.finalizadaEm ?: j.inicioEm)
                val total = porJornada[j.id]
                val r = Financeiro.resumir(
                    faturamentoBruto = total?.total ?: 0.0,
                    km = d.km,
                    horasProdutivas = ms / 3_600_000.0,
                    custoCombustivelPorKm = j.custoKmUsado,
                    custoFixoPorHora = j.custoFixoHoraUsado ?: 0.0,
                )
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().clickable { aberta = j.id },
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            FORMATO_DATA.format(Instant.ofEpochMilli(j.inicioEm).atZone(ZoneId.systemDefault())),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "${Formatos.duracao(ms)} • ${Formatos.km(d.km)} • ${total?.quantidade ?: 0} corridas",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "Bruto ${Formatos.moeda(r.faturamentoBruto)}  •  Resultado ${Formatos.moeda(r.resultadoEstimado)}",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }

    aberta?.let { id -> DialogoResumo(id) { aberta = null } }
}
