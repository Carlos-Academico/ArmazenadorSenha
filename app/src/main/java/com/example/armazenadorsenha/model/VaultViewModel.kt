package com.example.armazenadorsenha.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.armazenadorsenha.data.descrypto.EncryptionUseCase
import com.example.armazenadorsenha.repository.PasswordRepository
import com.example.armazenadorsenha.service.InMemoryPasswordSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

// Configuração manual de dependências (em um projeto real, use DI como Hilt)
class VaultViewModel(masterKey: String) : ViewModel() {

    private val repository: PasswordRepository = InMemoryPasswordSource()
    private val encryptionUseCase: EncryptionUseCase = EncryptionUseCase()
    private val masterPassword: String = masterKey

    private val _passwordList = MutableStateFlow<List<PasswordData>>(emptyList())
    val passwordList: StateFlow<List<PasswordData>> = _passwordList.asStateFlow()

    private var fullList = listOf<PasswordData>()

    init {
        // Inicializa a lista (pode ser vazia no início ou carregar dados iniciais)
        loadPasswords()
    }

    private fun loadPasswords() {
        fullList = repository.getAllPasswords()
        _passwordList.value = fullList
    }

    fun filterPasswords(query: String) {
        val filteredList = if (query.isBlank()) {
            fullList
        } else {
            val lowerCaseQuery = query.lowercase(Locale.ROOT)
            fullList.filter { entry ->
                entry.serviceTitle.lowercase(Locale.ROOT).contains(lowerCaseQuery) ||
                        entry.username.lowercase(Locale.ROOT).contains(lowerCaseQuery)
            }
        }
        _passwordList.value = filteredList
    }

    fun addNewPassword(service: String, username: String, plainPassword: String) = viewModelScope.launch {
        if (service.isBlank() || plainPassword.isBlank()) return@launch

        val (encryptedPass, iv) = encryptionUseCase.encrypt(plainPassword, masterPassword)

        val newEntry = PasswordData(
            id = 0, // O repositório vai atribuir o ID real
            serviceTitle = service,
            username = username,
            encryptedPasswordBase64 = encryptedPass,
            ivBase64 = iv
        )

        repository.addPassword(newEntry)
        loadPasswords() // Atualiza a lista exibida
    }

    fun decryptPassword(entry: PasswordData): String {
        return try {
            encryptionUseCase.decrypt(entry.encryptedPasswordBase64, entry.ivBase64, masterPassword)
        } catch (e: Exception) {
            "ERRO DE CRIPTOGRAFIA"
        }
    }

    fun updatePassword(entry: PasswordData, newService: String, newUsername: String, newPlainPassword: String) = viewModelScope.launch {
        if (newService.isBlank() || newPlainPassword.isBlank()) return@launch

        val (encryptedPass, iv) = encryptionUseCase.encrypt(newPlainPassword, masterPassword)

        val updatedEntry = entry.copy(
            serviceTitle = newService,
            username = newUsername,
            encryptedPasswordBase64 = encryptedPass,
            ivBase64 = iv
        )

        repository.updatePassword(updatedEntry)
        loadPasswords()
    }

    fun deletePassword(passwordId: Int) = viewModelScope.launch {
        repository.deletePassword(passwordId)
        loadPasswords()
    }
}