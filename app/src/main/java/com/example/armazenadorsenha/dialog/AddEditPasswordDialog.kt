package com.example.armazenadorsenha.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.armazenadorsenha.model.PasswordData

@Composable
fun AddEditPasswordDialog(
    // Dados iniciais (null para Adição, PasswordData para Edição)
    initialData: PasswordData?,
    onDismiss: () -> Unit,
    // Callback que retorna os novos valores inseridos pelo usuário
    onSave: (service: String, username: String, password: String) -> Unit
) {
    // 1. Estados do Formulário
    var newService by remember { mutableStateOf(initialData?.serviceTitle ?: "") }
    var newUsername by remember { mutableStateOf(initialData?.username ?: "") }
    var newPassword by remember { mutableStateOf("") } // A senha SEMPRE deve começar vazia para edição
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Verifica se todos os campos estão preenchidos para habilitar o botão
    // Se for EDIÇÃO (initialData != null), a nova senha é obrigatória.
    val isFormValid = newService.isNotBlank() && newUsername.isNotBlank() && newPassword.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialData == null) "Adicionar Nova Senha" else "Atualizar Senha: ${initialData.serviceTitle}") },
        text = {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                // Campo Serviço
                OutlinedTextField(
                    value = newService,
                    onValueChange = { newService = it },
                    label = { Text("Serviço") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Campo Usuário
                OutlinedTextField(
                    value = newUsername,
                    onValueChange = { newUsername = it },
                    label = { Text("Usuário") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Campo Senha (Sempre solicita a nova senha)
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(if (initialData == null) "Senha" else "Nova Senha") },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (isPasswordVisible) "Ocultar senha" else "Visualizar senha"
                            )
                        }
                    }
                )

                if (initialData != null) {
                    Text(
                        "Insira a NOVA senha para substituir a anterior (Obrigatório).",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(newService, newUsername, newPassword) },
                enabled = isFormValid
            ) {
                Text(if (initialData == null) "Salvar" else "Atualizar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}