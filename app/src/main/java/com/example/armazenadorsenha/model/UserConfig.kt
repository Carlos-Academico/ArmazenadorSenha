package com.example.armazenadorsenha.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_config")
data class UserConfig(
    @PrimaryKey(autoGenerate = false)
    val id: Int = 1, // Garantir que há apenas um registro
    val email: String,
    val masterKeyHash: String, // Hash da senha mestra
    val masterKeySalt: String, // Salt usado no hashing
    val biometricEnabled: Boolean = false // Flag para login biométrico,
)