package com.driverapp.jornada

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.driverapp.calculo.EstadoJornada
import com.driverapp.calculo.FiltroGps
import com.driverapp.calculo.PontoGps
import com.driverapp.dados.JornadaEntity
import com.driverapp.dados.paraDominio
import com.driverapp.repositorio
import com.driverapp.ui.Formatos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Serviço em primeiro plano que mede os km da jornada pelo GPS.
 *
 * - Fica ligado enquanto existir jornada ativa ou pausada (com notificação fixa, exigida pelo Android).
 * - Liga o GPS só quando precisa: jornada ativa, ou pausada com "km na pausa" ligado.
 * - Usa o GPS do próprio Android (LocationManager), sem depender do Google Play Services —
 *   funciona também em celulares sem serviços Google.
 * - Desliga sozinho quando a jornada é finalizada.
 */
class JornadaService : Service() {

    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val gerenciador by lazy { getSystemService(LocationManager::class.java) }

    private var observador: Job? = null
    private var vigia: Job? = null
    private var gpsLigado = false
    private var referencia: PontoGps? = null
    private var ultimoSinalMs = 0L
    private var gpsLigadoEmMs = 0L
    private var ultimaJornada: JornadaEntity? = null

    private val ouvinte = LocationListener { local -> aoReceberLocal(local) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!entrarEmPrimeiroPlano()) {
            // O Android não deixou (ex.: reiniciado em segundo plano): avisa o motorista para abrir o app.
            Notificacoes.avisoRetomar(this)
            stopSelf()
            return START_NOT_STICKY
        }
        if (observador == null) {
            observador = escopo.launch {
                combine(repositorio.jornadaAtual, repositorio.config) { j, c -> j to c.contarKmNaPausa }
                    .collect { (jornada, contarNaPausa) -> reagir(jornada, contarNaPausa) }
            }
        }
        if (vigia == null) {
            // Atualiza a notificação a cada 30 s (tempo e aviso de "sem sinal de GPS").
            vigia = escopo.launch {
                while (isActive) {
                    delay(30_000)
                    atualizarNotificacao()
                }
            }
        }
        return START_STICKY
    }

    private fun entrarEmPrimeiroPlano(): Boolean = try {
        ServiceCompat.startForeground(
            this,
            Notificacoes.ID_JORNADA,
            Notificacoes.jornada(this, "Jornada em andamento", "Preparando o GPS…"),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
        )
        true
    } catch (_: Exception) {
        false
    }

    private fun reagir(jornada: JornadaEntity?, contarNaPausa: Boolean) {
        ultimaJornada = jornada
        if (jornada == null) {
            encerrar()
            return
        }
        val precisaGps = jornada.paraDominio().precisaGps(contarNaPausa)
        if (precisaGps && !gpsLigado) ligarGps()
        if (!precisaGps && gpsLigado) desligarGps()
        atualizarNotificacao()
    }

    private fun ligarGps() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        val provedor = when {
            gerenciador.allProviders.contains(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            gerenciador.allProviders.contains(LocationManager.FUSED_PROVIDER) -> LocationManager.FUSED_PROVIDER
            else -> return
        }
        try {
            gerenciador.requestLocationUpdates(provedor, 2_000L, 5f, ouvinte, Looper.getMainLooper())
            gpsLigado = true
            referencia = null
            gpsLigadoEmMs = SystemClock.elapsedRealtime()
        } catch (_: SecurityException) {
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun desligarGps() {
        if (!gpsLigado) return
        gerenciador.removeUpdates(ouvinte)
        gpsLigado = false
        referencia = null
    }

    private fun aoReceberLocal(local: Location) {
        ultimoSinalMs = SystemClock.elapsedRealtime()
        val ponto = PontoGps(
            latitude = local.latitude,
            longitude = local.longitude,
            precisaoM = if (local.hasAccuracy()) local.accuracy else null,
            tempoMs = local.elapsedRealtimeNanos / 1_000_000,
        )
        val avaliacao = FiltroGps.avaliar(referencia, ponto)
        if (!avaliacao.aceito) return
        referencia = ponto
        if (avaliacao.metros > 0) {
            escopo.launch { repositorio.registrarDeslocamento(avaliacao.metros) }
        }
    }

    private fun atualizarNotificacao() {
        val j = ultimaJornada ?: return
        val dominio = j.paraDominio()
        val agora = System.currentTimeMillis()
        val estado = if (dominio.estado == EstadoJornada.ATIVA) "Jornada ativa" else "Jornada pausada"
        val semSinal = gpsLigado &&
            SystemClock.elapsedRealtime() - maxOf(ultimoSinalMs, gpsLigadoEmMs) > 60_000
        val texto = buildString {
            append(Formatos.duracao(dominio.msProdutivos(agora)))
            append("  •  ")
            append(Formatos.km(dominio.km))
            if (semSinal) append("  •  sem sinal de GPS")
            if (!gpsLigado && dominio.estado == EstadoJornada.PAUSADA) append("  •  km pausado")
        }
        try {
            NotificationManagerCompat.from(this)
                .notify(Notificacoes.ID_JORNADA, Notificacoes.jornada(this, estado, texto))
        } catch (_: SecurityException) {
        }
    }

    private fun encerrar() {
        desligarGps()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        desligarGps()
        escopo.cancel()
        super.onDestroy()
    }

    companion object {
        /** Liga o serviço. Chamar com o app aberto e a permissão de localização concedida. */
        fun iniciar(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, JornadaService::class.java))
            } catch (_: Exception) {
                // Android pode negar em segundo plano; o app tenta de novo ao ser aberto.
            }
        }

        fun temPermissaoLocal(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
}
