package com.driverapp.leitura

import kotlin.math.roundToInt

/**
 * Regras do card flutuante que não dependem do Android (testadas em RegrasCardTest).
 */

/** Opacidade do FUNDO do card. Os números e textos ficam sempre 100% opacos. */
object Opacidade {
    const val MINIMA = 0.35f
    const val DISCRETO = 0.55f
    const val EQUILIBRADO = 0.80f
    const val MAIS_VISIVEL = 0.95f

    val niveis = listOf("Discreto" to DISCRETO, "Equilibrado" to EQUILIBRADO, "Mais visível" to MAIS_VISIVEL)

    /** Nunca deixa o fundo tão transparente que atrapalhe a leitura. */
    fun limitar(v: Float): Float = v.coerceIn(MINIMA, 1f)

    /** Canal alfa (0–255) para o fundo. */
    fun alfa(v: Float): Int = (limitar(v) * 255).roundToInt()

    /** Nível mais próximo do valor (para marcar o botão certo quando o slider é usado). */
    fun nivelMaisProximo(v: Float): Int = niveis.indices.minBy { kotlin.math.abs(niveis[it].second - v) }
}

/**
 * Encaixe ao soltar o card depois de arrastar:
 *  - vertical: nunca sai da tela (respeita barra de status e barra de navegação);
 *  - horizontal: gruda na lateral mais próxima; se o card ocupa quase toda a largura, fica centralizado.
 */
object Encaixe {
    data class Posicao(val x: Int, val y: Int)

    fun calcular(
        x: Int,
        y: Int,
        largura: Int,
        altura: Int,
        larguraTela: Int,
        alturaTela: Int,
        margem: Int,
        topoMinimo: Int,
        baseReservada: Int,
    ): Posicao {
        val yMax = (alturaTela - altura - baseReservada).coerceAtLeast(topoMinimo)
        val novoY = y.coerceIn(topoMinimo, yMax)
        val novoX = when {
            largura >= larguraTela - 2 * margem -> ((larguraTela - largura) / 2).coerceAtLeast(0)
            x + largura / 2 < larguraTela / 2 -> margem
            else -> larguraTela - largura - margem
        }
        return Posicao(novoX, novoY)
    }
}
