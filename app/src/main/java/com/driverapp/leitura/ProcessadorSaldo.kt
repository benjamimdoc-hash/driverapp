package com.driverapp.leitura

import android.content.Context
import com.driverapp.calculo.ConciliadorSaldo
import com.driverapp.calculo.Instantaneo
import com.driverapp.calculo.TipoDecisao
import com.driverapp.dados.SaldoEntity
import com.driverapp.dados.paraInstantaneo
import com.driverapp.leitores.LeitorSaldo
import com.driverapp.leitores.LeituraSaldo
import com.driverapp.leitores.PeriodoSaldo
import com.driverapp.leitores.Plataforma
import com.driverapp.repositorio
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Lê a tela de saldo/ganhos e concilia com a última leitura do mesmo período:
 *  - igual à anterior → nada muda (idempotente);
 *  - maior → substitui (incremental, nunca soma 5 + 8);
 *  - incoerente → fica PENDENTE para o motorista confirmar.
 */
class ProcessadorSaldo(private val context: Context) {

    private var ultimaAssinatura: String? = null

    suspend fun processar(plataforma: Plataforma, linhas: List<String>, agoraMs: Long): TipoDecisao? {
        val leitura = LeitorSaldo.ler(plataforma, linhas) ?: return null
        // A tela atualiza várias vezes por segundo: ignora a mesma leitura repetida.
        val assinatura = "${leitura.plataforma}|${leitura.valor}|${leitura.corridas}"
        if (assinatura == ultimaAssinatura) return TipoDecisao.SEM_MUDANCA
        ultimaAssinatura = assinatura
        return registrar(context, leitura, agoraMs)
    }

    companion object {
        /** Início do período da leitura no horário local: meia-noite (dia) ou segunda-feira (semana). */
        fun inicioDoPeriodo(periodo: PeriodoSaldo, agoraMs: Long, zona: ZoneId = ZoneId.systemDefault()): Long {
            val dia = Instant.ofEpochMilli(agoraMs).atZone(zona).toLocalDate()
            val inicio = if (periodo == PeriodoSaldo.SEMANA) dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) else dia
            return inicio.atStartOfDay(zona).toInstant().toEpochMilli()
        }

        suspend fun registrar(context: Context, leitura: LeituraSaldo, agoraMs: Long): TipoDecisao {
            val repo = context.repositorio
            val semanal = leitura.periodo == PeriodoSaldo.SEMANA
            val inicio = inicioDoPeriodo(leitura.periodo, agoraMs)
            val anterior = repo.ultimoSaldoAceito(leitura.plataforma.name, semanal, inicio)
            val nova = Instantaneo(leitura.plataforma.name, semanal, inicio, leitura.valor, leitura.corridas, agoraMs)
            val decisao = ConciliadorSaldo.conciliar(anterior?.paraInstantaneo(), nova)

            when (decisao.tipo) {
                TipoDecisao.SEM_MUDANCA -> Unit
                TipoDecisao.PENDENTE -> {
                    // Não repete a mesma pendência a cada atualização da tela.
                    val ultima = repo.ultimoSaldoPendente(leitura.plataforma.name)
                    val repetida = ultima != null && ultima.valor == decisao.resultado.valor &&
                        ultima.corridas == decisao.resultado.corridas && ultima.periodoInicio == inicio
                    if (!repetida) repo.inserirSaldo(entidade(decisao.resultado, leitura, "PENDENTE", "LEITURA", decisao.motivo))
                }
                else -> repo.inserirSaldo(entidade(decisao.resultado, leitura, "ACEITO", "LEITURA", null))
            }
            return decisao.tipo
        }

        /** Correção manual do motorista: vira o total aceito do período (fica no histórico). */
        suspend fun corrigir(context: Context, plataforma: Plataforma, valor: Double?, corridas: Int?, agoraMs: Long) {
            val periodo = periodoDe(plataforma)
            val inicio = inicioDoPeriodo(periodo, agoraMs)
            context.repositorio.inserirSaldo(
                SaldoEntity(
                    plataforma = plataforma.name, semanal = periodo == PeriodoSaldo.SEMANA, periodoInicio = inicio,
                    valor = valor, corridas = corridas, ultimaCorrida = null, lidoEm = agoraMs,
                    estado = "ACEITO", origem = "MANUAL", motivo = "Correção manual",
                )
            )
        }

        fun periodoDe(p: Plataforma): PeriodoSaldo = if (p == Plataforma.NOVENTA_E_NOVE) PeriodoSaldo.SEMANA else PeriodoSaldo.DIA

        private fun entidade(i: Instantaneo, l: LeituraSaldo, estado: String, origem: String, motivo: String?) = SaldoEntity(
            plataforma = i.plataforma, semanal = i.semanal, periodoInicio = i.periodoInicio,
            valor = i.valor, corridas = i.corridas, ultimaCorrida = l.ultimaCorrida, lidoEm = i.lidoEm,
            estado = estado, origem = origem, motivo = motivo,
        )
    }
}
