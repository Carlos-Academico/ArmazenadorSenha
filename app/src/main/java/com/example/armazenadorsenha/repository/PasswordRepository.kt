package com.example.armazenadorsenha.repository

import com.example.armazenadorsenha.model.PasswordData

interface PasswordRepository {
    fun getAllPasswords(): List<PasswordData>
    fun addPassword(password: PasswordData)
    fun updatePassword(password: PasswordData)
    fun deletePassword(passwordId: Int)
}