package com.driverapp.ui.leitura

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driverapp.calculo.Nivel
import com.driverapp.leitores.Interpretador
import com.driverapp.leitores.LeitorUber
import com.driverapp.leitura.EstadoLeitura
import com.driverapp.leitura.ExemplosReferencia
import com.driverapp.leitura.Opacidade
import com.driverapp.leitura.ProcessadorOfertas
import com.driverapp.leitura.ResultadoAnalise
import com.driverapp.ui.Formatos
import com.driverapp.ui.LocalPaleta
import com.driverapp.ui.Medidas
import com.driverapp.ui.componentes.BotaoPrincipal
import com.driverapp.ui.componentes.BotaoSecundario
import com.driverapp.ui.componentes.CartaoVidro
import com.driverapp.ui.componentes.SeletorSegmentado
import com.driverapp.ui.componentes.SliderComValor

/** Ajustes do card flutuante: opacidade, posição, recolhido e teste na tela. */
@Composable
fun TelaCardFlutuante(aoVoltar: () -> Unit) {
    val p = LocalPaleta.current
    val contexto = LocalContext.current
    BackHandler(onBack = aoVoltar)

    var opacidade by remember { mutableFloatStateOf(EstadoLeitura.opacidade(contexto)) }
    var recolhido by remember { mutableStateOf(EstadoLeitura.cardRecolhido(contexto)) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var exemplo by remember { mutableStateOf<ResultadoAnalise?>(null) }
    val ativo = EstadoLeitura.servicoAtivo(contexto)

    // Exemplo da prévia: o print de referência da Uber, calculado com o SEU cadastro.
    LaunchedEffect(Unit) {
        LeitorUber.ler(ExemplosReferencia.UBER)?.let {
            exemplo = ProcessadorOfertas.analisarComCadastro(contexto, Interpretador.validar(it))
        }
    }

    fun mudarOpacidade(v: Float) {
        opacidade = Opacidade.limitar(v)
        EstadoLeitura.definirOpacidade(contexto, opacidade)
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(Medidas.margem),
        verticalArrangement = Arrangement.spacedBy(Medidas.espaco),
    ) {
        Text("Card flutuante", style = MaterialTheme.typography.headlineSmall, color = p.texto)
        Text(
            "Aparece por cima da Uber/99 quando chega uma oferta. Arraste para mudar de lugar, toque para ver detalhes " +
                "e use o \"–\" para recolher numa bolinha.",
            color = p.textoSecundario, style = MaterialTheme.typography.bodyMedium,
        )

        // Prévia sobre um "mapa" de fundo, para ver o efeito da opacidade.
        Box(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(Medidas.raioCard))
                .background(Brush.linearGradient(listOf(Color(0xFFDDE6EE), Color(0xFF9FB4C7), Color(0xFFE9D9C4))))
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            exemplo?.let { PreviaCardCompacto(it, opacidade) }
        }

        CartaoVidro(Modifier.fillMaxWidth()) {
            Text("Transparência do fundo", fontWeight = FontWeight.SemiBold, color = p.texto)
            Text(
                "Só o fundo fica transparente; os números continuam bem visíveis.",
                style = MaterialTheme.typography.bodySmall, color = p.textoSecundario,
            )
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

        CartaoVidro(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Começar recolhido", fontWeight = FontWeight.SemiBold, color = p.texto)
                    Text(
                        "As ofertas aparecem só como bolinha com o R$/km. Toque nela para abrir.",
                        style = MaterialTheme.typography.bodySmall, color = p.textoSecundario,
                    )
                }
                Switch(
                    checked = recolhido,
                    onCheckedChange = {
                        recolhido = it
                        EstadoLeitura.definirCardRecolhido(contexto, it)
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = p.destaque),
                )
            }
        }

        BotaoPrincipal("Mostrar card de teste na tela", {
            if (ativo) {
                EstadoLeitura.pedidoTeste.tryEmit(Unit)
                aviso = "Pronto! Saia do app para ver o card por cima de outras telas. Teste arrastar, tocar e recolher."
            } else {
                aviso = "Ative a leitura de ofertas primeiro (Ajustes → Leitura de corridas)."
            }
        })
        BotaoSecundario("Voltar o card para a posição inicial", {
            EstadoLeitura.restaurarPosicaoCard(contexto)
            aviso = "Posição restaurada. Vale a partir do próximo card."
        })
        aviso?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = p.destaque) }
        BotaoSecundario("Voltar", aoVoltar)
    }
}

/** Réplica em Compose do card compacto (mesmas medidas), usada na prévia dos ajustes. */
@Composable
fun PreviaCardCompacto(r: ResultadoAnalise, opacidade: Float) {
    val p = LocalPaleta.current
    val c = r.analise?.principal
    val fundo = (if (p.escuro) Color(0xFF10141C) else Color.White).copy(alpha = Opacidade.limitar(opacidade))
    fun cor(n: Nivel?) = if (n == null) p.neutro else p.corDe(n.cor)

    Row(
        Modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(18.dp))
            .background(fundo)
            .border(1.dp, p.bordaVidro.copy(alpha = Opacidade.limitar(opacidade) * 0.6f), RoundedCornerShape(18.dp))
            .padding(start = 10.dp, top = 8.dp, bottom = 8.dp, end = 6.dp),
    ) {
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(cor(r.nivel)),
        )
        Column(Modifier.padding(start = 10.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MetricaPrevia(c?.reaisPorKm?.let { Formatos.numero(it) }, "/km", cor(r.nivelKm), Modifier.weight(1f))
                SeparadorPrevia(30)
                MetricaPrevia(c?.reaisPorHora?.let { Formatos.numero(it) }, "/hora", cor(r.nivelHora), Modifier.weight(1f))
                SeparadorPrevia(30)
                MetricaPrevia(r.leitura.notaPassageiro?.let { Formatos.numero(it) }, "nota", p.destaque, Modifier.weight(1f))
                Text("–", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = p.textoSecundario, modifier = Modifier.padding(horizontal = 8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Text(Formatos.moeda(r.leitura.valor), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = p.texto)
                val dt = listOfNotNull(c?.km?.let { Formatos.km(it) }, c?.minutos?.let { "${it.toInt()} min" }).joinToString("  ")
                Text("  $dt", fontSize = 13.sp, color = p.textoSecundario)
                r.leitura.destino?.let {
                    SeparadorPrevia(14, Modifier.padding(horizontal = 8.dp))
                    Icon(Icons.Filled.Place, contentDescription = null, tint = p.textoSecundario, modifier = Modifier.size(14.dp))
                    Text(" $it", fontSize = 13.sp, color = p.textoSecundario, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun MetricaPrevia(valor: String?, rotulo: String, cor: Color, modifier: Modifier) {
    val p = LocalPaleta.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor ?: "—", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (valor != null) cor else p.neutro, maxLines = 1)
        Text(rotulo, fontSize = 11.sp, color = p.textoSecundario)
    }
}

@Composable
private fun SeparadorPrevia(alturaDp: Int, modifier: Modifier = Modifier) {
    Box(modifier.width(1.dp).height(alturaDp.dp).background(LocalPaleta.current.bordaVidro))
}
