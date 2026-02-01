package com.example.armazenadorsenha.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// ** CORREÇÃO: Adicionando as anotações do Room **
@Entity(tableName = "passwords")
data class PasswordData(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val serviceTitle: String,
    val username: String,
    val encryptedPasswordBase64: String,
    val ivBase64: String,
    val imageUrl: String? = null // <-- NOVO CAMPO PARA O COIL
)