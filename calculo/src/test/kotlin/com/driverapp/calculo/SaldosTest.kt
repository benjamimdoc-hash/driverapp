package com.driverapp.calculo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val D = 0.001
private const val H = 3_600_000L
private const val DIA = 24 * H

class ConciliadorSaldoTest {
    private val hoje = 100 * DIA
    private fun uber(corridas: Int?, valor: Double?, lido: Long = hoje + 10 * H) =
        Instantaneo("UBER", semanal = false, periodoInicio = hoje, valor = valor, corridas = corridas, lidoEm = lido)

    @Test
    fun cinco_depois_oito_da_oito_e_nao_treze() {
        val primeira = ConciliadorSaldo.conciliar(null, uber(5, 55.23))
        assertEquals(TipoDecisao.BASE, primeira.tipo)
        val segunda = ConciliadorSaldo.conciliar(primeira.resultado, uber(8, 92.10, hoje + 12 * H))
        assertEquals(TipoDecisao.ATUALIZA, segunda.tipo)
        assertEquals(8, segunda.resultado.corridas)       // substitui, não soma
        assertEquals(3, segunda.corridasNovas)
        assertEquals(36.87, segunda.valorNovo, D)
    }

    @Test
    fun ler_a_mesma_tela_de_novo_nao_muda_nada() {
        val base = uber(5, 55.23)
        val d = ConciliadorSaldo.conciliar(base, uber(5, 55.23, hoje + 11 * H))
        assertEquals(TipoDecisao.SEM_MUDANCA, d.tipo)
        assertEquals(0, d.corridasNovas)
    }

    @Test
    fun total_menor_fica_pendente_de_confirmacao() {
        val d = ConciliadorSaldo.conciliar(uber(8, 92.10), uber(5, 55.23))
        assertEquals(TipoDecisao.PENDENTE, d.tipo)
        assertTrue(d.motivo!!.contains("diminuiu"))
    }

    @Test
    fun salto_muito_grande_fica_pendente() {
        assertEquals(TipoDecisao.PENDENTE, ConciliadorSaldo.conciliar(uber(2, 20.0), uber(40, 400.0)).tipo)
    }

    @Test
    fun valor_oculto_mantem_o_anterior() {
        val d = ConciliadorSaldo.conciliar(uber(5, 55.23), uber(6, null))
        assertEquals(TipoDecisao.ATUALIZA, d.tipo)
        assertEquals(55.23, d.resultado.valor!!, D)
        assertEquals(6, d.resultado.corridas)
    }

    @Test
    fun novo_dia_comeca_nova_base() {
        val amanha = Instantaneo("UBER", false, hoje + DIA, 10.0, 1, hoje + DIA + H)
        assertEquals(TipoDecisao.BASE, ConciliadorSaldo.conciliar(uber(8, 92.10), amanha).tipo)
    }
}

class TotaisSaldoTest {
    private val segunda = 100 * DIA
    private fun s99(corridas: Int, valor: Double, lido: Long) = Instantaneo("NOVENTA_E_NOVE", true, segunda, valor, corridas, lido)

    @Test
    fun plataforma_diaria_soma_o_ultimo_de_cada_dia() {
        val leituras = listOf(
            Instantaneo("UBER", false, segunda, 30.0, 3, segunda + 10 * H),
            Instantaneo("UBER", false, segunda, 55.0, 5, segunda + 20 * H),     // último de segunda
            Instantaneo("UBER", false, segunda + DIA, 40.0, 4, segunda + DIA + 15 * H),
        )
        val semana = TotaisSaldo.total(leituras, segunda, segunda + 7 * DIA)!!
        assertEquals(95.0, semana.valor!!, D)
        assertEquals(9, semana.corridas)
        val hoje = TotaisSaldo.total(leituras, segunda + DIA, segunda + 2 * DIA)!!
        assertEquals(40.0, hoje.valor!!, D)
    }

    @Test
    fun semanal_no_dia_usa_a_diferenca_desde_ontem() {
        val leituras = listOf(
            s99(6, 60.0, segunda + 22 * H),                  // segunda à noite
            s99(9, 92.69, segunda + DIA + 18 * H),           // terça
        )
        val terca = TotaisSaldo.total(leituras, segunda + DIA, segunda + 2 * DIA)!!
        assertEquals(32.69, terca.valor!!, D)
        assertEquals(3, terca.corridas)
        assertTrue(!terca.parcial)
    }

    @Test
    fun semanal_sem_leitura_anterior_fica_parcial() {
        val leituras = listOf(s99(7, 70.0, segunda + DIA + 9 * H), s99(9, 92.69, segunda + DIA + 18 * H))
        val terca = TotaisSaldo.total(leituras, segunda + DIA, segunda + 2 * DIA)!!
        assertEquals(2, terca.corridas)
        assertTrue(terca.parcial)
    }

    @Test
    fun semanal_na_semana_usa_o_ultimo_total() {
        val leituras = listOf(s99(6, 60.0, segunda + 22 * H), s99(9, 92.69, segunda + DIA + 18 * H))
        val semana = TotaisSaldo.total(leituras, segunda, segunda + 7 * DIA)!!
        assertEquals(92.69, semana.valor!!, D)
        assertEquals(9, semana.corridas)
    }

    @Test
    fun sem_leituras_retorna_null() {
        assertNull(TotaisSaldo.total(emptyList(), 0, DIA))
    }
}

class TotaisCombinadosTest {
    private val hoje = 100 * DIA

    @Test
    fun saldo_da_plataforma_substitui_os_lancamentos_manuais_da_mesma_plataforma() {
        val saldos = listOf(Instantaneo("UBER", false, hoje, 55.23, 5, hoje + 20 * H))
        val manuais = listOf(
            ManualPlataforma("UBER", 2, 30.0),            // ignorado: a Uber já informou o total
            ManualPlataforma("NOVENTA_E_NOVE", 1, 6.86),  // sem leitura da 99: vale o manual
        )
        val t = TotaisCombinados.combinar(saldos, manuais, hoje, hoje + DIA)
        assertEquals(62.09, t.valor, D)
        assertEquals(6, t.corridas)
        assertEquals(FonteTotal.SALDO_DA_PLATAFORMA, t.linhas.first { it.plataforma == "UBER" }.fonte)
        assertEquals(FonteTotal.LANCAMENTO_MANUAL, t.linhas.first { it.plataforma == "NOVENTA_E_NOVE" }.fonte)
    }

    @Test
    fun sem_nada_da_zero() {
        val t = TotaisCombinados.combinar(emptyList(), emptyList(), hoje, hoje + DIA)
        assertEquals(0.0, t.valor, D)
        assertEquals(0, t.corridas)
    }
}
