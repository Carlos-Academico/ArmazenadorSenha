package com.example.armazenadorsenha.model

data class PasswordData(
    val id: Int,
    val serviceTitle: String,
    val username: String,
    val encryptedPasswordBase64: String,
    val ivBase64: String
)