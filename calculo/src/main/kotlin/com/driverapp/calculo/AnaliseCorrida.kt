package com.driverapp.calculo

/**
 * Análise de UMA oferta de corrida.
 *
 * Mostra sempre dois cenários, quando os dados existem:
 *  - TOTAL: deslocamento até o passageiro (coleta) + viagem com o passageiro.
 *  - SÓ VIAGEM: apenas o trecho com o passageiro.
 *
 * Nada é inventado: se um dado não foi lido, o resultado correspondente fica null.
 */
data class Trecho(val minutos: Double?, val km: Double?)

data class DadosCorrida(
    val valor: Double,
    val coleta: Trecho?,
    val viagem: Trecho?,
)

enum class TipoCenario { TOTAL, SO_VIAGEM }

data class Cenario(
    val tipo: TipoCenario,
    val km: Double?,
    val minutos: Double?,
    val reaisPorKm: Double?,
    val reaisPorHora: Double?,
    /** Custo variável: combustível dos km considerados. */
    val custoCombustivel: Double?,
    /** Valor − combustível. */
    val resultado: Double?,
    /** Parte dos custos fixos proporcional ao tempo da corrida. */
    val custoFixoAlocado: Double?,
    /** Valor − combustível − custo fixo alocado. */
    val resultadoAposFixos: Double?,
)

data class AnaliseCorrida(
    val valor: Double,
    val total: Cenario?,
    val soViagem: Cenario?,
) {
    /** Cenário principal: o total, quando existe; senão, só a viagem. */
    val principal: Cenario? get() = total ?: soViagem
}

object Analisador {

    /**
     * @param custoCombustivelPorKm custo de combustível por km (null = não configurado).
     * @param custoFixoPorHora custos fixos por hora trabalhada (0 = não alocar).
     */
    fun analisar(
        dados: DadosCorrida,
        custoCombustivelPorKm: Double?,
        custoFixoPorHora: Double = 0.0,
    ): AnaliseCorrida {
        val viagem = dados.viagem
        val coleta = dados.coleta

        val soViagem = viagem?.let {
            cenario(TipoCenario.SO_VIAGEM, dados.valor, it.km, it.minutos, custoCombustivelPorKm, custoFixoPorHora)
        }

        val total = if (viagem != null && coleta != null) {
            val km = somar(coleta.km, viagem.km)
            val min = somar(coleta.minutos, viagem.minutos)
            cenario(TipoCenario.TOTAL, dados.valor, km, min, custoCombustivelPorKm, custoFixoPorHora)
        } else null

        return AnaliseCorrida(dados.valor, total, soViagem)
    }

    private fun somar(a: Double?, b: Double?): Double? = if (a != null && b != null) a + b else null

    private fun cenario(
        tipo: TipoCenario,
        valor: Double,
        km: Double?,
        minutos: Double?,
        custoKm: Double?,
        custoFixoHora: Double,
    ): Cenario {
        val kmValido = km?.takeIf { it > 0 }
        val minValido = minutos?.takeIf { it > 0 }
        val combustivel = if (kmValido != null && custoKm != null) kmValido * custoKm else null
        val resultado = combustivel?.let { valor - it }
        val fixo = if (minValido != null && custoFixoHora > 0) custoFixoHora * minValido / 60.0 else null
        val aposFixos = if (resultado != null && fixo != null) resultado - fixo else null
        return Cenario(
            tipo = tipo,
            km = km,
            minutos = minutos,
            reaisPorKm = kmValido?.let { valor / it },
            reaisPorHora = minValido?.let { valor / (it / 60.0) },
            custoCombustivel = combustivel,
            resultado = resultado,
            custoFixoAlocado = fixo,
            resultadoAposFixos = aposFixos,
        )
    }
}
