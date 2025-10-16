package com.example.appconsultas.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appconsultas.data.ConsultaRecord
import com.example.appconsultas.data.ConsultaRequestBody
import com.example.appconsultas.data.RetrofitInstance
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ColunaFiltro {
    TODAS, PLACA, TRACK_ID
}

enum class Coluna(val displayName: String) {
    DATA_HORA("Data/Hora"),
    ID_MENSAGEM("ID Mensagem"),
    LATITUDE("Latitude"),
    LONGITUDE("Longitude"),
    PLACA("Placa"),
    TRACK_ID("TrackID")
}

class ConsultaViewModel : ViewModel() {

    private val authHeader = "Basic dmlwOjgzMTE0ZDhmYzMxNjRkZTRlODViNGU2ZWU4YTA0YmJk"

    private val _todosOsRegistos = MutableStateFlow<List<ConsultaRecord>>(emptyList())
    private val _textoDoFiltro = MutableStateFlow("")
    private val _textoIdConsulta = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)
    private val _colunaFiltroSelecionada = MutableStateFlow(ColunaFiltro.TODAS)
    private val _colunaOrdenacao = MutableStateFlow<Coluna?>(null)
    private val _ordemDescendente = MutableStateFlow(true)
    // <<<----------- ALTERAÇÃO AQUI: Define as colunas visíveis por defeito -----------
    private val _colunasVisiveis = MutableStateFlow(
        setOf(Coluna.DATA_HORA, Coluna.ID_MENSAGEM, Coluna.PLACA, Coluna.TRACK_ID)
    )
    private val _registoSelecionado = MutableStateFlow<ConsultaRecord?>(null)

    val textoDoFiltro = _textoDoFiltro.asStateFlow()
    val textoIdConsulta = _textoIdConsulta.asStateFlow()
    val isLoading = _isLoading.asStateFlow()
    val colunaFiltroSelecionada = _colunaFiltroSelecionada.asStateFlow()
    val colunaOrdenacao = _colunaOrdenacao.asStateFlow()
    val ordemDescendente = _ordemDescendente.asStateFlow()
    val colunasVisiveis = _colunasVisiveis.asStateFlow()
    val registoSelecionado = _registoSelecionado.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme = _isDarkTheme.asStateFlow()

    fun setTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
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

    fun carregarDadosIniciais() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _textoIdConsulta.value = ""
                val requestBody = ConsultaRequestBody(IDMENSAGEM = 0)
                _todosOsRegistos.value = RetrofitInstance.api.buscarTodos(authHeader, requestBody)
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
                _todosOsRegistos.value = RetrofitInstance.api.consultarPorId(authHeader, requestBody)
            } catch (e: Exception) {
                Log.e("ConsultaViewModel", "Falha ao consultar por ID: ${e.message}", e)
                _todosOsRegistos.value = emptyList()
            }
            _isLoading.value = false
        }
    }

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

    fun toggleVisibilidadeColuna(coluna: Coluna) {
        val visiveisAtuais = _colunasVisiveis.value.toMutableSet()
        if (visiveisAtuais.contains(coluna)) {
            if (visiveisAtuais.size > 1) {
                visiveisAtuais.remove(coluna)
            }
        } else {
            visiveisAtuais.add(coluna)
        }
        _colunasVisiveis.value = visiveisAtuais
    }

    fun onRegistoClicked(registo: ConsultaRecord) {
        _registoSelecionado.value = registo
    }

    fun onDetailsDialogDismiss() {
        _registoSelecionado.value = null
    }
}