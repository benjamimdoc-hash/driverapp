package com.driverapp.dados

import com.driverapp.calculo.Jornada
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Único ponto de acesso aos dados. As telas e o serviço de GPS só falam com ele.
 * Toda alteração da jornada passa por um Mutex, para o GPS e os botões não se atropelarem.
 */
class Repositorio(private val db: BancoDados) {

    private val travaJornada = Mutex()

    // ---------- Configuração ----------

    val config: Flow<ConfiguracaoEntity> = db.configuracao().observar().map { it ?: ConfiguracaoEntity() }

    suspend fun obterConfig(): ConfiguracaoEntity = db.configuracao().obter() ?: ConfiguracaoEntity()

    /** Altera a configuração e salva na hora (o cadastro nunca perde o que já foi preenchido). */
    suspend fun alterarConfig(alteracao: (ConfiguracaoEntity) -> ConfiguracaoEntity) {
        db.configuracao().salvar(alteracao(obterConfig()))
    }

    // ---------- Despesas ----------

    val despesas: Flow<List<DespesaEntity>> = db.despesas().observar()

    suspend fun adicionarDespesa(d: DespesaEntity) { db.despesas().inserir(d) }
    suspend fun excluirDespesa(id: Long) = db.despesas().excluir(id)

    // ---------- Jornada ----------

    val jornadaAtual: Flow<JornadaEntity?> = db.jornadas().observarAtual()
    val jornadasFinalizadas: Flow<List<JornadaEntity>> = db.jornadas().observarFinalizadas()

    suspend fun obterJornadaAtual(): JornadaEntity? = db.jornadas().obterAtual()

    suspend fun iniciarJornada(agora: Long): Long = travaJornada.withLock {
        db.jornadas().obterAtual()?.let { return@withLock it.id } // já existe uma em andamento
        db.jornadas().inserir(Jornada.iniciar(agora).paraEntidade())
    }

    suspend fun pausar(agora: Long) = alterarJornada { it.pausar(agora) }
    suspend fun retomar(agora: Long) = alterarJornada { it.retomar(agora) }

    /** Finaliza e grava os custos usados, para o histórico não mudar depois. Retorna o id. */
    suspend fun finalizar(agora: Long): Long? = travaJornada.withLock {
        val atual = db.jornadas().obterAtual() ?: return@withLock null
        val cfg = obterConfig()
        val despesas = db.despesas().listar()
        val fim = atual.comDominio(atual.paraDominio().finalizar(agora)).copy(
            custoKmUsado = cfg.custoCombustivelPorKm(),
            custoFixoHoraUsado = cfg.custoFixoPorHora(despesas),
        )
        db.jornadas().atualizar(fim)
        fim.id
    }

    suspend fun obterJornada(id: Long): JornadaEntity? = db.jornadas().obter(id)

    /** Chamado pelo serviço de GPS a cada deslocamento válido. */
    suspend fun registrarDeslocamento(metros: Double) {
        val contarNaPausa = obterConfig().contarKmNaPausa
        alterarJornada { it.registrarDeslocamento(metros, contarNaPausa) }
    }

    private suspend fun alterarJornada(op: (Jornada) -> Jornada) {
        travaJornada.withLock {
            val atual = db.jornadas().obterAtual()
            if (atual != null) {
                val nova = op(atual.paraDominio())
                if (nova != atual.paraDominio()) {
                    db.jornadas().atualizar(atual.comDominio(nova))
                }
            }
        }
    }

    // ---------- Corridas ----------

    fun corridasDaJornada(jornadaId: Long): Flow<List<CorridaEntity>> = db.corridas().observarDaJornada(jornadaId)
    fun faturadoDesde(desde: Long): Flow<Double> = db.corridas().observarFaturadoDesde(desde)
    val totaisPorJornada: Flow<List<TotalPorJornada>> = db.corridas().observarTotaisPorJornada()
    suspend fun totalDaJornada(id: Long): TotalPorJornada? = db.corridas().totalDaJornada(id)

    suspend fun adicionarCorrida(c: CorridaEntity) { db.corridas().inserir(c) }
    suspend fun excluirCorrida(id: Long) = db.corridas().excluir(id)

    // ---------- Privacidade ----------

    /** Apaga TODOS os dados do motorista neste celular. */
    suspend fun apagarTudo() {
        db.corridas().apagarTudo()
        db.jornadas().apagarTudo()
        db.despesas().apagarTudo()
        db.configuracao().apagarTudo()
    }
}
