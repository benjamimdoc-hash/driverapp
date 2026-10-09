package com.driverapp.dados

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfiguracaoDao {
    @Query("SELECT * FROM configuracao WHERE id = 1")
    fun observar(): Flow<ConfiguracaoEntity?>

    @Query("SELECT * FROM configuracao WHERE id = 1")
    suspend fun obter(): ConfiguracaoEntity?

    @Upsert
    suspend fun salvar(config: ConfiguracaoEntity)

    @Query("DELETE FROM configuracao")
    suspend fun apagarTudo()
}

@Dao
interface DespesaDao {
    @Query("SELECT * FROM despesa ORDER BY id")
    fun observar(): Flow<List<DespesaEntity>>

    @Query("SELECT * FROM despesa ORDER BY id")
    suspend fun listar(): List<DespesaEntity>

    @Insert
    suspend fun inserir(despesa: DespesaEntity): Long

    @Upsert
    suspend fun salvar(despesa: DespesaEntity)

    @Query("SELECT * FROM despesa WHERE categoria = :categoria ORDER BY id LIMIT 1")
    suspend fun porCategoria(categoria: String): DespesaEntity?

    @Query("DELETE FROM despesa WHERE id = :id")
    suspend fun excluir(id: Long)

    @Query("DELETE FROM despesa")
    suspend fun apagarTudo()
}

@Dao
interface JornadaDao {
    /** A jornada em andamento (ativa ou pausada), se houver. */
    @Query("SELECT * FROM jornada WHERE estado != 'FINALIZADA' ORDER BY id DESC LIMIT 1")
    fun observarAtual(): Flow<JornadaEntity?>

    @Query("SELECT * FROM jornada WHERE estado != 'FINALIZADA' ORDER BY id DESC LIMIT 1")
    suspend fun obterAtual(): JornadaEntity?

    @Query("SELECT * FROM jornada WHERE id = :id")
    suspend fun obter(id: Long): JornadaEntity?

    @Query("SELECT * FROM jornada WHERE estado = 'FINALIZADA' ORDER BY inicioEm DESC")
    fun observarFinalizadas(): Flow<List<JornadaEntity>>

    /** Jornadas que começaram dentro do período [inicio, fim). */
    @Query("SELECT * FROM jornada WHERE inicioEm >= :inicio AND inicioEm < :fim")
    fun observarNoPeriodo(inicio: Long, fim: Long): Flow<List<JornadaEntity>>

    @Insert
    suspend fun inserir(jornada: JornadaEntity): Long

    @Update
    suspend fun atualizar(jornada: JornadaEntity)

    @Query("DELETE FROM jornada")
    suspend fun apagarTudo()
}

@Dao
interface CorridaDao {
    @Query("SELECT * FROM corrida WHERE jornadaId = :jornadaId ORDER BY criadaEm DESC")
    fun observarDaJornada(jornadaId: Long): Flow<List<CorridaEntity>>

    @Query("SELECT COALESCE(SUM(valor), 0) FROM corrida WHERE criadaEm >= :desde")
    fun observarFaturadoDesde(desde: Long): Flow<Double>

    @Query("SELECT COUNT(*) AS quantidade, COALESCE(SUM(valor), 0) AS total FROM corrida WHERE criadaEm >= :inicio AND criadaEm < :fim")
    fun observarTotalPeriodo(inicio: Long, fim: Long): Flow<TotalPeriodo>

    @Query("SELECT plataforma, COUNT(*) AS quantidade, COALESCE(SUM(valor), 0) AS total FROM corrida WHERE criadaEm >= :inicio AND criadaEm < :fim GROUP BY plataforma")
    fun observarPorPlataforma(inicio: Long, fim: Long): Flow<List<TotalPlataforma>>

    @Query("SELECT jornadaId, COUNT(*) AS quantidade, COALESCE(SUM(valor), 0) AS total FROM corrida WHERE jornadaId IS NOT NULL GROUP BY jornadaId")
    fun observarTotaisPorJornada(): Flow<List<TotalPorJornada>>

    @Query("SELECT jornadaId, COUNT(*) AS quantidade, COALESCE(SUM(valor), 0) AS total FROM corrida WHERE jornadaId = :jornadaId GROUP BY jornadaId")
    suspend fun totalDaJornada(jornadaId: Long): TotalPorJornada?

    @Insert
    suspend fun inserir(corrida: CorridaEntity): Long

    @Query("DELETE FROM corrida WHERE id = :id")
    suspend fun excluir(id: Long)

    @Query("DELETE FROM corrida")
    suspend fun apagarTudo()
}

@Dao
interface LimiteDao {
    @Query("SELECT * FROM limite")
    fun observar(): Flow<List<LimiteEntity>>

    @Query("SELECT * FROM limite")
    suspend fun listar(): List<LimiteEntity>

    @Upsert
    suspend fun salvar(limite: LimiteEntity)

    @Query("DELETE FROM limite WHERE chave = :chave")
    suspend fun excluir(chave: String)

    @Query("DELETE FROM limite")
    suspend fun apagarTudo()
}

@Dao
interface OfertaDao {
    @Query("SELECT * FROM oferta ORDER BY vistaEm DESC LIMIT :limite")
    fun observarRecentes(limite: Int): Flow<List<OfertaEntity>>

    @Insert
    suspend fun inserir(oferta: OfertaEntity): Long

    @Update
    suspend fun atualizar(oferta: OfertaEntity)

    /** Mantém só os registros mais recentes (o resto é apagado). */
    @Query("DELETE FROM oferta WHERE id NOT IN (SELECT id FROM oferta ORDER BY vistaEm DESC LIMIT :manter)")
    suspend fun podar(manter: Int)

    @Query("DELETE FROM oferta")
    suspend fun apagarTudo()
}

@Dao
interface SaldoDao {
    /** Leituras aceitas a partir de uma data (para os totais da dashboard). */
    @Query("SELECT * FROM saldo WHERE estado = 'ACEITO' AND lidoEm >= :desde ORDER BY lidoEm")
    fun observarAceitosDesde(desde: Long): Flow<List<SaldoEntity>>

    @Query("SELECT * FROM saldo WHERE estado = 'PENDENTE' ORDER BY lidoEm DESC")
    fun observarPendentes(): Flow<List<SaldoEntity>>

    @Query("SELECT * FROM saldo ORDER BY lidoEm DESC LIMIT :limite")
    fun observarHistorico(limite: Int): Flow<List<SaldoEntity>>

    @Query("SELECT * FROM saldo WHERE plataforma = :plataforma AND semanal = :semanal AND periodoInicio = :periodoInicio AND estado = 'ACEITO' ORDER BY lidoEm DESC LIMIT 1")
    suspend fun ultimoAceito(plataforma: String, semanal: Boolean, periodoInicio: Long): SaldoEntity?

    @Query("SELECT * FROM saldo WHERE plataforma = :plataforma AND estado = 'PENDENTE' ORDER BY lidoEm DESC LIMIT 1")
    suspend fun ultimoPendente(plataforma: String): SaldoEntity?

    @Insert
    suspend fun inserir(s: SaldoEntity): Long

    @Query("UPDATE saldo SET estado = :estado WHERE id = :id")
    suspend fun mudarEstado(id: Long, estado: String)

    @Query("DELETE FROM saldo")
    suspend fun apagarTudo()
}
