package com.example.appconsultas.data

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

object DateUtils {

    // Define o formato de saída (o que será mostrado ao usuário)
    // Exemplo: "27/10/2023 10:30:00"
    private val outputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    // Lista de possíveis formatos que podem vir da API
    private val possibleInputFormatters = listOf(
        // Formato mais comum, com offset (ex: 2023-10-27T10:30:00.123+00:00)
        DateTimeFormatter.ISO_OFFSET_DATE_TIME,
        // Formato sem milissegundos (ex: 2023-10-27T10:30:00+00:00)
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()),
        // Formato sem offset (assume a hora local do dispositivo)
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    )

    /**
     * Formata uma string de data e hora de vários formatos possíveis para um formato legível.
     * @param dataHoraString A data vinda da API.
     * @return A data formatada como "dd/MM/yyyy HH:mm:ss" ou a string original se houver erro.
     */
    fun formatarDataHora(dataHoraString: String?): String {
        if (dataHoraString.isNullOrEmpty()) {
            return "Data indisponível"
        }

        // Tenta converter usando os formatos com offset primeiro
        for (formatter in possibleInputFormatters.filter { it != possibleInputFormatters.last() }) {
            try {
                val offsetDateTime = OffsetDateTime.parse(dataHoraString, formatter)
                return offsetDateTime.format(outputFormatter)
            } catch (e: DateTimeParseException) {
                // Ignora o erro e tenta o próximo formato
            }
        }

        // Tenta converter usando o formato sem offset como última opção
        try {
            val localDateTime = LocalDateTime.parse(dataHoraString, possibleInputFormatters.last())
            return localDateTime.format(outputFormatter)
        } catch (e: DateTimeParseException) {
            // Se nenhum formato funcionar, devolve a string original
            return dataHoraString
        }
    }
}