package com.driverapp.ui.ajustes

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

/**
 * Guia para liberar o app da economia de bateria.
 * Cada fabricante esconde essa opção num lugar diferente; mostramos o caminho da marca do celular.
 */
@Composable
fun GuiaBateria(aoVoltar: () -> Unit) {
    val contexto = LocalContext.current
    val ciclo = LocalLifecycleOwner.current
    var liberado by remember { mutableStateOf(estaLiberado(contexto)) }

    // Rechecar quando o motorista volta das configurações do Android.
    LaunchedEffect(Unit) {
        ciclo.repeatOnLifecycle(Lifecycle.State.RESUMED) { liberado = estaLiberado(contexto) }
    }

    val marca = Build.MANUFACTURER.lowercase()
    val passos = when {
        "samsung" in marca -> listOf(
            "Toque em \"Abrir ajustes da bateria do app\" abaixo.",
            "Em Bateria, escolha \"Sem restrições\".",
            "Depois, em Configurações → Bateria → Limites de uso em segundo plano, confira se o app NÃO está em \"Apps em suspensão\".",
        )
        "xiaomi" in marca || "redmi" in marca || "poco" in marca -> listOf(
            "Toque em \"Abrir ajustes da bateria do app\" abaixo.",
            "Em Economia de bateria, escolha \"Sem restrições\".",
            "Ative também \"Início automático\" (Autostart) nas permissões do app.",
            "Na tela de apps recentes, segure o app e toque no cadeado para ele não ser fechado.",
        )
        "motorola" in marca || "lenovo" in marca -> listOf(
            "Toque em \"Abrir ajustes da bateria do app\" abaixo.",
            "Em Bateria, escolha \"Sem restrições\" (ou desative \"Otimizar\").",
        )
        "huawei" in marca || "honor" in marca -> listOf(
            "Toque em \"Abrir ajustes da bateria do app\" abaixo.",
            "Em Inicialização de apps, desative \"Gerenciar automaticamente\" e ative as três opções manuais.",
        )
        else -> listOf(
            "Toque em \"Abrir ajustes da bateria do app\" abaixo.",
            "Procure \"Bateria\" e escolha \"Sem restrições\" ou \"Não otimizar\".",
        )
    }

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Funcionar com a tela apagada", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Muitos celulares fecham apps em segundo plano para economizar bateria. Isso pode parar a contagem de km " +
                "durante a jornada. Libere o app para ele continuar funcionando.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (liberado) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
            ),
        ) {
            Text(
                if (liberado) "✓ Liberado: o Android não vai restringir o app." else "Ainda restrito pela economia de bateria.",
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text("Seu celular: ${Build.MANUFACTURER} ${Build.MODEL}", style = MaterialTheme.typography.labelLarge)
        passos.forEachIndexed { i, passo -> Text("${i + 1}. $passo") }
        Button(onClick = { abrirAjustesBateria(contexto) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Abrir ajustes da bateria do app")
        }
        OutlinedButton(onClick = aoVoltar, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Voltar") }
    }
}

private fun estaLiberado(contexto: Context): Boolean =
    contexto.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(contexto.packageName)

/** Abre a tela de detalhes do app (funciona em todas as marcas); de lá se chega em Bateria. */
private fun abrirAjustesBateria(contexto: Context) {
    val detalhes = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${contexto.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        contexto.startActivity(detalhes)
    } catch (_: Exception) {
        try {
            contexto.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
    }
}
