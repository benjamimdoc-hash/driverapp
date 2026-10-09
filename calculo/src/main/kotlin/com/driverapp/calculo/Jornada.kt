package com.driverapp.calculo

/**
 * Estado de uma jornada de trabalho. Lógica pura (sem Android), testada em JornadaTest.
 *
 * O tempo é guardado com marcações de horário (e não com um cronômetro em memória):
 * assim ele continua correto mesmo se o app for fechado ou o celular reiniciar.
 *
 *  - Tempo produtivo = tempo acumulado até a última pausa + (agora − momento em que retomou).
 *  - Pausada: o tempo produtivo não conta.
 *  - Km na pausa: só contam se o motorista ativar essa opção (padrão: não contam).
 */
enum class EstadoJornada { ATIVA, PAUSADA, FINALIZADA }

data class Jornada(
    val inicioEm: Long,
    val estado: EstadoJornada,
    val msAcumulados: Long,
    /** Quando está ATIVA: momento em que começou/retomou o trecho atual. */
    val retomadaEm: Long?,
    val metros: Double,
    /** Parte dos metros que foi registrada durante pausas (só quando a opção está ativa). */
    val metrosNaPausa: Double,
    val finalizadaEm: Long?,
) {
    fun msProdutivos(agora: Long): Long {
        val trechoAtual = if (estado == EstadoJornada.ATIVA && retomadaEm != null) {
            (agora - retomadaEm).coerceAtLeast(0) // protege contra relógio alterado
        } else 0L
        return msAcumulados + trechoAtual
    }

    val km: Double get() = metros / 1000.0

    fun pausar(agora: Long): Jornada =
        if (estado != EstadoJornada.ATIVA) this
        else copy(estado = EstadoJornada.PAUSADA, msAcumulados = msProdutivos(agora), retomadaEm = null)

    fun retomar(agora: Long): Jornada =
        if (estado != EstadoJornada.PAUSADA) this
        else copy(estado = EstadoJornada.ATIVA, retomadaEm = agora)

    fun finalizar(agora: Long): Jornada =
        if (estado == EstadoJornada.FINALIZADA) this
        else pausar(agora).copy(estado = EstadoJornada.FINALIZADA, finalizadaEm = agora)

    /** Soma um deslocamento medido pelo GPS, respeitando o estado e a opção de km na pausa. */
    fun registrarDeslocamento(metrosNovos: Double, contarNaPausa: Boolean): Jornada {
        if (metrosNovos <= 0) return this
        return when (estado) {
            EstadoJornada.ATIVA -> copy(metros = metros + metrosNovos)
            EstadoJornada.PAUSADA ->
                if (contarNaPausa) copy(metros = metros + metrosNovos, metrosNaPausa = metrosNaPausa + metrosNovos)
                else this
            EstadoJornada.FINALIZADA -> this
        }
    }

    /** Se o GPS deve estar ligado neste estado. */
    fun precisaGps(contarNaPausa: Boolean): Boolean =
        estado == EstadoJornada.ATIVA || (estado == EstadoJornada.PAUSADA && contarNaPausa)

    companion object {
        fun iniciar(agora: Long) = Jornada(
            inicioEm = agora,
            estado = EstadoJornada.ATIVA,
            msAcumulados = 0,
            retomadaEm = agora,
            metros = 0.0,
            metrosNaPausa = 0.0,
            finalizadaEm = null,
        )
    }
}

/** Resumo financeiro de um período (jornada, dia, semana). Null = dado não configurado. */
data class ResumoFinanceiro(
    val faturamentoBruto: Double,
    val custoCombustivel: Double?,
    val outrosCustos: Double,
    /** Faturamento − combustível − outros custos. Estimado; não é lucro líquido contábil. */
    val resultadoEstimado: Double?,
)

object Financeiro {
    fun resumir(
        faturamentoBruto: Double,
        km: Double,
        horasProdutivas: Double,
        custoCombustivelPorKm: Double?,
        custoFixoPorHora: Double,
    ): ResumoFinanceiro {
        val combustivel = custoCombustivelPorKm?.let { km * it }
        val outros = (custoFixoPorHora * horasProdutivas).coerceAtLeast(0.0)
        return ResumoFinanceiro(
            faturamentoBruto = faturamentoBruto,
            custoCombustivel = combustivel,
            outrosCustos = outros,
            resultadoEstimado = combustivel?.let { faturamentoBruto - it - outros },
        )
    }
}
