package com.example.appconsultas.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appconsultas.data.Cliente
import com.example.appconsultas.data.ConsultaRecord
import com.example.appconsultas.data.ConsultaRequestBody
import com.example.appconsultas.data.RetrofitInstance
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Enumeração para o filtro
enum class ColunaFiltro {
    TODAS, PLACA, TRACK_ID
}

// Enumeração para as colunas
enum class Coluna(val displayName: String) {
    DATA_HORA("Data/Hora"),
    ID_MENSAGEM("ID Mensagem"),
    LATITUDE("Latitude"),
    LONGITUDE("Longitude"),
    PLACA("Placa"),
    TRACK_ID("TrackID")
}

// Enum para controlar o estado do tema
enum class ThemeMode { Light, Dark, System }

class ConsultaViewModel : ViewModel() {

    val clientesDisponiveis = listOf(
        Cliente(
            nome = "Vip",
            authHeader = "Basic dmlwOjgzMTE0ZDhmYzMxNjRkZTRlODViNGU2ZWU4YTA0YmJk"
        ),
        Cliente(
            nome = "CKL",
            authHeader = "Basic Y2tsOmQ0Zjg2NGU4NDIxZWIwYmYwNzM4NGExYWU4MzFhYjdi"
        ),
        Cliente(
            nome = "Reverselog",
            authHeader = "Basic cmV2ZXJzZWxvZzpkYmJjNmZhN2JkYWE3YzkwOTJhMmIyNTYwNTk0ZWM1NQ=="
        ),
        Cliente(
            nome = "Rodoleve",
            authHeader = "Basic cm9kb2xldmU6ZDNhMGI3MWM5NTk3MmFkZjE3ODIyZTMwMzYyNjgwZjg="
        ),
        Cliente(
            nome = "servidorGxBeloog",
            authHeader = "Basic c2Vydmlkb3JHeEJlbG9vZzowMzMwMjY1Y2JiMmJmNDUyYWU1NDIyNmM0M2IzZDA4MQ=="
        ),
        Cliente(
            nome = "Gallotti",
            authHeader = "Basic Z2FsbG90dGk6MGNiOWJjZmIzYmQyNGE4YWQzNzNiYjFjMDA1ZTI1YzA="
        ),
        Cliente(
            nome = "Transgires",
            authHeader = "Basic dHJhbnNnaXJlczoxODgzM2VkZjg4NjZiN2YyODAyNjZlY2VlNzMzYTQzZA=="
        ),
        Cliente(
            nome = "Agregamais",
            authHeader = "Basic YWdyZWdhbWFpczplYWJmZTFmZmRiOTYzZWQzNjU2ZGE0ZWQ5MWY3YjM3YQ=="
        )
    )

    // --- ESTADOS INTERNOS (Privados) ---
    private val _todosOsRegistos = MutableStateFlow<List<ConsultaRecord>>(emptyList())
    private val _textoDoFiltro = MutableStateFlow("")
    private val _textoIdConsulta = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)
    private val _colunaFiltroSelecionada = MutableStateFlow(ColunaFiltro.TODAS)
    private val _colunaOrdenacao = MutableStateFlow<Coluna?>(null)
    private val _ordemDescendente = MutableStateFlow(true)
    private val _registoSelecionado = MutableStateFlow<ConsultaRecord?>(null)
    private val _clienteSelecionado = MutableStateFlow(clientesDisponiveis.first())

    // O estado do tema começa como "Sistema"
    private val _themeMode = MutableStateFlow(ThemeMode.System)


    // --- ESTADOS EXPOSTOS PARA A UI (Públicos) ---
    val textoDoFiltro = _textoDoFiltro.asStateFlow()
    val textoIdConsulta = _textoIdConsulta.asStateFlow()
    val isLoading = _isLoading.asStateFlow()
    val colunaFiltroSelecionada = _colunaFiltroSelecionada.asStateFlow()
    val colunaOrdenacao = _colunaOrdenacao.asStateFlow()
    val ordemDescendente = _ordemDescendente.asStateFlow()
    val registoSelecionado = _registoSelecionado.asStateFlow()
    val clienteSelecionado = _clienteSelecionado.asStateFlow()
    val themeMode = _themeMode.asStateFlow()


    // Função para alterar o tema (chamada pelo AppDrawer)
    fun setTheme(mode: ThemeMode) {
        _themeMode.value = mode
    }

    val registosFinais: StateFlow<List<ConsultaRecord>> = combine(
        _todosOsRegistos, _textoDoFiltro, _colunaFiltroSelecionada, _colunaOrdenacao, _ordemDescendente
    ) { lista, texto, colFiltro, colSort, isDesc ->
        val listaFiltrada = if (texto.isBlank()) {
            lista
        } else {
            lista.filter { record ->
                when (colFiltro) {
                    ColunaFiltro.TODAS -> record.placa?.contains(texto, true) == true || record.trackId?.contains(texto, true) == true
                    ColunaFiltro.PLACA -> record.placa?.contains(texto, true) == true
                    ColunaFiltro.TRACK_ID -> record.trackId?.contains(texto, true) == true
                }
            }
        }
        val listaOrdenada = when (colSort) {
            null -> listaFiltrada
            Coluna.ID_MENSAGEM -> listaFiltrada.sortedBy { it.idMensagem }
            Coluna.TRACK_ID -> listaFiltrada.sortedBy { it.trackId }
            Coluna.PLACA -> listaFiltrada.sortedBy { it.placa }
            Coluna.LATITUDE -> listaFiltrada.sortedBy { it.latitude }
            Coluna.LONGITUDE -> listaFiltrada.sortedBy { it.longitude }
            Coluna.DATA_HORA -> listaFiltrada.sortedBy { it.dataHora }
        }
        if (isDesc) listaOrdenada.reversed() else listaOrdenada
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        onOrdenarPor(Coluna.DATA_HORA)
        carregarDadosIniciais()
    }

    // --- FUNÇÕES CHAMADAS PELA UI ---

    fun onClienteSelecionadoChange(novoCliente: Cliente) {
        if (novoCliente == _clienteSelecionado.value) return

        _clienteSelecionado.value = novoCliente
        limparFiltros()
        carregarDadosIniciais()
    }


    fun carregarDadosIniciais() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _textoIdConsulta.value = ""
                val requestBody = ConsultaRequestBody(IDMENSAGEM = 0)
                val header = _clienteSelecionado.value.authHeader
                _todosOsRegistos.value = RetrofitInstance.api.buscarTodos(header, requestBody)

            } catch (e: Exception) {
                Log.e("ConsultaViewModel", "Falha ao carregar dados iniciais: ${e.message}", e)
                _todosOsRegistos.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    fun consultarPorId() {
        val id = _textoIdConsulta.value.toLongOrNull() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _textoDoFiltro.value = ""
                val requestBody = ConsultaRequestBody(IDMENSAGEM = id)
                val header = _clienteSelecionado.value.authHeader
                _todosOsRegistos.value = RetrofitInstance.api.consultarPorId(header, requestBody)

            } catch (e: Exception) {
                Log.e("ConsultaViewModel", "Falha ao consultar por ID: ${e.message}", e)
                _todosOsRegistos.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    // Funções de exportação
    fun gerarConteudoCSV(): String {
        val registos = registosFinais.value
        val stringBuilder = StringBuilder()
        stringBuilder.append("DATAHORA,IDMENSAGEM,LATITUDE,LONGITUDE,PLACA,TrackID\n")
        registos.forEach { registo ->
            stringBuilder.append(registo.dataHora ?: "").append(",")
            stringBuilder.append(registo.idMensagem).append(",")
            stringBuilder.append(registo.latitude?.toString() ?: "").append(",")
            stringBuilder.append(registo.longitude?.toString() ?: "").append(",")
            stringBuilder.append(registo.placa ?: "").append(",")
            stringBuilder.append(registo.trackId ?: "")
            stringBuilder.append("\n")
        }
        return stringBuilder.toString()
    }

    fun gerarConteudoXML(): String {
        val registos = registosFinais.value
        val stringBuilder = StringBuilder()
        stringBuilder.append("<consultas>\n")
        registos.forEach { registo ->
            stringBuilder.append("  <registo>\n")
            stringBuilder.append("    <DATAHORA>${registo.dataHora ?: ""}</DATAHORA>\n")
            stringBuilder.append("    <IDMENSAGEM>${registo.idMensagem}</IDMENSAGEM>\n")
            stringBuilder.append("    <LATITUDE>${registo.latitude?.toString() ?: ""}</LATITUDE>\n")
            stringBuilder.append("    <LONGITUDE>${registo.longitude?.toString() ?: ""}</LONGITUDE>\n")
            stringBuilder.append("    <PLACA>${registo.placa ?: ""}</PLACA>\n")
            stringBuilder.append("    <TrackID>${registo.trackId ?: ""}</TrackID>\n")
            stringBuilder.append("  </registo>\n")
        }
        stringBuilder.append("</consultas>")
        return stringBuilder.toString()
    }

    // Funções de controlo de estado
    fun onTextoDoFiltroChange(novoTexto: String) { _textoDoFiltro.value = novoTexto }
    fun onTextoIdChange(novoId: String) { _textoIdConsulta.value = novoId }
    fun onColunaFiltroChange(novaColuna: ColunaFiltro) { _colunaFiltroSelecionada.value = novaColuna }

    fun onOrdenarPor(novaColuna: Coluna) {
        if (_colunaOrdenacao.value == novaColuna) {
            _ordemDescendente.value = !_ordemDescendente.value
        } else {
            _colunaOrdenacao.value = novaColuna
            _ordemDescendente.value = true
        }
    }

    fun limparFiltros() {
        _textoDoFiltro.value = ""
    }

    fun onRegistoClicked(registo: ConsultaRecord) {
        _registoSelecionado.value = registo
    }

    fun onDetailsDialogDismiss() {
        _registoSelecionado.value = null
    }
}