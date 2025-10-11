package com.example.appconsultas.data

import com.google.gson.annotations.SerializedName

data class ConsultaRecord(
    @SerializedName("IDMENSAGEM")
    val idMensagem: Long,

    @SerializedName("TrackID")
    val trackId: String?,

    @SerializedName("PLACA")
    val placa: String?,

    @SerializedName("LATITUDE")
    val latitude: Double?,

    @SerializedName("LONGITUDE")
    val longitude: Double?,

    @SerializedName("DATAHORA")
    val dataHora: String?
)