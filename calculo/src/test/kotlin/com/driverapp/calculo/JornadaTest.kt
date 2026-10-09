package com.driverapp.calculo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MIN = 60_000L

class JornadaTest {
    private val t0 = 1_000_000L

    @Test
    fun tempo_conta_so_enquanto_ativa() {
        var j = Jornada.iniciar(t0)
        assertEquals(30 * MIN, j.msProdutivos(t0 + 30 * MIN))
        j = j.pausar(t0 + 30 * MIN)
        assertEquals(30 * MIN, j.msProdutivos(t0 + 60 * MIN))     // pausada: não conta
        j = j.retomar(t0 + 60 * MIN)
        assertEquals(45 * MIN, j.msProdutivos(t0 + 75 * MIN))
        j = j.finalizar(t0 + 90 * MIN)
        assertEquals(EstadoJornada.FINALIZADA, j.estado)
        assertEquals(60 * MIN, j.msProdutivos(t0 + 500 * MIN))    // finalizada: congela
        assertEquals(t0 + 90 * MIN, j.finalizadaEm)
    }

    @Test
    fun km_na_pausa_so_conta_com_a_opcao_ligada() {
        val pausada = Jornada.iniciar(t0).registrarDeslocamento(1000.0, false).pausar(t0 + MIN)
        assertEquals(1000.0, pausada.registrarDeslocamento(500.0, contarNaPausa = false).metros, 0.01)
        val comOpcao = pausada.registrarDeslocamento(500.0, contarNaPausa = true)
        assertEquals(1500.0, comOpcao.metros, 0.01)
        assertEquals(500.0, comOpcao.metrosNaPausa, 0.01)
        assertTrue(!pausada.precisaGps(false))
        assertTrue(pausada.precisaGps(true))
    }

    @Test
    fun acoes_repetidas_nao_quebram_o_estado() {
        val j = Jornada.iniciar(t0).pausar(t0 + MIN)
        assertEquals(j, j.pausar(t0 + 2 * MIN))                     // pausar 2x
        val ativa = j.retomar(t0 + 3 * MIN)
        assertEquals(ativa, ativa.retomar(t0 + 4 * MIN))            // retomar 2x
        val fim = ativa.finalizar(t0 + 5 * MIN)
        assertEquals(fim, fim.finalizar(t0 + 6 * MIN))              // finalizar 2x
        assertEquals(fim, fim.registrarDeslocamento(100.0, true))   // finalizada não soma km
    }

    @Test
    fun relogio_voltando_nao_gera_tempo_negativo() {
        val j = Jornada.iniciar(t0)
        assertEquals(0L, j.msProdutivos(t0 - 10 * MIN))
        assertNull(Jornada.iniciar(t0).finalizadaEm)
    }
}

class FinanceiroTest {
    @Test
    fun resultado_desconta_combustivel_e_outros_custos() {
        val r = Financeiro.resumir(
            faturamentoBruto = 200.0, km = 100.0, horasProdutivas = 8.0,
            custoCombustivelPorKm = 0.52375, custoFixoPorHora = 15.5,
        )
        assertEquals(52.375, r.custoCombustivel!!, 0.001)
        assertEquals(124.0, r.outrosCustos, 0.001)
        assertEquals(23.625, r.resultadoEstimado!!, 0.001)
    }

    @Test
    fun sem_combustivel_configurado_resultado_fica_indisponivel() {
        val r = Financeiro.resumir(100.0, 10.0, 1.0, custoCombustivelPorKm = null, custoFixoPorHora = 0.0)
        assertNull(r.custoCombustivel)
        assertNull(r.resultadoEstimado)
        assertEquals(100.0, r.faturamentoBruto, 0.001)
    }
}

class FiltroGpsTest {
    private fun p(lat: Double, lon: Double, t: Long, prec: Float? = 5f) = PontoGps(lat, lon, prec, t)

    @Test
    fun distancia_conhecida() {
        // ~111,2 km por grau de latitude
        assertEquals(111_195.0, FiltroGps.distanciaMetros(p(0.0, 0.0, 0), p(1.0, 0.0, 0)), 50.0)
    }

    @Test
    fun primeiro_ponto_vira_referencia_sem_somar() {
        val a = FiltroGps.avaliar(null, p(-23.5, -46.6, 0))
        assertTrue(a.aceito)
        assertEquals(0.0, a.metros, 0.0)
    }

    @Test
    fun descarta_ponto_impreciso_tremida_e_salto() {
        val ref = p(-23.5, -46.6, 0)
        assertTrue(!FiltroGps.avaliar(ref, p(-23.5005, -46.6, 10_000, prec = 80f)).aceito) // imprecisão
        assertTrue(!FiltroGps.avaliar(ref, p(-23.50003, -46.6, 10_000)).aceito)            // ~3 m: tremida
        assertTrue(!FiltroGps.avaliar(ref, p(-23.6, -46.6, 10_000)).aceito)                // 11 km em 10 s
    }

    @Test
    fun aceita_movimento_normal() {
        val ref = p(-23.5, -46.6, 0)
        val a = FiltroGps.avaliar(ref, p(-23.5009, -46.6, 10_000)) // ~100 m em 10 s = 36 km/h
        assertTrue(a.aceito)
        assertEquals(100.0, a.metros, 2.0)
    }
}
