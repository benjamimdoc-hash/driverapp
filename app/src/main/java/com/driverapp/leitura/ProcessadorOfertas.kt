package com.driverapp.leitura

import android.content.Context
import com.driverapp.dados.OfertaEntity
import com.driverapp.dados.criteriosPara
import com.driverapp.dados.custoCombustivelPorKm
import com.driverapp.dados.custoFixoPorHora
import com.driverapp.leitores.Anonimizador
import com.driverapp.leitores.Deduplicador
import com.driverapp.leitores.Interpretador
import com.driverapp.leitores.Plataforma
import com.driverapp.leitores.TipoLeitura
import com.driverapp.repositorio

/**
 * Recebe os textos de uma tela e decide o que fazer:
 * ler → validar → evitar duplicidade → calcular com os custos do motorista → registrar.
 * Usado pelo serviço de leitura e pelos testes dentro do app.
 */
class ProcessadorOfertas(private val context: Context) {

    private val deduplicador = Deduplicador()
    private var idUltimoRegistro: Long = 0

    /** Retorna null quando não é uma oferta ou quando é a mesma oferta já processada. */
    suspend fun processar(plataforma: Plataforma, linhas: List<String>, agoraMs: Long): ResultadoAnalise? {
        val leitor = EstadoLeitura.leitorDe(plataforma)
        val oferta = leitor.ler(linhas) ?: return null
        val leitura = Interpretador.validar(oferta)
        val tipo = deduplicador.classificar(leitura, agoraMs)
        if (tipo == TipoLeitura.REPETIDA) return null

        val resultado = analisarComCadastro(context, leitura)

        // Registro para conferência, SEM dados pessoais (endereços e nomes são removidos).
        val repo = context.repositorio
        val registro = OfertaEntity(
            id = if (tipo == TipoLeitura.ATUALIZACAO) idUltimoRegistro else 0,
            plataforma = plataforma.name,
            valor = leitura.valor,
            minColeta = leitura.coleta?.minutos,
            kmColeta = leitura.coleta?.km,
            minViagem = leitura.viagem?.minutos,
            kmViagem = leitura.viagem?.km,
            categoria = resultado.categoria,
            nota = leitura.notaPassageiro,
            confianca = leitura.confianca.name,
            alertas = leitura.alertas.joinToString(" | "),
            textoAnonimo = Anonimizador.filtrar(linhas, leitor.termosConhecidos).joinToString("\n"),
            vistaEm = agoraMs,
        )
        idUltimoRegistro = repo.registrarOferta(registro)
        EstadoLeitura.ultima.value = resultado
        return resultado
    }

    companion object {
        /** Calcula a análise com os dados reais do cadastro (combustível, custos fixos e limites). */
        suspend fun analisarComCadastro(context: Context, leitura: com.driverapp.leitores.LeituraValidada): ResultadoAnalise {
            val repo = context.repositorio
            val config = repo.obterConfig()
            val despesas = repo.despesasAtuais()
            val limites = repo.listarLimites()
            val categoria = com.driverapp.leitores.Categorias.canonica(leitura.categoria)
            return AnaliseOferta.analisar(
                leitura = leitura,
                custoCombustivelPorKm = config.custoCombustivelPorKm(),
                custoFixoPorHora = config.custoFixoPorHora(despesas),
                criterios = criteriosPara(leitura.plataforma.name, categoria, limites),
            )
        }
    }
}
