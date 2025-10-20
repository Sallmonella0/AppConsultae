// Ficheiro: app/src/main/java/com/example/appconsultas/ui/screen/ConsultaScreen.kt
// (Apenas para confirmação - este código está correto)

package com.example.appconsultas.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.appconsultas.data.ConsultaRecord
import com.example.appconsultas.data.DateUtils
import com.example.appconsultas.ui.viewmodel.Coluna
import com.example.appconsultas.ui.viewmodel.ColunaFiltro
import com.example.appconsultas.ui.viewmodel.ConsultaViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// --- ESTES SÃO OS IMPORTS CORRETOS (do Material 3) ---
import androidx.compose.material3.pulltorefresh.PullRefreshIndicator
import androidx.compose.material3.pulltorefresh.pullRefresh
import androidx.compose.material3.pulltorefresh.rememberPullRefreshState
// --- FIM DOS IMPORTS ---


@OptIn(ExperimentalMaterial3Api::class) // Necessário para o M3 PullRefresh
@Composable
fun ConsultaScreen(
    viewModel: ConsultaViewModel,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    val registos by viewModel.registosFinais.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val registoSelecionado by viewModel.registoSelecionado.collectAsState()
    val context = LocalContext.current
    var showExportMenu by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // --- LÓGICA DO PULL TO REFRESH (M3) ---
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = { viewModel.carregarDadosIniciais() }
    )
    // --- FIM DA LÓGICA ---

    // Launchers (sem alteração)
    val csvFileSaverLauncher = rememberLauncherForActivityResult(/*...código omitido...*/)
    val xmlFileSaverLauncher = rememberLauncherForActivityResult(/*...código omitido...*/)

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Consultas") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch { drawerState.open() }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Abrir Menu"
                        )
                    }
                },
                actions = {
                    MenuDeOrdenacao(viewModel = viewModel)
                }
            )
        },
        floatingActionButton = {
            // FAB (Sem alteração)
            Box {
                FloatingActionButton(
                    onClick = { showExportMenu = true }
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Exportar Dados")
                }

                DropdownMenu(
                    expanded = showExportMenu,
                    onDismissRequest = { showExportMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Exportar para CSV") },
                        onClick = {
                            showExportMenu = false
                            val timestamp = System.currentTimeMillis()
                            csvFileSaverLauncher.launch("consulta_$timestamp.csv")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Exportar para XML") },
                        onClick = {
                            showExportMenu = false
                            val timestamp = System.currentTimeMillis()
                            xmlFileSaverLauncher.launch("consulta_$timestamp.xml")
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            ControlesDaConsulta(viewModel)
            Spacer(modifier = Modifier.height(16.dp))

            // --- ÁREA DA LISTA MODIFICADA (M3) ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState) // Aplicador do M3
            ) {
                ListaDeRegistosEmCartoes(
                    registos = registos,
                    onRegistoClick = { viewModel.onRegistoClicked(it) }
                )

                // Indicador do M3
                PullRefreshIndicator(
                    refreshing = isLoading,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            }
            // --- FIM DA ÁREA DA LISTA ---
        }

        registoSelecionado?.let { record ->
            DetailsDialog(
                record = record,
                onDismiss = { viewModel.onDetailsDialogDismiss() }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlesDaConsulta(viewModel: ConsultaViewModel) {
    val textoIdConsulta by viewModel.textoIdConsulta.collectAsState()
    val textoDoFiltro by viewModel.textoDoFiltro.collectAsState()
    val colunaFiltro by viewModel.colunaFiltroSelecionada.collectAsState()

    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = textoIdConsulta,
                onValueChange = { viewModel.onTextoIdChange(it) },
                label = { Text("Consultar por ID") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { viewModel.consultarPorId() }) {
                Icon(Icons.Default.Search, contentDescription = "Consultar por ID")
            }
            // O botão de Refresh foi removido
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = textoDoFiltro,
                onValueChange = { viewModel.onTextoDoFiltroChange(it) },
                label = { Text("Filtrar na lista...") },
                leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = "Ícone de filtro") },
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

// ==================================================================
// O RESTANTE DOS COMPOSABLES (Menu, Lista, Card, Dialogs)
// (Estão iguais à resposta anterior e não precisam de alteração)
// ==================================================================

@Composable
fun MenuDeOrdenacao(viewModel: ConsultaViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val colunaOrdenacao by viewModel.colunaOrdenacao.collectAsState()
    val ordemDescendente by viewModel.ordemDescendente.collectAsState()

    val colunasParaOrdenar = listOf(
        Coluna.DATA_HORA,
        Coluna.PLACA,
        Coluna.ID_MENSAGEM,
        Coluna.TRACK_ID,
        Coluna.LATITUDE,
        Coluna.LONGITUDE
    )

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Sort, contentDescription = "Ordenar por", tint = MaterialTheme.colorScheme.onPrimary)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            colunasParaOrdenar.forEach { coluna ->
                DropdownMenuItem(
                    text = { Text(coluna.displayName) },
                    onClick = {
                        viewModel.onOrdenarPor(coluna)
                        expanded = false
                    },
                    leadingIcon = {
                        if (colunaOrdenacao == coluna) {
                            Icon(
                                imageVector = if (ordemDescendente) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = "Ordem atual"
                            )
                        } else {
                            Spacer(modifier = Modifier.size(24.dp))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ListaDeRegistosEmCartoes(
    registos: List<ConsultaRecord>,
    onRegistoClick: (ConsultaRecord) -> Unit
) {
    if (registos.isEmpty()) {
        EstadoVazio()
        return
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(registos) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 500))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(registos, key = { it.idMensagem }) { registo ->
                RegistoCard(
                    registo = registo,
                    onClick = { onRegistoClick(registo) }
                )
            }
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
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "TrackID: ${registo.trackId ?: "N/A"}",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = DateUtils.formatarDataHora(registo.dataHora),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun EstadoVazio() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Nenhum Resultado", style = MaterialTheme.typography.headlineSmall)
        Text(
            "A sua busca não encontrou registos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Puxe para atualizar",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DetailsDialog(record: ConsultaRecord, onDismiss: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detalhes do Registo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val details = listOf(
                    "Data/Hora" to DateUtils.formatarDataHora(record.dataHora),
                    "IDMENSAGEM" to (record.idMensagem.toString()),
                    "Latitude" to (record.latitude?.toString() ?: "N/A"),
                    "Longitude" to (record.longitude?.toString() ?: "N/A"),
                    "Placa" to (record.placa ?: "N/A"),
                    "TrackID" to (record.trackId ?: "N/A")
                )
                details.forEach { (key, value) ->
                    Row {
                        Text("$key:", fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp))
                        Text(value)
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (record.latitude != null && record.longitude != null) {
                    TextButton(
                        onClick = {
                            val gmmIntentUri = Uri.parse("geo:${record.latitude},${record.longitude}?q=${record.latitude},${record.longitude}(Registo)")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            try {
                                context.startActivity(mapIntent)
                            } catch (e: Exception) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, gmmIntentUri))
                            }
                        }
                    ) {
                        Text("Ver no Mapa")
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onDismiss) {
                    Text("Fechar")
                }
            }
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