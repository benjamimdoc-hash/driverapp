package com.driverapp.ui.leitura

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.driverapp.calculo.Cenario
import com.driverapp.calculo.Nivel
import com.driverapp.calculo.TipoCenario
import com.driverapp.leitores.Confianca
import com.driverapp.leitura.ResultadoAnalise
import com.driverapp.ui.Formatos
import com.driverapp.ui.LocalPaleta
import com.driverapp.ui.componentes.CartaoVidro
import com.driverapp.ui.componentes.LinhaValor
import com.driverapp.ui.estiloNumeroDestaque

/**
 * Card de análise de uma corrida, no mesmo sistema visual do app (tema claro/escuro).
 * Dados ausentes aparecem como "indisponível"; nada é preenchido por estimativa.
 */
@Composable
fun CartaoAnalise(r: ResultadoAnalise, modifier: Modifier = Modifier) {
    val p = LocalPaleta.current
    val corNivel = p.corDe(r.nivel?.cor)
    val principal = r.analise?.principal

    CartaoVidro(modifier.fillMaxWidth(), brilho = corNivel) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${r.leitura.plataforma.nome} • ${r.categoria ?: "categoria indisponível"}",
                    style = MaterialTheme.typography.labelLarge, color = p.textoSecundario,
                )
                Text(Formatos.moeda(r.leitura.valor), style = estiloNumeroDestaque(30))
            }
            Pilula(nomeNivel(r.nivel), corNivel)
        }

        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Metrica("R$/km", principal?.reaisPorKm?.let { Formatos.numero(it) }, corNivel, Modifier.weight(1f))
            Metrica("R$/hora", principal?.reaisPorHora?.let { Formatos.numero(it) }, corNivel, Modifier.weight(1f))
            val res = principal?.resultado
            Metrica("Resultado", res?.let { Formatos.numero(it) }, if ((res ?: 0.0) >= 0) p.verde else p.vermelho, Modifier.weight(1f))
        }

        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (principal != null) Cenarios(principal, r.analise?.soViagem?.takeIf { r.analise?.total != null })
            LinhaValor("Coleta", r.leitura.coleta?.let { "${it.minutos.toInt()} min • ${Formatos.km(it.km)}" } ?: "indisponível")
            LinhaValor("Viagem", r.leitura.viagem?.let { "${it.minutos.toInt()} min • ${Formatos.km(it.km)}" } ?: "indisponível")
            LinhaValor("Combustível estimado", principal?.custoCombustivel?.let { Formatos.moeda(it) } ?: "indisponível")
            principal?.custoFixoAlocado?.let { LinhaValor("Custos fixos do tempo", Formatos.moeda(it)) }
            principal?.resultadoAposFixos?.let { LinhaValor("Resultado após custos fixos", Formatos.moeda(it)) }
            LinhaValor("Nota do passageiro", r.leitura.notaPassageiro?.let { Formatos.numero(it) } ?: "indisponível")
        }

        if (r.leitura.confianca == Confianca.BAIXA) {
            Text(
                "⚠ Leitura incerta: " + r.leitura.alertas.joinToString("; "),
                style = MaterialTheme.typography.bodySmall, color = p.amarelo, modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (principal?.custoCombustivel == null && r.leitura.valor != null) {
            Text(
                "Configure o combustível para ver o resultado estimado.",
                style = MaterialTheme.typography.bodySmall, color = p.textoSecundario,
            )
        }
    }
}

@Composable
private fun Cenarios(principal: Cenario, soViagem: Cenario?) {
    val p = LocalPaleta.current
    val nome = if (principal.tipo == TipoCenario.TOTAL) "Considerando coleta + viagem" else "Considerando só a viagem"
    Text(nome, style = MaterialTheme.typography.labelMedium, color = p.destaque)
    if (soViagem != null) {
        Text(
            "Só a viagem: ${soViagem.reaisPorKm?.let { Formatos.moeda(it) } ?: "—"}/km • " +
                "${soViagem.reaisPorHora?.let { Formatos.moeda(it) } ?: "—"}/h",
            style = MaterialTheme.typography.bodySmall, color = p.textoSecundario,
        )
    }
}

@Composable
private fun Metrica(rotulo: String, valor: String?, cor: Color, modifier: Modifier) {
    val p = LocalPaleta.current
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(p.vidro)
            .padding(10.dp),
    ) {
        Text(valor ?: "—", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (valor != null) cor else p.neutro)
        Text(rotulo, style = MaterialTheme.typography.labelSmall, color = p.textoSecundario)
    }
}

@Composable
fun Pilula(texto: String, cor: Color) {
    val p = LocalPaleta.current
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(cor)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(
            texto, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
            color = if (p.escuro) Color(0xFF04131F) else Color.White,
        )
    }
}

fun nomeNivel(n: Nivel?): String = when (n) {
    Nivel.RUIM -> "Ruim"
    Nivel.RAZOAVEL -> "Razoável"
    Nivel.BOA -> "Boa"
    Nivel.EXCELENTE -> "Excelente"
    null -> "Sem dados"
}
