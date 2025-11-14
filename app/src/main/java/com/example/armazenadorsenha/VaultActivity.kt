package com.example.armazenadorsenha

import android.os.Bundle
import androidx.appcompat.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.armazenadorsenha.adapter.PasswordAdapter
import com.example.armazenadorsenha.dao.PasswordData
import com.example.armazenadorsenha.databinding.ActivityVaultBinding
import com.example.armazenadorsenha.databinding.DialogAddPasswordBinding // Reutilizado para Edit/View
import java.util.concurrent.atomic.AtomicInteger

// IMPORTANTE: Assumindo que você tem as classes EncryptionHelper e o modelo PasswordData

class VaultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVaultBinding
    private lateinit var masterPassword: String
    private lateinit var adapter: PasswordAdapter

    // A lista de senhas, que NÃO é persistente
    private val passwordList = mutableListOf<PasswordData>()
    private val nextId = AtomicInteger(1)

    companion object {
        const val EXTRA_MASTER_PASSWORD = "extra_master_password"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVaultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        masterPassword = intent.getStringExtra(EXTRA_MASTER_PASSWORD) ?: run {
            Toast.makeText(this, "Erro de autenticação.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupRecyclerView()
        setupSearch()

        binding.fabAddPassword.setOnClickListener {
            showAddPasswordDialog()
        }
    }

    // Método para abrir o diálogo de Visualização/Edição
    private fun onViewClicked(entry: PasswordData) {
        // Reutiliza o binding do diálogo de adição para visualização e edição
        val dialogBinding = DialogAddPasswordBinding.inflate(layoutInflater)

        try {
            val decryptedPassword = EncryptionHelper.decrypt(
                entry.encryptedPasswordBase64,
                entry.ivBase64,
                masterPassword
            )

            // 1. Pré-preencher campos com os dados existentes
            dialogBinding.editService.setText(entry.serviceTitle)
            dialogBinding.editUsername.setText(entry.username)
            dialogBinding.editPassword.setText(decryptedPassword)

            // 2. Configurar o diálogo
            AlertDialog.Builder(this)
                .setTitle("Detalhes e Edição: ${entry.serviceTitle}")
                .setView(dialogBinding.root)

                // Botão de Excluir
                .setNeutralButton("EXCLUIR") { _, _ ->
                    confirmDelete(entry)
                }

                // Botão de Salvar/Editar
                .setPositiveButton("SALVAR EDIÇÕES") { _, _ ->
                    // Coleta os dados editados
                    val newService = dialogBinding.editService.text.toString()
                    val newUsername = dialogBinding.editUsername.text.toString()
                    val newPlainPassword = dialogBinding.editPassword.text.toString()

                    if (newService.isNotBlank() && newPlainPassword.isNotBlank()) {
                        editExistingPassword(entry, newService, newUsername, newPlainPassword)
                    } else {
                        Toast.makeText(this, "Serviço e Senha são obrigatórios.", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("FECHAR", null)
                .show()

        } catch (e: SecurityException) {
            Toast.makeText(this, "Falha de segurança: chave inválida.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        adapter = PasswordAdapter(onViewClicked = ::onViewClicked)
        binding.recyclerViewVault.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewVault.adapter = adapter

        // Popula a lista completa
        adapter.updateFullList(passwordList.toList())
    }

    private fun showAddPasswordDialog() {
        val dialogBinding = DialogAddPasswordBinding.inflate(layoutInflater)

        AlertDialog.Builder(this)
            .setTitle("Registrar Nova Senha")
            .setView(dialogBinding.root)
            .setPositiveButton("Salvar") { dialog, which ->
                val service = dialogBinding.editService.text.toString()
                val username = dialogBinding.editUsername.text.toString()
                val plainPassword = dialogBinding.editPassword.text.toString()

                if (service.isNotBlank() && plainPassword.isNotBlank()) {
                    addNewPassword(service, username, plainPassword)
                } else {
                    Toast.makeText(this, "Serviço e Senha são obrigatórios.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {
                adapter.filter(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter(newText)
                return true
            }
        })
    }

    // --- MÉTODO: CONFIRMAR EXCLUSÃO ---
    private fun confirmDelete(entry: PasswordData) {
        AlertDialog.Builder(this)
            .setTitle("Confirmação de Exclusão")
            .setMessage("Tem certeza que deseja EXCLUIR a senha para '${entry.serviceTitle}'? Esta ação é irreversível.")
            .setPositiveButton("Sim, Excluir") { _, _ ->
                deletePassword(entry)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // --- MÉTODO: DELETAR SENHA ---
    private fun deletePassword(entry: PasswordData) {
        // Remove o item da lista em memória
        val removed = passwordList.removeIf { it.id == entry.id }

        if (removed) {
            // Atualiza o Adapter com a lista modificada (importante para o filtro)
            adapter.updateFullList(passwordList.toList())

            Toast.makeText(this, "Senha para ${entry.serviceTitle} removida.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- MÉTODO: EDITAR SENHA EXISTENTE ---
    private fun editExistingPassword(
        originalEntry: PasswordData,
        newService: String,
        newUsername: String,
        newPlainPassword: String
    ) {
        // Criptografa a nova senha
        val (encryptedPass, iv) = EncryptionHelper.encrypt(newPlainPassword, masterPassword)

        // Cria uma nova entrada com o ID original
        val updatedEntry = originalEntry.copy(
            serviceTitle = newService,
            username = newUsername,
            encryptedPasswordBase64 = encryptedPass,
            ivBase64 = iv
        )

        // Encontra e substitui o item na lista em memória
        val index = passwordList.indexOfFirst { it.id == originalEntry.id }
        if (index != -1) {
            passwordList[index] = updatedEntry

            // Atualiza o Adapter com a lista modificada
            adapter.updateFullList(passwordList.toList())

            Toast.makeText(this, "Senha para ${newService} atualizada.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- MÉTODO: ADICIONAR NOVA SENHA ---
    private fun addNewPassword(service: String, username: String, plainPassword: String) {
        val (encryptedPass, iv) = EncryptionHelper.encrypt(plainPassword, masterPassword)

        val newEntry = PasswordData(
            id = nextId.getAndIncrement(),
            serviceTitle = service,
            username = username,
            encryptedPasswordBase64 = encryptedPass,
            ivBase64 = iv
        )

        passwordList.add(newEntry)
        adapter.updateFullList(passwordList.toList())
        Toast.makeText(this, "Senha de $service registrada com sucesso!", Toast.LENGTH_SHORT).show()
    }
}