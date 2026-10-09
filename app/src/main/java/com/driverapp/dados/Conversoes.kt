package com.driverapp.dados

import com.driverapp.calculo.CategoriaDespesa
import com.driverapp.calculo.Combustivel
import com.driverapp.calculo.CustosFixos
import com.driverapp.calculo.Despesa
import com.driverapp.calculo.EstadoJornada
import com.driverapp.calculo.Jornada
import com.driverapp.calculo.Metas
import com.driverapp.calculo.Periodicidade
import com.driverapp.calculo.PlanoDeTrabalho
import com.driverapp.calculo.TipoCombustivel

/** Conversões entre as tabelas do banco e a lógica do módulo `calculo`. */

fun ConfiguracaoEntity.listaPlataformas(): List<String> = plataformas.split(',').filter { it.isNotBlank() }
fun ConfiguracaoEntity.listaDias(): List<Int> = diasTrabalho.split(',').mapNotNull { it.trim().toIntOrNull() }

fun ConfiguracaoEntity.combustivel(): Combustivel? {
    val preco = precoLitro ?: return null
    val consumo = kmPorLitro ?: return null
    val tipo = runCatching { TipoCombustivel.valueOf(tipoCombustivel) }.getOrDefault(TipoCombustivel.OUTRO)
    return Combustivel(tipo, preco, consumo)
}

fun ConfiguracaoEntity.custoCombustivelPorKm(): Double? = combustivel()?.custoPorKm

fun ConfiguracaoEntity.plano(): PlanoDeTrabalho? {
    val dias = listaDias().size
    val horas = horasPorDia ?: return null
    if (dias !in 1..7 || horas <= 0 || horas > 24) return null
    return PlanoDeTrabalho(dias, horas)
}

fun ConfiguracaoEntity.metas(): Metas? {
    val meta = metaSemanal ?: return null
    val plano = plano() ?: return null
    return Metas(meta, plano)
}

/**
 * Todas as despesas fixas: as cadastradas + a do veículo (parcela ou aluguel).
 * A do veículo vem só da configuração, com id fixo — por isso nunca é contada duas vezes.
 */
fun ConfiguracaoEntity.despesasCompletas(cadastradas: List<DespesaEntity>): List<Despesa> {
    val lista = cadastradas.map { it.paraDominio() }.toMutableList()
    when (veiculo) {
        "FINANCIADO" -> parcelaMensal?.takeIf { it > 0 }?.let {
            lista += Despesa("veiculo", "Parcela do veículo", CategoriaDespesa.PARCELA_VEICULO, it, Periodicidade.MENSAL)
        }
        "ALUGADO" -> aluguelSemanal?.takeIf { it > 0 }?.let {
            lista += Despesa("veiculo", "Aluguel do veículo", CategoriaDespesa.ALUGUEL_VEICULO, it, Periodicidade.SEMANAL)
        }
    }
    return lista
}

/** Custos fixos por hora trabalhada (0 se o plano de trabalho não estiver configurado). */
fun ConfiguracaoEntity.custoFixoPorHora(cadastradas: List<DespesaEntity>): Double {
    val plano = plano() ?: return 0.0
    return CustosFixos.resumir(despesasCompletas(cadastradas), plano).porHoraTrabalhada
}

fun DespesaEntity.paraDominio() = Despesa(
    id = "d$id",
    nome = nome,
    categoria = runCatching { CategoriaDespesa.valueOf(categoria) }.getOrDefault(CategoriaDespesa.OUTRA),
    valor = valor,
    periodicidade = runCatching { Periodicidade.valueOf(periodicidade) }.getOrDefault(Periodicidade.MENSAL),
)

fun JornadaEntity.paraDominio() = Jornada(
    inicioEm = inicioEm,
    estado = runCatching { EstadoJornada.valueOf(estado) }.getOrDefault(EstadoJornada.PAUSADA),
    msAcumulados = msAcumulados,
    retomadaEm = retomadaEm,
    metros = metros,
    metrosNaPausa = metrosNaPausa,
    finalizadaEm = finalizadaEm,
)

fun JornadaEntity.comDominio(j: Jornada) = copy(
    inicioEm = j.inicioEm,
    estado = j.estado.name,
    msAcumulados = j.msAcumulados,
    retomadaEm = j.retomadaEm,
    metros = j.metros,
    metrosNaPausa = j.metrosNaPausa,
    finalizadaEm = j.finalizadaEm,
)

fun Jornada.paraEntidade() = JornadaEntity(
    inicioEm = inicioEm,
    estado = estado.name,
    msAcumulados = msAcumulados,
    retomadaEm = retomadaEm,
    metros = metros,
    metrosNaPausa = metrosNaPausa,
    finalizadaEm = finalizadaEm,
)
