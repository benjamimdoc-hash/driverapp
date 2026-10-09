package com.driverapp.calculo

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Filtro dos pontos de GPS, para a quilometragem não "inflar" com ruído.
 *
 * Regras:
 *  1. Ponto com precisão pior que [PRECISAO_MAXIMA_M] é descartado.
 *  2. Movimento menor que a margem de erro (mínimo [DISTANCIA_MINIMA_M]) não conta
 *     — evita somar "tremidas" do GPS com o carro parado.
 *  3. Salto impossível (acima de [VELOCIDADE_MAXIMA_MS], ~200 km/h) é descartado.
 *
 * Lógica pura, testada em FiltroGpsTest.
 */
data class PontoGps(val latitude: Double, val longitude: Double, val precisaoM: Float?, val tempoMs: Long)

data class AvaliacaoPonto(
    /** O ponto vira a nova referência. */
    val aceito: Boolean,
    /** Metros a somar na jornada (0 quando não aceito). */
    val metros: Double,
)

object FiltroGps {
    const val PRECISAO_MAXIMA_M = 30f
    const val DISTANCIA_MINIMA_M = 10.0
    const val VELOCIDADE_MAXIMA_MS = 55.6

    fun avaliar(referencia: PontoGps?, novo: PontoGps): AvaliacaoPonto {
        val precisao = novo.precisaoM
        if (precisao != null && precisao > PRECISAO_MAXIMA_M) return AvaliacaoPonto(false, 0.0)
        if (referencia == null) return AvaliacaoPonto(true, 0.0)

        val segundos = (novo.tempoMs - referencia.tempoMs) / 1000.0
        if (segundos <= 0) return AvaliacaoPonto(false, 0.0)

        val d = distanciaMetros(referencia, novo)
        val margem = max(DISTANCIA_MINIMA_M, (precisao ?: 0f).toDouble())
        if (d < margem) return AvaliacaoPonto(false, 0.0)
        if (d / segundos > VELOCIDADE_MAXIMA_MS) return AvaliacaoPonto(false, 0.0)
        return AvaliacaoPonto(true, d)
    }

    /** Distância em linha reta entre dois pontos (fórmula de Haversine). */
    fun distanciaMetros(a: PontoGps, b: PontoGps): Double {
        val r = 6_371_000.0
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * r * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }
}
