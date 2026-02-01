package com.example.armazenadorsenha.api

import com.example.armazenadorsenha.model.PasswordData
import retrofit2.http.*

interface PasswordApiService {
    @GET("passwords")
    suspend fun getAllPasswords(): List<PasswordData>

    @POST("passwords")
    suspend fun savePassword(@Body entry: PasswordData): PasswordData

    @DELETE("passwords/{id}")
    suspend fun deletePassword(@Path("id") id: Int)

    @PUT("passwords/{id}")
    suspend fun updatePassword(@Path("id") id: Int, @Body entry: PasswordData): PasswordData
}