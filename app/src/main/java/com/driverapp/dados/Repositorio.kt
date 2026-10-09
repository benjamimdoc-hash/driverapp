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

    suspend fun despesasAtuais(): List<DespesaEntity> = db.despesas().listar()
    suspend fun adicionarDespesa(d: DespesaEntity): Long = db.despesas().inserir(d)
    suspend fun excluirDespesa(id: Long) = db.despesas().excluir(id)

    /**
     * Salvamento automático de uma despesa (etapa de custos adicionais).
     *  - id = 0: cria; id > 0: atualiza a mesma linha (nunca duplica).
     *  - valor vazio ou zero: remove a despesa.
     * Retorna o id da linha (0 se foi removida).
     */
    suspend fun salvarDespesa(d: DespesaEntity): Long {
        if (d.valor <= 0) {
            if (d.id > 0) db.despesas().excluir(d.id)
            return 0
        }
        if (d.id == 0L) return db.despesas().inserir(d)
        db.despesas().salvar(d)
        return d.id
    }

    // ---------- Jornada ----------

    val jornadaAtual: Flow<JornadaEntity?> = db.jornadas().observarAtual()
    val jornadasFinalizadas: Flow<List<JornadaEntity>> = db.jornadas().observarFinalizadas()

    suspend fun obterJornadaAtual(): JornadaEntity? = db.jornadas().obterAtual()

    fun jornadasNoPeriodo(inicio: Long, fim: Long): Flow<List<JornadaEntity>> = db.jornadas().observarNoPeriodo(inicio, fim)

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
    fun totalPeriodo(inicio: Long, fim: Long): Flow<TotalPeriodo> = db.corridas().observarTotalPeriodo(inicio, fim)
    val totaisPorJornada: Flow<List<TotalPorJornada>> = db.corridas().observarTotaisPorJornada()
    suspend fun totalDaJornada(id: Long): TotalPorJornada? = db.corridas().totalDaJornada(id)

    suspend fun adicionarCorrida(c: CorridaEntity) { db.corridas().inserir(c) }
    suspend fun excluirCorrida(id: Long) = db.corridas().excluir(id)

    // ---------- Limites de classificação ----------

    val limites: Flow<List<LimiteEntity>> = db.limites().observar()
    suspend fun listarLimites(): List<LimiteEntity> = db.limites().listar()
    suspend fun salvarLimite(l: LimiteEntity) = db.limites().salvar(l)
    suspend fun excluirLimite(chave: String) = db.limites().excluir(chave)

    // ---------- Registro de ofertas lidas ----------

    fun ofertasRecentes(limite: Int = 30): Flow<List<OfertaEntity>> = db.ofertas().observarRecentes(limite)

    /** Grava (ou atualiza, quando é a mesma oferta) e mantém só os 200 registros mais recentes. */
    suspend fun registrarOferta(o: OfertaEntity): Long {
        val id = if (o.id > 0) {
            db.ofertas().atualizar(o)
            o.id
        } else db.ofertas().inserir(o)
        db.ofertas().podar(200)
        return id
    }

    suspend fun apagarRegistroOfertas() = db.ofertas().apagarTudo()

    // ---------- Saldos das plataformas ----------

    fun saldosAceitosDesde(desde: Long): Flow<List<SaldoEntity>> = db.saldos().observarAceitosDesde(desde)
    val saldosPendentes: Flow<List<SaldoEntity>> = db.saldos().observarPendentes()
    fun historicoSaldos(limite: Int = 40): Flow<List<SaldoEntity>> = db.saldos().observarHistorico(limite)
    fun corridasPorPlataforma(inicio: Long, fim: Long): Flow<List<TotalPlataforma>> = db.corridas().observarPorPlataforma(inicio, fim)

    suspend fun ultimoSaldoAceito(plataforma: String, semanal: Boolean, periodoInicio: Long): SaldoEntity? =
        db.saldos().ultimoAceito(plataforma, semanal, periodoInicio)

    suspend fun ultimoSaldoPendente(plataforma: String): SaldoEntity? = db.saldos().ultimoPendente(plataforma)
    suspend fun inserirSaldo(s: SaldoEntity): Long = db.saldos().inserir(s)
    suspend fun confirmarSaldo(id: Long) = db.saldos().mudarEstado(id, "ACEITO")
    suspend fun descartarSaldo(id: Long) = db.saldos().mudarEstado(id, "DESCARTADO")

    // ---------- Privacidade ----------

    /** Apaga TODOS os dados do motorista neste celular. */
    suspend fun apagarTudo() {
        db.saldos().apagarTudo()
        db.ofertas().apagarTudo()
        db.limites().apagarTudo()
        db.corridas().apagarTudo()
        db.jornadas().apagarTudo()
        db.despesas().apagarTudo()
        db.configuracao().apagarTudo()
    }
}
