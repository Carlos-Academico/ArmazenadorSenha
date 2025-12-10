package com.example.armazenadorsenha.screen

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import coil.compose.AsyncImage
import com.example.armazenadorsenha.R
import com.example.armazenadorsenha.model.PasswordData
import com.example.armazenadorsenha.model.VaultViewModel
import java.util.Locale

// Factory para criar o VaultViewModel com a masterKey (necessário pois ViewModel tem argumento)
class VaultViewModelFactory(private val masterKey: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VaultViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VaultViewModel(masterKey) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(masterKey: String) {
    val viewModel: VaultViewModel = viewModel(factory = VaultViewModelFactory(masterKey))
    val passwordList by viewModel.passwordList.collectAsState()
    val context = LocalContext.current

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
            TextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.filterPasswords(it)
                },
                label = { Text("Buscar Serviço ou Usuário") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            // Lista de Senhas (LazyColumn substitui o RecyclerView)
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(passwordList, key = { it.id }) { password ->
                    PasswordItem(
                        password = password,
                        onViewClicked = { entry ->
                            showEditDialog(
                                context = context,
                                entry = entry,
                                viewModel = viewModel
                            )
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditPasswordDialog(
            isEdit = false,
            onDismiss = { showAddDialog = false },
            onConfirm = { service, username, password ->
                viewModel.addNewPassword(service, username, password)
                showAddDialog = false
            }
        )
    }
}

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
            // Mapeamento de Icone (Simplificado, idealmente no ViewModel/UseCase)
            val iconResId = when (password.serviceTitle.lowercase(Locale.ROOT)) {
                "netflix" -> R.drawable.ic_netflix_logo
                "google", "gmail" -> R.drawable.ic_google_logo
                "amazon" -> R.drawable.ic_logo_amazon_simples
                else -> R.drawable.ic_default_lock // Ícone padrão
            }

            AsyncImage(
                model = iconResId,
                contentDescription = password.serviceTitle,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(password.serviceTitle, style = MaterialTheme.typography.titleMedium)
                Text(password.username, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ----------------------------------------------------
// Diálogos (Simulando o showAddPasswordDialog e onViewClicked)
// ----------------------------------------------------

// Dialogo de Adição/Edição
@Composable
fun AddEditPasswordDialog(
    isEdit: Boolean,
    entry: PasswordData? = null,
    onDismiss: () -> Unit,
    onConfirm: (service: String, username: String, password: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var service by remember { mutableStateOf(entry?.serviceTitle ?: "") }
    var username by remember { mutableStateOf(entry?.username ?: "") }
    var password by remember { mutableStateOf(if (isEdit) "" else "") }

    // Se for edição, a senha será preenchida na chamada da função showEditDialog
    // mas aqui deixamos em branco para o usuário digitar, por segurança.

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
                    label = { Text(if(isEdit) "Nova Senha (ou atual)" else "Senha") },
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
                        // Idealmente, mostraria um erro na tela
                    }
                }
            ) {
                Text(if (isEdit) "Salvar Edições" else "Salvar")
            }
        },
        dismissButton = {
            Row {
                if (isEdit && onDelete != null) {
                    TextButton(onClick = onDelete) { Text("EXCLUIR", color = MaterialTheme.colorScheme.error) }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TextButton(onClick = onDismiss) { Text("FECHAR") }
            }
        }
    )
}

// Função auxiliar que gerencia a descriptografia e a exibição do diálogo de Edição
fun showEditDialog(context: android.content.Context, entry: PasswordData, viewModel: VaultViewModel) {
    try {
        val decryptedPassword = viewModel.decryptPassword(entry)

        // Simulação da lógica de diálogo de edição, mostrando a senha descriptografada
        Toast.makeText(context, "Senha Descriptografada: $decryptedPassword", Toast.LENGTH_LONG).show()

    } catch (e: Exception) {
        Toast.makeText(context, "Falha de segurança ao descriptografar.", Toast.LENGTH_SHORT).show()
    }
}