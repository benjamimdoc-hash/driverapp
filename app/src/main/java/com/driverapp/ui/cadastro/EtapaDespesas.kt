package com.driverapp.ui.cadastro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.driverapp.R
import com.driverapp.calculo.CategoriaDespesa
import com.driverapp.calculo.Conversao
import com.driverapp.calculo.CustosFixos
import com.driverapp.calculo.Periodicidade
import com.driverapp.dados.ConfiguracaoEntity
import com.driverapp.dados.DespesaEntity
import com.driverapp.dados.despesasCompletas
import com.driverapp.dados.plano
import com.driverapp.repositorio
import com.driverapp.ui.Formatos
import com.driverapp.ui.LocalPaleta
import com.driverapp.ui.componentes.BotaoSecundario
import com.driverapp.ui.componentes.CampoMoeda
import com.driverapp.ui.componentes.CampoTexto
import com.driverapp.ui.componentes.CartaoVidro
import com.driverapp.ui.componentes.Chips
import com.driverapp.ui.componentes.EstadoSalvamento
import com.driverapp.ui.componentes.IndicadorSalvamento
import com.driverapp.ui.componentes.LinhaValor
import com.driverapp.ui.componentes.MolduraEtapa
import com.driverapp.ui.componentes.SliderComValor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Etapa de custos adicionais — cada categoria é uma seção que abre com um toque e já mostra
 * os campos (valor, periodicidade, observação). Tudo é salvo sozinho, sem botão de "adicionar".
 *
 * Regras para não duplicar:
 *  - Cada categoria fixa (IPVA, Seguro…) corresponde a UMA linha no banco: editar atualiza a mesma linha.
 *  - Apagar o valor remove a despesa.
 *  - "Outras despesas" aceita quantas forem necessárias, cada uma com nome próprio.
 *  - Parcela/aluguel do carro e combustível NÃO entram aqui (já vêm de outras etapas).
 */
private data class CategoriaFixa(
    val categoria: CategoriaDespesa,
    val nome: String,
    val periodicidadePadrao: Periodicidade,
)

private val categoriasFixas = listOf(
    CategoriaFixa(CategoriaDespesa.IPVA, "IPVA", Periodicidade.ANUAL),
    CategoriaFixa(CategoriaDespesa.SEGURO, "Seguro", Periodicidade.MENSAL),
    CategoriaFixa(CategoriaDespesa.MANUTENCAO, "Manutenção", Periodicidade.MENSAL),
    CategoriaFixa(CategoriaDespesa.PNEUS, "Pneus", Periodicidade.VALOR_UNICO),
    CategoriaFixa(CategoriaDespesa.LAVAGEM, "Lavagem", Periodicidade.SEMANAL),
    CategoriaFixa(CategoriaDespesa.LICENCIAMENTO, "Licenciamento", Periodicidade.ANUAL),
    CategoriaFixa(CategoriaDespesa.PEDAGIO_ESTACIONAMENTO, "Pedágio e estacionamento", Periodicidade.POR_DIA_TRABALHADO),
)

private val periodicidades = listOf(
    Periodicidade.POR_DIA_TRABALHADO to "Por dia",
    Periodicidade.SEMANAL to "Por semana",
    Periodicidade.MENSAL to "Por mês",
    Periodicidade.ANUAL to "Por ano",
    Periodicidade.VALOR_UNICO to "Valor único",
)

fun nomePeriodicidade(p: String, prazoMeses: Int? = null): String = when (p) {
    Periodicidade.POR_DIA_TRABALHADO.name -> "por dia"
    Periodicidade.SEMANAL.name -> "por semana"
    Periodicidade.MENSAL.name -> "por mês"
    Periodicidade.ANUAL.name -> "por ano"
    Periodicidade.VALOR_UNICO.name -> "único, em ${prazoMeses ?: Conversao.PRAZO_PADRAO_MESES} meses"
    else -> p
}

@Composable
private fun iconeDe(c: CategoriaDespesa): Painter = when (c) {
    CategoriaDespesa.IPVA, CategoriaDespesa.LICENCIAMENTO -> painterResource(R.drawable.ic_documento)
    CategoriaDespesa.SEGURO -> painterResource(R.drawable.ic_escudo)
    CategoriaDespesa.MANUTENCAO -> rememberVectorPainter(Icons.Filled.Build)
    CategoriaDespesa.PNEUS -> painterResource(R.drawable.ic_pneu)
    CategoriaDespesa.LAVAGEM -> painterResource(R.drawable.ic_gota)
    CategoriaDespesa.PEDAGIO_ESTACIONAMENTO -> rememberVectorPainter(Icons.Filled.Place)
    else -> rememberVectorPainter(Icons.Filled.List)
}

@Composable
fun EtapaDespesas(config: ConfiguracaoEntity, acoes: AcoesEtapa) {
    val repo = LocalContext.current.repositorio
    val carregadas by repo.despesas.collectAsStateWithLifecycle<List<DespesaEntity>?>(initialValue = null)
    val semAlteracao: Alteracao = { it }

    MolduraEtapa(
        titulo = "Custos adicionais",
        subtitulo = "Toque numa categoria e preencha. Cada despesa tem a sua periodicidade e é salva automaticamente. " +
            "Combustível e parcela/aluguel do carro já foram informados — não repita aqui.",
        textoPrincipal = acoes.textoPrincipal,
        principalHabilitado = true,
        aoPrincipal = { acoes.aoConfirmar(semAlteracao) },
        aoVoltar = acoes.aoVoltar?.let { voltar -> { voltar(semAlteracao) } },
        topo = acoes.topo,
        rodapeExtra = acoes.rodape,
    ) {
        val lista = carregadas
        if (lista == null) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            ConteudoDespesas(config, lista)
        }
    }
}

@Composable
private fun ConteudoDespesas(config: ConfiguracaoEntity, lista: List<DespesaEntity>) {
    val p = LocalPaleta.current
    var aberta by rememberSaveable { mutableStateOf<String?>(null) }

    // Para cada categoria fixa, a primeira linha do banco é a "oficial". Linhas extras da mesma
    // categoria (de versões antigas) aparecem em "Outras", para nada ficar escondido nem contado em dobro.
    val oficiais = categoriasFixas.associate { cf -> cf.categoria to lista.firstOrNull { it.categoria == cf.categoria.name } }
    val idsOficiais = oficiais.values.mapNotNull { it?.id }.toSet()
    val novasIds = remember { mutableStateMapOf<Int, Long>() } // chave da nova → id salvo
    val novas = remember { mutableStateListOf<Int>() }
    var proximaChave by rememberSaveable { mutableIntStateOf(1) }
    val outrasSalvas = lista.filter { it.id !in idsOficiais && it.id !in novasIds.values }

    categoriasFixas.forEach { cf ->
        SecaoDespesa(
            chave = cf.categoria.name,
            titulo = cf.nome,
            icone = iconeDe(cf.categoria),
            categoria = cf.categoria,
            existente = oficiais[cf.categoria],
            periodicidadePadrao = cf.periodicidadePadrao,
            nomeEditavel = false,
            aberta = aberta == cf.categoria.name,
            aoAlternar = { aberta = if (aberta == cf.categoria.name) null else cf.categoria.name },
        )
    }

    Text("Outras despesas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = p.texto)
    outrasSalvas.forEach { d ->
        val chave = "d${d.id}"
        SecaoDespesa(
            chave = chave, titulo = d.nome, icone = iconeDe(CategoriaDespesa.OUTRA),
            categoria = runCatching { CategoriaDespesa.valueOf(d.categoria) }.getOrDefault(CategoriaDespesa.OUTRA),
            existente = d, periodicidadePadrao = Periodicidade.MENSAL, nomeEditavel = true,
            aberta = aberta == chave, aoAlternar = { aberta = if (aberta == chave) null else chave },
        )
    }
    novas.forEach { k ->
        val chave = "n$k"
        SecaoDespesa(
            chave = chave, titulo = "Nova despesa", icone = iconeDe(CategoriaDespesa.OUTRA),
            categoria = CategoriaDespesa.OUTRA, existente = null, periodicidadePadrao = Periodicidade.MENSAL,
            nomeEditavel = true, aberta = aberta == chave, aoAlternar = { aberta = if (aberta == chave) null else chave },
            aoSalvarId = { id -> if (id > 0) novasIds[k] = id else novasIds.remove(k) },
        )
    }
    BotaoSecundario(
        "+ Adicionar outra despesa",
        {
            val k = proximaChave
            proximaChave += 1
            novas.add(k)
            aberta = "n$k"
        },
    )

    val plano = config.plano()
    if (plano != null) {
        val resumo = CustosFixos.resumir(config.despesasCompletas(lista), plano)
        CartaoVidro(Modifier.fillMaxWidth(), brilho = p.destaque) {
            Text("Total de custos fixos", fontWeight = FontWeight.Bold, color = p.texto)
            LinhaValor("Por dia de trabalho", Formatos.moeda(resumo.porDiaTrabalhado), destaque = true)
            LinhaValor("Por hora de trabalho", Formatos.moeda(resumo.porHoraTrabalhada))
            resumo.porKm(config.kmPorDia)?.let { LinhaValor("Por km", Formatos.moeda(it)) }
            LinhaValor("Por mês", Formatos.moeda(resumo.porMes))
            Text(
                "Inclui a parcela/aluguel do veículo, se houver. Calculado com os ${plano.diasPorSemana} dia(s) e " +
                    "${Formatos.numero(plano.horasPorDia, 1)} h por dia da sua rotina.",
                style = MaterialTheme.typography.bodySmall, color = p.textoSecundario,
            )
        }
    }
}

/**
 * Uma seção expansível de despesa, com salvamento automático.
 * O estado local nasce do que está no banco e é gravado ~0,6 s depois da última edição.
 */
@Composable
private fun SecaoDespesa(
    chave: String,
    titulo: String,
    icone: Painter,
    categoria: CategoriaDespesa,
    existente: DespesaEntity?,
    periodicidadePadrao: Periodicidade,
    nomeEditavel: Boolean,
    aberta: Boolean,
    aoAlternar: () -> Unit,
    aoSalvarId: (Long) -> Unit = {},
) {
    val p = LocalPaleta.current
    val repo = LocalContext.current.repositorio
    val escopo = rememberCoroutineScope()

    var id by remember(chave) { mutableLongStateOf(existente?.id ?: 0L) }
    var nome by rememberSaveable(chave) { mutableStateOf(existente?.nome ?: if (nomeEditavel) "" else titulo) }
    var valor by rememberSaveable(chave) { mutableStateOf(existente?.valor) }
    var periodicidade by rememberSaveable(chave) { mutableStateOf(existente?.periodicidade ?: periodicidadePadrao.name) }
    var prazo by rememberSaveable(chave) { mutableFloatStateOf((existente?.prazoMeses ?: Conversao.PRAZO_PADRAO_MESES).toFloat()) }
    var obs by rememberSaveable(chave) { mutableStateOf(existente?.observacao ?: "") }
    var salvamento by remember(chave) { mutableStateOf(EstadoSalvamento.NADA) }
    var primeira by remember(chave) { mutableStateOf(true) }

    // Salvamento automático com pausa (evita gravar a cada tecla e gravações duplicadas).
    LaunchedEffect(nome, valor, periodicidade, prazo, obs) {
        if (primeira) {
            primeira = false
            return@LaunchedEffect
        }
        salvamento = EstadoSalvamento.SALVANDO
        delay(600)
        val nomeFinal = nome.trim().ifEmpty { if (nomeEditavel) "Outra despesa" else titulo }
        id = repo.salvarDespesa(
            DespesaEntity(
                id = id,
                nome = nomeFinal,
                categoria = categoria.name,
                valor = valor ?: 0.0,
                periodicidade = periodicidade,
                prazoMeses = if (periodicidade == Periodicidade.VALOR_UNICO.name) prazo.toInt() else null,
                observacao = obs.trim().ifEmpty { null },
            )
        )
        aoSalvarId(id)
        salvamento = EstadoSalvamento.SALVO
    }

    val preenchida = (valor ?: 0.0) > 0
    val giro by animateFloatAsState(if (aberta) 180f else 0f, label = "seta")

    CartaoVidro(Modifier.fillMaxWidth(), brilho = if (aberta) p.destaque else null, preenchimento = 0.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = aoAlternar)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (preenchida) p.destaque.copy(alpha = 0.2f) else p.vidroForte),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icone, contentDescription = null, tint = if (preenchida) p.destaque else p.textoSecundario, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    if (nomeEditavel && nome.isNotBlank()) nome else titulo,
                    fontWeight = FontWeight.SemiBold, color = p.texto,
                )
                Text(
                    if (preenchida) "${Formatos.moeda(valor)} ${nomePeriodicidade(periodicidade, prazo.toInt())}"
                    else "Toque para preencher",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (preenchida) p.verde else p.textoSecundario,
                )
            }
            Icon(
                Icons.Filled.KeyboardArrowDown, contentDescription = if (aberta) "Fechar" else "Abrir",
                tint = p.textoSecundario, modifier = Modifier.rotate(giro),
            )
        }
        AnimatedVisibility(visible = aberta) {
            Column(
                Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (nomeEditavel) CampoTexto("Nome da despesa", nome, { nome = it })
                CampoMoeda("Valor", valor, { valor = it })
                Chips(periodicidades.map { it.first.name to it.second }, setOf(periodicidade)) { periodicidade = it }
                if (periodicidade == Periodicidade.VALOR_UNICO.name) {
                    SliderComValor(
                        rotulo = "Distribuir o valor em",
                        valor = prazo,
                        faixa = 1f..36f,
                        passo = 1f,
                        formatar = { "${it.toInt()} meses" },
                        aoMudar = { prazo = it.coerceAtLeast(1f) },
                        observacao = "Ex.: pneus de R$ 1.200 em 12 meses entram como R$ 100 por mês.",
                    )
                }
                CampoObservacao(obs) { obs = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IndicadorSalvamento(salvamento)
                }
                if (preenchida || nomeEditavel) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                valor = null
                                obs = ""
                                if (nomeEditavel) nome = ""
                                escopo.launch { if (id > 0) repo.excluirDespesa(id) }
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = p.vermelho, modifier = Modifier.size(18.dp))
                        Text("Remover esta despesa", color = p.vermelho, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
