package com.driverapp.ui.ajustes

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.driverapp.jornada.PreferenciasPainel
import com.driverapp.leitura.Opacidade
import com.driverapp.ui.LocalPaleta
import com.driverapp.ui.Medidas
import com.driverapp.ui.componentes.BotaoPrincipal
import com.driverapp.ui.componentes.BotaoSecundario
import com.driverapp.ui.componentes.CartaoVidro
import com.driverapp.ui.componentes.SeletorSegmentado
import com.driverapp.ui.componentes.SliderComValor

/** Ajustes do painel flutuante da jornada (Fase 4). */
@Composable
fun TelaPainelFlutuante(aoVoltar: () -> Unit) {
    val p = LocalPaleta.current
    val contexto = LocalContext.current
    val ciclo = LocalLifecycleOwner.current
    BackHandler(onBack = aoVoltar)

    var ativo by remember { mutableStateOf(PreferenciasPainel.ativo(contexto)) }
    var permissao by remember { mutableStateOf(PreferenciasPainel.temPermissao(contexto)) }
    var opacidade by remember { mutableFloatStateOf(PreferenciasPainel.opacidade(contexto)) }
    var aviso by remember { mutableStateOf<String?>(null) }

    // Rechecar a permissão quando o motorista volta dos ajustes do Android.
    LaunchedEffect(Unit) {
        ciclo.repeatOnLifecycle(Lifecycle.State.RESUMED) { permissao = PreferenciasPainel.temPermissao(contexto) }
    }

    fun mudarOpacidade(v: Float) {
        opacidade = Opacidade.limitar(v)
        PreferenciasPainel.definirOpacidade(contexto, opacidade)
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(Medidas.margem),
        verticalArrangement = Arrangement.spacedBy(Medidas.espaco),
    ) {
        Text("Painel flutuante", style = MaterialTheme.typography.headlineSmall, color = p.texto)
        Text(
            "Um ícone pequeno por cima dos apps durante a jornada. Toque para abrir: tempo, km, corridas, resultado, " +
                "meta do dia, pausar/retomar/finalizar e ajuste rápido do combustível. Arraste para mudar de lugar. " +
                "Ele some quando o app está aberto e quando a jornada termina.",
            color = p.textoSecundario, style = MaterialTheme.typography.bodyMedium,
        )

        CartaoVidro(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Mostrar o painel", fontWeight = FontWeight.SemiBold, color = p.texto)
                    Text(
                        "Aparece só com a jornada aberta (e com o GPS permitido).",
                        style = MaterialTheme.typography.bodySmall, color = p.textoSecundario,
                    )
                }
                Switch(
                    checked = ativo,
                    onCheckedChange = {
                        ativo = it
                        PreferenciasPainel.definirAtivo(contexto, it)
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = p.destaque),
                )
            }
        }

        CartaoVidro(Modifier.fillMaxWidth(), brilho = if (permissao) p.verde else p.amarelo) {
            Text(
                if (permissao) "✓ Permissão de sobreposição concedida" else "Falta permitir \"Sobrepor a outros apps\"",
                fontWeight = FontWeight.SemiBold, color = if (permissao) p.verde else p.amarelo,
            )
            if (!permissao) {
                Text(
                    "Na próxima tela, ative a opção para este app e volte.",
                    style = MaterialTheme.typography.bodySmall, color = p.textoSecundario,
                )
                BotaoPrincipal("Permitir", {
                    try {
                        contexto.startActivity(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${contexto.packageName}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (_: Exception) {
                    }
                }, Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        }

        CartaoVidro(Modifier.fillMaxWidth()) {
            Text("Transparência do fundo", fontWeight = FontWeight.SemiBold, color = p.texto)
            SeletorSegmentado(
                opcoes = Opacidade.niveis.map { it.first },
                selecionado = Opacidade.nivelMaisProximo(opacidade),
                aoMudar = { i -> mudarOpacidade(Opacidade.niveis[i].second) },
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        SliderComValor(
            rotulo = "Ajuste fino",
            valor = opacidade,
            faixa = Opacidade.MINIMA..1f,
            passo = 0.05f,
            formatar = { "${(it * 100).toInt()}%" },
            aoMudar = { mudarOpacidade(it) },
        )

        BotaoSecundario("Voltar o painel para a posição inicial", {
            PreferenciasPainel.restaurarPosicao(contexto)
            aviso = "Posição restaurada."
        })
        aviso?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = p.destaque) }
        BotaoSecundario("Voltar", aoVoltar)
    }
}
