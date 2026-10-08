package com.driverapp.calculo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

private const val D = 0.001

class CombustivelTest {
    @Test
    fun custoPorKm_eh_preco_dividido_pelo_consumo() {
        val c = Combustivel(TipoCombustivel.GASOLINA, precoPorLitro = 4.19, kmPorLitro = 8.0)
        assertEquals(0.52375, c.custoPorKm!!, D)
        assertEquals(3.404, c.custoPara(6.5)!!, D)
    }

    @Test
    fun consumo_zero_nao_gera_valor_inventado() {
        assertNull(Combustivel(TipoCombustivel.ETANOL, 4.0, 0.0).custoPorKm)
    }
}

class DespesasTest {
    private val plano = PlanoDeTrabalho(diasPorSemana = 6, horasPorDia = 10.0)

    @Test
    fun converte_cada_periodicidade_para_dia_trabalhado() {
        assertEquals(100.0, Conversao.porDiaTrabalhado(600.0, Periodicidade.SEMANAL, plano), D)
        assertEquals(50.0, Conversao.porDiaTrabalhado(1300.0, Periodicidade.MENSAL, plano), D)
        assertEquals(5.0, Conversao.porDiaTrabalhado(1560.0, Periodicidade.ANUAL, plano), D)
        assertEquals(20.0, Conversao.porDiaTrabalhado(20.0, Periodicidade.POR_DIA_TRABALHADO, plano), D)
    }

    @Test
    fun mensal_vezes_12_eh_igual_ao_anual() {
        val mensal = Conversao.porDiaTrabalhado(100.0, Periodicidade.MENSAL, plano)
        val anual = Conversao.porDiaTrabalhado(1200.0, Periodicidade.ANUAL, plano)
        assertEquals(anual, mensal, D)
    }

    @Test
    fun resumo_soma_tudo_na_mesma_base() {
        val despesas = listOf(
            Despesa("aluguel", "Aluguel", CategoriaDespesa.ALUGUEL_VEICULO, 600.0, Periodicidade.SEMANAL),
            Despesa("parcela", "Parcela", CategoriaDespesa.PARCELA_VEICULO, 1300.0, Periodicidade.MENSAL),
            Despesa("ipva", "IPVA", CategoriaDespesa.IPVA, 1560.0, Periodicidade.ANUAL),
        )
        val r = CustosFixos.resumir(despesas, plano)
        assertEquals(155.0, r.porDiaTrabalhado, D)
        assertEquals(15.5, r.porHoraTrabalhada, D)
        assertEquals(930.0, r.porSemana, D)
        assertEquals(4030.0, r.porMes, D)
    }

    @Test
    fun mesma_despesa_informada_duas_vezes_conta_uma_vez() {
        val ipva = Despesa("ipva", "IPVA", CategoriaDespesa.IPVA, 1560.0, Periodicidade.ANUAL)
        val r = CustosFixos.resumir(listOf(ipva, ipva), plano)
        assertEquals(5.0, r.porDiaTrabalhado, D)
    }
}

class MetasTest {
    private val metas = Metas(metaSemanal = 1500.0, plano = PlanoDeTrabalho(6, 10.0))

    @Test
    fun meta_diaria_e_por_hora() {
        assertEquals(250.0, metas.metaDiaria, D)
        assertEquals(25.0, metas.metaPorHora, D)
        assertEquals(1.666, metas.metaPorKm(150.0)!!, D)
    }

    @Test
    fun quanto_falta_e_quanto_precisa_por_hora() {
        assertEquals(500.0, metas.faltaNaSemana(1000.0), D)
        assertEquals(25.0, metas.necessarioPorHora(1000.0, horasRestantes = 20.0)!!, D)
        assertEquals(0.0, metas.faltaNaSemana(2000.0), D)
        assertNull(metas.necessarioPorHora(1000.0, horasRestantes = 0.0))
        assertEquals(1.0, metas.progressoSemanal(2000.0), D)
    }
}

class AnaliseCorridaTest {
    private val custoKm = 4.19 / 8.0

    /** Oferta real do print da Uber: R$ 15,30 | coleta 4 min (1.0 km) | viagem 14 min (5.5 km). */
    @Test
    fun oferta_uber_do_print() {
        val dados = DadosCorrida(15.30, coleta = Trecho(4.0, 1.0), viagem = Trecho(14.0, 5.5))
        val a = Analisador.analisar(dados, custoKm)

        val total = a.total!!
        assertEquals(6.5, total.km!!, D)
        assertEquals(18.0, total.minutos!!, D)
        assertEquals(2.354, total.reaisPorKm!!, D)   // o app de referência mostra 2,35
        assertEquals(51.0, total.reaisPorHora!!, D)  // o app de referência mostra 51,00
        assertEquals(3.404, total.custoCombustivel!!, D)
        assertEquals(11.896, total.resultado!!, D)

        val viagem = a.soViagem!!
        assertEquals(2.782, viagem.reaisPorKm!!, D)
        assertEquals(65.571, viagem.reaisPorHora!!, D)
        assertEquals(TipoCenario.TOTAL, a.principal!!.tipo)
    }

    /** Oferta real do print da 99: R$ 6,86 | coleta 6 min (1,3 km) | viagem 5 min (1,4 km). */
    @Test
    fun oferta_99_do_print() {
        val dados = DadosCorrida(6.86, coleta = Trecho(6.0, 1.3), viagem = Trecho(5.0, 1.4))
        val total = Analisador.analisar(dados, custoKm).total!!
        assertEquals(2.7, total.km!!, D)
        assertEquals(2.541, total.reaisPorKm!!, D)
        assertEquals(37.418, total.reaisPorHora!!, D)
    }

    @Test
    fun aloca_custo_fixo_pelo_tempo_da_corrida() {
        val dados = DadosCorrida(15.30, Trecho(4.0, 1.0), Trecho(14.0, 5.5))
        val total = Analisador.analisar(dados, custoKm, custoFixoPorHora = 15.5).total!!
        assertEquals(4.65, total.custoFixoAlocado!!, D)          // 15,50/h × 18 min
        assertEquals(7.246, total.resultadoAposFixos!!, D)
    }

    @Test
    fun sem_coleta_nao_inventa_cenario_total() {
        val a = Analisador.analisar(DadosCorrida(10.0, coleta = null, viagem = Trecho(10.0, 5.0)), custoKm)
        assertNull(a.total)
        assertNotNull(a.soViagem)
        assertEquals(TipoCenario.SO_VIAGEM, a.principal!!.tipo)
    }

    @Test
    fun sem_combustivel_configurado_nao_calcula_resultado() {
        val a = Analisador.analisar(DadosCorrida(10.0, null, Trecho(10.0, 5.0)), custoCombustivelPorKm = null)
        assertEquals(2.0, a.soViagem!!.reaisPorKm!!, D)
        assertNull(a.soViagem!!.resultado)
    }
}

class ClassificacaoTest {
    private val ref = ReferenciasIniciais.ECONOMICA

    @Test
    fun fica_com_o_pior_dos_dois_criterios() {
        assertEquals(Nivel.BOA, Classificador.classificar(2.35, 51.0, ref))       // Uber do print
        assertEquals(Nivel.RAZOAVEL, Classificador.classificar(2.54, 37.4, ref))  // 99 do print
        assertEquals(Nivel.RUIM, Classificador.classificar(1.20, 70.0, ref))
    }

    @Test
    fun usa_so_o_criterio_disponivel_e_null_sem_dados() {
        assertEquals(Nivel.EXCELENTE, Classificador.classificar(3.0, null, ref))
        assertNull(Classificador.classificar(null, null, ref))
    }

    @Test
    fun categoria_premium_tem_referencia_propria() {
        assertEquals(ReferenciasIniciais.PREMIUM, ReferenciasIniciais.paraCategoria("Black"))
        assertEquals(ReferenciasIniciais.ECONOMICA, ReferenciasIniciais.paraCategoria("UberX"))
    }
}
