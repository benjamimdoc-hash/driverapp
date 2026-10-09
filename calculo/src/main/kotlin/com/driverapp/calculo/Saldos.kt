package com.driverapp.calculo

/**
 * Conciliação das leituras de saldo das plataformas — de forma INCREMENTAL e IDEMPOTENTE.
 *
 * A plataforma mostra o TOTAL ACUMULADO do período (dia na Uber, semana na 99).
 * Por isso uma leitura nova SUBSTITUI a anterior do mesmo período, nunca soma:
 *   1ª leitura: 5 corridas  → total 5
 *   2ª leitura: 8 corridas  → total 8 (3 novas), e NÃO 13.
 *   Ler de novo 8 corridas → nada muda.
 *
 * Quando algo não faz sentido (total diminuiu, salto enorme), a leitura fica PENDENTE de
 * confirmação do motorista em vez de mudar o resultado em silêncio.
 * Lógica pura, testada em SaldosTest.
 */
data class Instantaneo(
    val plataforma: String,
    /** true = total da semana (99); false = total do dia (Uber). */
    val semanal: Boolean,
    /** Início do período (meia-noite do dia, ou da segunda-feira), em ms. */
    val periodoInicio: Long,
    val valor: Double?,
    val corridas: Int?,
    val lidoEm: Long,
)

enum class TipoDecisao { BASE, ATUALIZA, SEM_MUDANCA, PENDENTE }

data class DecisaoSaldo(
    val tipo: TipoDecisao,
    /** O que será guardado (leitura completada com o que faltava da anterior). */
    val resultado: Instantaneo,
    val corridasNovas: Int,
    val valorNovo: Double,
    val motivo: String? = null,
)

object ConciliadorSaldo {
    /** Mais corridas novas do que isto entre duas leituras é suspeito. */
    const val SALTO_MAXIMO = 15
    private const val TOLERANCIA = 0.009

    fun conciliar(anterior: Instantaneo?, nova: Instantaneo): DecisaoSaldo {
        if (anterior == null || anterior.periodoInicio != nova.periodoInicio || anterior.semanal != nova.semanal) {
            return DecisaoSaldo(TipoDecisao.BASE, nova, nova.corridas ?: 0, nova.valor ?: 0.0)
        }
        // Campo que não apareceu nesta leitura (ex.: valor oculto) mantém o anterior.
        val completa = nova.copy(
            valor = nova.valor ?: anterior.valor,
            corridas = nova.corridas ?: anterior.corridas,
        )
        val dc = diferenca(anterior.corridas, completa.corridas)
        val dv = diferenca(anterior.valor, completa.valor)

        if ((dc ?: 0) < 0) return pendente(completa, "O número de corridas diminuiu (${anterior.corridas} → ${completa.corridas})")
        if ((dv ?: 0.0) < -TOLERANCIA) return pendente(completa, "O total diminuiu em relação à leitura anterior")
        if ((dc ?: 0) > SALTO_MAXIMO) return pendente(completa, "Muitas corridas novas de uma vez (+$dc)")
        if ((dc ?: 0) == 0 && kotlin.math.abs(dv ?: 0.0) <= TOLERANCIA) {
            return DecisaoSaldo(TipoDecisao.SEM_MUDANCA, completa, 0, 0.0)
        }
        return DecisaoSaldo(TipoDecisao.ATUALIZA, completa, dc ?: 0, (dv ?: 0.0).coerceAtLeast(0.0))
    }

    private fun pendente(i: Instantaneo, motivo: String) = DecisaoSaldo(TipoDecisao.PENDENTE, i, 0, 0.0, motivo)

    private fun diferenca(a: Int?, b: Int?): Int? = if (a != null && b != null) b - a else null
    private fun diferenca(a: Double?, b: Double?): Double? = if (a != null && b != null) b - a else null
}

/** Total de uma plataforma num intervalo, calculado a partir das leituras aceitas. */
data class TotalSaldo(
    val valor: Double?,
    val corridas: Int?,
    /** O total pode estar incompleto (ex.: corridas de hoje feitas antes da primeira leitura). */
    val parcial: Boolean,
)

object TotaisSaldo {
    private const val DIA_MS = 24 * 3_600_000L

    /**
     * @param aceitos leituras ACEITAS de UMA plataforma.
     * @param inicio/fim intervalo pedido (Hoje, Semana ou Mês, no horário local).
     * Regras:
     *  - Plataforma diária: soma o último total de cada dia do intervalo.
     *  - Plataforma semanal, intervalo de um dia: último total de hoje − último total antes de hoje
     *    na mesma semana. Sem leitura anterior, usa a primeira de hoje (fica "parcial").
     *  - Plataforma semanal, intervalo maior: soma o último total de cada semana que começa no intervalo.
     * Retorna null se não houver nenhuma leitura que se aplique.
     */
    fun total(aceitos: List<Instantaneo>, inicio: Long, fim: Long): TotalSaldo? {
        if (aceitos.isEmpty()) return null
        val semanal = aceitos.first().semanal
        return if (!semanal || fim - inicio > DIA_MS + 3_600_000L) somaUltimosPorPeriodo(aceitos, inicio, fim)
        else diaDeSemanal(aceitos, inicio, fim)
    }

    private fun somaUltimosPorPeriodo(aceitos: List<Instantaneo>, inicio: Long, fim: Long): TotalSaldo? {
        val ultimos = aceitos.filter { it.periodoInicio in inicio until fim }
            .groupBy { it.periodoInicio }
            .map { (_, lista) -> lista.maxBy { it.lidoEm } }
        if (ultimos.isEmpty()) return null
        return TotalSaldo(
            valor = ultimos.mapNotNull { it.valor }.takeIf { it.isNotEmpty() }?.sum(),
            corridas = ultimos.mapNotNull { it.corridas }.takeIf { it.isNotEmpty() }?.sum(),
            parcial = ultimos.any { it.valor == null || it.corridas == null },
        )
    }

    private fun diaDeSemanal(aceitos: List<Instantaneo>, inicio: Long, fim: Long): TotalSaldo? {
        val deHoje = aceitos.filter { it.lidoEm in inicio until fim }
        val ultimoHoje = deHoje.maxByOrNull { it.lidoEm } ?: return null
        val base = aceitos
            .filter { it.periodoInicio == ultimoHoje.periodoInicio && it.lidoEm < inicio }
            .maxByOrNull { it.lidoEm }
        val referencia = base ?: deHoje.minBy { it.lidoEm }
        return TotalSaldo(
            valor = sub(ultimoHoje.valor, referencia.valor),
            corridas = sub(ultimoHoje.corridas, referencia.corridas),
            parcial = base == null,
        )
    }

    private fun sub(a: Double?, b: Double?): Double? = if (a != null && b != null) (a - b).coerceAtLeast(0.0) else null
    private fun sub(a: Int?, b: Int?): Int? = if (a != null && b != null) (a - b).coerceAtLeast(0) else null
}

// ---------------------------------------------------------------- totais combinados

/** Corridas lançadas à mão de uma plataforma num período. */
data class ManualPlataforma(val plataforma: String, val quantidade: Int, val total: Double)

enum class FonteTotal { SALDO_DA_PLATAFORMA, LANCAMENTO_MANUAL }

data class LinhaPlataforma(
    val plataforma: String,
    val valor: Double,
    val corridas: Int,
    val fonte: FonteTotal,
    val parcial: Boolean,
)

data class TotalCombinado(val valor: Double, val corridas: Int, val linhas: List<LinhaPlataforma>) {
    val parcial: Boolean get() = linhas.any { it.parcial }
}

object TotaisCombinados {
    /**
     * Junta as plataformas sem contar nada duas vezes:
     *  - se há leitura de saldo aceita da plataforma no período, valem os números DELA
     *    (as corridas lançadas à mão dessa plataforma não são somadas por cima);
     *  - se não há, valem os lançamentos manuais.
     */
    fun combinar(saldosAceitos: List<Instantaneo>, manuais: List<ManualPlataforma>, inicio: Long, fim: Long): TotalCombinado {
        val porPlataforma = saldosAceitos.groupBy { it.plataforma }
        val nomes = (porPlataforma.keys + manuais.map { it.plataforma }).toSortedSet()
        val linhas = nomes.map { nome ->
            val manual = manuais.firstOrNull { it.plataforma == nome }
            val saldo = porPlataforma[nome]?.let { TotaisSaldo.total(it, inicio, fim) }
            if (saldo != null && (saldo.valor != null || saldo.corridas != null)) {
                LinhaPlataforma(
                    plataforma = nome,
                    valor = saldo.valor ?: manual?.total ?: 0.0,
                    corridas = saldo.corridas ?: manual?.quantidade ?: 0,
                    fonte = FonteTotal.SALDO_DA_PLATAFORMA,
                    parcial = saldo.parcial || saldo.valor == null || saldo.corridas == null,
                )
            } else {
                LinhaPlataforma(nome, manual?.total ?: 0.0, manual?.quantidade ?: 0, FonteTotal.LANCAMENTO_MANUAL, false)
            }
        }
        return TotalCombinado(linhas.sumOf { it.valor }, linhas.sumOf { it.corridas }, linhas)
    }
}
