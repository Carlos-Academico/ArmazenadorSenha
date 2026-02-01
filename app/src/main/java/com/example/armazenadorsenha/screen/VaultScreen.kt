package com.example.armazenadorsenha.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController // NOVO IMPORT
import coil.compose.AsyncImage
import com.example.armazenadorsenha.R
import com.example.armazenadorsenha.model.PasswordData
import com.example.armazenadorsenha.model.VaultViewModel
import com.example.armazenadorsenha.Screen // Certifique-se de que o objeto Screen está aqui
import com.example.armazenadorsenha.api.PasswordApiService
import com.example.armazenadorsenha.factory.VaultViewModelFactory
import com.example.armazenadorsenha.repository.PasswordRepository
import com.example.armazenadorsenha.repository.UserRepository
import java.util.Locale

// Factory para criar o VaultViewModel com a masterKey (mantido)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    masterKey: String,
    navController: NavController,
    // NOVO PARÂMETRO: Injeta o Repositório, que será passado para a Factory do ViewModel
    repository: PasswordRepository,
    userRepository: UserRepository
) {
    // 1. CRIAÇÃO DO VIEWMODEL com a Factory
    val factory = remember {
        VaultViewModelFactory(
            masterKey = masterKey,
            passwordRepository = repository, // Corrigido para o nome esperado
            userRepository = userRepository  // NOVO PARÂMETRO PASSADO
        )
    }
    val viewModel: VaultViewModel = viewModel(factory = factory)

    // 2. OBSERVAÇÃO REATIVA: Coleta o StateFlow 'passwordList' do ViewModel.
    // Qualquer mudança no DB reflete aqui, forçando a recomposição.
    val passwordList by viewModel.passwordList.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Meu Cofre") },
                actions = {
                    IconButton(onClick = { /* Ações Futuras */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Mais Opções")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Senha")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // Campo de Pesquisa
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    // Chama o método no ViewModel que atualiza o Flow de pesquisa
                    viewModel.filterPasswords(it)
                },
                label = { Text("Buscar Serviço ou Usuário") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            // Lista de Senhas
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 'passwordList' já contém a lista filtrada e atualizada pelo Flow
                items(passwordList, key = { it.id }) { password ->
                    PasswordItem(
                        password = password,
                        // LÓGICA DE NAVEGAÇÃO para a tela de Edição
                        onViewClicked = { entry ->
                            val route = Screen.EDIT
                                .replace("{masterKey}", masterKey)
                                .replace("{itemId}", entry.id.toString())
                            navController.navigate(route)
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        // Seu Diálogo para Adição
        AddEditPasswordDialog(
            isEdit = false,
            entry = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { service, username, password ->
                // O ViewModel faz a inserção, e o Flow atualiza a lista automaticamente.
                viewModel.addNewPassword(service, username, password)
                showAddDialog = false
            }
        )
    }
}

// ----------------------------------------------------------------------------------
// ** Funções Composable Auxiliares **
// ----------------------------------------------------------------------------------

@Composable
fun PasswordItem(password: PasswordData, onViewClicked: (PasswordData) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewClicked(password) }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- COIL INTEGRADO AQUI ---
            AsyncImage(
                // Se a imageUrl estiver vazia, ele usa o ícone de cadeado padrão
                model = password.imageUrl ?: R.drawable.ic_default_lock,
                contentDescription = password.serviceTitle,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape), // Opcional: deixa a imagem redonda
                placeholder = painterResource(R.drawable.ic_default_lock), // Imagem enquanto carrega
                error = painterResource(R.drawable.ic_default_lock)       // Imagem se der erro
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(password.serviceTitle, style = MaterialTheme.typography.titleMedium)
                Text(
                    password.username,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AddEditPasswordDialog(
    isEdit: Boolean,
    entry: PasswordData? = null,
    onDismiss: () -> Unit,
    onConfirm: (service: String, username: String, password: String) -> Unit,
) {
    var service by remember { mutableStateOf(entry?.serviceTitle ?: "") }
    var username by remember { mutableStateOf(entry?.username ?: "") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Editar Senha: ${entry?.serviceTitle}" else "Registrar Nova Senha") },
        text = {
            Column {
                OutlinedTextField(value = service, onValueChange = { service = it }, label = { Text("Serviço") })
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Usuário") })
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if(isEdit) "Nova Senha" else "Senha") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (service.isNotBlank() && password.isNotBlank()) {
                        onConfirm(service, username, password)
                    } else {
                        // Implementar lógica de erro/snackbar
                    }
                }
            ) {
                Text(if (isEdit) "Salvar Edições" else "Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("FECHAR") }
        }
    )
}