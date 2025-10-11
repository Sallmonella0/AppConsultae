// Ficheiro: ui/screen/ConsultaScreen.kt (VERSÃO COMPLETA E CORRIGIDA)
package com.example.appconsultas.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.appconsultas.data.ConsultaRecord
import com.example.appconsultas.ui.viewmodel.Coluna
import com.example.appconsultas.ui.viewmodel.ColunaFiltro
import com.example.appconsultas.ui.viewmodel.ConsultaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultaScreen(viewModel: ConsultaViewModel) {
    // A correção do erro anterior está aqui: removemos o `initialValue`
    val registos by viewModel.registosFinais.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var mostrarDialogoColunas by remember { mutableStateOf(false) }
    val registoSelecionado by viewModel.registoSelecionado.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App de Consulta") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            ControlesDaConsulta(viewModel, onMostrarDialogoColunas = { mostrarDialogoColunas = true })
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(50.dp))
                }
            } else {
                ListaDeRegistos(
                    viewModel = viewModel,
                    registos = registos,
                    onRegistoClick = { viewModel.onRegistoClicked(it) }
                )
            }
        }

        if (mostrarDialogoColunas) {
            val colunasVisiveis by viewModel.colunasVisiveis.collectAsState()
            ColumnSelectionDialog(
                colunasVisiveis = colunasVisiveis,
                onDismiss = { mostrarDialogoColunas = false },
                onToggle = { viewModel.toggleVisibilidadeColuna(it) }
            )
        }

        registoSelecionado?.let { record ->
            DetailsDialog(
                record = record,
                onDismiss = { viewModel.onDetailsDialogDismiss() }
            )
        }
    }
}

@Composable
fun ControlesDaConsulta(viewModel: ConsultaViewModel, onMostrarDialogoColunas: () -> Unit) {
    val textoIdConsulta by viewModel.textoIdConsulta.collectAsState()
    val textoDoFiltro by viewModel.textoDoFiltro.collectAsState()
    val colunaFiltro by viewModel.colunaFiltroSelecionada.collectAsState()

    Column {
        OutlinedTextField(
            value = textoIdConsulta,
            onValueChange = { viewModel.onTextoIdChange(it) },
            label = { Text("Consultar por IDMENSAGEM") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Ícone de busca") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.consultarPorId() }, modifier = Modifier.weight(1f)) { Text("Consultar") }
            Button(onClick = { viewModel.carregarDadosIniciais() }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Refresh, contentDescription = "Atualizar"); Spacer(Modifier.size(ButtonDefaults.IconSpacing)); Text("Atualizar")
            }
            Button(onClick = onMostrarDialogoColunas, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.List, contentDescription = "Colunas"); Spacer(Modifier.size(ButtonDefaults.IconSpacing)); Text("Colunas")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Divider()

        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = textoDoFiltro,
                onValueChange = { viewModel.onTextoDoFiltroChange(it) },
                label = { Text("Filtrar na lista...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Ícone de filtro") },
                trailingIcon = {
                    if (textoDoFiltro.isNotEmpty()) {
                        IconButton(onClick = { viewModel.limparFiltros() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpar filtro")
                        }
                    }
                },
                modifier = Modifier.weight(2f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            FiltroColunaDropDown(
                selected = colunaFiltro,
                onSelectedChange = { viewModel.onColunaFiltroChange(it) },
                modifier = Modifier.weight(1.5f)
            )
        }
    }
}

@Composable
fun ListaDeRegistos(
    viewModel: ConsultaViewModel,
    registos: List<ConsultaRecord>,
    onRegistoClick: (ConsultaRecord) -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsState()

    if (registos.isEmpty() && !isLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Sem Resultados", style = MaterialTheme.typography.headlineSmall)
            Text("A sua busca não encontrou registos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(registos) { registo ->
            RegistoCard(
                registo = registo,
                onClick = { onRegistoClick(registo) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistoCard(registo: ConsultaRecord, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = registo.placa ?: "Sem Placa",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "TrackID: ${registo.trackId ?: "N/A"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = registo.dataHora ?: "Data indisponível",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun DetailsDialog(record: ConsultaRecord, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detalhes do Registo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val details = listOf(
                    "IDMENSAGEM" to record.idMensagem,
                    "TrackID" to record.trackId,
                    "Placa" to record.placa,
                    "Latitude" to record.latitude,
                    "Longitude" to record.longitude,
                    "Data/Hora" to record.dataHora
                )
                details.forEach { (key, value) ->
                    Row {
                        Text("$key:", fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp))
                        Text(value?.toString() ?: "N/A")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Fechar") }
        }
    )
}

@Composable
fun ColumnSelectionDialog(
    colunasVisiveis: Set<Coluna>,
    onDismiss: () -> Unit,
    onToggle: (Coluna) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Selecionar Colunas Visíveis") },
        text = {
            Column {
                Coluna.values().forEach { coluna ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(coluna) }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(checked = colunasVisiveis.contains(coluna), onCheckedChange = { onToggle(coluna) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(coluna.displayName)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("OK") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltroColunaDropDown(
    selected: ColunaFiltro,
    onSelectedChange: (ColunaFiltro) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val items = ColunaFiltro.values()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Coluna") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.name) },
                    onClick = {
                        onSelectedChange(item)
                        expanded = false
                    }
                )
            }
        }
    }
}