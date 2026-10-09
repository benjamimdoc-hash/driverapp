package com.driverapp.ui.cadastro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.driverapp.R
import com.driverapp.calculo.CustosFixos
import com.driverapp.dados.ConfiguracaoEntity
import com.driverapp.dados.custoCombustivelPorKm
import com.driverapp.dados.despesasCompletas
import com.driverapp.dados.listaDias
import com.driverapp.dados.listaPlataformas
import com.driverapp.dados.metas
import com.driverapp.dados.plano
import com.driverapp.repositorio
import com.driverapp.ui.Formatos
import com.driverapp.ui.LocalPaleta
import com.driverapp.ui.componentes.CampoMoeda
import com.driverapp.ui.componentes.CampoNumero
import com.driverapp.ui.componentes.CampoTexto
import com.driverapp.ui.componentes.CartaoSelecao
import com.driverapp.ui.componentes.CartaoVidro
import com.driverapp.ui.componentes.Chips
import com.driverapp.ui.componentes.GradeSelecao
import com.driverapp.ui.componentes.LinhaValor
import com.driverapp.ui.componentes.MolduraEtapa
import com.driverapp.ui.componentes.SliderComValor
import com.driverapp.ui.estiloNumeroDestaque
import kotlinx.coroutines.delay

/**
 * Etapas do cadastro. Cada etapa é usada em dois lugares:
 *  - no cadastro inicial (botões "Continuar"/"Voltar", com barra de progresso);
 *  - nos Ajustes, para editar depois (botão "Concluído").
 *
 * SALVAMENTO AUTOMÁTICO: toda alteração é salva sozinha ~0,5 s depois que o motorista para
 * de mexer. Cada etapa entrega uma ALTERAÇÃO (função) que o repositório aplica sobre o que
 * já está no banco — assim uma etapa nunca apaga o que outra salvou.
 */
typealias Alteracao = (ConfiguracaoEntity) -> ConfiguracaoEntity

object Plataformas {
    val motorista = listOf("UBER" to "Uber", "NOVENTA_E_NOVE" to "99", "INDRIVE" to "inDrive")
    val entregador = listOf("IFOOD" to "iFood", "KEETA" to "Keeta", "NOVENTA_E_NOVE_FOOD" to "99Food")
    private val siglas = mapOf(
        "UBER" to "U", "NOVENTA_E_NOVE" to "99", "INDRIVE" to "iD",
        "IFOOD" to "iF", "KEETA" to "K", "NOVENTA_E_NOVE_FOOD" to "99F",
    )
    fun nome(chave: String): String = (motorista + entregador).firstOrNull { it.first == chave }?.second ?: chave
    fun sigla(chave: String): String = siglas[chave] ?: chave.take(2)
}

/** Parâmetros comuns a todas as etapas. */
class AcoesEtapa(
    val textoPrincipal: String,
    val aoConfirmar: (Alteracao) -> Unit,
    /** Volta salvando. Null = sem botão "Voltar". */
    val aoVoltar: ((Alteracao) -> Unit)?,
    /** Salvamento automático (chamado após uma pausa na edição). */
    val autoSalvar: (Alteracao) -> Unit,
    val topo: @Composable () -> Unit = {},
    val rodape: @Composable () -> Unit = {},
)

/** Salva automaticamente quando [chave] muda (ignora a primeira exibição da tela). */
@Composable
fun AutoSalvar(chave: Any?, acoes: AcoesEtapa, alteracao: Alteracao) {
    var primeira by remember { mutableStateOf(true) }
    LaunchedEffect(chave) {
        if (primeira) {
            primeira = false
            return@LaunchedEffect
        }
        delay(500)
        acoes.autoSalvar(alteracao)
    }
}

// ---------------------------------------------------------------- 1. Perfil

@Composable
fun EtapaPerfil(config: ConfiguracaoEntity, acoes: AcoesEtapa) {
    var perfil by rememberSaveable { mutableStateOf(config.perfil) }
    val alteracao: Alteracao = { atual ->
        // Ao trocar de perfil, as plataformas do outro perfil deixam de valer.
        val validas = if (perfil == "ENTREGADOR") Plataformas.entregador else Plataformas.motorista
        val mantidas = atual.listaPlataformas().filter { p -> validas.any { it.first == p } }
        atual.copy(perfil = perfil, plataformas = mantidas.joinToString(","))
    }
    AutoSalvar(perfil, acoes, alteracao)

    MolduraEtapa(
        titulo = "Como você trabalha?",
        subtitulo = "Escolha o seu perfil. Você pode mudar depois nos Ajustes.",
        textoPrincipal = acoes.textoPrincipal,
        principalHabilitado = perfil != null,
        aoPrincipal = { acoes.aoConfirmar(alteracao) },
        aoVoltar = acoes.aoVoltar?.let { voltar -> { voltar(alteracao) } },
        topo = acoes.topo,
        rodapeExtra = acoes.rodape,
    ) {
        val opcoes = listOf(
            Triple("MOTORISTA", "Motorista de carro", "Corridas com passageiros"),
            Triple("ENTREGADOR", "Motoboy / entregador", "Entregas de comida e pacotes"),
        )
        val icones = listOf(painterResource(R.drawable.ic_carro), painterResource(R.drawable.ic_moto))
        GradeSelecao(quantidade = 2, larguraMinima = 140.dp) { i, mod ->
            val (chave, titulo, desc) = opcoes[i]
            CartaoSelecao(titulo, desc, perfil == chave, { perfil = chave }, mod, icone = icones[i])
        }
    }
}

// ---------------------------------------------------------------- 2. Plataformas

@Composable
fun EtapaPlataformas(config: ConfiguracaoEntity, acoes: AcoesEtapa) {
    val opcoes = if (config.perfil == "ENTREGADOR") Plataformas.entregador else Plataformas.motorista
    var marcadas by rememberSaveable {
        mutableStateOf(config.listaPlataformas().filter { p -> opcoes.any { it.first == p } }.toSet())
    }
    val alteracao: Alteracao = { it.copy(plataformas = opcoes.map { o -> o.first }.filter { p -> p in marcadas }.joinToString(",")) }
    AutoSalvar(marcadas, acoes, alteracao)

    MolduraEtapa(
        titulo = "Quais plataformas você usa?",
        subtitulo = "Toque para marcar. Pode escolher mais de uma.",
        textoPrincipal = acoes.textoPrincipal,
        principalHabilitado = marcadas.isNotEmpty(),
        aoPrincipal = { acoes.aoConfirmar(alteracao) },
        aoVoltar = acoes.aoVoltar?.let { voltar -> { voltar(alteracao) } },
        topo = acoes.topo,
        rodapeExtra = acoes.rodape,
    ) {
        GradeSelecao(quantidade = opcoes.size, larguraMinima = 96.dp) { i, mod ->
            val (chave, nome) = opcoes[i]
            CartaoSelecao(
                titulo = nome, descricao = null, selecionado = chave in marcadas,
                aoTocar = { marcadas = if (chave in marcadas) marcadas - chave else marcadas + chave },
                modifier = mod, sigla = Plataformas.sigla(chave), compacto = true,
            )
        }
        if (config.perfil == "ENTREGADOR") {
            Text(
                "A leitura automática das plataformas de entrega virá numa fase futura. " +
                    "Jornada, GPS e lançamento manual já funcionam.",
                style = MaterialTheme.typography.bodySmall,
                color = LocalPaleta.current.textoSecundario,
            )
        }
    }
}

// ---------------------------------------------------------------- 3. Combustível

private const val CONSUMO_SUGERIDO = 8f

@Composable
fun EtapaCombustivel(config: ConfiguracaoEntity, acoes: AcoesEtapa) {
    val p = LocalPaleta.current
    var tipo by rememberSaveable { mutableStateOf(config.tipoCombustivel) }
    var preco by rememberSaveable { mutableStateOf(config.precoLitro) }
    var consumo by rememberSaveable { mutableFloatStateOf(config.kmPorLitro?.toFloat() ?: CONSUMO_SUGERIDO) }
    var consumoAjustado by rememberSaveable { mutableStateOf(config.kmPorLitro != null) }

    val precoValido = preco != null && preco!! > 0
    val alteracao: Alteracao = {
        it.copy(tipoCombustivel = tipo, precoLitro = preco, kmPorLitro = if (consumoAjustado) consumo.toDouble() else it.kmPorLitro)
    }
    AutoSalvar(listOf(tipo, preco, consumo, consumoAjustado), acoes, alteracao)

    MolduraEtapa(
        titulo = "Combustível",
        subtitulo = "Usado para calcular quanto cada km custa. Dá para ajustar o preço a qualquer momento.",
        textoPrincipal = acoes.textoPrincipal,
        principalHabilitado = precoValido,
        aoPrincipal = {
            // Ao continuar, o consumo mostrado passa a ser o informado pelo motorista.
            consumoAjustado = true
            acoes.aoConfirmar { it.copy(tipoCombustivel = tipo, precoLitro = preco, kmPorLitro = consumo.toDouble()) }
        },
        aoVoltar = acoes.aoVoltar?.let { voltar -> { voltar(alteracao) } },
        topo = acoes.topo,
        rodapeExtra = acoes.rodape,
    ) {
        val opcoes = listOf("GASOLINA" to "Gasolina", "ETANOL" to "Etanol", "OUTRO" to "Outro")
        val icones = listOf(
            painterResource(R.drawable.ic_bomba), painterResource(R.drawable.ic_folha), painterResource(R.drawable.ic_raio),
        )
        GradeSelecao(quantidade = 3, larguraMinima = 96.dp) { i, mod ->
            val (chave, nome) = opcoes[i]
            CartaoSelecao(nome, null, tipo == chave, { tipo = chave }, mod, icone = icones[i], compacto = true)
        }
        CampoMoeda("Preço por litro", preco, { preco = it }, ajuda = "Digite só os números: 589 vira R$ 5,89")
        SliderComValor(
            rotulo = "Consumo médio",
            valor = consumo,
            faixa = 4f..20f,
            passo = 0.5f,
            formatar = { "${Formatos.numero(it.toDouble(), 1)} km/l" },
            aoMudar = { consumo = it; consumoAjustado = true },
            observacao = if (!consumoAjustado) "Valor sugerido. Ajuste para o consumo real do seu veículo." else null,
        )
        val custoKm = if (precoValido && consumo > 0) preco!! / consumo else null
        CartaoVidro(Modifier.fillMaxWidth(), brilho = if (custoKm != null) p.ciano else null) {
            Text("Custo de combustível por km", style = MaterialTheme.typography.labelLarge, color = p.textoSecundario)
            Text(
                custoKm?.let { Formatos.moeda(it) } ?: "Informe o preço",
                style = estiloNumeroDestaque(32, if (custoKm != null) p.ciano else p.textoSecundario),
            )
            Text("Preço do litro ÷ consumo", style = MaterialTheme.typography.bodySmall, color = p.textoSecundario)
        }
    }
}

// ---------------------------------------------------------------- 4. Veículo

@Composable
fun EtapaVeiculo(config: ConfiguracaoEntity, acoes: AcoesEtapa) {
    var veiculo by rememberSaveable { mutableStateOf(config.veiculo) }
    // Os valores de cada modalidade são guardados separados: trocar de modalidade não apaga o outro.
    var parcela by rememberSaveable { mutableStateOf(config.parcelaMensal) }
    var aluguel by rememberSaveable { mutableStateOf(config.aluguelSemanal) }

    val valido = when (veiculo) {
        "PROPRIO" -> true
        "FINANCIADO" -> (parcela ?: 0.0) > 0
        "ALUGADO" -> (aluguel ?: 0.0) > 0
        else -> false
    }
    val alteracao: Alteracao = { it.copy(veiculo = veiculo, parcelaMensal = parcela, aluguelSemanal = aluguel) }
    AutoSalvar(listOf(veiculo, parcela, aluguel), acoes, alteracao)

    MolduraEtapa(
        titulo = "Seu veículo",
        subtitulo = "Só o custo da modalidade escolhida entra nos cálculos.",
        textoPrincipal = acoes.textoPrincipal,
        principalHabilitado = valido,
        aoPrincipal = { acoes.aoConfirmar(alteracao) },
        aoVoltar = acoes.aoVoltar?.let { voltar -> { voltar(alteracao) } },
        topo = acoes.topo,
        rodapeExtra = acoes.rodape,
    ) {
        val opcoes = listOf(
            Triple("PROPRIO", "Próprio", "Quitado"),
            Triple("FINANCIADO", "Financiado", "Parcela mensal"),
            Triple("ALUGADO", "Alugado", "Aluguel semanal"),
        )
        val icones = listOf(
            painterResource(R.drawable.ic_chave), painterResource(R.drawable.ic_banco), rememberVectorPainter(Icons.Filled.DateRange),
        )
        GradeSelecao(quantidade = 3, larguraMinima = 96.dp) { i, mod ->
            val (chave, nome, desc) = opcoes[i]
            CartaoSelecao(nome, desc, veiculo == chave, { veiculo = chave }, mod, icone = icones[i], compacto = true)
        }
        when (veiculo) {
            "FINANCIADO" -> CampoMoeda("Valor da parcela mensal", parcela, { parcela = it })
            "ALUGADO" -> {
                CampoMoeda("Valor do aluguel semanal", aluguel, { aluguel = it })
                val plano = config.plano()
                val a = aluguel
                if (plano != null && a != null && a > 0) {
                    val porDia = a / plano.diasPorSemana
                    CartaoVidro(Modifier.fillMaxWidth()) {
                        LinhaValor("Por dia de trabalho", Formatos.moeda(porDia), destaque = true)
                        LinhaValor("Por hora de trabalho", Formatos.moeda(porDia / plano.horasPorDia))
                    }
                } else {
                    Text(
                        "O custo por dia e por hora aparece depois que você informar seus dias e horas de trabalho.",
                        style = MaterialTheme.typography.bodySmall, color = LocalPaleta.current.textoSecundario,
                    )
                }
            }
            "PROPRIO" -> Text(
                "Sem parcela nem aluguel. Despesas como IPVA e seguro ficam na etapa de custos.",
                style = MaterialTheme.typography.bodySmall, color = LocalPaleta.current.textoSecundario,
            )
        }
    }
}

// ---------------------------------------------------------------- 5. Dias e horas

private val diasSemana = listOf(
    "1" to "Segunda", "2" to "Terça", "3" to "Quarta", "4" to "Quinta", "5" to "Sexta", "6" to "Sábado", "7" to "Domingo",
)

/** Formata o que foi digitado como horário "HH:MM". */
private fun mascaraHora(texto: String): String {
    val d = texto.filter { it.isDigit() }.take(4)
    return if (d.length <= 2) d else d.substring(0, 2) + ":" + d.substring(2)
}

private fun horaValida(h: String): Boolean {
    val partes = h.split(":")
    if (partes.size != 2 || partes[1].length != 2) return false
    val hh = partes[0].toIntOrNull() ?: return false
    val mm = partes[1].toIntOrNull() ?: return false
    return hh in 0..23 && mm in 0..59
}

@Composable
fun EtapaJornadaTrabalho(config: ConfiguracaoEntity, acoes: AcoesEtapa) {
    val p = LocalPaleta.current
    var dias by rememberSaveable { mutableStateOf(config.listaDias().map { it.toString() }.toSet()) }
    var horas by rememberSaveable { mutableFloatStateOf(config.horasPorDia?.toFloat() ?: 8f) }
    var inicio by rememberSaveable { mutableStateOf(config.horaInicio ?: "") }
    var fim by rememberSaveable { mutableStateOf(config.horaFim ?: "") }
    var kmDia by rememberSaveable { mutableStateOf(Formatos.campo(config.kmPorDia)) }

    val alteracao: Alteracao = {
        it.copy(
            diasTrabalho = dias.mapNotNull { d -> d.toIntOrNull() }.sorted().joinToString(","),
            horasPorDia = horas.toDouble(),
            horaInicio = inicio.takeIf { h -> horaValida(h) },
            horaFim = fim.takeIf { h -> horaValida(h) },
            kmPorDia = Formatos.lerNumero(kmDia)?.takeIf { k -> k > 0 },
        )
    }
    AutoSalvar(listOf(dias, horas, inicio, fim, kmDia), acoes, alteracao)

    MolduraEtapa(
        titulo = "Sua rotina",
        subtitulo = "Usada para dividir a meta e os custos fixos pelos dias que você realmente trabalha.",
        textoPrincipal = acoes.textoPrincipal,
        principalHabilitado = dias.isNotEmpty() && horas > 0,
        aoPrincipal = { acoes.aoConfirmar(alteracao) },
        aoVoltar = acoes.aoVoltar?.let { voltar -> { voltar(alteracao) } },
        topo = acoes.topo,
        rodapeExtra = acoes.rodape,
    ) {
        Text("Dias de trabalho", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = p.texto)
        Chips(diasSemana, dias) { d -> dias = if (d in dias) dias - d else dias + d }
        Text(
            if (dias.isEmpty()) "Marque pelo menos um dia." else "${dias.size} dia(s) por semana",
            style = MaterialTheme.typography.bodySmall,
            color = if (dias.isEmpty()) p.vermelho else p.textoSecundario,
        )
        SliderComValor(
            rotulo = "Horas por dia",
            valor = horas,
            faixa = 1f..16f,
            passo = 0.5f,
            formatar = { "${Formatos.numero(it.toDouble(), 1)} h" },
            aoMudar = { horas = it.coerceIn(0.5f, 24f) },
        )
        Text("Horário habitual (opcional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = p.texto)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CampoNumero("Início", inicio, { inicio = mascaraHora(it) }, Modifier.weight(1f), ajuda = "Ex.: 0700")
            CampoNumero("Término", fim, { fim = mascaraHora(it) }, Modifier.weight(1f), ajuda = "Ex.: 1800")
        }
        CampoNumero(
            "Km rodados por dia (opcional)", kmDia, { kmDia = it }, sufixo = "km",
            ajuda = "Permite calcular os custos fixos por km",
        )
    }
}

// ---------------------------------------------------------------- 7. Meta

@Composable
fun EtapaMeta(config: ConfiguracaoEntity, acoes: AcoesEtapa) {
    val p = LocalPaleta.current
    val repo = LocalContext.current.repositorio
    val despesas by repo.despesas.collectAsStateWithLifecycle(initialValue = emptyList())
    var meta by rememberSaveable { mutableFloatStateOf(config.metaSemanal?.toFloat() ?: 1500f) }

    val alteracao: Alteracao = { it.copy(metaSemanal = meta.toDouble()) }
    AutoSalvar(meta, acoes, alteracao)
    val simulada = config.copy(metaSemanal = meta.toDouble())

    MolduraEtapa(
        titulo = "Meta da semana",
        subtitulo = "Quanto você quer FATURAR por semana (valor bruto pago pelas plataformas)?",
        textoPrincipal = acoes.textoPrincipal,
        principalHabilitado = meta > 0,
        aoPrincipal = { acoes.aoConfirmar(alteracao) },
        aoVoltar = acoes.aoVoltar?.let { voltar -> { voltar(alteracao) } },
        topo = acoes.topo,
        rodapeExtra = acoes.rodape,
    ) {
        SliderComValor(
            rotulo = "Meta semanal",
            valor = meta,
            faixa = 0f..10000f,
            passo = 50f,
            formatar = { Formatos.moeda(it.toDouble()) },
            aoMudar = { meta = it },
            digitarComoMoeda = true,
            observacao = "Arraste para ajustar rápido ou toque em Digitar para o valor exato.",
        )
        val metas = simulada.metas()
        val plano = simulada.plano()
        CartaoVidro(Modifier.fillMaxWidth(), brilho = p.destaque) {
            Text("Como fica a sua meta", fontWeight = FontWeight.Bold, color = p.texto)
            if (metas != null) {
                Text(Formatos.moeda(metas.metaDiaria) + " por dia", style = estiloNumeroDestaque(28, p.destaque))
                LinhaValor("Por hora", Formatos.moeda(metas.metaPorHora))
                config.kmPorDia?.let { km -> metas.metaPorKm(km)?.let { LinhaValor("Por km", Formatos.moeda(it)) } }
            } else {
                Text("Informe seus dias e horas para ver a meta diária.", color = p.textoSecundario)
            }
            LinhaValor("Combustível por km", Formatos.moeda(simulada.custoCombustivelPorKm()))
            if (plano != null) {
                val custos = CustosFixos.resumir(simulada.despesasCompletas(despesas), plano)
                LinhaValor("Custos fixos por dia", Formatos.moeda(custos.porDiaTrabalhado))
                LinhaValor("Custos fixos por semana", Formatos.moeda(custos.porSemana))
            }
            Text(
                "A meta é de faturamento bruto. O resultado estimado, já descontando os custos, aparece separado no painel.",
                style = MaterialTheme.typography.bodySmall, color = p.textoSecundario,
            )
        }
    }
}

/** Observação usada pela etapa de despesas e pelos ajustes. */
@Composable
internal fun CampoObservacao(valor: String, aoMudar: (String) -> Unit) =
    CampoTexto("Observação (opcional)", valor, aoMudar)
