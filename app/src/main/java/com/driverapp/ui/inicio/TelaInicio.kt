package com.driverapp.ui.inicio

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.driverapp.R
import com.driverapp.calculo.Cor
import com.driverapp.calculo.DadosPeriodo
import com.driverapp.calculo.EstadoJornada
import com.driverapp.calculo.Periodo
import com.driverapp.calculo.Periodos
import com.driverapp.calculo.ResumoDePeriodo
import com.driverapp.calculo.Ritmo
import com.driverapp.dados.ConfiguracaoEntity
import com.driverapp.dados.CorridaEntity
import com.driverapp.dados.TotalPeriodo
import com.driverapp.dados.criteriosPara
import com.driverapp.dados.custoCombustivelPorKm
import com.driverapp.dados.custoFixoPorHora
import com.driverapp.dados.listaDias
import com.driverapp.dados.listaPlataformas
import com.driverapp.dados.metas
import com.driverapp.dados.paraDominio
import com.driverapp.jornada.JornadaService
import com.driverapp.repositorio
import com.driverapp.ui.Formatos
import com.driverapp.ui.LocalPaleta
import com.driverapp.ui.Medidas
import com.driverapp.ui.cadastro.Plataformas
import com.driverapp.ui.componentes.BarraProgresso
import com.driverapp.ui.componentes.BotaoPrincipal
import com.driverapp.ui.componentes.BotaoSecundario
import com.driverapp.ui.componentes.CartaoVidro
import com.driverapp.ui.componentes.LinhaValor
import com.driverapp.ui.componentes.SeletorSegmentado
import com.driverapp.ui.estiloNumeroDestaque
import com.driverapp.ui.leitura.Pilula
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DATA_TOPO = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Formatos.BR)

/**
 * Dashboard financeira: filtro Hoje/Semana/Mês, jornada em destaque, resumo financeiro,
 * métricas operacionais com cores pelos limites do motorista e progresso da meta semanal.
 * Tudo vem dos registros reais do banco.
 */
@Composable
fun TelaInicio(config: ConfiguracaoEntity) {
    val p = LocalPaleta.current
    val contexto = LocalContext.current
    val repo = contexto.repositorio
    val escopo = rememberCoroutineScope()

    // Relógio da tela (atualiza a cada segundo) e data local.
    var agora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            agora = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val zona = ZoneId.systemDefault()
    val hoje = LocalDate.now(zona)

    var periodo by rememberSaveable { mutableIntStateOf(0) }
    val intervalo = remember(periodo, hoje) { Periodos.intervalo(Periodo.entries[periodo], hoje, zona) }
    val semana = remember(hoje) { Periodos.intervalo(Periodo.SEMANA, hoje, zona) }

    val jornadaEnt by repo.jornadaAtual.collectAsStateWithLifecycle(initialValue = null)
    val despesas by repo.despesas.collectAsStateWithLifecycle(initialValue = emptyList())
    val limites by repo.limites.collectAsStateWithLifecycle(initialValue = emptyList())
    val jornadasPeriodo by remember(intervalo) { repo.jornadasNoPeriodo(intervalo.inicioMs, intervalo.fimMs) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val totalPeriodo by remember(intervalo) { repo.totalPeriodo(intervalo.inicioMs, intervalo.fimMs) }
        .collectAsStateWithLifecycle(initialValue = TotalPeriodo(0, 0.0))
    val totalSemana by remember(semana) { repo.totalPeriodo(semana.inicioMs, semana.fimMs) }
        .collectAsStateWithLifecycle(initialValue = TotalPeriodo(0, 0.0))
    val corridasJornada by remember(jornadaEnt?.id) {
        jornadaEnt?.id?.let { repo.corridasDaJornada(it) } ?: flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    // Se há jornada aberta (ex.: depois de reiniciar o celular), religa o GPS ao abrir o app.
    LaunchedEffect(jornadaEnt?.id) {
        if (jornadaEnt != null && JornadaService.temPermissaoLocal(contexto)) JornadaService.iniciar(contexto)
    }

    // ---------- Agregação do período ----------
    val custoKmAtual = config.custoCombustivelPorKm()
    val fixoHoraAtual = config.custoFixoPorHora(despesas)
    var msOnline = 0L
    var km = 0.0
    var combustivel: Double? = 0.0
    var fixos = 0.0
    jornadasPeriodo.forEach { j ->
        val d = j.paraDominio()
        val ms = d.msProdutivos(j.finalizadaEm ?: agora)
        msOnline += ms
        km += d.km
        // Jornada finalizada usa os custos gravados no fechamento; a aberta usa os atuais.
        val ck = j.custoKmUsado ?: custoKmAtual
        combustivel = if (ck == null) null else combustivel?.plus(d.km * ck)
        fixos += (j.custoFixoHoraUsado ?: fixoHoraAtual) * ms / 3_600_000.0
    }
    val resumo = ResumoDePeriodo.calcular(
        DadosPeriodo(totalPeriodo.total, totalPeriodo.quantidade, msOnline, km, combustivel, fixos)
    )
    val criterios = criteriosPara(null, null, limites)

    // ---------- Estados de diálogos e permissões ----------
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

    val jornada = jornadaEnt?.paraDominio()

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(Medidas.espaco),
    ) {
        // ---------- Topo ----------
        Column {
            Text("Painel", style = MaterialTheme.typography.headlineSmall, color = p.texto)
            Text(DATA_TOPO.format(hoje).replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium, color = p.textoSecundario)
        }
        SeletorSegmentado(listOf("Hoje", "Semana", "Mês"), periodo, { periodo = it })

        // ---------- Jornada ----------
        val ativa = jornada?.estado == EstadoJornada.ATIVA
        CartaoVidro(Modifier.fillMaxWidth(), brilho = if (ativa) p.ciano else null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (texto, cor) = when (jornada?.estado) {
                    EstadoJornada.ATIVA -> "Jornada ativa" to p.verde
                    EstadoJornada.PAUSADA -> "Pausada" to p.amarelo
                    else -> "Sem jornada aberta" to p.neutro
                }
                Box(Modifier.size(10.dp).clip(CircleShape).background(cor))
                Text("  $texto", fontWeight = FontWeight.SemiBold, color = p.texto, modifier = Modifier.weight(1f))
                if (jornada?.estado == EstadoJornada.PAUSADA) {
                    Text(
                        if (config.contarKmNaPausa) "km contando" else "km parados",
                        style = MaterialTheme.typography.labelMedium, color = p.textoSecundario,
                    )
                }
            }
            val msJornada = jornada?.msProdutivos(agora) ?: 0L
            Text(Formatos.relogio(msJornada), style = estiloNumeroDestaque(44, if (ativa) p.ciano else p.texto))
            Text(
                "${Formatos.km(jornada?.km ?: 0.0)} nesta jornada • ${corridasJornada.size} corrida(s)",
                color = p.textoSecundario, style = MaterialTheme.typography.bodyMedium,
            )
            if (jornada != null && !JornadaService.temPermissaoLocal(contexto)) {
                Text("GPS sem permissão: os km não estão sendo registrados.", color = p.vermelho, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(10.dp))
            when (jornada?.estado) {
                null, EstadoJornada.FINALIZADA -> BotaoPrincipal(
                    "Iniciar jornada",
                    { if (JornadaService.temPermissaoLocal(contexto)) comecar() else explicarPermissao = true },
                    icone = rememberVectorPainter(Icons.Filled.PlayArrow),
                )
                else -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (ativa) {
                        BotaoPrincipal(
                            "Pausar", { escopo.launch { repo.pausar(System.currentTimeMillis()) } },
                            Modifier.weight(1f), cor = p.amarelo, icone = painterResource(R.drawable.ic_pausa),
                        )
                    } else {
                        BotaoPrincipal(
                            "Retomar", { escopo.launch { repo.retomar(System.currentTimeMillis()) } },
                            Modifier.weight(1f), cor = p.verde, icone = rememberVectorPainter(Icons.Filled.PlayArrow),
                        )
                    }
                    BotaoPrincipal("Finalizar", { confirmarFim = true }, Modifier.weight(1f), cor = p.vermelho)
                }
            }
            if (jornada != null) {
                BotaoSecundario("+ Lançar corrida", { lancarCorrida = true }, Modifier.fillMaxWidth().padding(top = 10.dp))
            }
        }

        // ---------- Resumo financeiro ----------
        CartaoVidro(Modifier.fillMaxWidth(), brilho = p.destaque) {
            Text("Faturamento bruto", style = MaterialTheme.typography.labelLarge, color = p.textoSecundario)
            Text(Formatos.moeda(resumo.faturamentoBruto), style = estiloNumeroDestaque(38, p.destaque))
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Mini("Despesas", resumo.despesasTotais?.let { Formatos.moeda(-it) } ?: "—", p.vermelho, Modifier.weight(1f))
                val res = resumo.resultadoEstimado
                Mini(
                    "Resultado estimado", res?.let { Formatos.moeda(it) } ?: "—",
                    if (res == null) p.neutro else if (res >= 0) p.verde else p.vermelho, Modifier.weight(1f),
                )
            }
            Text(
                if (resumo.incompleto) "Configure o combustível para calcular despesas e resultado."
                else "Estimativa: combustível dos km rodados + custos fixos pelas horas online.",
                style = MaterialTheme.typography.bodySmall, color = p.textoSecundario, modifier = Modifier.padding(top = 8.dp),
            )
        }

        // ---------- Métricas operacionais ----------
        val corHora = resumo.ganhoPorHora?.let { criterios.porHora.nivel(it).cor }
        val corKm = resumo.ganhoPorKm?.let { criterios.porKm.nivel(it).cor }
        val metricas = listOf(
            Metrica("Corridas", resumo.corridas.toString(), null, true),
            Metrica("Horas online", Formatos.duracao(msOnline), null, true),
            Metrica("Km rodados", Formatos.km(resumo.km), null, true),
            Metrica("Ticket médio", resumo.ticketMedio?.let { Formatos.moeda(it) }, null, false),
            Metrica("Ganho bruto/hora", resumo.ganhoPorHora?.let { Formatos.moeda(it) }, corHora, false),
            Metrica("Ganho bruto/km", resumo.ganhoPorKm?.let { Formatos.moeda(it) }, corKm, false),
            Metrica(
                "Lucro estimado/km", resumo.resultadoPorKm?.let { Formatos.moeda(it) },
                resumo.resultadoPorKm?.let { if (it < 0) Cor.VERMELHO else null }, false,
            ),
            Metrica("Custo/km", resumo.custoPorKm?.let { Formatos.moeda(it) }, null, false),
        )
        metricas.chunked(2).forEach { par ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                par.forEach { m -> TileMetrica(m, Modifier.weight(1f)) }
            }
        }

        // ---------- Meta semanal ----------
        config.metas()?.let { metas ->
            val faturado = totalSemana.total
            val falta = metas.faltaNaSemana(faturado)
            val diasRestantes = Ritmo.diasRestantes(config.listaDias().toSet(), hoje)
            val porDia = Ritmo.porDia(falta, diasRestantes)
            CartaoVidro(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Meta da semana", fontWeight = FontWeight.Bold, color = p.texto, modifier = Modifier.weight(1f))
                    Pilula("${(metas.progressoSemanal(faturado) * 100).toInt()}%", if (falta <= 0) p.verde else p.destaque)
                }
                Text(
                    "${Formatos.moeda(faturado)} de ${Formatos.moeda(metas.metaSemanal)}",
                    style = MaterialTheme.typography.titleMedium, color = p.texto, modifier = Modifier.padding(vertical = 8.dp),
                )
                BarraProgresso(metas.progressoSemanal(faturado).toFloat(), if (falta <= 0) p.verde else p.destaque)
                Spacer(Modifier.height(10.dp))
                if (falta > 0) {
                    LinhaValor("Falta", Formatos.moeda(falta), destaque = true)
                    LinhaValor("Dias de trabalho restantes", if (diasRestantes > 0) diasRestantes.toString() else "nenhum nesta semana")
                    porDia?.let {
                        LinhaValor("Ritmo necessário", "${Formatos.moeda(it)} por dia")
                        config.horasPorDia?.takeIf { h -> h > 0 }?.let { h -> LinhaValor("", "${Formatos.moeda(it / h)} por hora") }
                    }
                } else {
                    Text("Meta da semana alcançada!", color = p.verde, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ---------- Corridas da jornada ----------
        if (corridasJornada.isNotEmpty()) {
            CartaoVidro(Modifier.fillMaxWidth()) {
                Text("Corridas desta jornada", fontWeight = FontWeight.Bold, color = p.texto)
                corridasJornada.forEach { c -> LinhaCorrida(c) { escopo.launch { repo.excluirCorrida(c.id) } } }
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
            dismissButton = { TextButton(onClick = { explicarPermissao = false; comecar() }) { Text("Iniciar sem GPS") } },
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
            plataformas = config.listaPlataformas(),
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

private data class Metrica(val titulo: String, val valor: String?, val cor: Cor?, val semCor: Boolean)

/** Tile de métrica: valor colorido pelos limites; sem dados = estado neutro (não é "ruim"). */
@Composable
private fun TileMetrica(m: Metrica, modifier: Modifier) {
    val p = LocalPaleta.current
    val corValor = when {
        m.valor == null -> p.neutro
        m.semCor || m.cor == null -> p.texto
        else -> p.corDe(m.cor)
    }
    CartaoVidro(modifier, preenchimento = 14.dp, brilho = if (m.valor != null && m.cor != null) p.corDe(m.cor) else null) {
        Text(m.titulo, style = MaterialTheme.typography.labelMedium, color = p.textoSecundario)
        Text(m.valor ?: "Sem dados", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = corValor)
    }
}

@Composable
private fun Mini(rotulo: String, valor: String, cor: Color, modifier: Modifier) {
    val p = LocalPaleta.current
    Column(
        modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .background(p.vidro)
            .padding(12.dp),
    ) {
        Text(rotulo, style = MaterialTheme.typography.labelMedium, color = p.textoSecundario)
        Text(valor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = cor)
    }
}

@Composable
private fun LinhaCorrida(c: CorridaEntity, aoExcluir: () -> Unit) {
    val p = LocalPaleta.current
    val hora = java.time.Instant.ofEpochMilli(c.criadaEm).atZone(ZoneId.systemDefault()).toLocalTime()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("${Plataformas.nome(c.plataforma)} • ${Formatos.moeda(c.valor)}", fontWeight = FontWeight.SemiBold, color = p.texto)
            val detalhes = listOfNotNull(
                "%02d:%02d".format(hora.hour, hora.minute),
                c.km?.let { Formatos.km(it) },
                c.minutos?.let { "${it.toInt()} min" },
            ).joinToString(" • ")
            Text(detalhes, style = MaterialTheme.typography.bodySmall, color = p.textoSecundario)
        }
        IconButton(onClick = aoExcluir) { Icon(Icons.Filled.Delete, contentDescription = "Excluir corrida", tint = p.textoSecundario) }
    }
}
