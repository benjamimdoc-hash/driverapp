package com.driverapp.ui.calculadora

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.driverapp.dados.ConfiguracaoEntity
import com.driverapp.leitores.Categorias
import com.driverapp.leitores.Interpretador
import com.driverapp.leitores.Leitor99
import com.driverapp.leitores.LeitorUber
import com.driverapp.leitores.OfertaLida
import com.driverapp.leitores.Plataforma
import com.driverapp.leitores.TrechoLido
import com.driverapp.leitura.ExemplosReferencia
import com.driverapp.leitura.ProcessadorOfertas
import com.driverapp.leitura.ResultadoAnalise
import com.driverapp.ui.Formatos
import com.driverapp.ui.LocalPaleta
import com.driverapp.ui.Medidas
import com.driverapp.ui.componentes.BotaoSecundario
import com.driverapp.ui.componentes.CampoMoeda
import com.driverapp.ui.componentes.CampoNumero
import com.driverapp.ui.componentes.Chips
import com.driverapp.ui.leitura.CartaoAnalise

/**
 * Calculadora manual: o motorista digita os números de uma oferta e vê a mesma análise
 * do card automático, com o combustível, os custos e os limites do cadastro.
 */
@Composable
fun TelaCalculadora(config: ConfiguracaoEntity) {
    val p = LocalPaleta.current
    val contexto = LocalContext.current

    var plataforma by rememberSaveable { mutableStateOf(Plataforma.UBER.name) }
    var categoria by rememberSaveable { mutableStateOf("UberX") }
    var valor by rememberSaveable { mutableStateOf<Double?>(null) }
    var coletaMin by rememberSaveable { mutableStateOf("") }
    var coletaKm by rememberSaveable { mutableStateOf("") }
    var viagemMin by rememberSaveable { mutableStateOf("") }
    var viagemKm by rememberSaveable { mutableStateOf("") }
    var resultado by androidx.compose.runtime.remember { mutableStateOf<ResultadoAnalise?>(null) }

    fun preencher(o: OfertaLida?) {
        if (o == null) return
        plataforma = o.plataforma.name
        categoria = Categorias.canonica(o.categoria) ?: ""
        valor = o.valor
        coletaMin = Formatos.campo(o.coleta?.minutos)
        coletaKm = Formatos.campo(o.coleta?.km)
        viagemMin = Formatos.campo(o.viagem?.minutos)
        viagemKm = Formatos.campo(o.viagem?.km)
    }

    // Recalcula sempre que um campo muda (com os dados atuais do cadastro).
    LaunchedEffect(plataforma, categoria, valor, coletaMin, coletaKm, viagemMin, viagemKm, config) {
        val plat = Plataforma.entries.firstOrNull { it.name == plataforma } ?: Plataforma.UBER
        fun trecho(min: String, km: String): TrechoLido? {
            val m = Formatos.lerNumero(min) ?: return null
            val k = Formatos.lerNumero(km) ?: return null
            return TrechoLido(m, k)
        }
        val oferta = OfertaLida(
            plataforma = plat, valor = valor,
            coleta = trecho(coletaMin, coletaKm), viagem = trecho(viagemMin, viagemKm),
            categoria = categoria.ifBlank { null }, notaPassageiro = null, corridasPassageiro = null,
            reaisPorKmInformado = null, observacoes = emptyList(),
        )
        resultado = if (valor == null) null else ProcessadorOfertas.analisarComCadastro(contexto, Interpretador.validar(oferta))
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(Medidas.margem),
        verticalArrangement = Arrangement.spacedBy(Medidas.espaco),
    ) {
        Text("Calculadora de corrida", style = MaterialTheme.typography.headlineSmall, color = p.texto)
        Text(
            "Digite os números de uma oferta. A análise usa o seu combustível, seus custos e seus limites.",
            color = p.textoSecundario, style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BotaoSecundario("Exemplo Uber", { preencher(LeitorUber.ler(ExemplosReferencia.UBER)) }, Modifier.weight(1f))
            BotaoSecundario("Exemplo 99", { preencher(Leitor99.ler(ExemplosReferencia.NOVENTA_E_NOVE)) }, Modifier.weight(1f))
        }
        resultado?.let { CartaoAnalise(it) }

        Chips(listOf(Plataforma.UBER.name to "Uber", Plataforma.NOVENTA_E_NOVE.name to "99"), setOf(plataforma)) { plataforma = it }
        val categorias = Categorias.daPlataforma(Plataforma.entries.firstOrNull { it.name == plataforma } ?: Plataforma.UBER)
        Chips(categorias.map { it to it }, setOf(categoria)) { categoria = it }
        CampoMoeda("Valor da corrida", valor, { valor = it })
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CampoNumero("Coleta (min)", coletaMin, { coletaMin = it }, Modifier.weight(1f))
            CampoNumero("Coleta (km)", coletaKm, { coletaKm = it }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CampoNumero("Viagem (min)", viagemMin, { viagemMin = it }, Modifier.weight(1f))
            CampoNumero("Viagem (km)", viagemKm, { viagemKm = it }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
    }
}
