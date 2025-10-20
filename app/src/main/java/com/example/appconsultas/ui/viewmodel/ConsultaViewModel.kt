package com.example.appconsultas.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// IMPORTAR O NOVO MODELO (Assume que criaste Cliente.kt em 'data')
import com.example.appconsultas.data.Cliente
import com.example.appconsultas.data.ConsultaRecord
import com.example.appconsultas.data.ConsultaRequestBody
import com.example.appconsultas.data.RetrofitInstance
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Enumeração para o filtro (igual ao original)
enum class ColunaFiltro {
    TODAS, PLACA, TRACK_ID
}

// Enumeração para as colunas (igual ao original)
enum class Coluna(val displayName: String) {
    DATA_HORA("Data/Hora"),
    ID_MENSAGEM("ID Mensagem"),
    LATITUDE("Latitude"),
    LONGITUDE("Longitude"),
    PLACA("Placa"),
    TRACK_ID("TrackID")
}

class ConsultaViewModel : ViewModel() {

    val clientesDisponiveis = listOf(
        Cliente(
            nome = "vip",
            authHeader = "Basic 83114d8fc3164de4e85b4e6ee8a04bbd"
        ),
        Cliente(
            nome = "ckl",
            authHeader = "Basic d4f864e8421eb0bf07384a1ae831ab7b"
        ),
        Cliente(
            nome = "everselog",
            authHeader = "Basic dbbc6fa7bdaa7c9092a2b2560594ec55"
        ),
        Cliente(
            nome = "rodoleve",
            authHeader = "Basic d3a0b71c95972adf17822e30362680f8"
        ),
        Cliente(
            nome = "servidorGxBeloog",
            authHeader = "Basic 0330265cbb2bf452ae54226c43b3d081"
        ),
        Cliente(
            nome = "gallotti",
            authHeader = "Basic 0cb9bcfb3bd24a8ad373bb1c005e25c0"
        ),
        Cliente(
            nome = "transgires",
            authHeader = "Basic 18833edf8866b7f280266ecee733a43d"
        ),
        Cliente(
            nome = "Agregamais",
            authHeader = "Basic eabfe1ffdb963ed3656da4ed91f7b37a"
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
    private val _isDarkTheme = MutableStateFlow(false)

    // ADICIONADO: Estado interno para o cliente selecionado
    // Começa com o primeiro cliente da lista
    private val _clienteSelecionado = MutableStateFlow(clientesDisponiveis.first())


    // --- ESTADOS EXPOSTOS PARA A UI (Públicos) ---
    val textoDoFiltro = _textoDoFiltro.asStateFlow()
    val textoIdConsulta = _textoIdConsulta.asStateFlow()
    val isLoading = _isLoading.asStateFlow()
    val colunaFiltroSelecionada = _colunaFiltroSelecionada.asStateFlow()
    val colunaOrdenacao = _colunaOrdenacao.asStateFlow()
    val ordemDescendente = _ordemDescendente.asStateFlow()
    val registoSelecionado = _registoSelecionado.asStateFlow()
    val isDarkTheme = _isDarkTheme.asStateFlow()

    // ADICIONADO: Estado exposto para o cliente selecionado
    val clienteSelecionado = _clienteSelecionado.asStateFlow()

    fun setTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
    }

    // Lógica de filtragem e ordenação (igual ao original)
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

    // ADICIONADO: Função para a UI atualizar o cliente
    fun onClienteSelecionadoChange(novoCliente: Cliente) {
        if (novoCliente == _clienteSelecionado.value) return // Não faz nada se for o mesmo

        _clienteSelecionado.value = novoCliente
        // Ao trocar de cliente, limpamos os filtros e carregamos os dados desse novo cliente
        limparFiltros()
        carregarDadosIniciais()
    }


    fun carregarDadosIniciais() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _textoIdConsulta.value = ""
                val requestBody = ConsultaRequestBody(IDMENSAGEM = 0) //

                // MODIFICADO: Usar o header do cliente selecionado
                val header = _clienteSelecionado.value.authHeader
                _todosOsRegistos.value = RetrofitInstance.api.buscarTodos(header, requestBody) //

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
                val requestBody = ConsultaRequestBody(IDMENSAGEM = id) //

                // MODIFICADO: Usar o header do cliente selecionado
                val header = _clienteSelecionado.value.authHeader
                _todosOsRegistos.value = RetrofitInstance.api.consultarPorId(header, requestBody) //

            } catch (e: Exception) {
                Log.e("ConsultaViewModel", "Falha ao consultar por ID: ${e.message}", e)
                _todosOsRegistos.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    // Funções de exportação (iguais ao original)
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

    // Funções de controlo de estado (iguais ao original)
    fun onTextoDoFiltroChange(novoTexto: String) { _textoDoFiltro.value = novoTexto }
    fun onTextoIdChange(novoId: String) { _textoIdConsulta.value = novoId }
    fun onColunaFiltroChange(novaColuna: ColunaFiltro) { _colunaFiltroSelecionada.value = novaColuna }

    fun onOrdenarPor(novaColuna: Coluna) {
        if (_colunaOrdenacao.value == novaColuna) {
            _ordemDescendente.value = !_ordemDescendente.value
        } else {
            _colunaOrdenacao.value = novaColuna
            _ordemDescendente.value = true // Sempre começa descendente ao trocar de coluna
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