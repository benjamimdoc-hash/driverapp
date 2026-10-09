package com.driverapp.calculo

/**
 * Máscara de dinheiro no padrão brasileiro, estilo "caixa eletrônico":
 * o motorista digita só números e os dois últimos viram centavos.
 *   "570"    → R$ 5,70
 *   "3700"   → R$ 37,00
 *   "123456" → R$ 1.234,56
 * O valor guardado é numérico (centavos), nunca o texto formatado.
 */
object MascaraMoeda {
    /** Máximo de dígitos aceitos (R$ 9.999.999,99). */
    const val MAX_DIGITOS = 9

    /** Mantém só os dígitos, sem zeros à esquerda, limitado a [MAX_DIGITOS]. */
    fun limpar(texto: String): String = texto.filter { it.isDigit() }.trimStart('0').take(MAX_DIGITOS)

    fun centavos(digitos: String): Long? = limpar(digitos).takeIf { it.isNotEmpty() }?.toLong()

    fun valor(digitos: String): Double? = centavos(digitos)?.let { it / 100.0 }

    /** Texto exibido (sem o "R$"): "5,70", "1.234,56". Vazio quando não há valor. */
    fun formatar(digitos: String): String {
        val c = centavos(digitos) ?: return ""
        val reais = c / 100
        val cent = (c % 100).toString().padStart(2, '0')
        val reaisTexto = reais.toString().reversed().chunked(3).joinToString(".").reversed()
        return "$reaisTexto,$cent"
    }

    /** Converte um valor salvo de volta para dígitos (ex.: 5.7 → "570"). */
    fun digitosDe(valor: Double?): String =
        valor?.takeIf { it > 0 }?.let { Math.round(it * 100).toString() } ?: ""
}
