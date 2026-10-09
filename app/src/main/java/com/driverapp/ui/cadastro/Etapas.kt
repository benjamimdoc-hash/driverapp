package com.driverapp.ui.cadastro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.driverapp.calculo.CategoriaDespesa
import com.driverapp.calculo.CustosFixos
import com.driverapp.calculo.Periodicidade
import com.driverapp.dados.ConfiguracaoEntity
import com.driverapp.dados.DespesaEntity
import com.driverapp.dados.custoCombustivelPorKm
import com.driverapp.dados.despesasCompletas
import com.driverapp.dados.listaDias
import com.driverapp.dados.listaPlataformas
import com.driverapp.dados.metas
import com.driverapp.dados.plano
import com.driverapp.repositorio
import com.driverapp.ui.Formatos
import com.driverapp.ui.componentes.CampoNumero
import com.driverapp.ui.componentes.CampoTexto
import com.driverapp.ui.componentes.CartaoOpcao
import com.driverapp.ui.componentes.Chips
import com.driverapp.ui.componentes.LinhaValor
import com.driverapp.ui.componentes.MolduraEtapa
import kotlinx.coroutines.launch

/**
 * Etapas do cadastro. Cada etapa é usada em dois lugares:
 *  - no cadastro inicial (botão "Continuar", com barra de progresso);
 *  - nos Ajustes, para editar depois (botão "Salvar").
 *
 * Cada etapa devolve uma ALTERAÇÃO da configuração (uma função), que o repositório
 * aplica sobre o que está salvo no banco — assim uma etapa nunca apaga o que a outra salvou.
 */
typealias Alteracao = (ConfiguracaoEntity) -> ConfiguracaoEntity

object Plataformas {
    val motorista = listOf("UBER" to "Uber", "NOVENTA_E_NOVE" to "99", "INDRIVE" to "inDrive")
    val entregador = listOf("IFOOD" to "iFood", "KEETA" to "Keeta", "NOVENTA_E_NOVE_FOOD" to "99Food")
    fun nome(chave: String): String = (motorista + entregador).firstOrNull { it.first == chave }?.second ?: chave
}

/** Parâmetros comuns a todas as etapas. */
class AcoesEtapa(
    val textoPrincipal: String,
    val aoConfirmar: (Alteracao) -> Unit,
    /** No cadastro: volta salvando. Nos ajustes: cancela. Null = sem botão. */
    val aoVoltar: ((Alteracao) -> Unit)?,
    val topo: @Composable () -> Unit = {},
)

// ---------------------------------------------------------------- 1. Perfil + plataformas

@Composable
fun EtapaPerfil(config: ConfiguracaoEntity, acoes: AcoesEtapa) {
    var perfil by rememberSaveable { mutableStateOf(config.perfil) }
    var plataformas by rememberSaveable { mutableStateOf(config.listaPlataformas().toSet()) }

    val opcoes = if (perfil == "ENTREGADOR") Plataformas.entregador else Plataformas.motorista
    val validas = plataformas.filter { p -> opcoes.any { it.first == p } }.toSet()
    val alteracao: Alteracao = { it.copy(perfil = perfil, plataformas = validas.joinToString(",")) }

    MolduraEtapa(
        titulo = "Como você trabalha?",
        subtitulo = "Escolha o seu perfil e as plataformas que usa. Pode marcar mais de uma.",
        textoPrincipal = acoes.textoPrincipal,
        principalHabilitado = perfil != null && validas.isNotEmpty(),
        aoPrincipal = { acoes.aoConfirmar(alteracao) },
        aoVoltar = acoes.aoVoltar?.let { voltar -> { voltar(alteracao) } },
        topo = acoes.topo,
    ) {
        CartaoOpcao("Motorista de carro", "Uber, 99, inDrive", perfil == "MOTORISTA") { perfil = "MOTORISTA" }
        CartaoOpcao("Motoboy / entregador", "iFood, Keeta, 99Food", perfil == "ENTREGADOR") { perfil = "ENTREGADOR" }
        if (perfil != null) {
            Text("Plataformas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Chips(opcoes, validas) { chave ->
                plataformas = if (chave in plataformas) plataformas - chave else plataformas + chave
            }
            if (perfil == "ENTREGADOR") {
                Text(
                    "A leitura automática das plataformas de entrega virá numa fase futura. " +
                        "Por enquanto você pode usar a jornada, o GPS e o lançamento manual.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- 2. Combustível

@Composable
fun EtapaCombustivel(config: ConfiguracaoEntity, acoes: AcoesEtapa) {
    var tipo by rememberSaveable { mutableStateOf(config.tipoCombustivel) }
    var preco by rememberSaveable { mutableStateOf(Formatos.campo(config.precoLitro)) }
    var consumo by rememberSaveable { mutableStateOf(Formatos.campo(config.kmPorLitro)) }

    val precoNum = Formatos.lerNumero(preco)
    val consumoNum = Formatos.lerNumero(consumo)
    val valido = precoNum != null && precoNum > 0 && consumoNum != null && consumoNum > 0
    val alteracao: Alteracao = { it.copy(tipoCombustivel = tipo, precoLitro = precoNum, kmPorLitro = consumoNum) }

    MolduraEtapa(
        titulo = "Combustível",
        subtitulo = "Usado para calcular quanto cada km custa. Você pode mudar o preço a qualquer momento.",
        textoPrincipal = acoes.textoPrincipal,
        principalHabilitado = valido,
        aoPrincipal = { acoes.aoConfirmar(alteracao) },
        aoVoltar = acoes.aoVoltar?.let { voltar -> { voltar(alteracao) } },
        topo = acoes.topo,
    ) {
        Chips(
            listOf("GASOLINA" to "Gasolina", "ETANOL" to "Etanol", "OUTRO" to "Outro (GNV, elétrico…)"),
            setOf(tipo),
        ) { tipo = it }
        CampoNumero("Preço por litro (R$)", preco, { preco = it }, ajuda = "Ex.: 5,89")
        CampoNumero("Consumo médio (km por litro)", consumo, { consumo = it }, ajuda = "Ex.: 10")
        if (valido) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    LinhaValor("Custo de combustível por km", Formatos.moeda(precoNum!! / consumoNum!!), destaque = true)
                    Text(
                        "Preço do litro ÷ consumo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 3. Veículo

@Composable
fun EtapaVeiculo(config: ConfiguracaoEntity, acoes: AcoesEtapa) {
    var veiculo by rememberSaveable { mutableStateOf(config.veiculo) }
    var parcela by rememberSaveable { mutableStateOf(Formatos.campo(config.parcelaMensal)) }
    var aluguel by rememberSaveable { mutableStateOf(Formatos.campo(config.aluguelSemanal)) }

    val parcelaNum = Formatos.lerNumero(parcela)
    val aluguelNum = Formatos.lerNumero(aluguel)
    val valido = when (veiculo) {
        "PROPRIO" -> true
        "FINANCIADO" -> parcelaNum != null && parcelaNum > 0
        "ALUGADO" -> aluguelNum != null && aluguelNum > 0
        else -> false
    }
    val alteracao: Alteracao = {
        it.copy(
            veiculo = veiculo,
            parcelaMensal = if (veiculo == "FINANCIADO") parcelaNum else null,
            aluguelSemanal = if (veiculo == "ALUGADO") aluguelNum else null,
        )
    }

    MolduraEtapa(
        titulo = "Seu veículo",
        subtitulo = "O carro é seu, financiado ou alugado?",
        textoPrincipal = acoes.textoPrincipal,
        principalHabilitado = valido,
        aoPrincipal = { acoes.aoConfirmar(alteracao) },
        aoVoltar = acoes.aoVoltar?.let { voltar -> { voltar(alteracao) } },
        topo = acoes.topo,
    ) {
        CartaoOpcao("Próprio (quitado)", null, veiculo == "PROPRIO") { veiculo = "PROPRIO" }
        CartaoOpcao("Financiado", "Tem parcela mensal", veiculo == "FINANCIADO") { veiculo = "FINANCIADO" }
        CartaoOpcao("Alugado", "Paga aluguel semanal", veiculo == "ALUGADO") { veiculo = "ALUGADO" }

        if (veiculo == "FINANCIADO") {
            CampoNumero("Valor da parcela mensal (R$)", parcela, { parcela = it })
        }
        if (veiculo == "ALUGADO") {
            CampoNumero("Valor do aluguel semanal (R$)", aluguel, { aluguel = it })
            val plano = config.plano()
            if (plano != null && aluguelNum != null && aluguelNum > 0) {
                val porDia = aluguelNum / plano.diasPorSemana
                Text(
                    "≈ ${Formatos.moeda(porDia)} por dia de trabalho • ${Formatos.moeda(porDia / plano.horasPorDia)} por hora",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    "O custo por dia e por hora aparece depois que você informar seus dias e horas de trabalho.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- 4. Custos adicionais

private val categoriasDespesa = listOf(
    CategoriaDespesa.IPVA to "IPVA",
    CategoriaDespesa.SEGURO to "Seguro",
    CategoriaDespesa.MANUTENCAO to "Manutenção",
    CategoriaDespesa.PNEUS to "Pneus",
    CategoriaDespesa.LAVAGEM to "Lavagem",
    CategoriaDespesa.LICENCIAMENTO to "Licenciamento",
    CategoriaDespesa.PEDAGIO_ESTACIONAMENTO to "Pedágio / estacionamento",
    CategoriaDespesa.OUTRA to "Outra",
)

private val periodicidades = listOf(
    Periodicidade.POR_DIA_TRABALHADO to "Por dia trabalhado",
    Periodicidade.SEMANAL to "Por semana",
    Periodicidade.MENSAL to "Por mês",
    Periodicidade.ANUAL to "Por ano",
)

fun nomePeriodicidade(p: String): String =
    periodicidades.firstOrNull { it.first.name == p }?.second?.lowercase() ?: p

@Composable
fun EtapaDespesas(config: ConfiguracaoEntity, acoes: AcoesEtapa) {
    val repo = LocalContext.current.repositorio
    val escopo = rememberCoroutineScope()
    val despesas by repo.despesas.collectAsStateWithLifecycle(initialValue = emptyList())

    var categoria by rememberSaveable { mutableStateOf(CategoriaDespesa.IPVA.name) }
    var nome by rememberSaveable { mutableStateOf("") }
    var valor by rememberSaveable { mutableStateOf("") }
    var periodicidade by rememberSaveable { mutableStateOf(Periodicidade.ANUAL.name) }

    val valorNum = Formatos.lerNumero(valor)
    val nomeFinal = if (categoria == CategoriaDespesa.OUTRA.name) nome.trim()
    else categoriasDespesa.first { it.first.name == categoria }.second
    val podeAdicionar = valorNum != null && valorNum > 0 && nomeFinal.isNotEmpty()
    val semAlteracao: Alteracao = { it }

    MolduraEtapa(
        titulo = "Custos adicionais",
        subtitulo = "Cadastre cada despesa uma vez, com a periodicidade dela. O app converte tudo para dia e hora " +
            "sem contar nada duas vezes. Combustível e parcela/aluguel do carro já foram informados — não repita aqui.",
        textoPrincipal = acoes.textoPrincipal,
        principalHabilitado = true,
        aoPrincipal = { acoes.aoConfirmar(semAlteracao) },
        aoVoltar = acoes.aoVoltar?.let { voltar -> { voltar(semAlteracao) } },
        topo = acoes.topo,
    ) {
        Chips(categoriasDespesa.map { it.first.name to it.second }, setOf(categoria)) { categoria = it }
        if (categoria == CategoriaDespesa.OUTRA.name) {
            CampoTexto("Nome da despesa", nome, { nome = it })
        }
        CampoNumero("Valor (R$)", valor, { valor = it })
        Chips(periodicidades.map { it.first.name to it.second }, setOf(periodicidade)) { periodicidade = it }
        OutlinedButton(
            enabled = podeAdicionar,
            onClick = {
                val nova = DespesaEntity(nome = nomeFinal, categoria = categoria, valor = valorNum!!, periodicidade = periodicidade)
                escopo.launch { repo.adicionarDespesa(nova) }
                valor = ""
                nome = ""
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Adicionar despesa") }

        if (despesas.isEmpty()) {
            Text(
                "Nenhuma despesa cadastrada. Esta etapa é opcional.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            HorizontalDivider()
            despesas.forEach { d ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(d.nome, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${Formatos.moeda(d.valor)} ${nomePeriodicidade(d.periodicidade)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    IconButton(onClick = { escopo.launch { repo.excluirDespesa(d.id) } }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Excluir ${d.nome}")
                    }
                }
            }
            val plano = config.plano()
            if (plano != null) {
                val resumo = CustosFixos.resumir(config.despesasCompletas(despesas), plano)
                LinhaValor("Custos fixos por dia de trabalho", Formatos.moeda(resumo.porDiaTrabalhado), destaque = true)
            }
        }
    }
}

// ---------------------------------------------------------------- 5. Dias e horas

private val diasSemana = listOf("1" to "Seg", "2" to "Ter", "3" to "Qua", "4" to "Qui", "5" to "Sex", "6" to "Sáb", "7" to "Dom")

@Composable
fun EtapaJornadaTrabalho(config: ConfiguracaoEntity, acoes: AcoesEtapa) {
    var dias by rememberSaveable { mutableStateOf(config.listaDias().map { it.toString() }.toSet()) }
    var horas by rememberSaveable { mutableStateOf(Formatos.campo(config.horasPorDia)) }

    val horasNum = Formatos.lerNumero(horas)
    val valido = dias.isNotEmpty() && horasNum != null && horasNum > 0 && horasNum <= 24
    val alteracao: Alteracao = {
        it.copy(diasTrabalho = dias.mapNotNull { d -> d.toIntOrNull() }.sorted().joinToString(","), horasPorDia = horasNum)
    }

    MolduraEtapa(
        titulo = "Sua rotina",
        subtitulo = "Em quais dias você trabalha e quantas horas por dia, em média?",
        textoPrincipal = acoes.textoPrincipal,
        principalHabilitado = valido,
        aoPrincipal = { acoes.aoConfirmar(alteracao) },
        aoVoltar = acoes.aoVoltar?.let { voltar -> { voltar(alteracao) } },
        topo = acoes.topo,
    ) {
        Text("Dias de trabalho", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Chips(diasSemana, dias) { d -> dias = if (d in dias) dias - d else dias + d }
        CampoNumero("Horas de trabalho por dia", horas, { horas = it }, ajuda = "Ex.: 10")
        if (horasNum != null && horasNum > 24) {
            Text("Informe no máximo 24 horas.", color = MaterialTheme.colorScheme.error)
        }
    }
}

// ---------------------------------------------------------------- 6. Meta

@Composable
fun EtapaMeta(config: ConfiguracaoEntity, acoes: AcoesEtapa) {
    val repo = LocalContext.current.repositorio
    val despesas by repo.despesas.collectAsStateWithLifecycle(initialValue = emptyList())
    var meta by rememberSaveable { mutableStateOf(Formatos.campo(config.metaSemanal)) }

    val metaNum = Formatos.lerNumero(meta)
    val alteracao: Alteracao = { it.copy(metaSemanal = metaNum) }
    val simulada = config.copy(metaSemanal = metaNum)

    MolduraEtapa(
        titulo = "Meta da semana",
        subtitulo = "Quanto você quer FATURAR (valor bruto pago pelas plataformas) por semana?",
        textoPrincipal = acoes.textoPrincipal,
        principalHabilitado = metaNum != null && metaNum > 0,
        aoPrincipal = { acoes.aoConfirmar(alteracao) },
        aoVoltar = acoes.aoVoltar?.let { voltar -> { voltar(alteracao) } },
        topo = acoes.topo,
    ) {
        CampoNumero("Meta semanal de faturamento (R$)", meta, { meta = it })
        val metas = simulada.metas()
        val plano = simulada.plano()
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Seu resumo", fontWeight = FontWeight.Bold)
                if (metas != null) {
                    LinhaValor("Meta por dia", Formatos.moeda(metas.metaDiaria), destaque = true)
                    LinhaValor("Meta por hora", Formatos.moeda(metas.metaPorHora))
                }
                LinhaValor("Combustível por km", Formatos.moeda(simulada.custoCombustivelPorKm()))
                if (plano != null) {
                    val custos = CustosFixos.resumir(simulada.despesasCompletas(despesas), plano)
                    LinhaValor("Custos fixos por dia", Formatos.moeda(custos.porDiaTrabalhado))
                    LinhaValor("Custos fixos por semana", Formatos.moeda(custos.porSemana))
                }
                Text(
                    "A meta é de faturamento bruto. O resultado estimado (descontando custos) aparece separado no painel.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
