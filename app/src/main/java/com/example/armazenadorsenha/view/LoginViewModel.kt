package com.example.armazenadorsenha.view

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// A senha mestra é uma regra de negócio, então deveria vir de um UseCase ou Repository,
// mas mantive aqui para consistência com o seu código original.
private const val FIXED_MASTER_PASSWORD = "123"

class LoginViewModel : ViewModel() {

    private val _loginStatus = MutableStateFlow<LoginStatus>(LoginStatus.Idle)
    val loginStatus: StateFlow<LoginStatus> = _loginStatus.asStateFlow()

    fun attemptLogin(inputPassword: String) {
        if (inputPassword.isBlank()) {
            _loginStatus.value = LoginStatus.Error("Senha não pode ser vazia.")
            return
        }

        if (inputPassword == FIXED_MASTER_PASSWORD) {
            _loginStatus.value = LoginStatus.Success(inputPassword)
        } else {
            _loginStatus.value = LoginStatus.Error("Senha incorreta.")
        }
    }

    fun resetStatus() {
        _loginStatus.value = LoginStatus.Idle
    }
}

sealed class LoginStatus {
    data object Idle : LoginStatus()
    data class Success(val masterKey: String) : LoginStatus()
    data class Error(val message: String) : LoginStatus()
}