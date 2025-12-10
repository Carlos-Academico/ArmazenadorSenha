package com.example.armazenadorsenha.service


import com.example.armazenadorsenha.model.PasswordData
import com.example.armazenadorsenha.repository.PasswordRepository
import java.util.concurrent.atomic.AtomicInteger

/**
 * Repositório que armazena os dados APENAS na memória RAM.
 * Os dados são perdidos ao fechar o aplicativo.
 */
class InMemoryPasswordSource : PasswordRepository {

    private val passwordList = mutableListOf<PasswordData>()
    private val nextId = AtomicInteger(1)

    override fun getAllPasswords(): List<PasswordData> {
        return passwordList.toList()
    }

    override fun addPassword(password: PasswordData) {
        // Garante que a senha tenha um ID novo (mesmo que o ID seja gerado antes)
        val newPassword = password.copy(id = nextId.getAndIncrement())
        passwordList.add(newPassword)
    }

    override fun updatePassword(password: PasswordData) {
        val index = passwordList.indexOfFirst { it.id == password.id }
        if (index != -1) {
            passwordList[index] = password
        }
    }

    override fun deletePassword(passwordId: Int) {
        passwordList.removeIf { it.id == passwordId }
    }
}