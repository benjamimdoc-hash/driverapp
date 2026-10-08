package com.driverapp.leitores

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val D = 0.001

class NumerosTest {
    @Test
    fun formatos_das_telas() {
        assertEquals(15.30, Numeros.parse("15,30")!!, D)
        assertEquals(1.0, Numeros.parse("1.0")!!, D)
        assertEquals(1.3, Numeros.parse("1,3")!!, D)
        assertEquals(1234.56, Numeros.parse("1.234,56")!!, D)
        assertEquals(1234.56, Numeros.parse("1,234.56")!!, D)
        assertNull(Numeros.parse("abc"))
    }
}

class LeitorUberTest {
    /** Textos do print da oferta da Uber, na ordem em que aparecem na tela. */
    private val telaUber = listOf(
        "UberX", "Exclusivo",
        "R$ 15,30",
        "R$2,35/km aprox.",
        "4,94 (1332)", "Verificado",
        "4 min (1.0 km)",
        "Rua Santa Luzia da Boa Visão, Itaim Paulista, São Paulo",
        "14 minutos (5.5 km)",
        "Rua Rosa Ribas, 316, Poá, Poá",
        "Aceitar",
    )

    @Test
    fun le_a_oferta_do_print() {
        val o = LeitorUber.ler(telaUber)!!
        assertEquals(Plataforma.UBER, o.plataforma)
        assertEquals(15.30, o.valor!!, D)
        assertEquals(TrechoLido(4.0, 1.0), o.coleta)
        assertEquals(TrechoLido(14.0, 5.5), o.viagem)
        assertEquals("UberX", o.categoria)
        assertEquals(4.94, o.notaPassageiro!!, D)
        assertEquals(1332, o.corridasPassageiro)
        assertEquals(2.35, o.reaisPorKmInformado!!, D)
        assertTrue(o.observacoes.containsAll(listOf("Exclusivo", "Verificado")))
        assertTrue(o.completa)
    }

    @Test
    fun ignora_selo_de_dinamica_do_mapa() {
        val o = LeitorUber.ler(listOf("+R$ 1") + telaUber)!!
        assertEquals(15.30, o.valor!!, D)
    }

    @Test
    fun aceita_textos_juntos_numa_linha_so() {
        val o = LeitorUber.ler(listOf(telaUber.joinToString(" ")))!!
        assertEquals(15.30, o.valor!!, D)
        assertEquals(TrechoLido(14.0, 5.5), o.viagem)
    }

    @Test
    fun corrida_longa_com_horas_e_metros() {
        val o = LeitorUber.ler(listOf("Black", "R$ 120,00", "2 min (800 m)", "1 h 5 min (40,2 km)"))!!
        assertEquals(TrechoLido(2.0, 0.8), o.coleta)
        assertEquals(TrechoLido(65.0, 40.2), o.viagem)
        assertEquals("Black", o.categoria)
    }

    @Test
    fun tela_que_nao_e_oferta_retorna_null() {
        assertNull(LeitorUber.ler(listOf("R$ 55,23", "HOJE", "5 viagens concluídas", "Você está offline")))
    }
}

class Leitor99Test {
    /** Textos do print da oferta da 99. */
    private val tela99 = listOf(
        "Negocia", "Pgto. no app",
        "R$6,86",
        "R$2,56/km",
        "4,95", "359 corridas",
        "Perfil Premium",
        "6 min (1,3 km)",
        "Rua Tibúrcio de Sousa, 1391, Itaim Paulista",
        "5 min (1,4 km)",
        "Rua João Esteves Robalo, 21, Jardim Míriam",
        "Aceitar por R$6,86",
    )

    @Test
    fun le_a_oferta_do_print() {
        val o = Leitor99.ler(tela99)!!
        assertEquals(Plataforma.NOVENTA_E_NOVE, o.plataforma)
        assertEquals(6.86, o.valor!!, D)
        assertEquals(TrechoLido(6.0, 1.3), o.coleta)
        assertEquals(TrechoLido(5.0, 1.4), o.viagem)
        assertEquals("Negocia", o.categoria)
        assertEquals(4.95, o.notaPassageiro!!, D)
        assertEquals(359, o.corridasPassageiro)
        assertEquals(2.56, o.reaisPorKmInformado!!, D)
        assertTrue(o.observacoes.containsAll(listOf("Pgto. no app", "Perfil Premium")))
    }

    @Test
    fun nota_e_corridas_na_mesma_linha() {
        val tela = tela99.toMutableList().apply {
            remove("4,95"); remove("359 corridas"); add(4, "4,95 · 359 corridas")
        }
        val o = Leitor99.ler(tela)!!
        assertEquals(4.95, o.notaPassageiro!!, D)
        assertEquals(359, o.corridasPassageiro)
    }

    @Test
    fun sem_nota_visivel_fica_indisponivel() {
        val o = Leitor99.ler(listOf("Pop", "R$ 12,00", "3 min (1,0 km)", "10 min (4,0 km)"))!!
        assertNull(o.notaPassageiro)
        assertNotNull(o.viagem)
        assertEquals("Pop", o.categoria)
    }
}
