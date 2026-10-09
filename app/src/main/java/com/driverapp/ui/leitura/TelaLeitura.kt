package com.driverapp.ui.leitura

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.driverapp.dados.ConfiguracaoEntity
import com.driverapp.dados.OfertaEntity
import com.driverapp.leitores.Interpretador
import com.driverapp.leitores.Leitor99
import com.driverapp.leitores.LeitorUber
import com.driverapp.leitores.Plataforma
import com.driverapp.leitura.EstadoLeitura
import com.driverapp.leitura.ExemplosReferencia
import com.driverapp.leitura.LeituraDeImagem
import com.driverapp.leitura.ProcessadorOfertas
import com.driverapp.leitura.ResultadoAnalise
import com.driverapp.repositorio
import com.driverapp.ui.Formatos
import com.driverapp.ui.LocalPaleta
import com.driverapp.ui.Medidas
import com.driverapp.ui.componentes.BotaoPrincipal
import com.driverapp.ui.componentes.BotaoSecundario
import com.driverapp.ui.componentes.CartaoVidro
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss")

/**
 * Leitura automática das ofertas: explicação clara do que é lido, ativação guiada,
 * testes com prints de referência ou com uma imagem e o registro das últimas leituras.
 */
@Composable
fun TelaLeitura(config: ConfiguracaoEntity, aoVoltar: () -> Unit) {
    val p = LocalPaleta.current
    val contexto = LocalContext.current
    val repo = contexto.repositorio
    val escopo = rememberCoroutineScope()
    val ciclo = LocalLifecycleOwner.current

    BackHandler(onBack = aoVoltar)

    var ativo by remember { mutableStateOf(EstadoLeitura.servicoAtivo(contexto)) }
    var diagnostico by remember { mutableStateOf(EstadoLeitura.diagnosticoLigado(contexto)) }
    LaunchedEffect(Unit) {
        ciclo.repeatOnLifecycle(Lifecycle.State.RESUMED) { ativo = EstadoLeitura.servicoAtivo(contexto) }
    }
    val ultimoApp by EstadoLeitura.ultimoApp.collectAsStateWithLifecycle()
    val registros by remember { repo.ofertasRecentes(20) }.collectAsStateWithLifecycle(initialValue = emptyList())

    var testes by remember { mutableStateOf<List<ResultadoAnalise>>(emptyList()) }
    var testeImagem by remember { mutableStateOf<ResultadoAnalise?>(null) }
    var avisoImagem by remember { mutableStateOf<String?>(null) }

    val escolherImagem = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        avisoImagem = "Lendo a imagem…"
        testeImagem = null
        escopo.launch {
            try {
                val linhas = LeituraDeImagem.linhas(contexto, uri)
                val melhor = ExemplosReferencia.melhorLeitura(linhas)
                if (melhor == null) {
                    avisoImagem = "Não encontrei uma oferta nesta imagem (${linhas.size} linhas de texto lidas)."
                } else {
                    testeImagem = ProcessadorOfertas.analisarComCadastro(contexto, Interpretador.validar(melhor.second))
                    avisoImagem = "${linhas.size} linhas de texto lidas na imagem."
                }
            } catch (e: Exception) {
                avisoImagem = "Não foi possível ler a imagem."
            }
        }
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(Medidas.margem),
        verticalArrangement = Arrangement.spacedBy(Medidas.espaco),
    ) {
        Text("Leitura das ofertas", style = MaterialTheme.typography.headlineSmall, color = p.texto)

        CartaoVidro(Modifier.fillMaxWidth()) {
            Text("O que o app lê", fontWeight = FontWeight.Bold, color = p.texto)
            Text(
                "Quando a Uber ou a 99 mostram uma oferta, o app lê os textos da tela — valor, tempo, distância, " +
                    "categoria e nota — e mostra um card com R$/km, R$/hora e o resultado estimado com os seus custos.",
                color = p.textoSecundario, style = MaterialTheme.typography.bodyMedium,
            )
            Text("O que o app NÃO faz", fontWeight = FontWeight.Bold, color = p.texto, modifier = Modifier.padding(top = 8.dp))
            Text(
                "• Não toca na tela, não aceita nem recusa corridas: a decisão é sempre sua.\n" +
                    "• Não lê outros apps.\n" +
                    "• Não envia nada para a internet.\n" +
                    "• Não guarda endereços nem nomes de passageiros.",
                color = p.textoSecundario, style = MaterialTheme.typography.bodyMedium,
            )
        }

        CartaoVidro(Modifier.fillMaxWidth(), brilho = if (ativo) p.verde else p.amarelo) {
            Text(
                if (ativo) "✓ Leitura ativa" else "Leitura desligada",
                fontWeight = FontWeight.Bold, color = if (ativo) p.verde else p.amarelo,
            )
            if (!ativo) {
                Text(
                    "1. Toque em \"Ativar nos ajustes do Android\".\n" +
                        "2. Procure \"Leitura de ofertas (DriverApp)\" e ative.\n" +
                        "3. Se aparecer \"Configuração restrita\" (Android 13 ou superior): toque em \"Informações do app\" " +
                        "abaixo → menu ⋮ no canto superior → \"Permitir configurações restritas\". Depois repita o passo 1.",
                    color = p.textoSecundario, style = MaterialTheme.typography.bodyMedium,
                )
            }
            BotaoPrincipal(
                if (ativo) "Abrir ajustes de acessibilidade" else "Ativar nos ajustes do Android",
                { abrir(contexto, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            if (!ativo && Build.VERSION.SDK_INT >= 33) {
                BotaoSecundario(
                    "Informações do app",
                    { abrir(contexto, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${contexto.packageName}"))) },
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }

        Text("Testar a leitura", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = p.texto)
        Text(
            "Os testes usam o combustível, os custos e os limites do seu cadastro.",
            style = MaterialTheme.typography.bodySmall, color = p.textoSecundario,
        )
        BotaoSecundario("Testar com os prints de referência", {
            escopo.launch {
                testes = listOfNotNull(
                    LeitorUber.ler(ExemplosReferencia.UBER),
                    Leitor99.ler(ExemplosReferencia.NOVENTA_E_NOVE),
                ).map { ProcessadorOfertas.analisarComCadastro(contexto, Interpretador.validar(it)) }
            }
        })
        testes.forEach { CartaoAnalise(it) }

        BotaoSecundario("Testar com uma imagem (print)", {
            escolherImagem.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        })
        avisoImagem?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = p.textoSecundario) }
        testeImagem?.let { CartaoAnalise(it) }

        Text("Diagnóstico", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = p.texto)
        CartaoVidro(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Mostrar o app em uso", fontWeight = FontWeight.SemiBold, color = p.texto)
                    Text(
                        "Ajuda a identificar o app da Uber/99 se a leitura não funcionar. Mostra só o nome técnico do app.",
                        style = MaterialTheme.typography.bodySmall, color = p.textoSecundario,
                    )
                }
                Switch(
                    checked = diagnostico,
                    onCheckedChange = {
                        diagnostico = it
                        EstadoLeitura.definirDiagnostico(contexto, it)
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = p.destaque),
                )
            }
            val app = ultimoApp
            if (diagnostico && app != null) {
                Text("Último app: $app", color = p.destaque, modifier = Modifier.padding(top = 8.dp))
                val conhecido = EstadoLeitura.pacotes(contexto)[app]
                if (conhecido != null) {
                    Text("Reconhecido como ${conhecido.nome}.", color = p.verde, style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(
                        "Abra a Uber ou a 99, volte aqui e, se o nome acima for do app da plataforma, toque no botão certo:",
                        style = MaterialTheme.typography.bodySmall, color = p.textoSecundario,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { EstadoLeitura.adicionarPacote(contexto, app, Plataforma.UBER) }) { Text("É a Uber") }
                        TextButton(onClick = { EstadoLeitura.adicionarPacote(contexto, app, Plataforma.NOVENTA_E_NOVE) }) { Text("É a 99") }
                    }
                }
            }
        }

        Text("Últimas leituras", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = p.texto)
        if (registros.isEmpty()) {
            Text(
                "Nenhuma oferta lida ainda. Com a leitura ativa, abra a Uber/99 e fique online.",
                style = MaterialTheme.typography.bodySmall, color = p.textoSecundario,
            )
        } else {
            registros.forEach { RegistroOferta(it) }
            BotaoSecundario("Apagar registros de leitura", { escopo.launch { repo.apagarRegistroOfertas() } })
        }
        BotaoSecundario("Voltar", aoVoltar)
    }
}

@Composable
private fun RegistroOferta(o: OfertaEntity) {
    val p = LocalPaleta.current
    var aberto by remember { mutableStateOf(false) }
    val plataforma = Plataforma.entries.firstOrNull { it.name == o.plataforma }?.nome ?: o.plataforma
    CartaoVidro(Modifier.fillMaxWidth(), preenchimento = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "$plataforma • ${Formatos.moeda(o.valor)} • ${o.categoria ?: "—"}",
                    fontWeight = FontWeight.SemiBold, color = p.texto,
                )
                Text(
                    HORA.format(Instant.ofEpochMilli(o.vistaEm).atZone(ZoneId.systemDefault())) +
                        "  •  coleta ${trecho(o.minColeta, o.kmColeta)}  •  viagem ${trecho(o.minViagem, o.kmViagem)}",
                    style = MaterialTheme.typography.bodySmall, color = p.textoSecundario,
                )
            }
            Pilula(if (o.confianca == "ALTA") "Confiável" else "Incerta", if (o.confianca == "ALTA") p.verde else p.amarelo)
        }
        if (o.alertas.isNotBlank()) Text(o.alertas, style = MaterialTheme.typography.bodySmall, color = p.amarelo)
        TextButton(onClick = { aberto = !aberto }) { Text(if (aberto) "Ocultar texto lido" else "Ver texto lido (sem dados pessoais)") }
        if (aberto) Text(o.textoAnonimo, style = MaterialTheme.typography.bodySmall, color = p.textoSecundario)
    }
}

private fun trecho(min: Double?, km: Double?): String =
    if (min == null || km == null) "indisponível" else "${min.toInt()} min/${Formatos.km(km)}"

private fun abrir(contexto: Context, intent: Intent) {
    try {
        contexto.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
    }
}
