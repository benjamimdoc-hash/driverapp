package com.driverapp.jornada

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.driverapp.repositorio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Depois que o celular reinicia (ou o app é atualizado), o Android não permite religar o GPS
 * sozinho em segundo plano. Então, se havia jornada aberta, avisamos o motorista com uma
 * notificação: ao tocar, o app abre e volta a registrar os km.
 * O tempo da jornada não se perde (é calculado por horário salvo no banco).
 */
class ReinicioReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pendente = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (app.repositorio.obterJornadaAtual() != null) Notificacoes.avisoRetomar(app)
            } finally {
                pendente.finish()
            }
        }
    }
}
