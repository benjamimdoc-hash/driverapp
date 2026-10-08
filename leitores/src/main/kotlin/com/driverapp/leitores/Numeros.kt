package com.driverapp.leitores

/**
 * Converte números no formato que aparece nas telas.
 *  "15,30" → 15.30 (vírgula decimal, 99 e valores da Uber)
 *  "1.0"   → 1.0   (ponto decimal, km da Uber)
 *  "1.234,56" → 1234.56 (milhar com ponto)
 *  "1,234.56" → 1234.56 (formato inglês)
 */
object Numeros {
    fun parse(texto: String): Double? {
        val s = texto.trim().trimEnd('.', ',')
        if (s.isEmpty()) return null
        val normal = when {
            ',' in s && '.' in s ->
                if (s.lastIndexOf(',') > s.lastIndexOf('.')) s.replace(".", "").replace(',', '.')
                else s.replace(",", "")
            ',' in s -> s.replace(',', '.')
            else -> s
        }
        return normal.toDoubleOrNull()
    }
}
