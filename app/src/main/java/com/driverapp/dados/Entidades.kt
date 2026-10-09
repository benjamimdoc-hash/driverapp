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
    // ---- Versão 2 do banco (colunas novas são opcionais, para a migração ser segura) ----
    /** ESCURO, CLARO ou null = seguir o sistema. */
    val tema: String? = null,
    /** Horário habitual, "HH:MM" (opcional). */
    val horaInicio: String? = null,
    val horaFim: String? = null,
    /** Km rodados por dia, em média (opcional; permite calcular custo fixo por km). */
    val kmPorDia: Double? = null,
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
    // ---- Versão 2 ----
    /** Para VALOR_UNICO: em quantos meses distribuir. */
    val prazoMeses: Int? = null,
    val observacao: String? = null,
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
    // ---- Versão 2 ----
    val categoria: String? = null,
)

/**
 * Limites de classificação configurados pelo motorista (versão 2).
 * chave = "padrao" ou "PLATAFORMA:Categoria" (ex.: "UBER:UberX").
 */
@Entity(tableName = "limite")
data class LimiteEntity(
    @PrimaryKey val chave: String,
    val kmMinimo: Double,
    val kmBom: Double,
    val horaMinimo: Double,
    val horaBom: Double,
)

/**
 * Registro das ofertas lidas da tela (versão 2), para o motorista conferir a leitura.
 * Não guarda endereços nem nomes: o texto passa pelo Anonimizador antes.
 */
@Entity(tableName = "oferta", indices = [Index("vistaEm")])
data class OfertaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plataforma: String,
    val valor: Double?,
    val minColeta: Double?,
    val kmColeta: Double?,
    val minViagem: Double?,
    val kmViagem: Double?,
    val categoria: String?,
    val nota: Double?,
    /** ALTA ou BAIXA. */
    val confianca: String,
    /** Motivos da baixa confiança, separados por " | ". */
    val alertas: String,
    /** Texto da tela já sem dados pessoais, uma linha por item. */
    val textoAnonimo: String,
    val vistaEm: Long,
)

/** Resultado de consulta: quantidade e soma de corridas num período. */
data class TotalPeriodo(
    val quantidade: Int,
    val total: Double,
)

/** Resultado de consulta: totais de corridas por jornada (para o histórico). */
data class TotalPorJornada(
    val jornadaId: Long,
    val quantidade: Int,
    val total: Double,
)
