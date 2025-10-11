package com.example.appconsultas.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

object RetrofitInstance {
    private const val BASE_URL = "http://85.209.93.16/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

interface ApiService {
    @POST("api/data")
    suspend fun buscarTodos(
        @Header("Authorization") authHeader: String,
        @Body requestBody: ConsultaRequestBody
    ): List<ConsultaRecord>

    @POST("api/data")
    suspend fun consultarPorId(
        @Header("Authorization") authHeader: String,
        @Body requestBody: ConsultaRequestBody
    ): List<ConsultaRecord>
}