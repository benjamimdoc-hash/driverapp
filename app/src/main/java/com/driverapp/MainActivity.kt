package com.driverapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.driverapp.dados.ConfiguracaoEntity
import com.driverapp.ui.RaizApp
import com.driverapp.ui.TemaApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // O tema escolhido nos Ajustes vale para todas as telas.
            val config by repositorio.config.collectAsStateWithLifecycle<ConfiguracaoEntity?>(initialValue = null)
            TemaApp(config?.tema) {
                RaizApp(config)
            }
        }
    }
}
