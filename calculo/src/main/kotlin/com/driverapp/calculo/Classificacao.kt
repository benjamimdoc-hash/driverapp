package com.driverapp.calculo

/**
 * Classificação de uma corrida em 4 níveis, mostrados em 3 cores:
 *  RUIM → vermelho | RAZOAVEL → amarelo | BOA e EXCELENTE → verde.
 *
 * Regra: avalia R$/km e R$/hora separadamente e fica com o PIOR dos dois
 * (uma corrida só é "boa" se for boa nos dois critérios disponíveis).
 */
enum class Nivel(val cor: Cor) {
    RUIM(Cor.VERMELHO),
    RAZOAVEL(Cor.AMARELO),
    BOA(Cor.VERDE),
    EXCELENTE(Cor.VERDE),
}

enum class Cor { VERMELHO, AMARELO, VERDE }

/** Limites mínimos para cada nível (em ordem crescente). Abaixo de [razoavel] é RUIM. */
data class Faixas(val razoavel: Double, val boa: Double, val excelente: Double) {
    init {
        require(razoavel <= boa && boa <= excelente) { "Faixas devem estar em ordem crescente" }
    }

    fun nivel(valor: Double): Nivel = when {
        valor >= excelente -> Nivel.EXCELENTE
        valor >= boa -> Nivel.BOA
        valor >= razoavel -> Nivel.RAZOAVEL
        else -> Nivel.RUIM
    }
}

data class CriteriosClassificacao(val porKm: Faixas, val porHora: Faixas) {
    companion object {
        /**
         * Monta critérios a partir dos dois limites que o motorista configura por indicador:
         *  - mínimo: abaixo dele é vermelho;
         *  - bom: a partir dele é verde. Entre os dois, amarelo.
         * "Excelente" fica 25% acima do bom.
         */
        fun deLimites(kmMinimo: Double, kmBom: Double, horaMinimo: Double, horaBom: Double) = CriteriosClassificacao(
            porKm = Faixas(kmMinimo, maxOf(kmMinimo, kmBom), maxOf(kmMinimo, kmBom) * 1.25),
            porHora = Faixas(horaMinimo, maxOf(horaMinimo, horaBom), maxOf(horaMinimo, horaBom) * 1.25),
        )
    }
}

object Classificador {
    /** Null quando não há nem R$/km nem R$/hora para avaliar. */
    fun classificar(reaisPorKm: Double?, reaisPorHora: Double?, criterios: CriteriosClassificacao): Nivel? {
        val niveis = listOfNotNull(
            reaisPorKm?.let { criterios.porKm.nivel(it) },
            reaisPorHora?.let { criterios.porHora.nivel(it) },
        )
        return niveis.minByOrNull { it.ordinal }
    }
}

/**
 * VALORES INICIAIS SUGERIDOS — apenas referências para começar, editáveis pelo motorista.
 * Não são regras do mercado: cada cidade, carro e categoria tem uma realidade diferente.
 */
object ReferenciasIniciais {
    val ECONOMICA = CriteriosClassificacao(
        porKm = Faixas(razoavel = 1.50, boa = 2.00, excelente = 2.50),
        porHora = Faixas(razoavel = 30.0, boa = 45.0, excelente = 60.0),
    )
    val PREMIUM = CriteriosClassificacao(
        porKm = Faixas(razoavel = 2.20, boa = 2.80, excelente = 3.50),
        porHora = Faixas(razoavel = 40.0, boa = 60.0, excelente = 80.0),
    )

    private val categoriasPremium = setOf("black", "uber black", "comfort", "uber comfort", "top", "99top")

    /** Sugestão inicial por categoria. Categorias desconhecidas usam a econômica. */
    fun paraCategoria(categoria: String?): CriteriosClassificacao =
        if (categoria != null && categoria.trim().lowercase() in categoriasPremium) PREMIUM else ECONOMICA
}
