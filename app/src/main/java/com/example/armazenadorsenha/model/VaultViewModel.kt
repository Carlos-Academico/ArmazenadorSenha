package com.example.armazenadorsenha.model

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.armazenadorsenha.data.descrypto.EncryptionUseCase
import com.example.armazenadorsenha.repository.PasswordRepository
import com.example.armazenadorsenha.repository.UserRepository
import com.example.armazenadorsenha.service.EmailService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

// Configuração manual de dependências (em um projeto real, use DI como Hilt)
class VaultViewModel(
    private val masterPassword: String,
    private val repository: PasswordRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val encryptionUseCase: EncryptionUseCase = EncryptionUseCase()

    // 1. FONTE DA VERDADE (DB): Recebe o Flow reativo do Room.
    private val allPasswordsFlow = repository.getAllPasswords()

    // 2. QUERY/FILTRO: Armazena a consulta de pesquisa atual digitada pelo usuário.
    private val searchQuery = MutableStateFlow("")

    // 3. ESTADO DA UI: Combina o Flow de dados do DB e o Flow da consulta para gerar a lista final.
    val passwordList: StateFlow<List<PasswordData>> = allPasswordsFlow
        .combine(searchQuery) { allPasswords, query ->
            if (query.isBlank()) {
                allPasswords
            } else {
                val lowerCaseQuery = query.lowercase(Locale.ROOT)
                allPasswords.filter { entry ->
                    entry.serviceTitle.lowercase(Locale.ROOT).contains(lowerCaseQuery) ||
                            entry.username.lowercase(Locale.ROOT).contains(lowerCaseQuery)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            // Inicia quando a UI começa a observar (ex: quando o VaultScreen é composto)
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // O bloco 'init' agora é limpo, pois a lista carrega automaticamente via .stateIn
    init {
        // Não precisamos mais do loadPasswords() manual.
        // O Room inicia a coleta de dados reativos assim que o ViewModel é criado.
        Log.d("VaultViewModel", "Inicialização. Observando Flow de senhas do DB.")
    }

    // Antigo loadPasswords() e fullList não são mais necessários!
    // Seus dados são sempre os mais recentes via 'passwordList' StateFlow.

    /**
     * Atualiza o estado da consulta (searchQuery).
     * Isso irá automaticamente reprocessar o .combine e atualizar 'passwordList'.
     */
    fun filterPasswords(query: String) {
        // A lógica de filtragem foi movida para o bloco .combine acima
        searchQuery.value = query
    }

    /**
     * Adiciona um novo item.
     * Não precisa chamar loadPasswords() - o Room se encarrega de atualizar o Flow.
     */
    fun addNewPassword(service: String, username: String, plainPassword: String) = viewModelScope.launch {
        if (service.isBlank() || plainPassword.isBlank()) return@launch

        val (encryptedPass, iv) = encryptionUseCase.encrypt(plainPassword, masterPassword)

        val newEntry = PasswordData(
            id = 0,
            serviceTitle = service,
            username = username,
            encryptedPasswordBase64 = encryptedPass,
            ivBase64 = iv
        )

        // Room insere e o Flow atualiza a UI automaticamente
        repository.addPassword(newEntry)

        val recipientEmail = userRepository.getUserEmail()

        // 4. ENVIAR A NOTIFICAÇÃO
        if (recipientEmail != null) {
            EmailService.sendNewPasswordNotification(
                recipientEmail = recipientEmail,
                serviceTitle = service,
                username = username
            )
        }
    }

    // FUNÇÃO DECRIPTPASSWORD (Sem Alteração)
    fun decryptPassword(encryptedPass: String, iv: String): String {
        return try {
            encryptionUseCase.decrypt(encryptedPass, iv, masterPassword)
        } catch (e: Exception) {
            Log.e("VaultViewModel", "Erro de decifragem: ${e.message}")
            "ERRO DE CRIPTOGRAFIA"
        }
    }

    /**
     * Busca um item por ID.
     * Mantemos 'suspend' porque esta é uma chamada única e síncrona, e o DAO/Repository
     * precisam ser 'suspend' para rodar em thread de fundo (background thread).
     */
    suspend fun getPasswordById(itemId: Int): PasswordData? {
        return repository.getPasswordById(itemId)
    }

    /**
     * Atualiza um item de senha.
     * Não precisa chamar loadPasswords() - o Room se encarrega de atualizar o Flow.
     */
    fun updatePassword(
        id: Int,
        newService: String,
        newUsername: String,
        newPassword: String
    ) = viewModelScope.launch {
        val (encryptedPass, iv) = encryptionUseCase.encrypt(newPassword, masterPassword)

        val updatedEntry = PasswordData(
            id = id,
            serviceTitle = newService,
            username = newUsername,
            encryptedPasswordBase64 = encryptedPass,
            ivBase64 = iv
        )

        // Room atualiza e o Flow dispara a nova lista para a UI
        repository.updatePassword(updatedEntry)
        Log.d("VaultViewModel", "Atualizado item ID $id. O Flow irá atualizar a UI.")
    }

    /**
     * Deleta um item de senha.
     * Não precisa chamar loadPasswords() - o Room se encarrega de atualizar o Flow.
     */
    fun deletePassword(itemId: Int) = viewModelScope.launch {
        // Room deleta e o Flow dispara a nova lista para a UI
        repository.deletePassword(itemId)
        Log.d("VaultViewModel", "Deletado item ID $itemId. O Flow irá atualizar a UI.")
    }
}