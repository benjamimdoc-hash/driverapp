package com.driverapp.jornada

import android.content.Context
import android.provider.Settings
import com.driverapp.leitura.Opacidade
import kotlinx.coroutines.flow.MutableStateFlow

/** Preferências do painel flutuante da jornada (Fase 4). Ficam só no celular. */
object PreferenciasPainel {
    private const val PREFS = "painel"
    private const val ATIVO = "ativo"
    private const val OPACIDADE = "opacidade"
    private const val X = "x"
    private const val Y = "y"

    /** Avisa o serviço quando algo muda nos ajustes (para redesenhar na hora). */
    val mudou = MutableStateFlow(0)

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun ativo(c: Context): Boolean = prefs(c).getBoolean(ATIVO, true)
    fun definirAtivo(c: Context, v: Boolean) {
        prefs(c).edit().putBoolean(ATIVO, v).apply()
        mudou.value += 1
    }

    fun opacidade(c: Context): Float = Opacidade.limitar(prefs(c).getFloat(OPACIDADE, Opacidade.EQUILIBRADO))
    fun definirOpacidade(c: Context, v: Float) {
        prefs(c).edit().putFloat(OPACIDADE, Opacidade.limitar(v)).apply()
        mudou.value += 1
    }

    fun posicao(c: Context): Pair<Int, Int>? {
        val p = prefs(c)
        if (!p.contains(X) || !p.contains(Y)) return null
        return p.getInt(X, 0) to p.getInt(Y, 0)
    }

    fun salvarPosicao(c: Context, x: Int, y: Int) {
        prefs(c).edit().putInt(X, x).putInt(Y, y).apply()
    }

    fun restaurarPosicao(c: Context) {
        prefs(c).edit().remove(X).remove(Y).apply()
        mudou.value += 1
    }

    /** O Android permite mostrar o painel por cima de outros apps? */
    fun temPermissao(c: Context): Boolean = Settings.canDrawOverlays(c)
}

/** Se o app está aberto na tela (o painel se esconde nesse caso, para não duplicar informação). */
object EstadoApp {
    val visivel = MutableStateFlow(false)
}
