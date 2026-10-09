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
