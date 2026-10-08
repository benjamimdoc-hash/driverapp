package com.driverapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.driverapp.ui.TemaApp
import com.driverapp.ui.calculadora.TelaCalculadora

/**
 * Fase 1: o app abre direto numa calculadora manual de corrida.
 * Serve para conferir no celular que o cálculo bate com o que a Uber/99 mostram.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TemaApp {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TelaCalculadora()
                }
            }
        }
    }
}
