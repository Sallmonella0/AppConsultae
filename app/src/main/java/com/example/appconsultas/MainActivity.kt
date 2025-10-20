package com.example.appconsultas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.example.appconsultas.ui.screen.AppDrawerContent
import com.example.appconsultas.ui.screen.ConsultaScreen
import com.example.appconsultas.ui.theme.AppConsultasTheme
import com.example.appconsultas.ui.viewmodel.ConsultaViewModel
import com.example.appconsultas.ui.viewmodel.ThemeMode // <-- IMPORTAR O ENUM
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: ConsultaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            // --- LÓGICA DO TEMA MODIFICADA ---
            val themeMode by viewModel.themeMode.collectAsState()
            val useDarkTheme = when (themeMode) {
                // Se for Light ou Dark, força esse tema
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
                // Se for System, deteta o tema do telemóvel
                ThemeMode.System -> isSystemInDarkTheme()
            }
            // --- FIM DA LÓGICA ---

            // Aplica o tema escolhido (agora baseado na lógica acima)
            AppConsultasTheme(darkTheme = useDarkTheme) {

                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        AppDrawerContent(
                            viewModel = viewModel,
                            onCloseDrawer = {
                                scope.launch {
                                    drawerState.close()
                                }
                            }
                        )
                    }
                ) {
                    ConsultaScreen(
                        viewModel = viewModel,
                        drawerState = drawerState,
                        scope = scope
                    )
                }
            }
        }
    }
}