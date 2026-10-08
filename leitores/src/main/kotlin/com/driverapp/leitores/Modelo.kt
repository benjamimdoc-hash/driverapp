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
