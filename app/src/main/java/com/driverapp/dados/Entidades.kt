package com.driverapp.dados

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabelas do banco local. Tudo fica no celular; nada vai para servidor.
 * Listas simples (plataformas, dias) são guardadas como texto separado por vírgula.
 */

/** Configuração do motorista: uma linha só (id = 1). */
@Entity(tableName = "configuracao")
data class ConfiguracaoEntity(
    @PrimaryKey val id: Int = 1,
    /** MOTORISTA ou ENTREGADOR. */
    val perfil: String? = null,
    /** Ex.: "UBER,NOVENTA_E_NOVE". */
    val plataformas: String = "",
    /** GASOLINA, ETANOL ou OUTRO. */
    val tipoCombustivel: String = "GASOLINA",
    val precoLitro: Double? = null,
    val kmPorLitro: Double? = null,
    /** PROPRIO, FINANCIADO ou ALUGADO. */
    val veiculo: String? = null,
    val parcelaMensal: Double? = null,
    val aluguelSemanal: Double? = null,
    /** Dias trabalhados, 1 = segunda ... 7 = domingo. Ex.: "1,2,3,4,5,6". */
    val diasTrabalho: String = "",
    val horasPorDia: Double? = null,
    val metaSemanal: Double? = null,
    /** Etapa do cadastro inicial em que o motorista parou (para continuar de onde estava). */
    val etapaCadastro: Int = 0,
    val cadastroConcluido: Boolean = false,
    /** Se os km rodados durante a pausa também contam. Padrão: não. */
    val contarKmNaPausa: Boolean = false,
)

@Entity(tableName = "despesa")
data class DespesaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    /** Nome de [com.driverapp.calculo.CategoriaDespesa]. */
    val categoria: String,
    val valor: Double,
    /** Nome de [com.driverapp.calculo.Periodicidade]. */
    val periodicidade: String,
)

@Entity(tableName = "jornada", indices = [Index("estado")])
data class JornadaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val inicioEm: Long,
    /** ATIVA, PAUSADA ou FINALIZADA. */
    val estado: String,
    val msAcumulados: Long,
    val retomadaEm: Long?,
    val metros: Double,
    val metrosNaPausa: Double,
    val finalizadaEm: Long?,
    /** Custos usados no fechamento (o histórico não muda se a configuração mudar depois). */
    val custoKmUsado: Double? = null,
    val custoFixoHoraUsado: Double? = null,
)

@Entity(tableName = "corrida", indices = [Index("jornadaId"), Index("criadaEm")])
data class CorridaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jornadaId: Long?,
    /** UBER, NOVENTA_E_NOVE, ... */
    val plataforma: String,
    val valor: Double,
    val km: Double? = null,
    val minutos: Double? = null,
    val criadaEm: Long,
    /** MANUAL (lançada pelo motorista) ou AUTOMATICA (lida da tela, Fase 3). */
    val origem: String = "MANUAL",
)

/** Resultado de consulta: totais de corridas por jornada (para o histórico). */
data class TotalPorJornada(
    val jornadaId: Long,
    val quantidade: Int,
    val total: Double,
)
