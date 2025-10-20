package com.example.appconsultas.data

/**
 * Define a estrutura de um cliente para autenticação na API.
 *
 * @param nome O nome do cliente, usado para exibição na UI (ex: "Cliente VIP").
 * @param authHeader A chave de autenticação completa (ex: "Basic dmlw...").
 */
data class Cliente(
    val nome: String,
    val authHeader: String
)