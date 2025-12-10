package com.example.armazenadorsenha.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.armazenadorsenha.view.LoginStatus
import com.example.armazenadorsenha.view.LoginViewModel


@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onLoginSuccess: (masterKey: String) -> Unit
) {
    val context = LocalContext.current
    var passwordInput by remember { mutableStateOf("") }
    val loginStatus by viewModel.loginStatus.collectAsState()

    // Observa o status de login para navegação ou erro
    LaunchedEffect(loginStatus) {
        when (loginStatus) {
            is LoginStatus.Success -> {
                val key = (loginStatus as LoginStatus.Success).masterKey
                Toast.makeText(context, "Login bem-sucedido!", Toast.LENGTH_SHORT).show()
                onLoginSuccess(key)
                viewModel.resetStatus()
            }
            is LoginStatus.Error -> {
                val message = (loginStatus as LoginStatus.Error).message
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                viewModel.resetStatus()
            }
            LoginStatus.Idle -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Cofre de Senhas",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = passwordInput,
            onValueChange = { passwordInput = it },
            label = { Text("Senha Mestra") },
            visualTransformation = PasswordVisualTransformation(),
            // ********* USO CORRIGIDO *********
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            // *********************************
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.attemptLogin(passwordInput) },
            enabled = loginStatus is LoginStatus.Idle,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Entrar")
        }
    }
}