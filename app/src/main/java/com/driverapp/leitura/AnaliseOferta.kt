package com.driverapp.leitura

import com.driverapp.calculo.AnaliseCorrida
import com.driverapp.calculo.Analisador
import com.driverapp.calculo.Classificador
import com.driverapp.calculo.CriteriosClassificacao
import com.driverapp.calculo.DadosCorrida
import com.driverapp.calculo.Nivel
import com.driverapp.calculo.Trecho
import com.driverapp.leitores.Categorias
import com.driverapp.leitores.LeituraValidada

/**
 * Liga a LEITURA (módulo `leitores`) à parte FINANCEIRA (módulo `calculo`).
 * Lógica pura (sem Android), testada em AnaliseOfertaTest.
 *
 * Usa os custos reais do cadastro do motorista; quando algum dado falta, o resultado
 * correspondente fica null ("indisponível") em vez de mostrar um número enganoso.
 */
data class ResultadoAnalise(
    val leitura: LeituraValidada,
    val categoria: String?,
    /** Null quando não há valor lido. */
    val analise: AnaliseCorrida?,
    /** Null quando não há R$/km nem R$/h para classificar (estado neutro, não "ruim"). */
    val nivel: Nivel?,
)

object AnaliseOferta {
    fun analisar(
        leitura: LeituraValidada,
        custoCombustivelPorKm: Double?,
        custoFixoPorHora: Double,
        criterios: CriteriosClassificacao,
    ): ResultadoAnalise {
        val categoria = Categorias.canonica(leitura.categoria)
        val valor = leitura.valor ?: return ResultadoAnalise(leitura, categoria, null, null)
        val analise = Analisador.analisar(
            DadosCorrida(
                valor = valor,
                coleta = leitura.coleta?.let { Trecho(it.minutos, it.km) },
                viagem = leitura.viagem?.let { Trecho(it.minutos, it.km) },
            ),
            custoCombustivelPorKm = custoCombustivelPorKm,
            custoFixoPorHora = custoFixoPorHora,
        )
        val principal = analise.principal
        val nivel = principal?.let { Classificador.classificar(it.reaisPorKm, it.reaisPorHora, criterios) }
        return ResultadoAnalise(leitura, categoria, analise, nivel)
    }
}
