package com.driverapp.leitura

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.driverapp.leitores.Leitor99
import com.driverapp.leitores.LeitorDeOferta
import com.driverapp.leitores.LeitorUber
import com.driverapp.leitores.Plataforma
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

    fun leitorDe(plataforma: Plataforma): LeitorDeOferta = when (plataforma) {
        Plataforma.UBER -> LeitorUber
        Plataforma.NOVENTA_E_NOVE -> Leitor99
    }
}
