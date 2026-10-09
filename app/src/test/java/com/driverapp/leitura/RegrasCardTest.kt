package com.driverapp.leitura

import org.junit.Assert.assertEquals
import org.junit.Test

class RegrasCardTest {
    // Tela de 1080 x 2400 px (celular comum), margem 30 px, barra de status 80 px, navegação 150 px.
    private fun encaixar(x: Int, y: Int, largura: Int = 400, altura: Int = 160) =
        Encaixe.calcular(x, y, largura, altura, 1080, 2400, 30, 80, 150)

    @Test
    fun gruda_na_lateral_mais_proxima() {
        assertEquals(30, encaixar(x = 100, y = 500).x)          // perto da esquerda
        assertEquals(1080 - 400 - 30, encaixar(x = 700, y = 500).x) // perto da direita
    }

    @Test
    fun nunca_sai_da_tela_na_vertical() {
        assertEquals(80, encaixar(x = 0, y = -300).y)            // não passa da barra de status
        assertEquals(2400 - 160 - 150, encaixar(x = 0, y = 5000).y) // não entra na barra de navegação
        assertEquals(900, encaixar(x = 0, y = 900).y)
    }

    @Test
    fun card_quase_da_largura_da_tela_fica_centralizado() {
        assertEquals(20, encaixar(x = 500, y = 500, largura = 1040).x)
    }

    @Test
    fun opacidade_nunca_fica_ilegivel() {
        assertEquals(Opacidade.MINIMA, Opacidade.limitar(0.05f), 0.0001f)
        assertEquals(1f, Opacidade.limitar(3f), 0.0001f)
        assertEquals(204, Opacidade.alfa(Opacidade.EQUILIBRADO))
        assertEquals(89, Opacidade.alfa(0f)) // mínimo 35%
        assertEquals(1, Opacidade.nivelMaisProximo(0.78f))
    }
}
