package com.example.armazenadorsenha.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.armazenadorsenha.repository.UserRepository
import com.example.armazenadorsenha.view.LoginActionStatus
import com.example.armazenadorsenha.view.LoginUIState
import com.example.armazenadorsenha.view.LoginViewModel
import java.util.concurrent.Executor

/**
 * Função de extensão para encontrar a Activity a partir de um Context.
 */
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun LoginScreen(
    repository: UserRepository,
    onLoginSuccess: (masterKey: String) -> Unit
) {
    // Criação do ViewModel com Factory
    val factory = remember { LoginViewModelFactory(repository) }
    val viewModel: LoginViewModel = viewModel(factory = factory)

    val context = LocalContext.current

    // CORREÇÃO: Busca e cast seguro para FragmentActivity (necessário para BiometricPrompt)
    val fragmentActivity = LocalContext.current.findActivity() as? FragmentActivity

    val uiState by viewModel.uiState.collectAsState()
    val actionStatus by viewModel.actionStatus.collectAsState()

    var passwordInput by remember { mutableStateOf("") }
    var biometricCheckbox by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") } // NOVO ESTADO

    // --- Tratamento de Contexto para Biometria ---

    // Se a Activity não for válida para hospedar o BiometricPrompt, paramos aqui.
    if (fragmentActivity == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Erro: Activity não encontrada ou incompatível para biometria.",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    // Executor e BiometricPrompt
    val executor: Executor = remember { ContextCompat.getMainExecutor(context) }

    // BiometricPrompt criado com FragmentActivity não nula.
    val biometricPrompt: BiometricPrompt = remember(fragmentActivity) {
        BiometricPrompt(
            fragmentActivity, // Tipo FragmentActivity
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    viewModel.onBiometricAuthSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(context, "Erro Biométrico: $errString", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // PromptInfo memorizado
    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Login por Impressão Digital")
            .setSubtitle("Toque no sensor para entrar no Cofre.")
            .setNegativeButtonText("Usar Senha Mestra")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
    }

    // Observa o Status da Ação
    LaunchedEffect(actionStatus) {
        when (actionStatus) {
            is LoginActionStatus.Success -> {
                val key = (actionStatus as LoginActionStatus.Success).masterKey
                Toast.makeText(context, "Operação bem-sucedida!", Toast.LENGTH_SHORT).show()
                onLoginSuccess(key)
                viewModel.resetStatus()
            }
            is LoginActionStatus.Error -> {
                val message = (actionStatus as LoginActionStatus.Error).message
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                viewModel.resetStatus()
            }
            is LoginActionStatus.BiometricReady -> {
                if ((actionStatus as LoginActionStatus.BiometricReady).config.biometricEnabled) {
                    val manager = BiometricManager.from(context)
                    val canAuthenticate = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

                    if (canAuthenticate) {
                        biometricPrompt.authenticate(promptInfo)
                    } else {
                        Toast.makeText(context, "Biometria não disponível. Use a senha mestra.", Toast.LENGTH_LONG).show()
                    }
                }
                viewModel.resetStatus()
            }
            LoginActionStatus.Idle -> Unit
        }
    }

    // Conteúdo da Tela
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when (uiState) {
            LoginUIState.Loading -> CircularProgressIndicator()

            is LoginUIState.Register -> {
                RegisterForm(
                    passwordInput = passwordInput,
                    onPasswordChange = { passwordInput = it },
                    biometricCheckbox = biometricCheckbox,
                    emailInput = emailInput,
                    onEmailChange = { emailInput = it },
                    onBiometricCheck = { biometricCheckbox = it },
                    onRegisterClick = { email -> // RECEBE O EMAIL DA FUNÇÃO
                        viewModel.registerUser(passwordInput, biometricCheckbox, email) // MUDANÇA
                    }
                )
            }

            is LoginUIState.Login -> {
                LoginForm(
                    passwordInput = passwordInput,
                    onPasswordChange = { passwordInput = it },
                    biometricEnabled = (uiState as LoginUIState.Login).biometricEnabled,
                    onLoginClick = {
                        viewModel.attemptLogin(passwordInput)
                    },
                    onBiometricClick = {
                        biometricPrompt.authenticate(promptInfo)
                    }
                )
            }
        }
    }
}

// --- Funções Composable Auxiliares ---

@Composable
fun RegisterForm(
    passwordInput: String,
    onPasswordChange: (String) -> Unit,
    biometricCheckbox: Boolean,
    onBiometricCheck: (Boolean) -> Unit,
    emailInput: String, // NOVO
    onEmailChange: (String) -> Unit, // NOVO
    onRegisterClick: (email: String) -> Unit // MUDANÇA
) {
    val context = LocalContext.current
    val biometricManager = remember { BiometricManager.from(context) }
    val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Primeiro Acesso: Cadastro", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 24.dp))

        OutlinedTextField(
            value = emailInput,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = passwordInput,
            onValueChange = onPasswordChange,
            label = { Text("Definir Senha") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (canAuthenticate) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = biometricCheckbox, onCheckedChange = onBiometricCheck)
                Text("Habilitar Login com Impressão Digital", style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Text("Biometria não disponível ou não configurada.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = { onRegisterClick(emailInput) },
            modifier = Modifier.fillMaxWidth(),
            enabled = passwordInput.length >= 6
        ) {
            Text("Cadastrar e Entrar")
        }
    }
}

@Composable
fun LoginForm(
    passwordInput: String,
    onPasswordChange: (String) -> Unit,
    biometricEnabled: Boolean,
    onLoginClick: () -> Unit,
    onBiometricClick: () -> Unit
) {
    val context = LocalContext.current
    val biometricManager = remember { BiometricManager.from(context) }
    val canAuthenticateNow = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    val showBiometricButton = biometricEnabled && canAuthenticateNow

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Bem-Vindo(a)!", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 24.dp))

        OutlinedTextField(
            value = passwordInput,
            onValueChange = onPasswordChange,
            label = { Text("Senha Mestra") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (showBiometricButton) Arrangement.SpaceBetween else Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {

            // CORREÇÃO DO WEIGHT(0f):
            // Usa weight(1f) APENAS se o botão biométrico estiver visível.
            // Caso contrário, usa fillMaxWidth() para ocupar todo o espaço.
            Button(
                onClick = onLoginClick,
                modifier = if (showBiometricButton) {
                    Modifier.weight(1f)
                } else {
                    Modifier.fillMaxWidth()
                }
            ) {
                Text("Entrar")
            }

            if (showBiometricButton) {
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(
                    onClick = onBiometricClick,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.Filled.Fingerprint,
                        contentDescription = "Login Biométrico",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// --- Factory para o LoginViewModel ---

class LoginViewModelFactory(private val repository: UserRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}