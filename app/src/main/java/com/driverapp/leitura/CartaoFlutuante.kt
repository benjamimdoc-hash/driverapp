package com.driverapp.leitura

import android.accessibilityservice.AccessibilityService
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import com.driverapp.calculo.Nivel
import com.driverapp.calculo.TipoCenario
import com.driverapp.leitores.Confianca
import com.driverapp.repositorio
import com.driverapp.ui.Formatos
import com.driverapp.ui.Paleta
import com.driverapp.ui.PaletaClara
import com.driverapp.ui.PaletaEscura

/**
 * Card de análise mostrado POR CIMA da Uber/99 enquanto a oferta aparece.
 *
 * - Usa a janela de sobreposição própria dos serviços de acessibilidade (não precisa da
 *   permissão "sobrepor a outros apps").
 * - Fica no topo da tela, longe dos botões da oferta, e não recebe toques além do próprio card
 *   (tocar no card fecha; o resto da tela continua funcionando normalmente).
 * - Some sozinho depois de 15 segundos.
 * - Segue o tema escolhido no app (claro, escuro ou do sistema).
 */
class CartaoFlutuante(private val servico: AccessibilityService) {

    private val gerenciador = servico.getSystemService(WindowManager::class.java)
    private val principal = Handler(Looper.getMainLooper())
    private val esconderDepois = Runnable { esconder() }
    private var vista: View? = null

    suspend fun mostrar(r: ResultadoAnalise) {
        val tema = servico.repositorio.obterConfig().tema
        val escuro = when (tema) {
            "ESCURO" -> true
            "CLARO" -> false
            else -> (servico.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
        val nova = construir(r, if (escuro) PaletaEscura else PaletaClara)
        esconder()
        try {
            gerenciador.addView(nova, parametros())
            vista = nova
        } catch (_: Exception) {
            vista = null
        }
        principal.removeCallbacks(esconderDepois)
        principal.postDelayed(esconderDepois, 15_000)
    }

    fun esconder() {
        principal.removeCallbacks(esconderDepois)
        vista?.let { v ->
            try {
                gerenciador.removeView(v)
            } catch (_: Exception) {
            }
        }
        vista = null
    }

    private fun parametros() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP
        y = dp(40)
    }

    private fun dp(v: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), servico.resources.displayMetrics,
    ).toInt()

    private fun construir(r: ResultadoAnalise, p: Paleta): View {
        val c = r.analise?.principal
        val corNivel = p.corDe(r.nivel?.cor).toArgb()

        val raiz = LinearLayout(servico).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            background = GradientDrawable().apply {
                cornerRadius = dp(22).toFloat()
                setColor(if (p.escuro) 0xF2101828.toInt() else 0xF7FFFFFF.toInt())
                setStroke(dp(2), corNivel)
            }
            elevation = dp(8).toFloat()
            setOnClickListener { esconder() }
            contentDescription = "Análise da corrida. Toque para fechar."
        }
        val moldura = LinearLayout(servico).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(12), 0)
            addView(raiz)
        }

        // Linha 1: valor + categoria + classificação
        raiz.addView(linha(
            texto(Formatos.moeda(r.leitura.valor), 24f, p.texto.toArgb(), negrito = true),
            texto("  " + (r.categoria ?: r.leitura.plataforma.nome), 13f, p.textoSecundario.toArgb()).peso(),
            pilula(nomeNivel(r.nivel), corNivel, p),
        ))

        // Linha 2: as três métricas principais
        val metricas = LinearLayout(servico).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, dp(4))
        }
        metricas.addView(metrica("R$/km", c?.reaisPorKm?.let { Formatos.numero(it) }, corNivel, p).peso())
        metricas.addView(metrica("R$/hora", c?.reaisPorHora?.let { Formatos.numero(it) }, corNivel, p).peso())
        metricas.addView(
            metrica(
                "Resultado", c?.resultado?.let { Formatos.numero(it) },
                (if ((c?.resultado ?: 0.0) >= 0) p.verde else p.vermelho).toArgb(), p,
            ).peso()
        )
        raiz.addView(metricas)

        // Linha 3: distância, tempo, combustível e cenário usado
        val detalhes = buildList {
            c?.km?.let { add(Formatos.km(it)) }
            c?.minutos?.let { add("${it.toInt()} min") }
            add("comb. " + (c?.custoCombustivel?.let { Formatos.moeda(it) } ?: "indisponível"))
            add(if (c?.tipo == TipoCenario.TOTAL) "coleta + viagem" else "só viagem")
        }.joinToString("  •  ")
        raiz.addView(texto(detalhes, 12f, p.textoSecundario.toArgb()))

        if (r.leitura.confianca == Confianca.BAIXA) {
            raiz.addView(texto("⚠ Leitura incerta: " + (r.leitura.alertas.firstOrNull() ?: "confira na tela"), 12f, p.amarelo.toArgb()))
        }
        return moldura
    }

    private fun nomeNivel(n: Nivel?): String = when (n) {
        Nivel.RUIM -> "RUIM"
        Nivel.RAZOAVEL -> "RAZOÁVEL"
        Nivel.BOA -> "BOA"
        Nivel.EXCELENTE -> "EXCELENTE"
        null -> "SEM DADOS"
    }

    private fun texto(t: String, sp: Float, cor: Int, negrito: Boolean = false) = TextView(servico).apply {
        text = t
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        setTextColor(cor)
        if (negrito) typeface = Typeface.DEFAULT_BOLD
    }

    private fun pilula(t: String, cor: Int, p: Paleta) = texto(t, 12f, if (p.escuro) 0xFF04131F.toInt() else 0xFFFFFFFF.toInt(), negrito = true).apply {
        setPadding(dp(10), dp(4), dp(10), dp(4))
        background = GradientDrawable().apply {
            cornerRadius = dp(50).toFloat()
            setColor(cor)
        }
    }

    private fun metrica(rotulo: String, valor: String?, cor: Int, p: Paleta) = LinearLayout(servico).apply {
        orientation = LinearLayout.VERTICAL
        addView(texto(valor ?: "—", 20f, if (valor != null) cor else p.neutro.toArgb(), negrito = true))
        addView(texto(rotulo, 11f, p.textoSecundario.toArgb()))
    }

    private fun linha(vararg vistas: View) = LinearLayout(servico).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        vistas.forEach { addView(it) }
    }

    private fun View.peso(): View = apply {
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
    }
}
