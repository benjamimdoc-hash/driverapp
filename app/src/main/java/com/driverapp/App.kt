package com.driverapp

import android.app.Application
import com.driverapp.dados.BancoDados
import com.driverapp.dados.Repositorio
import com.driverapp.jornada.Notificacoes

/** Ponto de partida do app: cria o banco e os canais de notificação uma única vez. */
class App : Application() {

    lateinit var repositorio: Repositorio
        private set

    override fun onCreate() {
        super.onCreate()
        repositorio = Repositorio(BancoDados.criar(this))
        Notificacoes.criarCanais(this)
    }
}

/** Atalho para pegar o repositório a partir de qualquer Context. */
val android.content.Context.repositorio: Repositorio
    get() = (applicationContext as App).repositorio
