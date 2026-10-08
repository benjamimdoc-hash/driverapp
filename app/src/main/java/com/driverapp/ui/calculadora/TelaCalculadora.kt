package com.driverapp.ui.calculadora

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driverapp.calculo.AnaliseCorrida
import com.driverapp.calculo.Analisador
import com.driverapp.calculo.Cenario
import com.driverapp.calculo.Classificador
import com.driverapp.calculo.Combustivel
import com.driverapp.calculo.Cor
import com.driverapp.calculo.DadosCorrida
import com.driverapp.calculo.Nivel
import com.driverapp.calculo.ReferenciasIniciais
import com.driverapp.calculo.TipoCombustivel
import com.driverapp.calculo.Trecho
import com.driverapp.leitores.Leitor99
import com.driverapp.leitores.LeitorUber
import com.driverapp.leitores.OfertaLida
import com.driverapp.ui.CoresClassificacao
import java.util.Locale

private val BR: Locale = Locale.forLanguageTag("pt-BR")

private fun numero(texto: String): Double? = texto.trim().replace(',', '.').toDoubleOrNull()
private fun fmt(v: Double, casas: Int = 2): String = String.format(BR, "%.${casas}f", v)
private fun campo(v: Double?): String = v?.let { fmt(it, if (it % 1.0 == 0.0) 0 else 2) } ?: ""

/** Textos das ofertas reais dos prints, usados pelos botões de exemplo (testam o leitor no celular). */
private val EXEMPLO_UBER = listOf(
    "UberX", "Exclusivo", "R$ 15,30", "R$2,35/km aprox.", "4,94 (1332)", "Verificado",
    "4 min (1.0 km)", "Rua Santa Luzia da Boa Visão", "14 minutos (5.5 km)", "Rua Rosa Ribas, 316", "Aceitar",
)
private val EXEMPLO_99 = listOf(
    "Negocia", "Pgto. no app", "R$6,86", "R$2,56/km", "4,95", "359 corridas", "Perfil Premium",
    "6 min (1,3 km)", "Rua Tibúrcio de Sousa, 1391", "5 min (1,4 km)", "Rua João Esteves Robalo, 21", "Aceitar por R$6,86",
)

@Composable
fun TelaCalculadora() {
    var valor by rememberSaveable { mutableStateOf("15,30") }
    var coletaMin by rememberSaveable { mutableStateOf("4") }
    var coletaKm by rememberSaveable { mutableStateOf("1,0") }
    var viagemMin by rememberSaveable { mutableStateOf("14") }
    var viagemKm by rememberSaveable { mutableStateOf("5,5") }
    var precoLitro by rememberSaveable { mutableStateOf("4,19") }
    var kmPorLitro by rememberSaveable { mutableStateOf("8") }
    var categoria by rememberSaveable { mutableStateOf("UberX") }
    var origem by rememberSaveable { mutableStateOf("Exemplo: oferta da Uber do print") }

    fun preencher(oferta: OfertaLida?, nome: String) {
        if (oferta == null) {
            origem = "$nome: não foi possível ler a oferta"
            return
        }
        valor = campo(oferta.valor)
        coletaMin = campo(oferta.coleta?.minutos)
        coletaKm = campo(oferta.coleta?.km)
        viagemMin = campo(oferta.viagem?.minutos)
        viagemKm = campo(oferta.viagem?.km)
        categoria = oferta.categoria ?: ""
        origem = "$nome lida pelo leitor automático"
    }

    val valorNum = numero(valor)
    val combustivel = Combustivel(TipoCombustivel.GASOLINA, numero(precoLitro) ?: 0.0, numero(kmPorLitro) ?: 0.0)
    val analise: AnaliseCorrida? = valorNum?.let {
        Analisador.analisar(
            DadosCorrida(
                valor = it,
                coleta = trechoOuNull(numero(coletaMin), numero(coletaKm)),
                viagem = trechoOuNull(numero(viagemMin), numero(viagemKm)),
            ),
            custoCombustivelPorKm = combustivel.custoPorKm,
        )
    }

    Column(
        modifier = Modifier
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Calculadora de corrida", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Versão de teste — Fase 1. Confira se os números batem com os da Uber/99.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { preencher(LeitorUber.ler(EXEMPLO_UBER), "Oferta da Uber") }, modifier = Modifier.weight(1f)) {
                Text("Exemplo Uber")
            }
            OutlinedButton(onClick = { preencher(Leitor99.ler(EXEMPLO_99), "Oferta da 99") }, modifier = Modifier.weight(1f)) {
                Text("Exemplo 99")
            }
        }
        Text(origem, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (analise != null) {
            CartaoResultado(analise, categoria)
        }

        Campo("Valor da corrida (R$)", valor) { valor = it }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Campo("Coleta (min)", coletaMin, Modifier.weight(1f)) { coletaMin = it }
            Campo("Coleta (km)", coletaKm, Modifier.weight(1f)) { coletaKm = it }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Campo("Viagem (min)", viagemMin, Modifier.weight(1f)) { viagemMin = it }
            Campo("Viagem (km)", viagemKm, Modifier.weight(1f)) { viagemKm = it }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Campo("Preço do litro (R$)", precoLitro, Modifier.weight(1f)) { precoLitro = it }
            Campo("Consumo (km/l)", kmPorLitro, Modifier.weight(1f)) { kmPorLitro = it }
        }
        combustivel.custoPorKm?.let {
            Text("Combustível: R$ ${fmt(it)} por km", style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            "Faixas de cor: valores iniciais sugeridos e editáveis no futuro — não são regras do mercado.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
    }
}

private fun trechoOuNull(minutos: Double?, km: Double?): Trecho? =
    if (minutos == null && km == null) null else Trecho(minutos, km)

@Composable
private fun Campo(rotulo: String, valor: String, modifier: Modifier = Modifier.fillMaxWidth(), aoMudar: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@Composable
private fun CartaoResultado(analise: AnaliseCorrida, categoria: String) {
    val principal = analise.principal
    val nivel = principal?.let {
        Classificador.classificar(it.reaisPorKm, it.reaisPorHora, ReferenciasIniciais.paraCategoria(categoria))
    }
    val corFaixa = when (nivel?.cor) {
        Cor.VERMELHO -> CoresClassificacao.vermelho
        Cor.AMARELO -> CoresClassificacao.amarelo
        Cor.VERDE -> CoresClassificacao.verde
        null -> MaterialTheme.colorScheme.outline
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(corFaixa)
            )
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("R$ ${fmt(analise.valor)}", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        nomeNivel(nivel),
                        color = corFaixa,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                if (principal != null) {
                    LinhaCenario(principal, destaque = true)
                }
                if (analise.total != null && analise.soViagem != null) {
                    LinhaCenario(analise.soViagem!!, destaque = false)
                }
                principal?.let { c ->
                    val custo = c.custoCombustivel?.let { "R$ ${fmt(it)}" } ?: "indisponível"
                    val resultado = c.resultado?.let { "R$ ${fmt(it)}" } ?: "indisponível"
                    Text("Combustível: $custo  •  Resultado estimado: $resultado", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun LinhaCenario(c: Cenario, destaque: Boolean) {
    val nome = if (c.tipo.name == "TOTAL") "Total (coleta + viagem)" else "Só a viagem"
    val km = c.reaisPorKm?.let { "R$ ${fmt(it)}/km" } ?: "R$/km indisponível"
    val hora = c.reaisPorHora?.let { "R$ ${fmt(it)}/h" } ?: "R$/h indisponível"
    val dist = listOfNotNull(c.km?.let { "${fmt(it, 1)} km" }, c.minutos?.let { "${fmt(it, 0)} min" }).joinToString(" • ")
    Column {
        Text(nome, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "$km   $hora",
            fontWeight = if (destaque) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (destaque) 18.sp else 15.sp,
            color = if (destaque) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (dist.isNotEmpty()) Text(dist, style = MaterialTheme.typography.bodySmall)
    }
}

private fun nomeNivel(n: Nivel?): String = when (n) {
    Nivel.RUIM -> "Ruim"
    Nivel.RAZOAVEL -> "Razoável"
    Nivel.BOA -> "Boa"
    Nivel.EXCELENTE -> "Excelente"
    null -> "Sem dados"
}
