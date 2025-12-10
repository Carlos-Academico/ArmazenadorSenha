package com.example.armazenadorsenha.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.armazenadorsenha.model.VaultViewModel
import com.example.armazenadorsenha.repository.PasswordRepository

class VaultViewModelFactory(
    private val masterKey: String,
    // ** NOVO: Aceita a instância do Repository (RoomPasswordRepository) **
    private val repository: PasswordRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VaultViewModel::class.java)) {
            // ** Injeta a chave e o Repository no ViewModel **
            return VaultViewModel(
                masterPassword = masterKey,
                repository = repository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}