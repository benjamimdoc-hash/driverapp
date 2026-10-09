package com.driverapp.ui.ajustes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.driverapp.calculo.CriteriosClassificacao
import com.driverapp.dados.CHAVE_LIMITE_PADRAO
import com.driverapp.dados.ConfiguracaoEntity
import com.driverapp.dados.LimiteEntity
import com.driverapp.dados.chaveLimite
import com.driverapp.dados.criteriosPara
import com.driverapp.dados.listaPlataformas
import com.driverapp.leitores.Categorias
import com.driverapp.leitores.Plataforma
import com.driverapp.repositorio
import com.driverapp.ui.Formatos
import com.driverapp.ui.LocalPaleta
import com.driverapp.ui.Medidas
import com.driverapp.ui.componentes.BotaoPrincipal
import com.driverapp.ui.componentes.BotaoSecundario
import com.driverapp.ui.componentes.CartaoVidro
import com.driverapp.ui.componentes.EstadoSalvamento
import com.driverapp.ui.componentes.IndicadorSalvamento
import com.driverapp.ui.componentes.SliderComValor
import com.driverapp.ui.cadastro.Plataformas
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Limites que definem as cores (vermelho / amarelo / verde) da dashboard e do card da corrida.
 * Há um limite PADRÃO e, opcionalmente, um por plataforma e categoria (UberX ≠ Black).
 */
@Composable
fun TelaLimites(config: ConfiguracaoEntity, aoVoltar: () -> Unit) {
    val p = LocalPaleta.current
    val repo = LocalContext.current.repositorio
    val limites by repo.limites.collectAsStateWithLifecycle(initialValue = emptyList())
    var editando by rememberSaveable { mutableStateOf<String?>(null) }

    BackHandler { if (editando != null) editando = null else aoVoltar() }

    val chave = editando
    if (chave != null) {
        val partes = chave.split(":")
        val titulo = if (chave == CHAVE_LIMITE_PADRAO) "Padrão (todas as corridas)"
        else "${Plataformas.nome(partes[0])} • ${partes.getOrElse(1) { "" }}"
        EditorLimite(
            chave = chave,
            titulo = titulo,
            atual = limites.firstOrNull { it.chave == chave },
            sugestao = criteriosPara(partes.getOrNull(0), partes.getOrNull(1), limites.filter { it.chave == CHAVE_LIMITE_PADRAO }),
            aoVoltar = { editando = null },
        )
        return
    }

    val porChave = limites.associateBy { it.chave }
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(Medidas.margem),
        verticalArrangement = Arrangement.spacedBy(Medidas.espaco),
    ) {
        Text("Limites das cores", style = MaterialTheme.typography.headlineSmall, color = p.texto)
        Text(
            "Você define o que é uma corrida ruim, razoável ou boa. Os valores iniciais são apenas sugestões " +
                "para começar — não são regras do mercado.",
            color = p.textoSecundario,
        )
        ItemLimite("Padrão (todas as corridas)", porChave[CHAVE_LIMITE_PADRAO]) { editando = CHAVE_LIMITE_PADRAO }

        val plataformas = config.listaPlataformas().mapNotNull { nome -> Plataforma.entries.firstOrNull { it.name == nome } }
        plataformas.forEach { plat ->
            Text(
                Plataformas.nome(plat.name), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, color = p.texto, modifier = Modifier.padding(top = 8.dp),
            )
            Categorias.daPlataforma(plat).forEach { cat ->
                val k = chaveLimite(plat.name, cat)
                ItemLimite(cat, porChave[k]) { editando = k }
            }
        }
        if (plataformas.isEmpty()) {
            Text(
                "A leitura automática funciona com Uber e 99. Marque essas plataformas no seu perfil para ajustar por categoria.",
                style = MaterialTheme.typography.bodySmall, color = p.textoSecundario,
            )
        }
        BotaoSecundario("Voltar", aoVoltar)
    }
}

@Composable
private fun ItemLimite(titulo: String, limite: LimiteEntity?, aoTocar: () -> Unit) {
    val p = LocalPaleta.current
    CartaoVidro(Modifier.fillMaxWidth().clickable(onClick = aoTocar), preenchimento = 16.dp) {
        Text(titulo, fontWeight = FontWeight.SemiBold, color = p.texto)
        Text(
            if (limite == null) "Usando a sugestão inicial"
            else "R$/km: ${Formatos.moeda(limite.kmMinimo)} a ${Formatos.moeda(limite.kmBom)} • " +
                "R$/h: ${Formatos.moeda(limite.horaMinimo)} a ${Formatos.moeda(limite.horaBom)}",
            style = MaterialTheme.typography.bodySmall, color = if (limite == null) p.textoSecundario else p.destaque,
        )
    }
}

@Composable
private fun EditorLimite(
    chave: String,
    titulo: String,
    atual: LimiteEntity?,
    sugestao: CriteriosClassificacao,
    aoVoltar: () -> Unit,
) {
    val p = LocalPaleta.current
    val repo = LocalContext.current.repositorio
    val escopo = rememberCoroutineScope()

    var kmMin by remember(chave) { mutableFloatStateOf((atual?.kmMinimo ?: sugestao.porKm.razoavel).toFloat()) }
    var kmBom by remember(chave) { mutableFloatStateOf((atual?.kmBom ?: sugestao.porKm.boa).toFloat()) }
    var hMin by remember(chave) { mutableFloatStateOf((atual?.horaMinimo ?: sugestao.porHora.razoavel).toFloat()) }
    var hBom by remember(chave) { mutableFloatStateOf((atual?.horaBom ?: sugestao.porHora.boa).toFloat()) }
    var salvamento by remember { mutableStateOf(EstadoSalvamento.NADA) }
    var primeira by remember { mutableStateOf(true) }

    LaunchedEffect(kmMin, kmBom, hMin, hBom) {
        if (primeira) {
            primeira = false
            return@LaunchedEffect
        }
        salvamento = EstadoSalvamento.SALVANDO
        delay(500)
        repo.salvarLimite(
            LimiteEntity(
                chave, kmMin.toDouble(), maxOf(kmMin, kmBom).toDouble(), hMin.toDouble(), maxOf(hMin, hBom).toDouble(),
            )
        )
        salvamento = EstadoSalvamento.SALVO
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(Medidas.margem),
        verticalArrangement = Arrangement.spacedBy(Medidas.espaco),
    ) {
        Text(titulo, style = MaterialTheme.typography.headlineSmall, color = p.texto)
        Text(
            "Abaixo do mínimo: vermelho. Entre o mínimo e o bom: amarelo. A partir do bom: verde.",
            color = p.textoSecundario,
        )
        Legenda()
        SliderComValor("R$/km mínimo", kmMin, 0.5f..6f, 0.05f, { Formatos.moeda(it.toDouble()) }, { kmMin = it }, digitarComoMoeda = true)
        SliderComValor("R$/km bom", kmBom, 0.5f..6f, 0.05f, { Formatos.moeda(it.toDouble()) }, { kmBom = it }, digitarComoMoeda = true)
        if (kmBom < kmMin) Text("O valor bom deve ser maior que o mínimo.", color = p.vermelho)
        SliderComValor("R$/hora mínimo", hMin, 10f..150f, 1f, { Formatos.moeda(it.toDouble()) }, { hMin = it }, digitarComoMoeda = true)
        SliderComValor("R$/hora bom", hBom, 10f..150f, 1f, { Formatos.moeda(it.toDouble()) }, { hBom = it }, digitarComoMoeda = true)
        if (hBom < hMin) Text("O valor bom deve ser maior que o mínimo.", color = p.vermelho)
        IndicadorSalvamento(salvamento)
        if (atual != null) {
            BotaoSecundario("Voltar para a sugestão inicial", {
                escopo.launch { repo.excluirLimite(chave) }
                aoVoltar()
            })
        }
        BotaoPrincipal("Concluído", aoVoltar)
    }
}

@Composable
private fun Legenda() {
    val p = LocalPaleta.current
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(p.vermelho to "Ruim", p.amarelo to "Razoável", p.verde to "Boa", p.neutro to "Sem dados").forEach { (cor, nome) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Bolinha(cor)
                Text(" $nome", style = MaterialTheme.typography.labelMedium, color = p.textoSecundario)
            }
        }
    }
}

@Composable
fun Bolinha(cor: Color, tamanho: Int = 10) {
    Box(Modifier.size(tamanho.dp).clip(CircleShape).background(cor))
}
