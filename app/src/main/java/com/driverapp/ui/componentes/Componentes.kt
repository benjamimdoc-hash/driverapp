package com.driverapp.ui.componentes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driverapp.calculo.MascaraMoeda
import com.driverapp.ui.Formatos
import com.driverapp.ui.LocalPaleta
import com.driverapp.ui.Medidas
import kotlin.math.roundToInt

// =====================================================================================
// Superfícies
// =====================================================================================

/**
 * Card de "vidro": superfície translúcida com borda clara e sombra suave.
 * O desfoque real do fundo não é usado (pesa em celulares intermediários); o efeito vem
 * da transparência + gradiente, que mantém a leitura boa em qualquer aparelho.
 */
@Composable
fun CartaoVidro(
    modifier: Modifier = Modifier,
    brilho: Color? = null,
    preenchimento: Dp = 18.dp,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    val p = LocalPaleta.current
    val forma = RoundedCornerShape(Medidas.raioCard)
    val borda = brilho?.copy(alpha = 0.55f) ?: p.bordaVidro
    Column(
        modifier
            .then(
                if (p.escuro) Modifier else Modifier.shadow(6.dp, forma, ambientColor = Color(0x22000000), spotColor = Color(0x22000000))
            )
            .clip(forma)
            .background(
                Brush.verticalGradient(
                    if (p.escuro) listOf(p.vidroForte, p.vidro) else listOf(p.vidroForte, p.vidro)
                )
            )
            .border(BorderStroke(1.dp, borda), forma)
            .padding(preenchimento),
        content = conteudo,
    )
}

/** Fundo padrão das telas (gradiente suave do tema). */
@Composable
fun FundoApp(conteudo: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(LocalPaleta.current.fundoGradiente)) { conteudo() }
}

// =====================================================================================
// Botões
// =====================================================================================

@Composable
fun BotaoPrincipal(
    texto: String,
    aoTocar: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    habilitado: Boolean = true,
    cor: Color? = null,
    icone: Painter? = null,
) {
    val p = LocalPaleta.current
    val fundo = cor ?: p.destaque
    Button(
        onClick = aoTocar,
        enabled = habilitado,
        shape = RoundedCornerShape(Medidas.raioCampo),
        colors = ButtonDefaults.buttonColors(
            containerColor = fundo,
            contentColor = if (p.escuro) Color(0xFF04131F) else Color.White,
        ),
        modifier = modifier.heightIn(min = Medidas.alturaBotao),
    ) {
        if (icone != null) {
            Icon(icone, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(texto, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
fun BotaoSecundario(texto: String, aoTocar: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth(), habilitado: Boolean = true) {
    val p = LocalPaleta.current
    OutlinedButton(
        onClick = aoTocar,
        enabled = habilitado,
        shape = RoundedCornerShape(Medidas.raioCampo),
        border = BorderStroke(1.dp, p.bordaVidro),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = p.texto),
        modifier = modifier.heightIn(min = Medidas.alturaBotao),
    ) { Text(texto, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
}

// =====================================================================================
// Seleção
// =====================================================================================

/** Card grande de seleção com ícone (perfil, veículo, combustível, plataformas). */
@Composable
fun CartaoSelecao(
    titulo: String,
    descricao: String?,
    selecionado: Boolean,
    aoTocar: () -> Unit,
    modifier: Modifier = Modifier,
    icone: Painter? = null,
    sigla: String? = null,
    compacto: Boolean = false,
) {
    val p = LocalPaleta.current
    val forma = RoundedCornerShape(Medidas.raioCard)
    val corBorda by animateColorAsState(if (selecionado) p.destaque else p.bordaVidro, tween(180), label = "borda")
    val fundo by animateColorAsState(
        if (selecionado) p.destaque.copy(alpha = if (p.escuro) 0.16f else 0.10f) else p.vidro, tween(180), label = "fundo",
    )
    val escala by animateFloatAsState(if (selecionado) 1f else 0.98f, tween(180), label = "escala")
    Box(
        modifier
            .scale(escala)
            .clip(forma)
            .background(fundo)
            .border(BorderStroke(if (selecionado) 2.dp else 1.dp, corBorda), forma)
            .clickable(role = Role.Checkbox, onClick = aoTocar)
            .semantics { stateDescription = if (selecionado) "selecionado" else "não selecionado" }
            .padding(if (compacto) 14.dp else 18.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val tamanho = if (compacto) 44.dp else 60.dp
            Box(
                Modifier
                    .size(tamanho)
                    .clip(CircleShape)
                    .background(if (selecionado) p.destaque.copy(alpha = 0.22f) else p.vidroForte),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    icone != null -> Icon(
                        icone, contentDescription = null,
                        tint = if (selecionado) p.destaque else p.textoSecundario,
                        modifier = Modifier.size(tamanho * 0.55f),
                    )
                    sigla != null -> Text(
                        sigla, fontWeight = FontWeight.Bold, fontSize = if (compacto) 15.sp else 18.sp,
                        color = if (selecionado) p.destaque else p.textoSecundario,
                    )
                }
            }
            Text(
                titulo, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                style = if (compacto) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                color = p.texto,
            )
            if (descricao != null) {
                Text(
                    descricao, style = MaterialTheme.typography.bodySmall, color = p.textoSecundario,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (selecionado) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(p.destaque),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp),
                    tint = if (p.escuro) Color(0xFF04131F) else Color.White,
                )
            }
        }
    }
}

/**
 * Grade de cards de seleção: lado a lado quando há espaço, em coluna quando a tela é estreita.
 * [larguraMinima] é a largura mínima de cada card para caber lado a lado.
 */
@Composable
fun GradeSelecao(
    quantidade: Int,
    larguraMinima: Dp = 150.dp,
    item: @Composable (indice: Int, modifier: Modifier) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val porLinha = (maxWidth / larguraMinima).toInt().coerceIn(1, quantidade.coerceAtLeast(1))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            (0 until quantidade).chunked(porLinha).forEach { linha ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    linha.forEach { i -> item(i, Modifier.weight(1f)) }
                    repeat(porLinha - linha.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** Botões de seleção em formato de pílula (um ou vários), quebrando linha quando não cabem. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Chips(opcoes: List<Pair<String, String>>, selecionados: Set<String>, aoTocar: (String) -> Unit) {
    val p = LocalPaleta.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        opcoes.forEach { (chave, rotulo) ->
            val sel = chave in selecionados
            val fundo by animateColorAsState(if (sel) p.destaque else p.vidro, tween(150), label = "chip")
            Box(
                Modifier
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(fundo)
                    .border(1.dp, if (sel) p.destaque else p.bordaVidro, RoundedCornerShape(50))
                    .clickable(role = Role.Checkbox) { aoTocar(chave) }
                    .semantics { stateDescription = if (sel) "selecionado" else "não selecionado" }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    rotulo,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (sel) (if (p.escuro) Color(0xFF04131F) else Color.White) else p.texto,
                )
            }
        }
    }
}

/** Seletor segmentado (ex.: Hoje | Semana | Mês). */
@Composable
fun SeletorSegmentado(opcoes: List<String>, selecionado: Int, aoMudar: (Int) -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPaleta.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(p.vidro)
            .border(1.dp, p.bordaVidro, RoundedCornerShape(50))
            .padding(4.dp),
    ) {
        opcoes.forEachIndexed { i, rotulo ->
            val sel = i == selecionado
            val fundo by animateColorAsState(if (sel) p.destaque else Color.Transparent, tween(180), label = "seg")
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(50))
                    .background(fundo)
                    .clickable(role = Role.Tab) { aoMudar(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    rotulo, fontWeight = FontWeight.SemiBold,
                    color = if (sel) (if (p.escuro) Color(0xFF04131F) else Color.White) else p.textoSecundario,
                )
            }
        }
    }
}

// =====================================================================================
// Campos
// =====================================================================================

@Composable
private fun coresCampo() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = LocalPaleta.current.destaque,
    unfocusedBorderColor = LocalPaleta.current.bordaVidro,
    focusedContainerColor = LocalPaleta.current.vidro,
    unfocusedContainerColor = LocalPaleta.current.vidro,
)

/** Campo para números (aceita vírgula). */
@Composable
fun CampoNumero(
    rotulo: String,
    valor: String,
    aoMudar: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    ajuda: String? = null,
    sufixo: String? = null,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { novo -> aoMudar(novo.filter { it.isDigit() || it == ',' || it == '.' }) },
        label = { Text(rotulo) },
        supportingText = if (ajuda != null) {
            { Text(ajuda) }
        } else null,
        suffix = if (sufixo != null) {
            { Text(sufixo) }
        } else null,
        singleLine = true,
        shape = RoundedCornerShape(Medidas.raioCampo),
        colors = coresCampo(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@Composable
fun CampoTexto(rotulo: String, valor: String, aoMudar: (String) -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(
        value = valor, onValueChange = aoMudar, label = { Text(rotulo) }, singleLine = true,
        shape = RoundedCornerShape(Medidas.raioCampo), colors = coresCampo(), modifier = modifier,
    )
}

/**
 * Campo de dinheiro com máscara brasileira: o motorista digita só números
 * ("570" → R$ 5,70). O valor entregue é numérico (Double), nunca o texto.
 */
@Composable
fun CampoMoeda(
    rotulo: String,
    valor: Double?,
    aoMudar: (Double?) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    ajuda: String? = null,
) {
    var digitos by remember { mutableStateOf(MascaraMoeda.digitosDe(valor)) }
    // Se o valor mudar por fora (ex.: carregado do banco), acompanha.
    LaunchedEffect(valor) {
        if (MascaraMoeda.valor(digitos) != valor) digitos = MascaraMoeda.digitosDe(valor)
    }
    val texto = MascaraMoeda.formatar(digitos)
    OutlinedTextField(
        value = TextFieldValue(texto, selection = TextRange(texto.length)),
        onValueChange = { novo ->
            val limpos = MascaraMoeda.limpar(novo.text)
            digitos = limpos
            aoMudar(MascaraMoeda.valor(limpos))
        },
        label = { Text(rotulo) },
        prefix = { Text("R$ ") },
        placeholder = { Text("0,00") },
        supportingText = if (ajuda != null) {
            { Text(ajuda) }
        } else null,
        singleLine = true,
        shape = RoundedCornerShape(Medidas.raioCampo),
        colors = coresCampo(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = modifier,
    )
}

/**
 * Slider com o valor em destaque. Tocar no valor abre um campo para digitar o número exato
 * (acessibilidade e precisão).
 */
@Composable
fun SliderComValor(
    rotulo: String,
    valor: Float,
    faixa: ClosedFloatingPointRange<Float>,
    passo: Float,
    formatar: (Float) -> String,
    aoMudar: (Float) -> Unit,
    modifier: Modifier = Modifier,
    observacao: String? = null,
    digitarComoMoeda: Boolean = false,
) {
    val p = LocalPaleta.current
    var digitando by remember { mutableStateOf(false) }
    val passos = (((faixa.endInclusive - faixa.start) / passo).roundToInt() - 1).coerceAtLeast(0)

    CartaoVidro(modifier.fillMaxWidth()) {
        Text(rotulo, style = MaterialTheme.typography.labelLarge, color = p.textoSecundario)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatar(valor),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = p.texto,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "$rotulo: ${formatar(valor)}" },
            )
            TextButton(onClick = { digitando = true }) { Text("Digitar") }
        }
        Slider(
            value = valor.coerceIn(faixa),
            onValueChange = { bruto -> aoMudar(arredondarPasso(bruto, passo, faixa)) },
            valueRange = faixa,
            steps = if (passos in 1..400) passos else 0,
            colors = SliderDefaults.colors(
                thumbColor = p.destaque,
                activeTrackColor = p.destaque,
                inactiveTrackColor = p.bordaVidro,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatar(faixa.start), style = MaterialTheme.typography.labelSmall, color = p.textoSecundario)
            Text(formatar(faixa.endInclusive), style = MaterialTheme.typography.labelSmall, color = p.textoSecundario)
        }
        if (observacao != null) {
            Text(observacao, style = MaterialTheme.typography.bodySmall, color = p.textoSecundario)
        }
    }

    if (digitando) {
        var texto by remember { mutableStateOf(if (digitarComoMoeda) "" else Formatos.campo(valor.toDouble())) }
        var moeda by remember { mutableStateOf<Double?>(valor.toDouble()) }
        AlertDialog(
            onDismissRequest = { digitando = false },
            title = { Text(rotulo) },
            text = {
                if (digitarComoMoeda) CampoMoeda("Valor", moeda, { moeda = it })
                else CampoNumero("Valor", texto, { texto = it })
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = if (digitarComoMoeda) moeda else Formatos.lerNumero(texto)
                    // Valor digitado pode passar dos limites do slider (o slider só ajuda a ajustar rápido).
                    if (v != null && v >= 0) aoMudar(v.toFloat())
                    digitando = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { digitando = false }) { Text("Cancelar") } },
        )
    }
}

private fun arredondarPasso(v: Float, passo: Float, faixa: ClosedFloatingPointRange<Float>): Float {
    val n = ((v - faixa.start) / passo).roundToInt()
    return (faixa.start + n * passo).coerceIn(faixa)
}

// =====================================================================================
// Estrutura de telas
// =====================================================================================

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
    rodapeExtra: @Composable () -> Unit = {},
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    val p = LocalPaleta.current
    Column(Modifier.fillMaxSize().imePadding()) {
        topo()
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Medidas.margem, vertical = 12.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(Medidas.espaco),
        ) {
            Text(titulo, style = MaterialTheme.typography.headlineSmall, color = p.texto)
            if (subtitulo != null) {
                Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = p.textoSecundario)
            }
            Spacer(Modifier.height(2.dp))
            conteudo()
            Spacer(Modifier.height(8.dp))
        }
        rodapeExtra()
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (aoVoltar != null) {
                BotaoSecundario("Voltar", aoVoltar, Modifier.weight(1f))
            }
            BotaoPrincipal(
                textoPrincipal, aoPrincipal,
                Modifier.weight(if (aoVoltar != null) 2f else 1f),
                habilitado = principalHabilitado,
            )
        }
    }
}

/** Linha "rótulo .......... valor". */
@Composable
fun LinhaValor(rotulo: String, valor: String, destaque: Boolean = false, corValor: Color? = null) {
    val p = LocalPaleta.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(rotulo, style = MaterialTheme.typography.bodyMedium, color = p.textoSecundario, modifier = Modifier.weight(1f))
        Text(
            valor,
            style = if (destaque) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (destaque) FontWeight.Bold else FontWeight.Medium,
            color = corValor ?: p.texto,
        )
    }
}

/** Indicador discreto de salvamento automático. */
enum class EstadoSalvamento { NADA, SALVANDO, SALVO }

@Composable
fun IndicadorSalvamento(estado: EstadoSalvamento) {
    val p = LocalPaleta.current
    val texto = when (estado) {
        EstadoSalvamento.NADA -> ""
        EstadoSalvamento.SALVANDO -> "Salvando…"
        EstadoSalvamento.SALVO -> "✓ Salvo automaticamente"
    }
    Text(
        texto,
        style = MaterialTheme.typography.labelMedium,
        color = if (estado == EstadoSalvamento.SALVO) p.verde else p.textoSecundario,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Medidas.margem),
        textAlign = TextAlign.End,
    )
}

/** Barra de progresso do cadastro, com a contagem de etapas. */
@Composable
fun ProgressoEtapas(atual: Int, total: Int) {
    val p = LocalPaleta.current
    val fracao by animateFloatAsState((atual + 1) / total.toFloat(), tween(300), label = "progresso")
    Column(Modifier.fillMaxWidth().padding(start = Medidas.margem, end = Medidas.margem, top = 16.dp)) {
        Text("Etapa ${atual + 1} de $total", style = MaterialTheme.typography.labelLarge, color = p.textoSecundario)
        Box(
            Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(p.bordaVidro),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fracao)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(p.destaque, p.ciano))),
            )
        }
    }
}

/** Barra fina de progresso (metas). */
@Composable
fun BarraProgresso(fracao: Float, cor: Color = LocalPaleta.current.destaque, modifier: Modifier = Modifier) {
    val p = LocalPaleta.current
    val animada by animateFloatAsState(fracao.coerceIn(0f, 1f), tween(400), label = "barra")
    val altura by animateDpAsState(8.dp, label = "altura")
    Box(
        modifier
            .fillMaxWidth()
            .height(altura)
            .clip(RoundedCornerShape(50))
            .background(p.bordaVidro),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animada)
                .height(altura)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(cor, p.ciano))),
        )
    }
}
