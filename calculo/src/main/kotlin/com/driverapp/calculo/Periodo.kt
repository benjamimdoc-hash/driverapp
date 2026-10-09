package com.driverapp.calculo

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Indicadores de um período (Hoje, Semana, Mês) para a dashboard.
 *
 * Definições (as mesmas em todo o app):
 *  - Faturamento bruto: soma das corridas registradas no período.
 *  - Custos variáveis: combustível dos km rodados (km × custo por km).
 *  - Custos fixos alocados: custo fixo por hora × horas online.
 *  - Resultado operacional estimado: bruto − variáveis − fixos alocados.
 *  - Horas online: tempo produtivo das jornadas (pausas não contam).
 *  - Ticket médio: bruto ÷ número de corridas.
 * Qualquer indicador sem base suficiente (ex.: zero km, zero corridas) fica null — "sem dados",
 * que é diferente de desempenho ruim.
 */
enum class Periodo { HOJE, SEMANA, MES }

/** Intervalo [inicioMs, fimMs) no horário local. Semana começa na segunda-feira. */
data class Intervalo(val inicioMs: Long, val fimMs: Long) {
    operator fun contains(ms: Long): Boolean = ms >= inicioMs && ms < fimMs
}

object Periodos {
    fun intervalo(periodo: Periodo, hoje: LocalDate, zona: ZoneId): Intervalo {
        val inicio = when (periodo) {
            Periodo.HOJE -> hoje
            Periodo.SEMANA -> hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            Periodo.MES -> hoje.withDayOfMonth(1)
        }
        val fim = when (periodo) {
            Periodo.HOJE -> inicio.plusDays(1)
            Periodo.SEMANA -> inicio.plusWeeks(1)
            Periodo.MES -> inicio.plusMonths(1)
        }
        return Intervalo(
            inicio.atStartOfDay(zona).toInstant().toEpochMilli(),
            fim.atStartOfDay(zona).toInstant().toEpochMilli(),
        )
    }
}

data class DadosPeriodo(
    val faturamentoBruto: Double,
    val corridas: Int,
    val msOnline: Long,
    val km: Double,
    /** Null quando o combustível não está configurado. */
    val custoCombustivel: Double?,
    val custosFixosAlocados: Double,
)

data class ResumoPeriodo(
    val faturamentoBruto: Double,
    val corridas: Int,
    val horasOnline: Double,
    val km: Double,
    val custosVariaveis: Double?,
    val custosFixosAlocados: Double,
    val despesasTotais: Double?,
    val resultadoEstimado: Double?,
    val ganhoPorHora: Double?,
    val resultadoPorHora: Double?,
    val ganhoPorKm: Double?,
    val resultadoPorKm: Double?,
    val custoPorKm: Double?,
    val ticketMedio: Double?,
    /** Faltam dados para alguma parte do cálculo (ex.: combustível não configurado). */
    val incompleto: Boolean,
)

object ResumoDePeriodo {
    /** Abaixo disso não calculamos "por km" / "por hora" (evita números enganosos). */
    const val KM_MINIMO = 0.5
    const val MS_MINIMO = 5 * 60_000L

    fun calcular(d: DadosPeriodo): ResumoPeriodo {
        val horas = d.msOnline / 3_600_000.0
        val temKm = d.km >= KM_MINIMO
        val temTempo = d.msOnline >= MS_MINIMO
        val despesas = d.custoCombustivel?.let { it + d.custosFixosAlocados }
        val resultado = despesas?.let { d.faturamentoBruto - it }
        return ResumoPeriodo(
            faturamentoBruto = d.faturamentoBruto,
            corridas = d.corridas,
            horasOnline = horas,
            km = d.km,
            custosVariaveis = d.custoCombustivel,
            custosFixosAlocados = d.custosFixosAlocados,
            despesasTotais = despesas,
            resultadoEstimado = resultado,
            ganhoPorHora = if (temTempo) d.faturamentoBruto / horas else null,
            resultadoPorHora = if (temTempo && resultado != null) resultado / horas else null,
            ganhoPorKm = if (temKm) d.faturamentoBruto / d.km else null,
            resultadoPorKm = if (temKm && resultado != null) resultado / d.km else null,
            custoPorKm = if (temKm && despesas != null) despesas / d.km else null,
            ticketMedio = if (d.corridas > 0) d.faturamentoBruto / d.corridas else null,
            incompleto = d.custoCombustivel == null,
        )
    }
}

/** Ritmo para bater a meta semanal. */
object Ritmo {
    /**
     * Dias de trabalho que ainda restam na semana (segunda a domingo), contando hoje
     * se hoje for dia de trabalho. [diasTrabalho] usa 1 = segunda ... 7 = domingo.
     */
    fun diasRestantes(diasTrabalho: Set<Int>, hoje: LocalDate): Int {
        val hojeNum = hoje.dayOfWeek.value
        return (hojeNum..7).count { it in diasTrabalho }
    }

    /** Quanto faturar por dia de trabalho restante. Null se não restam dias. */
    fun porDia(falta: Double, diasRestantes: Int): Double? =
        if (diasRestantes > 0) falta.coerceAtLeast(0.0) / diasRestantes else null
}
