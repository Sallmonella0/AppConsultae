// Ficheiro: ui/viewmodel/ConsultaViewModel.kt (VERSÃO COMPLETA E CORRIGIDA)
package com.example.appconsultas.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appconsultas.data.ConsultaRecord
import com.example.appconsultas.data.ConsultaRequestBody
import com.example.appconsultas.data.RetrofitInstance
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Enums para representar as colunas de filtro e ordenação
enum class ColunaFiltro { TODAS, PLACA, TRACK_ID }
enum class Coluna(val displayName: String) {
    TRACK_ID("TrackID"),
    PLACA("Placa"),
    DATA_HORA("Data/Hora")
}

class ConsultaViewModel : ViewModel() {

    private val authHeader = "Basic dmlwOjgzMTE0ZDhmYzMxNjRkZTRlODViNGU2ZWU4YTA0YmJk"

    // --- ESTADOS INTERNOS (Privados) ---
    private val _todosOsRegistos = MutableStateFlow<List<ConsultaRecord>>(emptyList())
    private val _textoDoFiltro = MutableStateFlow("")
    private val _textoIdConsulta = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)
    private val _colunaFiltroSelecionada = MutableStateFlow(ColunaFiltro.TODAS)
    private val _colunaOrdenacao = MutableStateFlow<Coluna?>(null)
    private val _ordemDescendente = MutableStateFlow(true)
    private val _colunasVisiveis = MutableStateFlow(setOf(Coluna.TRACK_ID, Coluna.PLACA, Coluna.DATA_HORA))
    private val _registoSelecionado = MutableStateFlow<ConsultaRecord?>(null)

    // --- ESTADOS EXPOSTOS PARA A UI (Públicos) ---
    val textoDoFiltro = _textoDoFiltro.asStateFlow()
    val textoIdConsulta = _textoIdConsulta.asStateFlow()
    val isLoading = _isLoading.asStateFlow()
    val colunaFiltroSelecionada = _colunaFiltroSelecionada.asStateFlow()
    val colunaOrdenacao = _colunaOrdenacao.asStateFlow()
    val ordemDescendente = _ordemDescendente.asStateFlow()
    val colunasVisiveis = _colunasVisiveis.asStateFlow()
    val registoSelecionado = _registoSelecionado.asStateFlow()

    // Lógica reativa para filtrar e ordenar a lista, convertida para um StateFlow
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
            Coluna.TRACK_ID -> listaFiltrada.sortedBy { it.trackId }
            Coluna.PLACA -> listaFiltrada.sortedBy { it.placa }
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
    fun carregarDadosIniciais() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
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
        val id = _textoIdConsulta.value.toIntOrNull() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val requestBody = ConsultaRequestBody(IDMENSAGEM = id)
                _todosOsRegistos.value = RetrofitInstance.api.consultarPorId(authHeader, requestBody)
            } catch (e: Exception) {
                Log.e("ConsultaViewModel", "Falha ao consultar por ID: ${e.message}", e)
                _todosOsRegistos.value = emptyList()
            }
            _isLoading.value = false
        }
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