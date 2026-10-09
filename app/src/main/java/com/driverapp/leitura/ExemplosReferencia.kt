package com.driverapp.leitura

import com.driverapp.leitores.Leitor99
import com.driverapp.leitores.LeitorUber
import com.driverapp.leitores.OfertaLida
import com.driverapp.leitores.Plataforma

/**
 * Textos das telas de oferta dos prints de referência (na ordem em que aparecem).
 * Os endereços foram mantidos só aqui, por serem dos prints de exemplo; o app nunca grava endereços.
 */
object ExemplosReferencia {
    val UBER = listOf(
        "UberX", "Exclusivo", "R$ 15,30", "R$2,35/km aprox.", "4,94 (1332)", "Verificado",
        "4 min (1.0 km)", "Rua Santa Luzia da Boa Visão, Itaim Paulista, São Paulo",
        "14 minutos (5.5 km)", "Rua Rosa Ribas, 316, Poá, Poá", "Aceitar",
    )
    val NOVENTA_E_NOVE = listOf(
        "Negocia", "Pgto. no app", "R$6,86", "R$2,56/km", "4,95", "359 corridas", "Perfil Premium",
        "6 min (1,3 km)", "Rua Tibúrcio de Sousa, 1391, Itaim Paulista",
        "5 min (1,4 km)", "Rua João Esteves Robalo, 21, Jardim Míriam", "Aceitar por R$6,86",
    )

    /**
     * Para uma imagem de origem desconhecida: tenta os dois leitores e fica com o que
     * reconheceu mais campos. Empate favorece a plataforma com categoria reconhecida.
     */
    fun melhorLeitura(linhas: List<String>): Pair<Plataforma, OfertaLida>? {
        val candidatos = listOfNotNull(
            LeitorUber.ler(linhas)?.let { Plataforma.UBER to it },
            Leitor99.ler(linhas)?.let { Plataforma.NOVENTA_E_NOVE to it },
        )
        return candidatos.maxByOrNull { (_, o) ->
            listOf(o.valor, o.coleta, o.viagem, o.notaPassageiro).count { it != null } * 2 +
                (if (o.categoria != null) 1 else 0) + o.observacoes.size
        }
    }
}
