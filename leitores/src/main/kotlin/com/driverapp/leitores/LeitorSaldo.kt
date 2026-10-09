package com.driverapp.leitores

/**
 * Leitura das telas de SALDO / GANHOS das plataformas.
 *
 * O que aparece nos prints de referência:
 *  - Uber (tela inicial): "R$ 55,23"  "HOJE"  "5 viagens concluídas"   → total do DIA.
 *  - 99 (Painel): "R$92,69" "Ganhos desta semana", "9" "Solicitações",
 *                 "R$7,60" "Valor da última corrida"                    → total da SEMANA.
 *
 * Os valores ficam logo ACIMA do rótulo (às vezes logo abaixo, dependendo da ordem da tela).
 * Campos são independentes: se o valor estiver oculto (ícone de olho), o número de corridas
 * continua sendo lido, e vice-versa. Nada é inventado.
 */
enum class PeriodoSaldo { DIA, SEMANA }

data class LeituraSaldo(
    val plataforma: Plataforma,
    val periodo: PeriodoSaldo,
    /** Total ganho no período, como a plataforma mostra. */
    val valor: Double?,
    /** Corridas/viagens/solicitações concluídas no período. */
    val corridas: Int?,
    /** Valor da última corrida (só a 99 mostra). */
    val ultimaCorrida: Double?,
) {
    val temDados: Boolean get() = valor != null || corridas != null
}

object LeitorSaldo {

    fun ler(plataforma: Plataforma, linhas: List<String>): LeituraSaldo? {
        val l = linhas.map { it.trim() }.filter { it.isNotEmpty() }
        val leitura = when (plataforma) {
            Plataforma.UBER -> lerUber(l)
            Plataforma.NOVENTA_E_NOVE -> ler99(l)
        }
        return leitura?.takeIf { it.temDados }
    }

    private fun lerUber(l: List<String>): LeituraSaldo? {
        val texto = l.joinToString("\n")
        val corridas = RE_VIAGENS.find(texto)?.groupValues?.get(1)?.toIntOrNull()
        val iHoje = l.indexOfFirst { it.equals("HOJE", ignoreCase = true) }
        if (iHoje < 0 && corridas == null) return null
        val valor = if (iHoje >= 0) dinheiroPerto(l, iHoje) else null
        return LeituraSaldo(Plataforma.UBER, PeriodoSaldo.DIA, valor, corridas, ultimaCorrida = null)
    }

    private fun ler99(l: List<String>): LeituraSaldo? {
        val iGanhos = l.indexOfFirst { RE_GANHOS_SEMANA.containsMatchIn(it) }
        val iSolic = l.indexOfFirst { RE_SOLICITACOES.matches(it) }
        if (iGanhos < 0 && iSolic < 0) return null
        val iUltima = l.indexOfFirst { RE_ULTIMA.containsMatchIn(it) }
        return LeituraSaldo(
            plataforma = Plataforma.NOVENTA_E_NOVE,
            periodo = PeriodoSaldo.SEMANA,
            valor = if (iGanhos >= 0) dinheiroPerto(l, iGanhos) else null,
            corridas = if (iSolic >= 0) inteiroPerto(l, iSolic) else null,
            ultimaCorrida = if (iUltima >= 0) dinheiroPerto(l, iUltima) else null,
        )
    }

    /** Procura o valor junto do rótulo: primeiro acima (1 e 2 linhas), depois abaixo. */
    private fun vizinhos(i: Int) = listOf(i - 1, i - 2, i + 1)

    private fun dinheiroPerto(l: List<String>, i: Int): Double? =
        vizinhos(i).firstNotNullOfOrNull { j -> l.getOrNull(j)?.let { RE_SO_DINHEIRO.matchEntire(it) }?.let { Numeros.parse(it.groupValues[1]) } }

    private fun inteiroPerto(l: List<String>, i: Int): Int? =
        vizinhos(i).firstNotNullOfOrNull { j -> l.getOrNull(j)?.takeIf { RE_INTEIRO.matches(it) }?.toIntOrNull() }

    private val RE_VIAGENS = Regex("""(\d+)\s*viage(?:m|ns)\s+conclu[íi]das?""", RegexOption.IGNORE_CASE)
    private val RE_GANHOS_SEMANA = Regex("""ganhos\s+desta\s+semana""", RegexOption.IGNORE_CASE)
    private val RE_SOLICITACOES = Regex("""solicita[çc][õo]es\s*>?""", RegexOption.IGNORE_CASE)
    private val RE_ULTIMA = Regex("""valor\s+da\s+[úu]ltima""", RegexOption.IGNORE_CASE)
    private val RE_SO_DINHEIRO = Regex("""R\$\s*([\d.,]*\d)""")
    private val RE_INTEIRO = Regex("""\d{1,4}""")
}
