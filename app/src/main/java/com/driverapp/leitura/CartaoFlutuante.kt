package com.driverapp.leitura

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.driverapp.R
import com.driverapp.calculo.Nivel
import com.driverapp.calculo.TipoCenario
import com.driverapp.leitores.Confianca
import com.driverapp.repositorio
import com.driverapp.ui.Formatos
import com.driverapp.ui.Paleta
import com.driverapp.ui.PaletaClara
import com.driverapp.ui.PaletaEscura
import kotlin.math.abs
import kotlin.math.min

/**
 * Card flutuante COMPACTO mostrado por cima da Uber/99 enquanto a oferta aparece.
 *
 * Layout (igual à referência):
 *   ┃ 2,35   │ 51,00  │ 4,94     –
 *   ┃ /km    │ /hora  │ nota
 *   ┃ R$ 15,30  6,5 km  18 min │ ⦿ Poá
 *
 * - Cores de R$/km e R$/h pelos limites do motorista; faixa à esquerda = classificação geral.
 * - Opacidade do fundo configurável (os números ficam sempre 100% visíveis).
 * - Arrastar para qualquer lugar; ao soltar, encaixa na lateral e nunca sai da tela. Posição salva.
 * - "–" recolhe numa bolinha com o R$/km; tocar na bolinha reabre.
 * - Tocar no card abre/fecha os detalhes, sem perder a análise atual.
 * - Some sozinho depois de alguns segundos (a próxima oferta reaparece no mesmo lugar).
 * Usa a janela de sobreposição dos serviços de acessibilidade (não precisa de outra permissão).
 */
class CartaoFlutuante(private val servico: AccessibilityService) {

    private val gerenciador = servico.getSystemService(WindowManager::class.java)
    private val principal = Handler(Looper.getMainLooper())
    private val esconderDepois = Runnable { esconder() }

    private var janela: FrameLayout? = null
    private var parametros: WindowManager.LayoutParams? = null
    private var ultimo: ResultadoAnalise? = null
    private var paleta: Paleta = PaletaEscura
    private var expandido = false

    suspend fun mostrar(r: ResultadoAnalise) {
        val tema = servico.repositorio.obterConfig().tema
        val escuro = when (tema) {
            "ESCURO" -> true
            "CLARO" -> false
            else -> (servico.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
        paleta = if (escuro) PaletaEscura else PaletaClara
        ultimo = r
        expandido = false
        desenhar()
        agendarEsconder()
    }

    fun esconder() {
        principal.removeCallbacks(esconderDepois)
        janela?.let { v ->
            try {
                gerenciador.removeView(v)
            } catch (_: Exception) {
            }
        }
        janela = null
        parametros = null
    }

    private fun agendarEsconder() {
        principal.removeCallbacks(esconderDepois)
        principal.postDelayed(esconderDepois, if (expandido) 40_000 else 20_000)
    }

    // ------------------------------------------------------------------ janela

    private fun desenhar() {
        val r = ultimo ?: return
        val recolhido = EstadoLeitura.cardRecolhido(servico)
        val conteudo = if (recolhido) bolinha(r) else card(r)

        val existente = janela
        if (existente == null) {
            val nova = FrameLayout(servico)
            nova.addView(conteudo)
            prepararArraste(nova)
            val p = novosParametros()
            try {
                gerenciador.addView(nova, p)
                janela = nova
                parametros = p
            } catch (_: Exception) {
                janela = null
            }
        } else {
            existente.removeAllViews()
            existente.addView(conteudo)
            parametros?.let { p ->
                // Ao trocar entre card e bolinha, reencaixa para não ficar fora da tela.
                existente.post { encaixar(existente, p, salvar = false) }
            }
        }
    }

    private fun novosParametros() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        val salva = EstadoLeitura.posicaoCard(servico)
        if (salva != null) {
            x = salva.first
            y = salva.second
        } else {
            // Padrão: centralizado no topo, logo abaixo da barra de status.
            x = ((larguraTela() - larguraCard()) / 2).coerceAtLeast(0)
            y = dp(36)
        }
    }

    /** Arrastar com o dedo; um toque sem arrastar abre/fecha os detalhes (ou reabre a bolinha). */
    @SuppressLint("ClickableViewAccessibility")
    private fun prepararArraste(v: FrameLayout) {
        val folga = ViewConfiguration.get(servico).scaledTouchSlop
        var inicioToqueX = 0f
        var inicioToqueY = 0f
        var inicioX = 0
        var inicioY = 0
        var arrastando = false

        v.setOnTouchListener { _, e ->
            val p = parametros ?: return@setOnTouchListener false
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    inicioToqueX = e.rawX
                    inicioToqueY = e.rawY
                    inicioX = p.x
                    inicioY = p.y
                    arrastando = false
                    principal.removeCallbacks(esconderDepois) // não some enquanto o motorista mexe
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - inicioToqueX
                    val dy = e.rawY - inicioToqueY
                    if (!arrastando && (abs(dx) > folga || abs(dy) > folga)) arrastando = true
                    if (arrastando) {
                        p.x = inicioX + dx.toInt()
                        p.y = inicioY + dy.toInt()
                        atualizar(v, p)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (arrastando) {
                        encaixar(v, p, salvar = true)
                    } else if (e.actionMasked == MotionEvent.ACTION_UP) {
                        if (EstadoLeitura.cardRecolhido(servico)) {
                            EstadoLeitura.definirCardRecolhido(servico, false)
                        } else {
                            expandido = !expandido
                        }
                        desenhar()
                    }
                    agendarEsconder()
                    true
                }
                else -> false
            }
        }
    }

    private fun encaixar(v: View, p: WindowManager.LayoutParams, salvar: Boolean) {
        val pos = Encaixe.calcular(
            x = p.x, y = p.y,
            largura = v.width, altura = v.height,
            larguraTela = larguraTela(), alturaTela = servico.resources.displayMetrics.heightPixels,
            margem = dp(8), topoMinimo = dp(24), baseReservada = dp(56),
        )
        p.x = pos.x
        p.y = pos.y
        atualizar(v, p)
        if (salvar) EstadoLeitura.salvarPosicaoCard(servico, pos.x, pos.y)
    }

    private fun atualizar(v: View, p: WindowManager.LayoutParams) {
        try {
            gerenciador.updateViewLayout(v, p)
        } catch (_: Exception) {
        }
    }

    // ------------------------------------------------------------------ conteúdo

    private fun card(r: ResultadoAnalise): View {
        val p = paleta
        val c = r.analise?.principal
        val corGeral = p.corDe(r.nivel?.cor).toArgb()

        val raiz = LinearLayout(servico).apply {
            orientation = LinearLayout.HORIZONTAL
            background = fundo(dp(18).toFloat())
            setPadding(dp(10), dp(8), dp(6), dp(8))
            layoutParams = FrameLayout.LayoutParams(larguraCard(), FrameLayout.LayoutParams.WRAP_CONTENT)
            contentDescription = descricaoAcessivel(r)
        }

        // Faixa colorida da classificação geral
        raiz.addView(View(servico).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(2).toFloat()
                setColor(corGeral)
            }
            layoutParams = LinearLayout.LayoutParams(dp(4), LinearLayout.LayoutParams.MATCH_PARENT).apply {
                marginEnd = dp(10)
            }
        })

        val coluna = LinearLayout(servico).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        // Linha 1: R$/km │ R$/hora │ nota  (–)
        val metricas = LinearLayout(servico).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        metricas.addView(metrica(c?.reaisPorKm?.let { Formatos.numero(it) }, "/km", corDe(r.nivelKm)))
        metricas.addView(separador(dp(30)))
        metricas.addView(metrica(c?.reaisPorHora?.let { Formatos.numero(it) }, "/hora", corDe(r.nivelHora)))
        metricas.addView(separador(dp(30)))
        metricas.addView(metrica(r.leitura.notaPassageiro?.let { Formatos.numero(it) }, "nota", p.destaque.toArgb()))
        metricas.addView(texto("–", 22f, p.textoSecundario.toArgb(), negrito = true).apply {
            setPadding(dp(10), 0, dp(6), dp(4))
            contentDescription = "Recolher"
            setOnClickListener {
                EstadoLeitura.definirCardRecolhido(servico, true)
                expandido = false
                desenhar()
                agendarEsconder()
            }
        })
        coluna.addView(metricas)

        // Linha 2: valor, distância, tempo │ destino
        val linha2 = LinearLayout(servico).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, 0)
        }
        linha2.addView(texto(Formatos.moeda(r.leitura.valor), 13f, p.texto.toArgb(), negrito = true))
        val distTempo = listOfNotNull(c?.km?.let { Formatos.km(it) }, c?.minutos?.let { "${it.toInt()} min" }).joinToString("  ")
        if (distTempo.isNotEmpty()) linha2.addView(texto("  $distTempo", 13f, p.textoSecundario.toArgb()))
        r.leitura.destino?.let { destino ->
            linha2.addView(separador(dp(14)).apply {
                (layoutParams as LinearLayout.LayoutParams).apply { marginStart = dp(8); marginEnd = dp(8) }
            })
            linha2.addView(texto(destino, 13f, p.textoSecundario.toArgb()).apply {
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setCompoundDrawablesRelativeWithIntrinsicBounds(
                    ContextCompat.getDrawable(servico, R.drawable.ic_pino), null, null, null,
                )
                compoundDrawablePadding = dp(4)
                TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(p.textoSecundario.toArgb()))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
        if (r.leitura.confianca == Confianca.BAIXA) {
            linha2.addView(texto("  ⚠", 13f, p.amarelo.toArgb(), negrito = true).apply { contentDescription = "Leitura incerta" })
        }
        coluna.addView(linha2)

        if (expandido) coluna.addView(detalhes(r))
        raiz.addView(coluna)
        return raiz
    }

    /** Visualização expandida: os detalhes que não cabem no card compacto. */
    private fun detalhes(r: ResultadoAnalise): View {
        val p = paleta
        val c = r.analise?.principal
        val so = r.analise?.soViagem?.takeIf { r.analise?.total != null }
        val sec = p.textoSecundario.toArgb()
        val linhas = buildList {
            add("${r.leitura.plataforma.nome} • ${r.categoria ?: "categoria indisponível"} • ${nomeNivel(r.nivel)}")
            add("Coleta: " + (r.leitura.coleta?.let { "${it.minutos.toInt()} min • ${Formatos.km(it.km)}" } ?: "indisponível"))
            add("Viagem: " + (r.leitura.viagem?.let { "${it.minutos.toInt()} min • ${Formatos.km(it.km)}" } ?: "indisponível"))
            add("Combustível: " + (c?.custoCombustivel?.let { Formatos.moeda(it) } ?: "indisponível"))
            add("Resultado estimado: " + (c?.resultado?.let { Formatos.moeda(it) } ?: "indisponível"))
            c?.resultadoAposFixos?.let { add("Após custos fixos: ${Formatos.moeda(it)}") }
            if (c != null) add(if (c.tipo == TipoCenario.TOTAL) "Cálculo com coleta + viagem" else "Cálculo só com a viagem")
            so?.let { add("Só a viagem: ${it.reaisPorKm?.let { v -> Formatos.numero(v) } ?: "—"}/km • ${it.reaisPorHora?.let { v -> Formatos.numero(v) } ?: "—"}/h") }
        }
        return LinearLayout(servico).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
            addView(View(servico).apply {
                setBackgroundColor(p.bordaVidro.toArgb())
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply { bottomMargin = dp(6) }
            })
            linhas.forEach { addView(texto(it, 12f, sec)) }
            if (r.leitura.confianca == Confianca.BAIXA) {
                addView(texto("⚠ Leitura incerta: " + r.leitura.alertas.joinToString("; "), 12f, p.amarelo.toArgb()))
            }
        }
    }

    /** Card recolhido: bolinha com o R$/km na cor da classificação. */
    private fun bolinha(r: ResultadoAnalise): View {
        val p = paleta
        val cor = p.corDe(r.nivel?.cor).toArgb()
        val km = r.analise?.principal?.reaisPorKm
        return TextView(servico).apply {
            text = km?.let { Formatos.numero(it) } ?: "R$"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(corDe(r.nivelKm))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(corFundo())
                setStroke(dp(2), cor)
            }
            layoutParams = FrameLayout.LayoutParams(dp(54), dp(54))
            contentDescription = "Card recolhido. Toque para abrir."
        }
    }

    // ------------------------------------------------------------------ peças

    private fun metrica(valor: String?, rotulo: String, cor: Int) = LinearLayout(servico).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        addView(texto(valor ?: "—", 24f, if (valor != null) cor else paleta.neutro.toArgb(), negrito = true).apply {
            includeFontPadding = false
            maxLines = 1
        })
        addView(texto(rotulo, 11f, paleta.textoSecundario.toArgb()).apply { includeFontPadding = false })
    }

    private fun separador(altura: Int) = View(servico).apply {
        setBackgroundColor(paleta.bordaVidro.toArgb())
        layoutParams = LinearLayout.LayoutParams(dp(1), altura)
    }

    private fun texto(t: String, sp: Float, cor: Int, negrito: Boolean = false) = TextView(servico).apply {
        text = t
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        setTextColor(cor)
        if (negrito) typeface = Typeface.DEFAULT_BOLD
    }

    /** Fundo de vidro: só a cor de fundo e a borda ficam translúcidas. */
    private fun fundo(raio: Float) = GradientDrawable().apply {
        cornerRadius = raio
        setColor(corFundo())
        val alfaBorda = (Opacidade.alfa(EstadoLeitura.opacidade(servico)) * 0.6f).toInt()
        val borda = paleta.bordaVidro.toArgb()
        setStroke(dp(1), (alfaBorda shl 24) or (borda and 0x00FFFFFF))
    }

    private fun corFundo(): Int {
        val alfa = Opacidade.alfa(EstadoLeitura.opacidade(servico))
        val base = if (paleta.escuro) 0x10141C else 0xFFFFFF
        return (alfa shl 24) or base
    }

    private fun corDe(n: Nivel?): Int = (if (n == null) paleta.neutro else paleta.corDe(n.cor)).toArgb()

    private fun nomeNivel(n: Nivel?): String = when (n) {
        Nivel.RUIM -> "ruim"
        Nivel.RAZOAVEL -> "razoável"
        Nivel.BOA -> "boa"
        Nivel.EXCELENTE -> "excelente"
        null -> "sem dados"
    }

    private fun descricaoAcessivel(r: ResultadoAnalise): String {
        val c = r.analise?.principal
        return "Oferta de ${Formatos.moeda(r.leitura.valor)}: " +
            "${c?.reaisPorKm?.let { Formatos.numero(it) } ?: "indisponível"} reais por km, " +
            "${c?.reaisPorHora?.let { Formatos.numero(it) } ?: "indisponível"} reais por hora. " +
            "Toque para detalhes, arraste para mover."
    }

    private fun larguraTela() = servico.resources.displayMetrics.widthPixels

    /** Largura adaptável: ocupa a tela em celulares estreitos, mas nunca passa de 360 dp. */
    private fun larguraCard() = min(larguraTela() - dp(16), dp(360))

    private fun dp(v: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), servico.resources.displayMetrics,
    ).toInt()
}
