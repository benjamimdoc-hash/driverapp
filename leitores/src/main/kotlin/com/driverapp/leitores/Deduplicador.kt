package com.driverapp.leitores

/**
 * Evita tratar a mesma oferta como várias.
 *
 * A tela da oferta fica atualizando enquanto aparece (contador, mapa), e o Android avisa o app
 * dezenas de vezes. Cada leitura recebe uma "assinatura" (plataforma + valor + trechos):
 *  - NOVA: assinatura nunca vista (ou vista há mais de [janelaMs]).
 *  - ATUALIZACAO: mesma plataforma e valor de uma oferta recente, mas agora com mais
 *    ou outros detalhes (ex.: o texto terminou de carregar). Substitui a anterior.
 *  - REPETIDA: igual a uma já processada. Ignorar.
 */
enum class TipoLeitura { NOVA, ATUALIZACAO, REPETIDA }

class Deduplicador(
    private val janelaMs: Long = 120_000,
    private val janelaAtualizacaoMs: Long = 15_000,
) {
    private val vistas = LinkedHashMap<String, Long>()
    private var ultimaChaveValor: String? = null
    private var ultimaVezMs: Long = 0

    fun classificar(l: LeituraValidada, agoraMs: Long): TipoLeitura {
        vistas.entries.removeAll { agoraMs - it.value > janelaMs }

        val assinatura = assinatura(l)
        val chaveValor = "${l.plataforma}|${centavos(l.valor)}"
        val jaVista = vistas.containsKey(assinatura)
        vistas[assinatura] = agoraMs

        val tipo = when {
            jaVista -> TipoLeitura.REPETIDA
            chaveValor == ultimaChaveValor && agoraMs - ultimaVezMs <= janelaAtualizacaoMs -> TipoLeitura.ATUALIZACAO
            else -> TipoLeitura.NOVA
        }
        ultimaChaveValor = chaveValor
        ultimaVezMs = agoraMs
        return tipo
    }

    companion object {
        fun assinatura(l: LeituraValidada): String = listOf(
            l.plataforma.name,
            centavos(l.valor),
            trecho(l.coleta),
            trecho(l.viagem),
        ).joinToString("|")

        private fun centavos(v: Double?): String = v?.let { Math.round(it * 100).toString() } ?: "-"
        private fun trecho(t: TrechoLido?): String =
            t?.let { "${Math.round(it.minutos)}m${Math.round(it.km * 10)}" } ?: "-"
    }
}

/**
 * Remove dados pessoais antes de guardar um registro de depuração.
 * Fica só o que interessa para conferir a leitura: valores, tempos, distâncias, nota,
 * categoria e marcas conhecidas. Endereços, nomes e qualquer outro texto viram "•••".
 */
object Anonimizador {
    private val RE_UTIL = Regex(
        """R\$\s*[\d.,]*\d|\d+\s*(min|minutos?|h)\b|\(\s*[\d.,]*\d\s*(km|m)\s*\)|^\s*[★*]?\s*[1-5][.,]\d{1,2}\b|\d+\s*corridas|^\s*[\d.,]+\s*(km|min)""",
        RegexOption.IGNORE_CASE,
    )

    fun filtrar(linhas: List<String>, termosConhecidos: List<String>): List<String> =
        linhas.map { linha ->
            val t = linha.trim()
            when {
                t.isEmpty() -> t
                RE_UTIL.containsMatchIn(t) && !pareceEndereco(t) -> t
                termosConhecidos.any { it.equals(t, ignoreCase = true) } -> t
                else -> "•••"
            }
        }

    private fun pareceEndereco(t: String): Boolean =
        t.count { it == ',' } >= 1 && t.any { it.isLetter() } && !t.contains("R$") && !t.contains("km", ignoreCase = true)
}
