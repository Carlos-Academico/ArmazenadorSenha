package com.example.armazenadorsenha.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.armazenadorsenha.model.VaultViewModel
import com.example.armazenadorsenha.repository.PasswordRepository
import com.example.armazenadorsenha.repository.UserRepository

class VaultViewModelFactory(
    private val masterKey: String,
    private val passwordRepository: PasswordRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VaultViewModel::class.java)) {

            // CORREÇÃO: Usar os nomes de parâmetros esperados pelo VaultViewModel
            return VaultViewModel(
                masterPassword = masterKey,          // <-- AJUSTADO: De 'masterKey' para 'masterPassword'
                repository = passwordRepository,     // <-- AJUSTADO: De 'passwordRepository' para 'repository'
                userRepository = userRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}