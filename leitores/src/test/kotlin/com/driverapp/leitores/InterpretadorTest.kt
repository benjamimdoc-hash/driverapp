package com.driverapp.leitores

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val D = 0.001

/** Textos dos prints de referência (mesma ordem da tela). */
object Prints {
    val UBER = listOf(
        "UberX", "Exclusivo", "R$ 15,30", "R$2,35/km aprox.", "4,94 (1332)", "Verificado",
        "4 min (1.0 km)", "Rua Santa Luzia da Boa Visão, Itaim Paulista, São Paulo",
        "14 minutos (5.5 km)", "Rua Rosa Ribas, 316, Poá, Poá", "Aceitar",
    )
    val NOVENTA_E_NOVE = listOf(
        "Negocia", "Pgto. no app", "R$6,86", "R$2,56/km", "4,95", "359 corridas", "Perfil Premium",
        "6 min (1,3 km)", "Rua Tibúrcio de Sousa, 1391, Itaim Paulista",
        "5 min (1,4 km)", "Rua João Esteves Robalo, 21, Jardim Míriam", "Aceitar por R$6,86",
    )
}

class InterpretadorTest {
    @Test
    fun prints_reais_tem_confianca_alta() {
        val uber = Interpretador.validar(LeitorUber.ler(Prints.UBER)!!)
        assertEquals(Confianca.ALTA, uber.confianca)
        assertTrue(uber.utilizavel)
        assertTrue(uber.ausentes.isEmpty())

        val n99 = Interpretador.validar(Leitor99.ler(Prints.NOVENTA_E_NOVE)!!)
        assertEquals(Confianca.ALTA, n99.confianca)
        assertEquals(4.95, n99.notaPassageiro!!, D)
    }

    @Test
    fun campos_sao_independentes_sem_valor_o_resto_continua() {
        val semValor = Prints.UBER.filterNot { it == "R$ 15,30" || it.startsWith("R$2,35") }
        val o = LeitorUber.ler(semValor)!!
        assertNull(o.valor)
        val v = Interpretador.validar(o)
        assertTrue("valor" in v.ausentes)
        assertEquals(TrechoLido(14.0, 5.5), v.viagem)
        assertEquals("UberX", v.categoria)
        assertTrue(!v.utilizavel)
    }

    @Test
    fun um_trecho_so_gera_baixa_confianca() {
        val v = Interpretador.validar(LeitorUber.ler(listOf("UberX", "R$ 20,00", "15 min (6.0 km)"))!!)
        assertEquals(Confianca.BAIXA, v.confianca)
        assertTrue("coleta" in v.ausentes)
        assertNotNull(v.viagem)
    }

    @Test
    fun valor_absurdo_e_descartado_sem_inventar() {
        val v = Interpretador.validar(LeitorUber.ler(listOf("R$ 0,50", "2 min (0.5 km)", "10 min (4.0 km)"))!!)
        assertNull(v.valor)
        assertEquals(Confianca.BAIXA, v.confianca)
    }

    @Test
    fun tempo_e_distancia_incoerentes_baixam_a_confianca() {
        val v = Interpretador.validar(LeitorUber.ler(listOf("R$ 30,00", "2 min (1.0 km)", "5 min (60.0 km)"))!!)
        assertEquals(Confianca.BAIXA, v.confianca)
        assertNotNull(v.viagem) // o dado é mantido; só avisamos
    }

    @Test
    fun reais_por_km_divergente_da_plataforma_baixa_a_confianca() {
        val tela = Prints.UBER.map { if (it.startsWith("R$2,35")) "R$9,99/km" else it }
        assertEquals(Confianca.BAIXA, Interpretador.validar(LeitorUber.ler(tela)!!).confianca)
    }
}

class DeduplicadorTest {
    private fun leitura(linhas: List<String>) = Interpretador.validar(LeitorUber.ler(linhas)!!)

    @Test
    fun mesma_oferta_repetida_nao_conta_de_novo() {
        val d = Deduplicador()
        val l = leitura(Prints.UBER)
        assertEquals(TipoLeitura.NOVA, d.classificar(l, 0))
        assertEquals(TipoLeitura.REPETIDA, d.classificar(l, 500))
        assertEquals(TipoLeitura.REPETIDA, d.classificar(l, 30_000))
    }

    @Test
    fun leitura_parcial_seguida_da_completa_vira_atualizacao() {
        val d = Deduplicador()
        val parcial = leitura(listOf("R$ 15,30", "14 minutos (5.5 km)"))
        assertEquals(TipoLeitura.NOVA, d.classificar(parcial, 0))
        assertEquals(TipoLeitura.ATUALIZACAO, d.classificar(leitura(Prints.UBER), 800))
    }

    @Test
    fun ofertas_diferentes_sao_novas() {
        val d = Deduplicador()
        assertEquals(TipoLeitura.NOVA, d.classificar(leitura(Prints.UBER), 0))
        val outra = leitura(listOf("R$ 22,00", "3 min (1.0 km)", "20 min (8.0 km)"))
        assertEquals(TipoLeitura.NOVA, d.classificar(outra, 60_000))
    }

    @Test
    fun mesma_oferta_depois_da_janela_volta_a_ser_nova() {
        val d = Deduplicador(janelaMs = 120_000)
        val l = leitura(Prints.UBER)
        d.classificar(l, 0)
        assertEquals(TipoLeitura.NOVA, d.classificar(l, 200_000))
    }
}

class AnonimizadorTest {
    @Test
    fun remove_enderecos_e_mantem_os_numeros() {
        val r = Anonimizador.filtrar(Prints.NOVENTA_E_NOVE, Leitor99.termosConhecidos)
        assertTrue("R$6,86" in r)
        assertTrue("6 min (1,3 km)" in r)
        assertTrue("Negocia" in r)
        assertTrue(r.none { it.contains("Rua") })
        assertEquals("•••", r[8])
    }

    @Test
    fun nome_de_pessoa_nao_vai_para_o_registro() {
        val r = Anonimizador.filtrar(listOf("Maria Silva", "R$ 10,00"), LeitorUber.termosConhecidos)
        assertEquals(listOf("•••", "R$ 10,00"), r)
    }
}

class CategoriasTest {
    @Test
    fun nomes_diferentes_da_mesma_categoria_viram_um_so() {
        assertEquals("Black", Categorias.canonica("Uber Black"))
        assertEquals("Black", Categorias.canonica("Black"))
        assertEquals("UberX", Categorias.canonica("UberX"))
        assertEquals("Pop", Categorias.canonica("99Pop"))
        assertEquals("Negocia", Categorias.canonica("Negocia"))
        assertNull(Categorias.canonica(null))
    }
}
