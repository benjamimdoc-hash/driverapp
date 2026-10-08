package com.driverapp.calculo

/**
 * Custo de combustível.
 *
 * Fórmula: custo por km = preço por litro ÷ consumo (km/l).
 * Ex.: R$ 4,19/L ÷ 8 km/L = R$ 0,52 por km.
 */
enum class TipoCombustivel { GASOLINA, ETANOL, OUTRO }

data class Combustivel(
    val tipo: TipoCombustivel,
    val precoPorLitro: Double,
    val kmPorLitro: Double,
) {
    /** Custo de combustível por km rodado. Null se os dados forem inválidos (ex.: consumo zero). */
    val custoPorKm: Double?
        get() = if (precoPorLitro >= 0 && kmPorLitro > 0) precoPorLitro / kmPorLitro else null

    /** Custo de combustível para uma distância. */
    fun custoPara(km: Double): Double? = custoPorKm?.let { it * km }
}
