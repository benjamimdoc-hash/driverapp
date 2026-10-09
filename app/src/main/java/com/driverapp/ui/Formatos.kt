package com.driverapp.ui

import java.util.Locale

/** Formatação de números no padrão brasileiro, usada em todas as telas. */
object Formatos {
    val BR: Locale = Locale.forLanguageTag("pt-BR")

    fun moeda(v: Double?): String = v?.let {
        val limpo = if (kotlin.math.abs(it) < 0.005) 0.0 else it // evita "-0,00"
        "R$ " + String.format(BR, "%,.2f", limpo)
    } ?: "—"
    fun numero(v: Double, casas: Int = 2): String = String.format(BR, "%.${casas}f", v)
    fun km(v: Double): String = String.format(BR, "%.1f km", v)

    fun duracao(ms: Long): String {
        val totalMin = ms / 60_000
        val h = totalMin / 60
        val m = totalMin % 60
        return if (h > 0) "${h}h ${m.toString().padStart(2, '0')}min" else "${m}min"
    }

    fun relogio(ms: Long): String {
        val s = ms / 1000
        return "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
    }

    /** Lê o que o motorista digitou: aceita "4,19", "4.19", "1.234,50". */
    fun lerNumero(texto: String): Double? {
        val t = texto.trim().replace("R$", "").trim()
        if (t.isEmpty()) return null
        val normal = when {
            ',' in t && '.' in t -> t.replace(".", "").replace(',', '.')
            ',' in t -> t.replace(',', '.')
            else -> t
        }
        return normal.toDoubleOrNull()
    }

    /** Mostra um número num campo de texto (vazio se não houver). */
    fun campo(v: Double?): String = when {
        v == null -> ""
        v % 1.0 == 0.0 -> v.toLong().toString()
        else -> String.format(BR, "%.2f", v)
    }
}
