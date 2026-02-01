package com.example.armazenadorsenha.model

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.armazenadorsenha.api.RetrofitClient
import com.example.armazenadorsenha.data.descrypto.EncryptionUseCase
import com.example.armazenadorsenha.repository.PasswordRepository
import com.example.armazenadorsenha.repository.UserRepository
import com.example.armazenadorsenha.service.EmailService
import kotlinx.coroutines.Dispatchers
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

        // 1. CRIPTOGRAFIA (Permanece igual)
        val (encryptedPass, iv) = encryptionUseCase.encrypt(plainPassword, masterPassword)

        val newEntry = PasswordData(
            serviceTitle = service,
            username = username,
            encryptedPasswordBase64 = encryptedPass,
            ivBase64 = iv
            // imageUrl começará nulo aqui
        )

        try {
            // 2. MIDDLEWARE (Retrofit): Envia para a API Python (FastAPI)
            // A API deve processar e retornar o objeto com o campo 'imageUrl' preenchido
            val savedEntryFromApi = RetrofitClient.instance.savePassword(newEntry)

            // 3. BANCO LOCAL (Room): Salva o objeto QUE VEIO DA API (já com a imagem)
            repository.addPassword(savedEntryFromApi)

            Log.d("VaultViewModel", "Sincronizado com API e salvo no Room com imagem: ${savedEntryFromApi.imageUrl}")

        } catch (e: Exception) {
            // Caso a API esteja offline, salvamos apenas localmente para o app não travar
            Log.e("VaultViewModel", "Falha na API: ${e.message}. Salvando apenas local.")
            repository.addPassword(newEntry)
        }

        // 4. NOTIFICAÇÃO (Permanece igual)
        val recipientEmail = userRepository.getUserEmail()
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
        // 1. Criptografia
        val (encryptedPass, iv) = encryptionUseCase.encrypt(newPassword, masterPassword)

        // Buscamos o item atual para não perder a imageUrl que já existe
        val currentEntry = repository.getPasswordById(id)

        val updatedEntry = PasswordData(
            id = id,
            serviceTitle = newService,
            username = newUsername,
            encryptedPasswordBase64 = encryptedPass,
            ivBase64 = iv,
            imageUrl = currentEntry?.imageUrl // Mantém a imagem atual
        )

        try {
            // 2. Sincroniza com a API (Método PUT)
            RetrofitClient.instance.updatePassword(id, updatedEntry)

            // 3. Atualiza Localmente
            repository.updatePassword(updatedEntry)
            Log.d("VaultViewModel", "Atualizado na API e no Room.")
        } catch (e: Exception) {
            Log.e("VaultViewModel", "Erro ao atualizar na API: ${e.message}")
            // Opcional: atualizar apenas local se a API falhar
            repository.updatePassword(updatedEntry)
        }

        // 4. Notificação de Email
        val recipientEmail = userRepository.getUserEmail()
        if (recipientEmail != null) {
            EmailService.sendUpdateNotification(recipientEmail, newService, newUsername)
        }
    }
    /**
     * Deleta um item de senha.
     * Não precisa chamar loadPasswords() - o Room se encarrega de atualizar o Flow.
     */
    fun deletePassword(itemId: Int) = viewModelScope.launch(Dispatchers.IO) { // Garante que a operação é feita em IO

        // 1. BUSCAR O REGISTRO ANTES DE DELETAR
        // MUDANÇA AQUI: Usa 'repository' (que deve ser a variável do seu construtor)
        val entryToDelete = repository.getPasswordById(itemId)

        // 2. DELETAR O REGISTRO DO BANCO
        // MUDANÇA AQUI: Usa 'repository'
        repository.deletePassword(itemId)

        // 3. ENVIAR NOTIFICAÇÃO (apenas se o item foi encontrado e deletado)
        if (entryToDelete != null) {
            val recipientEmail = userRepository.getUserEmail()

            if (recipientEmail != null) {
                EmailService.sendDeleteNotification(
                    recipientEmail = recipientEmail,
                    serviceTitle = entryToDelete.serviceTitle,
                    username = entryToDelete.username
                )
            }
        }

        Log.d("VaultViewModel", "Deletado item ID $itemId. O Flow irá atualizar a UI.")
    }
}