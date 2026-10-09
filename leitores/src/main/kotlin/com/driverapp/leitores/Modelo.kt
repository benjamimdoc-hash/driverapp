package com.driverapp.leitores

enum class Plataforma(val nome: String) {
    UBER("Uber"),
    NOVENTA_E_NOVE("99"),
}

/** Um trecho lido da tela: "4 min (1.0 km)". */
data class TrechoLido(val minutos: Double, val km: Double)

/**
 * O que foi possível ler de uma oferta. Campo null = não estava visível ou não foi
 * reconhecido com segurança. Nunca preenchemos com chute.
 */
data class OfertaLida(
    val plataforma: Plataforma,
    val valor: Double?,
    /** Deslocamento até o passageiro (primeiro trecho da tela). */
    val coleta: TrechoLido?,
    /** Viagem com o passageiro (segundo trecho da tela). */
    val viagem: TrechoLido?,
    val categoria: String?,
    val notaPassageiro: Double?,
    val corridasPassageiro: Int?,
    /** O R$/km que a própria plataforma mostra (para conferência). */
    val reaisPorKmInformado: Double?,
    /** Marcas extras: "Exclusivo", "Verificado", "Pgto. no app"... */
    val observacoes: List<String>,
) {
    /** Tem o mínimo para calcular: valor e ao menos a viagem. */
    val completa: Boolean get() = valor != null && viagem != null
}

/**
 * Nome padronizado da categoria, para os limites de classificação não dependerem
 * de como a tela escreveu ("Uber Black" e "Black" são a mesma categoria).
 */
object Categorias {
    val uber = listOf("UberX", "Comfort", "Black", "Flash", "Moto", "Priority", "Juntos", "XL")
    val noventaENove = listOf("Pop", "Negocia", "Comfort", "Top", "Taxi", "Moto")

    fun canonica(categoria: String?): String? {
        val c = categoria?.trim() ?: return null
        if (c.equals("UberX", ignoreCase = true)) return "UberX"
        val semPrefixo = c.removePrefix("Uber ").removePrefix("uber ").removePrefix("99").trim()
        val todas = uber + noventaENove
        return todas.firstOrNull { it.equals(semPrefixo, ignoreCase = true) } ?: semPrefixo
    }

    fun daPlataforma(p: Plataforma): List<String> = when (p) {
        Plataforma.UBER -> uber
        Plataforma.NOVENTA_E_NOVE -> noventaENove
    }
}
