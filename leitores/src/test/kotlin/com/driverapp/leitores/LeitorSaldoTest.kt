package com.driverapp.leitores

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val D = 0.001

class LeitorSaldoTest {
    /** Textos do print da tela inicial da Uber. */
    private val telaUber = listOf(
        "1-2 min", "+R$ 1", "R$ 55,23", "HOJE", "5", "viagens concluídas", "5 pontos", "VER RESUMO SEMANAL",
        "COMEÇAR", "Você está offline", "Avance para a categoria Ouro", "32%", "10%", "87/500 pontos", "Ver tempo ao volante",
    )

    /** Textos do print do Painel da 99. */
    private val tela99 = listOf(
        "R$92,69", "Painel", "Editar", "Mais", "R$7,60", "Valor da última corrida", "24.85%", "Taxa99 (esta semana)",
        "R$92,69", "Ganhos desta semana", "R$10,30", "/ (solicitação) semana", "9", "Solicitações",
        "Ver Central de ganhos", "Recompensa (1)", "R$2,8/km garantidos", "Começa amanhã 18:00",
    )

    @Test
    fun le_a_tela_da_uber() {
        val s = LeitorSaldo.ler(Plataforma.UBER, telaUber)!!
        assertEquals(PeriodoSaldo.DIA, s.periodo)
        assertEquals(55.23, s.valor!!, D)
        assertEquals(5, s.corridas)
    }

    @Test
    fun le_o_painel_da_99() {
        val s = LeitorSaldo.ler(Plataforma.NOVENTA_E_NOVE, tela99)!!
        assertEquals(PeriodoSaldo.SEMANA, s.periodo)
        assertEquals(92.69, s.valor!!, D)
        assertEquals(9, s.corridas)
        assertEquals(7.60, s.ultimaCorrida!!, D)
    }

    @Test
    fun valor_oculto_nao_impede_ler_as_corridas() {
        val oculto = telaUber.map { if (it == "R$ 55,23") "R$ ••••" else it }
        val s = LeitorSaldo.ler(Plataforma.UBER, oculto)!!
        assertNull(s.valor)
        assertEquals(5, s.corridas)
    }

    @Test
    fun tela_de_oferta_nao_e_tela_de_saldo() {
        assertNull(LeitorSaldo.ler(Plataforma.UBER, listOf("UberX", "R$ 15,30", "4 min (1.0 km)", "14 minutos (5.5 km)")))
        assertNull(LeitorSaldo.ler(Plataforma.NOVENTA_E_NOVE, listOf("Negocia", "R$6,86", "6 min (1,3 km)")))
    }

    @Test
    fun valor_abaixo_do_rotulo_tambem_e_lido() {
        val s = LeitorSaldo.ler(Plataforma.NOVENTA_E_NOVE, listOf("Ganhos desta semana", "R$ 120,00", "Solicitações", "12"))!!
        assertEquals(120.0, s.valor!!, D)
        assertEquals(12, s.corridas)
    }
}
