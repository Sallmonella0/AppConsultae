package com.example.appconsultas.ui.screen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.appconsultas.ui.viewmodel.ConsultaViewModel
import com.example.appconsultas.ui.viewmodel.ThemeMode // <-- IMPORTAR O ENUM

@Composable
fun AppDrawerContent(
    viewModel: ConsultaViewModel,
    onCloseDrawer: () -> Unit // Função para fechar o menu ao clicar
) {
    val clienteSelecionado by viewModel.clienteSelecionado.collectAsState()
    val clientes = viewModel.clientesDisponiveis

    // --- MODIFICADO: Usa o novo estado do ViewModel ---
    val themeMode by viewModel.themeMode.collectAsState()

    ModalDrawerSheet {
        // Título para a seleção de clientes
        Text(
            "Selecionar Cliente",
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )

        // Lista de Clientes
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(clientes) { cliente ->
                NavigationDrawerItem(
                    label = { Text(cliente.nome) },
                    selected = cliente == clienteSelecionado,
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    onClick = {
                        viewModel.onClienteSelecionadoChange(cliente)
                        onCloseDrawer()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }

        // Divisor
        Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        // Título para o tema
        Text(
            "Alterar Tema",
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )

        // --- NOVO: Botão Padrão do Sistema ---
        NavigationDrawerItem(
            label = { Text("Padrão do Sistema") },
            selected = themeMode == ThemeMode.System,
            icon = { Icon(Icons.Default.Devices, contentDescription = "Padrão do Sistema") },
            onClick = {
                viewModel.setTheme(ThemeMode.System)
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        // Botão Tema Claro
        NavigationDrawerItem(
            label = { Text("Tema Claro") },
            selected = themeMode == ThemeMode.Light,
            icon = { Icon(Icons.Default.LightMode, contentDescription = "Tema Claro") },
            onClick = {
                viewModel.setTheme(ThemeMode.Light)
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        // Botão Tema Escuro
        NavigationDrawerItem(
            label = { Text("Tema Escuro") },
            selected = themeMode == ThemeMode.Dark,
            icon = { Icon(Icons.Default.DarkMode, contentDescription = "Tema Escuro") },
            onClick = {
                viewModel.setTheme(ThemeMode.Dark)
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}