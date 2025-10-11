// Ficheiro: MainActivity.kt
package com.example.appconsultas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.appconsultas.ui.screen.ConsultaScreen
import com.example.appconsultas.ui.theme.AppConsultasTheme // O nome do tema pode variar
import com.example.appconsultas.ui.viewmodel.ConsultaViewModel

class MainActivity : ComponentActivity() {
    // Cria uma instância do nosso ViewModel
    private val viewModel: ConsultaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppConsultasTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Chama a nossa tela principal, passando o ViewModel
                    ConsultaScreen(viewModel = viewModel)
                }
            }
        }
    }
}