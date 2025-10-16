package com.example.appconsultas.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.appconsultas.data.ConsultaRecord
import com.example.appconsultas.ui.viewmodel.Coluna
import com.example.appconsultas.ui.viewmodel.ColunaFiltro
import com.example.appconsultas.ui.viewmodel.ConsultaViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultaScreen(viewModel: ConsultaViewModel) {
    val registos by viewModel.registosFinais.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var mostrarDialogoColunas by remember { mutableStateOf(false) }
    val registoSelecionado by viewModel.registoSelecionado.collectAsState()
    val context = LocalContext.current
    var showExportMenu by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val csvFileSaverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri: Uri? ->
            uri?.let {
                try {
                    val content = viewModel.gerarConteudoCSV()
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(content.toByteArray())
                    }
                    scope.launch { snackbarHostState.showSnackbar("Ficheiro CSV guardado!") }
                } catch (e: Exception) {
                    scope.launch { snackbarHostState.showSnackbar("Erro ao guardar: ${e.message}") }
                }
            }
        }
    )

    val xmlFileSaverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/xml"),
        onResult = { uri: Uri? ->
            uri?.let {
                try {
                    val content = viewModel.gerarConteudoXML()
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(content.toByteArray())
                    }
                    scope.launch { snackbarHostState.showSnackbar("Ficheiro XML guardado!") }
                } catch (e: Exception) {
                    scope.launch { snackbarHostState.showSnackbar("Erro ao guardar: ${e.message}") }
                }
            }
        }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Consultas") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    ThemeMenu(viewModel = viewModel)
                }
            )
        },
        floatingActionButton = {
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
            ControlesDaConsulta(viewModel, onMostrarDialogoColunas = { mostrarDialogoColunas = true })
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(50.dp))
                }
            } else {
                TabelaDeRegistos(
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
            IconButton(onClick = { viewModel.carregarDadosIniciais() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Atualizar Lista")
            }
            IconButton(onClick = onMostrarDialogoColunas) {
                Icon(Icons.Default.Visibility, contentDescription = "Selecionar Colunas")
            }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabelaDeRegistos(
    viewModel: ConsultaViewModel,
    registos: List<ConsultaRecord>,
    onRegistoClick: (ConsultaRecord) -> Unit
) {
    val colunasVisiveis by viewModel.colunasVisiveis.collectAsState()
    val colunaOrdenacao by viewModel.colunaOrdenacao.collectAsState()
    val ordemDescendente by viewModel.ordemDescendente.collectAsState()

    if (registos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Sem Resultados", style = MaterialTheme.typography.headlineSmall)
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize()) {
        stickyHeader {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 4.dp)
            ) {
                for (coluna in Coluna.values()) {
                    if (colunasVisiveis.contains(coluna)) {
                        TableCell(
                            text = coluna.displayName,
                            weight = coluna.getWeight(),
                            isHeader = true,
                            onClick = { viewModel.onOrdenarPor(coluna) }
                        ) {
                            if (colunaOrdenacao == coluna) {
                                Icon(
                                    if (ordemDescendente) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = "Ordenação",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        itemsIndexed(registos, key = { _, item -> item.idMensagem }) { index, registo ->
            val corFundo = if (index % 2 == 0) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(corFundo)
                    .clickable { onRegistoClick(registo) }
                    .animateItemPlacement()
            ) {
                if (colunasVisiveis.contains(Coluna.DATA_HORA)) {
                    TableCell(text = registo.dataHora ?: "N/A", weight = Coluna.DATA_HORA.getWeight())
                }
                if (colunasVisiveis.contains(Coluna.ID_MENSAGEM)) {
                    TableCell(text = registo.idMensagem.toString(), weight = Coluna.ID_MENSAGEM.getWeight())
                }
                if (colunasVisiveis.contains(Coluna.PLACA)) {
                    TableCell(
                        text = registo.placa ?: "N/A",
                        weight = Coluna.PLACA.getWeight(),
                        fontWeight = FontWeight.Bold
                    )
                }
                if (colunasVisiveis.contains(Coluna.TRACK_ID)) {
                    TableCell(text = registo.trackId ?: "N/A", weight = Coluna.TRACK_ID.getWeight())
                }
            }
            Divider(thickness = 0.5.dp)
        }
    }
}

fun Coluna.getWeight(): Float {
    return when (this) {
        Coluna.DATA_HORA -> 3.0f
        Coluna.ID_MENSAGEM -> 2.5f
        Coluna.PLACA -> 1.5f
        Coluna.TRACK_ID -> 1.5f
        Coluna.LATITUDE -> 2.0f
        Coluna.LONGITUDE -> 2.0f
    }
}

@Composable
fun RowScope.TableCell(
    text: String,
    weight: Float,
    isHeader: Boolean = false,
    fontWeight: FontWeight = FontWeight.Normal,
    textColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    headerContent: @Composable RowScope.() -> Unit = {}
) {
    val finalFontWeight = if (isHeader) FontWeight.Bold else fontWeight

    val modifier = Modifier
        .weight(weight)
        .padding(horizontal = 8.dp, vertical = 16.dp)

    val content: @Composable () -> Unit = {
        Text(
            text = text,
            style = if (isHeader) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = finalFontWeight,
            color = textColor,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }

    if (isHeader && onClick != null) {
        Row(
            modifier = modifier.clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            content()
            headerContent()
        }
    } else {
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            content()
        }
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
                    "Data/Hora" to record.dataHora,
                    "IDMENSAGEM" to record.idMensagem,
                    "Latitude" to record.latitude,
                    "Longitude" to record.longitude,
                    "Placa" to record.placa,
                    "TrackID" to record.trackId
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
            LazyColumn {
                // <<<----------- CORREÇÃO AQUI -----------
                // Converte o Array para uma Lista para evitar ambiguidade
                items(Coluna.values().toList()) { coluna ->
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

@Composable
fun ThemeMenu(viewModel: ConsultaViewModel) {
    var showMenu by remember { mutableStateOf(false) }
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Menu de Opções", tint = MaterialTheme.colorScheme.onPrimary)
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Tema Claro") },
                onClick = {
                    viewModel.setTheme(false)
                    showMenu = false
                },
                leadingIcon = {
                    Icon(
                        if (!isDarkTheme) Icons.Filled.Check else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Tema Claro"
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("Tema Escuro") },
                onClick = {
                    viewModel.setTheme(true)
                    showMenu = false
                },
                leadingIcon = {
                    Icon(
                        if (isDarkTheme) Icons.Filled.Check else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Tema Escuro"
                    )
                }
            )
        }
    }
}