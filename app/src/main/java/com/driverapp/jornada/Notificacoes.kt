package com.driverapp.jornada

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.driverapp.MainActivity
import com.driverapp.R

object Notificacoes {
    const val CANAL_JORNADA = "jornada"
    const val CANAL_AVISOS = "avisos"
    const val ID_JORNADA = 1001
    const val ID_RETOMAR = 1002

    fun criarCanais(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CANAL_JORNADA, "Jornada em andamento", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Mostra tempo e km enquanto a jornada está aberta"
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CANAL_AVISOS, "Avisos", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Avisos importantes, como retomar a jornada após reiniciar o celular"
            }
        )
    }

    private fun abrirApp(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    /** Notificação fixa exigida pelo Android enquanto o GPS registra a jornada. */
    fun jornada(context: Context, titulo: String, texto: String): Notification =
        NotificationCompat.Builder(context, CANAL_JORNADA)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(abrirApp(context))
            .build()

    /** Aviso para retomar o registro de km (ex.: depois de reiniciar o celular). */
    fun avisoRetomar(context: Context) {
        if (!podeNotificar(context)) return
        val n = NotificationCompat.Builder(context, CANAL_AVISOS)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setContentTitle("Jornada em andamento")
            .setContentText("Toque para continuar registrando os km.")
            .setAutoCancel(true)
            .setContentIntent(abrirApp(context))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(ID_RETOMAR, n)
        } catch (_: SecurityException) {
        }
    }

    fun podeNotificar(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED ||
            android.os.Build.VERSION.SDK_INT < 33
}
