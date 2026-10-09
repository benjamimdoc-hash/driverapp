package com.driverapp.leitores

/**
 * Lê uma oferta de corrida a partir dos textos visíveis na tela (na ordem em que aparecem).
 *
 * A Uber e a 99 seguem o mesmo padrão:
 *   valor  →  "R$ 15,30" / "R$6,86"
 *   coleta →  "4 min (1.0 km)"      (1º trecho)
 *   viagem →  "14 minutos (5.5 km)" (2º trecho)
 * Cada plataforma só muda a lista de categorias e marcas conhecidas.
 */
open class LeitorDeOferta(
    val plataforma: Plataforma,
    private val categoriasConhecidas: List<String>,
    private val marcasConhecidas: List<String>,
) {

    /** Categorias e marcas que este leitor reconhece (usado também para anonimizar registros). */
    val termosConhecidos: List<String> get() = categoriasConhecidas + marcasConhecidas

    /**
     * Retorna null quando o texto não parece uma oferta: sem nenhum trecho "X min (Y km)",
     * que é o que diferencia a tela de oferta das demais telas.
     * Cada campo é lido de forma independente: se o valor não for encontrado, os trechos,
     * a categoria e a nota continuam sendo devolvidos (o valor fica null).
     */
    fun ler(linhas: List<String>): OfertaLida? {
        val texto = linhas.joinToString("\n") { it.trim() }
        val trechos = lerTrechos(texto)
        if (trechos.isEmpty()) return null
        val valor = lerValor(linhas)

        // Com um trecho só, não dá para saber com segurança se é coleta ou viagem:
        // tratamos como viagem e deixamos a coleta como "não disponível".
        val coleta = if (trechos.size >= 2) trechos[0] else null
        val viagem = if (trechos.size >= 2) trechos[1] else trechos[0]

        return OfertaLida(
            plataforma = plataforma,
            valor = valor,
            coleta = coleta,
            viagem = viagem,
            categoria = lerCategoria(texto),
            notaPassageiro = lerNota(texto),
            corridasPassageiro = lerCorridasPassageiro(texto),
            reaisPorKmInformado = RE_REAIS_POR_KM.find(texto)?.let { Numeros.parse(it.groupValues[1]) },
            observacoes = marcasConhecidas.filter { marca -> texto.contains(marca, ignoreCase = true) },
            destino = lerDestino(linhas),
        )
    }

    /**
     * Destino resumido (só o bairro/cidade, ex.: "Jardim Míriam"), tirado da linha logo abaixo
     * do trecho da viagem. Serve apenas para exibir no card: NÃO é gravado em lugar nenhum.
     */
    private fun lerDestino(linhas: List<String>): String? {
        val comTrecho = linhas.indices.filter { i ->
            RE_TRECHO.findAll(linhas[i]).any { m -> m.groups[1] != null || m.groups[2] != null }
        }
        val idx = comTrecho.getOrNull(1) ?: return null
        if (comTrecho.size > 2) return null // mais trechos que o esperado: não arriscar
        val proxima = linhas.getOrNull(idx + 1)?.trim() ?: return null
        if (proxima.contains("R$") || RE_TRECHO.containsMatchIn(proxima) || proxima.none { it.isLetter() }) return null
        if (proxima.startsWith("Aceitar", ignoreCase = true)) return null
        val partes = proxima.split(',').map { it.trim() }.filter { p -> p.isNotEmpty() && p.any { it.isLetter() } }
        if (partes.size < 2) return null
        return partes.last().takeIf { it.length in 2..40 }
    }

    /**
     * O valor da corrida é preferencialmente uma linha que contém SÓ o dinheiro ("R$ 15,30").
     * Ignora "R$2,35/km", "+R$ 1" (selo de dinâmica no mapa) e "Aceitar por R$6,86".
     */
    private fun lerValor(linhas: List<String>): Double? {
        linhas.forEach { linha ->
            RE_LINHA_SO_DINHEIRO.matchEntire(linha.trim())?.let { m -> return Numeros.parse(m.groupValues[1]) }
        }
        val texto = linhas.joinToString("\n")
        return RE_DINHEIRO.findAll(texto)
            .filterNot { m -> texto.substring(m.range.last + 1).trimStart().startsWith("/") }
            .filterNot { m -> m.range.first > 0 && texto[m.range.first - 1] == '+' }
            .firstNotNullOfOrNull { Numeros.parse(it.groupValues[1]) }
    }

    private fun lerTrechos(texto: String): List<TrechoLido> =
        RE_TRECHO.findAll(texto).mapNotNull { m ->
            val horas = m.groups[1]?.value?.toIntOrNull()
            val minutos = m.groups[2]?.value?.toIntOrNull()
            if (horas == null && minutos == null) return@mapNotNull null
            val distancia = Numeros.parse(m.groupValues[3]) ?: return@mapNotNull null
            val km = if (m.groupValues[4].equals("m", ignoreCase = true)) distancia / 1000.0 else distancia
            TrechoLido(minutos = (horas ?: 0) * 60.0 + (minutos ?: 0), km = km)
        }.toList()

    private fun lerCategoria(texto: String): String? =
        categoriasConhecidas
            .mapNotNull { cat ->
                Regex("(?<![\\p{L}\\d])" + Regex.escape(cat) + "(?![\\p{L}\\d])", RegexOption.IGNORE_CASE)
                    .find(texto)?.let { it.range.first to cat }
            }
            .minByOrNull { it.first }
            ?.second

    /** Nota do passageiro: número de 1,00 a 5,00 com duas casas, fora de valores em R$ e de distâncias. */
    private fun lerNota(texto: String): Double? {
        val semDinheiro = RE_DINHEIRO_COMPLETO.replace(texto, " ")
        return RE_NOTA.findAll(semDinheiro)
            .mapNotNull { Numeros.parse(it.groupValues[1]) }
            .firstOrNull { it in 1.0..5.0 }
    }

    private fun lerCorridasPassageiro(texto: String): Int? {
        RE_CORRIDAS.find(texto)?.let { return it.groupValues[1].replace(".", "").toIntOrNull() }
        RE_NOTA_COM_TOTAL.find(texto)?.let { return it.groupValues[1].replace(".", "").toIntOrNull() }
        return null
    }

    companion object {
        private val RE_LINHA_SO_DINHEIRO = Regex("""R\$\s*([\d.,]+)""")
        private val RE_DINHEIRO = Regex("""R\$\s*([\d.,]*\d)""")
        private val RE_DINHEIRO_COMPLETO = Regex("""\+?R\$\s*[\d.,]*\d(\s*/\s*\w+)?""")
        private val RE_REAIS_POR_KM = Regex("""R\$\s*([\d.,]*\d)\s*/\s*km""", RegexOption.IGNORE_CASE)
        private val RE_TRECHO = Regex(
            """(?:(\d+)\s*h(?:oras?|rs?)?\s*)?(?:(\d+)\s*min(?:utos?)?)?\s*\(\s*([\d.,]*\d)\s*(km|m)\s*\)""",
            RegexOption.IGNORE_CASE,
        )
        private val RE_NOTA = Regex("""(?<![\d.,])([1-5][.,]\d{2})(?![\d.,])(?!\s*(?:km|m)\b)""")
        private val RE_CORRIDAS = Regex("""(\d[\d.]*)\s*corridas""", RegexOption.IGNORE_CASE)
        private val RE_NOTA_COM_TOTAL = Regex("""[1-5][.,]\d{2}\s*\(\s*(\d[\d.]*)\s*\)""")
    }
}

object LeitorUber : LeitorDeOferta(
    plataforma = Plataforma.UBER,
    categoriasConhecidas = listOf(
        "UberX", "Uber Comfort", "Comfort", "Uber Black", "Black", "Uber Flash", "Flash",
        "Uber Moto", "Moto", "Priority", "Juntos", "XL", "Envios",
    ),
    marcasConhecidas = listOf("Exclusivo", "Verificado", "Dinheiro", "Viagem longa"),
)

object Leitor99 : LeitorDeOferta(
    plataforma = Plataforma.NOVENTA_E_NOVE,
    categoriasConhecidas = listOf(
        "Negocia", "99Pop", "Pop", "99Comfort", "Comfort", "99Top", "Top", "99Taxi", "Taxi", "99Moto", "Moto", "Entrega",
    ),
    marcasConhecidas = listOf("Pgto. no app", "Dinheiro", "Perfil Premium", "Verificado"),
)
