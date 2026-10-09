package com.driverapp.leitura

import com.driverapp.calculo.Combustivel
import com.driverapp.calculo.CriteriosClassificacao
import com.driverapp.calculo.Nivel
import com.driverapp.calculo.TipoCenario
import com.driverapp.calculo.TipoCombustivel
import com.driverapp.leitores.Interpretador
import com.driverapp.leitores.Leitor99
import com.driverapp.leitores.LeitorUber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val D = 0.001

/**
 * Prova de ponta a ponta da Fase 3: texto da tela → leitura → validação → cálculo → cor,
 * usando os prints reais e os custos de um motorista configurado.
 */
class AnaliseOfertaTest {
    private val custoKm = Combustivel(TipoCombustivel.GASOLINA, 4.19, 8.0).custoPorKm
    private val limites = CriteriosClassificacao.deLimites(kmMinimo = 1.8, kmBom = 2.2, horaMinimo = 30.0, horaBom = 45.0)

    private val telaUber = listOf(
        "UberX", "Exclusivo", "R$ 15,30", "R$2,35/km aprox.", "4,94 (1332)", "Verificado",
        "4 min (1.0 km)", "Rua Santa Luzia da Boa Visão, Itaim Paulista, São Paulo",
        "14 minutos (5.5 km)", "Rua Rosa Ribas, 316, Poá, Poá", "Aceitar",
    )
    private val tela99 = listOf(
        "Negocia", "Pgto. no app", "R$6,86", "R$2,56/km", "4,95", "359 corridas", "Perfil Premium",
        "6 min (1,3 km)", "Rua Tibúrcio de Sousa, 1391, Itaim Paulista",
        "5 min (1,4 km)", "Rua João Esteves Robalo, 21, Jardim Míriam", "Aceitar por R$6,86",
    )

    @Test
    fun oferta_uber_do_print_de_ponta_a_ponta() {
        val leitura = Interpretador.validar(LeitorUber.ler(telaUber)!!)
        val r = AnaliseOferta.analisar(leitura, custoKm, custoFixoPorHora = 15.5, criterios = limites)

        val total = r.analise!!.principal!!
        assertEquals(TipoCenario.TOTAL, total.tipo)           // considera a coleta
        assertEquals(6.5, total.km!!, D)
        assertEquals(18.0, total.minutos!!, D)
        assertEquals(2.354, total.reaisPorKm!!, D)
        assertEquals(51.0, total.reaisPorHora!!, D)
        assertEquals(3.404, total.custoCombustivel!!, D)       // usa o combustível do cadastro
        assertEquals(11.896, total.resultado!!, D)
        assertEquals(4.65, total.custoFixoAlocado!!, D)
        assertEquals(Nivel.BOA, r.nivel)                        // ≥ 2,20/km e ≥ R$ 45/h
        assertEquals("UberX", r.categoria)
    }

    @Test
    fun oferta_99_do_print_fica_amarela_pelo_valor_por_hora() {
        val leitura = Interpretador.validar(Leitor99.ler(tela99)!!)
        val r = AnaliseOferta.analisar(leitura, custoKm, 0.0, limites)
        val total = r.analise!!.principal!!
        assertEquals(2.541, total.reaisPorKm!!, D)
        assertEquals(37.418, total.reaisPorHora!!, D)
        assertEquals(Nivel.RAZOAVEL, r.nivel)                   // R$/h entre 30 e 45
    }

    @Test
    fun sem_combustivel_configurado_mostra_resultado_indisponivel() {
        val leitura = Interpretador.validar(LeitorUber.ler(telaUber)!!)
        val r = AnaliseOferta.analisar(leitura, custoCombustivelPorKm = null, custoFixoPorHora = 0.0, criterios = limites)
        assertNull(r.analise!!.principal!!.custoCombustivel)
        assertNull(r.analise!!.principal!!.resultado)
        assertEquals(2.354, r.analise!!.principal!!.reaisPorKm!!, D) // o resto continua
    }

    @Test
    fun sem_valor_lido_nao_calcula_nada() {
        val semValor = telaUber.filterNot { it.startsWith("R$") }
        val r = AnaliseOferta.analisar(Interpretador.validar(LeitorUber.ler(semValor)!!), custoKm, 0.0, limites)
        assertNull(r.analise)
        assertNull(r.nivel)
    }
}
