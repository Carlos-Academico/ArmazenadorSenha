package com.example.armazenadorsenha.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // 10.0.2.2 é o endereço que o emulador usa para acessar o localhost do seu PC
    private const val BASE_URL = "http://10.0.2.2:8000/"

    val instance: PasswordApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(PasswordApiService::class.java)
    }
}