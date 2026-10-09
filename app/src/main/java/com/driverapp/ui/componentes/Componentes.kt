package com.driverapp.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** Campo para números (aceita vírgula). */
@Composable
fun CampoNumero(
    rotulo: String,
    valor: String,
    aoMudar: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    ajuda: String? = null,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { novo -> aoMudar(novo.filter { it.isDigit() || it == ',' || it == '.' }) },
        label = { Text(rotulo) },
        supportingText = if (ajuda != null) {
            { Text(ajuda) }
        } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@Composable
fun CampoTexto(rotulo: String, valor: String, aoMudar: (String) -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(value = valor, onValueChange = aoMudar, label = { Text(rotulo) }, singleLine = true, modifier = modifier)
}

/** Cartão grande de escolha única (ex.: Motorista / Entregador). */
@Composable
fun CartaoOpcao(titulo: String, descricao: String?, selecionado: Boolean, aoTocar: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            if (selecionado) 2.dp else 1.dp,
            if (selecionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selecionado) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = aoTocar),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (descricao != null) {
                Text(descricao, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Botões de seleção (um ou vários), quebrando linha quando não cabem. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Chips(opcoes: List<Pair<String, String>>, selecionados: Set<String>, aoTocar: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        opcoes.forEach { (chave, rotulo) ->
            FilterChip(
                selected = chave in selecionados,
                onClick = { aoTocar(chave) },
                label = { Text(rotulo) },
            )
        }
    }
}

/**
 * Moldura padrão de uma etapa/tela de formulário: título, conteúdo com rolagem
 * e botões grandes no rodapé (usável com uma mão).
 */
@Composable
fun MolduraEtapa(
    titulo: String,
    subtitulo: String?,
    textoPrincipal: String,
    principalHabilitado: Boolean,
    aoPrincipal: () -> Unit,
    aoVoltar: (() -> Unit)?,
    topo: @Composable () -> Unit = {},
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().imePadding()) {
        topo()
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(titulo, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (subtitulo != null) {
                Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            conteudo()
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (aoVoltar != null) {
                OutlinedButton(onClick = aoVoltar, modifier = Modifier.weight(1f).height(56.dp)) { Text("Voltar") }
            }
            Button(
                onClick = aoPrincipal,
                enabled = principalHabilitado,
                modifier = Modifier.weight(if (aoVoltar != null) 2f else 1f).height(56.dp),
            ) { Text(textoPrincipal) }
        }
    }
}

/** Linha "rótulo .......... valor". */
@Composable
fun LinhaValor(rotulo: String, valor: String, destaque: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(rotulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            valor,
            style = if (destaque) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (destaque) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
