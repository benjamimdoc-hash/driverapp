package com.driverapp.jornada

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
import com.driverapp.calculo.EstadoJornada
import com.driverapp.calculo.Financeiro
import com.driverapp.dados.ConfiguracaoEntity
import com.driverapp.dados.JornadaEntity
import com.driverapp.dados.custoCombustivelPorKm
import com.driverapp.dados.metas
import com.driverapp.dados.paraDominio
import com.driverapp.leitura.Encaixe
import com.driverapp.leitura.Opacidade
import com.driverapp.ui.Formatos
import com.driverapp.ui.Paleta
import com.driverapp.ui.PaletaClara
import com.driverapp.ui.PaletaEscura
import kotlin.math.abs
import kotlin.math.min

/** Tudo que o painel precisa mostrar (montado pelo serviço da jornada). */
data class DadosPainel(
    val jornada: JornadaEntity,
    val config: ConfiguracaoEntity,
    val custoFixoHora: Double,
    val faturadoJornada: Double,
    val corridasJornada: Int,
    val faturadoHoje: Double,
)

/** O que os botões do painel fazem (implementado pelo serviço da jornada). */
interface AcoesPainel {
    fun pausar()
    fun retomar()
    fun finalizar()
    fun abrirApp()
    fun ajustarCombustivel(deltaPreco: Double, deltaConsumo: Double)
}

/**
 * FASE 4 — Painel flutuante da jornada, por cima de qualquer app.
 *
 *  - Recolhido: ícone pequeno (bolinha) com o tempo da jornada e a cor do estado.
 *  - Aberto: tempo, km, corridas, resultado estimado, meta do dia, botões de pausar/retomar
 *    e finalizar, abrir o app e atalho para ajustar preço e consumo do combustível.
 *  - Arrastável, encaixa na lateral, posição salva, opacidade do fundo configurável.
 *  - Fechar o painel (✕) só recolhe: a jornada continua.
 *  - Some quando o app está aberto na tela e quando a jornada é finalizada.
 * Precisa da permissão "Sobrepor a outros apps", concedida pelo motorista.
 */
class PainelFlutuante(private val contexto: Context, private val acoes: AcoesPainel) {

    private val gerenciador = contexto.getSystemService(WindowManager::class.java)
    private var janela: FrameLayout? = null
    private var parametros: WindowManager.LayoutParams? = null
    private var dados: DadosPainel? = null
    private var expandido = false
    private var paleta: Paleta = PaletaEscura
    private var assinaturaDesenho: String? = null
    private var confirmarFimAte = 0L

    // Textos atualizados a cada segundo, sem redesenhar o painel inteiro.
    private var tvTempo: TextView? = null
    private var tvKm: TextView? = null
    private var tvCorridas: TextView? = null
    private var tvResultado: TextView? = null
    private var tvMeta: TextView? = null
    private var barraMeta: View? = null
    private var restoMeta: View? = null
    private var tvCombustivel: TextView? = null
    private var tvFinalizar: TextView? = null

    /** Chamado pelo serviço quando os dados ou as condições mudam. */
    fun atualizar(novos: DadosPainel?, appVisivel: Boolean) {
        dados = novos
        val mostrar = novos != null && !appVisivel && PreferenciasPainel.ativo(contexto) && PreferenciasPainel.temPermissao(contexto)
        if (!mostrar) {
            remover()
            return
        }
        val d = novos!!
        paleta = if (escuro(d.config.tema)) PaletaEscura else PaletaClara
        val assinatura = listOf(expandido, d.jornada.estado, paleta.escuro, PreferenciasPainel.opacidade(contexto)).joinToString("|")
        if (janela == null || assinatura != assinaturaDesenho) desenhar(assinatura)
        preencher()
    }

    /** Atualização a cada segundo (relógio). */
    fun tique() {
        if (janela != null) preencher()
    }

    fun remover() {
        janela?.let {
            try {
                gerenciador.removeView(it)
            } catch (_: Exception) {
            }
        }
        janela = null
        parametros = null
        assinaturaDesenho = null
    }

    // ------------------------------------------------------------------ janela

    private fun desenhar(assinatura: String) {
        assinaturaDesenho = assinatura
        val conteudo = if (expandido) painel() else bolinha()
        val existente = janela
        if (existente == null) {
            val nova = FrameLayout(contexto)
            nova.addView(conteudo)
            prepararArraste(nova)
            val p = novosParametros()
            try {
                gerenciador.addView(nova, p)
                janela = nova
                parametros = p
            } catch (_: Exception) {
                janela = null
                assinaturaDesenho = null
            }
        } else {
            existente.removeAllViews()
            existente.addView(conteudo)
            parametros?.let { p -> existente.post { encaixar(existente, p, salvar = false) } }
        }
    }

    private fun novosParametros() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        val salva = PreferenciasPainel.posicao(contexto)
        if (salva != null) {
            x = salva.first
            y = salva.second
        } else {
            // Padrão: lateral direita, um pouco abaixo do meio (longe do card de oferta, que fica no topo).
            x = larguraTela() - dp(60)
            y = (contexto.resources.displayMetrics.heightPixels * 0.55).toInt()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun prepararArraste(v: FrameLayout) {
        val folga = ViewConfiguration.get(contexto).scaledTouchSlop
        var tx = 0f
        var ty = 0f
        var ix = 0
        var iy = 0
        var arrastando = false
        v.setOnTouchListener { _, e ->
            val p = parametros ?: return@setOnTouchListener false
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    tx = e.rawX; ty = e.rawY; ix = p.x; iy = p.y; arrastando = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - tx
                    val dy = e.rawY - ty
                    if (!arrastando && (abs(dx) > folga || abs(dy) > folga)) arrastando = true
                    if (arrastando) {
                        p.x = ix + dx.toInt()
                        p.y = iy + dy.toInt()
                        atualizarJanela(v, p)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (arrastando) encaixar(v, p, salvar = true)
                    else if (e.actionMasked == MotionEvent.ACTION_UP && !expandido) {
                        expandido = true
                        dados?.let { atualizar(it, appVisivel = false) }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun encaixar(v: View, p: WindowManager.LayoutParams, salvar: Boolean) {
        val pos = Encaixe.calcular(
            x = p.x, y = p.y, largura = v.width, altura = v.height,
            larguraTela = larguraTela(), alturaTela = contexto.resources.displayMetrics.heightPixels,
            margem = dp(6), topoMinimo = dp(24), baseReservada = dp(56),
        )
        p.x = pos.x
        p.y = pos.y
        atualizarJanela(v, p)
        if (salvar) PreferenciasPainel.salvarPosicao(contexto, pos.x, pos.y)
    }

    private fun atualizarJanela(v: View, p: WindowManager.LayoutParams) {
        try {
            gerenciador.updateViewLayout(v, p)
        } catch (_: Exception) {
        }
    }

    // ------------------------------------------------------------------ conteúdo

    private fun bolinha(): View {
        limparReferencias()
        val d = dados
        val ativa = d?.jornada?.estado == EstadoJornada.ATIVA.name
        val cor = (if (ativa) paleta.verde else paleta.amarelo).toArgb()
        val tv = TextView(contexto).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(paleta.texto.toArgb())
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(corFundo())
                setStroke(dp(2), cor)
            }
            layoutParams = FrameLayout.LayoutParams(dp(52), dp(52))
            contentDescription = "Painel da jornada. Toque para abrir, arraste para mover."
        }
        tvTempo = tv
        return tv
    }

    private fun painel(): View {
        limparReferencias()
        val p = paleta
        val d = dados
        val ativa = d?.jornada?.estado == EstadoJornada.ATIVA.name

        val raiz = LinearLayout(contexto).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(10), dp(12))
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(corFundo())
                setStroke(dp(1), p.bordaVidro.toArgb())
            }
            layoutParams = FrameLayout.LayoutParams(min(larguraTela() - dp(16), dp(300)), FrameLayout.LayoutParams.WRAP_CONTENT)
        }

        // Cabeçalho: estado + fechar (recolher)
        raiz.addView(linha(
            texto(if (ativa) "● Jornada ativa" else "● Pausada", 13f, (if (ativa) p.verde else p.amarelo).toArgb(), true).peso(),
            botaoTexto("✕", p.textoSecundario.toArgb(), "Recolher painel") {
                expandido = false
                dados?.let { atualizar(it, appVisivel = false) }
            },
        ))

        val tempo = texto("", 30f, p.texto.toArgb(), true)
        tvTempo = tempo
        raiz.addView(tempo)

        // Métricas: km │ corridas │ resultado
        val metricas = linha()
        tvKm = metrica(metricas, "rodados")
        tvCorridas = metrica(metricas, "corridas")
        tvResultado = metrica(metricas, "resultado est.")
        raiz.addView(metricas)

        // Meta do dia
        val meta = texto("", 12f, p.textoSecundario.toArgb()).apply { setPadding(0, dp(8), 0, dp(4)) }
        tvMeta = meta
        raiz.addView(meta)
        val trilho = LinearLayout(contexto).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply { cornerRadius = dp(4).toFloat(); setColor(p.bordaVidro.toArgb()) }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(6))
        }
        val barra = View(contexto).apply {
            background = GradientDrawable().apply { cornerRadius = dp(4).toFloat(); setColor(p.destaque.toArgb()) }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0f)
        }
        val resto = View(contexto).apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f) }
        barraMeta = barra
        restoMeta = resto
        trilho.addView(barra)
        trilho.addView(resto)
        raiz.addView(trilho)

        // Botões da jornada
        val botoes = linha().apply { setPadding(0, dp(10), 0, 0) }
        botoes.addView(
            if (ativa) botao("Pausar", p.amarelo.toArgb()) { acoes.pausar() }
            else botao("Retomar", p.verde.toArgb()) { acoes.retomar() }
        )
        botoes.addView(View(contexto).apply { layoutParams = LinearLayout.LayoutParams(dp(8), 1) })
        val finalizar = botao("Finalizar", p.vermelho.toArgb()) {
            val agora = System.currentTimeMillis()
            if (agora < confirmarFimAte) {
                confirmarFimAte = 0
                acoes.finalizar()
            } else {
                // Confirmação em dois toques, para não finalizar sem querer.
                confirmarFimAte = agora + 4_000
                preencher()
            }
        }
        tvFinalizar = finalizar
        botoes.addView(finalizar)
        raiz.addView(botoes)

        // Atalho de combustível
        val combustivel = texto("", 12f, p.textoSecundario.toArgb()).apply { setPadding(0, dp(10), 0, dp(2)) }
        tvCombustivel = combustivel
        raiz.addView(combustivel)
        raiz.addView(linha(
            mini("− R$", "Diminuir preço do litro") { acoes.ajustarCombustivel(-0.05, 0.0) },
            mini("+ R$", "Aumentar preço do litro") { acoes.ajustarCombustivel(0.05, 0.0) },
            mini("− km/l", "Diminuir consumo") { acoes.ajustarCombustivel(0.0, -0.5) },
            mini("+ km/l", "Aumentar consumo") { acoes.ajustarCombustivel(0.0, 0.5) },
        ))

        raiz.addView(botaoTexto("Abrir o app", p.destaque.toArgb(), "Abrir o app") { acoes.abrirApp() }.apply {
            setPadding(0, dp(10), 0, 0)
        })
        return raiz
    }

    /** Preenche os textos com os dados atuais (chamado a cada segundo). */
    private fun preencher() {
        val d = dados ?: return
        val j = d.jornada.paraDominio()
        val agora = System.currentTimeMillis()
        val ms = j.msProdutivos(agora)

        if (!expandido) {
            val min = ms / 60_000
            tvTempo?.text = "%d:%02d".format(min / 60, min % 60)
            return
        }
        tvTempo?.text = Formatos.relogio(ms)
        tvKm?.text = Formatos.km(j.km)
        tvCorridas?.text = d.corridasJornada.toString()
        val r = Financeiro.resumir(d.faturadoJornada, j.km, ms / 3_600_000.0, d.config.custoCombustivelPorKm(), d.custoFixoHora)
        val res = r.resultadoEstimado
        tvResultado?.text = res?.let { Formatos.moeda(it) } ?: "—"
        tvResultado?.setTextColor(
            (when {
                res == null -> paleta.neutro
                res < 0 -> paleta.vermelho
                else -> paleta.verde
            }).toArgb()
        )

        val meta = d.config.metas()?.metaDiaria
        if (meta != null && meta > 0) {
            val fracao = (d.faturadoHoje / meta).coerceIn(0.0, 1.0).toFloat()
            tvMeta?.text = "Meta de hoje: ${Formatos.moeda(d.faturadoHoje)} de ${Formatos.moeda(meta)} (${(fracao * 100).toInt()}%)"
            (barraMeta?.layoutParams as? LinearLayout.LayoutParams)?.weight = fracao
            (restoMeta?.layoutParams as? LinearLayout.LayoutParams)?.weight = 1f - fracao
            barraMeta?.requestLayout()
        } else {
            tvMeta?.text = "Meta diária não configurada"
        }

        val preco = d.config.precoLitro
        val consumo = d.config.kmPorLitro
        tvCombustivel?.text = "Combustível: ${Formatos.moeda(preco)}/L • ${consumo?.let { Formatos.numero(it, 1) } ?: "—"} km/l • " +
            "${Formatos.moeda(d.config.custoCombustivelPorKm())}/km"

        tvFinalizar?.text = if (agora < confirmarFimAte) "Confirmar?" else "Finalizar"
    }

    // ------------------------------------------------------------------ peças

    private fun limparReferencias() {
        tvTempo = null; tvKm = null; tvCorridas = null; tvResultado = null; tvMeta = null
        barraMeta = null; restoMeta = null; tvCombustivel = null; tvFinalizar = null
    }

    private fun linha(vararg vistas: View) = LinearLayout(contexto).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        vistas.forEach { addView(it) }
    }

    private fun metrica(pai: LinearLayout, rotulo: String): TextView {
        val valor = texto("—", 16f, paleta.texto.toArgb(), true)
        pai.addView(LinearLayout(contexto).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(valor)
            addView(texto(rotulo, 11f, paleta.textoSecundario.toArgb()))
        })
        return valor
    }

    private fun texto(t: String, sp: Float, cor: Int, negrito: Boolean = false) = TextView(contexto).apply {
        text = t
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        setTextColor(cor)
        if (negrito) typeface = Typeface.DEFAULT_BOLD
    }

    private fun botao(t: String, cor: Int, aoTocar: () -> Unit) = texto(t, 14f, if (paleta.escuro) 0xFF04131F.toInt() else 0xFFFFFFFF.toInt(), true).apply {
        gravity = Gravity.CENTER
        setPadding(dp(8), dp(10), dp(8), dp(10))
        background = GradientDrawable().apply { cornerRadius = dp(12).toFloat(); setColor(cor) }
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        setOnClickListener { aoTocar() }
    }

    private fun mini(t: String, descricao: String, aoTocar: () -> Unit) = texto(t, 12f, paleta.texto.toArgb(), true).apply {
        gravity = Gravity.CENTER
        setPadding(dp(4), dp(8), dp(4), dp(8))
        background = GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            setColor(paleta.vidroForte.toArgb())
            setStroke(dp(1), paleta.bordaVidro.toArgb())
        }
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(4) }
        contentDescription = descricao
        setOnClickListener { aoTocar() }
    }

    private fun botaoTexto(t: String, cor: Int, descricao: String, aoTocar: () -> Unit) = texto(t, 15f, cor, true).apply {
        setPadding(dp(10), dp(4), dp(6), dp(4))
        contentDescription = descricao
        setOnClickListener { aoTocar() }
    }

    private fun View.peso(): View = apply {
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
    }

    private fun corFundo(): Int {
        val alfa = Opacidade.alfa(PreferenciasPainel.opacidade(contexto))
        val base = if (paleta.escuro) 0x10141C else 0xFFFFFF
        return (alfa shl 24) or base
    }

    private fun escuro(tema: String?): Boolean = when (tema) {
        "ESCURO" -> true
        "CLARO" -> false
        else -> (contexto.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    private fun larguraTela() = contexto.resources.displayMetrics.widthPixels

    private fun dp(v: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), contexto.resources.displayMetrics,
    ).toInt()
}
