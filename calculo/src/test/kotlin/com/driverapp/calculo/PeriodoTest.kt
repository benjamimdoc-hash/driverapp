package com.driverapp.calculo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

private const val D = 0.001

class ValorUnicoTest {
    private val plano = PlanoDeTrabalho(diasPorSemana = 6, horasPorDia = 10.0)

    @Test
    fun valor_unico_eh_distribuido_pelo_prazo() {
        // R$ 1.200 em 12 meses = R$ 100/mês = mesmo custo de uma despesa mensal de R$ 100
        val unico = Conversao.porDiaTrabalhado(1200.0, Periodicidade.VALOR_UNICO, plano, prazoMeses = 12)
        val mensal = Conversao.porDiaTrabalhado(100.0, Periodicidade.MENSAL, plano)
        assertEquals(mensal, unico, D)
    }

    @Test
    fun valor_unico_sem_prazo_usa_12_meses() {
        val semPrazo = Conversao.porDiaTrabalhado(1200.0, Periodicidade.VALOR_UNICO, plano)
        val comPrazo = Conversao.porDiaTrabalhado(1200.0, Periodicidade.VALOR_UNICO, plano, 12)
        assertEquals(comPrazo, semPrazo, D)
    }

    @Test
    fun custo_fixo_por_km_usa_km_medio_por_dia() {
        val r = CustosFixos.resumir(
            listOf(Despesa("a", "Aluguel", CategoriaDespesa.ALUGUEL_VEICULO, 600.0, Periodicidade.SEMANAL)),
            plano,
        )
        assertEquals(0.5, r.porKm(200.0)!!, D) // R$ 100/dia ÷ 200 km
        assertNull(r.porKm(null))
    }
}

class IntervalosTest {
    private val sp = ZoneId.of("America/Sao_Paulo")
    private val quinta = LocalDate.of(2026, 10, 8)

    @Test
    fun hoje_semana_e_mes_no_horario_local() {
        val hoje = Periodos.intervalo(Periodo.HOJE, quinta, sp)
        assertEquals(24 * 3_600_000L, hoje.fimMs - hoje.inicioMs)
        assertEquals(LocalDate.of(2026, 10, 8).atStartOfDay(sp).toInstant().toEpochMilli(), hoje.inicioMs)

        val semana = Periodos.intervalo(Periodo.SEMANA, quinta, sp)
        assertEquals(LocalDate.of(2026, 10, 5).atStartOfDay(sp).toInstant().toEpochMilli(), semana.inicioMs) // segunda
        assertEquals(LocalDate.of(2026, 10, 12).atStartOfDay(sp).toInstant().toEpochMilli(), semana.fimMs)

        val mes = Periodos.intervalo(Periodo.MES, quinta, sp)
        assertEquals(LocalDate.of(2026, 10, 1).atStartOfDay(sp).toInstant().toEpochMilli(), mes.inicioMs)
        assertEquals(LocalDate.of(2026, 11, 1).atStartOfDay(sp).toInstant().toEpochMilli(), mes.fimMs)
        assertTrue(hoje.inicioMs in semana)
        assertTrue(semana.fimMs !in semana)
    }
}

class ResumoPeriodoTest {
    @Test
    fun calcula_todas_as_metricas() {
        val r = ResumoDePeriodo.calcular(
            DadosPeriodo(
                faturamentoBruto = 300.0, corridas = 12, msOnline = 8 * 3_600_000L, km = 150.0,
                custoCombustivel = 78.5625, custosFixosAlocados = 124.0,
            )
        )
        assertEquals(202.5625, r.despesasTotais!!, D)
        assertEquals(97.4375, r.resultadoEstimado!!, D)
        assertEquals(37.5, r.ganhoPorHora!!, D)
        assertEquals(12.18, r.resultadoPorHora!!, 0.01)
        assertEquals(2.0, r.ganhoPorKm!!, D)
        assertEquals(0.6496, r.resultadoPorKm!!, D)
        assertEquals(1.3504, r.custoPorKm!!, D)
        assertEquals(25.0, r.ticketMedio!!, D)
        assertTrue(!r.incompleto)
    }

    @Test
    fun sem_dados_nao_divide_por_zero_e_fica_neutro() {
        val r = ResumoDePeriodo.calcular(DadosPeriodo(0.0, 0, 0, 0.0, 0.0, 0.0))
        assertNull(r.ganhoPorHora)
        assertNull(r.ganhoPorKm)
        assertNull(r.ticketMedio)
        assertNull(r.custoPorKm)
        assertEquals(0.0, r.resultadoEstimado!!, D)
    }

    @Test
    fun sem_combustivel_configurado_marca_incompleto() {
        val r = ResumoDePeriodo.calcular(DadosPeriodo(100.0, 4, 3_600_000L, 40.0, null, 10.0))
        assertTrue(r.incompleto)
        assertNull(r.resultadoEstimado)
        assertEquals(2.5, r.ganhoPorKm!!, D) // o bruto continua disponível
    }
}

class RitmoTest {
    private val segASab = setOf(1, 2, 3, 4, 5, 6)

    @Test
    fun conta_dias_de_trabalho_restantes_incluindo_hoje() {
        assertEquals(3, Ritmo.diasRestantes(segASab, LocalDate.of(2026, 10, 8)))  // qui, sex, sáb
        assertEquals(0, Ritmo.diasRestantes(segASab, LocalDate.of(2026, 10, 11))) // domingo de folga
        assertEquals(1, Ritmo.diasRestantes(setOf(7), LocalDate.of(2026, 10, 11)))
    }

    @Test
    fun ritmo_por_dia() {
        assertEquals(300.0, Ritmo.porDia(900.0, 3)!!, D)
        assertNull(Ritmo.porDia(900.0, 0))
    }
}

class MascaraMoedaTest {
    @Test
    fun digitos_viram_reais_e_centavos() {
        assertEquals("5,70", MascaraMoeda.formatar("570"))
        assertEquals(5.70, MascaraMoeda.valor("570")!!, D)
        assertEquals("37,00", MascaraMoeda.formatar("3700"))
        assertEquals("1.234,56", MascaraMoeda.formatar("123456"))
        assertEquals("0,07", MascaraMoeda.formatar("007"))
        assertEquals("", MascaraMoeda.formatar(""))
        assertNull(MascaraMoeda.valor("abc"))
    }

    @Test
    fun ignora_simbolos_e_volta_do_valor_salvo() {
        assertEquals("5,70", MascaraMoeda.formatar("R$ 5,70"))
        assertEquals("570", MascaraMoeda.digitosDe(5.7))
        assertEquals("", MascaraMoeda.digitosDe(null))
        assertEquals(9, MascaraMoeda.limpar("12345678901234").length)
    }
}

class LimitesTest {
    @Test
    fun limites_do_motorista_definem_as_cores() {
        val c = CriteriosClassificacao.deLimites(kmMinimo = 1.8, kmBom = 2.5, horaMinimo = 30.0, horaBom = 50.0)
        assertEquals(Cor.VERMELHO, c.porKm.nivel(1.5).cor)
        assertEquals(Cor.AMARELO, c.porKm.nivel(2.0).cor)
        assertEquals(Cor.VERDE, c.porKm.nivel(2.6).cor)
        assertEquals(Cor.VERDE, c.porHora.nivel(51.0).cor)
    }
}
