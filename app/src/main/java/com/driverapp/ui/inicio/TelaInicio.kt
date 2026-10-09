package com.driverapp.ui.inicio

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.driverapp.calculo.EstadoJornada
import com.driverapp.calculo.Financeiro
import com.driverapp.dados.ConfiguracaoEntity
import com.driverapp.dados.CorridaEntity
import com.driverapp.dados.custoCombustivelPorKm
import com.driverapp.dados.custoFixoPorHora
import com.driverapp.dados.listaDias
import com.driverapp.dados.metas
import com.driverapp.dados.paraDominio
import com.driverapp.jornada.JornadaService
import com.driverapp.repositorio
import com.driverapp.ui.Formatos
import com.driverapp.ui.cadastro.Plataformas
import com.driverapp.ui.componentes.LinhaValor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Painel principal: jornada, números do dia e metas. */
@Composable
fun TelaInicio(config: ConfiguracaoEntity) {
    val contexto = LocalContext.current
    val repo = contexto.repositorio
    val escopo = rememberCoroutineScope()

    val jornadaEnt by repo.jornadaAtual.collectAsStateWithLifecycle(initialValue = null)
    val despesas by repo.despesas.collectAsStateWithLifecycle(initialValue = emptyList())
    val corridas by remember(jornadaEnt?.id) {
        jornadaEnt?.id?.let { repo.corridasDaJornada(it) } ?: flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val zona = ZoneId.systemDefault()
    val hoje = LocalDate.now(zona)
    val inicioDia = hoje.atStartOfDay(zona).toInstant().toEpochMilli()
    val inicioSemana = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(zona).toInstant().toEpochMilli()
    val faturadoHoje by remember(inicioDia) { repo.faturadoDesde(inicioDia) }.collectAsStateWithLifecycle(initialValue = 0.0)
    val faturadoSemana by remember(inicioSemana) { repo.faturadoDesde(inicioSemana) }.collectAsStateWithLifecycle(initialValue = 0.0)

    // Relógio da tela: atualiza a cada segundo.
    var agora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            agora = System.currentTimeMillis()
            delay(1_000)
        }
    }

    // Se há jornada aberta (ex.: depois de reiniciar o celular), religa o GPS ao abrir o app.
    LaunchedEffect(jornadaEnt?.id) {
        if (jornadaEnt != null && JornadaService.temPermissaoLocal(contexto)) JornadaService.iniciar(contexto)
    }

    var explicarPermissao by remember { mutableStateOf(false) }
    var avisoSemGps by remember { mutableStateOf(false) }
    var confirmarFim by remember { mutableStateOf(false) }
    var resumoId by rememberSaveable { mutableStateOf<Long?>(null) }
    var lancarCorrida by remember { mutableStateOf(false) }

    fun comecar() {
        escopo.launch {
            repo.iniciarJornada(System.currentTimeMillis())
            if (JornadaService.temPermissaoLocal(contexto)) JornadaService.iniciar(contexto) else avisoSemGps = true
        }
    }

    val pedirPermissoes = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ -> comecar() }

    fun aoTocarIniciar() {
        if (JornadaService.temPermissaoLocal(contexto)) comecar() else explicarPermissao = true
    }

    val jornada = jornadaEnt?.paraDominio()
    val ms = jornada?.msProdutivos(agora) ?: 0L
    val km = jornada?.km ?: 0.0
    val faturadoJornada = corridas.sumOf { it.valor }
    val resumo = Financeiro.resumir(
        faturamentoBruto = faturadoJornada,
        km = km,
        horasProdutivas = ms / 3_600_000.0,
        custoCombustivelPorKm = config.custoCombustivelPorKm(),
        custoFixoPorHora = config.custoFixoPorHora(despesas),
    )
    val metas = config.metas()

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ---------- Jornada ----------
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(
                Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val estadoTexto = when (jornada?.estado) {
                    EstadoJornada.ATIVA -> "JORNADA ATIVA"
                    EstadoJornada.PAUSADA -> if (config.contarKmNaPausa) "PAUSADA • km continuam contando" else "PAUSADA • km parados"
                    else -> "SEM JORNADA ABERTA"
                }
                Text(estadoTexto, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(Formatos.relogio(ms), fontSize = 44.sp, fontWeight = FontWeight.Bold)
                Text(Formatos.km(km), style = MaterialTheme.typography.titleMedium)
                if (jornada != null && !JornadaService.temPermissaoLocal(contexto)) {
                    Text(
                        "GPS sem permissão: os km não estão sendo registrados.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(8.dp))
                when (jornada?.estado) {
                    null, EstadoJornada.FINALIZADA -> Button(
                        onClick = { aoTocarIniciar() },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                    ) { Text("Iniciar jornada", fontSize = 18.sp) }

                    else -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (jornada?.estado == EstadoJornada.ATIVA) {
                            OutlinedButton(
                                onClick = { escopo.launch { repo.pausar(System.currentTimeMillis()) } },
                                modifier = Modifier.weight(1f).height(56.dp),
                            ) { Text("Pausar") }
                        } else {
                            Button(
                                onClick = { escopo.launch { repo.retomar(System.currentTimeMillis()) } },
                                modifier = Modifier.weight(1f).height(56.dp),
                            ) { Text("Retomar") }
                        }
                        Button(
                            onClick = { confirmarFim = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f).height(56.dp),
                        ) { Text("Finalizar") }
                    }
                }
            }
        }

        // ---------- Números da jornada ----------
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Esta jornada", fontWeight = FontWeight.Bold)
                LinhaValor("Corridas", corridas.size.toString())
                LinhaValor("Faturamento bruto", Formatos.moeda(resumo.faturamentoBruto))
                LinhaValor("Combustível (estimado)", Formatos.moeda(resumo.custoCombustivel?.let { -it }))
                LinhaValor("Outros custos (estimado)", Formatos.moeda(-resumo.outrosCustos))
                LinhaValor("Resultado estimado", Formatos.moeda(resumo.resultadoEstimado), destaque = true)
                Text(
                    "Resultado = faturamento − combustível − parte dos custos fixos pelo tempo trabalhado. É uma estimativa.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { lancarCorrida = true },
                    enabled = jornada != null,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text(if (jornada != null) "+ Lançar corrida" else "Inicie a jornada para lançar corridas") }
            }
        }

        // ---------- Metas ----------
        if (metas != null) {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Metas de faturamento", fontWeight = FontWeight.Bold)
                    LinhaValor("Hoje", "${Formatos.moeda(faturadoHoje)} de ${Formatos.moeda(metas.metaDiaria)}")
                    LinearProgressIndicator(progress = { metas.progressoDiario(faturadoHoje).toFloat() }, modifier = Modifier.fillMaxWidth())
                    LinhaValor("Semana", "${Formatos.moeda(faturadoSemana)} de ${Formatos.moeda(metas.metaSemanal)}")
                    LinearProgressIndicator(progress = { metas.progressoSemanal(faturadoSemana).toFloat() }, modifier = Modifier.fillMaxWidth())
                    val falta = metas.faltaNaSemana(faturadoSemana)
                    if (falta > 0) {
                        LinhaValor("Falta na semana", Formatos.moeda(falta))
                        val horasRestantes = horasRestantesNaSemana(config, hoje, ms)
                        metas.necessarioPorHora(faturadoSemana, horasRestantes)?.let {
                            LinhaValor("Precisa faturar por hora", "${Formatos.moeda(it)}/h")
                        }
                    } else {
                        Text("Meta da semana alcançada! 🎉", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // ---------- Corridas lançadas ----------
        if (corridas.isNotEmpty()) {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Corridas desta jornada", fontWeight = FontWeight.Bold)
                    corridas.forEach { c -> LinhaCorrida(c) { escopo.launch { repo.excluirCorrida(c.id) } } }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    // ---------- Diálogos ----------
    if (explicarPermissao) {
        AlertDialog(
            onDismissRequest = { explicarPermissao = false },
            title = { Text("Medir os km da jornada") },
            text = {
                Text(
                    "Para calcular os km e o custo de combustível, o app usa o GPS apenas enquanto a jornada estiver aberta. " +
                        "Nada é enviado para a internet.\n\nNa próxima tela, escolha \"Localização precisa\" e \"Durante o uso do app\". " +
                        "Também vamos pedir para mostrar notificações: o Android exige um aviso fixo enquanto o GPS trabalha."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    explicarPermissao = false
                    val lista = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    if (Build.VERSION.SDK_INT >= 33) lista += Manifest.permission.POST_NOTIFICATIONS
                    pedirPermissoes.launch(lista.toTypedArray())
                }) { Text("Continuar") }
            },
            dismissButton = {
                TextButton(onClick = { explicarPermissao = false; comecar() }) { Text("Iniciar sem GPS") }
            },
        )
    }

    if (avisoSemGps) {
        AlertDialog(
            onDismissRequest = { avisoSemGps = false },
            title = { Text("Jornada iniciada sem GPS") },
            text = {
                Text(
                    "O tempo está sendo contado, mas os km não. Para registrar os km, permita a localização precisa " +
                        "em Ajustes do Android → Apps → este app → Permissões."
                )
            },
            confirmButton = { TextButton(onClick = { avisoSemGps = false }) { Text("Entendi") } },
        )
    }

    if (confirmarFim) {
        AlertDialog(
            onDismissRequest = { confirmarFim = false },
            title = { Text("Finalizar a jornada?") },
            text = { Text("O tempo e os km param de contar e a jornada vai para o histórico.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarFim = false
                    escopo.launch { resumoId = repo.finalizar(System.currentTimeMillis()) }
                }) { Text("Finalizar") }
            },
            dismissButton = { TextButton(onClick = { confirmarFim = false }) { Text("Cancelar") } },
        )
    }

    resumoId?.let { id -> DialogoResumo(id) { resumoId = null } }

    if (lancarCorrida && jornadaEnt != null) {
        DialogoCorrida(
            plataformas = config.plataformas.split(',').filter { it.isNotBlank() },
            aoCancelar = { lancarCorrida = false },
            aoSalvar = { plataforma, valor, kmC, minC ->
                lancarCorrida = false
                val id = jornadaEnt?.id
                escopo.launch {
                    repo.adicionarCorrida(
                        CorridaEntity(
                            jornadaId = id, plataforma = plataforma, valor = valor,
                            km = kmC, minutos = minC, criadaEm = System.currentTimeMillis(),
                        )
                    )
                }
            },
        )
    }
}

@Composable
private fun LinhaCorrida(c: CorridaEntity, aoExcluir: () -> Unit) {
    val hora = java.time.Instant.ofEpochMilli(c.criadaEm).atZone(ZoneId.systemDefault()).toLocalTime()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("${Plataformas.nome(c.plataforma)} • ${Formatos.moeda(c.valor)}", fontWeight = FontWeight.SemiBold)
            val detalhes = listOfNotNull(
                "%02d:%02d".format(hora.hour, hora.minute),
                c.km?.let { Formatos.km(it) },
                c.minutos?.let { "${it.toInt()} min" },
            ).joinToString(" • ")
            Text(detalhes, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = aoExcluir) { Icon(Icons.Filled.Delete, contentDescription = "Excluir corrida") }
    }
}

/**
 * Horas de trabalho que ainda restam na semana (segunda a domingo), pelos dias e horas do cadastro.
 * Hoje conta só o que falta das horas planejadas.
 */
private fun horasRestantesNaSemana(config: ConfiguracaoEntity, hoje: LocalDate, msHoje: Long): Double {
    val horasDia = config.horasPorDia ?: return 0.0
    val dias = config.listaDias().toSet()
    val hojeNum = hoje.dayOfWeek.value
    var total = 0.0
    for (d in hojeNum..7) {
        if (d !in dias) continue
        total += if (d == hojeNum) (horasDia - msHoje / 3_600_000.0).coerceAtLeast(0.0) else horasDia
    }
    return total
}
