package com.driverapp.leitura

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.driverapp.leitores.Plataforma
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Serviço de leitura das ofertas (Serviço de Acessibilidade do Android).
 *
 * O QUE FAZ: quando a Uber ou a 99 mostram uma oferta, lê os TEXTOS visíveis (valor, tempo,
 * distância, categoria, nota), calcula com os custos do motorista e mostra um card por cima.
 *
 * O QUE NÃO FAZ: não toca na tela, não aceita nem recusa corridas, não lê outros apps
 * (eventos de outros apps são descartados na hora) e não envia nada para a internet.
 */
class LeitorTelaService : AccessibilityService() {

    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var processador: ProcessadorOfertas
    private var cartao: CartaoFlutuante? = null
    private var pacotes: Map<String, Plataforma> = emptyMap()
    private var ultimoProcessamentoMs = 0L
    private var ultimaAtualizacaoPacotesMs = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        processador = ProcessadorOfertas(applicationContext)
        cartao = CartaoFlutuante(this)
        pacotes = EstadoLeitura.pacotes(this)
        // "Mostrar card de teste" nos Ajustes: usa o print de referência da Uber com os custos reais.
        escopo.launch {
            EstadoLeitura.pedidoTeste.collect {
                val oferta = com.driverapp.leitores.LeitorUber.ler(ExemplosReferencia.UBER) ?: return@collect
                val r = ProcessadorOfertas.analisarComCadastro(applicationContext, com.driverapp.leitores.Interpretador.validar(oferta))
                cartao?.mostrar(r)
            }
        }
    }

    override fun onAccessibilityEvent(evento: AccessibilityEvent?) {
        val pacote = evento?.packageName?.toString() ?: return
        val agora = SystemClock.uptimeMillis()

        // Recarrega a lista de apps lidos de vez em quando (pode ter sido alterada no diagnóstico).
        if (agora - ultimaAtualizacaoPacotesMs > 10_000) {
            pacotes = EstadoLeitura.pacotes(this)
            ultimaAtualizacaoPacotesMs = agora
        }
        val plataforma = pacotes[pacote]
        if (plataforma == null) {
            // Outro app: nada é lido. Com o diagnóstico ligado, guardamos só o nome do pacote.
            if (pacote != packageName && pacote != "com.android.systemui" && EstadoLeitura.diagnosticoLigado(this)) {
                EstadoLeitura.ultimoApp.value = pacote
            }
            return
        }
        if (EstadoLeitura.diagnosticoLigado(this)) EstadoLeitura.ultimoApp.value = pacote

        // A tela da oferta gera muitos eventos seguidos; processamos no máximo a cada 300 ms.
        if (agora - ultimoProcessamentoMs < 300) return
        ultimoProcessamentoMs = agora

        val linhas = coletarTextos(pacote)
        if (linhas.isEmpty()) return
        escopo.launch {
            val resultado = processador.processar(plataforma, linhas, System.currentTimeMillis()) ?: return@launch
            if (resultado.leitura.utilizavel) cartao?.mostrar(resultado)
        }
    }

    /** Junta os textos visíveis de todas as janelas do app da plataforma, em ordem de leitura. */
    private fun coletarTextos(pacote: String): List<String> {
        val linhas = mutableListOf<String>()
        val raizes = try {
            windows.mapNotNull { it.root }.filter { it.packageName?.toString() == pacote }
        } catch (_: Exception) {
            emptyList()
        }.ifEmpty { listOfNotNull(rootInActiveWindow?.takeIf { it.packageName?.toString() == pacote }) }

        raizes.forEach { visitar(it, linhas, 0) }
        return linhas
    }

    private fun visitar(no: AccessibilityNodeInfo?, linhas: MutableList<String>, profundidade: Int) {
        if (no == null || profundidade > 40 || linhas.size > 300) return
        if (no.isVisibleToUser) {
            val texto = no.text?.toString()?.trim().orEmpty()
            val descricao = no.contentDescription?.toString()?.trim().orEmpty()
            if (texto.isNotEmpty() && linhas.lastOrNull() != texto) linhas += texto
            if (descricao.isNotEmpty() && descricao != texto && linhas.lastOrNull() != descricao) linhas += descricao
        }
        for (i in 0 until no.childCount) visitar(no.getChild(i), linhas, profundidade + 1)
    }

    override fun onInterrupt() {
        cartao?.esconder()
    }

    override fun onDestroy() {
        cartao?.esconder()
        escopo.cancel()
        super.onDestroy()
    }
}
