package com.driverapp.calculo

/**
 * Despesas fixas/periódicas (parcela, aluguel, IPVA, seguro, manutenção, lavagem...).
 *
 * Cada despesa é informada UMA vez, com a sua periodicidade. O sistema converte tudo
 * para uma mesma base — o custo POR DIA TRABALHADO — e só então soma.
 * Assim uma despesa nunca é contada duas vezes.
 *
 * Regras de conversão (documentadas e testadas):
 *  - POR_DIA_TRABALHADO: o próprio valor (ex.: estacionamento que só paga quando roda).
 *  - SEMANAL:  valor ÷ dias trabalhados por semana.
 *  - MENSAL:   valor ÷ (dias por semana × 52/12 semanas por mês).
 *  - ANUAL:    valor ÷ (dias por semana × 52 semanas).
 *
 * O combustível NÃO entra aqui: ele é custo variável (por km), ver [Combustivel].
 */
enum class Periodicidade { POR_DIA_TRABALHADO, SEMANAL, MENSAL, ANUAL }

enum class CategoriaDespesa {
    PARCELA_VEICULO, ALUGUEL_VEICULO, IPVA, SEGURO, MANUTENCAO, PNEUS,
    LAVAGEM, LICENCIAMENTO, PEDAGIO_ESTACIONAMENTO, OUTRA
}

data class Despesa(
    val id: String,
    val nome: String,
    val categoria: CategoriaDespesa,
    val valor: Double,
    val periodicidade: Periodicidade,
)

/** Como o motorista organiza a semana de trabalho. */
data class PlanoDeTrabalho(
    val diasPorSemana: Int,
    val horasPorDia: Double,
) {
    init {
        require(diasPorSemana in 1..7) { "Dias por semana deve ser de 1 a 7" }
        require(horasPorDia > 0 && horasPorDia <= 24) { "Horas por dia deve ser maior que 0 e até 24" }
    }

    val diasPorMes: Double get() = diasPorSemana * SEMANAS_POR_MES
    val horasPorSemana: Double get() = diasPorSemana * horasPorDia

    companion object {
        const val SEMANAS_POR_ANO = 52.0
        const val SEMANAS_POR_MES = SEMANAS_POR_ANO / 12.0
    }
}

object Conversao {
    /** Converte o valor de uma despesa para custo por dia trabalhado. */
    fun porDiaTrabalhado(valor: Double, periodicidade: Periodicidade, plano: PlanoDeTrabalho): Double =
        when (periodicidade) {
            Periodicidade.POR_DIA_TRABALHADO -> valor
            Periodicidade.SEMANAL -> valor / plano.diasPorSemana
            Periodicidade.MENSAL -> valor / plano.diasPorMes
            Periodicidade.ANUAL -> valor / (plano.diasPorSemana * PlanoDeTrabalho.SEMANAS_POR_ANO)
        }
}

/** Resumo dos custos fixos já convertidos em várias bases. */
data class ResumoCustosFixos(
    val porDiaTrabalhado: Double,
    val porHoraTrabalhada: Double,
    val porSemana: Double,
    val porMes: Double,
)

object CustosFixos {
    /**
     * Soma as despesas na mesma base. Despesas com o mesmo [Despesa.id] são consideradas
     * uma só (a última informada vale), evitando contagem dupla.
     */
    fun resumir(despesas: List<Despesa>, plano: PlanoDeTrabalho): ResumoCustosFixos {
        val unicas = despesas.associateBy { it.id }.values
        val porDia = unicas.sumOf { Conversao.porDiaTrabalhado(it.valor, it.periodicidade, plano) }
        return ResumoCustosFixos(
            porDiaTrabalhado = porDia,
            porHoraTrabalhada = porDia / plano.horasPorDia,
            porSemana = porDia * plano.diasPorSemana,
            porMes = porDia * plano.diasPorMes,
        )
    }
}
