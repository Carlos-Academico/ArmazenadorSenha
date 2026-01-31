package com.example.armazenadorsenha.screen

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.armazenadorsenha.model.PasswordData
import com.example.armazenadorsenha.model.VaultViewModel
import com.example.armazenadorsenha.dialog.AddEditPasswordDialog
import com.example.armazenadorsenha.factory.VaultViewModelFactory
import com.example.armazenadorsenha.repository.PasswordRepository
import com.example.armazenadorsenha.repository.UserRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    itemId: Int,
    onBack: () -> Unit,
    onUpdateSuccess: () -> Unit,
    masterKey: String,
    repository: PasswordRepository,
    userRepository: UserRepository
) {
    // Inicialização do ViewModel com a Factory
    val factory = remember {
        VaultViewModelFactory(
            masterKey = masterKey,
            passwordRepository = repository, // Corrigido para corresponder ao nome na Factory
            userRepository = userRepository  // NOVO PARÂMETRO AQUI
        )
    }
    val viewModel: VaultViewModel = viewModel(factory = factory)
    // 2. Estados de Carregamento e Interface
    var entryData by remember { mutableStateOf<PasswordData?>(null) }
    var service by remember { mutableStateOf("Carregando...") }
    var username by remember { mutableStateOf("") }
    // A senha criptografada do banco (Não exibida, apenas para referência)
    var passwordEncrypted by remember { mutableStateOf("") }
    var decryptedPassword by remember { mutableStateOf("...") } // Senha decifrada para visualização
    var isPasswordVisible by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 3. Efeito para Carregar e Decifrar Dados
    LaunchedEffect(itemId) {
        val loadedEntry = viewModel.getPasswordById(itemId) // Busca no repositório
        entryData = loadedEntry

        if (loadedEntry != null) {
            // ATUALIZA OS ESTADOS COM DADOS REAIS
            service = loadedEntry.serviceTitle
            username = loadedEntry.username
            passwordEncrypted = loadedEntry.encryptedPasswordBase64

            // ** 🔑 REALIZA A DECIFRAGEM 🔑 **
            try {
                // Chama a função decryptPassword (que foi corrigida para aceitar 2 argumentos no seu ViewModel)
                decryptedPassword = viewModel.decryptPassword(
                    loadedEntry.encryptedPasswordBase64,
                    loadedEntry.ivBase64
                )
            } catch (e: Exception) {
                Log.e("EditScreen", "Erro ao decifrar: ${e.message}")
                decryptedPassword = "ERRO: ${e.message?.take(20)}..."
            }
        } else {
            service = "Item Não Encontrado (ID: $itemId)"
            decryptedPassword = "Não foi possível carregar o item."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(service) }, // Título dinâmico
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    // Ícone da Lixeira para Excluir
                    if (entryData != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.Red)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ícone do Serviço (Pode ser melhorado com um AsyncImage como no VaultScreen)
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = service,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Campo Serviço (Visualização)
            OutlinedTextField(
                value = service,
                onValueChange = { /* readOnly */ },
                label = { Text("Serviço") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Campo Usuário (Visualização)
            OutlinedTextField(
                value = username,
                onValueChange = { /* readOnly */ },
                label = { Text("Usuário") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Campo Senha (Visualização - Usa a senha decifrada)
            OutlinedTextField(
                value = decryptedPassword,
                onValueChange = { /* readOnly */ },
                label = { Text("Senha") },
                readOnly = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    // Ícone do Olho para Visualizar a Senha
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (isPasswordVisible) "Ocultar senha" else "Visualizar senha"
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.weight(1f)) // Empurra os botões para baixo

            // Botões de Ação
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Botão Voltar
                Button(onClick = onBack, modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)) {
                    Text("Voltar")
                }

                // Botão Atualizar (Abre o Diálogo/Card)
                Button(
                    onClick = { showUpdateDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    enabled = entryData != null // Habilita apenas se o item foi carregado
                ) {
                    Text("Atualizar")
                }
            }
        }
    }

    // --- Diálogos (Alertas/Cards) ---

    // 1. Diálogo de Confirmação de Exclusão
    if (showDeleteConfirm && entryData != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Excluir Senha") },
            text = { Text("Tem certeza que deseja excluir a senha para o serviço '${entryData!!.serviceTitle}'? Esta ação é irreversível.") },
            confirmButton = {
                Button(
                    onClick = {
                        // ** LÓGICA DE EXCLUSÃO **
                        viewModel.deletePassword(entryData!!.id)
                        Log.d("EditScreen", "Excluindo item com ID: ${entryData!!.id}")
                        showDeleteConfirm = false
                        onUpdateSuccess() // Retorna para a tela principal
                    }
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // 2. Diálogo de Atualização (Usando o componente de formulário)
    if (showUpdateDialog && entryData != null) {
        AddEditPasswordDialog(
            initialData = entryData, // Passa os dados iniciais
            onDismiss = { showUpdateDialog = false },
            onSave = { newService, newUsername, newPassword ->
                // Chama o ViewModel para atualizar e criptografar a nova senha
                viewModel.updatePassword(
                    id = entryData!!.id,
                    newService = newService,
                    newUsername = newUsername,
                    newPassword = newPassword
                )

                Log.d("EditScreen", "Iniciando atualização para item ID: ${entryData!!.id}")
                showUpdateDialog = false
                onUpdateSuccess() // Retorna para a tela principal
            }
        )
    }
}