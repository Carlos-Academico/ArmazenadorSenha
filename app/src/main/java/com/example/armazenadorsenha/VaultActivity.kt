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
import com.example.armazenadorsenha.databinding.DialogAddPasswordBinding
import java.util.concurrent.atomic.AtomicInteger

class VaultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVaultBinding
    private lateinit var masterPassword: String
    private lateinit var adapter: PasswordAdapter

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

    /*
    private fun loadSavedData() {
        val loadedList = repository.loadPasswords()

        passwordList.addAll(loadedList)

        if (loadedList.isNotEmpty()) {
            val maxId = loadedList.maxOf { it.id }
            nextId.set(maxId + 1)
        }

        // NOVO USO: Popula a lista completa do adapter
        adapter.updateFullList(passwordList.toList())
    }
    */

    private fun onViewClicked(entry: PasswordData) {
        try {
            val decryptedPassword = EncryptionHelper.decrypt(
                entry.encryptedPasswordBase64,
                entry.ivBase64,
                masterPassword
            )

            AlertDialog.Builder(this)
                .setTitle(entry.serviceTitle)
                .setMessage("Usuário: ${entry.username}\nSenha: $decryptedPassword")
                .setPositiveButton("Fechar", null)
                .show()

        } catch (e: SecurityException) {
            Toast.makeText(this, "Falha de segurança: chave inválida.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        adapter = PasswordAdapter(onViewClicked = ::onViewClicked)
        binding.recyclerViewVault.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewVault.adapter = adapter

        adapter.updateFullList(passwordList.toList())
    }

    private fun showAddPasswordDialog() {
        // ... (lógica do Dialog Add Password) ...
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