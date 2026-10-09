package com.driverapp.leitura

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.driverapp.leitores.Leitor99
import com.driverapp.leitores.LeitorDeOferta
import com.driverapp.leitores.LeitorUber
import com.driverapp.leitores.Plataforma
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Estado compartilhado da leitura de corridas (serviço ↔ telas do app).
 * Nada daqui sai do celular.
 */
object EstadoLeitura {
    /** Última oferta analisada (para a tela de diagnóstico). */
    val ultima = MutableStateFlow<ResultadoAnalise?>(null)

    /** Último app em primeiro plano visto pelo serviço — só o NOME DO PACOTE, e só com o diagnóstico ligado. */
    val ultimoApp = MutableStateFlow<String?>(null)

    /** Pedido do app para o serviço mostrar um card de teste (com o print de referência da Uber). */
    val pedidoTeste = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** O Android está com o serviço de leitura ligado para este app? */
    fun servicoAtivo(context: Context): Boolean {
        val ativos = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val nosso = ComponentName(context, LeitorTelaService::class.java)
        return ativos.split(':').any { ComponentName.unflattenFromString(it) == nosso }
    }

    // ---------- Preferências simples da leitura (SharedPreferences) ----------

    private const val PREFS = "leitura"
    private const val CHAVE_DIAGNOSTICO = "diagnostico"
    private const val PREFIXO_PACOTE = "pacote:"

    fun diagnosticoLigado(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(CHAVE_DIAGNOSTICO, false)

    fun definirDiagnostico(context: Context, ligado: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(CHAVE_DIAGNOSTICO, ligado).apply()
        if (!ligado) ultimoApp.value = null
    }

    /** Pacotes conhecidos dos apps de motorista. Outros podem ser adicionados pelo diagnóstico. */
    private val PACOTES_PADRAO = mapOf(
        "com.ubercab.driver" to Plataforma.UBER,
        "com.app99.driver" to Plataforma.NOVENTA_E_NOVE,
        "com.taxis99.driver" to Plataforma.NOVENTA_E_NOVE,
    )

    fun pacotes(context: Context): Map<String, Plataforma> {
        val extras = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).all
            .filterKeys { it.startsWith(PREFIXO_PACOTE) }
            .mapNotNull { (k, v) ->
                val plataforma = Plataforma.entries.firstOrNull { it.name == v } ?: return@mapNotNull null
                k.removePrefix(PREFIXO_PACOTE) to plataforma
            }.toMap()
        return PACOTES_PADRAO + extras
    }

    fun adicionarPacote(context: Context, pacote: String, plataforma: Plataforma) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(PREFIXO_PACOTE + pacote, plataforma.name).apply()
    }

    // ---------- Preferências do card flutuante ----------

    private const val CHAVE_OPACIDADE = "card_opacidade"
    private const val CHAVE_X = "card_x"
    private const val CHAVE_Y = "card_y"
    private const val CHAVE_RECOLHIDO = "card_recolhido"

    fun opacidade(context: Context): Float =
        Opacidade.limitar(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat(CHAVE_OPACIDADE, Opacidade.EQUILIBRADO))

    fun definirOpacidade(context: Context, valor: Float) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putFloat(CHAVE_OPACIDADE, Opacidade.limitar(valor)).apply()
    }

    /** Posição salva do card em pixels (null = posição padrão, no topo). */
    fun posicaoCard(context: Context): Pair<Int, Int>? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(CHAVE_X) || !prefs.contains(CHAVE_Y)) return null
        return prefs.getInt(CHAVE_X, 0) to prefs.getInt(CHAVE_Y, 0)
    }

    fun salvarPosicaoCard(context: Context, x: Int, y: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(CHAVE_X, x).putInt(CHAVE_Y, y).apply()
    }

    fun restaurarPosicaoCard(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(CHAVE_X).remove(CHAVE_Y).apply()
    }

    fun cardRecolhido(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(CHAVE_RECOLHIDO, false)

    fun definirCardRecolhido(context: Context, recolhido: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(CHAVE_RECOLHIDO, recolhido).apply()
    }

    fun leitorDe(plataforma: Plataforma): LeitorDeOferta = when (plataforma) {
        Plataforma.UBER -> LeitorUber
        Plataforma.NOVENTA_E_NOVE -> Leitor99
    }
}
