package com.driverapp.leitores

import kotlin.math.abs

/**
 * Camada de validação: confere se o que foi lido faz sentido antes de usar nos cálculos.
 *
 *  - Campos são independentes: um campo inválido vira "indisponível" sem derrubar os outros.
 *  - Nada é inventado: valores fora de faixa são descartados, nunca "corrigidos" por chute.
 *  - Quando algo é ambíguo, a leitura é marcada como BAIXA confiança (o card avisa o motorista).
 */
enum class Confianca { ALTA, BAIXA }

data class LeituraValidada(
    val plataforma: Plataforma,
    val valor: Double?,
    val coleta: TrechoLido?,
    val viagem: TrechoLido?,
    val categoria: String?,
    val notaPassageiro: Double?,
    val observacoes: List<String>,
    /** Campos que não apareceram ou foram descartados: "valor", "coleta", "viagem", "categoria", "nota". */
    val ausentes: List<String>,
    /** Motivos da baixa confiança, em linguagem simples. */
    val alertas: List<String>,
    val confianca: Confianca,
    /** Bairro/cidade de destino, só para exibição (nunca é gravado). */
    val destino: String? = null,
) {
    /** Tem o mínimo para mostrar o card: valor e viagem. */
    val utilizavel: Boolean get() = valor != null && viagem != null
}

object Interpretador {
    const val VALOR_MIN = 2.0
    const val VALOR_MAX = 3000.0
    const val KM_MAX = 400.0
    const val MIN_MAX = 720.0
    const val VELOCIDADE_MAX_KMH = 150.0
    const val VELOCIDADE_MIN_KMH = 2.0
    const val TOLERANCIA_REAIS_POR_KM = 0.15

    fun validar(o: OfertaLida): LeituraValidada {
        val ausentes = mutableListOf<String>()
        val alertas = mutableListOf<String>()

        val valor = o.valor?.takeIf { it in VALOR_MIN..VALOR_MAX }
        if (o.valor == null) ausentes += "valor"
        else if (valor == null) {
            ausentes += "valor"
            alertas += "Valor fora do esperado"
        }

        val coleta = validarTrecho(o.coleta, "coleta", ausentes, alertas)
        val viagem = validarTrecho(o.viagem, "viagem", ausentes, alertas)
        if (o.coleta == null && o.viagem != null) {
            alertas += "Só um trecho na tela: não dá para separar coleta e viagem"
        }

        if (o.categoria == null) ausentes += "categoria"
        val nota = o.notaPassageiro?.takeIf { it in 1.0..5.0 }
        if (nota == null) ausentes += "nota"

        // Conferência com o R$/km que a própria plataforma mostra, quando existe.
        val informado = o.reaisPorKmInformado
        if (informado != null && informado > 0 && valor != null && viagem != null) {
            val candidatos = listOfNotNull(
                coleta?.let { valor / (it.km + viagem.km) },
                valor / viagem.km,
            )
            val bate = candidatos.any { abs(it - informado) / informado <= TOLERANCIA_REAIS_POR_KM }
            if (!bate) alertas += "R$/km calculado não bate com o da plataforma"
        }

        return LeituraValidada(
            plataforma = o.plataforma,
            valor = valor,
            coleta = coleta,
            viagem = viagem,
            categoria = o.categoria,
            notaPassageiro = nota,
            observacoes = o.observacoes,
            ausentes = ausentes,
            alertas = alertas,
            confianca = if (alertas.isEmpty()) Confianca.ALTA else Confianca.BAIXA,
            destino = o.destino,
        )
    }

    private fun validarTrecho(
        t: TrechoLido?,
        nome: String,
        ausentes: MutableList<String>,
        alertas: MutableList<String>,
    ): TrechoLido? {
        if (t == null) {
            ausentes += nome
            return null
        }
        if (t.km <= 0 || t.km > KM_MAX || t.minutos <= 0 || t.minutos > MIN_MAX) {
            ausentes += nome
            alertas += "Distância ou tempo da $nome fora do esperado"
            return null
        }
        val kmh = t.km / (t.minutos / 60.0)
        if (kmh > VELOCIDADE_MAX_KMH || (t.km >= 1.0 && kmh < VELOCIDADE_MIN_KMH)) {
            alertas += "Tempo e distância da $nome não combinam"
        }
        return t
    }
}
