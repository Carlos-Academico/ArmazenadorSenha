package com.example.armazenadorsenha.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// ** CORREÇÃO: Adicionando as anotações do Room **
@Entity(tableName = "passwords")
data class PasswordData(
    // O id deve ser a chave primária e ser gerado automaticamente
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // Definindo um valor padrão de 0 para Room entender que deve gerar
    val serviceTitle: String,
    val username: String,
    val encryptedPasswordBase64: String,
    val ivBase64: String
)