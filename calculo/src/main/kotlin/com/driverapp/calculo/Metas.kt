package com.driverapp.calculo

/**
 * Metas de FATURAMENTO BRUTO (o que as plataformas pagam), não de lucro.
 * O lucro estimado é calculado à parte, descontando os custos.
 */
data class Metas(
    val metaSemanal: Double,
    val plano: PlanoDeTrabalho,
) {
    val metaDiaria: Double get() = metaSemanal / plano.diasPorSemana
    val metaPorHora: Double get() = metaDiaria / plano.horasPorDia

    /** Meta por km, se o motorista informar quantos km roda por dia. */
    fun metaPorKm(kmPorDia: Double): Double? = if (kmPorDia > 0) metaDiaria / kmPorDia else null

    /** Quanto falta para a meta da semana (nunca negativo). */
    fun faltaNaSemana(faturadoNaSemana: Double): Double = (metaSemanal - faturadoNaSemana).coerceAtLeast(0.0)

    /** Quanto precisa faturar por hora nas horas que restam. Null se não restam horas. */
    fun necessarioPorHora(faturadoNaSemana: Double, horasRestantes: Double): Double? =
        if (horasRestantes > 0) faltaNaSemana(faturadoNaSemana) / horasRestantes else null

    /** Progresso de 0.0 a 1.0 (limitado a 1.0 quando passa da meta). */
    fun progressoSemanal(faturadoNaSemana: Double): Double =
        if (metaSemanal > 0) (faturadoNaSemana / metaSemanal).coerceIn(0.0, 1.0) else 0.0

    fun progressoDiario(faturadoNoDia: Double): Double =
        if (metaDiaria > 0) (faturadoNoDia / metaDiaria).coerceIn(0.0, 1.0) else 0.0
}
