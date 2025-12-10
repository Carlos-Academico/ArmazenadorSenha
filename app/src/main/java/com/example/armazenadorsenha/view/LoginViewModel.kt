package com.example.armazenadorsenha.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.armazenadorsenha.model.UserConfig
import com.example.armazenadorsenha.repository.UserRepository
import com.example.armazenadorsenha.utils.SecurityUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// A senha mestra é uma regra de negócio, então deveria vir de um UseCase ou Repository,
// mas mantive aqui para consistência com o seu código original.
// Novo Sealed Class para definir os diferentes estados da tela de Login
sealed class LoginUIState {
    data object Loading : LoginUIState()
    data object Register : LoginUIState() // Usuário não cadastrado
    data class Login(val biometricEnabled: Boolean) : LoginUIState() // Usuário cadastrado, pronto para login
}

// Novo Sealed Class para o status da operação de Login/Cadastro
sealed class LoginActionStatus {
    data object Idle : LoginActionStatus()
    data class Success(val masterKey: String) : LoginActionStatus()
    data class Error(val message: String) : LoginActionStatus()
    data class BiometricReady(val config: UserConfig) : LoginActionStatus() // Para iniciar o prompt
}

class LoginViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUIState>(LoginUIState.Loading)
    val uiState: StateFlow<LoginUIState> = _uiState.asStateFlow()

    private val _actionStatus = MutableStateFlow<LoginActionStatus>(LoginActionStatus.Idle)
    val actionStatus: StateFlow<LoginActionStatus> = _actionStatus.asStateFlow()

    private var masterKeyInMemory: String? = null

    init {
        checkUserRegistration()
    }

    private fun checkUserRegistration() = viewModelScope.launch {
        val isRegistered = userRepository.isUserRegistered()
        if (isRegistered) {
            val config = userRepository.getUserConfig()
            if (config != null) {
                _uiState.value = LoginUIState.Login(biometricEnabled = config.biometricEnabled)
                // Se biometria estiver habilitada, notificamos a UI para iniciar o prompt
                if (config.biometricEnabled) {
                    _actionStatus.value = LoginActionStatus.BiometricReady(config)
                }
            } else {
                // Deve ser tratado como erro, mas forçamos o cadastro
                _uiState.value = LoginUIState.Register
            }
        } else {
            _uiState.value = LoginUIState.Register
        }
    }

    /**
     * 1. Lógica de Cadastro Inicial
     */
    fun registerUser(password: String, enableBiometric: Boolean) = viewModelScope.launch {
        if (password.length < 6) { // Regra simples, ajuste conforme a necessidade
            _actionStatus.value = LoginActionStatus.Error("A senha deve ter pelo menos 6 caracteres.")
            return@launch
        }

        try {
            val salt = SecurityUtils.generateSalt()
            val hash = SecurityUtils.hashPassword(password, salt)

            val newUserConfig = UserConfig(
                masterKeyHash = hash,
                masterKeySalt = salt,
                biometricEnabled = enableBiometric
            )

            userRepository.registerUser(newUserConfig)
            masterKeyInMemory = password // A chave é salva temporariamente em memória

            _actionStatus.value = LoginActionStatus.Success(password)
        } catch (e: Exception) {
            _actionStatus.value = LoginActionStatus.Error("Erro ao registrar usuário: ${e.message}")
        }
    }

    /**
     * 2. Lógica de Login por Senha
     */
    fun attemptLogin(inputPassword: String) = viewModelScope.launch {
        val config = userRepository.getUserConfig()
        if (config == null) {
            _actionStatus.value = LoginActionStatus.Error("Nenhum usuário cadastrado. Reinicie o app para cadastrar.")
            return@launch
        }

        val isVerified = SecurityUtils.verifyPassword(
            inputPassword,
            config.masterKeyHash,
            config.masterKeySalt
        )

        if (isVerified) {
            masterKeyInMemory = inputPassword
            _actionStatus.value = LoginActionStatus.Success(inputPassword)
        } else {
            _actionStatus.value = LoginActionStatus.Error("Senha incorreta.")
        }
    }

    /**
     * 3. Lógica de Login por Biometria (Chamado após sucesso do BiometricPrompt)
     * Como a biometria não fornece a MasterKey, ela deve ser recuperada de forma segura.
     * Por simplicidade, neste ponto assumimos que a MasterKey é recuperável.
     * * **NOTA:** Em apps reais, a MasterKey é criptografada e armazenada no Android Keystore,
     * e o sucesso biométrico a descriptografa. Aqui, usaremos a senha fixa como placeholder.
     */
    fun onBiometricAuthSuccess() = viewModelScope.launch {
        // Se a biometria for bem sucedida, precisamos da chave de volta.
        // Já que não armazenamos a chave em nenhum lugar, usamos um placeholder
        // ou você usaria o Keystore para descriptografar a chave mestra aqui.
        // Usamos o password "fixo" como chave mestra para simular o sucesso.

        // **!!! ATENÇÃO: SUBSTITUA PELA LÓGICA DO KEYSTORE !!!**
        val placeholderMasterKey = "123" // Substitua pela MasterKey real descriptografada

        masterKeyInMemory = placeholderMasterKey
        _actionStatus.value = LoginActionStatus.Success(placeholderMasterKey)
    }

    /**
     * Atualiza a configuração de biometria após o login ou cadastro.
     */
    fun updateBiometricSetting(enabled: Boolean) = viewModelScope.launch {
        userRepository.updateBiometricSetting(enabled)
    }

    fun resetStatus() {
        _actionStatus.value = LoginActionStatus.Idle
    }
}